# Checkbox

## 1. Purpose, and when not to use

**Purpose.** A yes or no that belongs to a set the user ticks and then applies or reads: tags on a selection, filter lists, items to include. It has a mixed state.

**When not to use.** A setting that applies at once: Toggle. An exclusive choice: RadioList or SegmentedControl. Selecting rows in a list: the ListRow selection slot. A tree of nested folders: a separate component, not this one. A filter that is a chip: Chip.

## 2. Anatomy

```
 Tags                                   (group label row)
 [x] Biology
     Used on 12 notes                   (description)
 [-] Chemistry
 (alert_circle) Choose at least one.    (group message row)
```

Parts: `box` (`size/checkbox`), `mark` (`check` or `minus`), `label`, optional `description`. A group has the FieldShell label row and message row. The box sits in a slot of `size/hit/touch` (touch) or `size/hit/min` (compact).

## 3. Variants and sizes

- One look. `value` is `unchecked` | `checked` | `mixed` (state, not variant).
- `label`: `end` | `none` (inside a ListRow that carries the text).
- Size is the role token `size/checkbox`.

## 4. States

| State | Tokens that change |
|---|---|
| Rest | box `surface/control`, edge `border/control`, `border-width/emphasis` |
| Hover | `surface/control-hover`, edge `border/control-hover` |
| Focus-visible | ring around box and label |
| Active (unchecked) | `press/fill` |
| Selected (checked or mixed) | `selection/fill` with `selection/indicator` mark and edge; the mark is always drawn |
| Selected, hover | `selection/fill-hover` |
| Selected, pressed | `selection/fill-hover` with the mark kept; no switch back to `press/fill` |
| Disabled | `state/disabled/bg`, `border`, `fg`; a checked box keeps its mark; the reason is the description |
| Loading, empty | not used |
| Error | group message row: `alert_circle` and text in `state/danger/fg`; the box edge `border/error` |
| Drag | not applicable |

## 5. Content rules

- Labels wrap to the first-line alignment; never truncated.
- 1 item: reads as an option; the label says what ticking does.
- 500 items: a list with Filter above and a parent Checkbox. **Select all acts on the model query, not the rendered rows** (SYSTEM-MAP 4.7: by ids, range included). The parent's label is constant ("Select all"); the count ("12 shown") is in its description, with a plural key per language, and is announced after the filter debounce, not on every key.

## 6. Overflow rule

Groups longer than 8 get Filter and scroll; they never split across columns.

## 7. Extension slots

None. There is no `checkbox` schema node yet.

## 8. Pack content

None. A Checkbox list shows only what the host list holds; pack state belongs to the picker.

## 9. Input

- Touch and stylus: tap toggles; the label is part of the target. A mixed box moves to checked.
- Mouse: click; Shift+click ticks a range of the model query.
- Keyboard: Space toggles; Shift+Space ticks the range from the last tick; Shift plus Arrow moves and ticks; Tab between boxes.
- Mixed cycle: mixed, checked, unchecked, checked.
- ARIA: `checkbox` with `aria-checked="true|false|mixed"`; a group is `group` with the label row and `aria-describedby` for its message. Native: Compose `TriStateCheckbox`; WinUI `CheckBox` with `IsThreeState`; SwiftUI toggle with a custom style.

## 10. Tokens

`size/checkbox`, `size/hit/touch`, `size/hit/min`, `radius/key`, `border-width/emphasis`, `surface/control`, `surface/control-hover`, `press/fill`, `border/control`, `border/control-hover`, `border/error`, `selection/fill`, `selection/fill-hover`, `selection/indicator`, `text/default`, `text/muted`, `type/label`, `type/caption`, `state/disabled/bg`, `border`, `fg`, `state/danger/fg`, `space/sm`, `size/icon/small`, `focus/gap/*`, `focus/ring/*`.

## 11. Do and Don't

- Do draw the mark in every checked state.
- Do act on the model query for Select all.
- Don't use it for a setting that applies at once.
- Don't change the parent label with the filter text.

## 12. Traces to

Flows 05, 06, 08. Replaces Checkbox.
