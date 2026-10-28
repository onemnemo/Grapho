# Stepper

## 1. Purpose, and when not to use

**Purpose.** Pick a small whole number with minus and plus: the number of quick slots shown, 0 to 5. It is a bordered group of two quiet IconButtons around a value cell. It stays a component because the count is a state to press, not a position to drag, and because it names its limits ("0 to 5").

**When not to use.** Fine or continuous values: Slider. A typed value: TextField `numeric`. Three or fewer named options: SegmentedControl. More than about 100 steps: Slider or TextField.

## 2. Anatomy

```
 Quick slots                                (label row)
 [ minus ] 3 [ plus ]
 Up to 5.                                   (message row)
```

Parts: label (the shared label row), `minus` (IconButton `minus`, quiet), `value` cell, `plus` (IconButton `plus`, quiet), message row.

## 3. Variants and sizes

- `size`: `small` | `default` (small resolves to default at touch density). Each button is a square of that height; its hit slot is `size/hit/touch` at touch density (an invisible extension beyond the visual, with 8 dp between targets) and `size/hit/min` at compact.
- `layout`: `inline` (settings row) | `stacked`.

## 4. States

| State | Tokens that change |
|---|---|
| Rest | box `surface/control`, edge `border/control`, dividers `border/control` at `border-width/hairline`, value `text/default` |
| Hover | the hovered button `surface/control-hover` |
| Focus-visible | one ring around the group on the value cell |
| Active (pressed) | button `press/fill` |
| Selected | not applicable |
| Disabled (whole) | `state/disabled/bg`, `border`, `fg` |
| At a limit | the minus (at min) or plus (at max) button is disabled; the reason "Up to 5." is in the message row, with the button still announced by touch exploration |
| Loading, empty | not used |
| Error | A typed value outside the range follows the numeric contract: clamped, with "Set to 5." in the message row |
| Drag | not applicable |

## 5. Content rules

- Label wraps; the group keeps its width.
- Value: one to three digits in `type/body` with tabular figures (the same as TextField values); the cell grows to fit.
- Min and max are in the accessible description ("0 to 5").

## 6. Overflow rule

Not applicable. At under 480 dp container width the label stacks above.

## 7. Extension slots

None. Extension schema node `stepper` maps to this component once it is added to the schema.

## 8. Pack content

None.

## 9. Input

- Touch and stylus: tap minus or plus; holding repeats after 400 ms at one step per 150 ms, faster after 10 steps.
- Mouse: click; hold repeats; no wheel handling.
- Keyboard: the value cell is the tab stop; Up and Right add one, Down and Left subtract one (mirrored in right-to-left); Page Up and Page Down by 5; Home and End; typed digits follow the numeric contract (FieldShell 9.3).
- **Handwriting: opted out** (FieldShell 9.4). Pen users press the buttons or use the keyboard.
- Undo: in the document or Studio, one entry on the host's stack per release or per 400 ms of keys; on a Settings page it applies at once with no undo stack.
- Buttons are not in the Tab order but stay in the accessibility tree, so touch exploration finds them. Tooltips: "Decrease quick slots", "Increase quick slots" (the label is added). These are value edits, not registry commands.
- ARIA: group of `spinbutton` (value cell) plus two `button`s. Native: custom on Compose; WinUI `NumberBox` with inline spin buttons re-templated; SwiftUI `Stepper` re-styled.

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `size/hit/touch`, `size/hit/min` | button size, slots |
| `surface/control`, `surface/control-hover`, `press/fill` | fills |
| `border/control`, `border-width/hairline` | box edge, dividers |
| `radius/item` | corner |
| `size/icon/small`, `default` | icons by box role |
| `text/default`, `text/muted`, `type/body`, `type/caption` | value, message |
| `state/disabled/bg`, `border`, `fg` | disabled |
| `focus/gap/*`, `focus/ring/*` | ring |

## 11. Do and Don't

- Do disable at a limit and say why in the message row.
- Do give each button its Tooltip.
- Don't wrap around at a limit.
- Don't use it for large ranges.

## 12. Traces to

Flows 03, 12. Replaces Stepper.
