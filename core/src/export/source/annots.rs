//! Annotations of a source page. Links that open a web address stay links.
//! Every other annotation with an appearance is drawn into the page, so it
//! shows under the ink as it does on screen (the screen renders the PDF
//! first and draws ink over it). Links to other pages are dropped, as those
//! pages are not in the output.

use lopdf::{Dictionary, Object, ObjectId};

use super::Open;
use crate::export::content::Content;
use crate::pdf::rect;
use lopdf::Document as Pdf;

/// Flag bits of an annotation's /F entry that keep it off the screen.
const HIDDEN: i64 = 1 << 1;
const NO_VIEW: i64 = 1 << 5;
/// Keys a kept link carries over.
const LINK_KEYS: [&[u8]; 6] = [b"Type", b"Subtype", b"Rect", b"Border", b"C", b"F"];

pub(super) struct Kept {
    /// Operators that paint the flattened appearances.
    pub draws: Content,
    /// The appearance streams to add to the page's resources, by name.
    pub xobjects: Vec<(String, ObjectId)>,
    pub links: Vec<ObjectId>,
}

impl Open {
    pub(super) fn annotations(&mut self, page: ObjectId, out: &mut Pdf) -> Kept {
        let mut kept = Kept {
            draws: Content::new(),
            xobjects: Vec::new(),
            links: Vec::new(),
        };
        let list: Vec<Object> = self
            .pdf
            .get_dictionary(page)
            .ok()
            .and_then(|p| p.get(b"Annots").ok())
            .and_then(|a| self.pdf.dereference(a).ok())
            .and_then(|(_, a)| a.as_array().ok())
            .cloned()
            .unwrap_or_default();
        for item in &list {
            let Ok((_, Object::Dictionary(annot))) = self.pdf.dereference(item) else {
                continue;
            };
            let annot = annot.clone();
            match name(&annot, b"Subtype") {
                Some(b"Popup") => {}
                Some(b"Link") => {
                    if let Some(id) = self.link(&annot, out) {
                        kept.links.push(id);
                    }
                }
                _ => self.flatten(&annot, &mut kept, out),
            }
        }
        kept
    }

    /// A link with a web address, as a new annotation of the output.
    fn link(&mut self, annot: &Dictionary, out: &mut Pdf) -> Option<ObjectId> {
        let action = self.pdf.dereference(annot.get(b"A").ok()?).ok()?.1;
        let action = action.as_dict().ok()?;
        if name(action, b"S") != Some(b"URI") {
            return None;
        }
        let mut link = Dictionary::new();
        for key in LINK_KEYS {
            if let Ok(value) = annot.get(key) {
                link.set(key.to_vec(), value.clone());
            }
        }
        link.set("A", Object::Dictionary(action.clone()));
        let mut object = Object::Dictionary(link);
        self.rewrite(&mut object, out, 0);
        Some(out.add_object(object))
    }

    /// Queues the annotation's normal appearance to be drawn into its
    /// rectangle, the way a viewer would place it.
    fn flatten(&mut self, annot: &Dictionary, kept: &mut Kept, out: &mut Pdf) {
        let flags = annot
            .get(b"F")
            .ok()
            .and_then(|f| f.as_i64().ok())
            .unwrap_or(0);
        if flags & (HIDDEN | NO_VIEW) != 0 {
            return;
        }
        let Some(target) = rect(&self.pdf, annot.get(b"Rect").unwrap_or(&Object::Null)) else {
            return;
        };
        let Some(stream_id) = self.appearance(annot) else {
            return;
        };
        let Ok(Object::Stream(stream)) = self.pdf.get_object(stream_id) else {
            return;
        };
        let Some(bbox) = rect(&self.pdf, stream.dict.get(b"BBox").unwrap_or(&Object::Null)) else {
            return;
        };
        let m = self.matrix(&stream.dict);
        let [x0, y0, x1, y1] = transformed_bounds(bbox, m);
        let (w, h) = (x1 - x0, y1 - y0);
        if w <= 0.0 || h <= 0.0 {
            return;
        }
        let sx = (target.2 - target.0) / w;
        let sy = (target.3 - target.1) / h;
        let name = format!("GrAnnot{}", kept.xobjects.len());
        kept.draws.raw("q");
        kept.draws.op(
            &[sx, 0.0, 0.0, sy, target.0 - x0 * sx, target.1 - y0 * sy],
            "cm",
        );
        kept.draws.raw(&format!("/{name} Do"));
        kept.draws.raw("Q");
        let id = self.map_id(stream_id, out);
        kept.xobjects.push((name, id));
    }

    /// The stream of the annotation's normal appearance in its current state.
    fn appearance(&self, annot: &Dictionary) -> Option<ObjectId> {
        let ap = self.pdf.dereference(annot.get(b"AP").ok()?).ok()?.1;
        let normal = ap.as_dict().ok()?.get(b"N").ok()?;
        let (id, normal) = self.pdf.dereference(normal).ok()?;
        match normal {
            Object::Stream(_) => id,
            Object::Dictionary(states) => {
                let state = name(annot, b"AS")?;
                let entry = states.get(state).ok()?;
                let (id, entry) = self.pdf.dereference(entry).ok()?;
                matches!(entry, Object::Stream(_)).then_some(id)?
            }
            _ => None,
        }
    }

    /// The form's /Matrix, or the identity.
    fn matrix(&self, dict: &Dictionary) -> [f32; 6] {
        let mut m = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];
        let Some(items) = dict
            .get(b"Matrix")
            .ok()
            .and_then(|o| self.pdf.dereference(o).ok())
            .and_then(|(_, o)| o.as_array().ok())
            .filter(|a| a.len() == 6)
        else {
            return m;
        };
        for (slot, item) in m.iter_mut().zip(items) {
            match self
                .pdf
                .dereference(item)
                .ok()
                .and_then(|(_, o)| o.as_float().ok())
            {
                Some(v) if v.is_finite() => *slot = v,
                _ => return [1.0, 0.0, 0.0, 1.0, 0.0, 0.0],
            }
        }
        m
    }
}

fn name<'a>(dict: &'a Dictionary, key: &[u8]) -> Option<&'a [u8]> {
    dict.get(key).ok()?.as_name().ok()
}

/// Bounds of a box after the matrix `[a b c d e f]` moves it.
fn transformed_bounds(b: (f32, f32, f32, f32), m: [f32; 6]) -> [f32; 4] {
    let corners = [(b.0, b.1), (b.2, b.1), (b.2, b.3), (b.0, b.3)];
    let mut out = [
        f32::INFINITY,
        f32::INFINITY,
        f32::NEG_INFINITY,
        f32::NEG_INFINITY,
    ];
    for (x, y) in corners {
        let (tx, ty) = (m[0] * x + m[2] * y + m[4], m[1] * x + m[3] * y + m[5]);
        out = [
            out[0].min(tx),
            out[1].min(ty),
            out[2].max(tx),
            out[3].max(ty),
        ];
    }
    out
}
