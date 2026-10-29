# Picker

## 1. Purpose

The one component that picks and manages content of any registry type: pens, colour sets, covers,
folder styles, canvas presets, stickers, templates, themes, and any type added later. The registry
entry is the configuration; each renderer kind is a **slot** the Picker draws items with. There are not
eight pickers. The source filter and the footer slots are parts of it. The wheel's pen and colour
rings are its `wheel` placement, drawn by the wheel renderer.

**When not to use:**
- Choosing one of a short fixed list of setting values: Select or RadioList.
- Choosing a folder: FolderChooser. Editing one colour: ColourEditor.
- Acting on a document's pages or layers: SidePanel tabs.

## 2. Anatomy

```
Header        [Filter__________________________] [Sort v] [grid|list]     (no title: the host names it)
Source filter ( All 48 )( Built in 12 )( Yours 6 )( Botanica 30 )( Hidden 2 )
Items         +------+ +------+ +------+ +------+     virtualized, drawn by the renderer kind
              | tile | | tile | | tile | | tile |     name under the tile (type/caption)
              +------+ +------+ +------+ +------+
Footer        [Get more] [Import] [New] [Manage]       narrow: icon buttons; narrower: [More]
```

| Part | Notes |
|---|---|
| Header | Filter (SearchField `filter`) is always shown. Sort (Select, core enum: Recently used, Name, Newest, plus the type's keys, and Manual where the type allows) and View (SegmentedControl `small`, icons `grid` and `list`) sit beside it, or inside one View options IconButton (section 6). The Picker has no title: the host names it (panel tab, Sheet row, page title) |
| SourceFilter | Chips (Chip `small`) while they fit on one line, else one button with a Popover list (section 6) |
| Items | Virtualized grid or list of PickerTile or ListRow with the renderer slot inside. In `manage` mode each tile has the ... button (PickerTile part); in `pick` mode it has none |
| Status | An invisible `status` node always present for announcements. A visible line (`type/caption`, `text/muted`) appears only when it has text ("200 of 5,000 shown", "Sets the colour of Fine liner") and is otherwise not drawn |
| Footer | Get more, Import, New, Manage, in that order, in both modes. Hairline above |
| Selection area | `manage` only: SelectionBar `picker` above the footer |

## 3. Variants and sizes

Props are string unions.

| Prop | Values | Meaning |
|---|---|---|
| `type` | a registry id | The configuration (table below) |
| `mode` | `pick`, `manage` | Tap applies, or the selection grammar of SYSTEM-MAP 4.7 |
| `placement` | `wheel`, `panel`, `sheet`, `page` | Wheel rings, side panel, pushed view in a Sheet, Settings page |
| `view` | `grid`, `list` | Only where the entry's `viewModes` has both; remembered per type and device |
| `scope` | `all`, or a source id | A source id forces that source, hides the source filter and lists that source's Off pack items and Hidden items. Used by SourceDetail only (section 8) |
| `onPick` | `stay`, `close`, `pop` | `wheel` is `close`, `panel` and `page` are `stay`, `sheet` is `pop` (Back to the form with the pick set) |
| `target` | text from the host | What a pick changes ("Pen colour for Fine liner", "Cover of New notebook"); shown in the Status line and read to screen readers. A pick-mode Picker always has a target |

**Pick mode is a choice list, not a collection** (SYSTEM-MAP 4.7.1): a tap applies, tiles have no Menu
and no ... button, and long press then lift enters `manage` mode with that item selected, where the
actions live. Insert adds the drag route (section 9).

**Slot per renderer kind.** Sizes come from the registry entry's `previewSize`; this file carries no
dp values.

| Renderer kind | Draws | Selected look |
|---|---|---|
| `swatch` | colours of a set as a wrapping strip of slots at `size/hit/touch` (section 5) | check badge (below) |
| `image tile` | image or icon on `surface/sunken`, contained | check badge |
| `cover` | the cover at its ratio in its colours | check badge on art |
| `pen sample` | stroke sample in `canvas/ink-default` on `canvas/page` | `selection/fill` plus the leading bar `selection/indicator` |
| `page thumbnail` | background or template page on `canvas/page` | check badge on art |
| `theme preview` | miniature library and note in that theme's own tokens, in the current Mode | check badge on art; the active theme also reads "Current" as text |

**Check badge:** a disc in `media/check-bg` with `media/check-fg` and a 2 px `media/halo`, so it reads on
any art. Art tiles also carry a decorative `media/edge` hairline so white art has an edge. Pick mode
marks the applied item with the badge and "Current" only where it is not the same as manage selection
(manage uses the badge plus `selection/fill`).

**Configuration table.** Rows cite the registry; where this file and `registries.md` differ, the
registry wins and the difference is in the README ("Registry conflicts").

| Type | Kind | Views | New slot | Applies to | Hosts |
|---|---|---|---|---|---|
| Pens | pen sample | list, grid | Pen Studio | tool | wheel, panel, page |
| Colour sets | swatch | list | ColourEditor | palette | wheel (Sets segment opens the panel), panel, page |
| Covers | cover | grid | Studio | notebook | sheet, page |
| Folder styles | image tile | grid | Studio | folder | sheet, page |
| Canvas presets | page thumbnail | grid | Studio | new document, page background | sheet, panel, page |
| Stickers | image tile | grid | none (Import) | page | panel, page |
| Templates | page thumbnail | grid | Studio | page, new document | sheet, panel, page |
| Themes | theme preview | grid | ThemeEditor | app | page (Appearance) |
| Images (Insert only) | image tile | grid | Take photo, Choose image | page | panel |

Hosts, New, views, default item, names and sort default are registry fields (section 7 of the
README lists the ones to add), so a ninth type is a registry entry. Manage appears in `pick` mode where
the host allows it (panel, Appearance) and is absent in `sheet` (Manage lives in Settings or the panel);
Content `page` is already `manage`.

**Wheel placement.** The wheel's rings are the Picker's items drawn as ring segments by the wheel
renderer; ToolWheel owns the geometry (every segment at least `size/hit/touch` at its narrowest point),
the tokens (`wheel/*`) and the hub label. The Picker supplies the item list, order, paging and apply. Pens
are the user's wheel pens (a holder list, up to six in the hub, paged to 24); the colour ring is the
current set's colours, and its Sets segment opens the colour set Picker in the side panel. There is no
header, source filter or footer, and no second picker.

## 4. States

The Picker is `surface/raised` (`surface/overlay` in a Sheet). It takes no side padding in `panel` and
`sheet`; the host owns the edge.

| State | Behaviour and tokens |
|---|---|
| Default | Tiles at rest; names `text/default`, counts `text/muted` |
| Hover | `hover/fill`; the ... button appears (manage) |
| Focus-visible | The cell with roving focus: `focus/*`, inset |
| Active | `press/fill`; apply on lift |
| Selected (pick) | Applied item: check badge or bar plus fill, never fill alone |
| Selected (manage) | `selection/fill`, `selection/fill-hover`, check badge; count in the SelectionBar |
| Disabled item | `state/disabled/fg`, reason as description, still focusable |
| Loading | Skeleton over Items after `feedback/delay/skeleton`, held `feedback/hold/skeleton`; header, source filter, footer live. Per tile the frame stays and art fills in from the cache |
| Empty | EmptyState `sm`, no mascot, inside Items |
| Error | One row "Could not load" with Retry in Items; footer usable |
| Offline catalogue | Get more stays enabled and opens its own empty state (Import from file, Open grapho.one); nothing else changes |
| Remote source | A source from an extension keeps its own state: Skeleton, "Could not load" with Retry, Offline in the Status line. Filter text is debounced and handed to the extension; paging shows "200 of many". Manage on its items is Hide and Details until an item is embedded |
| Importing | ProgressItem `inline` above the footer after `feedback/delay/progress`. The footer stays usable; a second Import queues |
| Drag | `state/drag/*`; every drag has a Menu route |

## 5. Content rules

| Count | Behaviour |
|---|---|
| 0 | EmptyState `sm`. All with no default: "No stickers yet", Get more and Import. Yours: "Nothing of yours yet", New or Import. Hidden: "Nothing hidden". Filter: "No match for fern", Clear filter |
| 1 | One tile at the start edge |
| 9 | One screen, no scroll |
| 500 | Virtualized. Filter and Sort act on the core's model. Source counts are true totals |
| 5000 | As 500, cost keyed Filter debounce, locale grouped counts, draggable scroll thumb. An AlphabetRail only in `page`, list view, sorted by Name, above 200 items. Cells outside the window are not created |

- **Filter** matches name, the type's tags and keywords, and pack name; unnamed items match by tag.
- **Default Sort** is the registry's `sortDefault` (stickers: Recently used).
- **Select all** is offered at count 0 in the SelectionBar ("Select all 500") and acts on ids in the
  current source and Filter. Selection survives a change of source or Filter and the bar says so.
- **After Import** the source filter moves to Yours, scrolls to the new items and marks them until the
  next interaction.
- **Swatch strips** wrap: as many slots per row as the measure holds, at `size/hit/touch` per slot,
  with the swatch drawn at `size/swatch` and a `media/edge` hairline so white and black swatches show.
  A set of 24 takes as many rows as it needs. In a list row the strip shows the swatches that fit on
  one line and "+N".
- Long names: one line, end ellipsis, full name as Tooltip and accessible name; titles wrap to two
  lines on art tiles. Swatches show no name; the computed colour name appears in the Status line on focus.
- Names are `dir=auto`; German and Norwegian never truncate a button label.

Grid maths: columns are the previews that fit the width, gap `space/md`; a cell is at least
`size/hit/touch` both ways.

## 6. Overflow rule

**Smallest window (compact width, short window, or the bottom panel).** The panel opens at full height,
never `peek`. Chrome is one header row (Filter and View options), the source filter as its dropdown,
the SelectionBar only in manage mode, and the footer as More. The body keeps at least three rows of
tiles; if it would not, the Picker drops the Status line and the hairline gaps first, then the panel
snaps to full. Height of chrome = `size/control/default` for the header + `size/control/small` source
filter + `size/control/default` footer, plus `space/sm` gaps.

- **Header:** Filter takes the free width. Sort and View move into one IconButton `quiet` View options
  (Popover with the two controls, command `picker.viewOptions`) when they do not fit beside it, and
  always in the smallest window.
- **SourceFilter (measured):** chips while all of them fit on one line at the current width and text
  size, with six sources as the maximum, like SegmentedControl and Tabs; otherwise one button "All, 48"
  (and "2 hidden" when Hidden is not empty) opening a Popover list: All, Built in, Yours, packs A to Z,
  **Hidden last**, counts, single choice, virtualized, Filter above 30. It is a Picker part: Select and
  RadioList hold core enum values only. Role `radiogroup`.
- **Footer (measured):** labelled Buttons `secondary`; else four IconButtons `quiet` with Tooltips (Get
  more `download`, Import `upload`, New `plus`, Manage `settings`); else one More IconButton whose Menu
  lists them in order. Per footer, never per button; never wraps. Manage is a toggle (pressed in manage mode).
- **Items:** scroll. No "Show more" (a source-scoped Picker in SourceDetail has a Show all link).

## 7. Extension slots

None of its own. An extension's **insert source** and **content source** appear as a source in the
source filter, in the pack position, with its name. An extension cannot add a footer slot, header
control or renderer kind; its items use an existing kind. When it is off, its source and items
disappear and the position is remembered. If the selected source disappears, the Picker returns to All.

## 8. Pack content

Every item has one source: Built in, Yours, or one pack. Two of one name read "Botanica, Study Lab".
Pack state appears only in the source filter, Hidden, SourceDetail and the item's own detail.

| Case | Rendering |
|---|---|
| Yours | Delete in manage mode |
| Pack item | Under its pack; manage: Hide and Export as pack, never Delete |
| Linked item | Once, under its first source, description "also in Botanica" |
| Hidden | Out of every list but Hidden, which lists hidden items of all sources, each with Unhide. The default item and the active theme cannot be hidden (Hide disabled, reason "Always available" or "In use") |
| Pack **Off** | An ordinary Picker never lists its items and shows no pack text. A holder keeps working in its own host (wheel, theme row). Only a Picker with `scope` set to that pack lists them, quiet |
| **Kept from** | Copies sit in Yours; their name is shown alone (the caption lives in item detail and Content pages) |
| Missing asset | The type's placeholder, name kept, manage: Hide, Details |
| **Not installed** | Never in a Picker |

**While the Picker is open**, a pack change takes effect in one frame:

| Change | Rule |
|---|---|
| Pack turned off | Items and chip leave. If the source was that pack, return to All. Manage selection drops its items and the count updates. Drag cancelled. Focus moves to the next item, else the previous, else the source filter. Announced in the invisible status ("Botanica turned off. Showing All."). No Toast: the switch is its own undo |
| Pack removed | As off, and kept copies appear in Yours live |
| Pack updated | Changed items refresh in place; the selection keeps ids |
| Blocked | As off |
| Insert source turned off while selected | Falls back to All of the same type |
| Sync adds items during Select all | The selection is the ids at press; the bar says "412 selected, 3 new" |
| Sheet host holds the pick | The draft keeps the item; the form row shows it by name. A draft is not a holder, so the Picker does not list it under any "In use" |

## 9. Input

- **Stylus and touch:** tap applies (pick) or toggles (selection). Long press then lift in pick mode
  enters manage mode with the item selected; in manage mode it selects. The ... button (manage only)
  opens the Menu: Edit, Duplicate, Rename, Hide or Delete, Export as pack, Details. Targets are
  `size/hit/touch`. In the Insert tab a pen drag that moves horizontally toward the page past the tap
  travel starts a drag (vertical moves scroll), as does a drag with the pen button held; the page
  auto-scrolls at its edges and the panel stays open. Palm rule of the bars family applies.
- **Mouse:** click applies; Ctrl or Cmd click selects, Shift click selects a range; right click opens
  the Menu in manage mode; drag after the drag threshold.
- **Keyboard:** the grid is one tab stop, roving focus. Arrows move (Left and Right swap in right to
  left), Home and End, Page Up and Down. Enter applies (pick) or edits one item (manage). Space toggles
  in selection mode only; Shift+Arrow extends a range. `library.selectAll` selects all ids. Right arrow
  or Alt+Down reaches a cell's ... button in manage mode; the Menu key opens the Menu. Delete runs
  `picker.delete` for Yours items and `picker.hide` for others, never mixed. Escape follows the map: a
  pushed view or the bottom panel closes first, then selection mode (clears the selection, then leaves
  manage), then the host. Type-ahead by name. Tab order: Filter, View options, source filter, grid,
  footer; a "Skip to footer" landmark helps a 500 item grid.
- **Roles:** grid `grid`, cells `gridcell`; in pick mode each cell is a button, `aria-selected` only
  in manage mode. Each cell announces position ("Sticker 14 of 500"). The ... button is a sibling
  control in the cell, also exposed as the named custom action "Actions". Source filter `radiogroup`;
  invisible status `status`. Compose `LazyVerticalGrid`; WinUI `GridView`; SwiftUI `LazyVGrid`. All are
  custom controls that implement platform accessibility.
- **Right to left:** logical properties; grid order and arrows mirror; the Popover list opens toward
  the inline end; the AlphabetRail moves to the other side.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/raised`, `surface/overlay`, `surface/sunken`, `border/subtle` | frame, Sheet, art well, footer hairline |
| `space/sm`, `space/md` | header gaps, grid gap |
| `size/control/default`, `size/control/small`, `size/hit/touch`, `size/hit/min`, `size/row/list`, `size/swatch` | header, chips, slots, rows, swatches |
| `size/icon/default`, `size/icon/small` | footer and ... icons |
| `radius/item`, `radius/pill` | tiles; chips and buttons |
| `type/label`, `type/caption` | buttons; names and Status |
| `text/default`, `text/muted` | names; counts |
| `hover/fill`, `press/fill`, `selection/fill`, `selection/fill-hover`, `selection/indicator` | states |
| `media/edge`, `media/check-bg`, `media/check-fg`, `media/halo` | art edge and check badge (to add: check-bg, check-fg, halo) |
| `state/disabled/fg`, `state/drag/source-opacity`, `state/drag/target-fill`, `state/drag/target-line` | disabled, drag |
| `skeleton/fill`, `feedback/delay/skeleton`, `feedback/hold/skeleton`, `feedback/delay/progress` | loading |
| `canvas/page`, `canvas/ink-default` | pen sample and page backgrounds |
| `wheel/*` | wheel renderer only |
| `focus/*`, `motion/duration/fast` | focus, state changes (0 under reduced motion) |

## 11. Do and Don't

Do:
- Add a type with a registry entry and an existing renderer kind.
- State the target of every pick-mode Picker.
- Keep Filter visible and the footer order and Hidden last in every placement.
- Keep the body at three rows or more in the smallest window.

Don't:
- Build a per-type picker, tile or footer, or a second wheel picker.
- Show pack state or an Off item in an ordinary Picker.
- Give pick-mode tiles a Menu or a ... button.
- Put a primary Button in the Picker; the host owns it.

## 12. Traces to

Flows 02, 03, 07, 08, 11, 12, 15. Replaces PresetPicker, PackCategory, PenList, ManageColours lists
and the ColourSetSwitcher. Uses PickerTile, ListRow, SelectionBar `picker`, SearchField, Select,
SegmentedControl, Chip, Popover, Menu, EmptyState, Skeleton, ProgressItem.

Commands are in the README ("Commands to register"): `picker.manage`, `picker.edit`,
`picker.duplicate`, `picker.rename`, `picker.hide`, `picker.unhide`, `picker.delete`,
`picker.exportPack`, `picker.details`, `picker.getMore`, `picker.import`, `picker.new`,
`picker.viewOptions`, `picker.managePacks`. Existing: `library.select`, `library.selectAll`,
`selection.clear`.
