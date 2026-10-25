//! Polygon recogniser: triangles, quadrilaterals and up to six straight sides.

use super::geom::{
    angle_between, cross, dist, dist_to_polygon, dist_to_segment, segments_cross, sub, Line, P,
};
use super::regularise::regularise;
use super::{Candidate, Recognizer, Shape, Stroke};
use std::f32::consts::PI;

const MIN_CORNERS: usize = 3;
/// Hand-drawn hexagons are too close to circles to tell apart reliably, and
/// circles are far more common in notes.
const MAX_CORNERS: usize = 5;
/// More corners than this after detection means a scribble, not a shape.
const MAX_DETECTED: usize = 8;
/// Ramer Douglas Peucker tolerance, relative to the stroke diagonal.
const SIMPLIFY_EPS: f32 = 0.05;
/// Corners closer than this (relative to the diagonal) are one corner.
const MERGE_DIST: f32 = 0.07;
/// Corners that turn less than this are not corners.
const MIN_TURN_DEG: f32 = 25.0;
/// Fraction of each side, at both ends, left out of the line fit so rounded corners do not skew it.
const SIDE_TRIM: f32 = 0.2;
const MAX_CORNER_SHIFT: f32 = 0.12;
/// No point of the stroke may be farther than this (relative to the diagonal)
/// from the fitted outline, so one wild side cannot hide behind good ones.
const MAX_POINT_ERROR: f32 = 0.14;
const MIN_AREA: f32 = 0.02;
const CORNER_PENALTY: f32 = 0.001;
/// Extra penalty per corner beyond four, so a spurious fifth or sixth corner has to earn its place.
const EXTRA_CORNER_PENALTY: f32 = 0.012;
/// Every corner of a polygon with five or more corners must turn at least
/// this much. A shallow bend on one side of a rough square is a wobble, not a
/// fifth corner.
const MIN_EXTRA_CORNER_TURN_DEG: f32 = 45.0;
/// Mean local turn over full turn that polygons with five or more corners
/// need. Wobbly arcs approximated by many short sides turn gradually, real
/// corners turn within a couple of samples.
const MIN_SHARPNESS: f32 = 0.7;
const SHARPNESS_WINDOW: usize = 3;
const MIN_CORNER_SHARPNESS: f32 = 0.45;
const SHARPNESS_FROM: usize = 5;

/// Finds a closed polygon with three to six corners.
#[derive(Clone, Copy, Debug, Default)]
pub struct PolygonRecognizer;

impl Recognizer for PolygonRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        if !stroke.closed {
            return None;
        }
        let pts = trim_overshoot(&stroke.points);
        let mut indices = detect_corners(pts, stroke.diagonal)?;
        let mut best: Option<Candidate> = None;
        // Wobble can add a corner that is not there, so every smaller count
        // is tried and the best score wins.
        loop {
            if indices.len() <= MAX_CORNERS {
                let c = evaluate(pts, &indices, stroke.diagonal);
                if c.as_ref()
                    .is_some_and(|c| best.as_ref().is_none_or(|b| c.score < b.score))
                {
                    best = c;
                }
            }
            if indices.len() <= MIN_CORNERS {
                break;
            }
            let m = indices.len();
            let flattest = (0..m).min_by(|&a, &b| {
                let dev = |i: usize| {
                    dist_to_segment(
                        pts[indices[i]],
                        pts[indices[(i + m - 1) % m]],
                        pts[indices[(i + 1) % m]],
                    )
                };
                dev(a).total_cmp(&dev(b))
            })?;
            indices.remove(flattest);
        }
        best
    }
}

fn evaluate(pts: &[P], indices: &[usize], diagonal: f32) -> Option<Candidate> {
    if indices.len() >= SHARPNESS_FROM {
        let (mean, min) = sharpness(pts, indices);
        if mean < MIN_SHARPNESS || min < MIN_CORNER_SHARPNESS {
            return None;
        }
    }
    let refined = refine(pts, indices, diagonal);
    let n = refined.len();
    if n >= SHARPNESS_FROM
        && (0..n).any(|i| {
            turn_at(refined[(i + n - 1) % n], refined[i], refined[(i + 1) % n])
                < MIN_EXTRA_CORNER_TURN_DEG.to_radians()
        })
    {
        return None;
    }
    let corners = regularise(refined);
    if !is_simple(&corners) || area(&corners) < MIN_AREA * diagonal * diagonal {
        return None;
    }
    let errors: Vec<f32> = pts.iter().map(|&p| dist_to_polygon(p, &corners)).collect();
    let max_err = errors.iter().copied().fold(0.0, f32::max) / diagonal;
    if max_err > MAX_POINT_ERROR {
        return None;
    }
    let mean_err = errors.iter().sum::<f32>() / errors.len() as f32 / diagonal;
    let n = corners.len();
    let score =
        mean_err + CORNER_PENALTY * n as f32 + EXTRA_CORNER_PENALTY * n.saturating_sub(4) as f32;
    Some(Candidate {
        shape: Shape::Polygon { corners },
        score,
    })
}

/// Cuts a stroke that ran past its start back to the point nearest the start.
fn trim_overshoot(pts: &[P]) -> &[P] {
    let from = pts.len() * 3 / 4;
    let end = (from..pts.len())
        .min_by(|&a, &b| dist(pts[a], pts[0]).total_cmp(&dist(pts[b], pts[0])))
        .unwrap_or(pts.len() - 1);
    &pts[..=end]
}

/// Mean and minimum sharpness over the corners.
fn sharpness(pts: &[P], idx: &[usize]) -> (f32, f32) {
    let n = pts.len();
    let full = turns(pts, idx);
    let ratios: Vec<f32> = idx
        .iter()
        .zip(&full)
        .map(|(&i, &f)| {
            let local = turn_at(
                pts[(i + n - SHARPNESS_WINDOW % n) % n],
                pts[i],
                pts[(i + SHARPNESS_WINDOW) % n],
            );
            (local / f.max(1e-3)).min(1.0)
        })
        .collect();
    let mean = ratios.iter().sum::<f32>() / ratios.len() as f32;
    (mean, ratios.iter().copied().fold(1.0, f32::min))
}

fn turn_at(prev: P, here: P, next: P) -> f32 {
    PI - angle_between(sub(prev, here), sub(next, here))
}

fn turns(pts: &[P], idx: &[usize]) -> Vec<f32> {
    let m = idx.len();
    (0..m)
        .map(|i| {
            turn_at(
                pts[idx[(i + m - 1) % m]],
                pts[idx[i]],
                pts[idx[(i + 1) % m]],
            )
        })
        .collect()
}

/// Indices into `pts` of the corners, in drawing order, or None if the
/// count is outside 3 to 8.
fn detect_corners(pts: &[P], diagonal: f32) -> Option<Vec<usize>> {
    let n = pts.len();
    if n < 8 {
        return None;
    }
    let start = (0..n).max_by(|&a, &b| dist(pts[a], pts[0]).total_cmp(&dist(pts[b], pts[0])))?;
    let seq: Vec<usize> = (0..=n).map(|i| (start + i) % n).collect();
    let mut keep = vec![false; n + 1];
    keep[0] = true;
    keep[n] = true;
    simplify(pts, &seq, 0, n, SIMPLIFY_EPS * diagonal, &mut keep);
    let mut idx: Vec<usize> = (0..n).filter(|&i| keep[i]).map(|i| seq[i]).collect();

    while idx.len() > 3 {
        let m = idx.len();
        let close =
            (0..m).find(|&i| dist(pts[idx[i]], pts[idx[(i + 1) % m]]) < MERGE_DIST * diagonal);
        let Some(i) = close else { break };
        let t = turns(pts, &idx);
        idx.remove(if t[i] < t[(i + 1) % m] {
            i
        } else {
            (i + 1) % m
        });
    }
    while idx.len() >= MIN_CORNERS {
        let t = turns(pts, &idx);
        let (weakest, &least) = t.iter().enumerate().min_by(|a, b| a.1.total_cmp(b.1))?;
        if least >= MIN_TURN_DEG.to_radians() {
            break;
        }
        idx.remove(weakest);
    }
    (MIN_CORNERS..=MAX_DETECTED)
        .contains(&idx.len())
        .then_some(idx)
}

pub(crate) fn simplify(
    pts: &[P],
    seq: &[usize],
    lo: usize,
    hi: usize,
    eps: f32,
    keep: &mut [bool],
) {
    if hi <= lo + 1 {
        return;
    }
    let (a, b) = (pts[seq[lo]], pts[seq[hi]]);
    let far = (lo + 1..hi)
        .map(|i| (i, dist_to_segment(pts[seq[i]], a, b)))
        .max_by(|x, y| x.1.total_cmp(&y.1));
    if let Some((i, d)) = far {
        if d > eps {
            keep[i] = true;
            simplify(pts, seq, lo, i, eps, keep);
            simplify(pts, seq, i, hi, eps, keep);
        }
    }
}

/// Fits a line to each side and intersects neighbours for crisp corners.
fn refine(pts: &[P], idx: &[usize], diagonal: f32) -> Vec<P> {
    let m = idx.len();
    let n = pts.len();
    let lines: Vec<Option<Line>> = (0..m)
        .map(|i| {
            let (a, b) = (idx[i], idx[(i + 1) % m]);
            let steps = (b + n - a) % n;
            let steps = if steps == 0 { n } else { steps };
            let trim = (steps as f32 * SIDE_TRIM) as usize;
            let side: Vec<P> = (trim.max(1)..steps.saturating_sub(trim))
                .map(|k| pts[(a + k) % n])
                .collect();
            if side.len() < 3 {
                return None;
            }
            Line::fit(&side)
        })
        .collect();
    (0..m)
        .map(|i| {
            let original = pts[idx[i]];
            let prev = lines[(i + m - 1) % m];
            let here = lines[i];
            match (prev, here) {
                (Some(p), Some(h)) => p
                    .intersect(&h, 0.2)
                    .filter(|&q| dist(q, original) < MAX_CORNER_SHIFT * diagonal)
                    .unwrap_or(original),
                _ => original,
            }
        })
        .collect()
}

fn area(c: &[P]) -> f32 {
    let n = c.len();
    (0..n)
        .map(|i| cross(c[i], c[(i + 1) % n]))
        .sum::<f32>()
        .abs()
        / 2.0
}

fn is_simple(c: &[P]) -> bool {
    let n = c.len();
    if c.iter().any(|p| !p[0].is_finite() || !p[1].is_finite()) {
        return false;
    }
    for i in 0..n {
        for j in i + 2..n {
            if i == 0 && j == n - 1 {
                continue;
            }
            if segments_cross(c[i], c[(i + 1) % n], c[j], c[(j + 1) % n]) {
                return false;
            }
        }
    }
    true
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;
    use crate::InputSample;

    fn fit(points: &[InputSample]) -> Option<Vec<P>> {
        match PolygonRecognizer.fit(&Stroke::new(points)?)?.shape {
            Shape::Polygon { corners } => Some(corners),
            _ => None,
        }
    }

    fn right_angled(c: &[P]) -> bool {
        (0..c.len()).all(|i| {
            let n = c.len();
            let a = angle_between(sub(c[(i + n - 1) % n], c[i]), sub(c[(i + 1) % n], c[i]));
            (a - std::f32::consts::FRAC_PI_2).abs() < 1e-3
        })
    }

    #[test]
    fn a_leaning_hand_drawn_square_becomes_a_square() {
        // Sheared by 14 degrees, with one corner splayed further.
        let sq = [
            [100.0, 100.0],
            [300.0, 100.0],
            [350.0, 300.0],
            [150.0, 290.0],
        ];
        for seed in 0..6 {
            let c = fit(&drawn_polygon(&sq, 0.05, 0.015, seed)).expect("a polygon");
            assert_eq!(c.len(), 4, "seed {seed}: {c:?}");
            assert!(right_angled(&c), "seed {seed}: {c:?}");
        }
    }

    #[test]
    fn a_bump_on_one_side_is_not_a_corner() {
        let notched = [
            [100.0, 100.0],
            [200.0, 85.0],
            [300.0, 100.0],
            [300.0, 300.0],
            [100.0, 300.0],
        ];
        for seed in 0..6 {
            let c = fit(&drawn_polygon(&notched, 0.05, 0.01, seed)).expect("a polygon");
            assert_eq!(c.len(), 4, "seed {seed}: {c:?}");
            assert!(right_angled(&c), "seed {seed}: {c:?}");
        }
    }

    #[test]
    fn rectangle_gets_right_angles() {
        let rect = rect_corners([250.0, 200.0], 320.0, 140.0, 0.4);
        let got = fit(&drawn_polygon(&rect, 0.05, 0.02, 3)).expect("rectangle");
        assert_eq!(got.len(), 4);
        for i in 0..4 {
            let a = sub(got[(i + 3) % 4], got[i]);
            let b = sub(got[(i + 1) % 4], got[i]);
            assert!((a[0] * b[0] + a[1] * b[1]).abs() < 1e-1);
        }
    }

    #[test]
    fn parallelogram_keeps_its_slant() {
        // Slanted well past the lean a hand gives a rectangle.
        let para = vec![
            [100.0, 100.0],
            [380.0, 100.0],
            [480.0, 260.0],
            [200.0, 260.0],
        ];
        let got = fit(&drawn_polygon(&para, 0.05, 0.012, 2)).expect("parallelogram");
        assert_eq!(got.len(), 4);
        let a = sub(got[1], got[0]);
        let b = sub(got[2], got[3]);
        assert!(cross(a, b).abs() < 1e-1);
        let c = sub(got[3], got[0]);
        let d = sub(got[2], got[1]);
        assert!(cross(c, d).abs() < 1e-1);
        assert!(dot_axis(c) > 50.0);
    }

    fn dot_axis(v: P) -> f32 {
        v[0].abs()
    }

    #[test]
    fn rhombus_has_equal_sides() {
        let rh = vec![
            [300.0, 100.0],
            [420.0, 250.0],
            [300.0, 400.0],
            [180.0, 250.0],
        ];
        let got = fit(&drawn_polygon(&rh, 0.04, 0.012, 4)).expect("rhombus");
        let side = dist(got[0], got[1]);
        for i in 0..4 {
            assert!((dist(got[i], got[(i + 1) % 4]) - side).abs() < 1e-2);
        }
    }

    #[test]
    fn triangle_with_a_tilted_base_levels_out() {
        let tri = vec![[100.0, 300.0], [440.0, 310.0], [260.0, 80.0]];
        let got = fit(&drawn_polygon(&tri, 0.05, 0.01, 8)).expect("triangle");
        assert_eq!(got.len(), 3);
        let flat = (0..3).any(|i| axis_offset_deg(got[i], got[(i + 1) % 3]) < 1e-2);
        assert!(flat);
    }

    #[test]
    fn a_circle_is_not_a_polygon() {
        for seed in 1..6 {
            let s = drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 1.08, 0.02, seed);
            let got = fit(&s);
            assert!(
                got.as_ref().is_none_or(|c| c.len() < 5),
                "seed {seed}: {got:?}"
            );
        }
    }

    #[test]
    fn bowtie_is_rejected() {
        let bow = vec![
            [100.0, 100.0],
            [400.0, 300.0],
            [400.0, 100.0],
            [100.0, 300.0],
        ];
        assert!(fit(&drawn_polygon(&bow, 0.0, 0.005, 1)).is_none());
    }

    #[test]
    fn open_stroke_is_not_a_polygon() {
        let corners = rect_corners([250.0, 200.0], 300.0, 200.0, 0.0);
        let open = hand_drawn(|t| polygon_point(&corners, t), 100, 0.0, 0.6, 0.01, 1);
        assert!(fit(&open).is_none());
    }

    #[test]
    fn rounded_corners_still_make_a_rectangle() {
        let corners = rect_corners([250.0, 200.0], 300.0, 200.0, 0.0);
        let s = hand_drawn(
            |t| {
                let p = polygon_point(&corners, t);
                let q = polygon_point(&corners, t + 0.01);
                let r = polygon_point(&corners, t - 0.01);
                [
                    (p[0] * 2.0 + q[0] + r[0]) / 4.0,
                    (p[1] * 2.0 + q[1] + r[1]) / 4.0,
                ]
            },
            120,
            0.0,
            1.05,
            0.01,
            2,
        );
        assert_eq!(fit(&s).expect("rectangle").len(), 4);
    }
}
