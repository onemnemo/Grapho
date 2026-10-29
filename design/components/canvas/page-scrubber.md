# PageScrubber

## 1. Purpose

Move through a long document by dragging one handle on the off-hand edge. It appears while the
document scrolls, only when the document has more than 20 pages, and shows which page you are
about to reach.

**When not to use:**
- Going to a known page number: the Go to page Popover from the Note bar indicator.
- Seeing every page: the Pages tab in the SidePanel.
- A canvas (no pages): Minimap and Views.
- Showing position while scrolling with nothing to drag: CanvasChip `scroll`.

## 2. Anatomy

```
 off-hand edge (left for a right-handed writer)

 |#|                     bubble, on the page side of the handle
 | |    +------------------+
 | |    | [thumb]  12 of 300|
 |=|<-- +------------------+     handle: a pill, size/hit/touch wide and high
 | |
```

| Part | Notes |
|---|---|
| Handle | A pill, `radius/pill`, fill `media/glass`, a grip of two short bars in `media/text`. Visual smaller than its slot; the slot is at least `size/hit/touch` wide and `size/hit/min` tall, 44 dp wide as the flows state |
| Track | Not drawn. The handle's travel is the page area's height between the band stack and the bottom edge, and the handle's position is proportional to the current page |
| Bubble | Appears beside the handle while dragging: `media/glass`, `media/text`, `type/label` "12 of 300" with the page thumbnail above when it exists, inside a `media/edge` edge. No thumbnail yet: number only |

## 3. Variants and sizes

None. One size; the handle is not a Button and does not take a height role. `edge` follows
handedness (section 9), never the language direction.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Hidden | Documents under `count/scrubber-min-pages`, canvas documents, `toolbar.scrubber` off. Faded by opacity only; it stays in the accessibility tree (see Input) |
| Appearing | A finger, mouse or key scroll, or a pen hover at the edge (Input): opacity 0 to 1 over `motion/duration/fast`. Never while the pen is down. Reduced motion: 0 ms |
| Visible | `media/glass` handle, `media/text` grip; position tracks the scroll without animation |
| Hover | Cursor is a grab hand; keeps it visible |
| Focus-visible | `component/focus/*`, gap colour set to the glass fill |
| Active (dragging) | `action/glass/bg-pressed`, bubble shown. Pages scroll as placeholders and render once the drag rests for `delay/live-settle`, so a 300 page drag does not thrash rendering |
| Fine mode | Dragging sideways away from the edge slows the handle: past `size/hit/touch` of lateral travel it moves at a quarter speed, past twice that at a sixteenth. The bubble shows a second step ("Fine") so the user knows |
| Fading | Opacity to 0 over `motion/duration/exit` after `delay/pen-idle` without scroll or use |
| Pen down | A visible handle does not appear, move or fade while the pen is on the page |
| Loading | No thumbnail yet: the bubble shows the number only |
| Disabled, Empty, Error | n/a |

## 5. Content rules

- **Localised text:** the bubble reads "Seite 12 von 300", "12 sur 300", grouped digits; its width
  is measured from the longest formatted string for the locale. It flips to the page side of the
  handle so it is never clipped.
- **No text:** n/a. **One item:** a short document shows no scrubber.
- **500 and 10,000 pages:** the handle is proportional and fine mode gives single-page steps; the
  bubble includes a Go to page action (`nav.goToPage`) as its last element for a finger or mouse.

## 6. Overflow rule

One handle. If the band stack leaves less than three hit slots of travel (a window under
`layout/breakpoint/short` tall with the FindBar open) the scrubber is hidden and the Go to page Popover
and the Pages tab remain.

## 7. Extension slots

None.

## 8. Pack content

None.

## 9. Input

- **Handedness:** on the off-hand edge, `space/lg` clear of the dock or panel edge so a near miss
  never hits them. Right-handed: left edge of the page area; with the side panel
  open, the panel's page-side edge. Left-handed: the mirror. It sits clear of the writing-hand edge
  guard, so it never fights the page edge gestures.
- **Touch:** only a touch that starts on the handle drags it. A touch elsewhere on the strip does
  nothing, which holds in pen-only mode and in general, so a gripping hand never scrubs. A touch
  larger than `input/palm-threshold` on the handle is ignored while the pen is in proximity or
  within `delay/palm-window` of its last contact.
- **Stylus:** a pen contact that starts on the handle drags it; any other pen contact is ink. The
  pen summons the scrubber by hovering within `size/hit/touch` of the off-hand edge for
  `delay/hover-recall`; a pen without hover reaches pages through the Go to page Popover, the Pages
  tab and two-finger scroll. `nav.scrub` (Show page scrubber, no key, to register) is in the palette.
- **Mouse:** click and drag the handle; the wheel scrolls and shows the handle. Clicking the empty
  strip does nothing.
- **Keyboard:** Page Up and Page Down, Ctrl+Home and Ctrl+End scroll and show the handle. The handle
  is a Tab stop after the page while visible; focus holds it. Left, Right, Up and Down move one page,
  Shift with an arrow ten, Home and End the ends. Page Up and Page Down stay `nav.prevPage` and
  `nav.nextPage`.
- **Screen reader:** the handle is always in the accessibility tree for documents over 20 pages, even
  when faded; it appears on focus. Role `slider` with `aria-valuemin` 1, `aria-valuemax` N,
  `aria-valuenow` and `aria-valuetext` "Page 12 of 300"; swipe up and down changes the page. Compose
  `Modifier.semantics { progressBarRangeInfo }` with `setProgress`; WinUI `RangeValuePattern`;
  SwiftUI `accessibilityAdjustableAction`. Name "Scroll through pages".
- **Tooltip:** the handle is a control with a name: "Scroll through pages" (no shortcut).
- **Commands:** `nav.nextPage`, `nav.prevPage`, `nav.firstPage`, `nav.lastPage`. Setting
  `toolbar.scrubber` (Page scrubber, device, a toggle in Settings).

## 10. Tokens

| Token | Used for |
|---|---|
| `media/glass`, `media/text`, `media/edge`, `media/ring`, `action/glass/bg-pressed`, `component/focus/*` | Handle, grip, bubble, thumbnail edge, press, focus |
| `radius/pill`, `radius/item`, `space/sm`, `space/md` | Shapes and gaps |
| `size/hit/touch`, `size/hit/min`, `size/scrubber/handle-height` | Slot and visual (the last is to add) |
| `type/label` | Bubble text |
| `motion/duration/fast`, `exit`, `press` | Fades and press |
| `delay/pen-idle`, `delay/hover-recall`, `delay/live-settle`, `delay/palm-window`, `input/palm-threshold`, `count/scrubber-min-pages` | Timers, palm, threshold (last two new) |
| `z/floating-bar` | Layer |

## 11. Do and Don't

**Do**
- Show the handle while scrolling or on a pen hover at the edge, and keep it in the accessibility tree.
- Show the page number in the bubble even before thumbnails exist.
- Mirror to the off-hand edge for the left-handed writer.
- Let the handle follow the scroll without animation.

**Don't**
- Don't draw a track or a full-height touch strip; only the handle takes touch.
- Don't scrub from a pen stroke elsewhere or from a palm.
- Don't resize the bubble when a thumbnail arrives.
- Don't appear, fade or move the handle while the pen is on the page.

## 12. Traces to

Flows: 11 navigate-long-document (scrub, 300 pages). Replaces: none (new).
