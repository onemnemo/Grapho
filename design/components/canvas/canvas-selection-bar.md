# Canvas selection bar

## 1. Purpose

The canvas rules for the SelectionBar `canvas` placement: how the bar rides a lasso selection
without costing ink. This is not a second component. Anatomy, actions, count, labels versus icons,
More and the extension rules are in [bars/selection-bar.md](../bars/selection-bar.md); this file
adds only what the page changes.

**When not to use:**
- Selection in the library, a picker or the Pages tab: SelectionBar `library`, `picker`, `panel`.
- Handles for one object (resize, rotate): the canvas selection handles (`canvas/handle`), not a bar.
- A one-time choice with no selection (a recolour for the next stroke): the ToolWheel.

## 2. Anatomy

```
 above the selection (default)                 no room above: below it
   +--------------------------------+            ..............
   | [x] 3   [Copy][Dup][Group][Del] [...] |     ..selection...
   +--------------------------------+            ..............
        ............ lasso ............          +----------------+
        ..... selected strokes .......           | [x] 3  ...  [...] |
                                                 +----------------+
 selection larger than the view: pinned to the page area's bottom centre
```

The parts are SelectionBar's (Clear, Count, up to four Actions, More). One addition: when the
selection holds strokes, shapes or text, the first action is **Colour**, an IconButton showing the selection's
current colour as a dot; it opens the colour swatches Popover (a recipe from the Popover family).

## 3. Variants and sizes

`placement`: `canvas` (of SelectionBar). Height `size/bar/note`, items `small`, pill `radius/surface`,
fill `surface/raised`, edge `border/subtle`, `elevation/float`, `z/floating-bar`. The visible four
come from the registry, not the kind.

The visible four are the first four enabled commands in registry priority order, the same for every
kind; a command whose predicate does not allow the kind is skipped. What is left goes to More. One
label per command, the registry's own.

| Order | Command | Label | Enabled for |
|---|---|---|---|
| 1 | `selection.colour` (to register) | Colour | stroke, shape, textBlock |
| 2 | `edit.copy` | Copy | all |
| 3 | `edit.duplicate` | Duplicate | all |
| 4 | `edit.delete` | Delete | all |
| More | `edit.cut`, `edit.group`, `edit.bringForward`, `edit.sendBackward`, then extension rows | Cut, Group, Bring forward, Send backward | by predicate |

A disabled action shows the fixed reason strings of `registries.md` section 1.1.

Labels or icons are measured as for SelectionBar. On the canvas below 600 dp the bar is icons only.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Hidden | No selection, Escape, a tap on empty page, a new tool chosen |
| Appearing | At the lasso lift, opacity 0 to 1 over `motion/duration/fast`, no movement. Reduced motion: 0 ms |
| Idle | As SelectionBar: items `action/quiet/fg`, count `text/default` |
| Riding | While the page is panned or zoomed by finger, mouse or key, the bar follows the selection bounds each frame, with no easing. Positions recompute from the bounds; it does not jump between above and below mid-gesture |
| Dragging selection | The selection is moved or resized with the pen or finger: the bar is hidden |
| Pen down | The pen touching the page hides the bar by opacity over `motion/duration/chrome-recede`, with no translation, once. It does not return mid-stroke. If the selection remains after the lift (a move, a resize, a contact inside it) the bar returns after `delay/pen-idle`. If the contact made a new lasso, the bar belongs to the new selection and appears at that lift. If the selection was cleared there is no bar |
| Hover, Focus-visible, Active | As Button and IconButton |
| Disabled action | Stays in place with `state/disabled/fg` and the reason (the registry reason) |
| Loading | An action that runs uses inline busy on its own button |
| Empty, Error, Selected, Drag | n/a. A failed action is a Toast with Retry |

## 5. Content rules

- **Very long text:** labels never truncate; the bar goes to icons, then shortens the count, then
  moves actions into More (SelectionBar section 5). The bar is at most `size/bar/note-max` wide.
- **No text:** icons need a Tooltip each, name and shortcut ("Duplicate, Ctrl+Shift+V"). Group shows no chord until the registry resolves the clash of `edit.group` and `nav.goToPage` (Ctrl+Alt+G).
- **One item:** "1 selected". **500 items:** "500 selected" (a lasso over 2,000 strokes reads
  "All 2,000 selected"); the count is from ids in the model, never from what is on screen.

## 6. Overflow rule

Four actions and More (SYSTEM-MAP 8). Overflow is by command priority, as in SelectionBar. The bar
never wraps and never grows a second row on the canvas, except the count taking its own line at
200% text, in which case the bar's height grows by that line and the position recomputes at the
next lift.

## 7. Extension slots

Selection action: always in More, in the extension's group, labelled with the extension's name; at
most five, then "More from extensions". Turned off: the rows disappear and return on turning on.

## 8. Pack content

None.

## 9. Input

**Position** (recomputed from the selection bounds clamped to the page area, in order):

1. Centred above the bounds, `space/md` clear of the top edge of the selection.
2. If there is no room above (the band stack or the top of the view), centred below.
3. If the selection is larger than the view, pinned to the page area's bottom centre.

Always clamped inside the page area, `size/edge-guard` from the writing-hand edge, and clear of the
dock, the open side panel and the toast stack; if it would sit over the toasts, the toasts rise above it.
The bar is never over the lasso's own handles: it moves away along its axis.

**Stylus:** the bar is a hover-gated control. A pen runs an action only when the pen has hovered over that
action for `delay/pen-dwell` (300 ms) before it touches; any other pen contact on the bar is ink,
including its gaps, so a dot or a full stop beside the selection is never an action. A pen without
hover reports no dwell, so its contacts on the bar are ink and the bar is used by finger, mouse or
keyboard. Delete never runs from a pen alone: with a pen it needs the dwell and then a second contact
(Delete has Undo as well). **Clearing:** a pen contact on the page does not clear the selection. A new
lasso loop replaces it at the lift; a dot or a stroke that is not a closed lasso leaves it. The
clear routes are Escape, the Clear button, choosing another tool and `selection.clear`.

**Touch:** tap. While the pen is in proximity or within `delay/palm-window`, a touch larger than
`input/palm-threshold` is ignored on the bar, and a finger tap counts only if under
`input/chrome-tap/duration` and `input/chrome-tap/travel`. A palm never dismisses the bar.

**Mouse:** click; More opens a Menu. **Keyboard:** the bar is one Tab stop after the page while the
selection exists, F6 reaches it as the content landmark's neighbour; Left and Right move, Enter
activates, Escape clears (`selection.clear`). Edit shortcuts work from the page without focus in the
bar. **Roles:** `toolbar` named "Selection actions", count a `status`. Compose: a row with
`Role.Button` items; WinUI `CommandBar`; SwiftUI `ToolbarItemGroup` or a `HStack` of buttons.

**Commands:** `edit.copy`, `edit.cut`, `edit.duplicate`, `edit.delete`, `edit.group`,
`edit.bringForward`, `edit.sendBackward`, `selection.clear`, `selection.more`. To register:
`selection.colour` (Colour of selection).

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/raised`, `border/subtle`, `elevation/float`, `z/floating-bar` | The pill |
| `text/default`, `action/quiet/fg`, `state/disabled/fg` | Count, actions, disabled |
| `radius/surface`, `space/sm`, `space/md`, `size/bar/note`, `size/bar/note-max` | Shape, gaps, size |
| `canvas/handle`, `canvas/handle-edge` | The selection's handles the bar keeps clear of |
| `size/edge-guard` | Clamp |
| `motion/duration/fast`, `chrome-recede` | Appear, hide |
| `delay/pen-idle`, `delay/pen-dwell`, `delay/palm-window`, `input/palm-threshold`, `input/chrome-tap/*` | Timers and input (`delay/pen-dwell` to add) |

## 11. Do and Don't

**Do**
- Hide it by opacity only, once, when the pen touches the page, and bring it back after the lift.
- Pin it to the bottom centre when the selection is bigger than the view.
- Keep the same four actions in place; a disabled action stays with its reason.
- Return the bar only when the selection survived the contact.
- Count from the model, so the count is true off screen.

**Don't**
- Don't run an action, least of all Delete, from a plain pen contact.
- Don't let it cover the dock, the panel, the toasts or the lasso handles.
- Don't move it between above and below while the user pans.
- Don't run an action from a pen stroke that crosses the bar.
- Don't offer a primary Button or an extension action as one of the four.

## 12. Traces to

Flows: 03 write-and-switch-tools (lasso, recolour), 15 add-to-page (the new item arrives selected).
Replaces: LassoBar.
