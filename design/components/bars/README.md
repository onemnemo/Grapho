# Bars and panels

The chrome that frames the library and the document: the bars, the sidebar, the dock and the side
panel. Rules come from SYSTEM-MAP 1, 2, 3, 7 and 8, `system/document.md`, `system/accessibility.md`.
Every member traces to a flow in `flows/COMPONENTS.md`.

## Members

| Component | File | Variants | Job | Flows |
|---|---|---|---|---|
| TopBar | [top-bar.md](top-bar.md) | `place`: home, folder, starred, trash, search, settings | Name the place, four actions then More | 05, 06, 10, 12 |
| Sidebar | [sidebar.md](sidebar.md) | `kind` library, settings; `mode` persistent, drawer | Move between places | 05, 07, 10, 12, 14 |
| FolderTree | [folder-tree.md](folder-tree.md) | `kind` navigate, choose | Nested folders, drop target | 02, 05, 06, 14 |
| NoteBar | [note-bar.md](note-bar.md) | `position` floating, line; `layout` full, core | Document chrome that recedes | 03, 11, 13 |
| NoteBarHandle | [note-bar-handle.md](note-bar-handle.md) | none | Recall control for the receded bar | 03 |
| ToolDock | [tool-dock.md](tool-dock.md) | `orientation` vertical, horizontal | Tool, slots, Undo, Redo, never recedes | 03, 10, 13 |
| SidePanel | [side-panel.md](side-panel.md) | `placement` beside, over, bottom; `snap` peek, full | Work beside the page; TabRail is a part | 03, 07, 09, 11, 12, 15 |
| SelectionBar | [selection-bar.md](selection-bar.md) | `placement` library, canvas, panel, picker | Act on the selection | 05, 08, 10, 15 |
| FindBar | [find-bar.md](find-bar.md) | none | Search the document | 06 |
| LayersPanel | [layers-panel.md](layers-panel.md) | `documentKind` notebook, pdf, canvas | Layers tab body, document wide layers | 15 |
| ToolSettingsPanel | [tool-settings-panel.md](tool-settings-panel.md) | `tool` pen, highlighter, eraser, lasso, text, shape, extension | Tool settings tab body | 03, 04, 07 |
| ExtensionPanelHost | [extension-panel-host.md](extension-panel-host.md) | `provenance` installed, sideloaded; six states | Body of an extension's panel tab | 09 |
| Tabs | [tabs.md](tabs.md) | none | Two to four panels under a preview | 07, 14 |

Parts defined inside their host: QuickSlot (ToolDock), TabRail (SidePanel), PathBar (TopBar).

## How they line up

Heights are roles from `size/*`; specs never write a number. Rows read "touch / compact" only
where the token README gives both.

| Property | TopBar | NoteBar, FindBar, SelectionBar | ToolDock | SidePanel | Sidebar | Tabs |
|---|---|---|---|---|---|---|
| Bar height | `size/bar/top` | `size/bar/note` (FindBar a minimum) | strip: `size/dock/slot` + 2 `space/nudge` | title row and tab row, each `size/control/default` | n/a | `size/control/default` |
| Item role | `default` | `small` | `size/dock/slot` | `default` | `default` rows | `default` |
| Icon size | `size/icon/default` | `size/icon/small` | `size/icon/default` | `size/icon/default` | `size/icon/default` | `size/icon/default` |
| Surface | `surface/base` | `surface/raised` | `surface/raised` | `surface/raised` | `surface/raised` | `surface/raised` |
| Edge | hairline `border/subtle` | floating: `border/subtle` + `elevation/float`; docked: hairline | hairline on the page side | hairline; `elevation/float` over and bottom | hairline | divider |
| Radius | none | `radius/surface` floating, none docked | `radius/pill` targets | `radius/surface` over and bottom | `radius/item` rows | `radius/item` hover |
| Gap between items | `space/sm` | `space/sm` | `space/xs` | `space/sm` | row to row none, `space/xs` | none |
| Side padding | `layout/gutter/*` | `space/md` | `space/sm` | `space/lg` | `space/md` | `space/lg` |
| Label style | `type/heading`, `type/label` | `type/label` | none | `type/heading`, `type/label` | `type/label`, `type/caption` | `type/label` |
| Layer | `z/chrome` | `z/floating-bar` (`z/chrome` for `line`) | `z/chrome` | `z/panel` | `z/chrome`; drawer `z/drawer` | with host |
| Recedes | never | NoteBar only | never | never | never | never |

Shared rules:

- Items in one bar share a role. A bar holds four icon-only actions on its trailing side plus
  its More control; More is the overflow and is not counted.
- Overflow is by **command priority** (registry bands), never by use. Controls never shrink below
  their hit slot; they move into More.
- Chrome never animates while the pen is down. The only chrome that moves at pen contact is the
  NoteBar, once, in 150 ms; the dock, panel and FindBar stay still. Bars with a text field (TopBar
  Search, NoteBar title, FindBar) treat it as its own tab stop and rove arrows only among buttons.
- **Ink is never lost to chrome.** A pen tap anywhere at the top of the page inks. A pen-down on the
  page outside a Popover or Menu closes it and draws. A pen contact on a bar's gaps is ink.
- **Palm rule for chrome.** While the pen is in proximity or within `delay/palm-window` of its last
  contact, a touch larger than the platform palm threshold is ignored on the dock, panel, note bar,
  handle, FindBar, SelectionBar and scrims, and a finger tap counts only if under
  `input/chrome-tap/duration` and `input/chrome-tap/travel`. A palm on a scrim never dismisses
  anything. Pen taps always count.
- **Extension turned off:** everything it added to these bars disappears (note bar item, dock slot,
  panel tab, More rows, selection actions). Its positions are remembered and restored. Nothing is
  shown disabled.
- **Growth:** a new persistent mode indicator uses the dock's Status part (at most two, built in
  only). A new library place gets a group and heading in the Sidebar. A new long-lived status never
  goes into the note bar's four pinned items.
- **F6 and Shift+F6** cycle landmarks: top bar, sidebar, content, side panel, dock. Each bar sets a
  region or pane title. `bar.show` has no default key.
- Handedness (`general.handedness`) mirrors only what sits away from the hand: see the table below.
  Right to left language mirrors order and directional icons, never the dock's edge.

## Handedness and mirroring

| Item | Right-handed | Left-handed |
|---|---|---|
| ToolDock, vertical | left edge | right edge |
| ToolDock, horizontal corner strip | bottom left corner | bottom right corner |
| SidePanel beside or over | left, after the dock | right, after the dock |
| SidePanel bottom | full width, above the dock | full width, above the dock |
| TabRail | row inside the panel | row inside the panel |
| NoteBar, NoteBarHandle, FindBar | centred, no mirror | centred, no mirror |
| TopBar, Sidebar, SelectionBar library | no mirror | no mirror |
| SelectionBar canvas | clamps `size/edge-guard` off the writing-hand edge | same, mirrored |
| Item order inside the dock | tool, slots, Undo, Redo | the same, never reversed |

## Window classes and the short window

| Window | TopBar and Sidebar | NoteBar | Dock | Panel |
|---|---|---|---|---|
| Expanded, 840 and up | persistent sidebar | `full` | vertical | `beside` |
| Medium, 600 to 839 | drawer | `full` | vertical | `over` |
| Compact, under 600 | drawer, search icon | `core` | corner strip, bottom | `bottom` |
| Short, under 500 tall | drawer | as its width | corner strip, content width | `over` (bottom if compact) |

## Decisions

1. Chrome timings: recede 150 ms, return 200 ms, timer `delay/pen-idle`
   (1500 ms). The NoteBar slides up by its own height and fades, the one allowed
   motion at pen contact.
2. The NoteBar never reflows the page. `floating` overlays; `line` reserves its row. The band table
   in note-bar.md orders safe mode strip, NoteBar, FindBar and Banner.
3. A pen tap at the top of the page always inks. Return is by timer (paused while the pen hovers),
   pen hover on the top edge for 500 ms, a finger or mouse edge tap, the handle, F6 or Tab focus,
   or `bar.show`. Writing wins over a return in progress.
4. The handle is a glass pill sized from its pinned `size/icon/small` glyph; it is a part of the
   Note bar surface for SYSTEM-MAP 3.
5. More is the overflow control and not one of the four icon-only actions.
6. Overflow is by command priority: pinned note bar items, top bar actions (New never moves) and
   selection actions. Moved items go at the top of More.
7. The dock has no menu. Its settings are in the Tool settings tab, the note bar customise mode is a Customise link row in the note bar More, and sync state is in the Sidebar foot. Recent slots drop before pinned.
8. On compact the dock is a corner strip away from the writing hand, not a full width bar, with its
   length capped. At 360 wide it holds two slots.
9. No edge swipe opens the drawer or the panel. The sidebar can be collapsed on expanded with
   `library.drawer`. The drawer closes at the bottom panel step of the Back order.
10. SelectionBar has a fourth placement, `picker`, beyond the map's three. The visible four are
    fixed per selection kind; Trash has Restore and Delete forever.
11. The TabRail is a horizontal row inside the panel header, so tools (a column) and panels never
    look alike. Tabs falls back to a Select when labels do not fit.
12. Empty Trash is a labelled Button; New is the one primary in the Library TopBar and never overflows.
13. SidePanel `bottom` snaps between `peek` and `full`; `peek` favours the body when the window is short.
14. Registry: Ctrl+G is Find next on no platform; Go to page is Ctrl+Alt+G. Open question: Ctrl+Alt+1 to 4 clash with AltGr on Nordic and German layouts. Ctrl+Shift+1 to 4 is suggested as the replacement. The specs quote ids, not chords.
15. Sidebar folder icons stay uncoloured; the tile carries colour. Unpinned dock slots do not drag;
    the Menu Pin item is their route (an exception to 4.7.1).
16. The Escape rule for the panel follows accessibility.md (compact closes first; medium and expanded
    return focus to the page).

## Commands to register

`bar.more` (More), `selection.more` (More), `selection.clear` (Clear selection, Escape),
`library.emptyTrash` (also in sheets), `doc.rename` (Rename document, F2 in a document),
`tool.slotPin`, `tool.slotReplace`, `tool.slotMoveEarlier`, `tool.slotMoveLater`, `layer.add`, `layer.toggleVisible`, `layer.toggleLock`, `layer.rename`, `layer.moveUp`, `layer.moveDown`, `layer.mergeDown`, `layer.duplicate`, `layer.delete`, `selection.moveToLayer`. `bar.show` has no
default key. Existing ids used: `app.search`, `app.settings`, `app.palette`, `app.activity.show`,
`library.drawer`, `library.select`, `library.restore`, `library.deleteForever`, `doc.find`,
`doc.findNext`, `doc.findPrev`, `doc.undo`, `doc.redo`, `tool.wheel`, `tool.slot1` to `tool.slot5`,
`panel.toggle`, `panel.pages`, `panel.layers`, `panel.insert`, `panel.toolSettings`, `nav.goToPage`,
`view.minimap`. Setting to register: `toolbar.panelWidth` (device, between the panel min and max).

## Tokens to add

Each value is tied to one class or role.

| Token | Value | Used by |
|---|---|---|
| `size/sidebar/width` | 280 | Sidebar |
| `size/panel/width` | 360 | SidePanel beside |
| `size/panel/width-over` | 320 | SidePanel over |
| `size/panel/min`, `size/panel/max` | 320, 480 | SidePanel resize |
| `size/page/min` | 200 | Page height kept above a bottom panel |
| `size/page/min-width` | 400 | Page width kept beside a panel |
| `size/bar/note-max` | 720 | Floating NoteBar and FindBar width cap |
| `size/edge-guard` | 16 | Canvas SelectionBar clamp |
| `layout/breakpoint/short` | 500 | Short window rule |
| `delay/palm-window` | 1000 ms | Palm rule |
| `delay/hover-recall` | 500 ms | Pen hover at the top edge |
| `input/tap/duration`, `input/tap/travel` | 250 ms, 10 dp | Edge tap, finger and mouse |
| `input/chrome-tap/duration`, `input/chrome-tap/travel` | 300 ms, 8 dp | Finger tap on chrome under the palm rule |
| `input/palm-threshold` | set per platform by the shell as a contact size (proposed 36 dp across) | Palm rule |
| `motion/duration/chrome-recede`, `chrome-return` | 150 ms, 200 ms | NoteBar |
| `toolbar.panelWidth` (setting) | device, default `size/panel/width` | SidePanel |

## Pairs to declare (contrast matrix)

`action/quiet/fg` on `surface/base`, `surface/raised`, and on `action/quiet/bg-hover`, `bg-pressed`,
`selection/fill` and `selection/fill-hover` over those surfaces; `action/toggle/fg-on` on
`selection/fill` and `-hover`; `state/disabled/fg` as exempt on the same; `selection/indicator` at
3:1 on `selection/fill` over `surface/raised`; `media/edge` at 3:1 on `media/glass` (the handle's
press ring). `text/default` and `text/muted` on `surface/raised` and `surface/base` already exist.
