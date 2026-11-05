//! JNI entry point for exporting a notebook as a vector PDF.
//!
//! Failures are thrown as `java.io.IOException` with a message fit to show.
//! A panic anywhere inside is caught and thrown the same way.

use std::fs::File;
use std::io::Write;

use std::collections::HashMap;

use grapho_core::document::Id;
use grapho_core::export::{export_pdf, ExportOptions};
use jni::objects::{JClass, JFloatArray, JIntArray, JLongArray, JObjectArray, JString};
use jni::sys::{jboolean, jlong, JNI_FALSE};
use jni::JNIEnv;

use crate::pdf::guarded;
use crate::{join_id, notebook, read_string};

/// Reads a Java `int[]`, or `None` for null.
fn read_ints(env: &mut JNIEnv, arr: &JIntArray) -> Result<Option<Vec<i32>>, String> {
    if arr.is_null() {
        return Ok(None);
    }
    let len = env
        .get_array_length(arr)
        .map_err(|e| format!("couldn't read the page list: {e}"))? as usize;
    let mut buf = vec![0i32; len];
    env.get_int_array_region(arr, 0, &mut buf)
        .map_err(|e| format!("couldn't read the page list: {e}"))?;
    Ok(Some(buf))
}

/// Reads the text outlines: `ids` holds a high and a low half per box and
/// `shapes` one packed polygon list per box. Both may be null for none.
fn read_texts(
    env: &mut JNIEnv,
    ids: &JLongArray,
    shapes: &JObjectArray,
) -> Result<HashMap<Id, Vec<f32>>, String> {
    let mut out = HashMap::new();
    if ids.is_null() || shapes.is_null() {
        return Ok(out);
    }
    let fail = |e: jni::errors::Error| format!("couldn't read the text outlines: {e}");
    let len = env.get_array_length(ids).map_err(fail)? as usize;
    let mut halves = vec![0i64; len];
    env.get_long_array_region(ids, 0, &mut halves)
        .map_err(fail)?;
    for (i, pair) in halves.chunks_exact(2).enumerate() {
        let item = env
            .get_object_array_element(shapes, i as i32)
            .map_err(fail)?;
        let arr = JFloatArray::from(item);
        let n = env.get_array_length(&arr).map_err(fail)? as usize;
        let mut buf = vec![0f32; n];
        env.get_float_array_region(&arr, 0, &mut buf)
            .map_err(fail)?;
        out.insert(join_id(pair[0], pair[1]), buf);
    }
    Ok(out)
}

/// Writes the notebook to `out_path` as a PDF. With `backgrounds` the paper
/// colour, its pattern and the original content of PDF pages are kept;
/// without it every page is white with ink only. `pages` lists the page
/// indices to export, or is null for all of them. `text_ids` and
/// `text_shapes` carry the outlines of the text boxes, see [`read_texts`].
///
/// The document is copied under the lock and exported after it is released,
/// so drawing never waits on the export. The data goes to a temporary file
/// first, so a file at `out_path` is never seen half written.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeExportPdf(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    out_path: JString,
    backgrounds: jboolean,
    pages: JIntArray,
    text_ids: JLongArray,
    text_shapes: JObjectArray,
) {
    guarded(&mut env, (), |env| {
        let out = read_string(env, &out_path);
        let pages = read_ints(env, &pages)?
            .map(|list| {
                list.into_iter()
                    .map(|i| usize::try_from(i).map_err(|_| "that page doesn't exist".to_string()))
                    .collect::<Result<Vec<_>, _>>()
            })
            .transpose()?;
        let options = ExportOptions {
            pages,
            backgrounds: backgrounds != JNI_FALSE,
            texts: read_texts(env, &text_ids, &text_shapes)?,
        };
        let doc = unsafe { notebook(handle) }.document().clone();
        let bytes = export_pdf(&doc, &options).map_err(|e| e.to_string())?;
        let tmp = format!("{out}.tmp");
        let result = (|| -> Result<(), String> {
            let mut file = File::create(&tmp).map_err(|e| e.to_string())?;
            file.write_all(&bytes).map_err(|e| e.to_string())?;
            file.sync_all().map_err(|e| e.to_string())?;
            std::fs::rename(&tmp, &out).map_err(|e| e.to_string())
        })();
        result.map_err(|msg| {
            let _ = std::fs::remove_file(&tmp);
            format!("couldn't write the PDF: {msg}")
        })
    })
}
