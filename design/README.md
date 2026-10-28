# Grapho design

The living design system for Grapho: tokens, the asset library, components, blocks and complete
screens, all explored in Storybook. It is the visual source of truth for every platform shell.

```sh
cd design
npm install
npm run storybook   # http://localhost:6006
npm run check       # guard rails and type check, run before every commit
```

Storybook has a Theme toolbar (Ink, Graphite, Paper, Custom) and a Language toolbar (English, Norwegian, French, German, Spanish, Hindi, Italian, Swedish and a pseudo-locale). Check new work in Ink, Paper and the pseudo-locale.

Read [CONVENTIONS.md](CONVENTIONS.md) before adding or changing anything, and
[docs/branding.md](docs/branding.md) for the visual identity.

## Layout

| Folder | What lives there |
|---|---|
| `src/tokens/` | `tokens.css`: every colour, type style, space, radius, size, shadow and timing as a CSS variable |
| `src/styles/` | the global reset; imports the tokens |
| `src/assets/` | icons (`Icon` + one path per name), the logo, the pixel mascot, cover and folder artwork |
| `src/components/` | primitives: Button, IconButton, Segmented, Tabs, Toggle, Checkbox, Chip, TextField, Sheet, Dialog, Popover, Menu, Toast, AppFrame and so on |
| `src/blocks/` | product pieces built from components: folder and cover tiles, the note bar, the selection bar, the pen wheel, the settings overlay |
| `src/screens/` | complete screens composed from blocks and components, with no visual styling of their own |
| `src/lib/` | helpers without a look: hooks, `cx`, anchoring, `OverlayLayer`, the studio's editor logic |
| `src/stories-support/` | Storybook-only scaffolding: stages, demos, backdrops |
| `docs/` | branding, decisions |
| `scripts/` | `check-design.mjs`, the guard rails |

Change a token and every component follows. Change a block and every screen that uses it follows.
