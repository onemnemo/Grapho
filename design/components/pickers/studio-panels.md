# StudioPanels

## 1. Purpose

The rail and the per-type editor panels inside StudioShell: what the rail opens, and which existing
controls each look type (cover, folder style, canvas preset, template) shows in its panel. StudioShell owns
the frame (top bar, canvas, preview, Done, Cancel, Undo, Redo, unsaved Dialog); this file owns what
fills the canvas side and the panel. It adds no new control: every row below is an existing component.

**When not to use:**
- The frame, title field, Done, Cancel, history: StudioShell.
- A theme: ThemeEditor. A pen's width and opacity: the Tool settings tab.
- A sticker: it has no editor (it is imported).

## 2. Anatomy

```
Expanded, right-handed (rail then panel on the off-hand side)
+--+----------------------+---------------------------------------+
|R | Panel tabs           |   canvas                              |
|a | Colour|Pattern|Text  |      +-----------+                    |
|i | controls             |      | preview   |                    |
|l |                      |      +-----------+                    |
+--+----------------------+---------------------------------------+
Medium: rail is a top strip; panel is SidePanel over; preview inset
Compact: canvas above; rail is a strip above the bottom panel; panel is Tabs with peek and full
```

| Part | Notes |
|---|---|
| Rail | A column of IconButtons `quiet` (a row on medium and compact): the canvas tools of the look. Shares the ToolDock slot size and rules, but it is not a ToolDock and holds no quick slots |
| Panel | SidePanel structure: Tabs (two to four) or the TabRail rule above four; the active tab body scrolls |
| Canvas | The type's surface. Covers: a drawing canvas under the cover frame. Folder styles and canvas presets: the preview itself, with no free drawing |
| Preview | The real renderer-kind tile at its registry size, live on every change (one frame) |

## 3. Variants and sizes

`look`: `cover` | `folder-style` | `canvas-preset` | `template`. One size; controls role `default` (touch
density rules apply).

**Rail entries.** Each entry is a tool that changes what the pen does on the canvas, or opens an insert.

| Entry | Opens or does | Looks that show it | Command |
|---|---|---|---|
| Pen | Pen tool on the cover canvas; its colour and width appear as a row at the top of the active tab | cover, template | `tool.pen` |
| Eraser | Eraser tool | cover, template | `tool.eraser` |
| Shapes | Popover with five shapes; the pick arms the shape tool | cover, template | `studio.shapes` |
| Text | Text tool: tap the canvas to add a text run; the Text tab opens | cover, template | `tool.text` |
| Image | The Insert Picker (type Images) in the panel as a pushed view; a pick adds the image to the cover | cover, template | `insert.photo` |
| Select | Lasso, to move and resize what was added | cover, template | `tool.lasso` |

Folder styles and canvas presets have no rail: their canvas is the preview and every edit is a panel
control. The rail is then absent, not empty.

**Growth rule.** The rail shows up to five entries at once; further entries go into a More entry whose Menu
lists them in command priority order, the same rule as the top bar (More is not counted). A new look type
declares its rail entries in the registry's `editor` data; an extension cannot add one. The Image entry and
Select move into More first on compact. Entries are never reordered by use.

**Handedness.** The rail and the panel sit on the off-hand side: left for a right-handed user, right for a
left-handed one (`general.handedness`), beside the canvas, rail nearest the edge. Done stays at the top end
and Cancel at the top start in both hands. Right to left mirrors the order of top bar items, not the
handedness side.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Default | Rail IconButtons `action/quiet/fg`; panel `surface/raised` |
| Hover, focus-visible, active | IconButton and control states; `focus/*` |
| Selected | The armed rail tool: `selection/fill`, `action/toggle/fg-on`, short bar `selection/indicator` |
| Disabled | A control that does not apply (Spine when ratio is fixed): `state/disabled/fg`, reason as description |
| Loading | Skeleton in the panel body after `feedback/delay/skeleton` |
| Empty | A new look starts from the type's default |
| Error | A control failing validation shows its message row (`border/error`, `alert_circle`); the preview keeps the last valid value |
| Dirty | Any change marks the Studio dirty; see below |
| Saving | Owned by StudioShell (Done inline busy) |

**Unsaved changes.** The panels hold a draft. Nothing is written until Done. Cancel, Back and Escape with
a changed draft ask once with the Dialog `unsaved` (Keep editing is the default and Escape there is Keep
editing). Changing tab, rail tool or window class never discards the draft. Undo and Redo apply to the
whole Studio history, one entry per control release or per stroke; a Slider drag is one entry.

## 5. Content rules

- Tab labels are one word; at 200% text or German and Norwegian lengths Tabs becomes a Select (measured).
- Text controls: very long cover text wraps in the preview as it will on the tile; a title over 60
  characters is allowed and the preview truncates with an ellipsis as the library tile does.
- A cover with 1 colour slot shows one swatch row; with 12 slots the Colour tab lists them as a
  RadioList of slot names, each with its swatch.
- A palette picks from the colour sets (Picker, type colour set, in a pushed view); a custom colour opens
  ColourEditor `plain`.

## 6. Overflow rule

Rail: five entries then More. Tabs: four, then the TabRail rule. Panel body scrolls. On compact with the
keyboard up the body keeps a third of the visible height (the Sheet rule) and the active field scrolls
into view.

## 7. Extension slots

None. Panels are built in. An extension's contribution reaches a look only as a pack item (a cover,
a pattern), never as a rail entry or a tab.

## 8. Pack content

Opening a pack or built-in look for Edit works on a copy in Yours; the title shows the copy's name.
Pattern and icon lists come from the registry and may contain pack items; an item from a pack that is
**Off** is not listed, and a look already using it keeps it. No pack state is drawn in a panel.

## 9. Panels per look type

Controls are existing components only. Each row says which one.

**Cover** (rail: Pen, Eraser, Shapes, Text, Image, Select)

| Tab | Controls |
|---|---|
| Colour | RadioList of colour slots (each with a swatch); PickerTile `swatch` rows of the active colour set; button "Custom" opens ColourEditor `plain` |
| Pattern | PickerTile grid (image tile) of covers' built-in patterns; Slider for pattern scale |
| Text | TextField (title text), Select (font family, core enum), Slider (size), SegmentedControl Left, Centre, Right |
| Layout | SegmentedControl spine (Left, Right, None); Toggle "Show title on the cover" |

**Folder style** (no rail)

| Tab | Controls |
|---|---|
| Icon | PickerTile grid of built-in icons (image tile, `small`); Filter above 30 icons |
| Colour | RadioList of colour slot (the style has one slot) with PickerTile swatches; "Custom" opens ColourEditor `plain` |

**Canvas preset** (no rail)

| Tab | Controls |
|---|---|
| Paper | Select (paper size) or Toggle "Infinite canvas"; SegmentedControl orientation (Portrait, Landscape) |
| Background | Select (pattern: Blank, Lined, Grid, Dotted), Slider (spacing), PickerTile swatches (line colour), Toggle "Dark paper in dark mode" follows the Appearance setting, not here |
| Margins | Stepper for each of four margins, or one Stepper with a Toggle "Same on all sides" |

**Template** (rail: Pen, Eraser, Shapes, Text, Image, Select)

| Tab | Controls |
|---|---|
| Pages | The page strip: PickerTile `page thumbnail` with add, duplicate and remove in the tile Menu |
| Page | Select (paper), Select (background), as in the canvas preset |
| Elements | A list of the page's elements (ListRow) with Menu: Move up, Move down, Delete |

**Live preview.** The preview redraws in the next frame after any control change, with the real renderer
kind and text wrap; a failed validation leaves the previous drawing. The preview never shows the rail or
the panel.

## 10. Input

- **Stylus and touch:** the pen draws on the cover or template canvas; the panel ignores palm-size
  touches while the pen is in proximity. Sliders drag by pen; steppers and swatches are `size/hit/touch`.
  The rail IconButtons are `size/hit/touch`; a long press shows the Tooltip with the name and key.
- **Mouse:** click, drag the canvas; scroll over the panel scrolls it, not a Slider.
- **Keyboard:** F6 cycles top bar, rail, canvas, panel. The rail is a toolbar (Arrow keys, Home, End);
  the Tabs follow the Tabs spec; Ctrl+Z and Shift+Z undo and redo; Ctrl+Enter is `studio.done` from any
  focus; Escape is `studio.cancel`. Tools keep their registry keys (`tool.pen`, `tool.eraser`,
  `tool.lasso`, `tool.text`).
- **Roles:** rail `toolbar` with toggle buttons (`aria-pressed`); panel `region` labelled by the active
  tab; Tabs `tablist`; preview `img` with a text description updated on change. Compose, WinUI and
  SwiftUI use their toolbar and tab controls as visual reference; all are custom with platform
  accessibility.
- **Commands to register:** `studio.shapes` (Shapes). Existing: `tool.pen`, `tool.eraser`, `tool.lasso`,
  `tool.text`, `insert.photo`, `studio.done`, `studio.cancel`; `studio.undo`, `studio.redo` are in the README.

## 11. Tokens

| Token | Used for |
|---|---|
| `surface/base`, `surface/raised`, `surface/sunken` | shell, panel, canvas well |
| `size/dock/slot`, `size/hit/touch`, `size/control/default`, `size/icon/default` | rail entries, controls |
| `size/panel/width`, `size/panel/width-over` | panel width |
| `space/sm`, `space/md`, `space/lg` | gaps and padding |
| `radius/item`, `radius/surface`, `radius/pill` | controls, panel, rail targets |
| `type/label`, `type/body`, `type/caption` | labels, values, messages |
| `action/quiet/fg`, `action/quiet/bg-hover`, `action/quiet/bg-pressed`, `action/toggle/fg-on` | rail |
| `selection/fill`, `selection/indicator`, `border/error`, `state/disabled/fg` | states |
| `focus/*`, `feedback/delay/skeleton`, `elevation/float` | focus, loading, over panel |

## 12. Do and Don't, traces

Do:
- Keep the rail and panel on the off-hand side and Done and Cancel at the same corners in both hands.
- Redraw the real tile on every change and keep the last valid value on an error.
- Ask once, with Keep editing as the default, before throwing a draft away.

Don't:
- Add a rail entry, tab or control from an extension.
- Write anything before Done, or show pack state in a panel.
- Use a control that is not an existing component; a new need is a new component first.

Traces to: flow 07 (Studio and saved looks) and `screens/studio.md`, sections 4 and 5. Replaces
StudioRail, StudioSide and the per-type StudioPanel bodies. Uses StudioShell, SidePanel, Tabs,
IconButton, Slider, Select, TextField, SegmentedControl, Toggle, Stepper, RadioList, PickerTile,
ListRow, ColourEditor, Picker.
