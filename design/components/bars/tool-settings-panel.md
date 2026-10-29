# ToolSettingsPanel

## 1. Purpose

The body of the Side panel's Tool settings tab: everything about the current tool in one scrolling
page, beside the page and live. It holds the pen settings, the ColourEditor, the pens list, the wheel
and quick slot set up, and a "More in Settings" link for the rest. It is the one home for editing a
pen (SYSTEM-MAP 5); picking stays in the wheel and managing in Content, Pens.

**When not to use:**
- Picking a tool, pen or colour quickly: the ToolWheel.
- Device or app wide pen behaviour (pen-only, palm guard, gestures, button actions): Settings, Pen
  and tools, reached by the link row.
- Layers: [layers-panel.md](layers-panel.md).

## 2. Anatomy

```
Tool settings                                      (SidePanel header)
 [ wheel preview ]  [S1*][S2*][S3*][S4][S5]        preview and slot row
 Current tool: Pen   Fountain pen
 Width      ------o------  2.0 pt                 SettingRow, Slider
 Smoothing  ----o--------  30
 Pressure   ------o------  Medium                 curve
 Snap shapes                       [ toggle ]
 Colour     [ ColourEditor, role ink ]
 Pens       [ Picker, manage mode ] ... [Add]      root of this section, not pushed
 Tools ring [ ReorderList: Pen, Highlighter, ... ] each row Menu or Toggle
 Quick slots shown   [-] 5 [+]
 > More in Settings   Pen-only, palm guard, gestures
```

Sections are GroupHeadings (Current tool, Colour, Pens, Wheel, Quick slots, Settings). The body
scrolls; the tab title and Close are the SidePanel's.

| Part | Component |
|---|---|
| Preview and slots | ToolWheel preview (art) and the ToolDock slot row, read only; a tap on a slot opens its Menu |
| Pen settings | [SettingRow](../tiles/setting-row.md)s with Slider, Select, Toggle; each with Reset to default |
| Colour | [ColourEditor](../pickers/colour-editor.md), `host` `panel`, `role` `ink` or `highlight` by tool |
| Pens | [Picker](../pickers/picker.md), manage mode, content type pens; its footer holds Get more, Import and New |
| Tools ring | ReorderList of ListRows: tool name, a Toggle for an extension tool, row Menu |
| Quick slots shown | Stepper 0 to 5, `toolbar.slotsShown` |
| Link row | ListRow with a chevron: "More in Settings", deep link `grapho://settings/pen` |

## 3. Variants and sizes

`tool`: `pen` | `highlighter` | `eraser` | `lasso` | `text` | `shape` | `extension`. The section list
follows the tool (SYSTEM-MAP 4.5 contextual appearance):

| Tool | Current tool section | Colour | Others |
|---|---|---|---|
| `pen` | Width, Smoothing, Pressure, Snap shapes | ink | Pens, Wheel, Slots, link |
| `highlighter` | Width, Snap to text (PDF with a text layer), Snap shapes | highlight (with opacity) | same |
| `eraser` | Size, Mode (stroke or area) | none | Wheel, Slots, link |
| `lasso`, `text`, `shape` | The tool's own options (text size, shape fill) | ink where it applies | Wheel, Slots, link |
| `extension` | The extension's tool options as an [ExtensionPanelHost](extension-panel-host.md) tree | none | Wheel, Slots, link |

Controls are role `default`. Settings use the scope in the registry: `tool.<toolId>.width`,
`.color`, `.smoothing`, `.pressure` (tool scope, sync), `pen.snap` (app). Same values as the home in
Settings; no copy.

## 4. States

| State | Behaviour |
|---|---|
| Default | `surface/raised`; sections with `GroupHeading`; every change applies at once and syncs |
| Hover, Focus-visible, Active | Per component. Slider uses relative drag with direction lock, so a palm on the panel does not jump a value |
| Selected | The current pen row in the Pens Picker: `selection/fill` plus check. The edited colour swatch in the ColourEditor |
| Changed from default | A Reset appears on the SettingRow; Reset restores that setting only |
| Disabled | A setting that does not apply to this tool is not shown, not disabled. "Snap to text" with a scanned PDF (no text layer) is replaced by the line "This PDF has no text. Use the highlighter pen." |
| Loading | Skeleton rows in the Pens section after 300 ms; the pen settings are local and instant |
| Empty | Pens: the Picker's own empty state. Tools ring with only built-in tools: nothing to add |
| Error | A setting the core refuses snaps back with an inline message under the row; the panel stays |
| Extension tool turned off | Its row leaves the Tools ring and the wheel; its slot disappears (one rule); restored when on |
| Writing | The panel stays put. Sliders and Toggles accept touch; pen hover and press feedback off. Nothing animates while the pen is down |
| Drag | Tools ring and slots reorder with a drop line; each row has Move earlier and Move later in its Menu |

## 5. Content rules

- **Long labels** ("Glättung der Linie", "Trykkfølsomhet"): SettingRow wraps; the Slider keeps its
  track; readouts never truncate.
- **No tool settings:** the lasso with no options shows only the Wheel and link sections.
- **One pen, 500 pens:** the Picker virtualises and has its Filter; Add stays in its footer.
- **Compact `peek`:** the tab is a long scroll; the preview and slots stay on top of the scroll.

## 6. Overflow rule

No More. The page scrolls; sections that do not apply are absent. The Pens Picker's own footer
collapses to its More button in a narrow panel (SYSTEM-MAP 5). The Tools ring holds eight; beyond
eight, extension tools appear in the Tools page (`tool.toolsPage`), a pushed view with Back.

## 7. Extension slots

An extension tool shows its option tree in the host (above) and a Toggle in the Tools ring. The
panel has no other extension slot. Extensions cannot add sections.

## 8. Pack content

The Pens Picker owns pack states (Off, Hidden, Kept from, Not installed) as SYSTEM-MAP 5 and 6
allow; this panel adds none of its own. Colour sets come from the ColourEditor's set strip and the
set switcher (`tool.colourSets`, a pushed Picker with Back).

## 9. Input

- **Stylus:** drag sliders with relative movement; tap Toggles, rows and swatches. Hex entry uses
  handwriting where the platform has it. No pen action opens this tab by itself: the wheel's More does.
- **Touch:** as above; Palm rule (README) applies. A long press on a Tools ring or slot row then move
  reorders (4.7.1); long press then lift opens the Menu. Long press on a control shows its Tooltip.
- **Mouse:** click, hover, drag; the scroll wheel scrolls the page, not a Slider.
- **Keyboard:** Ctrl+Alt+4 opens the tab (`panel.toolSettings`); Tab walks the controls in reading
  order; Slider arrows step; Alt+Up and Alt+Down reorder a ring row; Enter on the link row follows it.
  Focus returns to the wheel's tool on close.
- **Pushed views:** Colour sets, All pens (`tool.pensMore`) and the Tools page push inside the tab
  with Back, up to three levels, closing before the panel (SYSTEM-MAP 3).
- **Commands:** `tool.pensMore`, `tool.toolsPage`, `tool.colourSets`, `colour.addToSet`,
  `colour.replace`, `colour.remove`. A link row opens Settings with `app.settings` at `pen`.

| Platform | Role |
|---|---|
| Web | `tabpanel` "Tool settings" with `group`s per section, labelled controls |
| Compose | `LazyColumn` of sections with `heading()` on each |
| WinUI | `ScrollViewer` with text headers, `AutomationProperties.Name` |
| SwiftUI | `List` with sections, `accessibilityHeading` |

## 10. Tokens

| Token | Use |
|---|---|
| `surface/raised`, `border/subtle`, `space/md`, `space/lg`, `radius/item` | Panel, sections |
| `size/control/default`, `size/row/list`, `size/hit/touch`, `size/icon/default` | Controls and rows |
| `type/heading`, `type/label`, `type/caption`, `text/default`, `text/muted` | Headings, labels, hints |
| `action/quiet/*`, `selection/fill`, `selection/indicator`, `state/disabled/fg` | Rows, selected |
| `state/drag/insertion`, `state/drag/source-opacity`, `component/focus/*` | Reorder, focus |
| `delay/palm-window` | Palm rule |

## 11. Do and Don't

**Do**
- Apply every change at once and show the same value as the Settings home, with Reset.
- Show only the settings that apply to the current tool.
- Give each section a heading and end with the "More in Settings" row.
- Keep the Pens list as the root of its section, not a pushed view.

**Don't**
- Don't add pen-only, palm guard or gesture settings here; they live in Settings, Pen and tools.
- Don't show a disabled control for a setting the tool does not have.
- Don't edit pens or colour sets in two places; this tab is the home for editing.
- Don't open a Sheet or a centred modal for colour.

## 12. Traces to

Flows: 03 write-and-switch-tools (set up my tools), 04 (highlighter on a scanned PDF), 07. Replaces:
CustomiseWheel, the WheelPanel and Inspector tool pages, ColourPane's host. Uses: SettingRow,
Slider, Toggle, Stepper, Select, ColourEditor, Picker, ListRow, ReorderList, GroupHeading.
