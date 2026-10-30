//! JNI entry points for the Shapes tool: a shape drawn from a drag, shown
//! live and stored like any other stroke.
//!
//! A panic is caught and thrown as `java.io.IOException`, never unwound into
//! the JVM.

use grapho_core::shape::{self, DrawKind, DrawSpec};
use grapho_core::StrokeBuilder;
use jni::objects::{JClass, JFloatArray};
use jni::sys::{jfloat, jfloatArray, jlong};
use jni::JNIEnv;

use crate::pdf::guarded;
use crate::{floats_to_java, pack_snapped, read_floats, read_samples};

/// The shape kinds of `NativeInk.nativeDrawShape`. The numbers are part of
/// the interface to Kotlin, so they never change.
const BOX: f32 = 0.0;
const OVAL: f32 = 1.0;
const POLYGON: f32 = 2.0;
const STAR: f32 = 3.0;
const LINE: f32 = 4.0;
const ELBOW: f32 = 5.0;
const CURVED: f32 = 6.0;

/// Bit of the ends field set when the start of a line has an arrow head.
const START_ARROW: u32 = 1;
/// Bit set when the end has one.
const END_ARROW: u32 = 2;

/// Decodes `[kind, a, b, head, ends]`: `a` is a box's corner radius, a
/// polygon's sides or a star's points, `b` is a star's sharpness from 0 to 1,
/// `head` is the arrow head size and `ends` holds the arrow bits. None for a
/// short array or an unknown kind.
fn spec_from(spec: &[f32]) -> Option<DrawSpec> {
    let &[kind, a, b, head, ends] = spec.first_chunk::<5>()?;
    let count = a.max(0.0) as u32;
    let kind = match kind {
        k if k == BOX => DrawKind::Box { radius: a },
        k if k == OVAL => DrawKind::Oval,
        k if k == POLYGON => DrawKind::Polygon { sides: count },
        k if k == STAR => DrawKind::Star {
            points: count,
            sharpness: b,
        },
        k if k == LINE => DrawKind::Line,
        k if k == ELBOW => DrawKind::Elbow,
        k if k == CURVED => DrawKind::Curved,
        _ => return None,
    };
    let ends = ends.max(0.0) as u32;
    Some(DrawSpec {
        kind,
        head: head.max(0.0),
        start_arrow: ends & START_ARROW != 0,
        end_arrow: ends & END_ARROW != 0,
    })
}

/// The shape for a drag from `(x0, y0)` to `(x1, y1)` in view pixels, in the
/// same layout as `nativeSnap`: `[line, x0, y0, x1, y1, n, <n point floats>,
/// <mesh floats>]`. `spec` is `[kind, a, b, head, ends]`, see [`spec_from`].
/// Empty when the drag draws nothing. Push the pen's samples to the stroke
/// as the drag goes, so the shape takes their pressure and timing.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeDrawShape(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    spec: JFloatArray,
    x0: jfloat,
    y0: jfloat,
    x1: jfloat,
    y1: jfloat,
) -> jfloatArray {
    let spec = spec_from(&read_floats(&mut env, &spec));
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let drawn = match spec {
            // SAFETY: the caller passes a live handle from `nativeNew`.
            Some(spec) => {
                unsafe { &mut *(handle as *mut StrokeBuilder) }.draw(&spec, [x0, y0], [x1, y1])
            }
            None => None,
        };
        Ok(floats_to_java(env, &pack_snapped(drawn)))
    })
}

/// Point spacing along the curved parts of a suggested shape, in view pixels.
const SUGGEST_SPACING_PX: f32 = 3.0;

/// The shape worth offering for a finished stroke, from its packed samples
/// `[x, y, pressure, time_ms, ...]` in view pixels: `[class, n, <n point
/// floats packed the same way>]`, where `class` is a `ShapeClass` code from
/// the core and the points redraw the stroke as that shape. Empty when no
/// shape fits.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeSuggest(
    mut env: JNIEnv,
    _class: JClass,
    samples: JFloatArray,
) -> jfloatArray {
    let samples = read_samples(&mut env, &samples);
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let packed = suggestion(&samples);
        Ok(floats_to_java(env, &packed))
    })
}

fn suggestion(samples: &[grapho_core::InputSample]) -> Vec<f32> {
    let Some((class, points)) = shape::suggest(samples, SUGGEST_SPACING_PX) else {
        return Vec::new();
    };
    let n = points.len() * grapho_core::InputSample::STRIDE;
    let mut out = Vec::with_capacity(2 + n);
    out.push(f32::from(class.code()));
    out.push(n as f32);
    out.extend(
        points
            .iter()
            .flat_map(|p| [p.x, p.y, p.pressure, p.time_ms]),
    );
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn kinds_decode_with_their_numbers() {
        let spec = |kind: f32, a: f32, b: f32, ends: f32| spec_from(&[kind, a, b, 12.0, ends]);
        assert_eq!(
            spec(BOX, 8.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Box { radius: 8.0 })
        );
        assert_eq!(
            spec(OVAL, 0.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Oval)
        );
        assert_eq!(
            spec(POLYGON, 6.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Polygon { sides: 6 })
        );
        assert_eq!(
            spec(STAR, 5.0, 0.5, 0.0).map(|s| s.kind),
            Some(DrawKind::Star {
                points: 5,
                sharpness: 0.5
            })
        );
        assert_eq!(
            spec(LINE, 0.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Line)
        );
        assert_eq!(
            spec(ELBOW, 0.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Elbow)
        );
        assert_eq!(
            spec(CURVED, 0.0, 0.0, 0.0).map(|s| s.kind),
            Some(DrawKind::Curved)
        );
    }

    #[test]
    fn ends_and_head_decode() {
        let s = spec_from(&[LINE, 0.0, 0.0, 9.0, 3.0]).expect("decodes");
        assert!(s.start_arrow && s.end_arrow);
        assert_eq!(s.head, 9.0);
        let end_only = spec_from(&[LINE, 0.0, 0.0, 9.0, 2.0]).expect("decodes");
        assert!(!end_only.start_arrow && end_only.end_arrow);
        let none = spec_from(&[LINE, 0.0, 0.0, -4.0, 0.0]).expect("decodes");
        assert!(!none.start_arrow && !none.end_arrow);
        assert_eq!(none.head, 0.0);
    }

    #[test]
    fn a_circle_is_suggested_with_its_class_and_points_and_a_dot_is_not() {
        use grapho_core::InputSample;
        let circle: Vec<InputSample> = (0..60)
            .map(|i| {
                let a = std::f32::consts::TAU * i as f32 / 56.0;
                InputSample::new(
                    300.0 + 120.0 * a.cos(),
                    300.0 + 120.0 * a.sin(),
                    0.5,
                    i as f32 * 8.0,
                )
            })
            .collect();
        let out = suggestion(&circle);
        assert_eq!(out[0], 3.0, "a circle");
        assert_eq!(out[1] as usize, out.len() - 2);
        assert_eq!((out.len() - 2) % InputSample::STRIDE, 0);
        assert!(suggestion(&[InputSample::new(5.0, 5.0, 0.5, 0.0); 40]).is_empty());
    }

    #[test]
    fn short_or_unknown_specs_are_refused() {
        assert!(spec_from(&[]).is_none());
        assert!(spec_from(&[LINE, 0.0, 0.0, 9.0]).is_none());
        assert!(spec_from(&[42.0, 0.0, 0.0, 9.0, 0.0]).is_none());
        assert!(spec_from(&[f32::NAN, 0.0, 0.0, 9.0, 0.0]).is_none());
    }
}
