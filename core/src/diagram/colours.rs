//! Splitting the drawing by ink colour, so each pen is traced on its own.
//! Traced together, a red and a blue line that cross or run side by side
//! join into one line of a mixed colour.

use super::mask::Mask;
use super::Pixels;

/// The drawing in one ink colour.
pub(super) struct Layer {
    pub mask: Mask,
    /// Where the mask's top left pixel is in the whole drawing.
    pub origin: [f32; 2],
    /// `0xRRGGBBAA`.
    pub color: u32,
}

/// Hue histogram bins, five degrees each.
const BINS: usize = 72;
/// A pixel counts as coloured from this much difference between its
/// strongest and weakest channel, and that much of its brightest channel.
const MIN_CHROMA: f32 = 40.0;
const MIN_SATURATION: f32 = 0.25;
/// A hue peak must top this many bins each side (20 degrees).
const PEAK_REACH: usize = 4;
/// A colour must cover this share of the ink to get its own layer.
const MIN_PEAK_SHARE: f32 = 0.02;
const MAX_COLOURS: usize = 8;
/// Colours whose channels differ by no more than this are grey.
const GREY_SPREAD: u8 = 40;
/// Neutral ink, the colour of a black pen.
pub(super) const BLACK: u32 = 0x1718_1CFF;
const NEUTRAL: u8 = 0;
const PAPER: u8 = u8::MAX;

/// The hue of a colour in bins, or `None` for a grey.
fn hue_bin(c: [u8; 3]) -> Option<usize> {
    let [r, g, b] = c.map(f32::from);
    let hi = r.max(g).max(b);
    let chroma = hi - r.min(g).min(b);
    if chroma < MIN_CHROMA || chroma < MIN_SATURATION * hi {
        return None;
    }
    let h = if hi == r {
        ((g - b) / chroma).rem_euclid(6.0)
    } else if hi == g {
        (b - r) / chroma + 2.0
    } else {
        (r - g) / chroma + 4.0
    };
    Some(((h / 6.0 * BINS as f32) as usize).min(BINS - 1))
}

fn hue_gap(a: usize, b: usize) -> usize {
    let d = a.abs_diff(b);
    d.min(BINS - d)
}

/// The peaks of a hue histogram, biggest first: bins no lower than any
/// bin within [`PEAK_REACH`], with enough pixels around them.
fn peaks(count: &[u32; BINS], min: u32) -> Vec<usize> {
    let smooth: Vec<u32> = (0..BINS)
        .map(|b| count[(b + BINS - 1) % BINS] + 2 * count[b] + count[(b + 1) % BINS])
        .collect();
    let mut found: Vec<(u32, usize)> = (0..BINS)
        .filter(|&b| {
            (1..=PEAK_REACH).all(|k| {
                // Ties go to the lower bin so a flat top gives one peak.
                smooth[b] > smooth[(b + BINS - k) % BINS] && smooth[b] >= smooth[(b + k) % BINS]
            })
        })
        .map(|b| {
            let near = (0..=2 * PEAK_REACH)
                .map(|k| count[(b + BINS - PEAK_REACH + k) % BINS])
                .sum();
            (near, b)
        })
        .filter(|&(near, _)| near >= min)
        .collect();
    found.sort_unstable_by_key(|f| std::cmp::Reverse(f.0));
    found.truncate(MAX_COLOURS);
    found.into_iter().map(|(_, b)| b).collect()
}

/// The colour of the picture under mask pixel `i`, for a mask `up` times
/// the picture's size.
fn rgb_at(m: &Mask, p: &Pixels, up: usize, i: usize) -> [u8; 3] {
    let (x, y) = m.xy(i);
    let o = ((y / up) * p.width as usize + x / up) * 4;
    [p.rgba[o], p.rgba[o + 1], p.rgba[o + 2]]
}

/// Gives parts of a colour smaller than `min_part` pixels to the colour
/// they touch most: a coloured fringe on black writing, or a dark spot in
/// a coloured line, is not a pen of its own. Black and grey parts that
/// touch more paper than other ink stay, as the bits of a black ring a
/// coloured line runs through are still the ring.
fn absorb_small_parts(m: &Mask, label: &mut [u8], min_part: usize) {
    let ring = m.ring();
    let mut seen = vec![false; label.len()];
    let mut stack = Vec::new();
    let mut part = Vec::new();
    for start in 0..label.len() {
        if label[start] == PAPER || seen[start] {
            continue;
        }
        let own = label[start];
        let mut around = [0u32; MAX_COLOURS + 1];
        let mut paper = 0u32;
        seen[start] = true;
        stack.push(start);
        part.clear();
        while let Some(i) = stack.pop() {
            part.push(i);
            for d in ring {
                let n = i.wrapping_add_signed(d);
                match label[n] {
                    PAPER => paper += 1,
                    l if l == own => {
                        if !seen[n] {
                            seen[n] = true;
                            stack.push(n);
                        }
                    }
                    l => around[l as usize] += 1,
                }
            }
        }
        if part.len() >= min_part {
            continue;
        }
        let best = (0..around.len()).max_by_key(|&l| around[l]).unwrap_or(0);
        let inside = around.iter().sum::<u32>() >= paper;
        if around[best] > 0 && (own != NEUTRAL || inside) {
            for &i in &part {
                label[i] = best as u8;
            }
        }
    }
}

/// The colour of a set of pixels: the middle of the most solid half.
fn colour_of(px: &[usize], m: &Mask, p: &Pixels, up: usize) -> u32 {
    let mut solid: Vec<(u8, [u8; 3])> = px
        .iter()
        .map(|&i| (m.strength[i], rgb_at(m, p, up, i)))
        .collect();
    if solid.is_empty() {
        return BLACK;
    }
    let keep = solid.len().div_ceil(2);
    if keep < solid.len() {
        solid.select_nth_unstable_by(keep - 1, |a, b| b.0.cmp(&a.0));
        solid.truncate(keep);
    }
    let mut rgb = [0u8; 3];
    for (k, out) in rgb.iter_mut().enumerate() {
        let mut v: Vec<u8> = solid.iter().map(|s| s.1[k]).collect();
        let mid = v.len() / 2;
        *out = *v.select_nth_unstable(mid).1;
    }
    let spread = rgb.iter().max().unwrap_or(&0) - rgb.iter().min().unwrap_or(&0);
    if spread <= GREY_SPREAD {
        return BLACK;
    }
    u32::from(rgb[0]) << 24 | u32::from(rgb[1]) << 16 | u32::from(rgb[2]) << 8 | 0xFF
}

/// The drawing split into one mask per ink colour, coloured layers first
/// and the black and grey ink last, so it is drawn on top as it usually
/// was. Pixels of a mixed colour, where two pens overlap, go to the
/// nearest hue. Parts smaller than `min_part` pixels never make a colour
/// of their own. The mask may be `up` times the size of the picture.
pub(super) fn split(m: &Mask, p: &Pixels, up: usize, min_part: usize) -> Vec<Layer> {
    let mut label = vec![PAPER; m.ink.len()];
    let mut hue = vec![0u8; m.ink.len()];
    let mut count = [0u32; BINS];
    let mut ink = 0u32;
    for (i, &v) in m.ink.iter().enumerate() {
        if v != 1 {
            continue;
        }
        ink += 1;
        label[i] = NEUTRAL;
        if let Some(b) = hue_bin(rgb_at(m, p, up, i)) {
            count[b] += 1;
            hue[i] = b as u8 + 1;
        }
    }
    let min = ((MIN_PEAK_SHARE * ink as f32) as u32).max(min_part as u32);
    let found = peaks(&count, min);
    if found.is_empty() {
        let color = colour_of(&pixels_of(&label, NEUTRAL), m, p, up);
        return vec![crop(m, |i| m.ink[i] == 1, color)];
    }
    for (i, l) in label.iter_mut().enumerate() {
        if *l == NEUTRAL && hue[i] > 0 {
            let b = usize::from(hue[i] - 1);
            let nearest = (0..found.len()).min_by_key(|&k| hue_gap(found[k], b));
            *l = nearest.map_or(NEUTRAL, |k| k as u8 + 1);
        }
    }
    absorb_small_parts(m, &mut label, min_part);

    // One layer per distinct colour; a "colour" that comes out grey joins
    // the neutral ink.
    let mut colours: Vec<(u32, Vec<u8>)> = Vec::new();
    for l in (1..=found.len() as u8).chain([NEUTRAL]) {
        let px = pixels_of(&label, l);
        if px.is_empty() {
            continue;
        }
        let c = if l == NEUTRAL {
            BLACK
        } else {
            colour_of(&px, m, p, up)
        };
        match colours.iter_mut().find(|e| e.0 == c) {
            Some(e) => e.1.push(l),
            None => colours.push((c, vec![l])),
        }
    }
    // Black last, so it draws on top.
    colours.sort_by_key(|e| e.0 == BLACK);
    colours
        .into_iter()
        .map(|(color, labels)| {
            let mine = |i: usize| m.ink[i] == 1 && labels.contains(&label[i]);
            crop(m, mine, color)
        })
        .collect()
}

/// A layer of just the pixels `mine` picks, cut down to the box around them.
fn crop(m: &Mask, mine: impl Fn(usize) -> bool, color: u32) -> Layer {
    let (mut x0, mut y0, mut x1, mut y1) = (usize::MAX, usize::MAX, 0, 0);
    for y in 0..m.h {
        for x in 0..m.w {
            if mine(m.at(x, y)) {
                (x0, y0) = (x0.min(x), y0.min(y));
                (x1, y1) = (x1.max(x + 1), y1.max(y + 1));
            }
        }
    }
    if x0 >= x1 {
        (x0, y0, x1, y1) = (0, 0, 1, 1);
    }
    let (w, h) = (x1 - x0, y1 - y0);
    let stride = w + 2;
    let mut mask = Mask {
        w,
        h,
        stride,
        ink: vec![0; stride * (h + 2)],
        strength: vec![0; stride * (h + 2)],
    };
    for y in 0..h {
        for x in 0..w {
            let (from, to) = (m.at(x0 + x, y0 + y), mask.at(x, y));
            mask.strength[to] = m.strength[from];
            mask.ink[to] = u8::from(mine(from));
        }
    }
    Layer {
        mask,
        origin: [x0 as f32, y0 as f32],
        color,
    }
}

fn pixels_of(label: &[u8], l: u8) -> Vec<usize> {
    (0..label.len()).filter(|&i| label[i] == l).collect()
}
