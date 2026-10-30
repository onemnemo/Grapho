//! Text boxes placed on a layer: a rectangle of the page that holds plain
//! text. The core stores what is written and how it is styled. Laying the
//! text out is left to the shell, which reports the height it needs.

use std::fmt;

use serde::{Deserialize, Serialize};

use super::Id;

/// The smallest and largest font size, in points.
pub const MIN_TEXT_SIZE: f32 = 4.0;
pub const MAX_TEXT_SIZE: f32 = 400.0;
/// The longest text a box holds, in characters.
pub const MAX_TEXT_CHARS: usize = 20_000;
/// The least a box may measure across, in points.
pub const MIN_TEXT_SIDE: f32 = 1.0;

/// Where lines sit across the box. Persisted: append only.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum TextAlign {
    #[default]
    Start,
    Centre,
    End,
}

impl TextAlign {
    pub fn from_u8(v: u8) -> Option<Self> {
        match v {
            0 => Some(TextAlign::Start),
            1 => Some(TextAlign::Centre),
            2 => Some(TextAlign::End),
            _ => None,
        }
    }

    pub fn to_u8(self) -> u8 {
        match self {
            TextAlign::Start => 0,
            TextAlign::Centre => 1,
            TextAlign::End => 2,
        }
    }
}

fn is_zero(v: &f32) -> bool {
    *v == 0.0
}

fn is_false(v: &bool) -> bool {
    !*v
}

/// A box of text on a layer. It is drawn under the layer's strokes, over its
/// images, in list order.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct TextBox {
    pub id: Id,
    /// Top left of the box before it is turned, in page points.
    pub x: f32,
    pub y: f32,
    pub width: f32,
    pub height: f32,
    /// Clockwise turn about the centre of the box, in radians. Absent in the
    /// file means none.
    #[serde(default, skip_serializing_if = "is_zero")]
    pub rotation: f32,
    pub text: String,
    /// Font size in points.
    pub size: f32,
    /// 0xRRGGBBAA.
    pub color: u32,
    #[serde(default)]
    pub align: TextAlign,
    #[serde(default, skip_serializing_if = "is_false")]
    pub bold: bool,
    #[serde(default, skip_serializing_if = "is_false")]
    pub italic: bool,
    #[serde(default, skip_serializing_if = "is_false")]
    pub underline: bool,
}

/// The look of a text box, apart from its words and place.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct TextStyle {
    pub size: f32,
    pub color: u32,
    pub align: TextAlign,
    pub bold: bool,
    pub italic: bool,
    pub underline: bool,
}

impl Default for TextStyle {
    fn default() -> Self {
        Self {
            size: 18.0,
            color: 0x1A1D_26FF,
            align: TextAlign::Start,
            bold: false,
            italic: false,
            underline: false,
        }
    }
}

impl TextBox {
    /// An unturned box with the given words and look.
    pub fn new(
        x: f32,
        y: f32,
        width: f32,
        height: f32,
        text: impl Into<String>,
        style: TextStyle,
    ) -> Self {
        Self {
            id: Id::new_v4(),
            x,
            y,
            width,
            height,
            rotation: 0.0,
            text: text.into(),
            size: style.size,
            color: style.color,
            align: style.align,
            bold: style.bold,
            italic: style.italic,
            underline: style.underline,
        }
    }

    pub fn style(&self) -> TextStyle {
        TextStyle {
            size: self.size,
            color: self.color,
            align: self.align,
            bold: self.bold,
            italic: self.italic,
            underline: self.underline,
        }
    }

    /// Takes on a look, with the font size held to its limits.
    pub fn set_style(&mut self, style: TextStyle) {
        self.size = style.size.clamp(MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        self.color = style.color;
        self.align = style.align;
        self.bold = style.bold;
        self.italic = style.italic;
        self.underline = style.underline;
    }

    pub fn centre(&self) -> (f32, f32) {
        (self.x + self.width * 0.5, self.y + self.height * 0.5)
    }

    /// Where a point of the box lands on the page, with `(0, 0)` its top
    /// left and `(width, height)` its bottom right before turning.
    pub fn to_page(&self, u: f32, v: f32) -> (f32, f32) {
        let (cx, cy) = self.centre();
        let (dx, dy) = (u - self.width * 0.5, v - self.height * 0.5);
        let (sin, cos) = self.rotation.sin_cos();
        (cx + dx * cos - dy * sin, cy + dx * sin + dy * cos)
    }

    /// The page positions of the top left, top right, bottom right and
    /// bottom left corners.
    pub fn corners(&self) -> [(f32, f32); 4] {
        let (w, h) = (self.width, self.height);
        [(0.0, 0.0), (w, 0.0), (w, h), (0.0, h)].map(|(u, v)| self.to_page(u, v))
    }

    /// Whether a page point is inside the box.
    pub fn contains(&self, x: f32, y: f32) -> bool {
        let (cx, cy) = self.centre();
        let (sin, cos) = self.rotation.sin_cos();
        let (dx, dy) = (x - cx, y - cy);
        let (u, v) = (dx * cos + dy * sin, -dx * sin + dy * cos);
        u.abs() <= self.width * 0.5 && v.abs() <= self.height * 0.5
    }

    /// A copy placed by `m` (`[sx, kx, tx, ky, sy, ty]`, as for
    /// `Editor::transform`), with the same id. The box follows where its
    /// sides go: a move keeps it exact, a turn turns it, a scale scales the
    /// box and the font by the matrix's area factor. A shear is dropped and
    /// a mirror does not mirror the words.
    pub fn transformed(&self, m: [f32; 6]) -> Self {
        let [sx, kx, tx, ky, sy, ty] = m;
        let mut out = self.clone();
        if (sx, kx, ky, sy) == (1.0, 0.0, 0.0, 1.0) {
            out.x += tx;
            out.y += ty;
            return out;
        }
        let (sin, cos) = self.rotation.sin_cos();
        let (ux, uy) = (sx * cos + kx * sin, ky * cos + sy * sin);
        let (vx, vy) = (-sx * sin + kx * cos, -ky * sin + sy * cos);
        let (cx, cy) = self.centre();
        let (nx, ny) = (sx * cx + kx * cy + tx, ky * cx + sy * cy + ty);
        let width = (ux.hypot(uy) * self.width).max(MIN_TEXT_SIDE);
        let height = (vx.hypot(vy) * self.height).max(MIN_TEXT_SIDE);
        let scale = (sx * sy - kx * ky).abs().sqrt();
        out.width = width;
        out.height = height;
        out.x = nx - width * 0.5;
        out.y = ny - height * 0.5;
        out.rotation = uy.atan2(ux);
        out.size = (self.size * scale).clamp(MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        out
    }

    /// A copy with a new id, moved by `(dx, dy)`.
    pub fn moved(&self, dx: f32, dy: f32) -> Self {
        Self {
            id: Id::new_v4(),
            x: self.x + dx,
            y: self.y + dy,
            ..self.clone()
        }
    }

    /// Whether the place, size and words are fit to keep.
    pub(super) fn is_sound(&self) -> bool {
        [self.x, self.y, self.width, self.height, self.rotation]
            .iter()
            .all(|v| v.is_finite())
            && self.width >= MIN_TEXT_SIDE
            && self.height >= MIN_TEXT_SIDE
            && (MIN_TEXT_SIZE..=MAX_TEXT_SIZE).contains(&self.size)
            && self.text.chars().count() <= MAX_TEXT_CHARS
    }
}

/// Why a text box could not be placed or changed.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum TextError {
    /// The page doesn't exist, or its active layer is hidden or locked.
    NoLayer,
    /// The box is empty, not a number, or its font size is out of range.
    BadBox,
    /// The text is longer than a box holds.
    TooLong,
    /// The text box isn't on the page, or is on a locked layer.
    NotFound,
}

impl fmt::Display for TextError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            TextError::NoLayer => write!(f, "the layer is hidden or locked"),
            TextError::BadBox => write!(f, "the text box's place or size is not valid"),
            TextError::TooLong => write!(f, "the text is too long"),
            TextError::NotFound => write!(f, "the text box is not on the page"),
        }
    }
}

impl std::error::Error for TextError {}

#[cfg(test)]
mod tests {
    use super::*;

    fn boxed(x: f32, y: f32) -> TextBox {
        TextBox::new(x, y, 100.0, 40.0, "Hello", TextStyle::default())
    }

    #[test]
    fn a_turned_box_is_hit_where_it_lies() {
        let mut t = boxed(5000.0, 3000.0);
        assert!(t.contains(5001.0, 3001.0));
        assert!(!t.contains(4999.0, 3001.0));
        t.rotation = std::f32::consts::FRAC_PI_2;
        // Turned a quarter about (5050, 3020): now 40 wide and 100 tall.
        assert!(t.contains(5050.0, 3065.0));
        assert!(!t.contains(5001.0, 3001.0));
        let c = t.corners();
        assert!((c[0].0 - 5070.0).abs() < 0.01 && (c[0].1 - 2970.0).abs() < 0.01);
    }

    #[test]
    fn a_move_is_exact_far_from_the_origin() {
        let t = boxed(5000.5, 7000.25);
        let m = t.transformed([1.0, 0.0, 12.5, 0.0, 1.0, -3.25]);
        assert_eq!((m.x, m.y), (5013.0, 6997.0));
        assert_eq!((m.width, m.height, m.size), (100.0, 40.0, 18.0));
    }

    #[test]
    fn a_scale_scales_the_box_and_the_font() {
        let t = boxed(5000.0, 5000.0);
        let m = t.transformed([2.0, 0.0, 0.0, 0.0, 2.0, 0.0]);
        assert!((m.width - 200.0).abs() < 1e-3 && (m.height - 80.0).abs() < 1e-3);
        assert!((m.size - 36.0).abs() < 1e-3);
        // The centre went from (5050, 5020) to (10100, 10040).
        assert!((m.x + m.width * 0.5 - 10100.0).abs() < 0.01);
        assert!((m.y + m.height * 0.5 - 10040.0).abs() < 0.01);
    }

    #[test]
    fn a_turn_turns_the_box_about_the_point() {
        let t = boxed(5000.0, 5000.0);
        let (s, c) = std::f32::consts::FRAC_PI_2.sin_cos();
        // A quarter turn about the origin: x' = -y, y' = x.
        let m = t.transformed([c, -s, 0.0, s, c, 0.0]);
        assert!((m.rotation - std::f32::consts::FRAC_PI_2).abs() < 1e-4);
        assert!((m.width - 100.0).abs() < 1e-2 && (m.height - 40.0).abs() < 1e-2);
        let (cx, cy) = m.centre();
        assert!((cx + 5020.0).abs() < 0.05 && (cy - 5050.0).abs() < 0.05);
    }

    #[test]
    fn a_flat_squash_keeps_a_box_that_can_be_kept() {
        let t = boxed(0.0, 0.0);
        let m = t.transformed([0.0, 0.0, 0.0, 0.0, 0.0, 0.0]);
        assert!(m.is_sound());
    }

    #[test]
    fn soundness_checks_numbers_and_limits() {
        assert!(boxed(0.0, 0.0).is_sound());
        let mut t = boxed(0.0, 0.0);
        t.width = 0.0;
        assert!(!t.is_sound());
        let mut t = boxed(f32::NAN, 0.0);
        assert!(!t.is_sound());
        t.x = 0.0;
        t.size = 1.0;
        assert!(!t.is_sound());
        t.size = 12.0;
        t.text = "a".repeat(MAX_TEXT_CHARS + 1);
        assert!(!t.is_sound());
    }

    #[test]
    fn optional_fields_stay_out_of_the_json_until_used() {
        let mut t = boxed(1.0, 2.0);
        let json = serde_json::to_string(&t).unwrap();
        assert!(!json.contains("rotation") && !json.contains("bold"));
        assert!(!json.contains("italic") && !json.contains("underline"));
        assert!(json.contains("\"align\":\"start\""));
        t.rotation = 0.5;
        t.bold = true;
        t.italic = true;
        t.underline = true;
        t.align = TextAlign::Centre;
        let json = serde_json::to_string(&t).unwrap();
        assert_eq!(serde_json::from_str::<TextBox>(&json).unwrap(), t);
    }
}
