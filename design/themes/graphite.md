# Graphite

Theme family `graphite`. Variants: `graphite-dark` and `graphite-light`.

## Character

A softer dark: blacks are lifted and carry a little blue, which lowers glare without losing contrast. Graphite light is the cool counterpart to Paper: pale blue-grey surfaces and a blue-black text, softer than Ink light.

## When to use

- People who find near-black harsh or who use the tablet in a moderately lit room.
- Graphite light for people who want a light theme without Paper's warmth.

## Semantic overrides

Each cell is the primitive the token points at, as `ramp.step`, or a tint written as base colour and opacity. These are the only values the theme sets; everything else (type, space, radius, motion, sizes) is shared. The source of truth is `design/tokens/tokens.json`.

| Token | Graphite dark | Graphite light |
|---|---|---|
| `state/drag/target-fill` | `frost at 10%` | `steel at 10%` |
| `state/drag/target-line` | `slate.50` | `slate.900` |
| `state/drag/insertion` | `slate.50` | `slate.900` |
| `state/disabled/fg` | `frost at 34%` | `steel at 34%` |
| `state/disabled/bg` | `frost at 6%` | `steel at 6%` |
| `state/disabled/border` | `frost at 14%` | `steel at 14%` |
| `state/readonly/fg` | `slate.300` | `fog.700` |
| `state/readonly/bg` | `frost at 4%` | `steel at 4%` |
| `state/readonly/border` | `frost at 22%` | `steel at 24%` |
| `state/danger/fg` | `red.300` | `red.700` |
| `state/danger/bg` | `red-500 at 16%` | `red-700 at 8%` |
| `state/danger/bg-hover` | `red-500 at 22%` | `red-700 at 12%` |
| `state/danger/line` | `red-300 at 22%` | `red-700 at 30%` |
| `state/danger/solid` | `red.600` | `red.600` |
| `state/danger/on-solid` | `grey.0` | `grey.0` |
| `state/warning/fg` | `gold.400` | `gold.800` |
| `state/warning/bg` | `gold-400 at 16%` | `gold-800 at 8%` |
| `state/warning/bg-hover` | `gold-400 at 26%` | `gold-800 at 12%` |
| `state/warning/line` | `gold-400 at 22%` | `gold-800 at 30%` |
| `state/success/fg` | `green.300` | `green.700` |
| `state/success/bg` | `green-500 at 20%` | `green-700 at 8%` |
| `state/success/bg-hover` | `green-500 at 30%` | `green-700 at 12%` |
| `state/success/line` | `green-500 at 22%` | `green-700 at 30%` |
| `state/info/fg` | `blue.300` | `blue.700` |
| `state/info/bg` | `blue-500 at 16%` | `blue-700 at 8%` |
| `state/info/bg-hover` | `blue-500 at 26%` | `blue-700 at 12%` |
| `state/info/line` | `blue-500 at 30%` | `blue-700 at 30%` |
| `surface/base` | `slate.900` | `fog.50` |
| `surface/raised` | `slate.850` | `fog.100` |
| `surface/control` | `slate.800` | `fog.200` |
| `surface/control-hover` | `slate.725` | `fog.250` |
| `surface/strong` | `slate.700` | `fog.300` |
| `surface/overlay` | `slate.750` | `fog.25` |
| `surface/sunken` | `slate.950` | `fog.300` |
| `text/default` | `slate.50` | `slate.900` |
| `text/muted` | `slate.300` | `fog.700` |
| `text/subtle` | `slate.400` | `fog.600` |
| `text/disabled` | `frost at 34%` | `steel at 34%` |
| `text/on-action` | `slate.900` | `fog.25` |
| `border/subtle` | `frost at 8%` | `steel at 10%` |
| `border/strong` | `frost at 14%` | `steel at 16%` |
| `border/control` | `frost at 40%` | `steel at 62%` |
| `border/control-hover` | `frost at 70%` | `steel at 85%` |
| `border/focus` | `slate.50` | `slate.900` |
| `border/focus-gap` | `slate.900` | `fog.50` |
| `border/error` | `red.300` | `red.700` |
| `border/warning` | `gold.400` | `gold.800` |
| `action/primary/bg` | `slate.50` | `slate.900` |
| `action/primary/fg` | `slate.900` | `fog.25` |
| `action/primary/bg-hover` | `grey.0` | `slate.950` |
| `action/primary/bg-pressed` | `slate.100` | `fog.700` |
| `action/primary/bg-disabled` | `frost at 12%` | `steel at 12%` |
| `action/primary/fg-disabled` | `frost at 34%` | `steel at 34%` |
| `action/secondary/bg` | `slate.800` | `fog.200` |
| `action/secondary/fg` | `slate.50` | `slate.900` |
| `action/secondary/bg-hover` | `slate.700` | `fog.300` |
| `action/secondary/bg-pressed` | `slate.650` | `fog.400` |
| `action/quiet/bg` | `transparent` | `transparent` |
| `action/quiet/fg` | `slate.50` | `slate.900` |
| `action/quiet/bg-hover` | `frost at 7%` | `steel at 6%` |
| `action/quiet/bg-pressed` | `frost at 12%` | `steel at 12%` |
| `selection/fill` | `frost at 10%` | `steel at 10%` |
| `selection/fill-hover` | `frost at 13%` | `steel at 14%` |
| `selection/indicator` | `slate.50` | `slate.900` |
| `selection/media-ring` | `grey.0` | `grey.0` |
| `selection/media-halo` | `grey.900` | `grey.900` |
| `hover/fill` | `frost at 7%` | `steel at 6%` |
| `press/fill` | `frost at 12%` | `steel at 12%` |
| `canvas/page` | `page.mist` | `page.white` |
| `canvas/ink-default` | `pen.ink` | `pen.ink` |
| `canvas/rule` | `rule at 12%` | `rule at 12%` |
| `canvas/selection-fill` | `ink at 6%` | `ink at 6%` |
| `canvas/handle` | `grey.0` | `grey.0` |
| `canvas/handle-edge` | `grey.900` | `grey.900` |
| `canvas/marquee` | `blue.700` | `blue.700` |
| `canvas/guide` | `blue.500` | `blue.500` |
| `scrim/default` | `black at 62%` | `black at 40%` |
| `scrim/stacked` | `black at 34%` | `black at 22%` |
| `shadow/soft` | `black at 70%` | `black at 16%` |
| `shadow/deep` | `black at 85%` | `black at 26%` |
| `shadow/thumb` | `black at 50%` | `black at 28%` |
| `highlight/match` | `gold-400 at 30%` | `gold-400 at 55%` |

## Contrast

Checked on every build by `build-tokens.mjs`. Lowest ratio of `text/default`, `text/muted` and `text/subtle` on any surface: Graphite dark 4.88:1, Graphite light 4.60:1 (needs 4.5:1). Control edges, the focus ring, error edges and the selection indicator meet 3:1 on every chrome surface; status text meets 4.5:1 alone and on its own tint. Variants differ from each other by at least 0.03 in OKLab on base and control surfaces.

## Notes

- Both variants carry a blue cast in surfaces, text and fills. Graphite light is new; a light variant makes sense because it is the only cool light option next to the warm Paper.
- The note page in Graphite dark is a pale mist grey (`page.mist`), a little softer than white to cut glare against the dark frame.
- Graphite light has the least text headroom of the six variants. Do not darken its surfaces without re-running the build.
- A user theme pack uses the same overrides, supplied through the inputs in [custom-themes.md](custom-themes.md).
