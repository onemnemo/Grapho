//! Exporting a note as a vector PDF that looks like the screen.
//!
//! Every stroke becomes filled paths built from the same outlines the
//! screen draws. With backgrounds on, PDF pages keep their original content
//! (copied, not rendered) and other pages get their paper colour and
//! pattern. With backgrounds off every page is white with only the ink.
//! Images are embedded once and drawn under the strokes of their layer.

mod colour;
mod content;
mod image;
mod ink;
mod page;
mod paper;
mod source;
#[cfg(test)]
mod tests;

use std::fmt;

use lopdf::{dictionary, Document as Pdf, Object, StringFormat};

use crate::document::Document;
use page::Builder;

/// What to export.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ExportOptions {
    /// Indices of the pages to export, in any order and without repeats
    /// mattering: they come out in document order. `None` is every page.
    /// An infinite canvas has the single page 0.
    pub pages: Option<Vec<usize>>,
    /// Keep the paper colour, the pattern and the original content of PDF
    /// pages. Off gives white pages with ink only.
    pub backgrounds: bool,
}

impl Default for ExportOptions {
    fn default() -> Self {
        Self {
            pages: None,
            backgrounds: true,
        }
    }
}

/// Why a note could not be exported.
#[derive(Debug, PartialEq, Eq)]
pub enum ExportError {
    /// No page was selected.
    NoPages,
    /// A selected page does not exist.
    PageOutOfRange(usize),
    /// A page refers to a PDF the note does not hold.
    MissingSource,
    /// An embedded PDF could not be read.
    Source(String),
    /// An embedded picture could not be read.
    Image(String),
    /// The output could not be written.
    Write(String),
}

impl fmt::Display for ExportError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ExportError::NoPages => write!(f, "There are no pages to export"),
            ExportError::PageOutOfRange(i) => write!(f, "Page {} doesn't exist", i + 1),
            ExportError::MissingSource => write!(f, "A page's PDF is missing from this note"),
            ExportError::Source(why) => write!(f, "An embedded PDF can't be read: {why}"),
            ExportError::Image(why) => write!(f, "An embedded picture can't be used: {why}"),
            ExportError::Write(why) => write!(f, "The PDF can't be written: {why}"),
        }
    }
}

impl std::error::Error for ExportError {}

/// Writes the note as a PDF, one page per exported page at the page's own
/// size in points. An infinite canvas becomes one page that fits its ink
/// with a margin, or A4 when it has none. Hidden layers are left out and
/// faded layers keep their opacity.
pub fn export_pdf(doc: &Document, options: &ExportOptions) -> Result<Vec<u8>, ExportError> {
    let count = doc.pages.len();
    let picked = match &options.pages {
        None => (0..count).collect(),
        Some(wanted) => {
            let mut wanted = wanted.clone();
            wanted.sort_unstable();
            wanted.dedup();
            if let Some(&bad) = wanted.iter().find(|&&i| i >= count) {
                return Err(ExportError::PageOutOfRange(bad));
            }
            wanted
        }
    };
    if picked.is_empty() {
        return Err(ExportError::NoPages);
    }

    let mut builder = Builder::new(doc, options.backgrounds);
    for index in picked {
        builder.add_page(index)?;
    }
    let mut pdf = builder.finish();
    let info = pdf.add_object(dictionary! {
        "Producer" => text_string("Grapho"),
        "Title" => text_string(&doc.title),
    });
    pdf.trailer.set("Info", info);
    pdf.compress();
    let mut bytes = Vec::new();
    pdf.save_to(&mut bytes)
        .map_err(|e| ExportError::Write(e.to_string()))?;
    Ok(bytes)
}

/// A PDF text string: plain when it is ASCII, else UTF-16 with a byte order
/// mark.
fn text_string(text: &str) -> Object {
    if text.is_ascii() {
        return Object::string_literal(text);
    }
    let mut bytes = vec![0xFE, 0xFF];
    for unit in text.encode_utf16() {
        bytes.extend_from_slice(&unit.to_be_bytes());
    }
    Object::String(bytes, StringFormat::Hexadecimal)
}

/// An empty output file with a page tree root reserved.
fn new_pdf() -> (Pdf, lopdf::ObjectId) {
    let mut pdf = Pdf::with_version("1.5");
    let pages = pdf.new_object_id();
    (pdf, pages)
}
