# Toggle

## 1. Purpose

A switch for one setting that is on or off and applies at once: Pen only, Sync, Turn on a pack, Turn
on an extension, Show the note bar. The row it sits in carries the label and the explanation.

**When not to use:**
- Two to three named states ("Light, Dark, System"): use SegmentedControl.
- A choice that applies only after Done: a checkbox or RadioList inside a Sheet.
- An action that happens once: use Button.
- A tool or an icon state (star, pin): use IconButton with `role` `toggle`.
- A filter in a result set: use Chip.

## 2. Anatomy

```
  Label text                          [ (  ) o ]  off
  Hint, one sentence                  [ o (  ) ]  on
                                         ^ knob
                                      ^ track: size/switch/width by size/switch/height
  hit slot: at least size/hit/min each way, track centred, whole row also taps
```

Parts: track, knob, and the host row's label and hint (owned by
SettingRow or ListRow, not by Toggle). The row binds the Toggle to a setting id from the settings registry (`pen.penOnly`, `appearance.pageDark`, `sync.*`), not to a command. The Toggle itself is the track and knob. It also exists
standalone with its own visible label to its start, using `type/label`.

## 3. Variants and sizes

One style. No `variant`, no `marked` variant (dropped), no `size` prop.

Size is the density role: `size/switch/width` by `size/switch/height` (52 by 32 touch, 40 by 24
compact). The knob is the track height minus `space/xs` on each side. State values are `off` | `on` (a
string, not a boolean). There is no `mixed` state: a switch has none in any platform role. A bulk
"Turn all on" is two Buttons, or a Checkbox for a parent row. A per notebook override ("Use app
setting") is a Select, not a third switch state.

## 4. States

| State | Track | Knob | Edge | Cue besides colour |
|---|---|---|---|---|
| Off, default | `switch/track-off` | `switch/thumb-off` | `border/control` | Knob at the start edge |
| Off, hover | `switch/track-off` | same | `border/control-hover` | Edge only |
| On, default | `switch/track-on` | `switch/thumb-on` | none | Knob at the end edge, a different position and a larger contrast, never colour alone. The track is a tint, not the accent |
| On, hover | `switch/track-on` | same | none | No change |
| Focus-visible | same | same | README focus ring | |
| Active (pressed) | off: `surface/control-hover`; on: `switch/track-on` | same | | No overlay. The knob does not widen |
| Selected | n/a (on is the selected state) | | | |
| Disabled | `state/disabled/bg` | `state/disabled/fg` | `state/disabled/border` | Reason in the description; stays focusable when the reason matters ("Needs an account") |
| Loading | n/a. The knob moves at once (optimistic); if the core refuses, it snaps back and the row shows an inline error with Retry | | | |
| Empty | n/a | | | |
| Error | n/a on the track | | | The row shows `border/error` under the row and a message with `state/danger/fg` |
| Drag | The knob can be dragged across; releasing past the midpoint commits | | | Same result as a tap |

Knob travel runs over `motion/duration/fast` with `motion/easing/standard`. No overshoot. Under
reduced motion the knob jumps and the track colour changes.

A disabled track is never the only sign: the row's text also uses `state/disabled/fg`, the reason is
read out on focus, and a tap on the row shows the reason hint (README). Toggles never use the
accent, so a page of them adds no accent region.

## 5. Content rules

- The label is the setting's name, a noun phrase or verb phrase in sentence case ("Pen only", "Sync
  this notebook"). It does not say "Enable" or "On" and does not change when the value changes.
- Very long text (German, Norwegian): the label and hint wrap in the row and the track stays at the
  end edge, vertically centred on the first line of the label. The track never shrinks or wraps.
- No text: a standalone Toggle with no visible label needs an accessible name from its row.
- One item: fine.
- Five hundred items: a settings list virtualizes at the list level; every Toggle keeps its slot.

## 6. Overflow rule

None. A Toggle is never grouped to fit. If a page has more than about 12 toggles, group them under
headings; Settings has the rule for sections and its Filter.

## 7. Extension slots

Settings page (SYSTEM-MAP 7): an extension's page is made of standard controls, and the schema's
`toggle` node maps to Toggle one to one (props `label`, `hint`, `value`; event `change`). The
extension's own "Turn off" row is a Toggle row, not a Button.

## 8. Pack content

A pack's on/off is a Toggle in its row on the Packs page only (flow 08); documents never show pack
state. The words are **Off** (the Toggle is off) and **Kept from <pack>**. A pack the user does not
have has no Toggle. A Toggle disabled by the system (needs a permission) carries the reason as its
description. A removed pack has no row; it lives in Trash. Turning a pack on or off is journaled and
shows no toast, because the Toggle undoes it in place.

## 9. Input

- **Stylus:** tap flips on lift. Hover shows the hover track. Dragging the knob works. Never reacts
  while the pen writes on the page.
- **Touch:** tap anywhere in the row (the row is the target; the track slot is at least
  `size/hit/min`) flips it. A drag on the knob flips on release past the middle.
- **Mouse:** click on release; hover track.
- **Keyboard:** Tab focuses; Space flips (on key up); Enter does not flip, so a Sheet's Enter can run
  its primary. Left and Right do nothing.

| Platform | Role |
|---|---|
| Web | `switch` with `aria-checked`; the label is `aria-labelledby` and the hint `aria-describedby` |
| Compose | `Role.Switch` on `Modifier.toggleable`, not Material `Switch` |
| WinUI | `ToggleSwitch` restyled, `OnContent` and `OffContent` empty |
| SwiftUI | `Toggle` with a custom `ToggleStyle` |

The state is spoken as "on" and "off". Changing it announces the new state.

## 10. Tokens

| Token | Use |
|---|---|
| `size/switch/width`, `size/switch/height` | Track |
| `size/hit/min` | Hit slot |
| `radius/pill` | Track and knob |
| `space/xs` | Knob inset |
| `switch/track-off`, `switch/thumb-off` | Off track and knob |
| `switch/track-on`, `switch/thumb-on` | On track and knob |
| `surface/control-hover` | Off pressed |
| `border/control`, `border/control-hover` | Off edge |
| `state/disabled/bg`, `fg`, `border` | Disabled off |
| `border/error`, `state/danger/fg` | Row error |
| `component/focus/*` | Focus ring |
| `motion/duration/fast`, `motion/easing/standard` | Travel |

## 11. Do and Don't

**Do**
- Apply the change at once and let the Toggle be its own undo.
- Make the whole row the touch target, with the track in the end slot.
- Keep the label constant; describe the effect in the hint.
- Let position carry the state: the knob is at the end edge when on, and the state is announced.

**Don't**
- Don't use a Toggle where a Done or Cancel is needed; use a Sheet with a Checkbox.
- Don't offer a second visual style, a marked variant or a size prop.
- Don't write "Enable" or "On" in the label.
- Don't ask to confirm a Toggle. If the effect is irreversible, it is a Button in a Dialog.

## 12. Traces to

Flows: 01 first-run (Pen only), 03 write-and-switch-tools (pen options), 07 customize-appearance
(page dark, mirror), 08 packs (Turn on), 09 extensions (Turn off), 12 settings (all), 14
share-and-sync (Sync on), 15 add-to-page (layer visible).

Replaces: Toggle (the `marked` variant and the `sm` size are dropped), the Android private Toggle.
