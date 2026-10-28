# TextArea

## 1. Purpose, and when not to use

**Purpose.** Several lines of plain text: a pack description, a note on an item. Renders in the FieldShell ([field-shell.md](field-shell.md)).

**When not to use.** One line: TextField. Writing on a page: the canvas. Ink kept as ink, or formatted text: out of scope for this component (a future block type on the canvas, not a field). A long document: not a field.

## 2. Anatomy

```
 Description
 +-----------------------------------------------+
 | Notes for the class pack, shown in the        |
 | catalogue.                                    |
 |                                          .:   |  <- grip (mouse only, optional)
 +-----------------------------------------------+
 Up to 400 characters.                    120/400
```

## 3. Variants and sizes

- `rows`: the minimum visible lines, `3` | `5`. `maxRows`: the cap, `5` | `8` | `12`. The box starts at `rows`, grows with content to `maxRows`, then scrolls inside. `rows` never exceeds `maxRows`.
- Minimum height is the taller of `rows` lines and `size/control/default`; there is no `small`.
- `resize`: `none` | `grip`. `grip` shows a resize handle for a mouse only.

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Focus-visible | Same ring |
| Active | Soft keyboard Enter is a new line |
| Error | Message row; the counter keeps `text/muted` (no negative count) |
| Read-only | Dashed edge; selectable; scrolls |
| Scroll thumb and grip | `border/control` on `surface/control`, a declared pair (README, Tokens to add) |

## 5. Content rules

- Long text wraps; no horizontal scroll. A word longer than the line breaks at the edge.
- No text: placeholder gives an example sentence.
- 1 item: one line of text still shows `rows` lines.
- 500 items: pasted text over the maximum is trimmed with a toast; the count is in grapheme clusters.
- Plain text only; NFC on commit.

## 6. Overflow rule

Past `maxRows` it scrolls inside. Inside a Sheet at less than 480 dp container height the box grows to its full content and the Sheet scrolls instead, so a finger is never caught in a nested scroll.

## 7. Extension slots

None. Schema `field` kind `multiline`.

## 8. Pack content

None.

## 9. Input

- Stylus: handwriting over the whole box (FieldShell 9.4). Pen down inside the box writes into the field.
- Touch: keyboard Enter is a new line; the box scrolls above the keyboard inset.
- Mouse: click, drag-select, optional grip.
- Keyboard: Enter inserts a line; Tab leaves the field; Ctrl+Enter submits the host form; Enter never runs a Sheet's primary action.
- ARIA: `textbox` with `aria-multiline="true"`. Native: Compose `BasicTextField(singleLine = false)`; WinUI `TextBox` with `AcceptsReturn`; SwiftUI `TextEditor`, in the custom frame.

## 10. Tokens

All FieldShell tokens, plus `space/sm` (vertical padding), `border/control` (scroll thumb, grip), `type/body`.

## 11. Do and Don't

- Do show the counter whenever a maximum exists.
- Do let the Sheet scroll instead of a nested scroll on short screens.
- Don't use it for one-line values.
- Don't promise ink or formatting here.

## 12. Traces to

Flow 08. Replaces TextArea.
