//! Moving a selection from one page onto another.

use super::{mapped, Editor};
use crate::document::{Id, TextBox};

impl Editor {
    /// Moves the strokes, images and text boxes on page `from` onto the
    /// active layer of page `to`, placed by `m` (`[sx, kx, tx, ky, sy, ty]`,
    /// from `from`'s page-local units to `to`'s), as one undo step. On the
    /// same page it is [`Editor::transform`]. Anything on a locked layer
    /// stays where it is. Returns the new ids (strokes, images, then text
    /// boxes), or nothing, changing nothing, if nothing can move or the
    /// active layer of `to` is locked or hidden.
    pub fn move_to_page(&mut self, from: usize, ids: &[Id], to: usize, m: [f32; 6]) -> Vec<Id> {
        if from == to {
            return self.transform(from, ids, m);
        }
        self.end_erase();
        let mut movable: Vec<Id> = self
            .locate(from, ids, true)
            .unwrap_or_default()
            .into_iter()
            .map(|f| f.id)
            .collect();
        movable.extend(self.image_ids(from, ids));
        movable.extend(self.text_ids(from, ids));
        let Some(copied) = self.copies(from, &movable) else {
            return Vec::new();
        };
        let strokes = copied.strokes.iter().map(|s| mapped(s, m)).collect();
        let images = copied.images.iter().map(|i| i.transformed(m)).collect();
        let texts = copied
            .texts
            .iter()
            .map(|t| TextBox {
                id: Id::new_v4(),
                ..t.transformed(m)
            })
            .collect();
        let Some((inserts, moved)) = self.insert_steps(to, strokes, images, texts) else {
            return Vec::new();
        };
        let mut steps = self.remove_steps(from, &movable);
        steps.extend(inserts);
        self.apply_steps(steps);
        moved
    }
}
