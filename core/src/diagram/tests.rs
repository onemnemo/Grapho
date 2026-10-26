//! Background removal and tracing on synthetic pictures.

use super::fixtures::{boxed_diagram, flat, gap_to, outline, paint};
use super::*;

const BLACK: u32 = 0x1718_1CFF;

fn alpha(p: &Pixels, x: u32, y: u32) -> u8 {
    p.rgba[((y * p.width + x) * 4 + 3) as usize]
}

fn rgb(p: &Pixels, x: u32, y: u32) -> [u8; 3] {
    let o = ((y * p.width + x) * 4) as usize;
    [p.rgba[o], p.rgba[o + 1], p.rgba[o + 2]]
}

fn polylines(lines: &[TracedLine]) -> Vec<Vec<[f32; 2]>> {
    lines.iter().map(|l| l.points.clone()).collect()
}

/// Every point of the lines is within `tolerance` of the ideal centre
/// lines `ideal`, and every sampled point of those is within it of a line.
fn assert_matches(lines: &[TracedLine], ideal: &[Vec<[f32; 2]>], tolerance: f32) {
    let traced = polylines(lines);
    for pts in &traced {
        for p in pts {
            let d = gap_to(ideal, p[0], p[1]);
            assert!(d <= tolerance, "traced point {p:?} is {d} from the drawing");
        }
    }
    for seg in ideal {
        for w in seg.windows(2) {
            let n = ((w[1][0] - w[0][0]).hypot(w[1][1] - w[0][1]) / 4.0).ceil() as u32;
            for k in 0..=n {
                let t = k as f32 / n.max(1) as f32;
                let (x, y) = (
                    w[0][0] + t * (w[1][0] - w[0][0]),
                    w[0][1] + t * (w[1][1] - w[0][1]),
                );
                let d = gap_to(&traced, x, y);
                assert!(d <= tolerance, "drawing at ({x}, {y}) is {d} from any line");
            }
        }
    }
}

/// The centre lines of `boxed_diagram`, pixel centres included.
fn boxed_ideal() -> Vec<Vec<[f32; 2]>> {
    let r = vec![
        [30.5, 30.5],
        [170.5, 30.5],
        [170.5, 120.5],
        [30.5, 120.5],
        [30.5, 30.5],
    ];
    vec![r, vec![[30.5, 75.5], [170.5, 75.5]]]
}

#[test]
fn argb_ints_become_rgba_and_bad_sizes_are_refused() {
    let p = Pixels::from_argb(2, 1, &[0x80FF_0010u32 as i32, 0xFF01_0203u32 as i32]).unwrap();
    assert_eq!(p.rgba, vec![0xFF, 0x00, 0x10, 0x80, 1, 2, 3, 0xFF]);
    assert!(Pixels::from_argb(2, 2, &[0, 0, 0]).is_none());
    assert!(Pixels::from_argb(0, 3, &[]).is_none());
}

#[test]
fn clean_diagram_gets_exact_transparency_and_solid_lines() {
    let src = boxed_diagram();
    let out = remove_background(&src);
    assert_eq!((out.width, out.height), (src.width, src.height));
    for (i, (a, b)) in src
        .rgba
        .as_chunks::<4>()
        .0
        .iter()
        .zip(out.rgba.as_chunks::<4>().0)
        .enumerate()
    {
        if a[0] == 255 {
            assert_eq!(*b, [0, 0, 0, 0], "paper pixel {i}");
        } else {
            assert_eq!(*b, [0, 0, 0, 255], "ink pixel {i}");
        }
    }
    // Inside the closed box too, not just outside.
    assert_eq!(alpha(&out, 50, 50), 0);
}

#[test]
fn uneven_light_does_not_leave_a_haze() {
    // Paper fades from 110 to 250 across, with dark lines at a fifth of it.
    let mut src = flat(320, 200, [0; 3]);
    let shade = |x: u32| 110.0 + 140.0 * x as f32 / 319.0;
    for y in 0..200 {
        for x in 0..320 {
            let v = shade(x) as u8;
            paint(
                &mut src,
                (x, y, x + 1, y + 1),
                [v, v, (v as f32 * 0.95) as u8],
            );
        }
    }
    for x in 0..320u32 {
        let v = (shade(x) * 0.2) as u8;
        paint(&mut src, (x, 60, x + 1, 63), [v, v, v]);
        paint(&mut src, (x, 140, x + 1, 143), [v, v, v]);
    }
    let out = remove_background(&src);
    let (mut haze, mut weak) = (0, 0);
    for y in 0..200 {
        for x in 0..320 {
            let ink = (60..63).contains(&y) || (140..143).contains(&y);
            match (ink, alpha(&out, x, y)) {
                (false, a) if a != 0 => haze += 1,
                (true, a) if a < 250 => weak += 1,
                _ => {}
            }
        }
    }
    assert_eq!(haze, 0, "paper pixels left behind");
    assert!(weak < 20, "{weak} line pixels not solid");
}

#[test]
fn light_lines_on_a_dark_board_are_kept() {
    let mut src = flat(200, 150, [24, 28, 26]);
    outline(&mut src, (30, 30, 170, 120), 3, [235, 235, 230]);
    let out = remove_background(&src);
    assert_eq!(alpha(&out, 5, 5), 0);
    assert_eq!(alpha(&out, 100, 70), 0);
    assert_eq!(alpha(&out, 100, 30), 255);
    assert_eq!(rgb(&out, 100, 30), [235, 235, 230]);
}

#[test]
fn soft_edges_have_the_paper_taken_out() {
    // A grey ramp edge on white: half way through, the pixel is partly
    // transparent and darker than it was, so it blends back to the same.
    let mut src = flat(64, 64, [255; 3]);
    paint(&mut src, (0, 0, 64, 20), [0; 3]);
    paint(&mut src, (0, 20, 64, 21), [150; 3]);
    let out = remove_background(&src);
    let a = alpha(&out, 10, 20);
    assert!(a > 0 && a < 255, "edge alpha {a}");
    let f = f32::from(rgb(&out, 10, 20)[0]);
    let blended = f * f32::from(a) / 255.0 + 255.0 * (1.0 - f32::from(a) / 255.0);
    assert!((blended - 150.0).abs() < 3.0, "blends to {blended}");
}

#[test]
fn transparent_pixels_stay_transparent_and_flat_pictures_vanish() {
    let mut src = flat(40, 40, [255; 3]);
    for px in src.rgba.as_chunks_mut::<4>().0 {
        px[3] = 0;
    }
    paint(&mut src, (10, 10, 14, 14), [0; 3]);
    let out = remove_background(&src);
    assert_eq!(alpha(&out, 1, 1), 0);
    assert_eq!(alpha(&out, 11, 11), 255);
    let blank = remove_background(&flat(40, 40, [250, 248, 240]));
    assert!(blank.rgba.as_chunks::<4>().0.iter().all(|p| p[3] == 0));
}

#[test]
fn png_output_reads_back() {
    let out = remove_background(&boxed_diagram());
    let bytes = encode_png(&out);
    let source = crate::document::ImageSource::read(bytes.clone()).unwrap();
    assert_eq!((source.width, source.height), (200, 150));
    let mut decoder = png::Decoder::new(std::io::Cursor::new(bytes))
        .read_info()
        .unwrap();
    let mut buf = vec![0; decoder.output_buffer_size().unwrap()];
    decoder.next_frame(&mut buf).unwrap();
    assert_eq!(buf, out.rgba);
    assert!(encode_png(&Pixels {
        width: 3,
        height: 3,
        rgba: vec![0; 5]
    })
    .is_empty());
}

#[test]
fn a_box_with_a_bar_is_traced_where_it_is() {
    let lines = trace(&boxed_diagram());
    assert_matches(&lines, &boxed_ideal(), 2.0);
    for l in &lines {
        assert!((l.width - 3.0).abs() <= 1.0, "width {}", l.width);
        assert_eq!(l.color, BLACK);
        assert!(l.points.len() >= 2);
    }
    // The bar ends on the box: some line has a point near each side.
    let near = |x: f32, y: f32| {
        lines
            .iter()
            .any(|l| gap_to(std::slice::from_ref(&l.points), x, y) < 1.5)
    };
    assert!(near(30.5, 75.5) && near(170.5, 75.5));
}

#[test]
fn a_lone_outline_is_one_closed_line() {
    let mut src = flat(160, 120, [255; 3]);
    outline(&mut src, (30, 20, 130, 90), 4, [0; 3]);
    let lines = trace(&src);
    assert_eq!(lines.len(), 1);
    let pts = &lines[0].points;
    assert_eq!(pts.first(), pts.last());
    assert!(pts.len() <= 12, "{} points for a rectangle", pts.len());
    assert!((lines[0].width - 4.0).abs() <= 1.0);
    let ideal = vec![vec![
        [30.0, 20.0],
        [130.0, 20.0],
        [130.0, 90.0],
        [30.0, 90.0],
        [30.0, 20.0],
    ]];
    assert_matches(&lines, &ideal, 2.5);
}

#[test]
fn a_straight_line_is_two_points() {
    let mut src = flat(120, 40, [255; 3]);
    paint(&mut src, (10, 19, 111, 22), [0; 3]);
    let lines = trace(&src);
    assert_eq!(lines.len(), 1);
    assert_eq!(lines[0].points.len(), 2);
    assert!((lines[0].width - 3.0).abs() < 0.6);
    assert!(lines[0].points[0][0] < 14.0 && lines[0].points[1][0] > 107.0);
}

#[test]
fn dust_and_stubs_do_not_become_lines() {
    let mut src = boxed_diagram();
    paint(&mut src, (10, 140, 12, 141), [0; 3]);
    paint(&mut src, (180, 10, 181, 11), [0; 3]);
    // A rough bump on the bar, shorter than the line is wide.
    paint(&mut src, (100, 72, 101, 74), [0; 3]);
    let lines = trace(&src);
    assert_matches(&lines, &boxed_ideal(), 2.0);
}

#[test]
fn a_small_break_is_closed() {
    let mut src = flat(200, 100, [255; 3]);
    paint(&mut src, (99, 10, 102, 91), [0; 3]);
    paint(&mut src, (20, 49, 96, 52), [0; 3]);
    let lines = trace(&src);
    let reaches = lines
        .iter()
        .flat_map(|l| &l.points)
        .any(|p| (p[0] - 100.5).abs() < 1.0 && (p[1] - 50.5).abs() < 2.0);
    assert!(reaches, "the bar stops short of the post");
}

#[test]
fn colours_are_kept_and_greys_become_black() {
    let mut src = flat(200, 120, [255; 3]);
    paint(&mut src, (10, 20, 190, 23), [220, 30, 30]);
    paint(&mut src, (10, 60, 190, 63), [40, 40, 40]);
    paint(&mut src, (10, 100, 190, 103), [30, 60, 200]);
    let mut lines = trace(&src);
    lines.sort_by(|a, b| a.points[0][1].total_cmp(&b.points[0][1]));
    let colours: Vec<u32> = lines.iter().map(|l| l.color).collect();
    assert_eq!(colours, vec![0xDC1E_1EFF, BLACK, 0x1E3C_C8FF]);
}

#[test]
fn photo_with_shadow_and_a_dark_board_trace_the_same_way() {
    let mut src = flat(320, 200, [0; 3]);
    for y in 0..200u32 {
        for x in 0..320u32 {
            let v = (120.0 + 130.0 * x as f32 / 319.0) as u8;
            paint(&mut src, (x, y, x + 1, y + 1), [v, v, v]);
        }
    }
    for x in 30..290u32 {
        let v = (120.0 + 130.0 * x as f32 / 319.0) as u8 / 5;
        paint(&mut src, (x, 99, x + 1, 102), [v, v, v]);
    }
    let ideal = vec![vec![[30.5, 100.5], [289.5, 100.5]]];
    let lines = trace(&src);
    assert_matches(&lines, &ideal, 2.0);

    let mut board = flat(320, 200, [20, 24, 22]);
    paint(&mut board, (30, 99, 290, 102), [230, 230, 225]);
    let lines = trace(&board);
    assert_matches(&lines, &ideal, 2.0);
    assert_eq!(lines[0].color, BLACK);
}

#[test]
fn cut_out_pictures_trace_from_their_alpha() {
    let cut = remove_background(&boxed_diagram());
    let lines = trace(&cut);
    assert_matches(&lines, &boxed_ideal(), 2.0);
    assert!(trace(&flat(30, 30, [255; 3])).is_empty());
}

#[test]
fn large_pictures_are_traced_at_full_size_coordinates() {
    let mut src = flat(3200, 1200, [255; 3]);
    paint(&mut src, (200, 598, 3000, 604), [0; 3]);
    let lines = trace(&src);
    assert_eq!(lines.len(), 1);
    let ideal = vec![vec![[200.0, 601.0], [3000.0, 601.0]]];
    assert_matches(&lines, &ideal, 6.0);
    assert!(
        (lines[0].width - 6.0).abs() <= 2.0,
        "width {}",
        lines[0].width
    );
}

/// Times both steps on a photo sized picture. Run with
/// `cargo test --release -p grapho-core -- --ignored --nocapture timing`.
#[test]
#[ignore = "slow, prints timings"]
fn timing_on_a_large_picture() {
    let mut src = flat(3000, 2000, [0; 3]);
    for y in 0..2000u32 {
        for x in 0..3000u32 {
            // Cheap repeatable grain on top of the lighting.
            let grain = (x.wrapping_mul(2_654_435_761) ^ y.wrapping_mul(40_503)) % 9;
            let v = (150.0 + 100.0 * x as f32 / 3000.0 - 30.0 * y as f32 / 2000.0) as u8;
            let v = v - 4 + grain as u8;
            paint(&mut src, (x, y, x + 1, y + 1), [v, v, v]);
        }
    }
    for k in 0..12u32 {
        outline(
            &mut src,
            (150 + k * 40, 150 + k * 30, 1500 + k * 60, 900 + k * 50),
            7,
            [30; 3],
        );
    }
    let huge = flat(4096, 4096, [240; 3]);
    let t = std::time::Instant::now();
    let _ = remove_background(&huge);
    println!("4096 x 4096 remove_background {:?}", t.elapsed());
    let t = std::time::Instant::now();
    let out = remove_background(&src);
    let removed = t.elapsed();
    let t = std::time::Instant::now();
    let lines = trace(&src);
    let traced = t.elapsed();
    let t = std::time::Instant::now();
    let png = encode_png(&out);
    println!(
        "remove_background {removed:?}, trace {traced:?} ({} lines), encode_png {:?} ({} bytes)",
        lines.len(),
        t.elapsed(),
        png.len()
    );
    assert!(!lines.is_empty());
}

#[test]
fn a_circle_with_a_diagonal_stays_round() {
    let mut src = flat(220, 220, [255; 3]);
    for y in 0..220u32 {
        for x in 0..220u32 {
            let r = (x as f32 - 110.0).hypot(y as f32 - 110.0);
            if (r - 70.0).abs() <= 1.5 {
                paint(&mut src, (x, y, x + 1, y + 1), [0; 3]);
            }
        }
    }
    for k in 0..90u32 {
        paint(&mut src, (20 + k, 20 + k, 23 + k, 23 + k), [0; 3]);
    }
    let lines = trace(&src);
    for l in &lines {
        for p in &l.points {
            let r = (p[0] - 110.5).hypot(p[1] - 110.5);
            let on_diagonal = (p[0] - p[1]).abs() < 2.5;
            assert!(
                on_diagonal || (r - 70.0).abs() < 2.5,
                "{p:?} is off the drawing"
            );
        }
    }
    // The ring is split where the diagonal meets it, so at least two
    // lines, and they share an end there.
    assert!(
        lines.len() >= 2 && lines.len() <= 4,
        "{} lines",
        lines.len()
    );
}
