//! Cleans up fitted polygons: right angles, parallel sides and axis alignment.

use super::geom::{
    add, angle_between, axis_angle_between, centroid, dot, len, rotate_about, scale, sub, unit, P,
};
use std::f32::consts::{FRAC_PI_2, FRAC_PI_4};

/// Hand-drawn rectangles lean and splay, so every corner may be this far
/// off a right angle as long as the corners average out close to one.
const RIGHT_ANGLE_DEG: f32 = 25.0;
const MEAN_RIGHT_ANGLE_DEG: f32 = 20.0;
const PARALLEL_DEG: f32 = 8.0;
/// Shorter over longer side above which a rectangle is a square, or a
/// parallelogram a rhombus.
const EQUAL_SIDES: f32 = 0.85;
const AXIS_SNAP_DEG: f32 = 4.0;

/// Regularises fitted corners. Quadrilaterals become rectangles, squares,
/// parallelograms, rhombuses or trapezoids when they are close to one, and
/// any polygon within a few degrees of axis aligned is made exactly so.
pub(crate) fn regularise(corners: Vec<P>) -> Vec<P> {
    let corners = if corners.len() == 4 {
        quad(&corners)
    } else {
        corners
    };
    align_to_axes(corners)
}

fn edges(c: &[P]) -> [P; 4] {
    std::array::from_fn(|i| sub(c[(i + 1) % 4], c[i]))
}

fn quad(c: &[P]) -> Vec<P> {
    let e = edges(c);
    let off: [f32; 4] = std::array::from_fn(|i| {
        let a = angle_between(sub(c[(i + 3) % 4], c[i]), sub(c[(i + 1) % 4], c[i]));
        (a - FRAC_PI_2).abs()
    });
    let right = off.iter().all(|&o| o < RIGHT_ANGLE_DEG.to_radians())
        && off.iter().sum::<f32>() / 4.0 < MEAN_RIGHT_ANGLE_DEG.to_radians();
    if right {
        return rectangle(c, &e);
    }
    let tol = PARALLEL_DEG.to_radians();
    let par02 = axis_angle_between(e[0], e[2]);
    let par13 = axis_angle_between(e[1], e[3]);
    if par02 < tol && par13 < tol {
        parallelogram(c, &e)
    } else if par02 < tol || par13 < tol {
        trapezoid(c, &e, usize::from(par13 < par02))
    } else {
        c.to_vec()
    }
}

fn rectangle(c: &[P], e: &[P; 4]) -> Vec<P> {
    let u0 = unit(sub(e[0], e[2]));
    let v0 = unit(sub(e[1], e[3]));
    let mut perp = [v0[1], -v0[0]];
    if dot(perp, u0) < 0.0 {
        perp = scale(perp, -1.0);
    }
    let u = unit(add(u0, perp));
    let v = [-u[1], u[0]];
    let (a, b) = if dot(e[0], u).abs() >= dot(e[0], v).abs() {
        (u, v)
    } else {
        (v, u)
    };
    let mut la = (len(e[0]) + len(e[2])) / 2.0;
    let mut lb = (len(e[1]) + len(e[3])) / 2.0;
    if la.min(lb) / la.max(lb) > EQUAL_SIDES {
        la = (la + lb) / 2.0;
        lb = la;
    }
    let da = scale(a, la * dot(e[0], a).signum());
    let db = scale(b, lb * dot(e[1], b).signum());
    let c0 = sub(centroid(c), scale(add(da, db), 0.5));
    let c1 = add(c0, da);
    vec![c0, c1, add(c1, db), add(c0, db)]
}

fn parallelogram(c: &[P], e: &[P; 4]) -> Vec<P> {
    let mut a = scale(sub(e[0], e[2]), 0.5);
    let mut b = scale(sub(e[1], e[3]), 0.5);
    let (la, lb) = (len(a), len(b));
    if la.min(lb) / la.max(lb) > EQUAL_SIDES {
        let mean = (la + lb) / 2.0;
        a = scale(unit(a), mean);
        b = scale(unit(b), mean);
    }
    let c0 = sub(centroid(c), scale(add(a, b), 0.5));
    let c1 = add(c0, a);
    let c2 = add(c1, b);
    vec![c0, c1, c2, sub(c2, a)]
}

/// Makes sides `p` and `p + 2` exactly parallel by turning each about its midpoint.
fn trapezoid(c: &[P], e: &[P; 4], p: usize) -> Vec<P> {
    let d = unit(sub(e[p], e[p + 2]));
    let mut out = c.to_vec();
    for (s, dir) in [(p, d), (p + 2, scale(d, -1.0))] {
        let a = c[s];
        let b = c[(s + 1) % 4];
        let mid = scale(add(a, b), 0.5);
        let half = scale(dir, len(sub(b, a)) / 2.0);
        out[s] = sub(mid, half);
        out[(s + 1) % 4] = add(mid, half);
    }
    out
}

/// Rotation in `(-pi/4, pi/4]` that would put the edge on the nearest axis.
fn axis_offset(d: P) -> f32 {
    let m = d[1].atan2(d[0]).rem_euclid(FRAC_PI_2);
    if m > FRAC_PI_4 {
        m - FRAC_PI_2
    } else {
        m
    }
}

fn align_to_axes(corners: Vec<P>) -> Vec<P> {
    let n = corners.len();
    let offset = (0..n)
        .map(|i| axis_offset(sub(corners[(i + 1) % n], corners[i])))
        .min_by(|a, b| a.abs().total_cmp(&b.abs()))
        .unwrap_or(0.0);
    if offset.abs() > AXIS_SNAP_DEG.to_radians() {
        return corners;
    }
    let c = centroid(&corners);
    corners
        .into_iter()
        .map(|p| rotate_about(p, c, -offset))
        .collect()
}
