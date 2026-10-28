# FieldShell (the one frame for every text-like input)

A part, not a screen component. TextField, TextArea, SearchField, Select, ShortcutField and DateField render inside it, so label, helper, error, counter, slots, focus, text rules and states are written once. Closes A-34 (focus) and A-35 (three input systems). The shared contracts in section 9 (text, numeric, handwriting, hosting) bind every member and are referenced by Stepper and Slider.

## 1. Purpose, and when not to use

**Purpose.** One label position, edge, focus ring, message row and state set for every text-like input.

**When not to use.** Slider, Stepper, Checkbox and RadioList are not text-like; they reuse the same sizes, tokens and message row but not the box. A search field in a bar uses SearchField (shell without a label row).

## 2. Anatomy

```
 Label                                   Optional   <- label row (wraps; hint wraps under)
 +-----------------------------------------------+
 | [lead]  value or placeholder      [status][act] |  <- box
 +-----------------------------------------------+
 (alert_circle) Message or helper            12/40   <- message row, always reserved
```

Parts: `label`, `hint`, `box`, `lead` and `trail` slots (logical start and end), `input`, `status` (spinner or `alert_circle`, not a target), one trail `action` (clear, reveal, chevron, calendar, or `more`), `message`, `counter`. A group label (Checkbox group, RadioList, Stepper, Slider) is this same label row; their errors and helpers are this same message row.

## 3. Variants and sizes

- `layout`: `stacked` | `inline` (rename in place, see 9.4).
- `size`: `small` | `default` | `large`, resolving to `size/control/*` by density. **The small field is never used at touch density:** at touch it resolves to the default height. Large is for the one field of a first-run screen only.
- Icon size follows the box role: `size/icon/small` in a small box, `size/icon/default` otherwise.
- Rows of siblings align on the box: the label row takes the height of the tallest sibling's label. Below 480 dp container width, or at text scale above 150%, a row stacks.
- The floating label is retired. A button beside a field (an "Add" next to a name) aligns to the box, not the label, and shares its height role; Buttons are `radius/pill`, boxes `radius/item`, so the pair is aligned, never merged.
- Reserved seam: the trail `action` may be `more`, which opens a Menu (for a later field action such as paste a link). Not used today.

## 4. States

| State | Fill | Edge | Content | Notes |
|---|---|---|---|---|
| Rest | `surface/control` | `border/control`, `border-width/hairline` | value `text/default`, placeholder `text/muted` | |
| Hover | `surface/control-hover` | `border/control-hover` | same | A pen that reports hover shows it |
| Focus-visible | same | same | same | Two-tone ring `focus/gap/*` then `focus/ring/*`; the gap colour is read from the host surface (a Popover is `surface/overlay`) |
| Active (typing) | same as rest | same | caret | |
| Selected | n/a | n/a | n/a | Selected text uses the platform highlight |
| Disabled | `state/disabled/bg` | `state/disabled/border` | `state/disabled/fg` | A field whose disabling hides information stays in the focus order and the reason is the helper |
| Read-only | `state/readonly/bg` | `state/readonly/border`, dashed | same | Focusable, selectable, copyable; the dashed edge needs a declared pair (README, Tokens to add) |
| Loading | same | same | trail `status` spinner after 300 ms, width reserved | The inline busy pattern; typing is never blocked |
| Empty | same | same | placeholder | An example, never the label |
| Error | same | `border/error`, `border-width/emphasis` | `alert_circle` icon plus message text in `state/danger/fg` | Never colour alone: icon and words |
| Could not check | same | same | message in `text/muted` with `info`: "Could not check. It will be checked when you are online." | A failed async check is a hint, not an error |
| Drag | n/a | `state/drag/target-line` | same | Dropped text only |

**Focus rule per kind.** Text-like fields show the ring after any input (they take a caret). Select, DateField trail, Checkbox, Radio, Stepper, Slider and ShortcutField show it for keyboard, switch and screen reader input, and also when focus returns from a closed Popover or Sheet (restored focus is always visible).

**Forced colours and More contrast.** Edges map to the system field edge, error edge to the system alert colour plus the icon and text, read-only dashed edge to the disabled text colour with the dashes kept, focus ring to the system highlight. With More contrast the contrastMore overrides apply to `border/control` and `text/muted`.

## 5. Content rules

- Label: sentence case, wraps, never truncated. German and Norwegian run 30 to 40% longer. The hint wraps under the label and never takes width from it. "Optional" is shown only in forms where most fields are required; otherwise the minority is marked with `aria-required` and the hint stays out of the accessible name.
- Value: single line truncates at the end; file names and paths in the middle. While focused it scrolls with the caret. A read-only value on touch shows a Copy action whose name announces the full value; the tooltip is a mouse extra.
- No text: placeholder.
- **Message row.** The row reserves the helper line always, so a message appears without moving the layout. A longer message wraps downward inside the field's own bottom gap and then grows the row; boxes in a horizontal row stay aligned because only the message row grows. Write messages short; there is no character cap, only the width.
- 1 or 500 items: not for the shell; a list of 500 goes to a Popover (Select) or a Picker.

## 6. Overflow rule

Each slot holds one item. The trail is one `action` plus one `status`. A second action goes into the control's own menu or beside the box (ShortcutField).

## 7. Extension slots

None. Extensions use these controls through the settings page schema (README, "Extension UI mapping"); they cannot add to the shell.

## 8. Pack content

None. Fields never show pack state. A pack-fed list is a Picker, not a Select.

## 9. Input

### 9.1 Validation, errors, limits

- **Timing.** No validation while typing the first time. Validate on blur and submit. Once an error shows, revalidate on every change and clear it the moment the value is valid. Guards that stop impossible input (hex letters past f) act silently. An invalid value is never discarded. On a failed submit focus goes to the first field in error and the message is announced once (polite live region).
- **Copy.** What is wrong, then what to do, in plain words: "Name is already used. Choose another." "Enter a number from 1 to 40." "Add a name." No "Invalid", no "Error:", no exclamation marks.
- **One limit rule.** A maximum is hard: typing stops at the maximum and paste is trimmed, with a toast "Pasted text was shortened." The counter appears at 80% ("32/40"), is announced politely at 90% and at the limit, and has no negative form. Escape never discards typed text, except in `inline` rename, which restores the old name.
- **Keyboard avoidance.** When the on-screen keyboard opens, the focused field and its message row scroll above the keyboard inset; the host Sheet or panel scrolls, the field never overlays the keyboard.
- ARIA: `<label for>`; message via `aria-describedby`; `aria-invalid` in error; `aria-required` for required.

### 9.2 Text rules (IME, graphemes, direction)

- **Never validate or count during IME composition.** Counter, limit, uniqueness and error all wait for composition to end.
- **Count in grapheme clusters.** The counter and the maximum count what a user sees as one character, so "e" with an accent and a flag emoji each count once.
- **Normalise to NFC on commit** (blur, submit, Enter), and compare names in NFC, so decomposed "å" from an Apple keyboard or recognised handwriting equals "å". The core owns the comparison.
- **Direction.** The field follows the direction of its content (`dir=auto`): caret, alignment and ellipsis flip with the text. Lead and trail slots use logical start and end and mirror in right-to-left text. Key caps and numbers keep their order.
- Props that make these testable: `maxLength`, `required`, `filter` (allowed characters), `inputMode`, `autocapitalize`, `spellcheck`, `autocomplete`, `enterKeyHint`; each `kind` sets sensible defaults (names capitalise, hex and password do not spellcheck).

### 9.3 The numeric contract

Used by TextField `numeric`, the Stepper value cell and the Slider readout.

- Parse by locale: accept the decimal comma or point, the grouping separator, a no-break or ordinary space before a unit, the percent sign, and other digit sets. "2,5" and "2.5" are both 2.5 in a decimal-comma locale.
- Clamp to `min` and `max` on commit (blur, Enter), snap to `step`, and say so: the field shows the clamped value and the message row reads "Set to 40." (polite). Out-of-range typing before commit is allowed.
- Not a number: "Enter a number from 1 to 40." Up and Down nudge by `step`, Page Up and Page Down by 10 steps (or 10% of the range), Home and End are not used in text.
- Display uses the locale format with fixed decimals matching `step`, tabular figures, so width does not jump.

### 9.4 Stylus, touch, mouse, handwriting

- **Handwriting uses the platform services**: S Pen handwriting on Android, Scribble on iPad, the Windows Ink handwriting panel. Grapho builds no recogniser. Shells enable the platform modifier on every field (Compose stylus handwriting on `BasicTextField`, `UIScribbleInteraction`, `InputScope` with handwriting panel) and do not clip the parent so the platform writing area can grow.
- **Opt-out**: ShortcutField, the Stepper value cell, and password fields (the platforms do not support them; a pen-only user gets the reveal action and the keyboard). Everything else opts in, including numeric TextField, DateField (the locale parser reads recognised digits) and hex.
- **Minimum writable height.** A box shorter than `size/hit/touch` extends its writing area invisibly to that height and across the full box width, including the lead slot and padding, but not the trail action's own target. TextArea is writable over its whole box.
- **Writing near a field on a page never starts field handwriting.** The canvas owns the page: a pen that lands outside a field's box (the invisible extension shrinks to zero wherever it would overlap the page) writes ink. Field handwriting starts only for a pen-down inside a field in chrome (Sheet, panel, settings, the title field).
- **Inline rename.** The document title field is `inline` and takes handwriting in place: pen down inside it focuses it, the platform writing area appears over it, and the title updates as ink converts. At touch density an `inline` field draws a resting `border/control` bottom edge so a finger or a pen without hover can see it is editable; hover and focus draw the full edge. Tap places the caret and never selects all; keyboard focus selects all.
- **Touch.** The box and label are the hit slot. Hit slots at touch density are `size/hit/touch` (48 Android, 44 Apple and Windows); at compact density `size/hit/min`. A slot may exceed the box as an invisible extension with at least 8 dp between targets. Clearing must not blur the field: the clear action takes pointer down without moving focus.
- **Mouse.** Click focuses; double click selects a word.
- **Tooltips.** Every icon-only part has a Tooltip with its name, no shortcut (none is a command): "Clear", "Show password", "Hide password", "Choose date, From". These are value edits, not commands, so they take no command id; their names come from the host.

### 9.5 Hosting rule

A Select or DateField inside a Popover pushes a view inside that same Popover (a header with Back; Escape or Back pops one view, then closes). Inside a Sheet, Side panel or Settings page it opens its own Popover. Never a Popover on a Popover, never a Sheet from a Sheet, never a Dialog with a list.

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `large` | box height |
| `size/hit/touch`, `size/hit/min` | slot size (touch, compact) |
| `radius/item` | box corner |
| `border-width/hairline`, `emphasis` | rest, error edge |
| `surface/control`, `surface/control-hover` | fill |
| `border/control`, `border/control-hover`, `border/error` | edge |
| `state/disabled/bg`, `border`, `fg` | disabled |
| `state/readonly/bg`, `border`, `fg` | read-only |
| `state/danger/fg` | error text and icon |
| `state/drag/target-line` | text drop |
| `text/default`, `text/muted` | value; hint, helper, placeholder, counter, spinner |
| `type/label`, `type/body`, `type/caption` | label, value, message |
| `space/xs`, `space/sm`, `space/md` | label gap, slot gap, padding |
| `size/icon/small`, `size/icon/default` | slot icons by box role |
| `focus/gap/width`, `focus/gap/color`, `focus/ring/width`, `focus/ring/color` | ring |
| `motion/duration/fast`, `motion/easing/standard` | edge change |

## 11. Do and Don't

- Do reserve the message row and show an icon with every error.
- Do count graphemes, normalise to NFC, and wait for IME composition.
- Don't use a placeholder as the label.
- Don't validate mid-composition or before the user leaves the field.
- Don't open a Popover from a Popover.

## 12. Traces to

Flows 01 to 09, 11 to 15. Replaces the three text-input systems and the FieldRow label styles (A-34, A-35).
