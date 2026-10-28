# 15 Add something to the page, and manage layers

## Goal

During a lecture, put a photo of the board, a sticker, a template page or a shape on the page
without leaving it, and arrange it in layers, with the page still live.

Counting rule: see 01. Mid-writing adds 1 for the Note bar reveal where the Note bar is used.

## Entry points

The Side panel Insert tab (Ctrl+Shift+3) and Layers tab (Ctrl+Shift+2); Note bar More, Insert pages;
Ctrl+Shift+I (`insert.photo`); paste (Ctrl+V); a file or image dropped on the page; the palette.

## Steps: insert from the Insert tab

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Open the Insert tab | Side panel | `SidePanel` with its tab rail |
| 2 | Choose the type in the `Tabs` row: Stickers, Images, Templates, an extension's insert source. Under it the Picker has its own single source filter | Side panel | `Tabs`, `Picker` (pick mode), `SearchField` |
| 3 | Tap an item to place it at the centre of the visible page, or drag it onto the page | Document | `PickerTile`, `DragGhost`; the target highlights on enter |
| 4 | The item arrives selected with its handles; the `SelectionBar` rides it | Document | `SelectionBar` (Duplicate, Layer, Delete, More) |
| 5 | Undo is one dock tap | Tool dock | `IconButton` |

Every drag has a menu route ("Insert" on the tile's Menu). Get more and Import are in the Picker
footer (08). Templates insert as new pages (Insert pages) after the current page, undoable.

## Steps: take a photo of the board

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Insert tab, Images type, Take photo (the Images type's New slot); or Ctrl+Shift+I | Side panel | `Picker` footer slot, `Button` |
| 2 | First time: the camera permission prompt, with one line before it "Grapho uses the camera only to take this picture." | system prompt | platform |
| 3 | Take the photo in the system camera; return | system | |
| 4 | Crop (optional): `CropMode` frame and bar over the page, Done or Skip | Document | `CropMode` (existing), `Button` |
| 5 | The photo is placed and selected | Document | as above |

Camera denied: the Take photo row shows "Camera is off for Grapho." with "Open system settings"
and "Choose from Photos" beside it; nothing is lost. No camera app: "No camera app found." Photo
library denied: the same pattern.

## Steps: layers

The Layers tab lists layers for the whole notebook (the same on every platform): `ListRow`s with
a thumbnail, name, visibility and lock `Toggle`s.

| Action | How |
|---|---|
| New layer | Add button; the current tool writes into the active layer |
| Rename | tap the name, in place |
| Reorder | drag in a `ReorderList`; Menu rows "Move up, Move down" are the equivalent |
| Show or hide, lock | the row `Toggle`s |
| Merge down, delete | row Menu; delete is undoable (document stack) |
| Move a selection to a layer | `SelectionBar` More, Move to layer |

## Tap counts

| Task | Taps |
| --- | --- |
| Place a sticker | 3 (panel toggle, Stickers source if not open, tile); open already 1 |
| Take a photo of the board and place it | 4 (panel toggle, Images, Take photo, shutter in the camera app) plus the first-time permission tap; Crop Skip +1 |
| Hide a layer | 2 (panel toggle, Layers tab) then 1 per layer |

## States

| State | What the user sees |
|---|---|
| Insert tab empty source (Yours stickers) | `EmptyState` sm "No stickers yet." Get more, Import |
| Filter with no match | "No stickers match 'moon'." Clear |
| Loading | `Skeleton` tiles after 300 ms |
| 500 stickers | Virtualised grid with Filter |
| Very large image | Scaled to the page; the original is kept in the document only when "Keep original" is on in Tool settings |
| Camera or photos denied | See above |
| Offline | Not applicable except Get more (08) |
| Error: cannot place | Toast "Could not add the picture." Retry; nothing partial |
| Compact | The Insert tab is the bottom panel at Peek, 200 dp of page above stays live; drag works from the panel onto the page |

## Exits

Close the panel (button, Escape); the placed item stays. Cancel in `CropMode` keeps the original
photo uncropped. Undo removes the placement.

## Wireframes

Insert tab, Expanded, right-handed:

```
+----+------------------+--------------------------------------+
| T  | Pages Layers     | < Week 4                    [More]   |
| S1 | Insert  Tool     |        +------------+                 |
| Un | [Filter]         |        | page   [S] |  S dragged here |
| Re | (All)(Stickers)  |        +------------+                 |
|    | [s][s][s][s]     |                                       |
|    | Take photo  Get more                                    |
+----+------------------+--------------------------------------+
```

Layers tab:

```
+------------------+
| Layers      [+]  |
| [t] Ink    [eye][lock] |
| [t] Photo  [eye][lock] |
| [t] Page   [eye][lock] |
+------------------+
```

Compact: the Insert and Layers tabs are the bottom panel at half height; drag a tile up onto the
page above. Medium: the panel overlays the off-hand edge, 320 dp. Left-handed: the panel mirrors.

## Access

Keyboard: Ctrl+Shift+3 and 2 open tabs; arrows move in the grid, Enter inserts; Delete removes the
selection. Screen reader: tiles are named; a placed item is announced "Sticker added, selected";
layer rows read "Ink, visible, unlocked". Focus returns to the tile after insert.
