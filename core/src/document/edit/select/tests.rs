//! Selection edit tests.

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

fn stroke(tool: Tool, color: u32, width: f32, pts: &[(f32, f32)]) -> Stroke {
    let points = pts
        .iter()
        .enumerate()
        .map(|(i, &(x, y))| InputSample::new(x, y, 0.5, i as f32))
        .collect();
    Stroke::new(
        StrokeStyle {
            tool,
            color,
            width,
            line: crate::LineStyle::Solid,
        },
        7,
        points,
    )
}

fn hline(y: f32, x0: f32, x1: f32) -> Stroke {
    stroke(Tool::Pen, 0x1122_33FF, 2.0, &[(x0, y), (x1, y)])
}

fn add(e: &mut Editor, s: Stroke) -> Id {
    e.add_stroke(0, s).unwrap()
}

fn square(x0: f32, y0: f32, x1: f32, y1: f32) -> Vec<(f32, f32)> {
    vec![(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
}

fn layer_ids(e: &Editor, layer: usize) -> Vec<Id> {
    e.document().pages[0].layers[layer]
        .strokes
        .iter()
        .map(|s| s.id)
        .collect()
}

fn layer_strokes(e: &Editor) -> Vec<Stroke> {
    e.document().pages[0].layers[0].strokes.clone()
}

fn run(y: f32, xs: &[f32]) -> Stroke {
    let pts: Vec<(f32, f32)> = xs.iter().map(|&x| (x, y)).collect();
    stroke(Tool::Pen, 0xFF, 2.0, &pts)
}

#[test]
fn selects_strokes_at_least_half_inside() {
    let mut e = editor();
    let inside = add(&mut e, run(10.0, &[2.0, 4.0, 8.0]));
    let half = add(&mut e, run(20.0, &[0.0, 5.0, 10.0, 15.0, 20.0]));
    let mostly_out = add(&mut e, run(30.0, &[0.0, 5.0, 10.0, 15.0, 20.0, 25.0]));
    let outside = add(&mut e, run(40.0, &[20.0, 30.0]));
    let lasso = square(0.0, 0.0, 10.0, 50.0);
    let got = e.select(0, &lasso);
    assert_eq!(got, vec![inside, half]);
    assert!(!got.contains(&mostly_out) && !got.contains(&outside));
    assert!(e.select(0, &lasso[..2]).is_empty());
}

#[test]
fn selects_single_points_and_skips_hidden_and_locked_layers() {
    let mut e = editor();
    let dot = add(&mut e, stroke(Tool::Pen, 0xFF, 2.0, &[(5.0, 5.0)]));
    let far = add(&mut e, stroke(Tool::Pen, 0xFF, 2.0, &[(50.0, 5.0)]));
    let lasso = square(0.0, 0.0, 10.0, 10.0);
    assert_eq!(e.select(0, &lasso), vec![dot]);
    assert_eq!(e.select(0, &square(0.0, 0.0, 99.0, 99.0)), vec![dot, far]);
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(e.select(0, &lasso).is_empty());
    e.document_mut().pages[0].layers[0].locked = false;
    e.document_mut().pages[0].layers[0].visible = false;
    assert!(e.select(0, &lasso).is_empty());
    assert!(e.select(5, &lasso).is_empty());
}

#[test]
fn selects_a_fill_by_its_closed_boundary() {
    let mut e = editor();
    let tri = stroke(
        Tool::Fill,
        0xFF00_00FF,
        0.0,
        &[(2.0, 2.0), (8.0, 2.0), (5.0, 8.0)],
    );
    let id = add(&mut e, tri);
    assert_eq!(e.select(0, &square(0.0, 0.0, 10.0, 10.0)), vec![id]);
    assert!(e.select(0, &square(20.0, 20.0, 30.0, 30.0)).is_empty());
}

fn five(e: &mut Editor) -> Vec<Id> {
    (0..5)
        .map(|i| add(e, hline(10.0 * i as f32 + 10.0, 0.0, 10.0)))
        .collect()
}

#[test]
fn transform_undo_redo_restores_exact_strokes_and_order() {
    let mut e = editor();
    let ids = five(&mut e);
    let before = layer_strokes(&e);
    let pick = [ids[3], ids[1], ids[4]];
    let new = e.transform(0, &pick, [1.0, 0.0, 100.0, 0.0, 1.0, 0.0]);
    assert_eq!(new.len(), 3);
    let after = layer_strokes(&e);
    let order: Vec<Id> = after.iter().map(|s| s.id).collect();
    assert_eq!(order, vec![ids[0], new[1], ids[2], new[0], new[2]]);
    assert_eq!(after[1].points[0].x, 100.0);
    assert_eq!(after[1].points[0].y, 20.0);
    assert_eq!(after[1].points[1].time_ms, 1.0);
    assert!(e.undo().is_some());
    assert_eq!(layer_strokes(&e), before);
    assert!(e.redo().is_some());
    assert_eq!(layer_strokes(&e), after);
    e.undo();
    assert_eq!(layer_strokes(&e), before);
}

#[test]
fn transform_keeps_layer_of_each_stroke() {
    let mut e = editor();
    let a = add(&mut e, hline(10.0, 0.0, 10.0));
    e.document_mut().pages[0].add_layer("Top");
    let b = add(&mut e, hline(20.0, 0.0, 10.0));
    let c = add(&mut e, hline(30.0, 0.0, 10.0));
    let before: Vec<_> = (0..2)
        .map(|l| e.document().pages[0].layers[l].strokes.clone())
        .collect();
    let new = e.transform(0, &[c, a, b], [2.0, 0.0, 0.0, 0.0, 2.0, 0.0]);
    assert_eq!(layer_ids(&e, 0), vec![new[1]]);
    assert_eq!(layer_ids(&e, 1), vec![new[2], new[0]]);
    e.undo();
    assert_eq!(e.document().pages[0].layers[0].strokes, before[0]);
    assert_eq!(e.document().pages[0].layers[1].strokes, before[1]);
}

#[test]
fn transform_scales_width_but_not_fill_width() {
    let mut e = editor();
    let pen = add(&mut e, hline(10.0, 0.0, 10.0));
    let fill = add(
        &mut e,
        stroke(Tool::Fill, 0xFF, 0.0, &[(0.0, 0.0), (4.0, 0.0), (0.0, 4.0)]),
    );
    let new = e.transform(0, &[pen, fill], [3.0, 0.0, 0.0, 0.0, 3.0, 0.0]);
    let strokes = layer_strokes(&e);
    assert!((strokes[0].style.width - 6.0).abs() < 1e-5);
    assert_eq!(strokes[1].style.width, 0.0);
    assert_eq!(strokes[0].points[0].pressure, 0.5);
    assert_eq!(new, vec![strokes[0].id, strokes[1].id]);
}

#[test]
fn nothing_is_recorded_when_nothing_changes() {
    let mut e = editor();
    let id = add(&mut e, hline(10.0, 0.0, 10.0));
    e.undo();
    e.redo();
    let missing = Id::new_v4();
    assert!(e
        .transform(0, &[missing], [1.0, 0.0, 0.0, 0.0, 1.0, 0.0])
        .is_empty());
    assert!(e.recolor(0, &[], 0xFF).is_empty());
    assert!(!e.delete(0, &[missing]));
    assert!(!e.copy(0, &[missing]));
    assert!(e.duplicate(0, &[missing], 1.0, 1.0).is_empty());
    assert!(e.paste(0, 1.0, 1.0).is_empty());
    assert!(e
        .transform(9, &[id], [1.0, 0.0, 0.0, 0.0, 1.0, 0.0])
        .is_empty());
    assert_eq!(e.undo.len(), 1);
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(e
        .transform(0, &[id], [1.0, 0.0, 0.0, 0.0, 1.0, 0.0])
        .is_empty());
    assert!(!e.delete(0, &[id]));
    assert_eq!(e.undo.len(), 1);
}

#[test]
fn recolor_keeps_each_alpha() {
    let mut e = editor();
    let a = add(
        &mut e,
        stroke(Tool::Pen, 0x0102_0380, 2.0, &[(0.0, 0.0), (5.0, 0.0)]),
    );
    let b = add(
        &mut e,
        stroke(Tool::Pen, 0x0405_06FF, 2.0, &[(0.0, 5.0), (5.0, 5.0)]),
    );
    let new = e.recolor(0, &[a, b], 0xAABB_CC10);
    let s = layer_strokes(&e);
    assert_eq!(s[0].style.color, 0xAABB_CC80);
    assert_eq!(s[1].style.color, 0xAABB_CCFF);
    assert_eq!(new, vec![s[0].id, s[1].id]);
    e.undo();
    assert_eq!(layer_strokes(&e)[0].id, a);
    assert_eq!(layer_strokes(&e)[0].style.color, 0x0102_0380);
}

#[test]
fn set_width_is_one_step_and_skips_fills() {
    let mut e = editor();
    let a = add(&mut e, hline(0.0, 0.0, 5.0));
    let b = add(&mut e, hline(5.0, 0.0, 5.0));
    let f = add(
        &mut e,
        stroke(Tool::Fill, 0xFF, 0.0, &[(0.0, 0.0), (5.0, 5.0)]),
    );
    let steps = e.undo.len();
    let new = e.set_width(0, &[a, b, f], 6.0);
    assert_eq!(e.undo.len(), steps + 1);
    let s = layer_strokes(&e);
    assert_eq!(
        (s[0].style.width, s[1].style.width, s[2].style.width),
        (6.0, 6.0, 0.0)
    );
    assert_eq!(new, vec![s[0].id, s[1].id, s[2].id]);
    e.undo();
    assert_eq!(layer_strokes(&e)[0].style.width, 2.0);
    assert_eq!(layer_ids(&e, 0), vec![a, b, f]);
}

#[test]
fn delete_is_one_undoable_step() {
    let mut e = editor();
    let ids = five(&mut e);
    let before = layer_strokes(&e);
    assert!(e.delete(0, &[ids[3], ids[0], ids[1]]));
    assert_eq!(layer_ids(&e, 0), vec![ids[2], ids[4]]);
    e.undo();
    assert_eq!(layer_strokes(&e), before);
    e.redo();
    assert_eq!(layer_ids(&e, 0), vec![ids[2], ids[4]]);
}

#[test]
fn copy_and_paste_centre_the_ink() {
    let mut e = editor();
    let a = add(&mut e, hline(10.0, 0.0, 20.0));
    let b = add(&mut e, hline(30.0, 0.0, 20.0));
    assert!(!e.has_clipboard());
    assert!(e.copy(0, &[b, a]));
    assert!(e.has_clipboard());
    let ids = e.paste(0, 100.0, 200.0);
    assert_eq!(ids.len(), 2);
    let r = e.ink_bounds(0, &ids).unwrap();
    assert!(((r.min_x + r.max_x) * 0.5 - 100.0).abs() < 1e-4);
    assert!(((r.min_y + r.max_y) * 0.5 - 200.0).abs() < 1e-4);
    let strokes = layer_strokes(&e);
    assert_eq!(strokes.len(), 4);
    assert_eq!(strokes[2].points[0].y, 190.0);
    assert_eq!(strokes[3].points[0].y, 210.0);
    e.undo();
    assert_eq!(layer_ids(&e, 0), vec![a, b]);
    e.redo();
    assert_eq!(layer_ids(&e, 0).len(), 4);
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(e.paste(0, 0.0, 0.0).is_empty());
}

#[test]
fn duplicate_offsets_copies_onto_the_active_layer() {
    let mut e = editor();
    let a = add(&mut e, hline(10.0, 0.0, 20.0));
    e.document_mut().pages[0].add_layer("Top");
    e.set_active_layer(0, 1);
    let ids = e.duplicate(0, &[a], 5.0, 7.0);
    assert_eq!(ids.len(), 1);
    assert_ne!(ids[0], a);
    let top = &e.document().pages[0].layers[1].strokes[0];
    assert_eq!((top.points[0].x, top.points[0].y), (5.0, 17.0));
    assert_eq!(e.document().pages[0].layers[0].strokes[0].points[0].x, 0.0);
    e.undo();
    assert!(e.document().pages[0].layers[1].strokes.is_empty());
    assert_eq!(layer_ids(&e, 0), vec![a]);
}

#[test]
fn ink_bounds_include_half_width() {
    let mut e = editor();
    let id = add(&mut e, hline(10.0, 0.0, 20.0));
    let half = e.document().pages[0].layers[0].strokes[0]
        .style
        .brush()
        .width_at(0.5)
        * 0.5;
    let r = e.ink_bounds(0, &[id]).unwrap();
    assert!((r.min_x + half).abs() < 1e-5 && (r.max_y - 10.0 - half).abs() < 1e-5);
    assert!(e.ink_bounds(0, &[Id::new_v4()]).is_none());
}

#[test]
fn common_width_is_the_most_used_whole_width_and_skips_fills() {
    let mut e = editor();
    let a = add(
        &mut e,
        stroke(Tool::Pen, 0xFF, 4.2, &[(0.0, 0.0), (10.0, 0.0)]),
    );
    let b = add(
        &mut e,
        stroke(Tool::Pen, 0xFF, 6.0, &[(0.0, 5.0), (10.0, 5.0)]),
    );
    let c = add(
        &mut e,
        stroke(Tool::Pen, 0xFF, 6.4, &[(0.0, 9.0), (10.0, 9.0)]),
    );
    let f = add(
        &mut e,
        stroke(Tool::Fill, 0xFF, 0.0, &square(0.0, 0.0, 9.0, 9.0)),
    );
    assert_eq!(e.common_width(0, &[a, b, c, f]), Some(6.0));
    assert_eq!(e.common_width(0, &[a]), Some(4.0));
    assert_eq!(e.common_width(0, &[f]), None);
    assert_eq!(e.common_width(0, &[Id::new_v4()]), None);
}
