# ItemTile

## 1. Purpose

One grid cell for a document in the library: a notebook, a PDF, a canvas or any later document kind.
A big cover or page preview, the user's title below it, a short meta line, and the two controls
every collection item has: the selection check and the ••• button. Covers are content, so they are
large and colourful; the chrome around them is grey ([branding](../../docs/branding.md)).

**When not to use:**
- A folder: use [FolderTile](folder-tile.md).
- The same document in list view, a search hit list or a chooser: use [ListRow](list-row.md) `kind` `item`.
- A cover, preset, sticker or page inside a picker or the Pages tab: use [PickerTile](picker-tile.md).
- Many snippets for one document in search: use [SearchResultGroup](search-result-group.md).

## 2. Anatomy

```
 +----------------------+
 |                      |
 |      cover art       |   art 3:4, radius/item, edge hairline
 |                (/)   |   check badge: selecting, hover, focus, selected
 +----------------------+
  My biology notes, ch 4      title: up to 2 lines, the user's text
  [nb] [*] 42 pages   [...]   meta: kind icon, star, count, time or status; ••• at the end
```

Parts: tile (focusable cell), art, art edge, check badge (start corner of the art), title, meta line
(kind icon, star, count, time, status slot), ••• button (IconButton `quiet`, `small`, at the end of
the meta line, never on the art), state layer, placeholder.

There are no chips and no glass buttons over the art. The only mark on art is the check badge, shown
while selecting, on hover or focus, and when selected.

## 3. Variants and sizes

- `kind` comes from the document kind registry (label, icon, art source, art ratio), starting with
  `notebook`, `pdf`, `canvas`. A new document type is a registry entry, not a new component.
  Notebook art is its cover (or the neutral cover). PDF art is the first page. Canvas art is a crop
  of the board. The kind is an icon at the start of the meta line with an accessible label.
- Tile width comes from the library's column rule. Art stays 3:4. A tile is never narrower than
  `size/tile/min` (padding included).

| Window class | Columns | Rule |
|---|---|---|
| Compact (under 600) | 2 to 3 | fit by `size/tile/min`, clamp to the class range |
| Medium (600 to 839) | 3 to 5 | same |
| Expanded (840 and up) | as many as fit, at least 4 | tiles stretch up to 1.5 times `size/tile/min` |
| 200% text | columns drop first; art never below `size/tile/min` | the text block grows; titles stay at two lines |

When the AlphabetRail shows, the grid reserves its width at the inline end, inside `size/edge-guard`,
so the last column's ••• and the system back gesture are never under it. Padding is `space/xs`
around the art and `space/sm` from art to title.

Meta drops in this order as width falls: page count, time, star, kind icon. The status slot always
keeps its icon, and its words are cut last. Below `size/tile/meta-min` the page count and time go.
The text block reserves two title lines at the tallest line floor of any script in the row, so mixed
scripts never make a ragged row.

## 4. States

| State | Changes |
|---|---|
| Default | art on its cover; title `text/default` `type/label`; meta `text/muted` `type/caption`; ••• shown on touch always, on hover or focus-within when the last pointer was a mouse or the last input a keyboard (keyed to the last-used input, not the device) |
| Hover | `hover/fill` behind the whole tile; the check badge appears on the art |
| Focus-visible | focus ring (actions README) around the tile, inset in a clipped grid |
| Active | `press/fill` behind the tile |
| Art edge | every art tile has a hairline (`border-width/hairline`) outside the art: `border/strong` on light themes, `border/subtle` on dark themes, so a white PDF or a near-black cover always has a boundary. `media/edge` is only an inner highlight |
| Selecting (not selected) | an empty check badge on every tile: outline `media/ring` on a disc of `media/check-bg`, halo `media/halo`. The mode is visible on any art |
| Selected | `selection/fill` behind the whole tile; the check badge is a disc in `selection/indicator` with glyph `media/check-fg` and a 2 px halo in `media/halo`; the art ring `selection/media-ring` with `selection/media-halo`. The art never shrinks or dims. ••• is hidden while selecting |
| Disabled | not used; a tile that cannot open shows a status |
| Loading | art is `skeleton/fill` at the real art size and radius; title and meta show when known |
| Thumbnail missing | `media/cover-neutral` with the kind icon in `media/cover-neutral-fg` at `size/icon/feature` (pair graded in all themes); no layout shift when replaced |
| Thumbnail failed | the same neutral art with `image_off`; the tile still opens; the Menu has "Rebuild preview"; no error colour |
| Status slot | one value at a time, in priority: Conflict (both versions kept), Could not open, Importing or Exporting (inline busy), Waiting to download (`cloud_down`), Open on another device (`device`, "Open on Tablet"), none. Information only: a tile that is open on another device opens normally, and sync keeps both versions if both change (flow 14) |
| Empty (no title) | title "Untitled" in `text/muted`; the accessible name uses the same word |
| Error | n/a on the tile; open errors belong to flow 13 |
| Drag source | `state/drag/source-opacity` on the tile in place |
| Drag ghost | the art alone at art size, `radius/item`, `elevation/drag`, `z/drag-ghost`, lifted above the contact point. Several selected: the top item plus a count chip (`surface/overlay`, `type/micro`) beside the art, not on it. Over an invalid target the ghost adds the reason ("Cannot move here") |
| Not a drop target | items accept no drops; files dropped from outside go to library import |

Drag with a mixed selection: long press then move on an unselected tile drags that tile alone and
selects it; on a selected tile it drags the whole selection. The grid auto-scrolls near its edge. On
compact, where the sidebar target is absent, Move to is the route.

## 5. Content rules

- The title is the user's text: any script, any length, emoji, mixed direction. `dir` is `auto` per
  title; the ellipsis cuts at the end of line 2 in the title's own direction. Line breaking follows
  the script (CJK breaks anywhere, Thai uses a dictionary). The tile never draws title text on art.
- Titles stay at two lines, also at 200% text. The full title is always the accessible name, shows
  as a Tooltip on hover and focus, and in item detail ("Details" is the first Menu row). Touch needs
  no Tooltip because long press selects.
- Meta: kind icon, star (if starred), page count, then time ("Yesterday", "12 Mar"), then the
  status slot. A device name is the user's text, cut at one line.
- No title: "Untitled". 1 item: one tile at column width, start aligned, not stretched. 500 items:
  virtualised by row; art is requested at the tile's pixel size and cached; scrolling shows the
  placeholder, never blank. 10,000 items: the scrubber (GroupHeading `rail`) gives fast travel in
  every sort.

## 6. Overflow rule

The tile has no overflow of its own. Extra actions are in the ••• Menu (`library.itemMenu`). Meta
drops in the order in section 3.

## 7. Extension slots

None on the tile. An extension adds Menu rows only, per SYSTEM-MAP section 7, shown with its name.

## 8. Pack content

The cover is a pack item embedded in the document, so the tile never shows pack state.
A cover whose pack is gone renders from the document's own copy; one that cannot be drawn is the
neutral cover. No provenance text, no "Off", no "Not installed" here.

## 9. Input

- **Stylus:** tap opens; hover (where reported) shows the hover fill. A pen swipe on the grid before
  the long-press time scrolls. Long press then move drags. While the pen is near or within 1 s of its
  last contact, touches over the palm threshold are ignored on the grid and the rail.
- **Touch:** tap opens or toggles; long press then lift selects it and enters the mode (4.7.1); long
  press then move drags it and selects it. ••• is always visible. No hit slot sits on the art corner:
  the badge is drawn small, and its slot is `size/hit/min` only while selecting.
- **Mouse:** click opens; Ctrl or Cmd click selects; Shift click selects a range; right click opens
  the Menu; drag after 4 px.
- **Keyboard:** Arrows in two dimensions; Home and End; Page Up and Down; Enter opens; Space toggles
  (and enters the mode); Shift+Arrows extend; Menu key or Shift+F10 opens the Menu; a letter jumps in
  Name sort. One tab stop for the grid.
- **Roles:** one `grid` for the whole library, group headings as full-width rows, and set sizes
  (`aria-rowcount`, `aria-colcount`, `aria-rowindex`) from the model so a virtualised grid says
  "3 of 10,000". Cell is `gridcell`. Compose `Modifier.selectable` with `CollectionInfo`; WinUI
  `GridView` item (UIA SelectionItem); SwiftUI element with `accessibilityRowCount`. Name: "Maths
  notes, notebook, 42 pages, starred, modified yesterday". Inner controls: "More actions for NAME",
  "Select NAME". Custom actions: Open, Select, Star, Move to, Delete. Commands: `library.open`,
  `library.select`, `library.moveTo`, `library.delete`, `library.itemMenu` (to register).

## 10. Tokens

| Token | Use |
|---|---|
| `size/tile/min`, `size/tile/meta-min`, `ratio/cover` (to add) | width rule, meta rule, 3:4 |
| `size/hit/min`, `size/icon/small`, `size/icon/feature`, `size/edge-guard` | slots, icons, rail gutter |
| `space/xs`, `space/sm`, `space/lg` | padding, title gap, grid gap |
| `radius/item`, `radius/surface` | art, tile fill |
| `type/label`, `type/caption`, `type/micro` | title, meta, count chip |
| `text/default`, `text/muted` | text |
| `hover/fill`, `press/fill`, `selection/fill`, `selection/indicator` | tile states |
| `selection/media-ring`, `selection/media-halo` | selected ring on art |
| `media/check-bg`, `media/check-fg`, `media/halo`, `media/ring` | check badge (to add the first three) |
| `border/strong`, `border/subtle`, `border-width/hairline` | art edge |
| `media/cover-neutral`, `media/cover-neutral-fg` (to add), `skeleton/fill` | missing art |
| `state/drag/source-opacity`, `elevation/drag`, `z/drag-ghost`, `surface/overlay` | drag |

`text/subtle` is never used on a tile fill.

## 11. Do and Don't

- Do keep the art at 3:4 at every column count; reduce columns before shrinking art.
- Do show selection as fill plus check badge plus ring.
- Do keep a long, empty or foreign-script title inside two lines, with the full title as the name.
- Don't dim or scale a selected tile.
- Don't put chips, kind marks or glass buttons on the art.
- Don't add a lock or a warning colour for "Open on Tablet".

## 12. Traces to

Flows 05, 07, 10, 13. The library list view is ListRow `item`.
