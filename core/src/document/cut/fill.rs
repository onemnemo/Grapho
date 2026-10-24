//! Cutting a fill where an eraser passes over it.
//!
//! The eraser's swept shape is subtracted from the fill polygon. Each shape
//! left over becomes a fill of its own, with any holes joined to the outer
//! boundary by a slit so that it still fits in one polygon.

use super::keyhole::bridge_holes;
use super::{Cut, Stroke};
use crate::InputSample;
use i_overlay::core::fill_rule::FillRule;
use i_overlay::core::overlay_rule::OverlayRule;
use i_overlay::float::single::SingleFloatOverlay;

/// How far (in points) the swept shape's edge may stray from a true circle.
const EDGE_TOLERANCE: f32 = 0.05;

/// Pieces covering less than this (in square points) are too small to draw
/// and are dropped.
const MIN_AREA: f32 = 0.5;

/// The least area (in square points) a sweep must take off a fill to count
/// as cutting it. Cut edges lie on the sweep's own edge, so without this an
/// eraser held still would cut the same fill again and again.
const MIN_BITE_AREA: f32 = 0.02;

type Contour = Vec<[f32; 2]>;
type Shape = Vec<Contour>;

/// Cuts the fill `stroke` where a circle of radius `r` moving from `(x0, y0)`
/// to `(x1, y1)` covers it.
pub(super) fn cut_fill(stroke: &Stroke, x0: f32, y0: f32, x1: f32, y1: f32, r: f32) -> Cut {
    let polygon: Contour = stroke.points.iter().map(|p| [p.x, p.y]).collect();
    if polygon.len() < 3 {
        return Cut::Miss;
    }
    let subject = vec![polygon];
    let sweep = vec![capsule(x0, y0, x1, y1, r)];
    let before: Vec<Shape> = subject.overlay(
        &Vec::<Contour>::new(),
        OverlayRule::Union,
        FillRule::NonZero,
    );
    let after: Vec<Shape> = subject.overlay(&sweep, OverlayRule::Difference, FillRule::NonZero);
    let area = |shapes: &[Shape]| shapes.iter().map(shape_area).sum::<f32>();
    if area(&before) - area(&after) < MIN_BITE_AREA {
        return Cut::Miss;
    }
    let pieces = after
        .into_iter()
        .filter(|s| shape_area(s) >= MIN_AREA)
        .map(|s| {
            let ring = bridge_holes(s);
            let points = ring
                .into_iter()
                .map(|[x, y]| InputSample::new(x, y, 1.0, 0.0))
                .collect();
            Stroke::new(stroke.style, stroke.started_at_ms, points)
        })
        .collect();
    Cut::Pieces(pieces)
}

/// The area of a shape: its outer boundary less its holes.
fn shape_area(shape: &Shape) -> f32 {
    shape
        .iter()
        .enumerate()
        .map(|(k, c)| {
            if k == 0 {
                area(c).abs()
            } else {
                -area(c).abs()
            }
        })
        .sum()
}

pub(super) fn area(v: &[[f32; 2]]) -> f32 {
    let mut sum = 0.0;
    for i in 0..v.len() {
        let (a, b) = (v[i], v[(i + 1) % v.len()]);
        sum += a[0] * b[1] - b[0] * a[1];
    }
    sum * 0.5
}

/// A polygon for a circle of radius `r` swept from one point to another.
fn capsule(x0: f32, y0: f32, x1: f32, y1: f32, r: f32) -> Contour {
    let r = r.max(EDGE_TOLERANCE);
    // The corners sit on a slightly larger circle so the flat sides, which
    // fall inside it, stray as much outward as inward.
    let rc = r + EDGE_TOLERANCE * 0.5;
    let half_turn = (std::f32::consts::PI / (1.0 - EDGE_TOLERANCE / rc).clamp(-1.0, 1.0).acos())
        .ceil()
        .clamp(4.0, 128.0) as usize;
    let heading = (y1 - y0).atan2(x1 - x0);
    let mut out = Vec::with_capacity(half_turn * 2 + 2);
    for (cx, cy, from) in [
        (x1, y1, heading - std::f32::consts::FRAC_PI_2),
        (x0, y0, heading + std::f32::consts::FRAC_PI_2),
    ] {
        for k in 0..=half_turn {
            let a = from + std::f32::consts::PI * k as f32 / half_turn as f32;
            out.push([cx + rc * a.cos(), cy + rc * a.sin()]);
        }
    }
    out
}
