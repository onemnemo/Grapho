# Earlier decisions

Current decisions. Where this file and [Design decisions](../DECISIONS.md) disagree, Design decisions wins.

## Foundations
- Dark by default (Ink); Graphite, Paper and a custom theme share the same token names. See Themes and language.
- One typeface (Geist), Caveat only as ink on covers and canvases.
- Five radii: 6, 10, 14, 24, full.
- Selection is white. No brand blue.
- Line icons for anything tappable, pixel art only for brand moments.

## Actions
- Buttons are pills. Primary is solid white. Primary and danger carry a leading icon, quiet and
  text buttons never do.
- Recoverable deletes happen at once with an Undo toast (6 second draining ring). Permanent
  deletes use a hold-to-confirm danger button.
- Notices sit inline in the page flow or as the centred toast, never floating in a corner.

## Library
- Layout: title, search, filter, settings, New; folders row with See all; Recent grid
  with Sort and the grid/list switch in its heading.
- Selection: selected items shrink with a white ring and check, the rest dim; a floating
  selection bar at the bottom with Move as the one solid action.
- Folders show a ••• menu on hover and touch. Long press selects.
- The New menu offers Notebook, Canvas, Folder, then Import (PDF, image, pack).
- Filters: type as plain chips, a searchable tag list with selected tags as chips, date, starred.

## Covers, folders and canvases
- One sheet per type for New and Edit, with tabs, Presets first.
- Canvas is its own type with background, pattern and size (infinite or fixed).
- The studio edits the one item it was opened from and asks before discarding changes.
- Packs show as tiles with cover art and categories inside; disabling or uninstalling a pack never
  changes existing covers.
- A `.graphopack` can hold several kinds of content; the import sheet shows what is inside.

## Note screen
- The floating bar is the default; a full line bar is an option. Customisation lives in one
  Settings overlay, reachable from the note's ••• menu and the library.
- Shape detection suggests ("Did you mean") instead of snapping silently.

## Pen wheel
- The app's wheel geometry and gestures, with pens in the middle, solid flat ring bands that read
  apart at a glance, no loose buttons around it.
- Editing a tool happens inside the wheel: its segment grows into size and opacity. No floating
  labels.

## Themes and language
- Themes: Ink (the default, today's look), Graphite (a softer dark), Paper (warm light) and Custom.
  `tokens.css` is in three layers: raw palette (`--p-*`), one block per theme setting the base semantic
  tokens, then derived tokens (lines, hover, press, selection) mixed from `--tint` with `color-mix`.
  Components only read semantic tokens.
- Custom has four inputs: accent, background, surface and page colour. Everything else is derived in
  CSS: surfaces are mixes of background and surface, and `--tint` flips between white and ink from the
  lightness of the background (relative colour syntax), so text stays readable on any choice. No colour
  library. Accent drives the focus ring and the on state of switches and sliders. The primary Button
  stays the solid text colour, white on dark themes.
- The pen wheel, studio canvas and note page keep their own dark and paper colours in every theme. They
  are working surfaces, not chrome.
- ThemeEditor: tiles with a live mini preview of the app (drawn from the real tokens, so it cannot
  drift), then Customise opens a large live preview, four colour swatches and the colour disc. Reset
  returns to the theme it started from.
- Selection style everywhere: a soft white fill (10 to 14%) with label and glyph at full opacity, fill
  on hover, no borders at rest, no solid white except the primary Button. Tokens `--sel-fill`,
  `--sel-fill-hover`, `--hover-fill`. Applied to OptionTile, SegmentedControl (the thumb), Chip, Swatch
  (a halo, not a ring), Tabs (pills, no underline) and Menu rows.
- Localisation without a library: `src/i18n` holds a typed English catalogue, partial catalogues for nb,
  fr, de, es, hi, it and sv that fall back to English, and a pseudo-locale `xx` generated from English
  (accents, about 35% longer). `useT()` and `LocaleProvider`. Components get their strings by key;
  screens and blocks are converted as they are touched. Rule in CONVENTIONS.md.
- Language picker: a full height list, never a dropdown. Each language in its own name with the English
  name small beneath, device languages pinned at the top, the current one ticked, a search field only
  above 12 languages. Hindi and other Indic scripts render through a Noto Sans Devanagari fallback in the
  font stack.
- Storybook has Theme and Language toolbars. Check new work in Ink, Paper and the `xx` language.

## Packs and extensions
- **Content and behaviour.** A pack is content only (pens, colour sets, covers, folder styles, stickers,
  templates, themes) in a .graphopack file, any mix in one pack. An extension adds behaviour (a panel or
  tool, sandboxed web code) in a .graphoext file. The two words are never mixed in the UI.
- **Browsing.** Content is browsed by type, not by pack. Settings has one Content page per type, with Sort
  (Recently used, Name, Newest) and a source filter (All, Built in, Your own, then each pack). Packs is its
  own page and lists packs as sources: turn on or off, export, remove.
- **Extension groups.** Extensions are grouped by plain headings: Built in, From Grapho, Installed by you.
  The group tells the source, so no badge sits on a row. Opening a sideloaded one shows what it can access.
- **Turning off and removing.** Turning a pack off or removing it never breaks a note. A note keeps its own
  copy of what it uses. The pack only leaves the pickers, and turning it on again brings it back. The confirm
  says so in one sentence.
- **Making packs.** People can make packs. New pack picks items across types in tabs with checkable tiles,
  then a name and cover, then exports a .graphopack. Each Content page also has Select with Export as pack.
- **Visual weight.** One solid white primary button per surface, and solid white is only for primary buttons.
  Selected is a soft white fill (10 to 14%) with the label at full opacity, never a solid or ringed tile.
  Tiles have no border at rest and fill on hover. Actions look like icon and label rows or buttons, options
  look like tiles or segments. Quiet labels are 11 to 12px grey medium. Space separates sections, not lines.
  Switches are on in a tinted track with a full thumb, so a column of them stays calm.
- **Counts and badges.** No redundant subtext or summary badges ("6 packs, 1 extension on", "8 of 8", "Removed").
  A count that matters is part of a control: a filled ring of slots, "Export 14 items", a source filter count.
- **Reordering.** Reorderable lists drag, by a handle that shows on hover and by long press on touch. No arrows.
  The item stays under the finger and the others make room. Turning one off moves it to its new place.
- Selection on the note: one floating bar rides on the selection and the toolbar stays as it was. The bar
  has colour, thickness, copy, duplicate and More. Cut, order, layer, flip, lock and convert are in More.
  The toolbar no longer has a selection mode.
- Settings places the real theme editor and language picker. Choosing a theme repaints the app and choosing
  a language switches it at once, in the Settings story as in the app.
