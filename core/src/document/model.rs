use std::sync::Arc;

use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{Image, ImageSource, TextBox};
use crate::{Brush, InputSample, LineStyle};

pub type Id = Uuid;

/// Points per millimetre.
pub const MM_TO_PT: f32 = 72.0 / 25.4;

fn new_id() -> Id {
    Uuid::new_v4()
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct Document {
    pub id: Id,
    pub title: String,
    pub canvas: Canvas,
    pub pages: Vec<Page>,
    /// How the notebook looks on the shelf. Optional, added after version 1
    /// shipped; files without it get the default cover.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub cover: Option<Cover>,
    /// The PDF files that pages draw as their background. Added in format
    /// version 2; empty for notes that have none.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub pdfs: Vec<PdfSource>,
    /// The image files that layers draw. Added in format version 3; empty
    /// for notes that have none. A source stays here while any undo step may
    /// need it, and is left out of the file once no layer uses it.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub images: Vec<ImageSource>,
}

/// A PDF embedded in the note. Its bytes live in `pdfs/<id>.pdf` in the file,
/// not in the JSON, and are shared cheaply between clones of the document.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct PdfSource {
    pub id: Id,
    /// The original file name, for showing to people.
    pub name: String,
    pub pages: u32,
    #[serde(skip)]
    pub bytes: Arc<Vec<u8>>,
}

/// The size of a page as it is shown, in points (1/72 inch).
#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct PageSize {
    pub width: f32,
    pub height: f32,
}

/// Which page of an embedded PDF a note page shows as its background.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PdfPage {
    pub source: Id,
    /// Zero-based page of that source.
    pub index: u32,
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct Cover {
    /// Cover colour as 0xRRGGBBAA.
    pub color: u32,
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum Canvas {
    /// Fixed-size pages, like a paper notebook.
    Paged {
        paper: PaperSize,
        orientation: Orientation,
        background: Background,
    },
    /// One page with no edges. Pans in every direction.
    Infinite { background: Background },
}

impl Canvas {
    pub fn background(&self) -> Background {
        match *self {
            Canvas::Paged { background, .. } | Canvas::Infinite { background } => background,
        }
    }

    pub fn set_background(&mut self, value: Background) {
        match self {
            Canvas::Paged { background, .. } | Canvas::Infinite { background } => {
                *background = value
            }
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "name", rename_all = "snake_case")]
pub enum PaperSize {
    A3,
    A4,
    A5,
    Letter,
    Custom { width_mm: f32, height_mm: f32 },
}

impl PaperSize {
    /// Portrait size in millimetres.
    pub fn size_mm(&self) -> (f32, f32) {
        match *self {
            PaperSize::A3 => (297.0, 420.0),
            PaperSize::A4 => (210.0, 297.0),
            PaperSize::A5 => (148.0, 210.0),
            PaperSize::Letter => (215.9, 279.4),
            PaperSize::Custom {
                width_mm,
                height_mm,
            } => (width_mm, height_mm),
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Orientation {
    Portrait,
    Landscape,
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct Background {
    /// Paper colour as 0xRRGGBBAA.
    pub color: u32,
    pub pattern: Pattern,
}

impl Default for Background {
    fn default() -> Self {
        Self {
            color: 0xFFFF_FFFF,
            pattern: Pattern::Blank,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum Pattern {
    Blank,
    Lined { spacing_mm: f32 },
    Grid { spacing_mm: f32 },
    Dotted { spacing_mm: f32 },
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct Page {
    pub id: Id,
    /// Bottom to top.
    pub layers: Vec<Layer>,
    /// Size of this page. Absent means the canvas paper size.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub size: Option<PageSize>,
    /// The PDF page drawn under the ink instead of the paper pattern. A page
    /// with this always has a `size`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub pdf: Option<PdfPage>,
}

impl Page {
    pub fn new() -> Self {
        Self {
            id: new_id(),
            layers: vec![Layer::new("Layer 1")],
            size: None,
            pdf: None,
        }
    }

    /// A blank page of its own size, such as one next to a PDF page.
    pub fn with_size(size: PageSize) -> Self {
        Self {
            size: Some(size),
            ..Self::new()
        }
    }

    pub fn layer(&self, id: Id) -> Option<&Layer> {
        self.layers.iter().find(|l| l.id == id)
    }

    pub fn layer_mut(&mut self, id: Id) -> Option<&mut Layer> {
        self.layers.iter_mut().find(|l| l.id == id)
    }

    /// Adds a layer on top and returns its id.
    pub fn add_layer(&mut self, name: impl Into<String>) -> Id {
        let layer = Layer::new(name);
        let id = layer.id;
        self.layers.push(layer);
        id
    }

    /// Removes a layer with its strokes, images and text boxes. The last layer can't be removed.
    pub fn remove_layer(&mut self, id: Id) -> Option<Layer> {
        if self.layers.len() <= 1 {
            return None;
        }
        let i = self.layers.iter().position(|l| l.id == id)?;
        Some(self.layers.remove(i))
    }

    /// Moves a layer to `index` in the bottom-to-top order.
    pub fn move_layer(&mut self, id: Id, index: usize) -> bool {
        let Some(from) = self.layers.iter().position(|l| l.id == id) else {
            return false;
        };
        let layer = self.layers.remove(from);
        self.layers.insert(index.min(self.layers.len()), layer);
        true
    }

    pub fn stroke_count(&self) -> usize {
        self.layers.iter().map(|l| l.strokes.len()).sum()
    }

    pub fn image_count(&self) -> usize {
        self.layers.iter().map(|l| l.images.len()).sum()
    }

    pub fn text_count(&self) -> usize {
        self.layers.iter().map(|l| l.texts.len()).sum()
    }
}

impl Default for Page {
    fn default() -> Self {
        Self::new()
    }
}

fn full_opacity() -> f32 {
    1.0
}

fn is_full_opacity(opacity: &f32) -> bool {
    *opacity == 1.0
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct Layer {
    pub id: Id,
    pub name: String,
    pub visible: bool,
    pub locked: bool,
    /// How opaque the layer is drawn, 0 to 1. Its strokes are drawn as a
    /// group at this opacity. Optional in the file: absent means 1.
    #[serde(default = "full_opacity", skip_serializing_if = "is_full_opacity")]
    pub opacity: f32,
    /// Stored in the page's binary stroke file, not in the JSON.
    #[serde(skip)]
    pub strokes: Vec<Stroke>,
    /// Pictures on this layer, bottom to top, drawn under its strokes.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub images: Vec<Image>,
    /// Text boxes on this layer, bottom to top, drawn over its images and
    /// under its strokes. Added in format version 4.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub texts: Vec<TextBox>,
}

impl Layer {
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            id: new_id(),
            name: name.into(),
            visible: true,
            locked: false,
            opacity: 1.0,
            strokes: Vec::new(),
            images: Vec::new(),
            texts: Vec::new(),
        }
    }

    /// Removes a stroke by id. Returns it so undo can put it back.
    pub fn remove_stroke(&mut self, id: Id) -> Option<Stroke> {
        let i = self.strokes.iter().position(|s| s.id == id)?;
        Some(self.strokes.remove(i))
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[repr(u8)]
pub enum Tool {
    Pen = 0,
    Pencil = 1,
    Highlighter = 2,
    /// A filled area. The stroke's points are the closed boundary polygon.
    Fill = 3,
}

impl Tool {
    pub(crate) fn from_u8(v: u8) -> Option<Self> {
        match v {
            0 => Some(Tool::Pen),
            1 => Some(Tool::Pencil),
            2 => Some(Tool::Highlighter),
            3 => Some(Tool::Fill),
            _ => None,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct StrokeStyle {
    pub tool: Tool,
    /// 0xRRGGBBAA.
    pub color: u32,
    /// Width in document units at full pressure.
    pub width: f32,
    /// Solid, dashed or dotted.
    #[serde(default)]
    pub line: LineStyle,
}

impl StrokeStyle {
    /// How pressure maps to width for this tool. Highlighters keep an even
    /// width; pens and pencils thin out when pressed lightly.
    pub fn brush(&self) -> Brush {
        match self.tool {
            Tool::Pen => Brush::with_width(self.width),
            Tool::Pencil => Brush {
                max_width: self.width,
                min_ratio: 0.5,
                gamma: 1.0,
            },
            Tool::Highlighter => Brush {
                max_width: self.width,
                min_ratio: 1.0,
                gamma: 1.0,
            },
            Tool::Fill => Brush::with_width(self.width),
        }
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct Stroke {
    pub id: Id,
    pub style: StrokeStyle,
    /// Wall-clock start time, milliseconds since the Unix epoch. With each
    /// point's `time_ms` offset, this lets audio playback follow the ink.
    pub started_at_ms: i64,
    /// Points in document units, with `time_ms` relative to `started_at_ms`.
    pub points: Vec<InputSample>,
}

impl Stroke {
    pub fn new(style: StrokeStyle, started_at_ms: i64, points: Vec<InputSample>) -> Self {
        Self {
            id: new_id(),
            style,
            started_at_ms,
            points,
        }
    }

    /// Whether a circle at `(x, y)` with radius `r` touches the drawn ink.
    pub fn hits(&self, x: f32, y: f32, r: f32) -> bool {
        let brush = self.style.brush();
        let Some((x0, y0, x1, y1)) = self.bounds() else {
            return false;
        };
        let reach = r + self.style.width * 0.5;
        if x < x0 - reach || x > x1 + reach || y < y0 - reach || y > y1 + reach {
            return false;
        }
        if self.style.tool == Tool::Fill {
            return self.hits_polygon(x, y, r);
        }
        let half = |p: &InputSample| brush.width_at(p.pressure) * 0.5;
        if let [p] = self.points.as_slice() {
            return (x - p.x).hypot(y - p.y) <= r + half(p);
        }
        self.points.windows(2).any(|w| {
            let (a, b) = (&w[0], &w[1]);
            let (dx, dy) = (b.x - a.x, b.y - a.y);
            let len2 = dx * dx + dy * dy;
            let t = if len2 > 0.0 {
                (((x - a.x) * dx + (y - a.y) * dy) / len2).clamp(0.0, 1.0)
            } else {
                0.0
            };
            let (px, py) = (a.x + dx * t, a.y + dy * t);
            let w = half(a) + (half(b) - half(a)) * t;
            (x - px).hypot(y - py) <= r + w
        })
    }

    /// Whether a circle touches a fill: its centre is inside the polygon, or
    /// the circle reaches an edge.
    fn hits_polygon(&self, x: f32, y: f32, r: f32) -> bool {
        let pts = &self.points;
        let n = pts.len();
        let mut inside = false;
        for i in 0..n {
            let (a, b) = (&pts[i], &pts[(i + 1) % n]);
            if (a.y > y) != (b.y > y) && x < a.x + (y - a.y) / (b.y - a.y) * (b.x - a.x) {
                inside = !inside;
            }
            let (dx, dy) = (b.x - a.x, b.y - a.y);
            let len2 = dx * dx + dy * dy;
            let t = if len2 > 0.0 {
                (((x - a.x) * dx + (y - a.y) * dy) / len2).clamp(0.0, 1.0)
            } else {
                0.0
            };
            if (x - a.x - dx * t).hypot(y - a.y - dy * t) <= r {
                return true;
            }
        }
        inside
    }

    /// Axis-aligned bounds `(min_x, min_y, max_x, max_y)`, ignoring width.
    pub fn bounds(&self) -> Option<(f32, f32, f32, f32)> {
        let first = self.points.first()?;
        Some(self.points.iter().fold(
            (first.x, first.y, first.x, first.y),
            |(x0, y0, x1, y1), p| (x0.min(p.x), y0.min(p.y), x1.max(p.x), y1.max(p.y)),
        ))
    }
}

impl Document {
    pub fn new_paged(title: impl Into<String>, paper: PaperSize, orientation: Orientation) -> Self {
        Self {
            id: new_id(),
            title: title.into(),
            canvas: Canvas::Paged {
                paper,
                orientation,
                background: Background::default(),
            },
            pages: vec![Page::new()],
            cover: None,
            pdfs: Vec::new(),
            images: Vec::new(),
        }
    }

    pub fn new_infinite(title: impl Into<String>) -> Self {
        Self {
            id: new_id(),
            title: title.into(),
            canvas: Canvas::Infinite {
                background: Background::default(),
            },
            pages: vec![Page::new()],
            cover: None,
            pdfs: Vec::new(),
            images: Vec::new(),
        }
    }

    /// Adds a page at the end. Infinite canvases always have exactly one
    /// page, so this returns `None` for them.
    pub fn add_page(&mut self) -> Option<Id> {
        if matches!(self.canvas, Canvas::Infinite { .. }) {
            return None;
        }
        let page = Page::new();
        let id = page.id;
        self.pages.push(page);
        Some(id)
    }

    pub fn page(&self, id: Id) -> Option<&Page> {
        self.pages.iter().find(|p| p.id == id)
    }

    pub fn page_mut(&mut self, id: Id) -> Option<&mut Page> {
        self.pages.iter_mut().find(|p| p.id == id)
    }

    /// Size of the page at `index` in points, or `None` for an infinite
    /// canvas or a page that doesn't exist. A page with its own size (a PDF
    /// page) uses it, any other page the canvas paper.
    pub fn page_size(&self, index: usize) -> Option<(f32, f32)> {
        let page = self.pages.get(index)?;
        if let (Some(size), Canvas::Paged { .. }) = (page.size, &self.canvas) {
            return Some((size.width, size.height));
        }
        let (w, h) = self.page_size_mm()?;
        Some((w * MM_TO_PT, h * MM_TO_PT))
    }

    /// Size of the canvas paper in millimetres, or `None` for an infinite
    /// canvas. Pages with their own size ignore it, see [`Document::page_size`].
    pub fn page_size_mm(&self) -> Option<(f32, f32)> {
        match self.canvas {
            Canvas::Paged {
                paper, orientation, ..
            } => {
                let (w, h) = paper.size_mm();
                Some(match orientation {
                    Orientation::Portrait => (w, h),
                    Orientation::Landscape => (h, w),
                })
            }
            Canvas::Infinite { .. } => None,
        }
    }

    /// Adds a stroke to a layer. Fails if the page or layer doesn't exist or
    /// the layer is locked.
    pub fn add_stroke(&mut self, page: Id, layer: Id, stroke: Stroke) -> Result<Id, Stroke> {
        match self.page_mut(page).and_then(|p| p.layer_mut(layer)) {
            Some(l) if !l.locked => {
                let id = stroke.id;
                l.strokes.push(stroke);
                Ok(id)
            }
            _ => Err(stroke),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn opacity_is_left_out_of_the_json_until_it_is_used() {
        let mut layer = Layer::new("Ink");
        let json = serde_json::to_string(&layer).unwrap();
        assert!(!json.contains("opacity"));
        layer.opacity = 0.4;
        let json = serde_json::to_string(&layer).unwrap();
        assert!(json.contains("\"opacity\":0.4"));
        let back: Layer = serde_json::from_str(&json).unwrap();
        assert_eq!(back.opacity, 0.4);
    }

    #[test]
    fn a_layer_without_opacity_loads_as_opaque() {
        let id = Uuid::new_v4();
        let json = format!(r#"{{"id":"{id}","name":"Layer 1","visible":true,"locked":false}}"#);
        let layer: Layer = serde_json::from_str(&json).unwrap();
        assert_eq!(layer.opacity, 1.0);
    }

    fn pen() -> StrokeStyle {
        StrokeStyle {
            tool: Tool::Pen,
            color: 0x1A1D_26FF,
            width: 2.0,
            line: LineStyle::Solid,
        }
    }

    #[test]
    fn infinite_canvas_has_one_page() {
        let mut d = Document::new_infinite("Board");
        assert_eq!(d.add_page(), None);
        assert_eq!(d.pages.len(), 1);
        assert_eq!(d.page_size_mm(), None);
    }

    #[test]
    fn landscape_swaps_paper_size() {
        let d = Document::new_paged("Notes", PaperSize::A4, Orientation::Landscape);
        assert_eq!(d.page_size_mm(), Some((297.0, 210.0)));
    }

    #[test]
    fn page_size_is_per_page_in_points() {
        let mut d = Document::new_paged("Notes", PaperSize::A4, Orientation::Portrait);
        d.add_page();
        d.pages[1].size = Some(PageSize {
            width: 300.0,
            height: 200.0,
        });
        let (w, h) = d.page_size(0).unwrap();
        assert!((w - 595.28).abs() < 0.01 && (h - 841.89).abs() < 0.01);
        assert_eq!(d.page_size(1), Some((300.0, 200.0)));
        assert_eq!(d.page_size(2), None);
        assert_eq!(Document::new_infinite("Board").page_size(0), None);
    }

    #[test]
    fn page_and_pdf_fields_stay_out_of_the_json_until_used() {
        let d = Document::new_paged("Notes", PaperSize::A4, Orientation::Portrait);
        let json = serde_json::to_string(&d).unwrap();
        assert!(!json.contains("\"size\"") && !json.contains("pdf"));
        let back: Document = serde_json::from_str(&json).unwrap();
        assert!(back.pages[0].size.is_none() && back.pdfs.is_empty());
    }

    #[test]
    fn locked_layer_rejects_strokes() {
        let mut d = Document::new_paged("Notes", PaperSize::A4, Orientation::Portrait);
        let page = d.pages[0].id;
        let layer = d.pages[0].layers[0].id;
        let s = Stroke::new(pen(), 0, vec![InputSample::new(1.0, 2.0, 0.5, 0.0)]);
        assert!(d.add_stroke(page, layer, s.clone()).is_ok());
        d.page_mut(page).unwrap().layer_mut(layer).unwrap().locked = true;
        assert!(d.add_stroke(page, layer, s).is_err());
        assert_eq!(d.pages[0].stroke_count(), 1);
    }

    #[test]
    fn layers_reorder_and_keep_at_least_one() {
        let mut p = Page::new();
        let first = p.layers[0].id;
        let top = p.add_layer("Sketch");
        assert!(p.move_layer(top, 0));
        assert_eq!(p.layers[0].id, top);
        assert!(p.remove_layer(first).is_some());
        assert!(p.remove_layer(top).is_none());
        assert_eq!(p.layers.len(), 1);
    }

    #[test]
    fn eraser_hits_near_the_line_only() {
        let s = Stroke::new(
            StrokeStyle {
                tool: Tool::Highlighter,
                color: 0xF5D3_3F73,
                width: 4.0,
                line: LineStyle::Solid,
            },
            0,
            vec![
                InputSample::new(0.0, 0.0, 1.0, 0.0),
                InputSample::new(100.0, 0.0, 1.0, 10.0),
            ],
        );
        assert!(s.hits(50.0, 2.5, 1.0));
        assert!(!s.hits(50.0, 3.5, 1.0));
        assert!(s.hits(102.0, 0.0, 0.5));
        assert!(!s.hits(-4.0, 0.0, 1.0));
    }

    #[test]
    fn files_without_a_cover_still_load() {
        let mut d = Document::new_infinite("Board");
        d.cover = Some(Cover { color: 0xA9C4_F5FF });
        let json = serde_json::to_string(&d).unwrap();
        assert!(json.contains("cover"));
        d.cover = None;
        let json = serde_json::to_string(&d).unwrap();
        assert!(!json.contains("cover"));
        assert_eq!(serde_json::from_str::<Document>(&json).unwrap(), d);
    }

    #[test]
    fn eraser_removes_fills_it_touches() {
        let at = |x, y| InputSample::new(x, y, 1.0, 0.0);
        let fill = Stroke::new(
            StrokeStyle {
                tool: Tool::Fill,
                color: 0x2346_D6FF,
                width: 0.0,
                line: LineStyle::Solid,
            },
            0,
            vec![at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0)],
        );
        assert!(fill.hits(5.0, 5.0, 0.5));
        assert!(fill.hits(11.0, 5.0, 1.5));
        assert!(!fill.hits(13.0, 5.0, 1.5));
    }

    #[test]
    fn stroke_bounds() {
        let s = Stroke::new(
            pen(),
            0,
            vec![
                InputSample::new(5.0, 1.0, 0.5, 0.0),
                InputSample::new(-2.0, 8.0, 0.5, 4.0),
            ],
        );
        assert_eq!(s.bounds(), Some((-2.0, 1.0, 5.0, 8.0)));
    }
}
