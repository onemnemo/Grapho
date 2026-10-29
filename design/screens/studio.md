# Screen: Studio

## 1. Purpose

A full-screen editor for one look (a cover, a folder style, a canvas preset, a template or a pen), with
a live preview of the real item. It is a place: Done saves, Cancel and Back discard (after one ask if
anything changed). The look's name is its title field: name it where it is made.

## 2. Flows served

07 customise appearance (main), 02 create (New look inside NewItemSheet), 08 (made looks become packs
through the Picker's Export as pack, not here). A sticker has no editor; it is imported.

## 3. Entry and return

| From | Studio opens | Done returns to |
|---|---|---|
| NewItemSheet (New or Edit look in a look Picker) | The Sheet closes with its draft kept | The Sheet, reopened, the new look selected; the Sheet commits with its own Done |
| Settings, Content, a type (ContentPage New or Edit) | Replaces the place | The Content page, the item in view |
| A document's Tool settings tab (a pen) | Replaces the document place | The document, the panel reopened on the same tab |

Cancel and Back with changes ask once with the Dialog `unsaved` (default Keep editing). Without changes
they leave at once. Studio is a place, so system Back follows the Back order step "the place".

## 4. Layout and regions

Regions: top bar (Cancel, title, Undo and Redo, Done), canvas, preview, panel (type-specific controls).
The top bar is `size/bar/top`. The panel and rail sit on the off-hand side; Done stays top end and
Cancel top start in both hands.

### Expanded (840 and up), R

```
+------------------------------------------------------------------------+
| [Cancel]   [ Cover 3        ]                  [Undo][Redo]    [ Done ] |
+--+---------------------+------------------------------------------------+
|R | Colour   Pattern    |                                                |
|a | [swatches]          |           +---------------+                    |
|i | Texture  [slider]   |           | live cover    |  preview of the    |
|l | Title text [field]  |           +---------------+  real tile         |
+--+---------------------+------------------------------------------------+
```

R: the rail (canvas tools) and the panel (type controls) sit together on the off-hand (left) side, the
canvas to their right, so the writing hand reaches a clear canvas. L mirrors this: rail and panel on
the right. Done stays at the top end and Cancel at the top start in both hands. The rail and the
editor panels per type are specified in `components/pickers/studio-panels.md`.

### Medium (600 to 839)

```
+--------------------------------------------------------+
| [Cancel]   [ Cover 3 ]            [Undo][Redo]  [ Done ] |
+--------------------------------------------------------+
| [rail as a top strip: brush  eraser  shapes  text ...]   |
|     +---------------+    +--------------+                |
|     | canvas        |    | preview inset|   P over the   |
|     +---------------+    +--------------+   off-hand edge|
+--------------------------------------------------------+
```

The panel is a SidePanel `over` at `size/panel/width-over`; the canvas shifts away from it and can still
pan. The real tile preview is an inset on the canvas corner.

### Compact (under 600)

```
+----------------------------------+
| [<] [ Cover 3      ] [Un][Re][Done]|  Cancel becomes Back when labels do not fit
+----------------------------------+
|  canvas  (the pen draws here)    |
|                    [preview inset]|
+----------------------------------+
| Colour | Pattern | Text            |  Tabs (2 to 4)
| controls ...                     |
+----------------------------------+
```

The controls are a bottom panel with peek and full snaps; the canvas stays above and live. With the
keyboard up the title field and the active control scroll into view.

### Short

The panel is `over`; the top bar remains; the preview inset is hidden unless the window is at least
600 wide; the Tabs content scrolls.

## 5. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Shell | StudioShell `layout` beside (Expanded), over (Medium), bottom (Compact) | The one frame for every look; the type supplies the canvas and the panel |
| Top bar | Button `text` (Cancel), TextField `inline` (title, defaults to the next name "Cover 3"), IconButton quiet (Undo, Redo: `studio.undo`, `studio.redo`), Button `primary` (Done, `studio.done`) | Done is the one primary. Done is disabled when the title is empty, with the reason "Name it to save" |
| Preview | The real renderer-kind tile at its registry size (PickerTile `cover`, `imageTile` or `pageThumbnail`) | Always visible, in its real text wrap, also at 200% |
| Rail | Studio rail from `studio-panels.md` | Canvas tools for the type, on the off-hand side |
| Panel | Editor panel per type from `studio-panels.md`, in SidePanel structure with Tabs (two to four) or the TabRail rule above four | Controls are Slider, PickerTile `swatch`, ColourEditor `role` plain, TextField, Select, Toggle, SegmentedControl (at most three short options) |
| Dialog | Dialog `unsaved` | "Discard changes?" Keep editing is the default focus |
| Feedback | Banner danger for a failed save with Retry; Toast after return "Cover changed" with Undo; Skeleton in the canvas | Work is kept on failure |

A pen is edited here as a look only when the type is editable; a pen's width and opacity live in the
Tool settings tab, not in the Studio.

## 6. States

| State | What changes |
|---|---|
| New | The type's default look, never blank; title "Cover 3" |
| Edit | The existing look loaded; title is its name |
| Dirty | Cancel and Back ask once |
| Undo or Redo unavailable | `state/disabled/fg`, plain; "Nothing to undo" as the description |
| Saving | Done shows inline busy after 300 ms and keeps its width |
| Save error | Banner danger in the top region "Could not save. Try again." Retry; nothing is lost; the Studio stays |
| Loading | Skeleton in the canvas well after 300 ms, held 300 ms |
| Offline | Not applicable |
| Very large history | Per-session history is bounded by the core; Redo clears on a new edit |
| Empty title | Done disabled; the field shows the message "Name it to save" |
| Long German | "Abbrechen" and "Fertig" would shrink the title below its minimum: Cancel becomes the Back arrow IconButton; the title field keeps its minimum width and truncates with the full text in the field |
| 200% text | The top bar wraps nothing: Undo and Redo stay, the title shrinks first, Cancel becomes Back; the panel scrolls; Tabs become a Select when labels do not fit |
| Palm | The canvas follows the document palm rule; Undo, Redo and the panel ignore palm-size touches while the pen is in proximity |

## 7. Behaviour on resize and split screen

The layout follows the window class live. Crossing 840 moves the panel from beside to over; below 600 it
becomes the bottom panel with Tabs. The canvas, the zoom, the active tab, the title text and the history
are kept. A change of class never discards a draft. The Sheet that opened Studio keeps its draft
meanwhile and is not drawn.

## 8. Keyboard and focus order

Regions: top bar, canvas (main region with a text description updated when controls change), controls
panel. F6 cycles them. Order in the top bar: Cancel, title, Undo, Redo, Done. Ctrl+Enter is Done;
Escape asks if anything changed, else cancels; Ctrl+Z and Ctrl+Y undo and redo. Arrival focus is the
title field. On return to the Sheet, focus lands on the Cover row. Screen reader: the preview has a
text description; controls are standard form controls; Done and Cancel are the last two buttons in
reading order.
