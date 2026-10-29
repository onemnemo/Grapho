use std::collections::HashSet;

use serde::Serialize;

use super::error::LibraryError;
use super::id::Id;
use super::store::Library;

/// How long things stay in Trash before [`Library::purge`] removes them.
pub const TRASH_DAYS: i64 = 30;
const TRASH_MS: i64 = TRASH_DAYS * 24 * 60 * 60 * 1000;

/// Where [`Library::restore_item`] puts an item whose original folder is in
/// Trash.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum RestoreMode {
    /// Restore to the original folder, or fail with
    /// [`LibraryError::NeedsChoice`] if that folder is in Trash.
    Auto,
    /// Restore the folder too (and any deleted folders above it).
    WithFolder,
    /// Restore to the Library top level.
    ToLibrary,
}

/// What a permanent removal took out. The shell deletes the files.
#[derive(Clone, Debug, Default, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Removed {
    /// The `.grapho` file names of the removed items, sorted.
    pub file_refs: Vec<String>,
    pub items: usize,
    pub folders: usize,
}

impl Library {
    /// Sends live items to Trash. Nothing changes unless every id is a live
    /// item.
    pub fn trash_items(&mut self, ids: &[Id], now: i64) -> Result<(), LibraryError> {
        for &id in ids {
            let item = self.items.get(&id).ok_or(LibraryError::NotFound)?;
            if item.deleted_at.is_some() {
                return Err(LibraryError::InTrash);
            }
        }
        for &id in ids {
            if let Some(item) = self.items.get_mut(&id) {
                item.deleted_at = Some(now);
                item.deleted_from_folder_id = item.folder_id;
            }
        }
        Ok(())
    }

    /// Sends a folder to Trash. The folder and every live item inside it, at
    /// any depth, get `now` and one shared batch id, which is returned. Items
    /// and folders already in Trash keep their own times.
    pub fn trash_folder(&mut self, id: Id, now: i64) -> Result<Id, LibraryError> {
        self.live_folder(id)?;
        let batch = Id::random();
        let tree = self.subtree(id);
        if let Some(folder) = self.folders.get_mut(&id) {
            folder.deleted_at = Some(now);
            folder.delete_batch_id = Some(batch);
            folder.modified_at = now;
        }
        for item in self.items.values_mut() {
            let inside = item.folder_id.is_some_and(|f| tree.contains(&f));
            if inside && item.deleted_at.is_none() {
                item.deleted_at = Some(now);
                item.deleted_from_folder_id = item.folder_id;
                item.delete_batch_id = Some(batch);
            }
        }
        Ok(batch)
    }

    /// Whether restoring this item needs [`RestoreMode::WithFolder`] or
    /// [`RestoreMode::ToLibrary`] because its original folder is in Trash.
    pub fn restore_needs_choice(&self, id: Id) -> Result<bool, LibraryError> {
        let item = self.items.get(&id).ok_or(LibraryError::NotFound)?;
        if item.deleted_at.is_none() {
            return Err(LibraryError::NotInTrash);
        }
        Ok(self
            .original_folder(item.deleted_from_folder_id)
            .is_some_and(|f| self.folder_in_trash(f)))
    }

    /// Takes an item out of Trash and returns the folder it went to (`None`
    /// for the top level). It goes back to the folder it was deleted from, or
    /// to the top level if that folder is gone. If that folder is in Trash,
    /// `mode` decides, and [`RestoreMode::Auto`] fails with
    /// [`LibraryError::NeedsChoice`].
    pub fn restore_item(
        &mut self,
        id: Id,
        mode: RestoreMode,
        now: i64,
    ) -> Result<Option<Id>, LibraryError> {
        let item = self.items.get(&id).ok_or(LibraryError::NotFound)?;
        if item.deleted_at.is_none() {
            return Err(LibraryError::NotInTrash);
        }
        let from = self.original_folder(item.deleted_from_folder_id);
        let target = match from {
            Some(f) if self.folder_in_trash(f) => match mode {
                RestoreMode::Auto => return Err(LibraryError::NeedsChoice),
                RestoreMode::ToLibrary => None,
                RestoreMode::WithFolder => {
                    self.restore_chain(f, now);
                    Some(f)
                }
            },
            other => other,
        };
        if let Some(item) = self.items.get_mut(&id) {
            if item.deleted_at.is_some() {
                item.folder_id = target;
                clear_trash(item);
            }
        }
        Ok(target)
    }

    /// Takes a deleted folder out of Trash together with the items that went
    /// with it. It returns to its parent, or to the top level if the parent
    /// is gone or in Trash, and is renamed if a sibling has its name now.
    pub fn restore_folder(&mut self, id: Id, now: i64) -> Result<(), LibraryError> {
        let folder = self.folders.get(&id).ok_or(LibraryError::NotFound)?;
        if folder.deleted_at.is_none() {
            return Err(LibraryError::NotInTrash);
        }
        let batch = folder.delete_batch_id;
        let parent = folder
            .parent_id
            .filter(|&p| self.folders.contains_key(&p) && !self.folder_in_trash(p));
        let moved = parent != folder.parent_id;
        let wanted = folder.name.clone();
        let name = self.unique_name(parent, &wanted, Some(id))?;
        let order = moved.then(|| self.next_order(parent));
        if let Some(f) = self.folders.get_mut(&id) {
            f.parent_id = parent;
            f.name = name;
            f.deleted_at = None;
            f.delete_batch_id = None;
            f.modified_at = now;
            if let Some(order) = order {
                f.sort_order = order;
            }
        }
        if let Some(batch) = batch {
            for item in self.items.values_mut() {
                if item.deleted_at.is_some() && item.delete_batch_id == Some(batch) {
                    clear_trash(item);
                }
            }
        }
        Ok(())
    }

    /// Removes a Trash item for good and returns its file name.
    pub fn delete_item_forever(&mut self, id: Id) -> Result<Removed, LibraryError> {
        let item = self.items.get(&id).ok_or(LibraryError::NotFound)?;
        if item.deleted_at.is_none() {
            return Err(LibraryError::NotInTrash);
        }
        let mut removed = Removed::default();
        self.take_item(id, &mut removed);
        removed.file_refs.sort();
        Ok(removed)
    }

    /// Removes a deleted folder for good, with every folder and item inside.
    pub fn delete_folder_forever(&mut self, id: Id) -> Result<Removed, LibraryError> {
        let folder = self.folders.get(&id).ok_or(LibraryError::NotFound)?;
        if folder.deleted_at.is_none() {
            return Err(LibraryError::NotInTrash);
        }
        Ok(self.remove_trees(&[id], &[]))
    }

    /// Removes everything in Trash for good.
    pub fn empty_trash(&mut self) -> Removed {
        let roots = self.trash_roots(|_| true);
        let items = self.trash_items_where(|_| true);
        self.remove_trees(&roots, &items)
    }

    /// Removes what has been in Trash for more than [`TRASH_DAYS`] days at
    /// `now`, and returns the file names for the shell to delete.
    pub fn purge(&mut self, now: i64) -> Removed {
        let cutoff = now.saturating_sub(TRASH_MS);
        let roots = self.trash_roots(|at| at < cutoff);
        let items = self.trash_items_where(|at| at < cutoff);
        self.remove_trees(&roots, &items)
    }

    /// How many items are in Trash.
    pub fn trash_item_count(&self) -> usize {
        self.items
            .values()
            .filter(|i| i.deleted_at.is_some())
            .count()
    }

    fn original_folder(&self, from: Option<Id>) -> Option<Id> {
        from.filter(|f| self.folders.contains_key(f))
    }

    /// Restores `folder` and each deleted folder above it, top down.
    fn restore_chain(&mut self, folder: Id, now: i64) {
        let mut chain = Vec::new();
        let mut cur = Some(folder);
        for _ in 0..=self.folders.len() {
            let Some(f) = cur.and_then(|c| self.folders.get(&c)) else {
                break;
            };
            if f.deleted_at.is_some() {
                chain.push(f.id);
            }
            cur = f.parent_id;
        }
        for id in chain.into_iter().rev() {
            // A folder in the chain was just seen deleted, so this cannot fail
            // on state; a name error cannot happen as the name is not empty.
            let _ = self.restore_folder(id, now);
        }
    }

    fn trash_roots(&self, due: impl Fn(i64) -> bool) -> Vec<Id> {
        self.folders
            .values()
            .filter(|f| f.deleted_at.is_some_and(&due))
            .map(|f| f.id)
            .collect()
    }

    fn trash_items_where(&self, due: impl Fn(i64) -> bool) -> Vec<Id> {
        self.items
            .values()
            .filter(|i| i.deleted_at.is_some_and(&due))
            .map(|i| i.id)
            .collect()
    }

    /// Removes the folder trees under `roots` with all their items, and the
    /// extra `items`.
    fn remove_trees(&mut self, roots: &[Id], items: &[Id]) -> Removed {
        let mut removed = Removed::default();
        let mut doomed: HashSet<Id> = HashSet::new();
        if !roots.is_empty() {
            let children = self.children_map();
            let mut stack: Vec<Id> = roots.to_vec();
            while let Some(id) = stack.pop() {
                if doomed.insert(id) {
                    if let Some(kids) = children.get(&Some(id)) {
                        stack.extend(kids.iter().copied());
                    }
                }
            }
        }
        let inside: Vec<Id> = self
            .items
            .values()
            .filter(|i| i.folder_id.is_some_and(|f| doomed.contains(&f)))
            .map(|i| i.id)
            .collect();
        for id in inside.into_iter().chain(items.iter().copied()) {
            self.take_item(id, &mut removed);
        }
        for id in doomed {
            if self.folders.remove(&id).is_some() {
                removed.folders += 1;
            }
        }
        removed.file_refs.sort();
        removed
    }

    fn take_item(&mut self, id: Id, removed: &mut Removed) {
        if let Some(item) = self.remove_item(id) {
            removed.items += 1;
            removed.file_refs.push(item.file_ref);
        }
    }
}

fn clear_trash(item: &mut super::model::Item) {
    item.deleted_at = None;
    item.deleted_from_folder_id = None;
    item.delete_batch_id = None;
}
