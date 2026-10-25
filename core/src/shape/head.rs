//! Arrow head drawn as its own stroke after a shaft.

use super::barbs::measure;
use super::geom::{dist, sub, unit, P};
use super::polygon::simplify;
use super::{Shape, Stroke};
use crate::InputSample;

const SIMPLIFY_EPS: f32 = 0.05;
/// The apex must be within this fraction of the head size of the shaft tip, but never less than 12.
const TIP_TOLERANCE_OF_HEAD: f32 = 0.35;
const TIP_TOLERANCE_MIN: f32 = 12.0;
const MIN_BARB: f32 = 4.0;
const MAX_BARB_OF_SHAFT: f32 = 0.8;

/// A V, a caret or a single barb drawn at the end of an existing shaft.
/// None when the stroke is not that, is not near the tip, or the shaft has
/// no length.
pub(crate) fn fit_head(points: &[InputSample], shaft_from: P, shaft_to: P) -> Option<Shape> {
    let stroke = Stroke::new(points)?;
    let shaft_len = dist(shaft_from, shaft_to);
    if !shaft_len.is_finite() || shaft_len < 1e-3 {
        return None;
    }
    let pts = &stroke.points;
    let n = pts.len();
    let mut keep = vec![false; n];
    keep[0] = true;
    keep[n - 1] = true;
    let seq: Vec<usize> = (0..n).collect();
    simplify(
        pts,
        &seq,
        0,
        n - 1,
        SIMPLIFY_EPS * stroke.diagonal,
        &mut keep,
    );
    let v: Vec<P> = (0..n).filter(|&i| keep[i]).map(|i| pts[i]).collect();

    let (apex, ends) = match v.as_slice() {
        [a, b, c] => (*b, vec![*a, *c]),
        [a, b] => {
            let (near, far) = if dist(*a, shaft_to) <= dist(*b, shaft_to) {
                (*a, *b)
            } else {
                (*b, *a)
            };
            (near, vec![far])
        }
        _ => return None,
    };
    let head = ends.iter().map(|&e| dist(e, apex)).sum::<f32>() / ends.len() as f32;
    let tol = (TIP_TOLERANCE_OF_HEAD * head).max(TIP_TOLERANCE_MIN);
    if dist(apex, shaft_to) > tol {
        return None;
    }
    let toward = unit(sub(shaft_from, shaft_to));
    let barbs = measure(apex, toward, &ends, MIN_BARB, MAX_BARB_OF_SHAFT * shaft_len)?;
    Some(Shape::Head {
        tip: shaft_to,
        toward,
        head: barbs.head,
        spread: barbs.spread,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::snap_head;
    use crate::shape::testing::*;

    /// A V (or caret) at the tip of a shaft from `from` along `heading`.
    fn v_stroke(
        from: P,
        heading: f32,
        shaft: f32,
        head: f32,
        spread: f32,
        offset: f32,
    ) -> (P, P, Vec<InputSample>) {
        let verts = arrow_vertices(from, heading, shaft, head, spread, HeadStyle::BackAndForth);
        let tip = verts[1];
        let shift = |p: P| [p[0] + offset, p[1] + offset];
        let stroke = open_polyline(&[shift(verts[2]), shift(tip), shift(verts[4])], 0.01, 4);
        (from, tip, stroke)
    }

    #[test]
    fn v_at_the_tip_becomes_a_head() {
        for deg in [0.0f32, 90.0, 37.0, 250.0] {
            let (from, tip, stroke) =
                v_stroke([100.0, 300.0], deg.to_radians(), 240.0, 60.0, 0.5, 4.0);
            let (shape, samples) = snap_head(&stroke, from, tip, 5.0).expect("head");
            let Shape::Head {
                tip: t,
                toward,
                head,
                spread,
            } = shape
            else {
                panic!("expected a head");
            };
            assert_eq!(t, tip);
            assert!((toward[0] * toward[0] + toward[1] * toward[1] - 1.0).abs() < 1e-4);
            assert!((head - 60.0).abs() < 12.0, "head {head}");
            assert!((spread - 0.5).abs() < 0.15);
            assert!(samples.len() > 3);
            assert!(samples.windows(2).all(|w| w[1].time_ms >= w[0].time_ms));
        }
    }

    #[test]
    fn single_barb_becomes_a_head() {
        let verts = arrow_vertices([100.0, 300.0], 0.0, 240.0, 60.0, 0.6, HeadStyle::OneBarb);
        let stroke = open_polyline(&[verts[1], verts[2]], 0.005, 1);
        let got = snap_head(&stroke, verts[0], verts[1], 5.0);
        assert!(matches!(got, Some((Shape::Head { .. }, _))), "{got:?}");
    }

    #[test]
    fn v_far_from_the_tip_is_none() {
        let (from, tip, stroke) = v_stroke([100.0, 300.0], 0.0, 240.0, 60.0, 0.5, 0.0);
        let far = [from[0] + 120.0, from[1]];
        assert!(snap_head(&stroke, from, far, 5.0).is_none());
        assert!(snap_head(&stroke, from, tip, 5.0).is_some());
    }

    #[test]
    fn bad_input_is_none() {
        let (from, tip, stroke) = v_stroke([100.0, 300.0], 0.0, 240.0, 60.0, 0.5, 0.0);
        assert!(snap_head(&stroke, tip, tip, 5.0).is_none());
        assert!(snap_head(&stroke, from, [f32::NAN, 0.0], 5.0).is_none());
        assert!(snap_head(&[], from, tip, 5.0).is_none());
        let line = open_polyline(&[[340.0, 300.0], [200.0, 300.0]], 0.0, 1);
        assert!(snap_head(&line, from, tip, 5.0).is_none());
        let right = open_polyline(&[[340.0, 200.0], [340.0, 300.0], [340.0, 400.0]], 0.0, 1);
        assert!(snap_head(&right, from, tip, 5.0).is_none());
    }
}
