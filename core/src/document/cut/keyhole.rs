//! Joining the holes of a shape to its outer boundary.

use super::fill::area;

type Contour = Vec<[f32; 2]>;

/// One polygon that covers the same area as a shape given as an outer
/// boundary followed by holes. Each hole is joined to the boundary by a slit
/// of no width: the path goes out along the slit, once around the hole, and
/// back. Filling the result with either the non-zero or the even-odd rule
/// leaves the holes empty.
pub(super) fn bridge_holes(shape: Vec<Contour>) -> Contour {
    let mut parts = shape.into_iter();
    let Some(mut ring) = parts.next() else {
        return Vec::new();
    };
    // The outer boundary winds one way and the holes the other.
    wind(&mut ring, true);
    let mut holes: Vec<Contour> = parts.filter(|h| h.len() >= 3).collect();
    for h in &mut holes {
        wind(h, false);
    }
    for (n, hole) in holes.iter().enumerate() {
        let (ring_at, hole_at) = best_bridge(&ring, hole, &holes[n + 1..]);
        let join = ring[ring_at];
        let mut joined = Vec::with_capacity(ring.len() + hole.len() + 2);
        joined.extend_from_slice(&ring[..=ring_at]);
        joined.extend(hole[hole_at..].iter().chain(&hole[..hole_at]));
        joined.push(hole[hole_at]);
        joined.push(join);
        joined.extend_from_slice(&ring[ring_at + 1..]);
        ring = joined;
    }
    ring
}

fn wind(c: &mut Contour, positive: bool) {
    if (area(c) > 0.0) != positive {
        c.reverse();
    }
}

/// The shortest slit from a vertex of `ring` to a vertex of `hole` that
/// crosses no edge of the ring, the hole, or the `later` holes, as
/// `(ring vertex, hole vertex)`. The hole's extreme vertices are tried first,
/// which keeps the search small; all of them only if none of those is clear.
fn best_bridge(ring: &[[f32; 2]], hole: &[[f32; 2]], later: &[Contour]) -> (usize, usize) {
    let extreme = |key: fn(&[f32; 2]) -> f32| {
        (0..hole.len())
            .min_by(|&a, &b| key(&hole[a]).total_cmp(&key(&hole[b])))
            .unwrap_or(0)
    };
    let mut few = vec![
        extreme(|p| p[0]),
        extreme(|p| -p[0]),
        extreme(|p| p[1]),
        extreme(|p| -p[1]),
    ];
    few.sort_unstable();
    few.dedup();
    let all: Vec<usize> = (0..hole.len()).collect();
    let free = |c: &[[f32; 2]], o: [f32; 2], h: [f32; 2]| {
        (0..c.len()).all(|k| {
            let (a, b) = (c[k], c[(k + 1) % c.len()]);
            // An edge that starts or ends at a slit end cannot cross it.
            a == o || b == o || a == h || b == h || !crosses(o, h, a, b)
        })
    };
    let mut fallback = None;
    for tries in [&few, &all] {
        let mut pairs: Vec<(f32, usize, usize)> = Vec::new();
        for &j in tries {
            for (i, o) in ring.iter().enumerate() {
                pairs.push(((hole[j][0] - o[0]).hypot(hole[j][1] - o[1]), i, j));
            }
        }
        pairs.sort_by(|a, b| a.0.total_cmp(&b.0));
        fallback = fallback.or(pairs.first().map(|&(_, i, j)| (i, j)));
        let clear = pairs.iter().find(|&&(_, i, j)| {
            let (o, h) = (ring[i], hole[j]);
            free(ring, o, h) && free(hole, o, h) && later.iter().all(|c| free(c, o, h))
        });
        if let Some(&(_, i, j)) = clear {
            return (i, j);
        }
    }
    // With no clear pair (a degenerate shape) the shortest one is used.
    fallback.unwrap_or((0, 0))
}

/// Whether segments `p1 p2` and `q1 q2` touch or cross.
fn crosses(p1: [f32; 2], p2: [f32; 2], q1: [f32; 2], q2: [f32; 2]) -> bool {
    let side = |a: [f32; 2], b: [f32; 2], c: [f32; 2]| {
        (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
    };
    let (d1, d2) = (side(q1, q2, p1), side(q1, q2, p2));
    let (d3, d4) = (side(p1, p2, q1), side(p1, p2, q2));
    if d1 * d2 < 0.0 && d3 * d4 < 0.0 {
        return true;
    }
    let within = |a: [f32; 2], b: [f32; 2], c: [f32; 2]| {
        c[0] >= a[0].min(b[0])
            && c[0] <= a[0].max(b[0])
            && c[1] >= a[1].min(b[1])
            && c[1] <= a[1].max(b[1])
    };
    (d1 == 0.0 && within(q1, q2, p1))
        || (d2 == 0.0 && within(q1, q2, p2))
        || (d3 == 0.0 && within(p1, p2, q1))
        || (d4 == 0.0 && within(p1, p2, q2))
}
