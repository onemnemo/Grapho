//! Stress tests for the eraser gesture.

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

fn strokes(e: &Editor) -> Vec<Stroke> {
    e.document().pages[0].layers[0].strokes.clone()
}

fn ids(e: &Editor) -> Vec<Id> {
    strokes(e).iter().map(|s| s.id).collect()
}

fn pen(points: Vec<InputSample>) -> Stroke {
    let style = StrokeStyle {
        tool: Tool::Pen,
        color: 0x1718_1CFF,
        width: 2.0,
        line: crate::LineStyle::Solid,
    };
    Stroke::new(style, 0, points)
}

fn line(y: f32, n: usize) -> Stroke {
    pen((0..n)
        .map(|i| InputSample::new(i as f32 * 400.0 / n as f32, y, 0.7, i as f32))
        .collect())
}

fn fill(corners: &[(f32, f32)]) -> Stroke {
    let style = StrokeStyle {
        tool: Tool::Fill,
        color: 0x8030_40FF,
        width: 0.0,
        line: crate::LineStyle::Solid,
    };
    let pts = corners
        .iter()
        .map(|&(x, y)| InputSample::new(x, y, 1.0, 0.0))
        .collect();
    Stroke::new(style, 0, pts)
}

fn block() -> Stroke {
    fill(&[(20.0, 20.0), (120.0, 20.0), (120.0, 120.0), (20.0, 120.0)])
}

#[test]
fn a_fill_is_removed_whole_when_the_eraser_touches_it() {
    for (x, y, r) in [(70.0, 70.0, 3.0), (15.0, 70.0, 6.0), (125.0, 20.0, 8.0)] {
        let mut e = editor();
        e.add_stroke(0, block());
        let before = strokes(&e);
        e.begin_erase(0, EraseMode::Stroke);
        assert_eq!(e.erase(x, y, x, y, r), 1, "at {x},{y}");
        e.end_erase();
        assert!(strokes(&e).is_empty());
        e.undo();
        assert_eq!(strokes(&e), before);
    }
}

#[test]
fn a_sweep_across_the_edge_of_a_fill_removes_it_in_stroke_mode() {
    let mut e = editor();
    e.add_stroke(0, block());
    e.add_stroke(0, line(300.0, 10));
    e.begin_erase(0, EraseMode::Stroke);
    assert_eq!(e.erase(0.0, 70.0, 30.0, 70.0, 2.0), 1);
    e.end_erase();
    assert_eq!(strokes(&e).len(), 1);
}

#[test]
fn a_fill_with_a_hole_can_be_erased_again_and_restored() {
    let mut e = editor();
    e.add_stroke(0, block());
    let before = strokes(&e);
    e.begin_erase(0, EraseMode::Area);
    e.erase(70.0, 70.0, 70.0, 70.0, 15.0);
    e.erase(40.0, 40.0, 100.0, 100.0, 6.0);
    e.erase(120.0, 20.0, 120.0, 20.0, 20.0);
    e.end_erase();
    assert!(!strokes(&e).is_empty());
    e.undo();
    assert_eq!(strokes(&e), before);
}

#[test]
fn a_fill_covered_by_the_eraser_disappears_and_undo_brings_it_back() {
    let mut e = editor();
    e.add_stroke(0, block());
    let before = strokes(&e);
    e.begin_erase(0, EraseMode::Area);
    e.erase(0.0, 70.0, 140.0, 70.0, 60.0);
    e.end_erase();
    assert!(strokes(&e).is_empty());
    e.undo();
    assert_eq!(strokes(&e), before);
}

#[test]
fn an_eraser_held_still_never_changes_ids() {
    let mut e = editor();
    e.add_stroke(0, line(200.0, 40));
    e.add_stroke(0, block());
    e.begin_erase(0, EraseMode::Area);
    assert_eq!(e.erase(60.0, 200.0, 60.0, 200.0, 8.0), 1);
    assert_eq!(e.erase(70.0, 70.0, 70.0, 70.0, 12.0), 1);
    let ids_after_cut = ids(&e);
    for _ in 0..200 {
        assert_eq!(e.erase(60.0, 200.0, 60.0, 200.0, 8.0), 0);
        assert_eq!(e.erase(70.0, 70.0, 70.0, 70.0, 12.0), 0);
    }
    assert_eq!(ids(&e), ids_after_cut);
    e.end_erase();
}

#[test]
fn an_eraser_that_reaches_nothing_leaves_every_id_and_records_nothing() {
    let mut e = editor();
    for k in 0..20 {
        e.add_stroke(0, line(k as f32 * 20.0, 30));
    }
    e.add_stroke(0, block());
    let before = ids(&e);
    for mode in [EraseMode::Area, EraseMode::Stroke] {
        e.begin_erase(0, mode);
        assert_eq!(e.erase(600.0, 600.0, 800.0, 700.0, 10.0), 0);
        e.end_erase();
        assert_eq!(ids(&e), before);
    }
    assert!(e.can_undo());
    assert_eq!(e.undo(), Some(0));
    assert_eq!(ids(&e).len(), before.len() - 1);
}

#[test]
fn five_hundred_erase_calls_over_fifty_strokes_are_one_undo_step() {
    let mut e = editor();
    for k in 0..50 {
        e.add_stroke(0, line(10.0 + k as f32 * 8.0, 400));
    }
    e.add_stroke(0, block());
    let before = strokes(&e);
    let created = e.undo.len();
    e.begin_erase(0, EraseMode::Area);
    let mut touched = 0;
    let (mut x, mut y) = (0.0_f32, 0.0_f32);
    for i in 0..500 {
        // Zig-zag down the page, with the odd long jump.
        let nx = if i % 2 == 0 { 400.0 } else { 0.0 };
        let ny = (i as f32 / 500.0) * 420.0;
        touched += e.erase(x, y, nx, ny, 3.0);
        (x, y) = (nx, ny);
    }
    e.end_erase();
    assert!(touched >= 50, "{touched}");
    assert_eq!(e.undo.len(), created + 1);
    for s in strokes(&e) {
        assert!(s.points.iter().all(|p| p.x.is_finite() && p.y.is_finite()));
    }
    assert_eq!(e.undo(), Some(0));
    assert_eq!(strokes(&e), before);
}

#[test]
fn a_gesture_cuts_each_touched_stroke_a_bounded_number_of_times() {
    let mut e = editor();
    e.add_stroke(0, line(100.0, 2000));
    e.begin_erase(0, EraseMode::Area);
    let mut touched = 0;
    // Ten crossings, then ten hundred events held still on one spot.
    for k in 0..10 {
        let x = 20.0 + k as f32 * 35.0;
        touched += e.erase(x, 80.0, x, 120.0, 4.0);
    }
    for _ in 0..1000 {
        touched += e.erase(55.0, 100.0, 55.0, 100.0, 4.0);
    }
    e.end_erase();
    assert_eq!(touched, 10);
    assert_eq!(strokes(&e).len(), 11);
}

#[test]
fn a_fill_cut_many_times_in_one_gesture_restores_exactly() {
    let mut e = editor();
    e.add_stroke(0, block());
    e.add_stroke(0, line(70.0, 50));
    let before = strokes(&e);
    e.begin_erase(0, EraseMode::Area);
    for k in 0..40 {
        let a = k as f32 * 0.7;
        let (x, y) = (70.0 + a.cos() * 40.0, 70.0 + a.sin() * 40.0);
        e.erase(x, y, x + 5.0, y + 3.0, 4.0 + (k % 5) as f32);
    }
    e.end_erase();
    let after = strokes(&e);
    assert!(after
        .iter()
        .all(|s| s.points.iter().all(|p| p.x.is_finite() && p.y.is_finite())));
    e.undo();
    assert_eq!(strokes(&e), before);
    e.redo();
    assert_eq!(strokes(&e), after);
}

#[test]
fn a_fixed_seed_fuzz_of_whole_gestures_never_panics_and_undoes_cleanly() {
    let mut seed = 99_u64;
    let mut next = move || {
        seed = seed
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        ((seed >> 40) as f32) / (1u64 << 24) as f32
    };
    for round in 0..12 {
        let mut e = editor();
        for _ in 0..8 {
            let n = 2 + (next() * 40.0) as usize;
            let mut pts = Vec::new();
            let (mut x, mut y) = (next() * 300.0, next() * 300.0);
            for i in 0..n {
                if next() > 0.2 {
                    x += (next() - 0.5) * 40.0;
                    y += (next() - 0.5) * 40.0;
                }
                pts.push(InputSample::new(x, y, next(), i as f32));
            }
            e.add_stroke(0, pen(pts));
        }
        let cx = 60.0 + next() * 200.0;
        let cy = 60.0 + next() * 200.0;
        e.add_stroke(
            0,
            fill(&[
                (cx, cy),
                (cx + 90.0, cy + 5.0),
                (cx + 70.0, cy + 90.0),
                (cx - 10.0, cy + 60.0),
            ]),
        );
        let before = strokes(&e);
        let mode = if round % 3 == 0 {
            EraseMode::Stroke
        } else {
            EraseMode::Area
        };
        e.begin_erase(0, mode);
        for _ in 0..60 {
            let (x0, y0) = (next() * 340.0, next() * 340.0);
            let jump = if next() < 0.3 { 400.0 } else { 15.0 };
            let (x1, y1) = (x0 + (next() - 0.5) * jump, y0 + (next() - 0.5) * jump);
            e.erase(x0, y0, x1, y1, 1.0 + next() * 14.0);
        }
        e.end_erase();
        for s in strokes(&e) {
            assert!(s.points.iter().all(|p| p.x.is_finite() && p.y.is_finite()));
            assert!(!s.points.is_empty());
        }
        e.undo();
        assert_eq!(strokes(&e), before, "round {round}");
    }
}
