# FindBar

## 1. Purpose

Search inside the current document, with the match count and next and previous. It takes input
and shows a count, which is why it is not a Banner. It stays visible while open and sits outside the
receding group, so a search is not hidden by the next stroke.

**When not to use:**
- Searching the library, titles or tags: SearchField in the TopBar (global scope).
- Narrowing a list: a Filter SearchField in the list.
- Reporting a lasting state: Banner.

## 2. Anatomy

```
+------------------------------------------------------------------------+
| [ mitosis                   x ]   3 of 17    [^] [v]   [x]              |
+------------------------------------------------------------------------+
   SearchField (document scope)  count      prev next   Close
```

| Part | Notes |
|---|---|
| Field | SearchField, scope `document`, `size/control/small` box, auto focus on open, selects the old query |
| Count | `type/label`, "3 of 17", "No matches", "Searching handwriting". A live status. Tabular figures |
| Previous, Next | IconButton `quiet` `small`, `chevron_up`, `chevron_down`; commands `doc.findPrev` (Shift+F3), `doc.findNext` (F3) |
| Close | IconButton `quiet` `small`, `close`, Tooltip "Close, Escape". A surface action, not a command |

## 3. Variants and sizes

None. One bar, height `size/bar/note`, items role `small`. It takes the NoteBar's band:

| Where | Layout |
|---|---|
| Expanded, medium | Directly under the NoteBar position, the same width and inset as the NoteBar in its `position` |
| Compact | Directly under the NoteBar `core`, full width. The NoteBar stays, so Back and More stay reachable |

It overlays the page in both NoteBar positions and pushes nothing, including under `line` (the
NoteBar's reserved row stays, the FindBar sits under it over the page). It is anchored to the band,
not to the NoteBar's visibility: when the NoteBar recedes the FindBar stays where it was.

**Slim form while writing.** When the pen is on the page the bar keeps its place but shrinks to
count, Previous, Next and Close at the inline end, hiding the field, so about half the covered page
returns while the user writes. It grows back at pen idle (`delay/pen-idle`) with no motion while
the pen is down. A pen contact on its empty gaps is ink (pen passes through chrome gaps). Surface `surface/raised`, `border/subtle`, `elevation/float` when
floating, `z/floating-bar`. No handedness mirroring.

## 4. States

| State | Tokens and behaviour |
|---|---|
| Closed | Not in the tree. Escape clears the marks |
| Open, empty | Field focused. Count hidden. Previous and Next disabled: "Type to search" |
| Searching | Count reads "Searching handwriting" with the inline busy pattern on the count after 300 ms. Marks appear as found |
| Matches | "3 of 17", Previous and Next live, marks on the page (FindMarks), current match emphasised |
| No matches | Count "No matches", Previous and Next disabled with the reason |
| Many | Over 9,999: "3 of 9,999+" |
| Error | The search failed: inline "Search stopped. Try again." with Retry in the count slot, never "No matches" |
| Hover, Focus-visible, Active, Disabled | As the IconButtons and field. Disabled uses `state/disabled/fg` |
| Writing | Stays visible, in the slim form above. It does not recede. Hover and press feedback are off for the pen |
| Selected, Drag | n/a |

## 5. Content rules

- **Long query:** scrolls inside the field; the field shrinks before the count or buttons do.
- **Long count text:** "Keine Treffer" and "Ingen treff" are measured; the count never truncates
  and never wraps. If the row cannot hold it, the field gets narrower.
- **No text:** the placeholder "Find in document" in `text/muted`.
- **One match:** "1 of 1", Previous and Next wrap to the same match. **5,000 matches:** the count
  caps as above; stepping stays fast because the core owns the list.
- **Right to left:** the row mirrors and the chevrons keep their meaning (up and down).

## 6. Overflow rule

None; the row is fixed at four parts. On the narrowest window the field gives width down to a
minimum of 8 characters; if that is not enough, the count moves under the field only at 200 percent
text, so the bar's height is a minimum of `size/bar/note` and grows by one line there.

## 7. Extension slots

None. Extensions cannot add to the FindBar or mark matches outside their own block.

## 8. Pack content

None.

## 9. Input

- **Open:** `doc.find`, Ctrl+F or Cmd+F, or More, Find. With the NoteBar receded, Ctrl+F still opens
  it; using More first needs the bar returned (one extra step when writing).
- **Stylus and touch:** tap the field to type (handwriting input is on, via the field's platform
  service); tap Previous and Next. The pen on the page does not close the bar.
- **Mouse:** click; the field keeps focus after Next.
- **Keyboard:** Enter next, Shift+Enter previous, F3 and Shift+F3 anywhere in the document (Ctrl+G is Find next on no platform; Go to page is Ctrl+Alt+G), Escape
  closes the bar, clears the marks and returns focus to the page at the current match. The field is its own tab stop and keeps its arrow keys; Tab moves field, Previous, Next, Close.
  Back or Escape treats the bar as a popover (step 1 of the order).
- **Focus:** opens with focus in the field; focus never moves while the pen writes. Arriving from
  a search result prefills the query and focuses Next so stepping works at once.
- **Commands:** `doc.find`, `doc.findNext`, `doc.findPrev`.

| Platform | Role |
|---|---|
| Web | `search` landmark "Find in document"; count `status`; buttons `button` |
| Compose | Row with `Role.Button` items; count `liveRegion` polite |
| WinUI | `AutoSuggestBox`-free custom `TextBox`; count with `LiveSetting` Polite |
| SwiftUI | `HStack` with `.searchable`-style field; count as `accessibilityLabel` |

## 10. Tokens

| Token | Use |
|---|---|
| `size/bar/note`, `size/control/small`, `size/hit/min`, `size/icon/small` | Height, items |
| `surface/raised`, `border/subtle`, `elevation/float`, `radius/surface` | Container |
| `space/sm`, `space/md`, `type/label`, `text/default`, `text/muted` | Layout, count, placeholder |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Buttons |
| `highlight/match` | Owned by FindMarks and Highlight, not by this bar |
| `delay/pen-idle` | Slim form returns |
| `z/floating-bar`, `component/focus/*` | Layer (below the NoteBar and handle), focus |

## 11. Do and Don't

**Do**
- Keep it open and in its band when the NoteBar recedes.
- Say "No matches" in words and keep Previous and Next disabled with the reason.
- Prefill from a search result and let Enter step at once.
- Close it with Escape and clear the marks in the same step.

**Don't**
- Don't make it part of the receding group, or let it push the page.
- Don't show a spinner on its own; the count carries the busy state.
- Don't search the library from it, or add filters.
- Don't announce every match; announce the count once per change.

## 12. Traces to

Flows: 06 find-something (in the document). Replaces: nothing; it is new (FindBar in COMPONENTS).
