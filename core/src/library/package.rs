//! Library packages: many notebooks, canvases and folders in one file, for
//! sharing a part of the library or backing all of it up. The layout is
//! described in `docs/library-package.md`.
//!
//! A package never touches the notebook files directly. The shell hands in a
//! [`Shelf`] that opens existing notebook files and adds new ones, so the same
//! code runs over a folder on disk, a document provider or memory in tests.

use std::collections::{HashMap, HashSet, VecDeque};
use std::fmt;
use std::io::{self, Read, Seek, Write};

use serde::{Deserialize, Serialize};
use zip::write::SimpleFileOptions;
use zip::{CompressionMethod, ZipArchive, ZipWriter};

use super::error::LibraryError;
use super::id::Id;
use super::model::{ItemKind, NewItem};
use super::store::Library;
use crate::document::{self, Canvas};

/// The `format` a package manifest names.
pub const PACKAGE_FORMAT: &str = "grapho-library";
/// Newest package version this build reads and writes.
pub const PACKAGE_VERSION: u32 = 1;

const MANIFEST: &str = "manifest.json";
const INDEX: &str = "library.json";
const NOTEBOOKS: &str = "notebooks/";
/// Entries at or above this size need zip64 headers.
const ZIP64_AT: u64 = 0xFFFF_FFFF;

/// Where the notebook files live, as the shell sees them.
pub trait Shelf {
    type File: Read + Seek;

    /// Opens the notebook file `file_ref` for reading.
    fn open(&mut self, file_ref: &str) -> io::Result<Self::File>;

    /// Writes a new notebook file from `data`, dated `modified_at` (UTC
    /// milliseconds), and returns its file ref.
    fn add(&mut self, data: &mut dyn Read, modified_at: i64) -> io::Result<String>;

    /// Deletes a file [`Shelf::add`] wrote. Failing to delete is ignored: the
    /// file shows up as a new item at the next start, never lost.
    fn remove(&mut self, file_ref: &str);
}

/// Why a package could not be written or read.
#[derive(Debug)]
pub enum PackageError {
    Io(io::Error),
    Zip(zip::result::ZipError),
    Json(serde_json::Error),
    /// The file is not a library package. It may still be a single notebook.
    NotPackage,
    /// The package was written by a newer version.
    UnsupportedVersion(u32),
    /// The package is broken in a way that cannot be skipped.
    Corrupt(String),
    /// Nothing was picked, or nothing picked is out of Trash.
    Empty,
    Library(LibraryError),
}

impl PackageError {
    /// A short stable code for each kind, for shells to match on.
    pub fn kind(&self) -> &'static str {
        match self {
            PackageError::Io(_) => "io",
            PackageError::Zip(_) => "zip",
            PackageError::Json(_) => "invalid_json",
            PackageError::NotPackage => "not_package",
            PackageError::UnsupportedVersion(_) => "unsupported_version",
            PackageError::Corrupt(_) => "corrupt",
            PackageError::Empty => "empty",
            PackageError::Library(e) => e.kind(),
        }
    }
}

impl fmt::Display for PackageError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PackageError::Io(e) => write!(f, "couldn't read or write the file: {e}"),
            PackageError::Zip(e) => write!(f, "the file isn't a valid package: {e}"),
            PackageError::Json(e) => write!(f, "the package list is invalid: {e}"),
            PackageError::NotPackage => f.write_str("this isn't a Grapho library package"),
            PackageError::UnsupportedVersion(v) => write!(
                f,
                "this package uses version {v}, which needs a newer version of Grapho"
            ),
            PackageError::Corrupt(why) => write!(f, "the package is damaged: {why}"),
            PackageError::Empty => f.write_str("there is nothing to export"),
            PackageError::Library(e) => e.fmt(f),
        }
    }
}

impl std::error::Error for PackageError {}

impl From<io::Error> for PackageError {
    fn from(e: io::Error) -> Self {
        PackageError::Io(e)
    }
}

impl From<zip::result::ZipError> for PackageError {
    fn from(e: zip::result::ZipError) -> Self {
        PackageError::Zip(e)
    }
}

impl From<serde_json::Error> for PackageError {
    fn from(e: serde_json::Error) -> Self {
        PackageError::Json(e)
    }
}

impl From<LibraryError> for PackageError {
    fn from(e: LibraryError) -> Self {
        PackageError::Library(e)
    }
}

/// What an export wrote or an import added.
#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct PackageReport {
    pub folders: usize,
    pub items: usize,
    /// Notebooks left out because their file could not be read.
    pub skipped: usize,
    /// The file refs an import added, in package order.
    pub file_refs: Vec<String>,
}

#[derive(Serialize, Deserialize)]
struct Manifest {
    format: String,
    version: u32,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct PackFolder {
    id: Id,
    #[serde(default)]
    parent_id: Option<Id>,
    name: String,
    #[serde(default)]
    style_ref: Option<String>,
    #[serde(default)]
    created_at: i64,
    #[serde(default)]
    modified_at: i64,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct PackItem {
    id: Id,
    #[serde(default = "notebook_kind")]
    kind: ItemKind,
    title: String,
    #[serde(default)]
    folder_id: Option<Id>,
    #[serde(default)]
    cover_ref: Option<String>,
    #[serde(default)]
    starred: bool,
    #[serde(default)]
    page_count: u32,
    #[serde(default)]
    created_at: i64,
    #[serde(default)]
    modified_at: i64,
    #[serde(default)]
    opened_at: Option<i64>,
    /// The entry that holds the notebook, under `notebooks/`.
    file: String,
}

fn notebook_kind() -> ItemKind {
    ItemKind::Notebook
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Index {
    #[serde(default)]
    exported_at: i64,
    #[serde(default)]
    folders: Vec<PackFolder>,
    #[serde(default)]
    items: Vec<PackItem>,
}

/// Whether `reader` holds a library package, judged by its manifest alone.
pub fn is_package<R: Read + Seek>(reader: R) -> bool {
    ZipArchive::new(reader)
        .ok()
        .and_then(|mut zip| read_manifest(&mut zip).ok())
        .is_some()
}

fn read_manifest<R: Read + Seek>(zip: &mut ZipArchive<R>) -> Result<Manifest, PackageError> {
    let manifest: Manifest = match zip.by_name(MANIFEST) {
        Ok(f) => serde_json::from_reader(f).map_err(|_| PackageError::NotPackage)?,
        Err(zip::result::ZipError::FileNotFound) => return Err(PackageError::NotPackage),
        Err(e) => return Err(e.into()),
    };
    if manifest.format != PACKAGE_FORMAT {
        return Err(PackageError::NotPackage);
    }
    if manifest.version > PACKAGE_VERSION {
        return Err(PackageError::UnsupportedVersion(manifest.version));
    }
    Ok(manifest)
}

fn kind_of(doc: &document::Document) -> ItemKind {
    if matches!(doc.canvas, Canvas::Infinite { .. }) {
        ItemKind::Canvas
    } else if doc.pages.iter().any(|p| p.pdf.is_some()) {
        ItemKind::Pdf
    } else {
        ItemKind::Notebook
    }
}

fn readable<S: Shelf>(shelf: &mut S, file_ref: &str) -> Option<document::Document> {
    let file = shelf.open(file_ref).ok()?;
    document::load_info(file).ok()
}

impl Library {
    /// Writes the live `folders` with everything below them and the live
    /// `items` to `out` as one package. A picked folder whose parent is not
    /// picked becomes a top level folder, and a picked item outside the picked
    /// folders goes to the top level. Things in Trash are left out, and so is
    /// a notebook whose file cannot be read, which the report counts.
    pub fn export_package<W: Write + Seek, S: Shelf>(
        &self,
        folders: &[Id],
        items: &[Id],
        shelf: &mut S,
        out: W,
        now: i64,
    ) -> Result<PackageReport, PackageError> {
        let mut picked: HashSet<Id> = HashSet::new();
        let mut order: Vec<Id> = Vec::new();
        for &id in folders {
            if self.live_folder(id).is_ok() && !picked.contains(&id) {
                self.gather(id, &mut picked, &mut order);
            }
        }
        let mut pack_folders = Vec::with_capacity(order.len());
        for id in &order {
            let f = &self.folders[id];
            pack_folders.push(PackFolder {
                id: f.id,
                parent_id: f.parent_id.filter(|p| picked.contains(p)),
                name: f.name.clone(),
                style_ref: f.style_ref.clone(),
                created_at: f.created_at,
                modified_at: f.modified_at,
            });
        }

        let mut seen: HashSet<Id> = HashSet::new();
        let mut chosen = Vec::new();
        let mut inside: Vec<_> = self
            .items
            .values()
            .filter(|i| i.deleted_at.is_none() && i.folder_id.is_some_and(|f| picked.contains(&f)))
            .collect();
        inside.sort_by_key(|i| i.id);
        for item in inside {
            seen.insert(item.id);
            chosen.push(item);
        }
        for id in items {
            let Some(item) = self.items.get(id) else {
                continue;
            };
            let live = item.deleted_at.is_none()
                && !item.folder_id.is_some_and(|f| self.folder_in_trash(f));
            if live && seen.insert(item.id) {
                chosen.push(item);
            }
        }
        if pack_folders.is_empty() && chosen.is_empty() {
            return Err(PackageError::Empty);
        }

        let mut zip = ZipWriter::new(out);
        let small = SimpleFileOptions::default().compression_method(CompressionMethod::Deflated);
        zip.start_file(MANIFEST, small)?;
        serde_json::to_writer_pretty(
            &mut zip,
            &Manifest {
                format: PACKAGE_FORMAT.into(),
                version: PACKAGE_VERSION,
            },
        )?;

        let mut report = PackageReport::default();
        let mut pack_items = Vec::with_capacity(chosen.len());
        for item in chosen {
            // A notebook is a zip already, so it is stored as it is.
            let Ok(mut file) = shelf.open(&item.file_ref) else {
                report.skipped += 1;
                continue;
            };
            let len = file.seek(io::SeekFrom::End(0))?;
            file.seek(io::SeekFrom::Start(0))?;
            let entry = format!("{NOTEBOOKS}{}.grapho", item.id);
            let stored = SimpleFileOptions::default()
                .compression_method(CompressionMethod::Stored)
                .large_file(len >= ZIP64_AT);
            zip.start_file(entry.as_str(), stored)?;
            io::copy(&mut file, &mut zip)?;
            pack_items.push(PackItem {
                id: item.id,
                kind: item.kind,
                title: item.title.clone(),
                folder_id: item.folder_id.filter(|f| picked.contains(f)),
                cover_ref: item.cover_ref.clone(),
                starred: item.starred,
                page_count: item.page_count,
                created_at: item.created_at,
                modified_at: item.modified_at,
                opened_at: item.opened_at,
                file: entry,
            });
        }

        report.folders = pack_folders.len();
        report.items = pack_items.len();
        zip.start_file(INDEX, small)?;
        serde_json::to_writer_pretty(
            &mut zip,
            &Index {
                exported_at: now,
                folders: pack_folders,
                items: pack_items,
            },
        )?;
        zip.finish()?;
        Ok(report)
    }

    /// Adds every folder and notebook of the package in `input` under `into`
    /// (`None` for the top level), keeping the folder tree. Folder names that
    /// clash with a sibling get " 2", " 3" and so on. A notebook in the
    /// package that cannot be read is skipped and counted. The library only
    /// changes when the whole import succeeds; on failure every file it added
    /// is removed again.
    pub fn import_package<R: Read + Seek, S: Shelf>(
        &mut self,
        input: R,
        into: Option<Id>,
        shelf: &mut S,
        now: i64,
    ) -> Result<PackageReport, PackageError> {
        self.check_live_parent(into)?;
        let mut zip = ZipArchive::new(input).map_err(|e| match e {
            zip::result::ZipError::InvalidArchive(_) => PackageError::NotPackage,
            e => e.into(),
        })?;
        read_manifest(&mut zip)?;
        let index: Index = match zip.by_name(INDEX) {
            Ok(f) => serde_json::from_reader(f)?,
            Err(zip::result::ZipError::FileNotFound) => {
                return Err(PackageError::Corrupt(
                    "the list of contents is missing".into(),
                ))
            }
            Err(e) => return Err(e.into()),
        };

        let mut next = self.clone();
        let mut report = PackageReport::default();
        match next.import_index(&index, &mut zip, into, shelf, now, &mut report) {
            Ok(()) => {
                *self = next;
                Ok(report)
            }
            Err(e) => {
                for file_ref in &report.file_refs {
                    shelf.remove(file_ref);
                }
                Err(e)
            }
        }
    }

    fn import_index<R: Read + Seek, S: Shelf>(
        &mut self,
        index: &Index,
        zip: &mut ZipArchive<R>,
        into: Option<Id>,
        shelf: &mut S,
        now: i64,
        report: &mut PackageReport,
    ) -> Result<(), PackageError> {
        let known: HashSet<Id> = index.folders.iter().map(|f| f.id).collect();
        if known.len() != index.folders.len() {
            return Err(PackageError::Corrupt("a folder is listed twice".into()));
        }
        let mut children: HashMap<Option<Id>, Vec<&PackFolder>> = HashMap::new();
        for f in &index.folders {
            // A parent the package does not hold makes a top level folder.
            let parent = f.parent_id.filter(|p| known.contains(p) && *p != f.id);
            children.entry(parent).or_default().push(f);
        }
        // Parents first, breadth by breadth and siblings in package order, so
        // each folder goes last among its siblings in the order it was listed.
        // A cycle is never reached from the top, so its folders are left out.
        let mut made: HashMap<Id, Id> = HashMap::new();
        let mut queue: VecDeque<(Option<Id>, Option<Id>)> = VecDeque::from([(None, into)]);
        while let Some((from, to)) = queue.pop_front() {
            let Some(list) = children.get(&from) else {
                continue;
            };
            for f in list {
                let name = if f.name.trim().is_empty() {
                    "Folder"
                } else {
                    f.name.as_str()
                };
                let id = self.create_folder(to, name, now)?;
                if let Some(folder) = self.folders.get_mut(&id) {
                    folder.style_ref = f.style_ref.clone();
                    if f.created_at > 0 {
                        folder.created_at = f.created_at;
                    }
                }
                made.insert(f.id, id);
                queue.push_back((Some(f.id), Some(id)));
            }
        }
        report.folders = made.len();

        for item in &index.items {
            if !item.file.starts_with(NOTEBOOKS) {
                return Err(PackageError::Corrupt(format!(
                    "{} is outside the package",
                    item.file
                )));
            }
            let file_ref = {
                let mut entry = match zip.by_name(&item.file) {
                    Ok(e) => e,
                    Err(zip::result::ZipError::FileNotFound) => {
                        report.skipped += 1;
                        continue;
                    }
                    Err(e) => return Err(e.into()),
                };
                let modified = if item.modified_at > 0 {
                    item.modified_at
                } else {
                    now
                };
                shelf.add(&mut entry, modified)?
            };
            let Some(doc) = readable(shelf, &file_ref) else {
                shelf.remove(&file_ref);
                report.skipped += 1;
                continue;
            };
            report.file_refs.push(file_ref.clone());
            let folder = match item.folder_id.and_then(|f| made.get(&f)) {
                Some(&f) => Some(f),
                None => into,
            };
            let id = self.add_item(
                NewItem {
                    kind: kind_of(&doc),
                    title: item.title.clone(),
                    folder_id: folder,
                    cover_ref: item.cover_ref.clone(),
                    page_count: u32::try_from(doc.pages.len()).unwrap_or(item.page_count),
                    file_ref,
                    created_at: (item.created_at > 0).then_some(item.created_at),
                    modified_at: (item.modified_at > 0).then_some(item.modified_at),
                },
                now,
            )?;
            if let Some(added) = self.items.get_mut(&id) {
                added.starred = item.starred;
                added.opened_at = item.opened_at;
            }
            report.items += 1;
        }
        Ok(())
    }

    /// Adds `id` and every live folder below it, parents first and siblings in
    /// their manual order.
    fn gather(&self, id: Id, picked: &mut HashSet<Id>, order: &mut Vec<Id>) {
        let mut stack = vec![id];
        while let Some(cur) = stack.pop() {
            if !picked.insert(cur) {
                continue;
            }
            order.push(cur);
            let below = self.subfolders(Some(cur));
            stack.extend(below.iter().rev().map(|f| f.id));
        }
    }
}
