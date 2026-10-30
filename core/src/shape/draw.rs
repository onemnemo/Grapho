//! Shapes drawn on purpose: the Shapes tool turns a drag from one corner to
//! the opposite one into a clean outline.

use super::geom::{add, dist, lerp, scale, sub, unit, P};
use super::model::Shape;
use std::f32::consts::{FRAC_PI_2, FRAC_PI_6, PI, TAU};

/// A drag shorter than this on an axis draws no closed shape.
const MIN_DRAG: f32 = 2.0;
/// Fewest and most sides or points a polygon or star takes.
const MIN_SIDES: u32 = 3;
const MAX_SIDES: u32 = 12;
/// How deep a star's notches may cut, as a fraction of the way to the centre.
const MIN_SHARPNESS: f32 = 0.1;
const MAX_SHARPNESS: f32 = 0.9;
/// Points along a curved line, and along each rounded corner.
const CURVE_STEPS: usize = 40;
const CORNER_STEPS: usize = 8;
/// Angle either side of the line that the barbs of an arrow end open to.
const BARB_SPREAD: f32 = FRAC_PI_6;

/// What the Shapes tool draws.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum DrawKind {
    /// A rectangle filling the drag, with corners rounded by `radius`.
    Box { radius: f32 },
    /// An ellipse filling the drag.
    Oval,
    /// A regular polygon with `sides` sides, point up, filling the drag.
    Polygon { sides: u32 },
    /// A star with `points` points. `sharpness` runs from 0 to 1: how deep
    /// the notches cut.
    Star { points: u32, sharpness: f32 },
    /// A straight line from the start of the drag to its end.
    Line,
    /// Two legs and a middle one at right angles, like a connector.
    Elbow,
    /// A smooth S between the two ends.
    Curved,
}

/// A kind of shape and how its line ends. Line kinds ([`DrawKind::Line`],
/// [`DrawKind::Elbow`], [`DrawKind::Curved`]) take arrow heads of size `head`
/// at either end; closed shapes ignore them.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct DrawSpec {
    pub kind: DrawKind,
    pub head: f32,
    pub start_arrow: bool,
    pub end_arrow: bool,
}

impl DrawSpec {
    pub fn new(kind: DrawKind) -> Self {
        Self {
            kind,
            head: 0.0,
            start_arrow: false,
            end_arrow: false,
        }
    }
}

/// The shape for a drag from `from` to `to`, or None when the drag is too
/// small or not finite. Closed shapes fill the box the two points span.
pub fn draw_shape(spec: &DrawSpec, from: P, to: P) -> Option<Shape> {
    if !from.iter().chain(to.iter()).all(|v| v.is_finite()) {
        return None;
    }
    let lo = [from[0].min(to[0]), from[1].min(to[1])];
    let hi = [from[0].max(to[0]), from[1].max(to[1])];
    let (w, h) = (hi[0] - lo[0], hi[1] - lo[1]);
    let closed = w >= MIN_DRAG && h >= MIN_DRAG;
    match spec.kind {
        DrawKind::Box { radius } => closed.then(|| rounded_box(lo, hi, radius)),
        DrawKind::Oval => closed.then(|| Shape::Ellipse {
            center: [lo[0] + w / 2.0, lo[1] + h / 2.0],
            rx: w / 2.0,
            ry: h / 2.0,
            rotation: 0.0,
        }),
        DrawKind::Polygon { sides } => closed.then(|| {
            let n = sides.clamp(MIN_SIDES, MAX_SIDES) as usize;
            let corners: Vec<P> = (0..n).map(|i| on_circle(i, n, 1.0)).collect();
            Shape::Polygon {
                corners: fit_to_box(&corners, lo, hi),
            }
        }),
        DrawKind::Star { points, sharpness } => closed.then(|| {
            let n = points.clamp(MIN_SIDES, MAX_SIDES) as usize;
            let inner = 1.0 - sharpness.clamp(MIN_SHARPNESS, MAX_SHARPNESS);
            let corners: Vec<P> = (0..2 * n)
                .map(|i| on_circle(i, 2 * n, if i % 2 == 0 { 1.0 } else { inner }))
                .collect();
            Shape::Polygon {
                corners: fit_to_box(&corners, lo, hi),
            }
        }),
        DrawKind::Line | DrawKind::Elbow | DrawKind::Curved => {
            if dist(from, to) < MIN_DRAG {
                return None;
            }
            let path = match spec.kind {
                DrawKind::Elbow => elbow(from, to),
                DrawKind::Curved => curved(from, to),
                _ => vec![from, to],
            };
            Some(with_ends(path, spec))
        }
    }
}

/// Point `i` of `n` on a circle of `radius`, the first one straight up.
fn on_circle(i: usize, n: usize, radius: f32) -> P {
    let a = TAU * i as f32 / n as f32 - FRAC_PI_2;
    [radius * a.cos(), radius * a.sin()]
}

/// `points` stretched and moved so their bounding box is the box `lo`..`hi`.
fn fit_to_box(points: &[P], lo: P, hi: P) -> Vec<P> {
    let (mut min, mut max) = ([f32::MAX; 2], [f32::MIN; 2]);
    for p in points {
        for k in 0..2 {
            min[k] = min[k].min(p[k]);
            max[k] = max[k].max(p[k]);
        }
    }
    let sx = (hi[0] - lo[0]) / (max[0] - min[0]).max(f32::EPSILON);
    let sy = (hi[1] - lo[1]) / (max[1] - min[1]).max(f32::EPSILON);
    points
        .iter()
        .map(|p| [lo[0] + (p[0] - min[0]) * sx, lo[1] + (p[1] - min[1]) * sy])
        .collect()
}

fn rounded_box(lo: P, hi: P, radius: f32) -> Shape {
    let r = radius
        .max(0.0)
        .min((hi[0] - lo[0]).min(hi[1] - lo[1]) / 2.0);
    if r < 0.5 {
        return Shape::Polygon {
            corners: vec![lo, [hi[0], lo[1]], hi, [lo[0], hi[1]]],
        };
    }
    // Each corner's centre and the angle its quarter turn starts at, in drawing order.
    let corners = [
        ([lo[0] + r, lo[1] + r], PI),
        ([hi[0] - r, lo[1] + r], PI + FRAC_PI_2),
        ([hi[0] - r, hi[1] - r], 0.0),
        ([lo[0] + r, hi[1] - r], FRAC_PI_2),
    ];
    let mut points = Vec::with_capacity(4 * (CORNER_STEPS + 1) + 1);
    for (c, start) in corners {
        for k in 0..=CORNER_STEPS {
            let a = start + FRAC_PI_2 * k as f32 / CORNER_STEPS as f32;
            points.push([c[0] + r * a.cos(), c[1] + r * a.sin()]);
        }
    }
    points.push(points[0]);
    Shape::Polyline { points }
}

/// Across to the middle, over to the end's height, then across again. A drag
/// that is taller than it is wide runs down first instead.
fn elbow(from: P, to: P) -> Vec<P> {
    if (to[0] - from[0]).abs() >= (to[1] - from[1]).abs() {
        let mid = (from[0] + to[0]) / 2.0;
        vec![from, [mid, from[1]], [mid, to[1]], to]
    } else {
        let mid = (from[1] + to[1]) / 2.0;
        vec![from, [from[0], mid], [to[0], mid], to]
    }
}

/// A cubic whose ends leave and arrive along the longer axis of the drag.
fn curved(from: P, to: P) -> Vec<P> {
    let across = (to[0] - from[0]).abs() >= (to[1] - from[1]).abs();
    let (c1, c2) = if across {
        let mid = (from[0] + to[0]) / 2.0;
        ([mid, from[1]], [mid, to[1]])
    } else {
        let mid = (from[1] + to[1]) / 2.0;
        ([from[0], mid], [to[0], mid])
    };
    (0..=CURVE_STEPS)
        .map(|i| {
            let t = i as f32 / CURVE_STEPS as f32;
            let (a, b, c) = (lerp(from, c1, t), lerp(c1, c2, t), lerp(c2, to, t));
            lerp(lerp(a, b, t), lerp(b, c, t), t)
        })
        .collect()
}

/// The path as a shape, with a head at each end that asks for one. Heads are
/// part of the one stroke: out along a barb, back to the tip, out along the other.
fn with_ends(path: Vec<P>, spec: &DrawSpec) -> Shape {
    let wants = |on: bool| on && spec.head > 0.0;
    let (start, end) = (wants(spec.start_arrow), wants(spec.end_arrow));
    if !start && !end {
        return match path.as_slice() {
            [from, to] => Shape::Line {
                from: *from,
                to: *to,
            },
            _ => Shape::Polyline { points: path },
        };
    }
    if end && !start && path.len() == 2 {
        return Shape::Arrow {
            from: path[0],
            to: path[1],
            head: spec.head,
            spread: BARB_SPREAD,
        };
    }
    let mut points = Vec::with_capacity(path.len() + 8);
    if start {
        let tip = path[0];
        let (b1, b2) = barbs(tip, path[1], spec.head);
        points.extend([b1, tip, b2, tip]);
    }
    points.extend(path.iter().copied().skip(usize::from(start)));
    if end {
        let tip = path[path.len() - 1];
        let (b1, b2) = barbs(tip, path[path.len() - 2], spec.head);
        points.extend([b1, tip, b2]);
    }
    Shape::Polyline { points }
}

/// The barb ends of a head at `tip` on a path whose neighbouring point is `prev`.
fn barbs(tip: P, prev: P, head: f32) -> (P, P) {
    let back = unit(sub(prev, tip));
    let turn = |a: f32| {
        let (s, c) = a.sin_cos();
        [back[0] * c - back[1] * s, back[0] * s + back[1] * c]
    };
    (
        add(tip, scale(turn(BARB_SPREAD), head)),
        add(tip, scale(turn(-BARB_SPREAD), head)),
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    fn spec(kind: DrawKind) -> DrawSpec {
        DrawSpec::new(kind)
    }

    fn bounds(points: &[P]) -> (P, P) {
        let mut lo = [f32::MAX; 2];
        let mut hi = [f32::MIN; 2];
        for p in points {
            for k in 0..2 {
                lo[k] = lo[k].min(p[k]);
                hi[k] = hi[k].max(p[k]);
            }
        }
        (lo, hi)
    }

    fn near(a: P, b: P) -> bool {
        dist(a, b) < 1e-3
    }

    fn corners_of(shape: Option<Shape>) -> Vec<P> {
        match shape {
            Some(Shape::Polygon { corners }) => corners,
            other => panic!("expected a polygon, got {other:?}"),
        }
    }

    fn points_of(shape: Option<Shape>) -> Vec<P> {
        match shape {
            Some(Shape::Polyline { points }) => points,
            other => panic!("expected a polyline, got {other:?}"),
        }
    }

    #[test]
    fn a_box_fills_the_drag_whichever_way_it_is_dragged() {
        let kind = spec(DrawKind::Box { radius: 0.0 });
        for (from, to) in [
            ([10.0, 20.0], [110.0, 80.0]),
            ([110.0, 80.0], [10.0, 20.0]),
            ([110.0, 20.0], [10.0, 80.0]),
        ] {
            let corners = corners_of(draw_shape(&kind, from, to));
            assert_eq!(corners.len(), 4);
            assert_eq!(bounds(&corners), ([10.0, 20.0], [110.0, 80.0]));
        }
    }

    #[test]
    fn rounded_corners_stay_in_the_box_and_clamp_to_half_the_short_side() {
        let kind = spec(DrawKind::Box { radius: 500.0 });
        let points = points_of(draw_shape(&kind, [0.0, 0.0], [100.0, 40.0]));
        let (lo, hi) = bounds(&points);
        assert!(near(lo, [0.0, 0.0]) && near(hi, [100.0, 40.0]));
        assert!(near(points[0], points[points.len() - 1]));
        // A radius of half the short side leaves the top edge starting 20 in.
        assert!(points.iter().any(|p| near(*p, [20.0, 0.0])));
    }

    #[test]
    fn an_oval_is_the_ellipse_in_the_box() {
        let s = draw_shape(&spec(DrawKind::Oval), [0.0, 0.0], [200.0, 100.0]);
        assert_eq!(
            s,
            Some(Shape::Ellipse {
                center: [100.0, 50.0],
                rx: 100.0,
                ry: 50.0,
                rotation: 0.0
            })
        );
    }

    #[test]
    fn a_polygon_has_its_sides_and_fills_the_box() {
        for sides in [3u32, 5, 6, 12] {
            let kind = spec(DrawKind::Polygon { sides });
            let corners = corners_of(draw_shape(&kind, [0.0, 0.0], [120.0, 90.0]));
            assert_eq!(corners.len(), sides as usize);
            let (lo, hi) = bounds(&corners);
            assert!(near(lo, [0.0, 0.0]) && near(hi, [120.0, 90.0]));
        }
        // A triangle points up: its apex is on the top edge, in the middle.
        let tri = spec(DrawKind::Polygon { sides: 3 });
        assert!(near(
            corners_of(draw_shape(&tri, [0.0, 0.0], [100.0, 100.0]))[0],
            [50.0, 0.0]
        ));
    }

    #[test]
    fn side_counts_are_clamped() {
        let count = |sides| {
            corners_of(draw_shape(
                &spec(DrawKind::Polygon { sides }),
                [0.0, 0.0],
                [50.0, 50.0],
            ))
            .len()
        };
        assert_eq!(count(0), 3);
        assert_eq!(count(99), 12);
    }

    #[test]
    fn a_sharper_star_cuts_deeper() {
        let notch_width = |sharpness| {
            let kind = spec(DrawKind::Star {
                points: 5,
                sharpness,
            });
            let corners = corners_of(draw_shape(&kind, [0.0, 0.0], [100.0, 100.0]));
            assert_eq!(corners.len(), 10);
            let (lo, hi) = bounds(&corners);
            assert!(near(lo, [0.0, 0.0]) && near(hi, [100.0, 100.0]));
            let notches: Vec<P> = corners.iter().skip(1).step_by(2).copied().collect();
            let (nlo, nhi) = bounds(&notches);
            nhi[0] - nlo[0]
        };
        assert!(notch_width(0.9) < notch_width(0.5));
        assert!(notch_width(0.5) < notch_width(0.1));
    }

    #[test]
    fn tiny_or_invalid_drags_draw_nothing() {
        for kind in [
            DrawKind::Box { radius: 0.0 },
            DrawKind::Oval,
            DrawKind::Polygon { sides: 4 },
            DrawKind::Star {
                points: 5,
                sharpness: 0.5,
            },
            DrawKind::Line,
            DrawKind::Elbow,
            DrawKind::Curved,
        ] {
            let s = spec(kind);
            assert!(draw_shape(&s, [5.0, 5.0], [5.5, 5.5]).is_none(), "{kind:?}");
            assert!(
                draw_shape(&s, [f32::NAN, 0.0], [50.0, 50.0]).is_none(),
                "{kind:?}"
            );
            assert!(
                draw_shape(&s, [0.0, 0.0], [f32::INFINITY, 50.0]).is_none(),
                "{kind:?}"
            );
        }
        // A flat drag has no box to fill, but it is still a line.
        let flat = [100.0, 0.0];
        assert!(draw_shape(&spec(DrawKind::Box { radius: 0.0 }), [0.0, 0.0], flat).is_none());
        assert!(draw_shape(&spec(DrawKind::Line), [0.0, 0.0], flat).is_some());
    }

    #[test]
    fn a_plain_line_is_a_line() {
        assert_eq!(
            draw_shape(&spec(DrawKind::Line), [0.0, 0.0], [30.0, 40.0]),
            Some(Shape::Line {
                from: [0.0, 0.0],
                to: [30.0, 40.0]
            })
        );
    }

    fn arrows(kind: DrawKind, start: bool, end: bool) -> DrawSpec {
        DrawSpec {
            kind,
            head: 10.0,
            start_arrow: start,
            end_arrow: end,
        }
    }

    #[test]
    fn an_arrow_at_the_end_is_an_arrow() {
        let s = arrows(DrawKind::Line, false, true);
        let Some(Shape::Arrow { from, to, head, .. }) = draw_shape(&s, [0.0, 0.0], [100.0, 0.0])
        else {
            panic!("expected an arrow");
        };
        assert_eq!((from, to, head), ([0.0, 0.0], [100.0, 0.0], 10.0));
    }

    #[test]
    fn heads_at_both_ends_sit_on_their_tips_and_trail_behind_them() {
        let s = arrows(DrawKind::Line, true, true);
        let points = points_of(draw_shape(&s, [0.0, 0.0], [100.0, 0.0]));
        assert!(near(points[1], [0.0, 0.0]) && near(points[3], [0.0, 0.0]));
        assert_eq!(points.iter().filter(|p| near(**p, [100.0, 0.0])).count(), 2);
        assert!(points.iter().all(|p| p[0] >= -1e-3 && p[0] <= 100.0 + 1e-3));
        let (_, hi) = bounds(&points);
        assert!(hi[1] > 4.0, "barbs open out sideways");
    }

    #[test]
    fn a_head_at_the_start_only_leaves_the_end_plain() {
        let s = arrows(DrawKind::Line, true, false);
        let points = points_of(draw_shape(&s, [0.0, 0.0], [100.0, 0.0]));
        assert!(near(points[points.len() - 1], [100.0, 0.0]));
        assert_eq!(points.iter().filter(|p| near(**p, [100.0, 0.0])).count(), 1);
    }

    #[test]
    fn an_elbow_turns_at_right_angles_and_keeps_its_ends() {
        let points = points_of(draw_shape(
            &spec(DrawKind::Elbow),
            [0.0, 0.0],
            [100.0, 60.0],
        ));
        assert_eq!(points.len(), 4);
        assert!(near(points[0], [0.0, 0.0]) && near(points[3], [100.0, 60.0]));
        assert!(near(points[1], [50.0, 0.0]) && near(points[2], [50.0, 60.0]));
        let tall = points_of(draw_shape(
            &spec(DrawKind::Elbow),
            [0.0, 0.0],
            [20.0, 100.0],
        ));
        assert!(near(tall[1], [0.0, 50.0]));
    }

    #[test]
    fn a_curve_runs_end_to_end_inside_its_box() {
        let points = points_of(draw_shape(
            &spec(DrawKind::Curved),
            [0.0, 0.0],
            [100.0, 60.0],
        ));
        assert!(near(points[0], [0.0, 0.0]) && near(points[points.len() - 1], [100.0, 60.0]));
        let (lo, hi) = bounds(&points);
        assert!(lo[0] >= -1e-3 && lo[1] >= -1e-3 && hi[0] <= 100.001 && hi[1] <= 60.001);
        // It leaves along the long axis.
        assert!((points[1][1] - points[0][1]).abs() < (points[1][0] - points[0][0]).abs());
    }

    #[test]
    fn an_arrow_on_an_elbow_trails_along_its_last_leg() {
        let s = arrows(DrawKind::Elbow, false, true);
        let points = points_of(draw_shape(&s, [0.0, 0.0], [100.0, 60.0]));
        let n = points.len();
        assert!(near(points[n - 2], [100.0, 60.0]));
        assert!(points[n - 3][0] < 100.0 && points[n - 1][0] < 100.0);
    }

    #[test]
    fn heads_need_a_size() {
        let s = DrawSpec {
            kind: DrawKind::Line,
            head: 0.0,
            start_arrow: true,
            end_arrow: true,
        };
        assert!(matches!(
            draw_shape(&s, [0.0, 0.0], [50.0, 0.0]),
            Some(Shape::Line { .. })
        ));
    }
}
