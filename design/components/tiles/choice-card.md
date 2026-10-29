# ChoiceCard

## 1. Purpose

Two or three large, illustrated choices where the choice is the screen: in first run, writing hand
(Left, Right) and a similar setup choice. One card applies one value.

**When not to use:**
- A visual set of many items: use [PickerTile](picker-tile.md).
- Text options in a form: use RadioList or SegmentedControl.
- A command: use Button.

## 2. Anatomy

```
 +------------------+   +------------------+
 |   illustration   |   |   illustration   |
 |                  |   |                  |
 | Left hand        |   | Right hand       |
 | Menus open right |   | Menus open left  |
 +------------------+   +------------------+
```

Parts: card, illustration (art prop, may use the mascot in first run), title, description, check.

## 3. Variants and sizes

One style. Two or three cards in a group; a fourth is a lint error. Cards share a height (the tallest)
and a width: equal columns in a row on medium and wider, stacked on compact. Minimum height is three
times `size/hit/min`. Padding `space/lg`, gap `space/md`, radius `radius/surface`. Card text is
`type/heading` for the title and `type/body` for the description.

## 4. States

| State | Changes |
|---|---|
| Default | `surface/control`, `border/control`; title `text/default`, description `text/muted` |
| Hover | `surface/control-hover`, `border/control-hover` |
| Focus-visible | ring |
| Active | `press/fill` |
| Selected | `selection/fill`, check in `selection/indicator` at the start corner, edge `border-width/emphasis` in `selection/indicator`; the card never shrinks or dims |
| Disabled | `state/disabled/bg`, `state/disabled/border`, `state/disabled/fg`; reason in the description |
| Loading | n/a. Choices are static |
| Empty | n/a |
| Error | n/a. A failed save shows a Toast with Retry |
| Drag | n/a |

No choice is preselected unless the system value is known (for example the device's hand setting),
and then it is selected and says "Suggested" in the description.

## 5. Content rules

- Title: one to three words. Description: one short sentence. German and Norwegian text runs longer:
  cards grow in height together; text wraps, never cuts.
- No illustration: the card is text only and keeps its height. 3 cards on compact stack in order.
- Not for more than three; a fourth option becomes a RadioList.
- Left and right hand cards stay in physical order (left card is the left hand) in right to left
  too, and their art is not mirrored.

## 6. Overflow rule

None. Over three choices, switch component.

## 7. Extension slots

None.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch:** tap selects; the card is a `size/hit/min` target in both dimensions.
- **Mouse:** click; hover. **Keyboard:** one Tab stop; arrows move focus only;
  Space or Enter selects, so menus do not flip while the user browses.
- **Roles:** `radiogroup` of `radio`s (web), selectable group on Compose (`Modifier.selectableGroup`),
  WinUI `RadioButtons`, SwiftUI `Picker` with `.radioGroup`-like cards. The group has a label from
  the screen title.

## 10. Tokens

| Token | Use |
|---|---|
| `size/hit/min`, `space/md`, `space/lg`, `radius/surface` | geometry |
| `type/heading`, `type/body`, `text/default`, `text/muted` | text |
| `surface/control`, `surface/control-hover`, `border/control`, `border/control-hover`, `border-width/emphasis` | rest, hover |
| `selection/fill`, `selection/indicator`, `press/fill` | states |
| `state/disabled/bg`, `state/disabled/border`, `state/disabled/fg` | disabled |

## 11. Do and Don't

- Do show selection as fill, check and edge together.
- Do give every card a title and a description.
- Don't use it for more than three options.
- Don't put the mascot in a card outside first run.

## 12. Traces to

Flow 01.
