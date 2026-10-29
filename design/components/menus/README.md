# Menus and popovers

Members: [Menu](menu.md) (commands; context menus are the same component), [Popover](popover.md) (a small
choice that applies at once, one pushed view, the first-use hint), [CommandPalette](command-palette.md)
(find a command by name). All three are light surfaces: no scrim, only one visible at a time, above Sheets
and Dialogs (`z/popover`, the palette at `z/palette`).

## Shared dismissal and input rules

- A finger or mouse tap outside closes and is not passed through. A **pen-down on the page outside closes
  and the stroke is drawn**.
- While the pen is in proximity or within 1 s of its last contact, touches larger than the palm threshold are
  ignored and never dismiss anything.
- Back and Escape close the topmost surface; a pushed view (submenu, Popover view, palette argument) closes
  first. The palette clears its query before closing.
- Placement: away from the writing hand relative to the contact point, then toward the inline end, then the
  remaining side, then shrink and scroll; never over the anchor; `space/md` inside the window. Everything
  mirrors in right to left; directional icons use the mirror flag.
- The hint is the one surface not light dismissed.
- Native control names (Compose, WinUI, SwiftUI) are visual reference only. All three are custom controls
  that implement platform accessibility.

## How they line up

| Property | Menu | Popover | CommandPalette |
|---|---|---|---|
| Surface | `surface/overlay`, `border/subtle` | same | same |
| Elevation, radius | `elevation/popover`, `radius/surface` | same | same |
| Width | `size/menu/min-width` to `size/menu/max-width` | `size/popover/width`, `size/popover/width-narrow`, hint `size/tooltip/max-width` | `size/palette/width` |
| Max height | `size/popover/max-height` | same | `size/palette/max-height` |
| Row height | `size/row/menu`, never below `size/hit/min` | controls' own | `size/row/menu`; field `size/control/default` |
| Padding | `space/sm` vertical, `space/md` sides | `space/md` | `space/md` |
| Row radius | `radius/item` | `radius/item` | `radius/item` |
| Icon size | `size/icon/default` lead, `small` trail | by control role | `size/icon/default` |
| Label, caption | `type/label`, `type/caption` | `type/label` (hint too), `type/body` | `type/label`, `type/caption` |
| Hover, press | `hover/fill`, `press/fill` | controls' own | same as Menu |
| Dismiss | run, outside, Escape | outside, Escape (hint: Got it, Escape hides unseen) | run, Escape, outside, Ctrl+K |

## Decisions to log

1. Menu grows in order: moved items, groups (two or more registry groups), extension group (at most five
   rows: four plus "More from extensions"; item menu three), one-level pushed submenu, scroll with sticky
   headings. A built-in Menu above 15 rows is a defect.
2. Menu rows are command, toggle, radio, submenu or link, derived from the registry. Every run closes the
   Menu. Filter and checklists are Popovers only. Radio rows are for short fixed value lists.
3. Disabled rows keep their height; the reason is a pinned line and the description. Arrow keys reach them.
4. Type-ahead: prefix, locale aware, Space is a character mid-prefix. Slide-to-choose and `at-point` by
   pen are dropped; `at-pointer` is mouse only.
5. No compact bottom list. Compact keeps the anchored placement with scroll.
6. Popover: two widths, one pushed view, Back with a Tooltip, Retry on error, Select all and Clear row, no
   keyboard raise by touch. One job lint: more than four controls or more than one Slider.
7. Hint: Seen only on Got it or the first stroke; Escape hides it unseen; no outside dismiss, no focus
   steal, `role="status"`.
8. Palette: SearchField scope `filter`; caption on every row; recency Recent; true count with Show all;
   keywords; argument view reserved; `modal_open` disabled reason; no `>` mode.
9. Extension rows are hidden when off and show their extension's name; the registry rejects label
   collisions with built-ins.
10. Escape clearing the palette query first is an exception to the SYSTEM-MAP order.

## Tokens to add

| Token | Proposed value | Why |
|---|---|---|
| `size/menu/min-width` | 200 | short menus stay readable |
| `size/menu/max-width` | 320 | fits German labels at two lines on a 600 dp window |
| `size/palette/width` | 560 | about 60 characters of command name |
| `size/palette/max-height` | 480 | same as `size/popover/max-height` |
| `size/popover/width-narrow` | 280 | single control Popovers; equals `size/tooltip/max-width` |
| `feedback/delay/typeahead` | 800 ms | prefix reset |
| `motion/duration/base` | exists; replaces `motion/duration/normal` | pushed view slide |
| `size/row/menu` at Android touch | 48 | meets `size/hit/min` without a second token |
| Pairs | `selection/indicator` on `hover/fill` and `press/fill` over `surface/overlay`; `state/danger/fg` on `hover/fill` and `press/fill` over `surface/overlay` | keyboard highlight on a chosen or destructive row |

Commands and reasons to register: disabled reason `modal_open`; a registry `destructive` flag; per-locale
`keywords`; a label collision check at install. Icons: `chevron_left`, `chevron_right` (mirrored).
