//! Circular arc recogniser.

use super::geom::{add, dist, dot, len, scale, sub, Line, P};
use super::{Candidate, Recognizer, Shape, Stroke};
use std::f32::consts::{PI, TAU};

const MIN_SWEEP_DEG: f32 = 40.0;
const MAX_SWEEP_DEG: f32 = 320.0;
/// Mean radial error relative to the radius.
const MAX_MEAN_ERROR: f32 = 0.06;
/// Radial error that 90 percent of the points must stay under.
const MAX_P90_ERROR: f32 = 0.12;
/// Sagitta over chord below which the arc is really a straight line.
const MIN_SAGITTA_OF_CHORD: f32 = 0.08;
/// Total angle travelled over net sweep. More means the stroke doubles back, as an S does.
const MAX_TRAVEL_RATIO: f32 = 1.35;
/// The stroke is cut into this many runs, and each run's mean radial error
/// (signed) is limited. A wave or a corner leaves one side of the circle
/// systematically off, which random wobble does not.
const MISFIT_RUNS: usize = 4;
const MAX_RUN_MISFIT: f32 = 0.022;
const SEMICIRCLE_SNAP_DEG: f32 = 12.0;
const QUARTER_SNAP_DEG: f32 = 8.0;
/// Gauss-Newton steps when refitting the circle through the stroke's ends.
const REFIT_STEPS: usize = 12;
const MAX_REFIT_SWEEP_CHANGE_DEG: f32 = 45.0;
/// Arcs are scaled up slightly so lines, ellipses and polygons win ties.
const ARC_BIAS: f32 = 1.3;

/// Finds part of a circle in an open stroke.
#[derive(Clone, Copy, Debug, Default)]
pub struct ArcRecognizer;

impl Recognizer for ArcRecognizer {
    fn fit(&self, stroke: &Stroke) -> Option<Candidate> {
        let pts = &stroke.points;
        if stroke.closed {
            return None;
        }
        let (center, radius) = kasa(pts)?;
        if !radius.is_finite() || radius < 1.0 {
            return None;
        }
        let mut errors: Vec<f32> = pts
            .iter()
            .map(|&p| (dist(p, center) - radius).abs() / radius)
            .collect();
        let mean = errors.iter().sum::<f32>() / errors.len() as f32;
        errors.sort_by(f32::total_cmp);
        let p90 = errors[(errors.len() - 1) * 9 / 10];
        if mean > MAX_MEAN_ERROR || p90 > MAX_P90_ERROR {
            return None;
        }

        let run = pts.len().div_ceil(MISFIT_RUNS);
        let lopsided = pts.chunks(run).any(|c| {
            let signed: f32 = c.iter().map(|&p| (dist(p, center) - radius) / radius).sum();
            (signed / c.len() as f32).abs() > MAX_RUN_MISFIT
        });
        if lopsided {
            return None;
        }

        let (mut sweep, mut travel) = (0.0f32, 0.0f32);
        let mut prev: Option<f32> = None;
        for p in pts {
            let a = (p[1] - center[1]).atan2(p[0] - center[0]);
            if let Some(q) = prev {
                let d = (a - q + PI).rem_euclid(TAU) - PI;
                sweep += d;
                travel += d.abs();
            }
            prev = Some(a);
        }
        let abs_sweep = sweep.abs();
        if !(MIN_SWEEP_DEG.to_radians()..=MAX_SWEEP_DEG.to_radians()).contains(&abs_sweep)
            || travel > MAX_TRAVEL_RATIO * abs_sweep
        {
            return None;
        }

        let first = pts[0];
        let last = pts[pts.len() - 1];
        let chord = dist(first, last);
        let sagitta = Line::fit(&[first, last])
            .map(|l| pts.iter().map(|&p| l.distance(p)).fold(0.0, f32::max))
            .unwrap_or(0.0);
        if sagitta < MIN_SAGITTA_OF_CHORD * chord {
            return None;
        }

        // The arc keeps the stroke's ends: its circle is refitted through them.
        let t = through_ends(pts, first, last, center)?;
        let t = regularise(first, last, t, sweep);
        let (center, radius, start, fitted) = arc_through(first, last, t, sweep.signum());
        // The refit must still describe the arc that was drawn, not its other side.
        if (fitted - sweep).abs() > MAX_REFIT_SWEEP_CHANGE_DEG.to_radians() {
            return None;
        }
        let sweep = fitted;
        Some(Candidate {
            shape: Shape::Arc {
                center,
                radius,
                start,
                sweep,
            },
            score: ARC_BIAS * mean * radius / stroke.diagonal,
        })
    }
}

/// Algebraic least squares circle (Kasa), computed on centred coordinates.
fn kasa(pts: &[P]) -> Option<(P, f32)> {
    let n = pts.len() as f32;
    let m = [
        pts.iter().map(|p| p[0]).sum::<f32>() / n,
        pts.iter().map(|p| p[1]).sum::<f32>() / n,
    ];
    let (mut suu, mut svv, mut suv, mut suuu, mut svvv, mut suvv, mut svuu) =
        (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    for p in pts {
        let d = sub(*p, m);
        let (u, v) = (d[0], d[1]);
        suu += u * u;
        svv += v * v;
        suv += u * v;
        suuu += u * u * u;
        svvv += v * v * v;
        suvv += u * v * v;
        svuu += v * u * u;
    }
    let det = suu * svv - suv * suv;
    if det.abs() < 1e-6 {
        return None;
    }
    let (b1, b2) = (0.5 * (suuu + suvv), 0.5 * (svvv + svuu));
    let uc = (b1 * svv - b2 * suv) / det;
    let vc = (suu * b2 - suv * b1) / det;
    let radius = (uc * uc + vc * vc + (suu + svv) / n).sqrt();
    Some(([m[0] + uc, m[1] + vc], radius))
}

/// Where the centre of a circle through `first` and `last` sits: its signed
/// distance from the chord's midpoint along the chord's left normal.
fn chord_frame(first: P, last: P) -> (P, P, f32) {
    let mid = scale(add(first, last), 0.5);
    let d = sub(last, first);
    let half = 0.5 * len(d);
    let normal = scale([-d[1], d[0]], 0.5 / half.max(1e-6));
    (mid, normal, half)
}

/// Least squares circle constrained to pass through both ends, found by
/// Gauss-Newton along the chord's perpendicular bisector, starting from the
/// free fit's centre.
fn through_ends(pts: &[P], first: P, last: P, free_center: P) -> Option<f32> {
    let (mid, normal, half) = chord_frame(first, last);
    if half < 1.0 {
        return None;
    }
    let mut t = dot(sub(free_center, mid), normal);
    for _ in 0..REFIT_STEPS {
        let c = add(mid, scale(normal, t));
        let r = (half * half + t * t).sqrt();
        let (mut num, mut den) = (0.0f32, 0.0f32);
        for &p in pts {
            let v = sub(p, c);
            let d = len(v).max(1e-6);
            let residual = d - r;
            let slope = -dot(v, normal) / d - t / r;
            num += residual * slope;
            den += slope * slope;
        }
        if den < 1e-9 {
            break;
        }
        let step = num / den;
        t -= step;
        if step.abs() < 1e-3 {
            break;
        }
    }
    t.is_finite().then_some(t)
}

/// The circle through both ends with its centre `t` along the normal, and the
/// arc on it from `first` to `last` turning the way the pen did.
fn arc_through(first: P, last: P, t: f32, direction: f32) -> (P, f32, f32, f32) {
    let (mid, normal, half) = chord_frame(first, last);
    let center = add(mid, scale(normal, t));
    let radius = (half * half + t * t).sqrt();
    let start = angle(sub(first, center));
    let end = angle(sub(last, center));
    let sweep = direction * (direction * (end - start)).rem_euclid(TAU);
    (center, radius, start, sweep)
}

/// Moves the centre so the sweep is exactly 180 or 90 degrees when it is
/// close, keeping both ends where they are.
fn regularise(first: P, last: P, t: f32, sweep: f32) -> f32 {
    let (.., half) = chord_frame(first, last);
    let deg = arc_through(first, last, t, sweep.signum())
        .3
        .abs()
        .to_degrees();
    if (deg - 180.0).abs() < SEMICIRCLE_SNAP_DEG {
        return 0.0;
    }
    if (deg - 90.0).abs() < QUARTER_SNAP_DEG {
        // A quarter turn puts the centre half a chord from the midpoint.
        return half.copysign(t);
    }
    t
}

fn angle(v: P) -> f32 {
    v[1].atan2(v[0])
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::shape::testing::*;
    use crate::shape::ShapeRecognizer;
    use crate::InputSample;
    use std::f32::consts::FRAC_PI_2;

    fn arc_stroke(heading: f32, sweep_deg: f32, wobble: f32, seed: u32) -> Vec<InputSample> {
        let sweep = sweep_deg.to_radians();
        hand_drawn(
            |t| {
                let a = heading + sweep * t;
                [300.0 + 140.0 * a.cos(), 300.0 + 140.0 * a.sin()]
            },
            90,
            0.0,
            1.0,
            wobble,
            seed,
        )
    }

    fn recognise(s: &[InputSample]) -> Option<Shape> {
        ShapeRecognizer::default().recognize(s)
    }

    #[test]
    fn half_circles_in_four_orientations() {
        for (i, heading) in [0.0f32, 90.0, 180.0, 270.0].iter().enumerate() {
            for sign in [1.0f32, -1.0] {
                let s = arc_stroke(heading.to_radians(), sign * 180.0, 0.02, i as u32 + 1);
                let got = recognise(&s);
                let Some(Shape::Arc { sweep, radius, .. }) = got else {
                    panic!("heading {heading} sign {sign}: {got:?}");
                };
                assert!((sweep - sign * PI).abs() < 1e-5);
                assert!((radius - 140.0).abs() < 25.0);
            }
        }
    }

    #[test]
    fn the_arc_starts_and_ends_where_the_pen_did() {
        for (deg, wobble, seed) in [
            (180.0f32, 0.02f32, 2u32),
            (120.0, 0.03, 3),
            (-90.0, 0.02, 4),
            (200.0, 0.02, 5),
        ] {
            let s = arc_stroke(0.07 + seed as f32, deg, wobble, seed);
            let (a, b) = (&s[0], s.last().unwrap());
            let Some(arc @ Shape::Arc { .. }) = recognise(&s) else {
                panic!("{deg}: expected an arc");
            };
            let path = arc.path(4.0);
            assert!(
                dist(path[0], [a.x, a.y]) < 0.5,
                "{deg}: start {:?}",
                path[0]
            );
            assert!(dist(*path.last().unwrap(), [b.x, b.y]) < 0.5, "{deg}: end");
        }
    }

    #[test]
    fn quarter_and_120_degree_arcs() {
        let cases = [
            (90.0f32, 1u32, 0.03f32),
            (-90.0, 2, 0.02),
            (120.0, 3, 0.04),
            (120.0, 4, 0.03),
            (-120.0, 5, 0.04),
            (120.0, 6, 0.02),
        ];
        for (deg, seed, wobble) in cases {
            {
                let s = arc_stroke(0.3 + seed as f32, deg, wobble, seed);
                let got = recognise(&s);
                let Some(Shape::Arc { sweep, .. }) = got else {
                    panic!("{deg} w={wobble} seed={seed}: {got:?}");
                };
                assert_eq!(sweep.signum(), deg.signum());
                if deg.abs() == 90.0 {
                    assert!((sweep.abs() - FRAC_PI_2).abs() < 1e-5);
                } else {
                    assert!((sweep.abs().to_degrees() - 120.0).abs() < 20.0);
                }
            }
        }
    }

    #[test]
    fn near_straight_stroke_stays_a_line() {
        let s = hand_drawn(
            |t| [40.0 + 300.0 * t, 100.0 + 12.0 * (t * PI).sin()],
            80,
            0.0,
            1.0,
            0.01,
            2,
        );
        assert!(matches!(recognise(&s), Some(Shape::Line { .. })));
    }

    #[test]
    fn full_circle_stays_an_ellipse() {
        let s = drawn_ellipse([300.0, 300.0], 140.0, 140.0, 0.0, 1.08, 0.02, 3);
        assert!(matches!(recognise(&s), Some(Shape::Ellipse { .. })));
    }

    #[test]
    fn s_curve_checkmark_and_seven_are_not_arcs() {
        let s_curve = hand_drawn(
            |t| [40.0 + 100.0 * t, 100.0 + 120.0 * (t * TAU).sin()],
            120,
            0.0,
            1.0,
            0.0,
            1,
        );
        let check = open_polyline(&[[0.0, 100.0], [50.0, 160.0], [250.0, 0.0]], 0.01, 1);
        let seven = open_polyline(&[[0.0, 0.0], [160.0, 0.0], [60.0, 260.0]], 0.01, 1);
        for (name, s) in [("S", s_curve), ("check", check), ("seven", seven)] {
            let got = Stroke::new(&s).and_then(|st| ArcRecognizer.fit(&st));
            assert!(got.is_none(), "{name}: {got:?}");
        }
    }

    #[test]
    fn arc_path_follows_the_drawing_direction() {
        let arc = Shape::Arc {
            center: [0.0, 0.0],
            radius: 50.0,
            start: 0.0,
            sweep: -PI,
        };
        let p = arc.path(6.0);
        assert!(p[1][1] < 0.0);
        assert!(p.windows(2).all(|w| dist(w[0], w[1]) <= 6.0 + 1e-3));
        assert!(dist(*p.last().unwrap(), [-50.0, 0.0]) < 1e-3);
    }
}
