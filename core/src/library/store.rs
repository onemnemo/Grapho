use std::collections::{HashMap, HashSet};

use serde::{Deserialize, Serialize};

use super::error::LibraryError;
use super::id::Id;
use super::model::{Folder, Item};

/// The version of the library JSON this build writes and reads.
pub const FORMAT_VERSION: u32 = 1;

/// All folders and items. See the [module](super) for the model.
#[derive(Clone, Debug, Default)]
pub struct Library {
    pub(super) folders: HashMap<Id, Folder>,
    pub(super) items: HashMap<Id, Item>,
    /// The file names of all items, so registering a file stays constant time.
    pub(super) files: HashSet<String>,
}

#[derive(Serialize)]
struct DocOut<'a> {
    version: u32,
    folders: Vec<&'a Folder>,
    items: Vec<&'a Item>,
}

#[derive(Deserialize)]
struct Head {
    version: u32,
}

#[derive(Deserialize)]
struct DocIn {
    #[serde(default)]
    folders: Vec<Folder>,
    #[serde(default)]
    items: Vec<Item>,
}

impl Library {
    /// An empty library.
    pub fn new() -> Library {
        Library::default()
    }

    /// Reads a library from its JSON. Fields this version does not know are
    /// ignored. A version above [`FORMAT_VERSION`] is refused rather than
    /// half read. Damage that can be repaired is repaired: a second item for
    /// the same file is dropped, a folder cycle or a missing parent moves the
    /// folder to the top level, and a live item whose folder is missing or in
    /// Trash moves to the top level.
    pub fn from_json(json: &str) -> Result<Library, LibraryError> {
        let head: Head =
            serde_json::from_str(json).map_err(|e| LibraryError::InvalidJson(e.to_string()))?;
        if head.version > FORMAT_VERSION {
            return Err(LibraryError::UnsupportedVersion(head.version));
        }
        let doc: DocIn =
            serde_json::from_str(json).map_err(|e| LibraryError::InvalidJson(e.to_string()))?;
        let mut lib = Library::new();
        for folder in doc.folders {
            lib.folders.insert(folder.id, folder);
        }
        for item in doc.items {
            if lib.items.contains_key(&item.id) || !lib.files.insert(item.file_ref.clone()) {
                continue;
            }
            lib.items.insert(item.id, item);
        }
        lib.repair();
        Ok(lib)
    }

    /// The library as JSON, ordered by id so the same library always gives the
    /// same text.
    pub fn to_json(&self) -> String {
        let mut folders: Vec<&Folder> = self.folders.values().collect();
        folders.sort_by_key(|f| f.id);
        let mut items: Vec<&Item> = self.items.values().collect();
        items.sort_by_key(|i| i.id);
        let doc = DocOut {
            version: FORMAT_VERSION,
            folders,
            items,
        };
        serde_json::to_string(&doc).unwrap_or_else(|_| String::from("{\"version\":1}"))
    }

    /// The folder with this id, in Trash or not.
    pub fn folder(&self, id: Id) -> Option<&Folder> {
        self.folders.get(&id)
    }

    /// The item with this id, in Trash or not.
    pub fn item(&self, id: Id) -> Option<&Item> {
        self.items.get(&id)
    }

    /// How many folders there are, in Trash or not.
    pub fn folder_count(&self) -> usize {
        self.folders.len()
    }

    /// How many items there are, in Trash or not.
    pub fn item_count(&self) -> usize {
        self.items.len()
    }

    /// The file names of every item, so a shell can find files nobody knows.
    pub fn file_refs(&self) -> Vec<&str> {
        let mut refs: Vec<&str> = self.files.iter().map(String::as_str).collect();
        refs.sort_unstable();
        refs
    }

    /// Whether an item points at this file.
    pub fn has_file(&self, file_ref: &str) -> bool {
        self.files.contains(file_ref)
    }

    /// Ids of folders by parent, built in one pass.
    pub(super) fn children_map(&self) -> HashMap<Option<Id>, Vec<Id>> {
        let mut map: HashMap<Option<Id>, Vec<Id>> = HashMap::new();
        for f in self.folders.values() {
            map.entry(f.parent_id).or_default().push(f.id);
        }
        map
    }

    /// The folder and everything below it.
    pub(super) fn subtree(&self, root: Id) -> HashSet<Id> {
        let children = self.children_map();
        let mut seen = HashSet::new();
        let mut stack = vec![root];
        while let Some(id) = stack.pop() {
            if !seen.insert(id) {
                continue;
            }
            if let Some(kids) = children.get(&Some(id)) {
                stack.extend(kids.iter().copied());
            }
        }
        seen
    }

    /// Every folder that is in Trash: the ones with `deleted_at` and all
    /// below them.
    pub(super) fn trashed_folders(&self) -> HashSet<Id> {
        let children = self.children_map();
        let mut seen = HashSet::new();
        let mut stack: Vec<Id> = self
            .folders
            .values()
            .filter(|f| f.deleted_at.is_some())
            .map(|f| f.id)
            .collect();
        while let Some(id) = stack.pop() {
            if !seen.insert(id) {
                continue;
            }
            if let Some(kids) = children.get(&Some(id)) {
                stack.extend(kids.iter().copied());
            }
        }
        seen
    }

    /// Whether this folder is in Trash, itself or through an ancestor. A
    /// missing folder is not.
    pub(super) fn folder_in_trash(&self, id: Id) -> bool {
        let mut cur = Some(id);
        for _ in 0..=self.folders.len() {
            let Some(folder) = cur.and_then(|c| self.folders.get(&c)) else {
                return false;
            };
            if folder.deleted_at.is_some() {
                return true;
            }
            cur = folder.parent_id;
        }
        false
    }

    /// Removes an item and its file name. The caller returns the file name to
    /// the shell.
    pub(super) fn remove_item(&mut self, id: Id) -> Option<Item> {
        let item = self.items.remove(&id)?;
        self.files.remove(&item.file_ref);
        Some(item)
    }

    fn repair(&mut self) {
        let mut ids: Vec<Id> = self.folders.keys().copied().collect();
        ids.sort();
        for id in ids {
            let parent = self.folders.get(&id).and_then(|f| f.parent_id);
            if parent.is_some_and(|p| !self.folders.contains_key(&p)) {
                self.set_parent(id, None);
                continue;
            }
            if parent.is_some() && self.is_in_cycle(id) {
                self.set_parent(id, None);
            }
        }
        let trashed = self.trashed_folders();
        for item in self.items.values_mut() {
            let Some(folder) = item.folder_id else {
                continue;
            };
            let unusable = trashed.contains(&folder) || !self.folders.contains_key(&folder);
            if item.deleted_at.is_none() && unusable {
                item.folder_id = None;
            }
        }
    }

    fn set_parent(&mut self, id: Id, parent: Option<Id>) {
        if let Some(f) = self.folders.get_mut(&id) {
            f.parent_id = parent;
        }
    }

    fn is_in_cycle(&self, start: Id) -> bool {
        let mut cur = self.folders.get(&start).and_then(|f| f.parent_id);
        for _ in 0..self.folders.len() {
            match cur {
                None => return false,
                Some(id) if id == start => return true,
                Some(id) => cur = self.folders.get(&id).and_then(|f| f.parent_id),
            }
        }
        true
    }
}
