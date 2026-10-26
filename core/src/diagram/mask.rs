//! The ink mask of a picture: which pixels are drawing and which are paper.
//!
//! Grids here carry a one pixel frame of zeros, so a pixel's eight
//! neighbours can be read without bounds checks.

use std::borrow::Cow;

use super::background::{ink_amount, Background};
use super::Pixels;

/// Pictures with a longer side than this are reduced before tracing.
pub(super) const MAX_TRACE_SIDE: u32 = 1600;
/// Ink amount from which a pixel is part of the drawing.
const MASK_AT: f32 = 0.50;
/// Pixels less opaque than this are background.
const MIN_OPAQUE: u8 = 128;
/// Connected bits of ink with fewer pixels than this are dust, at the
/// picture's own size.
pub(super) const MIN_SPECK: usize = 4;
/// Lines are traced at least this many pixels wide. Thinner lines, as in
/// a small screenshot of handwriting, are enlarged first: the skeleton of
/// a two pixel line cannot tell the strokes of a small letter apart.
const TRACE_WIDTH: f32 = 6.0;
/// Most a picture is enlarged by, and most pixels it may have after.
const MAX_ENLARGE: usize = 4;
const MAX_ENLARGED_PIXELS: usize = 4_000_000;

/// The drawing as a bit grid, plus how strongly each pixel is ink.
pub(super) struct Mask {
    pub w: usize,
    pub h: usize,
    /// Row length of the framed grids, `w + 2`.
    pub stride: usize,
    /// 1 where there is ink.
    pub ink: Vec<u8>,
    /// Ink amount times 255, same layout as `ink`.
    pub strength: Vec<u8>,
}

impl Mask {
    /// Index of pixel `(x, y)` in the framed grids.
    #[inline]
    pub fn at(&self, x: usize, y: usize) -> usize {
        (y + 1) * self.stride + x + 1
    }

    /// The pixel coordinates of a framed index.
    #[inline]
    pub fn xy(&self, i: usize) -> (usize, usize) {
        (i % self.stride - 1, i / self.stride - 1)
    }

    /// The eight neighbour offsets, clockwise from north.
    pub fn ring(&self) -> [isize; 8] {
        let s = self.stride as isize;
        [-s, -s + 1, 1, s + 1, s, s - 1, -1, -s - 1]
    }
}

/// The picture reduced by a whole factor so its long side is at most
/// [`MAX_TRACE_SIDE`], and that factor.
pub(super) fn reduce(p: &Pixels) -> (Cow<'_, Pixels>, usize) {
    let f = p.width.max(p.height).div_ceil(MAX_TRACE_SIDE).max(1) as usize;
    if f == 1 {
        return (Cow::Borrowed(p), 1);
    }
    let (w, h) = (p.width as usize, p.height as usize);
    let (ow, oh) = (w.div_ceil(f), h.div_ceil(f));
    let mut rgba = Vec::with_capacity(ow * oh * 4);
    // Per output column: alpha, alpha weighted colour, pixel count.
    let mut acc = vec![[0u32; 5]; ow];
    for oy in 0..oh {
        acc.fill([0; 5]);
        for y in oy * f..((oy + 1) * f).min(h) {
            for (x, px) in p.rgba[y * w * 4..(y + 1) * w * 4]
                .as_chunks::<4>()
                .0
                .iter()
                .enumerate()
            {
                let cell = &mut acc[x / f];
                let a = u32::from(px[3]);
                cell[0] += a;
                for k in 0..3 {
                    cell[1 + k] += a * u32::from(px[k]);
                }
                cell[4] += 1;
            }
        }
        for cell in &acc {
            let weight = cell[0].max(1);
            for k in 0..3 {
                rgba.push(((cell[1 + k] + weight / 2) / weight) as u8);
            }
            rgba.push(((cell[0] + cell[4] / 2) / cell[4].max(1)) as u8);
        }
    }
    let reduced = Pixels {
        width: ow as u32,
        height: oh as u32,
        rgba,
    };
    (Cow::Owned(reduced), f)
}

/// How many times a picture with this mask should be enlarged before
/// tracing so its lines are about [`TRACE_WIDTH`] wide, 1 for not at all.
pub(super) fn enlarge_factor(m: &Mask) -> usize {
    let width = line_width(m);
    let room = MAX_ENLARGED_PIXELS as f32 / (m.w * m.h) as f32;
    let most = (room.sqrt().floor() as usize).min(MAX_ENLARGE);
    ((TRACE_WIDTH / width).ceil() as usize).clamp(1, most.max(1))
}

/// The typical width of the lines: a line of width w has w pixels of area
/// for every two pixels of edge.
fn line_width(m: &Mask) -> f32 {
    let s = m.stride;
    let (mut area, mut edge) = (0usize, 0usize);
    for (i, &v) in m.ink.iter().enumerate() {
        if v == 1 {
            area += 1;
            if m.ink[i - 1] == 0 || m.ink[i + 1] == 0 || m.ink[i - s] == 0 || m.ink[i + s] == 0 {
                edge += 1;
            }
        }
    }
    if edge == 0 {
        return TRACE_WIDTH;
    }
    2.0 * area as f32 / edge as f32
}

/// The mask enlarged `f` times. The ink amounts of neighbouring pixels are
/// blended, so an edge between two pixels falls between the new pixels
/// where it fell before, and the line keeps a smooth edge rather than a
/// staircase of big pixels. Bits of ink smaller than `min_speck` new
/// pixels are dropped.
pub(super) fn enlarge(m: &Mask, f: usize, min_speck: usize) -> Mask {
    let (w, h) = (m.w * f, m.h * f);
    let stride = w + 2;
    // Source position of each new column or row: the lower neighbour and
    // the weight of the upper one.
    let taps = |n: usize, out: usize| -> Vec<(usize, usize, f32)> {
        (0..out)
            .map(|o| {
                let s = ((o as f32 + 0.5) / f as f32 - 0.5).clamp(0.0, (n - 1) as f32);
                let i = s.floor() as usize;
                (i, (i + 1).min(n - 1), s - i as f32)
            })
            .collect()
    };
    let (cols, rows) = (taps(m.w, w), taps(m.h, h));
    // Each source row widened once, then pairs of them blended.
    let wide: Vec<f32> = (0..m.h)
        .flat_map(|y| {
            let row = &m.strength[m.at(0, y)..m.at(0, y) + m.w];
            cols.iter()
                .map(move |&(x0, x1, fx)| f32::from(row[x0]) * (1.0 - fx) + f32::from(row[x1]) * fx)
        })
        .collect();
    let mut out = Mask {
        w,
        h,
        stride,
        ink: vec![0; stride * (h + 2)],
        strength: vec![0; stride * (h + 2)],
    };
    let at = (MASK_AT * 255.0).round() as u8;
    for (y, &(y0, y1, fy)) in rows.iter().enumerate() {
        let (top, bottom) = (&wide[y0 * w..(y0 + 1) * w], &wide[y1 * w..(y1 + 1) * w]);
        let base = (y + 1) * stride + 1;
        for x in 0..w {
            let v = (top[x] * (1.0 - fy) + bottom[x] * fy).round() as u8;
            out.strength[base + x] = v;
            out.ink[base + x] = u8::from(v >= at);
        }
    }
    drop_specks(&mut out, min_speck);
    out
}

/// The mask of a picture. A picture that is mostly transparent is taken as
/// already cut out: its opaque pixels are the drawing. Otherwise the paper
/// colour is estimated and pixels far enough from it are the drawing.
/// Bits of ink smaller than `min_speck` pixels are dropped as dust.
pub(super) fn build(p: &Pixels, min_speck: usize) -> Option<Mask> {
    if !p.is_sound() {
        return None;
    }
    let (w, h) = (p.width as usize, p.height as usize);
    let stride = w + 2;
    let mut mask = Mask {
        w,
        h,
        stride,
        ink: vec![0; stride * (h + 2)],
        strength: vec![0; stride * (h + 2)],
    };
    let clear = p
        .rgba
        .as_chunks::<4>()
        .0
        .iter()
        .filter(|px| px[3] < MIN_OPAQUE)
        .count();
    let cut_out = clear * 2 >= w * h;
    let bg = if cut_out {
        None
    } else {
        Some(Background::estimate(p)?)
    };
    let mut row = vec![[0.0f32; 3]; w];
    for y in 0..h {
        if let Some(bg) = &bg {
            bg.fill_row(y, &mut row);
        }
        let base = (y + 1) * stride + 1;
        for (x, px) in p.rgba[y * w * 4..(y + 1) * w * 4]
            .as_chunks::<4>()
            .0
            .iter()
            .enumerate()
        {
            let s = match &bg {
                _ if px[3] < MIN_OPAQUE => 0.0,
                Some(bg) => ink_amount(bg.mode, [px[0], px[1], px[2]], row[x]),
                None => f32::from(px[3]) / 255.0,
            };
            mask.strength[base + x] = (s * 255.0).round() as u8;
            mask.ink[base + x] = u8::from(s >= MASK_AT);
        }
    }
    drop_specks(&mut mask, min_speck);
    Some(mask)
}

/// Clears connected bits of ink smaller than `min_speck` pixels.
fn drop_specks(m: &mut Mask, min_speck: usize) {
    let ring = m.ring();
    let mut stack: Vec<usize> = Vec::new();
    let mut found: Vec<usize> = Vec::new();
    for start in 0..m.ink.len() {
        if m.ink[start] != 1 {
            continue;
        }
        m.ink[start] = 2;
        stack.push(start);
        found.clear();
        while let Some(i) = stack.pop() {
            found.push(i);
            for d in ring {
                let n = i.wrapping_add_signed(d);
                if m.ink[n] == 1 {
                    m.ink[n] = 2;
                    stack.push(n);
                }
            }
        }
        if found.len() < min_speck {
            for &i in &found {
                m.ink[i] = 0;
            }
        }
    }
    for v in &mut m.ink {
        *v = u8::from(*v == 2);
    }
}

/// Distance from each ink pixel centre to the nearest paper pixel centre,
/// by a two pass chamfer sweep. Same layout as the mask.
pub(super) fn distance(m: &Mask) -> Vec<f32> {
    const DIAG: f32 = std::f32::consts::SQRT_2;
    let s = m.stride;
    let mut d: Vec<f32> = m
        .ink
        .iter()
        .map(|&v| if v == 1 { f32::MAX / 4.0 } else { 0.0 })
        .collect();
    for y in 1..=m.h {
        for x in 1..=m.w {
            let i = y * s + x;
            if m.ink[i] == 1 {
                d[i] = (d[i - 1] + 1.0)
                    .min(d[i - s] + 1.0)
                    .min(d[i - s - 1] + DIAG)
                    .min(d[i - s + 1] + DIAG);
            }
        }
    }
    for y in (1..=m.h).rev() {
        for x in (1..=m.w).rev() {
            let i = y * s + x;
            if m.ink[i] == 1 {
                d[i] = d[i]
                    .min(d[i + 1] + 1.0)
                    .min(d[i + s] + 1.0)
                    .min(d[i + s + 1] + DIAG)
                    .min(d[i + s - 1] + DIAG);
            }
        }
    }
    d
}
