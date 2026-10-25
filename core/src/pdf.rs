//! Reading the page structure of a PDF: how many pages it has and how big
//! each one is when shown. The pages themselves are drawn by the platform
//! shell; the core only needs their sizes to lay out a note around them.

use std::fmt;
use std::panic::{catch_unwind, AssertUnwindSafe};

use lopdf::{Dictionary, Document as Pdf, Error as LopdfError, Object, ObjectId};

use crate::document::PageSize;

/// Size of a page with no usable box, US Letter, in points.
const DEFAULT_SIZE: (f32, f32) = (612.0, 792.0);
/// Smallest and largest side a page may have, in points. The PDF spec caps
/// pages at 200 inches, and a hairline page is not something to write on.
const MIN_SIDE: f32 = 36.0;
const MAX_SIDE: f32 = 14_400.0;
/// How many `/Parent` links are followed to find an inherited attribute.
const MAX_DEPTH: usize = 64;

/// Why a PDF could not be read. The messages are written for the person who
/// picked the file.
#[derive(Debug, PartialEq, Eq)]
pub enum PdfError {
    /// The file needs a password that is not empty.
    Encrypted,
    /// The file could not be parsed.
    Damaged(String),
    /// The file is a PDF but has no pages.
    NoPages,
    /// The note is an infinite canvas, which has no pages to add.
    NotPaged,
}

impl fmt::Display for PdfError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PdfError::Encrypted => write!(f, "This PDF is password protected"),
            PdfError::Damaged(why) => write!(f, "This PDF is damaged and can't be opened: {why}"),
            PdfError::NoPages => write!(f, "This PDF has no pages"),
            PdfError::NotPaged => write!(f, "A PDF can't be added to an infinite canvas"),
        }
    }
}

impl std::error::Error for PdfError {}

/// The displayed size of every page of a PDF, in points and in page order.
///
/// A page uses its CropBox, else its MediaBox, either of which may be
/// inherited from a parent node of the page tree. A /Rotate of 90 or 270
/// swaps width and height. Sides are clamped to a sane range, and a page
/// with no usable box gets US Letter. A file that needs a password (other
/// than the empty one) gives [`PdfError::Encrypted`].
pub fn read_page_sizes(bytes: &[u8]) -> Result<Vec<PageSize>, PdfError> {
    // The parser is third party code fed with arbitrary files; a panic in it
    // is reported as a damaged file instead of unwinding into the shell.
    catch_unwind(AssertUnwindSafe(|| read(bytes)))
        .unwrap_or_else(|_| Err(PdfError::Damaged("it could not be read".into())))
}

fn read(bytes: &[u8]) -> Result<Vec<PageSize>, PdfError> {
    let doc = Pdf::load_mem(bytes).map_err(|e| match e {
        LopdfError::InvalidPassword | LopdfError::Decryption(_) => PdfError::Encrypted,
        other => PdfError::Damaged(other.to_string()),
    })?;
    if doc.trailer.get(b"Encrypt").is_ok() && doc.encryption_state.is_none() {
        return Err(PdfError::Encrypted);
    }
    let sizes: Vec<PageSize> = doc.page_iter().map(|id| page_size(&doc, id)).collect();
    if sizes.is_empty() {
        return Err(PdfError::NoPages);
    }
    Ok(sizes)
}

fn page_size(doc: &Pdf, page: ObjectId) -> PageSize {
    let Geometry { area, turns } = geometry(doc, page);
    let (mut w, mut h) = (area.2 - area.0, area.3 - area.1);
    if turns == 90 || turns == 270 {
        std::mem::swap(&mut w, &mut h);
    }
    PageSize {
        width: w.clamp(MIN_SIDE, MAX_SIDE),
        height: h.clamp(MIN_SIDE, MAX_SIDE),
    }
}

/// Where a PDF page is shown: the visible area in the page's own user space
/// and the clockwise turn applied when it is displayed.
#[derive(Clone, Copy, Debug, PartialEq)]
pub(crate) struct Geometry {
    /// `(x0, y0, x1, y1)`: the crop box clipped to the media box, or
    /// whichever exists, or US Letter when neither is usable.
    pub area: Box4,
    /// 0, 90, 180 or 270.
    pub turns: u32,
}

pub(crate) fn geometry(doc: &Pdf, page: ObjectId) -> Geometry {
    let media = inherited(doc, page, b"MediaBox").and_then(|o| rect(doc, o));
    let crop = inherited(doc, page, b"CropBox").and_then(|o| rect(doc, o));
    let area = match (crop, media) {
        // The visible area never reaches past the media box.
        (Some(c), Some(m)) => intersect(c, m).or(Some(m)),
        (Some(c), None) => Some(c),
        (None, m) => m,
    };
    let turns = inherited(doc, page, b"Rotate")
        .and_then(|o| doc.dereference(o).ok())
        .and_then(|(_, o)| o.as_float().ok())
        .map_or(0, |r| (r as i64).rem_euclid(360) as u32);
    Geometry {
        area: area.unwrap_or((0.0, 0.0, DEFAULT_SIZE.0, DEFAULT_SIZE.1)),
        turns: if matches!(turns, 90 | 180 | 270) {
            turns
        } else {
            0
        },
    }
}

/// An attribute of a page, or of the nearest ancestor that has it.
pub(crate) fn inherited<'a>(doc: &'a Pdf, page: ObjectId, key: &[u8]) -> Option<&'a Object> {
    let mut node: &Dictionary = doc.get_dictionary(page).ok()?;
    for _ in 0..MAX_DEPTH {
        if let Ok(value) = node.get(key) {
            return Some(value);
        }
        let parent = node.get(b"Parent").ok()?.as_reference().ok()?;
        node = doc.get_dictionary(parent).ok()?;
    }
    None
}

pub(crate) type Box4 = (f32, f32, f32, f32);

/// A normalised `(x0, y0, x1, y1)` from a PDF rectangle, if it has area.
pub(crate) fn rect(doc: &Pdf, object: &Object) -> Option<Box4> {
    let (_, object) = doc.dereference(object).ok()?;
    let items = object.as_array().ok()?;
    if items.len() != 4 {
        return None;
    }
    let mut n = [0.0f32; 4];
    for (slot, item) in n.iter_mut().zip(items) {
        *slot = doc.dereference(item).ok()?.1.as_float().ok()?;
    }
    if n.iter().any(|v| !v.is_finite()) {
        return None;
    }
    let r = (
        n[0].min(n[2]),
        n[1].min(n[3]),
        n[0].max(n[2]),
        n[1].max(n[3]),
    );
    (r.2 > r.0 && r.3 > r.1).then_some(r)
}

fn intersect(a: Box4, b: Box4) -> Option<Box4> {
    let r = (a.0.max(b.0), a.1.max(b.1), a.2.min(b.2), a.3.min(b.3));
    (r.2 > r.0 && r.3 > r.1).then_some(r)
}

/// Small PDFs for tests, here and in the document tests.
#[cfg(test)]
pub(crate) mod fixtures {
    use lopdf::{dictionary, Document, Object};

    /// One page to build: its own boxes and rotation, all optional.
    #[derive(Default, Clone)]
    pub struct Spec {
        pub media: Option<[f32; 4]>,
        pub crop: Option<[f32; 4]>,
        pub rotate: Option<i64>,
    }

    pub fn media(w: f32, h: f32) -> Spec {
        Spec {
            media: Some([0.0, 0.0, w, h]),
            ..Spec::default()
        }
    }

    fn array(v: [f32; 4]) -> Object {
        Object::Array(v.iter().map(|n| Object::Real(*n)).collect())
    }

    /// A PDF with these pages. `root_media` is set on the page tree root, for
    /// pages that inherit it.
    pub fn build(pages: &[Spec], root_media: Option<[f32; 4]>) -> Document {
        let mut doc = Document::with_version("1.5");
        let pages_id = doc.new_object_id();
        let kids: Vec<Object> = pages
            .iter()
            .map(|s| {
                let mut d = dictionary! { "Type" => "Page", "Parent" => pages_id };
                if let Some(m) = s.media {
                    d.set("MediaBox", array(m));
                }
                if let Some(c) = s.crop {
                    d.set("CropBox", array(c));
                }
                if let Some(r) = s.rotate {
                    d.set("Rotate", r);
                }
                Object::Reference(doc.add_object(d))
            })
            .collect();
        let mut root = dictionary! {
            "Type" => "Pages",
            "Kids" => kids,
            "Count" => pages.len() as i64,
        };
        if let Some(m) = root_media {
            root.set("MediaBox", array(m));
        }
        doc.objects.insert(pages_id, Object::Dictionary(root));
        let catalog = doc.add_object(dictionary! { "Type" => "Catalog", "Pages" => pages_id });
        doc.trailer.set("Root", catalog);
        doc
    }

    pub fn bytes(doc: &mut Document) -> Vec<u8> {
        let mut out = Vec::new();
        doc.save_to(&mut out).unwrap();
        out
    }

    /// A plain PDF with these page sizes in points.
    pub fn pdf_of(sizes: &[(f32, f32)]) -> Vec<u8> {
        let specs: Vec<Spec> = sizes.iter().map(|&(w, h)| media(w, h)).collect();
        bytes(&mut build(&specs, None))
    }
}

#[cfg(test)]
mod tests {
    use super::fixtures::*;
    use super::*;

    fn sizes(bytes: &[u8]) -> Vec<(f32, f32)> {
        read_page_sizes(bytes)
            .unwrap()
            .iter()
            .map(|s| (s.width, s.height))
            .collect()
    }

    #[test]
    fn portrait_and_landscape_keep_their_shape() {
        let pdf = bytes(&mut build(
            &[media(595.0, 842.0), media(842.0, 595.0)],
            None,
        ));
        assert_eq!(sizes(&pdf), vec![(595.0, 842.0), (842.0, 595.0)]);
    }

    #[test]
    fn mixed_sizes_come_back_in_page_order() {
        let pdf = pdf_of(&[(612.0, 792.0), (300.0, 400.0), (1000.0, 500.0)]);
        assert_eq!(
            sizes(&pdf),
            vec![(612.0, 792.0), (300.0, 400.0), (1000.0, 500.0)]
        );
    }

    #[test]
    fn a_quarter_turn_swaps_the_sides() {
        let turned = |r| Spec {
            rotate: Some(r),
            ..media(300.0, 500.0)
        };
        let pdf = bytes(&mut build(
            &[turned(90), turned(270), turned(180), turned(-90), turned(0)],
            None,
        ));
        assert_eq!(
            sizes(&pdf),
            vec![
                (500.0, 300.0),
                (500.0, 300.0),
                (300.0, 500.0),
                (500.0, 300.0),
                (300.0, 500.0)
            ]
        );
    }

    #[test]
    fn the_crop_box_wins_over_the_media_box() {
        let page = Spec {
            crop: Some([10.0, 20.0, 410.0, 320.0]),
            ..media(612.0, 792.0)
        };
        assert_eq!(
            sizes(&bytes(&mut build(&[page], None))),
            vec![(400.0, 300.0)]
        );
    }

    #[test]
    fn a_crop_box_outside_the_media_box_is_ignored() {
        let page = Spec {
            crop: Some([1000.0, 1000.0, 1200.0, 1200.0]),
            ..media(300.0, 400.0)
        };
        assert_eq!(
            sizes(&bytes(&mut build(&[page], None))),
            vec![(300.0, 400.0)]
        );
    }

    #[test]
    fn pages_inherit_the_media_box_from_the_page_tree() {
        let own = media(200.0, 300.0);
        let pdf = bytes(&mut build(
            &[Spec::default(), own],
            Some([0.0, 0.0, 400.0, 500.0]),
        ));
        assert_eq!(sizes(&pdf), vec![(400.0, 500.0), (200.0, 300.0)]);
    }

    #[test]
    fn a_page_with_no_box_is_letter() {
        let pdf = bytes(&mut build(&[Spec::default()], None));
        assert_eq!(sizes(&pdf), vec![(612.0, 792.0)]);
    }

    #[test]
    fn reversed_corners_are_normalised() {
        let page = Spec {
            media: Some([300.0, 400.0, 0.0, 0.0]),
            ..Spec::default()
        };
        assert_eq!(
            sizes(&bytes(&mut build(&[page], None))),
            vec![(300.0, 400.0)]
        );
    }

    #[test]
    fn absurd_sizes_are_clamped() {
        let pdf = pdf_of(&[(1.0, 1.0e7)]);
        assert_eq!(sizes(&pdf), vec![(MIN_SIDE, MAX_SIDE)]);
    }

    #[test]
    fn garbage_is_a_damaged_error() {
        assert!(matches!(
            read_page_sizes(b"not a pdf at all"),
            Err(PdfError::Damaged(_))
        ));
        assert!(matches!(read_page_sizes(b""), Err(PdfError::Damaged(_))));
        let mut pdf = pdf_of(&[(100.0, 100.0)]);
        pdf.truncate(pdf.len() / 3);
        assert!(read_page_sizes(&pdf).is_err());
    }

    #[test]
    fn a_pdf_without_pages_is_an_error() {
        let pdf = bytes(&mut build(&[], None));
        assert_eq!(read_page_sizes(&pdf), Err(PdfError::NoPages));
    }

    fn encrypted(user_password: &str) -> Vec<u8> {
        let mut doc = build(&[media(300.0, 400.0)], None);
        doc.trailer.set(
            "ID",
            Object::Array(vec![
                Object::string_literal(b"0123456789abcdef"),
                Object::string_literal(b"fedcba9876543210"),
            ]),
        );
        let state = lopdf::EncryptionState::try_from(lopdf::EncryptionVersion::V2 {
            document: &doc,
            owner_password: "owner",
            user_password,
            key_length: 128,
            permissions: lopdf::Permissions::all(),
        })
        .unwrap();
        doc.encrypt(&state).unwrap();
        bytes(&mut doc)
    }

    #[test]
    fn the_empty_user_password_opens_an_encrypted_pdf() {
        assert_eq!(sizes(&encrypted("")), vec![(300.0, 400.0)]);
    }

    #[test]
    fn a_password_protected_pdf_says_so() {
        let err = read_page_sizes(&encrypted("secret")).unwrap_err();
        assert_eq!(err, PdfError::Encrypted);
        assert_eq!(err.to_string(), "This PDF is password protected");
    }
}
