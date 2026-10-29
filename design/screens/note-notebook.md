# Screen: Note, notebook (writing)

## 1. Purpose

The writing surface for a notebook: pages of ink on a template. The page belongs to the pen, so chrome
recedes while writing and returns in one move. This file is the reference for the shared chrome
contract of every document kind (NoteBar, ToolDock, ToolWheel, SidePanel, FindBar). note-pdf.md and
note-canvas.md state only what differs.

## 2. Flows served

03 write and switch tools (main), 11 navigate, 06 (find in document), 13 (open, read-only, banners),
15 add to page (Insert, Layers), 10 (document Undo), 09 (extension panels, tools, blocks), 14 (Share,
Export).

## 3. Layout and regions

Regions: NoteBar (top, recedes), Page (the canvas), ToolDock (edge away from the writing hand, never
recedes), SidePanel (after the dock), Canvas overlay (CanvasChip top centre, PageScrubber off-hand
edge), Banner band (under the NoteBar position, outside the receding group), Toast stack (bottom,
off-hand side). The library Sidebar is not part of a document.

The receded NoteBar leaves only the NoteBarHandle. The page never reflows when the bar recedes or
returns. `toolbar.position` `floating` overlays the page; `line` reserves its row and leaves it empty
when receded.

### Expanded (840 and up), R, bar returned, panel open

```
+-----------------------------------------------------------+
| < Week 4 Biology   12 of 40      [pin][pin][pin]   [More] | NoteBar
+----+----------+-------------------------------------------+
| T  | Pages    |                                           |
| S1*| Layers   |S           +-----------+                  |
| S2*| Insert   |S           |  page     |                  |
| S3*| Tool     |S           |           |             Z    |
| Un | [panel]  |            +-----------+                  |
| Re |          |                                           |
+----+----------+-------------------------------------------+
 S = scrubber on the off-hand (left) edge of the page area, next to the panel
 [Toast, bottom left]
```

### Expanded, R, writing: bar receded, panel closed

```
+-----------------------------------------------------------+
|                       [ = ] handle                         |
+----+------------------------------------------------------+
| T  |S scrubber only over 20 pages, while scrolling         |
| S1*|S           ####### ink ########                       |
| S2*|S           ####### ink ########        [ 150% ] chip   |
| Un |                                                       |
| Re |                                                       |
+----+------------------------------------------------------+
```

### Expanded, L

```
+-----------------------------------------------------------+
| < Week 4 Biology   12 of 40      [pin][pin][pin]   [More] |
+-------------------------------------------+----------+----+
|                                           |  Pages   | T  |
|                +-----------+              |  Layers  | S1 |
|    Z           |   page    |             S|  Insert  | S2 |
|                +-----------+             S|  Tool    | S3 |
|                                          S| [panel]  | Un |
+-------------------------------------------+----------+----+
                                 [Toast, bottom right]
```

The dock is on the right edge, the panel after it on the same side, and the scrubber and Toasts on the
right too: the off-hand side is the dock side for either hand (system/document.md section 5). The order in
the dock never reverses.

### Medium (600 to 839), panel overlays the off-hand edge

```
R:                                              L:
+---------------------------------------------+   +---------------------------------------------+
| < Title     12 of 40   [pin][pin]  [More]   |   | < Title     12 of 40   [pin][pin]  [More]   |
+--+------------------------------------------+   +-----------------------------------------+---+
|T |                                          |   |                                         | T |
|S1|P (320, over the page edge)    +----+     |   |     +----+    P (over the page edge)  | S1|
|S2|P                              |page|  Z  |   |  Z  |page|                            | S2|
|Un|P                              +----+     |   |     +----+                            | Un|
|Re|P                                         |   |                                         | Re|
+--+------------------------------------------+   +-----------------------------------------+---+
```

The page can be panned clear of the panel; it does not reflow.

### Compact (under 600), panel at half height

```
R:                                    L:
+----------------------------------+  +----------------------------------+
| < Title            12 of 40  ... |  | < Title            12 of 40  ... |  NoteBar `core`
+----------------------------------+  +----------------------------------+
|S|           #####                |  |                #####           |S|
|S|           #####   (live)       |  |                #####  (live)   |S|
+----------------------------------+  +----------------------------------+
|  ---- handle ----          [x]   |  |  ---- handle ----          [x]   |
|  [P][L][I][T][..]                |  |  [P][L][I][T][..]                |  TabRail
|  Pages ...                       |  |  Pages ...                       |
+----------------------------------+  +----------------------------------+
| [Tool][S1][S2][S3][Un][Re]       |  |       [Tool][S1][S2][S3][Un][Re] |  corner strip
| T                                |  |                                T |
+----------------------------------+  +----------------------------------+
```

At 360 wide the dock holds Tool, Undo, Redo and as many slots as fit (3 at 360, 5 from about 480);
pinned slots are never dropped. The page keeps at least `size/page/min` above the panel.

### Short (under 500 tall)

The dock is the horizontal corner strip whatever the width; the SidePanel is `over` (stays `bottom` if
the window is also Compact); the page minimum drops to half of `size/page/min` when it conflicts with
the panel body. The FindBar and the NoteBar share the band stack (section 6).

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | NoteBar `position` floating or line, `layout` full (Medium, Expanded) or core (Compact, 200% text) | Back, Title (rename in place), page indicator (Button "12 of 40"), up to four pinned IconButtons (two extension at most), More. Core group: back, title, indicator, More |
| Recall | NoteBarHandle | Visible, focusable pill; recall by top-edge finger or mouse tap, pen hover at the edge for 500 ms, the handle, 1.5 s without pen contact, `bar.show` (F6), or focus entering the bar. A pen tap at the top edge always inks |
| Dock | ToolDock `orientation` vertical or horizontal | Tool sample, QuickSlots 1 to 5 (pinned first, factory pins Pen, Eraser, Highlighter), Undo, Redo, optional Status. No menu |
| Wheel | ToolWheel `placement` pen or dock, `mode` radial or menu | Rings Tools (eight), Colours; Pens in the middle; width and opacity are edited in the pen's segment; Previous is `tool.previous` (Q, or a double tap on the dock tool) |
| Panel | SidePanel `placement` beside, over or bottom; `snap` peek or full | TabRail: Pages, Layers, Insert, Tool settings, extension tabs (six shown, rest in overflow Menu) |
| Pages tab | Picker slot PickerTile `pageThumbnail`, ReorderList, SelectionBar `panel`, Skeleton | Add, Duplicate, Delete (undoable), Insert pages, Move earlier or later |
| Layers tab | Layers panel from `components/bars/layers-panel.md` | Layers belong to the whole document and are listed in the panel, not floating over the page |
| Insert tab | Picker `placement` panel with Tabs for stickers, images, templates; SourceFilter; footer Get more, Import, New | Take photo and Choose image are Images' New slot; no tabs inside tabs |
| Tool settings tab | Tool settings panel from `components/bars/tool-settings-panel.md`, built from Picker (pens, manage mode) as the tab root, ColourEditor `host` panel, Picker for colour sets, SettingRow with Slider, Stepper (quick slots shown, 0 to 5), Toggle, Select | A "Tools" ListRow list with a Toggle per tool and a Menu "Remove from wheel" row; "More in Settings" link rows |
| Extension tab | ExtensionPanelHost `provenance` installed or sideloaded | Declarative tree; first-use header "Flashcards, installed by you" when sideloaded |
| Find | FindBar | Docked under the NoteBar position; stays visible while open; outside the receding group; FindMarks on the page |
| Overlay | CanvasChip `kind` scroll, zoom, snapped; PageScrubber (over 20 pages); Popover (Go to page, slot Menu) | Chips are never pen targets |
| Block | BlockPlaceholder `cause` | A missing extension block: received-document.md |
| Feedback | Banner, Toast (off-hand side, never over the writing centre), ProgressItem, Tooltip (first-use hint with Got it) | |

### The document menu (More), commands only, at most 10 rows

Find (Ctrl+F); Add page, Insert pages, Page setup; Export, Share; Extensions group (at most five, then
"More from extensions"); Command palette; Settings (opens the place). Rename is the Title. Star and
Move are library commands. Pen-only and dark paper live in Tool settings and Settings. Every row is a
registry command; none is dead or duplicated.

## 5. Writing, switching and recall (the pen behaviour)

- **Recede**: first frame of a drawing-tool contact starts the bar leaving; gone and the handle drawn in
  150 ms. After that, nothing in the chrome moves while the pen is down. Dock, panel and FindBar stay
  still and live to taps.
- **Return**: 1.5 s after the last contact (timer paused while the pen hovers), top-edge tap, hover
  500 ms, the handle, F6, or focus entering the bar. 200 ms ease out. Writing wins over a return.
- **Switching** by input (cost): erase and back with the S Pen button 0, others 2; colour 2 (1 if in a
  slot); width 2; lasso recolour 4; Undo 1. The wheel opens at the pen tip on a button click or beside
  the dock on a tool tap; hover tracks the pen; a tap chooses; a finger press, drag and lift chooses;
  leaving range or lifting off the plate never applies.
- **Pen-only** on once a stylus has touched; fingers scroll, pan and pinch. Palm window 1 s: touches
  above the palm threshold on chrome are ignored. The 16 dp writing-hand edge opens no chrome. Two-finger
  tap is Undo and three-finger Redo only in pen-only mode.
- **Wheel access**: keyboard mode (arrows within and between rings, Enter picks and stays, Space picks
  and closes, Home and End, Escape closes) and screen reader menu form with groups Tools, Pens, Colours.
  The Tool settings tab holds the same choices as a list at any time, so the wheel is never the only
  way.

## 6. Band stack and vertical budget

Order from the top: safe mode strip, NoteBar, FindBar, Banner. Compact 360 by 480: NoteBar 48 plus dock
56 leaves 376 for the page. A panel at Peek hides the NoteBar and keeps at least `size/page/min`. The
FindBar hides the NoteBar. The on-screen keyboard (a hex field in Tool settings) hides the NoteBar and
drops the panel to Peek; the field and a page strip scroll into view.

## 7. States

| State | What changes |
|---|---|
| Idle | Bar visible, dock full contrast |
| Writing | Bar receded, handle shown; dock static but live; Toasts wait |
| Undo or Redo unavailable | Dimmed with a non-colour cue; "Nothing to undo" as the description; never hidden |
| New notebook, 1 page | Indicator reads "1 of 1" and is hidden as a Button; no scrubber; Delete page disabled with the reason |
| First note | A first-use hint Popover on the dock tool: "Tap to change tools. Click the S Pen button to open the wheel anywhere." with Got it. Without a stylus: "Tap the pen to change tools." No reflow |
| Loading | The frame (NoteBar, dock, page area) appears at once; Skeleton page shape after 300 ms, held 300 ms; the first page writable first |
| Very large (2,000 pages) | First page now; Pages tab Skeleton tiles; ProgressItem "Preparing pages, 120 of 2000" after 1000 ms with Cancel; the document stays |
| Autosave fails | Banner danger "Changes are not saved. There is not enough space." with Free space; stays outside the receding group |
| Read-only | Banner (13); the dock Pen is unavailable with the Tooltip "Read-only"; read, search, zoom and export still work |
| Pen-only off | Fingers draw; finger-tap Undo and Redo off |
| No S Pen | Button gestures inert; slots, wheel taps and keys do everything |
| Offline | Not applicable. A not-yet-synced document shows its syncing Banner (13) |
| Extension off or missing | Its tab, slot, pinned item and menu rows disappear; blocks show BlockPlaceholder; positions restore when it returns |
| Long German | Pinned items drop into More first, then the title truncates with the full name in the Tooltip; dock icons never change width with language |
| 200% text | NoteBar is `core`; the dock keeps Tool, Undo, Redo and fewer slots; the panel body scrolls; wheel labels stay in the hub |

## 8. Resize and split screen

Class follows the window width live and keeps the page position, zoom, selection, the current tool and
the open panel tab. Expanded to Medium turns the beside panel into an overlay and keeps its tab;
Medium to Compact turns it into the bottom panel at Peek. A split-screen half width is Compact. The
dock switches vertical to a corner strip on Compact and Short without changing order. An open wheel
closes if the window is smaller than its minimum (`size/wheel/min-window`) and reopens as the menu form.
A Popover re-anchors. Handedness never changes with the window.

## 9. Keyboard and focus order

Document regions in order: NoteBar, Page, ToolDock, SidePanel, Toasts (F6 and Shift+F6 cycle; a
receded bar returns when focus enters it). One tab stop per bar, arrows rove (NoteBar left and right,
dock up and down or left and right on the strip). 1 to 5 select slots, T the wheel, Q previous tool,
E, P, H, L tool letters (suspended while a text field has focus), Ctrl+Z, Ctrl+Y or Ctrl+Shift+Z,
Ctrl+F find, Ctrl+Alt+G Go to page, Ctrl+Enter Add page, Ctrl+. toggles the panel, Ctrl+Shift+1 to 4
open its tabs, Ctrl+K palette, Ctrl+, Settings. Escape returns focus from the panel to the page on
Medium and Expanded and closes the bottom panel on Compact. Focus never moves on its own while the
pen writes. When the wheel closes focus returns to the dock tool; when the Go to page Popover closes it
returns to the indicator. Focus on the page lands when a document opens and on the tile on Back.

Back order: Popover or Menu, wheel and FindBar (as popovers), Dialog, pushed view in a Sheet, Sheet,
pushed view in the panel, bottom panel (Compact), selection, leave the document. Back leaves to the
place it came from at the same scroll and selection.
