# DateField

## 1. Purpose, and when not to use

**Purpose.** Enter or pick a date in the user's locale format. Renders in the FieldShell ([field-shell.md](field-shell.md)). Locale, first weekday, calendar system and digits come from the core's locale data; the calendar grid is drawn by the shell from tokens and shown in a Popover.

**When not to use.** Named ranges such as "Last 7 days": Select, whose last option "Choose dates" reveals two DateFields. Time of day: not offered.

## 2. Anatomy

```
 From
 +-----------------------------------------------+
 | 24.10.2026                          (calendar)|
 +-----------------------------------------------+
 Example: 24.10.2026                                 <- helper, locale example
```

Parts: label, box, trail action `calendar` (name "Choose date, From"), helper with an example date written in the locale.

## 3. Variants and sizes

- `size`: `small` | `default` (small resolves to default at touch density).
- Props: `min`, `max` (no future dates for "modified before"). A pair of fields is owned by a **DateRange** wrapper that passes `to.min = from` and `from.max = to`; the fields do not know each other.
- One date kind; no time.

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Active (calendar open) | Edge `border/control-hover`; the Popover shows |
| Calendar day rest, hover, selected, disabled | `text/default`; `hover/fill`; `selection/fill` with `selection/indicator` ring; `state/disabled/fg` |
| Error | "Enter a date like 24.10.2026." (example in the locale); out of range: "Choose a date from 1.1.2025." Range: "End date is before the start date. Choose a later one." Each with `alert_circle` |
| Empty | Placeholder shows the example; empty means no limit |

## 5. Content rules

- The pattern and helper come from the locale, shown as an example date, not pattern letters (not "dd.mm.yyyy").
- Typed input is one entry model: free text parsed by the locale. It accepts the locale separator, a dot, slash or space, and other digit sets. A two digit year resolves to the nearest year within 50 years of today. Up and Down do not edit segments.
- Non-Gregorian calendars follow the locale data.
- Long text, 1 and 500 items: not applicable.

## 6. Overflow rule

On narrow containers (under 480 dp) the From and To pair stacks.

## 7. Extension slots

None. DateField has no extension schema node.

## 8. Pack content

None.

## 9. Input

- **Hosting.** The calendar is a Popover anchored to the box. Inside a Popover it is a pushed view of that Popover; inside a Sheet it opens its own Popover. There is no platform modal dialog and no Sheet from a Sheet. The shell draws the grid; the locale supplies weekday, month names and digits.
- Stylus: tap the box to write or type (handwriting enabled; the parser reads recognised digits). Tap the button for the calendar.
- Touch: `inputMode` is the platform's number-and-punctuation keypad (it must carry the locale separator); otherwise the full keyboard.
- Mouse: click box or button.
- Keyboard: type; Alt+Down or F4 opens the calendar; in the grid arrows move by day, Page Up and Down by month, Enter picks, Escape closes (back to the field).
- ARIA: the field is `textbox` with the example in `aria-describedby`; the button is `button` named "Choose date, From" (or "To"); the grid is `grid`. Native: custom on all shells, locale data from the core.
- Tooltip on the button: its name.

## 10. Tokens

FieldShell tokens, plus `surface/overlay`, `elevation/popover`, `radius/surface`, `hover/fill`, `selection/fill`, `selection/indicator`, `size/hit/touch` and `size/hit/min` for day cells and the button slot.

## 11. Do and Don't

- Do show an example date in the locale.
- Do let DateRange own the pair rule.
- Don't open a Sheet or a modal from a Sheet.
- Don't expand two digit years to the current century blindly.

## 12. Traces to

Flow 06. New.
