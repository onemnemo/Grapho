//! Chains of skeleton pixels made into paths: spurs cut off, and chains
//! met at a junction that nothing else uses joined into one.

use super::mask::Mask;
use super::skeleton::Graph;

/// A run of the drawing, as points and the skeleton pixels they came from.
pub(super) struct Path {
    /// Pixel coordinates, centres at +0.5. A closed path does not repeat
    /// its first point.
    pub pts: Vec<[f32; 2]>,
    pub px: Vec<usize>,
    /// The junctions at either end; `None` for a free end.
    pub start: Option<u32>,
    pub end: Option<u32>,
    pub closed: bool,
}

impl Path {
    fn reverse(&mut self) {
        self.pts.reverse();
        self.px.reverse();
        std::mem::swap(&mut self.start, &mut self.end);
    }

    /// Line width in pixels from the distance field, at least 1. A skeleton
    /// pixel is as far from the paper as half the width less half a pixel
    /// or less, depending on whether the width is odd, so this is within
    /// half a pixel either way.
    pub fn width(&self, dist: &[f32]) -> f32 {
        let mut w: Vec<f32> = self.px.iter().map(|&i| 2.0 * dist[i] - 0.5).collect();
        if w.is_empty() {
            return 1.0;
        }
        let mid = w.len() / 2;
        let (_, &mut m, _) = w.select_nth_unstable_by(mid, f32::total_cmp);
        m.max(1.0)
    }
}

/// The paths of a graph, ends at junctions placed at the junction centre
/// so lines that meet there meet exactly.
pub(super) fn from_graph(m: &Mask, graph: &mut Graph) -> Vec<Path> {
    let centre = |i: usize| {
        let (x, y) = m.xy(i);
        [x as f32 + 0.5, y as f32 + 0.5]
    };
    graph
        .chains
        .drain(..)
        .map(|c| {
            let mut pts = Vec::with_capacity(c.pixels.len() + 2);
            if let Some(j) = c.start {
                pts.push(graph.joints[j as usize]);
            }
            pts.extend(c.pixels.iter().map(|&i| centre(i)));
            if let Some(j) = c.end {
                pts.push(graph.joints[j as usize]);
            }
            Path {
                pts,
                px: c.pixels,
                start: c.start,
                end: c.end,
                closed: c.closed,
            }
        })
        .collect()
}

/// Drops the short stubs that rough edges leave beside a line (a stub is a
/// path with one free end and one end at a junction, shorter than about
/// the line is wide), and loops that go nowhere around a junction.
pub(super) fn prune(paths: &mut Vec<Path>, dist: &[f32]) {
    paths.retain(|p| {
        if p.closed {
            return true;
        }
        let long = p.px.len() as f32;
        match (p.start, p.end) {
            (Some(a), Some(b)) if a == b => long > 2.0,
            // A spur must stick out of the line it leaves by more than
            // that line is wide, or it is a bump on its edge.
            (Some(_), None) => long >= (1.5 * p.width(dist) + 1.0).max(3.0 * dist[p.px[0]]),
            (None, Some(_)) => {
                long >= (1.5 * p.width(dist) + 1.0).max(3.0 * dist[p.px[p.px.len() - 1]])
            }
            _ => true,
        }
    });
}

/// Joins paths that meet at a junction where nothing else meets, so a
/// junction that lost its stubs does not split a line in two.
pub(super) fn join(paths: Vec<Path>, joint_count: usize) -> Vec<Path> {
    // Per junction: which path ends touch it, as (path, at its end).
    let mut at: Vec<Vec<(usize, bool)>> = vec![Vec::new(); joint_count];
    for (i, p) in paths.iter().enumerate() {
        if p.closed {
            continue;
        }
        if let Some(c) = p.start {
            at[c as usize].push((i, false));
        }
        if let Some(c) = p.end {
            at[c as usize].push((i, true));
        }
    }
    let through = |c: Option<u32>| c.is_some_and(|c| at[c as usize].len() == 2);

    let mut slots: Vec<Option<Path>> = paths.into_iter().map(Some).collect();
    let mut out = Vec::with_capacity(slots.len());
    // First the runs that have an end to start from, then what is left:
    // rings of paths that all pass through.
    for pass in 0..2 {
        for i in 0..slots.len() {
            let Some(mut p) = slots[i].take() else {
                continue;
            };
            if p.closed {
                out.push(p);
                continue;
            }
            let (s, e) = (through(p.start), through(p.end));
            if pass == 0 && s && e {
                slots[i] = Some(p);
                continue;
            }
            let mut here = (i, true);
            if s && !e {
                p.reverse();
                here = (i, false);
            }
            while let Some(c) = p.end.filter(|&c| through(Some(c))) {
                let Some(&(next, next_end)) = at[c as usize].iter().find(|&&t| t != here) else {
                    break;
                };
                if next == i {
                    p.closed = true;
                    p.pts.pop();
                    p.start = None;
                    p.end = None;
                    break;
                }
                let Some(mut q) = slots[next].take() else {
                    break;
                };
                if next_end {
                    q.reverse();
                }
                p.pts.extend_from_slice(&q.pts[1..]);
                p.px.extend_from_slice(&q.px);
                p.end = q.end;
                here = (next, !next_end);
            }
            out.push(p);
        }
    }
    out
}
