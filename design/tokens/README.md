# Design tokens

`tokens.json` is the single source of every colour, size, radius, shadow, timing and layer in Grapho,
in the W3C Design Tokens format (see [Design decisions](../DECISIONS.md#one-token-source-generated-into-every-shell)). `tokens.css`, `generated/GraphoTokens.kt`,
`usage-rules.json` and `../themes/contrast-matrix.md` are generated from it and never edited by hand.

```
node design/tokens/build-tokens.mjs                  # tokens.css, usage-rules.json, themes/contrast-matrix.md
node design/tokens/build-tokens.mjs --check          # CI: fail if any generated file is stale
node design/tokens/build-tokens.mjs --write-vectors  # regenerate themes/vectors.json (rules change only)
node design/tokens/build-kotlin.mjs [--check]        # generated/GraphoTokens.kt for Compose
```

Files: `tokens-model.mjs` (load and validate), `contrast.mjs` (colour maths and the matrix engine),
`report.mjs` (matrix report, usage rules, variant distances), `../themes/derive-theme.mjs` and
`../themes/verify-vectors.mjs` (custom themes). All dependency free. Each build run fails on: an unresolved alias,
a raw hex, dimension or duration outside the primitive tier, a dash character, a theme variant that
omits a themable token, any miss in the contrast matrix, two variants closer than 0.03 in OKLab, or a
custom theme vector that differs from the reference derivation. CI runs both `--check` commands.
Generated files are exempt from the 400 line guideline.

## Three tiers

| Tier | Group | Holds | Read by |
|---|---|---|---|
| 1 Primitive | `primitive` | Raw ramps and scales: `primitive.color.grey.900`, `primitive.space.4`. Only here do hex values live | Semantic and theme definitions. Never a component |
| 2 Semantic | `semantic` | Meaning: `surface/base`, `text/muted`, `size/control/default`. Every value aliases a primitive. Defaults are Ink dark | Components and screens |
| 3 Component | `component` | Values a component needs that no semantic token describes, each with a reason | The one component, and the focus recipe |

Themes are a fourth group, `themes`: each variant maps semantic tokens to primitive aliases.

**Component tokens kept:** `focus/*` (one focus recipe instead of 25 overrides), `wheel/*` (the pen wheel is a
fixed dark instrument over any page, so it does not follow the theme), `toast/*` (the only surface with
its own width and its own timing).

## Naming and the rule

Paths read general to specific: `action/primary/bg-hover` (JSON nesting `semantic.action.primary.bg-hover`).
Roles end in what they paint: `bg`, `fg`, `line`, `fill`, `edge`. States are a suffix: `-hover`, `-pressed`,
`-disabled`. Ramps run light (low) to dark (high). Alpha tints are `alpha.<base>.<percent>`. CSS: `--p-*`
for primitives, tier dropped for the rest (`--surface-base`). Kotlin and other shells: camel case
(`surfaceBase`).

**Components reference only semantic or component tokens.** Never a primitive, never a raw value. A new
colour need is a new semantic token with an override in every variant and a pair in the matrix.
`scripts/check-design.mjs` must fail on `--p-*`, hex, `rgb()` and px literals in component CSS, and on any use
listed in `usage-rules.json`.

## Contrast: the matrix and the usage rules

Every foreground and background that can really occur is declared in `tokens.json` under
`$extensions.grapho.pairs` as groups plus pairs. A pair names the foregrounds, the backgrounds, the host
surfaces that translucent fills sit over, and a requirement: `text` 4.5, `large` 3, `ui` 3, a number, or
`exempt` with a reason. The build checks the whole matrix for every variant (about 290 graded pairs
each) and writes [contrast-matrix.md](../themes/contrast-matrix.md). No token may be added to the system
without a pair, or a stated exemption.

**The pair list is an allow-list.** A foreground may sit only on a host a pair lists for it; anything unlisted is
forbidden by default. The build writes `usage-rules.json` with `allowed` and `forbidden` hosts per foreground
(text roles, `text/on-action`, `action/secondary/fg`, `media/text`, the four status foregrounds). Component specs
and `scripts/check-design.mjs` check every placement against it. The ones that matter:

- `text/subtle` goes on the seven surfaces only. Never on a hover, press, selection, quiet or drop target
  fill, never on a state tint. A row's meta text switches to `text/muted` when the row is hovered or selected.
- `text/default` and `text/muted` go on surfaces and on hover, press, selection, quiet and drop fills over
  base, raised, control, control-hover and overlay. They do not go on those fills over `surface/strong`,
  which is for wells and pressed controls and carries no tinted fills.
- On state tints and `highlight/match` only `text/default`, plus the state's own `fg` on its own tint.
- `state/*/fg` goes on surfaces and on its own tint and hover tint, nowhere else (not on another state's tint).
- `media/text` goes on `media/glass` only. Round buttons over artwork use `media/glass`, not `media/chip`, because
  artwork pixels are unknown.
- `text/on-action` goes on `action/primary/*` only.

**Selection never relies on fill alone.** Every selectable component draws `selection/fill` plus
`selection/indicator` (3:1 on the fill): a check mark, a 3 px leading bar (`border-width/emphasis`) or an
outline. Fill-only selection is a lint error. **Error** is a solid `border/error` edge (3:1) plus the message,
never `state/danger/line`. **Selection on images** (covers, pages, inserted pictures) is
`selection/media-ring` with `selection/media-halo`, a white ring with a dark halo, 3:1 against each other.

## Component states

Each cell is a token or "same as rest".

| State | Fill | Edge | Content |
|---|---|---|---|
| Rest | `surface/control` | `border/control` | `text/default` |
| Hover | `surface/control-hover` or `hover/fill` | `border/control-hover` | same |
| Pressed | `press/fill` | same | same |
| Focus | same | two-tone ring (below) | same |
| Selected | `selection/fill`, `-hover` | `selection/indicator` | same |
| Disabled | `state/disabled/bg` | `state/disabled/border` | `state/disabled/fg` (exempt) |
| Read-only | `state/readonly/bg` | `state/readonly/border`, dashed | `state/readonly/fg` |
| Error | same | `border/error` | `state/danger/fg` message |
| Drag source | same | same | `state/drag/source-opacity` |
| Drop target | `state/drag/target-fill` | `state/drag/target-line` | same |
| Drop position | none | `state/drag/insertion` (line) | none |

**Focus** is two-tone: a `focus/gap/width` (2 px) gap in the surface colour (`focus/gap/color`, set to the
surface the control sits on), then a `focus/ring/width` (2 px) ring in `border/focus`. In CSS:
`box-shadow: 0 0 0 var(--focus-gap-width) var(--focus-gap-color), 0 0 0 calc(var(--focus-gap-width) + var(--focus-ring-width)) var(--focus-ring-color)`;
inside a clipped list use `inset`. The ring contrasts with the gap, and the gap with the control, so it
shows on the page, on stage and on images. **More contrast**: `prefers-contrast: more` or
`data-contrast="more"` steps edges and secondary text up (`contrastMore` extensions) and shells map the
platform setting. **Forced colours**: a `forced-colors` block maps the key tokens to system colours
(`forcedColors` in `tokens.json`); WinUI contrast themes and iOS Increase Contrast do the same natively.
Hover has no equivalent on a finger; a pen that reports hover shows the hover fill, and touch shows only
pressed.

## Themes

Variant ids are `<family>-<mode>`: `ink-dark` (default, held in the semantic group), `ink-light`,
`graphite-dark`, `graphite-light`, `paper-light`, `paper-dark`. A setting stores family and appearance
(system, light, dark); the shell resolves the id. With no theme chosen, Ink follows the system. Each
non-default variant overrides **every** themable token (the build checks). Variants must differ: the
build measures OKLab distance on base and control surfaces and fails below **0.03** (current minimum:
see the table in the matrix report). Paper is warm throughout (sand surfaces, brown-grey text, warm tints);
Graphite is cool and blue tinted; Ink is neutral. User and pack themes follow
[custom-themes.md](../themes/custom-themes.md).

## Sizes and density

**Density is app state, never a media query.** Two values, `touch` and `compact`, set as
`data-density` on the root (CSS) or in the shell's theme object. Touch is the default. A setting in
Appearance, Controls, offers **Automatic** (default), **Touch** and **Compact**.

Automatic resolves to touch when the last primary input was touch or stylus, or when the device has no
fine pointer; otherwise compact:

| Platform | Touch when | Compact when |
|---|---|---|
| Android | Touchscreen present and the last input was touch or S Pen | Desktop windowing with a mouse or trackpad as the last input |
| Windows | Last input was pen or touch (a pen laptop is touch while the pen is in use), or no mouse or trackpad | Last input was mouse or trackpad |
| iPad | Last input was touch or Pencil | Last input was an attached trackpad or mouse |
| macOS | Last input was a Sidecar or tablet pen | Everything else |

It changes only at foreground, after a window move or resize, or once idle for 2 seconds with nothing
touching the screen, and needs three consecutive events of the other kind. Never during a gesture or a
stroke, and never animated. The touch hit slot is **`size/hit/touch`**: 44 on iOS, iPadOS, macOS, Windows and
the web, 48 on Android (Material), expressed as a `platform` extension in `tokens.json`. `tokens.css` defaults to 44 and
`[data-platform="android"]` sets 48; `GraphoTokens.kt` bakes in 48.

| Role token | Touch | Compact |
|---|---|---|
| `size/control/small` | 36 | 28 |
| `size/control/default` | 44 | 36 |
| `size/control/large` | 52 | 44 |
| `size/hit/min` | `size/hit/touch` (44, Android 48) | 24 |
| `size/bar/top` | 52 | 48 |
| `size/bar/note` | 48 | 40 |
| `size/dock/slot` | 52 | 44 |
| `size/sheet/header` | 56 | 48 |
| `size/row/list`, `size/row/menu` | 52, 44 | 44, 36 |
| `size/switch/width`, `/height` | 52, 32 | 40, 24 |
| `size/swatch`, `size/checkbox` | 36, 24 | 28, 20 |
| `size/icon/small`, `default`, `large` | 18, 20, 24 | 16, 18, 20 |

The primitives 28, 36, 44 and 52 are the only control heights. No component uses a primitive.

**Which role for which family.** Default: buttons, selects, fields, tabs, segmented controls, menu
triggers, chips, top bar contents. Small: controls in the note bar and dock, inspector rows of three or
more siblings, token chips. Large: the one primary action of a sheet, dialog or first run screen, never
inside a bar. Tool dock buttons use `size/dock/slot`. Wheel segments and hub are drawn by the wheel's
own geometry with a minimum of `size/hit/min`. Siblings in one row share a role. A segmented control's outer
height is the token. Heights are minimums: text at 200% grows the control and never clips. Retired as
control heights: 20 to 26, 30 to 34, 38, 40 and 56 (the floating label field is replaced by a default
field with the label above).

**Hit areas are reserved in layout.** Every interactive element occupies a layout slot of at least
`size/hit/min` in both dimensions, with the visual (a 36 button, a 24 checkbox) centred in it. Slots never
overlap, so spacing alone guarantees the target; there is no invisible overlap. Per shell:
- **Compose:** `Modifier.minimumInteractiveComponentSize()` with `LocalMinimumInteractiveComponentSize`
  provided from `GraphoMetrics.sizeHitMin`. It enlarges the layout node.
- **SwiftUI:** `.frame(minWidth:minHeight:)` on the control, then `.contentShape(Rectangle())`, so the
  slot is layout, not an overflowing hit shape.
- **WinUI:** `MinWidth` and `MinHeight` on a root element with `Background="Transparent"` so the whole
  slot hit tests; the visual sits inside it.
- **CSS:** `min-inline-size` and `min-block-size: var(--size-hit-min)` on the control, visual centred.
A conformance test per shell measures every interactive element in a story page and fails below the slot.

## Typography

Seven styles, Geist: `display` 34, `title` 22, `heading` 17, `body` 15, `label` 14, `caption` 13 and `micro` 12
(raised from 11). Line height is a multiplier (1.12 to 1.47) with a floor: a line box is never below 1.25 times the size
for Latin, Cyrillic and Greek, and 1.5 for Devanagari, Arabic, Hebrew and Thai. Tracking is in em and applies to
Latin, Cyrillic and Greek only; joining and abugida scripts use 0. `label` is the only label size.
Units: Android uses `sp` for size and tracking, `lineHeight` as a multiple; Apple uses Dynamic Type with
`ScaledMetric`; WinUI scales with the text scale factor. Per platform family lists and script fallbacks sit
in the `platform` extension of `font/sans`. Geist lacks Cyrillic extended, Greek, CJK, Arabic and Hebrew
coverage in some weights, so the fallback chain is part of the contract. Layouts hold at 200%.

## Space, shape, depth

Space `xs` 4 to `5xl` 64 on a 4 grid. `layout/gutter/*` by width class (compact below 600, medium from 600,
expanded from 840). Radius: `key` 6, `item` 10, `surface` 14, `container` 24, `pill` full. Border width:
`hairline` 1, `emphasis` 2, `focus` 2. **Elevation:** surfaces are flat. Floating things use `elevation/*`,
each one or two layers of (offset Y, blur, colour) with no spread, so every toolkit can draw them:

| Token | Layer | Compose | Used by |
|---|---|---|---|
| `float` | 6 / 20 | 8 dp | Floating and selection bars |
| `popover` | 8 / 24 | 8 dp | Menus, popovers (with a `border/subtle` hairline) |
| `sheet` | 12 / 40 | 16 dp | Sheets, dialogs |
| `drag` | 10 / 24 | 12 dp | Dragged item |
| `wheel` | 10 / 24 | 12 dp | Pen wheel |
| `toast` | 6 / 16 | 6 dp | Toasts |
| `thumb` | 1 / 4 | 2 dp | Slider knob, segmented thumb |

Shadow colours are themable. The cost of large blurs on a 120 Hz tablet is unmeasured. See Open items.

## Motion

Durations `press` 90, `fast` 140, `base` 220, `pop` 260, `slow` 340, `exit` 160 ms. Easing `standard`,
`enter` (decelerates, never overshoots: nothing bounces) and `exit`. **Chrome while writing** (system/document.md): chrome recedes over `chrome-recede` (150 ms) when the pen touches the
page and returns over `chrome-return` (200 ms, ease out) after a tap at the top edge or 1.5 s without pen contact
(`delay/pen-idle`). `motion/delay/chrome-return` is retired. The page never reflows, and no other animation starts
while a stroke is in progress. **Reduced motion** (`prefers-reduced-motion`, or the
platform setting: Android animator scale, Windows `UISettings.AnimationsEnabled`, Apple
`accessibilityReduceMotion`): every duration and delay becomes 0 (CSS 0.01 ms so end states apply), state
shows by fill and colour, and progress still updates without sliding. **Toasts** are reading time, not
motion: a toast stays at least its token duration or the system accessibility timeout, whichever is
longer, and never dismisses while it has focus or a pointer on it. Its width is `min(width-max, viewport
minus two gutters)`.

## Layers

| Token | Holds |
|---|---|
| `z/canvas` 0 | The page |
| `z/chrome` 10 | Top bar, dock, rails, banners |
| `z/floating-bar` 20 | Floating note and selection bars |
| `z/wheel` 25 | Pen wheel, above dock and bars |
| `z/panel` 30 | Panels beside or over the page |
| `z/drawer` 35 | Compact width sidebar drawer |
| `z/scrim` 40 | Scrim behind drawer, sheet, dialog (`scrim/default`; a dialog over a sheet uses `scrim/stacked`) |
| `z/sheet` 45, `z/dialog` 50 | Sheets, dialogs |
| `z/popover` 60 | Menus, popovers: above dialogs, since they open from inside them |
| `z/palette` 65 | Command search, over everything modal |
| `z/tooltip` 70, `z/toast` 80, `z/drag-ghost` 90 | Tooltips, toasts, the dragged item |

The integers are for the web. Native shells do not use OS windows for layered chrome (Compose `Dialog`
and `Popup`, SwiftUI sheets, WinUI popups all order by creation): each shell has **one overlay host** at
the window root that draws these layers in this order. OS pickers (files, share) are the only exceptions
and are never layered with in-app chrome. Section 3 of `design/SYSTEM-MAP.md` does not yet match
this table (see Open items).

## Icons

Sizes follow control roles (table above), stroke is optical per size (`icon/stroke/px16` to `px32`,
in 24 grid units). See [../icons/README.md](../icons/README.md).

## How the shells consume it

`build-kotlin.mjs` is the Compose generator: `GraphoColors` per variant (`GraphoThemes.all`), `GraphoContent`,
`GraphoTouch` and `GraphoCompact` metrics, `GraphoSpace`, `GraphoMotion`, `GraphoLayer`, `GraphoType`. The
Android shell compiles its own copy of the file in `ui/theme/` (the generator writes both and `--check`
verifies both), and a snapshot test in `android/app/src/test` compares it to the computed `tokens.css` values. WinUI
(a `ResourceDictionary` per variant, brush keys by the shell name) and SwiftUI (a `GraphoTheme` struct in
the environment) generators follow the same shape. Colour keeps alpha as alpha; density is two metric
objects; type is size, line height multiple, weight and tracking in em; durations are integers.
`$extensions.grapho` keys (`compact`, `reducedMotion`, `contrastMore`, `pairs`, `platform`) are vendor
extensions, which the format allows and generic transformers ignore, so these generators are the supported
path rather than Style Dictionary.

## Studio tokens

The old `--st-*` set (about 70 tokens) belongs to Studio screens that have not yet moved to the semantic tier. The
generic ones are already in the semantic tier: `canvas/handle`, `handle-edge`, `marquee`, `guide`,
`selection/media-ring`, `media/veil`, `media/crop-dim`, `state/drag/*`. The rest (panel widths, art
surfaces, scan effects, checker patterns) stay unmapped and will become `component/studio/*` tokens, each
with a reason, in the Studio work; they are layout and artwork of one set of screens, not system meaning.
They are removed from `tokens.css` here, so Studio keeps reading the old file until that work is done.

## Legacy token names

Legacy `tokens.css` names and their replacements.

| Old | New |
|---|---|
| `--bg`, `--s1`, `--s2`, `--s2-hover`, `--s3`, `--overlay`, `--stage` | `surface/base`, `raised`, `control`, `control-hover`, `strong`, `overlay`, `sunken` |
| `--paper`, `--paper-ink`, `--paper-rule`, `--paper-sel` | `canvas/page`, `ink-default`, `rule`, `selection-fill` |
| `--scrim` | `scrim/default` |
| `--fg`, `--mute`, `--dim`, `--on-fg`, `--off` | `text/default`, `muted`, `subtle` (restricted), `on-action`, `disabled` |
| `--solid-hover`, `--solid-press`, `--solid-off`, `--on-solid-off` | `action/primary/bg-hover`, `bg-pressed`, `bg-disabled`, `fg-disabled` |
| `--tint` | none; use the fills |
| `--accent` | `border/focus` (rings), `action/primary/bg` (on states) |
| `--line`, `--line2`, `--dot-edge` | `border/subtle`, `border/strong` |
| `--hl` | none, retired |
| `--hover`, `--hover-fill`, `--press`, `--press2` | `hover/fill`, `press/fill` |
| `--sel`, `--sel-fill`, `--sel-row`, `--sel-fill-hover` | `selection/fill`, `selection/fill-hover` (+ `selection/indicator`) |
| `--box-edge`, `--box-edge-hover` | `border/control`, `border/control-hover` |
| `--danger*`, `--ok*`, `--*-line` | `state/danger/*`, `state/success/*`, `state/warning/*`, `state/info/*` |
| `--tip` | `action/primary/bg` |
| `--sp-1` to `--sp-16`, `--gutter` | `space/xs` to `5xl`, `layout/gutter/*` |
| `--r-xs` to `--r-full` | `radius/key`, `item`, `surface`, `container`, `pill` |
| `--hit`, `--ctl`, `--bar`, `--bar-h`, `--pill-h`, `--row`, `--row-menu` | `size/hit/min`, `control/default`, `bar/top`, `bar/note`, `bar/note`, `row/list`, `row/menu` |
| `--icon-sm`, `--icon`, `--icon-lg` | `size/icon/small`, `default`, `large` |
| `--e1`, `--e2`, `--e3` | `elevation/float`, `popover`, `sheet` |
| `--ease`, `--spring`, `--ease-in` | `motion/easing/standard`, `enter`, `exit` |
| `--d-*` | `motion/duration/*`; `--d-toast*` to `toast/duration-*` |
| `--z-sheet`, `--z-dialog`, `--z-menu`, `--z-toast`, `--z-tip`, `--z-bar` | `z/sheet`, `dialog`, `popover`, `toast`, `tooltip`, `floating-bar` |
| `--sans`, `--hand`, `--t-*`, `--track-*` | `font/sans`, `font/hand`, `type/*` (`label-sm`, `label-lg` become `label`) |
| `--tail`, `--pink`..`--lilac`, `--f-*`, `--page-*` | `brand/*`, `content/folder/*`, `content/paper/*` |
| `--wh-ring` etc. | `wheel/*` |
| `--mark`, `--on-art`, `--on-media*`, `--art-edge` | `highlight/match`, `media/text`, `media/chip*`, `media/edge` |

## Decisions

1. Popover above dialog above sheet; palette above popover; wheel, drawer and scrim layers added; one
   overlay host per native shell.
2. Density is an app state (`touch`, `compact`, setting Automatic, Touch, Compact), not `pointer: coarse`;
   hit areas are reserved layout slots, not invisible overlap; Android hit minimum is 48 via `size/hit/touch`.
3. Roles are `small`, `default`, `large`; the 28/36/44/52 ladder is primitives only.
4. Selection is fill plus indicator; errors use a solid 3:1 edge; focus is two-tone.
5. The contrast matrix is declared data in `tokens.json`; tokens not guaranteed on a host may not be used
   there, and the build writes and enforces that list.
6. Accent drives only the primary action; the focus ring is `text/default`.
7. Paper is warm throughout; variants must be 0.03 apart in OKLab.
8. Chrome chroma is capped at 0.04 OKLCH in custom themes; overrides are guarded like inputs; rules
   versions are frozen; fallback is the theme's own scheme variant.
9. Motion never overshoots; chrome fades with opacity only around pen contact.
10. Elevation has no spread; separate hairline.
11. Tracking is em and Latin, Cyrillic, Greek only; line height is a multiplier with a floor; `micro` is 12.
12. Optical stroke per icon size; no universal fill layer.
13. `text/subtle` is restricted to surfaces; `--dim` renamed. The wheel stays a fixed dark instrument.
14. Dimensions are strings, durations are `ms`, tracking is `em`.
15. The default page stays a light paper in every theme.
16. Built-in themes are reference themes, not derived; their values are written as aliases for readability.

## Component tokens

Defined for toggles, progress, loading and toggled icon buttons, in every theme variant with allow-list pairs:
- `switch/track-off`, `thumb-off`, `track-on`, `thumb-on` (component tier, aliases of semantic tokens). The on
  track is a tint of the text colour (`border/control-hover`), not the accent and not `action/primary`. The thumb
  reaches 3:1 on its track in both states and the on track reaches 3:1 on every chrome surface.
- `state/danger/bg-pressed`, with `text/default` and `state/danger/fg` allowed on it (4.5:1).
- `progress/track` and `progress/fill`: the fill reaches 3:1 on the track and on every chrome surface.
- `skeleton/fill`: decorative, exempt.
- `action/toggle/fg-on`: toggled-on icon colour, 4.5:1 on `selection/fill` and `-hover`. The "icon colour never
  changes" rule is dropped.
- `action/quiet/fg` now has pairs on every surface and on hover, press and selection fills.
Custom themes derive the same tokens (the danger pressed tint from the derived danger colour, the switch tokens from
the derived surfaces).

## Input, feedback and action tokens

Sizes, timings and pairs from the [inputs](../components/inputs/README.md), [feedback](../components/feedback/README.md)
and [actions](../components/actions/README.md) component READMEs:
- Sizes: `size/track` 4, `size/thumb` (24 touch, 20 compact), `size/tick` 8, `size/keycap` (24, 20), `size/dot` 8,
  `size/popover/width` 360, `size/popover/max-height` 480, `size/tooltip/max-width` 280 and `layout/measure/empty` 320.
- Timing: `feedback/delay/*`, `hold/*`, `timeout/load`, `merge-window`, `recent-max` and `delay/pen-idle`, with the
  values given in the feedback README.
- `action/glass/bg-pressed` (black at 78%); `media/text` is allowed on it as well as on `media/glass`.
- Tag colours: `tag/<red|orange|yellow|green|teal|blue|violet|pink>/fg` and `/bg`, themable, chrome tints for tag
  chips and not user ink colours. Each fg passes 4.5:1 on its own tint over every fill host in every variant. Custom
  themes derive them with the status colour search.
- Pairs: `action/secondary/fg` on every `state/*/bg`; `selection/indicator` on all six chrome surfaces (Slider fill, Radio
  dot); `state/readonly/border` is now a graded 3:1 edge (it is the only boundary of a read-only field) and
  `text/muted` on `state/readonly/bg`.
- The fine pointer hit minimum is 24 (WCAG 2.5.8), the compact value of `size/hit/min`.

## Menu, bar and sheet tokens

Values for menus, bars and sheets: `size/menu/min-width` 200, `max-width` 320,
`size/palette/width` 560 and `max-height` 480, `size/popover/width-narrow` 280, `feedback/delay/typeahead` 800 ms;
`size/sidebar/width` 280, `size/panel/width` 360, `width-over` 320, `min` 320, `max` 480, `size/page/min` 200 and
`min-width` 400, `size/bar/note-max` 720, `size/edge-guard` 16, `layout/breakpoint/short` 500, `delay/palm-window`
1000 ms, `delay/hover-recall` 500 ms, `input/tap/*` (250 ms, 10), `input/chrome-tap/*` (300 ms, 8),
`input/palm-threshold` 36 (set per platform by the shell); `size/sheet/width/default` 560, `wide` 760,
`size/sheet/list` 200, `size/dialog/width` 400. `size/row/menu` gets the Android value 48 through its `platform` extension.
Pairs: `selection/indicator` (3:1) and `state/danger/fg` (4.5:1) on `hover/fill` and `press/fill` over
`surface/overlay`; `state/disabled/fg` exempt on the selection fills; `text/muted` on `surface/overlay` for the
container edge under more contrast (`border/strong` resolves to `text/muted` there). `media/edge` cannot reach 3:1
on `media/glass` (it is a 12% white cover edge, still used by `--art-edge`), so the glass press ring is the new
`media/ring` (white at 70%), graded 3:1 on the glass fills.

## Open items

- The `sheet`, `drag` and `wheel` elevations are to be measured on a Galaxy Tab at 120 Hz.
- The Windows and Apple generators are to be written.
- The Lucide provenance of the existing path set is to be checked.
- Section 3 of `SYSTEM-MAP.md` does not yet match the layer table above.
- `check-design.mjs` is to read `usage-rules.json`.

## Translucent fills on strong surfaces

Translucent fills (hover, press, selection and the status tints) never sit on `surface/strong`.
That surface is an inner well or the pressed step of a control, and the pressed step replaces the
hover fill rather than stacking under it. Some of these pairs measured 4.26 to 4.49 when composited on
`surface/strong`. This rule makes them impossible rather than tuning colours for a combination that does
not occur. The component lint checks it.

## Picker, tile and canvas tokens

Tokens: `size/preview/*`, `size/tile/*`, `ratio/cover`, `size/art/row`, `feedback/long-list`, `size/wheel/*` (Android 48 map), pen and snap delays, scrubber and minimap sizes, `canvas/minimap/*`, `canvas/placeholder-*`, `media/check-bg`, `media/check-fg`, `media/halo`, `media/cover-neutral(-fg)` and the `wheel/*` state tokens.

- The check badge uses `selection/indicator` with `text/on-action` (ui 3 pair). The halo is the host surface colour and is declared exempt-decorative.
- Retired: `wheel/well`, `wheel/grip`, `wheel/scroll-track` (legacy aliases dropped).
- `action/secondary/bg` is stronger in the dark themes, with hover and pressed steps above it.
