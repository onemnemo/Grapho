# Actions

The most reused controls in Grapho. Every other family copies the heights, label style, icon sizes
and state recipe set here, so when a number below changes, it changes for the whole app.

## Members

| Component | File | Variants | Sizes | Flows |
|---|---|---|---|---|
| Button | [button.md](button.md) | `primary`, `secondary`, `text`, `danger` | `small`, `default`, `large` | 01, 07, 08, 09, 10, 14, 15 |
| IconButton | [icon-button.md](icon-button.md) | `quiet`, `solid`, `glass` | `small`, `default`, `large` | 05, 07, 11, 15 |
| Toggle | [toggle.md](toggle.md) | none (one style) | one, set by density | 01, 03, 07, 08, 09, 12, 14, 15 |
| SegmentedControl | [segmented-control.md](segmented-control.md) | none | `small`, `default` | 04, 07, 12, 14 |
| Chip | [chip.md](chip.md) | none (filter) | `small`, `default` | 05, 06 |
| Token | [token.md](token.md) | none | `small`, `default` | 05, 06 |

Shared parts and recipes that belong to this family and are defined once, here: the focus ring,
the hit slot, the Dot, the loading recipe and the one primary rule. Component files point here.

## How they line up

Heights are roles from `size/control/*`. Touch and compact values are in `tokens/README.md`; specs
never write a number.

Token matches Chip in every row except the end, where its remove slot is `size/hit/min` wide.

| Property | Button | IconButton | Chip | SegmentedControl | Toggle |
|---|---|---|---|---|---|
| Height, small | `size/control/small` | `size/control/small` | `size/control/small` | `size/control/small` | n/a |
| Height, default | `size/control/default` | `size/control/default` | `size/control/default` | `size/control/default` | track `size/switch/height` |
| Height, large | `size/control/large` | `size/control/large` | n/a | n/a | n/a |
| Width | content, at least `size/hit/min` | equals height | content | equal parts, from the widest label | `size/switch/width` |
| Radius | `radius/pill` | `radius/pill` | `radius/pill` | `radius/pill` (track and thumb) | `radius/pill` |
| Side padding, small | `space/md` | none | `space/md` | `space/md` | n/a |
| Side padding, default | `space/lg` | none | `space/lg` | `space/lg` | n/a |
| Side padding, large | `space/xl` | none | n/a | n/a | n/a |
| Icon size | role icon: small, default, large | role icon | `size/icon/small` for the check | `size/icon/small` in small, `default` in default | none |
| Icon to label gap | `space/sm` | n/a | `space/sm` | `space/sm` | n/a |
| Label style | `type/label` | no label | `type/label` | `type/label` | `type/label` in the row, not the switch |
| Hit slot | `size/hit/min` both ways | `size/hit/min` both ways | `size/hit/min` high | each segment `size/hit/min` high | `size/hit/min` both ways |

Rules that hold for all five:

- Icon sizes follow the control role: `size/icon/small` in small, `default` in default, `large` in
  large. The icon is `currentColor`. The only icon colour change is a toggled-on IconButton, which
  uses `action/toggle/fg-on` on `selection/fill`.
- Siblings in one row share a role. Mixing a small and a default control in one row is a lint error.
- A visual smaller than the slot is centred in it. Slots never overlap, so the default gap between
  adjacent actions is `space/sm`.
- Labels are `type/label` only. Weight and size never carry meaning; the variant does.
- No scale or bounce on press (motion never overshoots). Colour changes run over
  `motion/duration/fast`, a press over `motion/duration/press`. Under reduced motion all are 0.
- Text at 200% grows the control and never clips. Heights are minimums.
- Selection is fill plus indicator (`selection/fill` with `selection/indicator`), never fill alone.
  The indicator is a check (Chip) or a short bar (IconButton, SegmentedControl), not an outline.

## The focus ring

One recipe, from the `component/focus/*` tokens, never overridden per component. Two tones: a
`component/focus/gap/width` gap in `component/focus/gap/color` (set to the surface the control sits
on), then a `component/focus/ring/width` ring in `component/focus/ring/color`. It follows the
control's shape (a pill for all five), is drawn outside the visual and may extend beyond the hit
slot (4 px past the visual), which is allowed because neighbours are at least `space/sm` apart. It
uses an inset form inside clipped lists. It shows for keyboard, switch and screen reader focus, not
for a pointer or touch press. With more contrast on, edges step up through the token overrides;
under forced colours the ring maps to the system focus colour.

## The hit slot

Every control reserves a layout slot of at least `size/hit/min` (44 touch, 24 compact, 48 on
Android) in both directions, with the visual centred in it. Adjacent slots do not overlap. A small
Button or a Toggle is small to look at and full size to hit. Per shell: Compose
`minimumInteractiveComponentSize()`, SwiftUI `.frame(minWidth:minHeight:)` then `.contentShape`,
WinUI `MinWidth` and `MinHeight` on a transparent root, CSS `min-inline-size` and `min-block-size`.

## The Dot

A Dot is a state of IconButton (and ListRow), not a component. It marks "updates available" on the
Packs and Extensions entries only (SYSTEM-MAP 4.3). It is the `attention` value `dot` on the
IconButton, defined in [icon-button.md](icon-button.md).

## The loading recipe

This is the inline busy pattern, one of the three loading patterns (skeleton, progress item, inline
busy). Used by Button and IconButton only. A Toggle, Chip or SegmentedControl moves at once and
snaps back with an inline error if the core refuses.

1. The control keeps its exact size. The label and icon stay in layout but are hidden from view, so
   the width cannot change.
2. A spinner, drawn at the role icon size and in the label colour, is centred over them.
3. It appears only after 300 ms of waiting, so quick actions never flash. Once shown it stays at
   least `motion/duration/slow`.
4. The fill stays the variant's rest fill. Loading does not use the disabled look.
5. Presses are ignored, focus stays, the accessible name is unchanged and a busy state is exposed
   (`aria-busy`; WinUI `AutomationProperties` busy via `ItemStatus` "Working"; Apple
   `updatesFrequently`; Android `stateDescription` "Working"). A screen reader hears "Working" once.
6. A job past 1000 ms becomes a progress item with Cancel and the control returns to rest. Reduced motion draws a static three dot glyph instead of a spinner.

## The reason hint

A disabled control that has a reason (Button, IconButton, Toggle, one SegmentedControl segment, Chip)
still takes a tap, press or Enter, and shows the reason once as a small hint at the control (the
Tooltip surface, held for 3 seconds or until focus leaves), for example "Name the notebook first". It
is also the accessible description. Nothing runs.

## Commands, tooltips and names

- A control that runs an app action names its command id from `system/registries.md` and reads its
  label, icon, shortcut and enabled state from the registry. Button and IconButton carry `command`.
- A control that edits a value (Toggle, SegmentedControl, Chip, Token) binds to a setting id or a
  filter id, not a command. A control that only dismisses or commits its own surface (Cancel in a
  Sheet, a Close) is a surface action. All three still get an accessible name.
- Every icon-only control has a Tooltip with its name and shortcut, with no exceptions: Close, More,
  clear, reveal, stepper buttons, dismiss and Token remove included. Content is
  `{name, shortcut, source}` read at show time (effective chord after remapping, for this context).

**Commands to register** (not yet in the registry): `bar.more` (More, Note bar), `selection.more`
(More, Selection bar), `library.restore`, `library.deleteForever`, `onboarding.next`,
`onboarding.skip`.

## Extension UI node to component

| Schema node | Component | Gap |
|---|---|---|
| `button` | Button | `quiet` is `text`; `danger` renders as `secondary` |
| `toggle` | Toggle | none |
| `select`, `segmented`, `radio list`, `checkbox` | Select, SegmentedControl, RadioList, Checkbox | not in the schema yet |
| Chip, Token, IconButton | none | not reachable by extensions; an IconButton comes only from a command in a bar slot |

## One primary per surface

A **surface** is one instance of Sheet, Dialog, Banner, Side panel, Popover, Menu, Note bar,
Selection bar, Top bar, a first run step, or an EmptyState. A surface shows at most one `primary`
or `danger` Button at a time, counted among what is visible, not what exists. Stacked surfaces each
own one; the topmost owns focus.

- The primary is the action that finishes the surface's job. It sits at the end edge of the foot,
  or last in reading order. Everything else is `secondary` or `text`.
- A Dialog that confirms an irreversible action uses `danger` instead of `primary`, never both.
- Count per screen, not per component: one accent region at most per surface, counting primary
  Buttons. An EmptyState primary suppresses the Top bar `New`, which becomes `secondary`. A Banner
  action is `secondary` or `text`, never `primary`. Toggles do not use the accent. The lint runs
  on the composed screen tree.
- A surface of choices only (a Menu, a Popover with a checklist) has no primary.
- Bars (Note bar, Selection bar, Dock) hold IconButtons and no primary. The Top bar may hold one
  `primary` at `default` size: New.
- `large` is for the one primary of a first run step or an EmptyState, and never inside a bar.
  Sheet and Dialog feet are `default` throughout, and a `text` Button in a foot takes the foot's size.
- Cancel is `text` in a Sheet and `secondary` in a Dialog. A destructive Dialog takes default focus on
  Cancel; a non-destructive Dialog and a Sheet on the primary.
- Two buttons that do not fit side by side stack, primary on top, both full width, before any label
  wraps.

## Decisions

1. Heights are roles `small`, `default`, `large`. The `xs`, `sm`, `md`, `lg` and 48 px `bar` sizes
   are not used; bars use `small` or `default` by density.
2. All five are pills (`radius/pill`), and so is the focus ring.
3. Danger is the tinted variant (`state/danger/bg`, `state/danger/fg`) with a solid `border/error`
   edge so it outweighs Cancel, and `state/danger/bg-pressed` for the pressed state.
4. IconButton has a third variant `glass` for round buttons over artwork, because `media/glass` is
   the only legal host for `media/text`.
5. Loading keeps width by keeping the label in layout under a centred spinner, after a delay.
6. Toggle uses `switch/track-off`, `track-on`, `thumb-off`, `thumb-on`. On is a tinted track, not the
   accent.
7. CloseButton is IconButton with the `close` icon. There is no HoldButton. RemovableChip is Token,
   a sixth component.
8. A Button never has a trailing icon, except a chevron on a menu trigger and an external link arrow.
9. One primary per surface is a lint rule on the composed tree, with the Top bar's New as the only
   primary allowed in a bar.
10. Selection indicator is a check or a short bar, not an outline, which keeps the soft fill of the
    brand rules and still meets fill plus indicator.
11. SegmentedControl measures at runtime and becomes a Select when labels do not fit.
12. Not yet defined: `size/dot`, a glass pressed token, a tag colour set, and the extension schema nodes for
    select, segmented, radio list and checkbox.
