//! Selection edits: pick strokes, images and text boxes with a lasso, then move,
//! recolour, delete or copy them. Each change is one undo step, however many
//! strokes, images and text boxes it touches.

use super::{Clipboard, Editor, Placed, PlacedImage, PlacedText, Replacement, Step};
use crate::document::lasso::{encloses, encloses_image, encloses_text};
use crate::document::{Id, Image, Stroke, TextBox, Tool};
use crate::Rect;

/// The widest and narrowest a stroke can be made, in points.
const MIN_WIDTH: f32 = 0.1;
const MAX_WIDTH: f32 = 200.0;

/// Where a stroke sits on a page.
struct Found {
    layer_pos: usize,
    layer: Id,
    index: usize,
    id: Id,
}

/// Bounds of the drawn ink of some strokes, half width included, and of the
/// corners of some images and text boxes.
fn ink_rect<'a>(
    strokes: impl Iterator<Item = &'a Stroke>,
    images: impl Iterator<Item = &'a Image>,
    texts: impl Iterator<Item = &'a TextBox>,
) -> Option<Rect> {
    let mut rect = Rect::EMPTY;
    for s in strokes {
        let brush = s.style.brush();
        for p in &s.points {
            rect.add_circle(p.x, p.y, brush.width_at(p.pressure) * 0.5);
        }
    }
    let corners = images
        .flat_map(|i| i.corners())
        .chain(texts.flat_map(|t| t.corners()));
    for (x, y) in corners {
        rect.add_circle(x, y, 0.0);
    }
    (!rect.is_empty()).then_some(rect)
}

/// A copy with a new id, so anything keyed by id never sees stale geometry.
fn renewed(s: &Stroke) -> Stroke {
    Stroke {
        id: Id::new_v4(),
        ..s.clone()
    }
}

fn moved(s: &Stroke, dx: f32, dy: f32) -> Stroke {
    let mut out = renewed(s);
    for p in &mut out.points {
        p.x += dx;
        p.y += dy;
    }
    out
}

/// A copy placed by `m`, as for [`Editor::transform`].
fn mapped(s: &Stroke, m: [f32; 6]) -> Stroke {
    let [sx, kx, tx, ky, sy, ty] = m;
    let mut out = renewed(s);
    out.style.width *= (sx * sy - kx * ky).abs().sqrt();
    for p in &mut out.points {
        let (x, y) = (p.x, p.y);
        p.x = sx * x + kx * y + tx;
        p.y = ky * x + sy * y + ty;
    }
    out
}

fn shifted(i: &Image, dx: f32, dy: f32) -> Image {
    i.transformed([1.0, 0.0, dx, 0.0, 1.0, dy])
}

impl Editor {
    /// Strokes, then images, then text boxes on the active layer that the
    /// closed lasso polygon mostly encloses (at least half of a stroke's
    /// length or of an image's or text box's area), each in drawing order. Empty if the page doesn't exist
    /// or the active layer is hidden or locked.
    pub fn select(&self, page: usize, lasso: &[(f32, f32)]) -> Vec<Id> {
        let Some(p) = self.doc.pages.get(page) else {
            return Vec::new();
        };
        let Some(layer) = self.active_index(p).map(|i| &p.layers[i]) else {
            return Vec::new();
        };
        if !layer.visible || layer.locked {
            return Vec::new();
        }
        let strokes = layer.strokes.iter().filter(|s| encloses(lasso, s));
        let images = layer.images.iter().filter(|i| encloses_image(lasso, i));
        let texts = layer.texts.iter().filter(|t| encloses_text(lasso, t));
        strokes
            .map(|s| s.id)
            .chain(images.map(|i| i.id))
            .chain(texts.map(|t| t.id))
            .collect()
    }

    /// Bounds of the strokes' drawn ink, half width included, and of the
    /// corners of the images and text boxes, or `None` if none of the ids
    /// exist.
    pub fn ink_bounds(&self, page: usize, ids: &[Id]) -> Option<Rect> {
        let found = self.locate(page, ids, false)?;
        let images = self.locate_images(page, ids, false)?;
        let texts = self.locate_texts(page, ids, false)?;
        let p = self.doc.pages.get(page)?;
        ink_rect(
            found
                .iter()
                .map(|f| &p.layers[f.layer_pos].strokes[f.index]),
            images
                .iter()
                .map(|f| &p.layers[f.layer_pos].images[f.index]),
            texts.iter().map(|f| &p.layers[f.layer_pos].texts[f.index]),
        )
    }

    /// The width, in whole points, most of the pen strokes among `ids` are
    /// drawn at (the first one met wins a tie), or `None` if there are none.
    /// Fills have no width, so they do not count.
    pub fn common_width(&self, page: usize, ids: &[Id]) -> Option<f32> {
        let found = self.locate(page, ids, false)?;
        let p = self.doc.pages.get(page)?;
        let mut counts: Vec<(i32, usize)> = Vec::new();
        for f in &found {
            let s = &p.layers[f.layer_pos].strokes[f.index];
            if s.style.tool == Tool::Fill {
                continue;
            }
            let whole = s.style.width.round() as i32;
            match counts.iter_mut().find(|(w, _)| *w == whole) {
                Some((_, n)) => *n += 1,
                None => counts.push((whole, 1)),
            }
        }
        let mut best: Option<(i32, usize)> = None;
        for &(w, n) in &counts {
            if best.is_none_or(|(_, top)| n > top) {
                best = Some((w, n));
            }
        }
        best.map(|(w, _)| w as f32)
    }

    /// Replaces the strokes and images with transformed copies, and moves,
    /// turns and scales the text boxes in place, see
    /// [`TextBox::transformed`]. `m` is
    /// `[sx, kx, tx, ky, sy, ty]`: `x' = sx*x + kx*y + tx`,
    /// `y' = ky*x + sy*y + ty`. Stroke widths scale by the matrix's area
    /// factor. Returns the new ids in the order of `ids`, where a text box
    /// keeps its id; unknown ids and anything on a locked layer are skipped.
    pub fn transform(&mut self, page: usize, ids: &[Id], m: [f32; 6]) -> Vec<Id> {
        self.end_erase();
        let strokes = self.replace_step(page, ids, |s| mapped(s, m));
        let images = self.replace_images_step(page, ids, |i| i.transformed(m));
        let texts = self.update_texts_step(page, ids, |t| t.transformed(m));
        let kept = self.text_ids(page, ids);
        self.run(ids, [strokes, images], texts, kept)
    }

    /// Replaces the strokes with copies in the colour of `rgba`, each
    /// keeping its own alpha, and colours the words of text boxes the same
    /// way, keeping their ids. Images have no colour to change: they stay as
    /// they are and their ids come back unchanged, so a selection of both
    /// keeps its images. Returns the ids in the order of `ids`.
    pub fn recolor(&mut self, page: usize, ids: &[Id], rgba: u32) -> Vec<Id> {
        self.end_erase();
        let strokes = self.replace_step(page, ids, |s| {
            let mut out = renewed(s);
            out.style.color = (rgba & 0xFFFF_FF00) | (s.style.color & 0xFF);
            out
        });
        let texts = self.update_texts_step(page, ids, |t| {
            let mut out = t.clone();
            out.color = (rgba & 0xFFFF_FF00) | (t.color & 0xFF);
            out
        });
        let mut kept = self.image_ids(page, ids);
        kept.extend(self.text_ids(page, ids));
        self.run(ids, [strokes, None], texts, kept)
    }

    /// Replaces the strokes with copies drawn `width` points wide. Fills
    /// have no width and images and text boxes none to change: they stay as they are and
    /// their ids come back unchanged. Returns the ids in the order of
    /// `ids`.
    pub fn set_width(&mut self, page: usize, ids: &[Id], width: f32) -> Vec<Id> {
        self.end_erase();
        let width = width.clamp(MIN_WIDTH, MAX_WIDTH);
        let strokes = self.replace_step(page, ids, |s| {
            let mut out = renewed(s);
            if s.style.tool != Tool::Fill {
                out.style.width = width;
            }
            out
        });
        let mut kept = self.image_ids(page, ids);
        kept.extend(self.text_ids(page, ids));
        self.run(ids, [strokes, None], None, kept)
    }

    /// The ids among `ids` of images on unlocked layers.
    fn image_ids(&self, page: usize, ids: &[Id]) -> Vec<Id> {
        let found = self.locate_images(page, ids, true).unwrap_or_default();
        found.into_iter().map(|f| f.id).collect()
    }

    /// The ids among `ids` of text boxes on unlocked layers.
    fn text_ids(&self, page: usize, ids: &[Id]) -> Vec<Id> {
        let found = self.locate_texts(page, ids, true).unwrap_or_default();
        found.into_iter().map(|f| f.id).collect()
    }

    /// Removes the strokes, images and text boxes. Returns false if there was nothing
    /// to remove.
    pub fn delete(&mut self, page: usize, ids: &[Id]) -> bool {
        self.end_erase();
        let steps = self.remove_steps(page, ids);
        let done = !steps.is_empty();
        self.apply_steps(steps);
        done
    }

    /// The steps, not yet applied, that remove the strokes, images and text
    /// boxes on unlocked layers.
    fn remove_steps(&self, page: usize, ids: &[Id]) -> Vec<Step> {
        let mut steps = Vec::new();
        let page_id = self.doc.pages.get(page).map(|p| p.id);
        let strokes = self.locate(page, ids, true).unwrap_or_default();
        if let (Some(page), false) = (page_id, strokes.is_empty()) {
            steps.push(Step::RemoveStrokes {
                page,
                ids: strokes.iter().map(|f| f.id).collect(),
            });
        }
        let images = self.locate_images(page, ids, true).unwrap_or_default();
        if let (Some(page), false) = (page_id, images.is_empty()) {
            steps.push(Step::RemoveImages {
                page,
                ids: images.iter().map(|f| f.id).collect(),
            });
        }
        let texts = self.locate_texts(page, ids, true).unwrap_or_default();
        if let (Some(page), false) = (page_id, texts.is_empty()) {
            steps.push(Step::RemoveTexts {
                page,
                ids: texts.iter().map(|f| f.id).collect(),
            });
        }
        steps
    }

    /// Stores copies of the strokes, images and text boxes, each in drawing order, for
    /// [`Editor::paste`]. The clipboard is not part of undo and is not
    /// saved. Returns false, and keeps the old clipboard, if none of the ids
    /// exist.
    pub fn copy(&mut self, page: usize, ids: &[Id]) -> bool {
        self.end_erase();
        let Some(copied) = self.copies(page, ids) else {
            return false;
        };
        if copied.is_empty() {
            return false;
        }
        self.clipboard = copied;
        true
    }

    pub fn has_clipboard(&self) -> bool {
        !self.clipboard.is_empty()
    }

    /// Adds the clipboard to the page's active layer with new ids, centred on
    /// `(x, y)`. Returns the new ids (strokes, images, then text boxes), or nothing if
    /// the clipboard is empty or the active layer is locked or hidden.
    pub fn paste(&mut self, page: usize, x: f32, y: f32) -> Vec<Id> {
        self.end_erase();
        let c = &self.clipboard;
        let Some(r) = ink_rect(c.strokes.iter(), c.images.iter(), c.texts.iter()) else {
            return Vec::new();
        };
        let (dx, dy) = (x - (r.min_x + r.max_x) * 0.5, y - (r.min_y + r.max_y) * 0.5);
        let strokes = c.strokes.iter().map(|s| moved(s, dx, dy)).collect();
        let images = c.images.iter().map(|i| shifted(i, dx, dy)).collect();
        let texts = c.texts.iter().map(|t| t.moved(dx, dy)).collect();
        self.add_to_active(page, strokes, images, texts)
    }

    /// Adds copies of the strokes, images and text boxes to the page's active
    /// layer, offset by `(dx, dy)`, with new ids in drawing order of the
    /// originals (strokes, images, then text boxes).
    pub fn duplicate(&mut self, page: usize, ids: &[Id], dx: f32, dy: f32) -> Vec<Id> {
        self.end_erase();
        let Some(copied) = self.copies(page, ids) else {
            return Vec::new();
        };
        let strokes = copied.strokes.iter().map(|s| moved(s, dx, dy)).collect();
        let images = copied.images.iter().map(|i| shifted(i, dx, dy)).collect();
        let texts = copied.texts.iter().map(|t| t.moved(dx, dy)).collect();
        self.add_to_active(page, strokes, images, texts)
    }

    /// Finds the strokes on a page in the order of `ids`, skipping repeats
    /// and unknown ids, and strokes on locked layers if `unlocked_only`.
    fn locate(&self, page: usize, ids: &[Id], unlocked_only: bool) -> Option<Vec<Found>> {
        let p = self.doc.pages.get(page)?;
        let mut found: Vec<Found> = Vec::with_capacity(ids.len());
        for &id in ids {
            if found.iter().any(|f| f.id == id) {
                continue;
            }
            for (layer_pos, l) in p.layers.iter().enumerate() {
                if unlocked_only && l.locked {
                    continue;
                }
                if let Some(index) = l.strokes.iter().position(|s| s.id == id) {
                    found.push(Found {
                        layer_pos,
                        layer: l.id,
                        index,
                        id,
                    });
                    break;
                }
            }
        }
        Some(found)
    }

    /// Clones of the strokes, images and text boxes in drawing order.
    fn copies(&self, page: usize, ids: &[Id]) -> Option<Clipboard> {
        let mut found = self.locate(page, ids, false)?;
        found.sort_by_key(|f| (f.layer_pos, f.index));
        let mut images = self.locate_images(page, ids, false)?;
        images.sort_by_key(|f| (f.layer_pos, f.index));
        let mut texts = self.locate_texts(page, ids, false)?;
        texts.sort_by_key(|f| (f.layer_pos, f.index));
        let p = self.doc.pages.get(page)?;
        Some(Clipboard {
            strokes: found
                .iter()
                .map(|f| p.layers[f.layer_pos].strokes[f.index].clone())
                .collect(),
            images: images
                .iter()
                .map(|f| p.layers[f.layer_pos].images[f.index].clone())
                .collect(),
            texts: texts
                .iter()
                .map(|f| p.layers[f.layer_pos].texts[f.index].clone())
                .collect(),
        })
    }

    /// Swaps each found stroke for `make(stroke)` at the same layer and
    /// position. Returns the step, not yet applied, and the pairs of old and
    /// new ids. `None` if nothing was found.
    fn replace_step(
        &self,
        page: usize,
        ids: &[Id],
        make: impl Fn(&Stroke) -> Stroke,
    ) -> Option<Replacement> {
        let found = self.locate(page, ids, true)?;
        if found.is_empty() {
            return None;
        }
        let page_id = self.doc.pages[page].id;
        let made: Vec<Stroke> = found
            .iter()
            .map(|f| make(&self.doc.pages[page].layers[f.layer_pos].strokes[f.index]))
            .collect();
        let pairs = found.iter().zip(&made).map(|(f, n)| (f.id, n.id)).collect();
        // Inserting in ascending position, after every old stroke is out,
        // puts each copy exactly where its original sat.
        let mut order: Vec<usize> = (0..found.len()).collect();
        order.sort_by_key(|&i| (found[i].layer_pos, found[i].index));
        let insert = order
            .into_iter()
            .map(|i| Placed {
                layer: found[i].layer,
                index: found[i].index,
                stroke: made[i].clone(),
            })
            .collect();
        let step = Step::ReplaceStrokes {
            page: page_id,
            remove: found.iter().map(|f| f.id).collect(),
            insert,
        };
        Some((step, pairs))
    }

    /// Applies the steps that exist as one undo step, and returns the new
    /// id of each id in `ids` that one of them replaced, in the order of
    /// `ids`. Ids in `unchanged` are returned as they are. `texts` updates
    /// boxes in place, so their ids belong in `unchanged`.
    fn run(
        &mut self,
        ids: &[Id],
        parts: [Option<Replacement>; 2],
        texts: Option<Step>,
        unchanged: Vec<Id>,
    ) -> Vec<Id> {
        let mut pairs: Vec<(Id, Id)> = unchanged.into_iter().map(|id| (id, id)).collect();
        let mut steps = Vec::new();
        for (step, mapped) in parts.into_iter().flatten() {
            pairs.extend(mapped);
            steps.push(step);
        }
        steps.extend(texts);
        self.apply_steps(steps);
        ids.iter()
            .filter_map(|id| pairs.iter().find(|(old, _)| old == id).map(|&(_, new)| new))
            .collect()
    }

    /// Applies steps and records them as a single undo step.
    fn apply_steps(&mut self, mut steps: Vec<Step>) {
        let step = match steps.len() {
            0 => return,
            1 => steps.remove(0),
            _ => Step::Group(steps),
        };
        self.apply_recorded(step);
    }

    fn add_to_active(
        &mut self,
        page: usize,
        strokes: Vec<Stroke>,
        images: Vec<Image>,
        texts: Vec<TextBox>,
    ) -> Vec<Id> {
        let Some((steps, ids)) = self.insert_steps(page, strokes, images, texts) else {
            return Vec::new();
        };
        self.apply_steps(steps);
        ids
    }

    /// The steps, not yet applied, that add the strokes, images and text
    /// boxes to the end of the page's active layer, and their ids. `None` if
    /// there is nothing to add or the active layer is locked or hidden.
    fn insert_steps(
        &self,
        page: usize,
        strokes: Vec<Stroke>,
        images: Vec<Image>,
        texts: Vec<TextBox>,
    ) -> Option<(Vec<Step>, Vec<Id>)> {
        if strokes.is_empty() && images.is_empty() && texts.is_empty() {
            return None;
        }
        let (page_id, layer_id) = self.drawing_target(page)?;
        let layer = self.doc.page(page_id).and_then(|p| p.layer(layer_id))?;
        let mut ids: Vec<Id> = strokes.iter().map(|s| s.id).collect();
        ids.extend(images.iter().map(|i| i.id));
        ids.extend(texts.iter().map(|t| t.id));
        let first = layer.strokes.len();
        let first_text = layer.texts.len();
        let first_image = layer.images.len();
        let placed_images = images
            .into_iter()
            .enumerate()
            .map(|(i, image)| PlacedImage {
                layer: layer_id,
                index: first_image + i,
                image,
            })
            .collect::<Vec<_>>();
        let placed_texts = texts
            .into_iter()
            .enumerate()
            .map(|(i, text)| PlacedText {
                layer: layer_id,
                index: first_text + i,
                text,
            })
            .collect::<Vec<_>>();
        let placed: Vec<Placed> = strokes
            .into_iter()
            .enumerate()
            .map(|(i, stroke)| Placed {
                layer: layer_id,
                index: first + i,
                stroke,
            })
            .collect();
        let mut steps = Vec::new();
        if !placed.is_empty() {
            steps.push(Step::InsertStrokes {
                page: page_id,
                strokes: placed,
            });
        }
        if !placed_images.is_empty() {
            steps.push(Step::InsertImages {
                page: page_id,
                images: placed_images,
            });
        }
        if !placed_texts.is_empty() {
            steps.push(Step::InsertTexts {
                page: page_id,
                texts: placed_texts,
            });
        }
        Some((steps, ids))
    }

    /// Applies a step and records its reverse as a new undo step.
    pub(super) fn apply_recorded(&mut self, step: Step) {
        if let Some((inverse, _)) = self.apply(step) {
            self.record(inverse);
        }
    }
}

mod pages;

#[cfg(test)]
mod tests;
