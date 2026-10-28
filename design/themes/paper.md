# Paper

Theme family `paper`. Variants: `paper-light` and `paper-dark`.

## Character

Warm, like a notebook on a desk. Sand surfaces with a clear yellow cast and warm brown-grey text, so long reading and writing sessions in daylight feel soft rather than clinical. Paper dark keeps the warmth for night: a brown-black with cream text, never a cold grey.

## When to use

- Daylight use, outdoors, libraries, and anyone who finds white screens harsh.
- Paper dark for evening study when the person still wants a warm tone.

## Semantic overrides

Each cell is the primitive the token points at, as `ramp.step`, or a tint written as base colour and opacity. These are the only values the theme sets; everything else (type, space, radius, motion, sizes) is shared. The source of truth is `design/tokens/tokens.json`.

| Token | Paper light | Paper dark |
|---|---|---|
| `state/drag/target-fill` | `bark at 10%` | `cream at 10%` |
| `state/drag/target-line` | `sand.800` | `umber.50` |
| `state/drag/insertion` | `sand.800` | `umber.50` |
| `state/disabled/fg` | `bark at 34%` | `cream at 34%` |
| `state/disabled/bg` | `bark at 6%` | `cream at 6%` |
| `state/disabled/border` | `bark at 14%` | `cream at 14%` |
| `state/readonly/fg` | `sand.700` | `umber.300` |
| `state/readonly/bg` | `bark at 4%` | `cream at 4%` |
| `state/readonly/border` | `bark at 24%` | `cream at 22%` |
| `state/danger/fg` | `red.700` | `red.300` |
| `state/danger/bg` | `red-700 at 8%` | `red-500 at 16%` |
| `state/danger/bg-hover` | `red-700 at 12%` | `red-500 at 22%` |
| `state/danger/line` | `red-700 at 30%` | `red-300 at 22%` |
| `state/danger/solid` | `red.600` | `red.600` |
| `state/danger/on-solid` | `grey.0` | `grey.0` |
| `state/warning/fg` | `gold.800` | `gold.400` |
| `state/warning/bg` | `gold-800 at 8%` | `gold-400 at 16%` |
| `state/warning/bg-hover` | `gold-800 at 12%` | `gold-400 at 26%` |
| `state/warning/line` | `gold-800 at 30%` | `gold-400 at 22%` |
| `state/success/fg` | `green.700` | `green.300` |
| `state/success/bg` | `green-700 at 8%` | `green-500 at 20%` |
| `state/success/bg-hover` | `green-700 at 12%` | `green-500 at 30%` |
| `state/success/line` | `green-700 at 30%` | `green-500 at 22%` |
| `state/info/fg` | `blue.700` | `blue.300` |
| `state/info/bg` | `blue-700 at 8%` | `blue-500 at 16%` |
| `state/info/bg-hover` | `blue-700 at 12%` | `blue-500 at 26%` |
| `state/info/line` | `blue-700 at 30%` | `blue-500 at 30%` |
| `surface/base` | `sand.50` | `umber.900` |
| `surface/raised` | `sand.100` | `umber.850` |
| `surface/control` | `sand.200` | `umber.800` |
| `surface/control-hover` | `sand.250` | `umber.775` |
| `surface/strong` | `sand.300` | `umber.750` |
| `surface/overlay` | `sand.25` | `umber.775` |
| `surface/sunken` | `sand.300` | `umber.950` |
| `text/default` | `sand.800` | `umber.50` |
| `text/muted` | `sand.700` | `umber.300` |
| `text/subtle` | `sand.600` | `umber.400` |
| `text/disabled` | `bark at 34%` | `cream at 34%` |
| `text/on-action` | `sand.25` | `umber.900` |
| `border/subtle` | `bark at 10%` | `cream at 8%` |
| `border/strong` | `bark at 16%` | `cream at 14%` |
| `border/control` | `bark at 62%` | `cream at 40%` |
| `border/control-hover` | `bark at 85%` | `cream at 70%` |
| `border/focus` | `sand.800` | `umber.50` |
| `border/focus-gap` | `sand.50` | `umber.900` |
| `border/error` | `red.700` | `red.300` |
| `border/warning` | `gold.800` | `gold.400` |
| `action/primary/bg` | `sand.800` | `umber.50` |
| `action/primary/fg` | `sand.25` | `umber.900` |
| `action/primary/bg-hover` | `sand.900` | `sand.25` |
| `action/primary/bg-pressed` | `sand.750` | `umber.100` |
| `action/primary/bg-disabled` | `bark at 12%` | `cream at 12%` |
| `action/primary/fg-disabled` | `bark at 34%` | `cream at 34%` |
| `action/secondary/bg` | `sand.200` | `umber.800` |
| `action/secondary/fg` | `sand.800` | `umber.50` |
| `action/secondary/bg-hover` | `sand.300` | `umber.750` |
| `action/secondary/bg-pressed` | `sand.400` | `umber.700` |
| `action/quiet/bg` | `transparent` | `transparent` |
| `action/quiet/fg` | `sand.800` | `umber.50` |
| `action/quiet/bg-hover` | `bark at 6%` | `cream at 7%` |
| `action/quiet/bg-pressed` | `bark at 12%` | `cream at 12%` |
| `selection/fill` | `bark at 10%` | `cream at 10%` |
| `selection/fill-hover` | `bark at 14%` | `cream at 13%` |
| `selection/indicator` | `sand.800` | `umber.50` |
| `selection/media-ring` | `grey.0` | `grey.0` |
| `selection/media-halo` | `grey.900` | `grey.900` |
| `hover/fill` | `bark at 6%` | `cream at 7%` |
| `press/fill` | `bark at 12%` | `cream at 12%` |
| `canvas/page` | `page.white` | `page.cream` |
| `canvas/ink-default` | `pen.ink` | `pen.ink` |
| `canvas/rule` | `rule at 12%` | `rule at 12%` |
| `canvas/selection-fill` | `ink at 6%` | `ink at 6%` |
| `canvas/handle` | `grey.0` | `grey.0` |
| `canvas/handle-edge` | `grey.900` | `grey.900` |
| `canvas/marquee` | `blue.700` | `blue.700` |
| `canvas/guide` | `blue.500` | `blue.500` |
| `scrim/default` | `black at 40%` | `black at 62%` |
| `scrim/stacked` | `black at 22%` | `black at 34%` |
| `shadow/soft` | `black at 16%` | `black at 70%` |
| `shadow/deep` | `black at 26%` | `black at 85%` |
| `shadow/thumb` | `black at 28%` | `black at 50%` |
| `highlight/match` | `gold-400 at 55%` | `gold-400 at 30%` |

## Contrast

Checked on every build by `build-tokens.mjs`. Lowest ratio of `text/default`, `text/muted` and `text/subtle` on any surface: Paper light 4.88:1, Paper dark 4.77:1 (needs 4.5:1). Control edges, the focus ring, error edges and the selection indicator meet 3:1 on every chrome surface; status text meets 4.5:1 alone and on its own tint. Variants differ from each other by at least 0.03 in OKLab on base and control surfaces.

## Notes

- Paper is warm all the way through: sand surfaces, brown-grey text and brown-tinted hover and selection fills, against the neutral grey of Ink light. It is at least 0.04 away from Ink light in OKLab on base and control surfaces.
- The note page in Paper light is pure white, lighter than the warm chrome, so the page reads as the brightest thing on screen. In Paper dark the page is cream.
- Paper dark has no equivalent in the earlier theme set and is new.
- A user theme pack uses the same overrides, supplied through the inputs in [custom-themes.md](custom-themes.md).
