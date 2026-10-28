# 06 Find something

## Goal

Find a note written weeks ago by a word, a tag, a date or a vague memory, land on the right page,
and be told honestly when the index is not ready.

Counting rule: see 01. Start at the library.

## Entry points

- Top bar Search field; Ctrl+F in the library (`app.search`); the palette.
- Inside a folder, the same field with that folder as a removable filter chip.
- In a document: More, Find; Ctrl+F (`doc.find`).
- Filter fields in pickers and lists are covered in 02 and 12.

## Steps: global search

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Tap the Search field | Top bar to Search results place | `SearchField` |
| 2 | Recents show; none yet shows nothing | Search results | `ListRow` recents, Clear |
| 3 | Type; results narrow live | Search results | `SearchResultGroup`: one group per document, `Highlight`ed snippets, page numbers, a thumbnail crop of the match |
| 4 | Tap a result | Document | opens at the page of the match with `FindMarks` on the match and the `FindBar` prefilled so Next walks the rest |
| 5 | Optional: filter | Search results | filter `Chip`s: Type, Date, Folder, Starred, Tag |

Filter controls (each applies at once; none has Done):
- Type: a `Popover` with a `Checkbox` list.
- Date: a `Popover` with a `Select` (Any time, This week, This month, This year, Custom). Custom adds
  two `DateField`s that open the platform date picker; the format follows the locale.
- Folder: a `Popover` with `FolderChooser`-style list and Filter.
- Starred: a toggle `Chip`, no Popover.
- Tag: a `Popover` with a `Checkbox` list and Filter.
Active filters stay as removable `Chip`s with "Clear all". Searching inside a folder presets the
Folder chip; its x widens the search.

## Steps: find in the document

1. Ctrl+F or More, Find (the bar must be visible; mid-writing +1 reveal). The `FindBar` docks under
   the Note bar position and stays visible while open, outside the receding group.
2. Type: matches in handwriting and PDF text are marked on the page with `FindMarks`; the count
   shows "3 of 17". Enter or the arrows (F3, Shift+F3) step; the page scrolls to each.
3. Escape closes the bar and clears the marks.

## Tap counts

| Task | Taps |
| --- | --- |
| Find by a word and open the page | 3 (Search field, type, result); opens at the match |
| Find by date | 3 (Search field, Date chip, a preset) |
| Find by a custom range | 6 (Search field, Date chip, Custom, two dates and OK in the platform picker) |
| Search inside a folder | the Folder chip is preset; 1 tap on its x widens it |
| Find in a document | 2 with the bar visible, 3 mid-writing; Ctrl+F |

## States

| State | What the user sees |
|---|---|
| First use | The field and "Search titles, handwriting, PDF text and tags." No mascot |
| No recents | Nothing rendered, no empty heading |
| Typing | Rows arrive as the core answers; `Skeleton` only if over 300 ms |
| No match | "Nothing matches 'mitosis'." naming the filters ("in Semester 1, this month"), Clear filters; no mascot |
| Indexing | `Banner` at the top of the results: "Indexing handwriting, 40 pages left. Results may be incomplete." with a link to Settings, Search index |
| Recognition unavailable | `Banner` "Handwriting search is not available for Norwegian yet." with a link to Settings, Language (recognition language). Titles, tags and PDF text still match |
| Charging rule | If "Index only while charging" is on and the index is behind, the Banner says so |
| PDF without text | The group says "No text in this PDF"; the title still matches |
| 10,000 documents | Results virtualised; "412 documents" in the heading |
| Error | Inline "Search stopped. Try again." Retry; never reads as no results |
| Offline | Not applicable: search is local |
| In document, no match | The FindBar count reads "No matches" |

## Exits

| Exit | Result |
|---|---|
| Tap a result | Document at the match; Back returns to the results with query, filters and scroll kept |
| Clear (x) | Query cleared, stays in the place |
| Back, Escape | Leaves Search for the place it was opened from |
| Escape in the FindBar | Closes the bar, marks cleared |

## Wireframes

Search results, Expanded:

```
+-----------+------------------------------------------------+
| Home      | < [ mitosis                         x ]         |
| Starred   | (Type v)(Date: Month v)(Folder: Biology x)(*)(Tag v)|
| Semester 1|  Clear all                    412 documents     |
| Trash     +------------------------------------------------+
|           | Week 4 Biology                  Semester 1      |
|           |  p.12 "...stages of [mitosis] begin with..."   |
|           |  [thumb crop]                                   |
|           | Lab report.pdf                  Biology         |
|           |  p.3  "...observed [mitosis] in onion..."       |
+-----------+------------------------------------------------+
```

FindBar (Expanded):

```
+------------------------------------------------------------+
| < Week 4 Biology   12 of 40                          [More]|
| [ mitosis          ]  3 of 17   [^] [v]  [x]               |
+------------------------------------------------------------+
```

Compact: the field fills the Top bar, chips scroll horizontally, the Sidebar is a drawer. The
FindBar is one line. German check: "Beliebiger Zeitraum" is a Select value, not a segment, so it
fits. Medium: as Expanded without the sidebar.

## Access

Keyboard: Ctrl+F; Down moves from the field into the first result, arrows move, Enter opens, Escape
returns to the field; Tab reaches the filter chips. Screen reader: results read as "Week 4 Biology,
page 12, stages of mitosis begin with"; the count is a live region ("3 of 17"). Focus returns to
the result row on Back. Recognised handwriting text is exposed to screen readers as the page's text
for a shared note.
