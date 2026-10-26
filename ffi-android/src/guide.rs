//! JNI entry points for the ruler and protractor: choosing the edge a new
//! stroke follows, and the angles the shell shows next to a guide.
//!
//! Guides cross as packed floats, `Guide::STRIDE` per guide, in view pixels.
//! A panic is caught and thrown as `java.io.IOException`, never unwound into
//! the JVM.

use grapho_core::guide::{self, Guide};
use grapho_core::StrokeBuilder;
use jni::objects::{JClass, JFloatArray};
use jni::sys::{jboolean, jfloat, jint, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::pdf::guarded;
use crate::read_floats;

/// Makes the stroke follow the closest edge of `guides` to the pen at
/// `(x, y)` within `reach`, all in view pixels. Returns whether an edge was
/// found. Call it right after `nativeNew`, before the first push.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeGuide(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    guides: JFloatArray,
    x: jfloat,
    y: jfloat,
    reach: jfloat,
) -> jboolean {
    let guides = Guide::unpack(&read_floats(&mut env, &guides));
    guarded(&mut env, JNI_FALSE, |_| {
        let edge = guide::nearest_edge(&guides, x, y, reach);
        // SAFETY: the caller passes a live handle from `nativeNew`.
        unsafe { &mut *(handle as *mut StrokeBuilder) }.guide(edge);
        Ok(if edge.is_some() { JNI_TRUE } else { JNI_FALSE })
    })
}

/// The index of the first protractor in `guides` whose centre mark is within
/// `reach` of `(x, y)`, or -1.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeGuide_nativeCentreNear(
    mut env: JNIEnv,
    _class: JClass,
    guides: JFloatArray,
    x: jfloat,
    y: jfloat,
    reach: jfloat,
) -> jint {
    let data = read_floats(&mut env, &guides);
    guarded(&mut env, -1, |_| {
        // Index by slot so it matches the caller's list even if a guide was skipped.
        let found = data
            .as_chunks::<{ Guide::STRIDE }>()
            .0
            .iter()
            .position(|g| {
                Guide::unpack(g)
                    .first()
                    .is_some_and(|g| g.centre_near(x, y, reach))
            });
        Ok(found.map_or(-1, |i| i as jint))
    })
}

/// The protractor angle in degrees, from 0 to 180, from its base to the line
/// from its centre to `(x, y)`. `guide` is one packed guide. -1 when it is
/// not a protractor or the point is on its centre.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeGuide_nativeReading(
    mut env: JNIEnv,
    _class: JClass,
    guide: JFloatArray,
    x: jfloat,
    y: jfloat,
) -> jfloat {
    let data = read_floats(&mut env, &guide);
    guarded(&mut env, -1.0, |_| {
        Ok(Guide::unpack(&data)
            .first()
            .and_then(|g| g.reading_degrees(x, y))
            .unwrap_or(-1.0))
    })
}

/// A turn in radians rounded to whole degrees, with a gentle pull to
/// multiples of 15 degrees.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeGuide_nativeSnapAngle(
    mut env: JNIEnv,
    _class: JClass,
    radians: jfloat,
) -> jfloat {
    guarded(&mut env, radians, |_| Ok(guide::snap_angle(radians)))
}

/// The label for a ruler lying along `radians`: whole degrees from the
/// horizontal, anticlockwise, 0 up to 179.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeGuide_nativeRulerDegrees(
    mut env: JNIEnv,
    _class: JClass,
    radians: jfloat,
) -> jfloat {
    guarded(&mut env, 0.0, |_| Ok(guide::ruler_degrees(radians)))
}
