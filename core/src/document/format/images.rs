//! The image side of saving and loading: which sources a file keeps, where
//! their bytes live and the checks a loaded file must pass.

use std::io::{Read, Seek};

use zip::ZipArchive;

use super::FormatError;
use crate::document::{sniff, Document, Id, ImageSource};

pub(super) fn path(source: &ImageSource) -> String {
    format!("images/{}.{}", source.id, source.kind.extension())
}

/// Whether any layer places the source with this id.
fn is_used(doc: &Document, source: Id) -> bool {
    doc.pages
        .iter()
        .flat_map(|p| &p.layers)
        .any(|l| l.images.iter().any(|i| i.source == source))
}

/// The sources some layer places, in document order. A source nothing uses
/// is left out, so deleting the last image of a file drops its bytes.
pub(super) fn used(doc: &Document) -> Vec<&ImageSource> {
    doc.images.iter().filter(|s| is_used(doc, s.id)).collect()
}

/// Checks that every placed image names a listed source and has a usable
/// placement.
pub(super) fn check(doc: &Document) -> Result<(), FormatError> {
    let placed = doc
        .pages
        .iter()
        .flat_map(|p| &p.layers)
        .flat_map(|l| &l.images);
    for image in placed {
        if !doc.images.iter().any(|s| s.id == image.source) {
            return Err(FormatError::Corrupt(
                "an image refers to a picture that the file doesn't list".into(),
            ));
        }
        if !image.is_sound() {
            return Err(FormatError::Corrupt("an image has an invalid place".into()));
        }
    }
    Ok(())
}

/// Reads the bytes of every source that is placed, and checks each against
/// the kind and size the file states for it.
pub(super) fn read_all<R: Read + Seek>(
    zip: &mut ZipArchive<R>,
    doc: &mut Document,
) -> Result<(), FormatError> {
    let wanted: Vec<Id> = used(doc).iter().map(|s| s.id).collect();
    for source in doc.images.iter_mut().filter(|s| wanted.contains(&s.id)) {
        let mut bytes = Vec::new();
        match zip.by_name(&path(source)) {
            Ok(mut f) => f.read_to_end(&mut bytes)?,
            Err(zip::result::ZipError::FileNotFound) => {
                return Err(FormatError::Corrupt(format!(
                    "the picture {} is missing from the file",
                    source.id
                )))
            }
            Err(e) => return Err(e.into()),
        };
        match sniff(&bytes) {
            Ok(found) if found == (source.kind, source.width, source.height) => {}
            _ => {
                return Err(FormatError::Corrupt(format!(
                    "the picture {} is not what the file says it is",
                    source.id
                )))
            }
        }
        source.bytes = bytes.into();
    }
    Ok(())
}
