# Screen: Received document (missing packs, missing extension, newer version, corrupt)

## 1. Purpose

A document that came from somewhere else (shared, synced, or imported) opens with a designed outcome
for every difference: packs this device lacks, an extension it lacks, a newer Grapho, or damage. The
rule behind all of it: a document never depends on a pack or an extension, and a file is never deleted by
the app because it could not be read. The screen is the document of note-notebook.md, note-pdf.md or
note-canvas.md with a Banner or an error panel; this file states those parts and the import review that
precedes them.

## 2. Flows served

13 open a document, and import a notebook file or an image (main), 09 (missing extension), 08 (missing
pack asset), 14 (share, sync conflict), 10 (never delete on a read failure).

## 3. Layout and regions

Regions as the document, plus the Banner band under the NoteBar position (outside the receding group),
an error panel in place of the page area when nothing is readable, and the BlockPlaceholder inside the
page. Banners appear once the pen has lifted and never move the page.

### Partly damaged (Expanded, R)

```
+------------------------------------------------------------+
| < Week 4 Biology   1 of 37                          [More] |
| (!) 3 pages could not be read. This notebook is open read-only. |
|     [ Open read-only ]   [ Export copy ]                 [x] |
+----+-------------------------------------------------------+
|[~]x|                                                       |
|    |             page 1 (readable)                          |
+----+-------------------------------------------------------+
```

### Fully unreadable (Compact; two stacked buttons)

```
+----------------------------------+
| < Week 4 Biology                 |
+----------------------------------+
|              (!)                 |
|  This file cannot be read.       |
|  The file has not been changed.  |
|  [ Save a copy to Files ]        |
|  [ Try again ]                   |
+----------------------------------+
```

Back is the NoteBar Back and never removes the item. L: the dock, panel and Toasts mirror to the right
as in the document; the error panel does not mirror.

### Missing extension block (inside the page)

```
+--------------------------------+
| Made with "Flashcards" (a1b2)  |
| [saved picture of the block]   |
| Picture saved in the note      |
+--------------------------------+
  tap: Get extension | Keep as image | Turn on
```

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Banner band | Banner `placement` inline, `tone` warn or danger, at most two buttons plus a dismiss | One at a time, highest priority first (damaged, newer, damaged asset, missing pack, syncing) |
| Error panel | Banner `placement` region `tone` danger, in the page area | Two buttons at most: "Save a copy to Files" and "Try again" (or "Update"). Back is the NoteBar Back, so it is not a third button |
| Block | BlockPlaceholder `cause` (missing, off, in Trash, blocked, no runtime, needs a newer version) | Frame headed "Made with 'Flashcards'" (stored name in quotes plus a short id), the stored fallback picture and text, caption "Picture saved in the note" |
| Block actions | Menu from the placeholder (also in SelectionBar More): Get extension, Keep as image (one undo step), Turn on (if installed but off), Update (newer data) | On a device with no runtime only Keep as image; status "Not available on this device" |
| Progress | ProgressItem "Opening Week 4" after 1000 ms with Cancel; Skeleton page shapes after 300 ms | |
| Import review | Sheet, ImportReviewSheet `notebook` or `image`, TextField (Name), ListRow (Location), RadioList ("Create as"), Banner status, Button | Section 6 |
| Doc chrome | NoteBar, ToolDock (Pen unavailable with the Tooltip "Read-only"), SidePanel | As the document |

## 5. Outcomes

| Situation | What the user sees | Actions |
|---|---|---|
| Normal | The document | |
| Missing pack assets | Renders from the embedded copies. After the pen lifts, one Banner neutral "Uses 6 stickers from Botanica by Sample Author." | Keep in my library (items become Yours, labelled "Kept from Botanica"), Get the pack (catalogue, or the unavailable state), dismiss (stored per note and pack on this device). Items marked as not extractable do not offer Keep |
| Damaged embedded asset | That asset draws the type's placeholder; Banner "1 sticker is damaged and cannot be shown." | Get the pack, if its id is known |
| Missing extension | The block shows its stored fallback in a BlockPlaceholder; the note opens normally; no Dialog | Get extension, Keep as image, Turn on |
| Needs a newer extension | The fallback with "Needs a newer version of Flashcards" | Update |
| Saved by a newer Grapho | Opens read-only if the readable parts allow. Banner warn "Saved by a newer version of Grapho. Update to edit." | Update, Export copy, dismiss (returns on the next open) |
| Newer and not readable | Error panel "Needs Grapho 1.6 or newer. You have 1.4." | Update, Save a copy to Files |
| Older Grapho file | Opens and edits normally; saved in the current format | |
| Partly damaged | Opens read-only with what can be read. Banner danger "3 pages could not be read. This notebook is open read-only." Tapping "3 pages" opens the DamageDetailsSheet recipe (each damaged page, what can be recovered, Export copy) | Open read-only, Export copy, dismiss |
| Fully unreadable | Error panel "This file cannot be read." The file stays untouched, never deleted or overwritten | Save a copy to Files, Try again, Back |
| Sync copy not here yet | The tile reads "Downloading"; opening shows Banner "This notebook is still syncing." | Retry, Cancel |
| Sync copy never arrives | Banner on the item "Could not get this notebook. Check your connection." | Retry |
| Conflict | Both versions kept; Banner with Compare | Compare |
| Storage full when saving | Banner "Changes are not saved. There is not enough space." | Free space |

Read-only is a document mode: the dock Pen is unavailable with a Tooltip, the page can still be read,
searched, zoomed and exported; Rename and Move still work on the library item.

## 6. Import review (notebook file, image)

New, Import, Notebook file (or drop a `.grapho` file, or the share target) opens the Sheet: cover thumbnail,
title, pages, size, Grapho version, last saved, Name, Location, and a status line.

| Situation | Status | Buttons |
|---|---|---|
| New id | New | Import, Cancel |
| Same id, identical content | Already in your library | Open it, Cancel |
| Same id, different content | "Another version of Week 4 (saved yesterday)" | Keep both (primary; imports as "Week 4 (2)" with a new id), "Replace the existing one" (text; the old one goes to Trash), Cancel |
| Different id, same name | New, "Same name as one you have" | Import, Cancel |
| Saved by a newer Grapho | "Saved by a newer version. It will open read-only." | Import, Cancel |
| Damaged | One sentence and Close, or "Import what can be read" | Import what can be read |

Image variant: thumbnail, Name, Location and RadioList "Create as" New canvas (default) or Page of a new
notebook. Over 50 megapixels the image is scaled down, with a line saying so. Refusals: "This image format
is not supported." and "This image is too large (120 MP)." A Toast "Imported Week 4" with Open ends the
job.

## 7. States

| State | What changes |
|---|---|
| Loading | Frame at once; Skeleton after 300 ms; first page writable first |
| Huge notebook (2,000 pages) | First page now; Pages tab Skeleton tiles; ProgressItem for thumbnails |
| Huge canvas | Reduced detail first, then sharpens; far regions are placeholders |
| Offline | A document on this device opens; a not-yet-synced one shows the syncing Banner |
| Permission denied (files) | The Import row explains and links to system settings |
| Long German text | Banner actions move under the message below the medium breakpoint; "Beide behalten" and "Abbrechen" stay on one line at 360 wide; placeholder caption wraps |
| 200% text | Banners wrap, buttons stack primary first; the error panel text wraps and stays centred |
| Many Banners | Highest priority only; the rest are listed under Notices in the activity list |

## 8. Behaviour on resize and split screen

Banners and the error panel re-flow with the window; actions move under the message below Medium. The
placeholder keeps its frame and moves its actions to a Menu below `size/placeholder/actions-min`. The
read-only mode, dismissed Banners and the open page are kept across classes. The import Sheet is a bottom
Sheet on Compact and keeps its status line.

## 9. Keyboard and focus order

Opening announces "Week 4 Biology, page 12 of 40"; read-only is announced and exposed as state; Banners
are announced when they appear. Order: NoteBar, Banner buttons, page, dock, panel. When a region Banner
replaces the page area, focus moves to its first button. Enter opens a placeholder Menu (the Menu key or
right click as well). Escape closes the topmost layer, then Back leaves. Focus returns to the tile on
Back, to the Import entry after the Sheet closes.
