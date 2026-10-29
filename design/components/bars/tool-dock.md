# ToolDock

## 1. Purpose

The always-present strip for the current tool, the quick slots, Undo and Redo. It is the one
chrome that never recedes, so the next tool or the last stroke is always one tap away, with the
pen or a finger.

**When not to use:**
- Picking or adjusting a pen, colour or width: the ToolWheel and the Tool settings tab.
- Page, document or selection commands: NoteBar, SelectionBar.
- A menu of settings: there is no dock menu. Handedness and the slot count are in Settings and
  the Tool settings tab; Previous tool is the wheel's centre and key Q.

## 2. Anatomy

```
Vertical, right-handed (left edge)     Horizontal, right-handed (bottom, left end)
+------+                               +--------------------------------------+
| Tool |  current tool, a real sample  | [Tool][S1][S2][S3][S4][S5][Undo][Redo]|
| S1*  |  slots 1..5 (* pinned)        +--------------------------------------+
| S2*  |
| S3   |  Left-handed: the same strip on the right edge, or at the right end
| S4   |  of the bottom strip. The order is the same, top to bottom or left
| Undo |  to right, and is never reversed for handedness.
| Redo |
+------+
```

| Part | Notes |
|---|---|
| Tool | A real sample of the current tool: its icon, ink colour and width mark. Tap opens the wheel beside the dock (`tool.wheel`, T). Not a slot |
| QuickSlot (part) | 0 to 5. A bundle: tool, pen, colour, width. Tap applies. A pinned slot shows a `pin` glyph at `size/icon/inline` |
| Undo, Redo | `doc.undo`, `doc.redo`. Always present, dimmed when nothing to do |
| Status (part) | 0 to 2 mode indicators after Redo, drawn only while a mode is on (recording, ruler, laser pointer). Each is an IconButton toggle that turns its mode off. Built in only; the growth rule is in the README |
| Container | `surface/raised`, `border/subtle` edge on the page side, no shadow |

Every part is an IconButton-based target at `size/dock/slot` square (glyph at `size/icon/default`, the `quiet` look), centred in its slot, gap
`space/xs`, container padding `space/sm` across and `space/nudge` along the strip.

## 3. Variants and sizes

`orientation`: `vertical` | `horizontal`. `handedness`: `right` | `left`, from `general.handedness`.

| Window | Orientation | Where |
|---|---|---|
| Expanded or medium, 500 dp tall or more | `vertical` | Edge away from the writing hand: left edge for right-handed, right edge for left-handed. Page inset by the strip |
| Compact (under 600 wide) | `horizontal`, a **corner strip** | A short strip in the bottom corner on the side away from the writing hand (bottom left for right-handed, bottom right for left-handed). It is not a full width bar: its length is at most the window width less `size/dock/slot` less `layout/gutter/compact`, so the writing-hand end stays clear of it. Page inset by its height |
| Short (under 500 tall), any width | `horizontal`, the same corner strip | Content width, in the same corner |

The strip is `size/dock/slot` plus two `space/nudge` high (56 at touch density), or the same width
vertically. Hand-held in portrait, the writing palm rests on the writing-hand side, so the strip
stays on the other. With the side panel open the dock keeps its edge and the panel sits after it on the
same side. The dock is outside the page canvas on compact. Toasts sit above the horizontal strip.

## 4. States

| State | When | Tokens and behaviour |
|---|---|---|
| Idle | Pen not touching | `action/quiet/fg` on `surface/raised`. All targets live, subject to the palm rule below |
| Hover | Mouse or pen hover (not while the pen is on the page) | `action/quiet/bg-hover`; Tooltip at 500 ms |
| Focus-visible | Keyboard | README focus ring, inset form (the strip clips) |
| Active | Held | `action/quiet/bg-pressed`, no scale. Press feedback is off for the pen while it writes |
| Selected (current slot) | A slot whose bundle equals the live tool | `selection/fill`, hover `selection/fill-hover`, icon `action/toggle/fg-on`, short bar `selection/indicator` at the strip's inner edge, `border-width/emphasis`. The Tool part always shows the live bundle and carries no selected look |
| Writing | The pen is down on the page | Same size, same place, fully visible. Nothing animates and nothing blinks or moves; hover and press feedback are off for the pen. Taps by pen or finger on Undo, Redo and slots still work. Nothing reorders |
| Disabled | Undo or Redo with nothing to do | `state/disabled/fg` and no hover fill, with the description "Nothing to undo". Focusable; a tap shows the reason in a Tooltip. Never hidden |
| Slot Menu open | Long press then lift on a slot | A Menu (light): Pin or Unpin, Replace with current tool, Move earlier, Move later (pinned only) |
| Drag | Long press then move on a pinned slot (4.7.1). An unpinned slot holds a recent that the app replaces in place, so moving it is Pin first; this is the one exception to 4.7.1 | Source at `state/drag/source-opacity`; `state/drag/insertion` line between slots; drag ghost on `z/drag-ghost` |
| Palm, off-hand rest | A touch larger than the platform palm threshold, or a finger touch that is long or moves, while the pen is in proximity or within `delay/palm-window` of its last contact | Ignored on every part. A finger tap counts only if under `input/chrome-tap/duration` (300 ms) and `input/chrome-tap/travel` (8 dp). Pen taps always count |
| Loading, Error | n/a | An extension tool that is not ready shows the Disabled form with its reason |
| Empty slot | Fewer recents than slots | The slot is simply not drawn; positions of the others do not move |

## 5. Content rules

- **Names:** slot "Quick slot 2, Eraser, pinned" and a Tooltip "Eraser, 2" (name, key). Undo and
  Redo carry no text, so "Rückgängig" and "Angre" never change the dock width.
- **No tool:** the Tool part shows the Pen default. It is never empty.
- **One slot, five slots:** positions are fixed, so the number keys 1 to 5 match what the user sees.
- **Colour:** the ink swatch is content, not a theme token. It always has a ring in `border/control`
  and the colour name in the accessible name ("Pen, Midnight blue, 2 point").
- **500 items:** n/a. The dock has at most eight parts.

## 6. Overflow rule

The dock never has a More and never scrolls. Fixed parts are Tool, Undo and Redo. Slots fill the
rest, as many as fit at `size/dock/slot`, up to `toolbar.slotsShown` (0 to 5).

- Capacity = floor((available length + gap) / (slot + gap)) minus 3, with available length the vertical
  free height, or on the corner strip the window width less `size/dock/slot` less the gutter. At 360
  dp wide that is 2 slots, from about 440 dp 5; the other pins are reached by number key and the Tool
  settings tab. Status parts count against capacity before slots.
- When slots must go: recent (unpinned) slots go first, last position first. Pinned slots keep
  their positions and are the last to go; if even they do not fit, the highest numbered pinned slot
  hides. A hidden slot still answers its number key, and the Tool settings tab lists all five.
- Never reordered, never made smaller than the slot, never moved into a menu.
- Text at 200 percent scales no label here, so capacity does not change; the Tooltips wrap.

## 7. Extension slots

Canvas tool (SYSTEM-MAP 7): a slot may hold an extension tool. It shows the extension's icon and
the Tooltip "Name, from Extension". The extension cannot pin, reorder, size or restyle slots. When
the extension is turned off, the slot disappears (the other slots keep their numbers and positions;
the gap closes only by the capacity rule) and its place is remembered and restored when it is turned
on again. A block or tool in a document keeps its stored fallback. Nothing else is extendable.

## 8. Pack content

None shown. A slot holds a copy of its bundle, so a pen from a pack that is Off or removed keeps
working and the dock shows no pack state. Pack state lives in the picker and Content pages.

## 9. Input

- **Stylus:** tap on lift. Hover Tooltip at 500 ms when not writing. Long press then lift on a slot
  opens its Menu; long press then move reorders a pinned slot. A pen contact on the dock does not
  recede the NoteBar.
- **Touch:** same, with the chrome palm rule: while the pen is in proximity or within 1 s of its
  last contact, large contacts on the dock are ignored and a finger tap counts only if it is short
  and still (300 ms, 8 dp). A hand resting on the dock never fires Undo. Long press on Undo or Redo
  shows the Tooltip only.
- **Mouse:** click; right click on a slot opens its Menu; drag after 4 px reorders a pinned slot.
- **Keyboard:** one tab stop for the dock. Up and Down (vertical) or Left and Right (horizontal)
  move; mirrored languages do not reverse the order. Enter activates. Shift+F10 or the Menu key
  opens a slot Menu; Alt+Arrow moves a pinned slot (the drag equivalent). 1 to 5 apply slots from
  anywhere in the document except while a text field has focus (a text block, the rename field,
  Go to page), where they type; T wheel; Q previous; Ctrl+Z, Ctrl+Shift+Z. F6 enters the dock as a landmark.
- **Handedness:** the dock is on the off-hand side. Switching `general.handedness` mirrors it live.
- **Commands:** `tool.wheel`, `tool.slot1` to `tool.slot5`, `doc.undo`, `doc.redo`, and to
  register: `tool.slotPin`, `tool.slotReplace`, `tool.slotMoveEarlier`, `tool.slotMoveLater`.

| Platform | Role |
|---|---|
| Web | `toolbar` "Tool dock", `aria-orientation`; slots `button`, Tool `button` with `aria-haspopup` |
| Compose | Row or Column with `Role.Button` items and `traversalIndex` in the fixed order |
| WinUI | `CommandBar`-style custom control; items `Button`, `AutomationProperties.Name` |
| SwiftUI | `HStack` or `VStack` with the `toolbar` container trait; items `Button` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/dock/slot`, `size/hit/touch`, `size/icon/default`, `size/icon/inline` | Targets, glyphs |
| `surface/raised`, `border/subtle`, `border/control` | Strip, edge, ink ring |
| `space/xs`, `space/sm`, `space/nudge`, `radius/pill` | Gaps, padding, target shape |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `action/toggle/fg-on` | Idle, hover, press, selected glyph |
| `selection/fill`, `selection/fill-hover`, `selection/indicator`, `border-width/emphasis` | Selected |
| `state/disabled/fg`, `state/drag/source-opacity`, `state/drag/insertion` | Disabled, drag |
| `delay/palm-window`, `input/chrome-tap/duration`, `input/chrome-tap/travel` | Palm rule |
| `z/chrome`, `z/drag-ghost`, `component/focus/*` | Layers, focus |
| `motion/duration/press`, `fast` | Press and colour changes, 0 under reduced motion |

## 11. Do and Don't

**Do**
- Keep it on screen, in place and at full size at every moment, including while the pen writes.
- Keep positions stable: pinned fixed, recents replaced in place, nothing moves with the pen down.
- Mirror to the off-hand edge from `general.handedness`, not from the language direction.
- Drop recent slots before pinned ones, and say why in the Tool settings tab.

**Don't**
- Don't add a dock menu, a More, or a scrolling strip.
- Don't animate, blink or highlight anything in it while the pen is down.
- Don't reverse its order for left-handed use or right to left text.
- Don't show pack or extension badges on slots, or put a state meant for the status part on a slot.

## 12. Traces to

Flows: 03 write-and-switch-tools, 10 recover (Undo), 13 open-document (responsive while ink loads).
Replaces: the NoteBar tool buttons. QuickSlot and Status are parts of this component.
