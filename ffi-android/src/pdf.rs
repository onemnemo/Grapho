//! JNI entry points for PDF pages on `NativeNotebook`: importing a PDF as a
//! new notebook, inserting blank or PDF pages, and handing a note's embedded
//! PDF back to the shell as a file it can render from.
//!
//! Failures are thrown as `java.io.IOException` with a message fit to show.
//! A panic anywhere inside is caught and thrown the same way, never unwound
//! into the JVM.

use std::fs::File;
use std::io::Write;
use std::panic::{catch_unwind, AssertUnwindSafe};

use grapho_core::document::{Document, Id, PdfImport};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong};
use jni::JNIEnv;

use crate::{into_handle, notebook, read_string, throw_io};

/// Runs `body`, throwing its error (or a caught panic) as an `IOException`
/// and returning `fallback` instead.
pub(crate) fn guarded<T>(
    env: &mut JNIEnv,
    fallback: T,
    body: impl FnOnce(&mut JNIEnv) -> Result<T, String>,
) -> T {
    match catch_unwind(AssertUnwindSafe(|| body(env))) {
        Ok(Ok(value)) => value,
        Ok(Err(message)) => {
            throw_io(env, &message);
            fallback
        }
        Err(_) => {
            throw_io(env, "something went wrong while handling the request");
            fallback
        }
    }
}

fn read_file(path: &str) -> Result<Vec<u8>, String> {
    std::fs::read(path).map_err(|e| format!("couldn't read the PDF: {e}"))
}

/// Creates a new paged notebook with one page per page of the PDF at
/// `pdf_path`, and returns its handle (freed with `nativeFree`, like
/// `nativeCreate`). `name` is the original file name. Throws for a PDF that
/// is password protected, damaged or has no pages.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeImportPdf(
    mut env: JNIEnv,
    _class: JClass,
    pdf_path: JString,
    title: JString,
    name: JString,
) -> jlong {
    guarded(&mut env, 0, |env| {
        let path = read_string(env, &pdf_path);
        let title = read_string(env, &title);
        let name = read_string(env, &name);
        let bytes = read_file(&path)?;
        let doc = Document::from_pdf(title, &name, bytes).map_err(|e| e.to_string())?;
        Ok(into_handle(doc))
    })
}

/// Writes the embedded PDF `pdf_id` (the `id` of an entry of `pdfs` in
/// `nativeInfo`) to `out_path`. The data goes to a temporary file first, so
/// a file that exists at `out_path` is never seen half written. Throws if the
/// notebook has no such PDF.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeWritePdf(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    pdf_id: JString,
    out_path: JString,
) {
    guarded(&mut env, (), |env| {
        let id = read_string(env, &pdf_id);
        let out = read_string(env, &out_path);
        let id: Id = id
            .parse()
            .map_err(|_| "that isn't a PDF id of this notebook".to_string())?;
        // Share the bytes under the lock and write them after it is released.
        let bytes = unsafe { notebook(handle) }
            .document()
            .pdfs
            .iter()
            .find(|s| s.id == id)
            .map(|s| s.bytes.clone())
            .ok_or_else(|| "this notebook doesn't hold that PDF".to_string())?;
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

/// Inserts an empty page at index `at` (`0` to the page count) as one undo
/// step. It takes the size of the page before it, or after it when `at` is
/// 0. Returns the new page's index, or -1 for an infinite canvas or an
/// invalid `at`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeInsertPage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    at: jint,
) -> jint {
    guarded(&mut env, -1, |_| {
        let Ok(at) = usize::try_from(at) else {
            return Ok(-1);
        };
        let mut e = unsafe { notebook(handle) };
        if at > e.document().pages.len() {
            return Ok(-1);
        }
        Ok(e.insert_page(at).map_or(-1, |i| i as jint))
    })
}

/// Inserts every page of the PDF at `pdf_path` at index `at` (`0` to the page
/// count) as one undo step, and returns how many pages were added. `name` is
/// the original file name. Throws for a PDF that is password protected,
/// damaged or has no pages, and for an infinite canvas or an invalid `at`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeInsertPdf(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    at: jint,
    pdf_path: JString,
    name: JString,
) -> jint {
    guarded(&mut env, 0, |env| {
        let path = read_string(env, &pdf_path);
        let name = read_string(env, &name);
        let at = usize::try_from(at).map_err(|_| "that page position is invalid".to_string())?;
        // Parse before taking the lock; that is the slow part.
        let import = PdfImport::read(&name, read_file(&path)?).map_err(|e| e.to_string())?;
        let mut e = unsafe { notebook(handle) };
        if at > e.document().pages.len() {
            return Err("that page position is invalid".into());
        }
        let count = e.insert_pdf_import(at, import).map_err(|e| e.to_string())?;
        Ok(count as jint)
    })
}
