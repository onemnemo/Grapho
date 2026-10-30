//! What a recognised shape is called, for offering it to the person.

use super::model::Shape;
use crate::InputSample;

/// How close the two radii of an ellipse may be for it to be called a circle,
/// as a fraction of the larger one.
const CIRCLE_TOLERANCE: f32 = 0.08;

/// The name a person would give a recognised shape. The numbers are part of
/// the interface to the platform shells, so they only ever grow.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[repr(u8)]
pub enum ShapeClass {
    Line = 1,
    Arrow = 2,
    Circle = 3,
    Ellipse = 4,
    Triangle = 5,
    Rectangle = 6,
    Polygon = 7,
    Arc = 8,
    Elbow = 9,
}

impl ShapeClass {
    /// The number the shells know this class by.
    pub fn code(self) -> u8 {
        self as u8
    }
}

impl Shape {
    /// What to call this shape, or None for the ones that are smoothing of a
    /// stroke rather than a shape someone meant to draw: a smoothed curve, and
    /// a lone arrow head.
    pub fn class(&self) -> Option<ShapeClass> {
        match self {
            Shape::Line { .. } => Some(ShapeClass::Line),
            Shape::Arrow { .. } => Some(ShapeClass::Arrow),
            Shape::Ellipse { rx, ry, .. } => {
                let (hi, lo) = (rx.abs().max(ry.abs()), rx.abs().min(ry.abs()));
                if hi > 0.0 && (hi - lo) / hi <= CIRCLE_TOLERANCE {
                    Some(ShapeClass::Circle)
                } else {
                    Some(ShapeClass::Ellipse)
                }
            }
            Shape::Polygon { corners } => Some(match corners.len() {
                3 => ShapeClass::Triangle,
                4 => ShapeClass::Rectangle,
                _ => ShapeClass::Polygon,
            }),
            Shape::Arc { .. } => Some(ShapeClass::Arc),
            Shape::Polyline { .. } => Some(ShapeClass::Elbow),
            Shape::Curve { .. } | Shape::Head { .. } => None,
        }
    }
}

/// A shape worth offering for a finished stroke: what it is called and the
/// stroke redrawn as it, as in [`snap`](super::snap). None when nothing fits,
/// or the best fit is only a smoothed curve.
pub fn suggest(points: &[InputSample], spacing: f32) -> Option<(ShapeClass, Vec<InputSample>)> {
    let (shape, samples) = super::snap(points, spacing)?;
    Some((shape.class()?, samples))
}

#[cfg(test)]
mod tests {
    use super::super::testing::{drawn_ellipse, drawn_polygon, hand_drawn, rect_corners};
    use super::*;

    fn ellipse(rx: f32, ry: f32) -> Shape {
        Shape::Ellipse {
            center: [0.0, 0.0],
            rx,
            ry,
            rotation: 0.0,
        }
    }

    #[test]
    fn near_equal_radii_make_a_circle() {
        assert_eq!(ellipse(100.0, 100.0).class(), Some(ShapeClass::Circle));
        assert_eq!(ellipse(100.0, 95.0).class(), Some(ShapeClass::Circle));
        assert_eq!(ellipse(100.0, 70.0).class(), Some(ShapeClass::Ellipse));
        assert_eq!(ellipse(0.0, 0.0).class(), Some(ShapeClass::Ellipse));
    }

    #[test]
    fn polygons_are_named_by_their_corners() {
        let poly = |n: usize| Shape::Polygon {
            corners: vec![[0.0, 0.0]; n],
        };
        assert_eq!(poly(3).class(), Some(ShapeClass::Triangle));
        assert_eq!(poly(4).class(), Some(ShapeClass::Rectangle));
        assert_eq!(poly(5).class(), Some(ShapeClass::Polygon));
        assert_eq!(poly(8).class(), Some(ShapeClass::Polygon));
    }

    #[test]
    fn lines_arrows_arcs_and_elbows_are_named() {
        let line = Shape::Line {
            from: [0.0, 0.0],
            to: [1.0, 1.0],
        };
        assert_eq!(line.class(), Some(ShapeClass::Line));
        let arrow = Shape::Arrow {
            from: [0.0, 0.0],
            to: [1.0, 1.0],
            head: 1.0,
            spread: 0.5,
        };
        assert_eq!(arrow.class(), Some(ShapeClass::Arrow));
        let arc = Shape::Arc {
            center: [0.0, 0.0],
            radius: 1.0,
            start: 0.0,
            sweep: 1.0,
        };
        assert_eq!(arc.class(), Some(ShapeClass::Arc));
        let elbow = Shape::Polyline {
            points: vec![[0.0, 0.0], [1.0, 0.0], [1.0, 1.0]],
        };
        assert_eq!(elbow.class(), Some(ShapeClass::Elbow));
    }

    #[test]
    fn smoothing_is_not_a_shape_to_offer() {
        assert_eq!(Shape::Curve { points: vec![] }.class(), None);
        let head = Shape::Head {
            tip: [0.0, 0.0],
            toward: [1.0, 0.0],
            head: 1.0,
            spread: 0.5,
        };
        assert_eq!(head.class(), None);
    }

    #[test]
    fn the_codes_never_change() {
        let codes = [
            ShapeClass::Line,
            ShapeClass::Arrow,
            ShapeClass::Circle,
            ShapeClass::Ellipse,
            ShapeClass::Triangle,
            ShapeClass::Rectangle,
            ShapeClass::Polygon,
            ShapeClass::Arc,
            ShapeClass::Elbow,
        ]
        .map(ShapeClass::code);
        assert_eq!(codes, [1, 2, 3, 4, 5, 6, 7, 8, 9]);
    }

    #[test]
    fn a_wobbly_circle_and_square_are_offered() {
        let circle = drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 1.08, 0.02, 1);
        let (class, samples) = suggest(&circle, 3.0).expect("circle");
        assert_eq!(class, ShapeClass::Circle);
        assert!(samples.len() > 8);
        let square = rect_corners([300.0, 300.0], 220.0, 220.0, 0.0);
        let (class, _) = suggest(&drawn_polygon(&square, 0.05, 0.02, 2), 3.0).expect("square");
        assert_eq!(class, ShapeClass::Rectangle);
    }

    #[test]
    fn a_gentle_curve_and_a_dot_are_not_offered() {
        let s_curve = hand_drawn(
            |t| {
                [
                    40.0 + 260.0 * t,
                    200.0 + 70.0 * (t * std::f32::consts::TAU).sin(),
                ]
            },
            120,
            0.0,
            1.0,
            0.01,
            1,
        );
        assert!(suggest(&s_curve, 3.0).is_none());
        assert!(suggest(&[], 3.0).is_none());
    }
}
