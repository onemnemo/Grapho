//! The shape value a recogniser produces, and its outline.

use super::geom::{add, dist, lerp, rotate_about, scale, sub, unit};
use std::f32::consts::TAU;

/// Upper bound on points generated per outline, whatever the spacing.
const MAX_PATH_POINTS: usize = 20_000;
/// Widest angle between neighbouring arc points, so an arc stays round even with no spacing.
const ARC_MAX_STEP: f32 = std::f32::consts::FRAC_PI_4;

/// A recognised shape, in the same units as the input points.
#[derive(Clone, Debug, PartialEq)]
pub enum Shape {
    Line {
        from: [f32; 2],
        to: [f32; 2],
    },
    /// `rotation` is the angle in radians of the `rx` axis from +x.
    Ellipse {
        center: [f32; 2],
        rx: f32,
        ry: f32,
        rotation: f32,
    },
    /// Corners in drawing order. Always closed (last corner joins the first).
    Polygon {
        corners: Vec<[f32; 2]>,
    },
    /// A shaft from `from` to the tip `to` with two barbs of length `head`
    /// at `spread` radians either side of the reversed shaft direction.
    Arrow {
        from: [f32; 2],
        to: [f32; 2],
        head: f32,
        spread: f32,
    },
    /// Part of a circle. `start` is the angle in radians of the first point
    /// from +x, and `sweep` is signed so the path follows the drawing
    /// direction from `start` to `start + sweep`.
    Arc {
        center: [f32; 2],
        radius: f32,
        start: f32,
        sweep: f32,
    },
    /// Straight legs joined at their corners, from the first point to the last.
    Polyline {
        points: Vec<[f32; 2]>,
    },
    /// A smoothed open stroke, as the points of its outline in drawing order.
    Curve {
        points: Vec<[f32; 2]>,
    },
    /// Only the two barbs, for a shaft that already exists. `toward` is the
    /// unit vector pointing from the tip back along the shaft.
    Head {
        tip: [f32; 2],
        toward: [f32; 2],
        head: f32,
        spread: f32,
    },
}

impl Shape {
    /// The outline as a polyline, at most `spacing` apart along curves, with
    /// every corner included exactly. Closed shapes end on their first point.
    ///
    /// An arc runs from its start angle through its signed sweep, and a curve
    /// is its points with long gaps filled in.
    ///
    /// An arrow is one open polyline: shaft, first barb, back to the tip,
    /// second barb. A head is the first barb, the tip, the second barb.
    ///
    /// A `spacing` that is zero, negative or not finite yields only the
    /// corners (or eight points for an ellipse). The number of points is
    /// capped, so a tiny spacing gives a coarser outline instead of a huge one.
    pub fn path(&self, spacing: f32) -> Vec<[f32; 2]> {
        let spacing = if spacing.is_finite() && spacing > 0.0 {
            spacing
        } else {
            f32::INFINITY
        };
        match self {
            Shape::Line { from, to } => {
                let mut out = vec![*from];
                push_segment(&mut out, *from, *to, spacing);
                out
            }
            Shape::Polyline { points } => {
                let mut out = Vec::new();
                if let Some(&first) = points.first() {
                    out.push(first);
                    for w in points.windows(2) {
                        push_segment(&mut out, w[0], w[1], spacing);
                    }
                }
                out
            }
            Shape::Polygon { corners } => {
                let mut out = Vec::new();
                if let Some(&first) = corners.first() {
                    out.push(first);
                    for i in 0..corners.len() {
                        let next = corners.get(i + 1).copied().unwrap_or(first);
                        push_segment(&mut out, corners[i], next, spacing);
                    }
                }
                out
            }
            Shape::Arrow {
                from,
                to,
                head,
                spread,
            } => {
                let (b1, b2) = barb_ends(*to, unit(sub(*from, *to)), *head, *spread);
                let mut out = vec![*from];
                push_segment(&mut out, *from, *to, spacing);
                push_segment(&mut out, *to, b1, spacing);
                push_segment(&mut out, b1, *to, spacing);
                push_segment(&mut out, *to, b2, spacing);
                out
            }
            Shape::Head {
                tip,
                toward,
                head,
                spread,
            } => {
                let (b1, b2) = barb_ends(*tip, unit(*toward), *head, *spread);
                let mut out = vec![b1];
                push_segment(&mut out, b1, *tip, spacing);
                push_segment(&mut out, *tip, b2, spacing);
                out
            }
            Shape::Arc {
                center,
                radius,
                start,
                sweep,
            } => {
                let by_spacing = (radius.abs() * sweep.abs() / spacing).ceil();
                let by_angle = (sweep.abs() / ARC_MAX_STEP).ceil();
                let steps = (by_spacing.max(by_angle) as usize).clamp(1, MAX_PATH_POINTS);
                (0..=steps)
                    .map(|k| {
                        let a = start + sweep * k as f32 / steps as f32;
                        [center[0] + radius * a.cos(), center[1] + radius * a.sin()]
                    })
                    .collect()
            }
            Shape::Curve { points } => {
                let mut out = Vec::with_capacity(points.len());
                if let Some(&first) = points.first() {
                    out.push(first);
                    for w in points.windows(2) {
                        push_segment(&mut out, w[0], w[1], spacing);
                    }
                }
                out
            }
            Shape::Ellipse {
                center,
                rx,
                ry,
                rotation,
            } => {
                let fastest = rx.abs().max(ry.abs()) * TAU;
                let steps = ((fastest / spacing).ceil() as usize).clamp(8, MAX_PATH_POINTS);
                let (s, c) = rotation.sin_cos();
                let at = |k: usize| {
                    let t = TAU * k as f32 / steps as f32;
                    let (x, y) = (rx * t.cos(), ry * t.sin());
                    [center[0] + x * c - y * s, center[1] + x * s + y * c]
                };
                let mut out: Vec<[f32; 2]> = (0..steps).map(at).collect();
                out.push(out[0]);
                out
            }
        }
    }
}

/// Ends of the two barbs of a head whose tip is at `tip`, given the unit
/// vector pointing back along the shaft.
fn barb_ends(tip: [f32; 2], back: [f32; 2], head: f32, spread: f32) -> ([f32; 2], [f32; 2]) {
    let end = |a: f32| add(tip, scale(rotate_about(back, [0.0, 0.0], a), head));
    (end(spread), end(-spread))
}

/// Appends the points after `a` up to and including `b`.
fn push_segment(out: &mut Vec<[f32; 2]>, a: [f32; 2], b: [f32; 2], spacing: f32) {
    let steps = ((dist(a, b) / spacing).ceil() as usize).clamp(1, MAX_PATH_POINTS);
    for k in 1..steps {
        out.push(lerp(a, b, k as f32 / steps as f32));
    }
    out.push(b);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn path_of_closed_shapes_ends_on_first_point() {
        let shapes = [
            Shape::Ellipse {
                center: [50.0, 50.0],
                rx: 40.0,
                ry: 20.0,
                rotation: 0.3,
            },
            Shape::Polygon {
                corners: vec![[0.0, 0.0], [100.0, 0.0], [50.0, 80.0]],
            },
        ];
        for s in shapes {
            let p = s.path(5.0);
            assert_eq!(p.first(), p.last());
        }
    }

    #[test]
    fn path_respects_spacing_and_includes_corners() {
        let corners = vec![[0.0, 0.0], [103.0, 0.0], [103.0, 57.0], [0.0, 57.0]];
        let poly = Shape::Polygon {
            corners: corners.clone(),
        };
        let p = poly.path(10.0);
        for c in &corners {
            assert!(p.contains(c));
        }
        assert!(p.windows(2).all(|w| dist(w[0], w[1]) <= 10.0 + 1e-3));

        let ell = Shape::Ellipse {
            center: [0.0, 0.0],
            rx: 90.0,
            ry: 30.0,
            rotation: 0.5,
        };
        let p = ell.path(8.0);
        assert!(p.windows(2).all(|w| dist(w[0], w[1]) <= 8.0 + 1e-3));

        let line = Shape::Line {
            from: [0.0, 0.0],
            to: [100.0, 0.0],
        };
        let p = line.path(30.0);
        assert_eq!(p.first(), Some(&[0.0, 0.0]));
        assert_eq!(p.last(), Some(&[100.0, 0.0]));
        assert!(p.windows(2).all(|w| dist(w[0], w[1]) <= 30.0 + 1e-3));
    }

    #[test]
    fn path_survives_bad_spacing() {
        let s = Shape::Polygon {
            corners: vec![[0.0, 0.0], [10.0, 0.0], [0.0, 10.0]],
        };
        for spacing in [0.0, -1.0, f32::NAN, f32::INFINITY, 1e-9] {
            assert!(s.path(spacing).len() >= 4);
        }
    }

    #[test]
    fn arrow_and_head_paths_visit_the_tip_and_both_barbs() {
        let arrow = Shape::Arrow {
            from: [0.0, 0.0],
            to: [100.0, 0.0],
            head: 20.0,
            spread: 0.5,
        };
        let p = arrow.path(10.0);
        assert_eq!(p.first(), Some(&[0.0, 0.0]));
        assert!(p.windows(2).all(|w| dist(w[0], w[1]) <= 10.0 + 1e-3));
        assert_eq!(p.iter().filter(|q| **q == [100.0, 0.0]).count(), 2);
        let head = Shape::Head {
            tip: [100.0, 0.0],
            toward: [-1.0, 0.0],
            head: 20.0,
            spread: 0.5,
        };
        let h = head.path(10.0);
        assert_eq!(h.iter().filter(|q| **q == [100.0, 0.0]).count(), 1);
        assert_eq!(h.first().map(|q| q[1] < 0.0), Some(true));
        assert_eq!(h.last().map(|q| q[1] > 0.0), Some(true));
    }
}
