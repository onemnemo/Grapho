# CanvasChip

## 1. Purpose

A small transient chip over the page that reports where you are or what just happened: the scroll
position ("12 of 300"), the zoom ("150%"), or a shape that was just snapped, with Undo. It is the
only floating text on the page, and it never costs ink or position.

The Note bar's page indicator ("12 of 300", opens Go to page) is a Button `text` `small` owned by
NoteBar and is not a CanvasChip.

**When not to use:**
- A lasting state of the page or document: Banner.
- Something that happened elsewhere and can be undone: Toast.
- Naming a control: Tooltip.
- A choice with more than one action: Popover.
- Page navigation by typing: the Go to page Popover.

## 2. Anatomy

```
 fixed slot, top centre                 beside the shape, off-hand side
     +--------------+                   +-----------------------+
     |  12 of 300   |  scroll           | Circle        [ Undo ] |  snapped
     +--------------+                   +-----------------------+
     +--------------+
     |    150%      |  zoom (a button)
     +--------------+
```

| Part | Notes |
|---|---|
| Pill | `radius/pill`, fill `media/glass`, text `media/text` (the page is unknown artwork; `media/chip` is not allowed). Height `size/control/small`, side padding `space/lg`. Glass items carry no elevation |
| Label | `type/label`, tabular figures. Width is reserved for the widest value in the current locale and document (see section 5) |
| Slot | `scroll` and `zoom` share one fixed slot at top centre, `space/md` below the **full height** of the band stack (Note bar, FindBar), whether or not each is showing, so the bar's return never moves the chip. The slot reserves that strip of the page: the pen draws through it (the chip is not hit-tested for the pen) |
| Snap anchor | `snapped` sits `space/lg` from the shape's bounds on the off-hand side, clamped inside the page area and `size/edge-guard` from the writing-hand edge |

## 3. Variants and sizes

`kind`: `scroll` | `zoom` | `snapped`. One size, `size/control/small`.

| Kind | Text | Role | Interactive | Command |
|---|---|---|---|---|
| `scroll` | "12 of 300". Shown only while the Note bar is receded, so there is one live region | status | No; pointer events pass through | none |
| `zoom` | "150%" | button; name is the command label, "Fit width" ("Fit all" on a Canvas) | Finger and mouse | `view.fitWidth` or `view.fitPage` |
| `snapped` | The shape ("Circle", "Line") and an Undo button | status with a button | Finger, mouse, keyboard. A pen contact is ink | `shape.undoSnap` (to register) |

There is no `scroll` chip on a Canvas; position there is the Minimap when toggled on.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Hidden | Not drawn; the slot is empty |
| Appearing | Opacity 0 to 1 over `motion/duration/fast`, no movement. `snapped` appears at the pen lift, never while the pen is down. Reduced motion: 0 ms |
| Visible | `media/glass`, `media/text`. Text updates in place, once per frame |
| Hover (`zoom`, `snapped`) | The control cursor; Tooltip after `feedback/delay/tooltip` |
| Focus-visible | The `component/focus/*` recipe, gap colour set to the glass fill |
| Active | `action/glass/bg-pressed` for `motion/duration/press`, with `media/ring` as the press ring |
| Disabled, Loading, Empty, Error | n/a. A chip that cannot act is not shown |
| Fading | Opacity to 0 over `motion/duration/exit` after `delay/pen-idle` (1.5 s) without scroll, zoom or pointer use. `snapped` lasts `delay/snap-undo` |
| Pen down | A visible chip does not change, fade or move while the pen is down. A timer that expires then fades at the lift. A new stroke on the page ends `snapped` at that stroke's lift |
| Held | Hover, focus or screen reader focus keeps the chip until they leave |

## 5. Content rules

- **Localised text:** strings are catalogue templates ("Seite 12 von 300", "150 %" with a space in
  Norwegian, German and French, grouped digits, Indic digits). The reserved width is measured from
  the longest formatted value for the locale: `N of N` for the page count, the maximum zoom, the
  longest shape name. It never resizes as digits change. The slot grows by line only at 200% text.
- **No text:** never. A chip with no value is not shown.
- **One item:** a one-page document has no `scroll` chip.
- **500 items:** "412 of 500" and "10 000 of 10 000" fit by measure. Zoom shows whole percent up to
  the document's maximum.
- **Shape names:** from a closed set; the status line uses a catalogue message per shape, so
  grammatical case is correct in every language.

## 6. Overflow rule

One chip of each kind at a time; `zoom` replaces `scroll` in the same slot by text swap, and back.
`snapped` never shares the slot. A second snap replaces the first at its lift.

## 7. Extension slots

None. Extensions cannot draw over the page outside a block or their panel (SYSTEM-MAP 7).

## 8. Pack content

None.

## 9. Input

- **Shape snapping is hold to snap.** Holding the pen still at the end of a stroke for
  `delay/snap-hold` (600 ms) snaps the stroke to the suggested shape. Nothing snaps without that hold.
  This is the one change under a pen that is down, and it is the user's own deliberate hold. The chip then appears at the lift with Undo.
- **Stylus:** a pen contact on any chip is ink and never activates it. Reset zoom and Undo snap are
  for finger, mouse and keyboard. A pen user has Undo in the dock and the two-finger tap.
- **Touch:** a finger tap activates on lift. While the pen is in proximity or within
  `delay/palm-window`, a touch larger than `input/palm-threshold` is ignored and a finger tap counts
  only if under `input/chrome-tap/duration` and `input/chrome-tap/travel`.
- **Mouse:** click. Ctrl and the wheel shows `zoom`; the plain wheel shows `scroll`.
- **Keyboard:** Tab from the page reaches a visible `zoom` or `snapped` chip and holds it. Enter
  activates, Escape hides it. Page keys and Ctrl+1/2/0 show the chips and take no focus.
- **Roles:** `scroll` `status` ("Page 12 of 300"); `zoom` `button` named "Fit width, 150%";
  `snapped` `status` plus a `button` "Undo". Compose: `liveRegion`, `Button`; WinUI `LiveSetting`,
  `Button`; SwiftUI `.updatesFrequently`, `Button`. The live region announces after the value settles
  for `delay/live-settle`, never per pinch frame.
- **Tooltip:** `zoom` "Fit width, Ctrl+1"; Undo "Undo, Ctrl+Z".
- **Commands:** `view.fitWidth`, `view.fitPage`, `doc.undo`. To register: `shape.undoSnap`. The
  setting `pen.snap` is renamed "Hold to snap shapes" (Tool settings tab).

## 10. Tokens

| Token | Used for |
|---|---|
| `media/glass`, `media/text`, `media/ring`, `action/glass/bg-pressed` | Pill, text, press |
| `component/focus/*` | Focus |
| `radius/pill`, `space/lg`, `space/md`, `size/control/small`, `size/hit/min` | Shape, spacing, size |
| `type/label`, `size/edge-guard` | Text, clamp |
| `motion/duration/fast`, `exit`, `press` | Fades |
| `delay/pen-idle`, `delay/palm-window`, `delay/snap-hold`, `delay/snap-undo`, `delay/live-settle` | Timers (last three to add) |
| `feedback/delay/tooltip`, `input/chrome-tap/*`, `input/palm-threshold` | Tooltip, palm |
| `z/floating-bar` | Layer |

## 11. Do and Don't

**Do**
- Reserve the width of the longest localised value.
- Let the pen draw straight through every chip.
- Freeze a visible chip while the pen is down, and fade it after the lift.
- Snap only after the 600 ms hold, and show the result with Undo.

**Don't**
- Don't use `media/chip` or a solid fill.
- Don't let a pen tap accept, reset or undo anything.
- Don't move the chip when the Note bar returns or the panel opens.
- Don't add a close button; the chip leaves by itself.

## 12. Traces to

Flows: 11 navigate-long-document (scroll, zoom), 03 write-and-switch-tools (zoom chip), 15
add-to-page (shapes). Snapping traces to the decision "Shape detection suggests". Replaces: PageIndicator and ZoomChip (merged), ShapeSuggest (as `snapped`).
