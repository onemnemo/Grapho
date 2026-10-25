//! Circle and ellipse recogniser.

use super::geom::{centroid, sub, P};
use super::{Candidate, Recognizer, Shape, Stroke};
use std::f32::consts::{FRAC_PI_2, PI, TAU};

/// Fraction of the 10 degree bins around the centre the stroke must visit.
const MIN_COVERAGE: f32 = 280.0 / 360.0;
/// Total turn around the centre, in degrees. A stroke whose ends do not meet
/// (an overdrawn loop that stops short of its start) must go further.
const MIN_SWEEP_CLOSED_DEG: f32 = 280.0;
const MIN_SWEEP_OPEN_DEG: f32 = 320.0;
/// Ellipse errors are scaled up so that a polygon with clean straight sides
/// beats an ellipse that merely comes close to it.
const ELLIPSE_BIAS: f32 = 1.2;
const BINS: usize = 36;
/// Axis ratio above which the result is a circle.
const CIRCLE_RATIO: f32 = 0.85;
const MIN_AXIS_RATIO: f32 = 0.2;
/// Most points this far (relative) from the fitted outline means corners, not a
/// curve. Checked at a high percentile so one lump where the stroke starts or
/// ends does not reject an otherwise round stroke.
const MAX_RADIAL_ERROR: f32 = 0.25;
const RADIAL_ERROR_PERCENTILE: f32 = 0.9;
const TIGHTEN_STEPS: usize = 12;
const TIGHTEN_MAX: f32 = 0.12;
const MAX_MEAN_RADIAL_ERROR: f32 = 0.1;
const AXIS_SNAP_DEG: f32 = 8.0;

/// Finds a circle or an ellipse in a closed stroke.
#[derive(Clone, Copy, Debug, Default)]
pub struct EllipseRecognizer;

impl Recognizer for EllipseRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        let pts = &stroke.points;
        let c0 = centroid(pts);
        let (mut sxx, mut syy, mut sxy) = (0.0, 0.0, 0.0);
        for p in pts {
            let d = sub(*p, c0);
            sxx += d[0] * d[0];
            syy += d[1] * d[1];
            sxy += d[0] * d[1];
        }
        let theta = 0.5 * (2.0 * sxy).atan2(sxx - syy);
        let (s, c) = theta.sin_cos();
        let local: Vec<P> = pts
            .iter()
            .map(|p| {
                let d = sub(*p, c0);
                [d[0] * c + d[1] * s, -d[0] * s + d[1] * c]
            })
            .collect();
        let (mut lo, mut hi) = (local[0], local[0]);
        for p in &local {
            for k in 0..2 {
                lo[k] = lo[k].min(p[k]);
                hi[k] = hi[k].max(p[k]);
            }
        }
        let mid = [(lo[0] + hi[0]) / 2.0, (lo[1] + hi[1]) / 2.0];
        let (rx, ry) = tighten(&local, mid, (hi[0] - lo[0]) / 2.0, (hi[1] - lo[1]) / 2.0);
        if rx.min(ry) < MIN_AXIS_RATIO * rx.max(ry) || rx.min(ry) < 1.0 {
            return None;
        }

        let mut bins = [false; BINS];
        let mut sweep = 0.0f32;
        let mut prev_angle: Option<f32> = None;
        let mut errors = Vec::with_capacity(local.len());
        for p in &local {
            let (nx, ny) = ((p[0] - mid[0]) / rx, (p[1] - mid[1]) / ry);
            let e = (nx.hypot(ny) - 1.0).abs();
            errors.push(e);
            let raw_angle = (p[1] - mid[1]).atan2(p[0] - mid[0]);
            if let Some(prev) = prev_angle {
                sweep += (raw_angle - prev + PI).rem_euclid(TAU) - PI;
            }
            prev_angle = Some(raw_angle);
            let a = raw_angle.rem_euclid(TAU);
            bins[((a / TAU * BINS as f32) as usize).min(BINS - 1)] = true;
        }
        let sum_err: f32 = errors.iter().sum();
        errors.sort_by(f32::total_cmp);
        let high = ((errors.len() - 1) as f32 * RADIAL_ERROR_PERCENTILE) as usize;
        let max_err = errors[high];
        let covered = bins.iter().filter(|&&b| b).count() as f32 / BINS as f32;
        let min_sweep = if stroke.closed {
            MIN_SWEEP_CLOSED_DEG
        } else {
            MIN_SWEEP_OPEN_DEG
        };
        if covered < MIN_COVERAGE
            || max_err > MAX_RADIAL_ERROR
            || sweep.abs() < min_sweep.to_radians()
        {
            return None;
        }
        let mean_err = sum_err / local.len() as f32;
        if mean_err > MAX_MEAN_RADIAL_ERROR {
            return None;
        }
        let mean_r = (rx + ry) / 2.0;
        let score = ELLIPSE_BIAS * mean_err * mean_r / stroke.diagonal;

        let center = [
            c0[0] + mid[0] * c - mid[1] * s,
            c0[1] + mid[0] * s + mid[1] * c,
        ];
        Some(Candidate {
            shape: regularise(center, rx, ry, theta),
            score,
        })
    }
}

fn mean_error(local: &[P], mid: P, rx: f32, ry: f32) -> f32 {
    let sum: f32 = local
        .iter()
        .map(|p| (((p[0] - mid[0]) / rx).hypot((p[1] - mid[1]) / ry) - 1.0).abs())
        .sum();
    sum / local.len() as f32
}

/// The extents are pushed out by every bump in the stroke, so shrink each
/// radius slightly when that lowers the mean radial error.
fn tighten(local: &[P], mid: P, rx: f32, ry: f32) -> (f32, f32) {
    let mut best = (rx, ry);
    let mut best_err = f32::INFINITY;
    for i in 0..=TIGHTEN_STEPS {
        for j in 0..=TIGHTEN_STEPS {
            let k = |n: usize| 1.0 - TIGHTEN_MAX * n as f32 / TIGHTEN_STEPS as f32;
            let (cx, cy) = (rx * k(i), ry * k(j));
            let e = mean_error(local, mid, cx, cy);
            if e < best_err {
                best_err = e;
                best = (cx, cy);
            }
        }
    }
    best
}

/// Turns near-circles into circles and near-axis-aligned ellipses into exactly
/// aligned ones, with the radii swapped so the rotation is zero.
fn regularise(center: P, rx: f32, ry: f32, theta: f32) -> Shape {
    if rx.min(ry) / rx.max(ry) > CIRCLE_RATIO {
        let r = (rx + ry) / 2.0;
        return Shape::Ellipse {
            center,
            rx: r,
            ry: r,
            rotation: 0.0,
        };
    }
    let rotation = theta.rem_euclid(PI);
    let snap = AXIS_SNAP_DEG.to_radians();
    let (rx, ry, rotation) = if rotation < snap || rotation > PI - snap {
        (rx, ry, 0.0)
    } else if (rotation - FRAC_PI_2).abs() < snap {
        (ry, rx, 0.0)
    } else {
        (rx, ry, rotation)
    };
    Shape::Ellipse {
        center,
        rx,
        ry,
        rotation,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;

    fn centre_error(shape: &Shape, expected: P) -> f32 {
        match shape {
            Shape::Ellipse { center, .. } => dist(*center, expected),
            _ => f32::INFINITY,
        }
    }

    fn fit(points: &[crate::InputSample]) -> Option<Shape> {
        EllipseRecognizer
            .fit(&Stroke::new(points)?)
            .map(|c| c.shape)
    }

    #[test]
    fn recognises_a_circle() {
        let s = drawn_ellipse([300.0, 250.0], 120.0, 120.0, 0.0, 1.05, 0.02, 2);
        let shape = fit(&s).expect("circle");
        let Shape::Ellipse {
            rx, ry, rotation, ..
        } = shape
        else {
            panic!("not an ellipse");
        };
        assert_eq!(rx, ry);
        assert_eq!(rotation, 0.0);
        assert!((rx - 120.0).abs() < 12.0);
        assert!(centre_error(&shape, [300.0, 250.0]) < 8.0);
    }

    #[test]
    fn overdrawn_circle_is_still_a_circle() {
        let s = drawn_ellipse([200.0, 200.0], 80.0, 80.0, 0.0, 1.3, 0.02, 4);
        let Some(Shape::Ellipse { rx, ry, .. }) = fit(&s) else {
            panic!("expected circle");
        };
        assert_eq!(rx, ry);
    }

    #[test]
    fn recognises_an_axis_aligned_ellipse() {
        let s = drawn_ellipse([300.0, 250.0], 180.0, 80.0, 0.03, 1.05, 0.015, 3);
        let Some(Shape::Ellipse {
            rx, ry, rotation, ..
        }) = fit(&s)
        else {
            panic!("expected ellipse");
        };
        assert_eq!(rotation, 0.0);
        assert!(rx > ry * 1.6);
    }

    #[test]
    fn tall_ellipse_swaps_radii() {
        let s = drawn_ellipse([300.0, 250.0], 70.0, 170.0, 0.0, 1.05, 0.015, 3);
        let Some(Shape::Ellipse {
            rx, ry, rotation, ..
        }) = fit(&s)
        else {
            panic!("expected ellipse");
        };
        assert_eq!(rotation, 0.0);
        assert!(ry > rx * 1.6);
    }

    #[test]
    fn keeps_a_rotated_ellipse_rotated() {
        let s = drawn_ellipse([300.0, 250.0], 170.0, 70.0, 0.6, 1.05, 0.015, 5);
        let Some(Shape::Ellipse {
            rx, ry, rotation, ..
        }) = fit(&s)
        else {
            panic!("expected ellipse");
        };
        assert!((rotation - 0.6).abs() < 0.1, "rotation {rotation}");
        assert!(rx > ry);
    }

    #[test]
    fn a_square_is_not_an_ellipse() {
        // A square fits an ellipse loosely; the polygon fit must still win.
        let sq = rect_corners([200.0, 200.0], 200.0, 200.0, 0.0);
        for seed in 0..6 {
            let got = crate::shape::ShapeRecognizer::default()
                .recognize(&drawn_polygon(&sq, 0.05, 0.02, seed));
            assert!(
                matches!(got, Some(Shape::Polygon { .. })),
                "seed {seed}: {got:?}"
            );
        }
    }

    #[test]
    fn open_arc_is_not_an_ellipse() {
        let s = drawn_ellipse([200.0, 200.0], 100.0, 100.0, 0.0, 0.6, 0.01, 1);
        assert!(fit(&s).is_none());
    }
}
