# NoteBar

## 1. Purpose

The document's chrome: back, title, page indicator, pinned items and More. It is the only chrome
that recedes, so the page is free while the pen is down. It is the same in every document kind
(Notebook, PDF, Canvas); a kind may add or hide commands, never move a part.

**When not to use:**
- Undo, Redo and the tools: the ToolDock (never recedes, [tool-dock.md](tool-dock.md)).
- Anything that acts on a selection: SelectionBar.
- Search inside the document: FindBar, which sits beside the bar and does not recede.
- Library places, Settings, Search results: TopBar.

## 2. Anatomy

```
Full (expanded, medium)
+------------------------------------------------------------------------------+
| [<] [ Title ................ ] [ 12 of 300 ]   [p1][p2][p3][p4]   [ ... More ] |
+------------------------------------------------------------------------------+
  back  title (rename in place)   page indicator   pinned items     More

Core (compact, or text at 200 percent)
+----------------------------------------+
| [<] [ Title ...... ] [ 12 of 300 ] [..] |
+----------------------------------------+

Receded (pen down): the bar is gone from the tree; the NoteBarHandle remains
          [ v ]   top centre, see note-bar-handle.md
```

| Part | What it is |
|---|---|
| Back | IconButton `quiet` `small`, `arrow_left`, command `doc.close`. Leaves to the place it came from, scrolled to the item. Mirrors in right to left |
| Title | A text target, `type/label`. Tap renames in place (inline TextField), command `doc.rename`. Ellipsis when long; the full title is in the Tooltip and the accessible name |
| Page indicator | A Button `text` `small`: "12 of 300", opens the Go to page Popover, command `nav.goToPage` (Ctrl+Alt+G). On Canvas it is an IconButton toggle `map`, command `view.minimap`. Hidden when a document has one page |
| Pinned items | 0 to 4 IconButtons `quiet` `small`, at most two from extensions. Curated in Settings, Toolbar (`toolbar.pinned`) |
| More | IconButton `quiet` `small`, `more`, command `bar.more`, opens the document Menu |
| Container | A row of `size/bar/note`, laid out in the position the user chose |

## 3. Variants and sizes

`position`: `floating` | `line`. Setting `toolbar.position`, default `floating`.
`layout`: `full` | `core`. Derived from width, never set by hand.
Height is `size/bar/note` in both; every item is role `small`.

| Position | Look | Layout effect |
|---|---|---|
| `floating` | Rounded `radius/surface` on `surface/raised`, `border/subtle` hairline, `elevation/float`, inset `space/sm` from the top and sides, centred, at most `size/bar/note-max` wide. Layer `z/floating-bar` | Overlays the page. Nothing reflows |
| `line` | Full width on `surface/raised`, a `border/subtle` hairline under it. Layer `z/chrome` | Reserves its row. When receded the row stays, empty in `surface/base`, so the page never moves |

Layout by window class (SYSTEM-MAP 1): compact is `core`; medium and expanded are `full`. A `full`
bar falls back to `core` when the title cannot keep 12 characters after all pinned items have
moved into More, and at 200 percent text earlier, as on compact.

## 4. States

| State | When | What changes |
|---|---|---|
| Idle | No pen contact for `delay/pen-idle`, or just opened | Fully visible, `surface/raised`, items `action/quiet/fg` |
| Receding | A tool contact begins on the page | Slides up by its own height and fades to 0 over `motion/duration/chrome-recede` (150 ms), `motion/easing/exit` |
| Receded | Until a return trigger | Not in the visual tree and not focusable. Only the NoteBarHandle shows |
| Returning | A return trigger, section 5 | Slides down and fades in over `motion/duration/chrome-return` (200 ms), `motion/easing/enter` |
| Held open | A Menu or the Go to page Popover is open from the bar | Does not recede while it is open. A pen-down on the page closes the Popover or Menu and the stroke is drawn (pen passes through light surfaces), then the bar recedes as normal |
| Focus in the bar | Keyboard, switch or screen reader focus is on a bar item | Does not recede on its own. The first pen contact on the page moves focus to the page, ends this hold and recedes the bar, unless a Menu or Popover is open. A pen tap on a bar item moves focus there and so holds the bar only until the next page contact |
| Hover, Focus-visible, Active | Per IconButton | `action/quiet/bg-hover`, the README focus ring, `action/quiet/bg-pressed`. Hover and press feedback are off for the pen while it writes |
| Item toggled on | Minimap toggle | `selection/fill` plus `selection/indicator` |
| Item disabled | Command not enabled | `state/disabled/fg`, reason as description. Page indicator on one page is hidden, not disabled |
| Loading | Never | Jobs report as Progress items, not on the bar |
| Empty title | Untitled document | Placeholder `text/muted` "Untitled", never blank |
| Error, Drag | n/a | |

## 5. Receding and returning, exactly

**Writing** means a contact by a drawing tool on the page: pen, highlighter, eraser, lasso, shapes,
text placement, or a finger when fingers draw. Finger scroll, pan, pinch and a pen hover do not
count. Contact on chrome never counts.

1. **Recede.** On the first frame of contact the bar starts leaving. It is gone, and the handle
   fully drawn, 150 ms later. The handle is drawn from the first frame at its final place and has no
   animation of its own, so after those 150 ms nothing in the chrome moves while the pen is down.
2. **Timer.** At every lift a timer of `delay/pen-idle` (1500 ms) starts. Each new contact stops
   and restarts it, so a pause between strokes never brings the bar back mid-sentence.
3. **Hover.** While the pen hovers in proximity over the page, the timer does not run. When the pen
   leaves proximity it starts fresh. Where the hardware reports no hover (finger, mouse, a pen
   without proximity), the timer runs from the lift.
4. **Return** happens on the first of:
   - the timer ending;
   - a **finger or mouse** tap in the top edge zone, a full width strip `size/hit/min` high. A
     tap is down and up within `input/tap/duration` (250 ms) and under `input/tap/travel` of
     movement. It is consumed: the page gets nothing. This strip is live for finger and mouse
     only. **A pen tap at the top of the page always inks**, whatever the bar is doing, so a full
     stop, an i dot or a tap that places text is never lost to chrome;
   - the pen hovering within the top edge zone for `delay/hover-recall` (500 ms), where the hardware
     reports hover. It does not draw and does not need a tap;
   - a press on the NoteBarHandle by any input (the handle is a control, not page);
   - the command `bar.show` (no default key; the palette and More list it);
   - focus entering the region (F6 or Tab onto the handle). Focus goes to the first item after
     the bar is in place. Focus never lands on a hidden bar.
   The palm rule (README) applies to the strip: a touch larger than the platform palm threshold,
   or any finger touch within `delay/palm-window` of pen activity, is ignored, so a resting palm
   never recalls the bar.
5. **Interrupted return.** A contact during the 200 ms return turns the bar round from where it is,
   with the 150 ms recede. Writing always wins.
6. **No reflow.** Neither move resizes, scrolls or re-lays out the page, in either position. The
   side panel, the dock and the FindBar stay where they are.
7. **Reduced motion.** Both moves are an opacity change at 0 ms. There is no slide.
8. **Not announced.** A screen reader hears nothing when the bar recedes or returns.
9. **Never alone.** Undo and Redo are in the dock, so a receded bar loses no editing command.
10. **Gaps pass ink.** A pen contact on the bar's empty gaps or padding (not on an item) is not
    chrome: it starts a stroke on the page and recedes the bar. So a floating bar never blocks
    writing under it. Items still activate on a pen tap.
11. **Does not return on its own after:** a page change, a zoom, a scroll, or the wheel opening. It
    returns only for the triggers above.

## 5a. Bands at the top of a document

One table, so strips, bars and banners never collide. All overlay the page area and push nothing.

| Order from the top | Band | Anchor | Notes |
|---|---|---|---|
| 1 | Safe mode strip | Above the NoteBar position | One line, `z/chrome`; does not move the page |
| 2 | NoteBar | Its `position` | Recedes. `line` keeps its row |
| 3 | FindBar | Directly under the NoteBar position, in place when the bar recedes | Never recedes; slim form while writing (find-bar.md) |
| 4 | Banner | Under the lowest band that is showing | Overlay, waits for pen idle, dismissible except a blocking error |

Within `z/floating-bar` the stack order is, top first: NoteBarHandle, NoteBar, FindBar, canvas
SelectionBar. On compact the NoteBar `core` stays one line and the FindBar sits under it (see find-bar.md).

## 6. Content rules

- **Very long title:** truncates at the end with an ellipsis, keeps at least 12 characters, and
  gives pinned items up first. German and Norwegian page text ("Seite 12 von 300", "Side 12 av 300")
  is about 40 percent longer: the indicator is measured, never fixed width, and never truncates.
  It drops to its short form "12 / 300" when measured space runs out, then the title shortens.
- **No title:** the placeholder above.
- **One pinned item, none:** the row closes up; More stays at the end.
- **500 pages:** "412 of 500" grows the indicator by measure. Nothing else changes.
- **Right to left:** the whole row mirrors, Back becomes the right-hand control and its arrow
  flips. The bar stays centred (it does not follow handedness).

## 7. Overflow rule

- The bar holds at most four pinned items, at most two from extensions (SYSTEM-MAP 8). The icon
  rule "four icon-only actions on the trailing side" counts pinned items; More is the overflow
  control and is not counted.
- When width runs out, items move into More one at a time **by command priority**, lowest
  importance first: the highest priority number leaves first, ties by id. Extension commands (band
  600 and up) therefore leave before built-in ones. Order on the bar is always the user's order.
- Order of giving way: pinned items, then the page indicator to its short form, then the title
  down to 12 characters, then the `core` layout.
- Items that moved appear at the top of More in a first group, in the same order. The Menu scrolls
  past 10 rows. Nothing is ever shrunk or squeezed under `size/hit/min`.
- More always holds, in groups: Find; Add page, Insert pages (Pages tab), Insert (opens the Insert
  tab, `panel.insert`), Page setup; Export, Share; Document settings; an Extensions group of at
  most five (the rest under "More from extensions"); Command palette, Activity, Settings;
  Customise bar (a link row to Settings, Toolbar); a quiet sync status line when sync is on and
  not up to date. Commands and link rows only. Add page is hidden on Canvas.

## 8. Extension slots

Note bar item (SYSTEM-MAP 7): an extension command appears among the pinned items as an
IconButton the same size and variant as its siblings, with the extension's icon and "Name, from
Extension" in the Tooltip. The user pins at most two. Extension commands in More sit in its
Extensions group. An extension never sets the position, size or order. When an extension is
turned off, everything it added disappears: its pinned item leaves the row and its rows leave
More. Its pin position is remembered and restored when it is turned on again.

## 9. Pack content

None. Documents embed their assets; the bar shows no pack state.

## 10. Input

- **Stylus:** hover fill and a Tooltip at 500 ms on pen hover, except while writing. A tap
  activates on lift. A pen contact on the bar never recedes it.
- **Touch:** short tap activates; long press on an icon-only item shows its Tooltip (4.7.1). The
  title is not draggable.
- **Mouse:** hover, click on release, right click does nothing special.
- **Keyboard:** one tab stop for the bar's buttons; Left and Right move between buttons, Home and
  End to the ends, Enter and Space activate, Escape returns focus to the page. A text field in the
  bar (the title while renaming) is its own tab stop and keeps its arrow keys for the caret; arrows
  rove only the buttons around it. F6 moves between landmarks and entering the bar returns it.
  Ctrl+Alt+G Go to page (Cmd+Option+G), F2 renames the title, Ctrl+F Find.
- **Handedness:** none. The bar is centred and never mirrors for the writing hand.

| Platform | Role |
|---|---|
| Web | `toolbar`, label "Note bar"; the title is a `button` "Title, Maths notes, rename" |
| Compose | `Row` with `selectableGroup`-style traversal and `Role.Button` items; `paneTitle` "Note bar" |
| WinUI | `CommandBar` pattern as a custom `ItemsControl`, `AutomationProperties.Name` "Note bar" |
| SwiftUI | `HStack` with `.accessibilityElement(children: .contain)` and the `toolbar` trait |

## 11. Tokens

| Token | Use |
|---|---|
| `size/bar/note`, `size/bar/note-max` | Height, floating width cap (proposed) |
| `size/control/small`, `size/hit/min`, `size/icon/small` | Items |
| `surface/raised`, `surface/base` | Bar, empty row in `line` |
| `border/subtle` | Hairline |
| `elevation/float` | Floating |
| `radius/surface`, `space/sm`, `space/md` | Shape, padding, gaps |
| `text/default`, `text/muted`, `type/label` | Title, indicator, placeholder |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `action/toggle/fg-on` | Items |
| `selection/fill`, `selection/indicator`, `state/disabled/fg` | Toggle, disabled |
| `motion/duration/chrome-recede`, `chrome-return`, `motion/easing/exit`, `enter` | Moves |
| `delay/pen-idle`, `delay/hover-recall`, `delay/palm-window` | Return timer, pen hover recall, palm window |
| `input/tap/duration`, `input/tap/travel` | Edge tap for finger and mouse |
| `z/floating-bar`, `z/chrome`, `component/focus/*` | Layer, focus |

## 12. Do and Don't

**Do**
- Recede on pen contact only, and return on the listed triggers only.
- Keep the page still: no reflow, no resize, in both positions.
- Move overflow to More by command priority and keep the user's order.
- Keep Back, Title and the page indicator on every kind of document.

**Don't**
- Don't recede for a finger pan, a hover, or a tap on chrome.
- Don't return the bar mid-stroke, and never swallow a pen tap: a pen tap at the top edge is ink.
- Don't put Undo or Redo here, or a destructive command as a pinned item.
- Don't add a fifth pinned item, or a third extension item.

## 13. Traces to

Flows: 03 write-and-switch-tools, 11 navigate-long-document, 13 open-document (and 06 for Find).
Replaces: NoteBar. Pairs with NoteBarHandle (kept).
