//! Layer edits: the active layer, and adding, removing, moving and merging
//! layers. Structural changes are undo steps; renaming, visibility, locking
//! and opacity are set directly, like a document title.

use std::collections::HashMap;

use super::{Editor, Step};
use crate::document::{Id, Layer, Page};

/// The most layers a page can have.
pub const MAX_LAYERS: usize = 10;

/// Index of the active layer of `page`, or of its top layer if none is set
/// or the stored one is gone.
pub(super) fn active_layer_in(active: &HashMap<Id, Id>, page: &Page) -> Option<usize> {
    active
        .get(&page.id)
        .and_then(|id| page.layers.iter().position(|l| l.id == *id))
        .or_else(|| page.layers.len().checked_sub(1))
}

impl Editor {
    /// Index of the page's active layer, counted from the bottom. It falls
    /// back to the top layer if none was chosen or the chosen one was
    /// removed. `None` if the page doesn't exist.
    pub fn active_layer(&self, page: usize) -> Option<usize> {
        active_layer_in(&self.active, self.doc.pages.get(page)?)
    }

    /// Makes the layer at `index` the one that drawing, filling, pasting,
    /// erasing and lasso selection work on. Returns false if either index is
    /// out of range. Not part of undo and not saved.
    pub fn set_active_layer(&mut self, page: usize, index: usize) -> bool {
        let Some(p) = self.doc.pages.get(page) else {
            return false;
        };
        let Some(layer) = p.layers.get(index) else {
            return false;
        };
        self.active.insert(p.id, layer.id);
        true
    }

    /// Index of a page's active layer, for the lasso.
    pub(super) fn active_index(&self, page: &Page) -> Option<usize> {
        active_layer_in(&self.active, page)
    }

    /// The page and active layer new ink goes to, or `None` if the page
    /// doesn't exist or that layer is hidden or locked.
    pub(super) fn drawing_target(&self, page: usize) -> Option<(Id, Id)> {
        let p = self.doc.pages.get(page)?;
        let layer = &p.layers[self.active_index(p)?];
        (layer.visible && !layer.locked).then_some((p.id, layer.id))
    }

    /// Adds an empty layer directly above the active one, makes it active
    /// and returns its index. Returns `None` at [`MAX_LAYERS`] or if the page
    /// doesn't exist.
    pub fn add_layer(&mut self, page: usize, name: &str) -> Option<usize> {
        self.end_erase();
        let p = self.doc.pages.get(page)?;
        if p.layers.len() >= MAX_LAYERS {
            return None;
        }
        let index = self.active_index(p)? + 1;
        let page_id = p.id;
        let step = Step::InsertLayer {
            page: page_id,
            index,
            layer: Layer::new(name),
        };
        self.apply_recorded(step);
        Some(index)
    }

    /// Removes a layer with its strokes and images. Fails for the last layer of a page
    /// and for an index out of range. If it was active, the layer below it
    /// (or the new bottom layer) becomes active.
    pub fn remove_layer(&mut self, page: usize, index: usize) -> bool {
        self.end_erase();
        let Some(p) = self.doc.pages.get(page) else {
            return false;
        };
        if p.layers.len() <= 1 || index >= p.layers.len() {
            return false;
        }
        let step = Step::RemoveLayer { page: p.id, index };
        self.apply_recorded(step);
        true
    }

    /// Moves the layer at `from` so it ends up at `to`. The active layer
    /// stays the same layer. Fails for an index out of range, and when
    /// `from == to`, since nothing would change.
    pub fn move_layer(&mut self, page: usize, from: usize, to: usize) -> bool {
        self.end_erase();
        let Some(p) = self.doc.pages.get(page) else {
            return false;
        };
        if from >= p.layers.len() || to >= p.layers.len() || from == to {
            return false;
        }
        let step = Step::MoveLayer {
            page: p.id,
            from,
            to,
        };
        self.apply_recorded(step);
        true
    }

    /// Moves the strokes, images and text boxes of the layer at `index` onto the layer
    /// below it, above that layer's own, and removes the upper layer. One undo
    /// step. Fails for the bottom layer and if either layer is locked. If the
    /// upper layer was active, the lower one becomes active.
    pub fn merge_down(&mut self, page: usize, index: usize) -> bool {
        self.end_erase();
        let Some(p) = self.doc.pages.get(page) else {
            return false;
        };
        if index == 0 || index >= p.layers.len() {
            return false;
        }
        if p.layers[index].locked || p.layers[index - 1].locked {
            return false;
        }
        let step = Step::MergeDown { page: p.id, index };
        self.apply_recorded(step);
        true
    }

    /// Renames a layer. Not part of undo.
    pub fn rename_layer(&mut self, page: usize, index: usize, name: &str) -> bool {
        self.layer_mut_at(page, index)
            .map(|l| l.name = name.into())
            .is_some()
    }

    /// Shows or hides a layer. Not part of undo.
    pub fn set_layer_visible(&mut self, page: usize, index: usize, visible: bool) -> bool {
        self.layer_mut_at(page, index)
            .map(|l| l.visible = visible)
            .is_some()
    }

    /// Locks or unlocks a layer. Not part of undo.
    pub fn set_layer_locked(&mut self, page: usize, index: usize, locked: bool) -> bool {
        self.layer_mut_at(page, index)
            .map(|l| l.locked = locked)
            .is_some()
    }

    /// Sets a layer's opacity, clamped to 0 to 1. Returns false for a layer
    /// that doesn't exist or a value that isn't a number. Not part of undo.
    pub fn set_layer_opacity(&mut self, page: usize, index: usize, opacity: f32) -> bool {
        if opacity.is_nan() {
            return false;
        }
        self.layer_mut_at(page, index)
            .map(|l| l.opacity = opacity.clamp(0.0, 1.0))
            .is_some()
    }

    fn layer_mut_at(&mut self, page: usize, index: usize) -> Option<&mut Layer> {
        self.doc.pages.get_mut(page)?.layers.get_mut(index)
    }

    /// Applies a layer step and returns its reverse and the page it touched.
    pub(super) fn apply_layer_step(&mut self, step: Step) -> Option<(Step, usize)> {
        match step {
            Step::InsertLayer { page, index, layer } => {
                let p = self.doc.page_mut(page)?;
                let index = index.min(p.layers.len());
                self.active.insert(page, layer.id);
                p.layers.insert(index, layer);
                Some((Step::RemoveLayer { page, index }, self.page_index(page)))
            }
            Step::RemoveLayer { page, index } => {
                let p = self.doc.page_mut(page)?;
                // A page always keeps at least one layer.
                if p.layers.len() <= 1 || index >= p.layers.len() {
                    return None;
                }
                let layer = p.layers.remove(index);
                self.hand_over_active(page, layer.id, index.saturating_sub(1));
                Some((
                    Step::InsertLayer { page, index, layer },
                    self.page_index(page),
                ))
            }
            Step::MoveLayer { page, from, to } => {
                let p = self.doc.page_mut(page)?;
                if from >= p.layers.len() || to >= p.layers.len() {
                    return None;
                }
                let layer = p.layers.remove(from);
                p.layers.insert(to, layer);
                Some((
                    Step::MoveLayer {
                        page,
                        from: to,
                        to: from,
                    },
                    self.page_index(page),
                ))
            }
            Step::MergeDown { page, index } => {
                let p = self.doc.page_mut(page)?;
                if index == 0 || index >= p.layers.len() {
                    return None;
                }
                let mut layer = p.layers.remove(index);
                let count = layer.strokes.len();
                let image_count = layer.images.len();
                let text_count = layer.texts.len();
                p.layers[index - 1].strokes.append(&mut layer.strokes);
                p.layers[index - 1].images.append(&mut layer.images);
                p.layers[index - 1].texts.append(&mut layer.texts);
                self.hand_over_active(page, layer.id, index - 1);
                Some((
                    Step::Unmerge {
                        page,
                        index,
                        layer,
                        count,
                        image_count,
                        text_count,
                    },
                    self.page_index(page),
                ))
            }
            Step::Unmerge {
                page,
                index,
                mut layer,
                count,
                image_count,
                text_count,
            } => {
                let p = self.doc.page_mut(page)?;
                let lower = p.layers.get_mut(index.checked_sub(1)?)?;
                let keep = lower.strokes.len().saturating_sub(count);
                layer.strokes = lower.strokes.split_off(keep);
                let keep = lower.images.len().saturating_sub(image_count);
                layer.images = lower.images.split_off(keep);
                let keep = lower.texts.len().saturating_sub(text_count);
                layer.texts = lower.texts.split_off(keep);
                p.layers.insert(index.min(p.layers.len()), layer);
                Some((Step::MergeDown { page, index }, self.page_index(page)))
            }
            // Only layer steps are routed here.
            _ => None,
        }
    }

    /// If `gone` was the page's active layer, makes the layer now at `index`
    /// active.
    fn hand_over_active(&mut self, page: Id, gone: Id, index: usize) {
        if self.active.get(&page) != Some(&gone) {
            return;
        }
        let next = self
            .doc
            .page_mut(page)
            .and_then(|p| p.layers.get(index).map(|l| l.id));
        match next {
            Some(id) => self.active.insert(page, id),
            None => self.active.remove(&page),
        };
    }
}

#[cfg(test)]
mod tests;
