use crate::{Brush, InputSample};

/// Turns a polyline of samples into triangles, as packed `[x, y]` vertex
/// pairs with three vertices per triangle.
///
/// Each sample becomes a disc, and each pair of neighbours is joined by a
/// quad whose ends match the two disc radii. Overlaps are fine for opaque
/// ink.
pub fn tessellate(points: &[InputSample], brush: &Brush, out: &mut Vec<f32>) {
    let mut prev = None;
    for p in points {
        tessellate_step(prev, p, brush, out);
        prev = Some(p);
    }
}

/// Appends the triangles that point `p` adds after `prev`: a disc at `p`
/// and the quad joining it to `prev`. Lets a stroke grow without
/// re-tessellating what is already drawn.
pub fn tessellate_step(
    prev: Option<&InputSample>,
    p: &InputSample,
    brush: &Brush,
    out: &mut Vec<f32>,
) {
    let r = brush.width_at(p.pressure) * 0.5;
    disc(p.x, p.y, r, SCREEN_TOLERANCE, out);
    if let Some(q) = prev {
        let qr = brush.width_at(q.pressure) * 0.5;
        segment(q.x, q.y, qr, p.x, p.y, r, out);
    }
}

/// Wet ink is tessellated in view pixels: keep edges within a quarter pixel.
const SCREEN_TOLERANCE: f32 = 0.25;

/// Number of sides for a circle of radius `r` whose edge stays within
/// `tolerance` of the true circle.
pub(crate) fn disc_sides(r: f32, tolerance: f32) -> usize {
    let r = r.max(tolerance * 2.0);
    ((std::f32::consts::PI / (1.0 - tolerance / r).clamp(-1.0, 1.0).acos()).ceil() as usize)
        .clamp(6, 32)
}

fn disc(cx: f32, cy: f32, r: f32, tolerance: f32, out: &mut Vec<f32>) {
    let n = disc_sides(r, tolerance);
    let step = std::f32::consts::TAU / n as f32;
    for i in 0..n {
        let a0 = i as f32 * step;
        let a1 = a0 + step;
        out.extend_from_slice(&[
            cx,
            cy,
            cx + r * a0.cos(),
            cy + r * a0.sin(),
            cx + r * a1.cos(),
            cy + r * a1.sin(),
        ]);
    }
}

fn segment(x0: f32, y0: f32, r0: f32, x1: f32, y1: f32, r1: f32, out: &mut Vec<f32>) {
    let (dx, dy) = (x1 - x0, y1 - y0);
    let len = (dx * dx + dy * dy).sqrt();
    if len < 1e-3 {
        return;
    }
    let (nx, ny) = (-dy / len, dx / len);
    let (ax, ay) = (x0 + nx * r0, y0 + ny * r0);
    let (bx, by) = (x0 - nx * r0, y0 - ny * r0);
    let (cx, cy) = (x1 + nx * r1, y1 + ny * r1);
    let (ex, ey) = (x1 - nx * r1, y1 - ny * r1);
    out.extend_from_slice(&[ax, ay, bx, by, cx, cy, bx, by, ex, ey, cx, cy]);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn output_is_whole_triangles() {
        let pts = [
            InputSample::new(0.0, 0.0, 0.5, 0.0),
            InputSample::new(10.0, 0.0, 0.5, 4.0),
            InputSample::new(20.0, 5.0, 0.9, 8.0),
        ];
        let mut out = Vec::new();
        tessellate(&pts, &Brush::default(), &mut out);
        assert!(!out.is_empty());
        assert_eq!(out.len() % 6, 0);
    }

    #[test]
    fn vertices_stay_within_stroke_bounds() {
        let brush = Brush::with_width(8.0);
        let pts = [
            InputSample::new(0.0, 0.0, 1.0, 0.0),
            InputSample::new(50.0, 0.0, 1.0, 4.0),
        ];
        let mut out = Vec::new();
        tessellate(&pts, &brush, &mut out);
        for v in out.as_chunks::<2>().0 {
            assert!(v[0] >= -4.01 && v[0] <= 54.01, "x out of range: {}", v[0]);
            assert!(v[1].abs() <= 4.01, "y out of range: {}", v[1]);
        }
    }

    #[test]
    fn single_point_draws_a_dot() {
        let mut out = Vec::new();
        tessellate(
            &[InputSample::new(5.0, 5.0, 0.5, 0.0)],
            &Brush::default(),
            &mut out,
        );
        assert!(out.len() >= 6 * 6);
    }
}
