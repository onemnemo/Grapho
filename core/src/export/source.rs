//! Bringing pages of the embedded source PDFs into the output: their
//! content, resources and annotations are copied object by object into the
//! new file, so the page stays vector and nothing else of the source does.

mod annots;

use std::collections::HashMap;
use std::panic::{catch_unwind, AssertUnwindSafe};

use lopdf::{Dictionary, Document as Pdf, Object, ObjectId};

use super::content::Content;
use super::ExportError;
use crate::document::{Document, Id, PdfPage};
use crate::pdf::{geometry, inherited, Geometry};

/// How deep nested arrays and dictionaries are followed when copying.
const MAX_DEPTH: usize = 64;

/// A source page ready to be placed in the output.
pub(super) struct SourcePage {
    pub geometry: Geometry,
    /// The page's own resources with the object ids of the output, and the
    /// names of the flattened annotations added to them.
    pub resources: Dictionary,
    /// The page's content streams, in order.
    pub contents: Vec<ObjectId>,
    /// Draws the annotation appearances. Goes after the page content.
    pub annotations: Content,
    /// Annotations the output page keeps (links that open a web address).
    pub links: Vec<ObjectId>,
    /// The page's transparency group, if it has one.
    pub group: Option<Object>,
}

/// The source PDFs of a note, opened when a page first needs one.
pub(super) struct Sources<'a> {
    doc: &'a Document,
    open: HashMap<Id, Open>,
}

struct Open {
    pdf: Pdf,
    pages: Vec<ObjectId>,
    /// Source object to output object.
    map: HashMap<ObjectId, ObjectId>,
    /// Objects that have an output id but are not copied yet.
    queue: Vec<(ObjectId, ObjectId)>,
}

impl<'a> Sources<'a> {
    pub fn new(doc: &'a Document) -> Self {
        Self {
            doc,
            open: HashMap::new(),
        }
    }

    /// Copies the page `page` refers to into `out`.
    pub fn import(&mut self, page: PdfPage, out: &mut Pdf) -> Result<SourcePage, ExportError> {
        if !self.open.contains_key(&page.source) {
            let source = self
                .doc
                .pdfs
                .iter()
                .find(|s| s.id == page.source)
                .ok_or(ExportError::MissingSource)?;
            self.open.insert(page.source, Open::load(&source.bytes)?);
        }
        let open = self
            .open
            .get_mut(&page.source)
            .ok_or(ExportError::MissingSource)?;
        let id = *open
            .pages
            .get(page.index as usize)
            .ok_or(ExportError::MissingSource)?;
        let imported = open.page(id, out);
        open.drain(out);
        Ok(imported)
    }
}

impl Open {
    fn load(bytes: &[u8]) -> Result<Self, ExportError> {
        // The parser is third party code; a panic in it is an error here.
        let pdf = catch_unwind(AssertUnwindSafe(|| Pdf::load_mem(bytes)))
            .map_err(|_| ExportError::Source("it could not be read".into()))?
            .map_err(|e| ExportError::Source(e.to_string()))?;
        let pages = pdf.page_iter().collect();
        Ok(Self {
            pdf,
            pages,
            map: HashMap::new(),
            queue: Vec::new(),
        })
    }

    fn page(&mut self, page: ObjectId, out: &mut Pdf) -> SourcePage {
        let geometry = geometry(&self.pdf, page);
        let mut resources = inherited(&self.pdf, page, b"Resources")
            .and_then(|o| self.pdf.dereference(o).ok())
            .and_then(|(_, o)| o.as_dict().ok())
            .cloned()
            .unwrap_or_default();
        // The sub-dictionaries become the page's own so names can be added.
        for key in [&b"XObject"[..], b"ExtGState"] {
            if let Ok(Object::Reference(id)) = resources.get(key) {
                if let Ok(Object::Dictionary(d)) = self.pdf.get_object(*id) {
                    resources.set(key.to_vec(), Object::Dictionary(d.clone()));
                }
            }
        }
        self.rewrite_dict(&mut resources, out, 0);
        let contents = self
            .pdf
            .get_page_contents(page)
            .into_iter()
            .map(|id| self.map_id(id, out))
            .collect();
        let group = self
            .pdf
            .get_dictionary(page)
            .ok()
            .and_then(|d| d.get(b"Group").ok())
            .cloned()
            .map(|mut g| {
                self.rewrite(&mut g, out, 0);
                g
            });
        let annots::Kept {
            draws,
            xobjects,
            links,
        } = self.annotations(page, out);
        if !xobjects.is_empty() {
            let mut table = match resources.get(b"XObject") {
                Ok(Object::Dictionary(d)) => d.clone(),
                _ => Dictionary::new(),
            };
            for (name, id) in xobjects {
                table.set(name, Object::Reference(id));
            }
            resources.set("XObject", Object::Dictionary(table));
        }
        SourcePage {
            geometry,
            resources,
            contents,
            annotations: draws,
            links,
            group,
        }
    }

    /// The output id for a source object, reserving one on first sight.
    fn map_id(&mut self, id: ObjectId, out: &mut Pdf) -> ObjectId {
        if let Some(&mapped) = self.map.get(&id) {
            return mapped;
        }
        let mapped = out.new_object_id();
        self.map.insert(id, mapped);
        self.queue.push((id, mapped));
        mapped
    }

    /// Points every reference inside `object` at the output.
    fn rewrite(&mut self, object: &mut Object, out: &mut Pdf, depth: usize) {
        if depth > MAX_DEPTH {
            *object = Object::Null;
            return;
        }
        match object {
            Object::Reference(id) => *id = self.map_id(*id, out),
            Object::Array(items) => {
                for item in items {
                    self.rewrite(item, out, depth + 1);
                }
            }
            Object::Dictionary(d) => self.rewrite_dict(d, out, depth),
            Object::Stream(s) => self.rewrite_dict(&mut s.dict, out, depth),
            _ => {}
        }
    }

    fn rewrite_dict(&mut self, dict: &mut Dictionary, out: &mut Pdf, depth: usize) {
        for (_, value) in dict.iter_mut() {
            self.rewrite(value, out, depth + 1);
        }
    }

    /// Copies every object reserved so far, and the ones they refer to.
    fn drain(&mut self, out: &mut Pdf) {
        while let Some((from, to)) = self.queue.pop() {
            let mut object = self.pdf.objects.get(&from).cloned().unwrap_or(Object::Null);
            self.rewrite(&mut object, out, 0);
            if let Object::Stream(s) = &mut object {
                s.dict.set("Length", s.content.len() as i64);
            }
            out.objects.insert(to, object);
        }
    }
}
