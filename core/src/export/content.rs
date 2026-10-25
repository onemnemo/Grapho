//! A small writer for PDF content streams.

use std::fmt::Write;

/// Content stream text under construction. Everything written is ASCII.
#[derive(Default)]
pub(super) struct Content {
    buf: String,
}

impl Content {
    pub fn new() -> Self {
        Self::default()
    }

    /// Appends a line of operator text, such as `q` or `/GA128 gs`.
    pub fn raw(&mut self, text: &str) {
        self.buf.push_str(text);
        self.buf.push('\n');
    }

    /// Appends a number with at most three decimals and no trailing zeros.
    pub fn num(&mut self, v: f32) {
        let v = if v.is_finite() { v } else { 0.0 };
        let start = self.buf.len();
        // Formatting into a String cannot fail.
        let _ = write!(self.buf, "{v:.3}");
        if self.buf[start..].contains('.') {
            let trimmed = self.buf.trim_end_matches('0').trim_end_matches('.').len();
            self.buf.truncate(trimmed);
        }
        if &self.buf[start..] == "-0" {
            self.buf.truncate(start);
            self.buf.push('0');
        }
    }

    /// Numbers separated by spaces, then an operator.
    pub fn op(&mut self, numbers: &[f32], operator: &str) {
        for &n in numbers {
            self.num(n);
            self.buf.push(' ');
        }
        self.buf.push_str(operator);
        self.buf.push('\n');
    }

    /// `r g b` as a fill (`rg`) or stroke (`RG`) colour from `0xRRGGBBAA`.
    pub fn color(&mut self, rgba: u32, operator: &str) {
        let c = |shift: u32| f32::from(((rgba >> shift) & 0xFF) as u8) / 255.0;
        self.op(&[c(24), c(16), c(8)], operator);
    }

    /// Closed subpaths from `[n, x0, y0, ..., n, ...]` packed polygons, with
    /// no paint operator.
    pub fn polygons(&mut self, packed: &[f32]) {
        let mut i = 0;
        while i < packed.len() {
            let count = packed[i] as usize;
            i += 1;
            let Some(points) = packed.get(i..i + 2 * count) else {
                return;
            };
            for (k, p) in points.as_chunks::<2>().0.iter().enumerate() {
                self.op(p, if k == 0 { "m" } else { "l" });
            }
            if count > 0 {
                self.raw("h");
            }
            i += 2 * count;
        }
    }

    /// Appends everything `other` holds.
    pub fn append(&mut self, other: &Content) {
        self.buf.push_str(&other.buf);
    }

    pub fn into_bytes(self) -> Vec<u8> {
        self.buf.into_bytes()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn text(f: impl FnOnce(&mut Content)) -> String {
        let mut c = Content::new();
        f(&mut c);
        String::from_utf8(c.into_bytes()).unwrap()
    }

    #[test]
    fn numbers_are_short_and_never_negative_zero() {
        assert_eq!(
            text(|c| c.op(&[1.0, 0.5, 12.3456, -0.0001], "x")),
            "1 0.5 12.346 0 x\n"
        );
        assert_eq!(text(|c| c.op(&[100.0, f32::NAN], "x")), "100 0 x\n");
    }

    #[test]
    fn polygons_become_closed_subpaths() {
        let packed = [3.0, 0.0, 0.0, 10.0, 0.0, 10.0, 5.0];
        assert_eq!(text(|c| c.polygons(&packed)), "0 0 m\n10 0 l\n10 5 l\nh\n");
        // A truncated polygon is ignored rather than read past the end.
        assert_eq!(text(|c| c.polygons(&[4.0, 1.0, 2.0])), "");
    }

    #[test]
    fn colours_are_unit_fractions() {
        assert_eq!(text(|c| c.color(0xFF80_00FF, "rg")), "1 0.502 0 rg\n");
    }
}
