# Chip

## 1. Purpose

A small pill that toggles one filter on or off: Notebooks 12, Starred, a tag in the filter list, a
source in the picker's source filter. A chosen value the user can remove is a Token, not a Chip
(see [token.md](token.md)).

**When not to use:**
- A command: use Button.
- One setting on or off: use Toggle.
- A choice among two or three options for one setting: use SegmentedControl.
- A removable chosen value: use Token.
- A status or label with no action: write text. Status chips as decoration are not allowed.

## 2. Anatomy

```
 off:  (  Notebooks  12 )
 on:   (  v  Notebooks  12 )        v = check, the selection indicator
```

Parts: container, check (when on), label, count (optional).

## 3. Variants and sizes

One style. `size`: `small` | `default`, resolving to `size/control/small` and `size/control/default`.
Chips in a row (result filters, source filter) are `small`, so a filter row does not read as a row of
buttons. `default` is for a lone chip inside a form or panel body.

Padding: `space/md` small, `space/lg` default, at both ends. The check's width is reserved at rest on
the start side and the same width is added at the end, so the label sits centred and toggling never
changes the chip's width or moves its neighbours. A chip has a label, an optional count, and the
check when on. No icon, no dot. The only chevron is on the `menu` variant below.

### Variant `menu`

A filter Chip whose tap opens a Popover (Type, Date, Folder, Tag in search). It has a trailing
chevron (`chevron_down`), reserved at rest so the width never changes. It shows the chosen value or
count in its label ("Date: This month") and is `selected` while a filter is active, with the same
fill plus check. A tap opens the Popover anchored to the chip; the Popover applies at once with no
Done. Escape closes it and returns focus to the chip. Its role is a menu button
(`aria-haspopup`, `aria-expanded`). A removable `menu` chip shows the Token remove slot only when
the filter is preset (for example the Folder chip in a folder search).

A chip binds to a filter id in the search or picker model (for example `search.filter.starred`), or
to a source id. It is a value control, not a command, so it has no command id.

## 4. States

| State | Fill | Edge | Content |
|---|---|---|---|
| Default (off) | `surface/control` | `border/control` | `text/default` label; count `text/muted` |
| Hover | `surface/control-hover` | `border/control-hover` | same |
| Focus-visible | same | README focus ring | same |
| Active (pressed) | `surface/control-hover` | `border/control-hover` | same |
| Selected (on) | `selection/fill`, hover `selection/fill-hover` | none | `text/default`, check in `selection/indicator` leading; count `text/default` |
| Disabled | `state/disabled/bg` | `state/disabled/border` | `state/disabled/fg`; the reason is the description and the reason hint on a tap |
| Loading | n/a. The count is omitted until known; the chip works at once | | |
| Empty | n/a. A chip always has a label. A filter with zero results stays enabled and shows 0 | | |
| Error | n/a | | |
| Drag | n/a. Chips are not draggable and not drop targets | | |

`menu` variant states: Default and Hover as above, with the chevron in `text/muted`; **Open**: the
Popover is shown, the chip uses `surface/control-hover`, the chevron points up and `aria-expanded` is
true; **Selected (filter active)**: `selection/fill` with the check and the value in the label, the
chevron kept; **Disabled**: as above with the reason; Loading, Empty, Error and Drag as the plain chip.

No `text/subtle` on any chip fill. The check is the indicator, so selection is fill plus check and
never fill alone.

## 5. Content rules

- Label: one or two words in sentence case ("Notebooks", "Starred"). The only glyph is the check.
- Very long text (German, Norwegian, a user's tag): the maximum width is the container width. Cut
  with an end ellipsis for a name, with a middle ellipsis for a tag, so the distinguishing end shows.
  It never wraps. The full text is the accessible name and the Tooltip. The count never truncates.
- No text: not allowed.
- One item: a Chip needs company. A lone filter is a Toggle; Starred sits in a row with the type
  filters.
- Five hundred items: see overflow. A tag filter with many tags uses the tag list in a Popover.

## 6. Overflow rule

One rule for every count. A row wraps to at most two lines. If chips still do not fit, the last
position is a `More` chip (same size, label "More 5") that opens a list of the rest as a checklist
in a Popover. A row never scrolls sideways, never swaps its control type when a source is added, and
never hides a chip behind an unlabelled gesture. The "More" chip is a Chip, off at rest, on when a
filter inside it is on.

## 7. Extension slots

Content source and Insert source (SYSTEM-MAP 7): an extension's source is a Chip in the source filter,
like a pack, with the extension's name as its label and "from Extension name" in the Tooltip.
Extensions never set the size or add a Chip elsewhere. The extension schema has no chip node.

## 8. Pack content

A Chip shows pack state only in the picker source filter. A pack is a source Chip; an Off pack is
not offered, its items leave the picker, and the **Hidden** entry is an ordinary Chip with the word
Hidden. Kept copies show as **Kept from <pack>** in the list, not on a Chip. Documents never show
pack state. A pack the user does not have has no Chip.

## 9. Input

- **Stylus:** tap toggles on lift; hover shows the hover fill. Never reacts while the pen writes.
- **Touch:** tap toggles. Slot at least `size/hit/min` high; spacing between chips at least `space/sm`.
- **Mouse:** click on release; hover fill.
- **Keyboard:** Tab reaches the row as one stop; Left and Right move among chips (Up and Down in a
  wrapped row); Space and Enter toggle. The Tooltip shows on keyboard focus for a cut label.

| Platform | Role |
|---|---|
| Web | `button` with `aria-pressed` (a filter row is a `group` with a name) |
| Compose | Custom pill with `toggleable` and `Role.Button` |
| WinUI | `ToggleButton`, restyled |
| SwiftUI | `Toggle` with a custom `ToggleStyle` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default` | Height |
| `size/hit/min` | Hit slot |
| `radius/pill` | Container |
| `space/lg`, `md`, `sm` | Padding, gap |
| `type/label` | Label and count |
| `size/icon/small`, `size/icon/default` | Check by size |
| `surface/control`, `surface/control-hover` | Rest, hover and pressed |
| `border/control`, `border/control-hover` | Edge |
| `selection/fill`, `selection/fill-hover`, `selection/indicator` | On |
| `text/default`, `text/muted` | Label, count |
| `state/disabled/bg`, `fg`, `border` | Disabled |
| `component/focus/*` | Focus ring |

## 11. Do and Don't

**Do**
- Reserve the check's width so toggling never moves the chip or its neighbours.
- Fold the count into the chip it describes ("Notebooks 12").
- Wrap to two lines, then use "More", at any count.
- Use `small` in rows.

**Don't**
- Don't use a Chip as a decorative status or badge.
- Don't add an icon, a dot or a chevron to a chip.
- Don't wrap the label or shrink the type.
- Don't use hover or pressed `hover/fill` on a chip; only the control fills apply.

## 12. Traces to

Flows: 05 organize-folders (tag and folder filters), 06 find-something (type filters with counts,
Starred, source filter).

Replaces: Chip and the Android private Chip. RemovableChip becomes Token.
