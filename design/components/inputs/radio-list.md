# RadioList

## 1. Purpose, and when not to use

**Purpose.** Choose exactly one of a few values that need full labels or a description and should all be visible: export format, a channel with a cost. Values come from the core's enums.

**When not to use.** Up to 3 short options: SegmentedControl. More than 8, or pack content, or options with pictures: Select or the Picker. Actions: Button or Menu. Large illustrated cards: ChoiceCard.

## 2. Anatomy

```
 Export as                              (group label row)
 ( ) PDF
 (o) Images
     One image per page.
 ( ) Grapho file
     Not available offline.             (reason on a disabled row)
 (alert_circle) Choose one.             (group message row)
```

Parts: `radio` circle (`size/checkbox`), `label`, optional `description`, optional `lead` slot (reserved, for a swatch or sample), group label row, group message row.

## 3. Variants and sizes

- `density`: `plain` | `described`. A group does not mix them, except that **a disabled row may add a reason line in any density**.
- `commit`: `on-press` (default: arrows move focus, Space or Enter selects) | `on-move` (arrows select, for groups whose options cost nothing, such as a preview-only choice).
- Row height `size/row/list` minimum; the circle is `size/checkbox`.
- An "Other" row is a Select plus a TextField, not a RadioList feature.

## 4. States

| State | Tokens that change |
|---|---|
| Rest | circle `surface/control`, edge `border/control`, `border-width/emphasis` |
| Hover | row `hover/fill`; circle edge `border/control-hover` |
| Focus-visible | inset ring around the row |
| Active | row `press/fill` |
| Selected | circle edge and dot in `selection/indicator`. **No row fill, no bar**: the dot is the cue, with the label weight kept the same. The dot on `surface/control` needs a declared pair (README) |
| Selected, hover | `hover/fill` row, dot kept |
| Disabled (option) | `state/disabled/fg`; reason line below; focusable so the reason is announced |
| Disabled (group) | all rows disabled |
| Loading, empty, drag | not used |
| Error | group message row: `alert_circle` and text in `state/danger/fg` |

Forced colours: the dot uses the system highlight; the circle edge the system edge.

## 5. Content rules

- Labels wrap to three lines, descriptions to two; the circle aligns with the first line.
- No selection: allowed before the first choice in a required group; otherwise one option is preselected.
- 1 item: do not use. 500 items: not supported.

## 6. Overflow rule

Up to 8 options. More becomes Select. In a Sheet the group scrolls with the Sheet.

## 7. Extension slots

None. There is no `radio list` schema node yet; an extension supplies its own values.

## 8. Pack content

None. RadioList never shows pack state or pack-fed options.

## 9. Input

- Touch and stylus: tap the row. Mouse: click.
- Keyboard: one tab stop for the group; Up and Down (and Left and Right) move focus; with `on-press` Space or Enter selects, with `on-move` focus and selection move together. Disabled options stay reachable by arrows so their reason is read. Tab leaves the group.
- A selection that has a cost (sync, deleting, a mode change that rewrites data) uses `on-press`, so keyboard traversal never triggers it.
- ARIA: `radiogroup` with label; rows `radio` with `aria-checked`; descriptions by `aria-describedby`. Native: Compose `selectableGroup()` with `Role.RadioButton`; WinUI `RadioButtons`, custom-templated; SwiftUI `Picker` inline, custom-styled.

## 10. Tokens

`size/row/list`, `size/hit/touch`, `size/hit/min`, `size/checkbox`, `radius/pill`, `border-width/emphasis`, `surface/control`, `hover/fill`, `press/fill`, `border/control`, `border/control-hover`, `selection/indicator`, `text/default`, `text/muted`, `type/label`, `type/caption`, `state/disabled/fg`, `state/danger/fg`, `focus/gap/*`, `focus/ring/*`.

## 11. Do and Don't

- Do mark selection with the dot alone; keep fills for hover.
- Do give a disabled option a reason line.
- Don't select on arrow keys when the choice has a cost.
- Don't dress a row like a multi-selected item.

## 12. Traces to

Flows 12, 13, 14. Replaces Radio and RadioGroup.
