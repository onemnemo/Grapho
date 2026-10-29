# ToolWheel

## 1. Purpose

Pick the tool, the pen and the colour, and adjust the pen's width and opacity, without taking
attention off the page. It opens at the pen tip or beside the dock. The pens sit in the middle,
the tools and the colours form the two rings. It is the fast path; the Tool settings tab and the
Picker in the side panel are the complete ones.

The tool ring and the colour ring are the **`wheel` placement of the Picker** (SYSTEM-MAP 5): the
Picker supplies the items and renders them with the wheel renderer. The wheel is a host, not a
second picker.

**When not to use:**
- Smoothing, pressure, a colour set's colours: the Tool settings tab (SidePanel).
- Browsing, filtering, hiding or managing pens and colours: Picker in the side panel (pens, colour
  sets), opened from the wheel.
- A tool already in a quick slot: one tap on the ToolDock slot is faster.
- Going back to the last tool: `tool.previous` (Q, or a double tap on the current tool in the dock).

## 2. Anatomy

```
          .------ colour ring ------.
        /  o  o  o  o  o  o  o  o  [S] \       S = Sets segment (fixed)
       |   .---- tool ring -----.        |
       |  / pen hl er la tx ex .. [T] \   |     T = Tools page segment (fixed)
       | |   .---- pens hub ----.     |  |
       | |   |  [p1] [p2] [p3]  |      | |
       | |   |  [p4] [p5] [p6]  |      | |
       | |   |  Fountain pen  . o o  |  | |     label line, then page dots
        \ \   '------------------'   / /
          '-------------------------'
```

Menu form (screen reader, or when the wheel does not fit):

```
 Tools    (o) Pen  ( ) Highlighter  ( ) Eraser ...   Tools page
 Pens     (o) Fine  ( ) Brush ...                    All pens
 Colours  (o) Ink black ( ) Blue ...                 Colour sets
 Pen size and opacity  >  (pushes a Popover with two Sliders)
```

| Part | Notes |
|---|---|
| Plate | The disc: `wheel/plate`, `elevation/wheel`, `z/wheel`. Solid flat bands, no gradients, no blur |
| Colour ring | Outer. Up to 15 swatches a page plus the fixed **Sets segment** (last slot), which opens the colour set Picker in the side panel. A set holds at most 24 colours, so two pages |
| Tool ring | Inner ring. Up to 8 tool segments, plus the fixed **Tools page segment** that opens the Tools page |
| Pens hub | The middle. Up to six pen cells a page (3 by 2), real nibs in the pen's colour, paged by dots up to 24 pens. The last cell after the final pen is **All pens**, which opens the pens Picker in the side panel |
| Label line | Inside the hub, under the grid: the name of the segment that is pressed, hovered or focused (and its shortcut), else the current pen's name; page dots share the line. This is the wheel's own label, not a Tooltip |
| Editing view | Tapping the current pen's cell turns the wheel into its editor: the tool ring becomes the **width arc**, the colour ring the **opacity arc**, each a band of at least `size/hit/touch`, the hub shows the pen sample. The marker on the arc carries the value; no floating labels |
| Dividers | `wheel/divider` between rings, `wheel/band-edge` on the outer edge |

**Geometry is derived from the hit rule, never drawn first.** Every segment, cell and arc band is at
least `size/hit/touch` at its narrowest point, measured on the chord, not the centre line.
- Hub radius: the 3 by 2 grid of `size/hit/touch` cells plus the label line must sit inside the
  circle, so hub radius is the half diagonal of that block plus the divider.
- Each ring depth is `size/hit/touch` plus a divider.
- Outer radius is hub radius plus two ring depths.
- Slots in a ring are limited so that the arc length at the ring's inner edge divided by the slot
  count is at least `size/hit/touch`. This allows 8 tools and 16 colour slots.
- At 44 the diameter is about 344 dp and at Android's 48 about 372 dp (token values in the README).
Both placements use the full disc. The half disc and the edge tab are dropped.

## 3. Variants and sizes

`placement`: `pen` | `dock`. `mode`: `radial` | `menu`.

| Value | Meaning |
|---|---|
| `pen`, `radial` | Disc centred on the pen tip; moved, not resized, to stay `size/wheel/margin` inside the window |
| `dock`, `radial` | Disc beside the dock, at the current tool's height, `space/lg` clear of the dock edge, in the page area |
| `menu` | A Menu with radio groups, anchored at the pen tip or the dock tool |

One size per density, derived above. `size/hit/touch` is used even on fine pointers: the wheel does
not shrink for a mouse. The radial form is used only when the whole disc plus margin fits; else
`menu`. That includes 200% text and the short window.

The first slot of each ring starts nearest the dock and runs away from the writing hand, so
handedness changes only the start and direction of the order, not the geometry. Angular order of items
is fixed by the item's index and never by the current choice.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Closed | Not in the tree |
| Opening | Opacity 0 to 1 over `motion/duration/fast`, `motion/easing/enter`. Refused while the pen is down on the page. Reduced motion: 0 ms |
| Idle | Plate `wheel/plate`; glyphs `wheel/icon`; swatch edge `wheel/swatch-edge`; nibs on `wheel/nib-well` |
| Hover | Pen or mouse hover: segment fill `wheel/hover`; the label line shows its name and shortcut in `wheel/text`. Leaving the segment clears it and applies nothing |
| Pressed | Finger or pen contact on a segment: fill `wheel/press`, label line shows its name. Lift on it applies. Sliding off and lifting off any segment cancels |
| Focus-visible | `component/focus/*` recipe, with the gap colour set to `wheel/plate` |
| Selected | The current tool, pen and colour: `wheel/selected-fill` plus `wheel/selected-indicator` (a bar on the inner edge for tools and pens, a ring for a swatch). Not fill alone |
| Disabled | A tool that cannot apply is not drawn. `tool.previous` is a command, not a segment, so nothing in the wheel is ever shown disabled |
| Loading | n/a. An extension tool draws at once |
| Empty | An empty pens hub shows only the All pens cell |
| Error | n/a. A pen whose pack is Off stays in its place; no pack state is shown |
| Editing | As anatomy. The arcs are drag controls (step by `Left` and `Right`); changes apply live; the wheel stays open until the hub is tapped, Escape, a tap outside, or the pen is out of range for `delay/pen-idle` |
| Turning | Dragging along a ring or the hub, or a mouse wheel, turns to the next page over `motion/duration/base` with `motion/easing/standard`. The ring does not spring |
| Closing | After a choice, Escape, or tap outside: opacity to 0 over `motion/duration/exit`. A pen-down on the page outside: hit-testing stops and the wheel is hidden at 0 ms in the same frame as the stroke starts |

Nothing in the wheel moves, blinks or reflows while the pen is down on the page: it never opens or
redraws under a stroke. What the springs gave (a settling bounce on dots and bands) is lost on
purpose; nothing overshoots.

## 5. Content rules

- **Very long text:** the label line wraps to two lines in `type/label`, then truncates in the middle
  so the distinguishing end of a name survives; the full name is the accessible name and the menu
  row. German "Kalligrafie-Füllfederhalter" never resizes the hub. The extension's name is a second
  caption line when there is room, else only in the menu form and the accessible name.
- **No text:** segments are glyphs, nibs and swatches. The label line is the current pen's name.
- **One item:** a ring with one slot draws it at its normal angle; the hub with one pen shows one cell and All pens.
- **500 items:** the wheel is curated: 8 tools, up to 24 pens (four pages of six), up to 24 colours
  (two pages). The whole library is behind All pens, Sets and the Tools page.
- **Names:** every colour has a name or a computed name ("Blue 3 of 15"). Never a hex alone.

## 6. Overflow rule

- **Tools:** eight in the ring; more tools live on the **Tools page**, which is the Tool settings
  tab's Tools list where the user curates which eight show (SYSTEM-MAP 7 wording becomes "Tools page
  (the Tool settings tab)"). Two commands: `tool.pensMore` (All pens) and `tool.toolsPage`.
- **Pens, colours:** pages with dots. The wheel opens on page 1 of each, or the page the user last
  turned to in this document, never the page of the current item, so positions are stable. The current
  item is marked on its page and on the page dot.
- **No room:** the menu form. Segments never shrink below `size/hit/touch`.

## 7. Extension slots

Canvas tool (SYSTEM-MAP 7): a segment in the tool ring, in a monochrome glyph in `wheel/icon` (no
raw colour). The ring holds eight; more are on the Tools page. Its name and the extension's name show
in the label line and the menu row. Turning the extension off removes the segment, the ring closes
nothing up (its slot stays empty), and the position is remembered. Turning it on returns it to its slot, or to the
first empty slot, or to the Tools page if the ring is full. If the current tool is an extension tool
that is turned off, the tool becomes Previous tool, else Pen, with no toast (the dock shows the
change). Its dock slot follows the dock's rule.

## 8. Pack content

Pens and colours from packs and from the user look the same.
- **Off:** the pen or colour stays in place; the wheel shows no pack text. The Picker shows "Off".
- **Hidden:** hidden items do not appear; Unhide is in the Picker.
- **Removed:** the user's kept copy ("Kept from <pack>" in the Picker only) is a Yours item and
  stays in its place.
- **Not installed:** a received document's pens are embedded copies, shown only as that document's
  current tool.

## 9. Input

**Summon**

| Input | Result |
|---|---|
| S Pen button click (setting `pen.buttonClick`, default `tool.wheel`) | `pen` at the pen tip; a second click closes |
| S Pen button hold (setting `pen.buttonHold`, default `tool.eraserHold`) | Eraser while held; no wheel. Released within `input/pen-button/hold` is a click |
| Tap the current tool in the dock, `T` | `dock` placement; `T` starts in keyboard mode |
| Double tap the current tool in the dock | `tool.previous` |
| Screen reader | `menu` form |

A summon during a stroke is ignored (a button click is applied at the lift).

**Choosing:** a tap or a pen-tip tap applies. A finger or pen press, drag and lift on a segment also
applies (radial-menu style). While pressed, hovered or focused, the label line shows the name, so
names exist for touch and for pens without hover. Hover exit never applies. Lifting off the plate
cancels. Apply and close, except in Editing and in keyboard mode with Enter. Tapping the current pen
opens Editing. Sets, Tools page and All pens open their panel and close the wheel.

**Pen-down outside** closes the wheel and the stroke is drawn, so a dismissal tap leaves a
dot; Escape and the second button click are the clean exits.

**Palm and edge:** while the pen is in proximity or within `delay/palm-window` of its last contact,
touches larger than `input/palm-threshold` are ignored and never close the wheel; a finger tap counts
only if under `input/chrome-tap/duration` and `input/chrome-tap/travel`. A drag along a ring starts only
after `input/tap/travel`. The wheel stays `size/wheel/margin` inside the window, off the edge guard.

**Mouse:** hover, click; wheel over a ring or the hub turns the page. **Keyboard (radial):** Up and Down
move between Tools, Pens, Colours in that order; Left and Right move within a ring or the hub grid;
Home and End jump; Page Up and Page Down turn the page; Enter picks and stays; Space picks and closes;
Escape closes. In Editing, Up and Down move between the width and opacity arcs and Left and Right step.
Focus returns to the dock's current tool.

**Cost table** (contacts, S Pen button wheel; the same for touch):

| Job | Contacts |
|---|---|
| Erase and back | 0 (hold the button), then release |
| New colour | 2 (click, tap swatch) |
| New pen | 2 |
| Pen then colour | 4 |
| Colour in a slot | 1 (the dock slot) |
| Width of the current pen | 3 (click, tap the pen, drag the arc), closes after |
| Lasso, recolour, back | 4, as flow 03 |

**Roles:** the wheel is `role="toolbar"` named "Tools, pens and colours" with three `radiogroup`
children (Tools, Pens, Colours) of `radio` items with `aria-checked`, and Sets, Tools page and All
pens as `button`s. The menu form is a `menu` with `menuitemradio` groups, the standard type-ahead and
Enter or Space activates and closes; its Pen size and opacity row pushes a Popover with two `slider`s.
Compose: `selectableGroup` and `RadioButton` roles; WinUI `MenuFlyout` with `RadioMenuFlyoutItem`;
SwiftUI accessibility container with `.isSelected`.

**Commands:** `tool.wheel`, `tool.previous` (to register, Q), `tool.pen`, `tool.highlighter`,
`tool.eraser`, `tool.lasso`, `tool.text`. To register: `tool.pensMore`, `tool.toolsPage`,
`tool.colourSets`. `pen.buttonClick` and `pen.buttonHold` are settings, not commands.

## 10. Tokens

| Token | Used for |
|---|---|
| `wheel/plate`, `wheel/divider`, `wheel/band-edge`, `wheel/icon`, `wheel/dot`, `wheel/hub`, `wheel/hub-edge` | Surfaces, edges, glyphs, dots, the pens hub |
| `wheel/hover`, `wheel/press`, `wheel/selected-fill`, `wheel/selected-indicator`, `wheel/text`, `wheel/text-muted`, `wheel/swatch-edge`, `wheel/swatch-ring`, `wheel/nib-well` (to add) | Fixed dark states, each with a declared pair |
| `elevation/wheel`, `z/wheel` | Depth, layer |
| `component/focus/*` | Focus |
| `type/label`, `type/caption` | Label line |
| `size/wheel/hub-radius`, `size/wheel/outer-radius`, `size/wheel/margin`, `size/wheel/min-window` (to add, derived) | Geometry |
| `size/hit/touch`, `size/dot`, `size/row/menu`, `space/lg` | Targets and gaps |
| `motion/duration/press`, `fast`, `base`, `exit`, `motion/easing/standard`, `enter` | Motion |
| `delay/palm-window`, `delay/pen-idle`, `input/palm-threshold`, `input/chrome-tap/*`, `input/tap/travel`, `input/pen-button/hold` | Input |

`wheel/well`, `wheel/grip` and `wheel/scroll-track` are not used; `wheel/plate` is the whole
disc. All wheel colours are fixed: they do not follow the theme, so no theme token such
as `selection/fill` or `action/quiet/*` is used inside it.

## 11. Do and Don't

**Do**
- Derive every size from `size/hit/touch`.
- Keep ring order fixed by index so muscle memory holds.
- Show the name in the label line on hover, press and focus.
- Hide the wheel at 0 ms and draw the stroke when the pen goes down outside it.

**Don't**
- Don't add a third ring, a hub control, or a second home for width.
- Don't apply on hover exit or on a drag that ends off the plate.
- Don't show pack state, or use theme tokens, in the wheel.
- Don't move any item to the page of the current choice.

## 12. Traces to

Flows: 03 write-and-switch-tools. Replaces: PenWheel, WheelFace, PenWheelTab, CustomiseWheel (to the
Tool settings tab).
