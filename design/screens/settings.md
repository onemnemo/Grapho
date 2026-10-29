# Screen: Settings (index, Appearance, Pen and tools, Packs, Extensions, one Content page)

## 1. Purpose

One home for every preference and for content management. Settings is a place, never a modal. It has an
index (find any setting, content page, pack or extension), App pages, one Content page per content
type, and Sources (Packs and Extensions). A contextual copy of a setting elsewhere shows the same value
with "More in Settings", which deep-links here.

## 2. Flows served

12 settings (main), 07 appearance and themes, 08 packs, 09 extensions, 03 (Pen and tools), 14 (Sync),
10 (reset a bad theme, safe mode), 02 and 07 (Content pages).

## 3. Structure

| Group | Pages |
|---|---|
| App | General (includes Keyboard), Appearance, Language, Pen and tools, Toolbar, Sync, Battery, Search index, About |
| Content | One page per registry content type (Pens, Colour sets, Covers, Folder styles, Canvas presets, Stickers, Templates). Themes links to Appearance |
| Sources | Packs, Extensions |

Text size is not a setting; it follows the operating system. Deep links: `grapho://settings`,
`grapho://settings/pen`, `grapho://settings/pen/pen.penOnly`, `grapho://content/covers`,
`grapho://packs/<id>`, `grapho://extensions/<id>`. A missing target falls back to the nearest parent
with a Toast "Setting not found". Commands are not linkable.

## 4. Layout and regions

Regions: TopBar `place` settings (Back and title; More holds Command palette), section list (Sidebar
`kind` settings, with the index SearchField above it), page (scrolls), Toast stack.

### Expanded (840 and up)

```
+--------------+---------------------------------------------+
| [Filter all ]| < Pen and tools                              |
| App          |  Pen-only drawing                  [ on  ]   |
|  General     |   Fingers scroll and zoom, never draw.       |
|  Appearance  |   This device only.                          |
|  Language    |  Two-finger tap Undo               [ on  ]   |
|  Pen & tools |  Pen button click        [ Open wheel v ]    |
|  Toolbar     |  Pen button hold         [ Eraser v ]        |
|  Sync        |  Palm guard      (Standard)(Strict)          |
|  About       |  Writing hand    (Right)(Left)               |
| Content >    |                                              |
| Sources >    |                                              |
|  Packs  .    |                                              |
+--------------+---------------------------------------------+
```

The Sidebar is persistent beside the page. It never mirrors for handedness. Right to left mirrors it to
the right.

### Medium (600 to 839)

Portrait: one column at a time (the list, then a page pushed with Back). Landscape: list and page as
two columns. Content and Sources are collapsed groups in the list.

### Compact (under 600)

```
+--------------------------+     +--------------------------+
| Settings                 |     | < Pen and tools          |
| [ Find a setting     ]   |     | Pen-only drawing  [ on ] |
| App                      |     |  Fingers scroll...        |
|  General              >  |     | Two-finger Undo   [ on ] |
|  Appearance           >  |     | ...                      |
|  ...                     |     |                          |
| Content               v  |     |                          |
| Sources               v  |     |                          |
+--------------------------+     +--------------------------+
```

The section list is the Settings root page; each page pushes with Back; Back from a page goes to the
list first, then leaves Settings. Short: same, the index field scrolls with the list so it is not
pinned over a short window.

Handedness: Settings does not mirror. The only handed control is the Writing hand SegmentedControl.

## 5. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | TopBar `place` settings | Back and title only; More: Command palette |
| List | Sidebar `kind` settings; SearchField scope filter; GroupHeading | Dot (IconButton or ListRow state) on Packs and Extensions only, for updates |
| Rows | SettingRow `layout` inline or stacked: label, helper, control, Reset when not default | A device-scoped setting says "This device only." |
| Controls | Toggle, SegmentedControl (at most three short options), Select, RadioList, Slider, Stepper, ShortcutField, ListRow (link rows, pushed pages) | Controls apply at once; no Save |
| Banners | Banner inline: "Sync is off." with a Toggle; "Safe mode. Extensions are off."; "This theme was adjusted for contrast." with the text Button "Use the default theme" | One at a time |
| Feedback | Skeleton, Toast, ProgressItem (rebuild index), EmptyState, Dialog | |

### 5.1 Index

SearchField scope `filter` narrows the whole registry, including Content, pack and extension pages. A
result is a ListRow `plain` with the setting name and a breadcrumb subtitle (Section, then Setting); selecting it opens the page scrolled to the row,
highlighted for 2 s, focused and announced. No match: EmptyState `filtered` "No settings match 'sync2'."
with Clear. Ctrl+F filters the index while Settings has focus.

### 5.2 Appearance

```
< Appearance
(i) This theme was adjusted for contrast.   [ Use the default theme ]
Mode      (Light)(Dark)(System)
Density   (Automatic)(Touch)(Compact)
Dark paper in dark mode                      [ off ]
 Paper stays light unless this is on.
Theme     Dusk                                    >
```

SegmentedControl Mode (Light, Dark, System, follows the device), SegmentedControl Density,
Toggle `appearance.pageDark`, ListRow Theme that pushes the Picker (pick mode, `themePreview`,
SourceFilter, footer Get more, Import, New; New pushes the ThemeEditor page with Back). Tapping a
theme repaints at once and stays on the page. The contrast guard adjusts failing pairs and the Banner
offers the text Button "Use the default theme". The same reset is the palette command
`appearance.resetTheme`. A custom theme never makes controls unreadable. Values persist and sync
(`appearance.theme`).

### 5.3 Pen and tools

SettingRow list: Writing hand (SegmentedControl Right, Left), Pen-only drawing (Toggle), Two-finger
tap Undo and three-finger Redo (Toggle), Pen button click and hold (Select of commands), Palm guard
(SegmentedControl), Hold to snap shapes (Toggle), Pen smoothing and pressure (Slider), Quick slots shown
(Stepper 0 to 5). The Tool settings tab in a document shows the same values and "More in Settings". This
page is the one home for pen settings.

### 5.4 Packs

```
Packs  .   [Filter] Sort v         [Get more][Import][New pack]
Botanica 1.2  Sample Author  42 items         [on ]  ...
Lab book 2.0  Grapho    12 items         [off]  ...
2 removed, in Trash                                  >
```

ListRow `kind` source per pack: name, version, author, item count, Toggle, ... Menu; a Dot on updates.
Footer buttons Get more, Import, New pack (the Packs page's own, in the order Get more, Import, New).
The link row "2 removed, in Trash" opens Trash filtered to Packs. Tapping a row opens pack-detail.md. On
and off: Toast "Botanica is off" with Undo; usages stay. Empty: EmptyState `first-use` `sm` "No packs
yet." Get more, Import. Offline: Banner "You are offline. Showing packs you have already seen." Get
disabled for uncached files; Import stays. No catalogue: "No pack catalogue yet." Import, Open
grapho.one. Error: Banner region "Could not load packs. Try again." Retry.

### 5.5 Extensions

ListRow `kind` source per extension: name, version, publisher, source ("From Grapho", "Installed by
you"), Toggle, Menu. A row needing a decision reads "Needs your OK for 2 new permissions" with Review
and contributes nothing until granted. The built-in group is off by default. Empty: "Installed by you:
nothing yet. Get extensions or import a file." Safe mode: Banner "Safe mode. Extensions are off." and each
extension turns on one by one; one that fails again is marked "Turned off after an error". 50
extensions: virtualised list with Filter. Install review and consent are Sheets (pack-detail.md).

### 5.6 One Content page: Covers

ContentPage with the Picker (`placement` page, `mode` manage): title "Covers" (wraps at 200%), one text
Button "Manage packs", the source filter (All, Built in, Yours, packs, Hidden), Filter, Sort, grid of
PickerTile `cover`, SelectionBar `picker` (Edit, Duplicate, Rename, Hide or Delete, Export as pack),
footer Get more, Import, New. New and Edit open Studio. Delete goes to Trash. Empty "Yours": EmptyState
`first-use` `sm` "No covers of your own." New, Get more. 500 covers: virtualised, Filter first.

## 6. States

| State | What changes |
|---|---|
| First visit | Index field and the App group; the list shows no badges |
| Loading | SettingRow skeletons after 300 ms for a slow registry; Content and Packs pages show Picker or list Skeleton |
| Save error | Inline under the row "Could not save. Try again." Retry; the control returns to its stored value |
| Offline | Only Sync and extension network access have an offline state; Get more shows its offline Banner |
| Sync states | A status line on the Sync page and the SyncIndicator icon in the Sidebar foot: off, up to date, syncing, offline (queued), conflict, storage full |
| Battery | "Indexing paused. Not charging." under `battery.chargeOnly` |
| Search index | A progress line and "Rebuild index"; a running rebuild is a ProgressItem |
| Updates | Dot on Packs and Extensions only |
| Very large | 50 extensions: Filter; Content pages virtualise |
| Long German text | Labels wrap to two lines; the control moves below its label; no SegmentedControl with more than three short options (Mode and Density measure and become a Select when they do not fit) |
| 200% text | Rows wrap; Sidebar rows grow; ListRows stack; Reset moves below the control |
| Safe mode | Banner at the top of the Settings place and the library; default theme active |

## 7. Behaviour on resize and split screen

Crossing 840 turns the pushed-page flow into two columns and back, keeping the open page, scroll and the
highlighted row. On Compact the open page becomes the pushed page over the list. Index text and results
survive. A running Picker keeps its selection. Crossing between portrait and landscape on Medium switches
between one and two columns without leaving the page.

## 8. Keyboard and focus order

Two regions (F6): section list and page. In the list Up and Down move, Right expands a group, Enter opens;
Ctrl+, opens Settings from anywhere; Tab goes through the page's controls; Space toggles; Escape is
Back. Remapping a shortcut (General, Keyboard): ListRow per command with its chord, Enter captures the
next chord with ShortcutField; a clash names the command ("Already used by Undo") and refuses; Reset
restores; an extension command that clashes stays unbound and its page says so. Arrival focus is the
page title; Back returns to the opener (a library place or a document) at the same scroll and
selection; focus returns to the opening control. Screen reader: each row reads label, helper, state and
"This device only"; a dependent setting says why it is disabled.
