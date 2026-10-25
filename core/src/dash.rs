//! Dashed and dotted line styles.
//!
//! A [`Dasher`] walks along a polyline and reports the parts of it that are
//! inked. It keeps its place between calls, so wet ink can feed it one
//! segment at a time and still line up with the stored stroke.

use crate::{outline, outline_pieces, tessellate, tessellate_step, Brush, InputSample, Rect};
use serde::{Deserialize, Serialize};

/// How the pen's line is broken up.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(u8)]
pub enum LineStyle {
    #[default]
    Solid = 0,
    Dashed = 1,
    Dotted = 2,
}

impl LineStyle {
    pub(crate) fn from_u8(v: u8) -> Option<Self> {
        match v {
            0 => Some(LineStyle::Solid),
            1 => Some(LineStyle::Dashed),
            2 => Some(LineStyle::Dotted),
            _ => None,
        }
    }
}

// The pattern is a multiple of the brush's full width and nothing else, so
// wet ink (view pixels) and stored ink (document units) break up the same
// way. Lengths are between the centres of the round caps: a dash looks
// about 5 widths long with a gap of about 2, and a dot is just the pen's disc.
const DASH_ON: f32 = 4.0;
const DASH_OFF: f32 = 3.0;
const DOT_PERIOD: f32 = 2.5;
/// Widths below this would make the pattern too fine to walk.
const MIN_WIDTH: f32 = 0.05;

/// One inked part of a segment. A dot has `from == to`. A `joined` piece
/// carries on from the end of the previous one, so its start needs no disc.
#[derive(Clone, Copy, Debug, PartialEq)]
pub(crate) struct Piece {
    pub from: InputSample,
    pub to: InputSample,
    pub joined: bool,
}

/// Walks arc length along a polyline, starting "on" at distance 0.
#[derive(Clone, Debug)]
pub(crate) struct Dasher {
    on: f32,
    period: f32,
    /// Distance into the current period. Reaches `period` at the end of
    /// one and wraps when the next distance is walked.
    phase: f32,
    /// The last piece ended at the previous point.
    open: bool,
}

impl Dasher {
    /// None for a solid line, or a brush too thin to pattern.
    pub fn new(line: LineStyle, width: f32) -> Option<Self> {
        if !width.is_finite() || width < MIN_WIDTH {
            return None;
        }
        let (on, period) = match line {
            LineStyle::Solid => return None,
            LineStyle::Dashed => (DASH_ON * width, (DASH_ON + DASH_OFF) * width),
            LineStyle::Dotted => (0.0, DOT_PERIOD * width),
        };
        Some(Self {
            on,
            period,
            phase: 0.0,
            open: false,
        })
    }

    /// Advances to `p`, reporting the inked parts since `prev`. The first
    /// sample of a stroke has no `prev` and starts the first dash or dot.
    pub fn step(
        &mut self,
        prev: Option<&InputSample>,
        p: &InputSample,
        emit: &mut impl FnMut(Piece),
    ) {
        match prev {
            None => {
                emit(Piece {
                    from: *p,
                    to: *p,
                    joined: false,
                });
                self.open = true;
            }
            Some(a) => self.walk(a, p, emit),
        }
    }

    fn walk(&mut self, a: &InputSample, b: &InputSample, emit: &mut impl FnMut(Piece)) {
        let len = (b.x - a.x).hypot(b.y - a.y);
        if !len.is_finite() || len <= 0.0 {
            return;
        }
        let at = |d: f32| lerp(a, b, d / len);
        let mut carried = std::mem::take(&mut self.open);
        let mut d = 0.0;
        loop {
            if self.phase >= self.period {
                self.phase -= self.period;
            }
            if self.on == 0.0 {
                if self.phase == 0.0 && !carried {
                    let p = at(d);
                    emit(Piece {
                        from: p,
                        to: p,
                        joined: false,
                    });
                }
            } else if self.phase < self.on {
                let end = (d + self.on - self.phase).min(len);
                emit(Piece {
                    from: at(d),
                    to: at(end),
                    joined: carried,
                });
                self.phase += end - d;
                d = end;
                if d >= len {
                    self.open = true;
                    return;
                }
            }
            carried = false;
            let to_wrap = self.period - self.phase;
            if d + to_wrap > len {
                self.phase += len - d;
                return;
            }
            d += to_wrap;
            self.phase = self.period;
            if d >= len {
                return;
            }
        }
    }
}

fn lerp(a: &InputSample, b: &InputSample, t: f32) -> InputSample {
    let m = |u: f32, v: f32| u + (v - u) * t;
    InputSample::new(
        m(a.x, b.x),
        m(a.y, b.y),
        m(a.pressure, b.pressure),
        m(a.time_ms, b.time_ms),
    )
}

/// Appends the triangles for one inked piece.
pub(crate) fn tessellate_piece(piece: &Piece, brush: &Brush, out: &mut Vec<f32>) {
    if !piece.joined {
        tessellate_step(None, &piece.from, brush, out);
    }
    if piece.from != piece.to {
        tessellate_step(Some(&piece.from), &piece.to, brush, out);
    }
}

/// [`tessellate`] for a line style. A solid line is tessellated as is.
pub fn tessellate_styled(
    points: &[InputSample],
    brush: &Brush,
    line: LineStyle,
    out: &mut Vec<f32>,
) {
    let Some(mut dasher) = Dasher::new(line, brush.max_width) else {
        return tessellate(points, brush, out);
    };
    let mut prev = None;
    for p in points {
        dasher.step(prev, p, &mut |piece| tessellate_piece(&piece, brush, out));
        prev = Some(p);
    }
}

/// Splits a polyline into the runs that are inked, each a polyline of its
/// own. A dot is a run of one point. `width` is the brush's full width. A
/// solid line is one run.
pub fn dash_runs(points: &[InputSample], line: LineStyle, width: f32) -> Vec<Vec<InputSample>> {
    let Some(mut dasher) = Dasher::new(line, width) else {
        return if points.is_empty() {
            Vec::new()
        } else {
            vec![points.to_vec()]
        };
    };
    let mut runs: Vec<Vec<InputSample>> = Vec::new();
    let mut prev = None;
    for p in points {
        dasher.step(prev, p, &mut |piece| match runs.last_mut() {
            Some(run) if piece.joined => run.push(piece.to),
            _ if piece.from == piece.to => runs.push(vec![piece.from]),
            _ => runs.push(vec![piece.from, piece.to]),
        });
        prev = Some(p);
    }
    runs
}

/// [`outline`] for a line style: each run is outlined on its own and the
/// bounds are joined. A solid line takes the plain path.
pub fn outline_styled(
    points: &[InputSample],
    brush: &Brush,
    line: LineStyle,
    tolerance: f32,
    out: &mut Vec<f32>,
) -> Rect {
    if line == LineStyle::Solid {
        return outline(points, brush, tolerance, out);
    }
    dash_runs(points, line, brush.max_width)
        .iter()
        .fold(Rect::EMPTY, |bounds, run| {
            bounds.union(&outline(run, brush, tolerance, out))
        })
}

/// [`outline_pieces`] for a line style, in the same way as [`outline_styled`].
pub fn outline_pieces_styled(
    points: &[InputSample],
    brush: &Brush,
    line: LineStyle,
    tolerance: f32,
    out: &mut Vec<f32>,
) -> Rect {
    if line == LineStyle::Solid {
        return outline_pieces(points, brush, tolerance, out);
    }
    dash_runs(points, line, brush.max_width)
        .iter()
        .fold(Rect::EMPTY, |bounds, run| {
            bounds.union(&outline_pieces(run, brush, tolerance, out))
        })
}

#[cfg(test)]
mod tests;
