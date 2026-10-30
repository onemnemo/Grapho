//! Pens of different colours traced apart, and thin lines traced enlarged.

use super::fixtures::{flat, paint};
use super::*;

const BLACK: u32 = 0x1718_1CFF;
const RED: [u8; 3] = [210, 60, 80];
const BLUE: [u8; 3] = [40, 60, 180];

fn hex(rgb: [u8; 3]) -> u32 {
    u32::from(rgb[0]) << 24 | u32::from(rgb[1]) << 16 | u32::from(rgb[2]) << 8 | 0xFF
}

#[test]
fn pens_that_touch_keep_their_own_colours() {
    let mut src = flat(260, 80, [255; 3]);
    // A red and a blue line side by side with no gap, and a blue one that
    // crosses the red.
    paint(&mut src, (20, 30, 240, 33), RED);
    paint(&mut src, (20, 33, 240, 36), BLUE);
    paint(&mut src, (128, 5, 131, 75), BLUE);
    let lines = trace(&src);
    assert!(
        lines
            .iter()
            .all(|l| l.color == hex(RED) || l.color == hex(BLUE)),
        "{lines:?}"
    );
    let long = |c: [u8; 3]| {
        lines
            .iter()
            .filter(|l| l.color == hex(c))
            .flat_map(|l| &l.points)
            .fold((f32::MAX, f32::MIN), |a, p| (a.0.min(p[0]), a.1.max(p[0])))
    };
    let (red, blue) = (long(RED), long(BLUE));
    assert!(red.0 < 25.0 && red.1 > 235.0, "red spans {red:?}");
    assert!(blue.0 < 25.0 && blue.1 > 235.0, "blue spans {blue:?}");
}

#[test]
fn a_black_ring_on_a_coloured_line_stays_black() {
    let mut src = flat(200, 100, [255; 3]);
    paint(&mut src, (10, 49, 190, 52), BLUE);
    for y in 0..100u32 {
        for x in 0..200u32 {
            let r = (x as f32 - 100.0).hypot(y as f32 - 50.0);
            if r <= 7.0 {
                let v = if r >= 5.0 { 0 } else { 255 };
                paint(&mut src, (x, y, x + 1, y + 1), [v; 3]);
            }
        }
    }
    let lines = trace(&src);
    let ring: Vec<&TracedLine> = lines.iter().filter(|l| l.color == BLACK).collect();
    assert!(!ring.is_empty(), "{lines:?}");
    for p in ring.iter().flat_map(|l| &l.points) {
        let r = (p[0] - 100.0).hypot(p[1] - 50.0);
        assert!((r - 6.0).abs() < 2.0, "{p:?} is off the ring");
    }
    assert!(lines.iter().any(|l| l.color == hex(BLUE)));
}

#[test]
fn thin_lines_come_back_where_they_were() {
    // Two pixel lines are traced enlarged; the result is still in the
    // picture's own pixels.
    let mut src = flat(120, 60, [255; 3]);
    paint(&mut src, (10, 20, 110, 22), [0; 3]);
    paint(&mut src, (60, 30, 62, 55), [0; 3]);
    let mut lines = trace(&src);
    assert_eq!(lines.len(), 2, "{lines:?}");
    lines.sort_by(|a, b| a.points[0][1].total_cmp(&b.points[0][1]));
    let bar = &lines[0];
    assert!(bar.points.iter().all(|p| (p[1] - 21.0).abs() < 0.6));
    assert!((bar.width - 2.0).abs() < 0.8, "width {}", bar.width);
    let post = &lines[1];
    assert!(post.points.iter().all(|p| (p[0] - 61.0).abs() < 0.6));
}
