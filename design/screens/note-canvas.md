# Screen: Note, canvas (infinite canvas, minimap, views)

## 1. Purpose

One infinite surface with no pages: for mind maps, diagrams, a whiteboard. Same chrome, tools and input
rules as a notebook; the page indicator becomes a minimap toggle, the Pages tab becomes Views, and
zoom is wider. Everything not listed here is as note-notebook.md.

## 2. Flows served

11 navigate (canvas section), 03 (tools), 15 (Insert, Layers), 02 (New canvas), 13 (huge canvas open).

## 3. What differs from a notebook

| Aspect | Canvas |
|---|---|
| Page model | None. No Add page, no page indicator, no scrubber, no Go to page. Go to page is Go to view |
| NoteBar | The indicator is an IconButton toggle `map` (command `view.minimap`, Ctrl+M) showing the Minimap |
| Side panel | Pages becomes Views; Layers, Insert, Tool settings and extension tabs unchanged |
| Zoom | 5% to 1600%; Fit all frames the content bounds (the Fit page equivalent) |
| Background | A canvas preset (grid, dots, plain) chosen at New canvas; Page setup edits it through NewItemSheet in edit mode |
| Selection | The lasso with the canvas SelectionBar that rides the selection |

## 4. Layout and regions

Regions as the notebook, plus the Minimap in the off-hand top corner (below the band stack, movable).
Chips sit top centre; the Minimap and chips share the fixed slot rule: they are placed below the full
height of the band stack whether or not each is showing.

### Expanded, R, Minimap on, Views tab open

```
+-----------------------------------------------------------+
| < Mind map       [map*]        [pin][pin]            [More] |
+----+-----------+--------------------------------------------+
| T  | Views     | +------+                                   |
| S1 | + Save    | |mini  |        (infinite canvas)          |
| S2 | Intro  >  | |[view]|    ink, blocks, shapes            |
| Un | Chapter 2 | +------+                        [ 150% ]     |
| Re | Summary   |                                            |
+----+-----------+--------------------------------------------+
```

The Minimap sits in the off-hand top corner of the page area (left for R, next to the panel). When the
panel is open it moves to the panel's page-side edge, so it never hides under the panel.

### Expanded, L

The dock, panel and Minimap mirror to the right; the selection bar clamps `size/edge-guard` off the
right (writing-hand left) edge as in the bars README.

### Medium, Compact, Short

Medium: Views overlays the off-hand edge at 320 wide; Minimap width `size/minimap/width`.
Compact: Views is the bottom panel at Peek; the Minimap uses `size/minimap/width-compact` and closes when
the panel opens, so the page keeps its height; the dock is the corner strip on the off-hand side.
Short: panel `over`, Minimap height clamped to `size/minimap/height-min` to `height-max`.

## 5. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| NoteBar | IconButton toggle `map` | Selected look: `selection/fill` plus indicator. Tooltip "Minimap, Ctrl+M" |
| Minimap | Minimap | Content extents as ink (except the viewport rectangle), tap or drag to move the viewport (pen allowed on the viewport and the strip), letterboxed, reach limited to `limit/minimap-reach` viewports. Closes with the toggle, Escape or Ctrl+M |
| Views tab | ListRow per saved view (name, zoom, position, small thumbnail), ReorderList, Button "Save current view", IconButton | Save adds a row already in rename mode (inline TextField). Rename, Update, Delete (undoable), Go to |
| Go to view | Popover (Ctrl+Alt+G) listing Views | Applies on selection, no Done |
| Chips | CanvasChip `kind` zoom, snapped | Snapped: "Snapped to circle" with Undo for finger, mouse and keyboard; a pen holds 600 ms to snap and gets ink on every chip |
| Selection | SelectionBar `placement` canvas | Rides the lasso; opacity step at pen contact only; colour, group, delete; More for extension actions |
| Blocks | BlockPlaceholder | Missing extension blocks |
| Empty | EmptyState `first-use` `sm` on the canvas, no mascot | One sentence "Draw anywhere. Zoom out to see the whole canvas." and one primary "Start writing" that closes it and selects the Pen; it fades on the first stroke |

## 6. States

| State | What changes |
|---|---|
| Empty canvas | The EmptyState above; Fit all and Minimap toggle are disabled with the reason "Nothing on the canvas yet" |
| Loading | Frame at once; the viewport renders at reduced detail first, then sharpens; far regions are placeholders until viewed |
| Huge canvas (memory) | Same, with a ProgressItem "Opening Mind map" after 1000 ms and Cancel returning to the library |
| Views empty | EmptyState `quiet` in the tab: "No saved views." with the Button "Save current view" in the tab header |
| 50 views | Virtualised list, Filter in the tab header; thumbnails Skeleton until built |
| Zoom at limit | The chip shows "1600%" and further pinch does nothing; "5%" the same |
| Minimap off-screen content | Content beyond the reach limit is clipped; the viewport remains visible |
| Error, autosave | Banner danger as the notebook |
| Offline | Not applicable |
| Long German | View names wrap to two lines in the tab; the NoteBar title truncates after pinned items move to More |
| 200% text | Views rows grow; the Minimap stays at its token size; NoteBar `core` |
| Palm | As the notebook; touches on the Minimap above the palm threshold are ignored near the pen |

## 7. Behaviour on resize and split screen

Viewport centre, zoom, the selection and the active Views tab survive class changes. A change that
narrows the window keeps the same canvas point at the centre (letterboxed Minimap re-scales). Opening a
bottom panel on Compact closes the Minimap; closing the panel restores it if it was on. The Minimap
can be moved to another corner; the choice is remembered per device.

## 8. Keyboard and focus order

Order: NoteBar, canvas, ToolDock, SidePanel, Toasts. Arrows pan the canvas when it has focus; Ctrl+plus,
Ctrl+minus and Ctrl+0 zoom; Fit all is a view command (palette and its key); Ctrl+M toggles the
Minimap; Ctrl+Alt+G opens Go to view; Ctrl+Shift+1 opens the Views tab (Pages' slot). In Views: arrows
move rows, Enter goes to the view, F2 renames, Delete deletes (undoable), Alt+Up and Alt+Down reorder.
Focus returns to the NoteBar toggle after the Minimap closes by Escape. Screen reader: the Minimap is a
region with a text description of the viewport position ("Viewing the top left"); canvas objects are
reached through the selection model.
