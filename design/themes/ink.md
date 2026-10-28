# Ink

Theme family `ink`. Variants: `ink-dark` and `ink-light`.

## Character

Near-black and neutral, with no hue at all. Chrome is grey and white so the colour that is on screen is the user's: ink, covers, stickers. Ink dark is OLED friendly and is the default. Ink light is its crisp counterpart: white and cool-neutral grey, black text.

## When to use

- Default for everyone until they choose otherwise. With no theme chosen, a dark system gets Ink dark and a light system gets Ink light.
- Ink dark in lecture halls and at night, and on the AMOLED Galaxy Tab where near-black saves power.
- Ink light in bright rooms for people who want neutral rather than warm.

## Semantic overrides

Each cell is the primitive the token points at, as `ramp.step`, or a tint written as base colour and opacity. These are the only values the theme sets; everything else (type, space, radius, motion, sizes) is shared. The source of truth is `design/tokens/tokens.json`.

| Token | Ink dark | Ink light |
|---|---|---|
| `state/drag/target-fill` | `white at 10%` | `ink at 10%` |
| `state/drag/target-line` | `grey.50` | `grey.900` |
| `state/drag/insertion` | `grey.50` | `grey.900` |
| `state/disabled/fg` | `white at 34%` | `ink at 34%` |
| `state/disabled/bg` | `white at 6%` | `ink at 6%` |
| `state/disabled/border` | `white at 14%` | `ink at 14%` |
| `state/readonly/fg` | `grey.300` | `grey.600` |
| `state/readonly/bg` | `white at 4%` | `ink at 4%` |
| `state/readonly/border` | `white at 22%` | `ink at 24%` |
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
| `surface/base` | `grey.900` | `grey.25` |
| `surface/raised` | `grey.850` | `grey.75` |
| `surface/control` | `grey.800` | `grey.100` |
| `surface/control-hover` | `grey.775` | `grey.125` |
| `surface/strong` | `grey.750` | `grey.150` |
| `surface/overlay` | `grey.775` | `grey.0` |
| `surface/sunken` | `grey.950` | `grey.150` |
| `text/default` | `grey.50` | `grey.900` |
| `text/muted` | `grey.300` | `grey.600` |
| `text/subtle` | `grey.400` | `grey.500` |
| `text/disabled` | `white at 34%` | `ink at 34%` |
| `text/on-action` | `grey.900` | `grey.25` |
| `border/subtle` | `white at 8%` | `ink at 10%` |
| `border/strong` | `white at 14%` | `ink at 16%` |
| `border/control` | `white at 40%` | `ink at 62%` |
| `border/control-hover` | `white at 70%` | `ink at 85%` |
| `border/focus` | `grey.50` | `grey.900` |
| `border/focus-gap` | `grey.900` | `grey.25` |
| `border/error` | `red.300` | `red.700` |
| `border/warning` | `gold.400` | `gold.800` |
| `action/primary/bg` | `grey.50` | `grey.900` |
| `action/primary/fg` | `grey.900` | `grey.25` |
| `action/primary/bg-hover` | `grey.0` | `grey.950` |
| `action/primary/bg-pressed` | `grey.150` | `grey.700` |
| `action/primary/bg-disabled` | `white at 12%` | `ink at 12%` |
| `action/primary/fg-disabled` | `white at 34%` | `ink at 34%` |
| `action/secondary/bg` | `grey.800` | `grey.100` |
| `action/secondary/fg` | `grey.50` | `grey.900` |
| `action/secondary/bg-hover` | `grey.750` | `grey.150` |
| `action/secondary/bg-pressed` | `grey.700` | `grey.200` |
| `action/quiet/bg` | `transparent` | `transparent` |
| `action/quiet/fg` | `grey.50` | `grey.900` |
| `action/quiet/bg-hover` | `white at 7%` | `ink at 6%` |
| `action/quiet/bg-pressed` | `white at 12%` | `ink at 12%` |
| `selection/fill` | `white at 10%` | `ink at 10%` |
| `selection/fill-hover` | `white at 13%` | `ink at 14%` |
| `selection/indicator` | `grey.50` | `grey.900` |
| `selection/media-ring` | `grey.0` | `grey.0` |
| `selection/media-halo` | `grey.900` | `grey.900` |
| `hover/fill` | `white at 7%` | `ink at 6%` |
| `press/fill` | `white at 12%` | `ink at 12%` |
| `canvas/page` | `page.warm` | `page.white` |
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

Checked on every build by `build-tokens.mjs`. Lowest ratio of `text/default`, `text/muted` and `text/subtle` on any surface: Ink dark 4.74:1, Ink light 4.65:1 (needs 4.5:1). Control edges, the focus ring, error edges and the selection indicator meet 3:1 on every chrome surface; status text meets 4.5:1 alone and on its own tint. Variants differ from each other by at least 0.03 in OKLab on base and control surfaces.

## Notes

- Ink dark is the default of the semantic group in `tokens.json`, so it has no override block. Every other variant is measured against it.
- The note page in Ink dark is warm paper (`page.warm`) so dark chrome frames a light page. In Ink light the page is white.
- Selection is a soft tint of white (dark) or ink (light), never a hue, plus `selection/indicator`. The accent equals `text/default`.
- A user theme pack uses the same overrides, supplied through the inputs in [custom-themes.md](custom-themes.md).
