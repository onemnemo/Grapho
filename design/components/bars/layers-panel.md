# LayersPanel

## 1. Purpose

The body of the Side panel's Layers tab: the layers of the **whole document**, one list, the same
for notebooks, PDFs and canvases and on every platform. Layers are not per page.
Each layer can be shown or hidden, locked, renamed, reordered, merged down and deleted, and one
layer is active: the current tool writes into it. It is a composition of ListRow, IconButton,
SearchField and ReorderList, with no new control.

**When not to use:**
- Pages or Views: the Pages tab.
- Moving a selection to a layer: SelectionBar More, Move to layer (`selection.moveToLayer`).
- Anything about the tool or colour: [tool-settings-panel.md](tool-settings-panel.md).

## 2. Anatomy

```
Layers                                   [+]      title row (SidePanel header) and Add
[ Filter ................ ]                       only above 30 layers
 front  ----------------------------------------
 [::] [thumb] Highlights            [eye] [lock]  [...]
 [::] [thumb] Notes (active)  |     [eye] [lock]  [...]   active: tint plus bar
 [::] [thumb] Flashcards (missing)  [eye] [lock]  [...]   extension block layer, fallback
 [  ] [page ] PDF page                    [lock]  [...]   PDF only: locked, always last
 back   ----------------------------------------
```

| Part | Notes |
|---|---|
| Add | IconButton `quiet` `default`, `plus`, command `layer.add`, Tooltip "New layer". Adds above the active layer and makes it active |
| Row | A [ListRow](../tiles/list-row.md): drag handle, thumbnail lead, name, trailing visibility and lock, ••• |
| Thumbnail | A live miniature of the layer's content over the page colour, `radius/key`. A text-only fallback glyph for an empty layer |
| Name | `type/label`; tap the name of the active layer, or F2, renames in place (inline TextField) |
| Visibility | IconButton `quiet` `small`, `role` `toggle`, `eye` shown and `eye_off` hidden (the registry on form is a swap), `layer.toggleVisible` |
| Lock | IconButton `quiet` `small`, `role` `toggle`, `unlock` and `lock` (swap), `layer.toggleLock` |
| ••• | IconButton `more` per row, the layer Menu (section 9) |
| Filter | SearchField scope `filter`, shown above 30 layers |
| SelectionBar | The `panel` placement, at the foot, in selection mode only |

Top of the list is the front. Order is the stacking order the core stores.

## 3. Variants and sizes

`documentKind`: `notebook` | `pdf` | `canvas`. The list is identical; only the PDF adds a fixed row.
Rows use `size/row/list`; trailing toggles are role `small` in `size/hit/touch` slots. The panel
width (320 to 480) holds thumbnail, name and the three trailing controls; the name takes the rest.

- **PDF:** the PDF page is the **bottom layer**, a real row named "PDF page", locked for good. It
  cannot be moved, renamed, merged into or deleted, and nothing can be placed under it. Its lock
  toggle is disabled with the reason "The PDF page is always locked". Its visibility works: hiding it
  shows the annotations on blank paper. Export flattened is a document command, not here.
- **Notebook, canvas:** no fixed row. A page template or canvas background is not a layer.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Rest | `surface/raised`, name `text/default`, controls `action/quiet/fg` |
| Hover, Focus-visible, Active | `action/quiet/bg-hover`, README focus ring (inset), `action/quiet/bg-pressed`. Off for the pen while it writes |
| **Active layer** | `selection/fill`, `-hover`, plus a bar in `selection/indicator` at the start edge. Exactly one. A tap on a row sets it active. The accessible state is "active" |
| Selected (multi-select) | Same tint and a check in the selection slot; the active layer keeps its bar |
| **Hidden** | The eye turned `eye_off`; the thumbnail drawn at `state/drag/source-opacity`; name `text/muted`; "Hidden" in the accessible name and subtitle. The layer is not drawn on the page and cannot be written into |
| **Locked** | Lock turned `lock`; name stays `text/default`; "Locked" in the subtitle. The layer cannot be changed, selected or written into. It can still be shown, hidden, reordered, renamed, and deleted |
| Hidden and locked | Both cues |
| Active and locked or hidden | Allowed. The tool then refuses to write: after the pen lifts, a Tooltip hint at the pen says "Layer is locked" or "Layer is hidden", with no change to the panel. Setting the active layer to one of these does not move the active mark |
| One layer | The only layer: Delete and Merge down are disabled with the reason "The document needs one layer" and "Nothing below to merge into". It can be hidden and locked |
| **50 layers** | Scrolls inside the tab body; Filter shown; the active row scrolls into view on open. Over 200 rows the list virtualises. Drag auto scrolls at the list edges |
| **Extension block layer, extension missing** | The row stays, in its place and order. Thumbnail is the block's stored fallback in a placeholder; name is the stored name; subtitle "Needs Flashcards" (the extension's name only, no pack state). Show, hide, lock, reorder, rename and delete work. Merge down and Edit are disabled with "Needs Flashcards". When the extension is turned on again the row is live |
| Loading | Skeleton rows in the real layout after 300 ms; thumbnails fill as they render |
| Empty | Never; a document always has one layer |
| Error | A thumbnail that failed shows the glyph; the row still works |
| Drag | Source at `state/drag/source-opacity`; drop line `state/drag/insertion`; ghost on `z/drag-ghost`. The PDF row is not draggable and nothing drops below it |

## 5. Content rules

- **Very long name:** one line with an ellipsis, full name in the Tooltip and to screen readers; on
  touch, where a Tooltip is a long press, the Menu has Rename, which shows the full text in the
  field. German and Norwegian subtitles ("Ausgeblendet", "Skjult") are single words and measured.
- **No name:** new layers are "Layer 1", "Layer 2", counting up; never blank.
- **One, 50, 500 layers:** see States. Counts show in the title row: "Layers, 12".
- **Right to left:** the row mirrors; the handle moves to the end; the stacking direction does not.

## 6. Overflow rule

No More button. The list scrolls and Filter narrows it. Row controls never collapse: visibility and
lock are always visible, the rest are in the ••• Menu. Selection mode shows the `panel` SelectionBar
with four actions by command priority (Hide or Show, Lock or Unlock, Merge down, Delete) and More.

## 7. Extension slots

Extension block layers appear as ordinary rows (above). Extensions add no row actions here; a block
type's own commands are in the Selection bar More. Turned off: the row is not removed, because the
document owns the content, and shows its fallback as above.

## 8. Pack content

None. Layers hold the document's own content; pack assets in them are embedded copies.

## 9. Input

- **Stylus and touch:** tap a row to make it active; tap the eye or lock to toggle (not while the
  pen is on the page); long press then lift enters selection mode; long press then move drags the
  row and selects it (4.7.1). Palm rule: large contacts ignored in the panel while the pen is near.
- **Mouse:** click, hover shows the handle and ••• ; drag after 4 px; right click opens the Menu;
  Ctrl or Cmd click toggles selection, Shift click ranges.
- **Keyboard:** Up and Down move, Enter makes active, Space toggles selection;
  Alt+Up and Alt+Down move the layer (the drag equivalent), F2 renames, Delete deletes, Ctrl+Alt+2
  (the `panel.layers` chord) opens the tab. Eye and lock are tab stops inside the row.
- **Menu (per row, from •••, right click, Menu key):** Rename, Move up, Move down, Merge down,
  Duplicate layer, Delete. Every drag has these Move rows (4.8).
- **Commands to register:** `layer.add`, `layer.toggleVisible`, `layer.toggleLock`,
  `layer.rename`, `layer.moveUp`, `layer.moveDown`, `layer.mergeDown`, `layer.duplicate`,
  `layer.delete`, `selection.moveToLayer`. Each is one document undo entry; delete has no confirm
  Dialog (document undo, SYSTEM-MAP 4.2).

| Platform | Role |
|---|---|
| Web | `list` of `listitem`s in a `region` "Layers, 12"; "Notes, active, visible, unlocked"; toggles `button` with `aria-pressed` |
| Compose | `LazyColumn` with `Role` rows, `stateDescription`, custom actions Move up and Move down |
| WinUI | `ListView` with `CanReorderItems`, `AutomationProperties.Name` |
| SwiftUI | `List` with `onMove`, `accessibilityActions` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/row/list`, `size/control/small`, `size/hit/touch`, `size/icon/default`, `size/icon/small` | Rows, toggles |
| `surface/raised`, `border/subtle`, `radius/key`, `space/sm`, `space/md` | Rows, thumbnail |
| `selection/fill`, `-hover`, `selection/indicator`, `border-width/emphasis`, `action/toggle/fg-on` | Active, selected, toggles on |
| `action/quiet/*`, `state/disabled/fg`, `text/default`, `text/muted`, `type/label`, `type/caption` | Controls, text |
| `state/drag/source-opacity`, `state/drag/insertion`, `z/drag-ghost`, `skeleton/fill`, `component/focus/*` | Drag, loading, focus |

## 11. Do and Don't

**Do**
- Keep one list for the whole document, with the same behaviour in every kind and on every platform.
- Mark the active layer with tint and a bar, and say hidden or locked in words as well as icons.
- Keep the PDF page as the fixed, locked bottom row.
- Give every drag a Move up and Move down row, and one undo entry per change.

**Don't**
- Don't make layers per page, or show a separate layers surface over the writing area.
- Don't confirm a layer delete with a Dialog; it is undoable.
- Don't remove the row of a missing extension block; show its fallback.
- Don't let the pen write into a hidden or locked layer without saying why.

## 12. Traces to

Flows: 15 add-to-page (layers). Replaces: LayerList and the LayersPanel host.
Uses: ListRow, IconButton, SearchField, ReorderList, SelectionBar `panel`, Skeleton, Tooltip.
