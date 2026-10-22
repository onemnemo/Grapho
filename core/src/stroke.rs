use crate::dash::{tessellate_piece, Dasher};
use crate::guide::GuideEdge;
use crate::shape::{self, Shape, ShapeEdit};
use crate::{
    tessellate, tessellate_step, tessellate_styled, Brush, InputSample, LineStyle, Rect, Stabilizer,
};

/// Samples closer than this to the previous point, with similar pressure,
/// add nothing visible. Skipping them stops a pen held still from growing
/// the stroke without limit.
const MIN_STEP_PX: f32 = 0.4;
const MIN_PRESSURE_STEP: f32 = 0.02;
/// Extra margin around changed areas, for antialiasing.
const DIRTY_MARGIN_PX: f32 = 2.0;
/// The pen counts as held still while it stays this close to where it
/// stopped. Large enough for the hand's tremor, small enough that slow
/// drawing keeps moving the anchor.
const HOLD_SLOP_PX: f32 = 14.0;
/// Point spacing along the curved parts of a snapped shape.
const SNAP_SPACING_PX: f32 = 3.0;

/// What changed since the previous frame of a wet stroke.
#[derive(Clone, Debug, Default, PartialEq)]
pub struct Frame {
    /// Triangles for points added since the last frame. They never change,
    /// so the caller appends them to what it already has.
    pub new_committed: Vec<f32>,
    /// Triangles for the predicted tip. Replaced every frame.
    pub tail: Vec<f32>,
    /// Area that has to be redrawn: new ink, the new tip, and where the
    /// previous tip was.
    pub dirty: Rect,
}

/// A stroke redrawn as a shape by [`StrokeBuilder::snap`].
#[derive(Clone, Debug, PartialEq)]
pub struct Snapped {
    pub shape: Shape,
    /// The shape's outline as samples, ready to store as the stroke.
    pub points: Vec<InputSample>,
    /// Triangles for the whole shape.
    pub mesh: Vec<f32>,
}

/// Builds one stroke while the pen is down.
///
/// Real samples go through [`push`](Self::push) and become permanent.
/// Predicted samples are passed to [`frame`](Self::frame) and are drawn but
/// never stored, so a wrong prediction disappears on the next frame.
#[derive(Clone, Debug)]
pub struct StrokeBuilder {
    brush: Brush,
    line: LineStyle,
    /// None for a solid line.
    dasher: Option<Dasher>,
    stabilizer: Stabilizer,
    points: Vec<InputSample>,
    last_raw: Option<InputSample>,
    hold_anchor: Option<InputSample>,
    edit: Option<ShapeEdit>,
    guide: Option<GuideEdge>,
    pending: Vec<f32>,
    pending_dirty: Rect,
    prev_tail: Rect,
}

impl StrokeBuilder {
    pub fn new(brush: Brush, stabilizer_amount: f32, line: LineStyle) -> Self {
        Self {
            dasher: Dasher::new(line, brush.max_width),
            line,
            brush,
            stabilizer: Stabilizer::new(stabilizer_amount),
            points: Vec::new(),
            last_raw: None,
            hold_anchor: None,
            edit: None,
            guide: None,
            pending: Vec::new(),
            pending_dirty: Rect::EMPTY,
            prev_tail: Rect::EMPTY,
        }
    }

    /// Draws along `edge` from now on: every sample is projected onto it, the
    /// stabiliser is skipped, and [`snap`](Self::snap) finds no shape. Call it
    /// right after creating the builder. Samples already pushed stay as they
    /// are. None goes back to free drawing.
    pub fn guide(&mut self, edge: Option<GuideEdge>) {
        self.guide = edge;
    }

    /// True while the stroke follows a guide edge.
    pub fn is_guided(&self) -> bool {
        self.guide.is_some()
    }

    // The sample moved onto the guide edge, with the ink's own half width
    // between the edge and the line so the stroke sits against it.
    fn guided(&self, s: InputSample) -> InputSample {
        match self.guide {
            Some(edge) => {
                let half = self.brush.width_at(s.pressure) * 0.5;
                let (x, y) = edge.project(s.x, s.y, half);
                InputSample::new(x, y, s.pressure, s.time_ms)
            }
            None => s,
        }
    }

    pub fn push(&mut self, raw: InputSample) {
        let raw = self.guided(raw);
        self.last_raw = Some(raw);
        let still = self.hold_anchor.is_some_and(|a| {
            let (dx, dy) = (raw.x - a.x, raw.y - a.y);
            dx * dx + dy * dy <= HOLD_SLOP_PX * HOLD_SLOP_PX
        });
        if !still {
            self.hold_anchor = Some(raw);
        }
        let p = if self.guide.is_some() {
            raw
        } else {
            self.stabilizer.filter(raw)
        };
        if let Some(last) = self.points.last() {
            let (dx, dy) = (p.x - last.x, p.y - last.y);
            if dx * dx + dy * dy < MIN_STEP_PX * MIN_STEP_PX
                && (p.pressure - last.pressure).abs() < MIN_PRESSURE_STEP
            {
                return;
            }
        }
        match (self.guide, self.points.last().copied()) {
            (Some(edge), Some(prev)) => {
                let (a, b) = ((prev.x, prev.y), (p.x, p.y));
                let n = edge.segments(a, b);
                for i in 1..n {
                    let t = i as f32 / n as f32;
                    let (x, y) = edge.between(a, b, t);
                    let pressure = prev.pressure + (p.pressure - prev.pressure) * t;
                    let time = prev.time_ms + (p.time_ms - prev.time_ms) * t;
                    self.append(InputSample::new(x, y, pressure, time));
                }
                self.append(p);
            }
            _ => self.append(p),
        }
    }

    fn append(&mut self, p: InputSample) {
        let prev = self.points.last().copied();
        match &mut self.dasher {
            Some(d) => d.step(prev.as_ref(), &p, &mut |piece| {
                tessellate_piece(&piece, &self.brush, &mut self.pending)
            }),
            None => tessellate_step(prev.as_ref(), &p, &self.brush, &mut self.pending),
        }
        for q in prev.iter().chain(std::iter::once(&p)) {
            self.pending_dirty.add_circle(q.x, q.y, self.radius(q));
        }
        self.points.push(p);
    }

    fn radius(&self, p: &InputSample) -> f32 {
        self.brush.width_at(p.pressure) * 0.5 + DIRTY_MARGIN_PX
    }

    /// The stabilised points so far.
    pub fn points(&self) -> &[InputSample] {
        &self.points
    }

    /// How long the pen has stayed within a few pixels of one spot, at
    /// `now_ms` on the samples' clock. Zero before the first sample, and
    /// never negative.
    pub fn still_ms(&self, now_ms: f32) -> f32 {
        self.hold_anchor
            .map_or(0.0, |a| (now_ms - a.time_ms).max(0.0))
    }

    /// The stroke so far redrawn as the shape it looks like (a line, circle,
    /// rectangle, arrow and so on), with its points and full mesh. None if it
    /// does not look like any known shape. The stroke itself is left
    /// unchanged, and the shape is kept for [`reshape`](Self::reshape).
    ///
    /// `shaft` is a straight line drawn just before, as `(from, to)`. A stroke
    /// that looks like an arrow head at its `to` end becomes that head.
    pub fn snap(&mut self, shaft: Option<([f32; 2], [f32; 2])>) -> Option<Snapped> {
        if self.guide.is_some() {
            return None;
        }
        let drawn = self.before_hold();
        let (shape, points) = shaft
            .and_then(|(from, to)| shape::snap_head(drawn, from, to, SNAP_SPACING_PX))
            .or_else(|| shape::snap(drawn, SNAP_SPACING_PX))?;
        let grab = self.last_raw.map_or([0.0, 0.0], |p| [p.x, p.y]);
        self.edit = Some(ShapeEdit::new(shape.clone(), grab));
        Some(self.snapped(shape, points))
    }

    /// After [`snap`](Self::snap), reshapes the shape for the pen at
    /// `(x, y)`: a line follows the pen, an arc continues around its circle,
    /// and closed shapes scale and turn. None before a snap, or when the
    /// shape cannot be drawn.
    pub fn reshape(&mut self, x: f32, y: f32) -> Option<Snapped> {
        if self.guide.is_some() {
            return None;
        }
        let shape = self.edit.as_mut()?.move_to([x, y]).clone();
        let points = shape::samples_along(&shape, self.before_hold(), SNAP_SPACING_PX)?;
        Some(self.snapped(shape, points))
    }

    /// The shape the Shapes tool draws for a drag from `from` to `to`, with
    /// its points and full mesh, in the brush and line style of this stroke.
    /// Pressure and timing come from the samples pushed so far, so push the
    /// pen's samples as the drag goes. None before any sample, or when the
    /// drag draws nothing. Call it again on every move: nothing is kept.
    pub fn draw(
        &mut self,
        spec: &shape::DrawSpec,
        from: [f32; 2],
        to: [f32; 2],
    ) -> Option<Snapped> {
        let shape = shape::draw_shape(spec, from, to)?;
        let points = shape::samples_along(&shape, &self.points, SNAP_SPACING_PX)?;
        Some(self.snapped(shape, points))
    }

    fn snapped(&self, shape: Shape, points: Vec<InputSample>) -> Snapped {
        let mut mesh = Vec::new();
        tessellate_styled(&points, &self.brush, self.line, &mut mesh);
        Snapped {
            shape,
            points,
            mesh,
        }
    }

    // The pen wanders a little while it is held still, which would add a
    // small scribble to the end of the shape, so the points from where it
    // stopped onward are left out.
    fn before_hold(&self) -> &[InputSample] {
        let Some(anchor) = self.hold_anchor else {
            return &self.points;
        };
        let end = self.points.partition_point(|p| p.time_ms <= anchor.time_ms);
        &self.points[..end.max(1).min(self.points.len())]
    }

    /// Changes since the previous call, plus the predicted tip.
    pub fn frame(&mut self, predicted: &[InputSample]) -> Frame {
        let mut tail = Vec::new();
        let mut tail_dirty = Rect::EMPTY;
        if let (Some(&last), false) = (self.points.last(), predicted.is_empty()) {
            // Run predictions through a copy of the filter so they line up
            // with the real stroke without disturbing it.
            let mut stab = self.stabilizer.clone();
            let mut pts = vec![last];
            pts.extend(predicted.iter().map(|&s| {
                if self.guide.is_some() {
                    self.guided(s)
                } else {
                    stab.filter(s)
                }
            }));
            match &self.dasher {
                // A copy of the dasher, for the same reason as the filter.
                Some(d) => {
                    let mut d = d.clone();
                    for w in pts.windows(2) {
                        d.step(Some(&w[0]), &w[1], &mut |piece| {
                            tessellate_piece(&piece, &self.brush, &mut tail)
                        });
                    }
                }
                None => tessellate(&pts, &self.brush, &mut tail),
            }
            for q in &pts {
                tail_dirty.add_circle(q.x, q.y, self.radius(q));
            }
        }
        let dirty = self.pending_dirty.union(&self.prev_tail).union(&tail_dirty);
        self.prev_tail = tail_dirty;
        self.pending_dirty = Rect::EMPTY;
        Frame {
            new_committed: std::mem::take(&mut self.pending),
            tail,
            dirty,
        }
    }

    /// Ends the stroke and returns its points and full mesh. The stabiliser
    /// trails the pen, so the final raw sample is added to make the line
    /// reach where the pen lifted.
    pub fn finish(mut self) -> (Vec<InputSample>, Vec<f32>) {
        if let (Some(raw), Some(last)) = (self.last_raw, self.points.last()) {
            if (raw.x, raw.y) != (last.x, last.y) {
                self.points.push(raw);
            }
        }
        let mut out = Vec::new();
        tessellate_styled(&self.points, &self.brush, self.line, &mut out);
        (self.points, out)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn line(builder: &mut StrokeBuilder) {
        for i in 0..20 {
            builder.push(InputSample::new(i as f32 * 5.0, 0.0, 0.6, i as f32 * 4.0));
        }
    }

    #[test]
    fn frames_add_up_to_the_full_mesh() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        let mut drawn = Vec::new();
        for i in 0..20 {
            b.push(InputSample::new(i as f32 * 5.0, 0.0, 0.6, i as f32 * 4.0));
            drawn.extend(b.frame(&[]).new_committed);
        }
        let mut full = Vec::new();
        tessellate(b.points(), &Brush::default(), &mut full);
        assert_eq!(drawn, full);
    }

    #[test]
    fn a_drawn_shape_follows_the_drag_and_keeps_the_pens_pressure() {
        let spec = shape::DrawSpec::new(shape::DrawKind::Oval);
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        assert!(b.draw(&spec, [0.0, 0.0], [100.0, 80.0]).is_none());
        b.push(InputSample::new(0.0, 0.0, 0.7, 0.0));
        b.push(InputSample::new(100.0, 80.0, 0.7, 50.0));
        let small = b.draw(&spec, [0.0, 0.0], [40.0, 40.0]).expect("draws");
        let big = b.draw(&spec, [0.0, 0.0], [100.0, 80.0]).expect("draws");
        assert!(matches!(big.shape, Shape::Ellipse { .. }));
        assert!(big.points.len() > small.points.len());
        assert!(!big.mesh.is_empty());
        assert!(big.points.iter().all(|p| p.pressure == 0.7));
        assert!(b.draw(&spec, [0.0, 0.0], [0.5, 0.5]).is_none());
    }

    fn frames_for(line: LineStyle) -> (Vec<f32>, Vec<f32>) {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, line);
        let mut drawn = Vec::new();
        for i in 0..40 {
            let y = (i as f32 * 0.3).sin() * 20.0;
            b.push(InputSample::new(i as f32 * 5.0, y, 0.6, i as f32 * 4.0));
            let f = b.frame(&[InputSample::new(i as f32 * 5.0 + 4.0, y, 0.6, 0.0)]);
            drawn.extend(f.new_committed);
        }
        let mut full = Vec::new();
        tessellate_styled(b.points(), &Brush::default(), line, &mut full);
        (drawn, full)
    }

    #[test]
    fn styled_frames_add_up_to_the_full_mesh() {
        for line in [LineStyle::Dashed, LineStyle::Dotted] {
            let (drawn, full) = frames_for(line);
            assert!(!drawn.is_empty());
            assert_eq!(drawn, full, "{line:?}");
        }
        let (dashed, _) = frames_for(LineStyle::Dashed);
        let (solid, _) = frames_for(LineStyle::Solid);
        assert!(dashed.len() < solid.len());
    }

    #[test]
    fn the_predicted_tail_continues_the_pattern() {
        let mut b = StrokeBuilder::new(Brush::with_width(2.0), 0.0, LineStyle::Dashed);
        b.push(InputSample::new(0.0, 0.0, 1.0, 0.0));
        b.push(InputSample::new(10.0, 0.0, 1.0, 4.0));
        b.frame(&[]);
        // On 8, off 6: the dash ended at 8, so the next starts at 14.
        let f = b.frame(&[InputSample::new(30.0, 0.0, 1.0, 8.0)]);
        let xs: Vec<f32> = f.tail.chunks(2).map(|v| v[0]).collect();
        let min = xs.iter().cloned().fold(f32::MAX, f32::min);
        assert!((min - 13.0).abs() < 0.1, "tail starts at {min}");
    }

    #[test]
    fn a_snapped_dashed_line_is_dashed() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Dashed);
        for i in 0..60 {
            b.push(InputSample::new(i as f32 * 5.0, 100.0, 0.6, i as f32 * 8.0));
        }
        let snapped = b.snap(None).expect("a line");
        let mut solid = Vec::new();
        tessellate(&snapped.points, &Brush::default(), &mut solid);
        assert!(snapped.mesh.len() < solid.len());
    }

    #[test]
    fn predictions_are_not_stored_and_their_area_is_cleared_next_frame() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.5, LineStyle::Solid);
        line(&mut b);
        b.frame(&[]);
        let before = b.points().len();
        let f = b.frame(&[InputSample::new(200.0, 0.0, 0.6, 84.0)]);
        assert_eq!(b.points().len(), before);
        assert!(!f.tail.is_empty());
        assert!(f.new_committed.is_empty());
        // Without new input or prediction, the next frame must still cover
        // where the old predicted tip was, so it gets erased.
        let next = b.frame(&[]);
        assert!(next.tail.is_empty());
        assert!(next.dirty.max_x >= f.dirty.max_x);
    }

    #[test]
    fn dirty_area_stays_near_the_pen() {
        let mut b = StrokeBuilder::new(Brush::with_width(6.0), 0.0, LineStyle::Solid);
        line(&mut b);
        b.frame(&[]);
        b.push(InputSample::new(100.0, 0.0, 0.6, 80.0));
        let f = b.frame(&[]);
        assert!(f.dirty.min_x >= 90.0, "dirty starts at {}", f.dirty.min_x);
        assert!(f.dirty.max_x <= 106.0, "dirty ends at {}", f.dirty.max_x);
    }

    #[test]
    fn holding_the_pen_still_does_not_grow_the_stroke() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.3, LineStyle::Solid);
        line(&mut b);
        let before = b.points().len();
        for i in 0..5000 {
            // Tiny digitizer noise around a fixed point.
            let jitter = if i % 2 == 0 { 0.05 } else { -0.05 };
            b.push(InputSample::new(
                95.0 + jitter,
                jitter,
                0.6,
                80.0 + i as f32 * 4.0,
            ));
        }
        assert!(b.points().len() < before + 10);
    }

    #[test]
    fn holding_still_is_timed_from_where_the_pen_stopped() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        assert_eq!(b.still_ms(100.0), 0.0);
        line(&mut b);
        assert!(b.still_ms(76.0) <= 4.0);
        for i in 0..50 {
            let jitter = if i % 2 == 0 { 1.5 } else { -1.5 };
            b.push(InputSample::new(
                95.0 + jitter,
                jitter,
                0.6,
                80.0 + i as f32 * 4.0,
            ));
        }
        // The anchor is set where the pen came within reach of its resting place.
        assert!(b.still_ms(600.0) >= 600.0 - 80.0);
        b.push(InputSample::new(140.0, 0.0, 0.6, 610.0));
        assert_eq!(b.still_ms(610.0), 0.0);
    }

    #[test]
    fn a_straight_stroke_snaps_to_a_line_and_a_scribble_does_not() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        for i in 0..60 {
            let wobble = (i as f32 * 0.7).sin() * 1.5;
            b.push(InputSample::new(
                i as f32 * 5.0,
                100.0 + wobble,
                0.6,
                i as f32 * 8.0,
            ));
        }
        let s = b.snap(None).expect("a straight stroke is a line");
        assert!(matches!(s.shape, Shape::Line { .. }));
        assert!(s.points.iter().all(|p| (p.y - s.points[0].y).abs() < 1e-3));
        assert!(!s.mesh.is_empty());

        let mut s = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        for i in 0..60 {
            let t = i as f32 * 0.35;
            s.push(InputSample::new(
                t * 20.0 + 30.0 * t.cos(),
                40.0 * (2.0 * t).sin(),
                0.6,
                i as f32 * 8.0,
            ));
        }
        assert!(s.snap(None).is_none());
    }

    #[test]
    fn the_wander_while_holding_does_not_spoil_the_shape() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        let mut t = 0.0;
        for i in 0..=80 {
            let a = i as f32 / 80.0 * std::f32::consts::TAU;
            b.push(InputSample::new(
                200.0 + 120.0 * a.cos(),
                200.0 + 120.0 * a.sin(),
                0.6,
                t,
            ));
            t += 8.0;
        }
        // The pen drifts and scribbles a little while it is held at the end.
        for i in 0..60 {
            let k = i as f32;
            let (dx, dy) = (9.0 * (k * 0.9).sin(), 9.0 * (k * 1.3).cos());
            b.push(InputSample::new(320.0 + dx, 200.0 + dy, 0.6, t));
            t += 8.0;
        }
        assert!(b.still_ms(t) >= 400.0);
        let s = b.snap(None).expect("a held circle snaps");
        assert!(matches!(s.shape, Shape::Ellipse { .. }), "{:?}", s.shape);
    }

    #[test]
    fn a_snapped_line_follows_the_pen_until_lift() {
        let mut b = StrokeBuilder::new(Brush::default(), 0.0, LineStyle::Solid);
        for i in 0..60 {
            b.push(InputSample::new(i as f32 * 5.0, 100.0, 0.6, i as f32 * 8.0));
        }
        assert!(
            b.reshape(400.0, 100.0).is_none(),
            "nothing to reshape before a snap"
        );
        b.snap(None).expect("a straight stroke is a line");
        let s = b.reshape(400.0, 101.0).expect("a snapped line reshapes");
        let Shape::Line { from, to } = s.shape else {
            panic!("still a line");
        };
        assert_eq!(from[1], to[1]);
        assert!((to[0] - 400.0).abs() < 0.5, "{to:?}");
        assert!(s.points.last().is_some_and(|p| (p.x - to[0]).abs() < 1e-3));
        assert!(!s.mesh.is_empty());
    }

    fn ruler_edge() -> GuideEdge {
        GuideEdge::Line {
            origin: [0.0, 100.0],
            dir: [1.0, 0.0],
            outward: [0.0, -1.0],
        }
    }

    #[test]
    fn a_guided_stroke_is_exactly_straight_against_the_edge() {
        let mut b = StrokeBuilder::new(Brush::with_width(4.0), 1.0, LineStyle::Solid);
        b.guide(Some(ruler_edge()));
        assert!(b.is_guided());
        let mut drawn = Vec::new();
        for i in 0..40 {
            let wobble = (i as f32 * 0.9).sin() * 6.0;
            b.push(InputSample::new(
                i as f32 * 5.0,
                90.0 + wobble,
                1.0,
                i as f32 * 4.0,
            ));
            drawn.extend(
                b.frame(&[InputSample::new(i as f32 * 5.0 + 3.0, 80.0, 1.0, 0.0)])
                    .new_committed,
            );
        }
        assert!(b.snap(None).is_none());
        assert!(b.reshape(100.0, 100.0).is_none());
        let (points, mesh) = b.finish();
        // Full pressure on a 4 wide pen: half the width above the edge.
        assert!(points.iter().all(|p| (p.y - 98.0).abs() < 1e-4));
        assert_eq!(points.last().unwrap().x, 195.0);
        assert!(!mesh.is_empty());
        assert!(!drawn.is_empty());
    }

    #[test]
    fn a_guided_stroke_ignores_the_stabiliser() {
        let mut b = StrokeBuilder::new(Brush::default(), 1.0, LineStyle::Solid);
        b.guide(Some(ruler_edge()));
        b.push(InputSample::new(0.0, 100.0, 0.6, 0.0));
        b.push(InputSample::new(50.0, 100.0, 0.6, 8.0));
        assert_eq!(b.points().last().unwrap().x, 50.0);
    }

    #[test]
    fn a_guided_arc_is_round_even_when_the_pen_jumps() {
        let mut b = StrokeBuilder::new(Brush::with_width(2.0), 0.5, LineStyle::Solid);
        b.guide(Some(GuideEdge::Circle {
            centre: [0.0, 0.0],
            radius: 100.0,
        }));
        for (i, deg) in [0.0f32, 40.0, 95.0, 170.0].iter().enumerate() {
            let t = deg.to_radians();
            b.push(InputSample::new(
                90.0 * t.cos(),
                -120.0 * t.sin(),
                1.0,
                i as f32 * 8.0,
            ));
        }
        let (points, _) = b.finish();
        assert!(points.len() > 100, "{}", points.len());
        assert!(points.iter().all(|p| (p.x.hypot(p.y) - 101.0).abs() < 1e-2));
    }

    #[test]
    fn finish_reaches_the_lift_point() {
        let mut b = StrokeBuilder::new(Brush::default(), 1.0, LineStyle::Solid);
        line(&mut b);
        let (points, mesh) = b.finish();
        assert_eq!(points.last().unwrap().x, 95.0);
        assert!(!mesh.is_empty());
    }
}
