# Tiles, cards and rows

Covers, folders and choices are big and visual; the chrome around them is grey ([branding](../../docs/branding.md)). One
selection grammar everywhere (SYSTEM-MAP 4.7): tinted fill plus a check, never shrink and dim. The
Menu is always reachable without long press (4.7.1). Drag and drop follows 4.8, states 4.6.

## Members

| Component | File | Variants | Job | Flows |
|---|---|---|---|---|
| ItemTile | [item-tile.md](item-tile.md) | `kind` notebook, pdf, canvas | Library grid cell | 05, 07, 10, 13 |
| FolderTile | [folder-tile.md](folder-tile.md) | none | Folder cell, drop target | 05 |
| ListRow | [list-row.md](list-row.md) | `kind` plain, item, source | Every list row, and the `list` and `row` nodes | 02 to 09, 11 to 13, 15 |
| SettingRow | [setting-row.md](setting-row.md) | `layout` inline, stacked | Label, helper, control, Reset | 03, 07, 09, 12, 14 |
| PickerTile | [picker-tile.md](picker-tile.md) | `kind` swatch, imageTile, cover, penSample, pageThumbnail, themePreview | Any visual choice; page thumbnails; swatches | 03, 07, 08, 11, 15 |
| SearchResultGroup | [search-result-group.md](search-result-group.md) | `kind` typed, handwriting | A document with its snippets, and Highlight | 06 |
| GroupHeading | [group-heading.md](group-heading.md) | `kind` heading, rail | Group name, count, collapse; AlphabetRail | 05 |
| ChoiceCard | [choice-card.md](choice-card.md) | none | Two or three large choices | 01 |
| MediaImage | [media-image.md](media-image.md) | `fit` contain, cover | Extension `image` node | 09 |
| DrawingSurface | [drawing-surface.md](drawing-surface.md) | none | Extension `drawing` node | 09 |

## How they line up

| Property | ItemTile, FolderTile | PickerTile | ListRow | SettingRow | ChoiceCard |
|---|---|---|---|---|---|
| Height | art 3:4 or 4:3 plus 2 lines | preview size from the registry, slot at least `size/hit/min` | at least `size/row/list` | at least `size/row/list` | 3 times `size/hit/min` |
| Padding | `space/xs` | `space/xs` | `layout/gutter/*` page, `space/lg` panel | same as ListRow | `space/lg` |
| Gap | `space/lg` | `space/md` | rows touch, hairline | rows touch, hairline | `space/md` |
| Radius | art `radius/item`, fill `radius/surface` | `radius/item` | `radius/item` hover | `radius/item` hover | `radius/surface` |
| Icon size | `size/icon/small` on art | `size/icon/small` | `size/icon/default`, art `size/art/row` | none | `size/icon/feature` |
| Title, meta | `type/label`, `type/caption` | `type/caption`, `type/micro` | `type/label`, `type/caption` | `type/label`, `type/caption` | `type/heading`, `type/body` |
| Selected | `selection/fill` + check + media ring | same | `selection/fill` + check, or start bar in `one` lists | n/a | fill + check + edge |
| Hover, press | `hover/fill`, `press/fill` | same | same | same | `surface/control-hover`, `press/fill` |
| Drag source | `state/drag/source-opacity` | same | same | n/a | n/a |

## Shared rules

- Titles are the user's text: any script and length, `dir` auto, two lines then an end ellipsis on
  tiles, full text in the Tooltip and the accessible name, "Untitled" when empty.
- ••• is visible on touch and on hover or focus with a mouse; selection mode hides it. Selecting
  mode shows empty check circles on all items.
- Missing art uses the registry placeholder; failed art uses it with `image_off`; neither blocks
  opening. Loading is Skeleton at the real size (after 300 ms).
- Pack state (Off, Hidden, Kept from <pack>, Not installed) shows only on Content pages, the Packs
  and Extensions pages, the Hidden entry and item detail. Documents and pick mode show none.
- Library lists virtualise above 200 items; the rail shows in Name sort above 30.
- Grid columns: compact 2 to 3, medium 3 to 5, expanded as many as fit; reduce columns before
  shrinking art, including at 200% text.

## Selection primitives

One grammar, three primitives, named once. Each host uses the ones its surface allows.

| Primitive | Tokens | Hosts |
|---|---|---|
| Fill | `selection/fill`, `selection/fill-hover` | every tile, row and card |
| Indicator | check badge on art (`selection/indicator` disc, `media/check-fg` glyph, `media/halo`); plain check on chrome; start bar in single-choice lists | tiles and picker tiles (badge), rows (check or bar), cards (check and edge) |
| Media ring | `selection/media-ring` with `selection/media-halo` | art only |

"In use" (the current value) is a label and bottom bar, not selection.

## Decisions to log

1. FolderTile stays apart from ItemTile (it accepts drops and previews contents) but shares every part.
2. "Open on another device" is a value of the one status slot in the meta line, never a lock; sync keeps both versions.
3. Swatch is a PickerTile kind; Highlight is part of SearchResultGroup; AlphabetRail is part of GroupHeading.
4. Collection items (library, Pages tab, manage-mode pickers, Content pages) have ••• on touch; pick-mode tiles have none. The Selection bar acts on many.
5. Extension `row` is a ListRow `plain` with at most three trailing children.
6. `drawing` requires `alt` in the extension schema; a tappable drawing must pair with a button,
   since it has no keyboard route.
7. Page thumbnails are PickerTile `pageThumbnail`; reorder uses the insertion line.

## Tokens to add

| Token | Proposed value | Why |
|---|---|---|
| `size/tile/min` | 150 (tile width, padding included) | smallest library tile, sets columns; two columns fit a 360 window |
| `size/tile/meta-min` | 170 | below this page count and time are dropped |
| `ratio/cover` | 3:4 | cover, PDF and folder art |
| `size/tile/page` | 96 wide | Pages tab thumbnail |
| `size/tile/crop` | 48 high | ink crop in handwriting hits |
| `size/art/row` | 40 touch, 32 compact | pack art and mini cover in a row |
| `preview/small`, `medium`, `large`, `wide` | 72, 120 (3:4), 160 by 100, 96 by 48 | registry size classes |
| `media/check-bg` | black at 55% | disc behind an empty check on art, themable |
| `media/check-fg` | white | check glyph on `selection/indicator` over art |
| `media/halo` | host surface colour, 2 px | halo that separates the badge from any art |
| `media/cover-neutral` | mid grey, themable per variant | missing cover |
| `media/cover-neutral-fg` | contrast grey, 3:1 on the above | icon on a missing cover |
| `feedback/long-list` | 30 items | the rail and Filter threshold |
| Pairs | `media/check-fg` on `selection/indicator` (ui 3); `selection/indicator` on `media/halo`; `media/ring` on `media/check-bg`; `media/cover-neutral-fg` on `media/cover-neutral` in all four themes; `border/strong` on `surface/base` for light themes | contrast matrix |

Commands to register: `library.itemMenu` (Menu key), `list.moveUp`, `list.moveDown`,
`settings.reset`. Icons: `more` (the ••• icon), `check`, `drag_handle`, `image_off`,
`cloud_down`, `device`, `chevron_right`. MediaImage and DrawingSurface live here because the extension
schema maps `image` and `drawing` to this family; they are extension controls.
