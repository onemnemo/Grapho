# TopBar

## 1. Purpose

Name the current place and hold its main actions: Library places (Home, a folder, Starred,
Trash), Search results and Settings. One bar for all of them; the place decides the actions.

**When not to use:**
- Inside a document: NoteBar.
- Acting on selected items: SelectionBar (the TopBar does not change into it).
- Navigating between places: Sidebar. Settings and the SyncIndicator live at the Sidebar foot.
- A Sheet or Dialog title: the Sheet's own header.

## 2. Anatomy

```
Expanded
+---------------------------------------------------------------------------+
| Semester 1 / Biology        [ Search ......... ] [ New v ] [Select] [grid] [...] |
+---------------------------------------------------------------------------+
  title or PathBar            search   primary     action    action    More

Compact (title first; search is an icon that expands over the whole bar)
+------------------------------------------+
| [=] Biology        [Q] [ New v ]    [...] |
+------------------------------------------+
```

| Part | Notes |
|---|---|
| Lead | On compact and medium (and expanded when collapsed): Menu IconButton `menu`, command `library.drawer`. On a pushed Settings page, Search results, or a child page: Back IconButton `arrow_left` instead |
| Title | `type/heading`, the place name. It is the place's heading: focus goes here on arrival |
| PathBar (part) | Replaces the title below four folder levels, see section 6 |
| Search | SearchField scope `global` (`app.search`, Ctrl+F). A field on medium and expanded, an IconButton `search` on compact |
| Actions | Buttons and IconButtons per place, section 6 |
| More | IconButton `quiet` `default`, `more`, `bar.more`, a Menu |

The Windows shell draws the bar inside the title bar with the system caption area reserved; macOS
and iPad use the toolbar. The visual and the actions are the same.

## 3. Variants and sizes

`place`: `home` | `folder` | `starred` | `trash` | `search` | `settings`.
Height `size/bar/top`; contents role `default` (`size/control/default`). A hairline `border/subtle`
under the bar. Surface `surface/base`. Layer `z/chrome`. It never recedes and never floats.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Default | `surface/base`, title `text/default`, actions `action/quiet/fg` |
| Hover, Focus-visible, Active | As the child controls |
| Selection mode | Select shows selected; New becomes `secondary`. The SelectionBar appears at the bottom; the title keeps its place |
| Search open (compact) | The field fills the bar, with Back to collapse. Other actions are hidden, not removed from More |
| Loading | The title is shown at once from the model; actions are live. A busy action uses inline busy |
| Offline, Sync | No control here. The SyncIndicator is at the Sidebar foot; a problem is a Banner on the place and a line in More (section 6) |
| Disabled action | `state/disabled/fg` with the reason (New in Trash: not offered, not disabled) |
| Empty, Error, Selected, Drag | n/a. Items dragged to a path segment or title: no target |

## 5. Content rules

- **Very long folder name:** the title truncates at the end with an ellipsis; full name in the
  Tooltip and to screen readers. German and Norwegian names run 30 to 40 percent longer, so the
  title is measured and actions give way first (section 6).
- **No title:** never empty; Home is "Home".
- **Counts:** none in the bar. The count is in the Sidebar and the grid.

## 6. Overflow rule

The bar shows up to four actions, then More, in a fixed priority per place (flow 05). The Search
field is one of the four. The command priority (registry bands) decides what leaves first; use never
does.

| Place | Visible, in priority order | In More, in order |
|---|---|---|
| `home`, `folder` | Search, **New** (primary), Select, View | Sort, Activity, Command palette, Settings |
| `starred` | Search, Select, View | Sort, Activity, Command palette, Settings |
| `trash` | Search, Empty Trash, Select, Filter | Activity, Command palette, Settings |
| `search` | Search, Select, Filter | Activity, Command palette, Settings |
| `settings` | none: Back and title only | Command palette |

- **New never overflows.** It is the place's primary, so when width runs out the others leave
  first. Search is the next to stay, and shrinks to its icon before anything else moves.
- When width runs out, actions leave from the end of the priority list **into More, at its top, in
  a first group, in priority order**, then Sort, Activity, Command palette and Settings follow. Every
  action that leaves is reachable there; nothing is dropped. The order inside More is one list.
- Labelled or icon only, per action, measured at runtime (never fixed at a width): Select is a
  `text` Button with `check_square` and the label "Select" while it fits, else an IconButton
  toggle; View is always an IconButton toggle showing the view it would switch to (`layout_list`
  when in grid, `layout_grid` when in list), Tooltip "List view" or "Grid view", with the state
  in its accessible name; Filter is an IconButton `filter` with Tooltip. New keeps its label at
  every width: it is a Button `primary` with a trailing `chevron_down` for its Menu. Empty Trash is a
  `secondary` Button with its label and is never icon only. Search is the field, then an icon.
- Commands: `app.search`, `library.newNotebook`, `library.newFolder`, `library.import` (the New
  Menu), `library.select`, `app.activity.show`, `app.palette`, `app.settings`, `library.emptyTrash`
  (to register). View flips `general.libraryView` and Sort sets `general.librarySort`; a control that
  edits a setting binds to it, not to a command. Filter opens a Popover.
- **Settings has two homes, by window:** the Sidebar foot where the sidebar is persistent (expanded),
  and More everywhere the sidebar is a drawer, so it is one tap or two on every class. The sync state is
  at the Sidebar foot; where the sidebar is a drawer, a sync problem (offline queue, conflict,
  storage full) appears as a Banner on the place, which is how a lasting state is shown (4.3), and
  More shows a sync line when sync is on and not up to date.
- **Selection mode:** Select becomes a toggle in its selected form (`selection/fill`,
  `selection/indicator`) and New drops from `primary` to `secondary`, so the primary does not invite
  creation while the user is choosing what to delete. The count is in the SelectionBar.
- **200 percent text:** the title wraps to two lines and the bar grows; actions follow the rule
  above and labelled Buttons go to icons first.
- **PathBar:** below four folder levels the title becomes "... / Biology / Labs / Week 4": the
  current folder is last and is the heading, not a button; each ancestor is a Button `text` that goes
  up to it. Middle segments collapse to "..." which opens a Menu of the hidden ancestors. On compact
  the PathBar is the current name plus a `chevron_down` that opens that Menu.
- **500 items:** n/a.

## 7. Extension slots

None. Extensions cannot add places or change the Library layout. Library item actions from
extensions are in the item Menu.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch:** tap; a long press on an icon-only action shows its Tooltip (4.7.1). Targets
  are `size/hit/touch`. Back and the drawer button sit at the start edge on all handedness (the
  drawer does not mirror).
- **Mouse:** hover fill, Tooltips at 500 ms, right click on the title does nothing. Windows: the
  empty area drags the window.
- **Keyboard:** the buttons are one tab stop; Left and Right move between them; Enter activates. The
  Search field is its own tab stop and keeps its arrow keys for the caret; arrows rove only the buttons
  around it (the same rule in the NoteBar and the FindBar). Ctrl+F focuses Search; Ctrl+B toggles the
  sidebar; F6 and Shift+F6 cycle landmarks (top bar, sidebar, content, side panel, dock); Escape in
  an open compact search collapses it.
- **Focus:** on arrival, to the title; after a Menu closes, to its trigger.
- **Right to left:** order and Back mirror.

| Platform | Role |
|---|---|
| Web | `toolbar` "Library, toolbar" with `heading` level 1 for the title; Search is `search` |
| Compose | Custom top app bar; title `heading()` semantics; actions `Role.Button` |
| WinUI | Title-bar `CommandBar` with a `TextBlock` heading; `AutomationProperties.Name` |
| SwiftUI | `.toolbar` with `ToolbarItem`s; title via `navigationTitle` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/bar/top`, `size/control/default`, `size/hit/touch`, `size/icon/default` | Height, contents |
| `surface/base`, `border/subtle` | Bar, hairline |
| `type/heading`, `type/label`, `text/default`, `text/muted` | Title, path, labels |
| `action/primary/*`, `action/secondary/*`, `action/quiet/*` | New, Empty Trash, icons |
| `selection/fill`, `selection/indicator` | Select in selection mode |
| `space/sm`, `space/md`, `layout/gutter/*` | Gaps, side padding by class |
| `state/disabled/fg`, `z/chrome`, `component/focus/*` | Disabled, layer, focus |

## 11. Do and Don't

**Do**
- Keep at most four actions then More, by the place's fixed priority.
- Show one primary, New, only where New applies, and keep it on the bar at every width.
- Use a labelled Button for Empty Trash and send the user to the Dialog.
- Make the title the heading and move focus to it on arrival.

**Don't**
- Don't make Settings the only way in: it is in More, and at the Sidebar foot where that is persistent.
- Don't reorder actions by use, or hide an action instead of moving it to More; never move New.
- Don't change the bar into the SelectionBar.
- Don't add extension actions.

## 12. Traces to

Flows: 05 organize-folders (priority per place), 06, 10, 12. Replaces: LibraryHeader, SectionHeader.
PathBar is a part of this component.
