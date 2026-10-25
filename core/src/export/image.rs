//! Images in the output: one XObject per embedded source, and the drawing
//! operators that place it on a page.

use lopdf::{dictionary, Dictionary, Document as Pdf, Object, ObjectId, Stream};
use png::{ColorType, Transformations};

use super::content::Content;
use super::ExportError;
use crate::document::{compose, ImageKind, ImageSource};

/// The most decoded pixel data one PNG may take, in bytes.
const MAX_DECODED: usize = 1 << 28;

/// One image on a layer, ready to draw.
pub(super) struct ImageInk {
    /// Index of the source in the note's list.
    pub source: usize,
    /// Unit square of the shown part to page, see `Image::transform`.
    pub transform: [f32; 6],
    pub crop: [f32; 4],
}

/// The name an image XObject goes by in a resource dictionary.
pub(super) fn name(source: usize) -> String {
    format!("GrIm{source}")
}

/// Draws an image: clipped to the quad its shown part covers, with the whole
/// source mapped so the crop fills that quad. `c` works in document units
/// (y down), while an image XObject is a unit square with y up.
pub(super) fn draw(c: &mut Content, image: &ImageInk) {
    let t = image.transform;
    let [l, top, r, b] = image.crop;
    // Source fractions to the unit square of the crop.
    let crop = [
        1.0 / (r - l),
        0.0,
        -l / (r - l),
        0.0,
        1.0 / (b - top),
        -top / (b - top),
    ];
    // The XObject's own y runs up from the bottom of the picture.
    let flip = [1.0, 0.0, 0.0, 0.0, -1.0, 1.0];
    let m = compose(compose(t, crop), flip);
    c.raw("q");
    for (i, (u, v)) in [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)]
        .into_iter()
        .enumerate()
    {
        let x = t[0] * u + t[1] * v + t[2];
        let y = t[3] * u + t[4] * v + t[5];
        c.op(&[x, y], if i == 0 { "m" } else { "l" });
    }
    c.raw("h");
    c.raw("W n");
    // `cm` takes [a b c d e f], with x' = a*x + c*y + e and y' = b*x + d*y + f.
    c.op(&[m[0], m[3], m[1], m[4], m[2], m[5]], "cm");
    c.raw(&format!("/{} Do", name(image.source)));
    c.raw("Q");
}

/// Adds the source to the output and returns its XObject.
pub(super) fn embed(pdf: &mut Pdf, source: &ImageSource) -> Result<ObjectId, ExportError> {
    match source.kind {
        ImageKind::Jpeg => embed_jpeg(pdf, source),
        ImageKind::Png => embed_png(pdf, source),
    }
}

fn image_dict(source: &ImageSource, colour_space: &str) -> Dictionary {
    dictionary! {
        "Type" => "XObject",
        "Subtype" => "Image",
        "Width" => i64::from(source.width),
        "Height" => i64::from(source.height),
        "ColorSpace" => colour_space,
        "BitsPerComponent" => 8,
    }
}

/// A JPEG goes in as it is. Its component count tells gray from colour.
fn embed_jpeg(pdf: &mut Pdf, source: &ImageSource) -> Result<ObjectId, ExportError> {
    let colour_space = match jpeg_components(&source.bytes) {
        Some(1) => "DeviceGray",
        Some(3) => "DeviceRGB",
        _ => {
            return Err(ExportError::Image(
                "a JPEG has an unsupported colour model".into(),
            ))
        }
    };
    let mut dict = image_dict(source, colour_space);
    dict.set("Filter", "DCTDecode");
    Ok(pdf.add_object(Object::Stream(Stream::new(dict, (*source.bytes).clone()))))
}

/// The component count of the first frame header.
fn jpeg_components(bytes: &[u8]) -> Option<u8> {
    let mut at = 2;
    while at + 4 <= bytes.len() {
        if bytes[at] != 0xFF {
            return None;
        }
        let code = bytes[at + 1];
        match code {
            0xFF => {
                at += 1;
                continue;
            }
            0x01 | 0xD0..=0xD8 => {
                at += 2;
                continue;
            }
            0xC0..=0xC2 => return bytes.get(at + 9).copied(),
            _ => {}
        }
        at += 2 + usize::from(u16::from_be_bytes([bytes[at + 2], bytes[at + 3]]));
    }
    None
}

/// A PNG is decoded to 8 bit samples. Transparency becomes a soft mask, an
/// image of its own that the picture refers to.
fn embed_png(pdf: &mut Pdf, source: &ImageSource) -> Result<ObjectId, ExportError> {
    let bad = |e: png::DecodingError| ExportError::Image(format!("a PNG can't be read: {e}"));
    let limits = png::Limits { bytes: MAX_DECODED };
    let mut decoder =
        png::Decoder::new_with_limits(std::io::Cursor::new(&source.bytes[..]), limits);
    decoder.set_transformations(Transformations::EXPAND | Transformations::STRIP_16);
    let mut reader = decoder.read_info().map_err(bad)?;
    let mut data = vec![0; reader.output_buffer_size().ok_or_else(too_big)?];
    let info = reader.next_frame(&mut data).map_err(bad)?;
    data.truncate(info.buffer_size());

    let samples = match info.color_type {
        ColorType::Grayscale | ColorType::GrayscaleAlpha => 1,
        ColorType::Rgb | ColorType::Rgba => 3,
        ColorType::Indexed => return Err(ExportError::Image("a PNG was not expanded".into())),
    };
    let (colour, mask) = match info.color_type {
        ColorType::GrayscaleAlpha => {
            let pixels = data.as_chunks::<2>().0;
            let colour = pixels.iter().map(|&[g, _]| g).collect();
            (colour, Some(pixels.iter().map(|&[_, a]| a).collect()))
        }
        ColorType::Rgba => {
            let pixels = data.as_chunks::<4>().0;
            let colour = pixels.iter().flat_map(|&[r, g, b, _]| [r, g, b]).collect();
            (colour, Some(pixels.iter().map(|&[_, _, _, a]| a).collect()))
        }
        _ => (data, None),
    };

    let space = if samples == 1 {
        "DeviceGray"
    } else {
        "DeviceRGB"
    };
    let mut dict = image_dict(source, space);
    if let Some(mask) = mask {
        let soft = image_dict(source, "DeviceGray");
        let soft = pdf.add_object(Object::Stream(Stream::new(soft, mask)));
        dict.set("SMask", soft);
    }
    Ok(pdf.add_object(Object::Stream(Stream::new(dict, colour))))
}

fn too_big() -> ExportError {
    ExportError::Image("a PNG is too large to export".into())
}
