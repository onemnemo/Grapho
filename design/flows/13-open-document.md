# 13 Open a document, and import a notebook file or an image

## Goal

Opening is the most-used transition. It starts fast, shows the first page before the rest, and has a
designed outcome for a huge file, a damaged file, a file from a newer Grapho, a file whose pack
assets are missing, and a file that has not arrived yet. A file is never deleted by the app because
it could not be read .

Counting rule: see 01. Start at the library.

## Entry points

- Tap a tile or row (Enter, Open); Back from another place restores scroll and selection.
- A search result (06), a deep link `grapho://doc/<id>?page=<n>`, "Open with", a recent in Home.
- New, Import, Notebook file or Image (see the import steps below).

## Steps: open

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Tap the tile | Library to Document | `ItemTile` |
| 2 | The document frame appears at once: Note bar, dock, page area. If the page is not ready after 300 ms, `Skeleton` page shapes in the real layout, shown at least 300 ms | Document | `NoteBar`, `ToolDock`, `Skeleton` |
| 3 | The first page renders first (the last viewed page, or page 1), writable at once. Other pages render on demand: current, two ahead, one behind, placeholders of the right height so the scroll length is stable | Document | page area |
| 4 | If still working after 1000 ms (a 2,000 page notebook, a large canvas) | Progress item | `ProgressItem` "Opening Week 4" with Cancel; Cancel returns to the library |
| 5 | Banners for lasting states appear at the top of the document, outside the receding group | Document | `Banner` |

The pen can write as soon as the first page shows. Strokes written while later pages load are kept
in order. Zoom and position are restored (11).

## Outcomes

| Situation | What the user sees | Actions |
|---|---|---|
| Normal | The document | |
| Huge notebook (2,000 pages) | First page now; Pages tab `Skeleton` tiles; `ProgressItem` for thumbnails | Cancel preparing; the document stays |
| Huge canvas (memory) | The viewport renders at reduced detail first, then sharpens; far regions are placeholders until viewed | none needed |
| Partly damaged | Opens read-only with whatever can be read. `Banner` danger: "3 pages could not be read. This notebook is open read-only." | Open read-only, Export copy (details in the Damage details Sheet, opened from the message) |
| Fully unreadable | In place of the page area, an error panel: "This file cannot be read." | Save a copy to Files, Try again, Back. The file stays untouched; it is never deleted or overwritten |
| Saved by a newer Grapho | Opens read-only if the readable parts allow. `Banner` warn: "Saved by a newer version of Grapho. Update to edit." | Update, Export copy, dismiss (returns on next open) |
| Newer and not readable | The error panel: "Needs Grapho 1.6 or newer. You have 1.4." | Update, Save a copy to Files, Back |
| Older Grapho file | Opens and edits normally; saved in the current format | |
| Missing pack assets | Renders from embedded copies. One-time `Banner`: "Uses 6 stickers from Botanica by Sample Author." | Keep in my library, Get the pack, dismiss (content-system/flows.md, "Missing pack asset in a received document") |
| Damaged embedded asset | That asset draws the type's placeholder; `Banner` "1 sticker is damaged and cannot be shown." | Get the pack, if the id is known |
| Missing extension | The block shows its fallback in a frame "Made with Flashcards" (09) | Get extension, Keep as image |
| Sync copy not here yet | The tile shows "Downloading"; opening shows a `Banner` "This notebook is still syncing." | Retry, Cancel |
| Sync copy never arrives | `Banner` on the item "Could not get this notebook. Check your connection." | Retry |
| Conflict | Both versions kept; `Banner` with Compare (14) | Compare |
| Storage full when saving | `Banner` "Changes are not saved. There is not enough space." | Free space |

Read-only is a document mode: the dock shows the Pen as unavailable with a tooltip "Read-only", the
page can still be read, searched, zoomed and exported. Rename and Move work on the library item.

## Steps: Import, Notebook file

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | New, Import, Notebook file; or drop a `.grapho` file; or the share target | Menu | `Menu`, `DropZone` |
| 2 | Choose a file | system picker | files permission in context |
| 3 | Review | Sheet | `ImportReviewSheet`, notebook variant: cover thumbnail, title, pages, size, Grapho version, last saved; `TextField` Name; `ListRow` Location; status line |
| 4 | Import | Progress item if long, Toast "Imported Week 4" with Open | `ProgressItem`, `Toast` |

Duplicate rules (by notebook id first, then name):

| Situation | Status | Buttons |
|---|---|---|
| New id | New | Import, Cancel |
| Same id, identical content | Already in your library | Open it, Cancel |
| Same id, different content | "Another version of Week 4 (saved yesterday)" | **Keep both** (primary; imports as "Week 4 (2)" with a new id), "Replace the existing one" (text; the old one goes to Trash), Cancel |
| Different id, same name | New, "Same name as one you have" | Import, Cancel |
| Saved by a newer Grapho | "Saved by a newer version. It will open read-only." | Import, Cancel |
| Damaged | one sentence and Close, or "Import what can be read" | Import what can be read |

## Steps: Import, Image

New, Import, Image (or drop an image, share an image to Grapho). `ImportReviewSheet`, image
variant: thumbnail, `TextField` Name, `ListRow` Location, `RadioList` "Create as": **New canvas**
(default; the image is placed and fitted) or **Page of a new notebook**. Images over 50 megapixels
are scaled down with a line saying so. Refusals: "This image format is not supported." "This image is
too large (120 MP)." Inserting an image into an open page is Insert, in 15.

## Tap counts

| Task | Taps |
| --- | --- |
| Open a document | 1 |
| Import a notebook file | 4 (New, Notebook file, choose, Import) |
| Import an image | 4 (New, Image, choose, Import) |
| Recover an unreadable file | 2 (Save a copy to Files, choose place) |

## States

| State | What the user sees |
|---|---|
| Loading | `Skeleton` only after 300 ms, at least 300 ms |
| Error | See Outcomes; never a blank page, never a deleted file |
| Offline | A document on this device opens; a not-yet-synced one shows the syncing Banner |
| Very large | Progressive render; Pages tab and thumbnails are background jobs |
| Permission denied (files) | The Import row explains and links to system settings |

## Exits

Back leaves to the library, same scroll and selection. Cancel on the Progress item returns to the
library. Close on a Banner dismisses it for this open. The error panel's Back never removes the item.

## Wireframes

Partly damaged, Expanded:

```
+------------------------------------------------------------+
| < Week 4 Biology   1 of 37                          [More] |
| (!) 3 pages could not be read. Open read-only.             |
|     [ Open read-only ]   [ Export copy ]               [x]  |
+----+-------------------------------------------------------+
|[~]x|                                                       |
|    |             page 1 (readable)                          |
+----+-------------------------------------------------------+
```

Import, Notebook file, Same id different content:

```
+------------------------------------------------+
| Import notebook                           [x]  |
|  [cover] Week 4 Biology  37 pages, saved Mon   |
|  Name  [ Week 4 Biology (2)           ]        |
|  Location  Library                         >   |
|  (i) Another version of this notebook exists.  |
|      Replace the existing one                  |
+------------------------------------------------+
|                   [ Cancel ] [ Keep both ]     |
+------------------------------------------------+
```

Compact: the Sheet is a bottom Sheet; the error panel fills the page area with two stacked buttons.
German check: "Beide behalten" and "Abbrechen" stay on one line at 360 dp.

## Access

Screen reader: opening announces "Week 4 Biology, page 12 of 40"; Banners are announced when they
appear; read-only is announced and exposed as state. Keyboard: Enter opens, Escape or Back leaves.
Focus lands on the page area when open, and returns to the tile on Back.
