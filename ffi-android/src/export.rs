//! JNI entry point for exporting a notebook as a vector PDF.
//!
//! Failures are thrown as `java.io.IOException` with a message fit to show.
//! A panic anywhere inside is caught and thrown the same way.

use std::fs::File;
use std::io::Write;

use grapho_core::export::{export_pdf, ExportOptions};
use jni::objects::{JClass, JIntArray, JString};
use jni::sys::{jboolean, jlong, JNI_FALSE};
use jni::JNIEnv;

use crate::pdf::guarded;
use crate::{notebook, read_string};

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

/// Writes the notebook to `out_path` as a PDF. With `backgrounds` the paper
/// colour, its pattern and the original content of PDF pages are kept;
/// without it every page is white with ink only. `pages` lists the page
/// indices to export, or is null for all of them.
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
