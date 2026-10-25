//! Tests for the image parts of the file format.

use std::io::Cursor as IoCursor;

use super::pdf_tests::{version_of, without};
use super::tests::{sample_doc, to_bytes};
use super::*;
use crate::document::image_fixtures::{jpeg, png_with_alpha};
use crate::document::{Editor, ImageKind, ImageSource};

/// The sample note with a PNG on page 0 and a JPEG on page 1, the second one
/// cropped.
fn image_doc() -> Document {
    let mut doc = sample_doc();
    for (page, bytes) in [png_with_alpha(4, 3), jpeg()].into_iter().enumerate() {
        let source = ImageSource::read(bytes).unwrap();
        let mut image = crate::document::Image::new(source.id, 10.0, 20.0, 80.0, 60.0);
        if page == 1 {
            image.crop = [0.25, 0.0, 1.0, 0.5];
        }
        doc.pages[page].layers[1].images.push(image);
        doc.images.push(source);
    }
    doc
}

fn names(bytes: &[u8]) -> Vec<String> {
    let zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    zip.file_names()
        .filter_map(|n| n.ok())
        .map(String::from)
        .collect()
}

#[test]
fn images_round_trip_with_their_bytes_and_crop() {
    let doc = image_doc();
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 3);
    let loaded = load(IoCursor::new(bytes.clone())).unwrap();
    assert_eq!(loaded, doc);
    assert_eq!(loaded.images[0].bytes, doc.images[0].bytes);
    assert_eq!(loaded.images[1].kind, ImageKind::Jpeg);
    assert_eq!(
        loaded.pages[1].layers[1].images[0].crop,
        [0.25, 0.0, 1.0, 0.5]
    );

    let mut zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    for source in &doc.images {
        let entry = zip.by_name(&images::path(source)).unwrap();
        assert_eq!(entry.compression(), CompressionMethod::Stored);
    }
    assert!(images::path(&doc.images[0]).ends_with(".png"));
    assert!(images::path(&doc.images[1]).ends_with(".jpg"));
}

#[test]
fn notes_without_images_keep_their_older_versions() {
    assert_eq!(version_of(&to_bytes(&sample_doc())), 1);
    let pdf = crate::pdf::fixtures::pdf_of(&[(612.0, 792.0)]);
    let doc = Document::from_pdf("S", "s.pdf", pdf).unwrap();
    assert_eq!(version_of(&to_bytes(&doc)), 2);
    let json = serde_json::to_string(&sample_doc()).unwrap();
    assert!(!json.contains("images"));
}

#[test]
fn a_pdf_note_with_an_image_is_version_three() {
    let pdf = crate::pdf::fixtures::pdf_of(&[(612.0, 792.0)]);
    let mut doc = Document::from_pdf("S", "s.pdf", pdf).unwrap();
    let source = ImageSource::read(jpeg()).unwrap();
    let image = crate::document::Image::new(source.id, 0.0, 0.0, 10.0, 10.0);
    doc.pages[0].layers[0].images.push(image);
    doc.images.push(source);
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 3);
    assert_eq!(load(IoCursor::new(bytes)).unwrap(), doc);
}

#[test]
fn a_source_no_image_uses_is_not_written() {
    let mut doc = image_doc();
    doc.pages[1].layers[1].images.clear();
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 3);
    assert_eq!(
        names(&bytes)
            .iter()
            .filter(|n| n.starts_with("images/"))
            .count(),
        1
    );
    let loaded = load(IoCursor::new(bytes)).unwrap();
    assert_eq!(loaded.images.len(), 1);
    assert_eq!(loaded.images[0].id, doc.images[0].id);

    doc.pages[0].layers[1].images.clear();
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 1);
    assert!(names(&bytes).iter().all(|n| !n.starts_with("images/")));
    assert!(load(IoCursor::new(bytes)).unwrap().images.is_empty());
}

#[test]
fn a_shared_source_is_written_once() {
    let mut doc = image_doc();
    let again = doc.pages[0].layers[1].images[0].clone();
    doc.pages[1].layers[0].images.push(again);
    doc.pages[1].layers[0].images[0].id = Id::new_v4();
    let bytes = to_bytes(&doc);
    let count = names(&bytes)
        .iter()
        .filter(|n| n.starts_with("images/") && n.ends_with(".png"))
        .count();
    assert_eq!(count, 1);
    assert_eq!(load(IoCursor::new(bytes)).unwrap(), doc);
}

#[test]
fn info_lists_images_without_reading_their_bytes() {
    let doc = image_doc();
    let bytes = without(&to_bytes(&doc), "images/");
    let info = load_info(IoCursor::new(bytes)).unwrap();
    assert_eq!(info.images.len(), 2);
    assert!(info.images.iter().all(|s| s.bytes.is_empty()));
    assert_eq!(
        info.pages[0].layers[1].images,
        doc.pages[0].layers[1].images
    );
    assert_eq!(info.pages[0].stroke_count(), 0);
}

#[test]
fn an_image_file_that_is_missing_is_corrupt() {
    let bytes = without(&to_bytes(&image_doc()), "images/");
    assert!(matches!(
        load(IoCursor::new(bytes)),
        Err(FormatError::Corrupt(_))
    ));
}

#[test]
fn an_image_file_that_is_not_the_stated_picture_is_corrupt() {
    let mut doc = image_doc();
    doc.images[0].width += 1;
    assert!(matches!(
        load(IoCursor::new(to_bytes(&doc))),
        Err(FormatError::Corrupt(_))
    ));
    let mut doc = image_doc();
    doc.images[0].bytes = std::sync::Arc::new(b"not a picture".to_vec());
    assert!(matches!(
        load(IoCursor::new(to_bytes(&doc))),
        Err(FormatError::Corrupt(_))
    ));
}

#[test]
fn an_image_of_an_unlisted_source_is_corrupt() {
    let mut doc = image_doc();
    doc.images.clear();
    let bytes = to_bytes(&doc);
    assert!(matches!(
        load(IoCursor::new(bytes.clone())),
        Err(FormatError::Corrupt(_))
    ));
    assert!(matches!(
        load_info(IoCursor::new(bytes)),
        Err(FormatError::Corrupt(_))
    ));
}

#[test]
fn an_image_with_a_broken_place_is_corrupt() {
    for damage in [
        |i: &mut crate::document::Image| i.crop = [0.5, 0.0, 0.5, 1.0],
        |i: &mut crate::document::Image| i.crop = [0.0, 0.0, 2.0, 1.0],
        |i: &mut crate::document::Image| i.transform[0] = f32::NAN,
    ] {
        let mut doc = image_doc();
        damage(&mut doc.pages[0].layers[1].images[0]);
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
fn undo_leftovers_do_not_reach_the_file() {
    let mut e = Editor::new(sample_doc());
    e.insert_image(
        0,
        png_with_alpha(3, 3),
        crate::Rect {
            min_x: 0.0,
            min_y: 0.0,
            max_x: 10.0,
            max_y: 10.0,
        },
    )
    .unwrap();
    e.undo();
    assert_eq!(e.document().images.len(), 1);
    let bytes = to_bytes(e.document());
    assert_eq!(version_of(&bytes), 1);
    assert!(load(IoCursor::new(bytes)).unwrap().images.is_empty());
}
