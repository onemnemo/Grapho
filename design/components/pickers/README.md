# Pickers and managers

One Picker serves every content type through the registry; the rest of the family are hosts and
editors around it. Rules: SYSTEM-MAP 4.7.1 and 5, `content-system/registry.md`,
`content-system/surfaces.md` and `content-system/lifecycle.md`.

## Members

| Component | File | Variants | Flows |
|---|---|---|---|
| Picker | [picker.md](picker.md), configurations in [picker-recipes.md](picker-recipes.md) | `mode` pick, manage; `placement` wheel, panel, sheet, page; `view`; `scope`; `onPick`; slot per renderer kind | 02, 03, 07, 08, 11, 12, 15 |
| FolderChooser | [folder-chooser.md](folder-chooser.md) | `mode` single, multiple; `host` sheet, popover, inline | 02, 05, 06, 14 |
| ColourEditor | [colour-editor.md](colour-editor.md) | `role` ink, highlight, plain; `host` panel, page | 03, 07 |
| StudioShell | [studio-shell.md](studio-shell.md) | `layout` beside, over, bottom | 07 |
| StudioPanels | [studio-panels.md](studio-panels.md) | `look` cover, folder-style, canvas-preset, template; the rail | 07 |
| ThemeEditor | [theme-editor.md](theme-editor.md) | `layout` split, stacked | 07 |
| ContentPage | [content-page.md](content-page.md) | one per registry type | 07 |
| SourceDetail | [source-detail.md](source-detail.md) | `kind` pack, extension | 08, 09 |
| CompareView | [compare-view.md](compare-view.md) | `layout` side-by-side, single | 14 |

ColourSetSwitcher is deleted: colour sets are a configuration of the Picker (recipes, section 3).
ExtensionPanelHost is in the bars family. The wheel's rings are the Picker's `wheel` placement.

## How they line up

| Property | Picker | FolderChooser | ColourEditor | StudioShell | ThemeEditor | ContentPage | SourceDetail | CompareView |
|---|---|---|---|---|---|---|---|---|
| Control height | `size/control/default` | `size/control/default` | `size/control/default` | `size/control/default` | `size/control/default` | n/a | `size/control/default` | `size/control/small` |
| Row height | `size/row/list` | `size/row/list` | n/a | n/a | n/a | n/a | `size/row/list` | n/a |
| Touch target | `size/hit/touch` | same | same | same | same | same | same | same |
| Side padding | none (host owns it) | `space/md` | `space/md` | `space/lg` | `space/xl` | `space/xl` | `space/xl` | `space/md` |
| Gap | `space/md` grid, `space/sm` header | `space/sm` | `space/sm` | `space/md` | `space/md` | `space/lg` | `space/lg` | `space/md` |
| Radius | `radius/item` | `radius/item` | `radius/item` | `radius/surface` | `radius/item` | none | `radius/item` | `radius/item` |
| Icon size | `size/icon/default` | `size/icon/default` | `size/icon/small` | `size/icon/default` | `size/icon/default` | `size/icon/default` | `size/icon/default` | `size/icon/default` |
| Label | `type/label`, names `type/caption` | `type/label` | `type/label` | `type/label` | `type/label` | `type/title` | `type/label` | `type/label` |
| Surface | `surface/raised` | `surface/overlay` | `surface/raised` | `surface/base` | `surface/base` | `surface/base` | `surface/base` | `surface/overlay` |

Shared rules:
- Pick mode is a choice list: no item Menu, no ... button; long press then lift enters manage mode.
- The Picker has no title and no primary; the host names it and owns the one primary.
- Off items appear only in a `scope`d Picker (SourceDetail). Pack state appears only in the source
  filter, Hidden, Content pages, SourceDetail, Packs and Extensions pages and an item's detail.
- Selection is fill plus indicator; art uses the check badge.
- Loading: Skeleton, ProgressItem, inline busy only. Logical properties and mirroring everywhere.

## Decisions to log

1. One Picker, nine configuration rows, six renderer kinds. The wheel placement renders the wheel's rings.
2. Pick mode is a choice list; manage mode is the collection.
3. Source filter and footer collapse by measurement.
4. Filter is always visible; the Picker has no title.
5. Off items are listed only by a `scope`d Picker; no Toast for a pack switch.
6. Colour sets are Picker options; there is no ColourSetSwitcher.
7. A swatch tap applies the pen colour; a set row tap activates the set.
8. Insert: Tabs for the type, one source filter, Images' New slot is Take photo and Choose image.
9. Edit on a pack or built-in item works on a copy. The default item and the active theme cannot be hidden or deleted.

## Commands to register

| Id | Label | Context | Key |
|---|---|---|---|
| `picker.manage` | Manage | picker | none (toggle) |
| `picker.edit` | Edit | picker | Enter on one item in manage |
| `picker.duplicate` | Duplicate | picker | none |
| `picker.rename` | Rename | picker | F2 |
| `picker.hide`, `picker.unhide` | Hide, Unhide | picker | none |
| `picker.delete` | Delete (Yours; undoable, not Move to Trash) | picker | Delete |
| `picker.exportPack` | Export as pack | picker | none |
| `picker.details` | Details | picker | none |
| `picker.getMore`, `picker.import`, `picker.new` | Get more, Import, New | picker | none |
| `picker.viewOptions` | View options | picker | none |
| `picker.managePacks` | Manage packs | settings | none |
| `colour.addToSet`, `colour.replace`, `colour.remove` | Add to set, Replace, Remove | picker | none |
| `studio.undo`, `studio.redo` | Undo, Redo | studio | Ctrl or Cmd+Z, Shift+Z |
| `studio.shapes` | Shapes | studio | none |
| `appearance.saveTheme` | Save theme | settings | Ctrl or Cmd+S |
| `pack.update`, `pack.skipVersion`, `pack.revert`, `pack.share`, `pack.export`, `pack.remove`, `pack.restore` | pack actions | settings | none |
| `compare.next`, `compare.previous` | Next difference, Previous difference | sheet | F8, Shift+F8 |

Existing ids used: `library.select`, `library.selectAll`, `library.newFolder`, `selection.clear`,
`insert.photo`, `tool.previous`, `studio.done`, `studio.cancel`, `appearance.resetTheme`. The Filter field
has no chord of its own (focus order only).

## Tokens to add

| Token | Value | Used by |
|---|---|---|
| `media/check-bg` | `selection/indicator` | check badge on art and swatches |
| `media/check-fg` | `text/on-action` | check mark |
| `media/halo` | the host surface colour of the tile, like `focus/gap/color` | 2 px ring round the check badge |
| `size/preview/small` | 48 | image tile small |
| `size/preview/medium` | 72 | swatch, page thumbnail, image tile medium |
| `size/preview/large` | 120 | cover, template |
| `size/preview/wide-width`, `size/preview/wide-height` | 160, 100 | pen sample, theme preview |

The registry stays the source of which size a type uses.

## Registry fields to add

`hosts` (placements), `insertable` (adds an Insert tab type), `names` (ICU message keys: placeholder
name, empty, plural-aware counts), `sortDefault`, `tags` and `keywords` on items, `nameVisible` per
view, `dropAccepts` (colour sets), `limit` (24 for colour sets), `manual` as a sort key.
