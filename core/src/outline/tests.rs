use super::union::signed_area;
use super::*;

fn polygons(packed: &[f32]) -> Vec<Vec<[f32; 2]>> {
    let mut out = Vec::new();
    let mut i = 0;
    while i < packed.len() {
        let n = packed[i] as usize;
        let v = packed[i + 1..i + 1 + 2 * n]
            .chunks(2)
            .map(|c| [c[0], c[1]])
            .collect();
        out.push(v);
        i += 1 + 2 * n;
    }
    assert_eq!(i, packed.len(), "packing must end exactly");
    out
}

fn zigzag() -> Vec<InputSample> {
    [
        (0.0, 0.0),
        (10.0, 0.0),
        (12.0, 1.0),
        (14.0, 3.0),
        (4.0, 3.0),
        (4.0, 3.0),
    ]
    .iter()
    .enumerate()
    .map(|(i, &(x, y))| InputSample::new(x, y, 0.3 + 0.1 * i as f32, i as f32))
    .collect()
}

/// Every test stroke, with the brush it is drawn with.
fn all_strokes() -> Vec<(&'static str, Vec<InputSample>, Brush)> {
    let wide = Brush::with_width(10.0);
    let thin = Brush::with_width(6.0);
    vec![
        ("zigzag", zigzag(), Brush::with_width(3.0)),
        ("tight curl", tight_curl(), wide),
        ("tight loops", tight_loops(), wide),
        ("fast pressure", fast_pressure(), wide),
        ("hairpin", hairpin_stroke(), thin),
        ("sharp zigzag", sharp_zigzag(), thin),
        ("dense curve", dense_curve(), thin),
        ("loop", loop_stroke(), Brush::with_width(8.0)),
        ("handwriting 3", handwriting(3), Brush::with_width(12.0)),
        ("handwriting 6", handwriting(6), Brush::with_width(12.0)),
    ]
}

#[test]
fn contours_do_not_overlap() {
    for (name, pts, brush) in all_strokes() {
        let mut out = Vec::new();
        let b = outline(&pts, &brush, 0.05, &mut out);
        let polys = polygons(&out);
        assert!(!polys.is_empty(), "{name}");
        for p in &polys {
            assert!(p.len() >= 3, "{name}");
            assert!(p.iter().flatten().all(|v| v.is_finite()), "{name}");
            assert!(signed_area(p).abs() > 0.0, "{name}");
        }
        // Outer contours wind positive and holes negative, so the
        // winding of any point is 0 or 1 and never 2 or -1.
        let mut y = b.min_y;
        while y < b.max_y {
            let mut x = b.min_x;
            while x < b.max_x {
                let w = winding(&polys, [x + 0.0137, y + 0.0291]);
                assert!(w == 0 || w == 1, "{name}: winding {w} at {x},{y}");
                x += 0.37;
            }
            y += 0.37;
        }
    }
}

#[test]
fn holes_wind_against_outer_contours() {
    for (name, pts, brush) in all_strokes() {
        if !takes_union(&pts, &brush) {
            continue;
        }
        let mut out = Vec::new();
        outline(&pts, &brush, 0.05, &mut out);
        let total: f32 = polygons(&out).iter().map(|p| signed_area(p)).sum();
        let outer: f32 = polygons(&out)
            .iter()
            .map(|p| signed_area(p))
            .filter(|a| *a > 0.0)
            .sum();
        assert!(total > 0.0 && total <= outer + 1e-3, "{name}");
    }
}

#[test]
fn simple_strokes_are_one_contour() {
    let straight: Vec<InputSample> = (0..5)
        .map(|i| sample(5.0 * i as f32, 0.0, 1.0, i))
        .collect();
    let gentle: Vec<InputSample> = (0..30)
        .map(|i| sample(i as f32, 3.0 * (i as f32 * 0.1).sin(), 1.0, i))
        .collect();
    for pts in [straight, gentle, zigzag(), hairpin_stroke()] {
        let mut out = Vec::new();
        outline(&pts, &Brush::with_width(3.0), 0.05, &mut out);
        assert_eq!(polygons(&out).len(), 1);
    }
}

/// A lead-in that turns sharply onto a full circle of radius 15, drawn a
/// little more than once round.
fn loop_stroke() -> Vec<InputSample> {
    let mut pts = vec![sample(60.0, 30.0, 1.0, 0)];
    pts.extend((0..=140).map(|i| {
        let a = i as f32 * TAU * 1.08 / 140.0;
        sample(30.0 + 15.0 * a.cos(), 30.0 + 15.0 * a.sin(), 1.0, i + 1)
    }));
    pts
}

/// Whether `outline` draws some segments of this stroke as their own pieces.
fn takes_union(pts: &[InputSample], brush: &Brush) -> bool {
    let radius = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
    rough_segments(&distinct(pts), &radius).contains(&true)
}

#[test]
fn a_loop_has_one_outer_contour_and_one_hole() {
    let brush = Brush::with_width(8.0);
    assert!(takes_union(&loop_stroke(), &brush));
    let mut out = Vec::new();
    outline(&loop_stroke(), &brush, 0.05, &mut out);
    let polys = polygons(&out);
    assert_eq!(polys.len(), 2, "{} contours", polys.len());
    let areas: Vec<f32> = polys.iter().map(|p| signed_area(p)).collect();
    assert!(areas[0].signum() != areas[1].signum(), "{areas:?}");
    assert_eq!(winding(&polys, [30.0, 30.0]), 0);
    assert_eq!(winding(&polys, [45.0, 30.0]), 1);
}

#[test]
fn a_gentle_loop_has_no_edge_inside_the_ink() {
    let pts: Vec<InputSample> = (0..=140)
        .map(|i| {
            let a = i as f32 * TAU * 1.08 / 140.0;
            sample(30.0 + 15.0 * a.cos(), 30.0 + 15.0 * a.sin(), 1.0, i)
        })
        .collect();
    let brush = Brush::with_width(8.0);
    assert!(!takes_union(&pts, &brush));
    let mut out = Vec::new();
    outline(&pts, &brush, 0.05, &mut out);
    let polys = polygons(&out);
    // It crosses itself, so it is merged into an outer contour and a hole.
    assert_eq!(polys.len(), 2);
    for p in &polys {
        assert!(!crosses_itself(p.as_flattened()));
    }
    assert_eq!(winding(&polys, [30.0, 30.0]), 0);
    assert_ne!(winding(&polys, [45.0, 30.0]), 0);
}

#[test]
fn a_gentle_curve_skips_the_union() {
    let brush = Brush::with_width(6.0);
    assert!(!takes_union(&dense_curve(), &brush));
    assert!(takes_union(&tight_curl(), &Brush::with_width(10.0)));
}

#[test]
fn single_point_is_one_disc() {
    let mut out = Vec::new();
    let b = outline(
        &[InputSample::new(5.0, 5.0, 1.0, 0.0)],
        &Brush::with_width(4.0),
        0.05,
        &mut out,
    );
    assert_eq!(polygons(&out).len(), 1);
    assert_eq!((b.min_x, b.max_x), (3.0, 7.0));
}

#[test]
fn vertices_stay_inside_the_bounds() {
    let mut out = Vec::new();
    let b = outline(&zigzag(), &Brush::with_width(3.0), 0.05, &mut out);
    for p in polygons(&out) {
        for [x, y] in p {
            assert!(x >= b.min_x - 1e-4 && x <= b.max_x + 1e-4);
            assert!(y >= b.min_y - 1e-4 && y <= b.max_y + 1e-4);
        }
    }
}

#[test]
fn straight_stroke_hugs_its_line() {
    let (w, len, tol) = (4.0_f32, 20.0_f32, 0.05_f32);
    let pts: Vec<InputSample> = (0..5)
        .map(|i| InputSample::new(5.0 * i as f32, 0.0, 1.0, i as f32))
        .collect();
    let mut out = Vec::new();
    outline(&pts, &Brush::with_width(w), tol, &mut out);
    let polys = polygons(&out);
    assert_eq!(polys.len(), 1);
    for [x, y] in polys.iter().flatten() {
        let dx = (x - x.clamp(0.0, len)).abs();
        assert!(dx.hypot(*y) <= w / 2.0 + tol);
    }
    let want = len * w + PI * (w / 2.0) * (w / 2.0);
    let area: f32 = polys.iter().map(|p| signed_area(p).abs()).sum();
    assert!((area - want).abs() < want * 0.02, "area {area} vs {want}");
}

#[test]
fn sharp_bends_get_a_round_join() {
    let hairpin = [
        InputSample::new(0.0, 0.0, 1.0, 0.0),
        InputSample::new(10.0, 0.0, 1.0, 1.0),
        InputSample::new(0.0, 1.0, 1.0, 2.0),
    ];
    let mut out = Vec::new();
    outline(&hairpin, &Brush::with_width(2.0), 0.05, &mut out);
    let polys = polygons(&out);
    assert_eq!(polys.len(), 1);
    assert!(polys.iter().flatten().flatten().all(|v| v.is_finite()));
    // The outside of the bend reaches a full radius past the corner.
    let far = polys
        .iter()
        .flatten()
        .map(|v| v[0])
        .fold(f32::MIN, f32::max);
    assert!(far > 10.0 + 0.95, "far side at {far}");
}

/// Signed distance from `q` to the stroke shape: negative inside. The
/// function dist(q, P(t)) - r(t) is convex in t, so a ternary search
/// finds its minimum along each segment.
fn shape_distance(pts: &[InputSample], brush: &Brush, q: [f32; 2]) -> f32 {
    let r = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
    if pts.len() == 1 {
        return (q[0] - pts[0].x).hypot(q[1] - pts[0].y) - r(&pts[0]);
    }
    // The closest point of each segment gives an upper bound on the
    // answer, and the larger end radius a lower bound for the segment.
    let near = |w: &[InputSample]| {
        let (dx, dy) = (w[1].x - w[0].x, w[1].y - w[0].y);
        let t =
            (((q[0] - w[0].x) * dx + (q[1] - w[0].y) * dy) / (dx * dx + dy * dy)).clamp(0.0, 1.0);
        let d = (q[0] - w[0].x - dx * t).hypot(q[1] - w[0].y - dy * t);
        (d, d - (r(&w[0]) + (r(&w[1]) - r(&w[0])) * t))
    };
    let upper = pts.windows(2).map(|w| near(w).1).fold(f32::MAX, f32::min);
    let mut best = f32::MAX;
    for w in pts.windows(2) {
        if near(w).0 - r(&w[0]).max(r(&w[1])) > upper {
            continue;
        }
        let f = |t: f32| {
            let x = w[0].x + (w[1].x - w[0].x) * t;
            let y = w[0].y + (w[1].y - w[0].y) * t;
            (q[0] - x).hypot(q[1] - y) - (r(&w[0]) + (r(&w[1]) - r(&w[0])) * t)
        };
        let (mut lo, mut hi) = (0.0_f32, 1.0_f32);
        for _ in 0..40 {
            let (a, b) = (lo + (hi - lo) / 3.0, hi - (hi - lo) / 3.0);
            if f(a) < f(b) {
                hi = b;
            } else {
                lo = a;
            }
        }
        best = best.min(f((lo + hi) * 0.5));
    }
    best
}

/// Winding number of `q` summed over every polygon.
fn winding(polys: &[Vec<[f32; 2]>], q: [f32; 2]) -> i32 {
    let mut w = 0;
    for poly in polys {
        for i in 0..poly.len() {
            let (a, b) = (poly[i], poly[(i + 1) % poly.len()]);
            if (a[1] <= q[1]) != (b[1] <= q[1]) {
                let t = (q[1] - a[1]) / (b[1] - a[1]);
                if a[0] + t * (b[0] - a[0]) > q[0] {
                    w += if b[1] > a[1] { 1 } else { -1 };
                }
            }
        }
    }
    w
}

/// Counts grid points that are clearly inside the stroke but not drawn,
/// or clearly outside but drawn.
fn coverage_errors(pts: &[InputSample], brush: &Brush, tol: f32) -> (usize, usize) {
    let mut out = Vec::new();
    let b = outline(pts, brush, tol, &mut out);
    let polys = polygons(&out);
    let pts = distinct(pts);
    let margin = 2.0 * tol + 1e-3;
    let step = 0.1;
    let (mut holes, mut extra) = (0, 0);
    let mut y = b.min_y - 1.0;
    while y < b.max_y + 1.0 {
        let mut x = b.min_x - 1.0;
        while x < b.max_x + 1.0 {
            let d = shape_distance(&pts, brush, [x, y]);
            let drawn = winding(&polys, [x, y]) != 0;
            if d <= -margin {
                if !drawn {
                    holes += 1;
                }
            } else if d >= margin && drawn {
                extra += 1;
            }
            x += step;
        }
        y += step;
    }
    (holes, extra)
}

fn sample(x: f32, y: f32, pressure: f32, i: usize) -> InputSample {
    InputSample::new(x, y, pressure, i as f32)
}

/// Heading changes along `turns` (radius of curvature, angle in
/// radians) steps, 0.5 apart, between two straight tails.
fn walk(turns: &[(f32, f32)]) -> Vec<InputSample> {
    let (mut x, mut y, mut a) = (10.0_f32, 30.0_f32, 0.0_f32);
    let mut pts = vec![sample(x, y, 1.0, 0)];
    let mut step = |a: f32, pts: &mut Vec<InputSample>| {
        x += 0.5 * a.cos();
        y += 0.5 * a.sin();
        pts.push(sample(x, y, 1.0, pts.len()));
    };
    for _ in 0..10 {
        step(a, &mut pts);
    }
    for &(radius, angle) in turns {
        let n = (angle.abs() * radius / 0.5).ceil() as usize;
        for _ in 0..n {
            a += angle.signum() * 0.5 / radius;
            step(a, &mut pts);
        }
    }
    for _ in 0..10 {
        step(a, &mut pts);
    }
    pts
}

/// A U-turn whose radius of curvature is a fifth of the stroke width, with 0.5 between points.
fn tight_curl() -> Vec<InputSample> {
    walk(&[(1.0, PI)])
}

/// Handwriting-like loops: tight turns in alternating directions.
fn tight_loops() -> Vec<InputSample> {
    walk(&[(1.0, PI / 2.0), (1.0, -PI / 2.0), (1.0, PI / 2.0)])
}

fn fast_pressure() -> Vec<InputSample> {
    (0..40)
        .map(|i| {
            let t = i as f32 * 0.25;
            let pressure = 0.6 + 0.12 * (i as f32 * 0.9).sin();
            sample(20.0 + 12.0 * t.cos(), 20.0 + 12.0 * t.sin(), pressure, i)
        })
        .collect()
}

fn hairpin_stroke() -> Vec<InputSample> {
    vec![
        sample(0.0, 0.0, 1.0, 0),
        sample(20.0, 0.0, 1.0, 1),
        sample(0.0, 0.0, 1.0, 2),
    ]
}

fn sharp_zigzag() -> Vec<InputSample> {
    [
        (0.0, 0.0),
        (12.0, 2.0),
        (1.0, 5.0),
        (13.0, 9.0),
        (2.0, 12.0),
    ]
    .iter()
    .enumerate()
    .map(|(i, &(x, y))| sample(x, y, 1.0, i))
    .collect()
}

fn dense_curve() -> Vec<InputSample> {
    (0..400)
        .map(|i| {
            let a = i as f32 * 0.01;
            sample(20.0 + 15.0 * a.cos(), 20.0 + 15.0 * a.sin(), 1.0, i)
        })
        .collect()
}

#[test]
fn drawn_area_matches_the_stroke_shape() {
    let wide = Brush::with_width(10.0);
    let thin = Brush::with_width(6.0);
    let cases: Vec<(&str, Vec<InputSample>, Brush)> = vec![
        ("tight curl", tight_curl(), wide),
        ("tight loops", tight_loops(), wide),
        ("fast pressure", fast_pressure(), Brush::with_width(10.0)),
        ("hairpin", hairpin_stroke(), thin),
        ("zigzag", sharp_zigzag(), thin),
        ("dense curve", dense_curve(), thin),
    ];
    let mut failed = Vec::new();
    for (name, pts, brush) in &cases {
        let (holes, extra) = coverage_errors(pts, brush, 0.05);
        if holes > 0 || extra > 0 {
            failed.push(*name);
        }
    }
    assert!(failed.is_empty(), "coverage errors in {failed:?}");
}

#[test]
fn gentle_turns_add_few_vertices() {
    let pts = dense_curve();
    let mut out = Vec::new();
    outline(&pts, &Brush::with_width(6.0), 0.05, &mut out);
    let polys = polygons(&out);
    let vertices: usize = polys.iter().map(|p| p.len()).sum();
    assert!(vertices < 4 * pts.len(), "{vertices} vertices");
}

/// Run with `cargo test --release -p grapho-core -- --ignored --nocapture timing`.
#[test]
#[ignore]
fn timing() {
    let scribble: Vec<InputSample> = (0..2000)
        .map(|i| {
            let t = i as f32 * 0.05;
            sample(
                100.0 + 80.0 * (t * 0.7).sin() + 20.0 * (t * 3.1).cos(),
                100.0 + 60.0 * (t * 0.5).cos() + 20.0 * (t * 2.3).sin(),
                1.0,
                i,
            )
        })
        .collect();
    for (name, pts) in [
        ("dense 400", dense_curve()),
        ("scribble 2000", scribble),
        ("tight curl", tight_curl()),
        ("tight curl, wide", tight_curl()),
    ] {
        let width = if name.ends_with("wide") { 10.0 } else { 3.0 };
        let brush = Brush::with_width(width);
        let radius = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
        let folds = rough_segments(&distinct(&pts), &radius);
        let n = folds.iter().filter(|f| **f).count();
        let mut out = Vec::new();
        let runs = 50;
        let t = std::time::Instant::now();
        for _ in 0..runs {
            out.clear();
            outline(&pts, &brush, 0.05, &mut out);
        }
        let union_us = t.elapsed().as_micros() / runs;
        let t = std::time::Instant::now();
        for _ in 0..runs {
            out.clear();
            pieces(&pts, &brush, 0.05, &mut out);
        }
        let raw_us = t.elapsed().as_micros() / runs;
        println!(
            "{name}: {n} of {} segments fold, outline {union_us} us, pieces {raw_us} us",
            folds.len()
        );
    }
}

/// A small random number generator, so test strokes are the same every run.
struct Lcg(u64);

impl Lcg {
    /// The next number in `0..1`.
    fn next(&mut self) -> f32 {
        self.0 = self
            .0
            .wrapping_mul(6_364_136_223_846_793_005)
            .wrapping_add(1_442_695_040_888_963_407);
        ((self.0 >> 40) as f32) / ((1u64 << 24) as f32)
    }
}

/// A random handwriting-like stroke: wandering curves, with slow, jittery
/// points and quickly changing pressure where the pen lands and lifts.
fn handwriting(seed: u64) -> Vec<InputSample> {
    let mut r = Lcg(seed);
    let n = 20 + (r.next() * 60.0) as usize;
    let (mut x, mut y, mut a) = (50.0_f32, 50.0_f32, r.next() * TAU);
    let mut curve = 0.0_f32;
    let mut pts = Vec::new();
    for i in 0..n {
        let edge = i.min(n - 1 - i) as f32;
        let speed = if edge < 4.0 {
            0.05 + r.next() * 0.6
        } else {
            0.3 + r.next() * 2.5
        };
        curve = (curve + (r.next() - 0.5) * 0.6).clamp(-1.2, 1.2);
        a += if edge < 3.0 {
            curve * 6.0 * (r.next() - 0.5)
        } else {
            curve
        };
        x += speed * a.cos();
        y += speed * a.sin();
        let pressure = (0.15 + edge * 0.25).min(1.0) * (0.7 + 0.3 * r.next());
        pts.push(sample(x, y, pressure, i));
    }
    pts
}

/// Grid points clearly inside the stroke but not drawn, and clearly outside
/// but drawn, for one of the outline functions.
fn coverage_errors_of(
    draw: fn(&[InputSample], &Brush, f32, &mut Vec<f32>) -> Rect,
    pts: &[InputSample],
    brush: &Brush,
) -> (usize, usize) {
    let tol = 0.05;
    let mut out = Vec::new();
    let b = draw(pts, brush, tol, &mut out);
    let polys = polygons(&out);
    let pts = distinct(pts);
    let margin = 2.0 * tol + 1e-3;
    let (mut holes, mut extra) = (0, 0);
    let mut y = b.min_y - 1.0;
    while y < b.max_y + 1.0 {
        let mut x = b.min_x - 1.0;
        while x < b.max_x + 1.0 {
            let d = shape_distance(&pts, brush, [x, y]);
            let drawn = winding(&polys, [x, y]) != 0;
            if d <= -margin && !drawn {
                holes += 1;
            } else if d >= margin && drawn {
                extra += 1;
            }
            x += 0.2;
        }
        y += 0.2;
    }
    (holes, extra)
}

#[test]
fn handwriting_fills_its_shape_where_the_pen_lands_and_lifts() {
    let brush = Brush::with_width(12.0);
    let mut failed = Vec::new();
    for seed in 0..24 {
        let pts = handwriting(seed);
        let whole = coverage_errors_of(outline, &pts, &brush);
        let quick = coverage_errors_of(pieces, &pts, &brush);
        if whole != (0, 0) || quick != (0, 0) {
            failed.push((seed, whole, quick));
        }
    }
    assert!(
        failed.is_empty(),
        "coverage errors (seed, outline, pieces): {failed:?}"
    );
}

#[test]
fn strokes_far_from_the_origin_are_as_clean_as_near_it() {
    // An infinite canvas puts ink thousands of points out, where an f32 only
    // holds positions to a few ten-thousandths of a point.
    let (ox, oy) = (5083.0, -3499.0);
    let mut failed = Vec::new();
    for seed in 0..40u64 {
        let mut r = Lcg(seed * 7 + 1);
        let width = 0.6 + r.next() * 4.0;
        let brush = Brush::with_width(width);
        // Dense spacing: several samples per point of travel.
        let near: Vec<InputSample> = handwriting(seed)
            .windows(2)
            .flat_map(|w| {
                (0..4).map(move |k| {
                    let t = k as f32 / 4.0;
                    sample(
                        (w[0].x + (w[1].x - w[0].x) * t) * 0.3,
                        (w[0].y + (w[1].y - w[0].y) * t) * 0.3,
                        w[0].pressure + (w[1].pressure - w[0].pressure) * t,
                        0,
                    )
                })
            })
            .collect();
        let far: Vec<InputSample> = near
            .iter()
            .map(|p| sample(p.x + ox, p.y + oy, p.pressure, 0))
            .collect();
        let mut out = Vec::new();
        outline(&far, &brush, 0.02, &mut out);
        // Back near the origin, where the check itself is exact enough.
        let polys: Vec<Vec<[f32; 2]>> = polygons(&out)
            .iter()
            .map(|c| c.iter().map(|v| [v[0] - ox, v[1] - oy]).collect())
            .collect();
        let pts = distinct(&near);
        let (mut bad, mut holes) = (0, 0);
        for p in pts.iter().step_by(2) {
            let rad = brush.width_at(p.pressure) * 0.5;
            for i in -4..=4 {
                for j in -4..=4 {
                    let q = [
                        p.x + rad * i as f32 / 4.0 + 0.0013,
                        p.y + rad * j as f32 / 4.0 + 0.0029,
                    ];
                    let w = winding(&polys, q);
                    if w != 0 && w != 1 {
                        bad += 1;
                    }
                    if w == 0 && shape_distance(&pts, &brush, q) < -0.05 {
                        holes += 1;
                    }
                }
            }
        }
        if bad > 0 || holes > 0 {
            failed.push((seed, bad, holes));
        }
    }
    assert!(failed.is_empty(), "(seed, overlaps, holes): {failed:?}");
}

/// Out and straight back along a diagonal, far from the origin, as a quick
/// flick of the pen on an infinite canvas. Moved to its first point, the
/// first bend is straight but its cosine rounds to just over 1, and the
/// second turns all the way round.
fn flick_back() -> Vec<InputSample> {
    [
        (100000.0, 70000.0),
        (99999.99, 70000.01),
        (99999.984, 70000.016),
        (99999.99, 70000.01),
        (100000.0, 70000.0),
    ]
    .iter()
    .enumerate()
    .map(|(i, &(x, y))| InputSample::new(x, y, [0.8, 1.0, 0.0, 0.4, 0.9][i], i as f32))
    .collect()
}

#[test]
fn a_rounded_straight_bend_does_not_hide_a_turn_back() {
    // This stroke gives the union a NaN vertex if the rounded cosine is not clamped.
    for width in [3.0, 0.0] {
        let mut brush = Brush::with_width(width);
        brush.min_ratio = 0.0;
        let mut out = Vec::new();
        outline(&flick_back(), &brush, 0.05, &mut out);
        let polys = polygons(&out);
        assert!(width == 0.0 || !polys.is_empty(), "width {width}");
        assert!(
            polys.iter().flatten().flatten().all(|v| v.is_finite()),
            "width {width}"
        );
    }
}
