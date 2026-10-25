//! Building the output pages: plain pages with their own paper, and pages
//! that carry a source PDF page underneath the ink.

use std::collections::{BTreeMap, BTreeSet, HashMap};

use lopdf::{dictionary, Dictionary, Document as Pdf, Object, ObjectId, Stream};

use super::colour::WHITE;
use super::content::Content;
use super::image::{self, embed};
use super::ink::{collect, InkContent};
use super::source::{SourcePage, Sources};
use super::{new_pdf, paper, ExportError};
use crate::document::{Document, Page};

/// Space around the ink of an infinite canvas, in points.
const MARGIN: f32 = 24.0;
/// Page used for an infinite canvas with no ink.
const EMPTY_SIZE: (f32, f32) = (595.0, 842.0);
/// The largest side a PDF page may have, in points.
const MAX_SIDE: f32 = 14_400.0;

pub(super) struct Builder<'a> {
    doc: &'a Document,
    backgrounds: bool,
    pdf: Pdf,
    pages_id: ObjectId,
    kids: Vec<Object>,
    sources: Sources<'a>,
    /// The XObject of each embedded image, by index in the note's list, so
    /// a picture used on several pages is written once.
    images: HashMap<usize, ObjectId>,
}

impl<'a> Builder<'a> {
    pub fn new(doc: &'a Document, backgrounds: bool) -> Self {
        let (pdf, pages_id) = new_pdf();
        Self {
            doc,
            backgrounds,
            pdf,
            pages_id,
            kids: Vec::new(),
            sources: Sources::new(doc),
            images: HashMap::new(),
        }
    }

    /// Appends the page at `index` of the note.
    pub fn add_page(&mut self, index: usize) -> Result<(), ExportError> {
        let page = &self.doc.pages[index];
        let id = match page.pdf.filter(|_| self.backgrounds) {
            Some(pdf_page) => {
                let source = self.sources.import(pdf_page, &mut self.pdf)?;
                self.over_source(page, source)?
            }
            None => self.plain(index, page)?,
        };
        self.kids.push(Object::Reference(id));
        Ok(())
    }

    /// The page tree and catalog. Returns the finished file.
    pub fn finish(mut self) -> Pdf {
        let count = self.kids.len() as i64;
        self.pdf.objects.insert(
            self.pages_id,
            Object::Dictionary(dictionary! {
                "Type" => "Pages",
                "Kids" => self.kids,
                "Count" => count,
            }),
        );
        let catalog = self.pdf.add_object(dictionary! {
            "Type" => "Catalog",
            "Pages" => self.pages_id,
        });
        self.pdf.trailer.set("Root", catalog);
        self.pdf
    }

    /// A page on its own paper (or white), with the ink over it.
    fn plain(&mut self, index: usize, page: &Page) -> Result<ObjectId, ExportError> {
        let ink = collect(page, &self.doc.images);
        let background = self.doc.canvas.background();
        let (w, h, origin) = match self.doc.page_size(index) {
            Some((w, h)) => (w, h, (0.0, 0.0)),
            None if ink.bounds.is_empty() => (EMPTY_SIZE.0, EMPTY_SIZE.1, (0.0, 0.0)),
            None => (
                ink.bounds.max_x - ink.bounds.min_x + 2.0 * MARGIN,
                ink.bounds.max_y - ink.bounds.min_y + 2.0 * MARGIN,
                (ink.bounds.min_x - MARGIN, ink.bounds.min_y - MARGIN),
            ),
        };
        let (w, h) = (w.min(MAX_SIDE), h.min(MAX_SIDE));
        let paper_color = if self.backgrounds {
            background.color
        } else {
            WHITE
        };
        let rendered = ink.render(paper_color);

        let mut c = Content::new();
        c.raw("q");
        // Document units run down from the top left; the page runs up.
        c.op(&[1.0, 0.0, 0.0, -1.0, -origin.0, h + origin.1], "cm");
        let area = [origin.0, origin.1, origin.0 + w, origin.1 + h];
        if self.backgrounds {
            paper::draw(&mut c, &background, area);
        }
        c.append(&rendered.content);
        c.raw("Q");
        let mut resources = Dictionary::new();
        self.install(&mut resources, &rendered, area)?;
        let contents = self.stream(c.into_bytes());
        Ok(self.add(dictionary! {
            "Type" => "Page",
            "Parent" => self.pages_id,
            "MediaBox" => real_array(&[0.0, 0.0, w, h]),
            "Resources" => resources,
            "Contents" => contents,
        }))
    }

    /// A source PDF page kept as it was, with the ink drawn over it.
    fn over_source(&mut self, page: &Page, source: SourcePage) -> Result<ObjectId, ExportError> {
        let ink = collect(page, &self.doc.images);
        let rendered = ink.render(WHITE);
        let (x0, y0, x1, y1) = source.geometry.area;
        let turns = source.geometry.turns;
        let (w, h) = (x1 - x0, y1 - y0);
        let (dw, dh) = if turns == 90 || turns == 270 {
            (h, w)
        } else {
            (w, h)
        };

        // A white sheet under the original, which assumes a white page.
        let mut before = Content::new();
        before.raw("q");
        before.raw("q");
        before.raw("1 1 1 rg");
        before.op(&[x0, y0, w, h], "re");
        before.raw("f");
        before.raw("Q");
        let mut after = Content::new();
        after.raw("Q");
        after.append(&source.annotations);

        let mut resources = source.resources;
        self.install(&mut resources, &rendered, [0.0, 0.0, dw, dh])?;
        let mut contents = vec![Object::Reference(self.stream(before.into_bytes()))];
        contents.extend(source.contents.into_iter().map(Object::Reference));
        contents.push(Object::Reference(self.stream(after.into_bytes())));
        if !ink.is_empty() {
            let mut c = Content::new();
            c.raw("q");
            c.op(&display_matrix(turns, source.geometry.area), "cm");
            c.append(&rendered.content);
            c.raw("Q");
            contents.push(Object::Reference(self.stream(c.into_bytes())));
        }

        let mut dict = dictionary! {
            "Type" => "Page",
            "Parent" => self.pages_id,
            "MediaBox" => real_array(&[x0, y0, x1, y1]),
            "Resources" => resources,
            "Contents" => contents,
        };
        if turns != 0 {
            dict.set("Rotate", i64::from(turns));
        }
        if !source.links.is_empty() {
            let links: Vec<Object> = source.links.into_iter().map(Object::Reference).collect();
            dict.set("Annots", links);
        }
        if let Some(group) = source.group {
            dict.set("Group", group);
        }
        Ok(self.add(dict))
    }

    fn add(&mut self, dict: Dictionary) -> ObjectId {
        self.pdf.add_object(Object::Dictionary(dict))
    }

    fn stream(&mut self, bytes: Vec<u8>) -> ObjectId {
        self.pdf
            .add_object(Object::Stream(Stream::new(Dictionary::new(), bytes)))
    }

    /// Adds the ink's graphics states and groups to `resources`. `extent`
    /// is the page in document units, which bounds every group.
    fn install(
        &mut self,
        resources: &mut Dictionary,
        ink: &InkContent,
        extent: [f32; 4],
    ) -> Result<(), ExportError> {
        let mut states = BTreeMap::new();
        for &alpha in &ink.alphas {
            let a = f32::from(alpha) / 255.0;
            let id = self.pdf.add_object(dictionary! {
                "Type" => "ExtGState",
                "ca" => a,
                "CA" => a,
            });
            states.insert(format!("GA{alpha}"), id);
        }
        let mut gs = Dictionary::new();
        for (name, id) in &states {
            gs.set(name.as_bytes().to_vec(), Object::Reference(*id));
        }
        let mut forms = Dictionary::new();
        for (i, group) in ink.groups.iter().enumerate() {
            let mut group_resources = dictionary! { "ExtGState" => gs.clone() };
            let pictures = self.image_table(&group.images)?;
            if !pictures.is_empty() {
                group_resources.set("XObject", pictures);
            }
            let dict = dictionary! {
                "Type" => "XObject",
                "Subtype" => "Form",
                "BBox" => real_array(&extent),
                "Group" => dictionary! {
                    "S" => "Transparency",
                    "I" => true,
                },
                "Resources" => group_resources,
            };
            let id = self
                .pdf
                .add_object(Object::Stream(Stream::new(dict, group.content.clone())));
            forms.set(format!("Fm{i}").into_bytes(), Object::Reference(id));
        }
        merge(resources, "ExtGState", gs);
        merge(resources, "XObject", forms);
        merge(resources, "XObject", self.image_table(&ink.images)?);
        Ok(())
    }

    /// The resource entries for some image sources, embedding each one the
    /// first time it is needed.
    fn image_table(&mut self, used: &BTreeSet<usize>) -> Result<Dictionary, ExportError> {
        let mut table = Dictionary::new();
        for &index in used {
            let id = match self.images.get(&index) {
                Some(&id) => id,
                None => {
                    let id = embed(&mut self.pdf, &self.doc.images[index])?;
                    self.images.insert(index, id);
                    id
                }
            };
            table.set(image::name(index).into_bytes(), Object::Reference(id));
        }
        Ok(table)
    }
}

/// Adds `entries` to the sub-dictionary `key` of `resources`.
fn merge(resources: &mut Dictionary, key: &str, entries: Dictionary) {
    if entries.is_empty() {
        return;
    }
    let mut table = match resources.get(key.as_bytes()) {
        Ok(Object::Dictionary(d)) => d.clone(),
        _ => Dictionary::new(),
    };
    for (name, value) in entries.iter() {
        table.set(name.clone(), value.clone());
    }
    resources.set(key, Object::Dictionary(table));
}

/// The matrix `[a b c d e f]` from displayed page units (down from the top
/// left, after the page's rotation) to the page's own user space.
pub(super) fn display_matrix(turns: u32, area: (f32, f32, f32, f32)) -> [f32; 6] {
    let (x0, y0, x1, y1) = area;
    match turns {
        90 => [0.0, 1.0, 1.0, 0.0, x0, y0],
        180 => [-1.0, 0.0, 0.0, 1.0, x1, y0],
        270 => [0.0, -1.0, -1.0, 0.0, x1, y1],
        _ => [1.0, 0.0, 0.0, -1.0, x0, y1],
    }
}

fn real_array(values: &[f32]) -> Vec<Object> {
    values.iter().map(|&v| Object::Real(v)).collect()
}
