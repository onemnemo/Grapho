//! The eraser gesture: whole strokes, or only the part under the eraser.

use super::layers::active_layer_in;
use super::{Editor, Placed, Step};
use crate::document::cut::{cut_stroke, Cut};
use crate::document::{Id, Stroke};

/// What the eraser takes away from a stroke it touches.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum EraseMode {
    /// The whole stroke.
    #[default]
    Stroke,
    /// Only the part the eraser passes over. The rest stays as separate
    /// strokes, and a fill is cut down to the area left.
    Area,
}

/// An eraser gesture in progress. Ink disappears as the eraser touches it,
/// and the whole gesture becomes one undo step when it ends.
#[derive(Debug)]
pub(super) struct Erasing {
    page: Id,
    mode: EraseMode,
    /// Whole-stroke mode: the strokes taken out, with where they sat.
    removed: Vec<Placed>,
    /// Area mode: each touched layer as it was before the gesture. A piece
    /// cut again later in the gesture is then restored along with the rest.
    saved: Vec<(Id, Vec<Stroke>)>,
}

impl Editor {
    /// Starts an eraser gesture on a page.
    pub fn begin_erase(&mut self, page: usize, mode: EraseMode) {
        self.end_erase();
        if let Some(p) = self.doc.pages.get(page) {
            self.erasing = Some(Erasing {
                page: p.id,
                mode,
                removed: Vec::new(),
                saved: Vec::new(),
            });
        }
    }

    /// Erases what a circle of radius `r` touches as it moves from `(x0, y0)`
    /// to `(x1, y1)`, as the gesture's mode says. Only the active layer is
    /// touched, and not at all while it is hidden or locked. Returns how many strokes were removed or cut.
    pub fn erase(&mut self, x0: f32, y0: f32, x1: f32, y1: f32, r: f32) -> usize {
        let Some(erasing) = &mut self.erasing else {
            return 0;
        };
        let Some(page) = self.doc.page_mut(erasing.page) else {
            return 0;
        };
        let active = active_layer_in(&self.active, page);
        let target = active
            .and_then(|i| page.layers.get_mut(i))
            .filter(|l| l.visible && !l.locked);
        let mode = erasing.mode;
        // Test points along the path, close enough that nothing slips between.
        let steps = ((x1 - x0).hypot(y1 - y0) / (r * 0.5).max(1e-3))
            .ceil()
            .min(1000.0) as usize;
        let mut touched = 0;
        if let Some(layer) = target {
            let mut i = 0;
            while i < layer.strokes.len() {
                if mode == EraseMode::Area {
                    let stroke = &layer.strokes[i];
                    let Cut::Pieces(pieces) = cut_stroke(stroke, x0, y0, x1, y1, r) else {
                        i += 1;
                        continue;
                    };
                    if !erasing.saved.iter().any(|(id, _)| *id == layer.id) {
                        erasing.saved.push((layer.id, layer.strokes.clone()));
                    }
                    let n = pieces.len();
                    layer.strokes.splice(i..=i, pieces);
                    i += n;
                    touched += 1;
                    continue;
                }
                let hit = (0..=steps).any(|k| {
                    let t = if steps == 0 {
                        1.0
                    } else {
                        k as f32 / steps as f32
                    };
                    layer.strokes[i].hits(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r)
                });
                if hit {
                    erasing.removed.push(Placed {
                        layer: layer.id,
                        index: i,
                        stroke: layer.strokes.remove(i),
                    });
                    touched += 1;
                } else {
                    i += 1;
                }
            }
        }
        touched
    }

    /// Ends the eraser gesture, recording it as one undo step if it changed
    /// anything.
    pub fn end_erase(&mut self) {
        let Some(Erasing {
            page,
            mut removed,
            saved,
            ..
        }) = self.erasing.take()
        else {
            return;
        };
        if !saved.is_empty() {
            self.record(Step::SwapLayers {
                page,
                layers: saved,
            });
        }
        if !removed.is_empty() {
            // Putting strokes back in reverse order restores every position.
            removed.reverse();
            self.record(Step::InsertStrokes {
                page,
                strokes: removed,
            });
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::{Document, Orientation, PaperSize, StrokeStyle, Tool};
    use crate::InputSample;

    fn editor() -> Editor {
        Editor::new(Document::new_paged(
            "Notes",
            PaperSize::A4,
            Orientation::Portrait,
        ))
    }

    fn stroke(tool: Tool, y: f32) -> Stroke {
        let pts = (0..=10)
            .map(|i| InputSample::new(i as f32 * 10.0, y, 0.8, i as f32 * 5.0))
            .collect();
        Stroke::new(
            StrokeStyle {
                tool,
                color: 0x1718_1CFF,
                width: 2.0,
                line: crate::LineStyle::Solid,
            },
            0,
            pts,
        )
    }

    fn strokes(e: &Editor) -> Vec<Stroke> {
        e.document().pages[0].layers[0].strokes.clone()
    }

    fn sweep(e: &mut Editor, x: f32, y: f32, r: f32) -> usize {
        e.begin_erase(0, EraseMode::Area);
        let n = e.erase(x, y, x, y, r);
        e.end_erase();
        n
    }

    #[test]
    fn a_cut_through_the_middle_gives_two_pieces() {
        let mut e = editor();
        e.add_stroke(0, stroke(Tool::Pen, 10.0));
        assert_eq!(sweep(&mut e, 50.0, 10.0, 4.0), 1);
        let s = strokes(&e);
        assert_eq!(s.len(), 2);
        assert!(s[0].points.last().unwrap().x < 50.0);
        assert!(s[1].points[0].x > 50.0);
    }

    #[test]
    fn an_eraser_held_still_does_not_keep_cutting() {
        let mut e = editor();
        e.add_stroke(0, stroke(Tool::Pen, 10.0));
        e.begin_erase(0, EraseMode::Area);
        assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 4.0), 1);
        let ids: Vec<_> = strokes(&e).iter().map(|s| s.id).collect();
        for _ in 0..5 {
            assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 4.0), 0);
        }
        e.end_erase();
        let after: Vec<_> = strokes(&e).iter().map(|s| s.id).collect();
        assert_eq!(ids, after);
    }

    #[test]
    fn a_cut_at_the_end_gives_one_piece() {
        let mut e = editor();
        e.add_stroke(0, stroke(Tool::Pen, 10.0));
        sweep(&mut e, 100.0, 10.0, 4.0);
        let s = strokes(&e);
        assert_eq!(s.len(), 1);
        assert_eq!(s[0].points[0].x, 0.0);
        assert!(s[0].points.last().unwrap().x < 100.0);
    }

    #[test]
    fn erasing_everything_leaves_no_pieces() {
        let mut e = editor();
        e.add_stroke(0, stroke(Tool::Pen, 10.0));
        e.begin_erase(0, EraseMode::Area);
        e.erase(-5.0, 10.0, 105.0, 10.0, 4.0);
        e.end_erase();
        assert!(strokes(&e).is_empty());
        e.undo();
        assert_eq!(strokes(&e).len(), 1);
    }

    #[test]
    fn undo_and_redo_restore_exactly_in_one_step() {
        let mut e = editor();
        e.add_stroke(0, stroke(Tool::Pen, 10.0));
        e.add_stroke(0, stroke(Tool::Pen, 20.0));
        e.add_stroke(0, stroke(Tool::Pen, 30.0));
        let before = strokes(&e);
        e.begin_erase(0, EraseMode::Area);
        // Cut the middle line, then cut one of its pieces again.
        e.erase(50.0, 15.0, 50.0, 25.0, 3.0);
        e.erase(20.0, 20.0, 20.0, 20.0, 3.0);
        e.end_erase();
        let after = strokes(&e);
        assert_eq!(after.len(), 5);
        assert_eq!(after[0], before[0]);
        assert_eq!(after[4], before[2]);
        assert_eq!(e.undo(), Some(0));
        assert_eq!(strokes(&e), before);
        assert_eq!(e.redo(), Some(0));
        assert_eq!(strokes(&e), after);
        e.undo();
        e.undo();
        assert_eq!(strokes(&e), before[..2].to_vec());
    }

    #[test]
    fn a_fill_is_cut_down_in_area_mode_and_undo_restores_it() {
        let mut e = editor();
        let mut fill = stroke(Tool::Fill, 10.0);
        fill.points.push(InputSample::new(50.0, 40.0, 1.0, 0.0));
        let id = fill.id;
        e.add_stroke(0, fill);
        let before = strokes(&e);
        assert_eq!(sweep(&mut e, 50.0, 10.0, 4.0), 1);
        let after = strokes(&e);
        assert!(after
            .iter()
            .all(|s| s.id != id && s.style.tool == Tool::Fill));
        e.undo();
        assert_eq!(strokes(&e), before);
        e.redo();
        assert_eq!(strokes(&e), after);
    }

    #[test]
    fn a_fill_out_of_reach_keeps_its_id() {
        let mut e = editor();
        let mut fill = stroke(Tool::Fill, 10.0);
        fill.points.push(InputSample::new(50.0, 40.0, 1.0, 0.0));
        let id = fill.id;
        e.add_stroke(0, fill);
        assert_eq!(sweep(&mut e, 50.0, 100.0, 4.0), 0);
        assert_eq!(strokes(&e)[0].id, id);
        assert!(!e.can_redo());
    }

    #[test]
    fn pieces_keep_the_style_and_locked_layers_stay_whole() {
        let mut e = editor();
        let original = stroke(Tool::Highlighter, 10.0);
        let style = original.style;
        e.add_stroke(0, original);
        sweep(&mut e, 50.0, 10.0, 4.0);
        assert!(strokes(&e).iter().all(|s| s.style == style));
        e.document_mut().pages[0].layers[0].locked = true;
        assert_eq!(sweep(&mut e, 20.0, 10.0, 4.0), 0);
        assert_eq!(strokes(&e).len(), 2);
    }
}

#[cfg(test)]
mod stress;
