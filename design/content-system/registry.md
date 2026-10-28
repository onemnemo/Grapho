# Registry

Two registries drive the content system. The **content type registry** says what a pack can hold and
how pickers, managers and importers treat it. The **slot registry** says where an extension can add
to the UI. Both are data read by existing surfaces (principle 4).

## What "no new screens" means

- A new type that uses an **existing renderer kind** needs a registry entry and, if it has one, a
  `def` schema. No shell code, no new screens.
- A new type that needs a **new renderer kind** needs shell code for that kind in each shell, and
  still no new screens.
- Three points always need engineering and are listed, not hidden: a new **renderer kind**, a new
  **`appliesTo` target** (a new place an item can be applied; closed list below), and a new
  **validator** (closed list below). `embedAs`, `holders` and `keptCopy` are enumerations whose
  behaviour is written once in the core.
- Labels ship in an app release like any other string.
- Test of the claim: add a sixth example type before release (brush presets, or shape stamps) and
  see that only the entry, a schema and, for stamps, the image tile kind are needed.

## Entry shape

| Field | Type | Meaning |
|---|---|---|
| `id` | string | Stable, lowercase, append only. Written in manifests and documents |
| `schemaSince` | integer | First manifest schema that knows it |
| `label` | string key | Singular and plural user labels |
| `icon` | `GraphoIcon` name | Tabs, New pack, source filter |
| `assetFormats` | list | Accepted main asset formats |
| `def` | field table | Name of a field table in this file; the core generates the JSON Schema from it |
| `rendererKind` | `swatch`, `image tile`, `cover`, `pen sample`, `page thumbnail`, `theme preview` | The fixed set from SYSTEM-MAP 5 |
| `previewSize` | `small` 48, `medium` 72, `large` 120, `wide` 160 by 100 (dp) | |
| `views` | list | `grid`, `list` |
| `editable` | `none` or editor id | The editor behind Edit and New |
| `appliesTo` | list from: `page`, `page-background`, `notebook`, `folder`, `tool`, `palette`, `app`, `new-document` | Closed |
| `embedAs` | `item`, `parameters`, `none` | `item`: embedded as an item record. `parameters`: the resolved values are stored where used. `none`: not in documents |
| `holders` | list from: `wheel-slot`, `quick-slot`, `saved-look`, `default`, `active-theme`, `folder`, `editor` | Which library holders can reference this type. Registered with the core reference table; Remove enumerates them generically |
| `keptCopy` | `copy-item`, `copy-with-resources`, `none` | What Remove makes for an item still held |
| `sortKeys` | list | From `recent`, `name`, `newest` and type keys |
| `packable` | boolean | |
| `importFormats` | list | Non-pack files the type can turn into items (images into stickers) |
| `defaultItem` | built-in id or none | Always stays visible |
| `placeholder` | placeholder id | Shown for a damaged or blocked asset |
| `validators` | list from: `size`, `raster`, `svg-subset`, `json-schema`, `contrast-guard`, `font-embedding` | Closed |
| `limits` | object | Max item bytes, items per pack |
| `use` | text | What counts as a use of an item in a document (for compaction and usage counts) |

Any `def` may carry `components`: the item hashes an editor used to make it (cover and template
editors write it). Export as pack reads it to copy parts and check their licences.

Every item belongs to exactly one source: Built in, Yours, or one pack. Linking on import is in
`flows.md` (import, conflicts).

## The eight built-in types

Every `def` table lists the whole record a document or a holder embeds (documents.md, "Renderer
inputs"). Fields not in the table are ignored.

### pen

- Formats `json` plus resource `png` texture. Renderer kind: pen sample, `wide`. Views: list, grid.
- `def`: `kind` (pen, pencil, marker, brush), `widthMin`, `widthMax`, `pressureCurve` (4 points),
  `tiltResponse`, `opacity`, `nib` (round, flat, chisel), `smoothing`, `texture` (resource id).
- `appliesTo` tool. `embedAs` parameters (strokes store the full record). `holders` wheel-slot,
  quick-slot, saved-look. `keptCopy` copy-with-resources. Editable: Pen Studio. Default: Ball pen.
- Validators: `json-schema`, `raster` for the texture (1024 px). Use: a stroke drawn with it.

### colour set

- Format `json`. Renderer kind: swatch, `medium`. `def`: `colours` (up to 24, each `{argb, name?}`),
  `roles` (optional ink or highlight). More colours become another set (SYSTEM-MAP section 8).
- `appliesTo` palette. `embedAs` none (a colour on a page is stored by value in the stroke).
  `holders` editor, quick-slot, saved-look. `keptCopy` copy-item. Default: Basic.

### cover

- Formats `svg`, `png`, `json`. Renderer kind: cover, `large`.
- `def`: `ratio`, `spine` (left, right, none), `colourSlots` (named, adjustable), `size`.
- `appliesTo` notebook. `embedAs` item. `holders` default (new notebook cover). `keptCopy`
  copy-with-resources. Default: Plain (built-in; referenced by id, never embedded).
- Validators `size`, `raster`, `svg-subset`. Use: the notebook's cover.

### folder style

- Format `json` with optional `svg` icon. Renderer kind: image tile, `small`.
- `def`: `icon` (built-in name or resource), `colourSlot`. `appliesTo` folder. `embedAs` none.
  `holders` folder. `keptCopy` copy-with-resources. Default: Plain.

### canvas preset

- Format `json`. Renderer kind: page thumbnail, `medium`.
- `def`: `paper` (size or infinite), `orientation`, `background` (pattern, spacing, colour),
  `margins`. `appliesTo` new-document, page-background. `embedAs` parameters (the page stores its
  resolved background). `holders` default. `keptCopy` copy-item. Default: Blank.

### sticker

- Formats `svg` (subset), `png`, `webp`. Renderer kind: image tile, `medium`.
- `def`: `size`, `tintable`, `anchor`. `appliesTo` page. `embedAs` item (def and bytes).
  `holders` quick-slot, saved-look. `keptCopy` copy-item. `importFormats`: png, jpg, webp, svg
  (any image becomes a sticker in Yours; New pack's "Add images" uses the same path).
- Limits 20 MB, 4096 px. Use: a placement on a page.

### template

- Format `json` plus resources (images, fonts). Renderer kind: page thumbnail, `large`.
- `def`: `pages` (list of page layouts), `paper`, `elements` (typed: text, shape, image, line; no
  expressions, no extension blocks). Extension blocks inside a template are forbidden; a template
  states `requiresExtensions` informationally.
- `appliesTo` page, new-document. `embedAs` item. `holders` default. `keptCopy`
  copy-with-resources. Default: Blank page.
- Applying a template expands to ordinary content; each resource is embedded separately. A text run
  using a font counts as a use of the font. Validators `json-schema`, `raster`, `font-embedding`.

### theme

- Schema, derivation, adjustments and recovery: **`design/themes/custom-themes.md` is the single
  source** ("Theme pack data": `rules`, `id`, `name`, `scheme`, `inputs`, `overrides`). Nothing
  in this folder redefines theme data or outcomes.
- Renderer kind: theme preview, `wide`. `appliesTo` app. `embedAs` none. `holders` active-theme.
  `keptCopy` copy-item. Default: Ink. Validator `contrast-guard`: the guard's three outcomes
  (accepted, adjusted, rejected) apply as that file says; an installed pack shows a rejected theme
  as "Theme unavailable" and the rest of the pack works.
- Applying a theme from a pack follows the Keep or Revert banner in that file. The guard re-runs
  when a theme loads; a changed result applies silently (that file's rule), and an active theme that
  now fails is replaced by the default as in that file.

## Migration of existing content

On first launch of the version that ships the registry, today's saved pens, colour sets, covers,
folder styles and presets move to Yours as items with their current parameters. No pack record is
created. Ids and order are preserved, and a `.grapho` file written before the change keeps opening
(it has no items).

## Slot registry

The slots, where they appear and how they grow are in SYSTEM-MAP 7. The contract per slot (what it
receives, what it may return, what it implies) is the matrix in `extension-api.md`, section 9. Common
rules:

- A contribution has `id` (unique in the extension), `label` (text), `icon` where shown, and the
  slot's own fields. The extension's name is its group or tooltip. The extension detail lists every
  contribution with its own turn-off switch.
- A contribution is active only when its extension is On and the permissions it implies are granted.
- Manifest caps: 100 contributions in all; the visible caps in SYSTEM-MAP 7 apply on top.
- Extension UI is a tree of standard controls drawn natively; there is no custom styling or HTML.
- A panel is always drawn inside a shell-owned frame with the extension's name.
- A command that changes a document is one undo step labelled with the extension name, and takes a
  restore point first (`trust.md`).

## Adding a content type next year

1. A registry entry and a `def` table (and, if it has a new renderer kind, shell code for it).
2. If it uses a new `appliesTo` target or validator, engineering for that.
3. Nothing else: it appears as a tab in New pack, a Settings Content page, a source filter in
   pickers, an option in Export as pack and a recognised type in import review. A type unknown to an
   older app is listed "Not supported in this version" and its items stay in the stored original.
