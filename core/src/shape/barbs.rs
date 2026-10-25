//! Measuring the barbs of an arrow head against the shaft.

use super::geom::{angle_between, cross, dist, sub, P};

const MIN_ANGLE_DEG: f32 = 12.0;
const MAX_ANGLE_DEG: f32 = 75.0;
/// Barb angles this close to the common 30 degrees are made exactly 30.
const SNAP_SPREAD_DEG: f32 = 30.0;
const SNAP_SPREAD_TOLERANCE_DEG: f32 = 10.0;
/// Shorter over longer barb below which two barbs do not belong together.
const MIN_LENGTH_RATIO: f32 = 0.5;

/// Barb size and angle shared by both barbs of a head.
#[derive(Clone, Copy, Debug)]
pub(crate) struct Barbs {
    pub head: f32,
    pub spread: f32,
}

/// Checks one or two barb ends against the tip and the unit vector
/// `reversed` that points back along the shaft. Each barb must be between
/// `min_len` and `max_len` long and leave the shaft at 12 to 75 degrees, and
/// two barbs must lie on opposite sides with similar lengths.
pub(crate) fn measure(
    tip: P,
    reversed: P,
    ends: &[P],
    min_len: f32,
    max_len: f32,
) -> Option<Barbs> {
    if ends.is_empty() || ends.len() > 2 {
        return None;
    }
    let mut lengths = Vec::with_capacity(2);
    let mut angles = Vec::with_capacity(2);
    let mut sides = Vec::with_capacity(2);
    for &e in ends {
        let v = sub(e, tip);
        let l = dist(e, tip);
        if !(min_len..=max_len).contains(&l) {
            return None;
        }
        let a = angle_between(reversed, v);
        if !(MIN_ANGLE_DEG.to_radians()..=MAX_ANGLE_DEG.to_radians()).contains(&a) {
            return None;
        }
        lengths.push(l);
        angles.push(a);
        sides.push(cross(reversed, v));
    }
    if ends.len() == 2 {
        if sides[0] * sides[1] >= 0.0 {
            return None;
        }
        if lengths[0].min(lengths[1]) < MIN_LENGTH_RATIO * lengths[0].max(lengths[1]) {
            return None;
        }
    }
    let n = ends.len() as f32;
    let head = lengths.iter().sum::<f32>() / n;
    let mut spread = angles.iter().sum::<f32>() / n;
    if (spread - SNAP_SPREAD_DEG.to_radians()).abs() < SNAP_SPREAD_TOLERANCE_DEG.to_radians() {
        spread = SNAP_SPREAD_DEG.to_radians();
    }
    Some(Barbs { head, spread })
}
