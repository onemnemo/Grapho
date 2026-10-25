//! Reshaping a snapped shape while the pen stays down.
//!
//! Once a stroke has snapped, moving the pen keeps working on the shape:
//! a line or arrow follows the pen with its start fixed, an arc continues
//! along its circle, and closed shapes scale and turn about their centre.

use super::geom::{add, centroid, len, rotate_about, scale, sub, P};
use super::Shape;
use std::f32::consts::{FRAC_PI_4, PI, TAU};

/// Turning less than this is taken as the hand drifting, not as a rotation.
const ROTATION_DEADBAND_DEG: f32 = 8.0;
/// A line within this of horizontal, vertical or 45 degrees lands on it.
const LINE_SNAP_DEG: f32 = 4.0;
const MIN_SCALE: f32 = 0.05;
/// Closer than this to a shape's centre the pen gives no usable direction.
const MIN_REACH: f32 = 4.0;

/// A shape being reshaped by the pen after it snapped.
#[derive(Clone, Debug, PartialEq)]
pub struct ShapeEdit {
    base: Shape,
    grab: P,
    current: Shape,
    // Arcs accumulate their sweep so they can pass a half turn and keep going.
    sweep: f32,
    last_angle: f32,
}

impl ShapeEdit {
    /// Starts reshaping `base`, with the pen at `grab` where the shape snapped.
    pub fn new(base: Shape, grab: [f32; 2]) -> Self {
        let (sweep, last_angle) = match base {
            Shape::Arc { center, sweep, .. } => (sweep, angle(sub(grab, center))),
            _ => (0.0, 0.0),
        };
        Self {
            current: base.clone(),
            base,
            grab,
            sweep,
            last_angle,
        }
    }

    /// The shape as last reshaped.
    pub fn shape(&self) -> &Shape {
        &self.current
    }

    /// Reshapes for the pen at `pen` and returns the new shape. Smoothed
    /// curves and separate arrow heads keep their shape. A pen too close to
    /// the point it would measure from leaves the shape as it was, and
    /// non-finite input is ignored.
    pub fn move_to(&mut self, pen: [f32; 2]) -> &Shape {
        if !(pen[0].is_finite() && pen[1].is_finite()) {
            return &self.current;
        }
        if let Some(next) = self.reshaped(pen) {
            self.current = next;
        }
        &self.current
    }

    fn reshaped(&mut self, pen: P) -> Option<Shape> {
        match self.base.clone() {
            Shape::Line { from, .. } => Some(Shape::Line {
                from,
                to: follow(from, pen)?,
            }),
            Shape::Arrow {
                from, head, spread, ..
            } => Some(Shape::Arrow {
                from,
                to: follow(from, pen)?,
                head,
                spread,
            }),
            Shape::Arc {
                center,
                radius,
                start,
                ..
            } => {
                if len(sub(pen, center)) < MIN_REACH {
                    return None;
                }
                let a = angle(sub(pen, center));
                self.sweep = (self.sweep + wrap(a - self.last_angle)).clamp(-TAU, TAU);
                self.last_angle = a;
                if self.sweep.abs() >= TAU {
                    return Some(Shape::Ellipse {
                        center,
                        rx: radius,
                        ry: radius,
                        rotation: 0.0,
                    });
                }
                Some(Shape::Arc {
                    center,
                    radius,
                    start,
                    sweep: self.sweep,
                })
            }
            Shape::Ellipse {
                center,
                rx,
                ry,
                rotation,
            } => {
                let (k, turn) = self.scale_and_turn(center, pen)?;
                Some(Shape::Ellipse {
                    center,
                    rx: rx * k,
                    ry: ry * k,
                    rotation: if rx == ry { rotation } else { rotation + turn },
                })
            }
            Shape::Polygon { corners } => {
                let pivot = centroid(&corners);
                let (k, turn) = self.scale_and_turn(pivot, pen)?;
                let corners = corners
                    .iter()
                    .map(|&c| {
                        let scaled = add(pivot, scale(sub(c, pivot), k));
                        rotate_about(scaled, pivot, turn)
                    })
                    .collect();
                Some(Shape::Polygon { corners })
            }
            Shape::Curve { .. } | Shape::Polyline { .. } | Shape::Head { .. } => None,
        }
    }

    /// How far the pen has moved out from `pivot` relative to where it
    /// grabbed the shape, and how far it has turned around it.
    fn scale_and_turn(&self, pivot: P, pen: P) -> Option<(f32, f32)> {
        let (g, p) = (sub(self.grab, pivot), sub(pen, pivot));
        if len(g) < MIN_REACH || len(p) < MIN_REACH {
            return None;
        }
        let k = (len(p) / len(g)).max(MIN_SCALE);
        let turn = wrap(angle(p) - angle(g));
        let dead = ROTATION_DEADBAND_DEG.to_radians();
        // Measured past the dead band so the shape starts turning smoothly.
        let turn = if turn.abs() <= dead {
            0.0
        } else {
            turn - dead.copysign(turn)
        };
        Some((k, turn))
    }
}

/// The end of a line from `from` toward the pen, landing on horizontal,
/// vertical or 45 degrees when close.
fn follow(from: P, pen: P) -> Option<P> {
    let d = sub(pen, from);
    let length = len(d);
    if length < MIN_REACH {
        return None;
    }
    let a = angle(d);
    let target = (a / FRAC_PI_4).round() * FRAC_PI_4;
    if (a - target).abs() <= LINE_SNAP_DEG.to_radians() {
        return Some(add(from, [length * target.cos(), length * target.sin()]));
    }
    Some(pen)
}

fn angle(v: P) -> f32 {
    v[1].atan2(v[0])
}

/// An angle difference brought into `-PI..=PI`.
fn wrap(a: f32) -> f32 {
    (a + PI).rem_euclid(TAU) - PI
}

#[cfg(test)]
mod tests {
    use super::*;

    fn close(a: P, b: P) -> bool {
        len(sub(a, b)) < 1e-3
    }

    #[test]
    fn a_line_follows_the_pen_and_lands_on_the_horizontal() {
        let line = Shape::Line {
            from: [0.0, 0.0],
            to: [100.0, 0.0],
        };
        let mut e = ShapeEdit::new(line, [100.0, 0.0]);
        let Shape::Line { from, to } = e.move_to([200.0, 5.0]).clone() else {
            panic!("still a line");
        };
        assert!(close(from, [0.0, 0.0]));
        assert!(
            (to[1]).abs() < 1e-3 && (to[0] - 200.06).abs() < 0.1,
            "{to:?}"
        );
        let Shape::Line { to, .. } = e.move_to([100.0, 60.0]).clone() else {
            panic!("still a line");
        };
        assert!(close(to, [100.0, 60.0]));
    }

    #[test]
    fn an_arc_keeps_going_around_its_circle_and_closes_into_a_circle() {
        let arc = Shape::Arc {
            center: [0.0, 0.0],
            radius: 100.0,
            start: 0.0,
            sweep: PI,
        };
        let mut e = ShapeEdit::new(arc, [-100.0, 0.0]);
        // Past the half turn, three quarters of the way round.
        e.move_to([-70.0, -70.0]);
        let Shape::Arc { sweep, radius, .. } = e.move_to([0.0, -100.0]).clone() else {
            panic!("still an arc");
        };
        assert!((sweep - 1.5 * PI).abs() < 1e-3, "{sweep}");
        assert_eq!(radius, 100.0);
        e.move_to([70.0, -70.0]);
        e.move_to([100.0, 0.0]);
        assert!(matches!(e.shape(), Shape::Ellipse { rx, ry, .. } if rx == ry));
        // Coming back opens it again.
        let back = e.move_to([0.0, -100.0]).clone();
        assert!(matches!(back, Shape::Arc { .. }), "{back:?}");
    }

    #[test]
    fn an_arc_can_shrink() {
        let arc = Shape::Arc {
            center: [0.0, 0.0],
            radius: 50.0,
            start: 0.0,
            sweep: PI,
        };
        let mut e = ShapeEdit::new(arc, [-50.0, 0.0]);
        let Shape::Arc { sweep, .. } = e.move_to([0.0, 50.0]).clone() else {
            panic!("still an arc");
        };
        assert!((sweep - 0.5 * PI).abs() < 1e-3, "{sweep}");
    }

    #[test]
    fn a_square_scales_without_turning_for_a_small_drift() {
        let sq = Shape::Polygon {
            corners: vec![[-50.0, -50.0], [50.0, -50.0], [50.0, 50.0], [-50.0, 50.0]],
        };
        let mut e = ShapeEdit::new(sq, [50.0, 50.0]);
        let drift = 5f32.to_radians();
        let pen = rotate_about([100.0, 100.0], [0.0, 0.0], drift);
        let Shape::Polygon { corners } = e.move_to(pen).clone() else {
            panic!("still a polygon");
        };
        assert!(close(corners[2], [100.0, 100.0]), "{corners:?}");
        assert!(close(corners[0], [-100.0, -100.0]));
    }

    #[test]
    fn a_square_turns_once_the_pen_goes_round() {
        let sq = Shape::Polygon {
            corners: vec![[-50.0, -50.0], [50.0, -50.0], [50.0, 50.0], [-50.0, 50.0]],
        };
        let mut e = ShapeEdit::new(sq, [50.0, 50.0]);
        let pen = rotate_about([50.0, 50.0], [0.0, 0.0], 38f32.to_radians());
        let Shape::Polygon { corners } = e.move_to(pen).clone() else {
            panic!("still a polygon");
        };
        let expected = rotate_about([50.0, 50.0], [0.0, 0.0], 30f32.to_radians());
        assert!(close(corners[2], expected), "{corners:?}");
    }

    #[test]
    fn a_circle_scales_about_its_centre() {
        let c = Shape::Ellipse {
            center: [10.0, 10.0],
            rx: 40.0,
            ry: 40.0,
            rotation: 0.0,
        };
        let mut e = ShapeEdit::new(c, [50.0, 10.0]);
        let s = e.move_to([10.0, 30.0]).clone();
        assert_eq!(
            s,
            Shape::Ellipse {
                center: [10.0, 10.0],
                rx: 20.0,
                ry: 20.0,
                rotation: 0.0
            }
        );
    }

    #[test]
    fn curves_and_bad_input_leave_the_shape_alone() {
        let curve = Shape::Curve {
            points: vec![[0.0, 0.0], [10.0, 5.0], [20.0, 0.0]],
        };
        let mut e = ShapeEdit::new(curve.clone(), [20.0, 0.0]);
        assert_eq!(e.move_to([80.0, 80.0]), &curve);

        let c = Shape::Ellipse {
            center: [0.0, 0.0],
            rx: 40.0,
            ry: 40.0,
            rotation: 0.0,
        };
        let mut e = ShapeEdit::new(c.clone(), [40.0, 0.0]);
        assert_eq!(e.move_to([0.5, 0.0]), &c);
        assert_eq!(e.move_to([f32::NAN, 3.0]), &c);
    }
}
