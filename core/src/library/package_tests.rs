use std::collections::BTreeMap;
use std::io::{self, Cursor, Read, Write};

use zip::write::SimpleFileOptions;
use zip::ZipWriter;

use super::*;
use crate::document::{self, Document, Orientation, PaperSize};

/// Notebook files kept in memory, named `n1.grapho`, `n2.grapho` and so on.
#[derive(Default)]
struct MemShelf {
    files: BTreeMap<String, Vec<u8>>,
    dates: BTreeMap<String, i64>,
    next: usize,
    /// Fails the add after this many files, to test the rollback.
    fail_after: Option<usize>,
}

impl MemShelf {
    fn put(&mut self, doc: &Document) -> String {
        let mut out = Cursor::new(Vec::new());
        document::save(doc, &mut out).unwrap();
        self.next += 1;
        let name = format!("n{}.grapho", self.next);
        self.files.insert(name.clone(), out.into_inner());
        name
    }
}

impl Shelf for MemShelf {
    type File = Cursor<Vec<u8>>;

    fn open(&mut self, file_ref: &str) -> io::Result<Self::File> {
        self.files
            .get(file_ref)
            .cloned()
            .map(Cursor::new)
            .ok_or_else(|| io::Error::new(io::ErrorKind::NotFound, file_ref.to_string()))
    }

    fn add(&mut self, data: &mut dyn Read, modified_at: i64) -> io::Result<String> {
        if self.fail_after == Some(0) {
            return Err(io::Error::other("disk full"));
        }
        self.fail_after = self.fail_after.map(|n| n - 1);
        let mut bytes = Vec::new();
        data.read_to_end(&mut bytes)?;
        self.next += 1;
        let name = format!("n{}.grapho", self.next);
        self.files.insert(name.clone(), bytes);
        self.dates.insert(name.clone(), modified_at);
        Ok(name)
    }

    fn remove(&mut self, file_ref: &str) {
        self.files.remove(file_ref);
    }
}

fn paged(title: &str) -> Document {
    Document::new_paged(title, PaperSize::A4, Orientation::Portrait)
}

fn add(lib: &mut Library, shelf: &mut MemShelf, doc: &Document, folder: Option<Id>) -> Id {
    let file_ref = shelf.put(doc);
    lib.add_item(
        NewItem {
            kind: ItemKind::Notebook,
            title: doc.title.clone(),
            folder_id: folder,
            cover_ref: Some("color:#336699".into()),
            page_count: 1,
            file_ref,
            created_at: Some(100),
            modified_at: Some(200),
        },
        300,
    )
    .unwrap()
}

fn export(lib: &Library, shelf: &mut MemShelf, folders: &[Id], items: &[Id]) -> Vec<u8> {
    let mut out = Cursor::new(Vec::new());
    lib.export_package(folders, items, shelf, &mut out, 1_000)
        .unwrap();
    out.into_inner()
}

fn titles_in(lib: &Library, folder: Option<Id>) -> Vec<String> {
    let mut list: Vec<String> = lib
        .items
        .values()
        .filter(|i| i.folder_id == folder && i.deleted_at.is_none())
        .map(|i| i.title.clone())
        .collect();
    list.sort();
    list
}

fn names(lib: &Library, parent: Option<Id>) -> Vec<String> {
    lib.subfolders(parent)
        .iter()
        .map(|f| f.name.clone())
        .collect()
}

/// A library with School/{Maths, Biology/Cells} and notes at each level.
fn school(shelf: &mut MemShelf) -> (Library, Id) {
    let mut lib = Library::new();
    let school = lib.create_folder(None, "School", 1).unwrap();
    let maths = lib.create_folder(Some(school), "Maths", 1).unwrap();
    let bio = lib.create_folder(Some(school), "Biology", 1).unwrap();
    let cells = lib.create_folder(Some(bio), "Cells", 1).unwrap();
    add(&mut lib, shelf, &paged("Timetable"), Some(school));
    add(&mut lib, shelf, &paged("Algebra"), Some(maths));
    add(
        &mut lib,
        shelf,
        &Document::new_infinite("Mitosis"),
        Some(cells),
    );
    add(&mut lib, shelf, &paged("Loose"), None);
    (lib, school)
}

#[test]
fn a_folder_round_trips_with_its_tree() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let bytes = export(&lib, &mut shelf, &[school], &[]);
    assert!(is_package(Cursor::new(&bytes)));

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    let report = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!((report.folders, report.items, report.skipped), (4, 3, 0));

    assert_eq!(names(&target, None), ["School"]);
    let school2 = target.subfolders(None)[0].id;
    assert_eq!(names(&target, Some(school2)), ["Maths", "Biology"]);
    assert_eq!(titles_in(&target, Some(school2)), ["Timetable"]);
    let bio = target.subfolders(Some(school2))[1].id;
    let cells = target.subfolders(Some(bio))[0].id;
    assert_eq!(titles_in(&target, Some(cells)), ["Mitosis"]);
    let mitosis = target
        .items
        .values()
        .find(|i| i.title == "Mitosis")
        .unwrap();
    assert_eq!(mitosis.kind, ItemKind::Canvas);
    assert_eq!(mitosis.cover_ref.as_deref(), Some("color:#336699"));
    assert_eq!((mitosis.created_at, mitosis.modified_at), (100, 200));
    assert_eq!(dest.dates[&mitosis.file_ref], 200);
    // The loose note was not picked.
    assert!(titles_in(&target, None).is_empty());
}

#[test]
fn the_notebook_bytes_arrive_unchanged() {
    let mut shelf = MemShelf::default();
    let mut lib = Library::new();
    let id = add(&mut lib, &mut shelf, &paged("Exact"), None);
    let original = shelf.files[&lib.items[&id].file_ref].clone();
    let bytes = export(&lib, &mut shelf, &[], &[id]);

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    let report = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!(dest.files[&report.file_refs[0]], original);
}

#[test]
fn selecting_everything_at_the_top_backs_up_the_library() {
    let mut shelf = MemShelf::default();
    let (mut lib, school) = school(&mut shelf);
    let loose = lib.items.values().find(|i| i.title == "Loose").unwrap().id;
    lib.set_starred(loose, true).unwrap();
    let bytes = export(&lib, &mut shelf, &[school], &[loose]);

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!(target.item_count(), 4);
    assert_eq!(target.folder_count(), 4);
    assert_eq!(titles_in(&target, None), ["Loose"]);
    assert!(
        target
            .items
            .values()
            .find(|i| i.title == "Loose")
            .unwrap()
            .starred
    );
}

#[test]
fn a_nested_pick_becomes_top_level_and_items_are_not_doubled() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let maths = lib.subfolders(Some(school))[0].id;
    let algebra = lib
        .items
        .values()
        .find(|i| i.title == "Algebra")
        .unwrap()
        .id;
    let timetable = lib
        .items
        .values()
        .find(|i| i.title == "Timetable")
        .unwrap()
        .id;
    let bytes = export(&lib, &mut shelf, &[maths], &[algebra, timetable]);

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    let report = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!((report.folders, report.items), (1, 2));
    assert_eq!(names(&target, None), ["Maths"]);
    assert_eq!(titles_in(&target, None), ["Timetable"]);
}

#[test]
fn import_goes_into_a_folder_and_resolves_name_clashes() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let bytes = export(&lib, &mut shelf, &[school], &[]);

    let mut target = Library::new();
    let inbox = target.create_folder(None, "Inbox", 1).unwrap();
    target.create_folder(Some(inbox), "School", 1).unwrap();
    let mut dest = MemShelf::default();
    target
        .import_package(Cursor::new(bytes), Some(inbox), &mut dest, 5_000)
        .unwrap();
    assert_eq!(names(&target, Some(inbox)), ["School", "School 2"]);
}

#[test]
fn trash_is_left_out() {
    let mut shelf = MemShelf::default();
    let (mut lib, school) = school(&mut shelf);
    let maths = lib.subfolders(Some(school))[0].id;
    lib.trash_folder(maths, 10).unwrap();
    let loose = lib.items.values().find(|i| i.title == "Loose").unwrap().id;
    lib.trash_items(&[loose], 10).unwrap();
    let bytes = export(&lib, &mut shelf, &[school], &[loose]);

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    let report = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!((report.folders, report.items), (3, 2));
}

#[test]
fn nothing_to_export_is_an_error() {
    let mut shelf = MemShelf::default();
    let lib = Library::new();
    let mut out = Cursor::new(Vec::new());
    let err = lib
        .export_package(&[], &[Id::random()], &mut shelf, &mut out, 1)
        .unwrap_err();
    assert_eq!(err.kind(), "empty");
}

#[test]
fn a_missing_file_is_skipped_on_export() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let algebra = &lib
        .items
        .values()
        .find(|i| i.title == "Algebra")
        .unwrap()
        .file_ref;
    shelf.files.remove(algebra);
    let mut out = Cursor::new(Vec::new());
    let report = lib
        .export_package(&[school], &[], &mut shelf, &mut out, 1)
        .unwrap();
    assert_eq!((report.items, report.skipped), (2, 1));
}

#[test]
fn a_damaged_notebook_is_skipped_on_import() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let algebra = lib
        .items
        .values()
        .find(|i| i.title == "Algebra")
        .unwrap()
        .file_ref
        .clone();
    shelf.files.insert(algebra, b"not a zip".to_vec());
    let bytes = export(&lib, &mut shelf, &[school], &[]);

    let mut target = Library::new();
    let mut dest = MemShelf::default();
    let report = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap();
    assert_eq!((report.items, report.skipped), (2, 1));
    assert_eq!(dest.files.len(), 2);
}

#[test]
fn a_failed_import_changes_nothing() {
    let mut shelf = MemShelf::default();
    let (lib, school) = school(&mut shelf);
    let bytes = export(&lib, &mut shelf, &[school], &[]);

    let mut target = Library::new();
    let mut dest = MemShelf {
        fail_after: Some(1),
        ..MemShelf::default()
    };
    let err = target
        .import_package(Cursor::new(bytes), None, &mut dest, 5_000)
        .unwrap_err();
    assert_eq!(err.kind(), "io");
    assert_eq!((target.folder_count(), target.item_count()), (0, 0));
    assert!(dest.files.is_empty());
}

#[test]
fn a_single_notebook_is_not_a_package() {
    let mut shelf = MemShelf::default();
    let name = shelf.put(&paged("One"));
    let bytes = shelf.files[&name].clone();
    assert!(!is_package(Cursor::new(&bytes)));
    let mut target = Library::new();
    let err = target
        .import_package(Cursor::new(bytes), None, &mut MemShelf::default(), 1)
        .unwrap_err();
    assert_eq!(err.kind(), "not_package");
    let err = target
        .import_package(
            Cursor::new(b"plain text".to_vec()),
            None,
            &mut MemShelf::default(),
            1,
        )
        .unwrap_err();
    assert_eq!(err.kind(), "not_package");
}

fn handmade(manifest: &str, index: &str) -> Vec<u8> {
    let mut zip = ZipWriter::new(Cursor::new(Vec::new()));
    let opts = SimpleFileOptions::default();
    zip.start_file("manifest.json", opts).unwrap();
    zip.write_all(manifest.as_bytes()).unwrap();
    zip.start_file("library.json", opts).unwrap();
    zip.write_all(index.as_bytes()).unwrap();
    zip.finish().unwrap().into_inner()
}

#[test]
fn newer_packages_are_refused() {
    let bytes = handmade(r#"{"format":"grapho-library","version":99}"#, "{}");
    let err = Library::new()
        .import_package(Cursor::new(bytes), None, &mut MemShelf::default(), 1)
        .unwrap_err();
    assert_eq!(err.kind(), "unsupported_version");
}

#[test]
fn a_folder_cycle_is_left_out_and_paths_stay_inside() {
    let a = Id::random();
    let b = Id::random();
    let index = format!(
        r#"{{"folders":[{{"id":"{a}","parentId":"{b}","name":"A"}},{{"id":"{b}","parentId":"{a}","name":"B"}}],"items":[]}}"#
    );
    let bytes = handmade(r#"{"format":"grapho-library","version":1}"#, &index);
    let mut lib = Library::new();
    let report = lib
        .import_package(Cursor::new(bytes), None, &mut MemShelf::default(), 1)
        .unwrap();
    assert_eq!(report.folders, 0);

    let index = format!(r#"{{"items":[{{"id":"{a}","title":"X","file":"../escape.grapho"}}]}}"#);
    let bytes = handmade(r#"{"format":"grapho-library","version":1}"#, &index);
    let err = lib
        .import_package(Cursor::new(bytes), None, &mut MemShelf::default(), 1)
        .unwrap_err();
    assert_eq!(err.kind(), "corrupt");
}
