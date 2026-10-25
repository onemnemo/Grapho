//! Page edits: inserting a blank page, or all pages of a PDF, between the
//! pages of a paged note. Each one is a single undo step.

use super::{Editor, Step};
use crate::document::{Canvas, Page, PdfImport};
use crate::pdf::PdfError;

impl Editor {
    /// Inserts an empty page so that it ends up at index `at` (`0` to the
    /// page count, larger values count as the end) and returns that index.
    /// It takes the size of the page before it, or of the page after it when
    /// inserted first, but never a PDF background. Infinite canvases have one
    /// page only, so this returns `None` for them.
    pub fn insert_page(&mut self, at: usize) -> Option<usize> {
        self.end_erase();
        if matches!(self.doc.canvas, Canvas::Infinite { .. }) {
            return None;
        }
        let at = at.min(self.doc.pages.len());
        let neighbour = self.doc.pages.get(at.saturating_sub(1))?;
        let page = Page {
            size: neighbour.size,
            ..Page::new()
        };
        self.doc.pages.insert(at, page);
        self.record(Step::RemovePage { index: at });
        Some(at)
    }

    /// Inserts every page of a PDF so that the first lands at index `at`
    /// (clamped like [`Editor::insert_page`]) and returns how many pages
    /// were added. `name` is the original file name. A bad PDF, or an
    /// infinite canvas, changes nothing. See [`Editor::insert_pdf_import`].
    pub fn insert_pdf(&mut self, at: usize, name: &str, bytes: Vec<u8>) -> Result<usize, PdfError> {
        self.insert_pdf_import(at, PdfImport::read(name, bytes)?)
    }

    /// Like [`Editor::insert_pdf`] for a PDF that was already read, so the
    /// slow parse can happen without holding the document. The PDF is kept
    /// in the note, and adding it and its pages is one undo step.
    pub fn insert_pdf_import(&mut self, at: usize, import: PdfImport) -> Result<usize, PdfError> {
        self.end_erase();
        if matches!(self.doc.canvas, Canvas::Infinite { .. }) {
            return Err(PdfError::NotPaged);
        }
        let count = import.page_count();
        let step = Step::InsertPages {
            index: at.min(self.doc.pages.len()),
            pages: import.pages,
            source: Some(import.source),
        };
        let (inverse, _) = self.apply_page_run(step).ok_or(PdfError::NoPages)?;
        self.record(inverse);
        Ok(count)
    }

    /// Applies a step that adds or removes a run of pages and returns its
    /// reverse and the page it touched.
    pub(super) fn apply_page_run(&mut self, step: Step) -> Option<(Step, usize)> {
        match step {
            Step::InsertPages {
                index,
                pages,
                source,
            } => {
                let index = index.min(self.doc.pages.len());
                let count = pages.len();
                let source_id = source.as_ref().map(|s| s.id);
                self.doc.pages.splice(index..index, pages);
                if let Some(source) = source {
                    self.doc.pdfs.push(source);
                }
                Some((
                    Step::RemovePages {
                        index,
                        count,
                        source: source_id,
                    },
                    index,
                ))
            }
            Step::RemovePages {
                index,
                count,
                source,
            } => {
                let end = index.checked_add(count)?;
                // A document always keeps at least one page.
                if count == 0 || end > self.doc.pages.len() || count >= self.doc.pages.len() {
                    return None;
                }
                let pages: Vec<Page> = self.doc.pages.drain(index..end).collect();
                let source = source.and_then(|id| {
                    let at = self.doc.pdfs.iter().position(|s| s.id == id)?;
                    Some(self.doc.pdfs.remove(at))
                });
                let shown = index.min(self.doc.pages.len() - 1);
                Some((
                    Step::InsertPages {
                        index,
                        pages,
                        source,
                    },
                    shown,
                ))
            }
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::{Document, Orientation, PageSize, PaperSize, Stroke, StrokeStyle, Tool};
    use crate::pdf::fixtures::pdf_of;
    use crate::{InputSample, LineStyle};

    fn editor() -> Editor {
        Editor::new(Document::new_paged(
            "Notes",
            PaperSize::A4,
            Orientation::Portrait,
        ))
    }

    fn pdf_editor() -> Editor {
        let bytes = pdf_of(&[(612.0, 792.0), (800.0, 450.0)]);
        Editor::new(Document::from_pdf("Slides", "slides.pdf", bytes).unwrap())
    }

    fn ids(e: &Editor) -> Vec<crate::document::Id> {
        e.document().pages.iter().map(|p| p.id).collect()
    }

    fn mark(e: &mut Editor, page: usize) {
        let style = StrokeStyle {
            tool: Tool::Pen,
            color: 0x1718_1CFF,
            width: 2.0,
            line: LineStyle::Solid,
        };
        let pts = vec![InputSample::new(1.0, 1.0, 1.0, 0.0)];
        assert!(e.add_stroke(page, Stroke::new(style, 0, pts)).is_some());
    }

    #[test]
    fn a_blank_page_goes_in_at_the_index_and_undoes() {
        let mut e = editor();
        e.add_page();
        let before = ids(&e);
        assert_eq!(e.insert_page(1), Some(1));
        let after = ids(&e);
        assert_eq!(after.len(), 3);
        assert_eq!((after[0], after[2]), (before[0], before[1]));
        assert_eq!(e.undo(), Some(1));
        assert_eq!(ids(&e), before);
        assert_eq!(e.redo(), Some(1));
        assert_eq!(ids(&e), after);
    }

    #[test]
    fn a_blank_page_can_go_first_or_last_and_clamps() {
        let mut e = editor();
        assert_eq!(e.insert_page(0), Some(0));
        assert_eq!(e.insert_page(99), Some(2));
        assert_eq!(e.document().pages.len(), 3);
    }

    #[test]
    fn a_blank_page_copies_the_neighbour_size_without_the_pdf() {
        let mut e = pdf_editor();
        assert_eq!(e.insert_page(2), Some(2));
        let size = Some(PageSize {
            width: 800.0,
            height: 450.0,
        });
        assert_eq!(e.document().pages[2].size, size);
        assert!(e.document().pages[2].pdf.is_none());
        // At the front it copies the page after it.
        assert_eq!(e.insert_page(0), Some(0));
        assert_eq!(e.document().pages[0].size.unwrap().width, 612.0);
        assert!(e.document().pages[0].pdf.is_none());
        assert_eq!(e.document().page_size(0), Some((612.0, 792.0)));
    }

    #[test]
    fn inserting_keeps_ink_on_other_pages() {
        let mut e = editor();
        mark(&mut e, 0);
        e.insert_page(0);
        assert_eq!(e.document().pages[1].stroke_count(), 1);
        e.undo();
        assert_eq!(e.document().pages[0].stroke_count(), 1);
    }

    #[test]
    fn infinite_canvases_refuse_both() {
        let mut e = Editor::new(Document::new_infinite("Board"));
        assert_eq!(e.insert_page(0), None);
        let pdf = pdf_of(&[(100.0, 100.0)]);
        assert_eq!(e.insert_pdf(0, "a.pdf", pdf), Err(PdfError::NotPaged));
        assert!(!e.can_undo());
        assert!(e.document().pdfs.is_empty());
    }

    #[test]
    fn a_pdf_goes_in_as_one_undo_step() {
        let mut e = editor();
        e.add_page();
        let before = ids(&e);
        let pdf = pdf_of(&[(612.0, 792.0), (300.0, 400.0), (500.0, 500.0)]);
        assert_eq!(e.insert_pdf(1, "notes.pdf", pdf), Ok(3));
        let doc = e.document();
        assert_eq!(doc.pages.len(), 5);
        assert_eq!(doc.pdfs.len(), 1);
        assert_eq!((doc.pages[0].id, doc.pages[4].id), (before[0], before[1]));
        for (i, page) in doc.pages[1..4].iter().enumerate() {
            let pdf = page.pdf.unwrap();
            assert_eq!((pdf.source, pdf.index), (doc.pdfs[0].id, i as u32));
        }
        assert_eq!(doc.page_size(2), Some((300.0, 400.0)));
        let added = ids(&e);

        assert_eq!(e.undo(), Some(1));
        assert_eq!(ids(&e), before);
        assert!(e.document().pdfs.is_empty());
        assert!(e.can_undo(), "the earlier add_page is still on the stack");
        assert!(e.can_redo());

        assert_eq!(e.redo(), Some(1));
        assert_eq!(ids(&e), added);
        assert_eq!(e.document().pdfs.len(), 1);
        assert_eq!(e.document().pdfs[0].pages, 3);
        assert!(!e.document().pdfs[0].bytes.is_empty());
    }

    #[test]
    fn a_pdf_can_go_first_and_last() {
        let mut e = editor();
        assert_eq!(e.insert_pdf(0, "a.pdf", pdf_of(&[(200.0, 200.0)])), Ok(1));
        assert_eq!(e.insert_pdf(99, "b.pdf", pdf_of(&[(300.0, 300.0)])), Ok(1));
        let doc = e.document();
        assert_eq!(doc.pages.len(), 3);
        assert!(doc.pages[0].pdf.is_some() && doc.pages[1].pdf.is_none());
        assert_eq!(doc.pages[2].size.unwrap().width, 300.0);
        assert_eq!(doc.pdfs.len(), 2);
    }

    #[test]
    fn a_bad_pdf_changes_nothing() {
        let mut e = editor();
        assert!(matches!(
            e.insert_pdf(0, "bad.pdf", b"nope".to_vec()),
            Err(PdfError::Damaged(_))
        ));
        assert_eq!(e.document().pages.len(), 1);
        assert!(!e.can_undo());
    }

    #[test]
    fn undo_keeps_ink_drawn_on_pdf_pages_before_it() {
        let mut e = pdf_editor();
        mark(&mut e, 1);
        e.insert_page(1);
        e.undo();
        e.undo();
        assert_eq!(e.document().pages[1].stroke_count(), 0);
        e.redo();
        assert_eq!(e.document().pages[1].stroke_count(), 1);
        assert_eq!(e.document().pages.len(), 2);
    }

    #[test]
    fn a_run_of_pages_is_never_removed_down_to_nothing() {
        let mut e = pdf_editor();
        let step = Step::RemovePages {
            index: 0,
            count: 2,
            source: None,
        };
        assert!(e.apply_page_run(step).is_none());
        assert_eq!(e.document().pages.len(), 2);
    }

    #[test]
    fn fill_uses_the_size_of_a_pdf_page() {
        use crate::document::FillView;
        let mut e = pdf_editor();
        // Page 1 is 800 by 450 points. A tap inside it is walled in by the
        // page edges, so an empty page fills as a whole.
        let view = FillView {
            scale: 1.0,
            visible: (0.0, 0.0, 800.0, 450.0),
        };
        assert!(e.fill(1, 700.0, 400.0, 0xFF00_00FF, view).is_some());
        let fill = &e.document().pages[1].layers[0].strokes[0];
        let (x0, y0, x1, y1) = fill.bounds().unwrap();
        assert!(
            x1 > 700.0 && x1 <= 800.5,
            "fill reaches the page edge: {x1}"
        );
        assert!(
            y1 > 400.0 && y1 <= 450.5,
            "fill reaches the page edge: {y1}"
        );
        assert!(x0 >= -0.5 && y0 >= -0.5);
    }
}
