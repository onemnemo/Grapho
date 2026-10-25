//! Preprocessed stroke shared by every recogniser.

use super::geom::{dist, lerp, P};
use crate::InputSample;

/// Strokes with a bounding-box diagonal under this are dots or ticks, never shapes.
const MIN_DIAGONAL_PX: f32 = 24.0;
const MIN_STEP_PX: f32 = 1e-3;
const RESAMPLE_COUNT: usize = 96;
/// A stroke whose ends are closer than this fraction of its diagonal counts as closed.
const CLOSED_GAP: f32 = 0.25;

/// A stroke resampled uniformly by arc length, with the measurements the recognisers share.
#[derive(Clone, Debug)]
pub struct Stroke {
    pub(crate) points: Vec<P>,
    pub(crate) diagonal: f32,
    pub(crate) length: f32,
    pub(crate) closed: bool,
}

impl Stroke {
    /// Preprocesses raw samples. Returns None for fewer than three distinct
    /// points, a bounding box diagonal under 24 units, or any non-finite
    /// coordinate.
    pub fn new(points: &[InputSample]) -> Option<Stroke> {
        let mut raw: Vec<P> = Vec::with_capacity(points.len());
        for s in points {
            if !s.x.is_finite() || !s.y.is_finite() {
                return None;
            }
            let p = [s.x, s.y];
            if raw.last().is_none_or(|&q| dist(p, q) > MIN_STEP_PX) {
                raw.push(p);
            }
        }
        if raw.len() < 3 {
            return None;
        }
        let (mut min, mut max) = (raw[0], raw[0]);
        for p in &raw {
            for k in 0..2 {
                min[k] = min[k].min(p[k]);
                max[k] = max[k].max(p[k]);
            }
        }
        let diagonal = dist(min, max);
        if diagonal < MIN_DIAGONAL_PX {
            return None;
        }
        let (points, length) = resample(&raw, RESAMPLE_COUNT);
        let closed = dist(points[0], points[points.len() - 1]) < CLOSED_GAP * diagonal;
        Some(Stroke {
            points,
            diagonal,
            length,
            closed,
        })
    }
}

pub(crate) fn resample(raw: &[P], count: usize) -> (Vec<P>, f32) {
    let mut cumulative = Vec::with_capacity(raw.len());
    let mut total = 0.0;
    cumulative.push(0.0);
    for w in raw.windows(2) {
        total += dist(w[0], w[1]);
        cumulative.push(total);
    }
    let mut out = Vec::with_capacity(count);
    let mut seg = 0;
    for i in 0..count {
        let target = total * i as f32 / (count - 1) as f32;
        while seg + 2 < raw.len() && cumulative[seg + 1] < target {
            seg += 1;
        }
        let span = cumulative[seg + 1] - cumulative[seg];
        let t = if span > 0.0 {
            ((target - cumulative[seg]) / span).clamp(0.0, 1.0)
        } else {
            0.0
        };
        out.push(lerp(raw[seg], raw[seg + 1], t));
    }
    (out, total)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn samples(pts: &[(f32, f32)]) -> Vec<InputSample> {
        pts.iter()
            .enumerate()
            .map(|(i, &(x, y))| InputSample::new(x, y, 0.5, i as f32))
            .collect()
    }

    #[test]
    fn resamples_evenly_and_measures() {
        let s = Stroke::new(&samples(&[
            (0.0, 0.0),
            (10.0, 0.0),
            (100.0, 0.0),
            (100.0, 100.0),
        ]))
        .unwrap();
        assert_eq!(s.points.len(), RESAMPLE_COUNT);
        assert!((s.length - 200.0).abs() < 1e-3);
        assert!(!s.closed);
        let step = dist(s.points[10], s.points[11]);
        assert!((step - 200.0 / 95.0).abs() < 0.05);
    }

    #[test]
    fn detects_closed_strokes() {
        let s = Stroke::new(&samples(&[
            (0.0, 0.0),
            (100.0, 0.0),
            (100.0, 100.0),
            (0.0, 100.0),
            (3.0, 4.0),
        ]))
        .unwrap();
        assert!(s.closed);
    }

    #[test]
    fn rejects_degenerate_input() {
        assert!(Stroke::new(&[]).is_none());
        assert!(Stroke::new(&samples(&[(0.0, 0.0), (100.0, 0.0)])).is_none());
        assert!(Stroke::new(&samples(&[(0.0, 0.0), (5.0, 0.0), (5.0, 5.0)])).is_none());
        assert!(Stroke::new(&samples(&[(0.0, 0.0), (f32::NAN, 0.0), (100.0, 0.0)])).is_none());
        let same = vec![InputSample::new(10.0, 10.0, 0.5, 0.0); 50];
        assert!(Stroke::new(&same).is_none());
    }
}
