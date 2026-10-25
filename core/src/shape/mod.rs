//! Hand-drawn shape recognition.
//!
//! A stroke is preprocessed once into a [`Stroke`], every registered
//! [`Recognizer`] proposes a [`Candidate`], and the best one under a
//! threshold wins. A new shape kind is one new recogniser registered with
//! [`ShapeRecognizer::with`].

mod arc;
mod arrow;
mod barbs;
mod class;
mod curve;
mod draw;
mod edit;
mod elbow;
mod ellipse;
mod geom;
mod head;
mod line;
mod model;
mod polygon;
mod regularise;
mod stroke;
#[cfg(test)]
mod testing;

pub use arc::ArcRecognizer;
pub use arrow::ArrowRecognizer;
pub use class::{suggest, ShapeClass};
pub use draw::{draw_shape, DrawKind, DrawSpec};
pub use edit::ShapeEdit;
pub use elbow::ElbowRecognizer;
pub use ellipse::EllipseRecognizer;
pub use line::LineRecognizer;
pub use model::Shape;
pub use polygon::PolygonRecognizer;
pub use stroke::Stroke;

use crate::InputSample;

/// Candidates scoring above this are not trusted, so the stroke is left alone.
const ACCEPT_SCORE: f32 = 0.08;

/// A shape proposed for a stroke.
#[derive(Clone, Debug, PartialEq)]
pub struct Candidate {
    pub shape: Shape,
    /// Fit error normalised by stroke size (0 is perfect), already including
    /// any per-kind bias. Lower wins across recognisers.
    pub score: f32,
}

/// One kind of shape the recogniser can find.
pub trait Recognizer: Send + Sync {
    /// Best fit of this kind to the stroke, or None if it clearly is not one.
    fn fit(&self, stroke: &Stroke) -> Option<Candidate>;
}

/// Runs every registered recogniser and keeps the best candidate under a threshold.
pub struct ShapeRecognizer {
    recognizers: Vec<Box<dyn Recognizer>>,
    accept_score: f32,
    smoothing: bool,
}

impl ShapeRecognizer {
    /// A recogniser with nothing registered. It never recognises anything
    /// until given recognisers with [`with`](Self::with).
    pub fn new() -> Self {
        Self {
            recognizers: Vec::new(),
            accept_score: ACCEPT_SCORE,
            smoothing: false,
        }
    }

    /// Registers one more kind of shape.
    pub fn with(mut self, r: impl Recognizer + 'static) -> Self {
        self.recognizers.push(Box::new(r));
        self
    }

    /// Turns the smooth curve fallback on or off. When on, a gentle open
    /// stroke that no recogniser accepts comes back as a [`Shape::Curve`].
    /// Off by default for [`new`](Self::new), on for the default recogniser.
    pub fn with_smoothing(mut self, on: bool) -> Self {
        self.smoothing = on;
        self
    }

    /// The best shape for the stroke, or None when the stroke is too small, has
    /// non-finite coordinates, or nothing fits it well enough. With smoothing
    /// on, a gentle curve is smoothed instead of being left alone.
    pub fn recognize(&self, points: &[InputSample]) -> Option<Shape> {
        let stroke = Stroke::new(points)?;
        let best = self
            .recognizers
            .iter()
            .filter_map(|r| r.fit(&stroke))
            .filter(|c| c.score.is_finite() && c.score <= self.accept_score)
            .min_by(|a, b| a.score.total_cmp(&b.score))
            .map(|c| c.shape);
        best.or_else(|| {
            if self.smoothing {
                curve::smooth(&stroke).map(|points| Shape::Curve { points })
            } else {
                None
            }
        })
    }
}

impl Default for ShapeRecognizer {
    /// Every built-in recogniser.
    fn default() -> Self {
        Self::new()
            .with(LineRecognizer)
            .with(EllipseRecognizer)
            .with(PolygonRecognizer)
            .with(ArrowRecognizer)
            .with(ArcRecognizer)
            .with(ElbowRecognizer)
            .with_smoothing(true)
    }
}

/// Recognises the stroke with the default recognisers and returns the snapped
/// stroke as samples: points along [`Shape::path`] at `spacing`, every point
/// with the stroke's median pressure, times spread evenly from the first to
/// the last input time. None if nothing was recognised.
pub fn snap(points: &[InputSample], spacing: f32) -> Option<(Shape, Vec<InputSample>)> {
    let shape = ShapeRecognizer::default().recognize(points)?;
    let samples = samples_along(&shape, points, spacing)?;
    Some((shape, samples))
}

/// Snaps an arrow head drawn as its own stroke onto the end of an existing
/// straight shaft. The stroke must be a V, a caret or a single barb whose
/// apex (or, for a barb, the end nearest the tip) is within the larger of
/// 0.35 of the head size and 12 units of `shaft_to`, with arms leaving the
/// shaft at 12 to 75 degrees. Returns a [`Shape::Head`] with its tip exactly
/// at `shaft_to` and samples built as in [`snap`]. None for anything else,
/// including a shaft whose ends coincide or are not finite.
pub fn snap_head(
    points: &[InputSample],
    shaft_from: [f32; 2],
    shaft_to: [f32; 2],
    spacing: f32,
) -> Option<(Shape, Vec<InputSample>)> {
    let shape = head::fit_head(points, shaft_from, shaft_to)?;
    let samples = samples_along(&shape, points, spacing)?;
    Some((shape, samples))
}

/// Samples along `shape`'s outline at `spacing`, with the median pressure of
/// `points` and times spread evenly across theirs. None if either is empty.
pub fn samples_along(
    shape: &Shape,
    points: &[InputSample],
    spacing: f32,
) -> Option<Vec<InputSample>> {
    let path = shape.path(spacing);
    if path.is_empty() {
        return None;
    }
    let pressure = median_pressure(points);
    let t0 = points.first()?.time_ms;
    let t1 = points.last()?.time_ms.max(t0);
    let last = path.len().saturating_sub(1).max(1) as f32;
    Some(
        path.iter()
            .enumerate()
            .map(|(i, p)| InputSample::new(p[0], p[1], pressure, t0 + (t1 - t0) * i as f32 / last))
            .collect(),
    )
}

fn median_pressure(points: &[InputSample]) -> f32 {
    let mut p: Vec<f32> = points.iter().map(|s| s.pressure).collect();
    p.sort_by(f32::total_cmp);
    match p.len() {
        0 => 0.0,
        n if n % 2 == 1 => p[n / 2],
        n => (p[n / 2 - 1] + p[n / 2]) / 2.0,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use testing::*;

    fn recognize(points: &[InputSample]) -> Option<Shape> {
        ShapeRecognizer::default().recognize(points)
    }

    fn corners_of(shape: Option<Shape>) -> Vec<[f32; 2]> {
        match shape {
            Some(Shape::Polygon { corners }) => corners,
            other => panic!("expected a polygon, got {other:?}"),
        }
    }

    /// Largest distance from any expected corner to its nearest fitted corner.
    fn corner_error(got: &[[f32; 2]], expected: &[[f32; 2]]) -> f32 {
        expected
            .iter()
            .map(|e| {
                got.iter()
                    .map(|g| dist(*g, *e))
                    .fold(f32::INFINITY, f32::min)
            })
            .fold(0.0, f32::max)
    }

    #[test]
    fn empty_and_tiny_input_is_none() {
        assert!(recognize(&[]).is_none());
        let two = [
            InputSample::new(0.0, 0.0, 0.5, 0.0),
            InputSample::new(200.0, 0.0, 0.5, 1.0),
        ];
        assert!(recognize(&two).is_none());
        let tiny = hand_drawn(|t| lerp([0.0, 0.0], [10.0, 5.0], t), 30, 0.0, 1.0, 0.0, 1);
        assert!(recognize(&tiny).is_none());
        let dot = vec![InputSample::new(5.0, 5.0, 0.5, 0.0); 40];
        assert!(recognize(&dot).is_none());
    }

    #[test]
    fn non_finite_input_is_none() {
        let mut s = drawn_ellipse([200.0, 200.0], 100.0, 100.0, 0.0, 1.05, 0.01, 1);
        s[10].x = f32::NAN;
        assert!(recognize(&s).is_none());
        s[10].x = f32::INFINITY;
        assert!(recognize(&s).is_none());
    }

    #[test]
    fn circle_is_a_circle_not_a_polygon() {
        for seed in 1..6 {
            let s = drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 1.08, 0.02, seed);
            assert!(
                matches!(recognize(&s), Some(Shape::Ellipse { .. })),
                "seed {seed}: {:?}",
                recognize(&s)
            );
        }
    }

    #[test]
    fn square_is_a_polygon_not_an_ellipse() {
        for seed in 1..6 {
            let sq = rect_corners([300.0, 300.0], 220.0, 220.0, 0.0);
            let got = corners_of(recognize(&drawn_polygon(&sq, 0.06, 0.02, seed)));
            assert_eq!(got.len(), 4, "seed {seed}");
        }
    }

    #[test]
    fn square_becomes_an_exact_square() {
        let sq = rect_corners([300.0, 300.0], 220.0, 220.0, 0.0);
        let got = corners_of(recognize(&drawn_polygon(&sq, 0.05, 0.02, 2)));
        assert!(corner_error(&got, &sq) < 20.0);
        let side = dist(got[0], got[1]);
        for i in 0..4 {
            assert!((dist(got[i], got[(i + 1) % 4]) - side).abs() < 1e-2);
            assert!(axis_offset_deg(got[i], got[(i + 1) % 4]) < 1e-2);
        }
    }

    #[test]
    fn rotated_rectangle_stays_rotated() {
        let rect = rect_corners([300.0, 300.0], 300.0, 150.0, 30f32.to_radians());
        let got = corners_of(recognize(&drawn_polygon(&rect, 0.05, 0.015, 3)));
        assert_eq!(got.len(), 4);
        assert!(corner_error(&got, &rect) < 25.0);
        let d = axis_offset_deg(got[0], got[1]);
        assert!(d > 20.0 && d < 40.0, "offset {d}");
        let a = sub_dir(got[0], got[1]);
        let b = sub_dir(got[1], got[2]);
        assert!((a[0] * b[0] + a[1] * b[1]).abs() < 1e-3);
    }

    fn sub_dir(a: [f32; 2], b: [f32; 2]) -> [f32; 2] {
        let d = [b[0] - a[0], b[1] - a[1]];
        let l = d[0].hypot(d[1]);
        [d[0] / l, d[1] / l]
    }

    #[test]
    fn slightly_tilted_rectangle_becomes_axis_aligned() {
        let rect = rect_corners([300.0, 300.0], 320.0, 160.0, 2f32.to_radians());
        let got = corners_of(recognize(&drawn_polygon(&rect, 0.04, 0.01, 4)));
        assert_eq!(got.len(), 4);
        for i in 0..4 {
            assert!(axis_offset_deg(got[i], got[(i + 1) % 4]) < 1e-2);
        }
    }

    #[test]
    fn trapezoid_gets_exactly_parallel_sides() {
        let trap = vec![
            [120.0, 120.0],
            [300.0, 120.0],
            [380.0, 300.0],
            [40.0, 300.0],
        ];
        let got = corners_of(recognize(&drawn_polygon(&trap, 0.05, 0.012, 5)));
        assert_eq!(got.len(), 4);
        assert!(corner_error(&got, &trap) < 25.0);
        let parallel = (0..2).any(|p| {
            let a = sub_dir(got[p], got[p + 1]);
            let b = sub_dir(got[p + 2], got[(p + 3) % 4]);
            (a[0] * b[1] - a[1] * b[0]).abs() < 1e-3
        });
        assert!(parallel);
    }

    #[test]
    fn triangle_is_recognised() {
        let tri = vec![[300.0, 80.0], [480.0, 380.0], [100.0, 360.0]];
        let got = corners_of(recognize(&drawn_polygon(&tri, 0.05, 0.015, 6)));
        assert_eq!(got.len(), 3);
        assert!(corner_error(&got, &tri) < 25.0);
    }

    #[test]
    fn pentagon_is_recognised() {
        let pent = regular_polygon([300.0, 300.0], 160.0, 5, -std::f32::consts::FRAC_PI_2);
        let got = corners_of(recognize(&drawn_polygon(&pent, 0.05, 0.012, 7)));
        assert_eq!(got.len(), 5);
        assert!(corner_error(&got, &pent) < 25.0);
    }

    #[test]
    fn handwriting_like_open_strokes_are_none() {
        let e = hand_drawn(
            |t| {
                let a = t * 5.2;
                [
                    200.0 + 40.0 * a.cos() * (1.0 - t * 0.3) + 90.0 * t,
                    200.0 - 30.0 * a.sin(),
                ]
            },
            90,
            0.0,
            1.0,
            0.0,
            1,
        );
        assert!(recognize(&e).is_none());
        let zig = polyline_stroke(&[
            [0.0, 0.0],
            [60.0, 80.0],
            [120.0, 0.0],
            [180.0, 80.0],
            [240.0, 0.0],
        ]);
        assert!(recognize(&zig).is_none());
    }

    fn polyline_stroke(pts: &[[f32; 2]]) -> Vec<InputSample> {
        let n = pts.len() - 1;
        hand_drawn(
            |t| {
                let f = (t * n as f32).min(n as f32 - 1e-4);
                let i = f as usize;
                lerp(pts[i], pts[i + 1], f - i as f32)
            },
            100,
            0.0,
            1.0,
            0.0,
            1,
        )
    }

    #[test]
    fn snap_uses_median_pressure_and_monotonic_times() {
        let mut s = drawn_ellipse([300.0, 300.0], 120.0, 120.0, 0.0, 1.05, 0.01, 1);
        for (i, p) in s.iter_mut().enumerate() {
            p.pressure = if i % 3 == 0 { 0.9 } else { 0.3 };
            p.time_ms = 1000.0 + i as f32 * 7.0;
        }
        let (shape, out) = snap(&s, 6.0).expect("snaps");
        assert!(matches!(shape, Shape::Ellipse { .. }));
        assert!(out.iter().all(|p| p.pressure == 0.3));
        assert_eq!(out[0].time_ms, 1000.0);
        assert!((out.last().unwrap().time_ms - s.last().unwrap().time_ms).abs() < 1e-2);
        assert!(out.windows(2).all(|w| w[1].time_ms >= w[0].time_ms));
    }

    #[test]
    fn custom_recognizer_can_be_registered() {
        struct Always;
        impl Recognizer for Always {
            fn fit(&self, _: &Stroke) -> Option<Candidate> {
                Some(Candidate {
                    shape: Shape::Line {
                        from: [0.0, 0.0],
                        to: [1.0, 1.0],
                    },
                    score: 0.0,
                })
            }
        }
        let s = drawn_ellipse([300.0, 300.0], 120.0, 120.0, 0.0, 1.05, 0.01, 1);
        let r = ShapeRecognizer::new().with(Always);
        assert!(matches!(r.recognize(&s), Some(Shape::Line { .. })));
        assert!(ShapeRecognizer::new().recognize(&s).is_none());
    }

    #[test]
    fn scribbles_never_panic() {
        let mut state = 12345u32;
        let mut next = || {
            state = state.wrapping_mul(1664525).wrapping_add(1013904223);
            (state >> 8) as f32 / (1u32 << 24) as f32
        };
        for n in [3usize, 4, 9, 40, 200] {
            for _ in 0..30 {
                let pts: Vec<InputSample> = (0..n)
                    .map(|i| InputSample::new(next() * 300.0, next() * 300.0, next(), i as f32))
                    .collect();
                let _ = snap(&pts, 4.0);
            }
        }
        let flat: Vec<InputSample> = (0..30)
            .map(|i| InputSample::new(i as f32 * 20.0, 5.0, 0.5, i as f32))
            .collect();
        assert!(matches!(recognize(&flat), Some(Shape::Line { .. })));
    }

    /// Corner count a recognised stroke should have, 0 for an ellipse.
    fn matches_kind(shape: &Option<Shape>, corners: usize) -> bool {
        match shape {
            Some(Shape::Ellipse { .. }) => corners == 0,
            Some(Shape::Polygon { corners: c }) => c.len() == corners,
            _ => false,
        }
    }

    #[test]
    fn common_shapes_survive_heavy_wobble() {
        let r = ShapeRecognizer::default();
        let tri = [[300.0, 80.0], [480.0, 380.0], [100.0, 360.0]];
        let mut bad = vec![];
        for wobble in [0.01f32, 0.02, 0.03, 0.04, 0.05] {
            for seed in 1..9u32 {
                let cases = [
                    (
                        "circle",
                        0,
                        drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 1.08, wobble, seed),
                    ),
                    (
                        "square",
                        4,
                        drawn_polygon(
                            &rect_corners([300.0, 300.0], 220.0, 220.0, 0.0),
                            0.05,
                            wobble,
                            seed,
                        ),
                    ),
                    (
                        "short circle",
                        0,
                        drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 0.92, wobble, seed),
                    ),
                    ("triangle", 3, drawn_polygon(&tri, 0.05, wobble, seed)),
                    (
                        "rect30",
                        4,
                        drawn_polygon(
                            &rect_corners([300.0, 300.0], 300.0, 150.0, 0.52),
                            0.05,
                            wobble,
                            seed,
                        ),
                    ),
                ];
                for (name, corners, stroke) in cases {
                    // Polygon sides this wobbly no longer read as straight.
                    if corners > 0 && wobble > 0.03 || name == "short circle" && wobble > 0.04 {
                        continue;
                    }
                    let got = r.recognize(&stroke);
                    if !matches_kind(&got, corners) {
                        bad.push(format!("{name} w={wobble} seed={seed}: {got:?}"));
                    }
                }
            }
        }
        assert!(bad.is_empty(), "{bad:#?}");
    }

    fn s_curve(wobble: f32, seed: u32) -> Vec<InputSample> {
        hand_drawn(
            |t| {
                [
                    40.0 + 260.0 * t,
                    200.0 + 70.0 * (t * std::f32::consts::TAU).sin(),
                ]
            },
            120,
            0.0,
            1.0,
            wobble,
            seed,
        )
    }

    #[test]
    fn a_gentle_curve_is_smoothed_not_left_alone() {
        for seed in 1..4 {
            let stroke = s_curve(0.01, seed);
            let got = recognize(&stroke);
            let Some(Shape::Curve { points }) = got else {
                panic!("seed {seed}: {got:?}");
            };
            let first = &stroke[0];
            assert!(dist(points[0], [first.x, first.y]) < 6.0);
            let (shape, samples) = snap(&stroke, 4.0).expect("snaps");
            assert!(matches!(shape, Shape::Curve { .. }));
            assert!(samples
                .windows(2)
                .all(|w| dist([w[0].x, w[0].y], [w[1].x, w[1].y]) <= 4.0 + 1e-3));
        }
    }

    #[test]
    fn smoothing_can_be_switched_off() {
        let stroke = s_curve(0.01, 1);
        let off = ShapeRecognizer::default().with_smoothing(false);
        assert!(off.recognize(&stroke).is_none());
        assert!(ShapeRecognizer::new().recognize(&stroke).is_none());
    }

    #[test]
    fn corners_loops_and_handwriting_are_still_none() {
        let cursive_e = hand_drawn(
            |t| {
                let a = t * 5.2;
                [
                    200.0 + 40.0 * a.cos() * (1.0 - t * 0.3) + 90.0 * t,
                    200.0 - 30.0 * a.sin(),
                ]
            },
            90,
            0.0,
            1.0,
            0.0,
            1,
        );
        let curl = hand_drawn(
            |t| {
                let a = t * 9.0;
                [360.0 * t + 30.0 * a.cos(), 100.0 + 40.0 * a.sin()]
            },
            150,
            0.0,
            1.0,
            0.0,
            1,
        );
        let cases: Vec<(&str, Vec<InputSample>)> = vec![
            ("e", cursive_e),
            ("curl", curl),
            (
                "zigzag",
                open_polyline(
                    &[
                        [0.0, 0.0],
                        [60.0, 80.0],
                        [120.0, 0.0],
                        [180.0, 80.0],
                        [240.0, 0.0],
                    ],
                    0.01,
                    1,
                ),
            ),
            (
                "check",
                open_polyline(&[[0.0, 100.0], [50.0, 160.0], [250.0, 0.0]], 0.01, 1),
            ),
            (
                "Z",
                open_polyline(
                    &[[0.0, 0.0], [200.0, 0.0], [0.0, 200.0], [200.0, 200.0]],
                    0.01,
                    1,
                ),
            ),
            (
                "seven",
                open_polyline(&[[0.0, 0.0], [160.0, 0.0], [60.0, 260.0]], 0.01, 1),
            ),
            (
                "L",
                open_polyline(&[[0.0, 0.0], [0.0, 200.0], [150.0, 200.0]], 0.01, 1),
            ),
        ];
        for (name, stroke) in cases {
            let got = recognize(&stroke);
            assert!(
                !matches!(
                    got,
                    Some(Shape::Curve { .. }) | Some(Shape::Arc { .. }) | Some(Shape::Line { .. })
                ),
                "{name}: {got:?}"
            );
        }
    }
}
