//! The library: folders, items and Trash, owned by the core so every shell
//! behaves the same.
//!
//! A [`Library`] is the whole index of a person's notebooks. It never opens a
//! `.grapho` file: an [`Item`] only points at one by file name. Ids are 128 bit
//! random values written as 26 character Crockford base32 strings, and times
//! are UTC milliseconds that the caller passes in, so every operation is
//! deterministic and the core needs no clock.
//!
//! The library persists as one versioned JSON document ([`Library::to_json`]
//! and [`Library::from_json`]). Writing it to disk is the shell's job. Every
//! operation that can fail returns a [`LibraryError`]; nothing here panics.
//! [`call`] runs the same operations from JSON arguments, for ffi crates.

mod api;
mod error;
mod folders;
mod id;
mod items;
mod model;
mod package;
mod query;
mod store;
mod trash;

#[cfg(test)]
mod package_tests;
#[cfg(test)]
mod tests;

pub use api::call;
pub use error::LibraryError;
pub use folders::CanMove;
pub use id::Id;
pub use model::{Folder, Item, ItemKind, NewItem};
pub use package::{
    is_package, PackageError, PackageReport, Shelf, PACKAGE_FORMAT, PACKAGE_VERSION,
};
pub use query::{Listing, Sort, SortDir, SortKey, Window};
pub use store::{Library, FORMAT_VERSION};
pub use trash::{Removed, RestoreMode, TRASH_DAYS};
