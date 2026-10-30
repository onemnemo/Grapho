//! The options of the background cut: strength.

use super::fixtures::{boxed_diagram, flat, paint};
use super::outline::add_outline;
use super::*;

fn alpha(p: &Pixels, x: u32, y: u32) -> u8 {
    p.rgba[((y * p.width + x) * 4 + 3) as usize]
}

/// White paper with a solid black bar and a faint grey bar.
fn faint_marks() -> Pixels {
    let mut p = flat(120, 80, [255; 3]);
    paint(&mut p, (10, 10, 110, 20), [0; 3]);
    paint(&mut p, (10, 50, 110, 60), [215; 3]);
    p
}

#[test]
fn the_default_strength_is_the_plain_removal() {
    let src = boxed_diagram();
    assert_eq!(cut_out(&src, Cutout::default()), remove_background(&src));
    assert_eq!(Cutout::default().strength, DEFAULT_STRENGTH);
}

#[test]
fn a_stronger_cut_removes_faint_marks_and_a_weaker_one_keeps_them() {
    let src = faint_marks();
    let weak = cut_out(
        &src,
        Cutout {
            strength: 0,
            ..Cutout::default()
        },
    );
    let plain = cut_out(&src, Cutout::default());
    let strong = cut_out(
        &src,
        Cutout {
            strength: 100,
            ..Cutout::default()
        },
    );
    assert!(alpha(&weak, 50, 55) > alpha(&plain, 50, 55));
    assert!(alpha(&plain, 50, 55) >= alpha(&strong, 50, 55));
    assert_eq!(alpha(&strong, 50, 55), 0);
    assert!(alpha(&weak, 50, 55) > 0);
}

#[test]
fn solid_ink_survives_every_strength() {
    let src = faint_marks();
    for strength in [0, 25, 50, 75, 100, 200] {
        let out = cut_out(
            &src,
            Cutout {
                strength,
                ..Cutout::default()
            },
        );
        assert_eq!(alpha(&out, 50, 15), 255, "strength {strength}");
        assert_eq!(alpha(&out, 50, 35), 0, "strength {strength}");
    }
}

fn with_outline(src: &Pixels, outline: Outline) -> Pixels {
    cut_out(
        src,
        Cutout {
            outline,
            ..Cutout::default()
        },
    )
}

fn rgba(p: &Pixels, x: u32, y: u32) -> [u8; 4] {
    let o = ((y * p.width + x) * 4) as usize;
    [p.rgba[o], p.rgba[o + 1], p.rgba[o + 2], p.rgba[o + 3]]
}

/// A black square in the middle of a 300 by 300 white page.
fn square() -> Pixels {
    let mut p = flat(300, 300, [255; 3]);
    paint(&mut p, (100, 100, 200, 200), [0; 3]);
    p
}

#[test]
fn no_outline_changes_nothing() {
    let src = square();
    assert_eq!(with_outline(&src, Outline::None), remove_background(&src));
    assert_eq!(Outline::from_code(0), Outline::None);
    assert_eq!(Outline::from_code(1), Outline::Thin);
    assert_eq!(Outline::from_code(2), Outline::Thick);
    assert_eq!(Outline::from_code(9), Outline::None);
}

#[test]
fn an_outline_is_a_white_band_behind_what_is_kept() {
    let src = square();
    let plain = remove_background(&src);
    let thin = with_outline(&src, Outline::Thin);
    // The square keeps its colour, the band is white, far paper is clear.
    assert_eq!(rgba(&thin, 150, 150), [0, 0, 0, 255]);
    assert_eq!(rgba(&thin, 99, 150), [255, 255, 255, 255]);
    assert_eq!(rgba(&thin, 5, 5)[3], 0);
    assert_eq!(rgba(&plain, 99, 150)[3], 0);
}

#[test]
fn thick_is_wider_than_thin_and_corners_are_round() {
    let src = square();
    let thin = with_outline(&src, Outline::Thin);
    let thick = with_outline(&src, Outline::Thick);
    // 300 px side: the band is 2 px thin and 4 px thick, then a soft pixel.
    assert_eq!(alpha(&thin, 99, 150), 255);
    assert!(alpha(&thin, 98, 150) < 255);
    assert_eq!(alpha(&thin, 97, 150), 0);
    assert_eq!(alpha(&thick, 97, 150), 255);
    assert_eq!(alpha(&thick, 95, 150), 0);
    let band = |p: &Pixels| p.rgba.chunks(4).filter(|px| px[3] > 0).count();
    assert!(band(&thick) > band(&thin));
    // A square band would be solid out on the diagonal; a round one is not.
    assert!(alpha(&thick, 97, 97) < 255);
}

#[test]
fn the_band_fades_over_one_pixel_and_keeps_the_picture_size() {
    let src = square();
    let thick = with_outline(&src, Outline::Thick);
    assert_eq!((thick.width, thick.height), (300, 300));
    // Radius 4: three away is solid, four away is one soft step.
    let soft = alpha(&thick, 96, 150);
    assert!(soft > 0 && soft < 255, "edge alpha {soft}");
}

#[test]
fn a_blank_picture_gets_no_outline() {
    let blank = flat(60, 60, [250; 3]);
    let out = with_outline(&blank, Outline::Thick);
    assert!(out.rgba.chunks(4).all(|px| px[3] == 0));
}

#[test]
fn soft_ink_edges_blend_onto_white() {
    // A half see through red pixel next to nothing: the band shows through it.
    let mut p = Pixels {
        width: 9,
        height: 9,
        rgba: vec![0; 9 * 9 * 4],
    };
    let o = (4 * 9 + 4) * 4;
    p.rgba[o..o + 4].copy_from_slice(&[200, 0, 0, 255]);
    let o = (4 * 9 + 5) * 4;
    p.rgba[o..o + 4].copy_from_slice(&[200, 0, 0, 100]);
    add_outline(&mut p, Outline::Thin);
    let blended = rgba(&p, 5, 4);
    assert_eq!(blended[3], 255);
    assert!(blended[1] > 0 && blended[0] > blended[1]);
}
