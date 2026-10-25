//! Pictures placed on a layer: the embedded files and where they sit.
//!
//! The core never decodes pixels for editing. It reads the kind and the
//! pixel size from the file header, and keeps the encoded bytes as they came.

use std::fmt;
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use super::Id;

/// The widest or tallest picture the core accepts, in pixels.
pub const MAX_IMAGE_SIDE: u32 = 16_384;

/// How a source file is encoded. Persisted: append only.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ImageKind {
    Png,
    Jpeg,
}

impl ImageKind {
    /// The file extension the source is stored under in the archive.
    pub fn extension(self) -> &'static str {
        match self {
            ImageKind::Png => "png",
            ImageKind::Jpeg => "jpg",
        }
    }
}

/// An image file embedded in the note. Its bytes live in `images/<id>.png`
/// or `images/<id>.jpg`, not in the JSON, and are shared between clones.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct ImageSource {
    pub id: Id,
    pub kind: ImageKind,
    /// Size in pixels.
    pub width: u32,
    pub height: u32,
    #[serde(skip)]
    pub bytes: Arc<Vec<u8>>,
}

impl ImageSource {
    /// A new source for `bytes`, with its kind and size read from them.
    pub fn read(bytes: Vec<u8>) -> Result<Self, ImageError> {
        let (kind, width, height) = sniff(&bytes)?;
        Ok(Self {
            id: Id::new_v4(),
            kind,
            width,
            height,
            bytes: Arc::new(bytes),
        })
    }
}

/// The part of a source that is shown: `[left, top, right, bottom]` as
/// fractions of its size.
pub const FULL_CROP: [f32; 4] = [0.0, 0.0, 1.0, 1.0];

fn full_crop() -> [f32; 4] {
    FULL_CROP
}

fn is_full_crop(crop: &[f32; 4]) -> bool {
    *crop == FULL_CROP
}

/// An image placed on a layer. It is drawn under the layer's strokes, in
/// list order.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct Image {
    pub id: Id,
    pub source: Id,
    /// Maps the unit square (0..1 across, 0..1 down, y down) of the shown
    /// part of the source onto the page, in points:
    /// `[sx, kx, tx, ky, sy, ty]`, `x' = sx*x + kx*y + tx`,
    /// `y' = ky*x + sy*y + ty`. Same order as `Editor::transform`.
    pub transform: [f32; 6],
    /// The part of the source shown. Absent in the file means all of it.
    #[serde(default = "full_crop", skip_serializing_if = "is_full_crop")]
    pub crop: [f32; 4],
}

/// `outer` applied after `inner`, both as `[sx, kx, tx, ky, sy, ty]`.
pub fn compose(outer: [f32; 6], inner: [f32; 6]) -> [f32; 6] {
    let [a0, a1, a2, a3, a4, a5] = outer;
    let [b0, b1, b2, b3, b4, b5] = inner;
    [
        a0 * b0 + a1 * b3,
        a0 * b1 + a1 * b4,
        a0 * b2 + a1 * b5 + a2,
        a3 * b0 + a4 * b3,
        a3 * b1 + a4 * b4,
        a3 * b2 + a4 * b5 + a5,
    ]
}

fn apply(m: &[f32; 6], x: f32, y: f32) -> (f32, f32) {
    (m[0] * x + m[1] * y + m[2], m[3] * x + m[4] * y + m[5])
}

impl Image {
    /// An unrotated, uncropped image of `source` covering the rectangle.
    pub fn new(source: Id, x: f32, y: f32, width: f32, height: f32) -> Self {
        Self {
            id: Id::new_v4(),
            source,
            transform: [width, 0.0, x, 0.0, height, y],
            crop: FULL_CROP,
        }
    }

    /// Where a point of the unit square lands on the page.
    pub fn to_page(&self, u: f32, v: f32) -> (f32, f32) {
        apply(&self.transform, u, v)
    }

    /// The page positions of the top left, top right, bottom right and
    /// bottom left corners of the shown part.
    pub fn corners(&self) -> [(f32, f32); 4] {
        [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)].map(|(u, v)| self.to_page(u, v))
    }

    /// Where a page point falls in the unit square, or `None` if the image
    /// is squashed flat.
    pub fn to_unit(&self, x: f32, y: f32) -> Option<(f32, f32)> {
        let [sx, kx, tx, ky, sy, ty] = self.transform;
        let det = sx * sy - kx * ky;
        if !det.is_finite() || det.abs() <= f32::EPSILON {
            return None;
        }
        let (dx, dy) = (x - tx, y - ty);
        Some(((sy * dx - kx * dy) / det, (sx * dy - ky * dx) / det))
    }

    /// Whether a page point is on the shown part of the image.
    pub fn contains(&self, x: f32, y: f32) -> bool {
        self.to_unit(x, y)
            .is_some_and(|(u, v)| (0.0..=1.0).contains(&u) && (0.0..=1.0).contains(&v))
    }

    /// A copy with a new id, placed by `m` after its own transform.
    pub fn transformed(&self, m: [f32; 6]) -> Self {
        Self {
            id: Id::new_v4(),
            transform: compose(m, self.transform),
            ..self.clone()
        }
    }

    /// Whether the placement and crop are finite and the crop is a real,
    /// non-empty part of the source.
    pub(super) fn is_sound(&self) -> bool {
        let [l, t, r, b] = self.crop;
        self.transform.iter().all(|v| v.is_finite())
            && self.crop.iter().all(|v| (0.0..=1.0).contains(v))
            && l < r
            && t < b
    }
}

/// Why a picture could not be used.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ImageError {
    /// The bytes are neither a PNG nor a JPEG.
    Unrecognised,
    /// A PNG or JPEG the core can't use, with the reason.
    Unsupported(&'static str),
    /// The page doesn't exist, or its active layer is hidden or locked.
    NoLayer,
    /// The rectangle to place the image in is empty or not a number.
    BadRect,
    /// The image isn't on the page.
    NotFound,
    /// The pixels given are not the decoded source of the image.
    SizeMismatch,
    /// No drawing was found in the picture.
    NothingFound,
}

impl fmt::Display for ImageError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ImageError::Unrecognised => write!(f, "this isn't a PNG or JPEG picture"),
            ImageError::Unsupported(why) => write!(f, "this picture can't be used: {why}"),
            ImageError::NoLayer => write!(f, "the layer is hidden or locked"),
            ImageError::BadRect => write!(f, "the picture's place on the page is not valid"),
            ImageError::NotFound => write!(f, "the picture is not on the page"),
            ImageError::SizeMismatch => write!(f, "the pixels don't match the picture"),
            ImageError::NothingFound => write!(f, "no drawing was found in the picture"),
        }
    }
}

impl std::error::Error for ImageError {}

const PNG_SIGNATURE: [u8; 8] = [0x89, b'P', b'N', b'G', 0x0D, 0x0A, 0x1A, 0x0A];

/// Reads the kind and the pixel size from the header of a PNG or JPEG.
pub fn sniff(bytes: &[u8]) -> Result<(ImageKind, u32, u32), ImageError> {
    let (kind, width, height) = if bytes.starts_with(&PNG_SIGNATURE) {
        let (w, h) = png_size(bytes)?;
        (ImageKind::Png, w, h)
    } else if bytes.starts_with(&[0xFF, 0xD8]) {
        let (w, h) = jpeg_size(bytes)?;
        (ImageKind::Jpeg, w, h)
    } else {
        return Err(ImageError::Unrecognised);
    };
    if width == 0 || height == 0 {
        return Err(ImageError::Unsupported("it has no pixels"));
    }
    if width > MAX_IMAGE_SIDE || height > MAX_IMAGE_SIDE {
        return Err(ImageError::Unsupported("it is too large"));
    }
    Ok((kind, width, height))
}

fn be32(bytes: &[u8]) -> u32 {
    u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])
}

/// The size from the IHDR chunk, which always comes first.
fn png_size(bytes: &[u8]) -> Result<(u32, u32), ImageError> {
    match bytes.get(8..24) {
        Some(h) if &h[4..8] == b"IHDR" => {
            let data = bytes.get(16..24).ok_or(ImageError::Unrecognised)?;
            Ok((be32(data), be32(&data[4..])))
        }
        _ => Err(ImageError::Unrecognised),
    }
}

/// The size from the first baseline, extended or progressive frame header.
/// Other kinds of JPEG (lossless, arithmetic coded) are refused, as are
/// those that are not 8 bit gray or colour.
fn jpeg_size(bytes: &[u8]) -> Result<(u32, u32), ImageError> {
    let mut at = 2;
    loop {
        // A marker is 0xFF and a code, possibly after 0xFF padding.
        if bytes.get(at) != Some(&0xFF) {
            return Err(ImageError::Unrecognised);
        }
        while bytes.get(at + 1) == Some(&0xFF) {
            at += 1;
        }
        let code = *bytes.get(at + 1).ok_or(ImageError::Unrecognised)?;
        at += 2;
        match code {
            // Markers without a length: restarts, TEM and a stray SOI.
            0x01 | 0xD0..=0xD8 => continue,
            0xD9 | 0xDA => return Err(ImageError::Unrecognised),
            _ => {}
        }
        let len = bytes.get(at..at + 2).ok_or(ImageError::Unrecognised)?;
        let len = usize::from(u16::from_be_bytes([len[0], len[1]]));
        if len < 2 {
            return Err(ImageError::Unrecognised);
        }
        match code {
            0xC0..=0xC2 => {
                let f = bytes.get(at + 2..at + 8).ok_or(ImageError::Unrecognised)?;
                if f[0] != 8 {
                    return Err(ImageError::Unsupported("it is not 8 bit"));
                }
                let height = u32::from(u16::from_be_bytes([f[1], f[2]]));
                let width = u32::from(u16::from_be_bytes([f[3], f[4]]));
                if !matches!(f[5], 1 | 3) {
                    return Err(ImageError::Unsupported("it is not gray or RGB"));
                }
                return Ok((width, height));
            }
            0xC3 | 0xC5..=0xC7 | 0xC9..=0xCB | 0xCD..=0xCF => {
                return Err(ImageError::Unsupported(
                    "this kind of JPEG is not supported",
                ));
            }
            _ => at += len,
        }
    }
}

#[cfg(test)]
pub(crate) mod fixtures;
#[cfg(test)]
mod tests;
