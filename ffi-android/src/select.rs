//! JNI entry points for lasso selection on `NativeNotebook`: select, move,
//! recolour, delete, copy, paste and duplicate. Ids cross as `[hi, lo, ...]`
//! pairs like everywhere else.

use grapho_core::document::Id;
use jni::objects::{JClass, JFloatArray, JLongArray};
use jni::sys::{jboolean, jfloat, jfloatArray, jint, jlong, jlongArray, JNI_FALSE};
use jni::JNIEnv;

use crate::{
    floats_to_java, join_id, longs_to_java, notebook, read_floats, read_longs, rgba_from_argb,
    split_id,
};

pub(crate) fn read_ids(env: &mut JNIEnv, arr: &JLongArray) -> Vec<Id> {
    read_longs(env, arr)
        .as_chunks::<2>()
        .0
        .iter()
        .map(|&[hi, lo]| join_id(hi, lo))
        .collect()
}

pub(crate) fn ids_to_java(env: &mut JNIEnv, ids: &[Id]) -> jlongArray {
    let flat: Vec<i64> = ids.iter().flat_map(|&id| split_id(id)).collect();
    longs_to_java(env, &flat)
}

/// Strokes the closed lasso polygon, `[x, y, ...]` in page-local document
/// units, mostly encloses, in drawing order. Only the active layer is
/// searched, and not at all while it is hidden or locked.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSelect(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    polygon: JFloatArray,
) -> jlongArray {
    let flat = read_floats(&mut env, &polygon);
    let lasso: Vec<(f32, f32)> = flat
        .as_chunks::<2>()
        .0
        .iter()
        .map(|&[x, y]| (x, y))
        .collect();
    let ids = if page < 0 {
        Vec::new()
    } else {
        unsafe { notebook(handle) }.select(page as usize, &lasso)
    };
    ids_to_java(&mut env, &ids)
}

/// Bounds of the strokes' drawn ink as `[minX, minY, maxX, maxY]`, or an
/// empty array if none of them exist.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSelectionBounds(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
) -> jfloatArray {
    let ids = read_ids(&mut env, &ids);
    let bounds = if page < 0 {
        None
    } else {
        unsafe { notebook(handle) }.ink_bounds(page as usize, &ids)
    };
    let out = bounds.map_or_else(Vec::new, |r| vec![r.min_x, r.min_y, r.max_x, r.max_y]);
    floats_to_java(&mut env, &out)
}

/// The whole-point width most of the strokes are drawn at, or `0.0` if none
/// of them has one (fills and images have no width).
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeCommonWidth(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
) -> jfloat {
    let ids = read_ids(&mut env, &ids);
    if page < 0 {
        return 0.0;
    }
    unsafe { notebook(handle) }
        .common_width(page as usize, &ids)
        .unwrap_or(0.0)
}

/// Replaces the strokes with copies transformed by the matrix
/// `[sx, kx, tx, ky, sy, ty]` (Android `Matrix` order). Returns the new ids
/// in the order given, or an empty array if the matrix is not six values.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeTransform(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    matrix: JFloatArray,
) -> jlongArray {
    let ids = read_ids(&mut env, &ids);
    let m = read_floats(&mut env, &matrix);
    let out = match <[f32; 6]>::try_from(m.as_slice()) {
        Ok(m) if page >= 0 => unsafe { notebook(handle) }.transform(page as usize, &ids, m),
        _ => Vec::new(),
    };
    ids_to_java(&mut env, &out)
}

/// Replaces the strokes with copies in an ARGB colour, each keeping its own
/// alpha. Returns the new ids in the order given.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeRecolor(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    color: jint,
) -> jlongArray {
    let ids = read_ids(&mut env, &ids);
    let out = if page < 0 {
        Vec::new()
    } else {
        unsafe { notebook(handle) }.recolor(page as usize, &ids, rgba_from_argb(color))
    };
    ids_to_java(&mut env, &out)
}

/// Replaces the strokes with copies drawn `width` points wide. Returns the
/// new ids in the order given.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetWidth(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    width: jfloat,
) -> jlongArray {
    let ids = read_ids(&mut env, &ids);
    let out = if page < 0 {
        Vec::new()
    } else {
        unsafe { notebook(handle) }.set_width(page as usize, &ids, width)
    };
    ids_to_java(&mut env, &out)
}

/// Moves the strokes and images to the top of their layers (`front`) or to
/// the bottom, as one undo step. Returns false if nothing moved.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeReorder(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    front: jboolean,
) -> jboolean {
    let ids = read_ids(&mut env, &ids);
    if page < 0 {
        return JNI_FALSE;
    }
    let moved = if front != JNI_FALSE {
        unsafe { notebook(handle) }.bring_to_front(page as usize, &ids)
    } else {
        unsafe { notebook(handle) }.send_to_back(page as usize, &ids)
    };
    moved as jboolean
}

/// Removes the strokes. Returns false if there was nothing to remove.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeDelete(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
) -> jboolean {
    let ids = read_ids(&mut env, &ids);
    if page < 0 {
        return JNI_FALSE;
    }
    unsafe { notebook(handle) }.delete(page as usize, &ids) as jboolean
}

/// Copies the strokes to the notebook's clipboard. Returns false if none of
/// them exist.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeCopy(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
) -> jboolean {
    let ids = read_ids(&mut env, &ids);
    if page < 0 {
        return JNI_FALSE;
    }
    unsafe { notebook(handle) }.copy(page as usize, &ids) as jboolean
}

/// Whether the clipboard holds strokes to paste.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeHasClipboard(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    unsafe { notebook(handle) }.has_clipboard() as jboolean
}

/// Pastes the clipboard onto the page's active layer, centred on `(x, y)` in
/// page-local units. Returns the new ids, empty if nothing was pasted.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativePaste(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    x: jfloat,
    y: jfloat,
) -> jlongArray {
    let out = if page < 0 {
        Vec::new()
    } else {
        unsafe { notebook(handle) }.paste(page as usize, x, y)
    };
    ids_to_java(&mut env, &out)
}

/// Copies the strokes onto the page's active layer, offset by `(dx, dy)`.
/// Returns the new ids, empty if nothing was added.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeDuplicate(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    dx: jfloat,
    dy: jfloat,
) -> jlongArray {
    let ids = read_ids(&mut env, &ids);
    let out = if page < 0 {
        Vec::new()
    } else {
        unsafe { notebook(handle) }.duplicate(page as usize, &ids, dx, dy)
    };
    ids_to_java(&mut env, &out)
}
