//! Smooth curve fallback and the curvature measures arcs share with it.

use super::geom::{angle_between, dist_to_segment, sub, P};
use super::stroke::resample;
use super::Stroke;
/// Window, in resampled points, over which a corner is measured.
const CORNER_WINDOW: usize = 6;
const CORNER_SMOOTHING_PASSES: usize = 4;
const MIN_WINDOW: usize = 3;
/// A turn this sharp inside one window is a corner, not a curve.
const CORNER_DEG: f32 = 60.0;
/// A gentle curve turns by less than this in total.
const MAX_TOTAL_TURN_DEG: f32 = 270.0;
const SPACING_OF_DIAGONAL: f32 = 0.03;
const MIN_SPACING_PX: f32 = 3.0;
const MAX_POINTS: usize = 200;
const MAX_PASSES: usize = 12;
/// How far smoothing may move the outline from the drawn stroke.
const MAX_DISPLACEMENT_OF_DIAGONAL: f32 = 0.03;

/// Largest turn in radians between the direction into a point and the
/// direction out of it, each measured over a short window of the stroke.
pub(crate) fn sharpest_turn(pts: &[P]) -> f32 {
    // Hand wobble turns locally almost as much as a corner does, so look at a
    // lightly smoothed copy; a real corner survives it.
    let mut pts = pts.to_vec();
    for _ in 0..CORNER_SMOOTHING_PASSES {
        pts = pass(&pts);
    }
    let n = pts.len();
    let mut worst = 0.0f32;
    for i in 1..n.saturating_sub(1) {
        let w = CORNER_WINDOW.min(i).min(n - 1 - i);
        if w < MIN_WINDOW {
            continue;
        }
        let a = sub(pts[i], pts[i - w]);
        let b = sub(pts[i + w], pts[i]);
        worst = worst.max(angle_between(a, b));
    }
    worst
}

/// True when the stroke has no corner sharper than 60 degrees.
pub(crate) fn has_no_corner(pts: &[P]) -> bool {
    sharpest_turn(pts) < CORNER_DEG.to_radians()
}

fn total_turn(pts: &[P]) -> f32 {
    pts.windows(3)
        .map(|w| angle_between(sub(w[1], w[0]), sub(w[2], w[1])))
        .sum()
}

/// A smoothed copy of a gentle open stroke, or None for anything else:
/// closed strokes, strokes with a corner, and strokes that turn by more than
/// 270 degrees in total. The ends are pinned and no point moves farther than
/// 3 percent of the diagonal from the drawn stroke.
pub(crate) fn smooth(stroke: &Stroke) -> Option<Vec<P>> {
    if stroke.closed || !has_no_corner(&stroke.points) {
        return None;
    }
    let spacing = (SPACING_OF_DIAGONAL * stroke.diagonal).max(MIN_SPACING_PX);
    let count = ((stroke.length / spacing).ceil() as usize + 1).clamp(8, MAX_POINTS);
    let (mut pts, _) = resample(&stroke.points, count);
    let limit = MAX_DISPLACEMENT_OF_DIAGONAL * stroke.diagonal;
    for _ in 0..MAX_PASSES {
        let next = pass(&pts);
        let moved = next
            .iter()
            .map(|&p| polyline_distance(p, &stroke.points))
            .fold(0.0, f32::max);
        if moved > limit {
            break;
        }
        pts = next;
    }
    (total_turn(&pts) < MAX_TOTAL_TURN_DEG.to_radians()).then_some(pts)
}

fn pass(pts: &[P]) -> Vec<P> {
    let n = pts.len();
    (0..n)
        .map(|i| {
            if i == 0 || i == n - 1 {
                pts[i]
            } else {
                [
                    (pts[i - 1][0] + 2.0 * pts[i][0] + pts[i + 1][0]) / 4.0,
                    (pts[i - 1][1] + 2.0 * pts[i][1] + pts[i + 1][1]) / 4.0,
                ]
            }
        })
        .collect()
}

fn polyline_distance(p: P, line: &[P]) -> f32 {
    line.windows(2)
        .map(|w| dist_to_segment(p, w[0], w[1]))
        .fold(f32::INFINITY, f32::min)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::geom::dist;
    use crate::shape::testing::*;
    use crate::InputSample;
    use std::f32::consts::PI;

    fn stroke(s: &[InputSample]) -> Stroke {
        Stroke::new(s).expect("stroke")
    }

    #[test]
    fn a_wobbly_bend_is_smoothed_within_the_limit() {
        let s = hand_drawn(
            |t| [40.0 + 300.0 * t, 200.0 - 120.0 * (t * PI).sin()],
            110,
            0.0,
            1.0,
            0.02,
            3,
        );
        let st = stroke(&s);
        let pts = smooth(&st).expect("curve");
        assert_eq!(pts.first().copied(), Some(st.points[0]));
        let far = pts
            .iter()
            .map(|&p| polyline_distance(p, &st.points))
            .fold(0.0, f32::max);
        assert!(far <= 0.03 * st.diagonal + 1e-3);
        assert!(dist(pts[0], st.points[0]) < 1e-3);
    }

    #[test]
    fn corners_and_loops_are_not_curves() {
        let l = open_polyline(&[[0.0, 0.0], [0.0, 200.0], [150.0, 200.0]], 0.0, 1);
        assert!(smooth(&stroke(&l)).is_none());
        let ring = drawn_ellipse([200.0, 200.0], 100.0, 100.0, 0.0, 1.0, 0.0, 1);
        assert!(smooth(&stroke(&ring)).is_none());
        let spiral = hand_drawn(
            |t| {
                let a = t * 6.0;
                [200.0 + 50.0 * a.cos() + 80.0 * t, 200.0 + 50.0 * a.sin()]
            },
            150,
            0.0,
            1.0,
            0.0,
            1,
        );
        assert!(smooth(&stroke(&spiral)).is_none());
    }
}
