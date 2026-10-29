# Sidebar

## 1. Purpose

Move between places. The library variant lists Home, Starred, folders and Trash, and holds
Settings and the sync status at its foot. The settings variant lists the Settings sections.
Persistent on expanded windows, a drawer on every smaller one.

**When not to use:**
- Actions on the current place: TopBar.
- Switching panels of content in a Sheet or the Studio: Tabs.
- Document navigation (pages, layers): SidePanel.
- A choice inside a Sheet (move to a folder): FolderChooser, which reuses FolderTree.

## 2. Anatomy

```
Library, persistent                  Settings, expanded
+------------------+                 +------------------+
| Home             |                 | [ Filter ...... ]|
| Starred          |                 | App              |
| Semester 1   v   |  FolderTree     |  General         |
|  Biology         |                 |  Appearance      |
|  Maths           |                 |  ...             |
| Trash            |                 | Content          |
|------------------|                 |  Pens            |
| [cloud] Synced   |  SyncIndicator  | Sources          |
| [gear] Settings  |                 |  Packs  (dot)    |
+------------------+                 +------------------+
```

| Part | Notes |
|---|---|
| Places | Home, Starred, the folder tree, Trash, in that order. Rows are `size/control/default` high. Groups are separated by a GroupHeading row ("Folders"); a new kind of place (tags, shared, recent) gets its own group and heading in this list, between Starred and Folders, and never changes the order above it |
| FolderTree | The folders, from [folder-tree.md](folder-tree.md); a Filter field above 30 folders |
| Foot | The SyncIndicator row, then the Settings row. Always visible, never scrolls with the list. Folder icons stay uncoloured here (a decision, logged); colour belongs to the tile |
| Settings sections | Grouped App, Content, Sources, from the settings registry, with a Dot on Packs and Extensions only for updates |
| Scrim (drawer) | `scrim/default` over the page; none when persistent |

## 3. Variants and sizes

`kind`: `library` | `settings`. `mode`: `persistent` | `drawer`.

| Class | `library` | `settings` |
|---|---|---|
| Expanded (840 and up) | `persistent`, `size/sidebar/width`, may be collapsed with `library.drawer` (Ctrl+B, remembered), then a Menu IconButton appears in the TopBar | `persistent` beside the page |
| Medium, compact, or short | `drawer`, opened from the TopBar's Menu button, width the lesser of `size/sidebar/width` and the window less `size/hit/touch` | Not a drawer: the section list is the Settings root page and each page pushes with Back |

Rows are the `default` role. Surface `surface/raised`, edge `border/subtle`. A persistent sidebar
is on `z/chrome`; a drawer is on `z/drawer` under `z/scrim`. The sidebar sits at the start edge and
never mirrors for handedness; right to left mirrors it to the right.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Rest row | `action/quiet/fg`, count in `text/muted` `type/caption` |
| Hover | `action/quiet/bg-hover`; meta text `text/muted` |
| Focus-visible | README focus ring, inset form (the list clips) |
| Active | `action/quiet/bg-pressed` |
| Selected (current place) | `selection/fill`, `-hover`, bar `selection/indicator` at the start edge, `border-width/emphasis` thick. Name text `text/default` |
| Drop target | Folder rows only: `state/drag/target-fill` and `state/drag/target-line` on enter; invalid targets do not highlight and the ghost says "Cannot move here" |
| Disabled | A place that is unavailable keeps its row with `state/disabled/fg` and the reason |
| Palm | n/a: the library has no writing. The sidebar needs no palm rule |
| Loading | Skeleton rows in the real layout after 300 ms, held 300 ms |
| Empty | No folders: only Home, Starred, Trash. Nothing to explain |
| Error | Rows from the model; a failed folder load is an inline row "Could not load folders" with Retry |
| Drawer open | Opening: slide from the start edge `motion/duration/base`; closing `motion/duration/exit`. Reduced motion: cross fade 100 ms |
| Writing | The library has no writing; n/a |

## 5. Content rules

- **Very long folder name:** one line with an end ellipsis, full name in the Tooltip (the row is not
  icon-only, so the Tooltip is for truncation) and to screen readers. German names grow 30 to 40
  percent; width never changes.
- **No folders:** three rows and the foot.
- **500 folders:** the tree virtualises above 200 rows; Filter above 30 folders; counts stay.
- **Counts:** item counts are `type/caption`; Trash shows "removes in N days" only in its place.

## 6. Overflow rule

The list scrolls; the foot is fixed. Folders show four indent levels; deeper levels share the
fourth indent and the TopBar shows the path. Settings sections scroll under a Filter on the
index when the sections exceed what the window shows; extension pages appear only under
Extensions. Nothing is ever hidden in a More.

## 7. Extension slots

None. Extensions cannot add places or change the Library layout; their Settings page is one row
under Extensions.

## 8. Pack content

None. The Packs and Extensions rows show a Dot for updates after a background check, and nothing else.
Pack state (Off, Hidden, Kept from, Not installed) lives on those pages.

## 9. Input

- **Stylus and touch:** tap a row opens the place (and closes the drawer). Drag from a notebook
  tile onto a folder row drops it; hover over a collapsed folder for 600 ms expands it. A long
  press then move on a folder row drags it (4.7.1); long press then lift selects it. No edge swipe
  opens the drawer, because the start edge belongs to the system back gesture; the Menu button,
  `library.drawer` and Back are the openers. Dragging the open drawer toward the start edge closes it.
- **Mouse:** click, hover; right click opens the folder Menu; drag after 4 px.
- **Keyboard:** the sidebar is a landmark (F6 and Shift+F6 cycle landmarks: top bar, sidebar, content). Up and Down move, Right expands, Left collapses or
  goes to the parent, Enter opens, F2 renames in place, Delete moves the folder to Trash, Menu key
  or Shift+F10 opens the folder Menu. The drawer traps focus; Escape and Back close it and return
  focus to the Menu button.
- **Back and Escape:** the drawer is a library surface and the bottom panel a document one, so they
  are never open together; the drawer closes at the same step as the bottom panel (step 6 of the
  order in SYSTEM-MAP 3: after popovers, dialogs, sheets and pushed views, before selection mode). A
  persistent sidebar is never closed by Back.
- **Commands:** `library.drawer`, `app.settings`, `library.rename`, `library.delete`.

| Platform | Role |
|---|---|
| Web | `navigation` "Library places" containing a `tree`; Settings `navigation` with a `list` |
| Compose | `ModalNavigationDrawer` or `PermanentNavigationDrawer` around a tree of selectable rows |
| WinUI | `NavigationView` pane (left, `Top` disabled), items `NavigationViewItem` |
| SwiftUI | `NavigationSplitView` sidebar with `List` and `OutlineGroup` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/sidebar/width` | Width (proposed) |
| `size/control/default`, `size/icon/default`, `size/hit/touch` | Rows, icons, targets |
| `surface/raised`, `border/subtle`, `scrim/default` | Panel, edge, drawer scrim |
| `selection/fill`, `-hover`, `selection/indicator`, `border-width/emphasis` | Current place |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Rows |
| `state/drag/target-fill`, `state/drag/target-line` | Drop target |
| `type/label`, `type/caption`, `text/default`, `text/muted` | Text |
| `space/sm`, `space/md`, `radius/item` | Gaps, padding, row shape |
| `motion/duration/base`, `exit`, `z/chrome`, `z/drawer`, `z/scrim`, `component/focus/*` | Motion, layers, focus |

## 11. Do and Don't

**Do**
- Persist on expanded, drawer below 840 or when short, and keep state across a resize.
- Keep the foot (sync status, Settings) visible and fixed.
- Trap focus and close on scrim tap, Escape and Back when it is a drawer.
- Show a Dot only for updates on Packs and Extensions.

**Don't**
- Don't use a modal drawer for Settings sections on medium or compact; push pages.
- Don't open the drawer from an edge swipe.
- Don't put document tools, pack state or badges in it.
- Don't mirror it for left-handed use.

## 12. Traces to

Flows: 05 (folders, drop), 07, 10 (Trash), 12 (Settings), 14 (sync at the foot).
Replaces: SettingsOverlay, SettingsNav. Holds the SyncIndicator and the FolderTree.
