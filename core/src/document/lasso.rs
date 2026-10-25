//! Lasso geometry: which strokes, images and text boxes a closed freehand loop encloses.

use super::{Image, Stroke, TextBox, Tool};
use crate::InputSample;

/// Whether `(x, y)` is inside the polygon, by the even-odd rule.
fn contains(poly: &[(f32, f32)], x: f32, y: f32) -> bool {
    let mut inside = false;
    let mut prev = poly[poly.len() - 1];
    for &cur in poly {
        if (cur.1 > y) != (prev.1 > y)
            && x < cur.0 + (y - cur.1) / (prev.1 - cur.1) * (prev.0 - cur.0)
        {
            inside = !inside;
        }
        prev = cur;
    }
    inside
}

fn bounds(poly: &[(f32, f32)]) -> (f32, f32, f32, f32) {
    poly.iter().fold(
        (
            f32::INFINITY,
            f32::INFINITY,
            f32::NEG_INFINITY,
            f32::NEG_INFINITY,
        ),
        |(x0, y0, x1, y1), &(x, y)| (x0.min(x), y0.min(y), x1.max(x), y1.max(y)),
    )
}

/// Whether at least half of the stroke's length lies inside the lasso.
/// Segments are judged by their midpoint and weighted by length. A fill is
/// measured along its closed boundary, and a stroke with no length counts
/// when its first point is inside. A lasso of fewer than three points
/// encloses nothing.
pub(crate) fn encloses(lasso: &[(f32, f32)], stroke: &Stroke) -> bool {
    if lasso.len() < 3 {
        return false;
    }
    let Some((sx0, sy0, sx1, sy1)) = stroke.bounds() else {
        return false;
    };
    let (lx0, ly0, lx1, ly1) = bounds(lasso);
    if sx1 < lx0 || sx0 > lx1 || sy1 < ly0 || sy0 > ly1 {
        return false;
    }
    let pts = &stroke.points;
    let closing = stroke.style.tool == Tool::Fill && pts.len() > 2;
    let seg = |a: &InputSample, b: &InputSample| (a.x - b.x).hypot(a.y - b.y);
    let mut total = 0.0f32;
    let mut inside = 0.0f32;
    let mut add = |a: &InputSample, b: &InputSample| {
        let len = seg(a, b);
        total += len;
        if contains(lasso, (a.x + b.x) * 0.5, (a.y + b.y) * 0.5) {
            inside += len;
        }
    };
    for w in pts.windows(2) {
        add(&w[0], &w[1]);
    }
    if closing {
        add(&pts[pts.len() - 1], &pts[0]);
    }
    if total <= f32::EPSILON {
        return contains(lasso, pts[0].x, pts[0].y);
    }
    inside * 2.0 >= total
}

/// Samples per side when judging how much of an image the lasso covers.
const IMAGE_GRID: usize = 8;

/// Whether at least half of the image's area lies inside the lasso. The
/// area is judged on a grid of points over the image as placed, so a
/// rotated image counts by what it covers, not by its bounding box. A lasso
/// of fewer than three points encloses nothing.
pub(crate) fn encloses_image(lasso: &[(f32, f32)], image: &Image) -> bool {
    if lasso.len() < 3 {
        return false;
    }
    covers_half(lasso, &image.corners(), |u, v| image.to_page(u, v))
}

/// Whether at least half of the text box's area lies inside the lasso,
/// judged like an image's. The area is the whole box, however little of it
/// the words fill, so what is selected is what is outlined on screen.
pub(crate) fn encloses_text(lasso: &[(f32, f32)], text: &TextBox) -> bool {
    if lasso.len() < 3 {
        return false;
    }
    let (w, h) = (text.width, text.height);
    covers_half(lasso, &text.corners(), |u, v| text.to_page(u * w, v * h))
}

/// Whether at least half of a quadrilateral, sampled on a grid through
/// `at(u, v)` over the unit square, lies inside the lasso.
fn covers_half(
    lasso: &[(f32, f32)],
    corners: &[(f32, f32)],
    at: impl Fn(f32, f32) -> (f32, f32),
) -> bool {
    let (ix0, iy0, ix1, iy1) = bounds(corners);
    let (lx0, ly0, lx1, ly1) = bounds(lasso);
    if ix1 < lx0 || ix0 > lx1 || iy1 < ly0 || iy0 > ly1 {
        return false;
    }
    let step = |i: usize| (i as f32 + 0.5) / IMAGE_GRID as f32;
    let mut inside = 0;
    for row in 0..IMAGE_GRID {
        for col in 0..IMAGE_GRID {
            let (x, y) = at(step(col), step(row));
            if contains(lasso, x, y) {
                inside += 1;
            }
        }
    }
    inside * 2 >= IMAGE_GRID * IMAGE_GRID
}
