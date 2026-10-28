# Grapho brand guidelines

Grapho is a notes app people live in for hours. The interface should disappear while writing and
feel finished everywhere else.

## Character

- **Calm.** Few things on screen, quiet greys, strong contrast where it matters. Nothing blinks,
  bounces or glows while the pen is down.
- **Rich where it counts.** Covers, folders, presets and stickers are big, named and visual. A
  screen is never a plain settings form when it could show the thing itself.
- **Familiar first.** Samsung Notes is the baseline mental model: same places for things, then fix
  what annoys.
- **Customisable, never demanding.** Layouts, tools, colours, covers and packs can all change, and
  the defaults are good enough that nobody has to.

## Logo

The swash mark: one stroke with three uneven peaks that swings back under itself as an underline,
ending in the segmented tail (pink `--pink`, red `--red`, orange `--orange`). White stroke on the Ink
tile on dark backgrounds, navy on light ones. Use `Logo` with the variant for the background. Never
redraw, recolour, stretch or add effects.

## Colour

- **Dark by default.** Ink is the default theme; Graphite, Paper and Custom come from the same tokens. Near-black page (`--bg`), four surface steps (`--s1` to `--s3`, `--overlay`).
- **Content has the colour, chrome is grey and white.** Covers, folders and ink are colourful.
  Buttons, bars and icons are white and grey. The pink to orange tail gradient (`--tail`) is the only
  brand colour in chrome, used for progress and one drawn underline.
- **Selection is white**: a soft white fill with the label at full opacity, never a solid tile or a border. There is no blue accent.
- Danger is a soft red tint (`--danger`, `--danger-bg`) and only on destructive actions.

## Type

Geist everywhere, at the styles in the tokens: display (screen titles), title (sheets and
dialogs), heading (sections), body, label (buttons, rows), caption (meta) and micro (counts).
Caveat appears only as handwriting on covers and canvases. Headings use tight negative tracking.

## Shape and depth

Five radii: 6, 10, 14, 24 and full. Buttons are pills. Sheets, dialogs and big panels use 24.
Surfaces are flat and solid; depth appears only where something floats (menus, sheets, the pen
wheel, the floating bar), using the elevation tokens. No glass, blur-heavy frosting or glossy
gradients on controls.

## Iconography

- **Line icons** for everything you tap: 24 grid, 1.75 stroke, round caps and joins, current
  colour, shown at 16, 18 or 20 inside controls. One drawing per name in the icon library.
- **Pixel art** only for brand moments: the mascot, empty states, onboarding and sticker packs.

## The mascot

A 13 by 14 pixel creature with a shimmering rainbow body, drawn from the sprite in
`src/assets/mascot`. It never gets a name, is never redrawn, and appears in empty states, first run
and rare celebrations, never in tool chrome. It may carry one tiny lowercase speech bubble.

## Motion

Quick and soft (`--d-fast`, `--d-base`), with a gentle spring for things that pop in. Selection
slides rather than flickers. Everything respects reduced motion.

## Voice

Plain, short and calm. Say what it is: "Move to", not "Relocate". Errors say what went wrong and
what to do next. No emoji, no exclamation marks, no em or en dashes, no cute lines except the
mascot's bubble. "Preset" is a look for a cover, folder or canvas; "Template" is for pages and
paper.

## Never

- A beta look: default platform controls, mixed radii or fonts, stock icons.
- Generic generated-looking chrome: status chips, notices floating in corners, tag pills above
  headings, gradient section backgrounds, glassy everything.
- Busy bars: more than four things on the right side of a bar.
- Paywall signals: lock icons, upsell banners, "Pro".
