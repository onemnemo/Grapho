//! JNI entry points for text boxes on `NativeNotebook`: adding, finding,
//! listing, updating and deleting them. Moving, copying, recolouring and
//! reordering go through the selection entry points, which take text box ids
//! as well as stroke ids.
//!
//! A panic anywhere inside is caught and thrown as `java.io.IOException`,
//! never unwound into the JVM.

use grapho_core::document::{Id, TextAlign, TextBox, TextStyle};
use jni::objects::{JClass, JFloatArray, JLongArray, JString};
use jni::sys::{jboolean, jfloat, jint, jlong, jlongArray, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use serde_json::json;

use crate::pdf::guarded;
use crate::select::{ids_to_java, read_ids};
use crate::{notebook, read_floats, read_string, rgba_from_argb, split_id, string_to_java};

fn one_id(ids: Option<Id>) -> Vec<Id> {
    ids.into_iter().collect()
}

/// Android colours are ARGB; the file format stores RGBA.
fn argb_from_rgba(rgba: u32) -> i32 {
    rgba.rotate_right(8) as i32
}

/// The weight, slant and underline flags as `[bold, italic, underline]`.
type Marks = [jboolean; 3];

fn style_from(size: jfloat, argb: jint, align: jint, marks: Marks) -> TextStyle {
    TextStyle {
        size,
        color: rgba_from_argb(argb),
        align: u8::try_from(align)
            .ok()
            .and_then(TextAlign::from_u8)
            .unwrap_or_default(),
        bold: marks[0] != JNI_FALSE,
        italic: marks[1] != JNI_FALSE,
        underline: marks[2] != JNI_FALSE,
    }
}

fn text_json(t: &TextBox) -> serde_json::Value {
    let [hi, lo] = split_id(t.id);
    json!({
        "hi": hi,
        "lo": lo,
        "x": t.x,
        "y": t.y,
        "w": t.width,
        "h": t.height,
        "rot": t.rotation,
        "text": t.text,
        "size": t.size,
        "argb": argb_from_rgba(t.color),
        "align": t.align.to_u8(),
        "bold": t.bold,
        "italic": t.italic,
        "underline": t.underline,
    })
}

/// Places a text box on top of the page's active layer, as one undo step.
/// `frame` is `[x, y, width, height]` in page-local points, `size` the font
/// size in points, `argb` the colour, `align` 0 start, 1 centre or 2 end, then
/// the bold, italic and underline flags. Returns the id as `[hi, lo]`, or an empty array if the frame or size is
/// not valid, the text is too long, the page doesn't exist, or its active
/// layer is hidden or locked.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeAddText(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    frame: JFloatArray,
    text: JString,
    size: jfloat,
    argb: jint,
    align: jint,
    bold: jboolean,
    italic: jboolean,
    underline: jboolean,
) -> jlongArray {
    guarded(&mut env, std::ptr::null_mut(), |env| {
        let frame = <[f32; 4]>::try_from(read_floats(env, &frame).as_slice()).ok();
        let text = read_string(env, &text);
        let style = style_from(size, argb, align, [bold, italic, underline]);
        let id = match (usize::try_from(page), frame) {
            (Ok(p), Some(frame)) => unsafe { notebook(handle) }
                .add_text(p, frame, &text, style)
                .ok(),
            _ => None,
        };
        Ok(ids_to_java(env, &one_id(id)))
    })
}

/// The topmost text box under a page-local point on a visible, unlocked
/// layer, as `[hi, lo]`, or an empty array if there is none.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeTextAt(
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
            .and_then(|p| unsafe { notebook(handle) }.text_at(p, x, y));
        Ok(ids_to_java(env, &one_id(id)))
    })
}

/// The text boxes of a page, one inner array per visible layer in the same
/// order as `nativeLayerStrokeIds`, each in drawing order, as JSON:
/// `[[{"hi":1,"lo":2,"x":0,"y":0,"w":100,"h":30,"rot":0,"text":"...",
/// "size":18,"argb":-15196890,"align":0,"bold":false,
/// "italic":false,"underline":false}, ...], ...]`. The
/// frame is the box before it is turned, in page points, `rot` the clockwise
/// turn about its centre in radians. An unknown page gives `[]`.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativePageTexts(
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
                .map(|l| l.texts.iter().map(text_json).collect())
                .collect();
            serde_json::Value::from(layers).to_string()
        };
        Ok(string_to_java(env, &text))
    })
}

/// Changes a text box's frame, turn, words and look in one undo step, which
/// keeps its id. `frame` is `[x, y, width, height, rotation]`, the rest as
/// for `nativeAddText`. Returns true if the box changed, and false if it
/// doesn't exist, is on a locked layer, nothing differs, or a value is not
/// valid.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeUpdateText(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
    frame: JFloatArray,
    text: JString,
    size: jfloat,
    argb: jint,
    align: jint,
    bold: jboolean,
    italic: jboolean,
    underline: jboolean,
) -> jboolean {
    guarded(&mut env, JNI_FALSE, |env| {
        let id = read_ids(env, &id).first().copied();
        let frame = <[f32; 5]>::try_from(read_floats(env, &frame).as_slice()).ok();
        let text = read_string(env, &text);
        let style = style_from(size, argb, align, [bold, italic, underline]);
        let changed = match (usize::try_from(page), id, frame) {
            (Ok(p), Some(id), Some([x, y, width, height, rotation])) => {
                let mut next = TextBox::new(x, y, width, height, text, style);
                next.id = id;
                next.rotation = rotation;
                unsafe { notebook(handle) }.update_text(p, next) == Ok(true)
            }
            _ => false,
        };
        Ok(if changed { JNI_TRUE } else { JNI_FALSE })
    })
}

/// Removes a text box as one undo step. Returns false if it doesn't exist
/// or is on a locked layer.
#[no_mangle]
pub extern "system" fn Java_com_onemnemo_grapho_engine_NativeNotebook_nativeDeleteText(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    page: jint,
    id: JLongArray,
) -> jboolean {
    guarded(&mut env, JNI_FALSE, |env| {
        let id = read_ids(env, &id).first().copied();
        let done = match (usize::try_from(page), id) {
            (Ok(p), Some(id)) => {
                let mut e = unsafe { notebook(handle) };
                let present = e.text_box(p, id).is_some();
                present && e.delete(p, &[id])
            }
            _ => false,
        };
        Ok(if done { JNI_TRUE } else { JNI_FALSE })
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn colours_cross_as_argb_and_come_back() {
        let rgba = 0x1122_33CC;
        assert_eq!(argb_from_rgba(rgba) as u32, 0xCC11_2233);
        assert_eq!(rgba_from_argb(argb_from_rgba(rgba)), rgba);
    }

    #[test]
    fn json_carries_every_property_far_from_the_origin() {
        let mut t = TextBox::new(
            5000.5,
            7000.25,
            120.0,
            40.0,
            "Hei \u{00e5}",
            TextStyle {
                size: 24.0,
                color: 0xD62F_2FFF,
                align: TextAlign::End,
                bold: true,
                italic: true,
                underline: true,
            },
        );
        t.rotation = 0.5;
        let v = text_json(&t);
        assert_eq!(v["x"], 5000.5);
        assert_eq!(v["y"], 7000.25);
        assert_eq!(
            (v["w"].as_f64(), v["h"].as_f64()),
            (Some(120.0), Some(40.0))
        );
        assert_eq!(v["rot"], 0.5);
        assert_eq!(v["text"], "Hei \u{00e5}");
        assert_eq!(v["align"], 2);
        assert_eq!(v["bold"], true);
        assert_eq!(
            (v["italic"].as_bool(), v["underline"].as_bool()),
            (Some(true), Some(true))
        );
        assert_eq!(v["argb"].as_i64(), Some(i64::from(0xFFD6_2F2Fu32 as i32)));
        let [hi, lo] = split_id(t.id);
        assert_eq!((v["hi"].as_i64(), v["lo"].as_i64()), (Some(hi), Some(lo)));
    }

    #[test]
    fn style_reads_align_and_bold_safely() {
        let s = style_from(
            20.0,
            0x0000_00FFu32 as i32,
            9,
            [JNI_TRUE, JNI_FALSE, JNI_TRUE],
        );
        assert_eq!(s.align, TextAlign::Start);
        assert!(s.bold && !s.italic && s.underline);
        assert_eq!(
            style_from(20.0, 0, 1, [JNI_FALSE; 3]).align,
            TextAlign::Centre
        );
    }
}
