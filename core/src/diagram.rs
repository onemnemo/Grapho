//! Cleaning up pictures of drawings: lifting the paper away from the lines,
//! and tracing the lines into centre lines that can become pen strokes.
//!
//! Everything here works on plain pixel buffers and knows nothing about the
//! document. Both steps estimate the paper colour region by region, so a
//! phone photo with uneven light works as well as a clean export.

mod background;
mod blobs;
mod colours;
mod encode;
mod lines;
mod mask;
mod outline;
mod paths;
mod simplify;
mod skeleton;
mod thin;
mod trace;

pub use background::{cut_out, remove_background, Cutout, DEFAULT_STRENGTH, MAX_STRENGTH};
pub use encode::encode_png;
pub use outline::Outline;
pub use trace::trace;

/// A decoded picture, straight (not premultiplied) RGBA8, row major, top
/// row first.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Pixels {
    pub width: u32,
    pub height: u32,
    pub rgba: Vec<u8>,
}

impl Pixels {
    /// From Android style ints `0xAARRGGBB` (unpremultiplied), as `i32`.
    /// `None` when the length does not match or a side is 0.
    pub fn from_argb(width: u32, height: u32, argb: &[i32]) -> Option<Self> {
        let count = (width as usize).checked_mul(height as usize)?;
        if width == 0 || height == 0 || argb.len() != count {
            return None;
        }
        let mut rgba = Vec::with_capacity(count.checked_mul(4)?);
        for &v in argb {
            let [a, r, g, b] = v.to_be_bytes();
            rgba.extend_from_slice(&[r, g, b, a]);
        }
        Some(Self {
            width,
            height,
            rgba,
        })
    }

    /// Whether the buffer holds exactly `width * height` pixels, at least one.
    pub fn is_sound(&self) -> bool {
        let count = (self.width as usize).checked_mul(self.height as usize);
        self.width > 0
            && self.height > 0
            && count.and_then(|n| n.checked_mul(4)) == Some(self.rgba.len())
    }

    /// The part inside `[x0, x1) x [y0, y1)`, which must lie inside the
    /// picture and not be empty.
    pub fn region(&self, x0: u32, y0: u32, x1: u32, y1: u32) -> Self {
        let (w, h) = ((x1 - x0) as usize, (y1 - y0) as usize);
        let stride = self.width as usize * 4;
        let mut rgba = Vec::with_capacity(w * h * 4);
        for y in y0 as usize..y1 as usize {
            let start = y * stride + x0 as usize * 4;
            rgba.extend_from_slice(&self.rgba[start..start + w * 4]);
        }
        Self {
            width: x1 - x0,
            height: y1 - y0,
            rgba,
        }
    }
}

/// A centre line of the drawing, in pixel coordinates of the traced
/// picture (pixel centres at +0.5).
#[derive(Clone, Debug, PartialEq)]
pub struct TracedLine {
    /// At least two points. A closed outline repeats its first point last.
    /// For a filled shape, the corners of its outline, three or more, and
    /// the first point is not repeated.
    pub points: Vec<[f32; 2]>,
    /// Line width in pixels, at least 1. 0 for a filled shape.
    pub width: f32,
    /// `0xRRGGBBAA`.
    pub color: u32,
    /// A filled shape (an arrowhead, a dot) rather than a line.
    pub filled: bool,
}

#[cfg(test)]
mod colour_tests;
#[cfg(test)]
mod cutout_tests;
#[cfg(test)]
pub(crate) mod fixtures;
#[cfg(test)]
mod shapes_tests;
#[cfg(test)]
mod tests;
