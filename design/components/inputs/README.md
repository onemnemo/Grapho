# Inputs

Members: [FieldShell](field-shell.md) (the shared frame and the shared text, numeric, handwriting and hosting contracts), [TextField](text-field.md), [TextArea](text-area.md), [SearchField](search-field.md) (scopes global, document, filter), [Select](select.md), [DateField](date-field.md), [ShortcutField](shortcut-field.md), [Slider](slider.md), [Stepper](stepper.md), [Checkbox](checkbox.md), [RadioList](radio-list.md).

Text-like members share one frame. Slider, Stepper, Checkbox and RadioList share the sizes, state tokens, group label row and message row, but not the box.

## How they line up

| Member | Height | Padding | Radius | Icon size | Label style |
|---|---|---|---|---|---|
| TextField, SearchField, Select, DateField, ShortcutField | `size/control/default` (small and large by prop; small is default at touch density) | `space/md` | `radius/item` | `size/icon/small` in a small box, else `default` | `type/label` above, `type/body` value, `type/caption` message |
| TextArea | taller of its rows and `size/control/default` | `space/md`, `space/sm` | `radius/item` | same | same |
| Slider | `size/control/default` slot, thin track | none | `radius/pill` | none | `type/label`, readout `type/body` |
| Stepper | `size/control/default` | square buttons | `radius/item` | by box role | `type/label`, value `type/body` |
| Checkbox | `size/checkbox` in a hit slot | `space/sm` gap | `radius/key` | `size/icon/small` mark | `type/label`, `type/caption` |
| RadioList | rows `size/row/list` | `space/md` | circle `radius/pill`, row `radius/item` | none | `type/label`, `type/caption` |
| Button beside a field (Add) | same height role as the box | Button's own | `radius/pill` | by role | aligned to the box, not the label |

Shared: one focus recipe (`focus/*`), `state/disabled/*`, `state/readonly/*`, a message row that is always reserved, error as icon plus text plus `border/error`, and hit slots of `size/hit/touch` (touch) or `size/hit/min` (compact).

## Tokens to add

- `size/hit/touch`: 48 on Android, 44 on Apple and Windows, at touch density; compact keeps `size/hit/min`.
- Dimension tokens: `size/track` (slider track thickness), `size/thumb` (slider thumb diameter), `size/tick` (tick length), `size/keycap` (key cap height).
- Contrast pairs (type `ui`, 3:1) to declare:
  - `selection/indicator` on `surface/strong` (Slider fill) and on `surface/control` (Radio dot and circle edge)
  - `border/control` on `surface/raised` and `surface/strong` (Slider thumb edge, Stepper dividers, scroll thumb, grip, key cap edge)
  - `state/readonly/border` on `state/readonly/bg` and on each host surface (the only boundary of a read-only field)
  - `state/danger/fg` on `surface/control` and `surface/overlay` (error text and icon)
  - `text/muted` on `state/readonly/bg` for message rows next to a read-only field

## Extension UI mapping

| Schema node | Component | Props mapped | Gap |
|---|---|---|---|
| `field` text | TextField | kind `text`, `maxLength` | none |
| `field` number | TextField `numeric` | `unit`, `min`, `max`, `step` | schema has none |
| `field` multiline | TextArea | `rows`, `maxRows` | none |
| `slider` | Slider | `change` to `onChange`, `commit` to `onChangeEnd` | needs `unit` |
| `stepper`, `select`, `radio list`, `checkbox` | Stepper, Select, RadioList, Checkbox | | not yet in the schema |
| none | SearchField, DateField, ShortcutField | | not reachable by extensions |

## Commands to register

None. Clear, reveal, calendar, Reset, minus and plus edit a value; they are not registry commands. Ctrl+F focusing SearchField is an existing command.

## Decisions

1. One FieldShell for every text-like input; there is no floating label.
2. Validation timing and error copy rules; hard limit rule; error is icon plus text plus edge.
3. Platform handwriting services only, with an opt-out list and a minimum writing area; the canvas owns the page.
4. Grapheme counts, NFC on commit, no validation during IME composition, content direction with mirrored slots.
5. One numeric contract for TextField, Stepper and Slider.
6. Hosting: Select and DateField in a Popover push a view; in a Sheet they open a Popover; DateField draws its calendar in a Popover.
7. `size/hit/touch` per platform; the small field is not used at touch density.
8. SearchField Filter: in place, core-owned locale matching, cost-keyed debounce, Popover Escape closes. SearchField has no `>` mode.
9. Select and RadioList hold core enum values only; pack-fed lists are Pickers.
10. RadioList selects by a dot only; `commit` defaults to `on-press`.
11. ShortcutField: per-context clashes, physical keys, fallback Sheet.
12. Slider: relative drag with direction lock, one undo entry per release on document and Studio hosts, none on Settings pages.
