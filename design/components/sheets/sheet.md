# Sheet

## 1. Purpose

The one modal surface for a task that needs focus and room: create, pick a look, review an import or
install, move to a folder, first run. It owns a header, a body that can push views, optional steps,
and a foot. NewItemSheet and ImportReviewSheet are Sheets with a fixed body (their own files); the
recipes in [sheet-recipes.md](sheet-recipes.md) (Move, Remove, Export, Pack builder, Catalogue) are
Sheets with a composed body, not new components.

**When not to use:**
- A short decision with no list and no field: use Dialog.
- A small choice that applies at once and needs no Done: use Popover.
- Commands for a target: use Menu.
- Work that stays beside the page: use the Side panel.
- Editing a look with a live preview: use StudioShell.

## 2. Anatomy

```
 scrim (scrim/default)
 +--------------------------------------------------+  container: surface/overlay, radius/container
 | [Back]   Title                      [Close]       |  header: size/sheet/header, fixed
 |--------------------------------------------------|
 | [ tabs or SegmentedControl ]       (optional)     |  tab row, fixed
 |--------------------------------------------------|
 |  status Banner (one, top of body)                  |
 |  body: scrolls; pushed views replace it            |
 |--------------------------------------------------|
 | step label (above, full width)                     |
 |                       [Cancel]  [ Primary ]        |  foot: fixed
 +--------------------------------------------------+
  compact: container on the bottom edge, decorative handle above the header
```

Parts: scrim, container, handle (compact), header (Back, title, Close), tab row, Banner, body,
pushed view stack, foot (secondary text button, Cancel, primary), step label.

## 3. Variants and sizes

Placement follows the window class. `size`: `default` | `wide`. Size belongs to the Sheet, not the
view: a pushed view never resizes the container. A Picker view must fit the Sheet's size; a Sheet
that hosts a Picker is opened as `wide`.

| Window class | Placement | Width |
|---|---|---|
| Compact | bottom, full width | the window |
| Medium, expanded | centred | the smaller of `size/sheet/width/default` (or `wide`) and the window minus two `layout/gutter/*` |

- Height, bottom: at most the window height minus the top safe area. Centred: at most the window
  height minus two `space/4xl`. Both: content height up to that, then the body scrolls.
- Header height is `size/sheet/header`. Foot buttons are `size/control/default`, never large.

**Compact layout under pressure.** When the software keyboard is up, or text is at 200%, or the
header plus foot would take more than half the visible height, the Sheet switches to this layout:

1. It goes full height (bottom Sheet) or fills the window (centred).
2. The header is one line: Back (if any), title, Close. The handle hides.
3. The foot keeps only the primary. Cancel is the header Close. A secondary text button moves to
   the end of the body, above the foot.
4. The body always keeps at least a third of the visible height; if it would not, the step label
   and the Banner collapse into the body first.

## 4. States

| State | Change |
|---|---|
| default | `surface/overlay`, `elevation/sheet`, `z/sheet`; under more contrast an edge of `border/strong` |
| hover, focus-visible, active, selected | live on controls inside; the container has none |
| opening | slide up (compact) or scale 0.96 to 1 with fade (large) over `motion/duration/pop`; reduced motion: fade over `motion/duration/fast` |
| pushed view | body slides from the end edge over `motion/duration/base`; reduced motion cross fades |
| loading | focus sits on the title container until content lands, then never moves; Skeleton after 300 ms; the primary uses inline busy; Cancel stays live |
| empty | the body hosts an EmptyState `sm`; the primary is disabled with a reason hint |
| offline or stale target | Banner `warn`; the primary is disabled with a reason ("Folder was removed") |
| error | Banner `danger` with retry, announced as `alert`; the body scrolls to top; fields keep values |
| disabled primary | disabled `action/primary/*` with the reason hint |
| dragging (compact) | the handle drags down; past 30% of the height or a fast flick it closes (or asks, if dirty); otherwise it returns with no overshoot |

## 5. Content rules

- The title is one line, `type/heading`; a long title ellipsises in its middle. Back and Close
  never shrink. A Sheet always has a title (its accessible name).
- German and Norwegian run 30 to 40% longer. Foot buttons stack before any label truncates; stacked,
  the primary is on top and first in focus order, Cancel below, so visual and focus order agree. The
  step label sits above the stack, full width, and wraps.
- 1 item: a short form; the container stays at content height. 500 items: a virtualised list or
  grid; header, tab row, Banner and foot stay fixed.

## 6. Overflow rule

- **Foot**: Cancel, one primary and at most one secondary text button. A third action goes in the
  body or a Menu in the header.
- **Header end**: Close only, plus at most one IconButton (More) in a pushed view.
- **Tabs inside a sheet**: two or three sections use SegmentedControl. Four or five use an equal
  width tab row. Tabs fall back to a Select, by measured fit, when labels do not fit. More than five:
  no more tabs. The Sheet becomes a start-side list with a detail pane (width `size/sheet/list`);
  on compact the list is the first view and each entry pushes its detail. Tabs never nest.
- **Pushed views**: three levels below the root. Each has Back in its header and its own title.
  In a pushed view the Sheet foot is replaced by the view's own foot; "Cancel" there is Back and
  pops one level. A pushed view that applies a choice pops itself. Dirty state is the whole Sheet's:
  popping never asks; leaving the Sheet does.
- **Banner**: one at a time, by priority (error, warn, info). A new error replaces a lower one.
  Known statuses reserve their space at open; a late arrival animates in once and focus stays.

## 7. Extension slots

None. Extensions cannot add Sheets or fields. An extension install is reviewed in ImportReviewSheet
kind `extension`.

## 8. Pack content

A Sheet shows pack state only inside the Picker it hosts (source filter, Hidden entry) and in
ImportReviewSheet. Names are exact: **Off**, **Hidden**, **Kept from <pack>**, **Not installed**.

## 9. Input

**Dismiss and unsaved changes**

- Cancel, Close and Escape are all Cancel. A dirty Sheet (any field, selection or pushed choice
  differs from the opening value) first shows the Dialog `unsaved`. A clean Sheet closes at once.
  Close is not in the tab order while a foot Cancel exists; it stays a pointer target and the
  accessible name of the header control. Both have the same command and a Tooltip.
- Scrim tap closes only when nothing changed. When dirty it does not close: Cancel's focus ring
  pulses once and a polite status says "Unsaved changes. Choose Cancel to discard." The same for
  the Dialog scrim.
- First run: scrim tap does nothing. Skip and Escape keep the defaults and close with no Dialog.
- "More in Settings": ask keep or discard, close, then navigate; Back from that page returns to the
  place under the Sheet.
- Back and Escape follow SYSTEM-MAP section 3: Popover or Menu, Dialog, a pushed view, the Sheet.
  Predictive back on a dirty Sheet: committing the gesture cancels the preview and shows the Dialog.
- Studio opened from a Sheet closes the Sheet with its draft kept, and returns to it.

**A Sheet over a document**

- A Sheet takes all input. On open, a stroke in progress is finished and kept, and no stroke reaches
  the page behind. No Sheet opens while the pen is down: every opener queues until pen up.
- The scrim consumes every pointer. A tap dismisses only if it is finger sized, short (under 300 ms,
  under 8 dp of movement) and no pen is in proximity or within 1 s of its last contact. A palm, or
  any contact above the palm threshold, never dismisses anything.

**Footer, the one primary, and Enter**

- One primary, end side, naming its verb (Create, Import, Install, Move, Done). A Picker view has no
  primary: choosing applies and pops. Cancel is `text` before it.
- Enter runs the primary when focus is not in a multi line field and it is enabled. Ctrl or
  Cmd+Enter runs it from a multi line field only. Neither runs a primary in a consent or `danger`
  flow: there Enter activates only the focused button. A disabled primary takes a tap and shows its
  reason.
- At most one accent region, counting toggles in the on state (they do not use accent).

**Keyboard and focus**

- Focus is trapped (Tab and Shift+Tab wrap); the page behind is inert and hidden from readers.
- First focus: the first empty field, else the first body control, never Close. In a pushed view:
  its first control. Touch input raises no IME on open unless the last input was the keyboard.
- Tab order: Back, tabs, body, foot (stacked: top to bottom). Arrows move in a tab row and a grid.
- Focus returns to: the Menu's opener after a Menu item opened it; the previously focused element
  after a shortcut; the library heading after a drop; the pushing row after a pushed view pops; the
  opener of a Dialog when it closes; the opener after a queued open.
- Roles: ARIA `dialog`, `aria-modal`, labelled by the title; tabs use `tablist`. The handle is
  decorative and has a screen reader dismiss action; Close and Escape remain. Compose
  `ModalBottomSheet` (compact) and `Dialog` (large). SwiftUI `.sheet` with `presentationDetents`.
  The Windows shell uses its one overlay host; a Sheet and its Dialog are both drawn in it.

**Software keyboard**

- The Sheet resizes to the window minus the IME inset and never slides under the keyboard. The
  compact layout under pressure (section 3) applies. The focused field scrolls into view with
  `space/lg` clearance; instant under reduced motion. The preview collapses while the IME is up.
- Closing the keyboard restores the layout over `motion/duration/base`.

**Pointer and pen**: targets at least `size/hit/min`; scribble into a field where it allows it. Do
not drag the handle with Scribble active in a field.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `elevation/sheet`, `border/strong` (more contrast) | container |
| `scrim/default`, `z/scrim`, `z/sheet` | scrim and layer |
| `radius/container` (top corners on compact) | container |
| `size/sheet/header`, `size/sheet/width/default`, `size/sheet/width/wide`, `size/sheet/list` | geometry (see README, Tokens to add) |
| `layout/gutter/*`, `space/lg`, `space/xl`, `space/md`, `space/4xl` | spacing |
| `border/subtle` | header and foot hairlines when the body scrolls beneath |
| `type/heading`, `type/label`, `type/caption`, `text/default`, `text/muted` | text |
| `motion/duration/pop`, `base`, `fast` | transitions |
| `size/control/default`, `size/hit/min`, `component/focus/*` | foot, targets, ring |

## 11. Do and Don't

- Do push a view for a second picker or Get more, with Back in its header.
- Do keep the foot to Cancel, one primary and one secondary at most.
- Do compare dirty state with the opening value.
- Don't open a Sheet from a Sheet, or stack a second Sheet for a confirm; use a Dialog.
- Don't dismiss by scrim tap once anything changed, or from a palm.
- Don't resize the container when a view is pushed.
- Don't use more than five tabs.

## 12. Traces to

Flows 01, 02, 04, 05, 07, 08, 09, 13, 14. Replaces `Sheet`; absorbs QuickSheet, MoveToSheet,
RemoveSheet, ExportSheet, PackBuilderSheet and CatalogueView as recipes; ConsentView is
ImportReviewSheet kind `extension`.
