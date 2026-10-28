# SearchField

## 1. Purpose, and when not to use

**Purpose.** The one search input (SYSTEM-MAP 4.4) for three scopes: Global, In document and **Filter**. Renders in the FieldShell without a label row ([field-shell.md](field-shell.md)); the accessible name is the label. It is not the entry to the command palette: that opens from Ctrl+K or Cmd+K, the "Command palette" row in the top bar overflow, and the note bar More. A `>` typed in the field is just a character.

**When not to use.** A value that is not a query: TextField. Choosing from 8 or fewer options: Select. The match count and next and previous for In document live in FindBar, which wraps this component.

## 2. Anatomy

```
 +-----------------------------------------------+
 | (search)  Filter                       [close]|
 +-----------------------------------------------+
 12 of 200                                          <- message row: status, wraps
```

Parts: `lead` (`search`), `input`, trail action (`close`, "Clear"), message row (status, or the disabled reason).

## 3. Variants and sizes

- `scope`: `global` | `document` | `filter`. A `mode` seam is reserved on the field (today only `text`), so a later ink query or saved search needs no new field; chips and suggestions live in the Global Popover, not in the box.
  - `global`: top bar and library Ctrl+F; Enter opens the Search results place; a Popover of at most 5 recents and 5 tags.
  - `document`: inside FindBar; Enter next, Shift+Enter previous; status "3 of 12".
  - `filter`: below.
- `size`: `small` | `default` (small resolves to default at touch density).

### The Filter variant

- Placeholder "Filter"; the accessible name names the list ("Filter colours").
- Narrows the list in place. No Enter, no results place, no scope chip, no suggestions.
- **Matching is owned by the core**, locale-aware. It folds case and, per locale, the diacritics that are not letters (German ä and ö match a and o; Norwegian, Danish and Swedish å, æ and ø are distinct letters and are not folded). Word-start matching applies to space-separated scripts; for Japanese, Chinese and Thai it falls back to substring. Input is compared in NFC.
- **Debounce is keyed to measured cost:** none while a query completes within one frame; 150 ms when the core's query takes longer. Never keyed to item count.
- Status "12 of 200" in the message row; it wraps at 200% text. Announced politely after the debounce. No match: the list shows its EmptyState filtered state (a Clear Button, as EmptyState defines), not a second Clear link.
- Escape: **in a Popover host the first Escape closes the Popover** (clear is the `close` action). In any other host the first Escape clears non-empty text, the next returns focus to the list, and Back order does the rest. Down moves into the list and keeps the text; Up from the first row returns.
- Filter text is not remembered across opens, except within one visit to a settings index.
- A Select above 8 options and every picker use it as the first row; it is the picker's first Tab stop.

## 4. States

As FieldShell. Extra:

| State | Change |
|---|---|
| Rest | Lead icon `text/muted` |
| Focus-visible | Ring on the box; lead icon `text/default` |
| Active | `close` appears when text exists; slot reserved |
| Loading | Global: trail spinner after 300 ms while the core queries |
| Empty | Placeholder; no status |
| Error | Not used for the query. An index failure is a Banner on the results place |
| Disabled | While the index builds: message row "Search index is being built."; stays in the focus order |
| Could not search | message row `info`: "Search is not available right now. Try again." |

## 5. Content rules

- Long query scrolls in the box; `close` stays visible. Text direction follows content.
- No text: placeholder; Global may list recents.
- 1 item: "1 of 1". 500 items: the list virtualises.
- German and Norwegian placeholders may truncate; the accessible name carries the full text.

## 6. Overflow rule

Global Popover: 5 recents and 5 tags, then it scrolls. The status wraps rather than truncating.

## 7. Extension slots

None. Extension search providers add results (SYSTEM-MAP 4.4), not the field. SearchField has no extension schema node.

## 8. Pack content

None. Filter matches what the list contains; pack state belongs to the picker (SYSTEM-MAP 5).

## 9. Input

- Stylus: handwriting enabled; writing area per FieldShell 9.4.
- Touch: soft keyboard with a Search action key; `close` is a full slot of `size/hit/touch`.
- Mouse: click; the Escape rule above.
- Keyboard: Ctrl+F focuses Global in the library, FindBar in a document, Filter in a settings index; Down enters the list; Enter submits Global and goes to the next match in Document, and is ignored in Filter.
- ARIA: `searchbox`; Global and Document sit in a `search` landmark; Filter has `aria-controls` to its list. Native: custom frame on all shells (Compose `BasicTextField` with `ImeAction.Search`, WinUI `TextBox`, SwiftUI `TextField`); not `AutoSuggestBox` and not `.searchable`, which cannot take the frame.
- Name and tooltip: `close` is "Clear".

## 10. Tokens

All FieldShell tokens, plus `highlight/match` (match text on `text/default`), `text/muted`, `size/icon/small` or `default` by box role.

## 11. Do and Don't

- Do put Filter first in every picker, list and settings index.
- Do let the core own matching and normalisation.
- Don't fold Nordic letters.
- Don't give Filter an Enter step or a search button.
- Don't make `>` a mode.

## 12. Traces to

Flows 01, 05, 06, 08, 12, 15. Replaces SearchField and the ad hoc picker filters.
