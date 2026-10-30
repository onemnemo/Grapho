//! Text box edit tests.

use super::*;
use crate::document::{
    Document, Orientation, PaperSize, Stroke, StrokeStyle, TextAlign, Tool, MAX_TEXT_CHARS,
};
use crate::InputSample;

fn editor() -> Editor {
    Editor::new(Document::new_paged(
        "Notes",
        PaperSize::A4,
        Orientation::Portrait,
    ))
}

fn board() -> Editor {
    Editor::new(Document::new_infinite("Board"))
}

fn style() -> TextStyle {
    TextStyle::default()
}

fn add(e: &mut Editor, x: f32, y: f32, text: &str) -> Id {
    e.add_text(0, [x, y, 100.0, 30.0], text, style()).unwrap()
}

fn texts(e: &Editor, layer: usize) -> Vec<Id> {
    e.document().pages[0].layers[layer]
        .texts
        .iter()
        .map(|t| t.id)
        .collect()
}

fn square(x0: f32, y0: f32, x1: f32, y1: f32) -> Vec<(f32, f32)> {
    vec![(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
}

fn dash(y: f32) -> Stroke {
    let style = StrokeStyle {
        tool: Tool::Pen,
        color: 0x1122_33FF,
        width: 2.0,
        line: crate::LineStyle::Solid,
    };
    let pts = vec![
        InputSample::new(0.0, y, 1.0, 0.0),
        InputSample::new(10.0, y, 1.0, 1.0),
    ];
    Stroke::new(style, 0, pts)
}

#[test]
fn adding_a_text_box_is_one_undo_step() {
    let mut e = editor();
    let id = add(&mut e, 10.0, 20.0, "Hello");
    let t = e.text_box(0, id).unwrap();
    assert_eq!((t.x, t.y, t.width, t.height), (10.0, 20.0, 100.0, 30.0));
    assert_eq!(t.text, "Hello");
    assert_eq!(e.undo(), Some(0));
    assert!(e.text_box(0, id).is_none());
    assert_eq!(e.redo(), Some(0));
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello");
}

#[test]
fn a_bad_box_or_target_is_refused_without_a_step() {
    let mut e = editor();
    let bad = |e: &mut Editor, frame: [f32; 4], s: TextStyle| e.add_text(0, frame, "x", s);
    assert_eq!(
        bad(&mut e, [0.0, 0.0, 0.0, 10.0], style()),
        Err(TextError::BadBox)
    );
    assert_eq!(
        bad(&mut e, [f32::NAN, 0.0, 10.0, 10.0], style()),
        Err(TextError::BadBox)
    );
    let tiny = TextStyle {
        size: 1.0,
        ..style()
    };
    assert_eq!(
        bad(&mut e, [0.0, 0.0, 10.0, 10.0], tiny),
        Err(TextError::BadBox)
    );
    let long = "a".repeat(MAX_TEXT_CHARS + 1);
    assert_eq!(
        e.add_text(0, [0.0, 0.0, 10.0, 10.0], &long, style()),
        Err(TextError::TooLong)
    );
    assert_eq!(
        e.add_text(3, [0.0, 0.0, 10.0, 10.0], "x", style()),
        Err(TextError::NoLayer)
    );
    e.set_layer_locked(0, 0, true);
    assert_eq!(
        e.add_text(0, [0.0, 0.0, 10.0, 10.0], "x", style()),
        Err(TextError::NoLayer)
    );
    assert!(!e.can_undo());
}

#[test]
fn text_goes_on_the_active_layer_on_top() {
    let mut e = editor();
    let a = add(&mut e, 0.0, 0.0, "a");
    let b = add(&mut e, 0.0, 50.0, "b");
    e.add_layer(0, "Top");
    let c = add(&mut e, 0.0, 100.0, "c");
    assert_eq!(texts(&e, 0), vec![a, b]);
    assert_eq!(texts(&e, 1), vec![c]);
    assert_eq!(e.document().pages[0].text_count(), 3);
}

#[test]
fn text_at_finds_the_topmost_box_on_a_usable_layer() {
    let mut e = editor();
    let low = add(&mut e, 0.0, 0.0, "low");
    let high = add(&mut e, 50.0, 0.0, "high");
    assert_eq!(e.text_at(0, 10.0, 10.0), Some(low));
    assert_eq!(e.text_at(0, 60.0, 10.0), Some(high));
    assert_eq!(e.text_at(0, 75.0, 10.0), Some(high));
    assert_eq!(e.text_at(0, 300.0, 10.0), None);
    e.set_layer_locked(0, 0, true);
    assert_eq!(e.text_at(0, 10.0, 10.0), None);
    e.set_layer_locked(0, 0, false);
    e.set_layer_visible(0, 0, false);
    assert_eq!(e.text_at(0, 10.0, 10.0), None);
}

#[test]
fn setting_text_keeps_the_id_and_is_one_undo_step() {
    let mut e = editor();
    let id = add(&mut e, 10.0, 20.0, "Hello");
    assert_eq!(e.set_text(0, id, "Hello there"), Ok(true));
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello there");
    assert_eq!(e.set_text(0, id, "Hello there"), Ok(false));
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello");
    e.undo();
    assert!(e.text_box(0, id).is_none());
    e.redo();
    e.redo();
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello there");
    let long = "a".repeat(MAX_TEXT_CHARS + 1);
    assert_eq!(e.set_text(0, id, &long), Err(TextError::TooLong));
    assert_eq!(e.set_text(0, Id::new_v4(), "x"), Err(TextError::NotFound));
}

#[test]
fn setting_the_style_changes_every_look_at_once() {
    let mut e = editor();
    let id = add(&mut e, 10.0, 20.0, "Hello");
    let look = TextStyle {
        size: 32.0,
        color: 0xD62F_2F80,
        align: TextAlign::End,
        bold: true,
        italic: true,
        underline: true,
    };
    assert_eq!(e.set_text_style(0, id, look), Ok(true));
    let t = e.text_box(0, id).unwrap();
    assert_eq!(t.style(), look);
    assert_eq!(e.set_text_style(0, id, look), Ok(false));
    // The size is held to its limits, not refused.
    let huge = TextStyle {
        size: 9000.0,
        ..look
    };
    assert_eq!(e.set_text_style(0, id, huge), Ok(true));
    assert_eq!(e.text_box(0, id).unwrap().size, MAX_TEXT_SIZE);
    let nan = TextStyle {
        size: f32::NAN,
        ..look
    };
    assert_eq!(e.set_text_style(0, id, nan), Err(TextError::BadBox));
    e.undo();
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().style(), TextStyle::default());
}

#[test]
fn the_frame_can_be_written_back_far_from_the_origin() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 7000.0, "Hello");
    assert_eq!(
        e.set_text_frame(0, id, [5000.0, 7000.0, 100.0, 64.5]),
        Ok(true)
    );
    assert_eq!(e.text_box(0, id).unwrap().height, 64.5);
    assert_eq!(
        e.set_text_frame(0, id, [5000.0, 7000.0, 100.0, 0.0]),
        Err(TextError::BadBox)
    );
    assert_eq!(e.text_at(0, 5050.0, 7060.0), Some(id));
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().height, 30.0);
}

#[test]
fn update_replaces_everything_in_one_step() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 5000.0, "Hello");
    let mut next = e.text_box(0, id).unwrap().clone();
    next.text = "Bye".into();
    next.x = 5200.0;
    next.rotation = 0.5;
    next.bold = true;
    next.italic = true;
    next.underline = true;
    assert_eq!(e.update_text(0, next.clone()), Ok(true));
    assert_eq!(e.update_text(0, next.clone()), Ok(false));
    assert_eq!(e.text_box(0, id).unwrap(), &next);
    next.width = 0.0;
    assert_eq!(e.update_text(0, next), Err(TextError::BadBox));
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello");
    let back = e.text_box(0, id).unwrap();
    assert!(!back.bold && !back.italic && !back.underline);
    e.redo();
    let again = e.text_box(0, id).unwrap();
    assert!(again.italic && again.underline);
}

#[test]
fn a_locked_layer_keeps_its_text_as_it_is() {
    let mut e = editor();
    let id = add(&mut e, 10.0, 10.0, "Hello");
    e.set_layer_locked(0, 0, true);
    assert_eq!(e.set_text(0, id, "Nope"), Err(TextError::NotFound));
    assert!(e
        .transform(0, &[id], [1.0, 0.0, 5.0, 0.0, 1.0, 0.0])
        .is_empty());
    assert!(!e.delete(0, &[id]));
    assert_eq!(e.text_box(0, id).unwrap().text, "Hello");
}

#[test]
fn the_lasso_selects_boxes_it_mostly_covers() {
    let mut e = board();
    let inside = add(&mut e, 5010.0, 5010.0, "in");
    let half = add(&mut e, 5090.0, 5010.0, "half");
    let out = add(&mut e, 5400.0, 5010.0, "out");
    e.add_stroke(0, dash(5020.0)).unwrap();
    let got = e.select(0, &square(5000.0, 5000.0, 5150.0, 5100.0));
    assert!(got.contains(&inside) && got.contains(&half));
    assert!(!got.contains(&out));
    // Only 30 of the 100 points across the second box are inside.
    let got = e.select(0, &square(5000.0, 5000.0, 5120.0, 5100.0));
    assert_eq!(got, vec![inside]);
}

#[test]
fn bounds_cover_the_corners_of_a_turned_box() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 5000.0, "x");
    let mut t = e.text_box(0, id).unwrap().clone();
    t.rotation = std::f32::consts::FRAC_PI_2;
    e.update_text(0, t).unwrap();
    // 100 by 30 about (5050, 5015) turned a quarter: 30 wide, 100 tall.
    let r = e.ink_bounds(0, &[id]).unwrap();
    assert!((r.min_x - 5035.0).abs() < 0.01 && (r.max_x - 5065.0).abs() < 0.01);
    assert!((r.min_y - 4965.0).abs() < 0.01 && (r.max_y - 5065.0).abs() < 0.01);
}

#[test]
fn transform_moves_a_box_with_strokes_in_one_step_and_keeps_its_id() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 5000.0, "Hello");
    let s = e.add_stroke(0, dash(5000.0)).unwrap();
    let moved = e.transform(0, &[s, id], [1.0, 0.0, 40.0, 0.0, 1.0, -10.0]);
    assert_eq!(moved.len(), 2);
    assert_eq!(moved[1], id);
    let t = e.text_box(0, id).unwrap();
    assert_eq!((t.x, t.y), (5040.0, 4990.0));
    e.undo();
    let t = e.text_box(0, id).unwrap();
    assert_eq!((t.x, t.y), (5000.0, 5000.0));
    assert_eq!(e.document().pages[0].layers[0].strokes.len(), 1);
}

#[test]
fn transform_scales_and_turns_the_box() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 5000.0, "Hello");
    e.transform(0, &[id], [2.0, 0.0, 0.0, 0.0, 2.0, 0.0]);
    let t = e.text_box(0, id).unwrap();
    assert!((t.width - 200.0).abs() < 0.01 && (t.height - 60.0).abs() < 0.01);
    assert!((t.size - 36.0).abs() < 0.01);
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().size, 18.0);
}

#[test]
fn recolor_colours_the_words_and_keeps_alpha() {
    let mut e = editor();
    let id = e
        .add_text(
            0,
            [0.0, 0.0, 50.0, 20.0],
            "x",
            TextStyle {
                color: 0x1122_3380,
                ..style()
            },
        )
        .unwrap();
    let got = e.recolor(0, &[id], 0xAABB_CCFF);
    assert_eq!(got, vec![id]);
    assert_eq!(e.text_box(0, id).unwrap().color, 0xAABB_CC80);
    // Width has nothing to change on a text box, and the id comes back.
    assert_eq!(e.set_width(0, &[id], 4.0), vec![id]);
    assert!(e.undo().is_some());
    assert_eq!(e.text_box(0, id).unwrap().color, 0x1122_3380);
}

#[test]
fn delete_removes_boxes_and_undo_puts_them_back_in_place() {
    let mut e = editor();
    let a = add(&mut e, 0.0, 0.0, "a");
    let b = add(&mut e, 0.0, 40.0, "b");
    let c = add(&mut e, 0.0, 80.0, "c");
    assert!(e.delete(0, &[a, c]));
    assert_eq!(texts(&e, 0), vec![b]);
    e.undo();
    assert_eq!(texts(&e, 0), vec![a, b, c]);
    e.redo();
    assert_eq!(texts(&e, 0), vec![b]);
    assert!(!e.delete(0, &[a]));
}

#[test]
fn copy_paste_and_duplicate_make_new_boxes() {
    let mut e = board();
    let id = add(&mut e, 5000.0, 6000.0, "Hello");
    assert!(e.copy(0, &[id]));
    assert!(e.has_clipboard());
    let pasted = e.paste(0, 5500.0, 6500.0);
    assert_eq!(pasted.len(), 1);
    assert_ne!(pasted[0], id);
    let t = e.text_box(0, pasted[0]).unwrap();
    // Centred on the point: the box is 100 by 30.
    assert_eq!((t.x, t.y), (5450.0, 6485.0));
    assert_eq!(t.text, "Hello");
    let dup = e.duplicate(0, &[id], 12.0, 8.0);
    let d = e.text_box(0, dup[0]).unwrap();
    assert_eq!((d.x, d.y), (5012.0, 6008.0));
    assert_eq!(e.document().pages[0].text_count(), 3);
    e.undo();
    e.undo();
    assert_eq!(e.document().pages[0].text_count(), 1);
}

#[test]
fn ordering_moves_boxes_within_their_layer() {
    let mut e = editor();
    let a = add(&mut e, 0.0, 0.0, "a");
    let b = add(&mut e, 0.0, 40.0, "b");
    let c = add(&mut e, 0.0, 80.0, "c");
    assert!(e.bring_to_front(0, &[a]));
    assert_eq!(texts(&e, 0), vec![b, c, a]);
    assert!(e.send_to_back(0, &[c]));
    assert_eq!(texts(&e, 0), vec![c, b, a]);
    e.undo();
    assert_eq!(texts(&e, 0), vec![b, c, a]);
    e.undo();
    assert_eq!(texts(&e, 0), vec![a, b, c]);
    assert!(!e.bring_to_front(0, &[c]));
}

#[test]
fn merging_layers_carries_the_boxes_and_undo_returns_them() {
    let mut e = editor();
    let low = add(&mut e, 0.0, 0.0, "low");
    e.add_layer(0, "Top");
    let high = add(&mut e, 0.0, 40.0, "high");
    assert!(e.merge_down(0, 1));
    assert_eq!(texts(&e, 0), vec![low, high]);
    e.undo();
    assert_eq!(texts(&e, 0), vec![low]);
    assert_eq!(texts(&e, 1), vec![high]);
    e.redo();
    assert_eq!(texts(&e, 0), vec![low, high]);
}

#[test]
fn removing_a_layer_undoes_with_its_boxes() {
    let mut e = editor();
    e.add_layer(0, "Top");
    let id = add(&mut e, 0.0, 0.0, "x");
    assert!(e.remove_layer(0, 1));
    assert!(e.text_box(0, id).is_none());
    e.undo();
    assert_eq!(e.text_box(0, id).unwrap().text, "x");
}

#[test]
fn a_saved_note_keeps_the_edited_boxes() {
    let mut e = board();
    let id = add(&mut e, 5000.5, 7000.25, "Saved");
    e.set_text(0, id, "Saved and changed").unwrap();
    let mut buf = std::io::Cursor::new(Vec::new());
    crate::document::save(e.document(), &mut buf).unwrap();
    buf.set_position(0);
    let back = crate::document::load(buf).unwrap();
    assert_eq!(&back, e.document());
    assert_eq!(back.pages[0].layers[0].texts[0].text, "Saved and changed");
}
