//! Diagram cleanup edits: lifting the background off an image, and turning
//! the drawing in an image into pen strokes.

use super::{Editor, Placed, PlacedImage, Step};
use crate::diagram::{cut_out, encode_png, trace, Cutout, Pixels};
use crate::document::{Id, Image, ImageError, ImageSource, Stroke, StrokeStyle, Tool};
use crate::{InputSample, LineStyle};

/// An image found on a page, with the size of its source.
struct Target {
    page: Id,
    layer: Id,
    index: usize,
    image: Image,
}

impl Editor {
    /// Finds the image and checks `pixels` is the decoded source it shows.
    fn diagram_target(&self, page: usize, id: Id, pixels: &Pixels) -> Result<Target, ImageError> {
        let p = self.doc.pages.get(page).ok_or(ImageError::NoLayer)?;
        let found = self
            .locate_images(page, &[id], false)
            .and_then(|mut f| f.pop())
            .ok_or(ImageError::NotFound)?;
        let layer = &p.layers[found.layer_pos];
        if !layer.visible || layer.locked {
            return Err(ImageError::NoLayer);
        }
        let image = layer.images[found.index].clone();
        let source = self
            .doc
            .images
            .iter()
            .find(|s| s.id == image.source)
            .ok_or(ImageError::NotFound)?;
        if !pixels.is_sound() || (pixels.width, pixels.height) != (source.width, source.height) {
            return Err(ImageError::SizeMismatch);
        }
        Ok(Target {
            page: p.id,
            layer: layer.id,
            index: found.index,
            image,
        })
    }

    /// Replaces an image with a copy whose paper is see through, keeping
    /// its place, rotation and crop, as one undo step. `pixels` is the whole
    /// decoded source of the image and must match the source's size. The
    /// copy points at a new PNG source. `cutout` says how much counts as
    /// paper. Returns the new image's id. Fails
    /// if the image isn't on the page, its layer is hidden or locked, or the
    /// pixels are not the source's.
    pub fn remove_image_background(
        &mut self,
        page: usize,
        id: Id,
        pixels: &Pixels,
        cutout: Cutout,
    ) -> Result<Id, ImageError> {
        self.end_erase();
        let target = self.diagram_target(page, id, pixels)?;
        self.swap_source(target, id, &cut_out(pixels, cutout))
    }

    /// Replaces an image with a copy that shows `pixels` instead, keeping its
    /// place, rotation and crop, as one undo step. `pixels` must match the
    /// size of the image's source, as when a cut-out is touched up by hand.
    /// Returns the new image's id. Fails like
    /// [`Editor::remove_image_background`].
    pub fn replace_image_pixels(
        &mut self,
        page: usize,
        id: Id,
        pixels: &Pixels,
    ) -> Result<Id, ImageError> {
        self.end_erase();
        let target = self.diagram_target(page, id, pixels)?;
        self.swap_source(target, id, pixels)
    }

    /// Puts a copy of the target image showing `result` in place of `old`.
    fn swap_source(&mut self, target: Target, old: Id, result: &Pixels) -> Result<Id, ImageError> {
        let source = ImageSource::read(encode_png(result))?;
        let image = Image {
            id: Id::new_v4(),
            source: source.id,
            ..target.image
        };
        let new_id = image.id;
        self.doc.images.push(source);
        self.apply_recorded(Step::ReplaceImages {
            page: target.page,
            remove: vec![old],
            insert: vec![PlacedImage {
                layer: target.layer,
                index: target.index,
                image,
            }],
        });
        Ok(new_id)
    }

    /// Traces the drawing in the shown (cropped) part of an image and
    /// replaces the image with pen strokes on its layer, in the same place
    /// and size, as one undo step. `pixels` is the whole decoded source and
    /// must match the source's size. The strokes go on top of the layer's
    /// strokes and are returned in order. Filled shapes (arrowheads, dots) become
    /// fills, placed first so the lines draw over them. Fails like
    /// [`Editor::remove_image_background`], and with `NothingFound` when
    /// there is no drawing to trace.
    pub fn image_to_ink(
        &mut self,
        page: usize,
        id: Id,
        pixels: &Pixels,
        started_at_ms: i64,
    ) -> Result<Vec<Id>, ImageError> {
        self.end_erase();
        let target = self.diagram_target(page, id, pixels)?;
        let image = &target.image;
        let (w, h) = (pixels.width as f32, pixels.height as f32);
        let [l, t, r, b] = image.crop;
        let x0 = ((l * w).floor() as u32).min(pixels.width - 1);
        let y0 = ((t * h).floor() as u32).min(pixels.height - 1);
        let x1 = ((r * w).ceil() as u32).clamp(x0 + 1, pixels.width);
        let y1 = ((b * h).ceil() as u32).clamp(y0 + 1, pixels.height);
        let mut lines = trace(&pixels.region(x0, y0, x1, y1));
        // Fills go first so the lines are drawn over them.
        lines.sort_by_key(|l| !l.filled);
        if lines.is_empty() {
            return Err(ImageError::NothingFound);
        }

        // Pixels of the shown part per unit of the image's square, and the
        // page distance one of those pixels covers.
        let (shown_w, shown_h) = ((r - l) * w, (b - t) * h);
        let [sx, kx, _, ky, sy, _] = image.transform;
        let per_px = (sx * sy - kx * ky).abs().sqrt() / (shown_w * shown_h).sqrt();

        let base = self.doc.pages[page]
            .layers
            .iter()
            .find(|l| l.id == target.layer);
        let base = base.map_or(0, |l| l.strokes.len());
        let mut placed = Vec::with_capacity(lines.len());
        for (n, line) in lines.iter().enumerate() {
            let points = line
                .points
                .iter()
                .enumerate()
                .map(|(k, p)| {
                    let u = ((x0 as f32 + p[0] - l * w) / shown_w).clamp(0.0, 1.0);
                    let v = ((y0 as f32 + p[1] - t * h) / shown_h).clamp(0.0, 1.0);
                    let (x, y) = image.to_page(u, v);
                    InputSample::new(x, y, 1.0, k as f32)
                })
                .collect();
            // A fill is a bare polygon, like the fill tool makes: no width.
            let style = StrokeStyle {
                tool: if line.filled { Tool::Fill } else { Tool::Pen },
                color: line.color,
                width: line.width * per_px,
                line: LineStyle::Solid,
            };
            placed.push(Placed {
                layer: target.layer,
                index: base + n,
                stroke: Stroke::new(style, started_at_ms, points),
            });
        }
        let ids = placed.iter().map(|p| p.stroke.id).collect();
        self.apply_recorded(Step::Group(vec![
            Step::InsertStrokes {
                page: target.page,
                strokes: placed,
            },
            Step::RemoveImages {
                page: target.page,
                ids: vec![id],
            },
        ]));
        Ok(ids)
    }
}

#[cfg(test)]
mod tests;
