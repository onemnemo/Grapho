# 03 Write, switch tools, set up tools

## Goal

Keep writing while changing pen, colour, width, eraser, lasso and undo, with a cost that is honest
for every input, and set the tools up once so the lecture needs few switches. The page belongs to
the pen (principle 1).

Counting rule: see 01. Counts assume the tools are already set up unless marked "first time".

## Entry points

A document is open. Surfaces: `ToolDock` (never recedes; quick slots are its parts), `NoteBar`
(recedes), `ToolWheel`, `SidePanel` Tool settings tab, `Tooltip`.

## The dock

Right-handed: a vertical strip on the left edge, top to bottom: current tool (a real sample), slots
1 to 5, Undo, Redo. Compact portrait: a bottom strip in the same order. There is no dock menu.
Everything a dock menu would hold lives elsewhere: Previous tool is the wheel's centre and key Q;
slot pin, unpin and replace are the slot's Menu (long press then lift, SYSTEM-MAP 4.7.1); handedness
and the slot count are in Settings and the Tool settings tab.

At 360 dp wide the strip holds Tool, Undo, Redo plus as many slots as fit at 44 dp (3 at 360, 5 from
about 480); pinned slots are never dropped.

## Quick slots: stable positions

- Pinned slots keep fixed positions, in the order the user set. Factory pins: slot 1 Pen, slot 2
  Eraser, slot 3 Highlighter.
- Unpinned slots hold recents. A new recent replaces the least recently used recent slot in place;
  nothing else moves. A slot is a bundle: tool, pen, colour, width.
- Number keys 1 to 5 map to positions, so they are stable. Nothing reorders while the pen is down.

## Switch table, by input (cost to switch and return)

| Job | S Pen with button | Plain pen, Apple Pencil, finger | Mouse | Keyboard |
|---|---|---|---|---|
| Erase, then back | hold button: 0 taps | tap Eraser slot (1), tap Pen slot (1) = 2 | click both slots = 2 | E then Q = 2 keys, or hold X |
| New colour, same pen | click button, tap colour on the wheel = 2 | tap tool, tap colour = 2 | same = 2 | T, arrows, Enter = keys |
| Colour already in a slot | tap slot = 1 | 1 | 1 | 1 to 5 |
| Width | click button, drag width = 2 | tap tool, drag width = 2 | same | in Tool settings |
| Switch pen type | click, tap pen = 2 | 2 | 2 | T, arrows |
| Lasso, recolour, back to pen | Lasso slot, swatch button, swatch, Pen slot = 4 | 4 | 4 | L, ..., Q |
| Undo | 1 | 1 (two-finger tap in pen-only mode) | 1 | Ctrl+Z |
| Add a page | bar visible: More, Add page = 2; mid-writing +1 reveal = 3 | same | same | Ctrl+Enter |

First-time setup: pin a slot = 2 to set the bundle plus 2 to pin (long press, Pin) = 4 taps per slot; the
three factory pins need none.

## The wheel (hover tracking)

The wheel opens at the pen tip on a button click, or beside the dock on a tool tap. While open the
highlight tracks the pen's hover; a tap on a segment chooses it, and with a finger a press, drag and lift chooses it too. The hovered segment's name shows in the hub label. Moving out of range,
clicking the button again, tapping outside or Escape closes without a change. Rings: Tools (eight),
Pens, Colours. The centre holds the pens; width and opacity are edited in the selected pen's segment. Previous tool is `tool.previous` (Q, or a double tap on the dock's current tool). A mouse hovers and clicks the same.
One Highlighter tool has a "Snap to text" option, on by default when the PDF has text (04).

## Set up my tools (first time)

Entry: wheel More, or Side panel Tool settings tab (Ctrl+Shift+4), or `grapho://settings/pen`.

| # | Step | Components |
|---|---|---|
| 1 | Tool settings tab shows the wheel preview and the slot row | `SidePanel`, `ToolWheel` preview, `ToolDock` slots |
| 2 | Reorder the Tools ring by drag; every drag has a "Move earlier, Move later" Menu row | `ReorderList` |
| 3 | Add a pen to the Pens ring: the Pens list is the Tool settings tab's root (a `Picker` in manage mode); Add is in its footer and is not a pushed view | `Picker`, `PickerTile` |
| 4 | Remove or hide a tool: row Menu, Remove from wheel; an extension tool shows its name and a `Toggle` | `ListRow`, `Toggle` |
| 5 | Assign a slot: slot Menu, "Replace with current tool", or Pin | `Menu` |
| 6 | Colour sets: the colour ring's Sets segment opens the colour set `Picker` in the side panel, edit in `ColourEditor` with `Slider` and hex `TextField`; at most 24 colours per set | `Picker`, `ColourEditor` |
| 7 | Slot count: `Stepper` "Quick slots shown", 0 to 5 | `Stepper` |
| 8 | Pen smoothing, pressure, width: `Slider` rows. Pen-only, palm guard and gestures: "More in Settings" | `SettingRow` |

Changes apply at once and sync. The panel stays beside the page; on compact it is the bottom panel
at its Peek height, which keeps at least 200 dp of page.

## Document menu (More): the whole list

Commands only, grouped, at most 10 rows: Find (Ctrl+F); Add page, Insert pages, Page setup; Export,
Share; an Extensions group (at most five, then "More from extensions"); Command palette; "Settings"
(opens the Settings place). Rename is the title; Star and Move are library commands. Settings
such as pen-only and dark paper are in Tool settings and Settings, not in the menu.

## Note bar recall and accessibility of the bar

Receded means removed from the visual tree with a visible 12 dp handle (`NoteBarHandle`) at the top
edge. Recall by: a top-edge tap, the handle, 1.5 s without pen contact, the "Show note bar"
command (F6, palette), or any keyboard focus into the bar. Focus never lands on a hidden bar.

## Compact vertical budget (360 by 480)

| Open | Hides | Page left |
|---|---|---|
| Note bar 48 + dock 56 | nothing | 376 |
| Side panel at Peek | Note bar | at least 200 |
| Find bar 48 | Note bar | 376 - 48 |
| On-screen keyboard (hex field) | Note bar, panel drops to Peek | the field and page strip scroll into view |

A short window (under 500 tall) gets a horizontal dock whatever its width (see Open questions in COMPONENTS.md).

## States

| State | What the user sees |
|---|---|
| Idle | Bar visible, dock at full contrast |
| Writing | Bar receded in 150 ms with no reflow; dock static but live to taps |
| Undo or Redo unavailable | Dimmed with a non-colour cue; never hidden |
| Palm | Touches ignored while the pen is in proximity or within 1 s; the 16 dp writing-hand edge opens no chrome |
| Pen-only off | Fingers draw; finger-tap Undo and Redo off |
| No S Pen | Button gestures inert; slots, wheel taps and keys do everything |
| Very large page | Progressive render with placeholders (13); the dock and wheel respond while ink loads |
| Error: autosave fails | A `Banner` on the document "Changes are not saved. There is not enough space." with a link to storage |
| Offline | Not applicable |

## Exits

Wheel: choice applies and closes; Escape or tap outside closes, no change, tap not passed to the
page. Side panel: close button, Escape. Back leaves the document to where it came from.

## Wireframes

Expanded, right-handed (the page is writing; bar receded; toast bottom left):

```
+------------------------------------------------------------+
|  ===== (handle)                                            |
+----+-------------------------------------------------------+
|[~] |                                                       |
|S1* |            ####### ink ########                       |
|S2* |            ####### ink ########                       |
|S3* |                          [ 150% ] (top centre, 1.5 s)  |
|S4  |                                                       |
|Un  |                                                       |
|Re  |                                                       |
+----+-------------------------------------------------------+
 [~] current tool, tap = wheel;  * pinned slots
```

Wheel (flattened rings):

```
              Colours   ( o o o o o o o o )
            Pens        ( fine  brush  fountain  > )
        Tools           ( pen  hl  erase  lasso  text )
                        [ More ]
```

Compact portrait, left-handed (dock bottom strip, right end; toast right):

```
+----------------------------------+
| < Week 4     12 of 40        ... |
|              ####                |
+----------------------------------+
|     [Tool][S1][S2][S3][Un][Re]   |
|                                T |
+----------------------------------+
```

Long-string check: "Rückgängig" on Undo is an icon with a Tooltip, so the dock width never
changes with the language.

## Access

Keyboard (canvas focused, no text edited): T wheel, P/H/E/L/Y tools, Q previous, 1 to 5 slots,
Ctrl+Z. Wheel keyboard mode: arrows move within a ring and between rings, Enter picks and stays,
Space picks and closes, Escape closes; a visible focus ring marks the segment. Screen reader: the
wheel is a menu with groups Tools, Pens, Colours and a selected state; the Tool settings tab is the
same list; slots read "Slot 2, Eraser, pinned". Focus returns to the dock tool when the wheel
closes. Left-handed: dock, panel, scrubber and toasts mirror to the right; the wheel's rings fan
away from the writing hand (they open to the off-hand side of the pen tip).
