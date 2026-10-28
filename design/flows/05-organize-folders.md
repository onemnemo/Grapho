# 05 Organise: item Menu, move, drag, folders, tags, multi-select

## Goal

Put work where it belongs, move it back when wrong, and act on one or many items the same way for
notebooks, PDFs, canvases and folders.

Counting rule: see 01. Start at the library place.

## Gestures (SYSTEM-MAP 4.7.1)

| On an item (tile or row) | Result |
|---|---|
| Tap or click | Opens it (in selection mode: toggles) |
| Long press, then lift | Enters selection mode with it selected |
| Long press, then move | Drags it (and selects it) |
| The ... button (always visible on touch, on hover with a mouse), right click, the Menu key | The item Menu |
| Ctrl or Cmd click, Shift click | Toggle, range |

A document that is open on another device shows "Open on another device" as the value of the one status slot in the tile's meta line. It is never a lock: sync keeps both versions.

The item Menu is never behind long press.

## The item Menu (one list)

The item Menu and the Selection bar read the same command list, filtered by `selectionKind`. The bar
shows the first four available by priority, More holds the rest. One list, in priority order:

| Row | Notebook, PDF, canvas | Folder | Multi | Shortcut |
|---|---|---|---|---|
| Open | yes | yes | no | Enter |
| Rename | yes | yes | no | F2 |
| Star | yes | yes | yes | Ctrl+D |
| Tag | yes | no | yes | none |
| Move to | yes | yes | yes | Ctrl+Shift+M |
| Edit look (Edit cover, Edit background; Edit style for folders) | yes | yes | no | none |
| Duplicate | yes | no | yes | none |
| Export | yes | no | yes | Ctrl+Shift+E |
| Share | yes | no | yes | Ctrl+Shift+S |
| Select | yes | yes | n/a | Ctrl+Alt+A |
| Delete (Move to Trash) | yes | yes | yes | Delete |
| Extension actions (at most three, then a submenu) | yes | no | no | none |

Selection bar with several items: Move to, Star, Tag, Delete visible; More: Duplicate, Export, Share,
Select all. Below 600 dp wide the bar shows icons with Tooltips and the count; text labels from
600 dp. Counts use plural label keys, so each language supplies its own forms.

## Library Top bar: action priority

The Top bar holds four actions then overflow, in this priority.

| Place | Visible | Overflow |
|---|---|---|
| Home, a folder | Search field, New, Select, View | Sort, Activity, Settings |
| Starred | Search field, Select, View | Sort, Activity, Settings |
| Trash | Search field, Empty Trash, Filter | Activity, Settings |
| Search results | Search field, Filter | Settings |

On compact the search field is an icon that expands over the bar. Sync status is an icon at the foot
of the Sidebar (drawer on small windows), not a Top bar action (14). Settings is also at the Sidebar
foot. Home shows Recent (last opened, 8 items, "Show all"), Folders and All, each under a `GroupHeading`; folders are `FolderTile`s, drop targets that highlight on enter.

## Steps: move one notebook

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Tap the ... button on the tile | Menu | `ItemTile`, `IconButton`, `Menu` |
| 2 | Move to | Menu to Sheet | `MoveToSheet` (the Menu closes first): `FolderChooser` with `SearchField`, "Library" row first, indented tree, New folder |
| 3 | Tap a folder: it applies at once and the Sheet closes | Sheet | `FolderChooser` rows are `ListRow`s |
| 4 | Result | Toast | `Toast` "Moved to Semester 1" with Undo |

Selection route: long press the tile and lift (selected, selection mode), then Move to on the
`SelectionBar`, then the folder. Drag route: long press, move onto a folder tile or a Sidebar
folder; the target highlights on enter; one journal entry. Keyboard: arrows, Space, Ctrl+Shift+M.
On compact, Move to is the route (the drawer scrim covers the grid, so drag to the drawer is not
offered).

## Steps: folders, tags, multi-select

- **Nested folders**: New, Folder inside a folder; the Sidebar `FolderTree` indents four levels and
  deeper folders show a path in the `TopBar`. Moving a folder into itself or a descendant is
  disabled with "Cannot move a folder into itself". Delete moves the folder and its contents to
  Trash as one entry, Toast with Undo.
- **Rename**: Menu or F2, in place (`TextField` inline).
- **Tag**: Menu or bar, Tag opens a `Popover`: `SearchField` (Enter creates a tag), a `Checkbox`
  list (mixed when only some items have it). Each check applies at once; there is no Done. Tap
  outside to leave.
- **Starred filter**: a toggle `Chip` in the Top bar region of Search and Home, not a Popover.
- **Multi-select**: enter by long press and lift, the Select command, or Ctrl or Cmd click. Select
  all and ranges work on ids in the model. The bar says "All 412 selected" with Clear.
- **Keyboard**: Space toggles, arrows move, Shift+arrows extend a range, Ctrl+A all, F2 rename; in
  the tree, Right expands, Left collapses.

## Tap counts

| Task | Taps (expanded; compact the same) |
| --- | --- |
| Move a notebook into a folder | 3 (..., Move to, folder); via selection 3 (long press, Move to, folder); drag 1 gesture |
| Move out to the top level | 3 ("Library" row first) |
| Undo a wrong move | 1 (Toast); then Ctrl+Z, Undo in Top bar overflow, or Activity |
| Rename a folder | 2 (..., Rename) plus typing |
| Name a new folder | 4 (New, Folder, Name field, Create) |
| Tag 5 notebooks with an existing tag | 6 (long press, four taps, Tag, check the tag) |

## States

| State | What the user sees |
|---|---|
| Empty library | First-run empty state (01) |
| Empty folder | `EmptyState` sm "Nothing in this folder yet." with "New notebook" |
| 1 item, 500 items | Virtualised grid; `AlphabetRail` when sorted by Name; counts in controls |
| 40 folders | Sidebar scrolls with Filter above 30; chooser always has Filter |
| Move into itself | Row disabled with the reason under it |
| Target gone | "Folder not found" in the chooser; the list refreshes |
| Drag over an invalid target | No highlight; the `DragGhost` shows "Cannot move here" |
| Filtered view | "3 of 12 shown, 3 selected" |
| Error | Toast "Could not move 3 items." Retry |
| Offline | Not applicable: folders are local |
| Hover (Windows, macOS) | The selection checkbox appears; touch equivalent is long press and lift |

## Exits

| Exit | Result |
|---|---|
| Choose a target | Sheet closes, selection mode ends, Toast with Undo |
| Cancel, Escape, scrim tap | Sheet closes; selection kept |
| Back and Escape | Popover or menu, Dialog, pushed view, Sheet, bottom panel, selection mode, place |
| Failure | Toast with Retry |

## Wireframes

Library, Expanded, with selection (Toast sits above the bar, bottom left):

```
+-----------+------------------------------------------------+
| Home      | Semester 1 > Biology   [Search][New][Select][..] |
| Starred   +------------------------------------------------+
| Semester 1|  [x] Week 4   ...   [ ] Week 5 ...  [ ] Lab PDF |
|  Biology  |  [x] Week 6   ...   [ ] Folder: Exams           |
|  Chemistry|                                                  |
| Trash     |  +--------------------------------------------+ |
| [sync][gear]| 2 selected [Move to][Star][Tag][Delete][..]  | |
+-----------+------------------------------------------------+
```

Move to Sheet:

```
+------------------------------------------------+
| Move 2 items                              [x]  |
| [ Filter folders                         ]     |
|  Library (top level)                           |
|  v Semester 1                                  |
|      Biology            (current)              |
|      Chemistry                                 |
|  Exams / 2023 / Spring   (path for deep ones)  |
| New folder                          [ Cancel ] |
+------------------------------------------------+
```

Compact: the Sidebar is a drawer; the bar floats above the system bar, icons only; German check:
"Verschieben" is the Tooltip, the icon is unchanged. Medium: as compact with 3 to 5 columns.

## Access

Screen reader: custom actions on each item ("Select", "Move to", "Menu"); entering selection mode
announces "Selection mode, 1 selected"; each toggle "3 of 12 selected". Focus returns to the item's
... button after a Menu closes, and to the moved item's place after a Sheet closes. Toast Undo is
reachable by focus and holds at least 10 s when a screen reader is on.
