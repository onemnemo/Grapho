//! Arrow recogniser: a shaft with the head drawn at its end in the same stroke.

use super::barbs::{measure, Barbs};
use super::geom::{dist, dist_to_segment, rotate_about, scale, sub, unit, Line, P};
use super::line::snap_angle;
use super::polygon::simplify;
use super::{Candidate, Recognizer, Shape, Stroke};

/// Ramer Douglas Peucker tolerance, relative to the stroke diagonal.
const SIMPLIFY_EPS: f32 = 0.03;
/// Shaft, tip and at most five more vertices; more is handwriting.
const MAX_VERTICES: usize = 7;
const MIN_SHAFT_OF_DIAGONAL: f32 = 0.5;
const MIN_BARB_OF_SHAFT: f32 = 0.1;
const MAX_BARB_OF_SHAFT: f32 = 0.6;
/// A vertex within this fraction of the first barb's length of the tip, or
/// 4 percent of the diagonal, is the tip again.
const TIP_TOLERANCE_OF_BARB: f32 = 0.3;
const TIP_TOLERANCE_OF_DIAGONAL: f32 = 0.04;
const MAX_SHAFT_DEVIATION: f32 = 0.06;
/// Arrows are rare and deliberate, so they win ties against lines.
const SCORE_SCALE: f32 = 0.5;

/// Finds an open stroke that is a straight shaft followed by an arrow head.
#[derive(Clone, Copy, Debug, Default)]
pub struct ArrowRecognizer;

impl Recognizer for ArrowRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        if stroke.closed {
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
        let idx: Vec<usize> = (0..n).filter(|&i| keep[i]).collect();
        if idx.len() < 3 || idx.len() > MAX_VERTICES {
            return None;
        }
        let tip_at = idx[1];
        let raw_tip = pts[tip_at];

        let shaft_pts = &pts[..=tip_at];
        let trim = shaft_pts.len() / 10;
        let core = shaft_pts.get(trim..shaft_pts.len() - trim)?;
        let line = Line::fit(core)?;
        let (from, to) = snap_angle(pts[0], raw_tip);
        let shaft_len = dist(from, to);
        if shaft_len < MIN_SHAFT_OF_DIAGONAL * stroke.diagonal {
            return None;
        }
        let max_dev = shaft_pts
            .iter()
            .map(|&p| line.distance(p))
            .fold(0.0, f32::max);
        if max_dev > MAX_SHAFT_DEVIATION * shaft_len {
            return None;
        }

        let ends = barb_ends(pts, &idx, raw_tip, stroke.diagonal)?;
        let reversed = unit(sub(from, to));
        let Barbs { head, spread } = measure(
            to,
            reversed,
            &ends,
            MIN_BARB_OF_SHAFT * shaft_len,
            MAX_BARB_OF_SHAFT * shaft_len,
        )?;

        let mut segments = vec![(from, to)];
        for e in &ends {
            let side = if super::geom::cross(reversed, sub(*e, to)) > 0.0 {
                spread
            } else {
                -spread
            };
            let tip = rotate_about(reversed, [0.0, 0.0], side);
            segments.push((to, super::geom::add(to, scale(tip, head))));
        }
        let mean_err = pts
            .iter()
            .map(|&p| {
                segments
                    .iter()
                    .map(|&(a, b)| dist_to_segment(p, a, b))
                    .fold(f32::INFINITY, f32::min)
            })
            .sum::<f32>()
            / n as f32;
        Some(Candidate {
            shape: Shape::Arrow {
                from,
                to,
                head,
                spread,
            },
            score: SCORE_SCALE * mean_err / stroke.diagonal,
        })
    }
}

/// The barb ends of the vertices after the tip, or None when they do not
/// follow one of the head patterns: barb, barb and back, barb and back and
/// barb (optionally back again), barb to barb and back (a triangle), or
/// barb to barb through the tip region.
fn barb_ends(pts: &[P], idx: &[usize], tip: P, diagonal: f32) -> Option<Vec<P>> {
    let rest = &idx[2..];
    let first = pts[rest[0]];
    let tol = (TIP_TOLERANCE_OF_BARB * dist(tip, first)).max(TIP_TOLERANCE_OF_DIAGONAL * diagonal);
    let near: Vec<bool> = rest.iter().map(|&i| dist(pts[i], tip) < tol).collect();
    let at = |k: usize| pts[rest[k]];
    match near.as_slice() {
        [false] | [false, true] => Some(vec![at(0)]),
        [false, true, false] | [false, true, false, true] | [false, false, true] => {
            let b = if near[1] { at(2) } else { at(1) };
            Some(vec![at(0), b])
        }
        [false, false] => {
            let through = pts[rest[0]..=rest[1]].iter().any(|&p| dist(p, tip) < tol);
            through.then(|| vec![at(0), at(1)])
        }
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;
    use crate::shape::ShapeRecognizer;

    const STYLES: [HeadStyle; 4] = [
        HeadStyle::BackAndForth,
        HeadStyle::Triangle,
        HeadStyle::OneBarb,
        HeadStyle::Through,
    ];

    fn recognise(pts: &[[f32; 2]], wobble: f32, seed: u32) -> Option<Shape> {
        ShapeRecognizer::default().recognize(&open_polyline(pts, wobble, seed))
    }

    #[test]
    fn every_head_style_at_several_angles() {
        for deg in [0.0f32, 90.0, 37.0, 200.0] {
            for style in STYLES {
                for seed in 1..4 {
                    let v =
                        arrow_vertices([200.0, 300.0], deg.to_radians(), 260.0, 70.0, 0.55, style);
                    let got = recognise(&v, 0.008, seed);
                    let Some(Shape::Arrow {
                        from,
                        to,
                        head,
                        spread,
                    }) = got
                    else {
                        panic!("{style:?} at {deg} seed {seed}: {got:?}");
                    };
                    assert!(dist(from, v[0]) < 15.0, "{style:?} {deg}: from {from:?}");
                    assert!(dist(to, v[1]) < 15.0, "{style:?} {deg}: to {to:?}");
                    assert!((head - 70.0).abs() < 15.0, "{style:?} {deg}: head {head}");
                    assert!(
                        (spread - 0.55).abs() < 0.15,
                        "{style:?} {deg}: spread {spread}"
                    );
                }
            }
        }
    }

    #[test]
    fn near_horizontal_arrow_gets_a_level_shaft_and_30_degree_barbs() {
        let v = arrow_vertices(
            [100.0, 200.0],
            0.03,
            300.0,
            80.0,
            0.5,
            HeadStyle::BackAndForth,
        );
        let Some(Shape::Arrow {
            from, to, spread, ..
        }) = recognise(&v, 0.005, 2)
        else {
            panic!("expected an arrow");
        };
        assert!((from[1] - to[1]).abs() < 1e-3);
        assert!((spread - 30f32.to_radians()).abs() < 1e-5);
    }

    #[test]
    fn path_is_one_open_polyline_through_the_tip() {
        let v = arrow_vertices([100.0, 200.0], 0.0, 300.0, 80.0, 0.5, HeadStyle::OneBarb);
        let shape = recognise(&v, 0.0, 1).expect("arrow");
        let p = shape.path(10.0);
        assert_ne!(p.first(), p.last());
        assert!(p.iter().filter(|q| dist(**q, v[1]) < 20.0).count() >= 2);
    }

    #[test]
    fn shapes_that_are_not_arrows_are_left_alone() {
        let cases: Vec<(&str, Vec<[f32; 2]>)> = vec![
            ("line", vec![[0.0, 0.0], [300.0, 20.0]]),
            ("L", vec![[0.0, 0.0], [0.0, 250.0], [120.0, 250.0]]),
            ("checkmark", vec![[0.0, 100.0], [50.0, 160.0], [250.0, 0.0]]),
            (
                "zigzag",
                vec![
                    [0.0, 0.0],
                    [80.0, 90.0],
                    [160.0, 0.0],
                    [240.0, 90.0],
                    [320.0, 0.0],
                ],
            ),
            (
                "Z",
                vec![[0.0, 0.0], [200.0, 0.0], [0.0, 200.0], [200.0, 200.0]],
            ),
            ("seven", vec![[0.0, 0.0], [160.0, 0.0], [60.0, 260.0]]),
            ("long seven", vec![[0.0, 0.0], [260.0, 0.0], [120.0, 200.0]]),
        ];
        for (name, v) in cases {
            for seed in 1..4 {
                let got = recognise(&v, 0.01, seed);
                assert!(!matches!(got, Some(Shape::Arrow { .. })), "{name}: {got:?}");
            }
        }
    }

    #[test]
    fn a_triangle_is_not_an_arrow() {
        let tri = [[300.0, 80.0], [480.0, 380.0], [100.0, 360.0]];
        let s = drawn_polygon(&tri, 0.05, 0.01, 3);
        assert!(!matches!(
            ShapeRecognizer::default().recognize(&s),
            Some(Shape::Arrow { .. })
        ));
    }

    #[test]
    fn handwriting_is_not_an_arrow() {
        let curl = hand_drawn(
            |t| {
                let a = t * 9.0;
                [60.0 * t * 6.0 + 30.0 * a.cos(), 100.0 + 40.0 * a.sin()]
            },
            150,
            0.0,
            1.0,
            0.0,
            1,
        );
        assert!(!matches!(
            ShapeRecognizer::default().recognize(&curl),
            Some(Shape::Arrow { .. })
        ));
    }

    #[test]
    fn barbs_on_the_same_side_are_not_an_arrow() {
        let tip = [300.0, 100.0];
        let v = [[40.0, 100.0], tip, [250.0, 40.0], tip, [240.0, 70.0]];
        let got = recognise(&v, 0.0, 1);
        assert!(!matches!(got, Some(Shape::Arrow { .. })), "{got:?}");
    }
}
