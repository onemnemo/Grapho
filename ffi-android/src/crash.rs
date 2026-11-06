//! Writes a native panic to a file in the app's crash folder, so the app can
//! offer it as a report on the next launch. Nothing leaves the device.

use std::backtrace::Backtrace;
use std::fs;
use std::panic;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};

use jni::objects::{JClass, JString};
use jni::JNIEnv;

/// Keeps panics from filling the folder if something panics in a loop.
const MAX_FILES: usize = 5;

fn count_files(dir: &PathBuf) -> usize {
    fs::read_dir(dir).map(|d| d.count()).unwrap_or(0)
}

fn write_report(dir: &PathBuf, info: &panic::PanicHookInfo) {
    if count_files(dir) >= MAX_FILES {
        return;
    }
    let ms = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis())
        .unwrap_or(0);
    let place = info
        .location()
        .map(|l| format!("{}:{}", l.file(), l.line()))
        .unwrap_or_else(|| "unknown".to_string());
    let message = if let Some(s) = info.payload().downcast_ref::<&str>() {
        (*s).to_string()
    } else if let Some(s) = info.payload().downcast_ref::<String>() {
        s.clone()
    } else {
        "no message".to_string()
    };
    let text = format!(
        "Native panic at {place}\n{message}\n\n{}\n",
        Backtrace::force_capture()
    );
    let _ = fs::write(dir.join(format!("native-{ms}.txt")), text);
}

/// Called once at startup with the crash folder. Panics are written there
/// before the previous hook runs.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeCrash_nativeInstall(
    mut env: JNIEnv,
    _class: JClass,
    dir: JString,
) {
    let Ok(dir) = env.get_string(&dir) else {
        return;
    };
    let dir = PathBuf::from(String::from(dir));
    let _ = fs::create_dir_all(&dir);
    let previous = panic::take_hook();
    panic::set_hook(Box::new(move |info| {
        write_report(&dir, info);
        previous(info);
    }));
}
