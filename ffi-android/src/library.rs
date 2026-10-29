//! JNI entry points for `NativeLibrary`: the library of folders, items and
//! Trash that grapho-core owns. The shell holds a handle, saves the JSON the
//! core hands back, and sends every operation as an operation name with a JSON
//! object of arguments (see `grapho_core::library::call`).
//!
//! Failures are thrown as `java.io.IOException`. For a library error the
//! message is the error's short code (`not_found`, `into_descendant` and so
//! on), then a colon and a sentence, so the shell can match on the code.

use std::sync::{Mutex, MutexGuard};

use grapho_core::library::{call, Library, LibraryError};
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use jni::JNIEnv;

use crate::pdf::guarded;
use crate::{read_string, string_to_java};

type Shared = Mutex<Library>;

/// # Safety
/// `handle` must come from `nativeNew` or `nativeFromJson` and not be freed.
unsafe fn library<'a>(handle: jlong) -> MutexGuard<'a, Library> {
    // SAFETY: the caller guarantees `handle` is a live library.
    let m = unsafe { &*(handle as *const Shared) };
    m.lock().unwrap_or_else(|e| e.into_inner())
}

fn describe(e: &LibraryError) -> String {
    format!("{}: {e}", e.kind())
}

fn into_handle(lib: Library) -> jlong {
    Box::into_raw(Box::new(Mutex::new(lib))) as jlong
}

/// Makes an empty library and returns its handle, freed with `nativeFree`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeNew(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    guarded(&mut env, 0, |_| Ok(into_handle(Library::new())))
}

/// Reads a library from its JSON and returns its handle, freed with
/// `nativeFree`. Throws for damaged JSON or a version this build cannot read.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeFromJson(
    mut env: JNIEnv,
    _class: JClass,
    json: JString,
) -> jlong {
    let json = read_string(&mut env, &json);
    guarded(&mut env, 0, |_| {
        Library::from_json(&json)
            .map(into_handle)
            .map_err(|e| describe(&e))
    })
}

/// The library as JSON, to be written to disk by the shell.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeToJson(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let text = guarded(&mut env, None, |_| {
        Ok(Some(unsafe { library(handle) }.to_json()))
    });
    match text {
        Some(text) => string_to_java(&mut env, &text),
        None => std::ptr::null_mut(),
    }
}

/// Runs one operation and returns its JSON result. See
/// `grapho_core::library::call` for the operations and their arguments.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeCall(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    op: JString,
    args: JString,
) -> jstring {
    let op = read_string(&mut env, &op);
    let args = read_string(&mut env, &args);
    let text = guarded(&mut env, None, |_| {
        let mut lib = unsafe { library(handle) };
        call(&mut lib, &op, &args)
            .map(Some)
            .map_err(|e| describe(&e))
    });
    match text {
        Some(text) => string_to_java(&mut env, &text),
        None => std::ptr::null_mut(),
    }
}

/// Frees a library handle. The handle must not be used afterwards.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeFree(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        // SAFETY: the handle came from `nativeNew` or `nativeFromJson` and the
        // caller frees it once.
        drop(unsafe { Box::from_raw(handle as *mut Shared) });
    }
}
