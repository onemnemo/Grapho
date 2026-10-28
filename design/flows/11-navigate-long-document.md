# 11 Navigate a long document

## Goal

Know where you are in a 300 page PDF or a long notebook, get to any page in seconds, keep the zoom
you like, and work the same way on an infinite canvas.

Counting rule: see 01. Counts assume the Note bar is visible; mid-writing add 1 for the reveal.

## Entry points

A document is open. Surfaces: `NoteBar` page indicator, `Popover` Go to page, Canvas overlay
(`CanvasChip`, `PageScrubber`, `Minimap`; the Note bar page indicator is a `Button`), `SidePanel` Pages tab (Views on a canvas), keyboard, the
palette.

## Steps: go to a page

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Tap "12 of 300" in the Note bar (Ctrl+Alt+G) | Popover (light) | `Button` in the Note bar as the indicator, `Popover` |
| 2 | Type a number; Enter goes. Previous and Next beside it. A PDF with an outline lists it | Popover | `TextField` (numeric, validates 1 to N), `IconButton` x2, `ListRow` outline |
| 3 | The Popover closes; the page scrolls into place | | |

The Go to page Popover applies on Enter and has no Done. Keyboard: with the indicator focused,
Left and Right arrows go to the previous and next page; Page Up and Down, Ctrl+Home and End work
anywhere.

## Steps: scrub

Scrolling a document over 20 pages shows the `PageScrubber` on the off-hand edge (SYSTEM-MAP section
3 Canvas overlay row). It is a visible handle of 44 dp; only a touch that starts on the handle
drags it, and in pen-only mode a touch elsewhere on the strip does nothing, so a gripping hand does
not scrub. A bubble shows the page number and a thumbnail once it exists. It fades 1.5 s after use.
The scroll chip "12 of 300" appears top centre while scrolling and fades after 1.5 s. The scrubber
has an adjustable-value role for screen readers (swipe up or down changes the page).

## Steps: Pages tab

Panel toggle (Ctrl+.) or Ctrl+Shift+1: virtualised `PickerTile`s in the page-thumbnail renderer, the
current page marked. Add, Duplicate, Delete (undoable), Rotate (PDF), Insert pages from a PDF or
blank. Reorder by drag in a `ReorderList`; the Menu rows "Move earlier, Move later, Move to page"
are the equivalent. Tap a tile to go to the page. Multi-select uses the selection grammar.

## Steps: zoom

Pinch around the pinch centre, 25% to 800%, snap hints at 100% and fit. Commands: Fit width
(Ctrl+1), Fit page (Ctrl+2), 100% (Ctrl+0), Zoom in and out. The zoom chip "150%" is a `CanvasChip` (top centre, fades after 1.5 s)
at top centre while zooming and for 1.5 s after; tapping it is "Reset zoom" (Fit width). Zoom and
position are remembered per document. Finger double tap is not used for zoom.

## Steps: canvas

No pages: the indicator becomes a minimap toggle. The `Minimap` is a corner overview on the
off-hand side with content extents and the viewport rectangle; tap or drag to move; close with the
toggle, Escape or Ctrl+M. The Pages tab becomes **Views**: saved positions (name, zoom, position,
thumbnail) as `ListRow`s. "Save current view" adds a row already in rename mode (inline
`TextField`, the same in-place rename as in the library). Rename, update, delete (undoable), go to.
Go to page is Go to view (Ctrl+Alt+G opens the Views list in a Popover). Fit all frames the content.
Zoom 5% to 1600%. An empty canvas shows one sentence and one primary action.

## Tap counts

| Task | Taps |
| --- | --- |
| Go to page 200 | 2 taps (indicator, Enter) plus typing the number; the field has focus when the Popover opens, though on touch the keyboard raises with it; Ctrl+Alt+G with a keyboard |
| Jump by thumbnails | 2 (panel toggle, tile); panel stays open |
| Scrub through 300 pages | 1 drag on the handle |
| Reset zoom | 1 (chip, while it shows) or Ctrl+1 |
| Add a page at the end | 2 (More, Add page); mid-writing 3; Ctrl+Enter |
| Save a canvas view | 3 (panel toggle, Views tab, Save current view) plus typing the name; 2 when the Views tab is already open |

## States

| State | What the user sees |
|---|---|
| 1 page | "1 of 1"; no scrubber; Delete page disabled with the reason |
| Up to 20 pages | indicator and Pages tab, no scrubber |
| 300 pages | indicator, scrubber, virtualised Pages tab |
| Thumbnails not built | `Skeleton` tiles; the bubble shows only the number; `ProgressItem` "Preparing pages, 120 of 300" over 1000 ms |
| 10,000 pages | Lists virtualise; Go to page takes a number |
| Out of range | Typed 400 of 300: the field shows "1 to 300", Enter disabled; a deep link clamps silently |
| Zoomed beyond the window | Pan with two fingers; the chip shows the percent |
| Empty canvas | `EmptyState` sm on the canvas |
| A page fails to render | A framed placeholder "Could not show this page. Retry."; the rest work |
| Offline | Not applicable |
| Compact | The panel is the bottom panel at Peek or half height; the page above stays live |

## Exits

Enter goes and closes the Popover. Escape or tap outside closes it, the tap is not passed to the
page. Closing the panel returns the width. Back leaves the document, same scroll and selection.

## Wireframes

Expanded, right-handed, scrolling (chip top centre, scrubber on the off-hand left edge):

```
+------------------------------------------------------------+
| < Lecture slides  [ 12 of 300 ]                     [More] |
|                       [ 12 of 300 ]  (transient chip)      |
+----+-------------------------------------------------------+
| T  |S|                                                     |
| S1 |S|            +--------------+                          |
| Un |S|            |  page        |                          |
| Re |S|            +--------------+                          |
+----+-+-----------------------------------------------------+
```

Go to page Popover and Pages tab:

```
   +-------------------+       +-----------+---------------------+
   | Go to page        |       | Pages     | [+][Dup][Del][Rot]  |
   | [ 200   ] [<] [>] |       | [01][02]  |                     |
   | Outline: 1 Intro  |       | [03][**]  |  (* current)        |
   +-------------------+       +-----------+---------------------+
```

Canvas with minimap: a small overview in the off-hand corner with the viewport rectangle. Compact:
the indicator stays in the core group; the scrubber is a 44 dp handle on the off-hand edge. Medium:
the panel overlays the off-hand edge at 320 dp. Left-handed: scrubber, panel and minimap mirror.

## Access

Keyboard: Ctrl+Alt+G, Page Up and Down, Ctrl+Home and End, arrows on the focused indicator, Ctrl+1/2/0.
Screen reader: "Page 12 of 300"; the scrubber is an adjustable value; thumbnails are named "Page
12"; the Pages tab list reorders via the Menu rows. Focus returns to the indicator after Go to page.
