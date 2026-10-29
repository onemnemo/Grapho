# Menu

## 1. Purpose

Commands for a trigger or a target. Every row is a command from the registry (SYSTEM-MAP 4.1) and reads its
label, icon, shortcut text, enabled state, disabled reason and destructive flag from it. One component covers
the overflow menus, the folder, new, sort, selection and insert menus, the item menu, the quick slot menu
and the context menu. They differ only in their command list and in where they open. Light modality: no
scrim; a finger or mouse tap outside closes it and is not passed through; a pen-down on the page outside
closes it and the stroke is drawn (SYSTEM-MAP 3).

**When not to use**
- A checklist, a filter, or a choice that should stay open while it is adjusted: **Popover** (Filter is a
  Popover only, never a Menu).
- A value that belongs to a field (language, folder, sort order shown in a form): **Select**.
- Finding a command by name: **CommandPalette**.
- A task with Done or Cancel, or a second level of options ("Export as > PDF > Options"): a **Sheet**.
- A control that is not a command (Slider, Toggle, text field): never in a Menu.

## 2. Anatomy

```
 [ ••• ]                       trigger (IconButton, or Button with a chevron)
 +-----------------------------------+
 | Edit                              |  heading (sticky when the menu scrolls)
 | [ic] Rename                  F2   |  row: lead (always reserved), label, shortcut, trail
 | [ic] Pen only                [v]  |  toggle row: trail check when on
 | [ic] Name                    [v]  |  radio row: trail check on the chosen one
 | [ic] Sort by                  >   |  submenu row: trail chevron_right
 | [ic] More in Settings         ^   |  link row: trail external arrow
 |-----------------------------------|
 | Notes Plus                        |  heading, extension rows
 | [ic] Export as Markdown  Notes Plus|  every extension row names its extension
 | [ic] More from extensions     >   |
 |-----------------------------------|
 | [ic] Delete                       |  destructive group, last, danger label
 |-----------------------------------|
 | Select something first            |  reason line (pinned, for the focused disabled row)
 +-----------------------------------+
```

Parts: `surface`, `heading`, `row` (`lead`, `label`, `shortcut`, `source`, `trail`), `divider`, `reason`,
`submenu` (a pushed view, see 6). The lead slot is always reserved so labels share one start edge; a row
without an icon leaves it empty. A radio or toggle row keeps its own icon and draws the mark in `trail`.
Shortcut text is the registry chord for the platform, shown only after a hardware keyboard or a mouse has
been seen this session (never on a keyboardless tablet). A row never holds an IconButton, Toggle or input.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `opens` | `anchored` \| `at-pointer` |
| `rowKind` (derived per row, not set by hand) | `command` \| `toggle` \| `radio` \| `submenu` \| `link` |

`rowKind` comes from the registry: invocation mode `toggle` gives a toggle row; a short fixed value list
(sort order, up to six values) gives radio rows; "More in Settings" gives a link row (SYSTEM-MAP 4.5), which
every Menu showing a setting carries as its last row. `at-pointer` is for mouse right click on an item or
the page chrome. The Menu key and Shift+F10 open `anchored` to the focused item. Pen and touch reach an
item's Menu by its ••• button; a quick slot by long press then lift (4.7.1); neither uses `at-pointer`.

One size. Row height is `size/row/menu` at the density in force, which becomes touch density after touch or
pen input (the row never goes below `size/hit/min`). Width is content between `size/menu/min-width` and
`size/menu/max-width`, and never more than the window minus `space/lg` each side.

## 4. States

| State | Tokens that change |
|---|---|
| Surface | `surface/overlay`, `border/subtle`, `elevation/popover`, `radius/surface` |
| Row rest | `text/default`; shortcut and source `text/muted` |
| Row hover | `hover/fill` |
| Row focus-visible | the actions focus recipe (`component/focus/gap/*`, `component/focus/ring/*`), drawn inside the surface padding; the keyboard highlight also uses `hover/fill` |
| Row active | `press/fill`; runs on release for pen and touch, on press for mouse |
| Row selected (toggle on, radio chosen) | trail check in `selection/indicator`; no row fill, because a Menu is not a selection mode |
| Row disabled | `state/disabled/fg`; row height does not change. The reason (`type/caption`, `text/muted`) shows in the pinned `reason` line for the hovered or focused row and is the accessible description |
| Row destructive | label and icon `state/danger/fg`; the icon shape (trash, minus) is the non-colour cue |
| Empty | one line "Nothing here" in `text/muted`; a built-in Menu with no valid command is a lint error |
| Error | a failing command closes the Menu and reports by Toast |
| Loading, drag | not used; a Menu closes when the command starts |

Open and close fade over `motion/duration/fast`. Reduced motion: none. Forced colours: system menu
colours. More contrast: surface edge `border/strong`.

## 5. Content rules

- Labels are verb first and short ("Rename", "Move to folder"), from the registry only. A link row ends with
  the external icon, not dots.
- Very long text (German and Norwegian): the label wraps to at most two lines, the shortcut stays on the
  first line at the end edge, and the shortcut column is dropped before the label wraps a third time. The
  ceiling is two lines; a longer label is cut by the registry's own length lint, never by the Menu.
- No text: a row without a label is a lint error. A heading with no rows is not drawn.
- 1 row: allowed in a context menu. 30 commands: a built-in Menu above 15 rows is a defect and the extra
  commands go to the palette; extension rows are the only legitimate source of more (see 6).

## 6. Overflow rule

A Menu grows in this order and no other:

1. **Moved items.** In the note bar and top bar More, items that did not fit form the first group, in
   command priority order.
2. **Groups with headings.** When rows belong to two or more registry groups (edit, view, insert, page,
   file, tools, library, go) they are grouped with a heading and a divider; one group has none. Group order
   is fixed by the registry, rows inside follow priority, never use.
3. **Extension group.** Last, before the destructive group. It shows **at most five rows**. With six or more
   extension commands it shows four commands and a fifth row "More from extensions". The item menu shows at
   most three rows (two commands and the More row), per SYSTEM-MAP 7. Selection bar More and every other
   Menu that accepts extension commands follows the five row cap; the menus that accept them are the item
   menu, Selection bar More, Note bar More, Insert and the library New menu. "More from extensions" lists
   the rest.
4. **Submenus.** One level. They are for a related set that would need a second heading (Sort by, Export
   as). A submenu opens as a **pushed view** in the same surface with a Back row (`chevron_left`, mirrored
   in right to left) and the parent's row as its title. Back or Escape returns one level before closing. A
   third level is a Sheet.
5. **Scroll.** Past `size/popover/max-height`, or the window height minus `space/xl`, the Menu scrolls:
   headings stick, an edge fade shows more content, and the focused row stays fully visible.

Destructive rows (registry flag) form the last group with a divider, under a menu's other content.

## 7. Extension slots

Command slot (SYSTEM-MAP 7), per 6. Every extension row shows its extension's name as a trailing `source`
caption (type/caption, `text/muted`), and the heading is the extension's name when one extension is shown
and "Extensions" when several are. The registry rejects an extension label equal to a built-in label at
install. Extension rows look like built-ins otherwise. An extension adds command rows only. When an
extension is turned off its rows are hidden, not disabled, and come back in the same place when it is turned
on (the registry reason `extension_off` serves placed items such as a pinned note bar item, not menu rows).

## 8. Pack content

None. No pack content and no pack state. "Theme" opens the Picker.

## 9. Input

- **Stylus.** Tap to open and choose. Pen hover highlights a row where hover is reported. A pen-down on the
  page outside closes the Menu and draws the stroke.
- **Touch.** Rows are touch height. A finger tap outside closes and is not passed through. While the pen is
  in proximity or within 1 s of its last contact, touches larger than the palm threshold are ignored and
  never close a Menu. There is no slide-to-choose; long press follows 4.7.1 only.
- **Mouse.** Hover highlights; a submenu pushes on click or Right, never on hover alone, so a moving pointer
  cannot trigger it. Right click on an item opens `at-pointer`.
- **Keyboard.** Up and Down move through every row, **including disabled rows** (they carry `aria-disabled`
  and Enter on one re-announces the reason and runs nothing); Home and End. Right opens a submenu and Left
  goes back, mirrored in right to left (the arrow toward the submenu opens it). Enter runs, and every run
  closes the Menu. Escape goes back one level then closes, and returns focus to the trigger. Tab closes and
  moves on.
- **Type-ahead.** Printable characters jump to the next enabled row whose label starts with the typed text.
  Characters within `feedback/delay/typeahead` join one prefix; a pause resets it; the same letter again
  cycles. **Space is a character while the prefix is not empty; it runs a row only when the prefix is
  empty.** Matching is locale aware and keeps å, æ and ø distinct. Headings and disabled rows are skipped by
  the jump; the row jumped to is announced.
- **Placement.** Anchored, in this order: choose the side away from the writing hand relative to the contact
  point (the hover or press position, else the anchor), preferring above that point when the contact is in
  the lower half, then the inline end, then the remaining side; shrink and scroll last. It never covers its
  trigger or the Selection bar it came from, and keeps `space/md` inside the window. `at-pointer` opens at
  the pointer and flips on either axis.

  | Hand | Language direction | Horizontal preference |
  |---|---|---|
  | right | left to right | start to end side if away from the hand, else toward the page (leftward) |
  | right | right to left | inline end (leftward), which is also away from the hand |
  | left | left to right | inline end (rightward), which is away from the hand |
  | left | right to left | toward the page (rightward) if the anchor is near the hand side, else inline end |

  Rule behind the table: handedness decides the physical side, direction decides the tie. The compact
  window keeps this anchored placement with scroll; there is no bottom list.
- **Right to left.** The shortcut and trail sides mirror, directional icons use the mirror flag, and the
  arrow keys follow the submenu side.
- **Roles.** ARIA `menu`, `menuitem`, `menuitemcheckbox`, `menuitemradio`, labelled `group`; trigger
  `aria-haspopup="menu"` and `aria-expanded`. Native names (Compose `DropdownMenu`, WinUI `MenuFlyout`,
  SwiftUI `Menu`) are visual reference only: headings, pushed submenus, a reason line and type-ahead rules are
  not available in them, so the Menu is a custom control on all shells that implements the platform
  accessibility itself: UIA `Menu`, `MenuItem`, `ExpandCollapse`, `Toggle` and `SelectionItem` patterns on
  Windows; `AXMenu`, `AXMenuItem` and `AXMenuItemMarkChar` on Apple; `Role` plus `stateDescription` and
  `CollectionInfo` on Compose. Announce: trigger name, row count, "submenu" on a pushed level, the reason
  on a disabled row, the state on a toggle or radio row.
- **Stack.** Closes when anything else opens; a row that opens a Sheet closes the Menu first. Back and
  Escape close it (its pushed view first) before anything else.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `border/subtle`, `border/strong`, `elevation/popover`, `radius/surface` | surface |
| `size/row/menu`, `size/hit/min`, `size/menu/min-width`, `size/menu/max-width`, `size/popover/max-height` | size |
| `space/sm`, `space/md`, `space/lg`, `space/xl`, `radius/item` | padding and rows |
| `text/default`, `text/muted`, `state/disabled/fg`, `state/danger/fg`, `type/label`, `type/caption` | text |
| `hover/fill`, `press/fill`, `selection/indicator`, `component/focus/*` | states |
| `size/icon/default`, `size/icon/small` | lead and trail icons |
| `feedback/delay/typeahead`, `motion/duration/fast`, `motion/duration/base` | timing |
| `z/popover` | layer |

New tokens and pairs are in the README under Tokens to add.

## 11. Do and Don't

Do
- Take every row from the registry, reasons and destructive flag included, and keep row height constant.
- Cap extensions at five rows, name the extension on each row, and push a submenu with Back.
- Close the Menu on every run; use a Popover for anything that stays open.

Don't
- Put controls in a row, nest a submenu in a submenu, or add a bottom list.
- Run a row on Space while the user is typing a prefix.
- Hide a disabled row from arrow keys, or show its reason by growing the row.

## 12. Traces to

Flows 03, 04, 05, 07, 12, 13. Replaces `Menu`, `MenuItem`, `OverflowMenu`, `FolderMenu`, `NewMenu`,
`SortMenu`, `SelectionMenu`, `InsertMenu`. `FilterMenu` becomes a Popover.
