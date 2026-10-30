//! Tests for the text box parts of the file format.

use std::io::Cursor as IoCursor;

use super::pdf_tests::version_of;
use super::tests::{sample_doc, to_bytes};
use super::*;
use crate::document::image_fixtures::jpeg;
use crate::document::{ImageSource, TextAlign, TextBox, TextStyle};

/// The sample note with a text box on each page, the second far from the
/// origin, turned and styled.
fn text_doc() -> Document {
    let mut doc = sample_doc();
    let plain = TextBox::new(10.0, 20.0, 200.0, 40.0, "Hello", TextStyle::default());
    let mut far = TextBox::new(
        5000.5,
        7000.25,
        160.0,
        64.0,
        "Line one\nLine two, with \u{00e5}\u{00f8} and \u{1f600}",
        TextStyle {
            size: 24.0,
            color: 0xD62F_2FFF,
            align: TextAlign::Centre,
            bold: true,
            italic: true,
            underline: true,
        },
    );
    far.rotation = 0.25;
    doc.pages[0].layers[1].texts.push(plain);
    doc.pages[1].layers[0].texts.push(far);
    doc
}

#[test]
fn text_boxes_round_trip_and_make_the_file_version_four() {
    let doc = text_doc();
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 4);
    assert_eq!(FORMAT_VERSION, 4);
    let loaded = load(IoCursor::new(bytes)).unwrap();
    assert_eq!(loaded, doc);
    let far = &loaded.pages[1].layers[0].texts[0];
    assert_eq!((far.x, far.y, far.rotation), (5000.5, 7000.25, 0.25));
    assert_eq!(far.align, TextAlign::Centre);
    assert!(far.bold && far.italic && far.underline && far.text.contains('\n'));
}

#[test]
fn notes_without_text_boxes_keep_their_older_versions() {
    assert_eq!(version_of(&to_bytes(&sample_doc())), 1);
    let json = serde_json::to_string(&sample_doc()).unwrap();
    assert!(!json.contains("texts"));
    let mut doc = sample_doc();
    let source = ImageSource::read(jpeg()).unwrap();
    let image = crate::document::Image::new(source.id, 0.0, 0.0, 10.0, 10.0);
    doc.pages[0].layers[0].images.push(image);
    doc.images.push(source);
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 3);
    let loaded = load(IoCursor::new(bytes)).unwrap();
    assert_eq!(loaded.pages[0].text_count(), 0);
}

#[test]
fn text_boxes_sit_beside_images_in_one_file() {
    let mut doc = text_doc();
    let source = ImageSource::read(jpeg()).unwrap();
    let image = crate::document::Image::new(source.id, 0.0, 0.0, 10.0, 10.0);
    doc.pages[0].layers[1].images.push(image);
    doc.images.push(source);
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 4);
    assert_eq!(load(IoCursor::new(bytes)).unwrap(), doc);
}

#[test]
fn a_file_saved_before_text_boxes_loads_as_having_none() {
    // The layer JSON of a version 3 file has no `texts` key at all.
    let doc = sample_doc();
    let mut json = serde_json::to_value(&doc).unwrap();
    let layer = &mut json["pages"][0]["layers"][0];
    assert!(layer.get("texts").is_none());
    layer["opacity"] = serde_json::json!(0.5);
    let back: Document = serde_json::from_value(json).unwrap();
    assert!(back.pages[0].layers[0].texts.is_empty());
}

#[test]
fn info_lists_text_boxes_without_reading_the_ink() {
    let doc = text_doc();
    let info = load_info(IoCursor::new(to_bytes(&doc))).unwrap();
    assert_eq!(info.pages[0].text_count(), 1);
    assert_eq!(info.pages[1].text_count(), 1);
    assert_eq!(info.pages[0].stroke_count(), 0);
}

#[test]
fn a_text_box_with_a_broken_place_or_size_is_corrupt() {
    for damage in [
        |t: &mut TextBox| t.width = 0.0,
        |t: &mut TextBox| t.height = -3.0,
        |t: &mut TextBox| t.size = 0.5,
        |t: &mut TextBox| t.size = 5000.0,
        |t: &mut TextBox| t.x = f32::NAN,
        |t: &mut TextBox| t.text = "x".repeat(crate::document::MAX_TEXT_CHARS + 1),
    ] {
        let mut doc = text_doc();
        damage(&mut doc.pages[0].layers[1].texts[0]);
        // JSON has no NaN, so the damage may already stop the save.
        let mut buf = IoCursor::new(Vec::new());
        if save(&doc, &mut buf).is_ok() {
            assert!(matches!(
                load_info(IoCursor::new(buf.into_inner())),
                Err(FormatError::Corrupt(_) | FormatError::Json(_))
            ));
        }
    }
}

#[test]
fn a_newer_version_than_four_is_still_refused() {
    let mut buf = IoCursor::new(Vec::new());
    let mut zip = ZipWriter::new(&mut buf);
    zip.start_file(MANIFEST, SimpleFileOptions::default())
        .unwrap();
    zip.write_all(br#"{"format":"grapho","version":5}"#)
        .unwrap();
    zip.finish().unwrap();
    assert!(matches!(
        load(IoCursor::new(buf.into_inner())),
        Err(FormatError::UnsupportedVersion(5))
    ));
}
