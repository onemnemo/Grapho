# Custom and pack themes

How a user theme or a theme pack becomes a full set of semantic colours, and how the app keeps it
readable and recoverable. Every rule is data and
arithmetic, so the CSS build, Compose, WinUI and SwiftUI compute the same result.

**Normative sources.** This document explains the rules. The reference implementation
`themes/derive-theme.mjs` and the golden vectors `themes/vectors.json` are normative: a shell is
correct when it reproduces every vector exactly, and CI in every shell runs them. The pair list the
guard uses is the same matrix the built-in themes pass, in `tokens.json` under
`$extensions.grapho.pairs` and rendered in [contrast-matrix.md](contrast-matrix.md).

## What a theme is

A theme is a map from semantic colour token to a colour. Built-in themes write every value as a
primitive alias in `tokens.json`. A user or pack theme supplies a few inputs and the rules below
produce the map with literal colours. Only semantic colours can change. Type, space, radius, motion,
size and layer tokens belong to the design. The pen wheel, folder colours, page colours a person can
pick and media scrims are content and do not change either.

## Theme pack data

```json
{
  "rules": 1,
  "id": "harbour",
  "name": "Harbour",
  "scheme": "auto",
  "inputs": { "background": "#EAF0F4", "surface": "#D5E0E8", "accent": "#1F6F8B", "page": "#FFFFFF" },
  "overrides": { "text.subtle": "#4A5560" }
}
```

- `rules` is the rules version the pack was written for. It is **frozen**: version 1 means the
  derivation in this document forever. A newer version adds `derive-theme` rules under a new number; it
  never edits an old one. A pack that names a version the app does not know is shown as unavailable
  ("Needs a newer version of Grapho") and never partly applied.
- `background` is required; `surface`, `accent`, `page`, `scheme` (`auto`, `dark`, `light`) and
  `overrides` are optional. Input colours are opaque `#RRGGBB`. Anything else rejects the theme.
- `overrides` maps a themable semantic token (dotted path) to `#RRGGBB` or `#RRGGBBAA`. See "Overrides".

| Input | Drives | Default |
|---|---|---|
| `background` | `surface/base`, the whole surface ladder, the scheme | none, required |
| `surface` | `surface/control` | derived from `background` |
| `accent` | `action/primary/*` only: the solid button, the on state of switches and sliders | `text/default` |
| `page` | `canvas/page` | the base variant's page |

The accent never drives the focus ring, selection, hover, links or status colours. Chrome stays calm
(principle 5) and the focus ring stays visible whatever the person picks.

## Determinism

The derivation is a fixed sequence of steps with fixed step counts, so two shells cannot diverge.

- **8 bit rounding.** Every derived colour is rounded to 8 bit sRGB immediately, with
  half-up rounding of the unclamped channel (`floor(x + 0.5)`), then clamped to 0 to 255. The next
  step reads the rounded value, never the unrounded one. Tint alpha bytes are `floor(percent * 2.55 + 0.5)`.
- **OKLab.** The published Ottosson sRGB matrices, as in `tokens/contrast.mjs`. Lightness, chroma and hue are
  rounded to 6 decimals after every conversion, so cube root and power differences between runtimes
  cannot change an 8 bit result except at exact ties (the vectors catch these).
- **Gamut fitting.** Keep lightness and hue, bisect chroma exactly 24 times between 0 and the target,
  keep the in-gamut end, then round. A colour is in gamut when every linear channel is in
  [-0.0001, 1.0001].
- **Contrast.** WCAG 2.2 relative luminance and ratio on the rounded 8 bit values. Alpha tokens are
  composited over the host in gamma encoded sRGB, unrounded, then measured.
- **Iteration.** Background moves: at most 15 steps of 0.01 lightness. Secondary text fit: at most 16
  evaluations, `t` reduced by 0.02 each. Accent moves: at most 30 steps of 0.01. Status colours: at most
  60 steps of 0.01. When a limit is reached the step fails in the way stated below. Ties and orders
  are by the sorted token path.
- **Poles.** The light pole is `text/default` of Ink dark and the dark pole is `text/default` of
  Ink light.

## Derivation, rules version 1

1. **Scheme.** `scheme` if given. Otherwise dark when the contrast of `background` against the light
   pole is at least its contrast against the dark pole. This is the same ordering as luminance, with
   no cliff: the guard below handles mid greys. The theme's **base variant** is Ink dark for a dark
   scheme and Ink light for a light one. Every token the rules do not compute, including any token
   added after rules version 1, takes the base variant's value. It never falls back to the opposite
   scheme.
2. **Chroma limit.** Every chrome surface has OKLCH chroma at most **0.04**. A `background` or
   `surface` above that is pulled down to 0.04 at the same lightness and hue, and "Background toned
   down" is recorded. A neon `#FF00FF` background becomes a tinted pink grey. Page and accent are
   content: they are not limited, only checked.
3. **Surface ladder** from the background's lightness `L0`, direction `d` = +1 dark, -1 light:
   raised `L0 + d*0.03`, control `L0 + d*0.06` (or the `surface` input), control-hover control `+ d*0.02`,
   strong control `+ d*0.05`, overlay dark `L0 + 0.08` / light `min(1, L0 + 0.05)`, sunken dark
   `max(0, L0 - 0.08)` / light the strong surface. Hue is the background's.
4. **Text.** `text/default` is the pole. `text/muted` and `text/subtle` mix the pole toward
   `surface/base` in OKLab at `t` = 0.30 and 0.42 (dark) or 0.26 and 0.36 (light).
   a. **Pole check.** With muted and subtle equal to the pole, every pair of `text/default` in the
      matrix must pass. If not: drop a `surface` input if one is still in use ("Surface matched to the
      background"); else move the background lightness one step away from the pole; after 15 steps,
      **reject** ("unreadable").
   b. **Fit.** Reduce `t` for muted, then subtle, by 0.02 until every matrix pair of that token passes.
      Subtle is never lighter than muted. If `t` reaches 0 the role equals the pole and "Secondary text
      strengthened" is recorded.
5. **Tints.** Hover, press, selection, disabled, drag and read-only fills keep the base variant's tint
   and opacity. `border/control` is the smallest tint opacity from 40% in steps of 5 that reaches 3:1
   on all six chrome surfaces; `border/control-hover` is that plus 25 points, capped at 100%.
   `border/focus`, `selection/indicator`, the drop target line and the insertion line are `text/default`;
   `border/focus-gap` is `surface/base`.
6. **Accent.** If given, it is the primary background; the on-action colour is whichever pole contrasts
   more. The accent must reach 3:1 on base, raised, control and overlay, and the on-action colour 4.5:1
   on it and on hover and pressed. Hover is the accent moved 0.04 lightness toward the pole, pressed 0.04
   away; a state that fails 4.5:1 uses the accent unchanged. If the matrix fails, move the accent one
   step toward the pole (up to 30). Still failing: drop it, use the base variant's primary, record
   "Accent not used". The theme is accepted.
7. **Status colours.** OKLCH hue and chroma: danger 29 and 0.17, warning 85 and 0.13, success 155 and
   0.13, info 255 and 0.14. Start at lightness 0.78 (dark) or 0.45 (light) and step 0.01 toward the pole
   until every pair of that token passes (up to 60, then **reject**). Tints are the colour at 12% and 16%
   (dark) or 8% and 12% (light); `border/error` and `border/warning` equal the foreground. The solid
   danger fill starts at lightness 0.52, chroma 0.19, hue 29 and darkens by 0.01 until its white text
   reaches 4.5:1.
8. **Page.** The `page` input or the base variant's page. The pen colour is whichever of the two pen
   colours has the higher contrast on it, which must be at least 7:1; if not the page input is replaced
   and "Page colour replaced" is recorded. The marquee colour is the best of the blue, the light pole and
   the dark pole on the page.
9. **Overrides.** Applied last, in sorted token order, each guarded exactly like an input:
   an unknown or non-themable token is ignored; a surface override above the chroma limit is toned down;
   the whole matrix is then run with the override in place, and an override that causes any graded pair
   to fail is dropped. Each outcome is recorded.
10. **Final verification.** The whole matrix is run on the result. A failure here is a defect in the
    rules, not in the input: the theme is **rejected** ("verification-failed"), the default theme is
    used and a diagnostic is logged.

## Every load

A saved theme is re-evaluated each time the app loads it, with the pack's frozen `rules` version and
the **current** matrix. A pair added to the matrix in a later release therefore guards old themes
too, and a stored result is only a cache keyed on the rules version and the matrix and token-set
hashes. If a re-check changes the result it is applied silently; if it now rejects, the default theme
is used and the person sees "Your theme could not be loaded. Using the default."

## Outcomes and what the person sees

| Outcome | Condition | What the person sees |
|---|---|---|
| Accepted | No step changed an input | Nothing extra. The theme applies. |
| Adjusted | One or more adjustments | A quiet line under the inputs, "Adjusted so text stays readable", with a "What changed" list in plain words and a marker on each input swatch whose used value differs, with entered and used colours side by side. It can be saved and applied. There is no "use as entered". |
| Rejected | Not a colour, rules too new, unreadable, verification failed | The theme does not apply and the previous theme stays. The field at fault shows what to change. A pack shows the theme as unavailable and the rest of the pack keeps working. |

| Code | Text |
|---|---|
| (summary) | Adjusted so text stays readable. |
| `background-toned-down` | Background colour toned down. |
| `background-lighter`, `background-darker` | Background made lighter. Background made darker. |
| `surface-matched` | Surface matched to the background. |
| `muted-text-strengthened`, `subtle-text-strengthened` | Secondary text made stronger. |
| `accent-lighter`, `accent-darker` | Accent made lighter. Accent made darker. |
| `accent-dropped` | Accent not used, it is too close to the background. |
| `page-replaced` | Page colour replaced, handwriting would not be readable on it. |
| `override-ignored:*`, `override-toned-down:*`, `override-dropped:*` | One advanced setting was ignored, toned down or not used. |
| `invalid-colour:*` | That is not a colour. Use six hexadecimal digits after the hash sign. |
| `unreadable`, `verification-failed` | These colours cannot be made readable. Try a lighter or darker background. |
| `rules-too-new` | Needs a newer version of Grapho. |

## The way back

The default theme is Ink following the system light or dark setting. These routes always work.

1. **Appearance settings.** The built-in families are the first tiles of the picker, above every custom
   and pack theme, and choosing one replaces whatever is active. The custom editor has a visible "Reset to
   default theme".
2. **Keep this theme?** Applying a custom or pack theme shows a banner with Keep and Revert for 10
   seconds. Ignoring it reverts, like a display resolution change, and the theme stays in the list. The
   banner pauses while focused. It is not shown for built-in themes.
3. **Command.** `theme.reset` ("Reset theme") is a registered command with a keyboard path, reachable
   from command search on every screen, including the note screen.
4. **Safe mode.** Starts with the default theme and with packs and extensions off. Notes open
   normally; nothing is deleted. A strip stays at the top: "Safe mode. Themes and packs are off." with
   "Restore default theme" and "Leave safe mode". The strip and the Appearance screen in safe mode use
   values compiled into the shell from the Ink variants, never the user theme store. Entry points:
   Android, launcher shortcut "Open in safe mode"; Windows, hold Shift at launch or `--safe-mode`; iPad,
   home screen quick action; macOS, hold Option at launch.
5. **Automatic.** If the app does not reach the first frame twice in a row within five minutes of a
   theme or pack change, the next start is in safe mode and the strip says "Grapho started in safe mode
   because the last start did not finish."
6. **Unreadable store.** A saved theme setting that cannot be read, or fails the rules, loads the
   default theme with the message above.

"Restore default theme" never deletes a custom theme or a pack. It only changes which is active.

## Vectors

`themes/vectors.json` holds 16 inputs with exact expected outputs (outcome, adjustment list, and the
full token map): a light pack that is accepted unchanged, a neon magenta and a neon green background,
mid grey 808080, forced dark on mid grey 777777 (rejected, unreadable), pure black, pure white, a
surface that fights a black background, a saturated blue, an accent equal to the surface, a page too
close to the ink, guarded overrides (including an opaque hover fill, a neon raised surface and an
unknown token), a yellow background with a pale accent, a navy with a bright accent, a pack from a
newer rules version, and a malformed colour. The vectors were generated by the reference
implementation and reviewed by hand; `build-tokens.mjs` fails if the implementation and the file
disagree. Regenerate with `--write-vectors` only for an intentional rules change, which needs a new
`rules` version.
