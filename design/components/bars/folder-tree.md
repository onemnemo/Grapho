# FolderTree

## 1. Purpose

The nested folder list: indented four levels, collapsible, a drop target for items and folders. The
Sidebar uses it to navigate; the FolderChooser uses it to pick a destination.

**When not to use:**
- A flat list of choices: RadioList, Select, or a Picker.
- Showing folders as tiles in the grid: FolderTile.
- Showing where you are in a deep path: the TopBar PathBar.

## 2. Anatomy

```
Library                      (chooser only, first row)
v Semester 1            12
    > Biology            4
    v Maths              3
        Algebra          1
        Geometry         2
          Sets            (level 5 shares the level 4 indent)
> Archive                8
```

| Part | Notes |
|---|---|
| Row | `size/control/default` high. Start padding grows by one step per level to four levels |
| Disclosure | A chevron IconButton `quiet` `small`, `chevron_right` or `chevron_down` (it mirrors), Tooltip "Expand" or "Collapse", Right and Left keys. Absent on a folder with no children |
| Icon | `folder`, `size/icon/default`, `currentColor` |
| Name | `type/label`, one line |
| Count | `type/caption`, `text/muted`: items inside |
| More | IconButton `quiet` `small`, `more`, Tooltip "More": opens the folder Menu (Rename, Move to, Edit look, Details, Delete). Always visible on touch, on hover or focus with a mouse (4.7.1). Replaces long press as the route to the Menu |
| Filter | SearchField scope `filter`, at the top, shown above 30 folders in the Sidebar and always in the chooser |

## 3. Variants and sizes

`kind`: `navigate` | `choose`. One size (role `default`).

| Kind | Differences |
|---|---|
| `navigate` | Rows open a place; current place selected; rows are drop targets and draggable; F2 renames in place; Delete moves to Trash |
| `choose` | A "Library" row first (the top level); a row applies on tap; no drag; rows that are invalid are disabled with a reason line below ("Cannot move a folder into itself"); New folder row at the end |

## 4. States

| State | Tokens and behaviour |
|---|---|
| Rest | `action/quiet/fg`; count `text/muted` |
| Hover | `action/quiet/bg-hover`; count stays `text/muted` |
| Focus-visible | README focus ring, inset form |
| Active | `action/quiet/bg-pressed` |
| Selected | `navigate`: the current place (a folder row in the sidebar selects only to drag or act; the place stays the open one), `selection/fill` and the start bar `selection/indicator`. `choose`: the current location when known, same look |
| Disabled | `state/disabled/fg` and the reason line in `text/muted`, `type/caption`. Still focusable |
| Drag source | `state/drag/source-opacity` |
| Drop target | `state/drag/target-fill`, `state/drag/target-line`; between rows `state/drag/insertion` is not used (folders do not order by hand) |
| Drop invalid | No highlight; ghost "Cannot move here" |
| Renaming | Inline TextField in place of the name, Enter commits, Escape cancels |
| Loading | Skeleton rows after 300 ms, held 300 ms |
| Empty | `choose` with no folders: only the Library row and New folder. `navigate`: nothing |
| Error | A row "Could not load folders" with Retry |

## 5. Content rules

- **Long names:** one line, end ellipsis, full name in the Tooltip and to screen readers. On touch, where a Tooltip is a long press, the folder Menu has a Details row that shows the full name; at 200 percent text rows wrap to two lines.
- **No name:** never empty; a new folder starts in rename with "Untitled folder" selected.
- **Depth:** four visible indents; deeper rows keep the fourth indent. The accessible level is the
  true level ("level 6").
- **500 folders:** virtualised above 200 visible rows; Filter; counts stay.
- **Filter result:** matching rows show with their ancestors, expanded; the rest are hidden.

## 6. Overflow rule

The list scrolls. There is no "More". Collapsing keeps expanded state per folder, per device.
Auto expand on drag hover after 600 ms; it collapses again if nothing is dropped. Deep paths are
shown in the TopBar, not by truncating the tree.

## 7. Extension slots

None.

## 8. Pack content

None. Folder styles are looks on folders; the tree shows the folder icon only.

## 9. Input

- **Stylus and touch:** tap opens or applies. Tap the chevron to expand. Long press then lift
  selects the folder; long press then move drags it. Row targets are `size/hit/touch` high.
- **Mouse:** click, right click for the folder Menu, drag after 4 px.
- **Keyboard:** Up and Down, Right (expand, then first child), Left (collapse, then parent), Home
  and End, Enter opens or applies, F2 renames, Delete moves to Trash, Menu key opens the Menu,
  type-ahead by first letter. Moves have a Menu equivalent: Move to.
- **Drop rules:** a folder into itself or one of its own folders is refused with the reason, shown in the ghost ("Cannot move a folder into itself"). A valid drop is journaled and shows a Toast with Undo.

| Platform | Role |
|---|---|
| Web | `tree` with `treeitem`, `aria-level`, `aria-expanded`, `aria-setsize`; "Maths, folder, level 2, collapsed, 12 items" |
| Compose | Lazy list with `collapse`/`expand` semantics and `Role` on rows |
| WinUI | `TreeView` / `TreeViewItem` |
| SwiftUI | `OutlineGroup` in a `List` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/default`, `size/control/small`, `size/hit/touch`, `size/icon/default` | Rows, chevron, targets |
| `space/sm`, `space/md`, `space/lg`, `radius/item` | Gaps, indent steps, row shape |
| `type/label`, `type/caption`, `text/default`, `text/muted` | Text |
| `action/quiet/*` | The More button |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Rows |
| `selection/fill`, `-hover`, `selection/indicator`, `border-width/emphasis` | Selected |
| `state/drag/source-opacity`, `target-fill`, `target-line` | Drag and drop |
| `component/focus/*` | Focus |

## 11. Do and Don't

**Do**
- Cap the indent at four levels and keep the true level for assistive technology.
- Give every disabled row a reason line.
- Offer Move to as the equivalent of every drop.
- Give the chevron a Tooltip and the Right and Left keys.

**Don't**
- Don't reorder folders by drag; they sort by the place's sort.
- Don't truncate the tree to show a deep path; use the TopBar PathBar.
- Don't colour folder icons in chrome; folder colours live on tiles.
- Don't let a drop on an invalid target highlight.

## 12. Traces to

Flows: 05 organize-folders (Sidebar and MoveToSheet), 02, 06, 14 (via FolderChooser).
Replaces: nothing; it is new (FolderTree in COMPONENTS).
