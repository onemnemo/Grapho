# ColourEditor

## 1. Purpose

Make or change one colour, and place it in a colour set. It lives in the Tool settings tab of the side
panel (and in a page when a theme input or a Studio colour is edited). One editor replaces the
disc, the pane and the picker.

**When not to use:**
- Choosing a whole set: a Picker of colour sets.
- Choosing a colour from a fixed list in settings: Select.
- Colour roles of a theme: ThemeEditor.

## 2. Anatomy

```
[ colour field (saturation by brightness) ]      surface drawn by the shell
[ hue slider ============o========= ]
[ opacity slider ========o======== ]              only for highlight roles
 Hex [#2B6CB0___]   Name [Sky blue_______]
 In set: Basic  ( o o o o o o o o + )  8 of 24     set swatches, the edited one marked
 [Add to set]   [Replace]                         one is enabled
```

| Part | Notes |
|---|---|
| Colour field | A two-axis surface, `radius/item`, with a handle; the only free-form drawing surface in the editor |
| Hue, Opacity | Slider (inputs), each with a numeric readout |
| Hex field | TextField, numeric parse contract of FieldShell; accepts 3, 6 and 8 digits |
| Name | TextField, optional |
| Set strip | Swatch parts of the current set, up to 24, plus an add cell. A tap on a swatch **applies** it as the current pen's colour and loads it into the editor |
| Recent | A row of the last 8 colours used and not in any set, so a one-off colour needs no set |
| Actions | Add to set (`colour.addToSet`), Replace (`colour.replace`), Remove (`colour.remove`): Buttons `secondary`; the host owns the primary |

## 3. Variants and sizes

| Prop | Values | Meaning |
|---|---|---|
| `role` | `ink`, `highlight`, `plain` | `highlight` shows Opacity; `plain` is one colour with no set strip, no opacity and one Done (theme inputs: 3 or 6 digit hex only) |
| `host` | `panel`, `page` | Tool settings tab, or a pushed page (theme input, Studio) |

Controls are role `default`; at touch density the small field resolves to `default`.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Default | Fields in the FieldShell frame; swatches `media/edge` hairline plus the check badge (`media/check-bg`, `media/check-fg`, `media/halo`) |
| Hover | Field handle ring grows; swatches `hover/fill` |
| Focus-visible | `focus/*` on the handle and on each control |
| Active | Handle follows the pen; the preview chip updates every frame |
| Selected | The edited swatch: `selection/indicator` check and `selection/fill` behind |
| Disabled | Set full (24): Add to set becomes one step with two choices, "Replace the oldest" or "Start a new set with this colour" |
| Loading | Skeleton on the strip only |
| Empty | A set with no colours: only the add cell |
| Error | Invalid hex: `border/error`, `alert_circle`, "Use three or six digits after the hash sign" (one string, shared with ThemeEditor) in the message row. The colour does not change until valid |
| Low contrast | Info line for ink and highlight roles against the actual page colour, dark paper included: "Hard to see on this page". It never blocks |

## 5. Content rules

- Long colour names: one line with ellipsis, full name in the Tooltip. No name: a computed name (family and lightness, localised, such as "Blue, dark") is the accessible name and shows beside the hex. German and Norwegian names are not shortened.
- 1 colour in the set: shown, Remove disabled (a set keeps at least one). 24 colours: the strip is two
  rows and Add is disabled. A set never holds more than 24 (SYSTEM-MAP 8).
- Names are NFC normalised and counted by grapheme.

## 6. Overflow rule

The strip wraps to at most two rows and never scrolls on its own, so at 24 the whole set is visible.
The panel scrolls as a whole.

## 7. Extension slots

None. An extension may supply a colour only through a pack of colour sets.

## 8. Pack content

The set being edited may be Yours, built in, or from a pack. A built-in or pack set is read-only: the
first change makes "Basic copy" in Yours and says so ("Saved as a copy in Yours"). No pack state shows
in the editor. A colour in a document is stored by value.

## 9. Input

- **Stylus and touch:** drag the handle on the colour field with the pen; the preview follows. Tap a
  swatch to load it. The hex field raises the keyboard only by tap or focus.
- **Mouse:** drag; Alt plus scroll over the field nudges the hue (plain scroll scrolls the panel); double click a swatch to edit.
- **Keyboard:** the field handle moves with arrows (step 1, Shift step 10), Home and End on sliders,
  Enter applies Add to set or Replace, Delete removes the focused swatch (one undo step).
- **Right to left:** the hue slider and strip mirror; the colour field does not.
- **Roles:** the field `slider` with `aria-valuetext` "Saturation 40 percent, brightness 80 percent"
  (two linked sliders for assistive tech); hue and opacity `slider`; the strip `listbox` of `option`.
  Compose `Slider` plus `Canvas` with semantics; WinUI `ColorPicker` as visual reference only;
  SwiftUI `ColorPicker` as reference only. All are custom controls.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/raised`, `surface/sunken` | panel, field well |
| `space/md`, `space/sm` | gaps |
| `size/control/default`, `size/swatch`, `size/hit/touch`, `size/thumb`, `size/track` | fields, swatches, slider |
| `radius/item`, `radius/pill` | field, handle |
| `type/label`, `type/body`, `type/caption` | labels, values, messages |
| `media/edge`, `media/check-bg`, `media/check-fg`, `media/halo`, `border/error`, `selection/fill` | edge, check, error, selected |
| `hover/fill`, `state/disabled/fg`, `text/default`, `text/muted` | states, text |
| `canvas/page`, `focus/*` | contrast check, focus |

## 11. Do and Don't

Do:
- Update the preview on every pen move and commit once on release (one undo entry).
- Say when an edit became a copy.

Don't:
- Block a colour for contrast; inform only.
- Add a 25th colour; offer another set.
- Draw colour chrome with raw values; the swatches are the content.

## 12. Traces to

Flow 03 step 6. Replaces ColourPicker, ColourDisc, ColourPane.

Commands: `colour.addToSet`, `colour.replace`, `colour.remove` (to register). Tooltips: the add cell ("Add colour"), Remove ("Remove colour") and each slider's readout. Uses Slider, TextField, Swatch (tiles
family), Button, Tooltip.
