# TextField

## 1. Purpose, and when not to use

**Purpose.** One line of typed text: names, titles, URLs, hex colours, numbers, passwords. Renders in the FieldShell ([field-shell.md](field-shell.md)); label, message row, focus, text rules, numeric contract and handwriting are defined there.

**When not to use.** Several lines: TextArea. Searching or filtering: SearchField. A choice from a list: Select. A key chord: ShortcutField. A date: DateField. A count of 0 to 5: Stepper.

## 2. Anatomy

```
 Notebook name
 +-----------------------------------------------+
 | [lead]  Biology 101                    [close]|
 +-----------------------------------------------+
 (alert_circle) Name is already used. Choose another.   11/40
```

## 3. Variants and sizes

- `kind`: `text` | `password` | `numeric`. `kind` only changes behaviour; the rest are props from the shell: `inputMode`, `filter`, `maxLength`, `required`, `lead`, `unit`, `min`, `max`, `step`. A hex field is `text` with `lead="#"`, `filter` of 0 to 9 and a to f, `maxLength` 6, and no spellcheck. URL and email are `inputMode` values, not kinds.
  - `password`: trail action `eye` / `eye_off` ("Show password", "Hide password"), off by default; paste allowed; no handwriting (platforms do not support it).
  - `numeric`: follows the numeric contract (field-shell 9.3), optional `unit` text in trail, tabular figures. Up and Down nudge by `step`.
- `layout`: `stacked` | `inline`.
- `size`: `small` | `default` | `large` (small resolves to default at touch density).

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Focus-visible | `inline` keyboard focus selects all; a tap, or a pen tap, only places the caret. `numeric` never selects all on focus |
| Active | The clear action (`close`, "Clear") shows when the field has text and focus; its slot is reserved at all times so text does not shift, and pressing it does not blur the field |
| Loading | Name checks (unique folder name) show the trail spinner after 300 ms |
| Error | Per FieldShell 9.1: message row with `alert_circle` and text |
| Read-only | Dashed edge; selectable; Copy action in trail for URLs and ids; on touch it announces the full value |

## 5. Content rules

- Long text: end ellipsis; middle ellipsis for file names and paths. The full value is the accessible value.
- No text: placeholder gives an example ("Biology 101").
- Limits: the one limit rule in FieldShell 9.1 (typing stops at the maximum, paste is trimmed and a toast says so). The count is in grapheme clusters.
- Names are compared in NFC.
- 1 or 500 items: not applicable.

## 6. Overflow rule

Trail is one action plus one status. A second action becomes `more` (a Menu) or moves beside the box. Prefix or suffix text longer than 4 characters goes in the label or helper.

## 7. Extension slots

None. Extension settings use this component unchanged: schema `field` kinds `text`, `number` (mapped to `numeric` with `unit`, `min`, `max`, `step`) and `multiline` (TextArea).

## 8. Pack content

None.

## 9. Input

- Stylus: platform handwriting is enabled (opt-out only for `password`); writing area per FieldShell 9.4; hover shows the hover fill.
- Touch: the box is a slot of `size/hit/touch`; the soft keyboard follows `inputMode` and `enterKeyHint`.
- Mouse: click focuses, double click selects a word.
- Keyboard: Tab; Enter submits a single-line host form (not inside a Sheet with a dirty form); in `inline` Enter commits and Escape restores.
- ARIA: `textbox`; `password` uses the platform secure role; `numeric` is `spinbutton` with value bounds. Native: Compose `BasicTextField` with `KeyboardOptions`; WinUI `TextBox`, `PasswordBox`, `NumberBox` as parts of a custom frame; SwiftUI `TextField`, `SecureField` inside the custom frame.

## 10. Tokens

All FieldShell tokens, plus `size/icon/small` or `default` for trail icons by box role, `text/muted` for `unit`, `selection/fill` for the selected-text background where the shell draws it.

## 11. Do and Don't

- Do reserve the clear slot and keep focus when it is pressed.
- Do keep rename as `inline`.
- Don't select all on a pen or finger tap.
- Don't mask a hex or id field as a password.
- Don't validate during IME composition.

## 12. Traces to

Flows 02, 03, 04, 05, 07, 08, 09, 11, 13, 14. Replaces TextField.
