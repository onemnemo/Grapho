# 04 Import and annotate PDFs

## Goal

Bring in one PDF or a course's worth, including a 300 page file, write on it at once, move around it
and export it with annotations, with a designed outcome for every failure.

Counting rule: see 01. Start at the library.

## Entry points

- New (Menu), Import group, PDF; Ctrl+O; the palette.
- Drag files onto the window (desktop, iPad): `DropZone` "Drop to import" covers the window. A PDF
  dropped on an open notebook still opens the import Sheet; it is never placed as an image.
- The platform share target and "Open with".
- Inside a document, Pages tab, "Insert pages from a PDF": same Sheet, target is this document.

## Steps

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | New, PDF | Menu | `Menu` |
| 2 | Choose one or many files | system picker | files permission asked here, in context. Denied: the Menu row shows "Allow file access" and the system settings path; nothing is lost |
| 3 | Review | Sheet | `ImportReviewSheet`, PDF variant. One file: `TextField` Name (from the file name), `ListRow` Location, `SegmentedControl` Pages (All, Range) with range `TextField`s, `Banner` for text status. Many files: a `ListRow` per file with its own Name and size, one shared Location, "Import 6" primary, no range |
| 4 | Import runs | Progress item (after 1000 ms) | `ProgressItem` "Importing 3 of 6" with Cancel |
| 5 | The first document opens when its first page is ready, writable | Document | see 13 |
| 6 | Pages prepare in the background | Progress item (over 1000 ms) | `ProgressItem` "Preparing pages, 120 of 300"; Pages tab `Skeleton` tiles |
| 7 | Annotate | Document | pen, eraser, lasso as in 03; the one Highlighter with "Snap to text" on (text PDFs): drag over text, it snaps to words and lines. It is a wheel and slot tool, not a Note bar item |
| 8 | Move around | Document | 11 |
| 9 | Export or Share | Sheet | More, Export: `ExportSheet` (format PDF flattened, Images, Grapho file; pages all or range). Share is Export to a chosen format handed to the system sheet (14) |

PDFs open in edit mode every time. Page operations (rotate, delete, duplicate, reorder, insert
blank, insert from another PDF, extract) are in the Pages tab and its Menu, all undoable, and
change only the Grapho page list; the original bytes stay unchanged until Export. For an encrypted
PDF the stored copy stays encrypted and the document asks for the password when opened on a new
device.

## Tap counts (expanded, then compact)

| Task | Taps |
| --- | --- |
| One PDF to page 1 | 4 (New, PDF, choose file, Import), the same count, with name and location set; compact 4 |
| Six PDFs | 9 (New, PDF, six file taps in one picker, Import) |
| Import by drag | 1 gesture plus Import = 2 |
| Highlight a line of text | 1 (Highlighter slot) plus the drag; first time: 4 to put it in a slot |
| Export annotated PDF | 3 (More, Export, Export); mid-writing 4 (bar reveal) |

## States

| State | What the user sees |
|---|---|
| Reading the file | The Sheet opens in Reading state: file row `Skeleton`, Cancel only |
| Normal | "42 pages, 6 MB. Text found. Search and Snap to text will work." |
| 300 pages | "300 pages, 48 MB. It opens right away; pages finish preparing in the background." Pages render on demand around the viewport with placeholders of the right size |
| Scanned (no text) | `Banner` info in the Sheet: "No text found. Search will not find words in it. Use the highlighter pen to mark it." Snap to text is absent; the Tool settings tab carries the same hint |
| Password protected | One `TextField` (password) and "Unlock"; wrong: "That password did not work." |
| Corrupt or not a PDF | Inline refusal "This file is damaged and cannot be opened." Close. In a multi-file list the bad row is marked and the rest import |
| Too large for the device | "This PDF is too large to import at once (4,200 pages)." Primary "Import pages 1 to 1,000" (Range prefilled); Close |
| File moved or deleted later | The document holds its own copy; nothing refers back |
| Cancelled during copy | Nothing is created; Toast "Import cancelled." |
| Cancelled while preparing pages | The document stays; thumbnails fill when pages are viewed |
| Import fails midway | Toast "Could not import Lecture slides. There is not enough space." Retry; no half-made document |
| Drag of 6 files | One Sheet, not six |
| Export of 300 pages | `ProgressItem` with Cancel, Toast "Saved Lecture slides.pdf" with Share |
| Offline | Not applicable |
| Very large library | The new items appear in the folder; the list stays virtualised |

## Exits

| Exit | Result |
|---|---|
| Import | Sheet closes; first document opens (many: the library, items highlighted, Toast "6 PDFs imported" with Undo) |
| Cancel, Escape | Closes; nothing created |
| Scrim tap | Closes only if nothing changed |
| Failure | Inline error or Toast with Retry |
| Back from a document | The place it was imported from |

## Wireframes

Review Sheet, one file (Expanded):

```
+------------------------------------------------+
| Import PDF                                [x]  |
+------------------------------------------------+
|  [thumb]  Lecture slides.pdf  300 pages, 48 MB |
|  Name      [ Lecture slides              ]     |
|  Location  Semester 1                       >  |
|  Pages     (All) (Range)                       |
|  (i) Text found. Search and Snap to text work. |
+------------------------------------------------+
|                         [ Cancel ] [ Import ]  |
+------------------------------------------------+
```

Many files:

```
+------------------------------------------------+
| Import 6 PDFs                             [x]  |
|  Location  Semester 1                       >  |
|  Week 1.pdf  [ Week 1       ]  4 MB            |
|  Week 2.pdf  [ Week 2       ]  6 MB            |
|  broken.pdf  Cannot be opened (skipped)        |
+------------------------------------------------+
|                         [ Cancel ] [ Import 5 ]|
+------------------------------------------------+
```

PDF document, Expanded, right-handed (bar recalled; Pages tab filling):

```
+------------------------------------------------------------+
| < Lecture slides   1 of 300                         [More] |
+----+-----------+-------------------------------------------+
| T  | Pages     |        +--------------------+             |
| S1 | [01][02]  |        | slide 1  + ink     |             |
| S2 | [03][..]  |        +--------------------+             |
| Un | skeletons |                                           |
+----+-----------+-------------------------------------------+
 Progress item (bottom left): Preparing pages, 120 of 300 [Cancel]
```

Compact: bottom Sheet; Pages tab is the bottom panel with 3 thumbnail columns; Progress items stack
above the dock on the off-hand side. Medium: panel overlays the off-hand edge, 320 dp.

## Access

Keyboard: Ctrl+O; in the Sheet Tab through fields, Enter imports. Screen reader: progress is
announced every 25%; the Skeleton tiles are hidden from the tree; thumbnails are named "Page 12".
Focus returns to the New button, or to the imported item in the library.
