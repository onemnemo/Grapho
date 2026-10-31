//! JNI entry points for library packages on `NativeLibrary`: many notebooks
//! and folders in one file (see `grapho_core::library::Library::export_package`).
//!
//! The notebook files live in one folder that the shell names. Arguments
//! arrive as a JSON object and the report goes back as JSON:
//! `{"folders":n,"items":n,"skipped":n,"fileRefs":[...]}`. A failure is thrown
//! as `java.io.IOException` whose message is the error's short code, a colon
//! and a sentence, as for the other library calls.

use std::fs::{File, OpenOptions};
use std::io::{self, BufReader, BufWriter, Read};
use std::path::{Path, PathBuf};
use std::time::{Duration, UNIX_EPOCH};

use grapho_core::document;
use grapho_core::library::{is_package, Id, PackageError, PackageReport, Shelf};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use serde_json::{json, Value};

use crate::library::library;
use crate::pdf::guarded;
use crate::{read_string, string_to_java};

/// The notebook folder of the app. New files get a random UUID name, as the
/// shell's own files do.
struct DirShelf {
    dir: PathBuf,
}

impl DirShelf {
    fn path(&self, file_ref: &str) -> io::Result<PathBuf> {
        let plain = Path::new(file_ref)
            .file_name()
            .is_some_and(|n| n == file_ref);
        if !plain {
            return Err(io::Error::new(io::ErrorKind::InvalidInput, "bad file name"));
        }
        Ok(self.dir.join(file_ref))
    }
}

impl Shelf for DirShelf {
    type File = BufReader<File>;

    fn open(&mut self, file_ref: &str) -> io::Result<Self::File> {
        Ok(BufReader::new(File::open(self.path(file_ref)?)?))
    }

    fn add(&mut self, data: &mut dyn Read, modified_at: i64) -> io::Result<String> {
        let name = format!("{}.grapho", document::Id::new_v4());
        let target = self.path(&name)?;
        // Written under a name the library ignores, then renamed, so a crash
        // never leaves half a notebook that looks whole.
        let temp = self.dir.join(format!("{name}.part"));
        let written = (|| {
            let file = OpenOptions::new()
                .write(true)
                .create_new(true)
                .open(&temp)?;
            let mut out = BufWriter::new(file);
            io::copy(data, &mut out)?;
            let file = out.into_inner().map_err(io::IntoInnerError::into_error)?;
            if let Ok(ms) = u64::try_from(modified_at) {
                let _ = file.set_modified(UNIX_EPOCH + Duration::from_millis(ms));
            }
            file.sync_all()?;
            std::fs::rename(&temp, &target)
        })();
        if let Err(e) = written {
            let _ = std::fs::remove_file(&temp);
            return Err(e);
        }
        Ok(name)
    }

    fn remove(&mut self, file_ref: &str) {
        if let Ok(path) = self.path(file_ref) {
            let _ = std::fs::remove_file(path);
        }
    }
}

fn describe(e: &PackageError) -> String {
    format!("{}: {e}", e.kind())
}

fn bad(why: &str) -> String {
    format!("bad_request: bad request: {why}")
}

fn text<'a>(args: &'a Value, key: &str) -> Result<&'a str, String> {
    args.get(key)
        .and_then(Value::as_str)
        .ok_or_else(|| bad(key))
}

fn ids(args: &Value, key: &str) -> Result<Vec<Id>, String> {
    match args.get(key) {
        None | Some(Value::Null) => Ok(Vec::new()),
        Some(Value::Array(list)) => list
            .iter()
            .map(|v| v.as_str().and_then(Id::parse).ok_or_else(|| bad(key)))
            .collect(),
        Some(_) => Err(bad(key)),
    }
}

fn report(r: &PackageReport) -> String {
    json!({
        "folders": r.folders,
        "items": r.items,
        "skipped": r.skipped,
        "fileRefs": r.file_refs,
    })
    .to_string()
}

fn parse(args: &str) -> Result<Value, String> {
    serde_json::from_str(args).map_err(|e| bad(&e.to_string()))
}

/// Writes the picked folders and items as one package. `args`:
/// `{"folderIds":[...],"itemIds":[...],"dir":"<notebooks>","out":"<file>","now":ms}`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeExportPackage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    args: JString,
) -> jstring {
    let args = read_string(&mut env, &args);
    let text_out = guarded(&mut env, None, |_| {
        let a = parse(&args)?;
        let folders = ids(&a, "folderIds")?;
        let items = ids(&a, "itemIds")?;
        let mut shelf = DirShelf {
            dir: PathBuf::from(text(&a, "dir")?),
        };
        let now = a
            .get("now")
            .and_then(Value::as_i64)
            .ok_or_else(|| bad("now"))?;
        let out = File::create(text(&a, "out")?).map_err(|e| describe(&e.into()))?;
        let lib = unsafe { library(handle) };
        lib.export_package(&folders, &items, &mut shelf, BufWriter::new(out), now)
            .map(|r| Some(report(&r)))
            .map_err(|e| describe(&e))
    });
    match text_out {
        Some(t) => string_to_java(&mut env, &t),
        None => std::ptr::null_mut(),
    }
}

/// Adds the package at `path` to the library. `args`:
/// `{"path":"<file>","dir":"<notebooks>","folderId":id or null,"now":ms}`.
/// The shell saves the library afterwards.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeImportPackage(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    args: JString,
) -> jstring {
    let args = read_string(&mut env, &args);
    let text_out = guarded(&mut env, None, |_| {
        let a = parse(&args)?;
        let into = match a.get("folderId") {
            None | Some(Value::Null) => None,
            Some(v) => Some(
                v.as_str()
                    .and_then(Id::parse)
                    .ok_or_else(|| bad("folderId"))?,
            ),
        };
        let mut shelf = DirShelf {
            dir: PathBuf::from(text(&a, "dir")?),
        };
        let now = a
            .get("now")
            .and_then(Value::as_i64)
            .ok_or_else(|| bad("now"))?;
        let input = File::open(text(&a, "path")?).map_err(|e| describe(&e.into()))?;
        let mut lib = unsafe { library(handle) };
        lib.import_package(BufReader::new(input), into, &mut shelf, now)
            .map(|r| Some(report(&r)))
            .map_err(|e| describe(&e))
    });
    match text_out {
        Some(t) => string_to_java(&mut env, &t),
        None => std::ptr::null_mut(),
    }
}

/// Whether the file at `path` is a library package rather than one notebook.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeLibrary_nativeIsPackage(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jboolean {
    let path = read_string(&mut env, &path);
    guarded(&mut env, JNI_FALSE, |_| {
        let yes = File::open(path).is_ok_and(|f| is_package(BufReader::new(f)));
        Ok(if yes { JNI_TRUE } else { JNI_FALSE })
    })
}
