use std::collections::HashSet;

use super::error::LibraryError;
use super::id::Id;
use super::model::Folder;
use super::store::Library;

/// Spacing between sibling `sort_order` values, so a reorder touches one row.
const ORDER_GAP: i64 = 1024;

/// What [`Library::can_move`] says about moving a folder.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum CanMove {
    /// The move is fine as it is.
    Ok,
    /// The move works, but a sibling already has the name, so the moved folder
    /// is renamed with " 2", " 3" and so on.
    NameClashResolved,
    /// The target is the folder itself.
    IntoSelf,
    /// The target is inside the folder.
    IntoDescendant,
    /// The folder or the target is in Trash.
    InTrash,
    /// The folder or the target does not exist.
    NotFound,
}

impl CanMove {
    /// Whether the move can go ahead.
    pub fn is_allowed(self) -> bool {
        matches!(self, CanMove::Ok | CanMove::NameClashResolved)
    }
}

fn fold(name: &str) -> String {
    name.to_lowercase()
}

impl Library {
    /// Makes a folder under `parent` (`None` for the top level). The name is
    /// trimmed and, if a live sibling has it, gets " 2", " 3" and so on. The
    /// folder goes last among its siblings.
    pub fn create_folder(
        &mut self,
        parent: Option<Id>,
        name: &str,
        now: i64,
    ) -> Result<Id, LibraryError> {
        self.check_live_parent(parent)?;
        let name = self.unique_name(parent, name, None)?;
        let id = Id::random();
        let folder = Folder {
            id,
            parent_id: parent,
            name,
            style_ref: None,
            sort_order: self.next_order(parent),
            created_at: now,
            modified_at: now,
            deleted_at: None,
            delete_batch_id: None,
        };
        self.folders.insert(id, folder);
        Ok(id)
    }

    /// Renames a folder and returns the name it got, which differs from the
    /// trimmed `name` when a sibling already has it.
    pub fn rename_folder(&mut self, id: Id, name: &str, now: i64) -> Result<String, LibraryError> {
        let folder = self.live_folder(id)?;
        let parent = folder.parent_id;
        let name = self.unique_name(parent, name, Some(id))?;
        let folder = self.folders.get_mut(&id).ok_or(LibraryError::NotFound)?;
        folder.name = name.clone();
        folder.modified_at = now;
        Ok(name)
    }

    /// Sets the folder's style content ref, or `None` for the default.
    pub fn set_folder_style(
        &mut self,
        id: Id,
        style_ref: Option<String>,
        now: i64,
    ) -> Result<(), LibraryError> {
        self.live_folder(id)?;
        let folder = self.folders.get_mut(&id).ok_or(LibraryError::NotFound)?;
        folder.style_ref = style_ref;
        folder.modified_at = now;
        Ok(())
    }

    /// Whether `folder` may move under `target` (`None` for the top level).
    pub fn can_move(&self, folder: Id, target: Option<Id>) -> CanMove {
        let Some(f) = self.folders.get(&folder) else {
            return CanMove::NotFound;
        };
        if let Some(t) = target {
            if !self.folders.contains_key(&t) {
                return CanMove::NotFound;
            }
            if t == folder {
                return CanMove::IntoSelf;
            }
            if self.is_descendant(t, folder) {
                return CanMove::IntoDescendant;
            }
            if self.folder_in_trash(t) {
                return CanMove::InTrash;
            }
        }
        if self.folder_in_trash(folder) {
            return CanMove::InTrash;
        }
        if f.parent_id == target {
            return CanMove::Ok;
        }
        let clash = self.folders.values().any(|s| {
            s.id != folder
                && s.parent_id == target
                && s.deleted_at.is_none()
                && fold(&s.name) == fold(&f.name)
        });
        if clash {
            CanMove::NameClashResolved
        } else {
            CanMove::Ok
        }
    }

    /// Moves a folder under `target` (`None` for the top level), last among
    /// its new siblings, and returns its name, which changes when a sibling
    /// has the same one. Moving to the folder's current parent changes
    /// nothing.
    pub fn move_folder(
        &mut self,
        id: Id,
        target: Option<Id>,
        now: i64,
    ) -> Result<String, LibraryError> {
        match self.can_move(id, target) {
            CanMove::IntoSelf => return Err(LibraryError::IntoSelf),
            CanMove::IntoDescendant => return Err(LibraryError::IntoDescendant),
            CanMove::InTrash => return Err(LibraryError::InTrash),
            CanMove::NotFound => return Err(LibraryError::NotFound),
            CanMove::Ok | CanMove::NameClashResolved => {}
        }
        let current = self.folders.get(&id).ok_or(LibraryError::NotFound)?;
        if current.parent_id == target {
            return Ok(current.name.clone());
        }
        let wanted = current.name.clone();
        let name = self.unique_name(target, &wanted, Some(id))?;
        let order = self.next_order(target);
        let folder = self.folders.get_mut(&id).ok_or(LibraryError::NotFound)?;
        folder.parent_id = target;
        folder.name = name.clone();
        folder.sort_order = order;
        folder.modified_at = now;
        Ok(name)
    }

    /// Places a folder among its siblings, right after `after`, or first when
    /// `after` is `None`. Only the moved folder is written unless the gap
    /// between its neighbours is used up, then the siblings are renumbered.
    pub fn reorder_folder(
        &mut self,
        id: Id,
        after: Option<Id>,
        now: i64,
    ) -> Result<(), LibraryError> {
        let parent = self.live_folder(id)?.parent_id;
        if after == Some(id) {
            return Ok(());
        }
        let mut siblings = self.live_siblings(parent);
        siblings.retain(|f| f.id != id);
        let (prev, next) = match after {
            None => (None, siblings.first().map(|f| f.sort_order)),
            Some(a) => {
                let pos = siblings
                    .iter()
                    .position(|f| f.id == a)
                    .ok_or(LibraryError::NotFound)?;
                (
                    Some(siblings[pos].sort_order),
                    siblings.get(pos + 1).map(|f| f.sort_order),
                )
            }
        };
        let order = match (prev, next) {
            (None, None) => Some(ORDER_GAP),
            (Some(p), None) => Some(p + ORDER_GAP),
            (None, Some(n)) => Some(n - ORDER_GAP),
            (Some(p), Some(n)) if n - p > 1 => Some(p + (n - p) / 2),
            _ => None,
        };
        match order {
            Some(order) => self.write_order(id, order, now),
            None => {
                let mut ids: Vec<Id> = siblings.iter().map(|f| f.id).collect();
                let at = after
                    .and_then(|a| ids.iter().position(|&i| i == a).map(|p| p + 1))
                    .unwrap_or(0);
                ids.insert(at, id);
                for (n, sibling) in ids.into_iter().enumerate() {
                    self.write_order(sibling, ORDER_GAP * (n as i64 + 1), now);
                }
            }
        }
        Ok(())
    }

    /// The live folders directly under `parent` (`None` for the top level) in
    /// their manual order, ties by name then id.
    pub fn subfolders(&self, parent: Option<Id>) -> Vec<&Folder> {
        self.live_siblings(parent)
    }

    fn live_siblings(&self, parent: Option<Id>) -> Vec<&Folder> {
        let mut list: Vec<&Folder> = self
            .folders
            .values()
            .filter(|f| f.parent_id == parent && f.deleted_at.is_none())
            .collect();
        list.sort_by(|a, b| {
            a.sort_order
                .cmp(&b.sort_order)
                .then_with(|| fold(&a.name).cmp(&fold(&b.name)))
                .then_with(|| a.id.cmp(&b.id))
        });
        list
    }

    fn write_order(&mut self, id: Id, order: i64, now: i64) {
        if let Some(f) = self.folders.get_mut(&id) {
            if f.sort_order != order {
                f.sort_order = order;
                f.modified_at = now;
            }
        }
    }

    pub(super) fn live_folder(&self, id: Id) -> Result<&Folder, LibraryError> {
        let folder = self.folders.get(&id).ok_or(LibraryError::NotFound)?;
        if self.folder_in_trash(id) {
            return Err(LibraryError::InTrash);
        }
        Ok(folder)
    }

    /// A parent for something new must exist and be out of Trash.
    pub(super) fn check_live_parent(&self, parent: Option<Id>) -> Result<(), LibraryError> {
        match parent {
            Some(p) => self.live_folder(p).map(|_| ()),
            None => Ok(()),
        }
    }

    pub(super) fn next_order(&self, parent: Option<Id>) -> i64 {
        self.folders
            .values()
            .filter(|f| f.parent_id == parent)
            .map(|f| f.sort_order)
            .max()
            .map_or(ORDER_GAP, |m| m.saturating_add(ORDER_GAP))
    }

    /// The trimmed `wanted` name, with " 2", " 3" and so on until no live
    /// sibling other than `except` has it.
    pub(super) fn unique_name(
        &self,
        parent: Option<Id>,
        wanted: &str,
        except: Option<Id>,
    ) -> Result<String, LibraryError> {
        let base = wanted.trim();
        if base.is_empty() {
            return Err(LibraryError::EmptyName);
        }
        let taken: HashSet<String> = self
            .folders
            .values()
            .filter(|f| f.parent_id == parent && f.deleted_at.is_none() && Some(f.id) != except)
            .map(|f| fold(&f.name))
            .collect();
        let mut name = base.to_string();
        let mut n = 2u32;
        while taken.contains(&fold(&name)) {
            name = format!("{base} {n}");
            n += 1;
        }
        Ok(name)
    }

    /// Whether `folder` lies below `ancestor`.
    fn is_descendant(&self, folder: Id, ancestor: Id) -> bool {
        let mut cur = self.folders.get(&folder).and_then(|f| f.parent_id);
        for _ in 0..self.folders.len() {
            match cur {
                None => return false,
                Some(id) if id == ancestor => return true,
                Some(id) => cur = self.folders.get(&id).and_then(|f| f.parent_id),
            }
        }
        false
    }
}
