//! Image edit tests: placing, finding, selecting, moving, copying, cropping
//! and layers, each with its undo.

use super::*;
use crate::document::image_fixtures::{jpeg, png_with_alpha};
use crate::document::{
    Document, ImageKind, Orientation, PaperSize, Stroke, StrokeStyle, Tool, FULL_CROP,
};
use crate::{InputSample, LineStyle};

const SHIFT: [f32; 6] = [1.0, 0.0, 100.0, 0.0, 1.0, 50.0];

fn rect(x: f32, y: f32, w: f32, h: f32) -> Rect {
    Rect {
        min_x: x,
        min_y: y,
        max_x: x + w,
        max_y: y + h,
    }
}

fn editor() -> Editor {
    Editor::new(Document::new_paged(
        "Notes",
        PaperSize::A4,
        Orientation::Portrait,
    ))
}

fn put(e: &mut Editor, x: f32, y: f32, w: f32, h: f32) -> Id {
    e.insert_image(0, png_with_alpha(4, 3), rect(x, y, w, h))
        .unwrap()
}

fn images(e: &Editor, layer: usize) -> Vec<Image> {
    e.document().pages[0].layers[layer].images.clone()
}

fn image_ids(e: &Editor, layer: usize) -> Vec<Id> {
    images(e, layer).iter().map(|i| i.id).collect()
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

fn square(x0: f32, y0: f32, x1: f32, y1: f32) -> Vec<(f32, f32)> {
    vec![(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
}

fn near(a: (f32, f32), b: (f32, f32)) -> bool {
    (a.0 - b.0).abs() < 1e-3 && (a.1 - b.1).abs() < 1e-3
}

/// A quarter turn about `(cx, cy)`.
fn quarter_turn(cx: f32, cy: f32) -> [f32; 6] {
    [0.0, -1.0, cx + cy, 1.0, 0.0, cy - cx]
}

#[test]
fn inserting_places_the_image_on_top_and_undoes() {
    let mut e = editor();
    let a = put(&mut e, 10.0, 20.0, 80.0, 60.0);
    let b = put(&mut e, 0.0, 0.0, 40.0, 30.0);
    assert_eq!(image_ids(&e, 0), vec![a, b]);
    let first = e.image(0, a).unwrap();
    assert_eq!(first.transform, [80.0, 0.0, 10.0, 0.0, 60.0, 20.0]);
    assert_eq!(first.crop, FULL_CROP);
    let doc = e.document();
    assert_eq!(doc.images.len(), 1, "the same bytes share one source");
    assert_eq!((doc.images[0].width, doc.images[0].height), (4, 3));
    assert_eq!(first.source, doc.images[0].id);

    assert_eq!(e.undo(), Some(0));
    assert_eq!(image_ids(&e, 0), vec![a]);
    e.undo();
    assert!(image_ids(&e, 0).is_empty());
    e.redo();
    e.redo();
    assert_eq!(image_ids(&e, 0), vec![a, b]);
    assert_eq!(e.image(0, b).unwrap().transform[0], 40.0);
}

#[test]
fn different_bytes_make_a_second_source() {
    let mut e = editor();
    put(&mut e, 0.0, 0.0, 10.0, 10.0);
    e.insert_image(0, jpeg(), rect(0.0, 0.0, 10.0, 10.0))
        .unwrap();
    assert_eq!(e.document().images.len(), 2);
    assert_eq!(e.document().images[1].kind, ImageKind::Jpeg);
}

#[test]
fn inserting_refuses_what_it_cannot_place() {
    let mut e = editor();
    let ok = rect(0.0, 0.0, 10.0, 10.0);
    assert_eq!(
        e.insert_image(0, b"nope".to_vec(), ok),
        Err(ImageError::Unrecognised)
    );
    assert_eq!(
        e.insert_image(0, png_with_alpha(2, 2), rect(0.0, 0.0, 0.0, 5.0)),
        Err(ImageError::BadRect)
    );
    assert_eq!(
        e.insert_image(0, png_with_alpha(2, 2), rect(f32::NAN, 0.0, 5.0, 5.0)),
        Err(ImageError::BadRect)
    );
    assert_eq!(
        e.insert_image(9, png_with_alpha(2, 2), ok),
        Err(ImageError::NoLayer)
    );
    e.document_mut().pages[0].layers[0].locked = true;
    assert_eq!(
        e.insert_image(0, png_with_alpha(2, 2), ok),
        Err(ImageError::NoLayer)
    );
    e.document_mut().pages[0].layers[0].locked = false;
    e.document_mut().pages[0].layers[0].visible = false;
    assert_eq!(
        e.insert_image(0, png_with_alpha(2, 2), ok),
        Err(ImageError::NoLayer)
    );
    assert!(!e.can_undo());
    assert!(e.document().images.is_empty());
}

#[test]
fn image_at_finds_the_topmost_visible_unlocked_image() {
    let mut e = editor();
    let low = put(&mut e, 0.0, 0.0, 100.0, 100.0);
    let high = put(&mut e, 50.0, 50.0, 100.0, 100.0);
    assert_eq!(e.image_at(0, 10.0, 10.0), Some(low));
    assert_eq!(e.image_at(0, 75.0, 75.0), Some(high));
    assert_eq!(e.image_at(0, 200.0, 10.0), None);
    assert_eq!(e.image_at(5, 10.0, 10.0), None);

    e.add_layer(0, "Top");
    let top = put(&mut e, 60.0, 60.0, 10.0, 10.0);
    assert_eq!(e.image_at(0, 65.0, 65.0), Some(top));
    e.set_layer_locked(0, 1, true);
    assert_eq!(e.image_at(0, 65.0, 65.0), Some(high));
    e.set_layer_locked(0, 1, false);
    e.set_layer_visible(0, 1, false);
    assert_eq!(e.image_at(0, 65.0, 65.0), Some(high));
}

#[test]
fn image_at_follows_a_rotation() {
    let mut e = editor();
    // 100 wide and 20 tall, centred on (100, 100), then turned upright.
    let id = put(&mut e, 50.0, 90.0, 100.0, 20.0);
    assert_eq!(e.image_at(0, 140.0, 100.0), Some(id));
    assert_eq!(e.image_at(0, 100.0, 140.0), None);
    let [new] = e.transform(0, &[id], quarter_turn(100.0, 100.0))[..] else {
        panic!("one image moves");
    };
    assert_eq!(e.image_at(0, 100.0, 140.0), Some(new));
    assert_eq!(e.image_at(0, 140.0, 100.0), None);
    assert_eq!(e.image_at(0, 108.0, 100.0), Some(new));
    // Inside the old bounding box of the turned image, but off the image.
    assert_eq!(e.image_at(0, 120.0, 140.0), None);
}

#[test]
fn a_lasso_takes_images_it_mostly_covers() {
    let mut e = editor();
    let inside = put(&mut e, 10.0, 10.0, 20.0, 20.0);
    let half = put(&mut e, 40.0, 10.0, 40.0, 20.0);
    let outside = put(&mut e, 200.0, 10.0, 20.0, 20.0);
    let stroke = e.add_stroke(0, line(15.0)).unwrap();
    let got = e.select(0, &square(0.0, 0.0, 60.0, 50.0));
    assert_eq!(got, vec![stroke, inside, half]);
    assert!(!got.contains(&outside));
    // A sliver of the image is not enough.
    assert!(e.select(0, &square(0.0, 0.0, 45.0, 50.0)).len() == 2);
    assert!(!e.select(0, &square(0.0, 0.0, 45.0, 50.0)).contains(&half));
    assert!(e.select(0, &square(0.0, 0.0, 60.0, 50.0)[..2]).is_empty());
}

#[test]
fn a_lasso_judges_a_turned_image_by_its_area() {
    let mut e = editor();
    let id = put(&mut e, 50.0, 90.0, 100.0, 20.0);
    let [turned] = e.transform(0, &[id], quarter_turn(100.0, 100.0))[..] else {
        panic!("one image moves");
    };
    // The turned image is 20 wide and 100 tall around (100, 100). This box
    // covers its top and bottom thirds of the old shape only.
    assert!(e.select(0, &square(0.0, 85.0, 300.0, 115.0)).is_empty());
    assert_eq!(e.select(0, &square(80.0, 0.0, 120.0, 300.0)), vec![turned]);
}

#[test]
fn selection_ignores_images_on_other_layers() {
    let mut e = editor();
    put(&mut e, 10.0, 10.0, 20.0, 20.0);
    e.add_layer(0, "Top");
    assert!(e.select(0, &square(0.0, 0.0, 60.0, 50.0)).is_empty());
    e.set_active_layer(0, 0);
    assert_eq!(e.select(0, &square(0.0, 0.0, 60.0, 50.0)).len(), 1);
}

#[test]
fn bounds_cover_the_corners_of_a_turned_image() {
    let mut e = editor();
    let id = put(&mut e, 50.0, 90.0, 100.0, 20.0);
    let r = e.ink_bounds(0, &[id]).unwrap();
    assert!(near((r.min_x, r.min_y), (50.0, 90.0)) && near((r.max_x, r.max_y), (150.0, 110.0)));
    let [turned] = e.transform(0, &[id], quarter_turn(100.0, 100.0))[..] else {
        panic!("one image moves");
    };
    let r = e.ink_bounds(0, &[turned]).unwrap();
    assert!(near((r.min_x, r.min_y), (90.0, 50.0)) && near((r.max_x, r.max_y), (110.0, 150.0)));
    let stroke = e.add_stroke(0, line(300.0)).unwrap();
    let both = e.ink_bounds(0, &[turned, stroke]).unwrap();
    assert!(both.max_y > 300.0 && both.min_x < 90.0 + 1e-3);
}

#[test]
fn transform_replaces_images_and_strokes_in_one_step() {
    let mut e = editor();
    let a = put(&mut e, 0.0, 0.0, 40.0, 30.0);
    let s = e.add_stroke(0, line(5.0)).unwrap();
    let b = put(&mut e, 100.0, 100.0, 40.0, 30.0);
    let before = images(&e, 0);
    let strokes_before = e.document().pages[0].layers[0].strokes.clone();

    let new = e.transform(0, &[b, s, a], SHIFT);
    assert_eq!(new.len(), 3);
    let after = images(&e, 0);
    assert_eq!(after.len(), 2);
    assert_eq!((after[0].id, after[1].id), (new[2], new[0]));
    assert_eq!(after[0].transform, [40.0, 0.0, 100.0, 0.0, 30.0, 50.0]);
    assert_eq!(after[1].transform[2], 200.0);
    assert_eq!(after[0].source, before[0].source);
    assert_eq!(
        e.document().pages[0].layers[0].strokes[0].points[0].x,
        100.0
    );

    let depth = e.undo.len();
    assert_eq!(e.undo(), Some(0));
    assert_eq!(images(&e, 0), before);
    assert_eq!(
        e.document().pages[0].layers[0].strokes,
        strokes_before,
        "one undo restores both"
    );
    assert_eq!(e.undo.len(), depth - 1);
    e.redo();
    assert_eq!(images(&e, 0), after);
    assert_ne!(e.document().pages[0].layers[0].strokes, strokes_before);
}

#[test]
fn scaling_composes_with_the_placement() {
    let mut e = editor();
    let id = put(&mut e, 10.0, 10.0, 40.0, 30.0);
    let new = e.transform(0, &[id], [2.0, 0.0, 0.0, 0.0, 2.0, 0.0]);
    assert_eq!(
        e.image(0, new[0]).unwrap().transform,
        [80.0, 0.0, 20.0, 0.0, 60.0, 20.0]
    );
}

#[test]
fn nothing_changes_for_locked_or_unknown_images() {
    let mut e = editor();
    let id = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    let depth = e.undo.len();
    assert!(e.transform(0, &[Id::new_v4()], SHIFT).is_empty());
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(e.transform(0, &[id], SHIFT).is_empty());
    assert!(e.recolor(0, &[id], 0xFF).is_empty());
    assert!(!e.delete(0, &[id]));
    assert!(e.crop_image(0, id, [0.1, 0.1, 0.9, 0.9]).is_none());
    assert_eq!(e.undo.len(), depth);
}

#[test]
fn delete_removes_images_with_strokes_and_undoes_together() {
    let mut e = editor();
    let a = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    let s = e.add_stroke(0, line(5.0)).unwrap();
    let b = put(&mut e, 20.0, 0.0, 10.0, 10.0);
    assert!(e.delete(0, &[a, s]));
    assert_eq!(image_ids(&e, 0), vec![b]);
    assert_eq!(e.document().pages[0].stroke_count(), 0);
    e.undo();
    assert_eq!(image_ids(&e, 0), vec![a, b], "back at the same position");
    assert_eq!(e.document().pages[0].stroke_count(), 1);
    e.redo();
    assert_eq!(image_ids(&e, 0), vec![b]);
    assert!(e.delete(0, &[b]));
    assert!(image_ids(&e, 0).is_empty());
    assert!(!e.delete(0, &[b]));
}

#[test]
fn recolor_leaves_images_alone_and_keeps_them_selected() {
    let mut e = editor();
    let img = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    let s = e.add_stroke(0, line(5.0)).unwrap();
    let out = e.recolor(0, &[img, s], 0xAABB_CC00);
    assert_eq!(out.len(), 2);
    assert_eq!(out[0], img);
    assert_ne!(out[1], s);
    assert_eq!(image_ids(&e, 0), vec![img]);
    e.undo();
    assert_eq!(e.document().pages[0].layers[0].strokes[0].id, s);
    // Only images selected: nothing to recolour, nothing recorded.
    let depth = e.undo.len();
    assert_eq!(e.recolor(0, &[img], 0xFF), vec![img]);
    assert_eq!(e.undo.len(), depth);
}

#[test]
fn copy_and_paste_centre_images_and_share_the_source() {
    let mut e = editor();
    let a = put(&mut e, 10.0, 10.0, 40.0, 20.0);
    let s = e.add_stroke(0, line(90.0)).unwrap();
    assert!(e.copy(0, &[a, s]));
    let ids = e.paste(0, 300.0, 400.0);
    assert_eq!(ids.len(), 2);
    let r = e.ink_bounds(0, &ids).unwrap();
    assert!(((r.min_x + r.max_x) * 0.5 - 300.0).abs() < 1e-3);
    assert!(((r.min_y + r.max_y) * 0.5 - 400.0).abs() < 1e-3);
    assert_eq!(image_ids(&e, 0).len(), 2);
    let pasted = e.image(0, ids[1]).unwrap();
    assert_ne!(pasted.id, a);
    assert_eq!(pasted.source, e.image(0, a).unwrap().source);
    assert_eq!(e.document().images.len(), 1);
    e.undo();
    assert_eq!(image_ids(&e, 0), vec![a]);
    assert_eq!(e.document().pages[0].stroke_count(), 1);
    e.redo();
    assert_eq!(image_ids(&e, 0).len(), 2);
}

#[test]
fn an_image_only_clipboard_pastes() {
    let mut e = editor();
    let a = put(&mut e, 0.0, 0.0, 40.0, 20.0);
    assert!(e.copy(0, &[a]));
    assert!(e.has_clipboard());
    let ids = e.paste(0, 100.0, 100.0);
    assert_eq!(ids.len(), 1);
    let placed = e.image(0, ids[0]).unwrap();
    assert!(near(placed.to_page(0.5, 0.5), (100.0, 100.0)));
    e.document_mut().pages[0].layers[0].locked = true;
    assert!(e.paste(0, 0.0, 0.0).is_empty());
}

#[test]
fn duplicate_offsets_images_onto_the_active_layer() {
    let mut e = editor();
    let a = put(&mut e, 10.0, 10.0, 40.0, 20.0);
    e.add_layer(0, "Top");
    let ids = e.duplicate(0, &[a], 5.0, 7.0);
    assert_eq!(ids.len(), 1);
    assert_eq!(image_ids(&e, 0), vec![a]);
    let copy = e.image(0, ids[0]).unwrap();
    assert_eq!(copy.transform, [40.0, 0.0, 15.0, 0.0, 20.0, 17.0]);
    assert_eq!(image_ids(&e, 1), ids);
    e.undo();
    assert!(image_ids(&e, 1).is_empty());
    assert_eq!(image_ids(&e, 0), vec![a]);
}

/// Where the source fraction `(u, v)` lands on the page for an image.
fn source_point(image: &Image, u: f32, v: f32) -> (f32, f32) {
    let [l, t, r, b] = image.crop;
    image.to_page((u - l) / (r - l), (v - t) / (b - t))
}

#[test]
fn cropping_keeps_the_shown_part_in_place() {
    let mut e = editor();
    let id = put(&mut e, 10.0, 20.0, 80.0, 60.0);
    let before = e.image(0, id).unwrap().clone();
    let new = e.crop_image(0, id, [0.25, 0.1, 0.75, 0.9]).unwrap();
    let after = e.image(0, new).unwrap().clone();
    assert_ne!(new, id);
    assert_eq!(after.crop, [0.25, 0.1, 0.75, 0.9]);
    // The corners of the cropped square sit where those source points were.
    let corners = [(0.25, 0.1), (0.75, 0.1), (0.75, 0.9), (0.25, 0.9)];
    for (corner, (u, v)) in after.corners().into_iter().zip(corners) {
        assert!(near(corner, source_point(&before, u, v)));
    }
    assert!(near(after.corners()[0], (30.0, 26.0)));
    assert!(near(after.corners()[2], (70.0, 74.0)));
    assert_eq!(e.image(0, id), None);

    e.undo();
    assert_eq!(e.image(0, id), Some(&before));
    e.redo();
    assert_eq!(e.image(0, new), Some(&after));
}

#[test]
fn growing_the_crop_again_keeps_the_picture_in_place() {
    let mut e = editor();
    let id = put(&mut e, 10.0, 20.0, 80.0, 60.0);
    let full = e.image(0, id).unwrap().clone();
    let cropped = e.crop_image(0, id, [0.2, 0.3, 0.6, 0.8]).unwrap();
    let small = e.image(0, cropped).unwrap().clone();
    let grown = e.crop_image(0, cropped, [0.1, 0.0, 0.9, 1.0]).unwrap();
    let big = e.image(0, grown).unwrap().clone();
    for (u, v) in [(0.2, 0.3), (0.6, 0.8), (0.5, 0.5)] {
        assert!(near(source_point(&big, u, v), source_point(&full, u, v)));
        assert!(near(source_point(&big, u, v), source_point(&small, u, v)));
    }
    // Back to the whole picture gives the original placement.
    let whole = e.crop_image(0, grown, FULL_CROP).unwrap();
    let whole = e.image(0, whole).unwrap();
    assert_eq!(whole.crop, FULL_CROP);
    for (a, b) in whole.transform.iter().zip(full.transform) {
        assert!((a - b).abs() < 1e-3);
    }
}

#[test]
fn cropping_a_turned_image_keeps_it_in_place() {
    let mut e = editor();
    let id = put(&mut e, 50.0, 90.0, 100.0, 20.0);
    let [turned] = e.transform(0, &[id], quarter_turn(100.0, 100.0))[..] else {
        panic!("one image moves");
    };
    let before = e.image(0, turned).unwrap().clone();
    let new = e.crop_image(0, turned, [0.0, 0.0, 0.5, 1.0]).unwrap();
    let after = e.image(0, new).unwrap();
    for (u, v) in [(0.0, 0.0), (0.5, 1.0), (0.25, 0.5)] {
        assert!(near(source_point(after, u, v), source_point(&before, u, v)));
    }
}

#[test]
fn crop_values_are_clamped_and_unchanged_crops_do_nothing() {
    let mut e = editor();
    let id = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    assert!(e.crop_image(0, id, FULL_CROP).is_none());
    assert!(e.crop_image(0, id, [f32::NAN, 0.0, 1.0, 1.0]).is_none());
    assert!(!e.can_undo() || e.undo.len() == 1);
    let new = e.crop_image(0, id, [-1.0, 0.5, 0.5, 5.0]).unwrap();
    assert_eq!(e.image(0, new).unwrap().crop, [0.0, 0.5, 0.5, 1.0]);
    // Crossed sides keep the least size.
    let thin = e.crop_image(0, new, [0.8, 0.4, 0.2, 0.1]).unwrap();
    let crop = e.image(0, thin).unwrap().crop;
    assert!((crop[2] - crop[0] - 0.01).abs() < 1e-6 && (crop[3] - crop[1] - 0.01).abs() < 1e-6);
    assert!(e
        .crop_image(0, Id::new_v4(), [0.1, 0.1, 0.9, 0.9])
        .is_none());
}

#[test]
fn removing_a_layer_takes_its_images_and_undo_brings_them_back() {
    let mut e = editor();
    e.add_layer(0, "Top");
    let id = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    assert!(e.remove_layer(0, 1));
    assert_eq!(e.document().pages[0].image_count(), 0);
    assert_eq!(e.image_at(0, 5.0, 5.0), None);
    e.undo();
    assert_eq!(image_ids(&e, 1), vec![id]);
    e.redo();
    assert_eq!(e.document().pages[0].image_count(), 0);
}

#[test]
fn merging_down_appends_images_above_the_lower_ones_and_undoes() {
    let mut e = editor();
    let low = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    e.add_layer(0, "Top");
    let up1 = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    let up2 = put(&mut e, 5.0, 5.0, 10.0, 10.0);
    assert!(e.merge_down(0, 1));
    assert_eq!(e.document().pages[0].layers.len(), 1);
    assert_eq!(image_ids(&e, 0), vec![low, up1, up2]);
    e.undo();
    assert_eq!(image_ids(&e, 0), vec![low]);
    assert_eq!(image_ids(&e, 1), vec![up1, up2]);
    e.redo();
    assert_eq!(image_ids(&e, 0), vec![low, up1, up2]);
}

#[test]
fn pages_keep_their_images_when_others_come_and_go() {
    let mut e = editor();
    let id = put(&mut e, 0.0, 0.0, 10.0, 10.0);
    e.insert_page(0);
    assert_eq!(e.image(1, id).map(|i| i.id), Some(id));
    e.undo();
    assert_eq!(e.image(0, id).map(|i| i.id), Some(id));
    e.add_page();
    e.undo();
    assert!(e.image(0, id).is_some());
}

#[test]
fn erasing_and_filling_leave_images_alone() {
    use crate::document::{EraseMode, FillView};
    let mut e = editor();
    let id = put(&mut e, 0.0, 0.0, 100.0, 100.0);
    e.begin_erase(0, EraseMode::Stroke);
    e.erase(50.0, 50.0, 60.0, 60.0, 5.0);
    e.end_erase();
    let view = FillView {
        scale: 1.0,
        visible: (0.0, 0.0, 595.0, 842.0),
    };
    e.fill(0, 300.0, 300.0, 0xFF00_00FF, view);
    assert_eq!(image_ids(&e, 0), vec![id]);
}
