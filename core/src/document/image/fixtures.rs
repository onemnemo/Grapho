//! Small pictures for tests, built in place.

/// A valid 8 by 8 baseline JPEG, three components, one flat colour.
const JPEG_HEX: &str = concat!(
    "ffd8ffe000104a46494600010100000100010000ffdb004300100b0c0e0c0a100e0d0e1211101318281a181616183123",
    "251d283a333d3c3933383740485c4e404457453738506d51575f626768673e4d71797064785c656763ffdb0043011112",
    "121815182f1a1a2f63423842636363636363636363636363636363636363636363636363636363636363636363636363",
    "6363636363636363636363636363ffc00011080008000803011100021101031101ffc4001f0000010501010101010100",
    "000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d010203000411051221",
    "31410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a",
    "434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a9293949596979899",
    "9aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1",
    "f2f3f4f5f6f7f8f9faffc4001f0100030101010101010101010000000000000102030405060708090a0bffc400b51100",
    "020102040403040705040400010277000102031104052131061241510761711322328108144291a1b1c109233352f015",
    "6272d10a162434e125f11718191a262728292a35363738393a434445464748494a535455565758595a63646566676869",
    "6a737475767778797a82838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4",
    "c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00cb",
    "ae23e98fffd9",
);

pub(crate) fn jpeg() -> Vec<u8> {
    let hex = JPEG_HEX.as_bytes();
    hex.as_chunks::<2>()
        .0
        .iter()
        .map(|pair| {
            let digit = |c: u8| char::from(c).to_digit(16).expect("the fixture is hex") as u8;
            digit(pair[0]) << 4 | digit(pair[1])
        })
        .collect()
}

/// An RGBA PNG whose pixels fade from clear on the left to solid on the right.
pub(crate) fn png_with_alpha(width: u32, height: u32) -> Vec<u8> {
    let mut out = Vec::new();
    let mut enc = png::Encoder::new(&mut out, width, height);
    enc.set_color(png::ColorType::Rgba);
    enc.set_depth(png::BitDepth::Eight);
    let mut writer = enc.write_header().expect("a valid header");
    let mut pixels = Vec::with_capacity((width * height * 4) as usize);
    for _ in 0..height {
        for x in 0..width {
            let alpha = (x * 255 / width.max(1)) as u8;
            pixels.extend_from_slice(&[30, 120, 220, alpha]);
        }
    }
    writer.write_image_data(&pixels).expect("matching data");
    writer.finish().expect("a finished file");
    out
}
