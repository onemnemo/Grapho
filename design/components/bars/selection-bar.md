# SelectionBar

## 1. Purpose

Act on the current selection: it shows the count, up to four actions and More. One component for
every collection and for the canvas lasso, which share the bar and the commands but not the
gestures (SYSTEM-MAP 4.7).

**When not to use:**
- Commands for one item from its trigger or a right click: Menu.
- Choosing a value that applies at once: Popover.
- A single object's handles (resize, rotate): the canvas selection handles, not a bar.

## 2. Anatomy

```
Library, picker, panel (docked or floating, 600 dp and up)
+----------------------------------------------------------------------+
| [x] 3 selected    [Move to] [Star] [Tag] [Delete]    [ ... More ]     |
+----------------------------------------------------------------------+
Below 600 dp, or when labels do not fit (icons, each with a Tooltip)
+----------------------------------------------+
| [x] 3    [->] [*] [#] [bin]         [...]     |
+----------------------------------------------+
Canvas: the same row, floating above the selection and moving with it
```

| Part | Notes |
|---|---|
| Clear | IconButton `quiet` `small`, `close`, command `selection.clear` (to register), Tooltip "Clear selection, Escape" |
| Count | `type/label`, a live status: "3 selected", "All 412 selected", "3 of 12 shown, 3 selected". Plural keys per language |
| Actions | 0 to 4, each a Button `text` `small` with `type/label` and an icon, or an IconButton `quiet` `small` when icon only |
| More | IconButton `quiet` `small`, `more`, command `selection.more` (to register) |

## 3. Variants and sizes

`placement`: `library` | `canvas` | `panel` | `picker`. All are height `size/bar/note`, items role `small`.

| Placement | Where | Look |
|---|---|---|
| `library` | Bottom of a library place, floating, centred; above the system bar and the safe area | Pill `radius/surface` on `surface/raised`, `border/subtle`, `elevation/float`, `z/floating-bar` |
| `canvas` | Rides the lasso selection: centred above its bounds, below it when no room above, pinned to the page area's bottom centre when the selection is larger than the view | As `library` |
| `panel` | Bottom of the Pages tab in the Side panel, docked to the panel width | Flat on `surface/raised`, `border/subtle` hairline on top, `z/panel` |
| `picker` | Bottom of a Picker in manage mode, docked to the host. A fourth placement beyond the map's three, logged | As `panel` |

Labels versus icons is **measured at runtime for every placement**, never fixed at a window width:
the bar shows icon and label for all visible actions only if they fit with the count and More,
else icons only. Never a mix in one bar, and never a label without its icon. Below 600 dp it is
almost always icons.

The count has three forms, chosen by measure before anything wraps: long ("3 of 12 shown, 3
selected"), medium ("3 selected"), short ("3", with the full text as the accessible name and
Tooltip). If even the short form does not fit with two actions, the bar takes a second line for the
count.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Hidden | No selection, or selection mode ended (Escape, Back, Clear) |
| Appearing | Enters when selection mode starts, `motion/duration/base`, `motion/easing/enter`: a rise for `library`, a fade for the others. Reduced motion: opacity at 0 ms |
| Idle | Items `action/quiet/fg` on `surface/raised`; count `text/default` |
| Hover, Focus-visible, Active | As Button and IconButton |
| Disabled action | Stays in its place with `state/disabled/fg` and the reason ("Cannot move a folder into itself"). The set does not re-sort |
| Loading | A running action uses the inline busy pattern on its own button |
| Canvas, dragging | While the selection is moved, resized or the pen is on the page the bar is hidden by opacity, with no motion. A new lasso selection shows the bar at the lift. After a move or resize it returns after `delay/pen-idle`, so a pen that lifts and lands again does not flash it |
| Palm | While the pen is in proximity or within `delay/palm-window` of its last contact, a touch larger than the palm threshold is ignored, and a finger tap counts only if under `input/chrome-tap/duration` and `input/chrome-tap/travel` |
| Empty, Error, Selected, Drag | n/a. A failed action is a Toast with Retry, not bar state |

## 5. Content rules

- **Very long text:** labels never truncate ("Elementos seleccionados", "Valgte elementer").
  Under pressure: labels go to icons, then the count shortens, then actions go to More, then the
  count takes a second line.
- **No text:** icon-only mode needs a Tooltip per icon, name and shortcut, and the accessible name.
- **One item selected:** "1 selected". **500 items:** "All 412 selected" or "500 selected";
  Select all works on ids in the model, so the count is true off screen.
- **Filtered:** "3 of 12 shown, 3 selected".

## 6. Overflow rule

Four actions and More (SYSTEM-MAP 8). The icon rule of four icon-only actions on a bar's trailing
side counts these four; More is the overflow control and is not counted.

- The visible four are the first four commands, **by command priority**, whose context matches the
  placement and the selection kind (`selectionKind`). They do not re-sort by enabled state; a
  disabled one stays put with its reason.
- Library items: Move to, Star, Tag, Delete visible; Duplicate, Export, Share, Select all in More.
  Folders drop Tag and Share, so Edit look and Rename join in priority order. In Trash (items are
  not editable) the four are Restore (`library.restore`), Delete forever (`library.deleteForever`,
  a Dialog), Select all and Clear; Move to, Star and Tag are not offered.
- Canvas: the edit band by priority (Copy, Duplicate, Group, Delete and so on); layer and arrange
  commands in More. Pages tab: Duplicate, Move to page, Extract, Delete. Picker manage: Edit,
  Duplicate, Hide or Delete, Export as pack.
- When width runs out (a narrow `panel`, 200 percent text), actions leave from the end of the
  priority list into More, down to two; Clear, the count and More always stay.
- Extension selection actions are always in More, in the extension's group, which follows the Menu
  cap: at most five, then "More from extensions". More is a Menu.

## 7. Extension slots

Selection action (SYSTEM-MAP 7): always in More, as a row in the extension's group, labelled with
the extension's name. An extension never adds a visible action, a variant or a placement. When the
extension is turned off its rows disappear and return when it is turned on.

## 8. Pack content

None. In picker manage mode the actions are the Picker's (Hide for built-in and pack items,
Delete for the user's own); the bar adds no pack state.

## 9. Input

- **Entering:** touch long press then lift; any input the Select command (`library.select`); mouse
  Ctrl or Cmd click, Shift click for a range. The lasso creates the canvas bar by itself.
- **Stylus:** tap an action on lift. A pen contact on the page outside the bar clears or replaces the
  canvas selection. The bar ignores hover feedback while the pen writes.
- **Touch and mouse:** tap or click; More opens a Menu. The `library` bar floats above the system
  bar. Toasts rise above it so neither covers the other.
- **Keyboard:** the bar is one tab stop, reached by F6 or Tab after the grid; Left and Right move,
  Enter activates. Space toggles the focused item in the collection, arrows move. Escape ends
  selection mode (step 6 of Back). Ctrl+A selects all in the place. Delete and the shortcuts of
  the commands work without focus in the bar.
- **Handedness:** `canvas` clamps inside the page area, kept `size/edge-guard` from the writing-hand
  edge and clear of the dock; `library`, `panel` and `picker` do not mirror.
- **Commands to register:** `selection.clear` (Clear selection, Escape), `selection.more`.

| Platform | Role |
|---|---|
| Web | `toolbar` "Selection"; count `status`; More `button` with `aria-haspopup="menu"` |
| Compose | Row with a polite `liveRegion` on the count; items `Role.Button` |
| WinUI | `CommandBar`; count with `AutomationProperties.LiveSetting="Polite"` |
| SwiftUI | `HStack` with the toolbar trait; count `accessibilityLabel` as a status |

## 10. Tokens

| Token | Use |
|---|---|
| `size/bar/note`, `size/control/small`, `size/hit/min`, `size/icon/small` | Height, items, icons |
| `surface/raised`, `border/subtle`, `elevation/float`, `radius/surface` | Container |
| `space/sm`, `space/md`, `type/label`, `text/default` | Layout, count, labels |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Items |
| `motion/duration/base`, `motion/easing/enter`, `delay/pen-idle` | Appearing, canvas return |
| `delay/palm-window`, `input/chrome-tap/duration`, `input/chrome-tap/travel`, `size/edge-guard` | Palm rule, canvas clamp |
| `z/floating-bar`, `z/panel`, `component/focus/*` | Layers, focus (canvas bar is lowest in `z/floating-bar`, below the FindBar and NoteBar) |

## 11. Do and Don't

**Do**
- Keep the same visible four for a selection kind, and show a disabled one with its reason.
- Say the count in words and make it a live status.
- Hide the canvas bar by opacity while the pen is on the page, and show it again on lift.
- Keep the bar primary-free: the actions are `text` Buttons, none is `primary`.

**Don't**
- Don't show more than four actions or put a destructive one only in More without a Menu path.
- Don't drop the label of an icon-only action from its Tooltip and accessible name.
- Don't let it cover the toast stack or the dock; move the toasts or clamp the bar.
- Don't re-sort actions by use or by what is enabled.

## 12. Traces to

Flows: 05 organize-folders, 08 packs, 10 recover, 15 add-to-page (Pages tab, Layers).
Replaces: SelectionBar, LassoBar.
