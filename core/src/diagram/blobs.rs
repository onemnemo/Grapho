//! Filled shapes: arrowheads, node dots and plates. A line has a skeleton
//! that explains it, but a filled blob does not, so these are found before
//! thinning and handed back as outlines to be filled.

use super::mask::Mask;
use super::simplify::{simplify_closed, smooth, Straightness};

/// A filled region of the drawing.
pub(super) struct Blob {
    /// Corner coordinates of its outline, simplified. Not closed again at
    /// the end.
    pub outline: Vec<[f32; 2]>,
    /// Framed indexes of its pixels, for the colour.
    pub pixels: Vec<usize>,
}

/// Axis ratio from which a thick region counts as a bar, not a blob.
const MAX_AXIS_RATIO: f32 = 3.0;
/// Share of its outline a blob's pixels must fill, so a thick ring is not
/// taken for a disc.
const MIN_SOLID: f32 = 0.85;
const OUTLINE_TOLERANCE: f32 = 0.7;
/// Outlines keep their corners: no straight run is ever merged.
const PLAIN: Straightness = Straightness {
    wander: 0.0,
    min_run: f32::MAX,
    min_cos: 1.0,
};

/// Distance of every ink cell to the nearest seed, by a two pass chamfer
/// sweep that only steps through ink. Paper cells stay far away.
fn distance_from(m: &Mask, seed: &[u8]) -> Vec<f32> {
    const DIAG: f32 = std::f32::consts::SQRT_2;
    let s = m.stride;
    let mut d: Vec<f32> = seed
        .iter()
        .map(|&v| if v == 1 { 0.0 } else { f32::MAX / 4.0 })
        .collect();
    for y in 1..=m.h {
        for x in 1..=m.w {
            let i = y * s + x;
            if m.ink[i] != 1 {
                continue;
            }
            d[i] = d[i]
                .min(d[i - 1] + 1.0)
                .min(d[i - s] + 1.0)
                .min(d[i - s - 1] + DIAG)
                .min(d[i - s + 1] + DIAG);
        }
    }
    for y in (1..=m.h).rev() {
        for x in (1..=m.w).rev() {
            let i = y * s + x;
            if m.ink[i] != 1 {
                continue;
            }
            d[i] = d[i]
                .min(d[i + 1] + 1.0)
                .min(d[i + s] + 1.0)
                .min(d[i + s + 1] + DIAG)
                .min(d[i + s - 1] + DIAG);
        }
    }
    d
}

/// One connected region of a grid.
struct Region {
    pixels: Vec<usize>,
}

/// The 8-connected regions of the cells equal to 1.
fn regions(m: &Mask, grid: &[u8]) -> Vec<Region> {
    let ring = m.ring();
    let mut seen = vec![false; grid.len()];
    let mut out = Vec::new();
    for start in 0..grid.len() {
        if grid[start] != 1 || seen[start] {
            continue;
        }
        seen[start] = true;
        let mut pixels = vec![start];
        let mut at = 0;
        while at < pixels.len() {
            let i = pixels[at];
            at += 1;
            for d in ring {
                let n = i.wrapping_add_signed(d);
                if grid[n] == 1 && !seen[n] {
                    seen[n] = true;
                    pixels.push(n);
                }
            }
        }
        out.push(Region { pixels });
    }
    out
}

/// Ratio of the long axis to the short axis of a region.
fn axis_ratio(m: &Mask, pixels: &[usize]) -> f32 {
    let n = pixels.len() as f32;
    let (mut sx, mut sy, mut sxx, mut syy, mut sxy) = (0.0f32, 0.0f32, 0.0f32, 0.0f32, 0.0f32);
    for &i in pixels {
        let (x, y) = m.xy(i);
        let (x, y) = (x as f32, y as f32);
        sx += x;
        sy += y;
        sxx += x * x;
        syy += y * y;
        sxy += x * y;
    }
    let (mx, my) = (sx / n, sy / n);
    let (a, b, c) = (sxx / n - mx * mx, syy / n - my * my, sxy / n - mx * my);
    let mean = (a + b) / 2.0;
    let spread = ((a - b) / 2.0).hypot(c);
    let (long, short) = (mean + spread, (mean - spread).max(1e-3));
    (long / short).sqrt()
}

/// The outer boundary of a region, as the corners of its pixels, walked
/// with the region on the right hand.
fn outline_of(m: &Mask, grid: &[u8], region: &Region) -> Vec<[i32; 2]> {
    let on = |x: i32, y: i32| {
        x >= 0
            && y >= 0
            && (x as usize) < m.w
            && (y as usize) < m.h
            && grid[m.at(x as usize, y as usize)] == 1
    };
    // The first pixel in reading order has nothing above or to its left
    // in the region, so its top left corner is on the outline.
    let first = region.pixels.iter().copied().min().unwrap_or(0);
    let (x0, y0) = m.xy(first);
    let start = [x0 as i32, y0 as i32];
    let (mut v, mut dir) = (start, 0usize);
    let mut out = Vec::new();
    let limit = region.pixels.len() * 4 + 16;
    for _ in 0..limit {
        out.push(v);
        let (al, ar) = match dir {
            0 => ((v[0], v[1] - 1), (v[0], v[1])),
            1 => ((v[0], v[1]), (v[0] - 1, v[1])),
            2 => ((v[0] - 1, v[1]), (v[0] - 1, v[1] - 1)),
            _ => ((v[0] - 1, v[1] - 1), (v[0], v[1] - 1)),
        };
        dir = if on(al.0, al.1) {
            (dir + 3) % 4
        } else if on(ar.0, ar.1) {
            dir
        } else {
            (dir + 1) % 4
        };
        v = match dir {
            0 => [v[0] + 1, v[1]],
            1 => [v[0], v[1] + 1],
            2 => [v[0] - 1, v[1]],
            _ => [v[0], v[1] - 1],
        };
        if v == start {
            break;
        }
    }
    out
}

fn area_of(outline: &[[i32; 2]]) -> f32 {
    let n = outline.len();
    let twice: i64 = (0..n)
        .map(|i| {
            let (a, b) = (outline[i], outline[(i + 1) % n]);
            i64::from(a[0]) * i64::from(b[1]) - i64::from(b[0]) * i64::from(a[1])
        })
        .sum();
    twice.abs() as f32 / 2.0
}

/// Finds the filled shapes of the drawing and clears them from the mask,
/// so the lines are traced without them. `core_depth` is how deep inside
/// the ink a pixel must be to be part of a blob: well past what the
/// drawing's lines reach.
pub(super) fn take(m: &mut Mask, dist: &[f32], core_depth: f32) -> Vec<Blob> {
    let core: Vec<u8> = dist.iter().map(|&d| u8::from(d >= core_depth)).collect();
    if !core.contains(&1) {
        return Vec::new();
    }
    // Opening: what a disc as big as the core depth fits inside.
    let near_core = distance_from(m, &core);
    let opened: Vec<u8> = m
        .ink
        .iter()
        .zip(&near_core)
        .map(|(&ink, &d)| u8::from(ink == 1 && d <= core_depth))
        .collect();
    let mut seeds = vec![0u8; m.ink.len()];
    let mut any = false;
    for region in regions(m, &opened) {
        if axis_ratio(m, &region.pixels) >= MAX_AXIS_RATIO {
            continue;
        }
        let outline = outline_of(m, &opened, &region);
        if region.pixels.len() as f32 >= MIN_SOLID * area_of(&outline) {
            for &i in &region.pixels {
                seeds[i] = 1;
            }
            any = true;
        }
    }
    if !any {
        return Vec::new();
    }
    // The disc rounds off sharp tips, so take back the ink within reach.
    let near_seed = distance_from(m, &seeds);
    let grown: Vec<u8> = m
        .ink
        .iter()
        .zip(&near_seed)
        .map(|(&ink, &d)| u8::from(ink == 1 && d <= core_depth + 1.0))
        .collect();
    let mut blobs = Vec::new();
    for region in regions(m, &grown) {
        let dense = outline_of(m, &grown, &region);
        let pts: Vec<[f32; 2]> = dense.iter().map(|p| [p[0] as f32, p[1] as f32]).collect();
        let mut outline = simplify_closed(&smooth(&pts, true), OUTLINE_TOLERANCE, &PLAIN);
        outline.pop();
        if outline.len() < 3 {
            continue;
        }
        for &i in &region.pixels {
            m.ink[i] = 0;
        }
        blobs.push(Blob {
            outline,
            pixels: region.pixels,
        });
    }
    blobs
}
