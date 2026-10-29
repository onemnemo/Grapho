use super::error::LibraryError;
use super::id::Id;
use super::model::{Item, ItemKind, NewItem};
use super::store::Library;

impl Library {
    /// Registers a document. The folder must exist and be out of Trash, and
    /// no other item may use the same file.
    pub fn add_item(&mut self, new: NewItem, now: i64) -> Result<Id, LibraryError> {
        self.check_live_parent(new.folder_id)?;
        if self.files.contains(&new.file_ref) {
            return Err(LibraryError::DuplicateFile);
        }
        let id = Id::random();
        self.files.insert(new.file_ref.clone());
        self.items.insert(
            id,
            Item {
                id,
                kind: new.kind,
                title: new.title.trim().to_string(),
                folder_id: new.folder_id,
                cover_ref: new.cover_ref,
                page_count: new.page_count,
                created_at: new.created_at.unwrap_or(now),
                modified_at: new.modified_at.unwrap_or(now),
                opened_at: None,
                starred: false,
                deleted_at: None,
                deleted_from_folder_id: None,
                delete_batch_id: None,
                file_ref: new.file_ref,
            },
        );
        Ok(id)
    }

    /// The item that points at this file name, in Trash or not.
    pub fn item_by_file(&self, file_ref: &str) -> Option<&Item> {
        if !self.files.contains(file_ref) {
            return None;
        }
        self.items.values().find(|i| i.file_ref == file_ref)
    }

    /// Sets the title, trimmed. An empty title is allowed.
    pub fn rename_item(&mut self, id: Id, title: &str, now: i64) -> Result<(), LibraryError> {
        let item = self.live_item_mut(id)?;
        item.title = title.trim().to_string();
        item.modified_at = now;
        Ok(())
    }

    /// Moves live items to `folder` (`None` for the top level). Nothing moves
    /// unless every id is a live item and the folder is out of Trash.
    pub fn move_items(&mut self, ids: &[Id], folder: Option<Id>) -> Result<(), LibraryError> {
        self.check_live_parent(folder)?;
        for &id in ids {
            self.live_item(id)?;
        }
        for &id in ids {
            if let Some(item) = self.items.get_mut(&id) {
                item.folder_id = folder;
            }
        }
        Ok(())
    }

    /// Stars or unstars a live item. Starring is not an edit, so
    /// `modified_at` stays.
    pub fn set_starred(&mut self, id: Id, starred: bool) -> Result<(), LibraryError> {
        self.live_item_mut(id)?.starred = starred;
        Ok(())
    }

    /// Sets the cover content ref, or `None` for the default cover.
    pub fn set_cover(
        &mut self,
        id: Id,
        cover_ref: Option<String>,
        now: i64,
    ) -> Result<(), LibraryError> {
        let item = self.live_item_mut(id)?;
        item.cover_ref = cover_ref;
        item.modified_at = now;
        Ok(())
    }

    /// Records that the item was opened, which Recent sorts by.
    pub fn touch_opened(&mut self, id: Id, now: i64) -> Result<(), LibraryError> {
        self.items
            .get_mut(&id)
            .ok_or(LibraryError::NotFound)?
            .opened_at = Some(now);
        Ok(())
    }

    /// Records that the document was saved, with its page count if it
    /// changed.
    pub fn touch_modified(
        &mut self,
        id: Id,
        now: i64,
        page_count: Option<u32>,
    ) -> Result<(), LibraryError> {
        let item = self.items.get_mut(&id).ok_or(LibraryError::NotFound)?;
        item.modified_at = now;
        if let Some(n) = page_count {
            item.page_count = n;
        }
        Ok(())
    }

    /// Sets the cached kind, for a document that changed what it is, such as a
    /// notebook that gained a PDF page.
    pub fn set_kind(&mut self, id: Id, kind: ItemKind) -> Result<(), LibraryError> {
        self.items.get_mut(&id).ok_or(LibraryError::NotFound)?.kind = kind;
        Ok(())
    }

    /// Drops every item whose file is not in `existing` (file names), for a
    /// shell that finds a document gone. Returns the dropped ids.
    pub fn drop_missing_files(&mut self, existing: &[String]) -> Vec<Id> {
        let existing: std::collections::HashSet<&str> =
            existing.iter().map(String::as_str).collect();
        let gone: Vec<Id> = self
            .items
            .values()
            .filter(|i| !existing.contains(i.file_ref.as_str()))
            .map(|i| i.id)
            .collect();
        for &id in &gone {
            self.remove_item(id);
        }
        gone
    }

    fn live_item(&self, id: Id) -> Result<&Item, LibraryError> {
        let item = self.items.get(&id).ok_or(LibraryError::NotFound)?;
        if item.deleted_at.is_some() {
            return Err(LibraryError::InTrash);
        }
        Ok(item)
    }

    fn live_item_mut(&mut self, id: Id) -> Result<&mut Item, LibraryError> {
        let item = self.items.get_mut(&id).ok_or(LibraryError::NotFound)?;
        if item.deleted_at.is_some() {
            return Err(LibraryError::InTrash);
        }
        Ok(item)
    }
}
