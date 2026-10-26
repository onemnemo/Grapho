//! Filled shapes, noisy wires and the limits of gap closing.

use super::fixtures::{flat, paint};
use super::*;

const BLACK: u32 = 0x1718_1CFF;

/// A black line 2 px thick ending in a filled triangle, and a filled dot.
fn arrow_with_dot() -> Pixels {
    let mut src = flat(240, 120, [255; 3]);
    paint(&mut src, (20, 59, 150, 61), [0; 3]);
    for k in 0..24u32 {
        // A triangle 24 long and 22 tall, pointing right.
        let half = 11 - k * 11 / 24;
        paint(
            &mut src,
            (150 + k, 60 - half, 151 + k, 61 + half),
            [200, 30, 30],
        );
    }
    for y in 0..120u32 {
        for x in 0..240u32 {
            if (x as f32 - 60.0).hypot(y as f32 - 60.0) <= 5.0 {
                paint(&mut src, (x, y, x + 1, y + 1), [0; 3]);
            }
        }
    }
    src
}

#[test]
fn arrowheads_and_dots_come_back_as_filled_shapes() {
    let lines = trace(&arrow_with_dot());
    let fills: Vec<&TracedLine> = lines.iter().filter(|l| l.filled).collect();
    assert_eq!(fills.len(), 2, "{lines:?}");
    let head = fills
        .iter()
        .find(|l| l.color == 0xC81E_1EFF)
        .expect("a red head");
    assert!(head.points.len() >= 3 && head.points.len() <= 12);
    let (x0, x1) = head
        .points
        .iter()
        .fold((f32::MAX, f32::MIN), |a, p| (a.0.min(p[0]), a.1.max(p[0])));
    assert!(x0 < 154.0 && x1 > 169.0, "head spans {x0} to {x1}");
    let dot = fills
        .iter()
        .find(|l| l.color == BLACK)
        .expect("a black dot");
    // The fill may take in a few pixels of the line through the dot.
    for p in &dot.points {
        assert!(
            (p[0] - 60.5).hypot(p[1] - 60.5) < 9.0,
            "{p:?} is off the dot"
        );
    }
    // The shaft is still a line, reaching from the left to the head.
    let shaft: Vec<&TracedLine> = lines.iter().filter(|l| !l.filled).collect();
    assert!(!shaft.is_empty());
    let reach = shaft
        .iter()
        .flat_map(|l| &l.points)
        .fold(0.0f32, |a, p| a.max(p[0]));
    assert!(reach > 146.0, "the shaft stops at {reach}");
}

#[test]
fn bars_and_rings_stay_lines() {
    let mut src = flat(200, 120, [255; 3]);
    paint(&mut src, (20, 30, 150, 32), [0; 3]);
    paint(&mut src, (60, 80, 100, 92), [0; 3]);
    for y in 0..120u32 {
        for x in 0..200u32 {
            let r = (x as f32 - 160.0).hypot(y as f32 - 90.0);
            if (r - 18.0).abs() <= 4.0 {
                paint(&mut src, (x, y, x + 1, y + 1), [0; 3]);
            }
        }
    }
    let lines = trace(&src);
    assert!(lines.iter().all(|l| !l.filled), "{lines:?}");
}

#[test]
fn a_noisy_wire_is_one_straight_line() {
    let mut src = flat(700, 60, [255; 3]);
    // Edges that jog by a pixel every few dozen columns.
    for x in 20..680u32 {
        let jog = u32::from((x / 37) % 3 == 1);
        paint(&mut src, (x, 28 + jog, x + 1, 32 + jog), [0; 3]);
    }
    let lines = trace(&src);
    assert_eq!(lines.len(), 1, "{lines:?}");
    assert!(
        lines[0].points.len() <= 3,
        "{} points",
        lines[0].points.len()
    );
    assert!((lines[0].width - 4.0).abs() < 2.0);
}

#[test]
fn a_thin_line_is_not_closed_onto_a_much_thicker_bar() {
    let mut src = flat(200, 100, [255; 3]);
    paint(&mut src, (10, 49, 80, 51), [0; 3]);
    paint(&mut src, (88, 20, 100, 80), [0; 3]);
    let lines = trace(&src);
    let reach = lines
        .iter()
        .filter(|l| l.width < 3.0)
        .flat_map(|l| &l.points)
        .fold(0.0f32, |a, p| a.max(p[0]));
    assert!(reach < 84.0, "the thin line was drawn on to {reach}");
}
