//! Thinning of the ink mask to a skeleton one pixel wide (Zhang and Suen),
//! then the removal of the corner pixels that make false junctions.

use super::mask::Mask;

const ALIVE: u8 = 1;
/// Marks a pixel that is in the list of pixels still worth looking at.
const QUEUED: u8 = 2;

/// Whether the pixel at `i` can go in this sub iteration without breaking
/// the line it is part of. `v` holds the eight neighbours clockwise from
/// north.
fn removable(v: [u8; 8], second: bool) -> bool {
    let b: u8 = v.iter().sum();
    if !(2..=6).contains(&b) {
        return false;
    }
    let rises = (0..8).filter(|&k| v[k] == 0 && v[(k + 1) % 8] == 1).count();
    if rises != 1 {
        return false;
    }
    let [n, _, e, _, s, _, w, _] = v;
    if second {
        n * e * w == 0 && n * s * w == 0
    } else {
        n * e * s == 0 && e * s * w == 0
    }
}

fn around(grid: &[u8], ring: &[isize; 8], i: usize) -> [u8; 8] {
    ring.map(|d| grid[i.wrapping_add_signed(d)] & ALIVE)
}

/// The skeleton of the mask, same layout, 1 where the skeleton is.
pub(super) fn skeleton(m: &Mask) -> Vec<u8> {
    let ring = m.ring();
    let mut g = m.ink.clone();
    // Only pixels next to a change can change, so the sweeps keep a list
    // instead of scanning the whole picture every time.
    let mut active: Vec<usize> = Vec::new();
    for (i, v) in g.iter_mut().enumerate() {
        if *v == ALIVE && around(&m.ink, &ring, i).contains(&0) {
            *v |= QUEUED;
            active.push(i);
        }
    }
    let mut doomed: Vec<usize> = Vec::new();
    let mut idle = 0;
    let mut second = false;
    while idle < 2 && !active.is_empty() {
        doomed.clear();
        doomed.extend(
            active
                .iter()
                .copied()
                .filter(|&i| removable(around(&g, &ring, i), second)),
        );
        for &i in &doomed {
            g[i] = 0;
        }
        for &i in &doomed {
            for d in ring {
                let n = i.wrapping_add_signed(d);
                if g[n] == ALIVE {
                    g[n] |= QUEUED;
                    active.push(n);
                }
            }
        }
        active.retain(|&i| g[i] & ALIVE != 0);
        idle = if doomed.is_empty() { idle + 1 } else { 0 };
        second = !second;
    }
    for v in &mut g {
        *v &= ALIVE;
    }
    drop_corners(&mut g, m, &ring);
    g
}

/// Removes pixels the skeleton does not need: those whose neighbours all
/// touch each other, so the line stays connected without them. That takes
/// out the corners of a staircase and the runs where the skeleton came out
/// two pixels thick, which would otherwise read as long junctions. Ends,
/// lines and true junctions, whose neighbours fall apart without the
/// pixel, stay.
fn drop_corners(g: &mut [u8], m: &Mask, ring: &[isize; 8]) {
    loop {
        let mut changed = false;
        for y in 0..m.h {
            for x in 0..m.w {
                let i = m.at(x, y);
                if g[i] == ALIVE && redundant(around(g, ring, i)) {
                    g[i] = 0;
                    changed = true;
                }
            }
        }
        if !changed {
            break;
        }
    }
}

/// Whether the neighbours `v` (clockwise from north) are two or more and
/// all one 8-connected group among themselves.
fn redundant(v: [u8; 8]) -> bool {
    // Positions of the ring cells in the 3 x 3 block.
    const AT: [(i8, i8); 8] = [
        (0, -1),
        (1, -1),
        (1, 0),
        (1, 1),
        (0, 1),
        (-1, 1),
        (-1, 0),
        (-1, -1),
    ];
    let total: u8 = v.iter().sum();
    if total < 2 {
        return false;
    }
    let first = (0..8).find(|&k| v[k] == 1).unwrap_or(0);
    let mut reached = 1u8 << first;
    loop {
        let before = reached;
        for (a, pa) in AT.iter().enumerate() {
            if reached & (1 << a) == 0 {
                continue;
            }
            for (b, pb) in AT.iter().enumerate() {
                if v[b] == 1 && (pa.0 - pb.0).abs() <= 1 && (pa.1 - pb.1).abs() <= 1 {
                    reached |= 1 << b;
                }
            }
        }
        if reached == before {
            break;
        }
    }
    reached.count_ones() == u32::from(total)
}
