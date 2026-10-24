//! The polygon union behind `outline`. This is the only code that touches
//! the boolean operations crate.

use i_overlay::core::fill_rule::FillRule;
use i_overlay::core::overlay_rule::OverlayRule;
use i_overlay::float::single::SingleFloatOverlay;
use std::panic::{catch_unwind, AssertUnwindSafe};

/// Appends the non-zero union of the packed contours in `packed` to `out`,
/// in the same packing. Contours may wind either way, and may cross
/// themselves.
pub(super) fn union(packed: &[f32], out: &mut Vec<f32>) {
    let mut contours: Vec<Vec<[f32; 2]>> = Vec::new();
    let mut i = 0;
    while i < packed.len() {
        let n = packed[i] as usize;
        let mut contour: Vec<[f32; 2]> = packed[i + 1..i + 1 + 2 * n].as_chunks::<2>().0.to_vec();
        i += 1 + 2 * n;
        // The overlay panics on a point that is not a finite number, and
        // such a contour has nothing to draw anyway.
        if !contour.iter().flatten().all(|v| v.is_finite()) {
            continue;
        }
        // The non-zero rule only merges contours that wind the same way.
        if signed_area(&contour) < 0.0 {
            contour.reverse();
        }
        contours.push(contour);
    }
    // Should the overlay still panic, the contours as they are fill the same
    // shape with the non-zero rule, only with edges inside it. A stroke that
    // draws with seams beats a note that cannot open.
    let shapes = catch_unwind(AssertUnwindSafe(|| merge(&contours, DIRECT)))
        .unwrap_or_else(|_| contours.iter().map(|c| vec![c.clone()]).collect());
    for shape in &shapes {
        for (k, contour) in shape.iter().enumerate() {
            if contour.len() < 3 {
                continue;
            }
            let area = signed_area(contour);
            if area.abs() < MIN_AREA {
                continue;
            }
            // The first contour of a shape is its outer boundary and the
            // rest are holes. Outer boundaries wind with positive area.
            let reverse = (area > 0.0) != (k == 0);
            out.push(contour.len() as f32);
            if reverse {
                for v in contour.iter().rev() {
                    out.extend_from_slice(v);
                }
            } else {
                for v in contour {
                    out.extend_from_slice(v);
                }
            }
        }
    }
}

/// Lists up to this long are merged in a single overlay. A union is cheap
/// while few contours overlap, so ordinary strokes stay on this path.
const DIRECT: usize = 256;

/// Contours in the groups that the first split of a long list ends in.
const LEAF: usize = 16;

/// How many parts a long list is split into at each step.
const FAN: usize = 8;

/// The non-zero union of `contours` as shapes.
///
/// One overlay over thousands of heavily overlapping contours, like a dense
/// scribble, costs far more than the sum of overlays over parts of it, because
/// the cost grows faster than the number of crossings. So a long list is cut
/// into consecutive parts, each part is merged, and the merged parts (which no
/// longer overlap inside themselves) are merged once more.
fn merge(contours: &[Vec<[f32; 2]>], direct: usize) -> Vec<Vec<Vec<[f32; 2]>>> {
    let clip: [[f32; 2]; 0] = [];
    if contours.len() <= direct {
        return contours.overlay(&clip.as_slice(), OverlayRule::Union, FillRule::NonZero);
    }
    let part = contours.len().div_ceil(FAN).max(LEAF);
    let merged: Vec<Vec<[f32; 2]>> = contours
        .chunks(part)
        .flat_map(|c| merge(c, LEAF))
        .flatten()
        .collect();
    merged.overlay(&clip.as_slice(), OverlayRule::Union, FillRule::NonZero)
}

/// Contours enclosing less than this, in squared units of the points, are
/// numerical slivers left by the union and are dropped.
const MIN_AREA: f32 = 1e-4;

pub(super) fn signed_area(v: &[[f32; 2]]) -> f32 {
    let mut sum = 0.0;
    for i in 0..v.len() {
        let (a, b) = (v[i], v[(i + 1) % v.len()]);
        sum += a[0] * b[1] - b[0] * a[1];
    }
    sum * 0.5
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Thin rectangles criss-crossing a small box, like a dense scribble.
    fn scribble(n: usize) -> Vec<Vec<[f32; 2]>> {
        (0..n)
            .map(|i| {
                let a = i as f32 * 0.37;
                let (cx, cy) = (20.0 + (a * 3.1).sin() * 15.0, 20.0 + (a * 1.7).cos() * 15.0);
                let (dx, dy) = (a.cos() * 8.0, a.sin() * 8.0);
                vec![
                    [cx - dx, cy - dy],
                    [cx + dx, cy + dy],
                    [cx + dx - dy * 0.1, cy + dy + dx * 0.1],
                    [cx - dx - dy * 0.1, cy - dy + dx * 0.1],
                ]
            })
            .collect()
    }

    fn total(shapes: &[Vec<Vec<[f32; 2]>>]) -> f32 {
        shapes
            .iter()
            .map(|s| {
                s.iter()
                    .enumerate()
                    .map(|(k, c)| if k == 0 { 1.0 } else { -1.0 } * signed_area(c).abs())
                    .sum::<f32>()
            })
            .sum()
    }

    #[test]
    fn splitting_a_long_list_gives_the_same_area() {
        let mut contours = scribble(300);
        for c in &mut contours {
            if signed_area(c) < 0.0 {
                c.reverse();
            }
        }
        let whole = total(&merge(&contours, usize::MAX));
        let split = total(&merge(&contours, 40));
        assert!((whole - split).abs() < whole * 1e-3, "{whole} vs {split}");
    }
}
