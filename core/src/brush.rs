/// How pressure maps to stroke width.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Brush {
    /// Width in pixels at full pressure.
    pub max_width: f32,
    /// Width at zero pressure, as a fraction of `max_width`.
    pub min_ratio: f32,
    /// Pressure curve exponent. Above 1.0 needs a firmer press to get thick.
    pub gamma: f32,
}

impl Default for Brush {
    fn default() -> Self {
        Self {
            max_width: 6.0,
            min_ratio: 0.35,
            gamma: 1.4,
        }
    }
}

impl Brush {
    pub fn with_width(max_width: f32) -> Self {
        Self {
            max_width,
            ..Self::default()
        }
    }

    pub fn width_at(&self, pressure: f32) -> f32 {
        let p = pressure.clamp(0.0, 1.0).powf(self.gamma);
        self.max_width * (self.min_ratio + (1.0 - self.min_ratio) * p)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn width_grows_with_pressure_and_stays_in_range() {
        let b = Brush::with_width(10.0);
        assert!((b.width_at(0.0) - 3.5).abs() < 1e-5);
        assert!((b.width_at(1.0) - 10.0).abs() < 1e-5);
        assert!(b.width_at(0.3) < b.width_at(0.7));
        assert_eq!(b.width_at(2.0), b.width_at(1.0));
    }
}
