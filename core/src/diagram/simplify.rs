//! Smoothing and thinning out of pixel paths.

/// Averages each point with its neighbours (weights 1, 2, 3, 2, 1) to take
/// the staircase out of a pixel path. The ends of an open path stay put so
/// lines keep meeting where they met. A closed path is given without its
/// repeated last point and wraps around.
pub(super) fn smooth(pts: &[[f32; 2]], closed: bool) -> Vec<[f32; 2]> {
    const W: [f32; 5] = [1.0, 2.0, 3.0, 2.0, 1.0];
    let n = pts.len();
    if n < 3 {
        return pts.to_vec();
    }
    let mut out = Vec::with_capacity(n);
    for i in 0..n {
        if !closed && (i == 0 || i == n - 1) {
            out.push(pts[i]);
            continue;
        }
        let (mut x, mut y, mut total) = (0.0, 0.0, 0.0);
        for (k, w) in W.iter().enumerate() {
            let j = i as isize + k as isize - 2;
            let j = if closed {
                j.rem_euclid(n as isize) as usize
            } else if (0..n as isize).contains(&j) {
                j as usize
            } else {
                continue;
            };
            x += pts[j][0] * w;
            y += pts[j][1] * w;
            total += w;
        }
        out.push([x / total, y / total]);
    }
    out
}

fn gap(p: [f32; 2], a: [f32; 2], b: [f32; 2]) -> f32 {
    let (dx, dy) = (b[0] - a[0], b[1] - a[1]);
    let len2 = dx * dx + dy * dy;
    let t = if len2 > 0.0 {
        (((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / len2).clamp(0.0, 1.0)
    } else {
        0.0
    };
    let (qx, qy) = (a[0] + t * dx - p[0], a[1] + t * dy - p[1]);
    (qx * qx + qy * qy).sqrt()
}

/// Ramer-Douglas-Peucker: the indexes of the fewest points that keep every
/// dropped point within `tolerance` of the result. Keeps both ends.
fn rdp(pts: &[[f32; 2]], tolerance: f32) -> Vec<usize> {
    let n = pts.len();
    if n < 3 {
        return (0..n).collect();
    }
    let mut keep = vec![false; n];
    keep[0] = true;
    keep[n - 1] = true;
    let mut spans = vec![(0, n - 1)];
    while let Some((a, b)) = spans.pop() {
        let far = (a + 1..b)
            .map(|i| (i, gap(pts[i], pts[a], pts[b])))
            .max_by(|x, y| x.1.total_cmp(&y.1));
        if let Some((i, d)) = far {
            if d > tolerance {
                keep[i] = true;
                spans.push((a, i));
                spans.push((i, b));
            }
        }
    }
    (0..n).filter(|&i| keep[i]).collect()
}

fn direction(from: [f32; 2], to: [f32; 2]) -> [f32; 2] {
    let (dx, dy) = (to[0] - from[0], to[1] - from[1]);
    let len = dx.hypot(dy).max(1e-6);
    [dx / len, dy / len]
}

/// How far a straight run may bow to one side, on average, as a share of
/// how far it may wander. Noise along a straight line wanders to both
/// sides; a gentle curve bows to one, and is kept a curve.
const BOW_PER_WANDER: f32 = 0.25;

/// The average signed distance of the points from the chord between the
/// first and the last, as a size.
fn bow(pts: &[[f32; 2]]) -> f32 {
    let (a, b) = (pts[0], pts[pts.len() - 1]);
    let [ux, uy] = direction(a, b);
    let sum: f32 = pts
        .iter()
        .map(|p| (p[0] - a[0]) * uy - (p[1] - a[1]) * ux)
        .sum();
    (sum / pts.len() as f32).abs()
}

/// How far the path may wander off a straight run, how long a run must be
/// to count as one (both in pixels), and how far its ends may turn from
/// each other (as a cosine).
pub(super) struct Straightness {
    pub wander: f32,
    pub min_run: f32,
    pub min_cos: f32,
}

/// Simplifies an open path to `tolerance`, but lets long runs that are
/// straight overall, with their ends pointing the same way, wander up to
/// `straight.wander` to a single segment. That irons out the jogs a noisy
/// edge leaves along a wire without cutting corners off curves.
pub(super) fn simplify(pts: &[[f32; 2]], tolerance: f32, straight: &Straightness) -> Vec<[f32; 2]> {
    let n = pts.len();
    if n < 3 {
        return pts.to_vec();
    }
    let coarse = rdp(pts, straight.wander.max(tolerance));
    let mut keep = vec![0usize];
    for span in coarse.windows(2) {
        let (a, b) = (span[0], span[1]);
        let probe = 5.min((b - a) / 2).max(1);
        let straight_run = {
            let chord = (pts[b][0] - pts[a][0]).hypot(pts[b][1] - pts[a][1]);
            let (d0, d1) = (
                direction(pts[a], pts[a + probe]),
                direction(pts[b - probe], pts[b]),
            );
            chord >= straight.min_run
                && d0[0] * d1[0] + d0[1] * d1[1] >= straight.min_cos
                && bow(&pts[a..=b]) <= tolerance.max(BOW_PER_WANDER * straight.wander)
        };
        if !straight_run {
            keep.extend(
                rdp(&pts[a..=b], tolerance)
                    .into_iter()
                    .skip(1)
                    .map(|i| i + a),
            );
        } else {
            keep.push(b);
        }
    }
    keep.into_iter().map(|i| pts[i]).collect()
}

/// [`simplify`] for a closed path given without its repeated last point.
/// The result repeats its first point last. The path is cut at its point
/// farthest from the start so no corner is lost to the cut.
pub(super) fn simplify_closed(
    pts: &[[f32; 2]],
    tolerance: f32,
    straight: &Straightness,
) -> Vec<[f32; 2]> {
    let n = pts.len();
    if n < 4 {
        let mut out = pts.to_vec();
        out.push(pts[0]);
        return out;
    }
    let far = (1..n)
        .max_by(|&a, &b| {
            let d = |i: usize| {
                let (dx, dy) = (pts[i][0] - pts[0][0], pts[i][1] - pts[0][1]);
                dx * dx + dy * dy
            };
            d(a).total_cmp(&d(b))
        })
        .unwrap_or(1);
    let mut first = simplify(&pts[..=far], tolerance, straight);
    let mut back: Vec<[f32; 2]> = pts[far..].to_vec();
    back.push(pts[0]);
    let back = simplify(&back, tolerance, straight);
    first.extend_from_slice(&back[1..]);
    first
}
