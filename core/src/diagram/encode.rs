//! PNG output for a cleaned picture.

use super::Pixels;

/// The picture as an RGBA PNG file. Returns an empty vector if the buffer
/// does not match its size, which is not a picture.
pub fn encode_png(p: &Pixels) -> Vec<u8> {
    if !p.is_sound() {
        return Vec::new();
    }
    let mut out = Vec::with_capacity(p.rgba.len() / 8 + 64);
    let mut enc = png::Encoder::new(&mut out, p.width, p.height);
    enc.set_color(png::ColorType::Rgba);
    enc.set_depth(png::BitDepth::Eight);
    enc.set_compression(png::Compression::Fast);
    let written = enc
        .write_header()
        .and_then(|mut w| w.write_image_data(&p.rgba).and_then(|()| w.finish()));
    if written.is_err() {
        return Vec::new();
    }
    out
}
