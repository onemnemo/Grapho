# NoteBarHandle

## 1. Purpose

The small, visible and focusable control that stands for the NoteBar while it is receded. It tells
the user the bar exists and brings it back with one press. It is the recall control for pointer,
keyboard and screen reader users, who cannot use the top edge tap or wait for the timer.

**When not to use:**
- As a general "show or hide chrome" button: only the NoteBar has one.
- Anywhere the NoteBar is visible: the handle is not in the tree then.
- For the FindBar, which never recedes.

## 2. Anatomy

```
          +--------+
          |   v    |   pill, over the page, top centre; sized from its glyph
          +--------+
   hit slot: the pill expanded to at least size/hit/touch each way, no wider
```

| Part | Notes |
|---|---|
| Pill | Glyph plus `space/sm` padding all round, `radius/pill`, fill `media/glass`. Its size comes from the glyph, so the mark always fits |
| Mark | `chevron_down`, `size/icon/small`, `media/text`. Pinned to that size: it does not follow the text scale |
| Hit slot | The pill grown to at least `size/hit/touch` each way. It is a control, not the top edge zone; the zone is for finger and mouse only |

The glass fill is used because the pill sits over the page, whose pixels are unknown; `media/text`
is allowed on `media/glass`.

## 3. Variants and sizes

None. One shape; `position` follows the NoteBar:

| NoteBar position | Where the handle sits |
|---|---|
| `floating` | Top centre of the page area, where the floating bar's top edge was, `space/sm` from the top |
| `line` | Top centre of the reserved empty row, vertically centred in it |

## 4. States

| State | Look |
|---|---|
| Hidden | The NoteBar is visible. Not in the tree, not focusable |
| Default | Visible from the first frame of the recede. `media/glass`, `media/text` mark. No animation of its own |
| Hover | Pointer or pen hover shows the Tooltip after 500 ms. No fill change (glass has none) |
| Focus-visible | The README focus ring, gap colour `border/focus-gap`, drawn around the pill |
| Active | A `media/edge` ring at `border-width/emphasis` (the glass press look in IconButton) |
| Disabled, Loading, Error, Empty, Selected, Drag | n/a |

Reduced motion: it appears and disappears as an opacity change of 0 ms. While the pen is on the
page the handle is static. It never pulses, fades slowly or nudges to attract attention.

## 5. Content rules

- Name: "Show note bar", the label of `bar.show`. Tooltip "Show note bar" (the command has no default key). The accessible name
  is the same words, in the user's language, at any length.
- No visible text. In German "Notizleiste anzeigen" is only in the Tooltip, which wraps.
- One handle only. A document with the NoteBar position `line` and `floating` never shows both.

## 6. Overflow rule

None. It is one control.

## 7. Extension slots

None. Extensions cannot add to, move or hide the handle.

## 8. Pack content

None.

## 9. Input

- **Stylus:** a tap on the handle (down and up within `input/tap/duration`, under
  `input/tap/travel`) returns the bar. A pen contact that moves further or lasts longer is ink on the
  page beneath. The slot is small and is a visible control, so no inked area is taken that was not
  marked. A pen tap anywhere else at the top of the page is ink.
- **Touch:** a short tap returns the bar. The palm window applies: a finger touch within 1 s of
  pen activity is ignored.
- **Mouse:** click on release. Hover shows the Tooltip.
- **Keyboard:** F6 cycles landmarks and entering the note bar landmark returns the bar and puts
  focus on its first item (the handle is then gone). Tab onto the handle does the same. Enter and
  Space on the handle, a screen reader activation and `bar.show` also return it. Focus never moves
  on its own while the pen is writing.
- **Screen reader:** announced as a button "Show note bar". Receding and returning are not
  announced; only the handle's presence in the region is.
- **Palm:** a touch larger than the palm threshold, or a finger touch within `delay/palm-window` of pen activity, does not press it.
- **Command:** `bar.show`.

| Platform | Role |
|---|---|
| Web | `button`, `aria-label` "Show note bar", `aria-expanded="false"` |
| Compose | `Role.Button`, `contentDescription`, `onClickLabel` |
| WinUI | `Button`, `AutomationProperties.Name`, `ToolTipService` |
| SwiftUI | `Button` with `accessibilityLabel` and `.accessibilityHint` of the shortcut |

## 10. Tokens

| Token | Use |
|---|---|
| `size/hit/touch` | Hit slot |
| `media/glass`, `media/text`, `media/edge` | Fill, mark, press ring |
| `size/icon/small` | Mark (fixed, not text scaled) |
| `radius/pill`, `space/sm` | Shape, padding, offset |
| `delay/palm-window` | Palm window |
| `z/floating-bar`, `component/focus/*` | Layer, focus |

## 11. Do and Don't

**Do**
- Keep it drawn and still for the whole time the bar is receded.
- Give it a Tooltip and an accessible name, and make it the first stop for F6.
- Keep its slot inside the top edge zone, so one tap area has one meaning.

**Don't**
- Don't animate, pulse or hint with colour.
- Don't pass its press to the page, or let a stroke that started on it count as a press.
- Don't draw it while the NoteBar is shown.
- Don't make it smaller than the hit slot to keep the page clear; shrink the pill, not the slot.

## 12. Traces to

Flows: 03 write-and-switch-tools (recall of the bar, accessibility of the bar).
It is a part of the Note bar surface for SYSTEM-MAP section 3.
