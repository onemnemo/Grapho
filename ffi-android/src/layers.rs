//! JNI entry points for layers on `NativeNotebook`: listing, the active
//! layer, adding, removing, moving, merging and the per-layer properties.
//! Layers are addressed by page index and layer index, bottom to top.

use grapho_core::document::Tool;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jint, jlong, jlongArray, jstring, JNI_FALSE};
use jni::JNIEnv;
use serde_json::json;

use crate::{longs_to_java, notebook, read_string, split_id, string_to_java};

/// The page's layers, bottom to top, as JSON:
/// `{"active":1,"layers":[{"name":"Layer 1","visible":true,"locked":false,
/// "opacity":1.0,"strokes":12}, ...]}`. An unknown page gives an empty string.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeLayers(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
) -> jstring {
    let text = {
        let e = unsafe { notebook(handle) };
        let index = usize::try_from(page).ok();
        match index.and_then(|i| Some((i, e.document().pages.get(i)?))) {
            Some((i, p)) => {
                let layers: Vec<_> = p
                    .layers
                    .iter()
                    .map(|l| {
                        json!({
                            "name": l.name,
                            "visible": l.visible,
                            "locked": l.locked,
                            "opacity": l.opacity,
                            "strokes": l.strokes.len(),
                        })
                    })
                    .collect();
                json!({ "active": e.active_layer(i), "layers": layers }).to_string()
            }
            None => String::new(),
        }
    };
    string_to_java(&mut env, &text)
}

/// Makes a layer the one that drawing, filling, pasting, erasing and
/// selecting work on. False if the page or layer doesn't exist.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetActiveLayer(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => unsafe { notebook(handle) }.set_active_layer(p, i) as jboolean,
        _ => JNI_FALSE,
    }
}

/// Adds an empty layer above the active one and makes it active. Returns its
/// index, or -1 at the layer limit or for an unknown page.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeAddLayer(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    name: JString,
) -> jint {
    let name = read_string(&mut env, &name);
    let Ok(p) = usize::try_from(page) else {
        return -1;
    };
    unsafe { notebook(handle) }
        .add_layer(p, &name)
        .map_or(-1, |i| i as jint)
}

/// Removes a layer and its strokes, as one undo step. False for the last
/// layer of a page.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeRemoveLayer(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => unsafe { notebook(handle) }.remove_layer(p, i) as jboolean,
        _ => JNI_FALSE,
    }
}

/// Moves a layer from one position to another, as one undo step.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeMoveLayer(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    from: jint,
    to: jint,
) -> jboolean {
    match (
        usize::try_from(page),
        usize::try_from(from),
        usize::try_from(to),
    ) {
        (Ok(p), Ok(f), Ok(t)) => unsafe { notebook(handle) }.move_layer(p, f, t) as jboolean,
        _ => JNI_FALSE,
    }
}

/// Merges a layer onto the one below it, as one undo step. False for the
/// bottom layer and when either layer is locked.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeMergeDown(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => unsafe { notebook(handle) }.merge_down(p, i) as jboolean,
        _ => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeRenameLayer(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
    name: JString,
) -> jboolean {
    let name = read_string(&mut env, &name);
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => unsafe { notebook(handle) }.rename_layer(p, i, &name) as jboolean,
        _ => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetLayerVisible(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
    visible: jboolean,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => {
            unsafe { notebook(handle) }.set_layer_visible(p, i, visible != JNI_FALSE) as jboolean
        }
        _ => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetLayerLocked(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
    locked: jboolean,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => {
            unsafe { notebook(handle) }.set_layer_locked(p, i, locked != JNI_FALSE) as jboolean
        }
        _ => JNI_FALSE,
    }
}

/// Sets a layer's opacity, clamped to 0 to 1.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetLayerOpacity(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    index: jint,
    opacity: jfloat,
) -> jboolean {
    match (usize::try_from(page), usize::try_from(index)) {
        (Ok(p), Ok(i)) => unsafe { notebook(handle) }.set_layer_opacity(p, i, opacity) as jboolean,
        _ => JNI_FALSE,
    }
}

/// Stroke ids of the visible layers of a page, bottom to top. For each
/// layer: `[alpha, n, <n ids as hi, lo pairs>]`, where alpha is the layer's
/// opacity as 0 to 255. Within a layer the fills come first, so they sit
/// beneath its ink.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeLayerStrokeIds(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
) -> jlongArray {
    let mut out: Vec<i64> = Vec::new();
    {
        let e = unsafe { notebook(handle) };
        let page = usize::try_from(page)
            .ok()
            .and_then(|i| e.document().pages.get(i));
        for l in page
            .into_iter()
            .flat_map(|p| &p.layers)
            .filter(|l| l.visible)
        {
            fn is_fill(s: &&grapho_core::document::Stroke) -> bool {
                s.style.tool == Tool::Fill
            }
            out.push((l.opacity.clamp(0.0, 1.0) * 255.0).round() as i64);
            out.push(l.strokes.len() as i64);
            let fills = l.strokes.iter().filter(is_fill);
            let ink = l.strokes.iter().filter(|s| !is_fill(s));
            out.extend(fills.chain(ink).flat_map(|s| split_id(s.id)));
        }
    }
    longs_to_java(&mut env, &out)
}
