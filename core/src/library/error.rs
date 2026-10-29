use std::fmt;

/// Why a library operation failed.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum LibraryError {
    /// The folder or item does not exist.
    NotFound,
    /// The thing, or the folder it would go into, is in Trash.
    InTrash,
    /// The thing is not in Trash.
    NotInTrash,
    /// A folder cannot move into itself.
    IntoSelf,
    /// A folder cannot move into one of its descendants.
    IntoDescendant,
    /// A folder name is empty after trimming.
    EmptyName,
    /// Another item already points at this file.
    DuplicateFile,
    /// Restoring needs a choice, see [`RestoreMode`](crate::library::RestoreMode).
    NeedsChoice,
    /// A request had a missing or malformed argument, or an unknown operation.
    BadRequest(String),
    /// The library JSON could not be read.
    InvalidJson(String),
    /// The library JSON was written by a newer version.
    UnsupportedVersion(u32),
}

impl LibraryError {
    /// A short stable code for each kind, for shells to match on.
    pub fn kind(&self) -> &'static str {
        match self {
            LibraryError::NotFound => "not_found",
            LibraryError::InTrash => "in_trash",
            LibraryError::NotInTrash => "not_in_trash",
            LibraryError::IntoSelf => "into_self",
            LibraryError::IntoDescendant => "into_descendant",
            LibraryError::EmptyName => "empty_name",
            LibraryError::DuplicateFile => "duplicate_file",
            LibraryError::NeedsChoice => "needs_choice",
            LibraryError::BadRequest(_) => "bad_request",
            LibraryError::InvalidJson(_) => "invalid_json",
            LibraryError::UnsupportedVersion(_) => "unsupported_version",
        }
    }
}

impl fmt::Display for LibraryError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            LibraryError::NotFound => f.write_str("no such folder or item"),
            LibraryError::InTrash => f.write_str("the folder or item is in Trash"),
            LibraryError::NotInTrash => f.write_str("the folder or item is not in Trash"),
            LibraryError::IntoSelf => f.write_str("a folder cannot move into itself"),
            LibraryError::IntoDescendant => {
                f.write_str("a folder cannot move into one of its own folders")
            }
            LibraryError::EmptyName => f.write_str("the name is empty"),
            LibraryError::DuplicateFile => f.write_str("another item already uses this file"),
            LibraryError::NeedsChoice => {
                f.write_str("the original folder is in Trash, choose where to restore")
            }
            LibraryError::BadRequest(why) => write!(f, "bad request: {why}"),
            LibraryError::InvalidJson(why) => write!(f, "the library is damaged: {why}"),
            LibraryError::UnsupportedVersion(v) => {
                write!(f, "the library is version {v}, which this app cannot read")
            }
        }
    }
}

impl std::error::Error for LibraryError {}
