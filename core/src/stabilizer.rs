use crate::InputSample;

/// Smooths shaky input by letting the drawn point trail the pen.
///
/// `amount` runs from 0.0 (raw input) to 1.0 (heavy smoothing). The filter is
/// time-based, so it behaves the same on 120 Hz and 240 Hz digitizers.
#[derive(Clone, Debug)]
pub struct Stabilizer {
    time_constant_ms: f32,
    last: Option<InputSample>,
}

/// Time constant at `amount == 1.0`. Larger values feel laggy while writing.
const MAX_TIME_CONSTANT_MS: f32 = 40.0;

impl Stabilizer {
    pub fn new(amount: f32) -> Self {
        let a = amount.clamp(0.0, 1.0);
        Self {
            // Squared so the low end of a slider gives fine control.
            time_constant_ms: a * a * MAX_TIME_CONSTANT_MS,
            last: None,
        }
    }

    pub fn reset(&mut self) {
        self.last = None;
    }

    pub fn filter(&mut self, raw: InputSample) -> InputSample {
        let out = match self.last {
            Some(prev) if self.time_constant_ms > 0.0 => {
                let dt = (raw.time_ms - prev.time_ms).max(0.0);
                let k = 1.0 - (-dt / self.time_constant_ms).exp();
                InputSample {
                    x: prev.x + (raw.x - prev.x) * k,
                    y: prev.y + (raw.y - prev.y) * k,
                    pressure: prev.pressure + (raw.pressure - prev.pressure) * k,
                    time_ms: raw.time_ms,
                }
            }
            _ => raw,
        };
        self.last = Some(out);
        out
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn zero_amount_passes_input_through() {
        let mut s = Stabilizer::new(0.0);
        s.filter(InputSample::new(0.0, 0.0, 0.5, 0.0));
        let out = s.filter(InputSample::new(10.0, 5.0, 0.8, 4.0));
        assert_eq!(out, InputSample::new(10.0, 5.0, 0.8, 4.0));
    }

    #[test]
    fn smoothing_trails_the_pen_and_converges() {
        let mut s = Stabilizer::new(1.0);
        s.filter(InputSample::new(0.0, 0.0, 0.5, 0.0));
        let first = s.filter(InputSample::new(100.0, 0.0, 0.5, 4.0));
        assert!(first.x > 0.0 && first.x < 100.0);
        let mut last = first;
        for i in 2..200 {
            last = s.filter(InputSample::new(100.0, 0.0, 0.5, i as f32 * 4.0));
        }
        assert!((last.x - 100.0).abs() < 0.01);
    }

    #[test]
    fn same_result_at_different_sample_rates() {
        // Same 48 ms of pen movement, sampled at 125 Hz and at 250 Hz.
        let run = |steps: u32| {
            let mut s = Stabilizer::new(0.7);
            s.filter(InputSample::new(0.0, 0.0, 0.5, 0.0));
            let mut out = InputSample::new(0.0, 0.0, 0.5, 0.0);
            for i in 1..=steps {
                let t = 48.0 * i as f32 / steps as f32;
                out = s.filter(InputSample::new(100.0, 0.0, 0.5, t));
            }
            out.x
        };
        assert!((run(6) - run(12)).abs() < 0.01);
    }
}
