# SearchResultGroup

## 1. Purpose

One document in the search results, with its matching snippets under it, and the Highlight mark that
shows the match inside text. A document with many hits reads as one group, not as many rows.

**When not to use:**
- A document with no snippets (title match only) in a chooser or list: use [ListRow](list-row.md)
  `kind` `item`.
- A match inside the open document: FindBar and FindMarks.
- A command match: the command palette.

## 2. Anatomy

```
 [mini cover]  Biology notes                       3 matches
               Notebook, Yesterday
    p. 4   ... the cell **membrane** is made of ...
    p. 12  ... crossing the **membrane** by diffusion ...
    Show 1 more
```

Parts: header (a ListRow-like row: mini cover, title, meta, match count), snippet rows (page label,
text with Highlights), Show more, divider.

## 3. Variants and sizes

`kind`: `typed` (matches in typed text, PDF text or titles) | `handwriting` (matches in recognised
handwriting: the snippet is the recognised text plus a small crop of the ink at `size/tile/crop`, to
add). One style otherwise. The header row is `size/row/list` minimum; snippet rows are one or two
lines (`type/body`) with a `type/caption` page label at the start. Three snippets show; Show more
reveals the rest in place, ten at a time.

Highlight is an inline mark, not a component the author places. Matched text is `text/default` (the
only text role allowed on `highlight/match`) on a `highlight/match` fill with `radius/key`, and an
underline of `border-width/emphasis` so the mark is not colour alone. The surrounding snippet is
`text/muted`. The current match (the one a tap opens) adds `selection/indicator` for its underline.
There is no `text/on-highlight` token.

## 4. States

| State | Changes |
|---|---|
| Default | header `text/default`; snippets `text/muted`, matches Highlighted |
| Hover, active | `hover/fill`, `press/fill` on the snippet or header under the pointer |
| Focus-visible | ring on the snippet row or header |
| Selected | n/a. Search results are not selected; the Selection bar acts on documents from the library |
| Disabled | n/a |
| Loading | Skeleton `list`; results stream in by group and stay in place; a count line "Searching" is `type/caption` |
| Empty | EmptyState sm: what was searched and Clear; no mascot |
| Error | inline reason with Retry; never looks like empty |
| Partial | the groups that worked show and a Banner names how many documents failed, with View |
| Not yet indexed | a header line "Still reading handwriting" and a count |

## 5. Content rules

- Snippets are the user's text: around 80 characters either side of the match, cut at word
  boundaries, with an ellipsis at each cut. In CJK the cut is by character. Match marks keep their
  casing and diacritics; the match is found without them.
- Many matches in one line: marks are all drawn; the snippet is the first. 1 match: no "Show more".
  500 documents: virtualised by group; counts "3 matches"; German count words wrap.
- A title match with no snippet is a header only, with "Title" as the page label.

## 6. Overflow rule

Three snippets, then Show more (ten at a time). More than 100 matches in one document: the header
count says "100 or more"; open the document and use FindBar.

## 7. Extension slots

None. Extension blocks that implement `describe` appear as plain text snippets.

## 8. Pack content

None. Search never shows pack state.

## 9. Input

- **Stylus and touch:** tap a snippet opens the document at that page with the match current; tap the
  header opens at page 1 or the last place. Long press selects the document (4.7.1).
- **Mouse:** click; right click opens the item Menu on the header. **Keyboard:** Down moves
  group to snippet to next group; Enter opens; Space toggles selection on a header; Show more is a
  Tab stop. F6 cycles landmarks.
- **Roles:** group is a `list` item with a nested `list`; the match mark is `mark` on the web; Compose
  and SwiftUI use an attributed string with a label "match"; WinUI `TextBlock` highlighters. Name:
  "Biology notes, notebook, 3 matches". Snippet: "Page 4, the cell membrane is made of".

## 10. Tokens

| Token | Use |
|---|---|
| `size/row/list`, `size/tile/crop` (to add), `size/hit/min` | geometry |
| `space/sm`, `space/md`, `space/lg` | spacing |
| `type/label`, `type/body`, `type/caption` | text |
| `text/default`, `text/muted`, `highlight/match` | text, mark (`text/default` only on the mark) |
| `hover/fill`, `press/fill`, `border-width/emphasis`, `selection/indicator`, `radius/key`, `border/subtle` | states, underline |

`text/muted` and `text/subtle` are never used on `highlight/match`.

## 11. Do and Don't

- Do group every snippet under its document.
- Do mark matches by fill and edge, and announce them.
- Don't cut the match out of a snippet.
- Don't show more than three snippets before Show more.

## 12. Traces to

Flow 06. Kept separate from ListRow because it holds many snippets per document.
