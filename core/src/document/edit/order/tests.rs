//! Drawing order tests.

use super::*;
use crate::document::image_fixtures::png_with_alpha;
use crate::document::{Document, Orientation, PaperSize, Stroke, StrokeStyle, Tool};
use crate::{InputSample, LineStyle, Rect};

fn editor() -> Editor {
    Editor::new(Document::new_paged(
        "Notes",
        PaperSize::A4,
        Orientation::Portrait,
    ))
}

fn line(y: f32) -> Stroke {
    let style = StrokeStyle {
        tool: Tool::Pen,
        color: 0x1122_33FF,
        width: 2.0,
        line: LineStyle::Solid,
    };
    let pts = vec![
        InputSample::new(0.0, y, 1.0, 0.0),
        InputSample::new(10.0, y, 1.0, 1.0),
    ];
    Stroke::new(style, 0, pts)
}

fn four(e: &mut Editor) -> Vec<Id> {
    (0..4)
        .map(|i| e.add_stroke(0, line(i as f32 * 10.0)).unwrap())
        .collect()
}

fn order(e: &Editor, layer: usize) -> Vec<Id> {
    e.document().pages[0].layers[layer]
        .strokes
        .iter()
        .map(|s| s.id)
        .collect()
}

#[test]
fn bring_to_front_keeps_relative_order_and_ids() {
    let mut e = editor();
    let ids = four(&mut e);
    assert!(e.bring_to_front(0, &[ids[2], ids[0]]));
    assert_eq!(order(&e, 0), vec![ids[1], ids[3], ids[0], ids[2]]);
}

#[test]
fn send_to_back_is_one_undo_step() {
    let mut e = editor();
    let ids = four(&mut e);
    let before = e.undo.len();
    assert!(e.send_to_back(0, &[ids[3], ids[2]]));
    assert_eq!(e.undo.len(), before + 1);
    assert_eq!(order(&e, 0), vec![ids[2], ids[3], ids[0], ids[1]]);
    e.undo();
    assert_eq!(order(&e, 0), ids);
    e.redo();
    assert_eq!(order(&e, 0), vec![ids[2], ids[3], ids[0], ids[1]]);
}

#[test]
fn nothing_changes_when_already_in_place() {
    let mut e = editor();
    let ids = four(&mut e);
    let before = e.undo.len();
    assert!(!e.bring_to_front(0, &[ids[3]]));
    assert!(!e.send_to_back(0, &[ids[0], ids[1]]));
    assert!(!e.bring_to_front(0, &[]));
    assert!(!e.bring_to_front(9, &[ids[0]]));
    assert_eq!(e.undo.len(), before);
}

#[test]
fn locked_layers_are_left_alone() {
    let mut e = editor();
    let ids = four(&mut e);
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(!e.bring_to_front(0, &[ids[0]]));
    assert_eq!(order(&e, 0), ids);
}

#[test]
fn images_reorder_within_their_layer() {
    let mut e = editor();
    let area = Rect {
        min_x: 0.0,
        min_y: 0.0,
        max_x: 40.0,
        max_y: 30.0,
    };
    let a = e.insert_image(0, png_with_alpha(4, 3), area).unwrap();
    let b = e.insert_image(0, png_with_alpha(4, 3), area).unwrap();
    assert!(e.bring_to_front(0, &[a]));
    let ids = |e: &Editor| -> Vec<Id> {
        e.document().pages[0].layers[0]
            .images
            .iter()
            .map(|i| i.id)
            .collect()
    };
    assert_eq!(ids(&e), vec![b, a]);
    e.undo();
    assert_eq!(ids(&e), vec![a, b]);
}
