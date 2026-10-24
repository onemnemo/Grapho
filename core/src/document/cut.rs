//! Cutting a stroke where an eraser passes over it.
//!
//! The eraser is a circle of radius `r` swept along a segment. A point of a
//! stroke is covered when it lies within `r` plus the stroke's own half width
//! of that segment, the same test [`Stroke::hits`] uses. Each stroke segment is
//! solved for the exact span it is covered over, so the cut ends sit on the
//! eraser's edge instead of snapping to the nearest sample.

mod fill;
mod keyhole;
#[cfg(test)]
mod stress;

use super::{Stroke, Tool};
use crate::InputSample;
use fill::cut_fill;

/// Pieces shorter than this (in points) are too small to draw and are dropped.
const MIN_PIECE_LEN: f32 = 0.25;

/// How far (in points) the eraser must reach into a stroke before it counts
/// as covering it. Cut ends land where the eraser's edge just touches, so
/// without this margin a piece's own end would be cut again and again by an
/// eraser held still.
const MIN_BITE: f32 = 0.01;

/// A sample position along a stroke: whole numbers are samples, the fraction
/// is how far to the next one.
type Pos = f32;

/// What an eraser sweep did to one stroke.
#[derive(Debug, PartialEq)]
pub(super) enum Cut {
    /// The eraser did not reach the stroke.
    Miss,
    /// The stroke is gone, and these are the parts that survive (maybe none).
    Pieces(Vec<Stroke>),
}

/// Cuts `stroke` where a circle of radius `r` moving from `(x0, y0)` to
/// `(x1, y1)` covers it. A fill is cut the same way, as an area.
pub(super) fn cut_stroke(stroke: &Stroke, x0: f32, y0: f32, x1: f32, y1: f32, r: f32) -> Cut {
    let pts = &stroke.points;
    if pts.is_empty() {
        return Cut::Miss;
    }
    let reach = r + stroke.style.width * 0.5;
    let Some((bx0, by0, bx1, by1)) = stroke.bounds() else {
        return Cut::Miss;
    };
    if x0.max(x1) + reach < bx0
        || x0.min(x1) - reach > bx1
        || y0.max(y1) + reach < by0
        || y0.min(y1) - reach > by1
    {
        return Cut::Miss;
    }
    if stroke.style.tool == Tool::Fill {
        return cut_fill(stroke, x0, y0, x1, y1, r);
    }
    let brush = stroke.style.brush();
    let half = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
    let gap = |px: f32, py: f32| dist_to_segment(px, py, x0, y0, x1, y1);

    if let [p] = pts.as_slice() {
        return if gap(p.x, p.y) <= r + half(p) {
            Cut::Pieces(Vec::new())
        } else {
            Cut::Miss
        };
    }

    let (ex0, ex1) = (x0.min(x1), x0.max(x1));
    let (ey0, ey1) = (y0.min(y1), y0.max(y1));
    // Covered spans in stroke positions, in order.
    let mut spans: Vec<(Pos, Pos)> = Vec::new();
    for (i, w) in pts.windows(2).enumerate() {
        let (a, b) = (&w[0], &w[1]);
        let (ha, hb) = (half(a), half(b));
        // A segment whose box, grown by the widest reach, misses the eraser's
        // box cannot be covered, so the search is skipped.
        let grow = r + ha.max(hb);
        if a.x.max(b.x) + grow < ex0
            || a.x.min(b.x) - grow > ex1
            || a.y.max(b.y) + grow < ey0
            || a.y.min(b.y) - grow > ey1
        {
            continue;
        }
        // Distance to the eraser minus the allowed reach. Convex along the
        // segment because the half width is linear in it.
        let over =
            |t: f32| gap(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) - r - (ha + (hb - ha) * t);
        let Some((t0, t1)) = covered_span(over) else {
            continue;
        };
        let (s, e) = (i as Pos + t0, i as Pos + t1);
        match spans.last_mut() {
            Some(last) if s <= last.1 + 1e-4 => last.1 = last.1.max(e),
            _ => spans.push((s, e)),
        }
    }
    if spans.is_empty() {
        return Cut::Miss;
    }

    let end = (pts.len() - 1) as Pos;
    let mut pieces = Vec::new();
    let mut from = 0.0;
    for &(s, e) in &spans {
        if s > from {
            pieces.extend(piece(stroke, from, s));
        }
        from = e;
    }
    if from < end {
        pieces.extend(piece(stroke, from, end));
    }
    Cut::Pieces(pieces)
}

/// The sub-interval of `[0, 1]` where the convex function `over` is at most
/// zero, or `None` if it never is.
fn covered_span(over: impl Fn(f32) -> f32) -> Option<(f32, f32)> {
    let (mut lo, mut hi) = (0.0_f32, 1.0_f32);
    for _ in 0..24 {
        let third = (hi - lo) / 3.0;
        if over(lo + third) < over(hi - third) {
            hi -= third;
        } else {
            lo += third;
        }
    }
    let low = [0.0, (lo + hi) * 0.5, 1.0]
        .into_iter()
        .map(|t| (t, over(t)))
        .min_by(|a, b| a.1.total_cmp(&b.1))?;
    if low.1 > -MIN_BITE {
        return None;
    }
    let tm = low.0;
    let start = if over(0.0) <= 0.0 {
        0.0
    } else {
        bisect(&over, 0.0, tm)
    };
    let end = if over(1.0) <= 0.0 {
        1.0
    } else {
        bisect(&over, 1.0, tm)
    };
    Some((start, end))
}

/// Finds where `over` crosses zero between `outside` (positive) and `inside`
/// (zero or less), returning the point just outside, so what is left of the
/// stroke ends clear of the eraser.
fn bisect(over: &impl Fn(f32) -> f32, mut outside: f32, mut inside: f32) -> f32 {
    for _ in 0..24 {
        let mid = (outside + inside) * 0.5;
        if over(mid) <= 0.0 {
            inside = mid;
        } else {
            outside = mid;
        }
    }
    outside
}

/// The part of `stroke` between two positions as a stroke of its own, or
/// `None` if it is too short to draw.
fn piece(stroke: &Stroke, from: Pos, to: Pos) -> Option<Stroke> {
    let pts = &stroke.points;
    let mut out = Vec::with_capacity(pts.len());
    out.push(sample_at(pts, from));
    // Whole samples strictly between the two ends.
    let first = from.floor() as usize + 1;
    let last = to.ceil() as usize;
    for p in pts.iter().take(last).skip(first) {
        out.push(*p);
    }
    out.push(sample_at(pts, to));
    out.dedup_by(|b, a| a.x == b.x && a.y == b.y);
    let length: f32 = out
        .windows(2)
        .map(|w| (w[1].x - w[0].x).hypot(w[1].y - w[0].y))
        .sum();
    if out.len() < 2 || length < MIN_PIECE_LEN {
        return None;
    }
    // Sample times are offsets from the stroke start, so the piece starts
    // when its first sample was drawn.
    let shift = out[0].time_ms.round();
    for p in &mut out {
        p.time_ms -= shift;
    }
    Some(Stroke::new(
        stroke.style,
        stroke.started_at_ms + shift as i64,
        out,
    ))
}

/// The sample at a position, interpolating between neighbours.
fn sample_at(pts: &[InputSample], pos: Pos) -> InputSample {
    let i = (pos.floor() as usize).min(pts.len() - 1);
    let t = pos - i as f32;
    if t <= 0.0 || i + 1 >= pts.len() {
        return pts[i];
    }
    let (a, b) = (&pts[i], &pts[i + 1]);
    let mix = |u: f32, v: f32| u + (v - u) * t;
    InputSample::new(
        mix(a.x, b.x),
        mix(a.y, b.y),
        mix(a.pressure, b.pressure),
        mix(a.time_ms, b.time_ms),
    )
}

fn dist_to_segment(px: f32, py: f32, ax: f32, ay: f32, bx: f32, by: f32) -> f32 {
    let (dx, dy) = (bx - ax, by - ay);
    let len2 = dx * dx + dy * dy;
    let t = if len2 > 0.0 {
        (((px - ax) * dx + (py - ay) * dy) / len2).clamp(0.0, 1.0)
    } else {
        0.0
    };
    (px - ax - dx * t).hypot(py - ay - dy * t)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::StrokeStyle;

    fn stroke(tool: Tool) -> Stroke {
        let pts = (0..=10)
            .map(|i| InputSample::new(i as f32 * 10.0, 0.0, 0.5 + i as f32 * 0.04, i as f32 * 8.0))
            .collect();
        Stroke::new(
            StrokeStyle {
                tool,
                color: 0x1234_56FF,
                width: 2.0,
                line: crate::LineStyle::Solid,
            },
            1000,
            pts,
        )
    }

    fn pieces(c: Cut) -> Vec<Stroke> {
        match c {
            Cut::Pieces(p) => p,
            Cut::Miss => panic!("expected a cut"),
        }
    }

    #[test]
    fn a_cut_ends_on_the_eraser_edge() {
        let p = pieces(cut_stroke(&stroke(Tool::Pen), 50.0, 0.0, 50.0, 0.0, 5.0));
        assert_eq!(p.len(), 2);
        let left = p[0].points.last().unwrap();
        let right = p[1].points.first().unwrap();
        // Edge is the radius plus the stroke's half width away, not a sample.
        assert!(left.x > 40.0 && left.x < 50.0, "{}", left.x);
        assert!(right.x > 50.0 && right.x < 60.0, "{}", right.x);
        assert!((50.0 - left.x - (right.x - 50.0)).abs() < 0.5);
    }

    #[test]
    fn pieces_keep_style_and_time() {
        let s = stroke(Tool::Pen);
        let p = pieces(cut_stroke(&s, 50.0, 0.0, 50.0, 0.0, 5.0));
        assert!(p.iter().all(|q| q.style == s.style && q.id != s.id));
        assert_eq!(p[0].started_at_ms, 1000);
        assert_eq!(p[0].points[0].time_ms, 0.0);
        // The second piece starts later and counts time from its own start.
        assert!(p[1].started_at_ms > 1000);
        assert!(p[1].points[0].time_ms.abs() <= 0.5);
        assert!(p[1].points[0].pressure > 0.5);
    }

    #[test]
    fn misses_are_left_alone() {
        assert_eq!(
            cut_stroke(&stroke(Tool::Pen), 50.0, 30.0, 50.0, 30.0, 5.0),
            Cut::Miss
        );
    }

    #[test]
    fn a_sweep_across_everything_leaves_nothing() {
        let p = pieces(cut_stroke(&stroke(Tool::Pen), -10.0, 0.0, 110.0, 0.0, 5.0));
        assert!(p.is_empty());
    }

    #[test]
    fn an_end_cut_leaves_one_piece() {
        let p = pieces(cut_stroke(&stroke(Tool::Pen), 0.0, 0.0, 0.0, 0.0, 5.0));
        assert_eq!(p.len(), 1);
        assert!(p[0].points[0].x > 5.0);
        assert_eq!(p[0].points.last().unwrap().x, 100.0);
    }

    #[test]
    fn slivers_are_dropped() {
        for k in 0..30 {
            let x = 5.0 + k as f32 * 0.1;
            let p = pieces(cut_stroke(&stroke(Tool::Pen), x, 0.0, 120.0, 0.0, 5.0));
            assert!(p.iter().all(|q| q.points.len() >= 2));
            assert!(p.iter().all(|q| {
                let (a, b) = (q.points[0].x, q.points.last().unwrap().x);
                b - a >= MIN_PIECE_LEN
            }));
        }
    }
}
