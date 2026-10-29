# Canvas tools

What sits over or in the page while you write: the wheel, the chips, the scrubber, the minimap, the
selection bar that rides a lasso, and the frame for a block whose extension is missing. Rules come
from SYSTEM-MAP 3, 4.1, 5, 7 and 8, `system/document.md` and `content-system/surfaces.md` section 9.

## Members

| Component | File | Variants | Job | Flows |
|---|---|---|---|---|
| ToolWheel | [tool-wheel.md](tool-wheel.md) | `placement` pen, dock; `mode` radial, menu | Tools and colours as rings, pens in the middle, width and opacity inside the pen | 03 |
| CanvasChip | [canvas-chip.md](canvas-chip.md) | `kind` scroll, zoom, snapped | Position, zoom, and a snapped shape with Undo | 11, 03, 15 |
| PageScrubber | [page-scrubber.md](page-scrubber.md) | none | Drag to move through a long document | 11 |
| Minimap | [minimap.md](minimap.md) | none | Overview of a Canvas document | 11 |
| Canvas selection bar | [canvas-selection-bar.md](canvas-selection-bar.md) | SelectionBar `canvas` | The page rules of the one SelectionBar | 03, 15 |
| BlockPlaceholder | [block-placeholder.md](block-placeholder.md) | `cause` six values | Stored picture and text for a missing extension block | 09, 13 |

Not here: FindMarks (flow 06, with the FindBar), the Note bar indicator (a Button in NoteBar), Go to
page (a Popover recipe).

## How they line up

One surface rule: **glass** (`media/glass`, no elevation) for transient or toggled overlays on the page
(chips, scrubber, minimap); **raised** (`surface/raised`, `elevation/float`) for bars that ride the
page; the **wheel plate** for the fixed dark instrument; **page-relative** (`canvas/*`) for what lives in
the page. A fifth look needs a decision.

| Property | ToolWheel | CanvasChip | PageScrubber | Minimap | Canvas selection bar | BlockPlaceholder |
|---|---|---|---|---|---|---|
| Surface | `wheel/plate` | `media/glass` | `media/glass` | `media/glass` | `surface/raised` | `canvas/placeholder-bg` |
| Edge | `wheel/band-edge` | none | none | `media/edge` | `border/subtle` | `canvas/placeholder-edge` |
| Radius | round disc | `radius/pill` | `radius/pill` | `radius/surface` | `radius/surface` | `radius/item` |
| Elevation | `elevation/wheel` | none | none | none | `elevation/float` | none |
| Layer | `z/wheel` | `z/floating-bar` | `z/floating-bar` | `z/floating-bar` | `z/floating-bar` | `z/canvas` |
| Size | derived from `size/hit/touch` | `size/control/small` | handle slot `size/hit/touch` | `size/minimap/width` | `size/bar/note` | by block bounds |
| Smallest target | `size/hit/touch` | `size/hit/min` | `size/hit/touch` | the viewport, `size/minimap/viewport-min` | `size/hit/min` | action buttons |
| Text | `type/label`, `type/caption` | `type/label` | `type/label` | none | `type/label` | `type/label`, `type/caption`, `type/body` |
| Focus | `component/focus/*` | `component/focus/*` | `component/focus/*` | `component/focus/*` | `component/focus/*` | `component/focus/*` |
| Fade | `motion/duration/exit` | `delay/pen-idle` | `delay/pen-idle` | none (toggled) | at pen contact only | none |
| Handedness | first slot nearest the dock | top centre | off-hand edge | off-hand top corner (movable) | clamps off the writing-hand edge | none |

Shared rules:

- **Ink is never lost to chrome.** A pen contact on the page draws. The only pen-activated controls
  are the wheel's own segments, the scrubber's handle, the minimap's viewport and strip, and a
  selection bar action after a hover dwell. Chips are never pen targets. A pen-down outside the wheel
  hides it at 0 ms and the stroke is drawn.
- **Nothing moves, blinks or reflows while the pen is down.** The only motion at pen contact is the
  selection bar's opacity step, as the Note bar's slide. Everything else freezes and fades after the
  lift. The single permitted change under a held pen is hold to snap, the user's own 600 ms hold.
- **Palm rule.** While the pen is in proximity or within `delay/palm-window` of its last contact,
  touches larger than `input/palm-threshold` are ignored on all six, and a finger tap counts only if
  under `input/chrome-tap/duration` and `input/chrome-tap/travel`.
- **Fixed slots.** Chips and the minimap sit below the full height of the band stack (Note bar,
  FindBar), whether or not each is showing. This reserves a strip at the top of the page; the pen
  draws through it.
- **Reduced motion:** every fade is 0 ms.
- **Nothing is the only way.** The wheel has the menu form, the Tool settings tab and the Picker; the
  scrubber has Go to page and keys; the minimap has Views; the placeholder's actions are in the
  SelectionBar More.

## Decisions

1. Pens stay in the middle. The centre holds the pens; width and opacity are edited in the pen's segment; Previous is `tool.previous`.
2. Summon only: there is no edge tab and no carry-to-dock. A colour change is two contacts.
3. The label line in the hub is the wheel's visible name, so no Tooltip floats over the wheel.
4. Nothing in the wheel overshoots.
5. "Tools page" is the Tool settings tab's Tools list.
6. The chip slot is at top centre and fades after 1.5 s.
7. The Note bar indicator is a Button, not a CanvasChip.
8. Canvas selection bar is SelectionBar's placement, not a component.
9. Open question: `edit.group` and `nav.goToPage` share Ctrl+Alt+G.
10. `nav.scrub` and `view.minimap` need palette entries; `toolbar.scrubber` is a setting.

## Commands to register

`tool.previous` (Previous tool, Q; also a double tap on the dock's current tool), `tool.pensMore`
(All pens), `tool.toolsPage` (Tools page), `tool.colourSets` (Colour sets), `shape.undoSnap`
(Undo shape snap), `selection.colour` (Colour of selection), `nav.scrub` (Show page scrubber),
`block.keepAsImage`, `block.getExtension`, `block.turnOnExtension`, `block.restoreExtension`,
`block.updateExtension`. Settings: `pen.snap` is labelled "Hold to snap shapes"; `pen.buttonClick` and
`pen.buttonHold` are settings.

## Tokens to add

| Token | Value | Why |
|---|---|---|
| `size/wheel/hub-radius` | derived: half diagonal of a 3 by 2 grid of `size/hit/touch` cells plus the label line, plus a divider (about 90 at 44, 96 at 48) | Pens hub |
| `size/wheel/outer-radius` | hub radius plus two rings of `size/hit/touch` plus dividers (about 186 at 44, 198 at 48) | Disc |
| `size/wheel/margin` | `size/edge-guard` | Distance from the window edge |
| `size/wheel/min-window` | outer diameter plus twice the margin | Below this the wheel is the menu form |
| `wheel/hover`, `wheel/press` | white at 12% and 20% on `wheel/plate` | Fixed dark states |
| `wheel/selected-fill`, `wheel/selected-indicator` | white at 14%; white | Selection |
| `wheel/text`, `wheel/text-muted` | white; white at 70% | Label line (pairs on `wheel/plate`, 4.5:1) |
| `wheel/swatch-edge`, `wheel/swatch-ring` | edges at 3:1 against the plate and any swatch; a ring pair for the selected swatch | Dark swatches and nibs |
| `wheel/nib-well` | a light disc behind a nib | Black nibs on charcoal |
| `input/pen-button/hold` | 300 ms | Click versus hold |
| `delay/snap-hold`, `delay/snap-undo` | 600 ms, 2000 ms | Hold to snap, Undo chip |
| `delay/pen-dwell` | 300 ms | Pen hover before an action |
| `delay/live-settle` | 250 ms | Live region and scrubber render settle |
| `count/scrubber-min-pages` | 20 | When the scrubber exists |
| `size/scrubber/handle-height` | 56 | Visual handle |
| `size/minimap/width`, `width-compact`, `height-min`, `height-max` | 160, 120, 90, 160 | Size |
| `size/minimap/viewport-min` | 12 | Smallest viewport rectangle |
| `limit/minimap-reach` | 100 viewports | Clip distance |
| `canvas/minimap/content`, `canvas/minimap/viewport` | each 3:1 on `media/glass` over a white page | Content and view |
| `canvas/placeholder-bg`, `-edge`, `-text`, `-muted` | page-relative, fixed in every theme, pairs on `canvas/page` | Placeholder |
| `size/placeholder/actions-min` | 200 | Width below which actions go to a Menu |

The tokens `wheel/well`, `wheel/grip` and `wheel/scroll-track` are not used; `wheel/plate` is the whole disc.
Icons to add: `map` (the Minimap toggle).
