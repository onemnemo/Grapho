//! Walking a one pixel wide skeleton into chains of pixels between its
//! ends and junctions.

use super::mask::Mask;

/// Marks a pixel that is not part of a junction.
pub(super) const NO_JOINT: u32 = u32::MAX;

/// One run of the skeleton. Free ends include their end pixel in `pixels`;
/// ends at a junction do not include the junction's pixels, they name the
/// junction instead.
pub(super) struct Chain {
    pub pixels: Vec<usize>,
    pub start: Option<u32>,
    pub end: Option<u32>,
    pub closed: bool,
}

/// Everything found in a skeleton.
pub(super) struct Graph {
    pub chains: Vec<Chain>,
    /// Centre of each junction, in pixel coordinates.
    pub joints: Vec<[f32; 2]>,
    /// Which junction each skeleton pixel belongs to, or [`NO_JOINT`].
    pub joint_of: Vec<u32>,
    /// Pixels with no neighbour at all.
    pub dots: Vec<usize>,
}

/// Number of skeleton neighbours for every pixel.
fn degrees(g: &[u8], ring: &[isize; 8]) -> Vec<u8> {
    let mut deg = vec![0u8; g.len()];
    for (i, &v) in g.iter().enumerate() {
        if v == 1 {
            deg[i] = ring
                .iter()
                .map(|&d| g[i.wrapping_add_signed(d)])
                .sum::<u8>();
        }
    }
    deg
}

/// Groups touching junction pixels into junctions.
fn find_joints(m: &Mask, deg: &[u8], ring: &[isize; 8]) -> (Vec<[f32; 2]>, Vec<u32>) {
    let mut joint_of = vec![NO_JOINT; deg.len()];
    let mut joints = Vec::new();
    let mut stack = Vec::new();
    for start in 0..deg.len() {
        if deg[start] < 3 || joint_of[start] != NO_JOINT {
            continue;
        }
        let id = joints.len() as u32;
        joint_of[start] = id;
        stack.push(start);
        let (mut sx, mut sy, mut n) = (0.0f32, 0.0f32, 0.0f32);
        while let Some(i) = stack.pop() {
            let (x, y) = m.xy(i);
            sx += x as f32 + 0.5;
            sy += y as f32 + 0.5;
            n += 1.0;
            for d in ring {
                let j = i.wrapping_add_signed(*d);
                if deg[j] >= 3 && joint_of[j] == NO_JOINT {
                    joint_of[j] = id;
                    stack.push(j);
                }
            }
        }
        joints.push([sx / n, sy / n]);
    }
    (joints, joint_of)
}

/// Splits the skeleton `g` into chains at its ends and junctions.
pub(super) fn walk(m: &Mask, g: &[u8]) -> Graph {
    let ring = m.ring();
    let deg = degrees(g, &ring);
    let (joints, joint_of) = find_joints(m, &deg, &ring);
    let mut seen = vec![false; g.len()];
    let mut chains = Vec::new();
    let mut dots = Vec::new();

    let next_of = |at: usize, prev: usize, seen: &[bool]| {
        ring.iter()
            .map(|&d| at.wrapping_add_signed(d))
            .find(|&n| g[n] == 1 && n != prev && (deg[n] != 2 || !seen[n]))
    };

    for p in 0..g.len() {
        if g[p] != 1 {
            continue;
        }
        if deg[p] == 0 {
            dots.push(p);
            continue;
        }
        if deg[p] == 2 {
            continue;
        }
        for d in ring {
            let q = p.wrapping_add_signed(d);
            if g[q] != 1 {
                continue;
            }
            match (deg[p] == 1, deg[q]) {
                (_, 2) if seen[q] => continue,
                (false, 1) => continue,
                (false, k) if k >= 3 => continue,
                (true, 1) if q < p => continue,
                _ => {}
            }
            let mut chain = Chain {
                pixels: Vec::new(),
                start: None,
                end: None,
                closed: false,
            };
            if deg[p] == 1 {
                chain.pixels.push(p);
            } else {
                chain.start = Some(joint_of[p]);
            }
            let (mut prev, mut cur) = (p, q);
            loop {
                if deg[cur] != 2 {
                    if deg[cur] == 1 {
                        chain.pixels.push(cur);
                    } else {
                        chain.end = Some(joint_of[cur]);
                    }
                    break;
                }
                seen[cur] = true;
                chain.pixels.push(cur);
                match next_of(cur, prev, &seen) {
                    Some(n) => (prev, cur) = (cur, n),
                    None => break,
                }
            }
            chains.push(chain);
        }
    }

    // What is left over has no end or junction: closed loops.
    for s in 0..g.len() {
        if g[s] != 1 || deg[s] != 2 || seen[s] {
            continue;
        }
        let mut chain = Chain {
            pixels: Vec::new(),
            start: None,
            end: None,
            closed: true,
        };
        let (mut prev, mut cur) = (usize::MAX, s);
        loop {
            seen[cur] = true;
            chain.pixels.push(cur);
            match next_of(cur, prev, &seen) {
                Some(n) => (prev, cur) = (cur, n),
                None => break,
            }
        }
        chains.push(chain);
    }
    Graph {
        chains,
        joints,
        joint_of,
        dots,
    }
}
