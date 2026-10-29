# ListRow

## 1. Purpose

The one row for every list: an action or navigation row, a library item in list view, a pack or an
extension on its page, a result in a chooser, and the extension schema's `row`. A leading slot, a
text block, a trailing slot. The `list` container (selection, reorder, empty) is part of this file.

**When not to use:**
- A setting with a control and Reset: use [SettingRow](setting-row.md).
- A menu command: use Menu rows.
- A document with many search snippets: use [SearchResultGroup](search-result-group.md).
- A grid cell: use [ItemTile](item-tile.md) or [PickerTile](picker-tile.md).

## 2. Anatomy

```
 [::] [x] [lead]  Title                          [status] [toggle] [...]
                  Subtitle or meta (1 line)
                  Reason or risk line (optional)
```

Parts: drag handle, selection check, lead (icon at `size/icon/default`, mini cover, or 48 art),
title, subtitle, line (a reason or risk sentence), status text, trailing control (Toggle, chevron
or IconButton), ••• button, divider.

## 3. Variants and sizes

- `kind`: `plain` (navigation, action, choice) | `item` (library document or folder; lead is a mini
  cover at 3:4 or the folder style; subtitle is the meta line of ItemTile) | `source` (pack or
  extension; lead is 48 art for a pack, an icon for an extension; subtitle is author and version;
  Toggle and ••• trail).
- Height is a minimum of `size/row/list`; two and three line rows grow, never clip. `kind` `item`
  and `source` use a lead of `size/art/row` (to add: 40 touch, 32 compact) with vertical padding
  `space/sm`, so the row lands at `size/row/list` and not above it. A colour set row is `kind`
  `item` with a swatch strip as the lead.
- Selection: `none` | `one` | `many`, set by the list. `many` shows the check slot in selection mode.
- Compact density: row height `size/row/list` compact; trailing controls keep `size/hit/min`.
- A settings index result is a `plain` row: title is the setting name, subtitle is a breadcrumb
  "Section, then Setting" (for example "Pen and tools, Pen-only drawing"). `source` stays for packs and
  extensions.
- The extension `row` maps to `plain`: title, subtitle, icon, up to three trailing `button` or
  `toggle` children (a fourth is dropped and lint reports it). It cannot use `source` or drag.

Spacing: side padding `layout/gutter/*` in a page, `space/lg` in a Sheet or panel; lead to text and
between trailing controls `space/md` and `space/sm`; between text lines none; divider `border/subtle`
hairline, inset to the text start.

## 4. States

| State | Changes |
|---|---|
| Default | `surface/base` or the host; title `text/default` `type/label`; subtitle `text/muted` `type/caption`; line `text/muted` |
| Hover, active | `hover/fill`, `press/fill`; subtitle stays `text/muted` |
| Focus-visible | ring, inset |
| Selected | `selection/fill`, check `selection/indicator`; in `one` lists a start bar `selection/indicator` (`border-width/emphasis`) instead of a check |
| Disabled | `state/disabled/fg`; reason is the line and the accessible description; stays focusable |
| Loading | inline busy on the trailing control (spinner after 300 ms, width kept); or Skeleton `list` rows for a list |
| Empty | the list shows an EmptyState sm; a row is never empty |
| Error | the line states what failed in `state/danger/fg`; Retry as the trailing text Button; solid `border/error` edge only on an input row |
| Drag source | `state/drag/source-opacity`; ghost is the row at `elevation/drag` on `surface/overlay` |
| Drop position | `state/drag/insertion` line between rows (ordered lists only) |

Source rows add states, each as the line plus the trailing control, never colour alone:

| Source state | Line and control |
|---|---|
| On | Toggle on |
| Off | title stays `text/default`; Toggle off; the word "Off" in the status |
| Update available | line "Update to 1.3.0" and Button text "Review" |
| Installing | line "Installing", inline busy, Cancel |
| Failed | reason and "Try again" |
| Incompatible, blocked | reason line; Toggle disabled with the reason as accessible description |
| Partial | "Some content not supported" |
| Waiting to download | "Waiting to download" |
| Turned off after problems | "Turned off after problems", Button "Turn on again" |
| Needs permission | "Needs your OK for 1 permission", Button "Review" |
| Kept from <pack> | status "Kept from Botanica" on a user copy whose pack was removed |

One status slot shows one value. Priority when two apply: Failed, Blocked or Incompatible, Turned off
after problems, Needs permission, Installing, Update available, Waiting to download, Off. The Toggle
stays visible whatever the status is.

Extension rows add the risk line from the trust sentence in short form ("Can read your library and
connect to 1 site"), else nothing. The row for an extension that is Off hides everything it added to
chrome; the row itself stays on its page.

## 5. Content rules

- Titles are the user's or the pack author's text: any script and length. One line then an end
  ellipsis in `plain`; two lines in `item` and `source`. Full text in the Tooltip and accessible name.
  Author and version are `text/muted`, version after a space; a long author is cut before the version.
- German and Norwegian: the status text wraps below the title instead of cutting it. At 200% text
  the row grows and the trailing controls stack under the text block.
- No text: a row without a title is a defect except `item` ("Untitled"). 1 item: no divider. 500
  items: the list virtualises above 200 and stays smooth at 10,000; counts live in the control that
  opens it. Reorder lists show a handle in every row.

## 6. Overflow rule

Trailing slot: at most one status, one Toggle and ••• (source), or up to three extension children.
More actions go in the ••• Menu in command priority order. At compact width the drop order is: status moves under the title, then the lead shrinks to
`size/icon/large`, then the drag handle moves into the ••• Menu as Move up and Move down. The title
keeps at least half the row.

## 7. Extension slots

The extension row set is the `row` and `list` nodes only. No extension styling. Rows in Packs and
Extensions pages are built in and show the extension's name and version.

## 8. Pack content

The Packs and Extensions pages and item detail are the places pack state shows: Off, Hidden,
Kept from <pack>, Not installed. A pack row always shows its author. A "Made by you" pack row has no Toggle and shows Share. The row for a received document's
missing packs is a `source` row with "Not installed" and Get.

## 9. Input

- **Stylus and touch:** tap runs the row; long press then lift selects it (4.7.1); long press then
  move reorders or drags; ••• visible on touch. Handle slot is `size/hit/min` wide.
- **Mouse:** click; Ctrl or Cmd click selects; right click opens the Menu; drag after 4 px.
- **Keyboard:** Up and Down; Home and End; Enter runs; Space toggles selection only (a row's Toggle is
  its own Tab stop, so Space never turns a pack off by accident);
  Menu key opens the Menu; Alt+Up and Alt+Down move a row in an ordered list (commands
  `list.moveUp`, `list.moveDown`, to register). Type-ahead jumps by title.
- **Roles:** `listitem` in `list` (`listbox` `option` when selectable; `row` in a `grid` with a
  Toggle); Compose `Modifier.selectable` or `toggleable`; WinUI `ListViewItem`; SwiftUI `List` row.
  Name is the title, then subtitle, then status.

## 10. Tokens

| Token | Use |
|---|---|
| `size/row/list`, `size/art/row` (to add), `size/hit/min`, `size/icon/default`, `size/icon/large` | geometry |
| `space/sm`, `space/md`, `space/lg`, `layout/gutter/*`, `radius/item`, `radius/surface` | spacing, art |
| `type/label`, `type/caption` | text |
| `text/default`, `text/muted`, `state/danger/fg`, `state/disabled/fg` | text roles |
| `hover/fill`, `press/fill`, `selection/fill`, `selection/indicator` | states |
| `border/subtle`, `border-width/emphasis`, `border/error` | divider, bar |
| `state/drag/source-opacity`, `state/drag/insertion`, `elevation/drag`, `surface/overlay` | drag |

## 11. Do and Don't

- Do keep one row for packs, extensions, items and actions; vary only the slots.
- Do show a reason line for every disabled or failed row.
- Don't show `text/subtle` on a hover or selected fill.
- Don't put pack state on a row in a document-level list.
- Don't give an extension row a fourth trailing control or a colour.

## 12. Traces to

Flows 02, 03, 04, 05, 06, 07, 08, 09, 11, 12, 13, 15.
