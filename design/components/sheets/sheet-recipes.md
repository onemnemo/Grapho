# Sheet recipes

Composed bodies of Sheet. Each follows sheet.md for shell, dismiss, keyboard and compact layout.
Each has one primary, Cancel, and at most one secondary text button.

## Move to (MoveToSheet)

Body: SearchField Filter, FolderChooser ("Library" first, indented tree, disabled rows with the
reason "Already here", "Inside itself"). Foot: none; choosing a folder applies at once, closes, and
shows a Toast with Undo. Cancel in the header. Selection stays on Cancel. A target removed while the
Sheet is open turns its row disabled with a reason. 500 folders: virtualised, Filter focused.

## Remove pack (RemoveSheet)

Shown only when something uses the pack. Body: "3 items you use stay as your own copies", then a
list of the items by type (**Kept from <pack>**), virtualised. Foot: Cancel, primary **Remove pack**
(`danger`-free: removal is reversible and ends in a Toast with Undo). Dirty: never; Cancel is
instant. A pack in use by nothing skips the Sheet.

## Export (ExportSheet)

Body: format SegmentedControl (PDF flattened, Images, Grapho file; falls back to a Select), Pages
(All, Range, same field as ImportReviewSheet), Name, Location. Foot: Cancel, primary **Export**.
Running: the Sheet closes and a Progress item runs. Dirty: any change from the defaults.

## Pack builder (PackBuilderSheet)

Body: checkable rows by type with Select all and a count; Name and Author fields; one disclosure
for Licence. Foot: Cancel, primary **Share 14** (the count of checked rows), secondary text button
**Save to files**. Nothing checked disables the primary with "Choose at least one". Checkable rows
are ListRows with a Checkbox; 500 rows are virtualised and the count stays in the primary.

## Catalogue (CatalogueView)

A pushed view, up to three deep: list, then pack review (ImportReviewSheet kind `pack` body), then
nothing deeper. The view has Back and SearchField; its foot is the review's foot when a pack is
open. Install pops back to the host Sheet with the pack now in the Picker's source filter. From a
Sheet host the form keeps its draft throughout. Offline: Banner `warn` and the installed packs
stay available.

## Damage details (DamageDetailsSheet)

Opened from the partly damaged Banner on a document (flow 13). Body: a Banner line "3 of 37 pages could
not be read. The file has not been changed.", then a ListRow `plain` per damaged page: "Page 12", with
what can be recovered as the subtitle ("Ink recovered, background lost", "Nothing recovered"), and a
GroupHeading for the readable pages count. Foot: Cancel (labelled Close, as nothing is staged) and the
primary **Export copy**, which writes what can be read to a new document and never touches the
original. Running: the Sheet closes and a ProgressItem runs. Dirty: never. A list over 200 pages
virtualises.
