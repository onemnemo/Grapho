use super::*;

fn s(x: f32, y: f32) -> InputSample {
    InputSample::new(x, y, 0.8, x)
}

fn straight(len: f32, step: f32) -> Vec<InputSample> {
    let n = (len / step) as usize;
    (0..=n).map(|i| s(i as f32 * step, 0.0)).collect()
}

#[test]
fn dashes_follow_the_pattern() {
    let runs = dash_runs(&straight(100.0, 100.0), LineStyle::Dashed, 2.0);
    // On 8, off 6, so dashes start every 14 units.
    assert_eq!(runs.len(), 8);
    for (i, r) in runs.iter().enumerate() {
        let (a, b) = (r[0], r[r.len() - 1]);
        assert!((a.x - 14.0 * i as f32).abs() < 1e-3, "{i}: {}", a.x);
        let expect = if i == 7 { 2.0 } else { 8.0 };
        assert!((b.x - a.x - expect).abs() < 1e-3, "{i}: {}", b.x - a.x);
    }
}

#[test]
fn dots_are_single_points_at_the_period() {
    let runs = dash_runs(&straight(30.0, 30.0), LineStyle::Dotted, 2.0);
    let xs: Vec<f32> = runs.iter().map(|r| r[0].x).collect();
    assert!(runs.iter().all(|r| r.len() == 1));
    assert_eq!(xs.len(), 6);
    for (i, x) in xs.iter().enumerate() {
        assert!((x - 5.0 * i as f32).abs() < 1e-3, "{xs:?}");
    }
}

#[test]
fn feeding_in_pieces_gives_the_same_pattern() {
    for line in [LineStyle::Dashed, LineStyle::Dotted] {
        let whole = dash_runs(&straight(200.0, 200.0), line, 3.0);
        let split = dash_runs(&straight(200.0, 8.0), line, 3.0);
        assert_eq!(whole.len(), split.len(), "{line:?}");
        for (a, b) in whole.iter().zip(&split) {
            assert!((a[0].x - b[0].x).abs() < 1e-2, "{line:?}");
            assert!((a[a.len() - 1].x - b[b.len() - 1].x).abs() < 1e-2);
        }
    }
}

#[test]
fn a_dash_round_a_corner_is_one_run() {
    let pts = [s(0.0, 0.0), s(5.0, 0.0), s(5.0, 5.0)];
    let runs = dash_runs(&pts, LineStyle::Dashed, 2.0);
    assert_eq!(runs[0].len(), 3);
    assert_eq!(runs[0][1], s(5.0, 0.0));
}

#[test]
fn pressure_and_time_are_interpolated() {
    let pts = [
        InputSample::new(0.0, 0.0, 0.0, 0.0),
        InputSample::new(100.0, 0.0, 1.0, 100.0),
    ];
    let runs = dash_runs(&pts, LineStyle::Dashed, 2.0);
    let end = runs[0][1];
    assert!((end.pressure - 0.08).abs() < 1e-4 && (end.time_ms - 8.0).abs() < 1e-3);
}

#[test]
fn solid_is_untouched() {
    let pts = straight(50.0, 5.0);
    assert_eq!(dash_runs(&pts, LineStyle::Solid, 2.0), vec![pts.clone()]);
    let brush = Brush::with_width(2.0);
    let (mut a, mut b) = (Vec::new(), Vec::new());
    let ra = outline_styled(&pts, &brush, LineStyle::Solid, 0.1, &mut a);
    let rb = outline(&pts, &brush, 0.1, &mut b);
    assert_eq!((a, ra), (b, rb));
    let (mut a, mut b) = (Vec::new(), Vec::new());
    tessellate_styled(&pts, &brush, LineStyle::Solid, &mut a);
    tessellate(&pts, &brush, &mut b);
    assert_eq!(a, b);
}

#[test]
fn styled_outlines_are_separate_pieces_within_bounds() {
    let pts = straight(100.0, 100.0);
    let brush = Brush::with_width(2.0);
    for line in [LineStyle::Dashed, LineStyle::Dotted] {
        let (mut out, mut quick) = (Vec::new(), Vec::new());
        let b = outline_styled(&pts, &brush, line, 0.1, &mut out);
        let q = outline_pieces_styled(&pts, &brush, line, 0.1, &mut quick);
        assert!(!out.is_empty() && !quick.is_empty());
        assert_eq!(b, q);
        assert!(b.min_x < 0.0 && b.max_x <= 100.0 + 1.0);
        let mut contours = 0;
        let mut i = 0;
        while i < out.len() {
            i += 1 + out[i] as usize * 2;
            contours += 1;
        }
        assert_eq!(contours, dash_runs(&pts, line, 2.0).len());
    }
}

#[test]
fn a_single_tap_still_draws() {
    for line in [LineStyle::Dashed, LineStyle::Dotted] {
        let mut out = Vec::new();
        tessellate_styled(&[s(3.0, 3.0)], &Brush::default(), line, &mut out);
        assert!(!out.is_empty());
    }
}

#[test]
fn unknown_line_bytes_are_refused() {
    assert_eq!(LineStyle::from_u8(2), Some(LineStyle::Dotted));
    assert_eq!(LineStyle::from_u8(3), None);
}
