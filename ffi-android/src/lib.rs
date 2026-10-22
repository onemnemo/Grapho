//! JNI entry points for the Android app's `com.onemnemo.grapho.engine`
//! package: `NativeInk` (the stroke being drawn), `NativeNotebook` (an
//! open document with its undo history) and `NativeLibrary` (the library of
//! folders and items).
//!
//! Bulk data crosses the boundary as packed float arrays, so each call is
//! one array copy each way. Stroke ids cross as pairs of longs (the UUID's
//! high and low halves). Errors that Kotlin should show are thrown as
//! `java.io.IOException` with a readable message.

mod diagram;
mod export;
mod guide;
mod images;
mod layers;
mod library;
mod pdf;
mod select;
mod shapes;
mod texts;

use std::fs::File;
use std::io::{BufReader, BufWriter, Write};
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::{Mutex, MutexGuard};

use grapho_core::document::{
    self, Background, Cover, Document, Editor, EraseMode, FillView, Id, Orientation, PaperSize,
    Pattern, Stroke, StrokeStyle, Tool,
};
use grapho_core::shape::Shape;
use grapho_core::LineStyle;
use grapho_core::{
    outline_pieces_styled, outline_styled, Brush, InputSample, Rect, Snapped, StrokeBuilder,
};
use jni::objects::{JClass, JFloatArray, JLongArray, JString};
use jni::sys::{jboolean, jfloat, jfloatArray, jint, jlong, jlongArray, jstring, JNI_FALSE};
use jni::JNIEnv;

pub(crate) fn read_floats(env: &mut JNIEnv, arr: &JFloatArray) -> Vec<f32> {
    if arr.is_null() {
        return Vec::new();
    }
    let len = env.get_array_length(arr).unwrap_or(0) as usize;
    let mut buf = vec![0.0f32; len];
    if env.get_float_array_region(arr, 0, &mut buf).is_err() {
        return Vec::new();
    }
    buf
}

pub(crate) fn read_samples(env: &mut JNIEnv, arr: &JFloatArray) -> Vec<InputSample> {
    InputSample::unpack(&read_floats(env, arr)).collect()
}

pub(crate) fn read_longs(env: &mut JNIEnv, arr: &JLongArray) -> Vec<i64> {
    if arr.is_null() {
        return Vec::new();
    }
    let len = env.get_array_length(arr).unwrap_or(0) as usize;
    let mut buf = vec![0i64; len];
    if env.get_long_array_region(arr, 0, &mut buf).is_err() {
        return Vec::new();
    }
    buf
}

pub(crate) fn floats_to_java(env: &mut JNIEnv, data: &[f32]) -> jfloatArray {
    match env.new_float_array(data.len() as i32) {
        Ok(out) => {
            if env.set_float_array_region(&out, 0, data).is_err() {
                return std::ptr::null_mut();
            }
            out.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

pub(crate) fn longs_to_java(env: &mut JNIEnv, data: &[i64]) -> jlongArray {
    match env.new_long_array(data.len() as i32) {
        Ok(out) => {
            if env.set_long_array_region(&out, 0, data).is_err() {
                return std::ptr::null_mut();
            }
            out.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

pub(crate) fn string_to_java(env: &mut JNIEnv, s: &str) -> jstring {
    env.new_string(s)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

pub(crate) fn read_string(env: &mut JNIEnv, s: &JString) -> String {
    env.get_string(s).map(Into::into).unwrap_or_default()
}

pub(crate) fn throw_io(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/io/IOException", message);
}

pub(crate) fn split_id(id: Id) -> [i64; 2] {
    let (hi, lo) = id.as_u64_pair();
    [hi as i64, lo as i64]
}

pub(crate) fn join_id(hi: i64, lo: i64) -> Id {
    Id::from_u64_pair(hi as u64, lo as u64)
}

/// Android colours are ARGB; the file format stores RGBA.
pub(crate) fn rgba_from_argb(argb: jint) -> u32 {
    (argb as u32).rotate_left(8)
}

fn tool_from(v: jint) -> Tool {
    match v {
        1 => Tool::Pencil,
        2 => Tool::Highlighter,
        3 => Tool::Fill,
        _ => Tool::Pen,
    }
}

fn line_from(v: jint) -> LineStyle {
    match v {
        1 => LineStyle::Dashed,
        2 => LineStyle::Dotted,
        _ => LineStyle::Solid,
    }
}

/// # Safety
/// `handle` must come from `nativeNew` and not have been finished.
unsafe fn builder<'a>(handle: jlong) -> &'a mut StrokeBuilder {
    // SAFETY: the caller guarantees `handle` is a live StrokeBuilder.
    unsafe { &mut *(handle as *mut StrokeBuilder) }
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeNew(
    _env: JNIEnv,
    _class: JClass,
    width: jfloat,
    stabilizer: jfloat,
    tool: jint,
    line: jint,
) -> jlong {
    let brush = StrokeStyle {
        tool: tool_from(tool),
        color: 0,
        width,
        line: LineStyle::Solid,
    }
    .brush();
    let b = StrokeBuilder::new(brush, stabilizer, line_from(line));
    Box::into_raw(Box::new(b)) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativePush(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    samples: JFloatArray,
) {
    let samples = read_samples(&mut env, &samples);
    let b = unsafe { builder(handle) };
    for s in samples {
        b.push(s);
    }
}

/// Returns `[min_x, min_y, max_x, max_y, n, <n new committed floats>, <tail floats>]`.
/// An empty dirty area has `min > max`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeFrame(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    predicted: JFloatArray,
) -> jfloatArray {
    let predicted = read_samples(&mut env, &predicted);
    let f = unsafe { builder(handle) }.frame(&predicted);
    let mut out = Vec::with_capacity(5 + f.new_committed.len() + f.tail.len());
    out.extend_from_slice(&[
        f.dirty.min_x,
        f.dirty.min_y,
        f.dirty.max_x,
        f.dirty.max_y,
        f.new_committed.len() as f32,
    ]);
    out.extend_from_slice(&f.new_committed);
    out.extend_from_slice(&f.tail);
    floats_to_java(&mut env, &out)
}

/// How long the pen has stayed still, at `now_ms` on the samples' clock.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeStillMs(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    now_ms: jfloat,
) -> jfloat {
    unsafe { builder(handle) }.still_ms(now_ms)
}

/// The stroke snapped to the shape it looks like, as `[line, x0, y0, x1, y1,
/// n, <n point floats packed as x, y, pressure, time_ms>, <mesh floats>]`.
/// `line` is 1 when the shape is a straight line from `(x0, y0)` to
/// `(x1, y1)`, else 0 with zeros after it. Empty if no shape matched.
/// `shaft` is null or `[x0, y0, x1, y1]`, a line drawn just before that the
/// stroke may be the arrow head of. The stroke itself is left unchanged.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeSnap(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    shaft: JFloatArray,
) -> jfloatArray {
    let shaft = match read_floats(&mut env, &shaft)[..] {
        [x0, y0, x1, y1] => Some(([x0, y0], [x1, y1])),
        _ => None,
    };
    let snapped = unsafe { builder(handle) }.snap(shaft);
    floats_to_java(&mut env, &pack_snapped(snapped))
}

/// After `nativeSnap`, reshapes the snapped shape for the pen at `(x, y)`
/// in view pixels. Same layout as `nativeSnap`, empty if nothing snapped.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeReshape(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x: jfloat,
    y: jfloat,
) -> jfloatArray {
    let snapped = unsafe { builder(handle) }.reshape(x, y);
    floats_to_java(&mut env, &pack_snapped(snapped))
}

pub(crate) fn pack_snapped(snapped: Option<Snapped>) -> Vec<f32> {
    let Some(s) = snapped else {
        return Vec::new();
    };
    let line = match s.shape {
        Shape::Line { from, to } => [1.0, from[0], from[1], to[0], to[1]],
        _ => [0.0; 5],
    };
    let n = s.points.len() * InputSample::STRIDE;
    let mut out = Vec::with_capacity(line.len() + 1 + n + s.mesh.len());
    out.extend_from_slice(&line);
    out.push(n as f32);
    out.extend(
        s.points
            .iter()
            .flat_map(|p| [p.x, p.y, p.pressure, p.time_ms]),
    );
    out.extend_from_slice(&s.mesh);
    out
}

/// Ends the stroke, frees the builder and returns its stabilised points,
/// packed as `[x, y, pressure, time_ms, ...]`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeInk_nativeFinish(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jfloatArray {
    let b = unsafe { Box::from_raw(handle as *mut StrokeBuilder) };
    let (points, _) = b.finish();
    let packed: Vec<f32> = points
        .iter()
        .flat_map(|p| [p.x, p.y, p.pressure, p.time_ms])
        .collect();
    floats_to_java(&mut env, &packed)
}

type Notebook = Mutex<Editor>;

/// # Safety
/// `handle` must come from `nativeCreate` or `nativeOpen` and not be freed.
pub(crate) unsafe fn notebook<'a>(handle: jlong) -> MutexGuard<'a, Editor> {
    // SAFETY: the caller guarantees `handle` is a live Notebook.
    let m = unsafe { &*(handle as *const Notebook) };
    m.lock().unwrap_or_else(|e| e.into_inner())
}

pub(crate) fn into_handle(doc: Document) -> jlong {
    Box::into_raw(Box::new(Mutex::new(Editor::new(doc)))) as jlong
}

fn pattern_from(kind: jint, spacing_mm: f32) -> Pattern {
    match kind {
        1 => Pattern::Lined { spacing_mm },
        2 => Pattern::Grid { spacing_mm },
        3 => Pattern::Dotted { spacing_mm },
        _ => Pattern::Blank,
    }
}

fn paper_from(v: jint) -> PaperSize {
    match v {
        1 => PaperSize::A5,
        2 => PaperSize::Letter,
        3 => PaperSize::A3,
        _ => PaperSize::A4,
    }
}

#[no_mangle]
#[allow(clippy::too_many_arguments)]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    title: JString,
    infinite: jboolean,
    paper: jint,
    landscape: jboolean,
    pattern: jint,
    spacing_mm: jfloat,
    background: jint,
    cover: jint,
) -> jlong {
    let title = read_string(&mut env, &title);
    let mut doc = if infinite != JNI_FALSE {
        Document::new_infinite(title)
    } else {
        let orientation = if landscape != JNI_FALSE {
            Orientation::Landscape
        } else {
            Orientation::Portrait
        };
        Document::new_paged(title, paper_from(paper), orientation)
    };
    doc.canvas.set_background(Background {
        color: rgba_from_argb(background),
        pattern: pattern_from(pattern, spacing_mm),
    });
    doc.cover = Some(Cover {
        color: rgba_from_argb(cover),
    });
    into_handle(doc)
}

fn open_file(path: &str) -> Result<File, String> {
    File::open(path).map_err(|e| format!("couldn't open the file: {e}"))
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeOpen(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jlong {
    let path = read_string(&mut env, &path);
    let result =
        open_file(&path).and_then(|f| document::load(BufReader::new(f)).map_err(|e| e.to_string()));
    match result {
        Ok(doc) => into_handle(doc),
        Err(msg) => {
            throw_io(&mut env, &msg);
            0
        }
    }
}

/// The document's structure as JSON, without any ink. Used to list notebooks.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeReadInfo(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jstring {
    let path = read_string(&mut env, &path);
    let result = open_file(&path).and_then(|f| {
        let doc = document::load_info(BufReader::new(f)).map_err(|e| e.to_string())?;
        serde_json::to_string(&doc).map_err(|e| e.to_string())
    });
    match result {
        Ok(json) => string_to_java(&mut env, &json),
        Err(msg) => {
            throw_io(&mut env, &msg);
            std::ptr::null_mut()
        }
    }
}

/// Writes the document to `path`. The data goes to a temporary file first and
/// replaces the old file only once it is complete, so a crash or full disk
/// mid-save never leaves a half-written notebook.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSave(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    path: JString,
) {
    let path = read_string(&mut env, &path);
    // Copy under the lock and write without it, so drawing never waits on disk.
    let doc = unsafe { notebook(handle) }.document().clone();
    let tmp = format!("{path}.tmp");
    let result = (|| -> Result<(), String> {
        let file = File::create(&tmp).map_err(|e| e.to_string())?;
        let mut w = BufWriter::new(file);
        document::save(&doc, &mut w).map_err(|e| e.to_string())?;
        w.flush().map_err(|e| e.to_string())?;
        w.get_ref().sync_all().map_err(|e| e.to_string())?;
        std::fs::rename(&tmp, &path).map_err(|e| e.to_string())
    })();
    if let Err(msg) = result {
        let _ = std::fs::remove_file(&tmp);
        throw_io(&mut env, &format!("couldn't save the notebook: {msg}"));
    }
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeFree(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    drop(unsafe { Box::from_raw(handle as *mut Notebook) });
}

/// The document's structure as JSON (as in document.json), plus `can_undo`
/// and `can_redo`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeInfo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let json = {
        let e = unsafe { notebook(handle) };
        let mut v = serde_json::to_value(e.document()).unwrap_or_default();
        if let Some(obj) = v.as_object_mut() {
            obj.insert("can_undo".into(), e.can_undo().into());
            obj.insert("can_redo".into(), e.can_redo().into());
        }
        v.to_string()
    };
    string_to_java(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetTitle(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    title: JString,
) {
    let title = read_string(&mut env, &title);
    unsafe { notebook(handle) }.document_mut().title = title;
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeSetBackground(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    pattern: jint,
    spacing_mm: jfloat,
    color: jint,
) {
    unsafe { notebook(handle) }
        .document_mut()
        .canvas
        .set_background(Background {
            color: rgba_from_argb(color),
            pattern: pattern_from(pattern, spacing_mm),
        });
}

/// Adds a stroke to the active layer of a page. `points` are in document
/// units, packed as `[x, y, pressure, time_ms, ...]`. Returns false if the
/// layer is locked or hidden or the page doesn't exist.
#[no_mangle]
#[allow(clippy::too_many_arguments)]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeAddStroke(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    tool: jint,
    color: jint,
    width: jfloat,
    line: jint,
    started_at_ms: jlong,
    points: JFloatArray,
) -> jboolean {
    let points = read_samples(&mut env, &points);
    if points.is_empty() || page < 0 {
        return JNI_FALSE;
    }
    let style = StrokeStyle {
        tool: tool_from(tool),
        color: rgba_from_argb(color),
        width,
        line: line_from(line),
    };
    let stroke = Stroke::new(style, started_at_ms, points);
    unsafe { notebook(handle) }
        .add_stroke(page as usize, stroke)
        .is_some() as jboolean
}

/// Fills the area enclosed by ink around `(x, y)`, in page-local document
/// units, with an ARGB colour. Returns false if the tap isn't inside an
/// enclosed area (or the page can't take it). `scale` is screen pixels per
/// document unit and the visible rectangle is in the same page-local units.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeFill(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    x: jfloat,
    y: jfloat,
    color: jint,
    scale: jfloat,
    visible_left: jfloat,
    visible_top: jfloat,
    visible_right: jfloat,
    visible_bottom: jfloat,
) -> jboolean {
    if page < 0 {
        return JNI_FALSE;
    }
    let view = FillView {
        scale,
        visible: (visible_left, visible_top, visible_right, visible_bottom),
    };
    unsafe { notebook(handle) }
        .fill(page as usize, x, y, rgba_from_argb(color), view)
        .is_some() as jboolean
}

/// Adds a page at the end. Returns its index, or -1 for an infinite canvas.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeAddPage(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    unsafe { notebook(handle) }
        .add_page()
        .map_or(-1, |i| i as jint)
}

/// Starts an eraser gesture. `area` erases only the part the eraser passes
/// over, otherwise whole strokes go.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeBeginErase(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    area: jboolean,
) {
    if page >= 0 {
        let mode = if area != JNI_FALSE {
            EraseMode::Area
        } else {
            EraseMode::Stroke
        };
        unsafe { notebook(handle) }.begin_erase(page as usize, mode);
    }
}

/// Erases along a polyline of `[x, y, ...]` pairs in document units, one segment per consecutive
/// pair, under a single lock. Returns how many strokes were removed or cut.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeErasePath(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    points: JFloatArray,
    radius: jfloat,
) -> jint {
    let pts = read_floats(&mut env, &points);
    let mut e = unsafe { notebook(handle) };
    let mut removed = 0;
    for w in pts.as_chunks::<2>().0.windows(2) {
        removed += e.erase(w[0][0], w[0][1], w[1][0], w[1][1], radius);
    }
    removed as jint
}

#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeEndErase(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    unsafe { notebook(handle) }.end_erase();
}

/// Returns the index of the page that changed, or -1 if there was nothing to undo.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeUndo(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    unsafe { notebook(handle) }.undo().map_or(-1, |i| i as jint)
}

/// Returns the index of the page that changed, or -1 if there was nothing to redo.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeRedo(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    unsafe { notebook(handle) }.redo().map_or(-1, |i| i as jint)
}

/// Outlines for drawing the given strokes of a page, in document units.
///
/// For each requested id: `[tool, color_hi, color_lo, min_x, min_y, max_x,
/// max_y, n, <n floats of polygons>]`. The colour is ARGB split into two
/// 16-bit halves so floats carry it exactly. Polygons are packed as
/// `grapho_core::outline` describes. A stroke that no longer exists comes
/// back with tool -1 and no polygons.
///
/// The strokes are copied under the lock and the outlines computed after it
/// is released, so a long stroke never blocks editing. With `quick`, ink
/// comes back as overlapping per-segment polygons (see
/// `grapho_core::outline_pieces`): the same look when filled with the
/// non-zero rule, at a linear cost.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeStrokeGeometry(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    ids: JLongArray,
    tolerance: jfloat,
    quick: jboolean,
) -> jfloatArray {
    let ids = read_longs(&mut env, &ids);
    let found: Vec<Option<Stroke>> = {
        let e = unsafe { notebook(handle) };
        let strokes: std::collections::HashMap<Id, &Stroke> = e
            .document()
            .pages
            .get(page.max(0) as usize)
            .into_iter()
            .flat_map(|p| &p.layers)
            .flat_map(|l| &l.strokes)
            .map(|s| (s.id, s))
            .collect();
        ids.as_chunks::<2>()
            .0
            .iter()
            .map(|pair| {
                strokes
                    .get(&join_id(pair[0], pair[1]))
                    .map(|s| (*s).clone())
            })
            .collect()
    };
    let mut out = Vec::new();
    let mut polys = Vec::new();
    for s in &found {
        let Some(s) = s else {
            out.extend_from_slice(&[-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]);
            continue;
        };
        polys.clear();
        let b = if s.style.tool == Tool::Fill {
            // A fill's points are already its polygon.
            let mut b = Rect::EMPTY;
            polys.push(s.points.len() as f32);
            for p in &s.points {
                polys.extend_from_slice(&[p.x, p.y]);
                b.add_circle(p.x, p.y, 0.0);
            }
            b
        } else {
            let brush: Brush = s.style.brush();
            if quick != JNI_FALSE {
                outline_pieces_styled(&s.points, &brush, s.style.line, tolerance, &mut polys)
            } else {
                // A panic must never unwind into the JVM, which aborts the
                // app, and a stroke saved in a note would then abort it on
                // every open. Should the full outline fail, the stroke is
                // drawn from its quick pieces instead.
                let full = catch_unwind(AssertUnwindSafe(|| {
                    let mut p = Vec::new();
                    let b = outline_styled(&s.points, &brush, s.style.line, tolerance, &mut p);
                    (b, p)
                }));
                match full {
                    Ok((b, p)) => {
                        polys = p;
                        b
                    }
                    Err(_) => {
                        polys.clear();
                        outline_pieces_styled(
                            &s.points,
                            &brush,
                            s.style.line,
                            tolerance,
                            &mut polys,
                        )
                    }
                }
            }
        };
        let argb = s.style.color.rotate_right(8);
        out.extend_from_slice(&[
            s.style.tool as u8 as f32,
            (argb >> 16) as f32,
            (argb & 0xFFFF) as f32,
            b.min_x,
            b.min_y,
            b.max_x,
            b.max_y,
            polys.len() as f32,
        ]);
        out.extend_from_slice(&polys);
    }
    floats_to_java(&mut env, &out)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn colours_convert_both_ways() {
        let argb = 0x73F5_D33Fu32 as jint;
        let rgba = rgba_from_argb(argb);
        assert_eq!(rgba, 0xF5D3_3F73);
        assert_eq!(rgba.rotate_right(8), argb as u32);
    }

    #[test]
    fn ids_round_trip_through_longs() {
        let id = Id::new_v4();
        let [hi, lo] = split_id(id);
        assert_eq!(join_id(hi, lo), id);
    }
}
