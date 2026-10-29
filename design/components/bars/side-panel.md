# SidePanel

## 1. Purpose

Work beside the content without hiding it: Pages (Views on Canvas), Layers, Insert, Tool settings
and extension panels. One panel with a tab row replaces the old Panel, Inspector and WheelPanel.
It is non-modal: the page stays live and writable.

**When not to use:**
- A task that needs focus and room (create, review an import): Sheet.
- A choice tied to one control that applies at once: Popover or Menu.
- Acting on the selection: SelectionBar. Searching the document: FindBar.
- Library navigation: Sidebar.

## 2. Anatomy

```
Beside / over the edge (right-handed: dock on the left, panel next to it)
+------------------------------+
| Pages                    [x] |  row 1: active tab title, Close
| [P][L][I][T][..][more]       |  row 2: TabRail, a horizontal row of icon tabs
|------------------------------|
|   tab body (own scroll)      |
+------------------------------+

Bottom panel (compact)
+----------------------------------+
|          ----- handle -----  [x] |  row 1: drag handle, Close at the end
| [P][L][I][T][more]               |  row 2: TabRail
|  Pages  (first line of the body) |
|   tab body                       |
+----------------------------------+

A pushed view replaces the body and the title: [<] Title [x]
```

| Part | Notes |
|---|---|
| TabRail (part) | A horizontal row of icon tabs on every placement, so it never reads as a second dock (the dock is a column of round targets on the page side). Section 5 |
| Header | Active tab title (`type/heading`, wraps to two lines) and Close (IconButton `quiet` `default`, `close`, `panel.toggle`). On `bottom` the title is the first line of the body and row 1 holds the handle |
| Back | Shown in the header only inside a pushed view, in place of the title's start: IconButton `quiet` `default`, `arrow_left`, Tooltip "Back, Escape" |
| Body | The tab's content, scrolling on its own. Pages, Layers rows, a Picker (Insert), settings (Tool settings), an [ExtensionPanelHost](extension-panel-host.md) |
| Handle | Bottom panel only: separator with a `size/hit/touch` target, drags between `peek` and `full` |
| Divider | Beside only: the resize edge, `size/hit/min` wide slot |

## 3. Variants and sizes

`placement`: `beside` | `over` | `bottom`, chosen by window class and never by the user.
`snap` (bottom only): `peek` | `full`.

| Class (SYSTEM-MAP 1) | `placement` | Size | Page |
|---|---|---|---|
| Expanded, 840 and up | `beside` | `size/panel/width`, resizable between `size/panel/min` and the lesser of `size/panel/max` and the window width less the dock less `size/page/min-width` | Page area shrinks and stays centred, never narrower than `size/page/min-width` |
| Medium, 600 to 839 | `over` | `size/panel/width-over` over the page edge | Page can be panned clear; no reflow |
| Compact, under 600 | `bottom` | `peek`: half the window height, reduced so the page keeps `size/page/min` high, but never below handle row, tab row and two `size/row/list` of body. `full`: to under the NoteBar row | Page above stays live; no scrim |
| Short (under 500 tall) and 600 wide or more | `over` | As medium, even on an expanded width | The panel overlays instead of taking width. A short compact window stays `bottom` |

When the window is so short that `peek` and the page minimum conflict, the body wins and the page
minimum drops to half of `size/page/min`.

Side: the edge away from the writing hand, after the dock on the same side (right-handed: left;
left-handed: right). The panel starts below the NoteBar row, which never moves it. Layer
`z/panel`, above chrome, below Sheets and popovers. Controls inside use the `default` role.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Closed | Not in the tree. The page uses the full width |
| Open | `surface/raised`, `border/subtle` hairline on the page side, `elevation/float` on `over` and `bottom` only. Flat on `beside` |
| Hover, Focus-visible, Active | On tabs, Back and Close, as IconButton |
| Selected tab | `selection/fill`, `-hover`, icon `action/toggle/fg-on`, a bar in `selection/indicator` under the tab, `border-width/emphasis` thick |
| Disabled tab | Never for an extension (it disappears when off, section 8). A built-in tab whose content cannot apply is replaced (Pages by Views), not disabled |
| Pushed view | Up to three levels below the tab root. Header shows Back and the pushed title. A tab switch keeps each tab's own stack and shows the tab's root first time; switching away and back returns to where it was |
| Resizing | Divider drag: `border/control-hover` line, live width; ends on release; remembered per device (`toolbar.panelWidth`) |
| Snapping | Handle release picks `peek` or `full`, `motion/duration/base`, `motion/easing/standard`. No overshoot |
| Loading | A tab body uses Skeleton after 300 ms, held 300 ms. No panel spinner |
| Empty | The tab body shows an EmptyState `quiet` |
| Error | An inline error with Retry in that tab's body only |
| Palm | While the pen is in proximity or within `delay/palm-window` of its last contact, a touch larger than the palm threshold is ignored on tabs, Back, Close, the handle, the divider and rows; a finger tap counts only if under `input/chrome-tap/duration` and `input/chrome-tap/travel`. Pen taps always count |
| Writing | The panel stays put and visible. It never animates, snaps or resizes while the pen is down; pen hover and press feedback are off |
| Drag | Reorder and drop are owned by the tab body (ReorderList). The tab row is not draggable |

## 5. Tab rail

Built-in tabs, always first, in this order: Pages (Views on Canvas), Layers, Insert, Tool settings.
Then extension panels in install order. Commands: `panel.pages`, `panel.layers`, `panel.insert`,
`panel.toolSettings` (chords from the registry), `panel.toggle`.

- The row shows up to six tabs, fewer when the width cannot hold them at `size/control/default` in
  `size/hit/touch` slots (capacity from width, as the dock does for slots). The rest go into the
  overflow control (IconButton `more`, Tooltip "More panels"), a Menu of the remaining tabs in the
  same order. At 360 wide that is four tabs and overflow.
- If the active tab is in the overflow, the overflow control shows the selected look. The row never
  reorders. Built-in tabs never go into overflow before an extension tab does.
- A tab is an icon with a Tooltip (name, shortcut; extensions "Name, from Extension"). The title
  always names the active tab, so no tab relies on its icon alone.
- Keys on the row: Left and Right (mirrored in right to left), Home and End.

## 6. Content rules

- **Long tab name** ("Werkzeugeinstellungen", "Verktoyinnstillinger"): the title wraps to two lines
  and grows; the row is icon only and never changes.
- **No extension panels:** four tabs. **One tab open:** fine; the row stays.
- **500 items in a tab:** the body virtualises; the panel owns no list.
- **Opening state:** the tab last used in this document session; first open is Pages (Tool
  settings if opened from the wheel's More).
- **Right to left:** text and the header mirror; the panel's side still follows handedness.

## 7. Overflow rule

Tabs: up to six in the row by capacity, the rest in the overflow Menu (SYSTEM-MAP 7 and 8). The panel
never scrolls as a whole; each tab body scrolls. On `bottom`, `full` is the last step. The on-screen
keyboard drops the panel to `peek` and hides the NoteBar.

## 8. Extension slots

Side panel tab (SYSTEM-MAP 7): the extension's icon and name as a tab after the built-in ones, the
body an [ExtensionPanelHost](extension-panel-host.md). Extensions cannot reorder, restyle, resize or
open the panel on their own. Banners from an extension exist only inside its own panel, as the
host's status area, never as a Banner component outside it.

**When the extension is turned off** (one rule for all chrome): its tab disappears. If it was the
open tab, the panel moves to Pages, focus goes to that tab, and nothing announces more than
"Flashcards turned off". Its tab position is remembered and returns when it is turned on again.

## 9. Pack content

None in the panel itself. Insert and Tool settings host a Picker, which owns pack states.

## 10. Input

- **Stylus:** tap tabs, Back, Close, the handle. Dragging the handle by pen works. No edge swipe
  opens the panel: edges belong to the system back gesture. Open it with the tab keys,
  `panel.toggle`, the wheel's More, or More, Insert in the NoteBar.
- **Touch:** the same, with the palm rule. Swipe down on the handle or header of `bottom` closes it.
  Long press on a tab shows its Tooltip.
- **Mouse:** click, drag the divider (4 px start), double click the divider to restore the default
  width. The scroll wheel scrolls the body.
- **Keyboard:** the tab row is one tab stop; Tab enters the body. F6 moves between landmarks and
  includes the panel. The divider is a separator: Left and Right resize by `space/2xl` steps, Home
  and End go to the limits. The handle: Up and Down snap.
- **Back and Escape** (SYSTEM-MAP 3): a pushed view in the panel closes first (step 5), then on
  compact the `bottom` panel (step 6). On `beside` and `over` Escape returns focus to the page and
  the panel closes with Close, `panel.toggle` or the tab toggle.
- **Focus:** opening a tab moves focus to the tab; closing returns it to what opened it. Focus never
  moves while the pen writes.

| Platform | Role |
|---|---|
| Web | `complementary` "Side panel, Pages tab"; the row `tablist` (horizontal); tabs `tab` "2 of 5"; body `tabpanel` |
| Compose | Container with `Role.Tab` items in a `selectableGroup`; handle with `stateDescription` |
| WinUI | Custom pane with a `TabView`-style `ListView`; `AutomationProperties.Name` "Side panel" |
| SwiftUI | `.accessibilityElement(children: .contain)`, tab items with the tab-bar traits |

## 11. Tokens

| Token | Use |
|---|---|
| `size/panel/width`, `width-over`, `min`, `max`, `size/page/min`, `size/page/min-width` | Widths and page minimums (proposed) |
| `size/control/default`, `size/hit/touch`, `size/hit/min`, `size/icon/default` | Tabs, Close, divider |
| `surface/raised`, `border/subtle`, `border/control-hover`, `elevation/float` | Panel, edge, resize line, overlay |
| `selection/fill`, `-hover`, `selection/indicator`, `action/toggle/fg-on`, `border-width/emphasis` | Selected tab |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Tabs, Back, Close |
| `space/sm`, `space/lg`, `space/2xl`, `radius/surface`, `type/heading` | Padding, resize step, shape, title |
| `delay/palm-window`, `input/chrome-tap/duration`, `input/chrome-tap/travel` | Palm rule |
| `motion/duration/base`, `motion/easing/standard`, `z/panel`, `component/focus/*` | Snap, layer, focus |

## 12. Do and Don't

**Do**
- Choose placement from the window class only, and keep the page live in all three.
- Put it on the off-hand side next to the dock, and mirror it with handedness.
- Put the tabs in a horizontal row inside the panel, so tools and panels never look alike.
- Give every pushed view a Back, and close it before the panel.

**Don't**
- Don't use a scrim or block the page; the panel is not a Sheet.
- Don't open it from an edge swipe, or let it reflow the page on `over`.
- Don't nest tabs inside a tab. Insert is one Picker with a source filter.
- Don't let a resting palm press a tab, Close or the handle.

## 13. Traces to

Flows: 03, 07, 09, 11, 12, 15. Replaces: Panel, Inspector, WheelPanel, the LayersPanel host and
CustomiseWheel's host. TabRail is a part of this component.
