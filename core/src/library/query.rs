use std::cmp::Ordering;

use serde::{Deserialize, Serialize};

use super::id::Id;
use super::model::{Folder, Item};
use super::store::Library;

/// What a listing is ordered by.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SortKey {
    /// Last edit. For [`Library::recent`] it is the time last opened, and for
    /// [`Library::trash`] the time deleted.
    #[default]
    Edited,
    /// Title, ignoring case.
    Name,
    /// Page count.
    Size,
}

/// Which way a listing runs.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SortDir {
    /// Oldest first, A to Z, smallest first.
    Asc,
    /// Newest first, Z to A, largest first.
    #[default]
    Desc,
}

/// A sort. Items that tie are ordered by title (ignoring case, always A to
/// Z) and then id, so a listing is the same every time.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct Sort {
    pub key: SortKey,
    pub dir: SortDir,
}

/// Which part of a listing to return.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Window {
    pub offset: usize,
    pub limit: usize,
}

impl Window {
    /// Everything.
    pub fn all() -> Window {
        Window {
            offset: 0,
            limit: usize::MAX,
        }
    }
}

/// One page of a listing.
#[derive(Clone, Debug, PartialEq)]
pub struct Listing<'a> {
    /// How many items match, before the window.
    pub total: usize,
    /// The items in the window, in order.
    pub items: Vec<&'a Item>,
}

impl Library {
    /// Live items directly in `folder` (`None` for the top level).
    pub fn in_folder(&self, folder: Option<Id>, sort: Sort, window: Window) -> Listing<'_> {
        self.list(
            |i| i.deleted_at.is_none() && i.folder_id == folder,
            |i| i.modified_at,
            sort,
            window,
        )
    }

    /// Live items that have been opened, by time last opened.
    pub fn recent(&self, sort: Sort, window: Window) -> Listing<'_> {
        self.list(
            |i| i.deleted_at.is_none() && i.opened_at.is_some(),
            |i| i.opened_at.unwrap_or_default(),
            sort,
            window,
        )
    }

    /// Live starred items, in any folder.
    pub fn starred(&self, sort: Sort, window: Window) -> Listing<'_> {
        self.list(
            |i| i.deleted_at.is_none() && i.starred,
            |i| i.modified_at,
            sort,
            window,
        )
    }

    /// Items in Trash, by time deleted for the edited sort. This includes
    /// items that went with a deleted folder; group them with
    /// [`Item::delete_batch_id`].
    pub fn trash(&self, sort: Sort, window: Window) -> Listing<'_> {
        self.list(
            |i| i.deleted_at.is_some(),
            |i| i.deleted_at.unwrap_or_default(),
            sort,
            window,
        )
    }

    /// Every live item, in any folder.
    pub fn all(&self, sort: Sort, window: Window) -> Listing<'_> {
        self.list(|i| i.deleted_at.is_none(), |i| i.modified_at, sort, window)
    }

    /// The deleted folders that are the root of what was deleted, oldest
    /// deletion first.
    pub fn trash_folders(&self) -> Vec<&Folder> {
        let mut list: Vec<&Folder> = self
            .folders
            .values()
            .filter(|f| f.deleted_at.is_some())
            .collect();
        list.sort_by(|a, b| {
            a.deleted_at
                .cmp(&b.deleted_at)
                .then_with(|| a.name.to_lowercase().cmp(&b.name.to_lowercase()))
                .then_with(|| a.id.cmp(&b.id))
        });
        list
    }

    fn list(
        &self,
        keep: impl Fn(&Item) -> bool,
        time: impl Fn(&Item) -> i64,
        sort: Sort,
        window: Window,
    ) -> Listing<'_> {
        let mut rows: Vec<(String, &Item)> = self
            .items
            .values()
            .filter(|i| keep(i))
            .map(|i| (i.title.to_lowercase(), i))
            .collect();
        rows.sort_by(|(an, a), (bn, b)| {
            let primary = match sort.key {
                SortKey::Edited => time(a).cmp(&time(b)),
                SortKey::Name => an.cmp(bn),
                SortKey::Size => a.page_count.cmp(&b.page_count),
            };
            let primary = match sort.dir {
                SortDir::Asc => primary,
                SortDir::Desc => primary.reverse(),
            };
            let ties = if primary == Ordering::Equal && sort.key != SortKey::Name {
                an.cmp(bn)
            } else {
                Ordering::Equal
            };
            primary.then(ties).then_with(|| a.id.cmp(&b.id))
        });
        let total = rows.len();
        let items = rows
            .into_iter()
            .skip(window.offset)
            .take(window.limit)
            .map(|(_, i)| i)
            .collect();
        Listing { total, items }
    }
}
