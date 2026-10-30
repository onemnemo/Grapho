//! Text box edits: placing a box, finding one under a point and changing its
//! words, look or frame, plus the undo steps that add, remove and update
//! text boxes. Moving, copying and deleting go through the selection edits.
//! A change keeps the box's id, so an open editor never loses its box.

use super::{Editor, PlacedText, Step};
use crate::document::{
    Id, Page, TextBox, TextError, TextStyle, MAX_TEXT_CHARS, MAX_TEXT_SIZE, MIN_TEXT_SIDE,
    MIN_TEXT_SIZE,
};

/// Where a text box sits on a page.
pub(super) struct FoundText {
    pub layer_pos: usize,
    pub index: usize,
    pub id: Id,
}

impl Editor {
    /// Places a text box on top of the page's active layer, as one undo
    /// step. `(x, y, width, height)` is the frame in page points. Fails if
    /// the frame is empty or not a number, the font size is out of range,
    /// the text is too long, or the page doesn't exist or its active layer
    /// is hidden or locked.
    pub fn add_text(
        &mut self,
        page: usize,
        frame: [f32; 4],
        text: &str,
        style: TextStyle,
    ) -> Result<Id, TextError> {
        self.end_erase();
        let [x, y, width, height] = frame;
        let boxed = TextBox::new(x, y, width, height, text, style);
        if text.chars().count() > MAX_TEXT_CHARS {
            return Err(TextError::TooLong);
        }
        if !boxed.is_sound() {
            return Err(TextError::BadBox);
        }
        let (page_id, layer_id) = self.drawing_target(page).ok_or(TextError::NoLayer)?;
        let id = boxed.id;
        let index = self
            .doc
            .page(page_id)
            .and_then(|p| p.layer(layer_id))
            .map_or(0, |l| l.texts.len());
        self.apply_recorded(Step::InsertTexts {
            page: page_id,
            texts: vec![PlacedText {
                layer: layer_id,
                index,
                text: boxed,
            }],
        });
        Ok(id)
    }

    /// The topmost text box under a page point, on a visible, unlocked
    /// layer. Upper layers win, and within a layer the later box does.
    pub fn text_at(&self, page: usize, x: f32, y: f32) -> Option<Id> {
        let p = self.doc.pages.get(page)?;
        p.layers
            .iter()
            .rev()
            .filter(|l| l.visible && !l.locked)
            .find_map(|l| l.texts.iter().rev().find(|t| t.contains(x, y)))
            .map(|t| t.id)
    }

    /// A text box of the page by id.
    pub fn text_box(&self, page: usize, id: Id) -> Option<&TextBox> {
        let p = self.doc.pages.get(page)?;
        p.layers.iter().flat_map(|l| &l.texts).find(|t| t.id == id)
    }

    /// Changes the words of a text box, as one undo step. Returns false if
    /// the box doesn't exist, is on a locked layer, or already says this.
    pub fn set_text(&mut self, page: usize, id: Id, text: &str) -> Result<bool, TextError> {
        if text.chars().count() > MAX_TEXT_CHARS {
            return Err(TextError::TooLong);
        }
        self.change_text(page, id, |t| t.text = text.to_owned())
    }

    /// Changes the font size, colour, alignment, weight, slant and underline of a text box, as
    /// one undo step. The size is held to its limits. Returns false if the
    /// box doesn't exist, is on a locked layer, or already looks like this.
    pub fn set_text_style(
        &mut self,
        page: usize,
        id: Id,
        style: TextStyle,
    ) -> Result<bool, TextError> {
        if !style.size.is_finite() {
            return Err(TextError::BadBox);
        }
        self.change_text(page, id, |t| t.set_style(style))
    }

    /// Moves and sizes a text box without turning it, as one undo step. The
    /// shell uses this to write back the height its layout needs. Returns
    /// false if the box doesn't exist, is on a locked layer, or is already
    /// there.
    pub fn set_text_frame(
        &mut self,
        page: usize,
        id: Id,
        frame: [f32; 4],
    ) -> Result<bool, TextError> {
        let [x, y, width, height] = frame;
        if !frame.iter().all(|v| v.is_finite()) || width < MIN_TEXT_SIDE || height < MIN_TEXT_SIDE {
            return Err(TextError::BadBox);
        }
        self.change_text(page, id, |t| {
            (t.x, t.y, t.width, t.height) = (x, y, width, height)
        })
    }

    /// Changes everything about a text box at once, as one undo step: its
    /// frame, turn, words and look. This is what a shell calls when an edit
    /// session ends. Returns false if nothing changed.
    pub fn update_text(&mut self, page: usize, new: TextBox) -> Result<bool, TextError> {
        if new.text.chars().count() > MAX_TEXT_CHARS {
            return Err(TextError::TooLong);
        }
        let mut new = new;
        new.size = new.size.clamp(MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        if !new.is_sound() {
            return Err(TextError::BadBox);
        }
        self.change_text(page, new.id, |t| *t = new.clone())
    }

    fn change_text(
        &mut self,
        page: usize,
        id: Id,
        change: impl FnOnce(&mut TextBox),
    ) -> Result<bool, TextError> {
        self.end_erase();
        let found = self
            .locate_texts(page, &[id], true)
            .and_then(|mut f| f.pop())
            .ok_or(TextError::NotFound)?;
        let page_id = self.doc.pages[page].id;
        let old = &self.doc.pages[page].layers[found.layer_pos].texts[found.index];
        let mut new = old.clone();
        change(&mut new);
        new.id = id;
        if new == *old {
            return Ok(false);
        }
        if !new.is_sound() {
            return Err(TextError::BadBox);
        }
        self.apply_recorded(Step::UpdateTexts {
            page: page_id,
            texts: vec![new],
        });
        Ok(true)
    }

    /// Finds the text boxes on a page in the order of `ids`, skipping
    /// repeats and unknown ids, and boxes on locked layers if
    /// `unlocked_only`.
    pub(super) fn locate_texts(
        &self,
        page: usize,
        ids: &[Id],
        unlocked_only: bool,
    ) -> Option<Vec<FoundText>> {
        let p = self.doc.pages.get(page)?;
        let mut found: Vec<FoundText> = Vec::new();
        for &id in ids {
            if found.iter().any(|f| f.id == id) {
                continue;
            }
            for (layer_pos, l) in p.layers.iter().enumerate() {
                if unlocked_only && l.locked {
                    continue;
                }
                if let Some(index) = l.texts.iter().position(|t| t.id == id) {
                    found.push(FoundText {
                        layer_pos,
                        index,
                        id,
                    });
                    break;
                }
            }
        }
        Some(found)
    }

    /// A step that swaps each found box for `make(box)`, keeping ids and
    /// places, or `None` if nothing was found.
    pub(super) fn update_texts_step(
        &self,
        page: usize,
        ids: &[Id],
        make: impl Fn(&TextBox) -> TextBox,
    ) -> Option<Step> {
        let found = self.locate_texts(page, ids, true)?;
        if found.is_empty() {
            return None;
        }
        let p = &self.doc.pages[page];
        let texts = found
            .iter()
            .map(|f| make(&p.layers[f.layer_pos].texts[f.index]))
            .collect();
        Some(Step::UpdateTexts { page: p.id, texts })
    }

    /// Applies a step that adds, removes or updates text boxes and returns
    /// its reverse and the page it touched.
    pub(super) fn apply_text_step(&mut self, step: Step) -> Option<(Step, usize)> {
        match step {
            Step::InsertTexts { page, texts } => {
                let mut ids = Vec::with_capacity(texts.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for placed in texts {
                        ids.extend(put_text(p, placed));
                    }
                }
                // Removing in reverse insertion order finds every box at the
                // position it was put in.
                ids.reverse();
                Some((Step::RemoveTexts { page, ids }, self.page_index(page)))
            }
            Step::RemoveTexts { page, ids } => {
                let mut texts = Vec::with_capacity(ids.len());
                if let Some(p) = self.doc.page_mut(page) {
                    texts.extend(ids.into_iter().filter_map(|id| take_text(p, id)));
                }
                texts.reverse();
                Some((Step::InsertTexts { page, texts }, self.page_index(page)))
            }
            Step::UpdateTexts { page, texts } => {
                let mut before = Vec::with_capacity(texts.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for new in texts {
                        let slot = p
                            .layers
                            .iter_mut()
                            .flat_map(|l| l.texts.iter_mut())
                            .find(|t| t.id == new.id);
                        if let Some(slot) = slot {
                            before.push(std::mem::replace(slot, new));
                        }
                    }
                }
                Some((
                    Step::UpdateTexts {
                        page,
                        texts: before,
                    },
                    self.page_index(page),
                ))
            }
            _ => None,
        }
    }
}

/// Puts a text box at its position in its layer. Returns its id, or nothing
/// if the layer is gone.
fn put_text(page: &mut Page, placed: PlacedText) -> Option<Id> {
    let layer = page.layer_mut(placed.layer)?;
    let id = placed.text.id;
    let at = placed.index.min(layer.texts.len());
    layer.texts.insert(at, placed.text);
    Some(id)
}

fn take_text(page: &mut Page, id: Id) -> Option<PlacedText> {
    page.layers.iter_mut().find_map(|layer| {
        let index = layer.texts.iter().position(|t| t.id == id)?;
        Some(PlacedText {
            layer: layer.id,
            index,
            text: layer.texts.remove(index),
        })
    })
}

#[cfg(test)]
mod tests;
