//! Paper and ink colour rules shared with the screen: which paper counts as
//! dark, how ink is adapted to it, and the colour of the paper pattern.
//! Colours are `0xRRGGBBAA`.

const DARK_PAPER_BELOW: f32 = 0.4;
const NEAR_BLACK_BELOW: f32 = 0.06;
const NEAR_WHITE_ABOVE: f32 = 0.8;
/// The most colourful an ink can be (max channel minus min) and still count
/// as black or white.
const MAX_CHROMA: i32 = 40;
const LIGHT_INK: u32 = 0xECEC_EC00;
const DARK_INK: u32 = 0x1A1A_1A00;
const LIGHT_PATTERN_INK: u32 = 0x6C76_8E00;

pub(super) const WHITE: u32 = 0xFFFF_FFFF;

fn channels(rgba: u32) -> [u8; 3] {
    [(rgba >> 24) as u8, (rgba >> 16) as u8, (rgba >> 8) as u8]
}

/// WCAG relative luminance of the colour, ignoring alpha.
fn luminance(rgba: u32) -> f32 {
    let [r, g, b] = channels(rgba).map(|c| {
        let c = f32::from(c) / 255.0;
        if c <= 0.03928 {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).powf(2.4)
        }
    });
    0.2126 * r + 0.7152 * g + 0.0722 * b
}

pub(super) fn is_dark_paper(paper: u32) -> bool {
    luminance(paper) < DARK_PAPER_BELOW
}

/// The colour to draw ink with on a paper: near-black turns near-white on
/// dark paper and near-white turns near-black on light paper. Alpha is kept.
pub(super) fn display_color(ink: u32, paper: u32) -> u32 {
    let [r, g, b] = channels(ink).map(i32::from);
    if r.max(g).max(b) - r.min(g).min(b) > MAX_CHROMA {
        return ink;
    }
    let lum = luminance(ink);
    let alpha = ink & 0xFF;
    if is_dark_paper(paper) && lum < NEAR_BLACK_BELOW {
        LIGHT_INK | alpha
    } else if !is_dark_paper(paper) && lum > NEAR_WHITE_ABOVE {
        DARK_INK | alpha
    } else {
        ink
    }
}

/// Opaque blend of `from` toward `to` by `t`.
fn mix(from: u32, to: u32, t: f32) -> u32 {
    let [a, b, c] = channels(from);
    let [x, y, z] = channels(to);
    let one = |f: u8, o: u8| (f32::from(f) + (f32::from(o) - f32::from(f)) * t + 0.5) as u32;
    (one(a, x) << 24) | (one(b, y) << 16) | (one(c, z) << 8) | 0xFF
}

/// A subtle line or dot colour, tinted by the paper so cream stays warm.
pub(super) fn pattern_color(paper: u32) -> u32 {
    if is_dark_paper(paper) {
        mix(paper, WHITE, 0.18)
    } else {
        mix(paper, LIGHT_PATTERN_INK, 0.3)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn near_black_ink_turns_light_on_dark_paper_only() {
        let black = 0x0000_00FF;
        assert_eq!(display_color(black, 0x1C1D_21FF), 0xECEC_ECFF);
        assert_eq!(display_color(black, WHITE), black);
        assert_eq!(display_color(WHITE, WHITE), 0x1A1A_1AFF);
        assert_eq!(display_color(WHITE, 0x0000_00FF), WHITE);
    }

    #[test]
    fn coloured_ink_and_alpha_are_left_alone() {
        let red = 0xE53C_3CFF;
        assert_eq!(display_color(red, 0x0000_00FF), red);
        assert_eq!(display_color(0x0000_0040, 0x0000_00FF), 0xECEC_EC40);
    }

    #[test]
    fn pattern_colour_follows_the_paper() {
        // White mixed 30 percent toward the pattern ink, and black toward white.
        assert_eq!(pattern_color(WHITE), 0xD3D6_DDFF);
        assert_eq!(pattern_color(0x0000_00FF), 0x2E2E_2EFF);
    }
}
