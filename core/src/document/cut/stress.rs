//! Stress tests for cutting: long strokes, fast passes, fills
//! cut many times, and random input from a fixed seed.

use super::*;
use crate::document::StrokeStyle;

/// A small deterministic generator, so a failure can be replayed.
struct Rng(u64);

impl Rng {
    fn next(&mut self) -> f32 {
        self.0 = self
            .0
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        ((self.0 >> 40) as f32) / (1u64 << 24) as f32
    }

    fn range(&mut self, lo: f32, hi: f32) -> f32 {
        lo + (hi - lo) * self.next()
    }
}

fn style(tool: Tool, width: f32) -> StrokeStyle {
    StrokeStyle {
        tool,
        color: 0x1234_56FF,
        width,
        line: crate::LineStyle::Solid,
    }
}

fn path_length(s: &Stroke) -> f32 {
    s.points
        .windows(2)
        .map(|w| (w[1].x - w[0].x).hypot(w[1].y - w[0].y))
        .sum()
}

fn assert_sane(s: &Stroke) {
    assert!(s.points.len() >= 2, "piece with {} points", s.points.len());
    assert!(
        path_length(s) >= MIN_PIECE_LEN,
        "short piece {}",
        path_length(s)
    );
    for p in &s.points {
        assert!(p.x.is_finite() && p.y.is_finite(), "{p:?}");
        assert!(p.pressure.is_finite() && p.time_ms.is_finite(), "{p:?}");
    }
}

fn wavy(n: usize) -> Stroke {
    let pts = (0..n)
        .map(|i| {
            let t = i as f32 * 0.05;
            InputSample::new(t * 4.0, (t * 1.7).sin() * 30.0, 0.6, i as f32 * 4.0)
        })
        .collect();
    Stroke::new(style(Tool::Pen, 2.0), 0, pts)
}

fn random_stroke(rng: &mut Rng) -> Stroke {
    let n = 1 + (rng.next() * 60.0) as usize;
    let (mut x, mut y) = (rng.range(0.0, 200.0), rng.range(0.0, 200.0));
    let mut pts: Vec<InputSample> = Vec::new();
    for i in 0..n {
        match rng.next() {
            // The same point again, and a point a hair away from the last.
            v if v < 0.15 => {}
            v if v < 0.3 => {
                x += rng.range(-1e-4, 1e-4);
                y += rng.range(-1e-4, 1e-4);
            }
            _ => {
                x += rng.range(-25.0, 25.0);
                y += rng.range(-25.0, 25.0);
            }
        }
        pts.push(InputSample::new(x, y, rng.range(0.0, 1.0), i as f32 * 4.0));
    }
    let tool = [Tool::Pen, Tool::Pencil, Tool::Highlighter][(rng.next() * 3.0) as usize % 3];
    Stroke::new(style(tool, rng.range(0.5, 12.0)), 100, pts)
}

fn random_sweep(rng: &mut Rng) -> (f32, f32, f32, f32, f32) {
    let (x0, y0) = (rng.range(-20.0, 220.0), rng.range(-20.0, 220.0));
    // Some sweeps are tiny and some jump across the whole page.
    let reach = if rng.next() < 0.5 { 3.0 } else { 300.0 };
    let (x1, y1) = (x0 + rng.range(-reach, reach), y0 + rng.range(-reach, reach));
    (x0, y0, x1, y1, rng.range(0.5, 25.0))
}

#[test]
fn a_long_stroke_far_from_the_eraser_is_not_touched() {
    let s = wavy(20_000);
    assert_eq!(
        cut_stroke(&s, 5000.0, 5000.0, 5100.0, 5000.0, 10.0),
        Cut::Miss
    );
    // The eraser runs alongside, beyond the stroke's reach.
    assert_eq!(cut_stroke(&s, 0.0, 80.0, 3000.0, 80.0, 10.0), Cut::Miss);
}

#[test]
fn a_long_stroke_is_cut_where_the_eraser_crosses_it() {
    let s = wavy(20_000);
    let Cut::Pieces(p) = cut_stroke(&s, 400.0, -40.0, 400.0, 40.0, 3.0) else {
        panic!("expected a cut");
    };
    assert!(!p.is_empty());
    p.iter().for_each(assert_sane);
    let kept: usize = p.iter().map(|q| q.points.len()).sum();
    assert!(kept + 100 >= s.points.len(), "{kept} of {}", s.points.len());
}

#[test]
fn a_fast_pass_cuts_every_line_it_crosses() {
    let lines: Vec<Stroke> = (0..20)
        .map(|k| {
            let y = k as f32 * 10.0;
            let pts = (0..=20)
                .map(|i| InputSample::new(i as f32 * 5.0, y, 0.7, i as f32))
                .collect();
            Stroke::new(style(Tool::Pen, 1.5), 0, pts)
        })
        .collect();
    // One jump of 200 points between two events.
    for l in &lines {
        let Cut::Pieces(p) = cut_stroke(l, 50.0, -5.0, 50.0, 205.0, 2.0) else {
            panic!("line at {} not cut", l.points[0].y);
        };
        assert_eq!(p.len(), 2);
    }
}

#[test]
fn random_input_never_breaks_a_piece() {
    let mut rng = Rng(7);
    let mut cuts = 0;
    for _ in 0..300 {
        let mut live = vec![random_stroke(&mut rng)];
        for _ in 0..6 {
            let (x0, y0, x1, y1, r) = random_sweep(&mut rng);
            let mut next = Vec::new();
            for s in live {
                match cut_stroke(&s, x0, y0, x1, y1, r) {
                    Cut::Miss => next.push(s),
                    Cut::Pieces(p) => {
                        cuts += 1;
                        p.iter().for_each(assert_sane);
                        next.extend(p);
                    }
                }
            }
            live = next;
        }
    }
    assert!(
        cuts > 100,
        "only {cuts} cuts, the fuzz is not reaching strokes"
    );
}

#[test]
fn cutting_pieces_again_with_the_same_sweep_changes_nothing() {
    let mut rng = Rng(11);
    for _ in 0..100 {
        let s = random_stroke(&mut rng);
        let (x0, y0, x1, y1, r) = random_sweep(&mut rng);
        if let Cut::Pieces(p) = cut_stroke(&s, x0, y0, x1, y1, r) {
            for q in &p {
                assert_eq!(cut_stroke(q, x0, y0, x1, y1, r), Cut::Miss);
            }
        }
    }
}

fn square(side: f32) -> Stroke {
    let pts = [(0.0, 0.0), (side, 0.0), (side, side), (0.0, side)]
        .iter()
        .map(|&(x, y)| InputSample::new(x, y, 1.0, 0.0))
        .collect();
    Stroke::new(style(Tool::Fill, 0.0), 500, pts)
}

fn fill_area(s: &Stroke) -> f32 {
    let v: Vec<[f32; 2]> = s.points.iter().map(|p| [p.x, p.y]).collect();
    fill::area(&v).abs()
}

/// The winding number of a polygon around a point.
fn winding(ring: &[[f32; 2]], x: f32, y: f32) -> i32 {
    let mut w = 0;
    for i in 0..ring.len() {
        let (a, b) = (ring[i], ring[(i + 1) % ring.len()]);
        let left = (b[0] - a[0]) * (y - a[1]) - (x - a[0]) * (b[1] - a[1]);
        if a[1] <= y && b[1] > y && left > 0.0 {
            w += 1;
        } else if a[1] > y && b[1] <= y && left < 0.0 {
            w -= 1;
        }
    }
    w
}

fn total_area(pieces: &[Stroke]) -> f32 {
    pieces.iter().map(fill_area).sum()
}

#[test]
fn a_bite_out_of_a_fill_leaves_a_fill_with_the_same_style() {
    let f = square(100.0);
    let Cut::Pieces(p) = cut_stroke(&f, 0.0, 50.0, 0.0, 50.0, 10.0) else {
        panic!("expected a cut");
    };
    assert_eq!(p.len(), 1);
    assert_eq!(p[0].style, f.style);
    assert_eq!(p[0].started_at_ms, 500);
    assert_ne!(p[0].id, f.id);
    // A half circle of radius 10 is gone.
    let gone = 10_000.0 - total_area(&p);
    assert!((gone - 157.0).abs() < 3.0, "{gone}");
}

#[test]
fn a_sweep_through_a_fill_splits_it() {
    let f = square(100.0);
    let Cut::Pieces(p) = cut_stroke(&f, 50.0, -20.0, 50.0, 120.0, 5.0) else {
        panic!("expected a cut");
    };
    assert_eq!(p.len(), 2);
    assert!(
        (total_area(&p) - 9_000.0).abs() < 10.0,
        "{}",
        total_area(&p)
    );
}

#[test]
fn a_hole_in_a_fill_is_joined_by_a_slit_and_stays_empty() {
    let f = square(100.0);
    let Cut::Pieces(p) = cut_stroke(&f, 50.0, 50.0, 50.0, 50.0, 10.0) else {
        panic!("expected a cut");
    };
    assert_eq!(p.len(), 1);
    // The slit has no width, so the area is the square less the disc.
    let expect = 10_000.0 - std::f32::consts::PI * 100.0;
    assert!(
        (fill_area(&p[0]) - expect).abs() < 3.0,
        "{}",
        fill_area(&p[0])
    );
    assert!(p[0].points.len() > 20);
    let ring: Vec<[f32; 2]> = p[0].points.iter().map(|q| [q.x, q.y]).collect();
    assert_eq!(winding(&ring, 50.0, 50.0), 0);
    assert_eq!(winding(&ring, 50.0, 70.0).abs(), 1);
    assert_eq!(winding(&ring, 5.0, 5.0).abs(), 1);
    p[0].points.iter().for_each(|q| assert!(q.x.is_finite()));
}

#[test]
fn a_fill_with_several_holes_keeps_them_all() {
    let mut pieces = vec![square(200.0)];
    for (x, y) in [(40.0, 40.0), (150.0, 50.0), (60.0, 150.0), (150.0, 150.0)] {
        let mut next = Vec::new();
        for f in pieces {
            match cut_stroke(&f, x, y, x, y, 12.0) {
                Cut::Miss => next.push(f),
                Cut::Pieces(p) => next.extend(p),
            }
        }
        pieces = next;
    }
    assert_eq!(pieces.len(), 1);
    let expect = 40_000.0 - 4.0 * std::f32::consts::PI * 144.0;
    assert!(
        (fill_area(&pieces[0]) - expect).abs() < 15.0,
        "{}",
        fill_area(&pieces[0])
    );
}

#[test]
fn a_fill_that_is_covered_disappears() {
    let f = square(20.0);
    let Cut::Pieces(p) = cut_stroke(&f, 10.0, 10.0, 10.0, 10.0, 30.0) else {
        panic!("expected a cut");
    };
    assert!(p.is_empty());
}

#[test]
fn a_sweep_that_misses_a_fill_leaves_it() {
    let f = square(100.0);
    assert_eq!(cut_stroke(&f, 200.0, 50.0, 300.0, 50.0, 10.0), Cut::Miss);
    // Inside the bounding box but clear of a notched shape.
    let notch = [
        (0.0, 0.0),
        (100.0, 0.0),
        (100.0, 100.0),
        (60.0, 100.0),
        (60.0, 30.0),
        (40.0, 30.0),
        (40.0, 100.0),
        (0.0, 100.0),
    ]
    .iter()
    .map(|&(x, y)| InputSample::new(x, y, 1.0, 0.0))
    .collect();
    let u = Stroke::new(style(Tool::Fill, 0.0), 0, notch);
    assert_eq!(cut_stroke(&u, 50.0, 80.0, 50.0, 80.0, 5.0), Cut::Miss);
}

#[test]
fn a_fill_cut_again_by_the_same_sweep_is_a_miss() {
    let f = square(100.0);
    for (x, y, r) in [(50.0, 50.0, 10.0), (0.0, 50.0, 10.0), (3.3, 97.1, 17.0)] {
        let Cut::Pieces(p) = cut_stroke(&f, x, y, x + 20.0, y - 7.0, r) else {
            panic!("expected a cut");
        };
        for q in &p {
            assert_eq!(cut_stroke(q, x, y, x + 20.0, y - 7.0, r), Cut::Miss);
        }
    }
}

#[test]
fn random_sweeps_over_fills_stay_valid_and_only_shrink() {
    let mut rng = Rng(23);
    for _ in 0..60 {
        let mut live = vec![square(rng.range(40.0, 200.0))];
        let mut area = total_area(&live);
        for _ in 0..8 {
            let (x0, y0, x1, y1, r) = random_sweep(&mut rng);
            let mut next = Vec::new();
            for f in live {
                match cut_stroke(&f, x0, y0, x1, y1, r) {
                    Cut::Miss => next.push(f),
                    Cut::Pieces(p) => {
                        for q in &p {
                            assert!(q.points.len() >= 3);
                            assert!(q.points.iter().all(|v| v.x.is_finite() && v.y.is_finite()));
                            assert!(fill_area(q) >= 0.4);
                        }
                        next.extend(p);
                    }
                }
            }
            live = next;
            let now = total_area(&live);
            assert!(now <= area + 0.5, "{now} grew from {area}");
            area = now;
        }
    }
}
