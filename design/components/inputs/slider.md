# Slider

## 1. Purpose, and when not to use

**Purpose.** Pick a number on a range where the feel matters: pen width, smoothing, texture.

**When not to use.** A count of 0 to 5: Stepper. A few named levels: SegmentedControl or Select. A value typed exactly: TextField `numeric`. A colour channel: the ColourEditor draws its own tracks. Vertical, two-thumb, centred and gradient sliders are not part of this version; they would be new variants.

## 2. Anatomy

```
 Smoothing                                        (label row)
 [-----------o=====o------------------]   [ 40 % ]   <- track, fill, thumb, readout
 (alert_circle) Choose 1 to 40.                       <- message row
```

Parts: `label`, `track`, `fill`, `thumb`, `readout` (a borderless small numeric TextField, the numeric entry), optional `ticks`, a `bubble` while dragging, and the shared message row.

## 3. Variants and sizes

- `layout`: `row` | `stacked`. `row` becomes `stacked` below 480 dp container width or above 150% text scale.
- `size`: `small` | `default` (small resolves to default at touch density).
- `scale`: `linear` | `stepped` (ticks and snapping at 11 steps or fewer) | `mapped` (a named curve, for example pen width; the readout shows the real value, never a 0 to 100 proxy).
- Orientation is horizontal only.

## 4. States

| State | Tokens that change |
|---|---|
| Rest | track `surface/strong`; fill `selection/indicator`; thumb `surface/raised` with `border/control` edge |
| Hover | thumb edge `border/control-hover` |
| Focus-visible | ring around the thumb, never clipped |
| Active (dragging) | thumb edge `border/focus`; `press/fill` halo of `size/hit/touch`; bubble |
| Selected | not applicable |
| Disabled | track `state/disabled/bg`, thumb `state/disabled/border`, readout `state/disabled/fg`; reason in the message row |
| Loading, empty | not used; the slider keeps its last value |
| Error | A typed readout outside the range is clamped and the message row reads "Choose 1 to 40." with `alert_circle` in `state/danger/fg`. Visible without any animation |
| Drag | see Input |

Forced colours: track an outline in the system edge colour, fill and thumb the system highlight. The fill/track and thumb pairs are declared in the README.

## 5. Content rules

- Label wraps (German, Norwegian); the readout stays on the first line.
- Readout shows the unit in the locale format ("2,5 px", "40 %") with fixed decimals matching `step`.
- Min equals max: disabled.
- 1 to 500 steps: ticks only up to 11 steps. Beyond that, `step` governs.

## 6. Overflow rule

Track minimum 160 dp; below that the `stacked` layout puts the track on its own line.

## 7. Extension slots

None. Extension schema node `slider` maps to this component: `change` is `onChange`, `commit` is `onChangeEnd`, and the node needs `unit`.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch.** Contact on the thumb drags relative to where the contact began; there is no jump. Contact on the track moves the thumb by one page step (10% of the range) toward the contact, or, if the contact then moves horizontally past 8 dp, picks up the thumb relative to it. The hit slot is `size/hit/touch` tall.
- **Direction lock.** Horizontal intent starts a value drag; vertical intent first lets the parent scroll. **Fine adjust** applies only after a drag has started: moving away from the track vertically slows it to half speed beyond 48 dp and a quarter beyond 96 dp, both capped by half the distance to the host edge, and off in a bottom panel that scrolls vertically.
- The bubble sits 48 dp above the thumb so a finger does not hide it. Pointer capture holds the drag.
- **Mouse.** Click and drag; no wheel handling.
- **Keyboard.** One step: Right or Up adds in left-to-right text, and Left adds in right-to-left (keys follow the mirrored range); Shift plus an arrow moves 10 steps; Page Up and Page Down 10% of the range; Home and End. The readout is the next tab stop: typing digits there uses the numeric contract (FieldShell 9.3), Enter commits, Escape restores.
- **Undo.** `onChange` fires continuously for preview; `onChangeEnd` fires on release or after 400 ms of keys. A Slider in the document or Studio (pen width in the dock, Studio texture) makes one entry on that host's undo stack per release. A Slider on a Settings page applies at once and has no undo stack.
- **Handwriting** into the readout follows TextField `numeric` (enabled).
- ARIA: `slider` with `aria-valuemin`, `aria-valuemax`, `aria-valuenow`, `aria-valuetext` (value with unit), `aria-orientation="horizontal"`; the readout is a separate `spinbutton`. Native: custom on Compose with `semantics { setProgress }`, WinUI `Slider` re-templated, SwiftUI `Slider` re-styled.
- Tooltip: the readout, "Type a value".

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `size/hit/touch`, `size/hit/min` | height, hit slot |
| `size/track`, `size/thumb`, `size/tick` (to add) | track thickness, thumb diameter, tick length |
| `surface/strong`, `selection/indicator` | track, fill |
| `surface/raised`, `border/control`, `border/control-hover`, `border/focus` | thumb |
| `press/fill` | drag halo |
| `surface/overlay`, `elevation/popover`, `text/default` | bubble |
| `state/disabled/bg`, `border`, `fg` | disabled |
| `state/danger/fg` | clamp message |
| `text/default`, `text/muted`, `type/label`, `type/caption` | label, readout, message |
| `radius/pill`, `space/sm`, `space/md` | shape, gaps |
| `focus/gap/*`, `focus/ring/*` | ring |

## 11. Do and Don't

- Do show the real value and unit in the readout, with a visible message when it clamps.
- Do keep the thumb under pointer capture and let a vertical scroll win.
- Don't jump the thumb on a pen contact.
- Don't announce a clamp with colour or motion alone.

## 12. Traces to

Flows 03, 09, 12. Replaces Slider.
