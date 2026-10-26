//! Tracing a picture into centre lines.

use super::blobs;
use super::colours::{split, Layer};
use super::lines::{close_gaps, finish};
use super::mask::{build, distance, enlarge, enlarge_factor, reduce, MIN_SPECK};
use super::paths::{from_graph, join, prune};
use super::skeleton::walk;
use super::thin::skeleton;
use super::{Pixels, TracedLine};

/// A region this many times deeper than the typical line, plus most of a
/// pixel of the picture, is a filled shape and not a line.
const BLOB_DEPTH_PER_LINE: f32 = 1.6;
const BLOB_DEPTH_BASE: f32 = 0.8;
/// Never less deep than this, in pixels of the picture, so a drawing of
/// fine lines only has no blobs.
const MIN_BLOB_DEPTH: f32 = 2.5;
/// Fewest pixels, at the picture's own size, for a bit of one colour to be
/// traced in that colour.
const MIN_PART: usize = 12;

/// Traces the drawing in a picture into centre lines, in pixel coordinates
/// of `p` (pixel centres at +0.5), one ink colour at a time: the lines of
/// a colour, then its filled shapes.
///
/// Transparent pixels are paper. In a picture that is not mostly
/// transparent the paper colour is estimated region by region, as for
/// background removal, so light on dark and dark on light drawings both
/// work. Large pictures are reduced first and pictures with thin lines
/// enlarged, and the lines scaled back. Each line has a width, and the
/// colour of its pen: greys come out as plain black, real colours are
/// kept, and each colour is traced on its own so pens that overlap stay
/// apart. Regions much thicker than the drawing's lines and not long and
/// thin (arrowheads, dots) come back as filled outlines. An empty vector
/// means no drawing was found, or the buffer does not match the size.
pub fn trace(p: &Pixels) -> Vec<TracedLine> {
    let (small, factor) = reduce(p);
    let Some(mut mask) = build(&small, MIN_SPECK) else {
        return Vec::new();
    };
    let up = if factor == 1 {
        enlarge_factor(&mask)
    } else {
        1
    };
    let scale = if up > 1 {
        mask = enlarge(&mask, up, MIN_SPECK * up * up);
        1.0 / up as f32
    } else {
        factor as f32
    };
    let layers = split(&mask, &small, up, MIN_PART * up * up);
    drop(mask);
    let thinned = side_by_side(layers, |layer| {
        let dist = distance(&layer.mask);
        let thin = skeleton(&layer.mask);
        (layer, dist, thin)
    });
    // How thick a blob is, by the lines of the whole drawing: an arrowhead
    // in a colour of its own is still much thicker than the lines.
    let depth = typical_depth(thinned.iter().map(|(_, d, t)| (t.as_slice(), d.as_slice())));
    let px = up as f32;
    let core = (depth * BLOB_DEPTH_PER_LINE + BLOB_DEPTH_BASE * px).max(MIN_BLOB_DEPTH * px);
    side_by_side(thinned, |(layer, dist, thin)| {
        trace_layer(layer, dist, thin, core, scale)
    })
    .into_iter()
    .flatten()
    .collect()
}

/// `work` done on every item, each on its own thread.
fn side_by_side<T: Send, R: Send>(items: Vec<T>, work: impl Fn(T) -> R + Sync) -> Vec<R> {
    let work = &work;
    std::thread::scope(|s| {
        let running: Vec<_> = items
            .into_iter()
            .map(|item| s.spawn(move || work(item)))
            .collect();
        running
            .into_iter()
            .map(|t| t.join().unwrap_or_else(|e| std::panic::resume_unwind(e)))
            .collect()
    })
}

/// Traces the lines of one colour, given its distance field and skeleton.
/// Regions deeper than `core_depth` are filled shapes.
fn trace_layer(
    layer: Layer,
    mut dist: Vec<f32>,
    mut thin: Vec<u8>,
    core_depth: f32,
    scale: f32,
) -> Vec<TracedLine> {
    let Layer {
        mut mask,
        origin,
        color,
    } = layer;
    let blobs = blobs::take(&mut mask, &dist, core_depth);
    if !blobs.is_empty() {
        dist = distance(&mask);
        thin = skeleton(&mask);
    }
    let mut graph = walk(&mask, &thin);
    let mut paths = from_graph(&mask, &mut graph);
    prune(&mut paths, &dist);
    let mut paths = join(paths, graph.joints.len());
    let mut filled = vec![0u8; mask.ink.len()];
    for &i in blobs.iter().flat_map(|b| &b.pixels) {
        filled[i] = 1;
    }
    close_gaps(&mut paths, &mask, &thin, &graph.joint_of, &filled, &dist);
    let mut lines = finish(&paths, &graph.dots, &blobs, &mask, &dist, color, scale);
    let [dx, dy] = origin.map(|v| v * scale);
    for p in lines.iter_mut().flat_map(|l| &mut l.points) {
        p[0] += dx;
        p[1] += dy;
    }
    lines
}

/// The median distance to the paper along the skeletons: half a line's
/// width as the drawing's lines go.
fn typical_depth<'a>(layers: impl Iterator<Item = (&'a [u8], &'a [f32])>) -> f32 {
    let mut d: Vec<f32> = layers
        .flat_map(|(thin, dist)| {
            thin.iter()
                .zip(dist)
                .filter(|(&t, _)| t == 1)
                .map(|(_, &d)| d)
        })
        .collect();
    if d.is_empty() {
        return 1.5;
    }
    let mid = d.len() / 2;
    *d.select_nth_unstable_by(mid, f32::total_cmp).1
}
