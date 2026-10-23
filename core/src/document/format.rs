//! Reading and writing `.grapho` files. The layout is described in
//! `docs/file-format.md`.

use std::fmt;
use std::io::{Read, Seek, Write};

use serde::{Deserialize, Serialize};
use uuid::Uuid;
use zip::write::SimpleFileOptions;
use zip::{CompressionMethod, ZipArchive, ZipWriter};

use self::images::{check as check_images, read_all as read_images};
use self::texts::check as check_texts;
use super::strokes_file::{decode_strokes, encode_strokes};
use super::{Document, Id};

/// Newest version this build reads and writes. Files with a higher version
/// are refused instead of being half-read.
pub const FORMAT_VERSION: u32 = 4;
/// Version of a file that holds neither PDF pages, images nor text boxes. It is still
/// written for those, so older readers keep opening notes that don't use
/// newer features.
const BASE_VERSION: u32 = 1;
/// Version of a file that holds PDF pages but no images.
const PDF_VERSION: u32 = 2;
/// Version of a file that holds images but no text boxes.
const IMAGE_VERSION: u32 = 3;

mod images;
mod texts;

const MANIFEST: &str = "manifest.json";
const DOCUMENT: &str = "document.json";

#[derive(Serialize, Deserialize)]
struct Manifest {
    format: String,
    version: u32,
}

#[derive(Debug)]
pub enum FormatError {
    Io(std::io::Error),
    Zip(zip::result::ZipError),
    Json(serde_json::Error),
    NotGrapho,
    UnsupportedVersion(u32),
    Corrupt(String),
}

impl fmt::Display for FormatError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            FormatError::Io(e) => write!(f, "couldn't read or write the file: {e}"),
            FormatError::Zip(e) => write!(f, "the file isn't a valid .grapho archive: {e}"),
            FormatError::Json(e) => write!(f, "the document data is invalid: {e}"),
            FormatError::NotGrapho => write!(f, "this isn't a Grapho file"),
            FormatError::UnsupportedVersion(v) => write!(
                f,
                "this file uses format version {v}, which needs a newer version of Grapho"
            ),
            FormatError::Corrupt(why) => write!(f, "the file is damaged: {why}"),
        }
    }
}

impl std::error::Error for FormatError {}

impl From<std::io::Error> for FormatError {
    fn from(e: std::io::Error) -> Self {
        FormatError::Io(e)
    }
}

impl From<zip::result::ZipError> for FormatError {
    fn from(e: zip::result::ZipError) -> Self {
        FormatError::Zip(e)
    }
}

impl From<serde_json::Error> for FormatError {
    fn from(e: serde_json::Error) -> Self {
        FormatError::Json(e)
    }
}

fn strokes_path(page: Uuid) -> String {
    format!("pages/{page}.strokes")
}

fn pdf_path(source: Id) -> String {
    format!("pdfs/{source}.pdf")
}

/// Whether any page draws the PDF source with this id.
fn is_used(doc: &Document, source: Id) -> bool {
    doc.pages
        .iter()
        .any(|p| p.pdf.is_some_and(|pdf| pdf.source == source))
}

/// Writes `doc` as a `.grapho` archive.
pub fn save<W: Write + Seek>(doc: &Document, writer: W) -> Result<(), FormatError> {
    let mut zip = ZipWriter::new(writer);
    let opts = SimpleFileOptions::default().compression_method(CompressionMethod::Deflated);

    // A source no page shows is dropped, so removing the last page of a PDF
    // does not leave its bytes in the file.
    let used: Vec<_> = doc.pdfs.iter().filter(|s| is_used(doc, s.id)).collect();
    let pictures = images::used(doc);

    zip.start_file(MANIFEST, opts)?;
    serde_json::to_writer_pretty(
        &mut zip,
        &Manifest {
            format: "grapho".into(),
            version: if texts::any(doc) {
                FORMAT_VERSION
            } else if !pictures.is_empty() {
                IMAGE_VERSION
            } else if !used.is_empty() {
                PDF_VERSION
            } else {
                BASE_VERSION
            },
        },
    )?;

    zip.start_file(DOCUMENT, opts)?;
    let mut json = serde_json::to_value(doc)?;
    if let Some(object) = json.as_object_mut() {
        object.remove("pdfs");
        object.remove("images");
        if !pictures.is_empty() {
            object.insert("images".into(), serde_json::to_value(&pictures)?);
        }
        if !used.is_empty() {
            object.insert("pdfs".into(), serde_json::to_value(&used)?);
        }
    }
    serde_json::to_writer_pretty(&mut zip, &json)?;

    for page in &doc.pages {
        zip.start_file(strokes_path(page.id), opts)?;
        zip.write_all(&encode_strokes(page))?;
    }

    // PDFs and pictures are already compressed, so storing them keeps saving
    // quick.
    let stored = SimpleFileOptions::default().compression_method(CompressionMethod::Stored);
    for source in used {
        zip.start_file(pdf_path(source.id), stored)?;
        zip.write_all(&source.bytes)?;
    }

    for source in pictures {
        zip.start_file(images::path(source), stored)?;
        zip.write_all(&source.bytes)?;
    }

    zip.finish()?;
    Ok(())
}

/// Reads a `.grapho` archive.
pub fn load<R: Read + Seek>(reader: R) -> Result<Document, FormatError> {
    let (mut zip, mut doc) = open(reader)?;
    for page in &mut doc.pages {
        let mut bytes = Vec::new();
        zip.by_name(&strokes_path(page.id))?
            .read_to_end(&mut bytes)?;
        for (layer, stroke) in decode_strokes(&bytes, page.layers.len())? {
            page.layers[layer].strokes.push(stroke);
        }
    }
    for source in &mut doc.pdfs {
        let mut bytes = Vec::new();
        match zip.by_name(&pdf_path(source.id)) {
            Ok(mut f) => f.read_to_end(&mut bytes)?,
            Err(zip::result::ZipError::FileNotFound) => {
                return Err(FormatError::Corrupt(format!(
                    "the PDF {} is missing from the file",
                    source.name
                )))
            }
            Err(e) => return Err(e.into()),
        };
        source.bytes = bytes.into();
    }
    read_images(&mut zip, &mut doc)?;
    Ok(doc)
}

/// Reads only a document's structure (title, canvas, pages, layers and the
/// list of PDFs and pictures), leaving every layer without strokes and every
/// PDF and picture without its bytes.
/// Much faster than [`load`] for listing notebooks.
pub fn load_info<R: Read + Seek>(reader: R) -> Result<Document, FormatError> {
    Ok(open(reader)?.1)
}

fn open<R: Read + Seek>(reader: R) -> Result<(ZipArchive<R>, Document), FormatError> {
    let mut zip = ZipArchive::new(reader)?;

    let manifest: Manifest = match zip.by_name(MANIFEST) {
        Ok(f) => serde_json::from_reader(f)?,
        Err(zip::result::ZipError::FileNotFound) => return Err(FormatError::NotGrapho),
        Err(e) => return Err(e.into()),
    };
    if manifest.format != "grapho" {
        return Err(FormatError::NotGrapho);
    }
    if manifest.version > FORMAT_VERSION {
        return Err(FormatError::UnsupportedVersion(manifest.version));
    }

    let doc: Document = serde_json::from_reader(zip.by_name(DOCUMENT)?)?;
    if doc.pages.is_empty() {
        return Err(FormatError::Corrupt("the document has no pages".into()));
    }
    if doc.pages.iter().any(|p| p.layers.is_empty()) {
        return Err(FormatError::Corrupt("a page has no layers".into()));
    }
    for pdf in doc.pages.iter().filter_map(|p| p.pdf) {
        match doc.pdfs.iter().find(|s| s.id == pdf.source) {
            None => {
                return Err(FormatError::Corrupt(
                    "a page refers to a PDF that the file doesn't list".into(),
                ))
            }
            Some(s) if pdf.index >= s.pages => {
                return Err(FormatError::Corrupt(format!(
                    "a page refers to page {} of {}, which has {}",
                    pdf.index + 1,
                    s.name,
                    s.pages
                )))
            }
            Some(_) => {}
        }
    }
    check_images(&doc)?;
    check_texts(&doc)?;
    Ok((zip, doc))
}

#[cfg(test)]
pub(super) mod tests {
    use std::io::Cursor as IoCursor;

    use super::*;
    use crate::document::{Orientation, PaperSize, Stroke, StrokeStyle, Tool};
    use crate::{InputSample, LineStyle};

    pub(in crate::document) fn sample_doc() -> Document {
        let mut d = Document::new_paged("Biology", PaperSize::A4, Orientation::Portrait);
        d.add_page();
        let style = StrokeStyle {
            tool: Tool::Pen,
            color: 0x2346_D6FF,
            width: 1.5,
            line: LineStyle::Solid,
        };
        for page in &mut d.pages {
            let top = page.add_layer("Highlights");
            for (i, layer) in [page.layers[0].id, top].into_iter().enumerate() {
                let pts = (0..30)
                    .map(|k| InputSample::new(k as f32, i as f32 * 10.0, 0.6, k as f32 * 4.0))
                    .collect();
                page.layer_mut(layer).unwrap().strokes.push(Stroke::new(
                    style,
                    1_760_000_000_000,
                    pts,
                ));
            }
        }
        d
    }

    pub(in crate::document) fn to_bytes(doc: &Document) -> Vec<u8> {
        let mut buf = IoCursor::new(Vec::new());
        save(doc, &mut buf).unwrap();
        buf.into_inner()
    }

    #[test]
    fn round_trip_keeps_everything() {
        let doc = sample_doc();
        let loaded = load(IoCursor::new(to_bytes(&doc))).unwrap();
        assert_eq!(loaded, doc);
        assert_eq!(loaded.pages[1].layers[1].strokes[0].points.len(), 30);
    }

    #[test]
    fn layer_opacity_survives_a_round_trip() {
        let mut doc = sample_doc();
        doc.pages[0].layers[1].opacity = 0.35;
        let loaded = load(IoCursor::new(to_bytes(&doc))).unwrap();
        assert_eq!(loaded.pages[0].layers[1].opacity, 0.35);
        assert_eq!(loaded.pages[0].layers[0].opacity, 1.0);
    }

    #[test]
    fn info_skips_the_ink() {
        let doc = sample_doc();
        let info = load_info(IoCursor::new(to_bytes(&doc))).unwrap();
        assert_eq!(info.title, "Biology");
        assert_eq!(info.pages.len(), 2);
        assert_eq!(info.pages[0].stroke_count(), 0);
    }

    #[test]
    fn infinite_canvas_round_trips() {
        let doc = Document::new_infinite("Whiteboard");
        assert_eq!(load(IoCursor::new(to_bytes(&doc))).unwrap(), doc);
    }

    #[test]
    fn refuses_newer_versions() {
        let mut buf = IoCursor::new(Vec::new());
        let mut zip = ZipWriter::new(&mut buf);
        zip.start_file(MANIFEST, SimpleFileOptions::default())
            .unwrap();
        zip.write_all(br#"{"format":"grapho","version":99}"#)
            .unwrap();
        zip.finish().unwrap();
        let err = load(IoCursor::new(buf.into_inner())).unwrap_err();
        assert!(matches!(err, FormatError::UnsupportedVersion(99)));
    }

    #[test]
    fn rejects_other_zip_files() {
        let mut buf = IoCursor::new(Vec::new());
        let mut zip = ZipWriter::new(&mut buf);
        zip.start_file("hello.txt", SimpleFileOptions::default())
            .unwrap();
        zip.finish().unwrap();
        let err = load(IoCursor::new(buf.into_inner())).unwrap_err();
        assert!(matches!(err, FormatError::NotGrapho));
    }
}

#[cfg(test)]
mod image_tests;
#[cfg(test)]
mod pdf_tests;
#[cfg(test)]
mod text_tests;
