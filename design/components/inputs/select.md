# Select

## 1. Purpose, and when not to use

**Purpose.** Choose one value from a list that does not fit a segmented row. A FieldShell whose box is a button ([field-shell.md](field-shell.md)); the list opens in a Popover. Holds values that come from the core's enums (sort order, folder, language), not content from packs.

**When not to use.** Up to 3 short options: SegmentedControl. Few options that need descriptions: RadioList. Commands: Menu. Pack or extension content, or options with pictures: the Picker (a pack-fed list is a Picker, SYSTEM-MAP 5).

## 2. Anatomy

```
 Sort by
 +-----------------------------------------------+
 | Name                                (chevron_down)|
 +-----------------------------------------------+
   +-------------------------------+
   | (search) Filter               |   <- above 8 options
   | [lead] Name              check |
   |        Date modified          |
   | + New folder                  |   <- optional footer row
   +-------------------------------+
```

Parts: label, box (value, `chevron_down`), Popover with an optional Filter row, listbox, optional `lead` slot per option, optional footer row ("New folder" style create actions).

## 3. Variants and sizes

- `size`: `small` | `default` (small resolves to default at touch density).
- `layout`: `stacked` | `inline` (value-only box in a settings row).
- Searchable is **derived**, not a variant: the Popover shows Filter above 8 options. The shape is decided by the option count when the list opens and does not change while it is open or loading.
- Multi-select is not a Select: it is a Checkbox list in a Popover.

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Hover | `surface/control-hover`; chevron `text/default` |
| Active (open) | Edge `border/control-hover`; chevron flips |
| Option rest | `size/row/menu`, `text/default` |
| Option hover, keyboard focus | `hover/fill`; inset ring |
| Option selected | `selection/fill`, a `check` in `selection/indicator` at the end |
| Option disabled | `state/disabled/fg`, with the reason as a second line |
| Loading | Skeleton rows in the list after 300 ms; the box stays usable |
| Load failed | One row: "Could not load. Try again." with a Retry text Button |
| Empty | Box disabled, stays in the focus order, message "Nothing to choose yet." With Filter and no match: the EmptyState filtered state |
| Error | Required and empty on submit: "Choose a folder." with `alert_circle` |

## 5. Content rules

- Long option: wraps to two lines in the list. **The box grows to two lines** for a long value (it never truncates the differing suffix), and on touch tapping opens the list where the full value is checked.
- No selection: placeholder "Choose". A Select with a default never shows it.
- 1 option: it still opens; the box is not read-only unless the field is.
- 500 options: Filter, virtualised list, group headers in the shell's label style.

## 6. Overflow rule

The Popover is at most 8 rows tall at default density, then scrolls with the Filter pinned.

## 7. Extension slots

None. SYSTEM-MAP 7 has no slot that adds a Select option. A `select` node in the extension schema, once added, takes the extension's own values.

## 8. Pack content

None. A Select never shows pack state. Pack-fed choices are a Picker.

## 9. Input

- **Hosting** (FieldShell 9.5): inside a Popover the list is pushed as a view in that Popover; inside a Sheet or panel it opens its own Popover. Never a Popover on a Popover.
- **Focus model** (custom on all shells). The box is a `combobox` (`aria-haspopup="listbox"`, `aria-expanded`). Without Filter, focus stays on the listbox and uses `aria-activedescendant`. With Filter, focus moves to the Filter `searchbox`, and `aria-activedescendant` on it points into the listbox; Down and Up move the active option. On close, focus returns to the box.
- Stylus and touch: tap opens, tap an option selects and closes. Mouse: click, wheel, type-ahead when there is no Filter.
- Keyboard: Enter, Space or Down opens; Up and Down; Home and End; Enter selects; Escape closes (the first Escape closes, it does not clear the Filter); Tab closes without selecting.
- Native: custom on Compose, WinUI and SwiftUI; `ComboBox` and the menu `Picker` cannot hold a pinned Filter or group headers.

## 10. Tokens

FieldShell tokens, plus `surface/overlay`, `elevation/popover`, `border/subtle`, `radius/surface` (Popover), `size/row/menu`, `hover/fill`, `selection/fill`, `selection/indicator`, `size/icon/small` or `default`.

## 11. Do and Don't

- Do show Filter first above 8 options.
- Do let the box grow so a long value is readable.
- Don't put commands or pack content in a Select.
- Don't open a Select's Popover from inside a Popover.

## 12. Traces to

Flows 01, 02, 05, 06, 08, 10, 12, 15. Replaces Select.
