//! Drawing order within a layer: bring strokes, images and text boxes to the front of
//! their layer or send them to the back. Ids stay, so a selection survives.

use std::collections::HashMap;

use super::{Editor, Step};
use crate::document::{Id, Layer};

/// The full drawing order of one layer, bottom to top.
#[derive(Clone, Debug)]
pub(super) struct LayerOrder {
    layer: Id,
    strokes: Vec<Id>,
    images: Vec<Id>,
    texts: Vec<Id>,
}

/// Stably sorts `items` into the order of `order`; anything missing from it
/// goes last.
fn sort_by_order<T>(items: &mut [T], order: &[Id], id: impl Fn(&T) -> Id) {
    let rank: HashMap<Id, usize> = order.iter().enumerate().map(|(i, &id)| (id, i)).collect();
    items.sort_by_key(|item| rank.get(&id(item)).copied().unwrap_or(usize::MAX));
}

/// `ids` of a layer with the chosen ones moved to the end (or the start),
/// each group keeping its relative order.
fn lifted(ids: Vec<Id>, chosen: &[Id], front: bool) -> Vec<Id> {
    let (mut picked, mut rest): (Vec<Id>, Vec<Id>) =
        ids.into_iter().partition(|i| chosen.contains(i));
    if front {
        rest.append(&mut picked);
        rest
    } else {
        picked.append(&mut rest);
        picked
    }
}

fn current(layer: &Layer) -> LayerOrder {
    LayerOrder {
        layer: layer.id,
        strokes: layer.strokes.iter().map(|s| s.id).collect(),
        images: layer.images.iter().map(|i| i.id).collect(),
        texts: layer.texts.iter().map(|t| t.id).collect(),
    }
}

impl Editor {
    /// Moves the strokes, images and text boxes to the top of their layers, keeping
    /// their order among themselves. Locked layers are skipped. One undo
    /// step. Returns false if nothing changed.
    pub fn bring_to_front(&mut self, page: usize, ids: &[Id]) -> bool {
        self.reorder(page, ids, true)
    }

    /// Moves the strokes, images and text boxes to the bottom of their layers, like
    /// [`Editor::bring_to_front`].
    pub fn send_to_back(&mut self, page: usize, ids: &[Id]) -> bool {
        self.reorder(page, ids, false)
    }

    fn reorder(&mut self, page: usize, ids: &[Id], front: bool) -> bool {
        self.end_erase();
        let Some(p) = self.doc.pages.get(page) else {
            return false;
        };
        let page_id = p.id;
        let mut orders = Vec::new();
        for layer in p.layers.iter().filter(|l| !l.locked) {
            let before = current(layer);
            let after = LayerOrder {
                layer: layer.id,
                strokes: lifted(before.strokes.clone(), ids, front),
                images: lifted(before.images.clone(), ids, front),
                texts: lifted(before.texts.clone(), ids, front),
            };
            if after.strokes != before.strokes
                || after.images != before.images
                || after.texts != before.texts
            {
                orders.push(after);
            }
        }
        if orders.is_empty() {
            return false;
        }
        self.apply_recorded(Step::SetOrder {
            page: page_id,
            layers: orders,
        });
        true
    }

    /// Applies a new order to layers and returns the step that restores
    /// the old one, and the page index.
    pub(super) fn apply_order(
        &mut self,
        page: Id,
        layers: Vec<LayerOrder>,
    ) -> Option<(Step, usize)> {
        let mut before = Vec::with_capacity(layers.len());
        if let Some(p) = self.doc.page_mut(page) {
            for order in layers {
                let Some(layer) = p.layer_mut(order.layer) else {
                    continue;
                };
                before.push(current(layer));
                sort_by_order(&mut layer.strokes, &order.strokes, |s| s.id);
                sort_by_order(&mut layer.images, &order.images, |i| i.id);
                sort_by_order(&mut layer.texts, &order.texts, |t| t.id);
            }
        }
        let index = self.page_index(page);
        Some((
            Step::SetOrder {
                page,
                layers: before,
            },
            index,
        ))
    }
}

#[cfg(test)]
mod tests;
