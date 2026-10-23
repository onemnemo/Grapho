/// Axis-aligned rectangle in view pixels. An empty rect has `min > max`.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Rect {
    pub min_x: f32,
    pub min_y: f32,
    pub max_x: f32,
    pub max_y: f32,
}

impl Rect {
    pub const EMPTY: Rect = Rect {
        min_x: f32::INFINITY,
        min_y: f32::INFINITY,
        max_x: f32::NEG_INFINITY,
        max_y: f32::NEG_INFINITY,
    };

    pub fn is_empty(&self) -> bool {
        self.min_x > self.max_x || self.min_y > self.max_y
    }

    /// Grows the rect to cover a circle.
    pub fn add_circle(&mut self, x: f32, y: f32, r: f32) {
        self.min_x = self.min_x.min(x - r);
        self.min_y = self.min_y.min(y - r);
        self.max_x = self.max_x.max(x + r);
        self.max_y = self.max_y.max(y + r);
    }

    pub fn union(&self, other: &Rect) -> Rect {
        Rect {
            min_x: self.min_x.min(other.min_x),
            min_y: self.min_y.min(other.min_y),
            max_x: self.max_x.max(other.max_x),
            max_y: self.max_y.max(other.max_y),
        }
    }
}

impl Default for Rect {
    fn default() -> Self {
        Rect::EMPTY
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn union_with_empty_is_identity() {
        let mut r = Rect::EMPTY;
        assert!(r.is_empty());
        r.add_circle(10.0, 10.0, 2.0);
        assert_eq!(Rect::EMPTY.union(&r), r);
        assert_eq!(
            r,
            Rect {
                min_x: 8.0,
                min_y: 8.0,
                max_x: 12.0,
                max_y: 12.0
            }
        );
    }
}
