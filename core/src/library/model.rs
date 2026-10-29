use serde::{Deserialize, Serialize};

use super::id::Id;

/// A folder. Folders nest without a depth limit.
///
/// Optional and defaulted fields keep older and newer files loadable.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Folder {
    pub id: Id,
    /// `None` is the Library top level.
    #[serde(default)]
    pub parent_id: Option<Id>,
    /// Unique among live siblings, ignoring case, after trimming.
    pub name: String,
    /// Folder style content ref, or `None` for the default.
    #[serde(default)]
    pub style_ref: Option<String>,
    /// Manual order among siblings, in steps of 1024.
    #[serde(default)]
    pub sort_order: i64,
    #[serde(default)]
    pub created_at: i64,
    #[serde(default)]
    pub modified_at: i64,
    /// Set on the deleted folder only. Its descendants are in Trash by
    /// inheritance.
    #[serde(default)]
    pub deleted_at: Option<i64>,
    /// Shared with the items that went to Trash with this folder.
    #[serde(default)]
    pub delete_batch_id: Option<Id>,
}

/// What a library item is. Append only: these names are on disk.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ItemKind {
    Notebook,
    Pdf,
    Canvas,
}

/// One document entry in the library. The library never reads the document
/// file to list it, so everything shown is cached here.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Item {
    pub id: Id,
    pub kind: ItemKind,
    pub title: String,
    /// `None` is the Library top level.
    #[serde(default)]
    pub folder_id: Option<Id>,
    /// Cover content ref, such as `color:#RRGGBB`, or `None` for the default.
    #[serde(default)]
    pub cover_ref: Option<String>,
    /// Cached page count, updated by [`Library::touch_modified`](super::Library::touch_modified).
    #[serde(default)]
    pub page_count: u32,
    #[serde(default)]
    pub created_at: i64,
    #[serde(default)]
    pub modified_at: i64,
    /// Drives Recent. `None` until the item is first opened.
    #[serde(default)]
    pub opened_at: Option<i64>,
    #[serde(default)]
    pub starred: bool,
    /// `None` means live.
    #[serde(default)]
    pub deleted_at: Option<i64>,
    /// The folder the item was in when it went to Trash, used by Restore.
    #[serde(default)]
    pub deleted_from_folder_id: Option<Id>,
    /// Set when the item went to Trash with a deleted folder.
    #[serde(default)]
    pub delete_batch_id: Option<Id>,
    /// The `.grapho` file name.
    pub file_ref: String,
}

/// What it takes to register a new item.
#[derive(Clone, Debug, PartialEq)]
pub struct NewItem {
    pub kind: ItemKind,
    pub title: String,
    pub folder_id: Option<Id>,
    pub cover_ref: Option<String>,
    pub page_count: u32,
    pub file_ref: String,
    /// When the document was created. Registering an existing file may pass
    /// its own time here; `None` uses `now`.
    pub created_at: Option<i64>,
    /// When the document last changed. `None` uses `now`.
    pub modified_at: Option<i64>,
}
