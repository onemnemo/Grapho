use crate::{Brush, InputSample, Rect};
mod crossing;
mod union;

use crossing::crosses_itself;
use std::f32::consts::{PI, TAU};
use union::{signed_area, union};

/// Turns a finished stroke into filled polygons for antialiased drawing.
///
/// The output is packed as `[n, x0, y0, ..., x(n-1), y(n-1), n, ...]`: a
/// vertex count followed by that many `[x, y]` pairs, for each contour.
///
/// The stroke's shape is a disc at every point, as wide as the pressure
/// there, joined to the next disc by the lines tangent to both. Most of a
/// stroke is drawn as one contour: the left edge forward, a round cap, the
/// right edge back, and a round cap. That contour folds over itself on the
/// inside of a tight curve or a sharp bend, and where the width changes
/// faster than the stroke moves, as when the pen lands or lifts, it misses
/// the back of the wider disc and can turn inside out. So the stroke is
/// split into runs of segments where neither happens, and each run gets one
/// such contour. Every other segment gets its own piece, the convex hull of
/// its two discs.
///
/// The contours and pieces are then merged into their union, so the result
/// has no edges inside the shape: a GPU path renderer draws every edge with
/// its own antialiasing, and an edge inside the ink, such as where a stroke
/// crosses itself, shows as a thin dark seam. Outer contours of the union
/// wind one way and holes, such as the inside of a stroke that loops around,
/// wind the other, so both the non-zero and the even-odd rule fill the
/// shape. No two contours overlap or share an edge. A stroke that is one
/// contour which does not cross itself skips the union.
///
/// `tolerance` is how far a round part may stray from a true circle, in the
/// same units as the points.
///
/// Returns the bounds of everything drawn.
pub fn outline(points: &[InputSample], brush: &Brush, tolerance: f32, out: &mut Vec<f32>) -> Rect {
    let (pts, origin) = local(points);
    let start = out.len();
    let bounds = outline_local(&pts, brush, tolerance, out);
    shift(out, start, origin);
    moved(bounds, origin)
}

fn outline_local(pts: &[InputSample], brush: &Brush, tolerance: f32, out: &mut Vec<f32>) -> Rect {
    let mut bounds = Rect::EMPTY;
    let radius = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
    for p in pts {
        bounds.add_circle(p.x, p.y, radius(p));
    }
    match pts.len() {
        0 => {}
        1 => disc(pts[0].x, pts[0].y, radius(&pts[0]), tolerance, out),
        _ => {
            let normals = left_normals(pts);
            let rough = rough_segments(pts, &radius);
            let mut raw = Vec::new();
            let mut start = 0;
            for i in 0..=rough.len() {
                if i < rough.len() && !rough[i] {
                    continue;
                }
                if i > start {
                    let run = start..i + 1;
                    push_contour(&pts[run], &normals[start..i], &radius, tolerance, &mut raw);
                }
                if i < rough.len() {
                    let mut poly = Vec::new();
                    hull(&pts[i], &pts[i + 1], &radius, tolerance, &mut poly);
                    push_polygon(&poly, &mut raw);
                }
                start = i + 1;
            }
            if raw[0] as usize * 2 + 1 == raw.len() && !crosses_itself(&raw[1..]) {
                out.extend_from_slice(&raw);
            } else {
                union(&raw, out);
            }
        }
    }
    bounds
}

fn push_polygon(poly: &[[f32; 2]], out: &mut Vec<f32>) {
    out.push(poly.len() as f32);
    for v in poly {
        out.extend_from_slice(v);
    }
}

/// Left-hand unit normal of each segment.
fn left_normals(pts: &[InputSample]) -> Vec<[f32; 2]> {
    pts.windows(2)
        .map(|w| {
            let (dx, dy) = (w[1].x - w[0].x, w[1].y - w[0].y);
            let len = dx.hypot(dy);
            [-dy / len, dx / len]
        })
        .collect()
}

/// Marks each segment that a run contour cannot draw: one whose edge may
/// fold, with a sharp bend at either end or end insets (radius times the
/// tangent of half the turn) that together are not shorter than the segment,
/// or one whose width changes too fast for edges drawn straight out from the
/// points to follow the discs.
fn rough_segments(pts: &[InputSample], radius: &dyn Fn(&InputSample) -> f32) -> Vec<bool> {
    // Inset of the edge at each point, or infinity at a sharp bend.
    let inset: Vec<f32> = (0..pts.len())
        .map(|k| {
            if k == 0 || k + 1 == pts.len() {
                return 0.0;
            }
            let (a, p, b) = (&pts[k - 1], &pts[k], &pts[k + 1]);
            let (ux, uy) = (p.x - a.x, p.y - a.y);
            let (vx, vy) = (b.x - p.x, b.y - p.y);
            let cos = (ux * vx + uy * vy) / (ux.hypot(uy) * vx.hypot(vy));
            if cos.is_nan() || cos < MAX_BEVEL_TURN_COS {
                f32::INFINITY
            } else {
                // Rounding can put the cosine of a straight bend just over
                // 1. The root of the negative number there would be NaN,
                // which hides a sharp bend next to it from the check below.
                radius(p) * ((1.0 - cos) / (1.0 + cos)).max(0.0).sqrt()
            }
        })
        .collect();
    pts.windows(2)
        .enumerate()
        .map(|(i, w)| {
            let len = (w[1].x - w[0].x).hypot(w[1].y - w[0].y);
            let taper = (radius(&w[1]) - radius(&w[0])).abs();
            inset[i] + inset[i + 1] >= len || taper > MAX_TAPER * len
        })
        .collect()
}

/// How much the radius may change along a run segment, per unit of length.
/// Edges drawn straight out from the points lie inside the true tangent by
/// about `radius * (1 - cos(asin(taper)))`, a fraction of a percent of the
/// width at this slope.
const MAX_TAPER: f32 = 0.1;

/// Appends one closed contour around a run of points with gentle bends: the
/// left edge forward, a round cap, the right edge back, and a round cap.
/// `normals` holds the left unit normal of each segment of the run.
fn push_contour(
    pts: &[InputSample],
    normals: &[[f32; 2]],
    radius: &dyn Fn(&InputSample) -> f32,
    tolerance: f32,
    out: &mut Vec<f32>,
) {
    let last = pts.len() - 1;
    let mut c: Vec<[f32; 2]> = Vec::with_capacity(pts.len() * 2 + 16);
    let angle = |n: [f32; 2]| n[1].atan2(n[0]);
    let sides = |r: f32| crate::mesh::disc_sides(r, tolerance);
    let edge = |i: usize, side: f32| {
        let n = match i {
            0 => normals[0],
            i if i == last => normals[last - 1],
            i => bisector(normals[i - 1], normals[i]),
        };
        let (p, r) = (&pts[i], radius(&pts[i]));
        [p.x + side * n[0] * r, p.y + side * n[1] * r]
    };
    for i in 0..=last {
        c.push(edge(i, 1.0));
    }
    let r = radius(&pts[last]);
    let cap = angle(normals[last - 1]);
    arc(&pts[last], r, cap, -PI, sides(r), false, &mut c);
    for i in (0..=last).rev() {
        c.push(edge(i, -1.0));
    }
    let r = radius(&pts[0]);
    arc(
        &pts[0],
        r,
        angle(normals[0]) + PI,
        -PI,
        sides(r),
        false,
        &mut c,
    );
    // Wind positive, like the outer contours of a union.
    if signed_area(&c) < 0.0 {
        c.reverse();
    }
    push_polygon(&c, out);
}

/// Direction halfway between two unit normals. Only used for gentle bends,
/// where the normals are never opposite.
fn bisector(a: [f32; 2], b: [f32; 2]) -> [f32; 2] {
    let (x, y) = (a[0] + b[0], a[1] + b[1]);
    let len = x.hypot(y);
    [x / len, y / len]
}

/// A bend turning more than this is treated as sharp.
const MAX_BEVEL_TURN_COS: f32 = 0.8;

/// The same packing as [`outline`], but with one piece per segment as it
/// is: the convex hull of the segment's two discs, overlapping its
/// neighbours, all with one orientation. Filling them with the non-zero rule
/// gives the stroke, which is enough for callers that rasterise it
/// themselves and saves the cost of the union.
pub fn pieces(points: &[InputSample], brush: &Brush, tolerance: f32, out: &mut Vec<f32>) -> Rect {
    let (pts, origin) = local(points);
    let start = out.len();
    let bounds = pieces_local(&pts, brush, tolerance, out);
    shift(out, start, origin);
    moved(bounds, origin)
}

fn pieces_local(pts: &[InputSample], brush: &Brush, tolerance: f32, out: &mut Vec<f32>) -> Rect {
    let mut bounds = Rect::EMPTY;
    let radius = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
    for p in pts {
        bounds.add_circle(p.x, p.y, radius(p));
    }
    match pts.len() {
        0 => {}
        1 => disc(pts[0].x, pts[0].y, radius(&pts[0]), tolerance, out),
        _ => {
            let mut poly = Vec::new();
            for w in pts.windows(2) {
                poly.clear();
                hull(&w[0], &w[1], &radius, tolerance, &mut poly);
                push_polygon(&poly, out);
            }
        }
    }
    bounds
}

/// The convex hull of the discs around `a` and `b`, winding positive: an
/// arc around `a` and an arc around `b`, joined by the two lines tangent to
/// both discs. When one disc holds the other, it is just that disc.
fn hull(
    a: &InputSample,
    b: &InputSample,
    radius: &dyn Fn(&InputSample) -> f32,
    tolerance: f32,
    out: &mut Vec<[f32; 2]>,
) {
    let (ra, rb) = (radius(a), radius(b));
    let d = (b.x - a.x).hypot(b.y - a.y);
    let sides = |r: f32| crate::mesh::disc_sides(r, tolerance);
    if d <= (ra - rb).abs() {
        let (p, r) = if ra >= rb { (a, ra) } else { (b, rb) };
        arc(p, r, 0.0, TAU, sides(r), false, out);
        out.push([p.x + r, p.y]);
        return;
    }
    let toward = (b.y - a.y).atan2(b.x - a.x);
    // Angle from the direction of travel to the tangent points.
    let phi = ((ra - rb) / d).clamp(-1.0, 1.0).acos();
    arc(a, ra, toward + phi, TAU - 2.0 * phi, sides(ra), true, out);
    arc(b, rb, toward - phi, 2.0 * phi, sides(rb), true, out);
}

fn cross(a: [f32; 2], b: [f32; 2]) -> f32 {
    a[0] * b[1] - a[1] * b[0]
}

/// Appends points around `p` from `start` through `sweep` radians. With
/// `ends` the first and last points are included, otherwise only those
/// between them, for callers that already have the end points.
fn arc(
    p: &InputSample,
    r: f32,
    start: f32,
    sweep: f32,
    sides: usize,
    ends: bool,
    out: &mut Vec<[f32; 2]>,
) {
    let step = TAU / sides as f32;
    let steps = ((sweep.abs() / step).ceil() as usize).max(1);
    let (from, to) = if ends { (0, steps + 1) } else { (1, steps) };
    for k in from..to {
        let a = start + sweep * k as f32 / steps as f32;
        out.push([p.x + r * a.cos(), p.y + r * a.sin()]);
    }
}

/// The stroke moved so its first point is at the origin, with points that
/// sit on top of the previous one dropped, and the offset to move it back.
///
/// Far from the page's origin, as on an infinite canvas, an `f32` holds a
/// position only to a few ten-thousandths of a point, so normals and
/// crossings computed there come out wrong for closely spaced points. Near
/// the origin the same differences are exact.
fn local(points: &[InputSample]) -> (Vec<InputSample>, [f32; 2]) {
    let Some(first) = points.first() else {
        return (Vec::new(), [0.0, 0.0]);
    };
    let origin = [first.x, first.y];
    let moved: Vec<InputSample> = points
        .iter()
        .map(|p| InputSample {
            x: p.x - origin[0],
            y: p.y - origin[1],
            ..*p
        })
        .collect();
    (distinct(&moved), origin)
}

/// Moves every vertex of the contours packed into `out` from `start` on by
/// `origin`.
fn shift(out: &mut [f32], start: usize, origin: [f32; 2]) {
    let mut i = start;
    while i < out.len() {
        let n = out[i] as usize;
        for v in out[i + 1..i + 1 + 2 * n].as_chunks_mut::<2>().0 {
            v[0] += origin[0];
            v[1] += origin[1];
        }
        i += 1 + 2 * n;
    }
}

/// `r` moved by `origin`.
fn moved(r: Rect, origin: [f32; 2]) -> Rect {
    Rect {
        min_x: r.min_x + origin[0],
        min_y: r.min_y + origin[1],
        max_x: r.max_x + origin[0],
        max_y: r.max_y + origin[1],
    }
}

/// Points closer than this to the previous one are dropped: they carry no
/// direction the stroke's outline could follow.
const MIN_STEP: f32 = 0.01;

/// Drops points that sit on top of the previous one, which have no direction.
fn distinct(points: &[InputSample]) -> Vec<InputSample> {
    let mut out: Vec<InputSample> = Vec::with_capacity(points.len());
    for p in points {
        match out.last_mut() {
            Some(last) if (p.x - last.x).hypot(p.y - last.y) < MIN_STEP => {
                last.pressure = last.pressure.max(p.pressure);
            }
            _ => out.push(*p),
        }
    }
    out
}

fn disc(cx: f32, cy: f32, r: f32, tolerance: f32, out: &mut Vec<f32>) {
    let n = crate::mesh::disc_sides(r, tolerance);
    let step = TAU / n as f32;
    out.push(n as f32);
    for i in 0..n {
        let a = i as f32 * step;
        out.extend_from_slice(&[cx + r * a.cos(), cy + r * a.sin()]);
    }
}

#[cfg(test)]
mod tests;
