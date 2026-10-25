//! JNI entry points for pictures on `NativeNotebook`: placing, finding and
//! cropping them, and handing their encoded bytes to the shell to draw.
//! Moving, deleting and copying go through the selection entry points,
//! which take image ids as well as stroke ids.
//!
//! A panic anywhere inside is caught and thrown as `java.io.IOException`,
//! never unwound into the JVM.

use grapho_core::document::{Id, Image};
use grapho_core::Rect;
use jni::objects::{JByteArray, JClass, JFloatArray, JLongArray, JString};
use jni::sys::{jbyteArray, jfloat, jint, jintArray, jlong, jlongArray, jstring};
use jni::JNIEnv;
use serde_json::json;

use crate::pdf::guarded;
use crate::select::{ids_to_java, read_ids};
use crate::{longs_to_java, notebook, read_floats, read_string, split_id, string_to_java};

fn one_id(ids: Option<Id>) -> Vec<Id> {
    ids.into_iter().collect()
}

fn image_json(image: &Image) -> serde_json::Value {
    let [hi, lo] = split_id(image.id);
    json!({
        "hi": hi,
        "lo": lo,
        "source": image.source.to_string(),
        "m": image.transform,
        "crop": image.crop,
    })
}

/// Embeds a PNG or JPEG and places it on top of the page's active layer,
/// covering the rectangle `(x, y, width, height)` in page-local points, as
/// one undo step. Returns the image id as `[hi, lo]`, or an empty array if
/// the layer is hidden or locked, the page doesn't exist, the rectangle is
/// empty, or the bytes are not a PNG or JPEG the engine can use.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeInsertImage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    bytes: JByteArray,
    x: jfloat,
    y: jfloat,
    width: jfloat,
    height: jfloat,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        // Copy the bytes out of the JVM before taking the lock.
        let bytes = env.convert_byte_array(&bytes).unwrap_or_default();
        let rect = Rect {
            min_x: x,
            min_y: y,
            max_x: x + width,
            max_y: y + height,
        };
        let id = usize::try_from(page).ok().and_then(|p| {
            unsafe { notebook(handle) }
                .insert_image(p, bytes, rect)
                .ok()
        });
        Ok(ids_to_java(env, &one_id(id)))
    })
}

/// The topmost image under a page-local point on a visible, unlocked layer,
/// as `[hi, lo]`, or an empty array if there is none.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeImageAt(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    x: jfloat,
    y: jfloat,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let id = usize::try_from(page)
            .ok()
            .and_then(|p| unsafe { notebook(handle) }.image_at(p, x, y));
        Ok(ids_to_java(env, &one_id(id)))
    })
}

/// The images of a page, one inner array per visible layer in the same
/// order as `nativeLayerStrokeIds`, each in drawing order, as JSON:
/// `[[{"hi":1,"lo":2,"source":"<uuid>","m":[sx,kx,tx,ky,sy,ty],
/// "crop":[l,t,r,b]}, ...], ...]`. `m` maps the unit square of the shown
/// part onto the page in points, `crop` is the shown part of the source as
/// fractions. An unknown page gives `[]`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativePageImages(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
) -> jstring {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let text = {
            let e = unsafe { notebook(handle) };
            let page = usize::try_from(page)
                .ok()
                .and_then(|i| e.document().pages.get(i));
            let layers: Vec<Vec<_>> = page
                .into_iter()
                .flat_map(|p| &p.layers)
                .filter(|l| l.visible)
                .map(|l| l.images.iter().map(image_json).collect())
                .collect();
            serde_json::Value::from(layers).to_string()
        };
        Ok(string_to_java(env, &text))
    })
}

/// The encoded bytes (PNG or JPEG) of an embedded image source, by the
/// `source` id of a `nativePageImages` entry, or null if the notebook has
/// no such source.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeImageBytes(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    source: JString,
) -> jbyteArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let Ok(id) = read_string(env, &source).parse::<Id>() else {
            return Ok(std::ptr::null_mut());
        };
        // Share the bytes under the lock and copy them to the JVM after it.
        let bytes = unsafe { notebook(handle) }
            .document()
            .images
            .iter()
            .find(|s| s.id == id)
            .map(|s| s.bytes.clone());
        Ok(bytes.map_or(std::ptr::null_mut(), |b| {
            env.byte_array_from_slice(&b)
                .map_or(std::ptr::null_mut(), |a| a.into_raw())
        }))
    })
}

/// The pixel size of an image source as `[width, height]`, or an empty array
/// if the notebook has no such source.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeImageSize(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    source: JString,
) -> jintArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let size: Vec<i32> = read_string(env, &source)
            .parse::<Id>()
            .ok()
            .and_then(|id| {
                let e = unsafe { notebook(handle) };
                let s = e.document().images.iter().find(|s| s.id == id)?;
                Some(vec![s.width as i32, s.height as i32])
            })
            .unwrap_or_default();
        let Ok(out) = env.new_int_array(size.len() as i32) else {
            return Ok(std::ptr::null_mut());
        };
        if env.set_int_array_region(&out, 0, &size).is_err() {
            return Ok(std::ptr::null_mut());
        }
        Ok(out.into_raw())
    })
}

/// Shows only `crop` (`[left, top, right, bottom]`, fractions of the
/// source) of the image, keeping what is still shown in place, as one undo
/// step. Returns the new id as `[hi, lo]`, or an empty array if the image
/// doesn't exist, is on a locked layer, the crop is not four numbers, or it
/// changes nothing.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeCropImage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
    crop: JFloatArray,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let id = read_ids(env, &id).first().copied();
        let crop = <[f32; 4]>::try_from(read_floats(env, &crop).as_slice()).ok();
        let new = match (usize::try_from(page), id, crop) {
            (Ok(p), Some(id), Some(crop)) => unsafe { notebook(handle) }.crop_image(p, id, crop),
            _ => None,
        };
        Ok(longs_to_java(
            env,
            &new.map(split_id).map(Vec::from).unwrap_or_default(),
        ))
    })
}
