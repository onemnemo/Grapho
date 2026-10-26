//! Small synthetic pictures for tests.

use super::Pixels;

/// A picture of one flat colour.
pub(crate) fn flat(w: u32, h: u32, rgb: [u8; 3]) -> Pixels {
    let mut rgba = Vec::with_capacity((w * h * 4) as usize);
    for _ in 0..w * h {
        rgba.extend_from_slice(&[rgb[0], rgb[1], rgb[2], 255]);
    }
    Pixels {
        width: w,
        height: h,
        rgba,
    }
}

/// Paints the pixels `[x0, x1) x [y0, y1)`.
pub(crate) fn paint(p: &mut Pixels, (x0, y0, x1, y1): (u32, u32, u32, u32), rgb: [u8; 3]) {
    for y in y0..y1.min(p.height) {
        for x in x0..x1.min(p.width) {
            let o = ((y * p.width + x) * 4) as usize;
            p.rgba[o..o + 4].copy_from_slice(&[rgb[0], rgb[1], rgb[2], 255]);
        }
    }
}

/// An outline of the rectangle `[x0, x1] x [y0, y1]`, `t` pixels thick,
/// with its centre line on the given edges.
pub(crate) fn outline(
    p: &mut Pixels,
    (x0, y0, x1, y1): (u32, u32, u32, u32),
    t: u32,
    rgb: [u8; 3],
) {
    let h = t / 2;
    paint(p, (x0 - h, y0 - h, x1 + h + 1, y0 - h + t), rgb);
    paint(p, (x0 - h, y1 - h, x1 + h + 1, y1 - h + t), rgb);
    paint(p, (x0 - h, y0 - h, x0 - h + t, y1 + h + 1), rgb);
    paint(p, (x1 - h, y0 - h, x1 - h + t, y1 + h + 1), rgb);
}

/// White paper with a black rectangle outline and a black line through
/// the middle of it, all 3 px thick.
pub(crate) fn boxed_diagram() -> Pixels {
    let mut p = flat(200, 150, [255; 3]);
    outline(&mut p, (30, 30, 170, 120), 3, [0; 3]);
    paint(&mut p, (30, 74, 171, 77), [0; 3]);
    p
}

/// Distance from `(x, y)` to the nearest of the segments of `lines`.
pub(crate) fn gap_to(lines: &[Vec<[f32; 2]>], x: f32, y: f32) -> f32 {
    let mut best = f32::MAX;
    for pts in lines {
        for seg in pts.windows(2) {
            let (a, b) = (seg[0], seg[1]);
            let (dx, dy) = (b[0] - a[0], b[1] - a[1]);
            let len2 = dx * dx + dy * dy;
            let t = if len2 > 0.0 {
                (((x - a[0]) * dx + (y - a[1]) * dy) / len2).clamp(0.0, 1.0)
            } else {
                0.0
            };
            best = best.min((a[0] + t * dx - x).hypot(a[1] + t * dy - y));
        }
    }
    best
}
