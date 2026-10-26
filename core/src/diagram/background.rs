//! Paper colour estimate and the background removal built on it.
//!
//! The picture is cut into tiles. Each tile votes for the colour of the
//! paper in it (a bright percentile on light paper, a dark one on a
//! blackboard), tiles that are mostly ink are filled in from their
//! neighbours, and the grid is blended smoothly into a colour for every
//! pixel. That follows shadows and uneven light that one global colour
//! cannot.

use super::outline::{add_outline, Outline};
use super::Pixels;

/// Below this amount of ink a pixel is fully see through.
pub(super) const INK_LOW: f32 = 0.10;
/// From this amount of ink a pixel is fully solid.
pub(super) const INK_HIGH: f32 = 0.50;
/// Pixels less opaque than this do not vote for the paper colour.
const MIN_OPAQUE: u8 = 128;
/// How far a tile may sit from the median of its neighbours, in luma, on
/// the side of the ink before it counts as covered by ink.
const INK_TILE_GAP: f32 = 30.0;
/// Luma levels each side of the percentile that are averaged into the
/// tile's colour.
const BAND: usize = 6;

/// Whether the ink is darker or lighter than the paper.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub(super) enum Mode {
    Light,
    Dark,
}

/// How much of `c` is ink over paper `b`, from 0 (paper) to 1 (all ink),
/// relative to the paper's own brightness so dim parts of a photo are not
/// penalised. Ink is the side away from the paper: darker on light paper,
/// brighter on dark.
#[inline]
pub(super) fn ink_amount(mode: Mode, c: [u8; 3], b: [f32; 3]) -> f32 {
    let mut s = 0.0f32;
    for k in 0..3 {
        let v = f32::from(c[k]);
        let d = match mode {
            Mode::Light => (b[k] - v) / b[k].max(48.0),
            Mode::Dark => (v - b[k]) / (255.0 - b[k]).max(48.0),
        };
        s = s.max(d);
    }
    s.min(1.0)
}

#[inline]
fn luma(px: &[u8]) -> usize {
    ((77 * u32::from(px[0]) + 151 * u32::from(px[1]) + 28 * u32::from(px[2])) >> 8) as usize
}

/// What one tile says about the paper.
struct TileVote {
    light: [f32; 3],
    dark: [f32; 3],
    median: f32,
}

/// The estimated paper colour of a picture.
pub(super) struct Background {
    pub mode: Mode,
    tile: usize,
    gw: usize,
    gh: usize,
    grid: Vec<[f32; 3]>,
    /// Per pixel column: the two tile columns to blend and the weight of
    /// the second.
    cols: Vec<(u32, u32, f32)>,
}

impl Background {
    /// `None` when no pixel is opaque enough to say anything.
    pub fn estimate(p: &Pixels) -> Option<Self> {
        let (w, h) = (p.width as usize, p.height as usize);
        let tile = (w.max(h) / 40).clamp(32, 64).min(w.min(h)).max(1);
        let (gw, gh) = (w.div_ceil(tile), h.div_ceil(tile));
        let votes = tile_votes(p, tile, gw, gh);

        let mut medians: Vec<f32> = votes.iter().flatten().map(|v| v.median).collect();
        if medians.is_empty() {
            return None;
        }
        let mid = medians.len() / 2;
        let (_, &mut median, _) = medians.select_nth_unstable_by(mid, f32::total_cmp);
        let mode = if median < 128.0 {
            Mode::Dark
        } else {
            Mode::Light
        };

        let picked: Vec<Option<[f32; 3]>> = votes
            .iter()
            .map(|v| {
                v.as_ref().map(|v| match mode {
                    Mode::Light => v.light,
                    Mode::Dark => v.dark,
                })
            })
            .collect();
        let grid = repair(&picked, gw, gh, mode);
        let cols = (0..w)
            .map(|x| {
                let f = ((x as f32 + 0.5) / tile as f32 - 0.5).clamp(0.0, (gw - 1) as f32);
                let i0 = f.floor() as usize;
                (i0 as u32, (i0 + 1).min(gw - 1) as u32, f - i0 as f32)
            })
            .collect();
        Some(Self {
            mode,
            tile,
            gw,
            gh,
            grid,
            cols,
        })
    }

    /// The paper colour under every pixel of row `y`.
    pub fn fill_row(&self, y: usize, out: &mut [[f32; 3]]) {
        let f = ((y as f32 + 0.5) / self.tile as f32 - 0.5).clamp(0.0, (self.gh - 1) as f32);
        let j0 = f.floor() as usize;
        let j1 = (j0 + 1).min(self.gh - 1);
        let wy = f - j0 as f32;
        let (top, bottom) = (j0 * self.gw, j1 * self.gw);
        for (o, &(i0, i1, wx)) in out.iter_mut().zip(&self.cols) {
            let (i0, i1) = (i0 as usize, i1 as usize);
            let (tl, tr) = (self.grid[top + i0], self.grid[top + i1]);
            let (bl, br) = (self.grid[bottom + i0], self.grid[bottom + i1]);
            *o = std::array::from_fn(|k| {
                let a = tl[k] * (1.0 - wx) + tr[k] * wx;
                let b = bl[k] * (1.0 - wx) + br[k] * wx;
                a * (1.0 - wy) + b * wy
            });
        }
    }
}

/// One vote per tile, `None` where too few pixels are opaque.
fn tile_votes(p: &Pixels, tile: usize, gw: usize, gh: usize) -> Vec<Option<TileVote>> {
    let (w, h) = (p.width as usize, p.height as usize);
    let mut votes = Vec::with_capacity(gw * gh);
    let mut count = [0u32; 256];
    let mut sum = [[0u32; 3]; 256];
    for ty in 0..gh {
        for tx in 0..gw {
            count.fill(0);
            sum.fill([0; 3]);
            let (x0, x1) = (tx * tile, ((tx + 1) * tile).min(w));
            let (y0, y1) = (ty * tile, ((ty + 1) * tile).min(h));
            let mut n = 0u32;
            for y in y0..y1 {
                let row = &p.rgba[(y * w + x0) * 4..(y * w + x1) * 4];
                for px in row.as_chunks::<4>().0 {
                    if px[3] < MIN_OPAQUE {
                        continue;
                    }
                    let l = luma(px);
                    count[l] += 1;
                    for k in 0..3 {
                        sum[l][k] += u32::from(px[k]);
                    }
                    n += 1;
                }
            }
            let area = (x1 - x0) * (y1 - y0);
            votes.push((n as usize * 4 >= area).then(|| vote(&count, &sum, n)));
        }
    }
    votes
}

fn vote(count: &[u32; 256], sum: &[[u32; 3]; 256], n: u32) -> TileVote {
    let level = |percent: u32| {
        let mut seen = 0;
        for (l, &c) in count.iter().enumerate() {
            seen += c;
            if seen * 100 >= n * percent {
                return l;
            }
        }
        255
    };
    let band = |centre: usize| {
        let (mut total, mut acc) = (0u32, [0u32; 3]);
        for l in centre.saturating_sub(BAND)..=(centre + BAND).min(255) {
            total += count[l];
            for k in 0..3 {
                acc[k] += sum[l][k];
            }
        }
        let total = total.max(1) as f32;
        acc.map(|v| v as f32 / total)
    };
    TileVote {
        light: band(level(85)),
        dark: band(level(15)),
        median: level(50) as f32,
    }
}

fn colour_luma(c: [f32; 3]) -> f32 {
    0.30 * c[0] + 0.59 * c[1] + 0.11 * c[2]
}

/// Fills tiles with no vote, and tiles that sit far on the ink side of
/// their neighbours (mostly covered by a line or a filled shape), with the
/// median colour of the nearby tiles.
fn repair(picked: &[Option<[f32; 3]>], gw: usize, gh: usize, mode: Mode) -> Vec<[f32; 3]> {
    let mut out = Vec::with_capacity(picked.len());
    let mut near: Vec<(f32, [f32; 3])> = Vec::with_capacity(25);
    for ty in 0..gh {
        for tx in 0..gw {
            let own = picked[ty * gw + tx];
            let mut radius = 2;
            let reach = gw + gh;
            loop {
                near.clear();
                let (x0, x1) = (tx.saturating_sub(radius), (tx + radius).min(gw - 1));
                let (y0, y1) = (ty.saturating_sub(radius), (ty + radius).min(gh - 1));
                for y in y0..=y1 {
                    for x in x0..=x1 {
                        if let Some(c) = picked[y * gw + x] {
                            near.push((colour_luma(c), c));
                        }
                    }
                }
                if !near.is_empty() || radius >= reach {
                    break;
                }
                radius += 1;
            }
            near.sort_by(|a, b| a.0.total_cmp(&b.0));
            let middle = near.get(near.len() / 2).copied();
            let colour = match (own, middle) {
                (Some(c), Some((m, _))) => {
                    let l = colour_luma(c);
                    let covered = match mode {
                        Mode::Light => l < m - INK_TILE_GAP,
                        Mode::Dark => l > m + INK_TILE_GAP,
                    };
                    if covered {
                        middle.map_or(c, |(_, m)| m)
                    } else {
                        c
                    }
                }
                (Some(c), None) => c,
                (None, Some((_, m))) => m,
                (None, None) => [255.0; 3],
            };
            out.push(colour);
        }
    }
    out
}

/// The strength that gives the built in thresholds, [`INK_LOW`] and
/// [`INK_HIGH`].
pub const DEFAULT_STRENGTH: u8 = 50;
/// The strongest setting.
pub const MAX_STRENGTH: u8 = 100;

/// How a background is taken out.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Cutout {
    /// How much counts as background, from 0 (only the clearest paper goes)
    /// to 100 (faint marks and shadows go too). Larger values are cut to
    /// 100.
    pub strength: u8,
    /// A band drawn around what is kept.
    pub outline: Outline,
}

impl Default for Cutout {
    fn default() -> Self {
        Self {
            strength: DEFAULT_STRENGTH,
            outline: Outline::None,
        }
    }
}

impl Cutout {
    /// The amounts of ink where a pixel stops being see through and where
    /// it becomes solid. The default strength gives exactly
    /// [`INK_LOW`] and [`INK_HIGH`]; every 50 steps doubles or halves them.
    fn thresholds(self) -> (f32, f32) {
        let strength = self.strength.min(MAX_STRENGTH);
        if strength == DEFAULT_STRENGTH {
            return (INK_LOW, INK_HIGH);
        }
        let scale = ((f32::from(strength) - f32::from(DEFAULT_STRENGTH)) / 50.0).exp2();
        (INK_LOW * scale, (INK_HIGH * scale).min(0.98))
    }
}

/// A copy with the paper made see through, at the default strength. See
/// [`cut_out`].
pub fn remove_background(p: &Pixels) -> Pixels {
    cut_out(p, Cutout::default())
}

/// A copy with the paper made see through. Pixels that are ink keep their
/// colour; pixels partly ink (the soft edge of a line) become partly
/// transparent with the paper colour taken back out of them, so the line
/// looks the same on any new background. Already transparent pixels stay
/// transparent. A picture with nothing opaque in it is returned as it is.
/// The outline, if any, is added last.
pub fn cut_out(p: &Pixels, options: Cutout) -> Pixels {
    let Some(bg) = p.is_sound().then(|| Background::estimate(p)).flatten() else {
        return p.clone();
    };
    let (low, high) = options.thresholds();
    let (w, h) = (p.width as usize, p.height as usize);
    let mut out = vec![0u8; p.rgba.len()];
    let mut row = vec![[0.0f32; 3]; w];
    for y in 0..h {
        bg.fill_row(y, &mut row);
        let src = &p.rgba[y * w * 4..(y + 1) * w * 4];
        let dst = &mut out[y * w * 4..(y + 1) * w * 4];
        for ((s, d), b) in src
            .as_chunks::<4>()
            .0
            .iter()
            .zip(dst.as_chunks_mut::<4>().0)
            .zip(&row)
        {
            *d = lift(bg.mode, *s, *b, low, high);
        }
    }
    let mut cut = Pixels {
        width: p.width,
        height: p.height,
        rgba: out,
    };
    add_outline(&mut cut, options.outline);
    cut
}

/// One pixel with the paper `b` taken out. Ink below `low` is see through
/// and from `high` solid.
#[inline]
fn lift(mode: Mode, s: [u8; 4], b: [f32; 3], low: f32, high: f32) -> [u8; 4] {
    if s[3] == 0 {
        return [0; 4];
    }
    let ink = ink_amount(mode, [s[0], s[1], s[2]], b);
    let a = ((ink - low) / (high - low)).clamp(0.0, 1.0);
    if a <= 0.0 {
        return [0; 4];
    }
    if a >= 1.0 {
        return s;
    }
    let mut d = [0u8; 4];
    for k in 0..3 {
        let f = (f32::from(s[k]) - (1.0 - a) * b[k]) / a;
        d[k] = f.round().clamp(0.0, 255.0) as u8;
    }
    d[3] = (a * f32::from(s[3])).round() as u8;
    d
}
