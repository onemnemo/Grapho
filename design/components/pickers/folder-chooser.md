# FolderChooser

## 1. Purpose

Choose a folder as a destination or a scope: Move to, the Location of a new notebook, the folder filter
in search, and the folders to sync. It is the list that wraps FolderTree `choose` with Filter, "Library"
first, disabled rows with reasons and New folder.

**When not to use:**
- Navigating the library: Sidebar with FolderTree `navigate`.
- Choosing a look for a folder: Picker (folder styles).
- Two or three fixed destinations: RadioList.

## 2. Anatomy

```
[Filter folders_______________]            SearchField scope filter, always shown
 Library                                    first row, the top level
 v Semester 1              12
     Biology                4
     Maths  (current)       3
 > Archive                  8
   Reading list            Cannot move into itself        disabled with a reason line
 + New folder                               last row
```

| Part | Notes |
|---|---|
| Filter | SearchField `filter`; matching rows show with ancestors expanded |
| Tree | FolderTree `choose`: rows `size/control/default`, four visible indents, disclosure, count |
| Library row | The top level; always first; never filtered out |
| Reason line | `type/caption`, `text/muted`, under a disabled row |
| New folder row | Last row, labelled with its parent: "New folder in Biology" (the last expanded folder, else Library). Command `library.newFolder`. Opens inline rename ("Untitled folder" selected) |
| Check column | Only in `multiple` |

## 3. Variants and sizes

| Prop | Values | Meaning |
|---|---|---|
| `mode` | `single`, `multiple` | `single` applies on tap (Move to, Location); `multiple` toggles rows (sync scope, and the search folder filter in a Popover) |
| `host` | `sheet`, `popover`, `inline` | Move recipe of Sheet (`single`); Popover for the search folder filter (`multiple`); inline in a Settings page (`multiple`) |

One size. Rows are `size/row/list` (ListRow), never below `size/hit/touch`.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Default | `text/default`, counts `text/muted` |
| Hover | `hover/fill` |
| Focus-visible | `focus/*`, inset |
| Active | `press/fill` |
| Selected | The current location (`single`) or each ticked folder (`multiple`): `selection/fill`, `selection/indicator` |
| Disabled | `state/disabled/fg`, reason line; focusable; Enter announces the reason. The current row is the only non-selectable row, reason "Already here". Other reasons: "Cannot move a folder into itself", "Cannot move a folder into one of its own folders". A bare "Not allowed" is never used |
| Loading | Skeleton rows after `feedback/delay/skeleton`, held `feedback/hold/skeleton` |
| Empty | Library row and New folder only; no EmptyState |
| Error | A row "Could not load folders" with Retry |
| Creating | The New folder row becomes an inline TextField; Escape cancels; Enter commits and selects the new folder (`single`: applies) |

## 5. Content rules

- Applying in `single` is one undoable step with the Toast "Moved 10 to Biology", Undo.
- Long names: one line, end ellipsis, full name as Tooltip; at 200% text rows wrap to two lines.
  Norwegian and German names do not shorten.
- No folders: Library and New folder. 1 folder: shown under Library. 500 folders: virtualized above 200
  rows; Filter live; counts stay.
- Depth beyond four keeps the fourth indent; the accessible level is the true level.
- Filter shows matches with their ancestors; nothing else.

## 6. Overflow rule

The list scrolls inside its host. Collapsing state is per device. In a Popover the height is
`size/popover/max-height`; the Filter stays pinned.

## 7. Extension slots

None.

## 8. Pack content

None. A folder's pack style is not shown here (FolderTree shows the folder icon only).

## 9. Input

- **Stylus and touch:** tap applies (`single`) or ticks (`multiple`); tap the chevron to expand. No drag.
- **Mouse:** click; double click on a folder expands it.
- **Right to left:** indents, chevrons and Left and Right keys mirror; the Popover opens toward the inline end.
- **Keyboard:** Up and Down move; Right expands, Left collapses; Enter applies; Space toggles in
  `multiple`; type-ahead jumps; typing in Filter filters; Escape closes the host (a Sheet or Popover).
  Tab order: Filter, tree, New folder.
- **Roles:** `tree` and `treeitem` with `aria-level`, `aria-expanded`, `aria-disabled`; `multiple` adds
  `aria-multiselectable`. Compose `LazyColumn` with tree semantics; WinUI `TreeView`; SwiftUI
  `OutlineGroup`.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/overlay`, `surface/raised` | Sheet or Popover host, inline |
| `size/row/list`, `size/control/default`, `size/hit/touch` | rows, Filter, targets |
| `space/md`, `space/sm` | indents, padding |
| `radius/item` | row hover and selection |
| `type/label`, `type/caption` | name; count and reason |
| `text/default`, `text/muted` | name; count and reason |
| `hover/fill`, `press/fill` | row states |
| `selection/fill`, `selection/indicator` | current or ticked |
| `state/disabled/fg` | disabled |
| `size/icon/default`, `size/icon/small` | folder icon, disclosure |
| `focus/*`, `feedback/delay/skeleton` | focus, loading |

## 11. Do and Don't

Do:
- Put "Library" first and keep it visible when the Filter has no match elsewhere.
- Give every disabled row a reason line in words.
- Apply at once in `single`, so Move to is one tap.

Don't:
- Hide a disabled folder; the user wonders where it went.
- Add a confirm button to `single`; the host Sheet has Cancel only.
- Show pack or style information.

## 12. Traces to

Commands: `library.newFolder` (existing). Tooltips: the disclosure chevron says Expand or Collapse.

Flows 02, 05, 06, 14. Replaces the MoveMenu list. Uses FolderTree `choose`, SearchField, ListRow
rows. Hosted by the Move recipe of Sheet, NewItemSheet (Location), Popover (search), Settings.
