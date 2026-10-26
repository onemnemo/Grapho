//! Drawing guides: a ruler and a protractor that straighten the pen.
//!
//! A [`Guide`] lies on the page in whatever unit the caller works in (the
//! shells pass view pixels so wet ink needs no conversion), and every length
//! and angle on it is in that same unit and in radians. When a pen lands near
//! one of a guide's edges, [`nearest_edge`] returns the [`GuideEdge`] to draw
//! along, and [`StrokeBuilder`](crate::StrokeBuilder) projects every sample
//! onto it.

use std::f32::consts::PI;

#[cfg(test)]
mod tests;

/// Rotation snaps to whole degrees, and to a multiple of this when within
/// [`SNAP_NEAR_DEGREES`] of one.
const SNAP_STEP_DEGREES: f32 = 15.0;
const SNAP_NEAR_DEGREES: f32 = 2.0;
/// Longest turn between two points on a circle, so a fast pen still draws a
/// round arc instead of a chord.
const ARC_STEP: f32 = 1.5 * PI / 180.0;

/// A ruler or protractor placed on the page. Angles are radians in the page's
/// own axes (y down), so a positive angle turns clockwise on screen.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum Guide {
    /// A bar `length` long and `thickness` wide, whose long edges run along
    /// `angle`.
    Ruler {
        centre: [f32; 2],
        angle: f32,
        length: f32,
        thickness: f32,
    },
    /// A half disc of `radius`. Its straight base, through `centre`, runs
    /// along `angle`, and the disc lies on the side `angle - 90 degrees`.
    Protractor {
        centre: [f32; 2],
        angle: f32,
        radius: f32,
    },
}

/// Where a pen is drawing along: a straight line, or a circle.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum GuideEdge {
    /// The line through `origin` along the unit vector `dir`. `outward` is the
    /// unit normal pointing away from the guide's body, the side ink sits on.
    Line {
        origin: [f32; 2],
        dir: [f32; 2],
        outward: [f32; 2],
    },
    /// A circle around `centre`. Ink sits outside it.
    Circle { centre: [f32; 2], radius: f32 },
}

impl Guide {
    /// Floats per guide in the packed layout: a kind (0 ruler, 1 protractor),
    /// the centre x and y, the angle, then the length and thickness of a
    /// ruler or the radius and an unused zero of a protractor.
    pub const STRIDE: usize = 6;

    /// Reads guides packed as described at [`STRIDE`](Self::STRIDE). A guide
    /// of an unknown kind or with a size that is not positive is skipped, and
    /// a trailing partial guide is ignored.
    pub fn unpack(data: &[f32]) -> Vec<Guide> {
        let (chunks, _tail) = data.as_chunks::<{ Self::STRIDE }>();
        chunks
            .iter()
            .filter_map(|&[kind, x, y, angle, a, b]| match kind as i32 {
                0 if a > 0.0 && b > 0.0 => Some(Guide::Ruler {
                    centre: [x, y],
                    angle,
                    length: a,
                    thickness: b,
                }),
                1 if a > 0.0 => Some(Guide::Protractor {
                    centre: [x, y],
                    angle,
                    radius: a,
                }),
                _ => None,
            })
            .collect()
    }

    /// The closest edge of this guide to `(x, y)` within `reach`, with its
    /// distance. A protractor gives no edge to a pen within `reach` of its
    /// centre mark, because a ray drawn from there must stay free.
    pub fn nearest_edge(&self, x: f32, y: f32, reach: f32) -> Option<(GuideEdge, f32)> {
        match *self {
            Guide::Ruler {
                centre,
                angle,
                length,
                thickness,
            } => {
                let (dir, normal) = axes(angle);
                let (u, v) = local(centre, dir, normal, x, y);
                let along = (u.abs() - length / 2.0).max(0.0);
                let mut best: Option<(GuideEdge, f32)> = None;
                for side in [-1.0f32, 1.0] {
                    let d = along.hypot(v - side * thickness / 2.0);
                    if d <= reach && best.is_none_or(|(_, b)| d < b) {
                        let outward = [normal[0] * side, normal[1] * side];
                        let origin = [
                            centre[0] + outward[0] * thickness / 2.0,
                            centre[1] + outward[1] * thickness / 2.0,
                        ];
                        best = Some((
                            GuideEdge::Line {
                                origin,
                                dir,
                                outward,
                            },
                            d,
                        ));
                    }
                }
                best
            }
            Guide::Protractor {
                centre,
                angle,
                radius,
            } => {
                if (x - centre[0]).hypot(y - centre[1]) < reach {
                    return None;
                }
                let (dir, normal) = axes(angle);
                let (u, v) = local(centre, dir, normal, x, y);
                // The disc is on the negative side of the normal.
                let base = (u.abs() - radius).max(0.0).hypot(v);
                let arc = (u.hypot(v) - radius).abs();
                if arc <= reach && v <= reach && arc <= base {
                    return Some((GuideEdge::Circle { centre, radius }, arc));
                }
                (base <= reach).then_some((
                    GuideEdge::Line {
                        origin: centre,
                        dir,
                        outward: normal,
                    },
                    base,
                ))
            }
        }
    }

    /// True for a protractor when `(x, y)` is within `reach` of its centre
    /// mark.
    pub fn centre_near(&self, x: f32, y: f32, reach: f32) -> bool {
        match *self {
            Guide::Protractor { centre, .. } => (x - centre[0]).hypot(y - centre[1]) < reach,
            Guide::Ruler { .. } => false,
        }
    }

    /// For a protractor, the angle in degrees from its base to the line from
    /// its centre to `(x, y)`, from 0 to 180, counted from the end of the base
    /// that lies along `angle`. None for a ruler, and for a point on the
    /// centre.
    pub fn reading_degrees(&self, x: f32, y: f32) -> Option<f32> {
        let Guide::Protractor { centre, angle, .. } = *self else {
            return None;
        };
        let (dir, normal) = axes(angle);
        let (u, v) = local(centre, dir, normal, x, y);
        (u.hypot(v) > 1e-3).then(|| v.abs().atan2(u).to_degrees())
    }
}

impl GuideEdge {
    /// `(x, y)` moved onto the edge, pushed out by `half_width` so a stroke of
    /// that half width lies against the edge instead of under it.
    pub fn project(&self, x: f32, y: f32, half_width: f32) -> (f32, f32) {
        match *self {
            GuideEdge::Line {
                origin,
                dir,
                outward,
            } => {
                let t = (x - origin[0]) * dir[0] + (y - origin[1]) * dir[1];
                (
                    origin[0] + dir[0] * t + outward[0] * half_width,
                    origin[1] + dir[1] * t + outward[1] * half_width,
                )
            }
            GuideEdge::Circle { centre, radius } => {
                let (dx, dy) = (x - centre[0], y - centre[1]);
                let len = dx.hypot(dy);
                let (ux, uy) = if len > 1e-6 {
                    (dx / len, dy / len)
                } else {
                    (1.0, 0.0)
                };
                let r = radius + half_width;
                (centre[0] + ux * r, centre[1] + uy * r)
            }
        }
    }

    /// How many segments to draw between two points already on the edge: one
    /// for a line, and enough for a round arc on a circle.
    pub fn segments(&self, a: (f32, f32), b: (f32, f32)) -> usize {
        match *self {
            GuideEdge::Line { .. } => 1,
            GuideEdge::Circle { centre, .. } => {
                let turn = self.turn(centre, a, b).abs();
                (turn / ARC_STEP).ceil().max(1.0) as usize
            }
        }
    }

    /// The point a fraction `t` of the way from `a` to `b` along the edge,
    /// for two points already on it. On a circle that is the short way round.
    pub fn between(&self, a: (f32, f32), b: (f32, f32), t: f32) -> (f32, f32) {
        match *self {
            GuideEdge::Line { .. } => (a.0 + (b.0 - a.0) * t, a.1 + (b.1 - a.1) * t),
            GuideEdge::Circle { centre, .. } => {
                let turn = self.turn(centre, a, b) * t;
                let (dx, dy) = (a.0 - centre[0], a.1 - centre[1]);
                let (s, c) = turn.sin_cos();
                (centre[0] + dx * c - dy * s, centre[1] + dx * s + dy * c)
            }
        }
    }

    // Signed short-way angle from a to b around the centre.
    fn turn(&self, centre: [f32; 2], a: (f32, f32), b: (f32, f32)) -> f32 {
        let (ax, ay) = (a.0 - centre[0], a.1 - centre[1]);
        let (bx, by) = (b.0 - centre[0], b.1 - centre[1]);
        (ax * by - ay * bx).atan2(ax * bx + ay * by)
    }
}

/// The nearest edge among `guides` to `(x, y)` within `reach`.
pub fn nearest_edge(guides: &[Guide], x: f32, y: f32, reach: f32) -> Option<GuideEdge> {
    guides
        .iter()
        .filter_map(|g| g.nearest_edge(x, y, reach))
        .min_by(|a, b| a.1.total_cmp(&b.1))
        .map(|(edge, _)| edge)
}

/// `radians` rounded to a whole degree, then pulled to the nearest multiple of
/// 15 degrees when within 2 degrees of it, so common angles are easy to hit
/// and any other whole degree is still reachable.
pub fn snap_angle(radians: f32) -> f32 {
    let whole = radians.to_degrees().round();
    let step = (whole / SNAP_STEP_DEGREES).round() * SNAP_STEP_DEGREES;
    let degrees = if (whole - step).abs() <= SNAP_NEAR_DEGREES {
        step
    } else {
        whole
    };
    degrees.to_radians()
}

/// The angle a ruler lying along `radians` makes with the horizontal, counted
/// anticlockwise on screen as on paper, in whole degrees from 0 up to 179.
pub fn ruler_degrees(radians: f32) -> f32 {
    let degrees = (-radians.to_degrees()).round().rem_euclid(180.0);
    if degrees >= 180.0 {
        0.0
    } else {
        degrees
    }
}

fn axes(angle: f32) -> ([f32; 2], [f32; 2]) {
    let (s, c) = angle.sin_cos();
    ([c, s], [-s, c])
}

// Coordinates of (x, y) along `dir` and `normal`, from `centre`.
fn local(centre: [f32; 2], dir: [f32; 2], normal: [f32; 2], x: f32, y: f32) -> (f32, f32) {
    let (dx, dy) = (x - centre[0], y - centre[1]);
    (dx * dir[0] + dy * dir[1], dx * normal[0] + dy * normal[1])
}
