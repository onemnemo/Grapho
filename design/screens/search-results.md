# Screen: Search results

## 1. Purpose

Find a note written weeks ago by a word, tag, date or folder and land on the right page, with an honest
statement when the index is not ready. It is a place: it opens from the Search field or Ctrl+F in the
library, groups results by document and shows the matching text.

## 2. Flows served

06 find something (main), 13 open (a result opens at the page of the match), 05 (folder-scoped
search), 12 (the Filter scope is a separate use of SearchField, not this screen).

## 3. Layout and regions

Regions: TopBar `place` search, filter row, Banner band, results (virtualised), Toast stack. The
Sidebar shows on Expanded with no place selected; on smaller classes it is a drawer reached from the
TopBar only while the field is empty, otherwise the lead is Back.

### Expanded, R

```
+-----------+--------------------------------------------------------+
| Home      | < [ mitosis                       x ] [Select][Filter]  |
| Starred   | (Type v)(Date: Month v)(Folder: Biology x)(*)(Tag v) Clear all |
| Semester 1| 412 documents                                           |
| Trash     +--------------------------------------------------------+
| [sync][gear]| Week 4 Biology                       Semester 1       |
|           |  p.12  "...stages of [mitosis] begin with..."  [crop]   |
|           |  p.14  "...late [mitosis] spindle..."                   |
|           | Lab report.pdf                       Biology            |
|           |  p.3   "...observed [mitosis] in onion..."              |
+-----------+--------------------------------------------------------+
```

L: Toasts bottom right; nothing else mirrors.

### Medium

As Expanded without the Sidebar. The filter Chips wrap to a second row when they do not fit.

### Compact

```
+--------------------------------+
| < [ mitosis             x ]    |   the field fills the bar
| (Type)(Date)(Folder)(*)(Tag) > |   Chips scroll sideways
| 412 documents    Clear all     |
| Week 4 Biology     Semester 1  |
|  p.12 "...[mitosis] begin..."  |
+--------------------------------+
```

Select and Filter go to More on Compact; Filter opens the same Popovers from there.

### Short

The field and one Chip row stay pinned; the count line scrolls away. Keyboard-up behaviour: the Chip row
scrolls with the results so the field and at least two result rows remain visible.

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | TopBar `place` search; SearchField scope `global` | Visible: Search, Select, Filter. More: Activity, Command palette, Settings. Lead is Back |
| Recents | ListRow `kind` plain per recent query, with a Clear text Button | Shown when the field is empty and recents exist; none yet draws nothing |
| Filter row | Chip `menu` for Type, Date, Folder, Tag (removable when preset); Chip toggle for Starred; text Button "Clear all" | Each filter Chip opens a Popover that applies at once with no Done; Type, Date, Folder and Tag are Chip `menu` (trailing chevron) and Starred is a toggle Chip |
| Popovers | Type: Checkbox list. Date: Select (Any time, This week, This month, This year, Custom) plus two DateField for Custom. Folder: FolderChooser `host` popover with Filter. Tag: Checkbox list with SearchField | Select and DateField inside a Popover push a view in it, never a second Popover |
| Banner band | Banner neutral: "Indexing handwriting, 40 pages left. Results may be incomplete." link to Settings, Search index. Banner warn: "Handwriting search is not available for Norwegian yet." link to Settings, Language. Charging rule line when "Index only while charging" is on | One at a time; link, not a button, to a Settings page |
| Results | SearchResultGroup `kind` typed or handwriting: title, folder, snippets with Highlight, page numbers, a thumbnail crop of the match | One group per document |
| Feedback | Skeleton, EmptyState `filtered`, Banner region, Toast | |

A group for a PDF without text reads "No text in this PDF"; the title still matches. Tapping a result
opens the document at the page of the match with FindMarks on the match and the FindBar prefilled
(note-notebook.md and note-pdf.md).

## 5. States

| State | What changes |
|---|---|
| First use | The field with "Search titles, handwriting, PDF text and tags." No mascot, no EmptyState |
| No recents | Nothing drawn under the field, no heading |
| Typing | Rows arrive as the core answers; Skeleton rows only if over 300 ms, held 300 ms |
| No match | EmptyState `filtered`, no mascot: "Nothing matches 'mitosis' in Semester 1, this month." Primary Clear filters |
| Indexing | The indexing Banner; results shown so far remain |
| Recognition unavailable | The Norwegian Banner; titles, tags and PDF text still match |
| Error | Banner region danger "Search stopped. Try again." Retry. Never reads as no results |
| Offline | Not applicable. Search is local |
| Very large | 10,000 documents: virtualised, heading "412 documents", no pagination |
| Long German text | Chip labels measured: a Chip row wraps (Medium and up) or scrolls (Compact); "Beliebiger Zeitraum" is a Select value so it fits; snippets wrap to two lines then ellipsis, Highlight kept inside the visible part |
| 200% text | Chips wrap onto lines; the field keeps full width; snippets two lines; the thumbnail crop moves under the snippet |
| Selection mode | Select works on documents: SelectionBar `library` with Move to, Star, Tag, Delete |
| Scoped | Opened from a folder: the Folder Chip is preset; its x widens the search to the whole library |

## 6. Behaviour on resize and split screen

The query, filters, scroll position and the focused result survive every class change and a Back from
a document. Chips re-measure on width change: wrap, then scroll sideways, then a More Chip (chip spec).
An open filter Popover re-anchors to its Chip; if the Chip leaves view it closes. At 360 wide the field
fills the bar.

## 7. Keyboard and focus order

Arrival focus goes into the field (a search place is entered to type). Ctrl+F in the library opens it.
Order: field, Select and Filter, Chip row (one tab stop, arrows rove), results. Down from the field
moves into the first result, arrows move through snippets, Enter opens at the match, Escape returns
to the field and a second Escape leaves the place. F3 and Shift+F3 are not used here (the FindBar owns
them). Back returns to the place Search was opened from; the result row regains focus and keeps query,
filters and scroll. Screen reader: "Week 4 Biology, page 12, stages of mitosis begin with"; the count is
a live region and "412 documents" updates politely, at most once a second.
