# SettingRow

## 1. Purpose

One setting: a label, an optional helper, its control, and Reset when the value is not the default.
It hosts any input component and gives them one rhythm on every Settings page, in side panel tabs
and in extension settings pages.

**When not to use:**
- A list of things (packs, notebooks): use [ListRow](list-row.md).
- A form in a Sheet where the field has its own label above: use the input component alone
  (TextField, Select).
- A choice among large visuals: use [PickerTile](picker-tile.md) or [ChoiceCard](choice-card.md).

## 2. Anatomy

```
 Label                                     [ control ]   (Reset)
 Helper text, up to 2 lines
```

Stacked:

```
 Label                                          (Reset)
 Helper text
 [ control, full width ................................ ]
```

Parts: label, helper, control (slot), Reset, error line, divider.

## 3. Variants and sizes

- `layout`: `inline` | `stacked`. The default follows the control type, so a page keeps one rhythm:
  Toggle, Checkbox, Select, Stepper and SegmentedControl of two are inline; Slider, TextArea, radio
  list, TextField and SegmentedControl of three are stacked. A row flips from inline to stacked
  only when its label cannot fit beside the control, measured at runtime, never the other way.
- The Reset slot is reserved at the end of the row, so the control does not move when Reset appears.
- Row height is a minimum of `size/row/list`; a stacked row grows. The control keeps its own size
  role (`default` in a row; `small` only for three or more sibling inspector rows).
- Control width inline: its natural width, at most 45% of the row; Select and TextField use
  `size/popover/width-narrow` as the minimum.

Spacing: side padding `layout/gutter/*` in a page, `space/lg` in a panel; label to helper none;
helper to a stacked control `space/sm`; label to control `space/lg`.

## 4. States

| State | Changes |
|---|---|
| Default | label `text/default` `type/label`; helper `text/muted` `type/caption` |
| Hover | `hover/fill` on the row when the control is a Toggle or Checkbox (the whole row toggles); other controls keep their own hover |
| Focus-visible | the control's own ring; a row of a Toggle focuses the Toggle |
| Active | `press/fill` for a whole row toggle |
| Selected | n/a. A setting is not selectable |
| Disabled | label and helper `state/disabled/fg`; the helper line becomes the reason ("Needs sync turned on"); the control is disabled and focusable |
| Changed | Reset appears, a text Button; the default value is in its Tooltip ("Reset to Medium") |
| Loading | control shows inline busy; label and helper stay; or Skeleton `list` for a whole page |
| Empty | n/a |
| Error | the control's solid `border/error` edge and a message in `state/danger/fg` under the row, never colour alone |
| Drag | n/a. Settings do not reorder |
| Search match | label and helper use [Highlight](search-result-group.md); the page scrolls the row into view |

## 5. Content rules

- Labels are short ("Pen only"), sentence case, not a question. Helper is one or two sentences, plain
  and calm; it says what the setting changes, not how to use it.
- Very long text (German, Norwegian, 200% text): label wraps up to three lines, helper up to four;
  the control drops under the text (stacked) before anything is cut. Nothing is truncated.
- No helper: omit it, the row is a single line. 1 row on a page: no divider. 500 rows: a settings
  page is never that long (12 sections, SYSTEM-MAP 8); the index filters.
- Reset restores the registry default for this setting only and is undoable with a Toast.

## 6. Overflow rule

Control and Reset never overflow into a menu. Reset moves under the control when stacked. A
setting with more than one control is two settings.

## 7. Extension slots

Extension settings pages are `list` and control nodes inside a SettingRow each; the shell wraps the
control in a SettingRow from its `label`. The extension supplies the label and value (the schema has no helper prop; a `row` subtitle serves); Reset appears only if the extension declares a default. The page title and
extension name belong to the shell.

## 8. Pack content

None. Settings never show pack state. The Content pages use ListRow and PickerTile.

## 9. Input

- **Stylus and touch:** tap the row to toggle a Toggle or Checkbox; other controls take the tap on
  the control. Reset is a `size/hit/min` target. No long press.
- **Mouse:** click; hover on a toggle row. **Keyboard:** Tab reaches the control then Reset; Space and
  arrows are the control's own; Enter on a row of a Toggle toggles. Escape leaves a control being
  edited and keeps the value only if committed.
- **Roles:** a `group` labelled by the label, except for a Toggle or Checkbox row, where the control itself carries
  the label and no group wraps it (no double announcement); the helper is `aria-describedby`; Compose `Modifier.semantics` merge; WinUI control with `HeaderTemplate`;
  SwiftUI `LabeledContent`. Reset: `button` "Reset NAME to default". Command: `settings.reset`
  (to register), with the setting id.

## 10. Tokens

| Token | Use |
|---|---|
| `size/row/list`, `size/hit/min`, `size/popover/width-narrow` | geometry |
| `space/sm`, `space/lg`, `layout/gutter/*` | spacing |
| `type/label`, `type/caption` | text |
| `text/default`, `text/muted`, `state/disabled/fg`, `state/danger/fg` | text roles |
| `hover/fill`, `press/fill`, `border/subtle`, `border/error` | states, divider |

## 11. Do and Don't

- Do stack the control when text would cut; never cut the label.
- Do show Reset only when the value differs from the default.
- Don't hide a disabled setting; say why it is off.
- Don't add a second control or an icon to the row.

## 12. Traces to

Flows 03, 07, 09, 12, 14.
