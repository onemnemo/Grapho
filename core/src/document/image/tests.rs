use super::fixtures::{jpeg, png_with_alpha};
use super::*;

#[test]
fn sniffs_a_png_and_a_jpeg() {
    assert_eq!(sniff(&png_with_alpha(5, 7)), Ok((ImageKind::Png, 5, 7)));
    assert_eq!(sniff(&jpeg()), Ok((ImageKind::Jpeg, 8, 8)));
}

#[test]
fn refuses_anything_else() {
    assert_eq!(sniff(b""), Err(ImageError::Unrecognised));
    assert_eq!(sniff(b"GIF89a......"), Err(ImageError::Unrecognised));
    assert_eq!(sniff(&PNG_SIGNATURE), Err(ImageError::Unrecognised));
    assert_eq!(
        sniff(&png_with_alpha(5, 7)[..20]),
        Err(ImageError::Unrecognised)
    );
    assert_eq!(sniff(&jpeg()[..30]), Err(ImageError::Unrecognised));
    assert_eq!(sniff(&[0xFF, 0xD8]), Err(ImageError::Unrecognised));
}

#[test]
fn refuses_empty_and_oversize_pictures() {
    let mut png = png_with_alpha(5, 7);
    png[16..20].copy_from_slice(&0u32.to_be_bytes());
    assert!(matches!(sniff(&png), Err(ImageError::Unsupported(_))));
    png[16..20].copy_from_slice(&(MAX_IMAGE_SIDE + 1).to_be_bytes());
    assert!(matches!(sniff(&png), Err(ImageError::Unsupported(_))));
}

#[test]
fn refuses_jpegs_it_cannot_embed() {
    let mut cmyk = jpeg();
    let frame = cmyk.windows(2).position(|w| w == [0xFF, 0xC0]).unwrap();
    cmyk[frame + 9] = 4;
    assert!(matches!(sniff(&cmyk), Err(ImageError::Unsupported(_))));
    let mut lossless = jpeg();
    lossless[frame + 1] = 0xC3;
    assert!(matches!(sniff(&lossless), Err(ImageError::Unsupported(_))));
    let mut deep = jpeg();
    deep[frame + 4] = 12;
    assert!(matches!(sniff(&deep), Err(ImageError::Unsupported(_))));
}

#[test]
fn progressive_jpegs_are_read_like_baseline_ones() {
    let mut progressive = jpeg();
    let frame = progressive
        .windows(2)
        .position(|w| w == [0xFF, 0xC0])
        .unwrap();
    progressive[frame + 1] = 0xC2;
    assert_eq!(sniff(&progressive), Ok((ImageKind::Jpeg, 8, 8)));
}

#[test]
fn json_has_no_bytes_and_leaves_a_full_crop_out() {
    let source = ImageSource::read(png_with_alpha(4, 3)).unwrap();
    let json = serde_json::to_value(&source).unwrap();
    assert_eq!(json["kind"], "png");
    assert_eq!(json["width"], 4);
    assert_eq!(json.as_object().unwrap().len(), 4, "id, kind and size only");

    let mut image = Image::new(source.id, 1.0, 2.0, 30.0, 20.0);
    let json = serde_json::to_string(&image).unwrap();
    assert!(!json.contains("crop"));
    assert_eq!(serde_json::from_str::<Image>(&json).unwrap(), image);
    image.crop = [0.1, 0.2, 0.9, 1.0];
    let back: Image = serde_json::from_str(&serde_json::to_string(&image).unwrap()).unwrap();
    assert_eq!(back, image);
}

#[test]
fn composing_applies_the_inner_matrix_first() {
    let scale = [2.0, 0.0, 0.0, 0.0, 3.0, 0.0];
    let shift = [1.0, 0.0, 10.0, 0.0, 1.0, 20.0];
    assert_eq!(compose(shift, scale), [2.0, 0.0, 10.0, 0.0, 3.0, 20.0]);
    assert_eq!(compose(scale, shift), [2.0, 0.0, 20.0, 0.0, 3.0, 60.0]);
}

#[test]
fn a_flat_image_contains_nothing() {
    let mut image = Image::new(Id::new_v4(), 0.0, 0.0, 10.0, 10.0);
    assert!(image.contains(5.0, 5.0) && !image.contains(11.0, 5.0));
    image.transform = [10.0, 0.0, 0.0, 0.0, 0.0, 0.0];
    assert!(!image.contains(5.0, 0.0));
}
