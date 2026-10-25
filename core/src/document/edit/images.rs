//! Image edits: placing a picture, finding one under a point and cropping,
//! plus the undo steps that add, remove and replace images. Moving, copying
//! and deleting go through the selection edits.

use super::{Editor, PlacedImage, Replacement, Step};
use crate::document::{compose, Id, Image, ImageError, ImageSource, Page};
use crate::Rect;

/// The least a crop may keep along each side, as a fraction of the source.
const MIN_CROP: f32 = 0.01;

/// Where an image sits on a page.
pub(super) struct FoundImage {
    pub layer_pos: usize,
    pub layer: Id,
    pub index: usize,
    pub id: Id,
}

impl Editor {
    /// Embeds a PNG or JPEG and places it on top of the page's active layer,
    /// covering `rect` (page points), as one undo step. A picture with the
    /// same bytes as one already in the note shares its source. Fails if the
    /// bytes are not a usable picture, the rectangle is empty, or the page
    /// doesn't exist or its active layer is hidden or locked.
    pub fn insert_image(
        &mut self,
        page: usize,
        bytes: Vec<u8>,
        rect: Rect,
    ) -> Result<Id, ImageError> {
        self.end_erase();
        let (w, h) = (rect.max_x - rect.min_x, rect.max_y - rect.min_y);
        let sound = [rect.min_x, rect.min_y, w, h].iter().all(|v| v.is_finite());
        if !sound || w <= 0.0 || h <= 0.0 {
            return Err(ImageError::BadRect);
        }
        let (page_id, layer_id) = self.drawing_target(page).ok_or(ImageError::NoLayer)?;
        let source = match self.doc.images.iter().find(|s| *s.bytes == bytes) {
            Some(known) => known.id,
            None => {
                let new = ImageSource::read(bytes)?;
                let id = new.id;
                self.doc.images.push(new);
                id
            }
        };
        let image = Image::new(source, rect.min_x, rect.min_y, w, h);
        let id = image.id;
        let index = self
            .doc
            .page(page_id)
            .and_then(|p| p.layer(layer_id))
            .map_or(0, |l| l.images.len());
        self.apply_recorded(Step::InsertImages {
            page: page_id,
            images: vec![PlacedImage {
                layer: layer_id,
                index,
                image,
            }],
        });
        Ok(id)
    }

    /// The topmost image under a page point, on a visible, unlocked layer.
    /// Upper layers win, and within a layer the later image does.
    pub fn image_at(&self, page: usize, x: f32, y: f32) -> Option<Id> {
        let p = self.doc.pages.get(page)?;
        p.layers
            .iter()
            .rev()
            .filter(|l| l.visible && !l.locked)
            .find_map(|l| l.images.iter().rev().find(|i| i.contains(x, y)))
            .map(|i| i.id)
    }

    /// An image of the page by id, with its full placement and crop.
    pub fn image(&self, page: usize, id: Id) -> Option<&Image> {
        let p = self.doc.pages.get(page)?;
        p.layers.iter().flat_map(|l| &l.images).find(|i| i.id == id)
    }

    /// Shows only `crop` (`[left, top, right, bottom]`, fractions of the
    /// source) of an image, keeping what is still shown exactly where it was
    /// on the page. Values are clamped to 0..1 and each side keeps at least
    /// 1% of the source. Growing the crop again works the same way. Returns
    /// the image's new id, or `None` if it doesn't exist, is on a locked
    /// layer, or the crop is the one it has. One undo step.
    pub fn crop_image(&mut self, page: usize, id: Id, crop: [f32; 4]) -> Option<Id> {
        self.end_erase();
        if crop.iter().any(|v| v.is_nan()) {
            return None;
        }
        let found = self.locate_images(page, &[id], true)?.pop()?;
        let page_id = self.doc.pages[page].id;
        let old = self.doc.pages[page].layers[found.layer_pos].images[found.index].clone();

        let [l, t, r, b] = crop;
        let left = l.clamp(0.0, 1.0 - MIN_CROP);
        let top = t.clamp(0.0, 1.0 - MIN_CROP);
        let right = r.max(left + MIN_CROP).min(1.0);
        let bottom = b.max(top + MIN_CROP).min(1.0);
        let crop = [left, top, right, bottom];
        if crop == old.crop {
            return None;
        }

        // A point at fraction u of the source sits at (u - l) / (r - l) of
        // the old square and (u - l') / (r' - l') of the new one. So the new
        // square, in the old one's terms, starts at (l' - l) / (r - l) and is
        // (r' - l') / (r - l) as wide.
        let [ol, ot, or, ob] = old.crop;
        let (ow, oh) = (or - ol, ob - ot);
        let into_old = [
            (right - left) / ow,
            0.0,
            (left - ol) / ow,
            0.0,
            (bottom - top) / oh,
            (top - ot) / oh,
        ];
        let new = Image {
            id: Id::new_v4(),
            transform: compose(old.transform, into_old),
            crop,
            ..old
        };
        let new_id = new.id;
        self.apply_recorded(Step::ReplaceImages {
            page: page_id,
            remove: vec![id],
            insert: vec![PlacedImage {
                layer: found.layer,
                index: found.index,
                image: new,
            }],
        });
        Some(new_id)
    }

    /// Finds the images on a page in the order of `ids`, skipping repeats
    /// and unknown ids, and images on locked layers if `unlocked_only`.
    pub(super) fn locate_images(
        &self,
        page: usize,
        ids: &[Id],
        unlocked_only: bool,
    ) -> Option<Vec<FoundImage>> {
        let p = self.doc.pages.get(page)?;
        let mut found: Vec<FoundImage> = Vec::new();
        for &id in ids {
            if found.iter().any(|f| f.id == id) {
                continue;
            }
            for (layer_pos, l) in p.layers.iter().enumerate() {
                if unlocked_only && l.locked {
                    continue;
                }
                if let Some(index) = l.images.iter().position(|i| i.id == id) {
                    found.push(FoundImage {
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

    /// Swaps each found image for `make(image)` at the same layer and
    /// position. Returns the step, not yet applied, and the pairs of old and
    /// new ids. `None` if nothing was found.
    pub(super) fn replace_images_step(
        &self,
        page: usize,
        ids: &[Id],
        make: impl Fn(&Image) -> Image,
    ) -> Option<Replacement> {
        let found = self.locate_images(page, ids, true)?;
        if found.is_empty() {
            return None;
        }
        let p = &self.doc.pages[page];
        let made: Vec<Image> = found
            .iter()
            .map(|f| make(&p.layers[f.layer_pos].images[f.index]))
            .collect();
        let pairs = found.iter().zip(&made).map(|(f, n)| (f.id, n.id)).collect();
        // Ascending positions, inserted after every old image is out, put
        // each copy exactly where its original sat.
        let mut order: Vec<usize> = (0..found.len()).collect();
        order.sort_by_key(|&i| (found[i].layer_pos, found[i].index));
        let insert = order
            .into_iter()
            .map(|i| PlacedImage {
                layer: found[i].layer,
                index: found[i].index,
                image: made[i].clone(),
            })
            .collect();
        let step = Step::ReplaceImages {
            page: p.id,
            remove: found.iter().map(|f| f.id).collect(),
            insert,
        };
        Some((step, pairs))
    }

    /// Applies a step that adds, removes or replaces images and returns its
    /// reverse and the page it touched.
    pub(super) fn apply_image_step(&mut self, step: Step) -> Option<(Step, usize)> {
        match step {
            Step::InsertImages { page, images } => {
                let mut ids = Vec::with_capacity(images.len());
                if let Some(p) = self.doc.page_mut(page) {
                    for placed in images {
                        ids.extend(put_image(p, placed));
                    }
                }
                // Removing in reverse insertion order finds every image at
                // the position it was put in.
                ids.reverse();
                Some((Step::RemoveImages { page, ids }, self.page_index(page)))
            }
            Step::RemoveImages { page, ids } => {
                let mut images = Vec::with_capacity(ids.len());
                if let Some(p) = self.doc.page_mut(page) {
                    images.extend(ids.into_iter().filter_map(|id| take_image(p, id)));
                }
                images.reverse();
                Some((Step::InsertImages { page, images }, self.page_index(page)))
            }
            Step::ReplaceImages {
                page,
                remove,
                insert,
            } => {
                let mut taken = Vec::with_capacity(remove.len());
                let mut added = Vec::with_capacity(insert.len());
                if let Some(p) = self.doc.page_mut(page) {
                    taken.extend(remove.into_iter().filter_map(|id| take_image(p, id)));
                    for placed in insert {
                        added.extend(put_image(p, placed));
                    }
                }
                added.reverse();
                taken.reverse();
                Some((
                    Step::ReplaceImages {
                        page,
                        remove: added,
                        insert: taken,
                    },
                    self.page_index(page),
                ))
            }
            _ => None,
        }
    }
}

/// Puts an image at its position in its layer. Returns its id, or nothing
/// if the layer is gone.
fn put_image(page: &mut Page, placed: PlacedImage) -> Option<Id> {
    let layer = page.layer_mut(placed.layer)?;
    let id = placed.image.id;
    let at = placed.index.min(layer.images.len());
    layer.images.insert(at, placed.image);
    Some(id)
}

fn take_image(page: &mut Page, id: Id) -> Option<PlacedImage> {
    page.layers.iter_mut().find_map(|layer| {
        let index = layer.images.iter().position(|i| i.id == id)?;
        Some(PlacedImage {
            layer: layer.id,
            index,
            image: layer.images.remove(index),
        })
    })
}

#[cfg(test)]
mod tests;
