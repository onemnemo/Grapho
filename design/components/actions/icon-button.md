# IconButton

## 1. Purpose

A square control with one icon and no visible label, for actions people already know or learn as a
set: close, more, undo, search, add page, the tools in a row. It always has a Tooltip and an
accessible name. It also serves as a two state icon toggle (Star, Pin).

**When not to use:**
- The single primary action of a Sheet or Dialog, a destructive action outside an overflow Menu, or
  anything a first run screen introduces: use Button with a label.
- A setting with a label in a row: use Toggle.
- A choice of tool in the Dock: use the Dock slot part, which is built on this component at
  `size/dock/slot`.
- More than four icon-only actions on the trailing side of a bar: the rest go into More.

## 2. Anatomy

```
   +----------+
   |    ( )   |   container: height role, equal width, radius/pill
   |   icon   |   icon: role size, currentColor
   |        o |   Dot: top end, only when attention = dot
   +----------+
   hit slot (at least size/hit/min square), container centred in it
   Tooltip: name and shortcut, anchored, outside the slot
```

Parts: container, icon, Dot (optional), busy layer, Tooltip (required, supplied by the shell).

## 3. Variants and sizes

`variant`: `quiet` | `solid` | `glass`.
`size`: `small` | `default` | `large`, which resolve to `size/control/small`, `default`, `large`; the
container is square at that value.
`attention`: `none` | `dot`.
`command`: the command id it runs (required for an app action, see section 9).
`role`: `action` | `toggle`. A `toggle` is pressed or not (`aria-pressed`). `glass` is `action` only.

| Variant | Where | Look |
|---|---|---|
| `quiet` | Bars, rows, panels: the default | No fill at rest, `action/quiet/fg` icon |
| `solid` | Two places only: a lone icon action in an empty area, and items of a floating bar over the page | `action/secondary/bg` fill, `action/secondary/fg` icon |
| `glass` | Round buttons over artwork: the item ••• on a tile, page number badges | `media/glass` fill, `media/text` icon, nowhere else |

| Size | Used for |
|---|---|
| `small` | Note bar and Selection bar items, inspector rows |
| `default` | Top bar, panels, Sheet headers, tile corners |
| `large` | Rare: a lone icon action in an empty state. Never inside a bar |

Icon size is the role icon size. A Sheet header close button is `default` in the header.

## 4. States

| State | Fill | Content | Other |
|---|---|---|---|
| Default | `quiet`: none; `solid`: `action/secondary/bg`; `glass`: `media/glass` | `action/quiet/fg`, `action/secondary/fg`, `media/text` | |
| Hover | `quiet`: `action/quiet/bg-hover`; `solid`: `action/secondary/bg-hover`; `glass`: no change | same | Opens the Tooltip after 500 ms |
| Focus-visible | same | same | README focus ring; Tooltip on keyboard focus |
| Active (pressed) | `action/quiet/bg-pressed`, `action/secondary/bg-pressed`; `glass`: no fill change | same | No scale. `glass` shows the press with a `media/edge` ring at `border-width/emphasis` (a glass pressed token is a gap, logged in the README) |
| Selected (`role` `toggle`, on) | `selection/fill`, hover `selection/fill-hover` | icon `action/toggle/fg-on` | Indicator: a short bar in `selection/indicator` under the icon, `border-width/emphasis` thick, plus the registry's on form (`fill` or `swap` for star, pin, bookmark). No outline. Never fill alone |
| Disabled | none (`quiet`), `state/disabled/bg` (`solid`) | `state/disabled/fg` | Stays focusable when it holds information; the reason is the description ("Nothing to undo") and the Tooltip adds it |
| Loading | rest fill | icon hidden, spinner centred | README loading recipe |
| Empty | n/a | | An IconButton always has an icon. A missing icon draws a neutral placeholder of the right size |
| Error | n/a | | |
| Drag | n/a | | Not draggable. A Dock slot is reorderable, a separate part |

**Dot** (`attention` `dot`): a filled circle of `size/dot` diameter (token needed, a gap) at the top end of the icon, fill
`state/info/fg`, ringed by a `border-width/emphasis` gap in the colour of the surface the control sits on (the Dot does not take the hover or pressed fill). Colour is not
the only cue: the Tooltip and accessible description add "Updates available". It appears only for
updates on Packs and Extensions (SYSTEM-MAP 4.3).

## 5. Content rules

- Name (the Tooltip and accessible name, same words): the command's `labelKey`, sentence case. The
  Tooltip content is `{name, shortcut, source}` read from the registry at show time: the effective
  shortcut for this platform and context after remapping, in the registry's text form ("Undo, Ctrl+Z"),
  and for an extension "from Extension name". Long names (German) wrap in the Tooltip to a
  maximum width and never truncate the accessible name.
- No text on the control. A count does not go on an IconButton; a count lives in a Chip, a Button
  label or the trailing end of a ListRow.
- One item: a lone IconButton is fine in a Sheet header (Close).
- Five hundred items: n/a; see the overflow rule.

## 6. Overflow rule

A bar shows at most four icon-only actions on its trailing side (branding rule and icons README).
Beyond that, items go into a "More" IconButton (`more` icon, command `bar.more`, with `aria-haspopup="menu"` and expanded state) that opens a Menu, in priority order.
The Note bar holds at most four pinned items, two of them extension items. The Selection bar holds
four and More. Overflow is never a shrink; the control sizes stay.

## 7. Extension slots

Note bar item (SYSTEM-MAP 7): an extension's command appears as an IconButton the same size and
variant as its siblings, with the extension's namespaced icon (`ext.<package>.<name>`) and its
name in the Tooltip, in the form "Name, from Extension name". The user pins at most two. Extension
items in the Selection bar and item Menu are always in More. An extension never sets the variant,
size or Dot.

## 8. Pack content

None directly. If a pack's icon cannot load, the neutral placeholder icon of the same size
draws, and the Tooltip still names the command. If the extension that contributed an item is
turned off or removed, the item leaves the bar and a row in More reads "Name, turned off" and is
disabled with the reason as its description; it returns when the extension does.

## 9. Input

- **Stylus:** hover (S Pen, Pencil) shows the hover fill and, after 500 ms, the Tooltip. Tap
  activates on lift. Never reacts while the pen writes on the page. A bar item in the edge guard
  is inset by 16 dp on the writing hand side, not shrunk.
- **Touch:** a short tap activates; a disabled control with a reason shows the reason hint (README). A long press shows the Tooltip and does not activate (4.7.1),
  held for 1.5 s after release. Controls with their own long press (Dock slots) keep theirs.
- **Mouse:** hover fill, Tooltip at 500 ms, click on release.
**Command ids.** Undo and Redo are `doc.undo` and `doc.redo` (`library.undo` and `library.redo` in the library); page arrows `nav.prevPage` and `nav.nextPage`; Add page `page.add`; Star `library.star` (toggle); Search `app.search`; side panel `panel.toggle`; More `bar.more`; Selection bar More `selection.more`. The last two are proposed. Close in a Sheet header is `studio.cancel` in Studio and a surface dismiss elsewhere.

- **Keyboard:** Tab, or Left and Right inside a bar (one tab stop for the bar). Enter and Space
  activate; a toggle flips. Tooltip shows on keyboard focus. Escape hides the Tooltip.

| Platform | Role |
|---|---|
| Web | `button`, with `aria-pressed` for `toggle`, `aria-label` = name |
| Compose | `Role.Button`, with `toggleable` for a toggle, and `contentDescription` |
| WinUI | `Button` or `ToggleButton`, `AutomationProperties.Name` and `ToolTipService` |
| SwiftUI | `Button` or `Toggle` with a custom style, `accessibilityLabel` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `large` | Container |
| `size/hit/min` | Hit slot |
| `size/icon/small`, `default`, `large` | Icon |
| `radius/pill` | Container |
| `action/quiet/fg`, `bg-hover`, `bg-pressed` | Quiet |
| `action/secondary/bg`, `fg`, `bg-hover`, `bg-pressed` | Solid |
| `media/glass`, `media/text`, `media/edge` | Glass, and its pressed ring |
| `selection/fill`, `selection/fill-hover`, `selection/indicator` | Toggle on |
| `action/toggle/fg-on` | Icon of a toggled-on control |
| `border-width/emphasis` | Indicator, Dot gap |
| `state/disabled/bg`, `state/disabled/fg` | Disabled |
| `state/info/fg` | Dot |
| `size/dot` | Dot diameter |
| `component/focus/*` | Focus ring |
| `motion/duration/press`, `fast`, `base`, `slow` | Transitions, loading |

## 11. Do and Don't

**Do**
- Give every IconButton a Tooltip with its name and shortcut, and an accessible name with the same words. No exceptions: clear, reveal, close, dismiss and remove included.
- Name its command id; a control that only edits a value or dismisses a surface is not a command but still gets the Tooltip.
- Show a toggle's state with the selection fill, `action/toggle/fg-on` on it, the indicator bar and the on form of the icon.
- Keep the size equal to its siblings in a bar.
- Use `glass` for round buttons over artwork and nowhere else.

**Don't**
- Don't tint the icon with the accent; the only change is `action/toggle/fg-on` on `selection/fill`.
- Don't put a count, a label or a chevron next to the icon.
- Don't use an IconButton for the primary action of a Sheet or Dialog.
- Don't use the Dot for anything but "updates available".
- Don't make a hit area smaller than the slot to fit a tight bar; drop an item into More.

## 12. Traces to

Flows: 05 organize-folders (item More, Select), 07 customize-appearance (undo, reset, close
panels), 11 navigate-long-document (page arrows, scrubber ends), 15 add-to-page (Insert, layer row
actions).

Replaces: IconButton, CloseButton (an IconButton with the `close` icon). The Dot (a part in COMPONENTS.md) is
`attention` `dot`.
