//! Straight line recogniser.

use super::geom::{dist, rotate_about, sub, Line, P};
use super::{Candidate, Recognizer, Shape, Stroke};
use std::f32::consts::FRAC_PI_4;

/// Largest perpendicular deviation from the fitted line, as a fraction of its length.
const MAX_DEVIATION: f32 = 0.08;
const MAX_PATH_RATIO: f32 = 1.3;
const AXIS_SNAP_DEG: f32 = 4.0;
const DIAGONAL_SNAP_DEG: f32 = 3.0;

/// Finds a single straight segment in an open stroke.
#[derive(Clone, Copy, Debug, Default)]
pub struct LineRecognizer;

impl Recognizer for LineRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        if stroke.closed {
            return None;
        }
        let line = Line::fit(&stroke.points)?;
        // The line runs from where the pen went down to where it stopped; the
        // fitted line only judges how straight the stroke is.
        let from = *stroke.points.first()?;
        let to = *stroke.points.last()?;
        let length = dist(from, to);
        if length < 1e-3 {
            return None;
        }
        let n = stroke.points.len() as f32;
        let mut max_dev = 0.0f32;
        let mut sum_sq = 0.0;
        for &p in &stroke.points {
            let d = line.distance(p);
            max_dev = max_dev.max(d);
            sum_sq += d * d;
        }
        // A stroke that doubles back is a scribble even when it stays on one line.
        if max_dev / length > MAX_DEVIATION || stroke.length > MAX_PATH_RATIO * length {
            return None;
        }
        let (from, to) = snap_angle(from, to);
        Some(Candidate {
            shape: Shape::Line { from, to },
            score: (sum_sq / n).sqrt() / length,
        })
    }
}

/// Rotates the segment about its start onto the nearest multiple of 45
/// degrees when it is already close to one, so the start stays put.
pub(crate) fn snap_angle(from: P, to: P) -> (P, P) {
    let d = sub(to, from);
    let angle = d[1].atan2(d[0]);
    let k = (angle / FRAC_PI_4).round();
    let target = k * FRAC_PI_4;
    let tolerance = if (k as i64) % 2 == 0 {
        AXIS_SNAP_DEG
    } else {
        DIAGONAL_SNAP_DEG
    };
    let off = angle - target;
    if off.abs() > tolerance.to_radians() {
        return (from, to);
    }
    (from, rotate_about(to, from, -off))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;

    fn fit(points: &[crate::InputSample]) -> Option<Shape> {
        LineRecognizer.fit(&Stroke::new(points)?).map(|c| c.shape)
    }

    #[test]
    fn recognises_a_wobbly_line() {
        let a = [50.0, 80.0];
        let b = [420.0, 310.0];
        let stroke = hand_drawn(|t| lerp(a, b, t), 70, 0.0, 1.0, 0.02, 3);
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        assert!(dist(from, a) < 15.0 && dist(to, b) < 15.0);
    }

    #[test]
    fn the_line_starts_and_ends_where_the_pen_did() {
        let a = [50.0, 80.0];
        let b = [420.0, 310.0];
        let stroke = hand_drawn(|t| lerp(a, b, t), 70, 0.0, 1.0, 0.02, 3);
        let (first, last) = (&stroke[0], stroke.last().unwrap());
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        assert!(dist(from, [first.x, first.y]) < 1e-3);
        assert!(dist(to, [last.x, last.y]) < 1e-3);
    }

    #[test]
    fn an_axis_snap_keeps_the_start() {
        let (from, to) = snap_angle([10.0, 20.0], [310.0, 30.0]);
        assert_eq!(from, [10.0, 20.0]);
        assert!((to[1] - 20.0).abs() < 1e-3);
    }

    #[test]
    fn near_horizontal_line_becomes_horizontal() {
        let stroke = hand_drawn(
            |t| [40.0 + 300.0 * t, 100.0 + 8.0 * t],
            60,
            0.0,
            1.0,
            0.01,
            5,
        );
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        assert!((from[1] - to[1]).abs() < 1e-3);
    }

    #[test]
    fn near_vertical_line_becomes_vertical() {
        let stroke = hand_drawn(
            |t| [100.0 + 6.0 * t, 40.0 + 300.0 * t],
            60,
            0.0,
            1.0,
            0.01,
            6,
        );
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        assert!((from[0] - to[0]).abs() < 1e-3);
    }

    #[test]
    fn diagonal_line_snaps_to_45_degrees() {
        let stroke = hand_drawn(
            |t| [40.0 + 300.0 * t, 40.0 + 312.0 * t],
            60,
            0.0,
            1.0,
            0.01,
            7,
        );
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        assert!(((to[0] - from[0]).abs() - (to[1] - from[1]).abs()).abs() < 1e-2);
    }

    #[test]
    fn a_20_degree_line_is_left_alone() {
        let a = 20f32.to_radians();
        let stroke = hand_drawn(
            |t| [40.0 + 300.0 * t * a.cos(), 40.0 + 300.0 * t * a.sin()],
            60,
            0.0,
            1.0,
            0.01,
            8,
        );
        let Some(Shape::Line { from, to }) = fit(&stroke) else {
            panic!("expected a line");
        };
        let got = (to[1] - from[1]).atan2(to[0] - from[0]);
        assert!((got - a).abs() < 3f32.to_radians());
    }

    #[test]
    fn a_line_drawn_back_and_forth_is_not_a_line() {
        let stroke = hand_drawn(
            |t| [40.0 + 300.0 * (1.0 - (2.0 * t - 1.0).abs()), 100.0],
            80,
            0.0,
            1.0,
            0.004,
            1,
        );
        assert!(fit(&stroke).is_none());
    }

    #[test]
    fn squiggle_is_not_a_line() {
        let stroke = hand_drawn(
            |t| [40.0 + 300.0 * t, 100.0 + 40.0 * (t * 18.0).sin()],
            120,
            0.0,
            1.0,
            0.0,
            1,
        );
        assert!(fit(&stroke).is_none());
    }

    #[test]
    fn a_shallow_arc_is_not_a_line() {
        let stroke = hand_drawn(
            |t| {
                [
                    40.0 + 300.0 * t,
                    100.0 + 90.0 * (t * std::f32::consts::PI).sin(),
                ]
            },
            80,
            0.0,
            1.0,
            0.0,
            1,
        );
        assert!(fit(&stroke).is_none());
    }
}
