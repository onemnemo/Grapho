# PickerTile

## 1. Purpose

One tile for anything chosen from a visual set: a cover, folder style, canvas preset, template,
sticker, colour swatch, pen sample, theme preview, and a page thumbnail in the Pages tab. The tile is
drawn by the content type's renderer kind from the registry, so a new type with an existing kind needs
no new component. Swatch is a kind of this tile, not a component of its own.

**When not to use:**
- A library document: use [ItemTile](item-tile.md).
- A choice between two or three large options with words: use [ChoiceCard](choice-card.md).
- A text option: use RadioList or Select.
- A pack or extension: use [ListRow](list-row.md) `source`.

## 2. Anatomy

```
 +--------------+     +------+     +--------+
 |  preview     |     | pen  |     | swatch |
 |  (renderer)  |     +------+     +--------+
 +--------------+      name          (no name)
   Name              Botanica       (value in Tooltip)
   From Botanica     (provenance, manage hosts only)
```

Parts: preview (renderer output), check badge, "In use" mark, name, provenance caption (manage
hosts), ••• button (manage mode only, in the name line), page number, placeholder, drop line. There
are no chips over the preview.

## 3. Variants and sizes

`kind` (the registry's `rendererKind`): `swatch` | `imageTile` | `cover` | `penSample` |
`pageThumbnail` | `themePreview`. Preview size comes from the content type; the tile never draws it
smaller than the type's minimum, and columns reduce first.

The preview size is the registry's size class, read from the type, not written here:
`preview/small`, `preview/medium`, `preview/large`, `preview/wide` (tokens to add, values in
README). Covers, folder styles, templates and canvas presets are `medium`; stickers `small`; pens
`wide`; themes `large`. Swatches are `size/swatch`. A new type with an existing kind is a registry
entry only; a new preview shape needs a new renderer kind and says so. The Pages tab uses the
document page ratio at `size/tile/page`.

Names: every pickable item has a registry `label`, required (stickers and swatches included, so a
screen reader never reads "Sticker, Sticker"). Swatches have a colour name ("Red 2"). Pages are
"Page 4".

Grid: columns = fit by preview width plus `space/md` gaps; the Sheet and side panel host decide the
width. Hit slot is at least `size/hit/min` each way; a 36 swatch sits centred in its slot.

## 4. States

| State | Changes |
|---|---|
| Default | preview on its own colours, `radius/item`, hairline `media/edge`; name `text/default` `type/caption` |
| Art edge | the ItemTile hairline: `border/strong` on light themes, `border/subtle` on dark |
| Hover | `hover/fill` behind preview and name; ••• appears in manage mode |
| Focus-visible | ring around the tile slot |
| Active | `press/fill` |
| In use (current value) | a small "In use" label in the name line, `type/micro` `text/default`, and a `border-width/emphasis` bottom bar in `selection/indicator` under the preview. It is not the selection look, so in Content pages a theme in use and a theme selected for deletion differ |
| Selecting (manage mode) | an empty check badge on every tile: `media/check-bg` disc, `media/ring` outline, `media/halo` |
| Selected (manage mode) | `selection/fill`, a check badge in `selection/indicator` with `media/check-fg` and `media/halo`, `selection/media-ring` on the preview. For a swatch the badge sits on the slot corner with its halo, never on the colour alone, and the ring shows too |
| Disabled | `state/disabled/fg` on the name and a quiet preview; reason in the Tooltip and accessible description (for example a template that does not fit the notebook) |
| Loading | `skeleton/fill` at the preview size (Skeleton `grid`); pen sample and thumbnails fill when drawn |
| Empty | the registry placeholder on `media/cover-neutral` with its icon in `media/cover-neutral-fg`; the accessible description says "missing", no word is drawn |
| Error | the placeholder with `image_off`; the tile still works if the item does |
| Drag source | `state/drag/source-opacity`; stickers and templates drag to the page (4.8); ghost is the preview at `elevation/drag` |
| Drag, page thumbnail | reorder in Pages: a `state/drag/insertion` line between thumbnails; the dragged page leaves a hole at `source-opacity`; the current page keeps its mark |

Pages tab: the current page has the "In use" bottom bar, the number sits below the thumbnail in
`type/micro`, not on it. Page thumbnails are a collection, so they have ••• (duplicate, delete,
move) like manage-mode tiles. Thumbnails not yet drawn show `skeleton/fill`.

## 5. Content rules

- Names are the user's or the pack author's text: one line then an end ellipsis, any script, full
  text in the Tooltip and as the accessible name. Unnamed items show their kind ("Sticker", "Page 4").
- German and Norwegian names wrap to two lines in covers and stay one line in stickers (the
  Tooltip carries the rest). 1 item: one tile, not stretched. 500 items: the grid virtualises; a
  Filter above 30; previews drawn at tile pixel size and cached.
- Sticker images that are huge or wide letterbox in the slot at `fit` contain.

## 6. Overflow rule

No overflow on the tile. In **manage** mode and on page thumbnails the ••• Menu holds Edit,
Duplicate, Rename, Hide or Delete, Export as pack; the Selection bar acts on many. In **pick** mode
a tile is a choice, not a collection item: no ••• and no item Menu. Long press then lift in pick mode
enters manage mode with that item selected. Too many sources become the source
filter dropdown above six.

## 7. Extension slots

Extension contributions appear as items from their pack or as a Menu row on the item, per SYSTEM-MAP
section 7. Tiles carry no extension styling.

## 8. Pack content

Provenance shows only on Content pages, in the Hidden entry and in item detail.
There the tile shows a caption under the name, `type/micro`, `text/muted`:

| Item | Caption and badge |
|---|---|
| Built in | "Built in" |
| Yours | "Yours" |
| From a pack | "From Botanica" (pack name, one line, cut at the end) |
| Pack turned off | caption "Off", preview quiet, not pickable. Shown only in a source-scoped Picker (a pack's own page) and in the "In use" group, never in an ordinary picker |
| In use from an Off pack | caption "In use", as surfaces.md; it is visible where a holder uses the item |
| Hidden | caption "Hidden" in the Hidden entry, with Unhide in the Selection bar |
| Pack removed, copy kept | "Kept from Botanica" |
| Pack not installed (received document) | "Not installed" in the Packs list, not on tiles |

In pick mode the tile shows no caption; the source filter conveys it, and the accessible description
adds "From Botanica". An item whose pack file is missing shows its stored copy, or the placeholder.

## 9. Input

- **Stylus and touch:** tap applies (pick) or opens detail (manage); long press then lift enters
  manage mode with the item selected (from pick) or selection mode (in manage); long press then move
  drags a sticker or template to the page. A pen swipe scrolls before the long-press time. ••• is a
  `size/hit/min` slot in the name line in manage mode.
- **Mouse:** click; Ctrl or Cmd click selects in manage; hover shows the Tooltip after the delay.
- **Keyboard:** arrows in 2D; Enter applies or opens; Space toggles selection; Home and End; Tab leaves
  the grid to the footer in the order Get more, Import, New, Manage. In Pages, Alt+Arrow moves a page.
- **Roles:** `option` in a `listbox` (pick) or `gridcell` (manage); Compose `selectable`; WinUI
  `GridViewItem`; SwiftUI button in a lazy grid. Name: "Fern, cover, from Botanica, selected".

## 10. Tokens

| Token | Use |
|---|---|
| `size/swatch`, `size/hit/min`, `size/tile/page`, `preview/small`, `preview/medium`, `preview/large`, `preview/wide` (to add) | geometry |
| `space/xs`, `space/sm`, `space/md` | gaps |
| `radius/item`, `border/strong`, `border/subtle`, `border-width/hairline`, `border-width/emphasis` | preview, edge, In use bar |
| `media/check-bg`, `media/check-fg`, `media/halo`, `media/ring` | check badge (first three to add) |
| `type/caption`, `type/micro`, `text/default`, `text/muted` | text |
| `hover/fill`, `press/fill`, `selection/fill`, `selection/indicator`, `selection/media-ring`, `selection/media-halo` | states |
| `skeleton/fill`, `state/disabled/fg`, `media/cover-neutral`, `media/cover-neutral-fg` (to add) | loading, disabled, placeholder |
| `state/drag/source-opacity`, `state/drag/insertion`, `elevation/drag` | drag |

## 11. Do and Don't

- Do keep the preview at its registry size and drop columns instead.
- Do draw the registry placeholder for a missing item.
- Don't show "Off" or provenance on a document or in pick mode.
- Don't put ••• on a pick-mode tile or any chip over a preview.

## 12. Traces to

Flows 03, 07, 08, 11, 15.
