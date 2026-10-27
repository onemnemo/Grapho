//! Self-intersection tests for the contours behind `outline`.

use super::cross;

/// Whether a closed polygon, packed as `[x0, y0, x1, y1, ...]`, has two
/// edges that cross or touch, other than neighbours at their shared vertex.
/// Edges are bucketed into horizontal bands so only nearby ones are tested.
pub(super) fn crosses_itself(flat: &[f32]) -> bool {
    let v = flat.as_chunks::<2>().0;
    let n = v.len();
    if n < 4 {
        return false;
    }
    let (mut lo, mut hi) = (f32::INFINITY, f32::NEG_INFINITY);
    for p in v {
        lo = lo.min(p[1]);
        hi = hi.max(p[1]);
    }
    let bands = n.clamp(1, 4096);
    let height = ((hi - lo) / bands as f32).max(f32::MIN_POSITIVE);
    let band = |y: f32| (((y - lo) / height) as usize).min(bands - 1);
    let mut buckets: Vec<Vec<usize>> = vec![Vec::new(); bands];
    for i in 0..n {
        let (a, b) = (v[i], v[(i + 1) % n]);
        let (from, to) = (band(a[1].min(b[1])), band(a[1].max(b[1])));
        for bucket in &mut buckets[from..=to] {
            bucket.push(i);
        }
    }
    let adjacent = |i: usize, j: usize| {
        j == i + 1 || i == j + 1 || (i == 0 && j == n - 1) || (j == 0 && i == n - 1)
    };
    for bucket in &buckets {
        for (x, &i) in bucket.iter().enumerate() {
            for &j in &bucket[x + 1..] {
                if !adjacent(i, j) && segments_meet(v[i], v[(i + 1) % n], v[j], v[(j + 1) % n]) {
                    return true;
                }
            }
        }
    }
    false
}

/// Whether segments `ab` and `cd` cross or touch.
fn segments_meet(a: [f32; 2], b: [f32; 2], c: [f32; 2], d: [f32; 2]) -> bool {
    let sub = |p: [f32; 2], q: [f32; 2]| [p[0] - q[0], p[1] - q[1]];
    let side = |p: [f32; 2], q: [f32; 2], r: [f32; 2]| cross(sub(q, p), sub(r, p));
    if a[0].max(b[0]) < c[0].min(d[0])
        || c[0].max(d[0]) < a[0].min(b[0])
        || a[1].max(b[1]) < c[1].min(d[1])
        || c[1].max(d[1]) < a[1].min(b[1])
    {
        return false;
    }
    let (d1, d2) = (side(c, d, a), side(c, d, b));
    let (d3, d4) = (side(a, b, c), side(a, b, d));
    (d1 * d2 <= 0.0) && (d3 * d4 <= 0.0)
}
