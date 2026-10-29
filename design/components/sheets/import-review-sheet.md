# ImportReviewSheet

## 1. Purpose

Shows what is about to be added and lets the user set a name and location or choose what to take,
before anything is written. Nothing runs until the primary is pressed. For an extension it is also
the consent view. It is built from **ImportReviewBody** (the content, no shell), which a Sheet host
shows as its body and a Panel or Sheet host shows as a pushed view. In a pushed host the view's own
foot replaces the host foot, and Cancel there is Back.

**When not to use:**
- Creating from scratch: use NewItemSheet.
- Exporting: the Export recipe in [sheet-recipes.md](sheet-recipes.md).
- Removing a pack in use: the Remove recipe.
- A file needing no choice (an image dropped on a page): insert it and show a Toast.

## 2. Anatomy

```
 +----------------------------------------------+
 | Import PDF                           [Close]  |
 |----------------------------------------------|
 | Banner (status)                                |
 | [thumb]  Name  [ Chemistry notes_________ ]   |  file row
 |          12 MB, 48 pages                      |
 | Location  Library                          >  |
 | Pages     ( All | Range )  [ 1 to 5, 8 ]      |
 | Not imported (2)                               |  group, always open
 |   scan.pdf   Not a PDF                         |
 |   big.pdf    Failed          [Retry]           |
 |----------------------------------------------|
 |                    [Cancel]  [  Import 4  ]   |
 +----------------------------------------------+
```

Parts: status Banner, review rows grouped by kind, Not imported group, optional secondary text
button, foot.

## 3. Variants and sizes

`kind`: `pdf` | `notebook` | `image` | `pack` | `extension`. Sheet `size`: `default`; `wide` for `pack`.

| Kind | Body | Primary | Secondary |
|---|---|---|---|
| `pdf`, one | Name, Location, Pages | Import | none |
| `pdf`, many | a row per file with its own Name, one shared Location, no range | Import 6 | none |
| `notebook`, `image` | Name, Location, page count | Import | none |
| `pack` | content grid by type, status, theme results | Install | none |
| `extension` | the consent body (below) | Install | Install turned off |

**Lifecycle matrix (pack and extension).**

| Situation | Primary | Secondary | Banner |
|---|---|---|---|
| New | Install | none | none |
| Update available | Update | none | "3 items in your wheel changed" with Keep mine, Use new |
| Already installed | Open | none | `info` "Already installed" |
| Older than installed | Install older | Cancel stays | `warn` "Older than the one you have" |
| Removed earlier | Restore | none | `info` |
| Same name, other publisher | Install as a separate pack | none | `warn` |
| Refused | Close only | none | `danger`, one sentence |

**Mixed drops.** One Sheet, rows grouped by kind (PDFs, notebooks, images, packs), one section each
with its own options. The primary counts only checked, valid rows ("Import 7"). An extension is
never mixed in: it is reviewed alone, after the others, in its own Sheet.

**Extension consent body.** Top to bottom:
1. Header: the extension name, the publisher and the signature state ("Signed by Ana Ruiz",
   "Signed by Grapho", or "Not signed", with Source: From Grapho, Installed by you).
2. What it adds, every slot, in plain words ("A side panel, 2 commands, a block").
3. Permissions grouped by tier, high risk first: Outside the app, Your library, This document.
   Each line has its plain phrase and "needed by" labels. `storage.own` is not shown.
4. The combined sentence from `content-system/trust.md`: "It can read this document and send
   information to cards.example.com." plus "Only allow sites you trust."
5. On an update, only the new permissions carry New, with "Keeps 2 others".
Foot: Cancel, primary **Install** (**Update** on an update), secondary **Install turned off**
(**Update later**), which grants nothing and leaves the extension in Needs permission. Default focus
is Cancel. The primary is enabled but is never reached by Enter or Ctrl+Enter from the body; it
needs focus. Compact: the secondary sits at the end of the body, above the foot.

## 4. States

| State | Change |
|---|---|
| reading | opens at once: file rows as Skeleton, Cancel only, primary disabled "Reading the file". Past 1000 ms an indeterminate line shows in the Sheet |
| password | a TextField "Password" replaces the row; wrong password keeps it with an error |
| unreadable | the row moves to Not imported with the reason |
| default | rows filled, primary enabled |
| selected | files and pack items are checkable rows; the primary count follows the checked rows |
| disabled primary | nothing valid checked; hint "Choose at least one" |
| loading | on press the primary shows inline busy, the Sheet closes, a Progress item runs after 1000 ms |
| empty | no readable files: EmptyState `sm` "Nothing to import", Close |
| error | Banner `danger` for a whole-sheet failure; per-item reasons stay in the rows |
| status | Banner `info` (scanned: "No text found. Search will not find words in it."), `warn` (older) |
| queued | a share intent with the pen down waits, then opens with a Toast "1 file to review" |

## 5. Content rules

- Names are one line with end ellipsis; the field scrolls. Reasons are plain, up to two lines, and
  wrap in German and Norwegian.
- One file: the single form. Six: a row each. 450 hidden files: the list is always virtualised; the
  count of valid rows, and a Filter (All, Will import, Not imported), sit above it.
- **Not imported** group: every refused item (with the reason) and every failed item (with Retry)
  is listed, always open, never collapsed into "and N more". Imported items may collapse to a count.
- Pack with 500 items: all are included; the checkable rows are removed and the primary is all or
  nothing ("Install"). Refused theme results and unsupported items list in Not imported.

## 6. Overflow rule

One primary, Cancel, one secondary. Further detail uses a pushed view (Items, Details), counted
within the Sheet's three levels.

## 7. Import model, failure and cancel

- **Per file, all or nothing.** A file is written fully or not at all; a failed file leaves no
  partial notebook.
- **Result.** The Progress item ends in a Toast: "Imported 4 of 6, 2 failed" with View. The failed
  items are logged in the activity list under Notices with Retry, which keeps the file handle until
  the app closes or the user dismisses it.
- **Cancel during import** keeps what finished, says how many ("3 imported, 3 stopped") in a Toast
  with Undo, and removes any half written file.
- **Dirty.** A half read import is clean (no choice made). Edited names, unchecked rows or a changed
  range make it dirty; Cancel then asks "Discard this import?".

## 8. Range field

Two inputs "from" and "to" that also accept a list ("1 to 5, 8"). Errors are inline ("Pages go to
48"). They stack at compact. Empty means All. The primary is disabled while the range is invalid.

## 9. Extension slots and pack content

None extend the Sheet. This is a place that shows pack state: **Already installed**, **Off**,
**Hidden**, **Kept from <pack>**, **Not installed**, in the pack and extension kinds only. Refused
and failed rows are not hoverable or selectable and use `text/muted` for the reason with
`state/danger/fg` for the status word.

## 10. Input

- Tab through fields; Enter runs the primary where the Sheet rules allow (never for `extension`).
- Drag and drop onto the library or an open notebook opens this Sheet; a drop never inserts a file
  as an image. Files permission is asked here, in context.
- Software keyboard and 200% text: the Sheet compact layout; Range stacks; the field scrolls in.
- Roles: the Sheet's `dialog`; checkable rows are `checkbox`; the Banner is `status`, a failure is
  `alert`.

## 11. Tokens

| Token | Use |
|---|---|
| all of Sheet | shell |
| `size/row/list`, `space/lg`, `space/md` | rows, gaps |
| `radius/item`, `elevation/thumb` | thumbnails |
| `state/info/*`, `state/warning/*`, `state/danger/fg` | status and refusals |
| `type/body`, `type/caption`, `text/muted` | text |

## 12. Do and Don't

- Do list every refused and failed item with a reason, always visible.
- Do put the count in the primary ("Import 6").
- Do make Cancel the default focus on the extension consent.
- Don't open one Sheet per file, or mix an extension into a drop.
- Don't use a negated label as a button; "Install turned off" is a state, not a refusal.
- Don't run anything before the primary.

## 13. Traces to

Flows 04, 08, 09, 13, 14. Replaces PackImportSheet; absorbs ConsentView.
