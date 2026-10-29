# Screen: Library, Folder

## 1. Purpose

The contents of one folder, nested to any depth. It is Home's layout with a path in the title, a
folder-scoped New, and rules for deep nesting, moving and deleting folders. Starred uses the same
layout with the TopBar `place` starred; the differences are in section 4.

## 2. Flows served

05 organise (the main flow), 02 create (New preselects this folder), 04 import (into this folder),
06 find (search inside a folder presets the Folder Chip), 10 recover (folder delete as one entry).

## 3. Layout and regions

Regions as Home: TopBar, Sidebar or drawer, Content, SelectionBar, Toast stack. Content has two
groups: Folders (FolderTile) then Items (ItemTile). There is no Recent group.

### Expanded, R

```
+-----------+--------------------------------------------------------+
| Sidebar   | Semester 1 / Biology  [Search.....][ New v ][Select][View][...] |
| Home      +--------------------------------------------------------+
| Starred   | Folders                                                 |
| Semester 1|  [Exams][Labs]                                          |
|  Biology* | Items (14)                                    Name v    |
|  Chemistry|  [tile][tile][tile][tile][tile]                     [A] |
| Trash     |  +-------------------------------------------------+   |
| [sync][gear] | 2 selected [Move to][Star][Tag][Delete][...]  |   |
+-----------+--------------------------------------------------------+
   [Toast, bottom left, above the SelectionBar]
```

L: the Toast stack moves to the bottom right. Nothing else mirrors.

### Medium and Compact

As Home: the Sidebar is a drawer, columns 3 to 5 (Medium) and 2 to 3 (Compact). On Compact the TopBar
holds Menu, the folder name, Search icon, New, More. The path is not drawn in the bar below four levels
(see below); the drawer shows the tree with the current folder marked.

### Short

As Home. A deep path collapses to the last folder name; the full path is in the Tooltip and the
Details row of the folder Menu.

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | TopBar `place` folder with the PathBar part | Same actions as Home. PathBar shows "Semester 1 / Biology"; each segment is a link to that folder. Below four levels the PathBar replaces the title and truncates the middle segments first, keeping the first and last |
| Start | Sidebar `kind` library with FolderTree `kind` navigate | Indents four levels; deeper folders show only in the PathBar. Filter above 30 folders |
| Folders | GroupHeading, FolderTile | FolderTile is a drop target; it previews contents |
| Items | GroupHeading with count, ItemTile | Same tile as Home |
| Selection | SelectionBar `placement` library | A selection of folders shows the folder command list: Open, Rename, Star, Move to, Edit look, Select, Delete |
| Starred variant | TopBar `place` starred | Visible: Search, Select, View. No New. Items from every folder, grouped by folder path in the GroupHeading |
| Overlays | NewItemSheet kind folder (Name, Style row, Location), MoveToSheet with FolderChooser, Popover for Tag, Dialog | The folder Menu has Details and a More button on folders |

The folder Menu is the item Menu filtered by `selectionKind` folder. "Sync this folder" is a link
row that opens Settings, Sync. Rename is in place with the inline TextField (F2).

## 5. Rules specific to folders

- **New inside a folder** preselects this folder as Location; the Sheet shows it and it can be
  changed. Create is valid with the defaults.
- **Move a folder**: Move to opens MoveToSheet. The folder itself and every descendant are disabled
  rows with the reason "Cannot move a folder into itself" under them. The "Library" row first moves it
  to the top level.
- **Delete a folder** moves the folder and its contents to Trash as one journal entry; Toast "Moved
  Biology to Trash" with Undo. No Dialog.
- **Drag** a tile onto a FolderTile, a Sidebar folder or a PathBar segment: only FolderTile and
  Sidebar folders are targets (the PathBar is not a target, per the TopBar spec). Invalid targets do
  not highlight.
- **Folder style** changes through Edit look, Edit style: NewItemSheet in edit mode with the Picker.

## 6. States

| State | What changes |
|---|---|
| Empty folder | EmptyState `first-use` `sm`, no mascot: "Nothing in this folder yet." Primary "New notebook" (the TopBar New becomes `secondary`). Import is a text Button |
| Loading | Skeleton tiles after 300 ms, held 300 ms; the PathBar is live at once |
| Error | Banner region danger "Could not open this folder. Try again." Retry. A deleted folder opened from a link: the screen falls back to Home with a Toast "Folder not found" |
| Offline | Not applicable. Folders sync later; the SyncIndicator carries sync state |
| Very large | 500 items: virtualised, AlphabetRail in Name sort above 30, counts in the headings. 40 folders in the tree: Sidebar scrolls with Filter |
| Long German names | PathBar truncates middle segments; a single long name wraps to two lines on tiles; full name in the Tooltip |
| 200% text | Path uses the last folder name only; actions give way to More; tiles as Home |
| Filtered view | The SelectionBar and heading read "3 of 12 shown, 3 selected" |
| Target gone while moving | MoveToSheet shows "Folder not found" and refreshes its list |
| Move failed | Toast "Could not move 3 items." Retry |

## 7. Behaviour on resize and split screen

Same as Home. The PathBar re-measures on every width change and falls back to the last folder name
before any action leaves the bar. Selection and the open folder are kept across class changes. A Sheet
(New, Move to) keeps its draft.

## 8. Keyboard and focus order

As Home, plus: in the Sidebar tree Right expands, Left collapses or moves to the parent, F2 renames,
Delete moves to Trash. Focus order: TopBar (PathBar segments are one tab stop with arrows), Sidebar,
grid, SelectionBar. Arrival focus is the TopBar title or PathBar. Alt+Up goes to the parent folder.
After Move to, focus goes to the place the item was; after Rename, back to the tile.
