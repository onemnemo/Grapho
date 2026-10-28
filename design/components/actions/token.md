# Token

## 1. Purpose

A chosen value shown as a pill that the user can take away: a selected tag in a tag field, a folder
in a path filter. It was RemovableChip; it is a separate component because it is not a toggle and its
anatomy, keyboard and growth differ from Chip.

**When not to use:**
- A filter that turns on and off: use Chip.
- A value that is edited as text: use TextField.
- A status or label: write text.

## 2. Anatomy

```
 (  maths  [x] )      label, then the remove slot at the end
              ^ remove: its own IconButton-like target, full chip height, size/hit/min wide
```

Parts: container, label, remove slot with Tooltip.

## 3. Variants and sizes

One style. `size`: `small` | `default` (`size/control/small`, `size/control/default`). Start padding
`space/md`; the remove slot ends the pill. No dot, no icon, no count (tags carry no colour in
chrome; a tag colour set with matrix pairs is a gap). The Token body is not interactive. The
remove slot is the only target, `size/hit/min` wide and the full height, with its x at the role icon
size.

Command: removing is a value edit, not a command, so there is no command id. The remove slot has a
Tooltip named "Remove maths" with no shortcut, and the same accessible name.

## 4. States

| State | Fill | Edge | Content |
|---|---|---|---|
| Default | `surface/control` | `border/control` | `text/default` label; x `text/muted` |
| Hover (remove slot) | `action/quiet/bg-hover` inside the slot, clipped by the pill | same | x `text/default` |
| Focus-visible | same | README focus ring on the remove slot, or on the Token when it is the focus stop | same |
| Active (pressed) | `action/quiet/bg-pressed` in the slot | same | same |
| Selected | n/a | | |
| Disabled | `state/disabled/bg` | `state/disabled/border` | `state/disabled/fg`; no remove slot action |
| Loading | n/a | | |
| Empty | n/a. A token always has a name | | |
| Error | `border/error` solid edge, message in the field | | |
| Drag | In a reorderable field `state/drag/source-opacity` while dragged; drop position `state/drag/insertion` | | |

## 5. Content rules

- Label: the value as the user typed it. Long text is cut with a middle ellipsis at the container
  width, so the end of a long tag shows; the full text is the accessible name and the Tooltip.
- No text: not allowed. One item: fine. 500 items: a field shows the first few and a `text` Button
  "+ 497" at the Token's size, which opens the full list in a Popover.

## 6. Overflow rule

Tokens wrap to the next line inside their field. Above about ten, the field collapses to the first
few plus the "+ n" Button. A field never scrolls sideways.

## 7. Extension slots

None.

## 8. Pack content

None. A token for a pack-owned tag or folder renders like any other; documents never show pack state.

## 9. Input

- **Stylus and touch:** tap the remove slot to remove; the rest of the pill does nothing. Reorder, if
  the field allows it, has a drag and the alternative: Move earlier and Move later in the focused
  token's context Menu.
- **Mouse:** hover fills the remove slot only.
- **Keyboard:** Left and Right (Up and Down in wrapped rows) move among tokens; Backspace or Delete
  removes the focused token and moves focus to the next, or to the field; Home and End.

| Platform | Role |
|---|---|
| Web | `listitem` in a `list` with a nested `button` named "Remove maths" |
| Compose | Row with a clickable remove `IconButton`, `Role.Button` |
| WinUI | `Border` with an inner `Button` named for removal |
| SwiftUI | `HStack` with a `Button` and `accessibilityLabel` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `size/hit/min` | Height, remove slot |
| `radius/pill` | Container |
| `space/md`, `sm` | Padding, gap |
| `type/label` | Label |
| `size/icon/small`, `size/icon/default` | Remove x |
| `surface/control`, `border/control` | Rest |
| `action/quiet/bg-hover`, `action/quiet/bg-pressed` | Remove slot |
| `text/default`, `text/muted` | Label, x |
| `border/error` | Error |
| `state/disabled/bg`, `fg`, `border` | Disabled |
| `state/drag/source-opacity`, `state/drag/insertion` | Reorder |
| `component/focus/*` | Focus ring |

## 11. Do and Don't

**Do**
- Make the remove slot a full height target with its own Tooltip.
- Cut long tags in the middle.

**Don't**
- Don't make the body of the Token tappable.
- Don't colour a Token with a tag colour until the tokens and pairs exist.

## 12. Traces to

Flows: 05 organize-folders (tags in a field), 06 find-something (applied tag filters).

Replaces: RemovableChip (the `token` variant of the earlier Chip).
