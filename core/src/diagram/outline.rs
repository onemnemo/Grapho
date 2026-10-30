//! A sticker style outline: a band of solid colour around what the cut
//! kept, drawn behind it.
//!
//! The band is every pixel within a radius of a kept pixel, measured with
//! an exact distance transform so corners come out round. Its edge is
//! anti-aliased over one pixel. The picture keeps its size, so the band is
//! cut off at the picture's edges.

use super::Pixels;

/// Pixels at least this opaque are what the band follows.
const KEPT: u8 = 128;
/// Stands in for "no kept pixel anywhere on this line". Far beyond any real
/// squared distance, yet small enough to keep `f32` sums exact enough.
const FAR: f32 = 1.0e9;

/// How thick the outline is.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum Outline {
    #[default]
    None,
    Thin,
    Thick,
}

impl Outline {
    /// From the numbers the shell uses: 0 none, 1 thin, 2 thick. Anything
    /// else is none.
    pub fn from_code(code: i32) -> Self {
        match code {
            1 => Self::Thin,
            2 => Self::Thick,
            _ => Self::None,
        }
    }

    /// The band's width in pixels for a picture of this size: a fixed share
    /// of the longer side, so the outline looks the same at any resolution.
    fn radius(self, width: u32, height: u32) -> f32 {
        let side = width.max(height) as f32;
        match self {
            Self::None => 0.0,
            Self::Thin => (side * 0.006).max(2.0),
            Self::Thick => (side * 0.012).max(4.0),
        }
    }
}

/// `p` with a white band of the given thickness behind everything that is
/// opaque. Nothing changes for [`Outline::None`] or a picture with no kept
/// pixel.
pub(super) fn add_outline(p: &mut Pixels, outline: Outline) {
    let radius = outline.radius(p.width, p.height);
    if radius <= 0.0 || !p.is_sound() {
        return;
    }
    let (w, h) = (p.width as usize, p.height as usize);
    let mut dist: Vec<f32> = p
        .rgba
        .as_chunks::<4>()
        .0
        .iter()
        .map(|px| if px[3] >= KEPT { 0.0 } else { FAR })
        .collect();
    if !dist.contains(&0.0) {
        return;
    }
    squared_distance(&mut dist, w, h);
    for (px, &d2) in p.rgba.as_chunks_mut::<4>().0.iter_mut().zip(&dist) {
        // Full inside the radius, fading to nothing over the next pixel.
        let band = (radius + 0.5 - d2.sqrt()).clamp(0.0, 1.0);
        if band > 0.0 {
            *px = under_white(*px, band);
        }
    }
}

/// `px` over white of opacity `band`.
fn under_white(px: [u8; 4], band: f32) -> [u8; 4] {
    let a = f32::from(px[3]) / 255.0;
    let out = a + band * (1.0 - a);
    let mut d = [0u8; 4];
    for k in 0..3 {
        let c = (f32::from(px[k]) * a + 255.0 * band * (1.0 - a)) / out;
        d[k] = c.round().clamp(0.0, 255.0) as u8;
    }
    d[3] = (out * 255.0).round().clamp(0.0, 255.0) as u8;
    d
}

/// Turns `grid`, 0 at the kept pixels and [`FAR`] elsewhere, into the
/// squared distance to the nearest kept pixel.
fn squared_distance(grid: &mut [f32], w: usize, h: usize) {
    let mut line = vec![0.0f32; w.max(h)];
    let mut out = vec![0.0f32; w.max(h)];
    for x in 0..w {
        for y in 0..h {
            line[y] = grid[y * w + x];
        }
        transform_line(&line[..h], &mut out[..h]);
        for y in 0..h {
            grid[y * w + x] = out[y];
        }
    }
    for y in 0..h {
        let row = &mut grid[y * w..(y + 1) * w];
        line[..w].copy_from_slice(row);
        transform_line(&line[..w], row);
    }
}

/// One dimensional squared distance transform: the lower envelope of the
/// parabolas rooted at every sample.
fn transform_line(f: &[f32], out: &mut [f32]) {
    let n = f.len();
    let mut v = vec![0usize; n];
    let mut z = vec![0.0f32; n + 1];
    let mut k = 0;
    z[0] = f32::NEG_INFINITY;
    z[1] = f32::INFINITY;
    let reach = |q: usize, p: usize| {
        let (qf, pf) = (q as f32, p as f32);
        ((f[q] + qf * qf) - (f[p] + pf * pf)) / (2.0 * (qf - pf))
    };
    for q in 1..n {
        let mut s = reach(q, v[k]);
        while s <= z[k] {
            k -= 1;
            s = reach(q, v[k]);
        }
        k += 1;
        v[k] = q;
        z[k] = s;
        z[k + 1] = f32::INFINITY;
    }
    k = 0;
    for (q, o) in out.iter_mut().enumerate() {
        while z[k + 1] < q as f32 {
            k += 1;
        }
        let d = q as f32 - v[k] as f32;
        *o = d * d + f[v[k]];
    }
}
