# Grapho theme contrast report

Computed from `design/tokens/tokens.json` by an independent script that resolves the aliases itself. It does not use `build-tokens.mjs`, and the script is not part of the repository. Six variants are checked: Ink dark (semantic defaults plus its two dark overrides), Ink light, Graphite dark, Graphite light, Paper dark and Paper light.

## Method

- **Colour values.** Primitive and semantic aliases are resolved. A theme variant's override wins, otherwise the semantic default (Ink dark) is used. Every theme-varied token is overridden in every light and dark variant. The only tokens taken from the shared value are brand, folder, paper, media and ink-on-dark colours, which the themes group does not vary.
- **Luminance.** WCAG 2.2 relative luminance: each sRGB channel is linearised with the standard curve (threshold 0.03928), `Y = 0.2126 R + 0.7152 G + 0.0722 B`, and the ratio is `(Lighter + 0.05) / (Darker + 0.05)`. Black on white gives 21.00 and `#767676` on white gives 4.54, as expected.
- **Alpha and mixes.** Colours with an alpha byte (every `alpha.*` primitive, such as `white.14` or `ink.16`) are composited source-over in sRGB, unrounded, onto the surface they sit on. An alpha surface is composited over each chrome surface (`surface/base` to `surface/sunken`), and the reported value is the worst case with the host named in the table. `canvas/selection-fill` is composited over `canvas/page` only. Values are not rounded to 8 bits before measuring. Browsers round, which moves a ratio by less than about 0.01.
- **Text (4.5:1).** `text/default`, `text/muted`, `text/subtle`, `text/on-action`, `action/primary/fg`, `action/secondary/fg`, `action/quiet/fg`, `state/*/fg`, `state/danger/on-solid` and `media/text`. The state foregrounds are graded at 4.5:1 because they carry labels, which is the stricter reading. Icon-only use could meet 3:1, and that is not assumed here.
- **UI (3:1).** `border/control`, `border/control-hover`, `border/focus` (WCAG 1.4.11 and the focus appearance criteria), `border/strong`, `action/primary/bg`, `action/secondary/bg`, `state/danger/solid` (as a fill), `canvas/ink-default` (pen ink on the page) and `content/ink-on-dark`. `border/strong` is graded because the popover and sheet elevations draw it as a 1px outline. It is the only edge there apart from a soft shadow.
- **Exempt (not graded).** `text/disabled`, `action/primary/fg-disabled` and `action/primary/bg-disabled`. WCAG 1.4.3 exempts inactive and disabled components. They are computed and shown.
- **Decorative (not graded).** `border/subtle`, `canvas/rule` and the four `state/*/line` tokens. These carry no required information, so no minimum applies. They are computed and shown.
- **Not checked.** `brand/*` (logos are exempt), folder colours and elevation shadows (illustrations and effects, not foregrounds), the scrim, and the `component/wheel/*` tokens (a fixed dark component tier, not semantic).
- **Role pairs and cross pairs.** A role pair is a surface the subject is designed to sit on. Chrome text is graded on chrome surfaces, their hover, selection and highlight overlays, and state banners. Chrome text and controls are not graded on `canvas/page`, which is the note paper, not chrome. Primary and secondary foregrounds are graded on their own fills, and borders and fills on the chrome surfaces. Every other subject against every other surface is a cross pair. It is computed and shown in parentheses in the matrices but not counted as a pass or fail.
- **Media.** `media/text` sits on photo thumbnails, which are not a semantic surface. The chips and glass are checked over the chrome surfaces as a proxy. Read those ratios as a warning, not a measurement of a real photo.
- **Counts per variant.** 220 graded role pairs, 30 exempt, 51 decorative and 935 cross pairs, 1236 in total. Across the six variants that is 7416 pairs.

## Summary per theme

| Variant | Graded role pairs | Pass | Fail | Disabled (exempt) | Decorative (not graded) | Cross pairs (not graded) | Cross pairs below their threshold |
|---|---:|---:|---:|---:|---:|---:|---:|
| ink/dark | 220 | 178 | 42 | 30 | 51 | 935 | 306 |
| ink/light | 220 | 157 | 63 | 30 | 51 | 935 | 378 |
| graphite/dark | 220 | 162 | 58 | 30 | 51 | 935 | 325 |
| graphite/light | 220 | 156 | 64 | 30 | 51 | 935 | 386 |
| paper/dark | 220 | 165 | 55 | 30 | 51 | 935 | 320 |
| paper/light | 220 | 171 | 49 | 30 | 51 | 935 | 366 |

## Every FAIL

### ink/dark (42 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/muted` (text) | `selection/fill-hover` | `surface/strong` | 4.13:1 | 4.5:1 |
| `text/muted` (text) | `press/fill` | `surface/strong` | 3.70:1 | 4.5:1 |
| `text/muted` (text) | `highlight/match` | `surface/strong` | 3.13:1 | 4.5:1 |
| `text/muted` (text) | `state/warning/bg-hover` | `surface/strong` | 3.51:1 | 4.5:1 |
| `text/muted` (text) | `state/success/bg-hover` | `surface/strong` | 3.90:1 | 4.5:1 |
| `text/muted` (text) | `state/info/bg-hover` | `surface/strong` | 4.47:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/strong` | 3.85:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/strong` | 3.25:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/strong` | 3.25:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/strong` | 2.95:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/strong` | 3.85:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/strong` | 2.65:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/strong` | 2.24:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/strong` | 4.08:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/strong` | 3.81:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/strong` | 3.26:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/strong` | 2.51:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/strong` | 3.40:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/strong` | 2.79:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/strong` | 3.76:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/strong` | 3.20:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/strong` | 4.31:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/strong` | 4.31:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/strong` | 3.92:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/strong` | 3.51:1 | 4.5:1 |
| `state/danger/fg` (text) | `highlight/match` | `surface/strong` | 2.97:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/strong` | 4.11:1 | 4.5:1 |
| `state/info/fg` (text) | `highlight/match` | `surface/strong` | 3.48:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.41:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.49:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.53:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.55:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.56:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.55:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.35:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.13:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.06:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.05:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.11:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.05:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.19:1 | 3.0:1 |

### ink/light (63 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/muted` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.46:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill-hover` | `surface/sunken` | 4.28:1 | 4.5:1 |
| `text/muted` (text) | `press/fill` | `surface/sunken` | 4.28:1 | 4.5:1 |
| `text/subtle` (text) | `surface/sunken` | n/a | 4.26:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/sunken` | 3.81:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.36:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/sunken` | 3.49:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/sunken` | 3.22:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/sunken` | 3.81:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/sunken` | 3.22:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/sunken` | 4.11:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/sunken` | 3.78:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/sunken` | 3.54:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/sunken` | 3.83:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/sunken` | 3.60:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/sunken` | 3.84:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/sunken` | 3.62:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/sunken` | 3.84:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/sunken` | 3.62:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.49:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.96:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/sunken` | 4.12:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.80:1 | 4.5:1 |
| `state/danger/fg` (text) | `hover/fill` | `surface/sunken` | 4.49:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/sunken` | 3.80:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg` | `surface/sunken` | 4.46:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg-hover` | `surface/sunken` | 4.17:1 | 4.5:1 |
| `state/warning/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.20:1 | 4.5:1 |
| `state/warning/fg` (text) | `selection/fill` | `surface/sunken` | 4.37:1 | 4.5:1 |
| `state/warning/fg` (text) | `selection/fill-hover` | `surface/sunken` | 4.04:1 | 4.5:1 |
| `state/warning/fg` (text) | `press/fill` | `surface/sunken` | 4.04:1 | 4.5:1 |
| `state/success/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.38:1 | 4.5:1 |
| `state/success/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.87:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill` | `surface/sunken` | 4.02:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.71:1 | 4.5:1 |
| `state/success/fg` (text) | `hover/fill` | `surface/sunken` | 4.38:1 | 4.5:1 |
| `state/success/fg` (text) | `press/fill` | `surface/sunken` | 3.71:1 | 4.5:1 |
| `state/success/fg` (text) | `state/success/bg` | `surface/sunken` | 4.42:1 | 4.5:1 |
| `state/success/fg` (text) | `state/success/bg-hover` | `surface/sunken` | 4.16:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.34:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.83:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill` | `surface/sunken` | 3.98:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.68:1 | 4.5:1 |
| `state/info/fg` (text) | `hover/fill` | `surface/sunken` | 4.34:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/sunken` | 3.68:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg` | `surface/sunken` | 4.38:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg-hover` | `surface/sunken` | 4.13:1 | 4.5:1 |
| `media/text` (text) | `media/chip` | `surface/overlay` | 1.89:1 | 4.5:1 |
| `media/text` (text) | `media/chip-hover` | `surface/overlay` | 3.11:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.38:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.41:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.38:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.17:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.09:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.08:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.16:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.22:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.27:1 | 3.0:1 |

### graphite/dark (58 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/muted` (text) | `action/quiet/bg-pressed` | `surface/strong` | 4.31:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill` | `surface/strong` | 4.31:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill-hover` | `surface/strong` | 3.94:1 | 4.5:1 |
| `text/muted` (text) | `press/fill` | `surface/strong` | 3.56:1 | 4.5:1 |
| `text/muted` (text) | `highlight/match` | `surface/strong` | 3.10:1 | 4.5:1 |
| `text/muted` (text) | `state/warning/bg` | `surface/strong` | 4.37:1 | 4.5:1 |
| `text/muted` (text) | `state/warning/bg-hover` | `surface/strong` | 3.44:1 | 4.5:1 |
| `text/muted` (text) | `state/success/bg-hover` | `surface/strong` | 3.85:1 | 4.5:1 |
| `text/muted` (text) | `state/info/bg-hover` | `surface/strong` | 4.37:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/strong` | 3.70:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/strong` | 3.14:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/strong` | 3.14:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/strong` | 2.87:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/strong` | 3.70:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/strong` | 2.59:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/strong` | 2.25:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/strong` | 4.04:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/strong` | 3.81:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/strong` | 3.17:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/strong` | 2.50:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/strong` | 3.34:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/strong` | 2.80:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/strong` | 3.68:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/strong` | 3.18:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-hover` | `surface/strong` | 4.26:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/strong` | 3.61:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/strong` | 3.61:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/strong` | 3.30:1 | 4.5:1 |
| `state/danger/fg` (text) | `hover/fill` | `surface/strong` | 4.26:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/strong` | 2.97:1 | 4.5:1 |
| `state/danger/fg` (text) | `highlight/match` | `surface/strong` | 2.59:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg-hover` | `surface/strong` | 4.37:1 | 4.5:1 |
| `state/warning/fg` (text) | `highlight/match` | `surface/strong` | 4.06:1 | 4.5:1 |
| `state/success/fg` (text) | `highlight/match` | `surface/strong` | 4.12:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-pressed` | `surface/strong` | 4.22:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill` | `surface/strong` | 4.22:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill-hover` | `surface/strong` | 3.86:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/strong` | 3.48:1 | 4.5:1 |
| `state/info/fg` (text) | `highlight/match` | `surface/strong` | 3.03:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg-hover` | `surface/strong` | 4.28:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.51:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.54:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.46:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.19:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.11:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.08:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.15:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.06:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.26:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/control` | n/a | 2.98:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/control-hover` | n/a | 2.76:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/strong` | n/a | 2.60:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/overlay` | n/a | 2.83:1 | 3.0:1 |

### graphite/light (64 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/muted` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.37:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill-hover` | `surface/sunken` | 4.19:1 | 4.5:1 |
| `text/muted` (text) | `press/fill` | `surface/sunken` | 4.19:1 | 4.5:1 |
| `text/subtle` (text) | `surface/sunken` | n/a | 4.17:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/sunken` | 3.72:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.29:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/sunken` | 3.42:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/sunken` | 3.16:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/sunken` | 3.72:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/sunken` | 3.16:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/sunken` | 4.06:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/sunken` | 3.70:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/sunken` | 3.47:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/sunken` | 3.75:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/sunken` | 3.53:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/sunken` | 3.76:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/sunken` | 3.54:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/sunken` | 3.76:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/sunken` | 3.55:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.39:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.88:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/sunken` | 4.03:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.72:1 | 4.5:1 |
| `state/danger/fg` (text) | `hover/fill` | `surface/sunken` | 4.39:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/sunken` | 3.72:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg` | `surface/sunken` | 4.37:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg-hover` | `surface/sunken` | 4.09:1 | 4.5:1 |
| `state/warning/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.11:1 | 4.5:1 |
| `state/warning/fg` (text) | `selection/fill` | `surface/sunken` | 4.28:1 | 4.5:1 |
| `state/warning/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.95:1 | 4.5:1 |
| `state/warning/fg` (text) | `press/fill` | `surface/sunken` | 3.95:1 | 4.5:1 |
| `state/warning/fg` (text) | `state/warning/bg-hover` | `surface/sunken` | 4.41:1 | 4.5:1 |
| `state/success/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.29:1 | 4.5:1 |
| `state/success/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.78:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill` | `surface/sunken` | 3.94:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.64:1 | 4.5:1 |
| `state/success/fg` (text) | `hover/fill` | `surface/sunken` | 4.29:1 | 4.5:1 |
| `state/success/fg` (text) | `press/fill` | `surface/sunken` | 3.64:1 | 4.5:1 |
| `state/success/fg` (text) | `state/success/bg` | `surface/sunken` | 4.33:1 | 4.5:1 |
| `state/success/fg` (text) | `state/success/bg-hover` | `surface/sunken` | 4.08:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.25:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.75:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill` | `surface/sunken` | 3.90:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.60:1 | 4.5:1 |
| `state/info/fg` (text) | `hover/fill` | `surface/sunken` | 4.25:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/sunken` | 3.60:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg` | `surface/sunken` | 4.29:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg-hover` | `surface/sunken` | 4.05:1 | 4.5:1 |
| `media/text` (text) | `media/chip` | `surface/overlay` | 1.89:1 | 4.5:1 |
| `media/text` (text) | `media/chip-hover` | `surface/overlay` | 3.11:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.38:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.41:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.37:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.17:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.09:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.08:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.14:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.28:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.24:1 | 3.0:1 |

### paper/dark (55 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/muted` (text) | `action/quiet/bg-pressed` | `surface/strong` | 4.34:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill` | `surface/strong` | 4.34:1 | 4.5:1 |
| `text/muted` (text) | `selection/fill-hover` | `surface/strong` | 3.96:1 | 4.5:1 |
| `text/muted` (text) | `press/fill` | `surface/strong` | 3.57:1 | 4.5:1 |
| `text/muted` (text) | `highlight/match` | `surface/strong` | 3.06:1 | 4.5:1 |
| `text/muted` (text) | `state/warning/bg` | `surface/strong` | 4.36:1 | 4.5:1 |
| `text/muted` (text) | `state/warning/bg-hover` | `surface/strong` | 3.41:1 | 4.5:1 |
| `text/muted` (text) | `state/success/bg-hover` | `surface/strong` | 3.84:1 | 4.5:1 |
| `text/muted` (text) | `state/info/bg-hover` | `surface/strong` | 4.39:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/strong` | 3.90:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/strong` | 3.30:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/strong` | 3.30:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/strong` | 3.01:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/strong` | 3.90:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/strong` | 2.71:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/strong` | 2.32:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/strong` | 4.18:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/strong` | 3.93:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/strong` | 3.31:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/strong` | 2.59:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/strong` | 3.50:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/strong` | 2.92:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/strong` | 3.88:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/strong` | 3.34:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-hover` | `surface/strong` | 4.50:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/strong` | 3.80:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/strong` | 3.80:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/strong` | 3.47:1 | 4.5:1 |
| `state/danger/fg` (text) | `hover/fill` | `surface/strong` | 4.50:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/strong` | 3.13:1 | 4.5:1 |
| `state/danger/fg` (text) | `highlight/match` | `surface/strong` | 2.68:1 | 4.5:1 |
| `state/warning/fg` (text) | `highlight/match` | `surface/strong` | 4.20:1 | 4.5:1 |
| `state/success/fg` (text) | `highlight/match` | `surface/strong` | 4.26:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-pressed` | `surface/strong` | 4.46:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill` | `surface/strong` | 4.46:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill-hover` | `surface/strong` | 4.07:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/strong` | 3.66:1 | 4.5:1 |
| `state/info/fg` (text) | `highlight/match` | `surface/strong` | 3.14:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.49:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.53:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.56:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.57:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.44:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.17:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.09:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.06:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.12:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.06:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.23:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/control-hover` | n/a | 2.91:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/strong` | n/a | 2.75:1 | 3.0:1 |
| `state/danger/solid` (ui) | `surface/overlay` | n/a | 2.91:1 | 3.0:1 |

### paper/light (49 fails)

| Subject (kind) | Surface | Worst host (alpha surfaces) | Ratio | Needed |
|---|---|---|---:|---:|
| `text/subtle` (text) | `action/quiet/bg-hover` | `surface/sunken` | 4.02:1 | 4.5:1 |
| `text/subtle` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 3.54:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill` | `surface/sunken` | 3.69:1 | 4.5:1 |
| `text/subtle` (text) | `selection/fill-hover` | `surface/sunken` | 3.40:1 | 4.5:1 |
| `text/subtle` (text) | `hover/fill` | `surface/sunken` | 4.02:1 | 4.5:1 |
| `text/subtle` (text) | `press/fill` | `surface/sunken` | 3.40:1 | 4.5:1 |
| `text/subtle` (text) | `highlight/match` | `surface/sunken` | 4.22:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg` | `surface/sunken` | 3.99:1 | 4.5:1 |
| `text/subtle` (text) | `state/danger/bg-hover` | `surface/sunken` | 3.73:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg` | `surface/sunken` | 4.04:1 | 4.5:1 |
| `text/subtle` (text) | `state/warning/bg-hover` | `surface/sunken` | 3.79:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg` | `surface/sunken` | 4.05:1 | 4.5:1 |
| `text/subtle` (text) | `state/success/bg-hover` | `surface/sunken` | 3.81:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg` | `surface/sunken` | 4.05:1 | 4.5:1 |
| `text/subtle` (text) | `state/info/bg-hover` | `surface/sunken` | 3.81:1 | 4.5:1 |
| `state/danger/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.17:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill` | `surface/sunken` | 4.35:1 | 4.5:1 |
| `state/danger/fg` (text) | `selection/fill-hover` | `surface/sunken` | 4.01:1 | 4.5:1 |
| `state/danger/fg` (text) | `press/fill` | `surface/sunken` | 4.01:1 | 4.5:1 |
| `state/danger/fg` (text) | `state/danger/bg-hover` | `surface/sunken` | 4.40:1 | 4.5:1 |
| `state/warning/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.43:1 | 4.5:1 |
| `state/warning/fg` (text) | `selection/fill-hover` | `surface/sunken` | 4.25:1 | 4.5:1 |
| `state/warning/fg` (text) | `press/fill` | `surface/sunken` | 4.25:1 | 4.5:1 |
| `state/success/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.08:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill` | `surface/sunken` | 4.24:1 | 4.5:1 |
| `state/success/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.91:1 | 4.5:1 |
| `state/success/fg` (text) | `press/fill` | `surface/sunken` | 3.91:1 | 4.5:1 |
| `state/success/fg` (text) | `state/success/bg-hover` | `surface/sunken` | 4.38:1 | 4.5:1 |
| `state/info/fg` (text) | `action/quiet/bg-pressed` | `surface/sunken` | 4.04:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill` | `surface/sunken` | 4.20:1 | 4.5:1 |
| `state/info/fg` (text) | `selection/fill-hover` | `surface/sunken` | 3.88:1 | 4.5:1 |
| `state/info/fg` (text) | `press/fill` | `surface/sunken` | 3.88:1 | 4.5:1 |
| `state/info/fg` (text) | `state/info/bg-hover` | `surface/sunken` | 4.35:1 | 4.5:1 |
| `media/text` (text) | `media/chip` | `surface/overlay` | 1.89:1 | 4.5:1 |
| `media/text` (text) | `media/chip-hover` | `surface/overlay` | 3.11:1 | 4.5:1 |
| `border/strong` (ui) | `surface/base` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/raised` | n/a | 1.40:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/control-hover` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/strong` | n/a | 1.39:1 | 3.0:1 |
| `border/strong` (ui) | `surface/overlay` | n/a | 1.41:1 | 3.0:1 |
| `border/strong` (ui) | `surface/sunken` | n/a | 1.38:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/base` | n/a | 1.17:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/raised` | n/a | 1.10:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control` | n/a | 1.00:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/control-hover` | n/a | 1.07:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/strong` | n/a | 1.12:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/overlay` | n/a | 1.22:1 | 3.0:1 |
| `action/secondary/bg` (ui) | `surface/sunken` | n/a | 1.20:1 | 3.0:1 |

## Column legend

| Id | Surface token |
|---|---|
| C1 | `surface/base` |
| C2 | `surface/raised` |
| C3 | `surface/control` |
| C4 | `surface/control-hover` |
| C5 | `surface/strong` |
| C6 | `surface/overlay` |
| C7 | `surface/sunken` |
| C8 | `canvas/page` |
| C9 | `content/paper/white` |
| C10 | `content/paper/cream` |
| C11 | `content/paper/slate` |
| C12 | `content/paper/dark` |
| C13 | `content/paper/black` |
| C14 | `action/primary/bg` |
| C15 | `action/primary/bg-hover` |
| C16 | `action/primary/bg-pressed` |
| C17 | `action/secondary/bg` |
| C18 | `action/secondary/bg-hover` |
| C19 | `action/secondary/bg-pressed` |
| C20 | `state/danger/solid` |
| C21 | `action/primary/bg-disabled` |
| C22 | `action/quiet/bg-hover` |
| C23 | `action/quiet/bg-pressed` |
| C24 | `selection/fill` |
| C25 | `selection/fill-hover` |
| C26 | `hover/fill` |
| C27 | `press/fill` |
| C28 | `highlight/match` |
| C29 | `canvas/selection-fill` |
| C30 | `state/danger/bg` |
| C31 | `state/danger/bg-hover` |
| C32 | `state/warning/bg` |
| C33 | `state/warning/bg-hover` |
| C34 | `state/success/bg` |
| C35 | `state/success/bg-hover` |
| C36 | `state/info/bg` |
| C37 | `state/info/bg-hover` |
| C38 | `media/chip` |
| C39 | `media/chip-hover` |
| C40 | `media/glass` |

## Full tables per theme

Cell rules: a bold ratio is a graded role pair that passes; `**x F**` is a graded role pair that fails. A plain ratio is a role pair that is exempt or decorative. A ratio in parentheses is a cross pair (subject against a surface it is not designed to sit on), shown for information and not counted as a fail. Alpha surfaces are shown at their worst host.

### ink/dark

Ink dark has no override list of its own beyond two entries, so theme-varied tokens use the semantic defaults.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **18.34** | **17.28** | **16.23** | **15.39** | **14.68** | **15.39** | **19.25** | (1.03) | (1.09) | (1.04) | (10.24) | (15.44) | (19.25) | (1.00) | (1.09) | (1.30) | (16.23) | (14.68) | (11.32) | (4.75) | (10.05) | **11.91** | **10.05** | **10.05** | **9.15** | **11.91** | **8.20** | **6.94** | (1.16) | **12.63** | **11.80** | **10.08** | **7.78** | **10.51** | **8.64** | **11.63** | **9.90** | (16.25) | (17.12) | (17.85) |
| text/muted | text | **8.28** | **7.80** | **7.32** | **6.94** | **6.62** | **6.94** | **8.69** | (2.14) | (2.42) | (2.13) | (4.62) | (6.97) | (8.69) | (2.22) | (2.42) | (1.70) | (7.32) | (6.62) | (5.11) | (2.15) | (4.54) | **5.37** | **4.54** | **4.54** | **4.13 F** | **5.37** | **3.70 F** | **3.13 F** | (1.90) | **5.70** | **5.33** | **4.55** | **3.51 F** | **4.74** | **3.90 F** | **5.25** | **4.47 F** | (7.33) | (7.73) | (8.05) |
| text/subtle | text | **5.93** | **5.58** | **5.24** | **4.97** | **4.74** | **4.97** | **6.22** | (2.99) | (3.38) | (2.97) | (3.31) | (4.99) | (6.22) | (3.10) | (3.38) | (2.38) | (5.24) | (4.74) | (3.66) | (1.54) | (3.25) | **3.85 F** | **3.25 F** | **3.25 F** | **2.95 F** | **3.85 F** | **2.65 F** | **2.24 F** | (2.66) | **4.08 F** | **3.81 F** | **3.26 F** | **2.51 F** | **3.40 F** | **2.79 F** | **3.76 F** | **3.20 F** | (5.25) | (5.53) | (5.77) |
| text/disabled | exempt | 3.01 | 3.11 | 3.13 | 3.12 | 3.10 | 3.12 | 2.91 | (1.04) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.03) | (1.00) | (1.13) | (3.13) | (3.10) | (2.86) | (1.75) | (2.73) | 2.91 | 2.73 | 2.73 | 2.62 | 2.91 | 2.50 | 2.31 | (1.09) | 2.95 | 2.86 | 2.73 | 2.44 | 2.77 | 2.54 | 2.88 | 2.70 | (2.91) | (2.91) | (2.91) |
| text/on-action | text | (1.00) | (1.06) | (1.13) | (1.19) | (1.25) | (1.19) | (1.05) | (17.73) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | **18.34** | **20.01** | **14.09** | (1.13) | (1.25) | (1.62) | (3.86) | (1.21) | (1.07) | (1.21) | (1.21) | (1.32) | (1.07) | (1.47) | (1.85) | (15.75) | (1.07) | (1.14) | (1.23) | (1.62) | (1.22) | (1.50) | (1.11) | (1.30) | (1.01) | (1.01) | (1.01) |
| action/primary/fg | text | (1.00) | (1.06) | (1.13) | (1.19) | (1.25) | (1.19) | (1.05) | (17.73) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | **18.34** | **20.01** | **14.09** | (1.13) | (1.25) | (1.62) | (3.86) | (1.21) | (1.07) | (1.21) | (1.21) | (1.32) | (1.07) | (1.47) | (1.85) | (15.75) | (1.07) | (1.14) | (1.23) | (1.62) | (1.22) | (1.50) | (1.11) | (1.30) | (1.01) | (1.01) | (1.01) |
| action/primary/fg-disabled | exempt | (3.01) | (3.11) | (3.13) | (3.12) | (3.10) | (3.12) | (2.91) | (1.04) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.03) | (1.00) | (1.13) | (3.13) | (3.10) | (2.86) | (1.75) | 2.73 | (2.91) | (2.73) | (2.73) | (2.62) | (2.91) | (2.50) | (2.31) | (1.09) | (2.95) | (2.86) | (2.73) | (2.44) | (2.77) | (2.54) | (2.88) | (2.70) | (2.91) | (2.91) | (2.91) |
| action/secondary/fg | text | (18.34) | (17.28) | (16.23) | (15.39) | (14.68) | (15.39) | (19.25) | (1.03) | (1.09) | (1.04) | (10.24) | (15.44) | (19.25) | (1.00) | (1.09) | (1.30) | **16.23** | **14.68** | **11.32** | (4.75) | (10.05) | (11.91) | (10.05) | (10.05) | (9.15) | (11.91) | (8.20) | (6.94) | (1.16) | (12.63) | (11.80) | (10.08) | (7.78) | (10.51) | (8.64) | (11.63) | (9.90) | (16.25) | (17.12) | (17.85) |
| action/quiet/fg | text | **18.34** | **17.28** | **16.23** | **15.39** | **14.68** | **15.39** | **19.25** | (1.03) | (1.09) | (1.04) | (10.24) | (15.44) | (19.25) | (1.00) | (1.09) | (1.30) | (16.23) | (14.68) | (11.32) | (4.75) | (10.05) | **11.91** | **10.05** | **10.05** | **9.15** | **11.91** | **8.20** | **6.94** | (1.16) | **12.63** | **11.80** | **10.08** | **7.78** | **10.51** | **8.64** | **11.63** | **9.90** | (16.25) | (17.12) | (17.85) |
| state/danger/fg | text | **7.86** | **7.40** | **6.95** | **6.59** | **6.29** | **6.59** | **8.25** | (2.26) | (2.55) | (2.24) | (4.39) | (6.61) | (8.25) | (2.33) | (2.55) | (1.79) | (6.95) | (6.29) | (4.85) | (2.04) | (4.31) | **5.10** | **4.31 F** | **4.31 F** | **3.92 F** | **5.10** | **3.51 F** | **2.97 F** | (2.00) | **5.41** | **5.06** | (4.32) | (3.33) | (4.50) | (3.70) | (4.98) | (4.24) | (6.96) | (7.34) | (7.65) |
| state/warning/fg | text | **12.33** | **11.61** | **10.91** | **10.34** | **9.87** | **10.34** | **12.94** | (1.44) | (1.62) | (1.43) | (6.88) | (10.38) | (12.94) | (1.49) | (1.62) | (1.14) | (10.91) | (9.87) | (7.61) | (3.20) | (6.76) | **8.00** | **6.76** | **6.76** | **6.15** | **8.00** | **5.51** | **4.66** | (1.28) | (8.49) | (7.93) | **6.78** | **5.23** | (7.06) | (5.81) | (7.82) | (6.65) | (10.92) | (11.51) | (12.00) |
| state/success/fg | text | **12.50** | **11.77** | **11.06** | **10.48** | **10.00** | **10.48** | **13.12** | (1.42) | (1.60) | (1.41) | (6.98) | (10.52) | (13.12) | (1.47) | (1.60) | (1.13) | (11.06) | (10.00) | (7.71) | (3.24) | (6.85) | **8.11** | **6.85** | **6.85** | **6.23** | **8.11** | **5.59** | **4.73** | (1.26) | (8.61) | (8.04) | (6.87) | (5.30) | **7.16** | **5.89** | (7.93) | (6.74) | (11.07) | (11.66) | (12.16) |
| state/info/fg | text | **9.21** | **8.67** | **8.14** | **7.72** | **7.37** | **7.72** | **9.66** | (1.93) | (2.17) | (1.91) | (5.14) | (7.75) | (9.66) | (1.99) | (2.17) | (1.53) | (8.14) | (7.37) | (5.68) | (2.39) | (5.04) | **5.98** | **5.04** | **5.04** | **4.59** | **5.98** | **4.11 F** | **3.48 F** | (1.71) | (6.34) | (5.92) | (5.06) | (3.90) | (5.27) | (4.34) | **5.84** | **4.97** | (8.16) | (8.59) | (8.96) |
| state/danger/on-solid | text | (20.01) | (18.85) | (17.70) | (16.79) | (16.01) | (16.79) | (21.00) | (1.13) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (1.09) | (1.00) | (1.42) | (17.70) | (16.01) | (12.35) | **5.19** | (10.97) | (12.99) | (10.97) | (10.97) | (9.98) | (12.99) | (8.94) | (7.57) | (1.27) | (13.78) | (12.88) | (11.00) | (8.49) | (11.47) | (9.42) | (12.69) | (10.80) | (17.73) | (18.68) | (19.47) |
| media/text | text | (16.88) | (16.01) | (15.11) | (14.38) | (13.75) | (14.38) | (17.62) | (1.12) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (1.08) | (1.00) | (1.38) | (15.11) | (13.75) | (10.75) | (4.62) | (9.60) | (11.28) | (9.60) | (9.60) | (8.77) | (11.28) | (7.90) | (6.73) | (1.25) | (11.92) | (11.17) | (9.62) | (7.51) | (10.01) | (8.30) | (11.03) | (9.45) | **15.13** | **15.87** | **16.48** |
| border/control | ui | **3.75** | **3.82** | **3.82** | **3.78** | **3.73** | **3.78** | **3.66** | (1.05) | (1.00) | (1.05) | (3.21) | (3.78) | (3.66) | (1.04) | (1.00) | (1.16) | (3.82) | (3.73) | (3.37) | (1.94) | (3.19) | (3.45) | (3.19) | (3.19) | (3.04) | (3.45) | (2.88) | (2.63) | (1.10) | (3.51) | (3.39) | (3.19) | (2.79) | (3.24) | (2.93) | (3.40) | (3.15) | (3.66) | (3.66) | (3.66) |
| border/control-hover | ui | **9.81** | **9.52** | **9.15** | **8.81** | **8.52** | **8.81** | **10.02** | (1.09) | (1.00) | (1.09) | (6.45) | (8.83) | (10.02) | (1.06) | (1.00) | (1.29) | (9.15) | (8.52) | (6.98) | (3.27) | (6.35) | (7.26) | (6.35) | (6.35) | (5.89) | (7.26) | (5.39) | (4.70) | (1.19) | (7.58) | (7.16) | (6.36) | (5.16) | (6.57) | (5.60) | (7.12) | (6.26) | (9.15) | (9.46) | (9.69) |
| border/focus | ui | **18.34** | **17.28** | **16.23** | **15.39** | **14.68** | **15.39** | **19.25** | (1.03) | (1.09) | (1.04) | (10.24) | (15.44) | (19.25) | (1.00) | (1.09) | (1.30) | (16.23) | (14.68) | (11.32) | (4.75) | (10.05) | (11.91) | (10.05) | (10.05) | (9.15) | (11.91) | (8.20) | (6.94) | (1.16) | (12.63) | (11.80) | (10.08) | (7.78) | (10.51) | (8.64) | (11.63) | (9.90) | (16.25) | (17.12) | (17.85) |
| border/strong | ui | **1.41 F** | **1.49 F** | **1.53 F** | **1.55 F** | **1.56 F** | **1.55 F** | **1.35 F** | (1.02) | (1.00) | (1.02) | (1.54) | (1.55) | (1.35) | (1.01) | (1.00) | (1.05) | (1.53) | (1.56) | (1.56) | (1.24) | (1.54) | (1.49) | (1.54) | (1.54) | (1.52) | (1.49) | (1.49) | (1.44) | (1.04) | (1.46) | (1.49) | (1.54) | (1.47) | (1.54) | (1.49) | (1.52) | (1.53) | (1.35) | (1.35) | (1.35) |
| action/primary/bg | ui | **18.34** | **17.28** | **16.23** | **15.39** | **14.68** | **15.39** | **19.25** | (1.03) | (1.09) | (1.04) | (10.24) | (15.44) | (19.25) | same | (1.09) | (1.30) | (16.23) | (14.68) | (11.32) | (4.75) | (10.05) | (11.91) | (10.05) | (10.05) | (9.15) | (11.91) | (8.20) | (6.94) | (1.16) | (12.63) | (11.80) | (10.08) | (7.78) | (10.51) | (8.64) | (11.63) | (9.90) | (16.25) | (17.12) | (17.85) |
| action/primary/bg-disabled | exempt | 1.32 | 1.39 | 1.43 | 1.45 | 1.46 | 1.45 | 1.27 | (1.02) | (1.00) | (1.02) | (1.45) | (1.45) | (1.27) | (1.01) | (1.00) | (1.05) | (1.43) | (1.46) | (1.46) | (1.20) | same | (1.40) | (1.45) | (1.45) | (1.43) | (1.40) | (1.41) | (1.37) | (1.03) | (1.37) | (1.40) | (1.45) | (1.40) | (1.44) | (1.41) | (1.42) | (1.44) | (1.27) | (1.27) | (1.27) |
| action/secondary/bg | ui | **1.13 F** | **1.06 F** | **1.00 F** | **1.05 F** | **1.11 F** | **1.05 F** | **1.19 F** | (15.68) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | (16.23) | (17.70) | (12.46) | same | (1.11) | (1.43) | (3.41) | (1.07) | (1.01) | (1.07) | (1.07) | (1.17) | (1.01) | (1.30) | (1.63) | (13.94) | (1.00) | (1.01) | (1.09) | (1.43) | (1.08) | (1.33) | (1.02) | (1.15) | (1.00) | (1.06) | (1.10) |
| state/danger/solid | ui | **3.86** | **3.63** | **3.41** | **3.24** | **3.09** | **3.24** | **4.05** | (4.59) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (4.75) | (5.19) | (3.65) | (3.41) | (3.09) | (2.38) | same | (2.11) | (2.51) | (2.11) | (2.11) | (1.92) | (2.51) | (1.72) | (1.46) | (4.08) | (2.66) | (2.48) | (2.12) | (1.64) | (2.21) | (1.82) | (2.45) | (2.08) | (3.42) | (3.60) | (3.75) |
| canvas/ink-default | ui | (1.19) | (1.12) | (1.05) | (1.00) | (1.05) | (1.00) | (1.25) | **14.91** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (15.43) | (16.83) | (11.85) | (1.05) | (1.05) | (1.36) | (3.25) | (1.02) | (1.04) | (1.02) | (1.02) | (1.11) | (1.04) | (1.24) | (1.55) | (13.25) | (1.03) | (1.02) | (1.04) | (1.36) | (1.02) | (1.26) | (1.00) | (1.10) | (1.05) | (1.11) | (1.16) |
| content/ink-on-dark | ui | (17.85) | (16.82) | (15.79) | (14.97) | (14.28) | (14.97) | (18.73) | (1.01) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (1.03) | (1.12) | (1.27) | (15.79) | (14.28) | (11.02) | (4.63) | (9.78) | (11.59) | (9.78) | (9.78) | (8.90) | (11.59) | (7.98) | (6.75) | (1.13) | (12.29) | (11.49) | (9.81) | (7.57) | (10.23) | (8.41) | (11.32) | (9.63) | (15.82) | (16.66) | (17.37) |
| border/subtle | decor | 1.17 | 1.21 | 1.24 | 1.26 | 1.26 | 1.26 | 1.14 | (1.01) | (1.00) | (1.01) | (1.27) | (1.25) | (1.14) | (1.01) | (1.00) | (1.03) | (1.24) | (1.26) | (1.28) | (1.12) | (1.26) | (1.22) | (1.26) | (1.26) | (1.26) | (1.22) | (1.25) | (1.23) | (1.02) | (1.20) | (1.22) | (1.26) | (1.24) | (1.25) | (1.25) | (1.23) | (1.26) | (1.14) | (1.14) | (1.14) |
| canvas/rule | decor | 1.06 | 1.07 | 1.08 | 1.08 | 1.08 | 1.08 | 1.06 | 1.20 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.20) | (1.21) | (1.18) | (1.08) | (1.08) | (1.05) | (1.10) | (1.04) | (1.06) | (1.04) | (1.04) | (1.03) | (1.06) | (1.01) | (1.00) | (1.19) | (1.06) | (1.05) | (1.04) | (1.00) | (1.04) | (1.02) | (1.06) | (1.04) | (1.06) | (1.06) | (1.06) |
| state/danger/line | decor | 1.38 | 1.43 | 1.45 | 1.46 | 1.46 | 1.46 | 1.34 | (1.20) | (1.22) | (1.20) | (1.38) | (1.45) | (1.34) | (1.21) | (1.22) | (1.15) | (1.45) | (1.46) | (1.41) | (1.17) | (1.38) | (1.43) | (1.38) | (1.38) | (1.35) | (1.43) | (1.32) | (1.28) | (1.18) | 1.46 | 1.46 | (1.39) | (1.32) | (1.36) | (1.29) | (1.40) | (1.35) | (1.34) | (1.34) | (1.34) |
| state/warning/line | decor | 1.58 | 1.65 | 1.68 | 1.70 | 1.70 | 1.70 | 1.51 | (1.09) | (1.11) | (1.08) | (1.62) | (1.70) | (1.51) | (1.09) | (1.11) | (1.04) | (1.68) | (1.70) | (1.65) | (1.27) | (1.62) | (1.65) | (1.62) | (1.62) | (1.58) | (1.65) | (1.54) | (1.49) | (1.06) | (1.65) | (1.67) | 1.63 | 1.53 | (1.62) | (1.54) | (1.65) | (1.58) | (1.51) | (1.51) | (1.51) |
| state/success/line | decor | 1.37 | 1.42 | 1.44 | 1.45 | 1.45 | 1.45 | 1.33 | (1.20) | (1.22) | (1.20) | (1.38) | (1.45) | (1.33) | (1.21) | (1.22) | (1.15) | (1.44) | (1.45) | (1.41) | (1.02) | (1.38) | (1.42) | (1.38) | (1.38) | (1.35) | (1.42) | (1.31) | (1.25) | (1.18) | (1.36) | (1.36) | (1.37) | (1.29) | 1.41 | 1.35 | (1.42) | (1.37) | (1.33) | (1.33) | (1.33) |
| state/info/line | decor | 1.53 | 1.58 | 1.60 | 1.60 | 1.60 | 1.60 | 1.48 | (1.34) | (1.37) | (1.34) | (1.47) | (1.60) | (1.48) | (1.35) | (1.37) | (1.27) | (1.60) | (1.60) | (1.51) | (1.01) | (1.46) | (1.53) | (1.46) | (1.46) | (1.42) | (1.53) | (1.37) | (1.27) | (1.30) | (1.52) | (1.48) | (1.45) | (1.33) | (1.48) | (1.40) | 1.54 | 1.47 | (1.48) | (1.48) | (1.48) |

### ink/light

Every theme-varied token is overridden by this variant. Shared tokens (brand, folder, paper, media, not theme-varied) are not overridden: 43.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **19.17** | **17.90** | **16.36** | **15.20** | **14.09** | **20.01** | **12.90** | (20.01) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | (1.00) | (1.05) | (1.62) | (16.36) | (14.09) | (12.90) | (3.86) | (10.17) | **11.53** | **10.17** | **10.58** | **9.76** | **11.53** | **9.76** | **12.44** | (17.75) | **11.46** | **10.72** | **11.59** | **10.90** | **11.62** | **10.95** | **11.63** | **10.97** | (6.74) | (4.15) | (2.44) |
| text/muted | text | **8.41** | **7.85** | **7.18** | **6.67** | **6.18** | **8.78** | **5.66** | (8.78) | (8.78) | (7.72) | (1.27) | (1.92) | (2.39) | (2.28) | (2.39) | (1.41) | (7.18) | (6.18) | (5.66) | (1.69) | (4.46) | **5.06** | **4.46 F** | **4.64** | **4.28 F** | **5.06** | **4.28 F** | **5.46** | (7.79) | **5.03** | **4.70** | **5.08** | **4.78** | **5.10** | **4.80** | **5.10** | **4.81** | (2.96) | (1.82) | (1.07) |
| text/subtle | text | **6.33** | **5.91** | **5.40** | **5.02** | **4.65** | **6.61** | **4.26 F** | (6.61) | (6.61) | (5.81) | (1.69) | (2.55) | (3.18) | (3.03) | (3.18) | (1.87) | (5.40) | (4.65) | (4.26) | (1.27) | (3.36) | **3.81 F** | **3.36 F** | **3.49 F** | **3.22 F** | **3.81 F** | **3.22 F** | **4.11 F** | (5.86) | **3.78 F** | **3.54 F** | **3.83 F** | **3.60 F** | **3.84 F** | **3.62 F** | **3.84 F** | **3.62 F** | (2.23) | (1.37) | (1.01) |
| text/disabled | exempt | 2.18 | 2.16 | 2.14 | 2.12 | 2.10 | 2.19 | 2.07 | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.03) | (1.04) | (1.17) | (2.14) | (2.10) | (2.07) | (1.64) | (1.99) | 2.03 | 1.99 | 2.00 | 1.97 | 2.03 | 1.97 | 2.06 | (2.16) | 2.03 | 2.01 | 2.03 | 2.01 | 2.03 | 2.01 | 2.03 | 2.01 | (1.83) | (1.62) | (1.37) |
| text/on-action | text | (1.00) | (1.07) | (1.17) | (1.26) | (1.36) | (1.04) | (1.49) | (1.04) | (1.04) | (1.09) | (10.70) | (16.13) | (20.12) | **19.17** | **20.12** | **11.83** | (1.17) | (1.36) | (1.49) | (4.97) | (1.24) | (1.08) | (1.24) | (1.18) | (1.29) | (1.08) | (1.29) | (1.25) | (1.08) | (1.09) | (1.18) | (1.08) | (1.16) | (1.08) | (1.16) | (1.08) | (1.16) | (1.90) | (3.22) | (5.93) |
| action/primary/fg | text | (1.00) | (1.07) | (1.17) | (1.26) | (1.36) | (1.04) | (1.49) | (1.04) | (1.04) | (1.09) | (10.70) | (16.13) | (20.12) | **19.17** | **20.12** | **11.83** | (1.17) | (1.36) | (1.49) | (4.97) | (1.24) | (1.08) | (1.24) | (1.18) | (1.29) | (1.08) | (1.29) | (1.25) | (1.08) | (1.09) | (1.18) | (1.08) | (1.16) | (1.08) | (1.16) | (1.08) | (1.16) | (1.90) | (3.22) | (5.93) |
| action/primary/fg-disabled | exempt | (2.18) | (2.16) | (2.14) | (2.12) | (2.10) | (2.19) | (2.07) | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.03) | (1.04) | (1.17) | (2.14) | (2.10) | (2.07) | (1.64) | 1.99 | (2.03) | (1.99) | (2.00) | (1.97) | (2.03) | (1.97) | (2.06) | (2.16) | (2.03) | (2.01) | (2.03) | (2.01) | (2.03) | (2.01) | (2.03) | (2.01) | (1.83) | (1.62) | (1.37) |
| action/secondary/fg | text | (19.17) | (17.90) | (16.36) | (15.20) | (14.09) | (20.01) | (12.90) | (20.01) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | (1.00) | (1.05) | (1.62) | **16.36** | **14.09** | **12.90** | (3.86) | (10.17) | (11.53) | (10.17) | (10.58) | (9.76) | (11.53) | (9.76) | (12.44) | (17.75) | (11.46) | (10.72) | (11.59) | (10.90) | (11.62) | (10.95) | (11.63) | (10.97) | (6.74) | (4.15) | (2.44) |
| action/quiet/fg | text | **19.17** | **17.90** | **16.36** | **15.20** | **14.09** | **20.01** | **12.90** | (20.01) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | (1.00) | (1.05) | (1.62) | (16.36) | (14.09) | (12.90) | (3.86) | (10.17) | **11.53** | **10.17** | **10.58** | **9.76** | **11.53** | **9.76** | **12.44** | (17.75) | **11.46** | **10.72** | **11.59** | **10.90** | **11.62** | **10.95** | **11.63** | **10.97** | (6.74) | (4.15) | (2.44) |
| state/danger/fg | text | **7.47** | **6.97** | **6.37** | **5.92** | **5.49** | **7.79** | **5.02** | (7.79) | (7.79) | (6.85) | (1.43) | (2.16) | (2.70) | (2.57) | (2.70) | (1.58) | (6.37) | (5.49) | (5.02) | (1.50) | (3.96) | **4.49 F** | **3.96 F** | **4.12 F** | **3.80 F** | **4.49 F** | **3.80 F** | **4.84** | (6.91) | **4.46 F** | **4.17 F** | (4.51) | (4.24) | (4.52) | (4.26) | (4.53) | (4.27) | (2.62) | (1.62) | (1.00) |
| state/warning/fg | text | **7.92** | **7.40** | **6.76** | **6.28** | **5.82** | **8.27** | **5.33** | (8.27) | (8.27) | (7.27) | (1.35) | (2.04) | (2.54) | (2.42) | (2.54) | (1.49) | (6.76) | (5.82) | (5.33) | (1.59) | (4.20) | **4.76** | **4.20 F** | **4.37 F** | **4.04 F** | **4.76** | **4.04 F** | **5.14** | (7.34) | (4.74) | (4.43) | **4.79** | **4.51** | (4.80) | (4.53) | (4.81) | (4.53) | (2.79) | (1.72) | (1.01) |
| state/success/fg | text | **7.29** | **6.81** | **6.22** | **5.78** | **5.36** | **7.61** | **4.91** | (7.61) | (7.61) | (6.69) | (1.47) | (2.21) | (2.76) | (2.63) | (2.76) | (1.62) | (6.22) | (5.36) | (4.91) | (1.47) | (3.87) | **4.38 F** | **3.87 F** | **4.02 F** | **3.71 F** | **4.38 F** | **3.71 F** | **4.73** | (6.75) | (4.36) | (4.08) | (4.41) | (4.14) | **4.42 F** | **4.16 F** | (4.42) | (4.17) | (2.56) | (1.58) | (1.02) |
| state/info/fg | text | **7.22** | **6.74** | **6.16** | **5.72** | **5.31** | **7.54** | **4.86** | (7.54) | (7.54) | (6.63) | (1.48) | (2.23) | (2.79) | (2.66) | (2.79) | (1.64) | (6.16) | (5.31) | (4.86) | (1.45) | (3.83) | **4.34 F** | **3.83 F** | **3.98 F** | **3.68 F** | **4.34 F** | **3.68 F** | **4.68** | (6.68) | (4.32) | (4.04) | (4.36) | (4.11) | (4.38) | (4.12) | **4.38 F** | **4.13 F** | (2.54) | (1.56) | (1.02) |
| state/danger/on-solid | text | (1.04) | (1.12) | (1.22) | (1.32) | (1.42) | (1.00) | (1.55) | (1.00) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (20.01) | (21.00) | (12.35) | (1.22) | (1.42) | (1.55) | **5.19** | (1.29) | (1.13) | (1.29) | (1.24) | (1.35) | (1.13) | (1.35) | (1.31) | (1.13) | (1.14) | (1.23) | (1.13) | (1.21) | (1.13) | (1.21) | (1.13) | (1.21) | (1.98) | (3.36) | (6.19) |
| media/text | text | (1.04) | (1.11) | (1.20) | (1.29) | (1.38) | (1.00) | (1.50) | (1.00) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (16.88) | (17.62) | (10.75) | (1.20) | (1.38) | (1.50) | (4.62) | (1.27) | (1.12) | (1.27) | (1.22) | (1.32) | (1.12) | (1.32) | (1.28) | (1.12) | (1.13) | (1.21) | (1.12) | (1.20) | (1.12) | (1.19) | (1.12) | (1.19) | **1.89 F** | **3.11 F** | **5.56** |
| border/control | ui | **5.05** | **4.93** | **4.77** | **4.63** | **4.50** | **5.12** | **4.34** | (5.12) | (5.12) | (4.90) | (1.40) | (1.06) | (1.08) | (1.05) | (1.08) | (1.31) | (4.77) | (4.50) | (4.34) | (2.44) | (3.91) | (4.14) | (3.91) | (3.98) | (3.84) | (4.14) | (3.84) | (4.29) | (4.91) | (4.13) | (4.01) | (4.15) | (4.04) | (4.15) | (4.05) | (4.15) | (4.05) | (3.20) | (2.43) | (1.73) |
| border/control-hover | ui | **11.46** | **10.89** | **10.18** | **9.62** | **9.07** | **11.83** | **8.47** | (11.83) | (11.83) | (10.76) | (1.55) | (1.08) | (1.12) | (1.07) | (1.12) | (1.42) | (10.18) | (9.07) | (8.47) | (3.17) | (7.00) | (7.75) | (7.00) | (7.23) | (6.78) | (7.75) | (6.78) | (8.25) | (10.82) | (7.71) | (7.31) | (7.78) | (7.41) | (7.80) | (7.44) | (7.80) | (7.44) | (4.98) | (3.29) | (2.05) |
| border/focus | ui | **19.17** | **17.90** | **16.36** | **15.20** | **14.09** | **20.01** | **12.90** | (20.01) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | (1.00) | (1.05) | (1.62) | (16.36) | (14.09) | (12.90) | (3.86) | (10.17) | (11.53) | (10.17) | (10.58) | (9.76) | (11.53) | (9.76) | (12.44) | (17.75) | (11.46) | (10.72) | (11.59) | (10.90) | (11.62) | (10.95) | (11.63) | (10.97) | (6.74) | (4.15) | (2.44) |
| border/strong | ui | **1.40 F** | **1.40 F** | **1.39 F** | **1.39 F** | **1.38 F** | **1.41 F** | **1.38 F** | (1.41) | (1.41) | (1.40) | (1.10) | (1.02) | (1.02) | (1.01) | (1.02) | (1.08) | (1.39) | (1.38) | (1.38) | (1.26) | (1.36) | (1.37) | (1.36) | (1.36) | (1.35) | (1.37) | (1.35) | (1.37) | (1.40) | (1.37) | (1.36) | (1.37) | (1.36) | (1.37) | (1.36) | (1.37) | (1.36) | (1.31) | (1.25) | (1.16) |
| action/primary/bg | ui | **19.17** | **17.90** | **16.36** | **15.20** | **14.09** | **20.01** | **12.90** | (20.01) | (20.01) | (17.60) | (1.79) | (1.19) | (1.05) | same | (1.05) | (1.62) | (16.36) | (14.09) | (12.90) | (3.86) | (10.17) | (11.53) | (10.17) | (10.58) | (9.76) | (11.53) | (9.76) | (12.44) | (17.75) | (11.46) | (10.72) | (11.59) | (10.90) | (11.62) | (10.95) | (11.63) | (10.97) | (6.74) | (4.15) | (2.44) |
| action/primary/bg-disabled | exempt | 1.29 | 1.28 | 1.28 | 1.28 | 1.27 | 1.29 | 1.27 | (1.29) | (1.29) | (1.28) | (1.07) | (1.01) | (1.02) | (1.01) | (1.02) | (1.06) | (1.28) | (1.27) | (1.27) | (1.19) | same | (1.26) | (1.26) | (1.26) | (1.25) | (1.26) | (1.25) | (1.27) | (1.28) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.23) | (1.18) | (1.12) |
| action/secondary/bg | ui | **1.17 F** | **1.09 F** | **1.00 F** | **1.08 F** | **1.16 F** | **1.22 F** | **1.27 F** | (1.22) | (1.22) | (1.08) | (9.14) | (13.77) | (17.17) | (16.36) | (17.17) | (10.10) | same | (1.16) | (1.27) | (4.24) | (1.05) | (1.03) | (1.05) | (1.01) | (1.10) | (1.03) | (1.10) | (1.07) | (1.08) | (1.03) | (1.01) | (1.03) | (1.01) | (1.03) | (1.01) | (1.03) | (1.01) | (1.62) | (2.75) | (5.06) |
| state/danger/solid | ui | **4.97** | **4.64** | **4.24** | **3.94** | **3.65** | **5.19** | **3.34** | (5.19) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (3.86) | (4.05) | (2.38) | (4.24) | (3.65) | (3.34) | same | (2.63) | (2.99) | (2.63) | (2.74) | (2.53) | (2.99) | (2.53) | (3.22) | (4.60) | (2.97) | (2.78) | (3.00) | (2.82) | (3.01) | (2.84) | (3.01) | (2.84) | (1.75) | (1.08) | (1.19) |
| canvas/ink-default | ui | (16.12) | (15.05) | (13.76) | (12.78) | (11.85) | (16.83) | (10.85) | **16.83** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (1.19) | (1.25) | (1.36) | (13.76) | (11.85) | (10.85) | (3.25) | (8.55) | (9.69) | (8.55) | (8.90) | (8.21) | (9.69) | (8.21) | (10.46) | (14.93) | (9.64) | (9.01) | (9.74) | (9.17) | (9.77) | (9.21) | (9.78) | (9.23) | (5.67) | (3.49) | (2.05) |
| content/ink-on-dark | ui | (1.07) | (1.00) | (1.09) | (1.17) | (1.27) | (1.12) | (1.38) | (1.12) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (17.85) | (18.73) | (11.02) | (1.09) | (1.27) | (1.38) | (4.63) | (1.15) | (1.01) | (1.15) | (1.10) | (1.20) | (1.01) | (1.20) | (1.16) | (1.01) | (1.02) | (1.10) | (1.01) | (1.08) | (1.01) | (1.08) | (1.01) | (1.08) | (1.77) | (3.00) | (5.53) |
| border/subtle | decor | 1.23 | 1.23 | 1.23 | 1.23 | 1.22 | 1.24 | 1.22 | (1.24) | (1.24) | (1.23) | (1.06) | (1.01) | (1.01) | (1.01) | (1.01) | (1.05) | (1.23) | (1.22) | (1.22) | (1.16) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.22) | (1.23) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.19) | (1.15) | (1.10) |
| canvas/rule | decor | 1.20 | 1.20 | 1.19 | 1.19 | 1.18 | 1.21 | 1.17 | 1.21 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.06) | (1.06) | (1.05) | (1.19) | (1.18) | (1.17) | (1.10) | (1.15) | (1.16) | (1.15) | (1.16) | (1.15) | (1.16) | (1.15) | (1.17) | (1.20) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.11) | (1.06) | (1.00) |
| state/danger/line | decor | 1.72 | 1.70 | 1.68 | 1.66 | 1.64 | 1.73 | 1.61 | (1.73) | (1.73) | (1.69) | (1.04) | (1.16) | (1.17) | (1.17) | (1.17) | (1.07) | (1.68) | (1.64) | (1.61) | (1.13) | (1.54) | (1.58) | (1.54) | (1.55) | (1.53) | (1.58) | (1.53) | (1.58) | (1.70) | 1.57 | 1.55 | (1.58) | (1.56) | (1.59) | (1.57) | (1.59) | (1.57) | (1.41) | (1.25) | (1.06) |
| state/warning/line | decor | 1.66 | 1.64 | 1.61 | 1.59 | 1.57 | 1.67 | 1.55 | (1.67) | (1.67) | (1.63) | (1.08) | (1.19) | (1.17) | (1.18) | (1.17) | (1.11) | (1.61) | (1.57) | (1.55) | (1.18) | (1.48) | (1.52) | (1.48) | (1.49) | (1.47) | (1.52) | (1.47) | (1.52) | (1.64) | (1.51) | (1.49) | 1.52 | 1.50 | (1.52) | (1.50) | (1.52) | (1.50) | (1.35) | (1.19) | (1.02) |
| state/success/line | decor | 1.64 | 1.62 | 1.59 | 1.57 | 1.55 | 1.65 | 1.53 | (1.65) | (1.65) | (1.62) | (1.10) | (1.22) | (1.19) | (1.20) | (1.19) | (1.13) | (1.59) | (1.55) | (1.53) | (1.32) | (1.46) | (1.49) | (1.46) | (1.47) | (1.44) | (1.49) | (1.44) | (1.53) | (1.62) | (1.50) | (1.48) | (1.50) | (1.48) | 1.49 | 1.48 | (1.50) | (1.48) | (1.33) | (1.17) | (1.00) |
| state/info/line | decor | 1.63 | 1.61 | 1.59 | 1.57 | 1.55 | 1.64 | 1.52 | (1.64) | (1.64) | (1.61) | (1.11) | (1.22) | (1.19) | (1.21) | (1.19) | (1.14) | (1.59) | (1.55) | (1.52) | (1.29) | (1.45) | (1.49) | (1.45) | (1.46) | (1.44) | (1.49) | (1.44) | (1.53) | (1.61) | (1.49) | (1.47) | (1.49) | (1.47) | (1.49) | (1.47) | 1.49 | 1.47 | (1.32) | (1.17) | (1.00) |

### graphite/dark

Every theme-varied token is overridden by this variant. Shared tokens (brand, folder, paper, media, not theme-varied) are not overridden: 43.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **15.56** | **14.53** | **13.08** | **12.08** | **11.41** | **12.39** | **16.43** | (1.01) | (1.18) | (1.04) | (9.45) | (14.24) | (17.75) | (1.00) | (1.18) | (1.32) | (13.08) | (11.41) | (9.99) | (4.38) | (7.76) | **9.16** | **7.76** | **7.76** | **7.09** | **9.16** | **6.40** | **5.57** | (1.11) | **9.99** | **9.42** | **7.86** | **6.18** | **8.26** | **6.92** | **9.10** | **7.86** | (13.53) | (14.79) | (15.88) |
| text/muted | text | **8.65** | **8.08** | **7.27** | **6.71** | **6.34** | **6.89** | **9.13** | (1.82) | (2.13) | (1.87) | (5.25) | (7.91) | (9.87) | (1.80) | (2.13) | (1.36) | (7.27) | (6.34) | (5.55) | (2.44) | (4.31) | **5.09** | **4.31 F** | **4.31 F** | **3.94 F** | **5.09** | **3.56 F** | **3.10 F** | (1.62) | **5.55** | **5.23** | **4.37 F** | **3.44 F** | **4.59** | **3.85 F** | **5.06** | **4.37 F** | (7.52) | (8.22) | (8.83) |
| text/subtle | text | **6.29** | **5.87** | **5.29** | **4.88** | **4.61** | **5.01** | **6.64** | (2.50) | (2.93) | (2.57) | (3.82) | (5.75) | (7.17) | (2.47) | (2.93) | (1.88) | (5.29) | (4.61) | (4.04) | (1.77) | (3.14) | **3.70 F** | **3.14 F** | **3.14 F** | **2.87 F** | **3.70 F** | **2.59 F** | **2.25 F** | (2.22) | **4.04 F** | **3.81 F** | **3.17 F** | **2.50 F** | **3.34 F** | **2.80 F** | **3.68 F** | **3.18 F** | (5.47) | (5.98) | (6.42) |
| text/disabled | exempt | 3.12 | 3.13 | 3.07 | 3.01 | 2.95 | 3.03 | 3.07 | (1.06) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.06) | (1.00) | (1.17) | (3.07) | (2.95) | (2.81) | (1.75) | (2.53) | 2.72 | 2.53 | 2.53 | 2.43 | 2.72 | 2.32 | 2.17 | (1.10) | 2.79 | 2.72 | 2.54 | 2.28 | 2.59 | 2.39 | 2.70 | 2.53 | (3.03) | (3.00) | (2.97) |
| text/on-action | text | (1.00) | (1.07) | (1.19) | (1.29) | (1.36) | (1.26) | (1.06) | (15.73) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | **15.56** | **18.41** | **11.80** | (1.19) | (1.36) | (1.56) | (3.55) | (1.29) | (1.11) | (1.29) | (1.29) | (1.42) | (1.11) | (1.59) | (1.95) | (13.99) | (1.08) | (1.16) | (1.30) | (1.71) | (1.27) | (1.58) | (1.15) | (1.36) | (1.00) | (1.00) | (1.02) |
| action/primary/fg | text | (1.00) | (1.07) | (1.19) | (1.29) | (1.36) | (1.26) | (1.06) | (15.73) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | **15.56** | **18.41** | **11.80** | (1.19) | (1.36) | (1.56) | (3.55) | (1.29) | (1.11) | (1.29) | (1.29) | (1.42) | (1.11) | (1.59) | (1.95) | (13.99) | (1.08) | (1.16) | (1.30) | (1.71) | (1.27) | (1.58) | (1.15) | (1.36) | (1.00) | (1.00) | (1.02) |
| action/primary/fg-disabled | exempt | (3.12) | (3.13) | (3.07) | (3.01) | (2.95) | (3.03) | (3.07) | (1.06) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.06) | (1.00) | (1.17) | (3.07) | (2.95) | (2.81) | (1.75) | 2.53 | (2.72) | (2.53) | (2.53) | (2.43) | (2.72) | (2.32) | (2.17) | (1.10) | (2.79) | (2.72) | (2.54) | (2.28) | (2.59) | (2.39) | (2.70) | (2.53) | (3.03) | (3.00) | (2.97) |
| action/secondary/fg | text | (15.56) | (14.53) | (13.08) | (12.08) | (11.41) | (12.39) | (16.43) | (1.01) | (1.18) | (1.04) | (9.45) | (14.24) | (17.75) | (1.00) | (1.18) | (1.32) | **13.08** | **11.41** | **9.99** | (4.38) | (7.76) | (9.16) | (7.76) | (7.76) | (7.09) | (9.16) | (6.40) | (5.57) | (1.11) | (9.99) | (9.42) | (7.86) | (6.18) | (8.26) | (6.92) | (9.10) | (7.86) | (13.53) | (14.79) | (15.88) |
| action/quiet/fg | text | **15.56** | **14.53** | **13.08** | **12.08** | **11.41** | **12.39** | **16.43** | (1.01) | (1.18) | (1.04) | (9.45) | (14.24) | (17.75) | (1.00) | (1.18) | (1.32) | (13.08) | (11.41) | (9.99) | (4.38) | (7.76) | **9.16** | **7.76** | **7.76** | **7.09** | **9.16** | **6.40** | **5.57** | (1.11) | **9.99** | **9.42** | **7.86** | **6.18** | **8.26** | **6.92** | **9.10** | **7.86** | (13.53) | (14.79) | (15.88) |
| state/danger/fg | text | **7.23** | **6.75** | **6.08** | **5.61** | **5.30** | **5.76** | **7.63** | (2.18) | (2.55) | (2.24) | (4.39) | (6.61) | (8.25) | (2.15) | (2.55) | (1.63) | (6.08) | (5.30) | (4.64) | (2.04) | (3.61) | **4.26 F** | **3.61 F** | **3.61 F** | **3.30 F** | **4.26 F** | **2.97 F** | **2.59 F** | (1.93) | **4.64** | **4.37 F** | (3.65) | (2.87) | (3.84) | (3.22) | (4.23) | (3.65) | (6.29) | (6.87) | (7.38) |
| state/warning/fg | text | **11.34** | **10.59** | **9.53** | **8.80** | **8.32** | **9.03** | **11.97** | (1.39) | (1.62) | (1.43) | (6.88) | (10.38) | (12.94) | (1.37) | (1.62) | (1.04) | (9.53) | (8.32) | (7.28) | (3.20) | (5.66) | **6.68** | **5.66** | **5.66** | **5.17** | **6.68** | **4.66** | **4.06 F** | (1.23) | (7.28) | (6.86) | **5.73** | **4.51** | (6.02) | (5.05) | (6.64) | (5.73) | (9.86) | (10.78) | (11.58) |
| state/success/fg | text | **11.50** | **10.74** | **9.66** | **8.92** | **8.43** | **9.16** | **12.14** | (1.37) | (1.60) | (1.41) | (6.98) | (10.52) | (13.12) | (1.35) | (1.60) | (1.03) | (9.66) | (8.43) | (7.38) | (3.24) | (5.73) | **6.77** | **5.73** | **5.73** | **5.24** | **6.77** | **4.73** | **4.12 F** | (1.22) | (7.38) | (6.96) | (5.80) | (4.57) | **6.10** | **5.11** | (6.73) | (5.81) | (10.00) | (10.93) | (11.73) |
| state/info/fg | text | **8.47** | **7.91** | **7.12** | **6.57** | **6.21** | **6.74** | **8.94** | (1.86) | (2.17) | (1.91) | (5.14) | (7.75) | (9.66) | (1.84) | (2.17) | (1.39) | (7.12) | (6.21) | (5.44) | (2.39) | (4.22) | **4.99** | **4.22 F** | **4.22 F** | **3.86 F** | **4.99** | **3.48 F** | **3.03 F** | (1.65) | (5.43) | (5.12) | (4.28) | (3.37) | (4.50) | (3.77) | **4.95** | **4.28 F** | (7.36) | (8.05) | (8.64) |
| state/danger/on-solid | text | (18.41) | (17.19) | (15.47) | (14.29) | (13.50) | (14.66) | (19.43) | (1.17) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (1.18) | (1.00) | (1.56) | (15.47) | (13.50) | (11.82) | **5.19** | (9.18) | (10.84) | (9.18) | (9.18) | (8.39) | (10.84) | (7.57) | (6.59) | (1.32) | (11.81) | (11.14) | (9.29) | (7.31) | (9.77) | (8.19) | (10.77) | (9.30) | (16.01) | (17.50) | (18.79) |
| media/text | text | (15.66) | (14.70) | (13.32) | (12.35) | (11.70) | (12.65) | (16.45) | (1.16) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (1.17) | (1.00) | (1.51) | (13.32) | (11.70) | (10.31) | (4.62) | (8.10) | (9.49) | (8.10) | (8.10) | (7.43) | (9.49) | (6.74) | (5.90) | (1.29) | (10.30) | (9.73) | (8.20) | (6.52) | (8.60) | (7.25) | (9.43) | (8.20) | **13.75** | **14.94** | **15.96** |
| border/control | ui | **3.83** | **3.80** | **3.69** | **3.59** | **3.50** | **3.62** | **3.80** | (1.07) | (1.00) | (1.05) | (3.21) | (3.78) | (3.66) | (1.07) | (1.00) | (1.21) | (3.69) | (3.50) | (3.30) | (1.94) | (2.91) | (3.17) | (2.91) | (2.91) | (2.78) | (3.17) | (2.64) | (2.45) | (1.12) | (3.28) | (3.18) | (2.93) | (2.58) | (3.00) | (2.73) | (3.15) | (2.92) | (3.73) | (3.74) | (3.71) |
| border/control-hover | ui | **9.38** | **8.96** | **8.31** | **7.82** | **7.48** | **7.97** | **9.68** | (1.12) | (1.00) | (1.09) | (6.45) | (8.83) | (10.02) | (1.13) | (1.00) | (1.38) | (8.31) | (7.48) | (6.74) | (3.27) | (5.51) | (6.29) | (5.51) | (5.51) | (5.12) | (6.29) | (4.72) | (4.21) | (1.22) | (6.72) | (6.40) | (5.56) | (4.58) | (5.78) | (5.00) | (6.25) | (5.55) | (8.52) | (9.07) | (9.50) |
| border/focus | ui | **15.56** | **14.53** | **13.08** | **12.08** | **11.41** | **12.39** | **16.43** | (1.01) | (1.18) | (1.04) | (9.45) | (14.24) | (17.75) | (1.00) | (1.18) | (1.32) | (13.08) | (11.41) | (9.99) | (4.38) | (7.76) | (9.16) | (7.76) | (7.76) | (7.09) | (9.16) | (6.40) | (5.57) | (1.11) | (9.99) | (9.42) | (7.86) | (6.18) | (8.26) | (6.92) | (9.10) | (7.86) | (13.53) | (14.79) | (15.88) |
| border/strong | ui | **1.51 F** | **1.54 F** | **1.57 F** | **1.57 F** | **1.57 F** | **1.57 F** | **1.46 F** | (1.02) | (1.00) | (1.02) | (1.54) | (1.55) | (1.35) | (1.02) | (1.00) | (1.07) | (1.57) | (1.57) | (1.55) | (1.24) | (1.50) | (1.54) | (1.50) | (1.50) | (1.47) | (1.54) | (1.44) | (1.40) | (1.04) | (1.53) | (1.53) | (1.50) | (1.43) | (1.51) | (1.46) | (1.53) | (1.49) | (1.42) | (1.40) | (1.39) |
| action/primary/bg | ui | **15.56** | **14.53** | **13.08** | **12.08** | **11.41** | **12.39** | **16.43** | (1.01) | (1.18) | (1.04) | (9.45) | (14.24) | (17.75) | same | (1.18) | (1.32) | (13.08) | (11.41) | (9.99) | (4.38) | (7.76) | (9.16) | (7.76) | (7.76) | (7.09) | (9.16) | (6.40) | (5.57) | (1.11) | (9.99) | (9.42) | (7.86) | (6.18) | (8.26) | (6.92) | (9.10) | (7.86) | (13.53) | (14.79) | (15.88) |
| action/primary/bg-disabled | exempt | 1.41 | 1.44 | 1.47 | 1.47 | 1.47 | 1.47 | 1.36 | (1.02) | (1.00) | (1.02) | (1.45) | (1.45) | (1.27) | (1.02) | (1.00) | (1.06) | (1.47) | (1.47) | (1.46) | (1.20) | same | (1.45) | (1.42) | (1.42) | (1.40) | (1.45) | (1.37) | (1.34) | (1.04) | (1.43) | (1.44) | (1.42) | (1.36) | (1.42) | (1.38) | (1.44) | (1.41) | (1.33) | (1.32) | (1.30) |
| action/secondary/bg | ui | **1.19 F** | **1.11 F** | **1.00 F** | **1.08 F** | **1.15 F** | **1.06 F** | **1.26 F** | (13.22) | (15.47) | (13.61) | (1.38) | (1.09) | (1.36) | (13.08) | (15.47) | (9.92) | same | (1.15) | (1.31) | (2.98) | (1.08) | (1.01) | (1.08) | (1.08) | (1.19) | (1.01) | (1.33) | (1.64) | (11.76) | (1.03) | (1.02) | (1.10) | (1.44) | (1.07) | (1.32) | (1.03) | (1.15) | (1.03) | (1.13) | (1.21) |
| state/danger/solid | ui | **3.55** | **3.32** | **2.98 F** | **2.76 F** | **2.60 F** | **2.83 F** | **3.75** | (4.43) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (4.38) | (5.19) | (3.32) | (2.98) | (2.60) | (2.28) | same | (1.77) | (2.09) | (1.77) | (1.77) | (1.62) | (2.09) | (1.46) | (1.27) | (3.94) | (2.28) | (2.15) | (1.79) | (1.41) | (1.88) | (1.58) | (2.08) | (1.79) | (3.09) | (3.37) | (3.62) |
| canvas/ink-default | ui | (1.09) | (1.02) | (1.09) | (1.18) | (1.25) | (1.15) | (1.15) | **14.38** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (14.23) | (16.83) | (10.79) | (1.09) | (1.25) | (1.42) | (3.25) | (1.18) | (1.01) | (1.18) | (1.18) | (1.30) | (1.01) | (1.45) | (1.78) | (12.79) | (1.01) | (1.06) | (1.19) | (1.57) | (1.16) | (1.44) | (1.05) | (1.25) | (1.00) | (1.04) | (1.12) |
| content/ink-on-dark | ui | (16.42) | (15.34) | (13.80) | (12.75) | (12.04) | (13.08) | (17.34) | (1.04) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (1.06) | (1.12) | (1.39) | (13.80) | (12.04) | (10.54) | (4.63) | (8.19) | (9.67) | (8.19) | (8.19) | (7.48) | (9.67) | (6.75) | (5.88) | (1.17) | (10.54) | (9.94) | (8.29) | (6.53) | (8.72) | (7.30) | (9.61) | (8.30) | (14.28) | (15.61) | (16.76) |
| border/subtle | decor | 1.22 | 1.25 | 1.27 | 1.28 | 1.28 | 1.27 | 1.19 | (1.01) | (1.00) | (1.01) | (1.27) | (1.25) | (1.14) | (1.01) | (1.00) | (1.04) | (1.27) | (1.28) | (1.27) | (1.12) | (1.25) | (1.26) | (1.25) | (1.25) | (1.24) | (1.26) | (1.23) | (1.21) | (1.02) | (1.24) | (1.25) | (1.25) | (1.22) | (1.26) | (1.24) | (1.26) | (1.25) | (1.17) | (1.16) | (1.15) |
| canvas/rule | decor | 1.07 | 1.08 | 1.07 | 1.07 | 1.06 | 1.07 | 1.07 | 1.20 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.19) | (1.21) | (1.17) | (1.07) | (1.06) | (1.05) | (1.10) | (1.02) | (1.04) | (1.02) | (1.02) | (1.01) | (1.04) | (1.00) | (1.00) | (1.19) | (1.04) | (1.03) | (1.02) | (1.00) | (1.03) | (1.00) | (1.04) | (1.02) | (1.06) | (1.06) | (1.06) |
| state/danger/line | decor | 1.44 | 1.45 | 1.45 | 1.44 | 1.43 | 1.45 | 1.40 | (1.19) | (1.22) | (1.20) | (1.38) | (1.45) | (1.34) | (1.19) | (1.22) | (1.13) | (1.45) | (1.43) | (1.40) | (1.17) | (1.32) | (1.38) | (1.32) | (1.32) | (1.30) | (1.38) | (1.26) | (1.23) | (1.17) | 1.43 | 1.42 | (1.34) | (1.27) | (1.32) | (1.25) | (1.36) | (1.31) | (1.38) | (1.37) | (1.36) |
| state/warning/line | decor | 1.66 | 1.69 | 1.70 | 1.69 | 1.68 | 1.69 | 1.62 | (1.08) | (1.11) | (1.08) | (1.62) | (1.70) | (1.51) | (1.08) | (1.11) | (1.02) | (1.70) | (1.68) | (1.64) | (1.27) | (1.55) | (1.61) | (1.55) | (1.55) | (1.51) | (1.61) | (1.47) | (1.43) | (1.05) | (1.65) | (1.63) | 1.56 | 1.47 | (1.56) | (1.48) | (1.59) | (1.53) | (1.59) | (1.57) | (1.55) |
| state/success/line | decor | 1.43 | 1.45 | 1.45 | 1.44 | 1.43 | 1.44 | 1.40 | (1.19) | (1.22) | (1.20) | (1.38) | (1.45) | (1.33) | (1.19) | (1.22) | (1.13) | (1.45) | (1.43) | (1.40) | (1.02) | (1.32) | (1.37) | (1.32) | (1.32) | (1.29) | (1.37) | (1.26) | (1.21) | (1.17) | (1.36) | (1.33) | (1.32) | (1.24) | 1.36 | 1.31 | (1.37) | (1.33) | (1.38) | (1.36) | (1.35) |
| state/info/line | decor | 1.59 | 1.60 | 1.59 | 1.57 | 1.55 | 1.58 | 1.56 | (1.33) | (1.37) | (1.34) | (1.47) | (1.60) | (1.48) | (1.32) | (1.37) | (1.24) | (1.59) | (1.55) | (1.50) | (1.01) | (1.39) | (1.46) | (1.39) | (1.39) | (1.35) | (1.46) | (1.30) | (1.22) | (1.29) | (1.47) | (1.43) | (1.38) | (1.27) | (1.42) | (1.34) | 1.47 | 1.40 | (1.54) | (1.52) | (1.51) |

### graphite/light

Every theme-varied token is overridden by this variant. Shared tokens (brand, folder, paper, media, not theme-varied) are not overridden: 43.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **16.74** | **15.60** | **14.34** | **13.31** | **12.56** | **18.41** | **11.61** | (18.41) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | (1.00) | (1.06) | (1.49) | (14.34) | (12.56) | (11.61) | (3.55) | (9.16) | **10.37** | **9.16** | **9.53** | **8.79** | **10.37** | **8.79** | **11.32** | (16.33) | **10.32** | **9.65** | **10.43** | **9.82** | **10.46** | **9.87** | **10.48** | **9.89** | (6.08) | (3.75) | (2.21) |
| text/muted | text | **7.98** | **7.44** | **6.84** | **6.35** | **5.99** | **8.78** | **5.54** | (8.78) | (8.78) | (7.72) | (1.27) | (1.92) | (2.39) | (2.10) | (2.21) | (1.41) | (6.84) | (5.99) | (5.54) | (1.69) | (4.37) | **4.95** | **4.37 F** | **4.54** | **4.19 F** | **4.95** | **4.19 F** | **5.40** | (7.79) | **4.92** | **4.60** | **4.97** | **4.68** | **4.99** | **4.71** | **5.00** | **4.71** | (2.90) | (1.79) | (1.06) |
| text/subtle | text | **6.01** | **5.60** | **5.15** | **4.78** | **4.51** | **6.61** | **4.17 F** | (6.61) | (6.61) | (5.81) | (1.69) | (2.55) | (3.18) | (2.79) | (2.94) | (1.87) | (5.15) | (4.51) | (4.17) | (1.27) | (3.29) | **3.72 F** | **3.29 F** | **3.42 F** | **3.16 F** | **3.72 F** | **3.16 F** | **4.06 F** | (5.86) | **3.70 F** | **3.47 F** | **3.75 F** | **3.53 F** | **3.76 F** | **3.54 F** | **3.76 F** | **3.55 F** | (2.18) | (1.35) | (1.00) |
| text/disabled | exempt | 2.17 | 2.15 | 2.13 | 2.10 | 2.09 | 2.19 | 2.06 | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.00) | (1.02) | (1.17) | (2.13) | (2.09) | (2.06) | (1.64) | (1.98) | 2.02 | 1.98 | 1.99 | 1.97 | 2.02 | 1.97 | 2.06 | (2.16) | 2.02 | 2.00 | 2.03 | 2.01 | 2.03 | 2.01 | 2.03 | 2.01 | (1.82) | (1.61) | (1.36) |
| text/on-action | text | (1.00) | (1.07) | (1.17) | (1.26) | (1.33) | (1.10) | (1.44) | (1.10) | (1.10) | (1.03) | (10.16) | (15.31) | (19.09) | **16.74** | **17.67** | **11.23** | (1.17) | (1.33) | (1.44) | (4.71) | (1.17) | (1.03) | (1.17) | (1.12) | (1.22) | (1.03) | (1.22) | (1.19) | (1.03) | (1.04) | (1.12) | (1.03) | (1.10) | (1.03) | (1.10) | (1.03) | (1.10) | (1.80) | (3.06) | (5.63) |
| action/primary/fg | text | (1.00) | (1.07) | (1.17) | (1.26) | (1.33) | (1.10) | (1.44) | (1.10) | (1.10) | (1.03) | (10.16) | (15.31) | (19.09) | **16.74** | **17.67** | **11.23** | (1.17) | (1.33) | (1.44) | (4.71) | (1.17) | (1.03) | (1.17) | (1.12) | (1.22) | (1.03) | (1.22) | (1.19) | (1.03) | (1.04) | (1.12) | (1.03) | (1.10) | (1.03) | (1.10) | (1.03) | (1.10) | (1.80) | (3.06) | (5.63) |
| action/primary/fg-disabled | exempt | (2.17) | (2.15) | (2.13) | (2.10) | (2.09) | (2.19) | (2.06) | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.00) | (1.02) | (1.17) | (2.13) | (2.09) | (2.06) | (1.64) | 1.98 | (2.02) | (1.98) | (1.99) | (1.97) | (2.02) | (1.97) | (2.06) | (2.16) | (2.02) | (2.00) | (2.03) | (2.01) | (2.03) | (2.01) | (2.03) | (2.01) | (1.82) | (1.61) | (1.36) |
| action/secondary/fg | text | (16.74) | (15.60) | (14.34) | (13.31) | (12.56) | (18.41) | (11.61) | (18.41) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | (1.00) | (1.06) | (1.49) | **14.34** | **12.56** | **11.61** | (3.55) | (9.16) | (10.37) | (9.16) | (9.53) | (8.79) | (10.37) | (8.79) | (11.32) | (16.33) | (10.32) | (9.65) | (10.43) | (9.82) | (10.46) | (9.87) | (10.48) | (9.89) | (6.08) | (3.75) | (2.21) |
| action/quiet/fg | text | **16.74** | **15.60** | **14.34** | **13.31** | **12.56** | **18.41** | **11.61** | (18.41) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | (1.00) | (1.06) | (1.49) | (14.34) | (12.56) | (11.61) | (3.55) | (9.16) | **10.37** | **9.16** | **9.53** | **8.79** | **10.37** | **8.79** | **11.32** | (16.33) | **10.32** | **9.65** | **10.43** | **9.82** | **10.46** | **9.87** | **10.48** | **9.89** | (6.08) | (3.75) | (2.21) |
| state/danger/fg | text | **7.09** | **6.61** | **6.07** | **5.63** | **5.32** | **7.79** | **4.91** | (7.79) | (7.79) | (6.85) | (1.43) | (2.16) | (2.70) | (2.36) | (2.49) | (1.58) | (6.07) | (5.32) | (4.91) | (1.50) | (3.88) | **4.39 F** | **3.88 F** | **4.03 F** | **3.72 F** | **4.39 F** | **3.72 F** | **4.79** | (6.91) | **4.37 F** | **4.09 F** | (4.42) | (4.16) | (4.43) | (4.18) | (4.43) | (4.19) | (2.57) | (1.59) | (1.02) |
| state/warning/fg | text | **7.52** | **7.01** | **6.45** | **5.98** | **5.64** | **8.27** | **5.22** | (8.27) | (8.27) | (7.27) | (1.35) | (2.04) | (2.54) | (2.23) | (2.35) | (1.49) | (6.45) | (5.64) | (5.22) | (1.59) | (4.11) | **4.66** | **4.11 F** | **4.28 F** | **3.95 F** | **4.66** | **3.95 F** | **5.09** | (7.34) | (4.64) | (4.34) | **4.69** | **4.41 F** | (4.70) | (4.43) | (4.71) | (4.44) | (2.73) | (1.69) | (1.00) |
| state/success/fg | text | **6.92** | **6.45** | **5.93** | **5.50** | **5.19** | **7.61** | **4.80** | (7.61) | (7.61) | (6.69) | (1.47) | (2.21) | (2.76) | (2.42) | (2.55) | (1.62) | (5.93) | (5.19) | (4.80) | (1.47) | (3.78) | **4.29 F** | **3.78 F** | **3.94 F** | **3.64 F** | **4.29 F** | **3.64 F** | **4.68** | (6.75) | (4.26) | (3.99) | (4.31) | (4.06) | **4.33 F** | **4.08 F** | (4.33) | (4.09) | (2.51) | (1.55) | (1.01) |
| state/info/fg | text | **6.85** | **6.39** | **5.87** | **5.45** | **5.14** | **7.54** | **4.75** | (7.54) | (7.54) | (6.63) | (1.48) | (2.23) | (2.79) | (2.44) | (2.58) | (1.64) | (5.87) | (5.14) | (4.75) | (1.45) | (3.75) | **4.25 F** | **3.75 F** | **3.90 F** | **3.60 F** | **4.25 F** | **3.60 F** | **4.63** | (6.68) | (4.22) | (3.95) | (4.27) | (4.02) | (4.28) | (4.04) | **4.29 F** | **4.05 F** | (2.49) | (1.54) | (1.02) |
| state/danger/on-solid | text | (1.10) | (1.18) | (1.28) | (1.38) | (1.47) | (1.00) | (1.59) | (1.00) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (18.41) | (19.43) | (12.35) | (1.28) | (1.47) | (1.59) | **5.19** | (1.29) | (1.13) | (1.29) | (1.24) | (1.35) | (1.13) | (1.35) | (1.31) | (1.13) | (1.14) | (1.23) | (1.13) | (1.21) | (1.13) | (1.21) | (1.13) | (1.21) | (1.98) | (3.36) | (6.19) |
| media/text | text | (1.09) | (1.16) | (1.26) | (1.35) | (1.42) | (1.00) | (1.53) | (1.00) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (15.66) | (16.45) | (10.75) | (1.26) | (1.42) | (1.53) | (4.62) | (1.27) | (1.12) | (1.27) | (1.22) | (1.32) | (1.12) | (1.32) | (1.28) | (1.12) | (1.13) | (1.21) | (1.12) | (1.20) | (1.12) | (1.19) | (1.12) | (1.19) | **1.89 F** | **3.11 F** | **5.56** |
| border/control | ui | **4.96** | **4.83** | **4.68** | **4.54** | **4.44** | **5.12** | **4.30** | (5.12) | (5.12) | (4.90) | (1.40) | (1.06) | (1.08) | (1.00) | (1.03) | (1.31) | (4.68) | (4.44) | (4.30) | (2.44) | (3.87) | (4.10) | (3.87) | (3.94) | (3.80) | (4.10) | (3.80) | (4.27) | (4.91) | (4.09) | (3.97) | (4.11) | (4.00) | (4.11) | (4.01) | (4.11) | (4.01) | (3.16) | (2.41) | (1.72) |
| border/control-hover | ui | **11.03** | **10.46** | **9.81** | **9.26** | **8.85** | **11.83** | **8.32** | (11.83) | (11.83) | (10.76) | (1.55) | (1.08) | (1.12) | (1.00) | (1.05) | (1.42) | (9.81) | (8.85) | (8.32) | (3.17) | (6.88) | (7.61) | (6.88) | (7.11) | (6.66) | (7.61) | (6.66) | (8.18) | (10.82) | (7.58) | (7.19) | (7.65) | (7.29) | (7.67) | (7.31) | (7.67) | (7.32) | (4.90) | (3.24) | (2.03) |
| border/focus | ui | **16.74** | **15.60** | **14.34** | **13.31** | **12.56** | **18.41** | **11.61** | (18.41) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | (1.00) | (1.06) | (1.49) | (14.34) | (12.56) | (11.61) | (3.55) | (9.16) | (10.37) | (9.16) | (9.53) | (8.79) | (10.37) | (8.79) | (11.32) | (16.33) | (10.32) | (9.65) | (10.43) | (9.82) | (10.46) | (9.87) | (10.48) | (9.89) | (6.08) | (3.75) | (2.21) |
| border/strong | ui | **1.40 F** | **1.40 F** | **1.39 F** | **1.39 F** | **1.38 F** | **1.41 F** | **1.37 F** | (1.41) | (1.41) | (1.40) | (1.10) | (1.02) | (1.02) | (1.00) | (1.01) | (1.08) | (1.39) | (1.38) | (1.37) | (1.26) | (1.35) | (1.37) | (1.35) | (1.36) | (1.35) | (1.37) | (1.35) | (1.37) | (1.40) | (1.36) | (1.36) | (1.37) | (1.36) | (1.37) | (1.36) | (1.37) | (1.36) | (1.31) | (1.25) | (1.16) |
| action/primary/bg | ui | **16.74** | **15.60** | **14.34** | **13.31** | **12.56** | **18.41** | **11.61** | (18.41) | (18.41) | (16.19) | (1.65) | (1.09) | (1.14) | same | (1.06) | (1.49) | (14.34) | (12.56) | (11.61) | (3.55) | (9.16) | (10.37) | (9.16) | (9.53) | (8.79) | (10.37) | (8.79) | (11.32) | (16.33) | (10.32) | (9.65) | (10.43) | (9.82) | (10.46) | (9.87) | (10.48) | (9.89) | (6.08) | (3.75) | (2.21) |
| action/primary/bg-disabled | exempt | 1.29 | 1.28 | 1.28 | 1.27 | 1.27 | 1.29 | 1.27 | (1.29) | (1.29) | (1.28) | (1.07) | (1.01) | (1.02) | (1.00) | (1.01) | (1.06) | (1.28) | (1.27) | (1.27) | (1.19) | same | (1.26) | (1.25) | (1.26) | (1.25) | (1.26) | (1.25) | (1.27) | (1.28) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.26) | (1.22) | (1.18) | (1.12) |
| action/secondary/bg | ui | **1.17 F** | **1.09 F** | **1.00 F** | **1.08 F** | **1.14 F** | **1.28 F** | **1.24 F** | (1.28) | (1.28) | (1.13) | (8.71) | (13.12) | (16.37) | (14.34) | (15.14) | (9.62) | same | (1.14) | (1.24) | (4.04) | (1.01) | (1.03) | (1.01) | (1.04) | (1.05) | (1.03) | (1.05) | (1.02) | (1.14) | (1.02) | (1.04) | (1.03) | (1.04) | (1.03) | (1.03) | (1.03) | (1.03) | (1.55) | (2.62) | (4.83) |
| state/danger/solid | ui | **4.71** | **4.40** | **4.04** | **3.75** | **3.54** | **5.19** | **3.27** | (5.19) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (3.55) | (3.75) | (2.38) | (4.04) | (3.54) | (3.27) | same | (2.58) | (2.92) | (2.58) | (2.68) | (2.48) | (2.92) | (2.48) | (3.19) | (4.60) | (2.91) | (2.72) | (2.94) | (2.77) | (2.95) | (2.78) | (2.95) | (2.79) | (1.71) | (1.06) | (1.19) |
| canvas/ink-default | ui | (15.30) | (14.27) | (13.12) | (12.17) | (11.49) | (16.83) | (10.61) | **16.83** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (1.09) | (1.15) | (1.36) | (13.12) | (11.49) | (10.61) | (3.25) | (8.37) | (9.49) | (8.37) | (8.71) | (8.04) | (9.49) | (8.04) | (10.35) | (14.93) | (9.43) | (8.83) | (9.54) | (8.98) | (9.57) | (9.02) | (9.58) | (9.04) | (5.55) | (3.43) | (2.02) |
| content/ink-on-dark | ui | (1.02) | (1.05) | (1.14) | (1.23) | (1.31) | (1.12) | (1.41) | (1.12) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (16.42) | (17.34) | (11.02) | (1.14) | (1.31) | (1.41) | (4.63) | (1.15) | (1.01) | (1.15) | (1.10) | (1.20) | (1.01) | (1.20) | (1.16) | (1.01) | (1.02) | (1.10) | (1.01) | (1.08) | (1.01) | (1.08) | (1.01) | (1.08) | (1.77) | (3.00) | (5.53) |
| border/subtle | decor | 1.23 | 1.23 | 1.23 | 1.22 | 1.22 | 1.24 | 1.22 | (1.24) | (1.24) | (1.23) | (1.06) | (1.01) | (1.01) | (1.00) | (1.01) | (1.05) | (1.23) | (1.22) | (1.22) | (1.16) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.22) | (1.23) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.21) | (1.18) | (1.15) | (1.10) |
| canvas/rule | decor | 1.20 | 1.19 | 1.19 | 1.18 | 1.18 | 1.21 | 1.17 | 1.21 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.07) | (1.07) | (1.05) | (1.19) | (1.18) | (1.17) | (1.10) | (1.15) | (1.16) | (1.15) | (1.15) | (1.15) | (1.16) | (1.15) | (1.17) | (1.20) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.16) | (1.11) | (1.06) | (1.00) |
| state/danger/line | decor | 1.71 | 1.69 | 1.67 | 1.65 | 1.63 | 1.73 | 1.61 | (1.73) | (1.73) | (1.69) | (1.04) | (1.16) | (1.17) | (1.18) | (1.18) | (1.07) | (1.67) | (1.63) | (1.61) | (1.13) | (1.54) | (1.57) | (1.54) | (1.55) | (1.53) | (1.57) | (1.53) | (1.58) | (1.70) | 1.57 | 1.54 | (1.57) | (1.55) | (1.58) | (1.57) | (1.58) | (1.56) | (1.41) | (1.24) | (1.06) |
| state/warning/line | decor | 1.64 | 1.62 | 1.60 | 1.58 | 1.57 | 1.67 | 1.54 | (1.67) | (1.67) | (1.63) | (1.08) | (1.19) | (1.17) | (1.20) | (1.19) | (1.11) | (1.60) | (1.57) | (1.54) | (1.18) | (1.47) | (1.51) | (1.47) | (1.48) | (1.46) | (1.51) | (1.46) | (1.52) | (1.64) | (1.51) | (1.48) | 1.51 | 1.49 | (1.51) | (1.50) | (1.51) | (1.50) | (1.34) | (1.19) | (1.02) |
| state/success/line | decor | 1.62 | 1.60 | 1.58 | 1.56 | 1.54 | 1.65 | 1.52 | (1.65) | (1.65) | (1.62) | (1.10) | (1.22) | (1.19) | (1.22) | (1.22) | (1.13) | (1.58) | (1.54) | (1.52) | (1.32) | (1.45) | (1.49) | (1.45) | (1.46) | (1.44) | (1.49) | (1.44) | (1.52) | (1.62) | (1.49) | (1.47) | (1.49) | (1.47) | 1.49 | 1.47 | (1.49) | (1.47) | (1.32) | (1.16) | (1.00) |
| state/info/line | decor | 1.62 | 1.60 | 1.57 | 1.55 | 1.54 | 1.64 | 1.51 | (1.64) | (1.64) | (1.61) | (1.11) | (1.22) | (1.19) | (1.23) | (1.22) | (1.14) | (1.57) | (1.54) | (1.51) | (1.29) | (1.44) | (1.48) | (1.44) | (1.46) | (1.43) | (1.48) | (1.43) | (1.53) | (1.61) | (1.48) | (1.46) | (1.48) | (1.47) | (1.48) | (1.47) | 1.48 | 1.46 | (1.32) | (1.16) | (1.01) |

### paper/dark

Every theme-varied token is overridden by this variant. Shared tokens (brand, folder, paper, media, not theme-varied) are not overridden: 43.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **16.15** | **15.16** | **13.85** | **13.03** | **12.31** | **13.03** | **17.00** | (1.02) | (1.16) | (1.02) | (9.66) | (14.55) | (18.15) | (1.00) | (1.11) | (1.25) | (13.85) | (12.31) | (10.68) | (4.48) | (8.37) | **9.90** | **8.37** | **8.37** | **7.64** | **9.90** | **6.88** | **5.90** | (1.11) | **10.62** | **9.97** | **8.40** | **6.57** | **8.89** | **7.40** | **9.84** | **8.47** | (14.29) | (15.44) | (16.42) |
| text/muted | text | **8.38** | **7.86** | **7.19** | **6.76** | **6.39** | **6.76** | **8.82** | (1.96) | (2.23) | (1.96) | (5.01) | (7.55) | (9.42) | (1.93) | (2.14) | (1.54) | (7.19) | (6.39) | (5.54) | (2.33) | (4.34) | **5.14** | **4.34 F** | **4.34 F** | **3.96 F** | **5.14** | **3.57 F** | **3.06 F** | (1.74) | **5.51** | **5.17** | **4.36 F** | **3.41 F** | **4.61** | **3.84 F** | **5.10** | **4.39 F** | (7.42) | (8.01) | (8.52) |
| text/subtle | text | **6.36** | **5.97** | **5.46** | **5.13** | **4.85** | **5.13** | **6.70** | (2.58) | (2.94) | (2.58) | (3.80) | (5.73) | (7.15) | (2.54) | (2.81) | (2.03) | (5.46) | (4.85) | (4.21) | (1.77) | (3.30) | **3.90 F** | **3.30 F** | **3.30 F** | **3.01 F** | **3.90 F** | **2.71 F** | **2.32 F** | (2.30) | **4.18 F** | **3.93 F** | **3.31 F** | **2.59 F** | **3.50 F** | **2.92 F** | **3.88 F** | **3.34 F** | (5.63) | (6.08) | (6.47) |
| text/disabled | exempt | 3.11 | 3.13 | 3.10 | 3.05 | 3.00 | 3.05 | 3.05 | (1.05) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.05) | (1.01) | (1.14) | (3.10) | (3.00) | (2.86) | (1.75) | (2.59) | 2.78 | 2.59 | 2.59 | 2.49 | 2.78 | 2.37 | 2.20 | (1.09) | 2.83 | 2.74 | 2.59 | 2.31 | 2.65 | 2.44 | 2.77 | 2.60 | (3.01) | (2.98) | (2.96) |
| text/on-action | text | (1.00) | (1.07) | (1.17) | (1.24) | (1.31) | (1.24) | (1.05) | (16.44) | (18.69) | (16.44) | (1.67) | (1.11) | (1.12) | **16.15** | **17.90** | **12.92** | (1.17) | (1.31) | (1.51) | (3.60) | (1.28) | (1.10) | (1.28) | (1.28) | (1.40) | (1.10) | (1.57) | (1.94) | (14.61) | (1.09) | (1.17) | (1.30) | (1.71) | (1.26) | (1.57) | (1.14) | (1.35) | (1.00) | (1.00) | (1.02) |
| action/primary/fg | text | (1.00) | (1.07) | (1.17) | (1.24) | (1.31) | (1.24) | (1.05) | (16.44) | (18.69) | (16.44) | (1.67) | (1.11) | (1.12) | **16.15** | **17.90** | **12.92** | (1.17) | (1.31) | (1.51) | (3.60) | (1.28) | (1.10) | (1.28) | (1.28) | (1.40) | (1.10) | (1.57) | (1.94) | (14.61) | (1.09) | (1.17) | (1.30) | (1.71) | (1.26) | (1.57) | (1.14) | (1.35) | (1.00) | (1.00) | (1.02) |
| action/primary/fg-disabled | exempt | (3.11) | (3.13) | (3.10) | (3.05) | (3.00) | (3.05) | (3.05) | (1.05) | (1.00) | (1.05) | (2.75) | (3.12) | (2.91) | (1.05) | (1.01) | (1.14) | (3.10) | (3.00) | (2.86) | (1.75) | 2.59 | (2.78) | (2.59) | (2.59) | (2.49) | (2.78) | (2.37) | (2.20) | (1.09) | (2.83) | (2.74) | (2.59) | (2.31) | (2.65) | (2.44) | (2.77) | (2.60) | (3.01) | (2.98) | (2.96) |
| action/secondary/fg | text | (16.15) | (15.16) | (13.85) | (13.03) | (12.31) | (13.03) | (17.00) | (1.02) | (1.16) | (1.02) | (9.66) | (14.55) | (18.15) | (1.00) | (1.11) | (1.25) | **13.85** | **12.31** | **10.68** | (4.48) | (8.37) | (9.90) | (8.37) | (8.37) | (7.64) | (9.90) | (6.88) | (5.90) | (1.11) | (10.62) | (9.97) | (8.40) | (6.57) | (8.89) | (7.40) | (9.84) | (8.47) | (14.29) | (15.44) | (16.42) |
| action/quiet/fg | text | **16.15** | **15.16** | **13.85** | **13.03** | **12.31** | **13.03** | **17.00** | (1.02) | (1.16) | (1.02) | (9.66) | (14.55) | (18.15) | (1.00) | (1.11) | (1.25) | (13.85) | (12.31) | (10.68) | (4.48) | (8.37) | **9.90** | **8.37** | **8.37** | **7.64** | **9.90** | **6.88** | **5.90** | (1.11) | **10.62** | **9.97** | **8.40** | **6.57** | **8.89** | **7.40** | **9.84** | **8.47** | (14.29) | (15.44) | (16.42) |
| state/danger/fg | text | **7.34** | **6.89** | **6.29** | **5.92** | **5.60** | **5.92** | **7.73** | (2.24) | (2.55) | (2.24) | (4.39) | (6.61) | (8.25) | (2.20) | (2.44) | (1.76) | (6.29) | (5.60) | (4.86) | (2.04) | (3.80) | **4.50 F** | **3.80 F** | **3.80 F** | **3.47 F** | **4.50 F** | **3.13 F** | **2.68 F** | (1.99) | **4.83** | **4.53** | (3.82) | (2.98) | (4.04) | (3.36) | (4.47) | (3.85) | (6.50) | (7.02) | (7.46) |
| state/warning/fg | text | **11.51** | **10.81** | **9.88** | **9.29** | **8.78** | **9.29** | **12.12** | (1.43) | (1.62) | (1.43) | (6.88) | (10.38) | (12.94) | (1.40) | (1.55) | (1.12) | (9.88) | (8.78) | (7.62) | (3.20) | (5.97) | **7.06** | **5.97** | **5.97** | **5.44** | **7.06** | **4.90** | **4.20 F** | (1.27) | (7.57) | (7.11) | **5.99** | **4.68** | (6.34) | (5.28) | (7.01) | (6.04) | (10.19) | (11.01) | (11.71) |
| state/success/fg | text | **11.67** | **10.95** | **10.01** | **9.42** | **8.90** | **9.42** | **12.29** | (1.41) | (1.60) | (1.41) | (6.98) | (10.52) | (13.12) | (1.38) | (1.53) | (1.11) | (10.01) | (8.90) | (7.72) | (3.24) | (6.05) | **7.15** | **6.05** | **6.05** | **5.52** | **7.15** | **4.97** | **4.26 F** | (1.25) | (7.68) | (7.21) | (6.07) | (4.75) | **6.42** | **5.35** | (7.11) | (6.12) | (10.33) | (11.16) | (11.87) |
| state/info/fg | text | **8.60** | **8.07** | **7.37** | **6.94** | **6.55** | **6.94** | **9.05** | (1.91) | (2.17) | (1.91) | (5.14) | (7.75) | (9.66) | (1.88) | (2.08) | (1.50) | (7.37) | (6.55) | (5.69) | (2.39) | (4.46) | **5.27** | **4.46 F** | **4.46 F** | **4.07 F** | **5.27** | **3.66 F** | **3.14 F** | (1.70) | (5.66) | (5.31) | (4.47) | (3.50) | (4.73) | (3.94) | **5.24** | **4.51** | (7.61) | (8.22) | (8.74) |
| state/danger/on-solid | text | (18.69) | (17.54) | (16.03) | (15.08) | (14.25) | (15.08) | (19.67) | (1.14) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (1.16) | (1.04) | (1.45) | (16.03) | (14.25) | (12.36) | **5.19** | (9.69) | (11.46) | (9.69) | (9.69) | (8.84) | (11.46) | (7.96) | (6.82) | (1.28) | (12.29) | (11.54) | (9.72) | (7.60) | (10.28) | (8.57) | (11.38) | (9.80) | (16.54) | (17.87) | (19.00) |
| media/text | text | (15.88) | (14.98) | (13.77) | (13.00) | (12.32) | (13.00) | (16.63) | (1.13) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (1.14) | (1.04) | (1.41) | (13.77) | (12.32) | (10.76) | (4.62) | (8.53) | (10.01) | (8.53) | (8.53) | (7.81) | (10.01) | (7.07) | (6.10) | (1.26) | (10.69) | (10.06) | (8.56) | (6.76) | (9.02) | (7.57) | (9.95) | (8.62) | **14.18** | **15.24** | **16.12** |
| border/control | ui | **3.83** | **3.81** | **3.73** | **3.66** | **3.58** | **3.66** | **3.78** | (1.05) | (1.00) | (1.05) | (3.21) | (3.78) | (3.66) | (1.06) | (1.02) | (1.17) | (3.73) | (3.58) | (3.37) | (1.94) | (2.99) | (3.25) | (2.99) | (2.99) | (2.86) | (3.25) | (2.71) | (2.49) | (1.11) | (3.33) | (3.21) | (2.99) | (2.63) | (3.08) | (2.79) | (3.24) | (3.01) | (3.75) | (3.72) | (3.70) |
| border/control-hover | ui | **9.47** | **9.09** | **8.53** | **8.15** | **7.80** | **8.15** | **9.73** | (1.09) | (1.00) | (1.09) | (6.45) | (8.83) | (10.02) | (1.11) | (1.03) | (1.30) | (8.53) | (7.80) | (6.99) | (3.27) | (5.75) | (6.58) | (5.75) | (5.75) | (5.34) | (6.58) | (4.91) | (4.32) | (1.19) | (6.92) | (6.57) | (5.76) | (4.72) | (6.02) | (5.19) | (6.54) | (5.80) | (8.72) | (9.20) | (9.56) |
| border/focus | ui | **16.15** | **15.16** | **13.85** | **13.03** | **12.31** | **13.03** | **17.00** | (1.02) | (1.16) | (1.02) | (9.66) | (14.55) | (18.15) | (1.00) | (1.11) | (1.25) | (13.85) | (12.31) | (10.68) | (4.48) | (8.37) | (9.90) | (8.37) | (8.37) | (7.64) | (9.90) | (6.88) | (5.90) | (1.11) | (10.62) | (9.97) | (8.40) | (6.57) | (8.89) | (7.40) | (9.84) | (8.47) | (14.29) | (15.44) | (16.42) |
| border/strong | ui | **1.49 F** | **1.53 F** | **1.56 F** | **1.57 F** | **1.57 F** | **1.57 F** | **1.44 F** | (1.02) | (1.00) | (1.02) | (1.54) | (1.55) | (1.35) | (1.02) | (1.01) | (1.06) | (1.56) | (1.57) | (1.56) | (1.24) | (1.51) | (1.55) | (1.51) | (1.51) | (1.49) | (1.55) | (1.46) | (1.41) | (1.04) | (1.52) | (1.53) | (1.51) | (1.44) | (1.52) | (1.47) | (1.54) | (1.51) | (1.41) | (1.40) | (1.38) |
| action/primary/bg | ui | **16.15** | **15.16** | **13.85** | **13.03** | **12.31** | **13.03** | **17.00** | (1.02) | (1.16) | (1.02) | (9.66) | (14.55) | (18.15) | same | (1.11) | (1.25) | (13.85) | (12.31) | (10.68) | (4.48) | (8.37) | (9.90) | (8.37) | (8.37) | (7.64) | (9.90) | (6.88) | (5.90) | (1.11) | (10.62) | (9.97) | (8.40) | (6.57) | (8.89) | (7.40) | (9.84) | (8.47) | (14.29) | (15.44) | (16.42) |
| action/primary/bg-disabled | exempt | 1.40 | 1.43 | 1.46 | 1.47 | 1.47 | 1.47 | 1.35 | (1.02) | (1.00) | (1.02) | (1.45) | (1.45) | (1.27) | (1.02) | (1.01) | (1.05) | (1.46) | (1.47) | (1.46) | (1.20) | same | (1.45) | (1.43) | (1.43) | (1.41) | (1.45) | (1.39) | (1.35) | (1.03) | (1.42) | (1.43) | (1.42) | (1.37) | (1.43) | (1.39) | (1.45) | (1.43) | (1.32) | (1.31) | (1.30) |
| action/secondary/bg | ui | **1.17 F** | **1.09 F** | **1.00 F** | **1.06 F** | **1.12 F** | **1.06 F** | **1.23 F** | (14.10) | (16.03) | (14.10) | (1.43) | (1.05) | (1.31) | (13.85) | (15.35) | (11.08) | same | (1.12) | (1.30) | (3.09) | (1.10) | (1.02) | (1.10) | (1.10) | (1.20) | (1.02) | (1.35) | (1.67) | (12.53) | (1.01) | (1.00) | (1.11) | (1.47) | (1.08) | (1.34) | (1.02) | (1.16) | (1.03) | (1.11) | (1.19) |
| state/danger/solid | ui | **3.60** | **3.38** | **3.09** | **2.91 F** | **2.75 F** | **2.91 F** | **3.79** | (4.56) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (4.48) | (4.97) | (3.58) | (3.09) | (2.75) | (2.38) | same | (1.87) | (2.21) | (1.87) | (1.87) | (1.70) | (2.21) | (1.53) | (1.32) | (4.05) | (2.37) | (2.22) | (1.88) | (1.47) | (1.98) | (1.65) | (2.20) | (1.89) | (3.19) | (3.45) | (3.66) |
| canvas/ink-default | ui | (1.11) | (1.04) | (1.05) | (1.12) | (1.18) | (1.12) | (1.17) | **14.80** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (14.54) | (16.12) | (11.63) | (1.05) | (1.18) | (1.36) | (3.25) | (1.15) | (1.01) | (1.15) | (1.15) | (1.26) | (1.01) | (1.42) | (1.75) | (13.15) | (1.02) | (1.05) | (1.17) | (1.54) | (1.14) | (1.41) | (1.03) | (1.22) | (1.02) | (1.06) | (1.13) |
| content/ink-on-dark | ui | (16.67) | (15.65) | (14.30) | (13.46) | (12.71) | (13.46) | (17.55) | (1.01) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (1.03) | (1.07) | (1.29) | (14.30) | (12.71) | (11.03) | (4.63) | (8.64) | (10.22) | (8.64) | (8.64) | (7.88) | (10.22) | (7.10) | (6.09) | (1.14) | (10.97) | (10.29) | (8.68) | (6.78) | (9.17) | (7.64) | (10.16) | (8.74) | (14.76) | (15.94) | (16.95) |
| border/subtle | decor | 1.22 | 1.24 | 1.26 | 1.27 | 1.28 | 1.27 | 1.18 | (1.01) | (1.00) | (1.01) | (1.27) | (1.25) | (1.14) | (1.01) | (1.00) | (1.03) | (1.26) | (1.28) | (1.28) | (1.12) | (1.26) | (1.25) | (1.26) | (1.26) | (1.25) | (1.25) | (1.24) | (1.21) | (1.02) | (1.24) | (1.24) | (1.26) | (1.23) | (1.26) | (1.24) | (1.26) | (1.26) | (1.17) | (1.16) | (1.15) |
| canvas/rule | decor | 1.07 | 1.08 | 1.07 | 1.07 | 1.07 | 1.07 | 1.06 | 1.20 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.20) | (1.20) | (1.18) | (1.07) | (1.07) | (1.05) | (1.10) | (1.02) | (1.04) | (1.02) | (1.02) | (1.01) | (1.04) | (1.00) | (1.00) | (1.19) | (1.04) | (1.03) | (1.02) | (1.00) | (1.03) | (1.01) | (1.04) | (1.03) | (1.06) | (1.06) | (1.06) |
| state/danger/line | decor | 1.44 | 1.46 | 1.46 | 1.46 | 1.45 | 1.46 | 1.40 | (1.20) | (1.22) | (1.20) | (1.38) | (1.45) | (1.34) | (1.19) | (1.21) | (1.15) | (1.46) | (1.45) | (1.42) | (1.17) | (1.35) | (1.40) | (1.35) | (1.35) | (1.32) | (1.40) | (1.28) | (1.25) | (1.17) | 1.44 | 1.43 | (1.36) | (1.28) | (1.34) | (1.27) | (1.38) | (1.33) | (1.38) | (1.37) | (1.36) |
| state/warning/line | decor | 1.66 | 1.69 | 1.71 | 1.71 | 1.70 | 1.71 | 1.60 | (1.08) | (1.11) | (1.08) | (1.62) | (1.70) | (1.51) | (1.08) | (1.10) | (1.03) | (1.71) | (1.70) | (1.66) | (1.27) | (1.58) | (1.64) | (1.58) | (1.58) | (1.54) | (1.64) | (1.50) | (1.45) | (1.06) | (1.66) | (1.64) | 1.58 | 1.49 | (1.59) | (1.51) | (1.62) | (1.56) | (1.58) | (1.56) | (1.54) |
| state/success/line | decor | 1.42 | 1.44 | 1.45 | 1.44 | 1.43 | 1.44 | 1.38 | (1.20) | (1.22) | (1.20) | (1.38) | (1.45) | (1.33) | (1.20) | (1.22) | (1.15) | (1.45) | (1.43) | (1.40) | (1.02) | (1.34) | (1.38) | (1.34) | (1.34) | (1.31) | (1.38) | (1.27) | (1.21) | (1.18) | (1.36) | (1.33) | (1.33) | (1.25) | 1.37 | 1.32 | (1.39) | (1.35) | (1.37) | (1.35) | (1.34) |
| state/info/line | decor | 1.58 | 1.59 | 1.59 | 1.57 | 1.56 | 1.57 | 1.54 | (1.34) | (1.37) | (1.34) | (1.47) | (1.60) | (1.48) | (1.33) | (1.36) | (1.26) | (1.59) | (1.56) | (1.50) | (1.01) | (1.40) | (1.47) | (1.40) | (1.40) | (1.36) | (1.47) | (1.32) | (1.23) | (1.30) | (1.47) | (1.43) | (1.39) | (1.27) | (1.43) | (1.35) | 1.48 | 1.42 | (1.53) | (1.51) | (1.50) |

### paper/light

Every theme-varied token is overridden by this variant. Shared tokens (brand, folder, paper, media, not theme-varied) are not overridden: 43.

| Subject | Kind | C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8 | C9 | C10 | C11 | C12 | C13 | C14 | C15 | C16 | C17 | C18 | C19 | C20 | C21 | C22 | C23 | C24 | C25 | C26 | C27 | C28 | C29 | C30 | C31 | C32 | C33 | C34 | C35 | C36 | C37 | C38 | C39 | C40 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| text/default | text | **16.96** | **15.96** | **14.47** | **13.55** | **12.91** | **17.70** | **12.06** | (17.70) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | (1.00) | (1.13) | (1.43) | (14.47) | (12.91) | (12.06) | (3.41) | (9.48) | **10.77** | **9.48** | **9.87** | **9.10** | **10.77** | **9.10** | **11.31** | (15.70) | **10.70** | **10.00** | **10.81** | **10.16** | **10.84** | **10.20** | **10.84** | **10.21** | (6.27) | (3.84) | (2.23) |
| text/muted | text | **8.41** | **7.92** | **7.17** | **6.72** | **6.40** | **8.78** | **5.98** | (8.78) | (8.78) | (7.72) | (1.27) | (1.92) | (2.39) | (2.02) | (2.28) | (1.41) | (7.17) | (6.40) | (5.98) | (1.69) | (4.70) | **5.34** | **4.70** | **4.90** | **4.51** | **5.34** | **4.51** | **5.61** | (7.79) | **5.30** | **4.96** | **5.36** | **5.04** | **5.37** | **5.06** | **5.38** | **5.06** | (3.11) | (1.90) | (1.11) |
| text/subtle | text | **6.33** | **5.96** | **5.40** | **5.06** | **4.82** | **6.61** | **4.50** | (6.61) | (6.61) | (5.81) | (1.69) | (2.55) | (3.18) | (2.68) | (3.03) | (1.87) | (5.40) | (4.82) | (4.50) | (1.27) | (3.54) | **4.02 F** | **3.54 F** | **3.69 F** | **3.40 F** | **4.02 F** | **3.40 F** | **4.22 F** | (5.86) | **3.99 F** | **3.73 F** | **4.04 F** | **3.79 F** | **4.05 F** | **3.81 F** | **4.05 F** | **3.81 F** | (2.34) | (1.43) | (1.00) |
| text/disabled | exempt | 2.18 | 2.17 | 2.14 | 2.12 | 2.11 | 2.19 | 2.09 | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.01) | (1.03) | (1.17) | (2.14) | (2.11) | (2.09) | (1.64) | (2.01) | 2.05 | 2.01 | 2.02 | 1.99 | 2.05 | 1.99 | 2.07 | (2.16) | 2.05 | 2.03 | 2.05 | 2.03 | 2.05 | 2.03 | 2.05 | 2.03 | (1.85) | (1.64) | (1.38) |
| text/on-action | text | (1.00) | (1.06) | (1.17) | (1.25) | (1.31) | (1.04) | (1.41) | (1.04) | (1.04) | (1.09) | (10.70) | (16.13) | (20.12) | **16.96** | **19.17** | **11.83** | (1.17) | (1.31) | (1.41) | (4.97) | (1.24) | (1.08) | (1.24) | (1.18) | (1.29) | (1.08) | (1.29) | (1.25) | (1.08) | (1.09) | (1.18) | (1.08) | (1.16) | (1.08) | (1.16) | (1.08) | (1.16) | (1.90) | (3.22) | (5.93) |
| action/primary/fg | text | (1.00) | (1.06) | (1.17) | (1.25) | (1.31) | (1.04) | (1.41) | (1.04) | (1.04) | (1.09) | (10.70) | (16.13) | (20.12) | **16.96** | **19.17** | **11.83** | (1.17) | (1.31) | (1.41) | (4.97) | (1.24) | (1.08) | (1.24) | (1.18) | (1.29) | (1.08) | (1.29) | (1.25) | (1.08) | (1.09) | (1.18) | (1.08) | (1.16) | (1.08) | (1.16) | (1.08) | (1.16) | (1.90) | (3.22) | (5.93) |
| action/primary/fg-disabled | exempt | (2.18) | (2.17) | (2.14) | (2.12) | (2.11) | (2.19) | (2.09) | (2.19) | (2.19) | (2.16) | (1.22) | (1.03) | (1.04) | (1.01) | (1.03) | (1.17) | (2.14) | (2.11) | (2.09) | (1.64) | 2.01 | (2.05) | (2.01) | (2.02) | (1.99) | (2.05) | (1.99) | (2.07) | (2.16) | (2.05) | (2.03) | (2.05) | (2.03) | (2.05) | (2.03) | (2.05) | (2.03) | (1.85) | (1.64) | (1.38) |
| action/secondary/fg | text | (16.96) | (15.96) | (14.47) | (13.55) | (12.91) | (17.70) | (12.06) | (17.70) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | (1.00) | (1.13) | (1.43) | **14.47** | **12.91** | **12.06** | (3.41) | (9.48) | (10.77) | (9.48) | (9.87) | (9.10) | (10.77) | (9.10) | (11.31) | (15.70) | (10.70) | (10.00) | (10.81) | (10.16) | (10.84) | (10.20) | (10.84) | (10.21) | (6.27) | (3.84) | (2.23) |
| action/quiet/fg | text | **16.96** | **15.96** | **14.47** | **13.55** | **12.91** | **17.70** | **12.06** | (17.70) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | (1.00) | (1.13) | (1.43) | (14.47) | (12.91) | (12.06) | (3.41) | (9.48) | **10.77** | **9.48** | **9.87** | **9.10** | **10.77** | **9.10** | **11.31** | (15.70) | **10.70** | **10.00** | **10.81** | **10.16** | **10.84** | **10.20** | **10.84** | **10.21** | (6.27) | (3.84) | (2.23) |
| state/danger/fg | text | **7.47** | **7.03** | **6.37** | **5.96** | **5.68** | **7.79** | **5.31** | (7.79) | (7.79) | (6.85) | (1.43) | (2.16) | (2.70) | (2.27) | (2.57) | (1.58) | (6.37) | (5.68) | (5.31) | (1.50) | (4.17) | **4.74** | **4.17 F** | **4.35 F** | **4.01 F** | **4.74** | **4.01 F** | **4.98** | (6.91) | **4.71** | **4.40 F** | (4.76) | (4.47) | (4.77) | (4.49) | (4.77) | (4.49) | (2.76) | (1.69) | (1.02) |
| state/warning/fg | text | **7.92** | **7.46** | **6.76** | **6.33** | **6.03** | **8.27** | **5.64** | (8.27) | (8.27) | (7.27) | (1.35) | (2.04) | (2.54) | (2.14) | (2.42) | (1.49) | (6.76) | (6.03) | (5.64) | (1.59) | (4.43) | **5.03** | **4.43 F** | **4.61** | **4.25 F** | **5.03** | **4.25 F** | **5.28** | (7.34) | (5.00) | (4.67) | **5.05** | **4.75** | (5.06) | (4.77) | (5.07) | (4.77) | (2.93) | (1.79) | (1.04) |
| state/success/fg | text | **7.29** | **6.86** | **6.22** | **5.82** | **5.55** | **7.61** | **5.18** | (7.61) | (7.61) | (6.69) | (1.47) | (2.21) | (2.76) | (2.33) | (2.63) | (1.62) | (6.22) | (5.55) | (5.18) | (1.47) | (4.08) | **4.63** | **4.08 F** | **4.24 F** | **3.91 F** | **4.63** | **3.91 F** | **4.86** | (6.75) | (4.60) | (4.30) | (4.65) | (4.37) | **4.66** | **4.38 F** | (4.66) | (4.39) | (2.69) | (1.65) | (1.00) |
| state/info/fg | text | **7.22** | **6.80** | **6.16** | **5.77** | **5.50** | **7.54** | **5.14** | (7.54) | (7.54) | (6.63) | (1.48) | (2.23) | (2.79) | (2.35) | (2.66) | (1.64) | (6.16) | (5.50) | (5.14) | (1.45) | (4.04) | **4.58** | **4.04 F** | **4.20 F** | **3.88 F** | **4.58** | **3.88 F** | **4.81** | (6.68) | (4.55) | (4.26) | (4.60) | (4.33) | (4.61) | (4.34) | **4.62** | **4.35 F** | (2.67) | (1.63) | (1.01) |
| state/danger/on-solid | text | (1.04) | (1.11) | (1.22) | (1.31) | (1.37) | (1.00) | (1.47) | (1.00) | (1.00) | (1.14) | (11.17) | (16.84) | (21.00) | (17.70) | (20.01) | (12.35) | (1.22) | (1.37) | (1.47) | **5.19** | (1.29) | (1.13) | (1.29) | (1.24) | (1.35) | (1.13) | (1.35) | (1.31) | (1.13) | (1.14) | (1.23) | (1.13) | (1.21) | (1.13) | (1.21) | (1.13) | (1.21) | (1.98) | (3.36) | (6.19) |
| media/text | text | (1.04) | (1.10) | (1.21) | (1.28) | (1.34) | (1.00) | (1.43) | (1.00) | (1.00) | (1.13) | (9.77) | (14.42) | (17.62) | (15.11) | (16.88) | (10.75) | (1.21) | (1.34) | (1.43) | (4.62) | (1.27) | (1.12) | (1.27) | (1.22) | (1.32) | (1.12) | (1.32) | (1.28) | (1.12) | (1.13) | (1.21) | (1.12) | (1.20) | (1.12) | (1.19) | (1.12) | (1.19) | **1.89 F** | **3.11 F** | **5.56** |
| border/control | ui | **5.05** | **4.94** | **4.77** | **4.65** | **4.56** | **5.12** | **4.44** | (5.12) | (5.12) | (4.90) | (1.40) | (1.06) | (1.08) | (1.02) | (1.05) | (1.31) | (4.77) | (4.56) | (4.44) | (2.44) | (4.01) | (4.24) | (4.01) | (4.08) | (3.93) | (4.24) | (3.93) | (4.34) | (4.91) | (4.22) | (4.10) | (4.24) | (4.13) | (4.25) | (4.14) | (4.25) | (4.14) | (3.28) | (2.50) | (1.77) |
| border/control-hover | ui | **11.46** | **10.96** | **10.18** | **9.68** | **9.33** | **11.83** | **8.85** | (11.83) | (11.83) | (10.76) | (1.55) | (1.08) | (1.12) | (1.03) | (1.07) | (1.42) | (10.18) | (9.33) | (8.85) | (3.17) | (7.31) | (8.09) | (7.31) | (7.55) | (7.07) | (8.09) | (7.07) | (8.43) | (10.82) | (8.05) | (7.63) | (8.12) | (7.73) | (8.13) | (7.75) | (8.13) | (7.75) | (5.20) | (3.42) | (2.11) |
| border/focus | ui | **16.96** | **15.96** | **14.47** | **13.55** | **12.91** | **17.70** | **12.06** | (17.70) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | (1.00) | (1.13) | (1.43) | (14.47) | (12.91) | (12.06) | (3.41) | (9.48) | (10.77) | (9.48) | (9.87) | (9.10) | (10.77) | (9.10) | (11.31) | (15.70) | (10.70) | (10.00) | (10.81) | (10.16) | (10.84) | (10.20) | (10.84) | (10.21) | (6.27) | (3.84) | (2.23) |
| border/strong | ui | **1.40 F** | **1.40 F** | **1.39 F** | **1.39 F** | **1.39 F** | **1.41 F** | **1.38 F** | (1.41) | (1.41) | (1.40) | (1.10) | (1.02) | (1.02) | (1.01) | (1.01) | (1.08) | (1.39) | (1.39) | (1.38) | (1.26) | (1.36) | (1.37) | (1.36) | (1.36) | (1.36) | (1.37) | (1.36) | (1.38) | (1.40) | (1.37) | (1.37) | (1.37) | (1.37) | (1.37) | (1.37) | (1.37) | (1.37) | (1.32) | (1.26) | (1.17) |
| action/primary/bg | ui | **16.96** | **15.96** | **14.47** | **13.55** | **12.91** | **17.70** | **12.06** | (17.70) | (17.70) | (15.57) | (1.58) | (1.05) | (1.19) | same | (1.13) | (1.43) | (14.47) | (12.91) | (12.06) | (3.41) | (9.48) | (10.77) | (9.48) | (9.87) | (9.10) | (10.77) | (9.10) | (11.31) | (15.70) | (10.70) | (10.00) | (10.81) | (10.16) | (10.84) | (10.20) | (10.84) | (10.21) | (6.27) | (3.84) | (2.23) |
| action/primary/bg-disabled | exempt | 1.29 | 1.29 | 1.28 | 1.28 | 1.28 | 1.29 | 1.27 | (1.29) | (1.29) | (1.28) | (1.07) | (1.01) | (1.02) | (1.00) | (1.01) | (1.06) | (1.28) | (1.28) | (1.27) | (1.19) | same | (1.27) | (1.26) | (1.26) | (1.26) | (1.27) | (1.26) | (1.27) | (1.28) | (1.27) | (1.26) | (1.27) | (1.26) | (1.27) | (1.26) | (1.27) | (1.26) | (1.23) | (1.19) | (1.12) |
| action/secondary/bg | ui | **1.17 F** | **1.10 F** | **1.00 F** | **1.07 F** | **1.12 F** | **1.22 F** | **1.20 F** | (1.22) | (1.22) | (1.08) | (9.13) | (13.76) | (17.16) | (14.47) | (16.35) | (10.09) | same | (1.12) | (1.20) | (4.24) | (1.05) | (1.02) | (1.05) | (1.01) | (1.10) | (1.02) | (1.10) | (1.07) | (1.09) | (1.03) | (1.01) | (1.02) | (1.01) | (1.02) | (1.01) | (1.02) | (1.01) | (1.62) | (2.75) | (5.06) |
| state/danger/solid | ui | **4.97** | **4.68** | **4.24** | **3.97** | **3.78** | **5.19** | **3.53** | (5.19) | (5.19) | (4.56) | (2.15) | (3.25) | (4.05) | (3.41) | (3.86) | (2.38) | (4.24) | (3.78) | (3.53) | same | (2.78) | (3.15) | (2.78) | (2.89) | (2.67) | (3.15) | (2.67) | (3.31) | (4.60) | (3.13) | (2.93) | (3.17) | (2.98) | (3.17) | (2.99) | (3.18) | (2.99) | (1.84) | (1.12) | (1.19) |
| canvas/ink-default | ui | (16.12) | (15.18) | (13.75) | (12.88) | (12.27) | (16.83) | (11.47) | **16.83** | **16.83** | **14.80** | (1.51) | (1.00) | (1.25) | (1.05) | (1.19) | (1.36) | (13.75) | (12.27) | (11.47) | (3.25) | (9.02) | (10.23) | (9.02) | (9.39) | (8.65) | (10.23) | (8.65) | (10.75) | (14.93) | (10.17) | (9.50) | (10.28) | (9.66) | (10.30) | (9.70) | (10.31) | (9.71) | (5.96) | (3.65) | (2.12) |
| content/ink-on-dark | ui | (1.07) | (1.01) | (1.09) | (1.17) | (1.22) | (1.12) | (1.31) | (1.12) | (1.12) | (1.01) | **9.97** | **15.02** | **18.73** | (15.79) | (17.85) | (11.02) | (1.09) | (1.22) | (1.31) | (4.63) | (1.15) | (1.01) | (1.15) | (1.10) | (1.20) | (1.01) | (1.20) | (1.16) | (1.01) | (1.02) | (1.10) | (1.01) | (1.08) | (1.01) | (1.08) | (1.01) | (1.08) | (1.77) | (3.00) | (5.53) |
| border/subtle | decor | 1.23 | 1.23 | 1.23 | 1.23 | 1.22 | 1.24 | 1.22 | (1.24) | (1.24) | (1.23) | (1.06) | (1.01) | (1.01) | (1.00) | (1.01) | (1.05) | (1.23) | (1.22) | (1.22) | (1.16) | (1.21) | (1.22) | (1.21) | (1.21) | (1.21) | (1.22) | (1.21) | (1.22) | (1.23) | (1.22) | (1.21) | (1.22) | (1.21) | (1.22) | (1.21) | (1.22) | (1.21) | (1.19) | (1.15) | (1.10) |
| canvas/rule | decor | 1.20 | 1.20 | 1.19 | 1.19 | 1.18 | 1.21 | 1.18 | 1.21 | (1.21) | (1.20) | (1.04) | (1.08) | (1.06) | (1.08) | (1.06) | (1.05) | (1.19) | (1.18) | (1.18) | (1.10) | (1.16) | (1.17) | (1.16) | (1.16) | (1.15) | (1.17) | (1.15) | (1.18) | (1.20) | (1.17) | (1.16) | (1.17) | (1.16) | (1.17) | (1.16) | (1.17) | (1.16) | (1.12) | (1.07) | (1.00) |
| state/danger/line | decor | 1.72 | 1.70 | 1.68 | 1.66 | 1.64 | 1.73 | 1.62 | (1.73) | (1.73) | (1.69) | (1.04) | (1.16) | (1.17) | (1.17) | (1.17) | (1.07) | (1.68) | (1.64) | (1.62) | (1.13) | (1.56) | (1.59) | (1.56) | (1.57) | (1.54) | (1.59) | (1.54) | (1.59) | (1.70) | 1.58 | 1.56 | (1.59) | (1.57) | (1.60) | (1.58) | (1.60) | (1.58) | (1.43) | (1.26) | (1.07) |
| state/warning/line | decor | 1.65 | 1.64 | 1.61 | 1.59 | 1.58 | 1.67 | 1.56 | (1.67) | (1.67) | (1.63) | (1.08) | (1.19) | (1.17) | (1.20) | (1.18) | (1.11) | (1.61) | (1.58) | (1.56) | (1.18) | (1.49) | (1.53) | (1.49) | (1.50) | (1.48) | (1.53) | (1.48) | (1.53) | (1.64) | (1.52) | (1.50) | 1.53 | 1.51 | (1.53) | (1.51) | (1.53) | (1.52) | (1.36) | (1.20) | (1.03) |
| state/success/line | decor | 1.64 | 1.62 | 1.60 | 1.58 | 1.56 | 1.65 | 1.54 | (1.65) | (1.65) | (1.62) | (1.10) | (1.22) | (1.19) | (1.22) | (1.20) | (1.13) | (1.60) | (1.56) | (1.54) | (1.32) | (1.47) | (1.51) | (1.47) | (1.49) | (1.46) | (1.51) | (1.46) | (1.54) | (1.62) | (1.51) | (1.50) | (1.51) | (1.50) | 1.51 | 1.49 | (1.51) | (1.49) | (1.35) | (1.19) | (1.01) |
| state/info/line | decor | 1.63 | 1.62 | 1.59 | 1.57 | 1.56 | 1.64 | 1.54 | (1.64) | (1.64) | (1.61) | (1.11) | (1.22) | (1.19) | (1.23) | (1.21) | (1.14) | (1.59) | (1.56) | (1.54) | (1.29) | (1.47) | (1.51) | (1.47) | (1.48) | (1.46) | (1.51) | (1.46) | (1.54) | (1.61) | (1.51) | (1.49) | (1.51) | (1.49) | (1.51) | (1.49) | 1.51 | 1.49 | (1.34) | (1.18) | (1.01) |

## Where these results disagree with the docs

The docs make these claims: `design/tokens/README.md` (Contrast, under the design principles), and `design/themes/ink.md`, `graphite.md` and `paper.md` (Contrast section). I did not run `build-tokens.mjs`, so I cannot say what its guard checks. The comparison below is with the numbers the docs state.

1. **"Text roles reach 4.5:1 on every surface"** (`tokens/README.md`). This does not hold. `text/subtle` on `surface/sunken` is 4.26:1 in Ink light, 4.17:1 in Graphite light, and 4.50:1 in Paper light (4.503, so it passes by a hair). `text/muted` on `surface/sunken` is 5.66:1 and 5.54:1 in the Ink and Graphite light variants. Text on the translucent overlays is much lower. `text/subtle` on the `highlight/match` tint over `surface/strong` is 2.24:1 in Ink dark. The worst overlay value per variant ranges from 2.24 to 3.40:1.

2. **Per-theme minimum text ratio "on any surface"** (`themes/ink.md`, `graphite.md`, `paper.md`). The docs quote Ink dark 4.74, Ink light 4.65, Graphite dark 4.61, Graphite light 4.51, Paper dark 4.85 and Paper light 4.82. I reproduce all six exactly, but only when `surface/sunken` is left out and the minimum is taken over `surface/base`, `raised`, `control`, `control-hover`, `strong` and `overlay`. With `surface/sunken` included, the Ink light, Graphite light and Paper light minima fall to 4.26, 4.17 and 4.50. The docs' scope is narrower than "any surface", and the sunken surface is the difference.

3. **"Status text meets 4.5:1 alone and on its own tint"** (all three theme docs and `tokens/README.md`). Holds on `surface/base`. Status text on its own tint over `surface/base` is between 5.7 and 9.5:1 in every variant. It does not hold on the other chrome surfaces. The worst host for the tint and its hover gives these values: Ink light danger 4.17, success 4.16, info 4.13 (warning 4.51). Graphite light 4.05 to 4.41. Paper light 4.35 to 4.75. Graphite dark danger 4.37 and info 4.28. The worst host is `surface/sunken` in the light variants and `surface/strong` in the dark variants.

4. **"Control edges and the focus ring reach 3:1 on every surface"** (`graphite.md`, `tokens/README.md`). Confirmed. The minimum over the chrome surfaces for `border/control`, `border/control-hover`, `border/focus` and `action/primary/bg` is 3.66:1 (Ink dark), 4.34 (Ink light), 3.50 (Graphite dark), 4.30 (Graphite light), 3.58 (Paper dark) and 4.44 (Paper light). "Primary action reaches 3:1" also holds.

5. **"Ink reaches 7:1 on the page"** (`tokens/README.md`). Confirmed. Ink on `canvas/page` is 14.91 (Ink dark), 16.83 (Ink light), 14.38 (Graphite dark), 16.83 (Graphite light), 14.80 (Paper dark) and 16.83 (Paper light).

Not claimed in the docs, but found:

- **`border/strong` is below 3:1 everywhere**, between 1.35 and 1.57 on the chrome surfaces in the six variants. The docs use it as a hairline (`--line2`, `--dot-edge`) and do not claim 3:1. The popover and sheet elevations draw it as their 1px outline, so where that outline is the only edge, it fails 1.4.11. Seven pairs fail per variant.
- **`action/secondary/bg` is 1.00 to 1.28:1 against the chrome surfaces** in all six variants, so the fill is nearly invisible. The docs do not claim otherwise. If `border/control` is treated as the component boundary, the fill can be decorative, but that is a design decision the docs should state.
- **`state/danger/solid` fails 3:1** against `surface/control`, `control-hover`, `strong` and `overlay` in Graphite dark (2.60:1 at its worst) and Paper dark (2.75:1). It passes in the other four variants (3.09 to 3.53:1).
- **`media/text` on a light chip** is 1.89:1 in the light variants. This is a proxy, since the real backdrop is a photo. A chip over a bright thumbnail would have the same problem.

## Summary of disagreement

The token build's text claim is correct for the surfaces it names. It is not correct for the sunken surface, for overlays, or for the status tint on the lighter chrome surfaces. The per-theme minima in the theme docs match the computation exactly once the scope is limited to `surface/base` through `surface/overlay`. The docs should either name that scope or extend the guard to `surface/sunken` and the overlays. Light-theme `text/subtle` on `surface/sunken` is the one real text failure that would need a token change.
