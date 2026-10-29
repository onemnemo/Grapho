use super::*;

const T0: i64 = 1_700_000_000_000;
const DAY: i64 = 24 * 60 * 60 * 1000;

fn new_item(title: &str, folder: Option<Id>, file: &str) -> NewItem {
    NewItem {
        kind: ItemKind::Notebook,
        title: title.to_string(),
        folder_id: folder,
        cover_ref: None,
        page_count: 1,
        file_ref: file.to_string(),
        created_at: None,
        modified_at: None,
    }
}

fn add(lib: &mut Library, title: &str, folder: Option<Id>) -> Id {
    let file = format!("{title}-{}.grapho", lib.item_count());
    lib.add_item(new_item(title, folder, &file), T0).unwrap()
}

fn folder(lib: &mut Library, parent: Option<Id>, name: &str) -> Id {
    lib.create_folder(parent, name, T0).unwrap()
}

fn titles(list: &Listing) -> Vec<String> {
    list.items.iter().map(|i| i.title.clone()).collect()
}

fn by(key: SortKey, dir: SortDir) -> Sort {
    Sort { key, dir }
}

#[test]
fn id_text_is_26_crockford_characters() {
    let id = Id::from_u128(0);
    assert_eq!(id.to_string(), "0".repeat(26));
    let max = Id::from_u128(u128::MAX);
    assert_eq!(max.to_string(), format!("7{}", "Z".repeat(25)));
    let random = Id::random().to_string();
    assert_eq!(random.len(), 26);
    assert!(random
        .bytes()
        .all(|b| b.is_ascii_digit() || b.is_ascii_uppercase()));
    assert!(!random.contains(['I', 'L', 'O', 'U']));
}

#[test]
fn id_round_trips_through_text() {
    for bits in [
        0u128,
        1,
        31,
        32,
        u128::MAX,
        0x0123_4567_89ab_cdef_0123_4567_89ab_cdef,
    ] {
        let id = Id::from_u128(bits);
        assert_eq!(Id::parse(&id.to_string()), Some(id));
    }
    for _ in 0..100 {
        let id = Id::random();
        assert_eq!(Id::parse(&id.to_string()), Some(id));
    }
}

#[test]
fn id_parse_forgives_case_and_lookalikes() {
    let id = Id::parse("0000000000000000000000001Z").unwrap();
    assert_eq!(Id::parse("0000000000000000000000001z"), Some(id));
    assert_eq!(Id::parse("OOOOOOOOOOOOOOOOOOOOOOOO1Z"), Some(id));
}

#[test]
fn id_parse_rejects_bad_text() {
    assert_eq!(Id::parse(""), None);
    assert_eq!(Id::parse("0000"), None);
    assert_eq!(Id::parse(&"0".repeat(27)), None);
    assert_eq!(Id::parse(&format!("U{}", "0".repeat(25))), None);
    // The first character holds only 3 bits.
    assert_eq!(Id::parse(&format!("8{}", "0".repeat(25))), None);
    assert_eq!(Id::parse(&format!("{}é", "0".repeat(24))), None);
}

#[test]
fn ids_are_not_repeated() {
    let ids: std::collections::HashSet<Id> = (0..10_000).map(|_| Id::random()).collect();
    assert_eq!(ids.len(), 10_000);
}

#[test]
fn folder_names_are_trimmed_and_unique_among_siblings() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "  Maths ");
    let b = folder(&mut lib, None, "Maths");
    let c = folder(&mut lib, None, "maths");
    let d = folder(&mut lib, None, "Maths");
    assert_eq!(lib.folder(a).unwrap().name, "Maths");
    assert_eq!(lib.folder(b).unwrap().name, "Maths 2");
    assert_eq!(lib.folder(c).unwrap().name, "maths 3");
    assert_eq!(lib.folder(d).unwrap().name, "Maths 4");
}

#[test]
fn same_name_is_fine_under_different_parents() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, None, "B");
    let x = folder(&mut lib, Some(a), "Notes");
    let y = folder(&mut lib, Some(b), "Notes");
    assert_eq!(lib.folder(x).unwrap().name, "Notes");
    assert_eq!(lib.folder(y).unwrap().name, "Notes");
}

#[test]
fn empty_folder_name_is_refused() {
    let mut lib = Library::new();
    assert_eq!(
        lib.create_folder(None, "   ", T0),
        Err(LibraryError::EmptyName)
    );
    let a = folder(&mut lib, None, "A");
    assert_eq!(lib.rename_folder(a, "", T0), Err(LibraryError::EmptyName));
}

#[test]
fn create_folder_needs_a_live_parent() {
    let mut lib = Library::new();
    let missing = Id::random();
    assert_eq!(
        lib.create_folder(Some(missing), "x", T0),
        Err(LibraryError::NotFound)
    );
    let a = folder(&mut lib, None, "A");
    lib.trash_folder(a, T0).unwrap();
    assert_eq!(
        lib.create_folder(Some(a), "x", T0),
        Err(LibraryError::InTrash)
    );
}

#[test]
fn sort_order_steps_by_1024() {
    let mut lib = Library::new();
    let ids: Vec<Id> = (0..4)
        .map(|n| folder(&mut lib, None, &format!("f{n}")))
        .collect();
    let orders: Vec<i64> = ids
        .iter()
        .map(|&i| lib.folder(i).unwrap().sort_order)
        .collect();
    assert_eq!(orders, vec![1024, 2048, 3072, 4096]);
}

#[test]
fn rename_folder_resolves_clashes_but_not_with_itself() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, None, "B");
    assert_eq!(lib.rename_folder(b, "A", T0 + 1).unwrap(), "A 2");
    assert_eq!(lib.rename_folder(a, " A ", T0 + 2).unwrap(), "A");
    assert_eq!(lib.folder(a).unwrap().modified_at, T0 + 2);
    assert_eq!(
        lib.rename_folder(Id::random(), "x", T0),
        Err(LibraryError::NotFound)
    );
}

#[test]
fn can_move_gives_reasons() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, Some(a), "B");
    let c = folder(&mut lib, Some(b), "C");
    let other = folder(&mut lib, None, "Other");
    assert_eq!(lib.can_move(a, Some(a)), CanMove::IntoSelf);
    assert_eq!(lib.can_move(a, Some(b)), CanMove::IntoDescendant);
    assert_eq!(lib.can_move(a, Some(c)), CanMove::IntoDescendant);
    assert_eq!(lib.can_move(c, Some(a)), CanMove::Ok);
    assert_eq!(lib.can_move(c, None), CanMove::Ok);
    assert_eq!(lib.can_move(a, Some(other)), CanMove::Ok);
    assert_eq!(lib.can_move(a, Some(Id::random())), CanMove::NotFound);
    assert_eq!(lib.can_move(Id::random(), None), CanMove::NotFound);
    assert!(CanMove::Ok.is_allowed());
    assert!(!CanMove::IntoSelf.is_allowed());
}

#[test]
fn can_move_reports_clash_and_trash() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "Same");
    let target = folder(&mut lib, None, "Target");
    let _twin = folder(&mut lib, Some(target), "same");
    assert_eq!(lib.can_move(a, Some(target)), CanMove::NameClashResolved);
    assert!(lib.can_move(a, Some(target)).is_allowed());
    lib.trash_folder(target, T0).unwrap();
    assert_eq!(lib.can_move(a, Some(target)), CanMove::InTrash);
    assert_eq!(lib.can_move(target, None), CanMove::InTrash);
}

#[test]
fn move_folder_renames_on_clash_and_goes_last() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "Same");
    let target = folder(&mut lib, None, "Target");
    let twin = folder(&mut lib, Some(target), "Same");
    assert_eq!(lib.move_folder(a, Some(target), T0 + 5).unwrap(), "Same 2");
    let f = lib.folder(a).unwrap();
    assert_eq!(f.parent_id, Some(target));
    assert_eq!(f.modified_at, T0 + 5);
    assert!(f.sort_order > lib.folder(twin).unwrap().sort_order);
}

#[test]
fn move_folder_refuses_cycles_and_keeps_the_tree() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, Some(a), "B");
    assert_eq!(lib.move_folder(a, Some(a), T0), Err(LibraryError::IntoSelf));
    assert_eq!(
        lib.move_folder(a, Some(b), T0),
        Err(LibraryError::IntoDescendant)
    );
    assert_eq!(lib.folder(a).unwrap().parent_id, None);
    assert_eq!(lib.folder(b).unwrap().parent_id, Some(a));
    lib.move_folder(b, None, T0).unwrap();
    assert_eq!(lib.folder(b).unwrap().parent_id, None);
}

#[test]
fn move_folder_to_its_own_parent_changes_nothing() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, None, "B");
    let before = lib.folder(a).unwrap().clone();
    assert_eq!(lib.move_folder(a, None, T0 + 9).unwrap(), "A");
    assert_eq!(lib.folder(a).unwrap(), &before);
    assert_ne!(lib.folder(b).unwrap().sort_order, 0);
}

#[test]
fn subfolders_follow_manual_order() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, None, "B");
    let c = folder(&mut lib, None, "C");
    let inner = folder(&mut lib, Some(a), "Inner");
    let ids = |lib: &Library, p| lib.subfolders(p).iter().map(|f| f.id).collect::<Vec<_>>();
    assert_eq!(ids(&lib, None), vec![a, b, c]);
    assert_eq!(ids(&lib, Some(a)), vec![inner]);
    lib.reorder_folder(c, None, T0).unwrap();
    assert_eq!(ids(&lib, None), vec![c, a, b]);
    lib.reorder_folder(c, Some(a), T0).unwrap();
    assert_eq!(ids(&lib, None), vec![a, c, b]);
    lib.reorder_folder(a, Some(b), T0).unwrap();
    assert_eq!(ids(&lib, None), vec![c, b, a]);
    assert_eq!(
        lib.reorder_folder(a, Some(inner), T0),
        Err(LibraryError::NotFound)
    );
}

#[test]
fn reorder_touches_one_folder_while_there_is_room() {
    let mut lib = Library::new();
    let ids: Vec<Id> = (0..5)
        .map(|n| folder(&mut lib, None, &format!("f{n}")))
        .collect();
    let before: Vec<i64> = ids
        .iter()
        .map(|&i| lib.folder(i).unwrap().sort_order)
        .collect();
    lib.reorder_folder(ids[4], Some(ids[0]), T0 + 1).unwrap();
    let after: Vec<i64> = ids
        .iter()
        .map(|&i| lib.folder(i).unwrap().sort_order)
        .collect();
    let changed = before.iter().zip(&after).filter(|(b, a)| b != a).count();
    assert_eq!(changed, 1);
}

#[test]
fn reorder_renumbers_when_the_gap_is_used_up() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    let b = folder(&mut lib, None, "B");
    let mover = folder(&mut lib, None, "M");
    // Squeeze: a and b are 1 apart.
    lib.folders.get_mut(&a).unwrap().sort_order = 10;
    lib.folders.get_mut(&b).unwrap().sort_order = 11;
    lib.reorder_folder(mover, Some(a), T0).unwrap();
    let order: Vec<Id> = lib.subfolders(None).iter().map(|f| f.id).collect();
    assert_eq!(order, vec![a, mover, b]);
    let numbers: Vec<i64> = lib.subfolders(None).iter().map(|f| f.sort_order).collect();
    assert_eq!(numbers, vec![1024, 2048, 3072]);
}

#[test]
fn folder_style_can_be_set() {
    let mut lib = Library::new();
    let a = folder(&mut lib, None, "A");
    lib.set_folder_style(a, Some("style:wood".into()), T0)
        .unwrap();
    assert_eq!(
        lib.folder(a).unwrap().style_ref.as_deref(),
        Some("style:wood")
    );
    lib.set_folder_style(a, None, T0).unwrap();
    assert_eq!(lib.folder(a).unwrap().style_ref, None);
}

#[test]
fn add_item_checks_folder_and_file() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let id = lib
        .add_item(new_item("  Hello ", Some(f), "a.grapho"), T0)
        .unwrap();
    let item = lib.item(id).unwrap();
    assert_eq!(item.title, "Hello");
    assert_eq!((item.created_at, item.modified_at), (T0, T0));
    assert_eq!(item.opened_at, None);
    assert!(!item.starred);
    assert_eq!(
        lib.add_item(new_item("Again", None, "a.grapho"), T0),
        Err(LibraryError::DuplicateFile)
    );
    assert_eq!(
        lib.add_item(new_item("x", Some(Id::random()), "b.grapho"), T0),
        Err(LibraryError::NotFound)
    );
    lib.trash_folder(f, T0).unwrap();
    assert_eq!(
        lib.add_item(new_item("x", Some(f), "b.grapho"), T0),
        Err(LibraryError::InTrash)
    );
    assert!(lib.has_file("a.grapho"));
    assert!(!lib.has_file("b.grapho"));
}

#[test]
fn add_item_can_carry_the_files_own_times() {
    let mut lib = Library::new();
    let mut new = new_item("Old", None, "o.grapho");
    new.created_at = Some(5);
    new.modified_at = Some(9);
    let id = lib.add_item(new, T0).unwrap();
    let item = lib.item(id).unwrap();
    assert_eq!((item.created_at, item.modified_at), (5, 9));
}

#[test]
fn item_edits() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let id = add(&mut lib, "One", None);
    lib.rename_item(id, " Two ", T0 + 1).unwrap();
    assert_eq!(lib.item(id).unwrap().title, "Two");
    assert_eq!(lib.item(id).unwrap().modified_at, T0 + 1);
    lib.set_starred(id, true).unwrap();
    assert!(lib.item(id).unwrap().starred);
    assert_eq!(lib.item(id).unwrap().modified_at, T0 + 1);
    lib.set_cover(id, Some("color:#FF0000".into()), T0 + 2)
        .unwrap();
    assert_eq!(
        lib.item(id).unwrap().cover_ref.as_deref(),
        Some("color:#FF0000")
    );
    lib.move_items(&[id], Some(f)).unwrap();
    assert_eq!(lib.item(id).unwrap().folder_id, Some(f));
    assert_eq!(lib.item(id).unwrap().modified_at, T0 + 2);
    lib.touch_opened(id, T0 + 3).unwrap();
    assert_eq!(lib.item(id).unwrap().opened_at, Some(T0 + 3));
    lib.touch_modified(id, T0 + 4, Some(7)).unwrap();
    let item = lib.item(id).unwrap();
    assert_eq!((item.modified_at, item.page_count), (T0 + 4, 7));
    lib.touch_modified(id, T0 + 5, None).unwrap();
    assert_eq!(lib.item(id).unwrap().page_count, 7);
    lib.set_kind(id, ItemKind::Pdf).unwrap();
    assert_eq!(lib.item(id).unwrap().kind, ItemKind::Pdf);
    let missing = Id::random();
    assert_eq!(
        lib.rename_item(missing, "x", T0),
        Err(LibraryError::NotFound)
    );
    assert_eq!(lib.touch_opened(missing, T0), Err(LibraryError::NotFound));
}

#[test]
fn move_items_is_all_or_nothing() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", None);
    let b = add(&mut lib, "B", None);
    assert_eq!(
        lib.move_items(&[a, Id::random()], Some(f)),
        Err(LibraryError::NotFound)
    );
    assert_eq!(lib.item(a).unwrap().folder_id, None);
    assert_eq!(
        lib.move_items(&[a], Some(Id::random())),
        Err(LibraryError::NotFound)
    );
    lib.move_items(&[a, b], Some(f)).unwrap();
    assert_eq!(lib.item(b).unwrap().folder_id, Some(f));
    lib.trash_items(&[a], T0).unwrap();
    assert_eq!(lib.move_items(&[a], None), Err(LibraryError::InTrash));
    assert_eq!(lib.move_items(&[b], Some(f)), Ok(()));
    lib.trash_folder(f, T0).unwrap();
    let c = add(&mut lib, "C", None);
    assert_eq!(lib.move_items(&[c], Some(f)), Err(LibraryError::InTrash));
}

#[test]
fn trashed_items_refuse_edits() {
    let mut lib = Library::new();
    let a = add(&mut lib, "A", None);
    lib.trash_items(&[a], T0).unwrap();
    assert_eq!(lib.rename_item(a, "x", T0), Err(LibraryError::InTrash));
    assert_eq!(lib.set_starred(a, true), Err(LibraryError::InTrash));
    assert_eq!(lib.set_cover(a, None, T0), Err(LibraryError::InTrash));
    assert_eq!(lib.trash_items(&[a], T0), Err(LibraryError::InTrash));
}

#[test]
fn item_by_file_finds_the_item() {
    let mut lib = Library::new();
    let a = lib.add_item(new_item("A", None, "a.grapho"), T0).unwrap();
    lib.trash_items(&[a], T0).unwrap();
    assert_eq!(lib.item_by_file("a.grapho").map(|i| i.id), Some(a));
    assert!(lib.item_by_file("b.grapho").is_none());
}

#[test]
fn drop_missing_files_drops_only_missing() {
    let mut lib = Library::new();
    let a = lib.add_item(new_item("A", None, "a.grapho"), T0).unwrap();
    let b = lib.add_item(new_item("B", None, "b.grapho"), T0).unwrap();
    let gone = lib.drop_missing_files(&["a.grapho".to_string(), "other.grapho".to_string()]);
    assert_eq!(gone, vec![b]);
    assert!(lib.item(a).is_some());
    assert!(lib.item(b).is_none());
    assert!(!lib.has_file("b.grapho"));
    assert_eq!(lib.file_refs(), vec!["a.grapho"]);
    // The file name is free again.
    lib.add_item(new_item("B2", None, "b.grapho"), T0).unwrap();
}

#[test]
fn trashing_an_item_remembers_its_folder() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    lib.trash_items(&[a], T0 + 7).unwrap();
    let item = lib.item(a).unwrap();
    assert_eq!(item.deleted_at, Some(T0 + 7));
    assert_eq!(item.deleted_from_folder_id, Some(f));
    assert_eq!(item.delete_batch_id, None);
    assert_eq!(
        lib.in_folder(Some(f), Sort::default(), Window::all()).total,
        0
    );
    assert_eq!(lib.trash(Sort::default(), Window::all()).total, 1);
    assert_eq!(lib.trash_item_count(), 1);
}

#[test]
fn trash_items_is_all_or_nothing() {
    let mut lib = Library::new();
    let a = add(&mut lib, "A", None);
    assert_eq!(
        lib.trash_items(&[a, Id::random()], T0),
        Err(LibraryError::NotFound)
    );
    assert_eq!(lib.item(a).unwrap().deleted_at, None);
}

#[test]
fn deleting_a_folder_marks_root_and_live_items_with_one_batch() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    let deep = folder(&mut lib, Some(sub), "Deep");
    let a = add(&mut lib, "A", Some(f));
    let b = add(&mut lib, "B", Some(deep));
    let kept = add(&mut lib, "Kept", None);
    let earlier = add(&mut lib, "Earlier", Some(sub));
    lib.trash_items(&[earlier], T0).unwrap();
    let batch = lib.trash_folder(f, T0 + 100).unwrap();
    let root = lib.folder(f).unwrap();
    assert_eq!(root.deleted_at, Some(T0 + 100));
    assert_eq!(root.delete_batch_id, Some(batch));
    // Descendant folders are in Trash by inheritance only.
    assert_eq!(lib.folder(sub).unwrap().deleted_at, None);
    assert_eq!(lib.folder(deep).unwrap().deleted_at, None);
    for id in [a, b] {
        let item = lib.item(id).unwrap();
        assert_eq!(item.deleted_at, Some(T0 + 100));
        assert_eq!(item.delete_batch_id, Some(batch));
        assert!(item.deleted_from_folder_id.is_some());
    }
    assert_eq!(lib.item(kept).unwrap().deleted_at, None);
    // The earlier deletion keeps its own time and has no batch.
    assert_eq!(lib.item(earlier).unwrap().deleted_at, Some(T0));
    assert_eq!(lib.item(earlier).unwrap().delete_batch_id, None);
    assert_eq!(lib.trash_folders().len(), 1);
    assert_eq!(lib.trash(Sort::default(), Window::all()).total, 3);
    assert_eq!(lib.all(Sort::default(), Window::all()).total, 1);
}

#[test]
fn deleting_a_folder_in_trash_is_refused() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    lib.trash_folder(f, T0).unwrap();
    assert_eq!(lib.trash_folder(f, T0), Err(LibraryError::InTrash));
    assert_eq!(lib.trash_folder(sub, T0), Err(LibraryError::InTrash));
    assert_eq!(
        lib.trash_folder(Id::random(), T0),
        Err(LibraryError::NotFound)
    );
    assert_eq!(lib.rename_folder(sub, "x", T0), Err(LibraryError::InTrash));
}

#[test]
fn restoring_a_folder_restores_exactly_its_batch() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    let a = add(&mut lib, "A", Some(sub));
    let earlier = add(&mut lib, "Earlier", Some(f));
    lib.trash_items(&[earlier], T0).unwrap();
    lib.trash_folder(f, T0 + 1).unwrap();
    lib.restore_folder(f, T0 + 2).unwrap();
    assert_eq!(lib.folder(f).unwrap().deleted_at, None);
    assert_eq!(lib.folder(f).unwrap().delete_batch_id, None);
    assert_eq!(lib.item(a).unwrap().deleted_at, None);
    assert_eq!(lib.item(a).unwrap().folder_id, Some(sub));
    assert_eq!(lib.item(earlier).unwrap().deleted_at, Some(T0));
    assert_eq!(lib.restore_folder(f, T0), Err(LibraryError::NotInTrash));
    assert_eq!(
        lib.restore_folder(Id::random(), T0),
        Err(LibraryError::NotFound)
    );
}

#[test]
fn restoring_a_folder_whose_parent_is_in_trash_goes_to_the_top() {
    let mut lib = Library::new();
    let parent = folder(&mut lib, None, "Parent");
    let child = folder(&mut lib, Some(parent), "Child");
    let a = add(&mut lib, "A", Some(child));
    lib.trash_folder(child, T0).unwrap();
    lib.trash_folder(parent, T0 + 1).unwrap();
    lib.restore_folder(child, T0 + 2).unwrap();
    assert_eq!(lib.folder(child).unwrap().parent_id, None);
    assert_eq!(lib.item(a).unwrap().deleted_at, None);
    assert_eq!(lib.subfolders(None).len(), 1);
}

#[test]
fn restoring_a_folder_resolves_a_name_clash() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "Maths");
    lib.trash_folder(f, T0).unwrap();
    let new = folder(&mut lib, None, "Maths");
    lib.restore_folder(f, T0 + 1).unwrap();
    assert_eq!(lib.folder(new).unwrap().name, "Maths");
    assert_eq!(lib.folder(f).unwrap().name, "Maths 2");
}

#[test]
fn restoring_an_item_goes_back_to_its_folder() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    lib.trash_items(&[a], T0).unwrap();
    assert_eq!(lib.restore_needs_choice(a), Ok(false));
    assert_eq!(lib.restore_item(a, RestoreMode::Auto, T0), Ok(Some(f)));
    let item = lib.item(a).unwrap();
    assert_eq!(item.folder_id, Some(f));
    assert_eq!((item.deleted_at, item.deleted_from_folder_id), (None, None));
    assert_eq!(
        lib.restore_item(a, RestoreMode::Auto, T0),
        Err(LibraryError::NotInTrash)
    );
    assert_eq!(lib.restore_needs_choice(a), Err(LibraryError::NotInTrash));
    assert_eq!(
        lib.restore_item(Id::random(), RestoreMode::Auto, T0),
        Err(LibraryError::NotFound)
    );
}

#[test]
fn restoring_an_item_whose_folder_is_gone_goes_to_the_top() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    lib.trash_items(&[a], T0).unwrap();
    lib.trash_folder(f, T0).unwrap();
    lib.delete_folder_forever(f).unwrap();
    // The item sat in the folder, so it went with it.
    assert!(lib.item(a).is_none());

    let g = folder(&mut lib, None, "G");
    let b = add(&mut lib, "B", Some(g));
    lib.trash_items(&[b], T0).unwrap();
    lib.items.get_mut(&b).unwrap().folder_id = None;
    lib.folders.remove(&g);
    assert_eq!(lib.restore_needs_choice(b), Ok(false));
    assert_eq!(lib.restore_item(b, RestoreMode::Auto, T0), Ok(None));
    assert_eq!(lib.item(b).unwrap().folder_id, None);
}

#[test]
fn restoring_an_item_in_a_trashed_folder_needs_a_choice() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    let b = add(&mut lib, "B", Some(f));
    lib.trash_items(&[a], T0).unwrap();
    lib.trash_folder(f, T0 + 1).unwrap();
    assert_eq!(lib.restore_needs_choice(a), Ok(true));
    assert_eq!(
        lib.restore_item(a, RestoreMode::Auto, T0),
        Err(LibraryError::NeedsChoice)
    );
    assert!(lib.item(a).unwrap().deleted_at.is_some());

    // To Library: only the item comes back.
    assert_eq!(lib.restore_item(a, RestoreMode::ToLibrary, T0), Ok(None));
    assert_eq!(lib.item(a).unwrap().folder_id, None);
    assert!(lib.folder(f).unwrap().deleted_at.is_some());
    assert!(lib.item(b).unwrap().deleted_at.is_some());
}

#[test]
fn restoring_with_the_folder_brings_back_the_folder_and_its_batch() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    let b = add(&mut lib, "B", Some(f));
    lib.trash_folder(f, T0).unwrap();
    assert_eq!(
        lib.restore_item(a, RestoreMode::WithFolder, T0 + 1),
        Ok(Some(f))
    );
    assert_eq!(lib.folder(f).unwrap().deleted_at, None);
    assert_eq!(lib.item(a).unwrap().deleted_at, None);
    assert_eq!(lib.item(b).unwrap().deleted_at, None);
}

#[test]
fn restoring_with_the_folder_also_restores_deleted_ancestors() {
    let mut lib = Library::new();
    let top = folder(&mut lib, None, "Top");
    let mid = folder(&mut lib, Some(top), "Mid");
    let a = add(&mut lib, "A", Some(mid));
    lib.trash_items(&[a], T0).unwrap();
    lib.trash_folder(mid, T0 + 1).unwrap();
    lib.trash_folder(top, T0 + 2).unwrap();
    assert_eq!(lib.restore_needs_choice(a), Ok(true));
    assert_eq!(
        lib.restore_item(a, RestoreMode::WithFolder, T0 + 3),
        Ok(Some(mid))
    );
    assert_eq!(lib.folder(top).unwrap().deleted_at, None);
    assert_eq!(lib.folder(mid).unwrap().deleted_at, None);
    assert_eq!(lib.folder(mid).unwrap().parent_id, Some(top));
    // The item was trashed on its own, not with the folder, and still comes back.
    let item = lib.item(a).unwrap();
    assert_eq!((item.deleted_at, item.folder_id), (None, Some(mid)));
}

#[test]
fn with_folder_is_the_same_as_auto_when_the_folder_is_live() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    lib.trash_items(&[a], T0).unwrap();
    assert_eq!(
        lib.restore_item(a, RestoreMode::WithFolder, T0),
        Ok(Some(f))
    );
}

#[test]
fn purge_removes_only_what_is_older_than_30_days() {
    let mut lib = Library::new();
    let old = add(&mut lib, "Old", None);
    let edge = add(&mut lib, "Edge", None);
    let fresh = add(&mut lib, "Fresh", None);
    let live = add(&mut lib, "Live", None);
    lib.trash_items(&[old], T0 - 31 * DAY).unwrap();
    lib.trash_items(&[edge], T0 - 30 * DAY).unwrap();
    lib.trash_items(&[fresh], T0 - DAY).unwrap();
    let removed = lib.purge(T0);
    assert_eq!(removed.items, 1);
    assert!(removed.file_refs[0].starts_with("Old-"));
    assert!(lib.item(old).is_none());
    assert!(lib.item(edge).is_some());
    assert!(lib.item(fresh).is_some());
    assert!(lib.item(live).is_some());
    assert!(!lib.has_file(&removed.file_refs[0]));
    // One more millisecond tips the edge item over.
    let removed = lib.purge(T0 + 1);
    assert_eq!(removed.items, 1);
    assert!(removed.file_refs[0].starts_with("Edge-"));
}

#[test]
fn purge_takes_old_folders_with_everything_inside() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    let a = lib
        .add_item(new_item("A", Some(f), "a.grapho"), T0)
        .unwrap();
    let b = lib
        .add_item(new_item("B", Some(sub), "b.grapho"), T0)
        .unwrap();
    let outside = add(&mut lib, "Out", None);
    let young = folder(&mut lib, None, "Young");
    lib.trash_folder(f, T0 - 40 * DAY).unwrap();
    lib.trash_folder(young, T0 - DAY).unwrap();
    let removed = lib.purge(T0);
    assert_eq!(removed.file_refs, vec!["a.grapho", "b.grapho"]);
    assert_eq!((removed.items, removed.folders), (2, 2));
    assert!(lib.folder(f).is_none() && lib.folder(sub).is_none());
    assert!(lib.item(a).is_none() && lib.item(b).is_none());
    assert!(lib.item(outside).is_some());
    assert!(lib.folder(young).is_some());
    assert_eq!(lib.purge(T0), Removed::default());
}

#[test]
fn delete_forever_returns_files() {
    let mut lib = Library::new();
    let a = lib.add_item(new_item("A", None, "a.grapho"), T0).unwrap();
    assert_eq!(lib.delete_item_forever(a), Err(LibraryError::NotInTrash));
    lib.trash_items(&[a], T0).unwrap();
    let removed = lib.delete_item_forever(a).unwrap();
    assert_eq!(removed.file_refs, vec!["a.grapho"]);
    assert_eq!(lib.delete_item_forever(a), Err(LibraryError::NotFound));

    let f = folder(&mut lib, None, "F");
    let b = lib
        .add_item(new_item("B", Some(f), "b.grapho"), T0)
        .unwrap();
    assert_eq!(lib.delete_folder_forever(f), Err(LibraryError::NotInTrash));
    lib.trash_folder(f, T0).unwrap();
    let removed = lib.delete_folder_forever(f).unwrap();
    assert_eq!(removed.file_refs, vec!["b.grapho"]);
    assert_eq!((removed.items, removed.folders), (1, 1));
    assert!(lib.item(b).is_none() && lib.folder(f).is_none());
}

#[test]
fn deleting_a_descendant_folder_forever_needs_its_own_deletion() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    lib.trash_folder(f, T0).unwrap();
    assert_eq!(
        lib.delete_folder_forever(sub),
        Err(LibraryError::NotInTrash)
    );
}

#[test]
fn empty_trash_removes_everything_in_trash_and_nothing_else() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let live_folder = folder(&mut lib, None, "Live");
    lib.add_item(new_item("In", Some(f), "in.grapho"), T0)
        .unwrap();
    let single = lib
        .add_item(new_item("Single", None, "single.grapho"), T0)
        .unwrap();
    let keep = lib
        .add_item(new_item("Keep", Some(live_folder), "keep.grapho"), T0)
        .unwrap();
    lib.trash_items(&[single], T0).unwrap();
    lib.trash_folder(f, T0).unwrap();
    let removed = lib.empty_trash();
    assert_eq!(removed.file_refs, vec!["in.grapho", "single.grapho"]);
    assert_eq!((removed.items, removed.folders), (2, 1));
    assert_eq!(lib.item_count(), 1);
    assert!(lib.item(keep).is_some());
    assert_eq!(lib.folder_count(), 1);
    assert_eq!(lib.empty_trash(), Removed::default());
}

fn sample() -> (Library, [Id; 4]) {
    let mut lib = Library::new();
    let mut ids = [Id::random(); 4];
    let rows = [
        ("banana", 10, 3),
        ("Apple", 30, 9),
        ("cherry", 20, 3),
        ("avocado", 20, 1),
    ];
    for (n, (title, modified, pages)) in rows.iter().enumerate() {
        let mut new = new_item(title, None, &format!("{n}.grapho"));
        new.modified_at = Some(*modified);
        new.page_count = *pages;
        ids[n] = lib.add_item(new, T0).unwrap();
    }
    (lib, ids)
}

#[test]
fn sort_edited_name_and_size() {
    let (lib, _) = sample();
    let q = |key, dir| titles(&lib.all(by(key, dir), Window::all()));
    assert_eq!(
        q(SortKey::Edited, SortDir::Desc),
        ["Apple", "avocado", "cherry", "banana"]
    );
    assert_eq!(
        q(SortKey::Edited, SortDir::Asc),
        ["banana", "avocado", "cherry", "Apple"]
    );
    assert_eq!(
        q(SortKey::Name, SortDir::Asc),
        ["Apple", "avocado", "banana", "cherry"]
    );
    assert_eq!(
        q(SortKey::Size, SortDir::Desc),
        ["Apple", "banana", "cherry", "avocado"]
    );
    assert_eq!(
        q(SortKey::Size, SortDir::Asc),
        ["avocado", "banana", "cherry", "Apple"]
    );
}

#[test]
fn ties_go_by_name_then_id_and_are_stable() {
    let mut lib = Library::new();
    let mut ids = Vec::new();
    for n in 0..6 {
        let mut new = new_item("same", None, &format!("{n}.grapho"));
        new.modified_at = Some(5);
        ids.push(lib.add_item(new, T0).unwrap());
    }
    ids.sort();
    for dir in [SortDir::Asc, SortDir::Desc] {
        let list = lib.all(by(SortKey::Edited, dir), Window::all());
        let got: Vec<Id> = list.items.iter().map(|i| i.id).collect();
        assert_eq!(got, ids);
        let again = lib.all(by(SortKey::Edited, dir), Window::all());
        assert_eq!(list, again);
    }
}

#[test]
fn windows_page_through_a_listing() {
    let (lib, _) = sample();
    let sort = by(SortKey::Name, SortDir::Asc);
    let page = lib.all(
        sort,
        Window {
            offset: 1,
            limit: 2,
        },
    );
    assert_eq!(page.total, 4);
    assert_eq!(titles(&page), ["avocado", "banana"]);
    assert_eq!(
        lib.all(
            sort,
            Window {
                offset: 3,
                limit: 10
            }
        )
        .items
        .len(),
        1
    );
    assert!(lib
        .all(
            sort,
            Window {
                offset: 9,
                limit: 10
            }
        )
        .items
        .is_empty());
    assert!(lib
        .all(
            sort,
            Window {
                offset: 0,
                limit: 0
            }
        )
        .items
        .is_empty());
    assert_eq!(
        lib.all(
            sort,
            Window {
                offset: 9,
                limit: 10
            }
        )
        .total,
        4
    );
}

#[test]
fn in_folder_lists_one_level_of_live_items() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let sub = folder(&mut lib, Some(f), "Sub");
    let top = add(&mut lib, "Top", None);
    let a = add(&mut lib, "A", Some(f));
    let _deeper = add(&mut lib, "Deeper", Some(sub));
    let gone = add(&mut lib, "Gone", Some(f));
    lib.trash_items(&[gone], T0).unwrap();
    let ids = |l: Listing| l.items.iter().map(|i| i.id).collect::<Vec<_>>();
    assert_eq!(
        ids(lib.in_folder(None, Sort::default(), Window::all())),
        vec![top]
    );
    assert_eq!(
        ids(lib.in_folder(Some(f), Sort::default(), Window::all())),
        vec![a]
    );
}

#[test]
fn recent_orders_by_time_opened() {
    let mut lib = Library::new();
    let a = add(&mut lib, "A", None);
    let b = add(&mut lib, "B", None);
    let c = add(&mut lib, "C", None);
    let never = add(&mut lib, "Never", None);
    lib.touch_opened(a, 100).unwrap();
    lib.touch_opened(b, 300).unwrap();
    lib.touch_opened(c, 200).unwrap();
    let ids = |l: Listing| l.items.iter().map(|i| i.id).collect::<Vec<_>>();
    assert_eq!(
        ids(lib.recent(Sort::default(), Window::all())),
        vec![b, c, a]
    );
    assert_eq!(
        ids(lib.recent(
            Sort::default(),
            Window {
                offset: 0,
                limit: 2
            }
        )),
        vec![b, c]
    );
    lib.trash_items(&[b], T0).unwrap();
    assert_eq!(ids(lib.recent(Sort::default(), Window::all())), vec![c, a]);
    assert!(!ids(lib.recent(Sort::default(), Window::all())).contains(&never));
}

#[test]
fn starred_lists_live_starred_items_across_folders() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let a = add(&mut lib, "A", Some(f));
    let b = add(&mut lib, "B", None);
    let c = add(&mut lib, "C", None);
    for id in [a, b, c] {
        lib.set_starred(id, true).unwrap();
    }
    lib.trash_items(&[c], T0).unwrap();
    let list = lib.starred(by(SortKey::Name, SortDir::Asc), Window::all());
    assert_eq!(titles(&list), ["A", "B"]);
}

#[test]
fn trash_query_orders_by_time_deleted() {
    let mut lib = Library::new();
    let a = add(&mut lib, "A", None);
    let b = add(&mut lib, "B", None);
    lib.trash_items(&[a], 10).unwrap();
    lib.trash_items(&[b], 20).unwrap();
    let list = lib.trash(Sort::default(), Window::all());
    assert_eq!(titles(&list), ["B", "A"]);
}

#[test]
fn json_round_trips() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    let g = folder(&mut lib, Some(f), "G");
    let a = add(&mut lib, "A", Some(g));
    let b = add(&mut lib, "B", None);
    lib.set_starred(b, true).unwrap();
    lib.set_cover(b, Some("color:#112233".into()), T0).unwrap();
    lib.touch_opened(b, T0 + 1).unwrap();
    lib.trash_items(&[a], T0 + 2).unwrap();
    lib.trash_folder(g, T0 + 3).unwrap();
    let json = lib.to_json();
    assert!(json.starts_with("{\"version\":1,"));
    let back = Library::from_json(&json).unwrap();
    assert_eq!(back.to_json(), json);
    assert_eq!(back.item(b), lib.item(b));
    assert_eq!(back.item(a), lib.item(a));
    assert_eq!(back.folder(g), lib.folder(g));
    assert_eq!(back.file_refs(), lib.file_refs());
}

#[test]
fn json_names_are_camel_case_and_ids_are_text() {
    let mut lib = Library::new();
    let f = folder(&mut lib, None, "F");
    lib.add_item(new_item("A", Some(f), "a.grapho"), T0)
        .unwrap();
    let v: serde_json::Value = serde_json::from_str(&lib.to_json()).unwrap();
    let item = &v["items"][0];
    assert_eq!(item["fileRef"], "a.grapho");
    assert_eq!(item["kind"], "notebook");
    assert_eq!(item["folderId"], f.to_string());
    assert_eq!(item["pageCount"], 1);
    assert!(item["deletedAt"].is_null());
    assert_eq!(v["folders"][0]["sortOrder"], 1024);
}

#[test]
fn unknown_fields_are_ignored() {
    let json = r#"{"version":1,"future":{"a":1},
        "folders":[{"id":"00000000000000000000000001","name":"F","tagColour":"red"}],
        "items":[{"id":"00000000000000000000000002","kind":"pdf","title":"T",
                  "fileRef":"t.grapho","tagIds":["x"],"extra":[1,2]}]}"#;
    let lib = Library::from_json(json).unwrap();
    assert_eq!(lib.folder_count(), 1);
    let item = lib
        .item(Id::parse("00000000000000000000000002").unwrap())
        .unwrap();
    assert_eq!(item.kind, ItemKind::Pdf);
    assert_eq!(item.page_count, 0);
    assert_eq!(item.folder_id, None);
}

#[test]
fn newer_versions_and_garbage_are_refused() {
    assert_eq!(
        Library::from_json(r#"{"version":2,"items":[]}"#).unwrap_err(),
        LibraryError::UnsupportedVersion(2)
    );
    assert!(matches!(
        Library::from_json("nope"),
        Err(LibraryError::InvalidJson(_))
    ));
    assert!(matches!(
        Library::from_json("{}"),
        Err(LibraryError::InvalidJson(_))
    ));
    assert!(matches!(
        Library::from_json(r#"{"version":1,"items":[{"id":"bad"}]}"#),
        Err(LibraryError::InvalidJson(_))
    ));
    assert_eq!(
        Library::from_json(r#"{"version":1}"#).unwrap().item_count(),
        0
    );
}

#[test]
fn loading_repairs_what_it_can() {
    let a = "00000000000000000000000001";
    let b = "00000000000000000000000002";
    let json = format!(
        r#"{{"version":1,
        "folders":[
          {{"id":"{a}","name":"A","parentId":"{b}"}},
          {{"id":"{b}","name":"B","parentId":"{a}"}},
          {{"id":"00000000000000000000000003","name":"Orphan","parentId":"0000000000000000000000000Z"}}],
        "items":[
          {{"id":"00000000000000000000000010","kind":"notebook","title":"One","fileRef":"x.grapho",
            "folderId":"0000000000000000000000000Z"}},
          {{"id":"00000000000000000000000011","kind":"notebook","title":"Two","fileRef":"x.grapho"}},
          {{"id":"00000000000000000000000012","kind":"notebook","title":"Three","fileRef":"y.grapho",
            "folderId":"00000000000000000000000005"}}],
        "extra":1}}"#
    );
    let lib = Library::from_json(&json).unwrap();
    // A cycle breaks by moving its members to the top level.
    let tops = [a, b, "00000000000000000000000003"]
        .iter()
        .filter(|id| {
            lib.folder(Id::parse(id).unwrap())
                .unwrap()
                .parent_id
                .is_none()
        })
        .count();
    assert_eq!(tops, 2);
    assert_eq!(lib.folder(Id::parse(a).unwrap()).unwrap().parent_id, None);
    // The second item for x.grapho is dropped, and the first loses its missing folder.
    assert_eq!(lib.item_count(), 2);
    assert_eq!(
        lib.item(Id::parse("00000000000000000000000010").unwrap())
            .unwrap()
            .folder_id,
        None
    );
    assert!(lib
        .item(Id::parse("00000000000000000000000011").unwrap())
        .is_none());
}

#[test]
fn loading_moves_live_items_out_of_trashed_folders() {
    let f = "00000000000000000000000001";
    let json = format!(
        r#"{{"version":1,
        "folders":[{{"id":"{f}","name":"F","deletedAt":5}}],
        "items":[{{"id":"00000000000000000000000010","kind":"notebook","title":"One",
                  "fileRef":"x.grapho","folderId":"{f}"}}]}}"#
    );
    let lib = Library::from_json(&json).unwrap();
    assert_eq!(
        lib.item(Id::parse("00000000000000000000000010").unwrap())
            .unwrap()
            .folder_id,
        None
    );
}

fn run(lib: &mut Library, op: &str, args: serde_json::Value) -> serde_json::Value {
    serde_json::from_str(&call(lib, op, &args.to_string()).unwrap()).unwrap()
}

#[test]
fn json_calls_cover_the_flow() {
    use serde_json::json;
    let mut lib = Library::new();
    let f = run(
        &mut lib,
        "createFolder",
        json!({"name": "Maths", "now": T0}),
    );
    assert_eq!(f["name"], "Maths");
    let fid = f["id"].as_str().unwrap().to_string();
    let item = run(
        &mut lib,
        "addItem",
        json!({"kind":"pdf","title":"Sheet","folderId":fid,"coverRef":"color:#FF0000",
               "pageCount":4,"fileRef":"s.grapho","now":T0}),
    );
    let iid = item["id"].as_str().unwrap().to_string();
    let list = run(
        &mut lib,
        "inFolder",
        json!({"folderId": fid, "sort":"name","dir":"asc"}),
    );
    assert_eq!(list["total"], 1);
    assert_eq!(list["items"][0]["title"], "Sheet");
    assert_eq!(list["items"][0]["pageCount"], 4);
    run(&mut lib, "setStarred", json!({"id": iid, "starred": true}));
    assert_eq!(run(&mut lib, "starred", json!({}))["total"], 1);
    run(&mut lib, "trashFolder", json!({"id": fid, "now": T0}));
    assert_eq!(
        run(&mut lib, "restoreNeedsChoice", json!({"id": iid}))["needsChoice"],
        true
    );
    let r = run(
        &mut lib,
        "restoreItem",
        json!({"id": iid, "mode": "withFolder", "now": T0}),
    );
    assert_eq!(r["folderId"], fid);
    assert_eq!(
        run(&mut lib, "canMove", json!({"id": fid, "targetId": fid}))["result"],
        "into_self"
    );
    run(&mut lib, "trashItems", json!({"ids": [iid], "now": T0}));
    let purged = run(&mut lib, "purge", json!({"now": T0 + 31 * DAY}));
    assert_eq!(purged["fileRefs"][0], "s.grapho");
    assert_eq!(
        run(&mut lib, "fileRefs", json!({}))
            .as_array()
            .unwrap()
            .len(),
        0
    );
}

#[test]
fn json_calls_report_errors() {
    let mut lib = Library::new();
    let err = call(&mut lib, "nope", "{}").unwrap_err();
    assert_eq!(err.kind(), "bad_request");
    assert_eq!(
        call(&mut lib, "renameFolder", "{}").unwrap_err().kind(),
        "bad_request"
    );
    assert_eq!(
        call(&mut lib, "renameFolder", "not json")
            .unwrap_err()
            .kind(),
        "bad_request"
    );
    let missing = format!("{{\"id\":\"{}\",\"name\":\"x\",\"now\":1}}", Id::random());
    assert_eq!(
        call(&mut lib, "renameFolder", &missing).unwrap_err().kind(),
        "not_found"
    );
    let bad_sort = call(&mut lib, "all", "{\"sort\":\"weird\"}").unwrap_err();
    assert_eq!(bad_sort.kind(), "bad_request");
    assert!(!bad_sort.to_string().is_empty());
}

#[test]
fn error_kinds_are_distinct() {
    let all = [
        LibraryError::NotFound,
        LibraryError::InTrash,
        LibraryError::NotInTrash,
        LibraryError::IntoSelf,
        LibraryError::IntoDescendant,
        LibraryError::EmptyName,
        LibraryError::DuplicateFile,
        LibraryError::NeedsChoice,
        LibraryError::BadRequest(String::new()),
        LibraryError::InvalidJson(String::new()),
        LibraryError::UnsupportedVersion(2),
    ];
    let kinds: std::collections::HashSet<&str> = all.iter().map(LibraryError::kind).collect();
    assert_eq!(kinds.len(), all.len());
    assert!(all.iter().all(|e| !e.to_string().is_empty()));
}

#[test]
fn ten_thousand_items_stay_responsive() {
    let start = std::time::Instant::now();
    let mut lib = Library::new();
    let folders: Vec<Id> = (0..100)
        .map(|n| folder(&mut lib, None, &format!("f{n}")))
        .collect();
    let mut ids = Vec::new();
    for n in 0..10_000usize {
        let mut new = new_item(
            &format!("note {}", n % 977),
            Some(folders[n % 100]),
            &format!("{n}.grapho"),
        );
        new.modified_at = Some((n as i64 * 7919) % 10_007);
        new.page_count = (n % 50) as u32;
        ids.push(lib.add_item(new, T0).unwrap());
    }
    assert_eq!(lib.item_count(), 10_000);
    for &id in ids.iter().step_by(3) {
        lib.set_starred(id, true).unwrap();
        lib.touch_opened(id, T0).unwrap();
    }
    for dir in [SortDir::Asc, SortDir::Desc] {
        for key in [SortKey::Edited, SortKey::Name, SortKey::Size] {
            let all = lib.all(by(key, dir), Window::all());
            assert_eq!(all.total, 10_000);
            let page = lib.all(
                by(key, dir),
                Window {
                    offset: 5000,
                    limit: 50,
                },
            );
            assert_eq!(page.items.len(), 50);
            assert_eq!(page.items[0].id, all.items[5000].id);
        }
    }
    assert_eq!(
        lib.in_folder(Some(folders[3]), Sort::default(), Window::all())
            .total,
        100
    );
    assert_eq!(lib.starred(Sort::default(), Window::all()).total, 3334);
    assert_eq!(lib.recent(Sort::default(), Window::all()).total, 3334);

    // Deleting every folder one after another does linear work each time.
    for &f in &folders[..50] {
        lib.trash_folder(f, T0).unwrap();
    }
    assert_eq!(lib.trash(Sort::default(), Window::all()).total, 5000);
    let json = lib.to_json();
    let back = Library::from_json(&json).unwrap();
    assert_eq!(back.item_count(), 10_000);
    let removed = lib.purge(T0 + 31 * DAY);
    assert_eq!(removed.items, 5000);
    assert_eq!(lib.item_count(), 5000);
    assert!(start.elapsed().as_secs() < 20, "took {:?}", start.elapsed());
}
