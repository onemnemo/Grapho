//! Small 2D vector helpers shared by the recognisers.

pub(crate) type P = [f32; 2];

pub(crate) fn sub(a: P, b: P) -> P {
    [a[0] - b[0], a[1] - b[1]]
}

pub(crate) fn add(a: P, b: P) -> P {
    [a[0] + b[0], a[1] + b[1]]
}

pub(crate) fn scale(a: P, k: f32) -> P {
    [a[0] * k, a[1] * k]
}

pub(crate) fn dot(a: P, b: P) -> f32 {
    a[0] * b[0] + a[1] * b[1]
}

pub(crate) fn cross(a: P, b: P) -> f32 {
    a[0] * b[1] - a[1] * b[0]
}

pub(crate) fn len(a: P) -> f32 {
    a[0].hypot(a[1])
}

pub(crate) fn dist(a: P, b: P) -> f32 {
    len(sub(a, b))
}

pub(crate) fn lerp(a: P, b: P, t: f32) -> P {
    [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]
}

/// Unit vector, or `[1, 0]` for a zero-length input.
pub(crate) fn unit(a: P) -> P {
    let l = len(a);
    if l > 1e-9 {
        scale(a, 1.0 / l)
    } else {
        [1.0, 0.0]
    }
}

pub(crate) fn centroid(points: &[P]) -> P {
    let n = points.len().max(1) as f32;
    let sum = points.iter().fold([0.0, 0.0], |acc, &p| add(acc, p));
    scale(sum, 1.0 / n)
}

/// Rotates `p` by `angle` radians around `center`.
pub(crate) fn rotate_about(p: P, center: P, angle: f32) -> P {
    let (s, c) = angle.sin_cos();
    let d = sub(p, center);
    [
        center[0] + d[0] * c - d[1] * s,
        center[1] + d[0] * s + d[1] * c,
    ]
}

pub(crate) fn dist_to_segment(p: P, a: P, b: P) -> f32 {
    let ab = sub(b, a);
    let l2 = dot(ab, ab);
    if l2 < 1e-12 {
        return dist(p, a);
    }
    let t = (dot(sub(p, a), ab) / l2).clamp(0.0, 1.0);
    dist(p, lerp(a, b, t))
}

/// Distance from `p` to the nearest edge of the closed polygon.
pub(crate) fn dist_to_polygon(p: P, corners: &[P]) -> f32 {
    let n = corners.len();
    (0..n)
        .map(|i| dist_to_segment(p, corners[i], corners[(i + 1) % n]))
        .fold(f32::INFINITY, f32::min)
}

/// Smallest angle between two directions ignoring sign, in `[0, pi/2]`.
pub(crate) fn axis_angle_between(a: P, b: P) -> f32 {
    let c = (dot(a, b) / (len(a) * len(b)).max(1e-12)).abs().min(1.0);
    c.acos()
}

/// Angle between two vectors, in `[0, pi]`.
pub(crate) fn angle_between(a: P, b: P) -> f32 {
    let c = (dot(a, b) / (len(a) * len(b)).max(1e-12)).clamp(-1.0, 1.0);
    c.acos()
}

/// A line through `origin` with unit direction `dir`.
#[derive(Clone, Copy, Debug)]
pub(crate) struct Line {
    pub origin: P,
    pub dir: P,
}

impl Line {
    /// Total least squares fit. Returns None for fewer than two points.
    pub fn fit(points: &[P]) -> Option<Line> {
        if points.len() < 2 {
            return None;
        }
        let c = centroid(points);
        let (mut sxx, mut syy, mut sxy) = (0.0, 0.0, 0.0);
        for p in points {
            let d = sub(*p, c);
            sxx += d[0] * d[0];
            syy += d[1] * d[1];
            sxy += d[0] * d[1];
        }
        let angle = 0.5 * (2.0 * sxy).atan2(sxx - syy);
        Some(Line {
            origin: c,
            dir: [angle.cos(), angle.sin()],
        })
    }

    pub fn distance(&self, p: P) -> f32 {
        cross(self.dir, sub(p, self.origin)).abs()
    }

    /// Intersection point, or None when the lines are nearly parallel
    /// (under `min_sin` of angle between them).
    pub fn intersect(&self, other: &Line, min_sin: f32) -> Option<P> {
        let denom = cross(self.dir, other.dir);
        if denom.abs() < min_sin {
            return None;
        }
        let t = cross(sub(other.origin, self.origin), other.dir) / denom;
        Some(add(self.origin, scale(self.dir, t)))
    }
}

/// True when segments `a1a2` and `b1b2` cross in their interiors.
pub(crate) fn segments_cross(a1: P, a2: P, b1: P, b2: P) -> bool {
    let d1 = cross(sub(a2, a1), sub(b1, a1));
    let d2 = cross(sub(a2, a1), sub(b2, a1));
    let d3 = cross(sub(b2, b1), sub(a1, b1));
    let d4 = cross(sub(b2, b1), sub(a2, b1));
    d1 * d2 < 0.0 && d3 * d4 < 0.0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fits_a_noisy_diagonal() {
        let pts: Vec<P> = (0..20)
            .map(|i| [i as f32, i as f32 + if i % 2 == 0 { 0.2 } else { -0.2 }])
            .collect();
        let line = Line::fit(&pts).unwrap();
        assert!((line.dir[0].abs() - line.dir[1].abs()).abs() < 0.01);
    }

    #[test]
    fn intersects_perpendicular_lines() {
        let a = Line {
            origin: [0.0, 5.0],
            dir: [1.0, 0.0],
        };
        let b = Line {
            origin: [3.0, 0.0],
            dir: [0.0, 1.0],
        };
        let p = a.intersect(&b, 0.1).unwrap();
        assert!(dist(p, [3.0, 5.0]) < 1e-5);
        assert!(a.intersect(&a, 0.1).is_none());
    }

    #[test]
    fn segment_distance_clamps_to_ends() {
        assert!((dist_to_segment([5.0, 3.0], [0.0, 0.0], [2.0, 0.0]) - 18f32.sqrt()).abs() < 1e-5);
        assert!((dist_to_segment([1.0, 3.0], [0.0, 0.0], [2.0, 0.0]) - 3.0).abs() < 1e-5);
    }

    #[test]
    fn crossing_segments_are_detected() {
        assert!(segments_cross(
            [0.0, 0.0],
            [2.0, 2.0],
            [0.0, 2.0],
            [2.0, 0.0]
        ));
        assert!(!segments_cross(
            [0.0, 0.0],
            [1.0, 0.0],
            [0.0, 1.0],
            [1.0, 1.0]
        ));
    }
}
