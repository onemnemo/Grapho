//! Layer edit tests.

use super::*;
use crate::document::EraseMode;
use crate::document::{Document, FillView, Orientation, PaperSize, Stroke, StrokeStyle, Tool};
use crate::InputSample;

fn editor() -> Editor {
    Editor::new(Document::new_paged(
        "Notes",
        PaperSize::A4,
        Orientation::Portrait,
    ))
}

fn hline(y: f32) -> Stroke {
    Stroke::new(
        StrokeStyle {
            tool: Tool::Pen,
            color: 0x1122_33FF,
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

fn names(e: &Editor) -> Vec<String> {
    e.document().pages[0]
        .layers
        .iter()
        .map(|l| l.name.clone())
        .collect()
}

fn counts(e: &Editor) -> Vec<usize> {
    e.document().pages[0]
        .layers
        .iter()
        .map(|l| l.strokes.len())
        .collect()
}

fn ys(e: &Editor, layer: usize) -> Vec<f32> {
    e.document().pages[0].layers[layer]
        .strokes
        .iter()
        .map(|s| s.points[0].y)
        .collect()
}

/// Layers "Layer 1", "B", "C" bottom to top, the top one active.
fn three() -> Editor {
    let mut e = editor();
    e.add_layer(0, "B");
    e.add_layer(0, "C");
    e
}

#[test]
fn the_top_layer_is_active_until_one_is_chosen() {
    let mut e = three();
    assert_eq!(e.active_layer(0), Some(2));
    assert!(e.set_active_layer(0, 0));
    assert_eq!(e.active_layer(0), Some(0));
    assert!(!e.set_active_layer(0, 3));
    assert!(!e.set_active_layer(1, 0));
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(e.active_layer(1), None);
}

#[test]
fn strokes_go_to_the_active_layer() {
    let mut e = three();
    e.set_active_layer(0, 1);
    e.add_stroke(0, hline(5.0)).unwrap();
    assert_eq!(counts(&e), vec![0, 1, 0]);
    e.undo();
    assert_eq!(counts(&e), vec![0, 0, 0]);
}

#[test]
fn a_locked_or_hidden_active_layer_takes_no_ink() {
    let mut e = three();
    e.set_active_layer(0, 1);
    e.set_layer_locked(0, 1, true);
    assert!(e.add_stroke(0, hline(5.0)).is_none());
    e.set_layer_locked(0, 1, false);
    e.set_layer_visible(0, 1, false);
    assert!(e.add_stroke(0, hline(5.0)).is_none());
    let view = FillView {
        scale: 1.0,
        visible: (0.0, 0.0, 595.0, 842.0),
    };
    assert!(e.fill(0, 50.0, 50.0, 0xFF00_00FF, view).is_none());
    e.set_layer_visible(0, 1, true);
    assert!(e.fill(0, 50.0, 50.0, 0xFF00_00FF, view).is_some());
    assert_eq!(counts(&e), vec![0, 1, 0]);
}

#[test]
fn the_eraser_only_touches_the_active_layer() {
    let mut e = three();
    e.set_active_layer(0, 0);
    e.add_stroke(0, hline(10.0)).unwrap();
    e.set_active_layer(0, 2);
    e.add_stroke(0, hline(10.0)).unwrap();
    for mode in [EraseMode::Stroke, EraseMode::Area] {
        e.begin_erase(0, mode);
        assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 3.0), 1);
        e.end_erase();
        assert_eq!(counts(&e)[0], 1);
        e.undo();
        assert_eq!(counts(&e), vec![1, 0, 1]);
    }
    e.set_layer_locked(0, 2, true);
    e.begin_erase(0, EraseMode::Stroke);
    assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 3.0), 0);
    e.end_erase();
    e.set_layer_locked(0, 2, false);
    e.set_layer_visible(0, 2, false);
    e.begin_erase(0, EraseMode::Stroke);
    assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 3.0), 0);
    e.end_erase();
    assert_eq!(counts(&e), vec![1, 0, 1]);
}

#[test]
fn the_lasso_only_selects_on_the_active_layer() {
    let mut e = three();
    e.set_active_layer(0, 0);
    let low = e.add_stroke(0, hline(10.0)).unwrap();
    e.set_active_layer(0, 2);
    let high = e.add_stroke(0, hline(10.0)).unwrap();
    let lasso = [(-5.0, 0.0), (105.0, 0.0), (105.0, 20.0), (-5.0, 20.0)];
    assert_eq!(e.select(0, &lasso), vec![high]);
    e.set_active_layer(0, 0);
    assert_eq!(e.select(0, &lasso), vec![low]);
    e.set_layer_locked(0, 0, true);
    assert!(e.select(0, &lasso).is_empty());
    e.set_layer_locked(0, 0, false);
    e.set_layer_visible(0, 0, false);
    assert!(e.select(0, &lasso).is_empty());
}

#[test]
fn paste_and_duplicate_use_the_active_layer() {
    let mut e = three();
    e.set_active_layer(0, 0);
    let id = e.add_stroke(0, hline(10.0)).unwrap();
    assert!(e.copy(0, &[id]));
    e.set_active_layer(0, 1);
    assert_eq!(e.paste(0, 50.0, 50.0).len(), 1);
    assert_eq!(e.duplicate(0, &[id], 0.0, 5.0).len(), 1);
    assert_eq!(counts(&e), vec![1, 2, 0]);
    e.set_layer_locked(0, 1, true);
    assert!(e.paste(0, 50.0, 50.0).is_empty());
    assert!(e.duplicate(0, &[id], 0.0, 5.0).is_empty());
    e.set_layer_locked(0, 1, false);
    e.set_layer_visible(0, 1, false);
    assert!(e.paste(0, 50.0, 50.0).is_empty());
    assert_eq!(counts(&e), vec![1, 2, 0]);
}

#[test]
fn add_layer_goes_above_the_active_layer_and_becomes_active() {
    let mut e = editor();
    e.add_layer(0, "B");
    e.add_layer(0, "C");
    e.set_active_layer(0, 0);
    assert_eq!(e.add_layer(0, "Mid"), Some(1));
    assert_eq!(names(&e), vec!["Layer 1", "Mid", "B", "C"]);
    assert_eq!(e.active_layer(0), Some(1));
    assert_eq!(e.undo(), Some(0));
    assert_eq!(names(&e), vec!["Layer 1", "B", "C"]);
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(e.redo(), Some(0));
    assert_eq!(names(&e), vec!["Layer 1", "Mid", "B", "C"]);
    assert_eq!(e.active_layer(0), Some(1));
}

#[test]
fn add_layer_stops_at_the_cap() {
    let mut e = editor();
    for _ in 1..MAX_LAYERS {
        assert!(e.add_layer(0, "L").is_some());
    }
    assert_eq!(e.document().pages[0].layers.len(), MAX_LAYERS);
    assert_eq!(e.add_layer(0, "One too many"), None);
    assert_eq!(e.document().pages[0].layers.len(), MAX_LAYERS);
    assert_eq!(e.add_layer(5, "Nowhere"), None);
}

#[test]
fn remove_layer_takes_its_strokes_and_undo_restores_them() {
    let mut e = three();
    e.set_active_layer(0, 1);
    e.add_stroke(0, hline(1.0)).unwrap();
    e.add_stroke(0, hline(2.0)).unwrap();
    e.set_layer_opacity(0, 1, 0.5);
    e.set_layer_locked(0, 1, true);
    let before = e.document().pages[0].layers[1].clone();
    assert!(e.remove_layer(0, 1));
    assert_eq!(names(&e), vec!["Layer 1", "C"]);
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(e.undo(), Some(0));
    assert_eq!(e.document().pages[0].layers[1], before);
    assert_eq!(names(&e), vec!["Layer 1", "B", "C"]);
    assert_eq!(e.redo(), Some(0));
    assert_eq!(names(&e), vec!["Layer 1", "C"]);
}

#[test]
fn removing_the_bottom_active_layer_activates_the_new_bottom() {
    let mut e = three();
    e.set_active_layer(0, 0);
    assert!(e.remove_layer(0, 0));
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(names(&e), vec!["B", "C"]);
}

#[test]
fn removing_another_layer_keeps_the_active_one() {
    let mut e = three();
    e.set_active_layer(0, 1);
    assert!(e.remove_layer(0, 2));
    assert_eq!(e.active_layer(0), Some(1));
    assert!(e.remove_layer(0, 0));
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(names(&e), vec!["B"]);
}

#[test]
fn the_last_layer_cannot_be_removed() {
    let mut e = editor();
    assert!(!e.remove_layer(0, 0));
    assert!(!e.can_undo());
    let mut e = three();
    assert!(!e.remove_layer(0, 3));
    assert!(!e.remove_layer(2, 0));
}

#[test]
fn move_layer_reorders_and_keeps_the_active_layer() {
    let mut e = three();
    e.set_active_layer(0, 1);
    assert!(e.move_layer(0, 0, 2));
    assert_eq!(names(&e), vec!["B", "C", "Layer 1"]);
    assert_eq!(e.active_layer(0), Some(0));
    assert_eq!(e.undo(), Some(0));
    assert_eq!(names(&e), vec!["Layer 1", "B", "C"]);
    assert_eq!(e.active_layer(0), Some(1));
    assert_eq!(e.redo(), Some(0));
    assert_eq!(names(&e), vec!["B", "C", "Layer 1"]);
    assert!(e.move_layer(0, 2, 0));
    assert_eq!(names(&e), vec!["Layer 1", "B", "C"]);
}

#[test]
fn move_layer_rejects_bad_indices_and_no_ops() {
    let mut e = three();
    assert!(!e.move_layer(0, 0, 3));
    assert!(!e.move_layer(0, 3, 0));
    assert!(!e.move_layer(0, 1, 1));
    assert!(!e.move_layer(4, 0, 1));
    assert!(e.can_undo());
    e.undo();
    e.undo();
    assert!(!e.can_undo());
}

#[test]
fn merge_down_appends_above_the_lower_strokes() {
    let mut e = three();
    e.set_active_layer(0, 1);
    e.add_stroke(0, hline(1.0)).unwrap();
    e.add_stroke(0, hline(2.0)).unwrap();
    e.set_active_layer(0, 2);
    e.add_stroke(0, hline(3.0)).unwrap();
    e.add_stroke(0, hline(4.0)).unwrap();
    assert!(e.merge_down(0, 2));
    assert_eq!(names(&e), vec!["Layer 1", "B"]);
    assert_eq!(ys(&e, 1), vec![1.0, 2.0, 3.0, 4.0]);
    assert_eq!(e.active_layer(0), Some(1));
    assert_eq!(e.undo(), Some(0));
    assert_eq!(names(&e), vec!["Layer 1", "B", "C"]);
    assert_eq!(ys(&e, 1), vec![1.0, 2.0]);
    assert_eq!(ys(&e, 2), vec![3.0, 4.0]);
    assert_eq!(e.redo(), Some(0));
    assert_eq!(ys(&e, 1), vec![1.0, 2.0, 3.0, 4.0]);
    assert_eq!(counts(&e), vec![0, 4]);
}

#[test]
fn merge_down_fails_at_the_bottom_and_for_locked_layers() {
    let mut e = three();
    assert!(!e.merge_down(0, 0));
    assert!(!e.merge_down(0, 3));
    e.set_layer_locked(0, 2, true);
    assert!(!e.merge_down(0, 2));
    e.set_layer_locked(0, 2, false);
    e.set_layer_locked(0, 1, true);
    assert!(!e.merge_down(0, 2));
    assert_eq!(names(&e).len(), 3);
    e.set_layer_locked(0, 1, false);
    e.set_active_layer(0, 0);
    assert!(e.merge_down(0, 2));
    assert_eq!(e.active_layer(0), Some(0));
}

#[test]
fn property_setters_apply_directly_without_undo() {
    let mut e = three();
    assert!(e.rename_layer(0, 1, "Sketch"));
    assert!(e.set_layer_visible(0, 1, false));
    assert!(e.set_layer_locked(0, 1, true));
    assert!(e.set_layer_opacity(0, 1, 0.25));
    let l = &e.document().pages[0].layers[1];
    assert_eq!(l.name, "Sketch");
    assert!(!l.visible && l.locked);
    assert_eq!(l.opacity, 0.25);
    assert!(e.set_layer_opacity(0, 1, 7.0));
    assert_eq!(e.document().pages[0].layers[1].opacity, 1.0);
    assert!(e.set_layer_opacity(0, 1, -1.0));
    assert_eq!(e.document().pages[0].layers[1].opacity, 0.0);
    assert!(!e.set_layer_opacity(0, 1, f32::NAN));
    assert!(!e.rename_layer(0, 3, "x"));
    assert!(!e.set_layer_visible(1, 0, true));
    assert!(!e.set_layer_locked(0, 9, true));
    assert!(!e.set_layer_opacity(0, 9, 0.5));
    // Only the two add_layer calls from setup are on the undo stack.
    e.undo();
    e.undo();
    assert!(!e.can_undo());
}

#[test]
fn layer_edits_end_an_erase_in_progress() {
    let mut e = editor();
    e.add_stroke(0, hline(10.0)).unwrap();
    e.begin_erase(0, EraseMode::Stroke);
    assert_eq!(e.erase(50.0, 10.0, 50.0, 10.0, 3.0), 1);
    e.add_layer(0, "B");
    assert_eq!(e.undo(), Some(0));
    assert_eq!(counts(&e), vec![0]);
    assert_eq!(e.undo(), Some(0));
    assert_eq!(counts(&e), vec![1]);
}
