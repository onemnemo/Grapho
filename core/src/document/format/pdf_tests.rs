//! Tests for the PDF parts of the file format.

use std::io::Cursor as IoCursor;

use super::tests::{sample_doc, to_bytes};
use super::*;

fn pdf_doc() -> Document {
    let bytes = crate::pdf::fixtures::pdf_of(&[(612.0, 792.0), (800.0, 450.0)]);
    let mut doc = Document::from_pdf("Slides", "slides.pdf", bytes).unwrap();
    doc.pages[1].layers[0]
        .strokes
        .push(sample_doc().pages[0].layers[0].strokes[0].clone());
    doc
}

pub(super) fn version_of(bytes: &[u8]) -> u32 {
    let mut zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    let m: Manifest = serde_json::from_reader(zip.by_name(MANIFEST).unwrap()).unwrap();
    m.version
}

/// The archive without the entries whose name starts with `prefix`.
pub(super) fn without(bytes: &[u8], prefix: &str) -> Vec<u8> {
    let mut zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    let mut out = ZipWriter::new(IoCursor::new(Vec::new()));
    for i in 0..zip.len() {
        let file = zip.by_index_raw(i).unwrap();
        if !file.name().is_ok_and(|n| n.starts_with(prefix)) {
            out.raw_copy_file(file).unwrap();
        }
    }
    out.finish().unwrap().into_inner()
}

#[test]
fn pdf_pages_round_trip_with_their_source() {
    let doc = pdf_doc();
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 2);
    let loaded = load(IoCursor::new(bytes.clone())).unwrap();
    assert_eq!(loaded, doc);
    assert_eq!(loaded.pdfs[0].bytes, doc.pdfs[0].bytes);
    assert_eq!(loaded.pages[1].pdf, doc.pages[1].pdf);
    assert_eq!(loaded.pages[1].size, doc.pages[1].size);

    let mut zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    let entry = zip.by_name(&pdf_path(doc.pdfs[0].id)).unwrap();
    assert_eq!(entry.compression(), CompressionMethod::Stored);
}

#[test]
fn notes_without_pdfs_stay_version_one() {
    assert_eq!(version_of(&to_bytes(&sample_doc())), 1);
}

#[test]
fn a_source_no_page_shows_is_not_written() {
    let mut doc = pdf_doc();
    doc.pages.truncate(1);
    let extra = doc.pdfs[0].clone();
    doc.pages[0].pdf = None;
    doc.pages[0].size = None;
    doc.pdfs = vec![extra.clone()];
    let bytes = to_bytes(&doc);
    assert_eq!(version_of(&bytes), 1);
    let loaded = load(IoCursor::new(bytes.clone())).unwrap();
    assert!(loaded.pdfs.is_empty());
    let zip = ZipArchive::new(IoCursor::new(bytes)).unwrap();
    assert!(zip
        .file_names()
        .all(|n| n.is_ok_and(|n| !n.starts_with("pdfs/"))));
}

#[test]
fn a_page_whose_pdf_file_is_missing_is_corrupt() {
    let bytes = without(&to_bytes(&pdf_doc()), "pdfs/");
    assert!(matches!(
        load(IoCursor::new(bytes)),
        Err(FormatError::Corrupt(_))
    ));
}

#[test]
fn a_page_whose_pdf_is_not_listed_is_corrupt() {
    let mut doc = pdf_doc();
    doc.pdfs.clear();
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
fn a_page_past_the_end_of_its_pdf_is_corrupt() {
    let mut doc = pdf_doc();
    doc.pages[1].pdf.as_mut().unwrap().index = 2;
    assert!(matches!(
        load(IoCursor::new(to_bytes(&doc))),
        Err(FormatError::Corrupt(_))
    ));
}

#[test]
fn info_lists_the_pdfs_without_reading_them() {
    let doc = pdf_doc();
    // Even with the PDF entry gone, listing the note works.
    let bytes = without(&to_bytes(&doc), "pdfs/");
    let info = load_info(IoCursor::new(bytes)).unwrap();
    assert_eq!(info.pdfs.len(), 1);
    assert_eq!(info.pdfs[0].name, "slides.pdf");
    assert_eq!(info.pdfs[0].pages, 2);
    assert!(info.pdfs[0].bytes.is_empty());
    assert_eq!(info.pages[0].pdf, doc.pages[0].pdf);
}
