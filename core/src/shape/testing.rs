//! Synthetic hand-drawn strokes for tests.

use super::geom::{add, scale, sub, P};
use crate::InputSample;

pub(crate) use super::geom::{dist, lerp};

/// Smooth deterministic displacement in `[-1, 1]` that differs per seed.
fn wiggle(t: f32, seed: u32) -> f32 {
    let s = seed as f32;
    0.6 * (t * 6.3 * (3.1 + 0.37 * s) + s).sin()
        + 0.4 * (t * 6.3 * (7.3 + 0.21 * s) + 2.0 * s).sin()
}

/// Samples `f` for `t` in `[t0, t1]` at uneven spacing and displaces every
/// point by up to `wobble` times the stroke's rough size.
pub(crate) fn hand_drawn(
    f: impl Fn(f32) -> P,
    count: usize,
    t0: f32,
    t1: f32,
    wobble: f32,
    seed: u32,
) -> Vec<InputSample> {
    let raw: Vec<P> = (0..count)
        .map(|i| {
            let u = (i as f32 + 0.4 * (i as f32 * 1.7 + seed as f32).sin()) / (count - 1) as f32;
            f(t0 + (t1 - t0) * u.clamp(0.0, 1.0))
        })
        .collect();
    let (mut min, mut max) = (raw[0], raw[0]);
    for p in &raw {
        for k in 0..2 {
            min[k] = min[k].min(p[k]);
            max[k] = max[k].max(p[k]);
        }
    }
    let size = dist(min, max);
    raw.iter()
        .enumerate()
        .map(|(i, p)| {
            let u = i as f32 / (count - 1) as f32;
            let dx = wobble * size * wiggle(u, seed);
            let dy = wobble * size * wiggle(u, seed + 17);
            InputSample::new(p[0] + dx, p[1] + dy, 0.4 + 0.3 * u, i as f32 * 8.0)
        })
        .collect()
}

/// Walks the closed polygon through `corners`, parametrised in `[0, 1]` per
/// lap, and wraps around for values above 1.
pub(crate) fn polygon_point(corners: &[P], t: f32) -> P {
    let n = corners.len();
    let lens: Vec<f32> = (0..n)
        .map(|i| dist(corners[i], corners[(i + 1) % n]))
        .collect();
    let total: f32 = lens.iter().sum();
    let mut s = (t * total).rem_euclid(total);
    for i in 0..n {
        if s <= lens[i] {
            return lerp(corners[i], corners[(i + 1) % n], s / lens[i]);
        }
        s -= lens[i];
    }
    corners[0]
}

/// A closed polygon stroke that overshoots the start by `overshoot` laps.
pub(crate) fn drawn_polygon(
    corners: &[P],
    overshoot: f32,
    wobble: f32,
    seed: u32,
) -> Vec<InputSample> {
    hand_drawn(
        |t| polygon_point(corners, t),
        110,
        0.0,
        1.0 + overshoot,
        wobble,
        seed,
    )
}

pub(crate) fn drawn_ellipse(
    center: P,
    rx: f32,
    ry: f32,
    rotation: f32,
    laps: f32,
    wobble: f32,
    seed: u32,
) -> Vec<InputSample> {
    let (s, c) = rotation.sin_cos();
    hand_drawn(
        |t| {
            let a = t * std::f32::consts::TAU;
            let (x, y) = (rx * a.cos(), ry * a.sin());
            add(center, [x * c - y * s, x * s + y * c])
        },
        110,
        0.0,
        laps,
        wobble,
        seed,
    )
}

pub(crate) fn rect_corners(center: P, w: f32, h: f32, rotation: f32) -> Vec<P> {
    let (s, c) = rotation.sin_cos();
    [[-1.0, -1.0], [1.0, -1.0], [1.0, 1.0], [-1.0, 1.0]]
        .iter()
        .map(|k| {
            let (x, y) = (k[0] * w / 2.0, k[1] * h / 2.0);
            add(center, [x * c - y * s, x * s + y * c])
        })
        .collect()
}

pub(crate) fn regular_polygon(center: P, radius: f32, n: usize, start: f32) -> Vec<P> {
    (0..n)
        .map(|i| {
            let a = start + std::f32::consts::TAU * i as f32 / n as f32;
            add(center, scale([a.cos(), a.sin()], radius))
        })
        .collect()
}

/// Angle in degrees between the edge `a -> b` and the nearest axis.
pub(crate) fn axis_offset_deg(a: P, b: P) -> f32 {
    let d = sub(b, a);
    let deg = d[1].atan2(d[0]).to_degrees().rem_euclid(90.0);
    deg.min(90.0 - deg)
}

/// An open stroke along the polyline through `pts`, evenly spread by arc length.
pub(crate) fn open_polyline(pts: &[P], wobble: f32, seed: u32) -> Vec<InputSample> {
    let lens: Vec<f32> = pts.windows(2).map(|w| dist(w[0], w[1])).collect();
    let total: f32 = lens.iter().sum();
    hand_drawn(
        |t| {
            let mut s = (t * total).clamp(0.0, total);
            for (i, l) in lens.iter().enumerate() {
                if s <= *l || i == lens.len() - 1 {
                    return lerp(pts[i], pts[i + 1], (s / l).min(1.0));
                }
                s -= l;
            }
            pts[0]
        },
        120,
        0.0,
        1.0,
        wobble,
        seed,
    )
}

/// How the head of a test arrow is drawn after the shaft.
#[derive(Clone, Copy, Debug)]
pub(crate) enum HeadStyle {
    /// Tip to barb A, back to the tip, then barb B.
    BackAndForth,
    /// Tip to barb A, across to barb B, back to the tip.
    Triangle,
    /// Tip to barb A only.
    OneBarb,
    /// Tip to barb A, back through a rounded tip region to barb B.
    Through,
}

/// Vertices of an arrow from `from` with the given heading, shaft length,
/// barb length and spread.
pub(crate) fn arrow_vertices(
    from: P,
    heading: f32,
    shaft: f32,
    head: f32,
    spread: f32,
    style: HeadStyle,
) -> Vec<P> {
    let dir = [heading.cos(), heading.sin()];
    let tip = add(from, scale(dir, shaft));
    let back = scale(dir, -1.0);
    let barb = |a: f32| {
        add(
            tip,
            scale(crate::shape::geom::rotate_about(back, [0.0, 0.0], a), head),
        )
    };
    let (a, b) = (barb(spread), barb(-spread));
    match style {
        HeadStyle::BackAndForth => vec![from, tip, a, tip, b],
        HeadStyle::Triangle => vec![from, tip, a, b, tip],
        HeadStyle::OneBarb => vec![from, tip, a],
        HeadStyle::Through => {
            let c = add(tip, scale(back, head * 0.12));
            vec![from, tip, a, c, b]
        }
    }
}
