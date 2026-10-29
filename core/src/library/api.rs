//! The library as JSON calls, so an ffi crate can expose it with a handful of
//! entry points and no logic of its own.

use serde::Serialize;
use serde_json::{json, Value};

use super::error::LibraryError;
use super::folders::CanMove;
use super::id::Id;
use super::model::{ItemKind, NewItem};
use super::query::{Listing, Sort, SortDir, SortKey, Window};
use super::store::Library;
use super::trash::RestoreMode;

type Reply = Result<Value, LibraryError>;

/// Runs the operation `op` on the library with JSON object `args` and returns
/// the JSON result. Ids are their 26 character text, times are UTC
/// milliseconds under `now`, and every name is camelCase as in the library
/// file.
///
/// Mutations: `createFolder`, `renameFolder`, `moveFolder`, `reorderFolder`,
/// `setFolderStyle`, `trashFolder`, `restoreFolder`, `deleteFolderForever`,
/// `addItem`, `setKind`, `renameItem`, `moveItems`, `setStarred`, `setCover`,
/// `touchOpened`, `touchModified`, `trashItems`, `restoreItem`,
/// `deleteItemForever`, `emptyTrash`, `purge`, `dropMissingFiles`.
///
/// Reads: `canMove`, `restoreNeedsChoice`, `subfolders`, `trashFolders`,
/// `folder`, `item`, `itemByFile`, `fileRefs`, `inFolder`, `recent`, `starred`, `trash`,
/// `all`. A listing takes `sort` (`edited`, `name`, `size`), `dir` (`asc`,
/// `desc`), `offset` and `limit`, and returns `{"total":n,"items":[...]}`.
///
/// An unknown operation or a missing argument is [`LibraryError::BadRequest`].
pub fn call(lib: &mut Library, op: &str, args: &str) -> Result<String, LibraryError> {
    let value: Value = if args.trim().is_empty() {
        json!({})
    } else {
        serde_json::from_str(args).map_err(|e| bad(&e.to_string()))?
    };
    let a = Args(value);
    let reply = dispatch(lib, op, &a)?;
    Ok(reply.to_string())
}

fn dispatch(lib: &mut Library, op: &str, a: &Args) -> Reply {
    match op {
        "createFolder" => {
            let id = lib.create_folder(a.opt_id("parentId")?, a.text("name")?, a.int("now")?)?;
            let name = lib.folder(id).map(|f| f.name.clone()).unwrap_or_default();
            Ok(json!({ "id": id, "name": name }))
        }
        "renameFolder" => {
            let name = lib.rename_folder(a.id("id")?, a.text("name")?, a.int("now")?)?;
            Ok(json!({ "name": name }))
        }
        "moveFolder" => {
            let name = lib.move_folder(a.id("id")?, a.opt_id("parentId")?, a.int("now")?)?;
            Ok(json!({ "name": name }))
        }
        "reorderFolder" => {
            unit(lib.reorder_folder(a.id("id")?, a.opt_id("afterId")?, a.int("now")?))
        }
        "setFolderStyle" => {
            unit(lib.set_folder_style(a.id("id")?, a.opt_text("styleRef")?, a.int("now")?))
        }
        "trashFolder" => {
            let batch = lib.trash_folder(a.id("id")?, a.int("now")?)?;
            Ok(json!({ "batchId": batch }))
        }
        "restoreFolder" => unit(lib.restore_folder(a.id("id")?, a.int("now")?)),
        "deleteFolderForever" => removed(lib.delete_folder_forever(a.id("id")?)?),
        "addItem" => {
            let kind = a.kind()?;
            let new = NewItem {
                kind,
                title: a.text("title")?.to_string(),
                folder_id: a.opt_id("folderId")?,
                cover_ref: a.opt_text("coverRef")?,
                page_count: u32::try_from(a.opt_int("pageCount")?.unwrap_or(0))
                    .map_err(|_| bad("pageCount"))?,
                file_ref: a.text("fileRef")?.to_string(),
                created_at: a.opt_int("createdAt")?,
                modified_at: a.opt_int("modifiedAt")?,
            };
            let id = lib.add_item(new, a.int("now")?)?;
            Ok(json!({ "id": id }))
        }
        "setKind" => unit(lib.set_kind(a.id("id")?, a.kind()?)),
        "renameItem" => unit(lib.rename_item(a.id("id")?, a.text("title")?, a.int("now")?)),
        "moveItems" => unit(lib.move_items(&a.ids("ids")?, a.opt_id("folderId")?)),
        "setStarred" => unit(lib.set_starred(a.id("id")?, a.flag("starred")?)),
        "setCover" => unit(lib.set_cover(a.id("id")?, a.opt_text("coverRef")?, a.int("now")?)),
        "touchOpened" => unit(lib.touch_opened(a.id("id")?, a.int("now")?)),
        "touchModified" => {
            let pages = match a.opt_int("pageCount")? {
                Some(n) => Some(u32::try_from(n).map_err(|_| bad("pageCount"))?),
                None => None,
            };
            unit(lib.touch_modified(a.id("id")?, a.int("now")?, pages))
        }
        "trashItems" => unit(lib.trash_items(&a.ids("ids")?, a.int("now")?)),
        "restoreItem" => {
            let mode = match a.opt_text("mode")?.as_deref() {
                None | Some("auto") => RestoreMode::Auto,
                Some("withFolder") => RestoreMode::WithFolder,
                Some("toLibrary") => RestoreMode::ToLibrary,
                Some(_) => return Err(bad("mode must be auto, withFolder or toLibrary")),
            };
            let folder = lib.restore_item(a.id("id")?, mode, a.int("now")?)?;
            Ok(json!({ "folderId": folder }))
        }
        "deleteItemForever" => removed(lib.delete_item_forever(a.id("id")?)?),
        "emptyTrash" => removed(lib.empty_trash()),
        "purge" => removed(lib.purge(a.int("now")?)),
        "dropMissingFiles" => {
            let gone = lib.drop_missing_files(&a.strings("files")?);
            Ok(json!({ "ids": gone }))
        }
        "canMove" => {
            let verdict = match lib.can_move(a.id("id")?, a.opt_id("targetId")?) {
                CanMove::Ok => "ok",
                CanMove::NameClashResolved => "name_clash_resolved",
                CanMove::IntoSelf => "into_self",
                CanMove::IntoDescendant => "into_descendant",
                CanMove::InTrash => "in_trash",
                CanMove::NotFound => "not_found",
            };
            Ok(json!({ "result": verdict }))
        }
        "restoreNeedsChoice" => {
            Ok(json!({ "needsChoice": lib.restore_needs_choice(a.id("id")?)? }))
        }
        "subfolders" => to_value(&lib.subfolders(a.opt_id("parentId")?)),
        "trashFolders" => to_value(&lib.trash_folders()),
        "folder" => to_value(&lib.folder(a.id("id")?)),
        "item" => to_value(&lib.item(a.id("id")?)),
        "itemByFile" => to_value(&lib.item_by_file(a.text("fileRef")?)),
        "fileRefs" => to_value(&lib.file_refs()),
        "inFolder" => listing(lib.in_folder(a.opt_id("folderId")?, a.sort()?, a.window()?)),
        "recent" => listing(lib.recent(a.sort()?, a.window()?)),
        "starred" => listing(lib.starred(a.sort()?, a.window()?)),
        "trash" => listing(lib.trash(a.sort()?, a.window()?)),
        "all" => listing(lib.all(a.sort()?, a.window()?)),
        other => Err(bad(&format!("unknown operation {other}"))),
    }
}

fn bad(why: &str) -> LibraryError {
    LibraryError::BadRequest(why.to_string())
}

fn unit(result: Result<(), LibraryError>) -> Reply {
    result.map(|()| json!({}))
}

fn removed(removed: super::trash::Removed) -> Reply {
    to_value(&removed)
}

fn to_value<T: Serialize>(value: &T) -> Reply {
    serde_json::to_value(value).map_err(|e| bad(&e.to_string()))
}

fn listing(list: Listing<'_>) -> Reply {
    Ok(json!({ "total": list.total, "items": list.items }))
}

struct Args(Value);

impl Args {
    fn get(&self, key: &str) -> Result<&Value, LibraryError> {
        self.0
            .get(key)
            .ok_or_else(|| bad(&format!("missing {key}")))
    }

    fn present(&self, key: &str) -> Option<&Value> {
        self.0.get(key).filter(|v| !v.is_null())
    }

    fn id(&self, key: &str) -> Result<Id, LibraryError> {
        self.get(key)?
            .as_str()
            .and_then(Id::parse)
            .ok_or_else(|| bad(&format!("{key} is not an id")))
    }

    fn opt_id(&self, key: &str) -> Result<Option<Id>, LibraryError> {
        match self.present(key) {
            None => Ok(None),
            Some(v) => v
                .as_str()
                .and_then(Id::parse)
                .map(Some)
                .ok_or_else(|| bad(&format!("{key} is not an id"))),
        }
    }

    fn ids(&self, key: &str) -> Result<Vec<Id>, LibraryError> {
        self.get(key)?
            .as_array()
            .ok_or_else(|| bad(&format!("{key} is not a list")))?
            .iter()
            .map(|v| {
                v.as_str()
                    .and_then(Id::parse)
                    .ok_or_else(|| bad(&format!("{key} holds a bad id")))
            })
            .collect()
    }

    fn strings(&self, key: &str) -> Result<Vec<String>, LibraryError> {
        self.get(key)?
            .as_array()
            .ok_or_else(|| bad(&format!("{key} is not a list")))?
            .iter()
            .map(|v| {
                v.as_str()
                    .map(str::to_string)
                    .ok_or_else(|| bad(&format!("{key} holds a non-string")))
            })
            .collect()
    }

    fn text(&self, key: &str) -> Result<&str, LibraryError> {
        self.get(key)?
            .as_str()
            .ok_or_else(|| bad(&format!("{key} is not text")))
    }

    fn opt_text(&self, key: &str) -> Result<Option<String>, LibraryError> {
        match self.present(key) {
            None => Ok(None),
            Some(v) => v
                .as_str()
                .map(|s| Some(s.to_string()))
                .ok_or_else(|| bad(&format!("{key} is not text"))),
        }
    }

    fn int(&self, key: &str) -> Result<i64, LibraryError> {
        self.get(key)?
            .as_i64()
            .ok_or_else(|| bad(&format!("{key} is not a whole number")))
    }

    fn opt_int(&self, key: &str) -> Result<Option<i64>, LibraryError> {
        match self.present(key) {
            None => Ok(None),
            Some(v) => v
                .as_i64()
                .map(Some)
                .ok_or_else(|| bad(&format!("{key} is not a whole number"))),
        }
    }

    fn kind(&self) -> Result<ItemKind, LibraryError> {
        serde_json::from_value(self.get("kind")?.clone())
            .map_err(|_| bad("kind must be notebook, pdf or canvas"))
    }

    fn flag(&self, key: &str) -> Result<bool, LibraryError> {
        self.get(key)?
            .as_bool()
            .ok_or_else(|| bad(&format!("{key} is not true or false")))
    }

    fn sort(&self) -> Result<Sort, LibraryError> {
        let key = match self.opt_text("sort")?.as_deref() {
            None | Some("edited") => SortKey::Edited,
            Some("name") => SortKey::Name,
            Some("size") => SortKey::Size,
            Some(_) => return Err(bad("sort must be edited, name or size")),
        };
        let dir = match self.opt_text("dir")?.as_deref() {
            None | Some("desc") => SortDir::Desc,
            Some("asc") => SortDir::Asc,
            Some(_) => return Err(bad("dir must be asc or desc")),
        };
        Ok(Sort { key, dir })
    }

    fn window(&self) -> Result<Window, LibraryError> {
        let offset = self.opt_int("offset")?.unwrap_or(0);
        let offset = usize::try_from(offset).map_err(|_| bad("offset"))?;
        let limit = match self.opt_int("limit")? {
            Some(n) => usize::try_from(n).map_err(|_| bad("limit"))?,
            None => usize::MAX,
        };
        Ok(Window { offset, limit })
    }
}
