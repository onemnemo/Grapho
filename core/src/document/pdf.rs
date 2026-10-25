//! Notes built around PDF files: importing one as a new note, and turning a
//! PDF into note pages.

use std::sync::Arc;

use super::{Document, Id, Orientation, Page, PageSize, PaperSize, PdfPage, PdfSource, MM_TO_PT};
use crate::pdf::{read_page_sizes, PdfError};

/// A PDF read and ready to join a note: its source record and one page per
/// PDF page, each sized like it. Reading is the slow part of an import, so it
/// is a separate step that needs no access to a document.
#[derive(Debug)]
pub struct PdfImport {
    pub(super) source: PdfSource,
    pub(super) pages: Vec<Page>,
}

impl PdfImport {
    /// Reads `bytes` as a PDF. `name` is the original file name.
    pub fn read(name: &str, bytes: Vec<u8>) -> Result<Self, PdfError> {
        let sizes = read_page_sizes(&bytes)?;
        let source = PdfSource {
            id: Id::new_v4(),
            name: name.to_owned(),
            pages: sizes.len() as u32,
            bytes: Arc::new(bytes),
        };
        let pages = sizes
            .into_iter()
            .zip(0u32..)
            .map(|(size, index)| pdf_page(source.id, index, size))
            .collect();
        Ok(Self { source, pages })
    }

    /// How many pages the import adds.
    pub fn page_count(&self) -> usize {
        self.pages.len()
    }
}

fn pdf_page(source: Id, index: u32, size: PageSize) -> Page {
    let mut page = Page::with_size(size);
    page.pdf = Some(PdfPage { source, index });
    page
}

impl Document {
    /// A paged note with one page per page of the PDF, each drawn over its
    /// PDF page. `name` is the original file name. The paper is the first
    /// page's size (portrait orientation), white and blank, which only
    /// matters for pages added later.
    pub fn from_pdf(
        title: impl Into<String>,
        name: &str,
        bytes: Vec<u8>,
    ) -> Result<Self, PdfError> {
        let PdfImport { source, pages } = PdfImport::read(name, bytes)?;
        let first = pages[0].size.unwrap_or(PageSize {
            width: 595.0,
            height: 842.0,
        });
        let mut doc = Document::new_paged(
            title,
            PaperSize::Custom {
                width_mm: first.width / MM_TO_PT,
                height_mm: first.height / MM_TO_PT,
            },
            Orientation::Portrait,
        );
        doc.pages = pages;
        doc.pdfs = vec![source];
        Ok(doc)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::{Background, Canvas};
    use crate::pdf::fixtures::pdf_of;

    #[test]
    fn one_page_per_pdf_page_with_its_size_and_source() {
        let bytes = pdf_of(&[(612.0, 792.0), (800.0, 450.0)]);
        let doc = Document::from_pdf("Slides", "slides.pdf", bytes).unwrap();
        assert_eq!(doc.pages.len(), 2);
        assert_eq!(doc.pdfs.len(), 1);
        assert_eq!(doc.pdfs[0].name, "slides.pdf");
        assert_eq!(doc.pdfs[0].pages, 2);
        for (i, page) in doc.pages.iter().enumerate() {
            let pdf = page.pdf.unwrap();
            assert_eq!((pdf.source, pdf.index), (doc.pdfs[0].id, i as u32));
            assert_eq!(page.layers.len(), 1);
            assert_eq!(page.layers[0].name, "Layer 1");
        }
        assert_eq!(doc.page_size(1), Some((800.0, 450.0)));
    }

    #[test]
    fn the_paper_follows_the_first_page_and_stays_white() {
        let doc = Document::from_pdf("Slides", "s.pdf", pdf_of(&[(720.0, 360.0)])).unwrap();
        let Canvas::Paged {
            paper,
            orientation,
            background,
        } = doc.canvas
        else {
            panic!("a pdf note is paged");
        };
        let PaperSize::Custom {
            width_mm,
            height_mm,
        } = paper
        else {
            panic!("the paper is custom");
        };
        assert!((width_mm - 254.0).abs() < 0.01 && (height_mm - 127.0).abs() < 0.01);
        assert_eq!(orientation, Orientation::Portrait);
        assert_eq!(background, Background::default());
        assert_eq!(background.color, 0xFFFF_FFFF);
    }

    #[test]
    fn the_json_carries_sizes_pdf_pages_and_sources_but_no_bytes() {
        let doc = Document::from_pdf("S", "lecture.pdf", pdf_of(&[(595.5, 842.0)])).unwrap();
        let json = serde_json::to_value(&doc).unwrap();
        let page = &json["pages"][0];
        assert_eq!(page["size"]["width"], 595.5);
        assert_eq!(page["size"]["height"], 842.0);
        assert_eq!(page["pdf"]["source"], doc.pdfs[0].id.to_string());
        assert_eq!(page["pdf"]["index"], 0);
        let source = json["pdfs"][0].as_object().unwrap();
        assert_eq!(source["name"], "lecture.pdf");
        assert_eq!(source["pages"], 1);
        assert_eq!(source.len(), 3, "id, name and pages only");
    }

    #[test]
    fn a_bad_pdf_makes_no_document() {
        assert!(Document::from_pdf("x", "x.pdf", b"nope".to_vec()).is_err());
    }
}
