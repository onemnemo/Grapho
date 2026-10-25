//! The paper under the ink: its colour and the lined, grid or dotted
//! pattern, drawn with the spacing and look of the screen.

use super::colour::pattern_color;
use super::content::Content;
use crate::document::{Background, Pattern, MM_TO_PT};

/// Patterns closer together than this, in points, would only be a grey wash.
const MIN_SPACING: f32 = 2.0;
/// A page with more marks than this has a pattern too fine to see.
const MAX_MARKS: f64 = 250_000.0;
const LINE_WIDTH: f32 = 0.5;
const DOT_WIDTH: f32 = 1.2;
/// Round caps need a stroke with a length to show; this is invisibly short.
const DOT_LENGTH: f32 = 0.01;

/// Paints `area` (`x0, y0, x1, y1`, document units) with the paper colour
/// and then its pattern.
pub(super) fn draw(c: &mut Content, bg: &Background, area: [f32; 4]) {
    let [x0, y0, x1, y1] = area;
    c.color(bg.color, "rg");
    c.op(&[x0, y0, x1 - x0, y1 - y0], "re");
    c.raw("f");
    let spacing_mm = match bg.pattern {
        Pattern::Blank => return,
        Pattern::Lined { spacing_mm }
        | Pattern::Grid { spacing_mm }
        | Pattern::Dotted { spacing_mm } => spacing_mm,
    };
    let spacing = spacing_mm * MM_TO_PT;
    if !spacing.is_finite() || spacing < MIN_SPACING {
        return;
    }
    let cols = marks(x0, x1, spacing);
    let rows = marks(y0, y1, spacing);
    if (cols.1 - cols.0 + 1.0).max(0.0) as f64 * (rows.1 - rows.0 + 1.0).max(0.0) as f64 > MAX_MARKS
    {
        return;
    }
    c.color(pattern_color(bg.color), "RG");
    match bg.pattern {
        Pattern::Blank => {}
        Pattern::Lined { .. } => lines(c, rows, spacing, |y| ([x0, y], [x1, y])),
        Pattern::Grid { .. } => {
            lines(c, rows, spacing, |y| ([x0, y], [x1, y]));
            lines(c, cols, spacing, |x| ([x, y0], [x, y1]));
        }
        Pattern::Dotted { .. } => {
            c.op(&[DOT_WIDTH], "w");
            c.raw("1 J");
            for r in rows.0 as i64..=rows.1 as i64 {
                let y = r as f32 * spacing;
                for k in cols.0 as i64..=cols.1 as i64 {
                    let x = k as f32 * spacing;
                    c.op(&[x, y], "m");
                    c.op(&[x + DOT_LENGTH, y], "l");
                }
            }
            c.raw("S");
        }
    }
}

/// The first and last multiples of `spacing` inside `lo..=hi`, as indices.
fn marks(lo: f32, hi: f32, spacing: f32) -> (f32, f32) {
    ((lo / spacing).ceil(), (hi / spacing).floor())
}

fn lines(
    c: &mut Content,
    marks: (f32, f32),
    spacing: f32,
    ends: impl Fn(f32) -> ([f32; 2], [f32; 2]),
) {
    c.op(&[LINE_WIDTH], "w");
    for i in marks.0 as i64..=marks.1 as i64 {
        let (a, b) = ends(i as f32 * spacing);
        c.op(&a, "m");
        c.op(&b, "l");
    }
    c.raw("S");
}
