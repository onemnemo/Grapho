//! Elbow recogniser: an open stroke of two straight legs meeting at one
//! corner, such as an L or a half square. A corner near a right angle is
//! squared up, and the ends stay where the pen started and stopped.

use super::geom::{add, dist, dist_to_segment, dot, len, scale, sub, unit, Line, P};
use super::{Candidate, Recognizer, Shape, Stroke};
use std::f32::consts::{FRAC_PI_2, PI};

/// Largest deviation of a leg from its fitted line, as a fraction of the leg.
const MAX_LEG_DEVIATION: f32 = 0.05;
/// Each leg is at least this fraction of the stroke's diagonal.
const MIN_LEG_OF_DIAGONAL: f32 = 0.2;
/// Interior corner angles outside this range (degrees) are not elbows. Arrow
/// heads sit below it, so an arrow drawn in one stroke stays an arrow.
const MIN_CORNER_DEG: f32 = 75.0;
const MAX_CORNER_DEG: f32 = 120.0;
/// A corner within this many degrees of a right angle is squared up.
const RIGHT_ANGLE_SNAP_DEG: f32 = 10.0;
/// Elbows are rare and deliberate, so they lose ties against plain shapes.
const ELBOW_BIAS: f32 = 1.3;

/// Finds an open stroke made of two straight legs.
#[derive(Clone, Copy, Debug, Default)]
pub struct ElbowRecognizer;

impl Recognizer for ElbowRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        if stroke.closed {
            return None;
        }
        let pts = &stroke.points;
        let n = pts.len();
        let (first, last) = (pts[0], pts[n - 1]);
        let (c, _) = (1..n - 1)
            .map(|i| (i, dist_to_segment(pts[i], first, last)))
            .max_by(|a, b| a.1.total_cmp(&b.1))?;
        let (leg_a, leg_b) = (&pts[..=c], &pts[c..]);
        let line_a = Line::fit(leg_a)?;
        let line_b = Line::fit(leg_b)?;
        let min_leg = MIN_LEG_OF_DIAGONAL * stroke.diagonal;
        if dist(first, pts[c]) < min_leg || dist(pts[c], last) < min_leg {
            return None;
        }
        let corner = line_a.intersect(&line_b, 0.3)?;
        let da = unit(sub(corner, first));
        let len_b = dist(corner, last);
        if len_b < 1e-3 {
            return None;
        }
        let db = unit(sub(last, corner));
        let interior = PI - dot(da, db).clamp(-1.0, 1.0).acos();
        if !(MIN_CORNER_DEG.to_radians()..=MAX_CORNER_DEG.to_radians()).contains(&interior) {
            return None;
        }
        let dev_a = max_deviation(leg_a, &line_a) / dist(first, corner).max(1e-3);
        let dev_b = max_deviation(leg_b, &line_b) / len_b;
        if dev_a > MAX_LEG_DEVIATION || dev_b > MAX_LEG_DEVIATION {
            return None;
        }

        // A right angle keeps both ends: the corner moves onto the circle whose
        // diameter is the stroke's chord, where every corner is a right angle.
        let corner = if (interior - FRAC_PI_2).abs() < RIGHT_ANGLE_SNAP_DEG.to_radians() {
            let mid = scale(add(first, last), 0.5);
            let radius = 0.5 * dist(first, last);
            let away = sub(corner, mid);
            if len(away) < 1e-3 {
                return None;
            }
            add(mid, scale(unit(away), radius))
        } else {
            corner
        };
        let points = vec![first, corner, last];
        let residual = pts
            .iter()
            .map(|&p| {
                dist_to_segment(p, points[0], points[1])
                    .min(dist_to_segment(p, points[1], points[2]))
            })
            .sum::<f32>()
            / n as f32;
        Some(Candidate {
            shape: Shape::Polyline { points },
            score: ELBOW_BIAS * residual / stroke.diagonal,
        })
    }
}

fn max_deviation(pts: &[P], line: &Line) -> f32 {
    pts.iter().map(|&p| line.distance(p)).fold(0.0, f32::max)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;
    use crate::shape::ShapeRecognizer;
    use crate::InputSample;

    fn recognise(s: &[InputSample]) -> Option<Shape> {
        ShapeRecognizer::default().recognize(s)
    }

    fn corner_of(s: &Shape) -> Option<[f32; 2]> {
        match s {
            Shape::Polyline { points } if points.len() == 3 => Some(points[1]),
            _ => None,
        }
    }

    #[test]
    fn a_wobbly_l_becomes_a_polyline_with_its_ends_kept() {
        let s = open_polyline(&[[100.0, 60.0], [100.0, 300.0], [340.0, 300.0]], 0.01, 1);
        let got = recognise(&s).expect("an elbow");
        let Shape::Polyline { points } = &got else {
            panic!("expected a polyline: {got:?}");
        };
        let (a, b) = (&s[0], s.last().unwrap());
        assert!(dist(points[0], [a.x, a.y]) < 1e-3);
        assert!(dist(points[2], [b.x, b.y]) < 1e-3);
        assert!(dist(points[1], [100.0, 300.0]) < 10.0);
    }

    #[test]
    fn a_near_right_angle_is_squared_up() {
        let s = open_polyline(&[[100.0, 60.0], [100.0, 300.0], [330.0, 318.0]], 0.0, 1);
        let got = recognise(&s).expect("an elbow");
        let c = corner_of(&got).expect("a polyline");
        let Shape::Polyline { points } = &got else {
            unreachable!();
        };
        let a = unit(sub(points[0], c));
        let b = unit(sub(points[2], c));
        assert!(
            dot(a, b).abs() < 1e-3,
            "legs are not perpendicular: {}",
            dot(a, b)
        );
    }

    #[test]
    fn a_straight_stroke_is_still_a_line() {
        let s = open_polyline(&[[40.0, 100.0], [340.0, 100.0]], 0.01, 2);
        assert!(matches!(recognise(&s), Some(Shape::Line { .. })));
    }

    #[test]
    fn a_quarter_arc_is_still_an_arc() {
        let s = hand_drawn(
            |t| {
                let a = FRAC_PI_2 * t;
                [300.0 + 140.0 * a.cos(), 300.0 + 140.0 * a.sin()]
            },
            90,
            0.0,
            1.0,
            0.01,
            3,
        );
        assert!(
            matches!(recognise(&s), Some(Shape::Arc { .. })),
            "{:?}",
            recognise(&s)
        );
    }

    #[test]
    fn a_wide_v_is_not_an_elbow() {
        let s = open_polyline(&[[40.0, 40.0], [200.0, 300.0], [360.0, 40.0]], 0.01, 4);
        assert!(!matches!(recognise(&s), Some(Shape::Polyline { .. })));
    }
}
