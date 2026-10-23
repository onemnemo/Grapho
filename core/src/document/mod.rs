//! The note document: pages, layers, strokes, images and text boxes.
//!
//! A [`Document`] is either paged (real paper sizes, one or more pages) or an
//! infinite canvas (a single unbounded page). Every page has its own ordered
//! layers, and strokes belong to a layer. Ids are random UUIDs so documents
//! edited on two devices can be merged later without collisions.

mod cut;
mod edit;
mod fill;
mod format;
mod image;
mod lasso;
mod model;
mod pdf;
mod strokes_file;
mod text;

pub use edit::{Editor, EraseMode, MAX_LAYERS};
pub use fill::{find_fill_region, search_area, FillView, SearchArea, Walls};
pub use format::{load, load_info, save, FormatError, FORMAT_VERSION};
#[cfg(test)]
pub(crate) use image::fixtures as image_fixtures;
pub use image::{
    compose, sniff, Image, ImageError, ImageKind, ImageSource, FULL_CROP, MAX_IMAGE_SIDE,
};
pub use model::*;
pub use pdf::PdfImport;
pub use text::{
    TextAlign, TextBox, TextError, TextStyle, MAX_TEXT_CHARS, MAX_TEXT_SIZE, MIN_TEXT_SIDE,
    MIN_TEXT_SIZE,
};
