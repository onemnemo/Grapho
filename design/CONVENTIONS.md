# Conventions

Rules for everyone who designs in this workspace. They exist so that the next screen reuses what is
here instead of growing a slightly different copy of it.

## Before you add anything

1. Search Storybook and `src/` for the thing you need. If it exists, use it.
2. If it almost exists, extend it with a prop or a variant. Do not fork it.
3. Only if nothing fits, add a new component, in the lowest layer that makes sense.

## Layers

`tokens` → `assets` → `components` → `blocks` → `screens`. A layer may import from layers to its
left, never to its right.

- **components** are product-agnostic primitives. They know nothing about notes or folders.
- **blocks** are Grapho pieces (FolderTile, CoverTile, NoteBar, SelectionBar, PenWheel,
  SettingsOverlay). They compose components.
- **screens** compose blocks and components and only do layout. A screen's CSS may place things
  (grid, flex, gap, padding from tokens) but never style them: no colours, borders, radii or
  shadows. If a screen needs a new look, it is a new variant of a block or component.

## Folders and files

Every component and block is one folder named in PascalCase:

```
src/components/Button/
  Button.tsx            the component, named export
  Button.module.css     its styles, CSS Modules
  Button.stories.tsx    its stories, colocated and auto-discovered
  index.ts              re-exports the component and its types
```

Screens have `Name.tsx` and `Name.stories.tsx`. Story titles follow the folder:
`Components/Button`, `Blocks/FolderTile`, `Screens/Library`.

## Where shared code lives

- `src/components`, `src/blocks`, `src/screens`: anything a person sees in the product, including
  frames, rows, close buttons and number animations. It has a folder, a story and an index, like
  everything else.
- `src/lib`: helpers with no look of their own. Hooks (`usePresence`, `useControllable`), pure
  functions (`cx`, `anchor`, `tokenMs`), the studio's editor logic, and `OverlayLayer`, the scrim,
  focus trap and exit that Sheet, Dialog and the wheel's panels share. If it renders something the
  user would recognise, it is a component, not a lib file.
- `src/i18n`: the translation catalogue and `useT()` (see Localisation below). No look of its own.
- `src/stories-support`: things that exist only to show other things in Storybook: `StoryStage`,
  `AnchorDemo`, `Surface`, `NoteStage`, the sheet demo and the wheel's note stage. Nothing in
  `components`, `blocks` or `screens` may import from it, except `*.stories.tsx` files and the
  screens that are themselves showcases (Studio sheets).
- Overlays take a `placement` (or `boundary` for Popover) of `viewport` or `container`. Use
  `container` inside a framed screen or a story so the overlay stays in its frame.

## Styling

- Every value comes from `src/tokens/tokens.css`: `var(--s2)`, `var(--r-lg)`, `var(--t-label)`.
  No hex, rgb or hsl anywhere else; `npm run check` fails on them. Artwork data under
  `src/assets/` is the only exception.
- A value that is missing becomes a new token first, with a comment saying where it is used.
- Styles live in the component's CSS Module. No inline styles except values computed at runtime
  (a position, a progress percentage, a user's chosen colour).
- Icons come from `Icon` and `src/assets/icons/iconPaths.ts`. Never paste an SVG into a component.
- Use the existing type styles (`--t-display` to `--t-micro`). Never set a font size by hand.
- Interaction states are `:hover`, `:active`, `:focus-visible`, `:disabled`, plus `aria-*` or
  `data-state` attributes for selected and open. No `is-*` classes passed in from outside.

## Themes

- Every colour a theme can change is a semantic token (`--bg`, `--s1` to `--s3`, `--fg`, `--mute`,
  `--dim`, `--tint`, `--accent`). Components use those and never a palette value. Raw palette values
  (`--p-*`) are read only by the theme blocks in `tokens.css`.
- Lines, hover, press and selection fills are mixed from `--tint` (white on dark themes, ink on light
  ones), so a new theme sets about twenty base tokens and the rest follows. Do not write
  `rgba(255,255,255,.x)` in a component or a new token: use `--line`, `--hover-fill`, `--sel-fill`.
- Themes are `[data-theme="..."]` blocks: ink (default), graphite, paper, and `custom`, which derives
  everything from four inputs (accent, background, surface, page). `lib/theme.ts` applies one.
- Never assume the page is dark: no `--white` or a fixed black for chrome, no text colour that only
  works on dark. Check a new component with the theme switch in the Storybook toolbar (Ink, Graphite,
  Paper).
- Selection is a soft fill, not a solid: `--sel-fill` (about 12% of `--tint`) with the label and glyph
  at full opacity, `--hover-fill` under the pointer, no borders at rest. Solid `--fg` is for the primary
  Button only. This applies to tiles, segments, chips, tabs, menu rows, swatches and list rows.

## Localisation

- No visible string is written in a component or a screen. Every one is a key in `src/i18n/en.ts`
  (`area.thing`, e.g. `language.search`) read with `const t = useT(); t('language.search')`.
  Values may hold `{name}` placeholders: `t('language.count', { n: 40 })`. Plurals get two keys.
- Text that is the user's own (a note title, a folder name) or story sample content stays literal.
- A string that a parent can reasonably pass in (`label`, `children`) stays a prop. The default
  value of an accessible name or empty state text comes from `t`.
- Add the key to `en.ts` first; other catalogues are partial and fall back to English, so a missing
  translation never shows a key. The pseudo-locale `xx` is generated from English (accented, about 35%
  longer): use it in the Storybook toolbar to find text that does not fit.
- Layout must survive a longer language: allow wrapping or ellipsis, no fixed widths on text, and use
  logical properties (`text-align: start`, `padding-inline`) so right-to-left can follow.
- Show a language by its own name, with the English name small beneath. Hindi and other scripts the
  main font lacks need a fallback in the `--sans` stack (Noto Sans Devanagari is there).

## Components

- Function components with named exports and a typed `Props` interface. Spread the remaining
  native props onto the root element.
- Variants and sizes are string unions (`variant="primary"`), not booleans.
- Every prop that someone may want to try gets a Storybook control. Every variant, size and state
  gets a story.
- Accessible by default: real buttons, labels on icon-only controls, focus visible, 44px targets.
- Respect `prefers-reduced-motion`. Motion timings come from the motion tokens.

## Copy

Plain, short and calm, as in [docs/branding.md](docs/branding.md). No emoji, no exclamation marks,
no em or en dashes. "Preset" is a look for a cover, folder or canvas. "Template" is for pages and
paper.

## Before you commit

`npm run check` passes, the stories you touched render without console errors, and anything new
has a story.
