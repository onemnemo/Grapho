//! Editing with undo and redo.
//!
//! Every change goes through an [`Editor`], which records how to reverse it.
//! Undoing a change applies its reverse and records the reverse of *that*
//! for redo, so both stacks hold the same kind of step.

mod diagram;
mod erase;
mod images;
mod layers;
mod order;
mod pages;
mod select;
mod texts;

pub use layers::MAX_LAYERS;

pub use erase::EraseMode;

use std::collections::HashMap;

use super::{
    find_fill_region, search_area, Document, FillView, Id, Image, Layer, Page, PdfSource, Stroke,
    StrokeStyle, TextBox, Tool,
};
use crate::InputSample;

/// One reversible step.
#[derive(Clone, Debug)]
enum Step {
    /// Put strokes back, in this order, at the given positions.
    InsertStrokes {
        page: Id,
        strokes: Vec<Placed>,
    },
    /// Take strokes out, in this order.
    RemoveStrokes {
        page: Id,
        ids: Vec<Id>,
    },
    /// Take the strokes out, then put these in, in this order. Inserted in
    /// ascending position, each lands where an original sat.
    ReplaceStrokes {
        page: Id,
        remove: Vec<Id>,
        insert: Vec<Placed>,
    },
    /// Swap the strokes of whole layers, which restores a layer the area
    /// eraser cut up. Applying it again swaps back.
    SwapLayers {
        page: Id,
        layers: Vec<(Id, Vec<Stroke>)>,
    },
    /// Put images back, in this order, at the given positions.
    InsertImages {
        page: Id,
        images: Vec<PlacedImage>,
    },
    /// Take images out, in this order.
    RemoveImages {
        page: Id,
        ids: Vec<Id>,
    },
    /// Take the images out, then put these in, in this order, like
    /// `ReplaceStrokes`.
    ReplaceImages {
        page: Id,
        remove: Vec<Id>,
        insert: Vec<PlacedImage>,
    },
    /// Put text boxes back, in this order, at the given positions.
    InsertTexts {
        page: Id,
        texts: Vec<PlacedText>,
    },
    /// Take text boxes out, in this order.
    RemoveTexts {
        page: Id,
        ids: Vec<Id>,
    },
    /// Swap text boxes for new versions with the same ids, in place. Its
    /// reverse holds the old versions.
    UpdateTexts {
        page: Id,
        texts: Vec<TextBox>,
    },
    /// Put the strokes, images and text boxes of layers into the given orders. Its
    /// reverse holds the orders they had.
    SetOrder {
        page: Id,
        layers: Vec<order::LayerOrder>,
    },
    /// Steps that make one undo step, applied in order. Its reverse holds
    /// the reverses of the parts in the opposite order.
    Group(Vec<Step>),
    /// Put a layer back at a position of the page's layer list, and make it
    /// the active layer.
    InsertLayer {
        page: Id,
        index: usize,
        layer: Layer,
    },
    /// Take the layer at a position out, with its strokes.
    RemoveLayer {
        page: Id,
        index: usize,
    },
    MoveLayer {
        page: Id,
        from: usize,
        to: usize,
    },
    /// Append the strokes of the layer at `index` to the layer below it and
    /// remove the upper layer.
    MergeDown {
        page: Id,
        index: usize,
    },
    /// Undo of a merge: cut the last `count` strokes, `image_count` images
    /// and `text_count` text boxes off the layer below `index` and put them
    /// back in `layer` (which holds none) at `index`.
    Unmerge {
        page: Id,
        index: usize,
        layer: Layer,
        count: usize,
        image_count: usize,
        text_count: usize,
    },
    InsertPage {
        index: usize,
        page: Page,
    },
    RemovePage {
        index: usize,
    },
    /// Put a run of pages back at a position, with the PDF they came with.
    InsertPages {
        index: usize,
        pages: Vec<Page>,
        source: Option<PdfSource>,
    },
    /// Take `count` pages out from a position, and the PDF source that only
    /// they used.
    RemovePages {
        index: usize,
        count: usize,
        source: Option<Id>,
    },
}

/// A stroke and where it sits: its layer and its position in that layer.
#[derive(Clone, Debug)]
struct Placed {
    layer: Id,
    index: usize,
    stroke: Stroke,
}

/// A step that swaps items for copies, not yet applied, with the pairs of
/// old and new ids.
type Replacement = (Step, Vec<(Id, Id)>);

/// An image and where it sits: its layer and its position in that layer.
#[derive(Clone, Debug)]
struct PlacedImage {
    layer: Id,
    index: usize,
    image: Image,
}

/// A text box and where it sits: its layer and its position in that layer.
#[derive(Clone, Debug)]
struct PlacedText {
    layer: Id,
    index: usize,
    text: TextBox,
}

/// Strokes, images and text boxes copied for pasting.
#[derive(Debug, Default)]
struct Clipboard {
    strokes: Vec<Stroke>,
    images: Vec<Image>,
    texts: Vec<TextBox>,
}

impl Clipboard {
    fn is_empty(&self) -> bool {
        self.strokes.is_empty() && self.images.is_empty() && self.texts.is_empty()
    }
}

/// A document plus its undo history.
#[derive(Debug)]
pub struct Editor {
    doc: Document,
    undo: Vec<Step>,
    redo: Vec<Step>,
    erasing: Option<erase::Erasing>,
    /// Not undoable and not saved.
    clipboard: Clipboard,
    /// The layer new ink goes to, per page. Not saved in the file.
    active: HashMap<Id, Id>,
    /// Every page the last undo or redo of a grouped step touched.
    history_pages: Vec<usize>,
}

/// Undo history is capped so a long session can't use unbounded memory.
const MAX_UNDO: usize = 200;

impl Editor {
    pub fn new(doc: Document) -> Self {
        Self {
            doc,
            undo: Vec::new(),
            redo: Vec::new(),
            erasing: None,
            clipboard: Clipboard::default(),
            active: HashMap::new(),
            history_pages: Vec::new(),
        }
    }

    pub fn document(&self) -> &Document {
        &self.doc
    }

    /// Changes that don't take part in undo, such as renaming.
    pub fn document_mut(&mut self) -> &mut Document {
        &mut self.doc
    }

    pub fn can_undo(&self) -> bool {
        !self.undo.is_empty()
    }

    pub fn can_redo(&self) -> bool {
        !self.redo.is_empty()
    }

    /// Adds a stroke to the active layer of a page. Returns `None` if the
    /// page doesn't exist or the active layer is locked or hidden.
    pub fn add_stroke(&mut self, page: usize, stroke: Stroke) -> Option<Id> {
        self.end_erase();
        let (page_id, layer) = self.drawing_target(page)?;
        let id = self.doc.add_stroke(page_id, layer, stroke).ok()?;
        self.record(Step::RemoveStrokes {
            page: page_id,
            ids: vec![id],
        });
        Some(id)
    }

    /// Fills the area around `(x, y)` that ink encloses, as one undo step.
    /// The search area and resolution follow `view` and the canvas, see
    /// [`search_area`]. Returns `None` if nothing is enclosed there,
    /// or the page or its active layer can't take it.
    pub fn fill(&mut self, page: usize, x: f32, y: f32, color: u32, view: FillView) -> Option<Id> {
        self.drawing_target(page)?;
        let p = self.doc.pages.get(page)?;
        let a = search_area(p, x, y, view, self.doc.page_size(page))?;
        let polygon = find_fill_region(p, x, y, a.bounds, a.cell, a.walls)?;
        let style = StrokeStyle {
            tool: Tool::Fill,
            color,
            width: 0.0,
            line: crate::LineStyle::Solid,
        };
        let points = polygon
            .into_iter()
            .map(|(x, y)| InputSample::new(x, y, 1.0, 0.0))
            .collect();
        self.add_stroke(page, Stroke::new(style, 0, points))
    }

    /// Adds an empty page at the end and returns its index. Infinite
    /// canvases have one page only, so this returns `None` for them.
    pub fn add_page(&mut self) -> Option<usize> {
        self.end_erase();
        self.doc.add_page()?;
        let index = self.doc.pages.len() - 1;
        self.record(Step::RemovePage { index });
        Some(index)
    }

    /// Reverses the last change. Returns the index of the page it touched.
    pub fn undo(&mut self) -> Option<usize> {
        self.end_erase();
        self.history_pages.clear();
        let step = self.undo.pop()?;
        let (inverse, page) = self.apply(step)?;
        self.redo.push(inverse);
        Some(page)
    }

    /// Repeats the last undone change. Returns the index of the page it touched.
    pub fn redo(&mut self) -> Option<usize> {
        self.end_erase();
        self.history_pages.clear();
        let step = self.redo.pop()?;
        let (inverse, page) = self.apply(step)?;
        self.undo.push(inverse);
        Some(page)
    }

    /// The pages the last undo or redo changed besides the one it returned,
    /// when it held edits to more than one page, such as a move from one
    /// page to another. May repeat pages.
    pub fn history_pages(&self) -> &[usize] {
        &self.history_pages
    }

    fn record(&mut self, inverse: Step) {
        self.redo.clear();
        self.undo.push(inverse);
        if self.undo.len() > MAX_UNDO {
            self.undo.remove(0);
        }
    }

    fn page_index(&self, id: Id) -> usize {
        self.doc.pages.iter().position(|p| p.id == id).unwrap_or(0)
    }

    /// Applies a step and returns its reverse and the page it touched.
    fn apply(&mut self, step: Step) -> Option<(Step, usize)> {
        match step {
            Step::InsertStrokes { page, strokes } => {
                let mut ids = Vec::with_capacity(strokes.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for s in strokes {
                        if let Some(layer) = p.layer_mut(s.layer) {
                            ids.push(s.stroke.id);
                            let at = s.index.min(layer.strokes.len());
                            layer.strokes.insert(at, s.stroke);
                        }
                    }
                }
                // Removing in reverse insertion order finds every stroke at
                // the position it was put in.
                ids.reverse();
                Some((Step::RemoveStrokes { page, ids }, self.page_index(page)))
            }
            Step::RemoveStrokes { page, ids } => {
                let mut strokes = Vec::with_capacity(ids.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for id in ids {
                        for layer in &mut p.layers {
                            if let Some(index) = layer.strokes.iter().position(|s| s.id == id) {
                                strokes.push(Placed {
                                    layer: layer.id,
                                    index,
                                    stroke: layer.strokes.remove(index),
                                });
                                break;
                            }
                        }
                    }
                }
                strokes.reverse();
                Some((Step::InsertStrokes { page, strokes }, self.page_index(page)))
            }
            Step::ReplaceStrokes {
                page,
                remove,
                insert,
            } => {
                let mut taken = Vec::with_capacity(remove.len());
                let mut added = Vec::with_capacity(insert.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for id in remove {
                        for layer in &mut p.layers {
                            if let Some(index) = layer.strokes.iter().position(|s| s.id == id) {
                                taken.push(Placed {
                                    layer: layer.id,
                                    index,
                                    stroke: layer.strokes.remove(index),
                                });
                                break;
                            }
                        }
                    }
                    for s in insert {
                        if let Some(layer) = p.layer_mut(s.layer) {
                            added.push(s.stroke.id);
                            let at = s.index.min(layer.strokes.len());
                            layer.strokes.insert(at, s.stroke);
                        }
                    }
                }
                // Reversed, as in the plain steps: the way back removes the
                // newest insertion first and re-inserts the last removal first.
                added.reverse();
                taken.reverse();
                Some((
                    Step::ReplaceStrokes {
                        page,
                        remove: added,
                        insert: taken,
                    },
                    self.page_index(page),
                ))
            }
            Step::SwapLayers { page, mut layers } => {
                if let Some(p) = self.doc.page_mut(page) {
                    for (id, strokes) in &mut layers {
                        if let Some(layer) = p.layer_mut(*id) {
                            std::mem::swap(&mut layer.strokes, strokes);
                        }
                    }
                }
                Some((Step::SwapLayers { page, layers }, self.page_index(page)))
            }
            step @ (Step::InsertImages { .. }
            | Step::RemoveImages { .. }
            | Step::ReplaceImages { .. }) => self.apply_image_step(step),
            step @ (Step::InsertTexts { .. }
            | Step::RemoveTexts { .. }
            | Step::UpdateTexts { .. }) => self.apply_text_step(step),
            Step::SetOrder { page, layers } => self.apply_order(page, layers),
            Step::Group(steps) => {
                let mut inverses = Vec::with_capacity(steps.len());
                let mut shown = 0;
                for part in steps {
                    let (inverse, page) = self.apply(part)?;
                    inverses.push(inverse);
                    self.history_pages.push(page);
                    shown = page;
                }
                inverses.reverse();
                Some((Step::Group(inverses), shown))
            }
            step @ (Step::InsertLayer { .. }
            | Step::RemoveLayer { .. }
            | Step::MoveLayer { .. }
            | Step::MergeDown { .. }
            | Step::Unmerge { .. }) => self.apply_layer_step(step),
            Step::InsertPage { index, page } => {
                let index = index.min(self.doc.pages.len());
                self.doc.pages.insert(index, page);
                Some((Step::RemovePage { index }, index))
            }
            step @ (Step::InsertPages { .. } | Step::RemovePages { .. }) => {
                self.apply_page_run(step)
            }
            Step::RemovePage { index } => {
                // A document always keeps at least one page.
                if self.doc.pages.len() <= 1 || index >= self.doc.pages.len() {
                    return None;
                }
                let page = self.doc.pages.remove(index);
                let shown = index.min(self.doc.pages.len() - 1);
                Some((Step::InsertPage { index, page }, shown))
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::{Orientation, PaperSize};

    fn line(y: f32) -> Stroke {
        Stroke::new(
            StrokeStyle {
                tool: Tool::Pen,
                color: 0x1718_1CFF,
                width: 2.0,
                line: crate::LineStyle::Solid,
            },
            0,
            vec![
                InputSample::new(0.0, y, 1.0, 0.0),
                InputSample::new(100.0, y, 1.0, 10.0),
            ],
        )
    }

    fn ys(e: &Editor) -> Vec<f32> {
        e.document().pages[0].layers[0]
            .strokes
            .iter()
            .map(|s| s.points[0].y)
            .collect()
    }

    fn editor() -> Editor {
        Editor::new(Document::new_paged(
            "Notes",
            PaperSize::A4,
            Orientation::Portrait,
        ))
    }

    #[test]
    fn undo_and_redo_a_stroke() {
        let mut e = editor();
        assert!(!e.can_undo());
        e.add_stroke(0, line(10.0)).unwrap();
        e.add_stroke(0, line(20.0)).unwrap();
        assert_eq!(e.undo(), Some(0));
        assert_eq!(ys(&e), vec![10.0]);
        assert!(e.can_redo());
        e.redo();
        assert_eq!(ys(&e), vec![10.0, 20.0]);
        assert!(!e.can_redo());
    }

    #[test]
    fn a_new_change_clears_redo() {
        let mut e = editor();
        e.add_stroke(0, line(10.0));
        e.undo();
        e.add_stroke(0, line(30.0));
        assert!(!e.can_redo());
        assert_eq!(ys(&e), vec![30.0]);
    }

    #[test]
    fn erasing_is_one_step_and_undo_restores_the_order() {
        let mut e = editor();
        for y in [10.0, 20.0, 30.0, 40.0] {
            e.add_stroke(0, line(y));
        }
        e.begin_erase(0, EraseMode::Stroke);
        // A vertical swipe through the 2nd and 4th lines, in two moves.
        assert_eq!(e.erase(50.0, 18.0, 50.0, 22.0, 1.0), 1);
        assert_eq!(e.erase(50.0, 38.0, 50.0, 42.0, 1.0), 1);
        e.end_erase();
        assert_eq!(ys(&e), vec![10.0, 30.0]);
        e.undo();
        assert_eq!(ys(&e), vec![10.0, 20.0, 30.0, 40.0]);
        e.redo();
        assert_eq!(ys(&e), vec![10.0, 30.0]);
        e.undo();
        e.undo();
        assert_eq!(ys(&e), vec![10.0, 20.0, 30.0]);
    }

    #[test]
    fn erasing_skips_locked_layers_and_misses() {
        let mut e = editor();
        e.add_stroke(0, line(10.0));
        e.document_mut().pages[0].layers[0].locked = true;
        e.begin_erase(0, EraseMode::Stroke);
        assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 2.0), 0);
        e.end_erase();
        assert_eq!(ys(&e), vec![10.0]);
        e.document_mut().pages[0].layers[0].locked = false;
        e.begin_erase(0, EraseMode::Stroke);
        assert_eq!(e.erase(50.0, 50.0, 60.0, 50.0, 2.0), 0);
        e.end_erase();
        assert_eq!(ys(&e), vec![10.0]);
    }

    #[test]
    fn add_page_undo_redo() {
        let mut e = editor();
        assert_eq!(e.add_page(), Some(1));
        e.add_stroke(1, line(5.0));
        assert_eq!(e.undo(), Some(1));
        assert_eq!(e.undo(), Some(0));
        assert_eq!(e.document().pages.len(), 1);
        assert_eq!(e.redo(), Some(1));
        assert_eq!(e.document().pages.len(), 2);
        e.redo();
        assert_eq!(e.document().pages[1].stroke_count(), 1);
    }

    #[test]
    fn infinite_canvas_cannot_add_pages() {
        let mut e = Editor::new(Document::new_infinite("Board"));
        assert_eq!(e.add_page(), None);
        assert!(!e.can_undo());
    }

    #[test]
    fn history_is_capped() {
        let mut e = editor();
        for i in 0..MAX_UNDO + 10 {
            e.add_stroke(0, line(i as f32));
        }
        let mut n = 0;
        while e.undo().is_some() {
            n += 1;
        }
        assert_eq!(n, MAX_UNDO);
        assert_eq!(e.document().pages[0].stroke_count(), 10);
    }

    #[test]
    fn fill_is_one_undo_step() {
        let mut ed = Editor::new(Document::new_paged(
            "Notes",
            PaperSize::A4,
            Orientation::Portrait,
        ));
        let view = FillView {
            scale: 1.0,
            visible: (0.0, 0.0, 595.0, 842.0),
        };
        assert!(ed.fill(0, 50.0, 50.0, 0xFF00_00FF, view).is_some());
        assert_eq!(ed.document().pages[0].stroke_count(), 1);
        assert_eq!(ed.undo(), Some(0));
        assert_eq!(ed.document().pages[0].stroke_count(), 0);
        assert_eq!(ed.redo(), Some(0));
        assert_eq!(ed.document().pages[0].stroke_count(), 1);
    }
}
