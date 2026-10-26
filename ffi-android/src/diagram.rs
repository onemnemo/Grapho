//! JNI entry points for cleaning up a picture of a drawing on
//! `NativeNotebook`: stripping its background, or tracing it into ink. The
//! shell decodes the picture and hands over its pixels, so the core never
//! needs an image decoder of its own.
//!
//! A panic anywhere inside is caught and thrown as `java.io.IOException`,
//! never unwound into the JVM.

use grapho_core::diagram::{Cutout, Outline, Pixels, MAX_STRENGTH};
use jni::objects::{JClass, JIntArray, JLongArray};
use jni::sys::{jint, jlong, jlongArray};
use jni::JNIEnv;

use crate::notebook;
use crate::pdf::guarded;
use crate::select::{ids_to_java, read_ids};

fn read_argb(env: &mut JNIEnv, arr: &JIntArray) -> Vec<i32> {
    if arr.is_null() {
        return Vec::new();
    }
    let len = env.get_array_length(arr).unwrap_or(0) as usize;
    let mut buf = vec![0i32; len];
    if env.get_int_array_region(arr, 0, &mut buf).is_err() {
        return Vec::new();
    }
    buf
}

fn pixels_from(width: jint, height: jint, argb: &[i32]) -> Option<Pixels> {
    Pixels::from_argb(
        u32::try_from(width).ok()?,
        u32::try_from(height).ok()?,
        argb,
    )
}

/// Replaces an image with a copy whose background is transparent, keeping
/// its place, turn and crop, as one undo step. `argb` is the whole decoded
/// source, `width * height` ints of `0xAARRGGBB` (not premultiplied), as
/// `Bitmap.getPixels` returns them. Returns the new id as `[hi, lo]`, or an
/// empty array if the image doesn't exist, is on a locked layer, the pixels
/// don't match the source's size, or the engine can't encode the result.
/// `strength` is how much counts as background, 0 to 100 (50 is the usual
/// cut); values outside are clamped. `outline` is 0 for none, 1 for a thin
/// and 2 for a thick white band around what is kept.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeRemoveImageBackground(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
    width: jint,
    height: jint,
    argb: JIntArray,
    strength: jint,
    outline: jint,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        // Copy the arrays out of the JVM before taking the lock.
        let id = read_ids(env, &id).first().copied();
        let argb = read_argb(env, &argb);
        let cutout = Cutout {
            strength: strength.clamp(0, i32::from(MAX_STRENGTH)) as u8,
            outline: Outline::from_code(outline),
        };
        let new = match (usize::try_from(page), id, pixels_from(width, height, &argb)) {
            (Ok(p), Some(id), Some(pixels)) => unsafe { notebook(handle) }
                .remove_image_background(p, id, &pixels, cutout)
                .ok(),
            _ => None,
        };
        Ok(ids_to_java(env, &new.into_iter().collect::<Vec<_>>()))
    })
}

/// Replaces an image with a copy that shows `argb` instead, keeping its
/// place, turn and crop, as one undo step. `argb` is as for
/// `nativeRemoveImageBackground` and must be the size of the image's source.
/// Returns the new id as `[hi, lo]`, or an empty array if the image doesn't
/// exist, is on a locked layer, the pixels don't match the source's size, or
/// the engine can't encode the result.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeReplaceImagePixels(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
    width: jint,
    height: jint,
    argb: JIntArray,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let id = read_ids(env, &id).first().copied();
        let argb = read_argb(env, &argb);
        let new = match (usize::try_from(page), id, pixels_from(width, height, &argb)) {
            (Ok(p), Some(id), Some(pixels)) => unsafe { notebook(handle) }
                .replace_image_pixels(p, id, &pixels)
                .ok(),
            _ => None,
        };
        Ok(ids_to_java(env, &new.into_iter().collect::<Vec<_>>()))
    })
}

/// Replaces an image with pen strokes traced from its drawing, on the same
/// layer and in the same place and size, as one undo step. `argb` is the
/// whole decoded source, as for `nativeRemoveImageBackground`, and
/// `startedAtMs` stamps the new strokes. Returns the new stroke ids as
/// `[hi, lo, ...]` in drawing order, or an empty array if the image doesn't
/// exist, is on a locked layer, the pixels don't match the source's size, or
/// no drawing was found in it.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeImageToInk(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
    width: jint,
    height: jint,
    argb: JIntArray,
    started_at_ms: jlong,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let id = read_ids(env, &id).first().copied();
        let argb = read_argb(env, &argb);
        let strokes = match (usize::try_from(page), id, pixels_from(width, height, &argb)) {
            (Ok(p), Some(id), Some(pixels)) => unsafe { notebook(handle) }
                .image_to_ink(p, id, &pixels, started_at_ms)
                .unwrap_or_default(),
            _ => Vec::new(),
        };
        Ok(ids_to_java(env, &strokes))
    })
}
