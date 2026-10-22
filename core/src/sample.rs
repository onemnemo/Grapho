/// One stylus reading, in view pixels.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct InputSample {
    pub x: f32,
    pub y: f32,
    /// Normalised pressure, 0.0 to 1.0.
    pub pressure: f32,
    /// Event time in milliseconds. Only differences matter.
    pub time_ms: f32,
}

impl InputSample {
    pub const fn new(x: f32, y: f32, pressure: f32, time_ms: f32) -> Self {
        Self {
            x,
            y,
            pressure,
            time_ms,
        }
    }

    /// Number of floats per sample in the packed FFI layout.
    pub const STRIDE: usize = 4;

    /// Reads samples packed as `[x, y, pressure, time_ms, ...]`.
    /// A trailing partial sample is ignored.
    pub fn unpack(data: &[f32]) -> impl Iterator<Item = InputSample> + '_ {
        let (chunks, _tail) = data.as_chunks::<{ Self::STRIDE }>();
        chunks
            .iter()
            .map(|&[x, y, pressure, time_ms]| InputSample::new(x, y, pressure, time_ms))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unpack_ignores_partial_tail() {
        let data = [1.0, 2.0, 0.5, 10.0, 3.0, 4.0, 0.6, 11.0, 9.0];
        let samples: Vec<_> = InputSample::unpack(&data).collect();
        assert_eq!(
            samples,
            vec![
                InputSample::new(1.0, 2.0, 0.5, 10.0),
                InputSample::new(3.0, 4.0, 0.6, 11.0)
            ]
        );
    }
}
