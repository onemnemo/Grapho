//! The text side of loading: the checks a loaded file's text boxes must
//! pass. Text boxes live in the layers of `document.json`, so saving needs
//! nothing more than the format version.

use super::FormatError;
use crate::document::Document;

fn placed(doc: &Document) -> impl Iterator<Item = &crate::document::TextBox> {
    doc.pages
        .iter()
        .flat_map(|p| &p.layers)
        .flat_map(|l| &l.texts)
}

/// Whether any layer holds a text box, which makes the file version 4.
pub(super) fn any(doc: &Document) -> bool {
    placed(doc).next().is_some()
}

/// Checks that every text box has a usable place, size and text.
pub(super) fn check(doc: &Document) -> Result<(), FormatError> {
    if placed(doc).any(|t| !t.is_sound()) {
        return Err(FormatError::Corrupt("a text box is not valid".into()));
    }
    Ok(())
}
