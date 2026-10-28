# Document: kinds, chrome, tools, navigation, panels, layouts

Detail spec for the document surface: its kinds, chrome, tools, navigation, panels and layouts. Where
this file and SYSTEM-MAP section 1 or the Side panel row disagree, this file wins: the side panel
sits on the side away from the writing hand, and on compact it is a non-modal bottom panel.
Window class is computed from window width in dp and changes live on resize, keeping all state.

## 1. Document kinds and the shared chrome contract

| Kind | Content | Page model | Kind specific commands (registry context) |
|---|---|---|---|
| Notebook | pages of ink on a template | pages, one template per page | Add page, change template (document) |
| PDF | pages over PDF backgrounds | pages equal PDF pages plus inserted blank pages | text highlight tool when the PDF has text, page operations, Export flattened (document) |
| Canvas | one infinite surface | no pages | Minimap, Views, Fit all (canvas) |

The **shared chrome contract**: every kind has, in the same places with the same names:

1. A note bar: back, title, page indicator (a minimap toggle on Canvas), pinned items, More.
2. A Tool dock: current tool, quick slots, Undo, Redo.
3. A side panel with the tabs in section 11; Pages becomes Views on Canvas.
4. The wheel and the same input rules (palm, edge, finger gestures).
5. The same Undo, zoom, Find and Export commands.

A kind may add commands and hide ones that cannot apply (Add page on Canvas). It never adds a
surface type or moves a shared control.

## 2. Note bar

The note bar is the document chrome that may recede.

### Anatomy (full, expanded and medium)

```
[ < back ] [ Title ............ ] [ 12 of 300 ] [ pinned 1..4 ] [ More ... ]
```

| Part | Notes |
|---|---|
| Back | leaves the document to the place it was opened from, scrolled to the item |
| Title | tap renames in place; truncates with an ellipsis, full title in the tooltip and to screen readers |
| Page indicator | "12 of 300"; tap opens Go to page. Canvas: a minimap toggle with a map icon |
| Pinned items | at most four, of which at most two are extension items. User curated in Settings, Toolbar |
| More | the document menu: find, export, share, page setup, document settings, command palette, activity |

At **compact** the title truncates harder, pinned items move into More, and the core group is back,
title, page indicator, More. Position is floating (default) or full line (`toolbar.position`).

### States and timings

| State | When | Behaviour |
|---|---|---|
| Idle | no pen contact for 1.5 s, or just opened | fully visible |
| Writing (receded) | the pen touches the page | slides up and fades out over 150 ms. The page does not reflow or resize |
| Returning | a finger or mouse tap at the top edge (a pen tap there inks), a pen hover at the top edge for 500 ms, or 1.5 s without pen contact | slides in over 200 ms, ease out |
| Pinned open | a menu or Go to page is open from the bar | stays visible until closed |

Rules:

- The 1.5 s timer restarts on every pen contact, and does not run while the pen hovers within
  proximity range over the page.
- A tap at the top edge returns the bar at once; the tap is not passed to the page.
- While a screen reader or keyboard has focus in the bar, it does not recede.
- With reduced motion on, both transitions are instant opacity changes (no slide), at 0 ms.
- The note bar is never the only place for Undo or Redo (the dock holds them).

## 3. Tool dock

A small strip that never recedes. It sits on the edge away from the writing hand.

### Anatomy

```
Right-handed (left edge)        Left-handed (right edge)
+------+                        +------+
| Tool |  current tool          | Tool |
| S1   |  quick slots 1..5      | S1   |
| S2   |  pinned first,         | S2   |
| S3   |  then recent           | S3   |
| Undo |                        | Undo |
| Redo |                        | Redo |
+------+                        +------+
```

On compact in portrait the dock is a horizontal strip along the bottom, away from the hand
horizontally (left end for a right-handed writer). Order is the same: tool, slots, Undo, Redo.

| Part | Notes |
|---|---|
| Current tool | shows the tool, its colour and width as a real sample; tap summons the wheel (section 4) |
| Quick slots | up to five. Pinned slots come first and are never dropped; recent tools and colours fill the rest. A slot is a tool plus pen plus colour plus width. Long press a slot to pin or unpin, drag to reorder pinned ones. Fewer slots show when the window cannot fit them at 44 dp |
| Undo, Redo | `doc.undo`, `doc.redo`; dimmed when `canUndo` or `canRedo` is false, with a non-colour cue |

### States

| State | When | Behaviour |
|---|---|---|
| Idle | pen not touching | full contrast, all targets live |
| Writing | the pen is down on the page | stays in place and visible, at full size, but nothing in it animates, hover and press feedback are off for the pen, and nothing moves or blinks. Taps by pen or finger on Undo, Redo and slots still work |
| Pressed | a control is held | pressed fill |
| Menu open | slot long press | a Menu, light modality |

The dock never overlaps the page's writing area: the page is inset from the dock edge by the dock
width in expanded and medium, and the dock is outside the page canvas on compact. On a document with
the side panel open, the dock stays on its edge and the panel sits on the same side after it (section 11).

## 4. Wheel summon

| Input | Action |
|---|---|
| Tap the current tool in the dock | wheel opens beside the dock, centred on the tool |
| S Pen side button click | wheel opens at the pen tip, kept inside the window with 16 dp margin |
| S Pen side button held | eraser while held (`tool.eraserHold`); release returns to the previous tool |
| Keyboard: `T` (Tools command) | wheel opens in keyboard mode |
| Screen reader | wheel is exposed as a menu, section 4.2 |

Closing: lift the pen or finger on a choice, tap outside (light modality, the tap is not passed to
the page), or Escape. The wheel has rings: Tools, Pens, Colours (the current colour set, up to 24).
A fourth placement of the picker: the wheel shows a compact pick of pens and the current colour
set, and **More** opens the full picker in the Side panel.

### 4.1 Keyboard mode

Arrow Left and Right move between segments in a ring, Arrow Up and Down move between rings, Enter
picks and keeps the wheel open for the next ring, Space picks and closes, Escape closes with no
change. Home and End jump to the first and last segment. A visible focus ring marks the segment.

### 4.2 Screen reader mode

When a screen reader is running, summon opens a menu instead of a radial layout, with groups Tools,
Pens, Colours. Each item is a menu item with its name, and a selected state. The Side panel Tools tab
(Tool settings, section 11) offers the same choices as a linear list at any time, so the wheel is
never the only way.

## 5. Handedness mirroring

`general.handedness` is asked at first run (default right) and changeable in Settings and from the
dock menu. Everything that sits away from the hand mirrors:

| Item | Right-handed | Left-handed |
|---|---|---|
| Tool dock | left edge | right edge |
| Side panel | left, next to the dock | right, next to the dock |
| Toasts | bottom, off-hand side (left) | bottom, off-hand side (right) |
| Page scrubber | off-hand edge (left) | off-hand edge (right) |
| Writing-hand edge guard | right edge | left edge |
| Library drawer | start edge | start edge (does not mirror) |
| Note bar | centred, no mirror | centred, no mirror |

Mirrored layouts reverse order inside the dock only along the screen axis, not top to bottom.
Right-to-left languages mirror the library, the note bar order and text, but the dock still follows
handedness, not language direction.

## 6. Palm and edge rules, finger gestures

- **Pen-only drawing** (`pen.penOnly`) turns on once a stylus has been used on the device.
  Fingers then scroll, pan and pinch and never draw.
- **Palm window**: while the pen is in proximity or within 1 s of its last contact, finger touches
  on the page are ignored, except two-finger pan and pinch.
- **Edge guard**: touches that start within 16 dp of the writing-hand screen edge do not open
  chrome (no swipe to open the panel, no drawer gesture) while pen-only is on.
- **Finger taps**: a two-finger tap is Undo, a three-finger tap is Redo. Only in pen-only mode,
  ignored within 1 s of pen activity, a tap means all fingers down and up within 250 ms without
  moving more than 10 dp. Both are configurable in Settings, Pen and tools and can be turned off.
- Without a stylus (no pen-only mode), fingers draw with the same tools and the finger taps are
  off, since they conflict with multi-touch drawing.
- A mouse or trackpad draws with the pen tool, scrolls with the wheel, pans with space plus drag
  or middle drag, zooms with Ctrl plus wheel.

## 7. Page navigation (Notebook and PDF)

| Part | Behaviour |
|---|---|
| Page indicator | "12 of 300" in the note bar; a transient chip with the same text appears top centre while scrolling and fades 1.5 s after scrolling stops |
| Go to page | tap the indicator or `nav.goToPage`: a small Popover with a number field (validates 1 to N, Enter goes), previous and next buttons, and for PDFs the document outline when it has one |
| Scrubber | on the off-hand edge, appears while scrolling in documents over 20 pages. Drag to scrub; a bubble shows the page number and a thumbnail once the thumbnail exists. It hides 800 ms after use. Touch target 44 dp wide |
| Pages tab | virtualized thumbnails; add page, duplicate, reorder by drag, delete (undoable), insert pages from a PDF or blank. Multi select with the selection grammar. A drag shows the drop line, and every drag has a menu equivalent (Move up, Move down, Move to page) |
| Keyboard | Page Up and Down, Ctrl+Home and End, Ctrl+G |

Rules: Page delete is a document undo entry, not a confirm dialog. Deleting the only page of a
notebook is disabled with the reason. Thumbnails of large documents come from a job (section 9).

## 8. Zoom

- Pinch zooms around the pinch centre. Range 25% to 800%, snap hints at 100% and fit.
- Commands: Fit width, Fit page, 100%, Zoom in, Zoom out (`view.*`).
- A transient zoom chip ("150%") shows top centre while zooming and for 1.5 s after.
  Tapping the chip resets to Fit width. It is a button with the label "Reset zoom".
- Zoom and scroll position are remembered per document (document scope) and restored on reopen.
- Double tap with a finger is not used for zoom (it conflicts with tapping to place); Fit width is a
  command and a chip tap.
- Canvas: zoom is unbounded within 5% to 1600%, Fit all is the Fit page equivalent.

## 9. PDF specifics

- **Text highlight tool** (`tool.textHighlight`): present only when the PDF has an extractable text
  layer (`documentKind(pdf)` and a text layer flag). Drag across text to highlight; it snaps to words
  and lines. Highlights are annotations stored in the document, with the text copy kept for search.
  Scanned PDFs without text do not show the tool and show a hint in the Tool settings tab on how
  to use the highlighter pen instead.
- **Page operations** (Pages tab and menu): rotate, delete, duplicate, reorder, insert blank page,
  insert pages from another PDF, extract pages to a new document. All undoable. Operations change the
  Grapho page list and leave the original PDF bytes unchanged until Export.
- **Large PDFs**: pages render on demand around the viewport (the current page, two ahead and one
  behind first), with a placeholder of the right size so scroll length is stable. Thumbnails are a
  core job (`thumbs.build`) that starts after open, runs in the background, and shows a progress
  item only if still running after 1000 ms. The Pages tab shows skeleton tiles while a thumbnail is
  missing and fills them as the job proceeds. A 300 page PDF must open and be writable before
  thumbnails finish.
- Import is a job. See ownership.md for job rules.

## 10. Canvas specifics

- **No pages.** The page indicator becomes a minimap toggle. The minimap is a small overview in the
  corner on the off-hand side, showing content extents and the viewport as a rectangle. Tap or drag
  in it to move the viewport. Closed by tapping the toggle, Escape, or `view.minimap`.
- **Views**: the Pages tab becomes Views, a list of saved positions (name, zoom, position, a small
  thumbnail). Actions: save current view, rename, update, delete (undoable), go to. Keyboard:
  Ctrl+Alt+1 opens the tab; Go to page on canvas is Go to view.
- Fit all frames the content bounds. An empty canvas explains one sentence and one primary action.

## 11. Side panel

Tabs, in order: **Pages** (Views on Canvas), **Layers**, **Insert**, **Tool settings**, then
extension panels. The rail shows six tabs; the rest sit in the rail's overflow menu.

| Tab | Holds |
|---|---|
| Pages / Views | section 7 and 10 |
| Layers | layer list with visibility, lock, rename, reorder by drag, merge, delete (undoable) |
| Insert | one picker with sources: stickers, images, templates, and extension insert sources as sources in the picker's source filter. No tabs inside tabs |
| Tool settings | pen settings and the colour editor for the current tool; Content, Pens link |
| Extension panels | declarative trees from the extension runtime |

One home per job: pens are picked in the wheel, edited in Tool settings, managed in Content, Pens.

### Behaviour by window class

| Class | Panel | Page | Notes |
|---|---|---|---|
| Compact (under 600) | non-modal bottom panel at half height, drag handle to full height; swipe down or Escape or Back closes it | the page above stays live and writable; panel overlaps nothing but the lower half | drag handle has a 44 dp target; at full height the page is covered but the panel is still non-modal; no scrim |
| Medium (600 to 839) | overlays the page edge on the off-hand side, 320 dp wide | the page can be panned clear of it; the page does not reflow | panel closes with its button, Escape, or the tab toggle |
| Expanded (840 and up) | beside the page on the off-hand side, 360 dp wide | the page area shrinks and stays centred in the remaining width | resizable by drag between 320 and 480 dp |

The panel is a Side panel surface (non-modal, no scrim). Its z order is above chrome and below
popovers.

## 12. Layouts

Legend: `NB` note bar (recedes while writing), `D` Tool dock, `P` side panel, `S` scrubber (only over
20 pages while scrolling), `T` toasts, `Z` zoom chip, `#` page. R is right-handed, L is left-handed.

### Compact (under 600), panel open at half height

R (dock at bottom, left end; scrubber and toasts on the left):

```
+----------------------------------+
| < Title            12 of 300 ... |  NB
+----------------------------------+
|S|                                |
|S|            #####               |
|S|            #####               |
|S|            #####   (live)      |
+----------------------------------+
|  ---- handle ----                |
|  Pages | Layers | Insert | Tool   |  P half height
|  [thumbs ...]                    |
+----------------------------------+
| [Tool][S1][S2][S3][Undo][Redo]   |  D
| T                                |
+----------------------------------+
```

L (dock at bottom, right end; toasts and scrubber on the right):

```
+----------------------------------+
| < Title            12 of 300 ... |  NB
+----------------------------------+
|                                |S|
|            #####               |S|
|            #####               |S|
+----------------------------------+
|  ---- handle ----                |
|  Pages | Layers | Insert | Tool   |  P
+----------------------------------+
|   [Tool][S1][S2][S3][Undo][Redo] |  D
|                                T |
+----------------------------------+
```

### Medium (600 to 839), panel overlays the off-hand edge

R:

```
+--------------------------------------------------+
| < Title          12 of 300     [pin][pin] [More] |  NB
+--+-----------------------------------------------+
|D |                                               |
|T |      +-------+                                |
|U |      | #####  |   (page can pan clear)        |
|S1|P     | #####  |                               |
|S2|P     +-------+                          Z      |
|Un|P                                               |
|Re|P                                               |
+--+-----------------------------------------------+
 T (toast bottom left)
```

L:

```
+--------------------------------------------------+
| < Title          12 of 300     [pin][pin] [More] |  NB
+---------------------------------------------+----+
|                                             | D  |
|                  +-------+                  | T  |
|  (page can pan)  | ##### |                P  | S1 |
|                  | ##### |                P  | S2 |
|                  +-------+                P  | Un |
|                                             | Re |
+---------------------------------------------+----+
                                     T (toast bottom right)
```

### Expanded (840 and up), panel beside the page

R:

```
+-----------------------------------------------------------+
| < Title             12 of 300      [pin][pin][pin] [More] |  NB
+----+----------+-------------------------------------------+
| D  | Pages    |                                           |
| S1 | Layers   |            +-----------+                  |
| S2 | Insert   |            |  #####    |                  |
| S3 | Tool     |            |  #####    |             Z    |
| Un | [panel]  |            +-----------+                  |
| Re |          |                                         S |
+----+----------+-------------------------------------------+
 T (bottom left, off-hand side)
```

Here the scrubber sits on the off-hand edge. In the right-handed layout the off-hand edge is the
left, so `S` is drawn at the left edge of the page area, next to the panel, when the panel is
closed; with the panel open the scrubber sits on the panel's page-side edge.

L:

```
+-----------------------------------------------------------+
| < Title             12 of 300      [pin][pin][pin] [More] |  NB
+-------------------------------------------+----------+----+
|                                           |  Pages   | D  |
|                +-----------+              |  Layers  | S1 |
|                |  #####    |              |  Insert  | S2 |
|    Z           |  #####    |              |  Tool    | S3 |
|                +-----------+              | [panel]  | Un |
|                                           |          | Re |
+-------------------------------------------+----------+----+
                                  T (bottom right, off-hand side)
```

On a hardware keyboard window the layouts are identical. The library sidebar is not part of the
document layout. Opening a document replaces the library place, and Back returns to it.

## 13. Modal and stacking inside a document

- Go to page, a slot menu and colour popovers are Popovers (light modality: no scrim, tap outside
  dismisses and is not passed to the page).
- Page export, Share, Page setup are Sheets. A Sheet never opens another Sheet, and Insert's "Get more"
  pushes the catalogue view inside the host. Back and Escape order is in accessibility.md.
- A Dialog is used for: delete forever and unsaved changes in Studio. Install consent and review are Sheets.
- Toasts in a document sit on the off-hand side bottom and never over the writing area centre.

## Open questions

1. The side panel sits on the side away from the writing hand, and this spec mirrors it for
   left-handed use. SYSTEM-MAP section 3 still says "end edge". Open question: update that row of
   the map to match.
2. On compact in portrait the dock is a bottom strip. The rule is "the edge away from the writing
   hand", which on a portrait phone-sized window could also be a vertical strip. The spec chose a
   bottom strip to keep the page width. Open question: confirm this choice.
3. Fingers draw without a stylus. The gesture rules apply only in pen-only mode. Open question:
   confirm that two-finger tap Undo stays off for finger drawers.
