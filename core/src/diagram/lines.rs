//! From paths to finished lines: gaps closed, width and colour measured,
//! the path smoothed and thinned out.

use super::blobs::Blob;
use super::mask::Mask;
use super::paths::Path;
use super::simplify::{simplify, simplify_closed, smooth, Straightness};
use super::skeleton::NO_JOINT;
use super::TracedLine;

/// Dropped points stay within this many pixels of the line (at the scale
/// the picture was traced at).
const TOLERANCE: f32 = 0.6;
/// Long straight runs may wander this many widths off their chord, must
/// be this many widths long, and their ends may turn up to about 8
/// degrees from each other.
const WANDER_PER_WIDTH: f32 = 0.5;
const RUN_PER_WIDTH: f32 = 10.0;
const RUN_MIN_COS: f32 = 0.99;
/// Gap allowed between a line end and the line it nearly touches, in
/// widths of the line plus pixels.
const GAP_PER_WIDTH: f32 = 1.5;
const GAP_BASE: f32 = 2.0;
/// Closest the direction to the target may be to the direction of the
/// line (cosine of about 25 degrees).
const MIN_COS: f32 = 0.9;
/// Never close toward a line this many times thinner or thicker.
const MAX_WIDTH_RATIO: f32 = 1.8;
/// How many points back the direction of a line end is read from.
const DIRECTION_POINTS: usize = 8;
const NO_OWNER: u32 = u32::MAX;

/// Extends line ends that stop just short of another line until they
/// touch it, so a small break in the drawing does not leave a gap.
pub(super) fn close_gaps(
    paths: &mut [Path],
    m: &Mask,
    skeleton: &[u8],
    joint_of: &[u32],
    blob: &[u8],
    dist: &[f32],
) {
    let mut owner = vec![NO_OWNER; skeleton.len()];
    let mut place = vec![0u32; skeleton.len()];
    for (n, p) in paths.iter().enumerate() {
        for (k, &i) in p.px.iter().enumerate() {
            owner[i] = n as u32;
            place[i] = k as u32;
        }
    }
    let ring = m.stride as isize;
    let widths: Vec<f32> = paths.iter().map(|p| p.width(dist)).collect();
    let widest = widths.iter().copied().fold(1.0, f32::max);
    let mut joins: Vec<(usize, bool, [f32; 2])> = Vec::new();
    for (n, p) in paths.iter().enumerate() {
        if p.closed || p.px.is_empty() {
            continue;
        }
        let w = widths[n];
        // The skeleton stops about half a width short of where the line
        // ends and the target's skeleton is half its width inside it, so
        // the gap between the two edges is the distance less both halves.
        let allowed = GAP_PER_WIDTH * w + GAP_BASE;
        let r = (allowed + 0.5 * (w + widest)).ceil() as isize;
        for at_start in [true, false] {
            if (if at_start { p.start } else { p.end }).is_some() {
                continue;
            }
            let e_pos = if at_start { 0 } else { p.px.len() - 1 };
            let e = p.px[e_pos];
            let tip = if at_start { 0 } else { p.pts.len() - 1 };
            let reach_back = DIRECTION_POINTS.min(p.pts.len() - 1);
            let back = if at_start {
                reach_back
            } else {
                tip - reach_back
            };
            let dir = [
                p.pts[tip][0] - p.pts[back][0],
                p.pts[tip][1] - p.pts[back][1],
            ];
            let len = dir[0].hypot(dir[1]);
            if len < 1e-3 {
                continue;
            }
            let mut best: Option<(f32, usize)> = None;
            let (ex, ey) = m.xy(e);
            let (ex, ey) = (ex as isize, ey as isize);
            let (right, below) = (m.w as isize - 1 - ex, m.h as isize - 1 - ey);
            for dy in (-r).max(-ey)..=r.min(below) {
                for dx in (-r).max(-ex)..=r.min(right) {
                    let q = e.wrapping_add_signed(dy * ring + dx);
                    if skeleton[q] != 1 && blob[q] != 1 {
                        continue;
                    }
                    let own = owner[q] == n as u32;
                    let far = own && (place[q] as i64 - e_pos as i64).unsigned_abs() > 4 * r as u64;
                    let target = if blob[q] == 1 || joint_of[q] != NO_JOINT {
                        w
                    } else if owner[q] != NO_OWNER && (!own || far) {
                        widths[owner[q] as usize]
                    } else {
                        continue;
                    };
                    let d = (dx as f32).hypot(dy as f32);
                    let ahead = dir[0] * dx as f32 + dir[1] * dy as f32;
                    let similar = target <= MAX_WIDTH_RATIO * w && w <= MAX_WIDTH_RATIO * target;
                    if similar
                        && d - 0.5 * (w + target) <= allowed
                        && ahead >= MIN_COS * d * len
                        && best.is_none_or(|b| d < b.0)
                    {
                        best = Some((d, q));
                    }
                }
            }
            if let Some((_, q)) = best {
                let (x, y) = m.xy(q);
                joins.push((n, at_start, [x as f32 + 0.5, y as f32 + 0.5]));
            }
        }
    }
    for (n, at_start, point) in joins {
        if at_start {
            paths[n].pts.insert(0, point);
        } else {
            paths[n].pts.push(point);
        }
    }
}

/// Measures, smooths and thins the paths, and scales them by `scale` back
/// to the pixels of the original picture. Everything is in `color`.
pub(super) fn finish(
    paths: &[Path],
    dots: &[usize],
    blobs: &[Blob],
    m: &Mask,
    dist: &[f32],
    color: u32,
    scale: f32,
) -> Vec<TracedLine> {
    let mut lines = Vec::with_capacity(paths.len() + dots.len());
    let up = |pts: Vec<[f32; 2]>| -> Vec<[f32; 2]> {
        pts.into_iter()
            .map(|p| [p[0] * scale, p[1] * scale])
            .collect()
    };
    for p in paths {
        let width = p.width(dist);
        let straight = Straightness {
            wander: TOLERANCE.max(WANDER_PER_WIDTH * width),
            min_run: RUN_PER_WIDTH * width,
            min_cos: RUN_MIN_COS,
        };
        let smoothed = smooth(&p.pts, p.closed);
        let pts = if p.closed {
            simplify_closed(&smoothed, TOLERANCE, &straight)
        } else {
            simplify(&smoothed, TOLERANCE, &straight)
        };
        let pts = match pts.as_slice() {
            [] => continue,
            [only] => vec![*only, [only[0] + 0.05, only[1]]],
            _ => pts,
        };
        lines.push(TracedLine {
            points: up(pts),
            width: width * scale,
            color,
            filled: false,
        });
    }
    for &i in dots {
        let (x, y) = m.xy(i);
        let at = [x as f32 + 0.5, y as f32 + 0.5];
        let one = Path {
            pts: vec![at],
            px: vec![i],
            start: None,
            end: None,
            closed: false,
        };
        lines.push(TracedLine {
            points: up(vec![at, [at[0] + 0.05, at[1]]]),
            width: one.width(dist) * scale,
            color,
            filled: false,
        });
    }
    for b in blobs {
        lines.push(TracedLine {
            points: up(b.outline.clone()),
            width: 0.0,
            color,
            filled: true,
        });
    }
    lines
}
