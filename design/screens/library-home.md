# Screen: Library, Home

## 1. Purpose

The place the app opens to. It answers "where is my work?" with the last opened items first, then
folders, then everything, and is the start of every journey: create, import, find, organise, recover.
It is one of four Library places (Home, Starred, a folder, Trash) that share this layout; folder.md,
trash.md and search-results.md only state what differs.

Conventions in all screen files: window classes are Compact (under 600 wide), Medium (600 to 839),
Expanded (840 and up), and Short (under 500 tall, any width). R is right-handed, L is left-handed.
Components are named exactly as their specs name them. Tokens are roles, never numbers.

## 2. Flows served

01 first run (empty state), 02 create, 04 import PDF, 05 organise, 06 find, 07 cover edit, 10 recover,
13 open and import, 14 share. Home also hosts the entry to Settings (Sidebar foot).

## 3. Layout and regions

Regions: TopBar (top), Sidebar (start edge or drawer), Content (scrolls), SelectionBar (bottom,
floating, only in selection mode), Toast stack (bottom, off-hand side), overlays.

### Expanded (840 and up), R

```
+-----------+--------------------------------------------------------+
| Sidebar   | Home        [Search.......][ New v ][Select][View][...]  | TopBar
| Home      +--------------------------------------------------------+
| Starred   | Recent (8)                                    Show all |
| Folders   |  [tile][tile][tile][tile][tile][tile][tile][tile]       |
|  Biology  | Folders                                                  |
|  Maths    |  [FolderTile][FolderTile][FolderTile]                    |
| Trash     | All (412)                                      Name v    |
|-----------|  [tile][tile][tile][tile][tile][tile] ...            [A] | AlphabetRail
| [sync]    |                                                          |
| [gear]    |  [Toast, bottom left]                                    |
+-----------+--------------------------------------------------------+
```

L: identical, except the Toast stack sits bottom right. The Sidebar, TopBar and SelectionBar never
mirror for handedness (components/bars README). Right to left languages mirror the whole screen,
Sidebar to the right.

### Medium (600 to 839)

```
+--------------------------------------------------------+
| [=] Home        [Search.....][ New v ][Select][View][...] |
+--------------------------------------------------------+
| Recent                                          Show all |
|  [tile][tile][tile][tile]  (3 to 5 columns)              |
| Folders ...   All ...                                    |
+--------------------------------------------------------+
```

The Sidebar is a drawer opened by the Menu IconButton (`library.drawer`), width the lesser of
`size/sidebar/width` and the window less `size/hit/touch`, over `scrim/default`.

### Compact (under 600)

```
+--------------------------------+
| [=] Home     [Q] [ New v] [...] |   Search is an IconButton; it expands over the bar
+--------------------------------+
| Recent  Show all               |
|  [tile][tile]  (2 to 3 columns)|
| Folders  [F][F]                |
| All                            |
|  [tile][tile]                  |
| [Toast, bottom left (R)]       |
+--------------------------------+
```

### Short (under 500 tall)

Same as the width class. Sidebar is always a drawer. The SelectionBar shows icons only with
Tooltips. A Sheet is full height with the foot keeping only the primary (sheets README). GroupHeading
rows scroll with the grid and are not sticky, so the first tile row stays visible.

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | TopBar `place` home | Visible: Search, New (the one primary, `primary` Button, default size), Select, View. More: Sort, Activity, Command palette, Settings. New never overflows; moved items go first in More |
| Start | Sidebar `kind` library, `mode` persistent (Expanded) or drawer; FolderTree `kind` navigate; SyncIndicator and a Settings row in the foot | Filter appears in the tree above 30 folders |
| Banner band | Banner `placement` inline: trash purge ("2 items leave the Trash in 3 days." View), sync conflict (Compare), safe mode, storage; one at a time, highest priority first | Under the TopBar, in the flow |
| Filter row | Chip (toggle) "Starred" in a filter row directly under the TopBar, drawn on Home and Search results | The row holds filter Chips only; it is not a TopBar slot. Scrolls with the content on Short |
| Recent | GroupHeading `heading` with Show all; up to 8 ItemTile | Last opened, not last modified |
| Folders | GroupHeading, FolderTile (drop target) | Preview contents; highlight on drag enter |
| All | GroupHeading with count; ItemTile `kind` notebook, pdf, canvas | Virtualised above 200; GroupHeading `rail` (AlphabetRail) when sorted by Name above 30 |
| Selection | SelectionBar `placement` library | Multi: Move to, Star, Tag, Delete; More: Duplicate, Export, Share, Select all |
| Menus | Menu: New (Notebook, Canvas, Folder, then Import: Notebook file, PDF, Image, Pack), item Menu, Sort (radio rows), View | The item Menu is the one list in flow 05 |
| Overlays | Sheet recipes NewItemSheet kinds notebook, canvas, folder; MoveToSheet with FolderChooser; ImportReviewSheet; Popover (Tag, Activity); Dialog; DropZone; DragGhost | Never two modal layers |
| Feedback | Toast (max two), ProgressItem, Skeleton, EmptyState, Tooltip | Toast stack on the off-hand side |

ItemTile shows: art (cover or first page), up to two lines of title, a meta line with page count or
date and one status slot ("Open on another device", "Downloading"), the ... IconButton (visible on
touch, on hover or focus with a mouse), and a Starred glyph. Selection is the tinted fill plus a
check, never shrink and dim.

## 5. One primary per surface

Home shows one `primary` Button: New in the TopBar. When the library is empty the EmptyState
primary "New notebook" is the one, and TopBar New becomes `secondary` (actions README).

## 6. States

| State | What changes |
|---|---|
| First run | First-run Sheet over the empty library: first-run.md |
| Empty library | EmptyState `first-use` `md` with the mascot: "Your first notebook starts here." Primary "New notebook" (large), text Buttons "Import" and "Sign in to restore" (shown where sync is available). Recent, Folders, All headings are not drawn |
| Empty Recent only | The Recent group is not drawn; no heading with nothing under it |
| Loading | TopBar title and actions are live at once. Skeleton tiles in the real grid after 300 ms, held at least 300 ms |
| Error, listing failed | Banner `placement` region, `tone` danger: "Grapho could not open its storage. Try again." Retry. It never looks like an empty library. Focus moves to Retry |
| Partial | The items that loaded show; Banner warn "3 items could not be read." View |
| Offline | Not applicable to the library; folders are local. Sync offline is the SyncIndicator offline state in the Sidebar foot (queued) |
| Sync conflict | Banner "Both versions of Week 4 were kept." Compare; tile status slot reads "Conflict" |
| Storage full | Banner "There is not enough space. Changes are not saved." with Free space |
| Very large | 500 items: virtualised, counts in controls ("All (412)"), AlphabetRail in Name sort. 10,000: same; Select all works on ids in the model |
| 1 item | One ItemTile; columns follow the art size, the tile never stretches |
| Long German titles | Two lines, then end ellipsis; full title in the Tooltip and accessible name. TopBar title is measured; actions give way to More before the title is cut |
| 200% text | Tiles keep art size and grow their text block; columns drop (Compact goes to 2 then 1); TopBar Select and View become icon only; Sidebar rows grow; GroupHeading wraps |
| Selection mode | TopBar New becomes `secondary`; ItemTile shows empty check circles on all items, ... hidden; SelectionBar appears with the count ("2 of 412 selected" or "All 412 selected" with Clear) |
| Drag | Section 8 |
| Palm | The library has no writing; no palm rule |

## 7. Behaviour on resize and split screen

Class follows window width live and keeps scroll position, selection, an open Menu and the focused
tile. Crossing 840 turns the persistent Sidebar into a drawer (closed) and shows the Menu IconButton
in the TopBar, or the reverse. Columns change before art shrinks. An open Sheet re-lays out (bottom
Sheet on Compact, centred otherwise) and keeps its draft. A half-width split screen on a tablet is
Compact; at 360 wide the grid shows 2 columns, the TopBar holds Menu, Title, Search icon, New and
More, and Select and View sit in More. The minimum window, 360 by 480, is supported with the same
rules.

## 8. Gestures, drag and selection

- Tap opens. Long press then lift enters selection mode with the item selected. Long press then move
  drags it and selects it. The ... button, right click and the Menu key open the item Menu; the Menu is
  never behind long press only (SYSTEM-MAP 4.7.1).
- Mouse: Ctrl or Cmd click toggles, Shift click selects a range. Hover shows the selection check.
- Drag onto a FolderTile or a Sidebar folder: the target highlights on enter (`state/drag/target-fill`
  and line); an invalid target does not highlight and the DragGhost says "Cannot move here". One
  journal entry; Toast "Moved 3 to Biology" with Undo. On Compact the drawer scrim covers the grid, so
  Move to is the route.
- Files dropped from outside: DropZone "Drop to import" covers the content; several PDFs open one
  ImportReviewSheet.
- Every drop has the menu equivalent: Move to opens MoveToSheet with FolderChooser ("Library" row
  first, Filter, New folder). A folder into itself or a descendant is disabled with "Cannot move a
  folder into itself".

## 9. Keyboard and focus order

Focus order: TopBar (title, then actions; Search is its own tab stop; arrows rove the buttons),
Sidebar (or drawer), Content grid, SelectionBar when present. F6 and Shift+F6 cycle TopBar, Sidebar,
Content. On arrival focus goes to the TopBar title (the place heading). Back from a document restores
scroll, selection and the focused tile.

| Keys | Action |
|---|---|
| Arrows, Home, End, Page Up and Down | Move between tiles (2D) |
| Enter, Space, Shift+Arrows, Ctrl+A | Open; toggle and enter selection mode; extend a range; select all |
| F2, Delete, Ctrl+D, Ctrl+Shift+M, Ctrl+Shift+E, Ctrl+Shift+S | Rename, Move to Trash, Star, Move to, Export, Share |
| Ctrl+N, Ctrl+Shift+N, Ctrl+O | New notebook, New folder, Import |
| Ctrl+F, Ctrl+K, Ctrl+B | Global search, palette, collapse the Sidebar |
| Ctrl+Z | Undoes the library journal (the last 100 actions per device) |
| Escape | Closes the topmost thing in the Back order: Popover or Menu, Dialog, pushed view, Sheet, selection mode. At the root of Home it does nothing; system Back on Android leaves the app |

A closed Menu returns focus to its ... button; a closed Sheet returns focus to the moved item's place
or to New. Toast Undo is reachable by F6, not in the Tab order, and holds at least 10 s with a screen
reader.
