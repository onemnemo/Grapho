//! Diagram edits: background removal and conversion to ink, with undo.

use super::*;
use crate::diagram::fixtures::{boxed_diagram, flat, paint};
use crate::document::{Document, Orientation, PaperSize};
use crate::Rect;

fn editor() -> Editor {
    Editor::new(Document::new_paged(
        "Notes",
        PaperSize::A4,
        Orientation::Portrait,
    ))
}

/// The 200 by 150 diagram placed one point per pixel at (100, 200).
fn with_diagram() -> (Editor, Id, Pixels) {
    let pixels = boxed_diagram();
    let mut e = editor();
    let rect = Rect {
        min_x: 100.0,
        min_y: 200.0,
        max_x: 300.0,
        max_y: 350.0,
    };
    let id = e
        .insert_image(0, encode_png(&pixels), rect)
        .expect("a valid picture");
    (e, id, pixels)
}

fn layer_images(e: &Editor) -> Vec<Image> {
    e.document().pages[0].layers[0].images.clone()
}

fn strokes(e: &Editor) -> Vec<Stroke> {
    e.document().pages[0].layers[0].strokes.clone()
}

fn corner_alpha(e: &Editor, source: Id) -> u8 {
    let source = e.document().images.iter().find(|s| s.id == source).unwrap();
    let mut decoder = png::Decoder::new(std::io::Cursor::new(&source.bytes[..]))
        .read_info()
        .unwrap();
    let mut buf = vec![0; decoder.output_buffer_size().unwrap()];
    decoder.next_frame(&mut buf).unwrap();
    buf[3]
}

#[test]
fn removing_the_background_swaps_the_source_in_one_step() {
    let (mut e, old, pixels) = with_diagram();
    let other = e
        .insert_image(
            0,
            encode_png(&flat(4, 4, [9, 9, 9])),
            Rect {
                min_x: 0.0,
                min_y: 0.0,
                max_x: 10.0,
                max_y: 10.0,
            },
        )
        .unwrap();
    let before = layer_images(&e);
    let depth = e.undo.len();

    let new = e
        .remove_image_background(0, old, &pixels, Cutout::default())
        .unwrap();
    assert_eq!(e.undo.len(), depth + 1);
    let after = layer_images(&e);
    assert_eq!(after.len(), 2);
    assert_eq!(after[0].id, new, "keeps its place in the stack");
    assert_eq!(after[1].id, other);
    assert_eq!(after[0].transform, before[0].transform);
    assert_eq!(after[0].crop, before[0].crop);
    assert_ne!(after[0].source, before[0].source);
    assert_eq!(corner_alpha(&e, after[0].source), 0);
    assert_eq!(corner_alpha(&e, before[0].source), 255);

    e.undo();
    assert_eq!(layer_images(&e), before);
    e.redo();
    assert_eq!(layer_images(&e), after);
}

#[test]
fn background_removal_keeps_the_crop_and_rotation() {
    let (mut e, id, pixels) = with_diagram();
    let id = e.crop_image(0, id, [0.25, 0.1, 0.75, 0.9]).unwrap();
    e.document_mut().pages[0].layers[0].images[0].transform = [0.0, -2.0, 400.0, 1.5, 0.0, 10.0];
    let before = layer_images(&e)[0].clone();
    let new = e
        .remove_image_background(0, id, &pixels, Cutout::default())
        .unwrap();
    let after = e.image(0, new).unwrap();
    assert_eq!(after.transform, before.transform);
    assert_eq!(after.crop, before.crop);
}

#[test]
fn edits_refuse_what_they_cannot_do() {
    let (mut e, id, pixels) = with_diagram();
    let depth = e.undo.len();
    let small = flat(10, 10, [255; 3]);
    assert_eq!(
        e.remove_image_background(0, id, &small, Cutout::default()),
        Err(ImageError::SizeMismatch)
    );
    assert_eq!(
        e.image_to_ink(0, id, &small, 0),
        Err(ImageError::SizeMismatch)
    );
    let ghost = Id::new_v4();
    assert_eq!(
        e.remove_image_background(0, ghost, &pixels, Cutout::default()),
        Err(ImageError::NotFound)
    );
    assert_eq!(
        e.remove_image_background(5, id, &pixels, Cutout::default()),
        Err(ImageError::NoLayer)
    );
    e.document_mut().pages[0].layers[0].locked = true;
    assert_eq!(
        e.remove_image_background(0, id, &pixels, Cutout::default()),
        Err(ImageError::NoLayer)
    );
    assert_eq!(e.image_to_ink(0, id, &pixels, 0), Err(ImageError::NoLayer));
    e.document_mut().pages[0].layers[0].locked = false;
    e.document_mut().pages[0].layers[0].visible = false;
    assert_eq!(
        e.remove_image_background(0, id, &pixels, Cutout::default()),
        Err(ImageError::NoLayer)
    );
    assert_eq!(e.undo.len(), depth, "nothing was recorded");
}

#[test]
fn ink_replaces_the_image_where_it_was_in_one_step() {
    let (mut e, id, pixels) = with_diagram();
    let first = e.add_stroke(0, strokes_line()).unwrap();
    let depth = e.undo.len();

    let ids = e.image_to_ink(0, id, &pixels, 5_000).unwrap();
    assert_eq!(e.undo.len(), depth + 1);
    assert!(layer_images(&e).is_empty());
    let now = strokes(&e);
    assert_eq!(now[0].id, first, "new ink goes on top");
    assert_eq!(
        now[1..].iter().map(|s| s.id).collect::<Vec<_>>(),
        ids,
        "returned in order"
    );
    assert!(ids.len() >= 2);
    for s in &now[1..] {
        assert_eq!(s.style.tool, Tool::Pen);
        assert_eq!(s.started_at_ms, 5_000);
        assert!(s.points.len() >= 2);
        assert!(s.points.iter().all(|p| p.pressure == 1.0));
        assert!(s.points.windows(2).all(|w| w[1].time_ms > w[0].time_ms));
        assert!((s.style.width - 3.5).abs() < 1.0, "width {}", s.style.width);
        for p in &s.points {
            // The drawing's lines run from (30, 30) to (170, 120) in a
            // picture placed at (100, 200).
            assert!(p.x >= 128.0 && p.x <= 273.0, "x {}", p.x);
            assert!(p.y >= 228.0 && p.y <= 323.0, "y {}", p.y);
        }
    }

    assert_eq!(e.undo(), Some(0));
    assert_eq!(layer_images(&e).len(), 1);
    assert_eq!(layer_images(&e)[0].id, id);
    assert_eq!(strokes(&e).len(), 1);
    assert_eq!(e.redo(), Some(0));
    assert!(layer_images(&e).is_empty());
    assert_eq!(strokes(&e).len(), 1 + ids.len());
}

fn strokes_line() -> Stroke {
    use crate::{InputSample, LineStyle};
    let style = StrokeStyle {
        tool: Tool::Pen,
        color: 0x1122_33FF,
        width: 2.0,
        line: LineStyle::Solid,
    };
    let pts = vec![
        InputSample::new(0.0, 5.0, 1.0, 0.0),
        InputSample::new(10.0, 5.0, 1.0, 1.0),
    ];
    Stroke::new(style, 0, pts)
}

/// Lowest and highest page x and y of a set of strokes.
fn extent(strokes: &[Stroke]) -> (f32, f32, f32, f32) {
    let all = strokes.iter().flat_map(|s| &s.points);
    all.fold((f32::MAX, f32::MAX, f32::MIN, f32::MIN), |a, p| {
        (a.0.min(p.x), a.1.min(p.y), a.2.max(p.x), a.3.max(p.y))
    })
}

#[test]
fn only_the_shown_part_is_traced() {
    // A line in the left half and another in the right half.
    let mut pixels = flat(200, 100, [255; 3]);
    paint(&mut pixels, (20, 20, 80, 23), [0; 3]);
    paint(&mut pixels, (120, 70, 180, 73), [0; 3]);
    let mut e = editor();
    let rect = Rect {
        min_x: 50.0,
        min_y: 50.0,
        max_x: 250.0,
        max_y: 150.0,
    };
    let id = e.insert_image(0, encode_png(&pixels), rect).unwrap();
    let id = e.crop_image(0, id, [0.5, 0.0, 1.0, 1.0]).unwrap();
    // The cropped image shows x 150..250 of the page, unmoved.
    let ids = e.image_to_ink(0, id, &pixels, 0).unwrap();
    assert_eq!(ids.len(), 1);
    let (x0, y0, x1, y1) = extent(&strokes(&e));
    assert!(
        (x0 - 170.0).abs() < 3.0 && (x1 - 230.0).abs() < 3.0,
        "{x0} {x1}"
    );
    assert!(
        (y0 - 121.5).abs() < 2.0 && (y1 - 121.5).abs() < 2.0,
        "{y0} {y1}"
    );
}

#[test]
fn ink_follows_the_transform() {
    let mut pixels = flat(200, 100, [255; 3]);
    paint(&mut pixels, (20, 48, 181, 52), [0; 3]);
    let mut e = editor();
    let rect = Rect {
        min_x: 0.0,
        min_y: 0.0,
        max_x: 200.0,
        max_y: 100.0,
    };
    let id = e.insert_image(0, encode_png(&pixels), rect).unwrap();
    // Turned a quarter, twice as big: the unit square's x runs down the
    // page and its y runs left, from (500, 100).
    e.document_mut().pages[0].layers[0].images[0].transform =
        [0.0, -200.0, 500.0, 400.0, 0.0, 100.0];
    e.image_to_ink(0, id, &pixels, 0).unwrap();
    let all = strokes(&e);
    assert_eq!(all.len(), 1);
    let s = &all[0];
    let (x0, y0, x1, y1) = extent(&all);
    // The line was horizontal across the middle: now vertical, at the
    // page x of the middle row (v = 0.5 gives x = 500 - 100).
    assert!(
        (x0 - 400.0).abs() < 2.0 && (x1 - 400.0).abs() < 2.0,
        "{x0} {x1}"
    );
    // u from 0.1 to 0.9 gives y from 140 to 460.
    assert!(
        (y0 - 140.0).abs() < 6.0 && (y1 - 460.0).abs() < 6.0,
        "{y0} {y1}"
    );
    // 4 px of line, and a pixel is 400 / 200 = 2 points on the long side
    // and 200 / 100 = 2 on the other.
    assert!((s.style.width - 8.0).abs() < 2.5, "width {}", s.style.width);
}

#[test]
fn a_blank_picture_has_no_ink_and_changes_nothing() {
    let pixels = flat(60, 40, [250; 3]);
    let mut e = editor();
    let rect = Rect {
        min_x: 0.0,
        min_y: 0.0,
        max_x: 60.0,
        max_y: 40.0,
    };
    let id = e.insert_image(0, encode_png(&pixels), rect).unwrap();
    let depth = e.undo.len();
    assert_eq!(
        e.image_to_ink(0, id, &pixels, 0),
        Err(ImageError::NothingFound)
    );
    assert_eq!(e.undo.len(), depth);
    assert_eq!(layer_images(&e).len(), 1);
}

#[test]
fn a_cut_out_image_converts_to_ink() {
    let (mut e, id, pixels) = with_diagram();
    let cut = e
        .remove_image_background(0, id, &pixels, Cutout::default())
        .unwrap();
    let ids = e
        .image_to_ink(0, cut, &crate::diagram::remove_background(&pixels), 0)
        .unwrap();
    assert!(ids.len() >= 2);
    assert!(layer_images(&e).is_empty());
}

#[test]
fn filled_shapes_become_fill_strokes_under_the_lines() {
    let mut pixels = flat(240, 120, [255; 3]);
    paint(&mut pixels, (20, 59, 150, 61), [0; 3]);
    for k in 0..24u32 {
        let half = 11 - k * 11 / 24;
        paint(
            &mut pixels,
            (150 + k, 60 - half, 151 + k, 61 + half),
            [0; 3],
        );
    }
    let mut e = editor();
    let rect = Rect {
        min_x: 10.0,
        min_y: 10.0,
        max_x: 130.0,
        max_y: 70.0,
    };
    let id = e.insert_image(0, encode_png(&pixels), rect).unwrap();
    e.image_to_ink(0, id, &pixels, 0).unwrap();
    let all = strokes(&e);
    assert_eq!(all.len(), 2);
    assert_eq!(all[0].style.tool, Tool::Fill);
    assert_eq!(all[0].style.width, 0.0);
    assert!(all[0].points.len() >= 3);
    assert_eq!(all[1].style.tool, Tool::Pen);
    // Half scale: the head spans page x 85 to 97 or so.
    let (x0, x1) = all[0]
        .points
        .iter()
        .fold((f32::MAX, f32::MIN), |a, p| (a.0.min(p.x), a.1.max(p.x)));
    assert!(x0 < 87.0 && x1 > 95.0, "{x0} {x1}");
    assert!(all[0].hits(90.0, 40.0, 0.5));
}

#[test]
fn replacing_pixels_is_one_undo_step_that_keeps_the_place() {
    let (mut e, id, pixels) = with_diagram();
    let before = layer_images(&e)[0].clone();
    let depth = e.undo.len();
    let mut touched = pixels.clone();
    touched.rgba[3] = 0;
    let new = e.replace_image_pixels(0, id, &touched).unwrap();
    assert_eq!(e.undo.len(), depth + 1);
    let after = e.image(0, new).unwrap();
    assert_eq!(after.transform, before.transform);
    assert_eq!(after.crop, before.crop);
    assert_ne!(after.source, before.source);
    assert_eq!(corner_alpha(&e, after.source), 0);
    e.undo();
    assert_eq!(layer_images(&e), vec![before]);
}

#[test]
fn replacing_pixels_needs_the_sources_size() {
    let (mut e, id, _) = with_diagram();
    let small = Pixels {
        width: 1,
        height: 1,
        rgba: vec![0, 0, 0, 255],
    };
    assert_eq!(
        e.replace_image_pixels(0, id, &small),
        Err(ImageError::SizeMismatch)
    );
}
