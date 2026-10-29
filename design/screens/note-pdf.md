# Screen: Note, PDF (300 pages, annotation, page navigation)

## 1. Purpose

Write on an imported PDF as on paper, including a 300 page course file, and know where you are, get to
any page in seconds, keep your zoom, and export with annotations. Chrome, dock, wheel and panel are the
ones in note-notebook.md; this file states what a PDF changes and adds the import and export screens
that lead to it.

## 2. Flows served

04 import and annotate (main), 11 navigate a long document (main), 13 open (progressive open, damaged,
newer version), 03 (tools), 14 (Export, Share).

## 3. What differs from a notebook

| Aspect | PDF |
|---|---|
| Pages | PDF pages plus inserted blank pages; page operations change only the Grapho page list; original bytes stay untouched until Export |
| Highlighter | The one Highlighter tool with the option "Snap to text", on by default when the PDF has a text layer; drag over text, it snaps to words and lines. A scanned PDF has no Snap to text; the Tool settings tab shows a hint on using the highlighter pen freehand. There is no separate text-highlight tool in the NoteBar |
| Pages tab | Thumbnails are a background job (`thumbs.build`); extra actions Rotate, Extract pages, Insert pages from another PDF |
| Search | FindBar covers PDF text and handwriting; FindMarks on both |
| Export | Flattened PDF, Images, or Grapho file; page range |
| Opening | Edit mode every time; no "read mode" |

## 4. Layout and regions

Same regions as the notebook. Differences that matter at 300 pages: the PageScrubber exists (over 20
pages), the Pages tab is virtualised, and ProgressItems appear while pages prepare.

### Expanded, R, bar returned, Pages tab filling

```
+------------------------------------------------------------+
| < Lecture slides   [ 12 of 300 ]                     [More] |
+----+-----------+-------------------------------------------+
| T  | Pages     |        +--------------------+             |
| S1 | [01][02]  |S       | slide 12 + ink     |             |
| S2 | [03][..]  |S       +--------------------+             |
| Un | skeletons |S                                           |
| Re |           |                                           |
+----+-----------+-------------------------------------------+
 ProgressItem (bottom left): Preparing pages, 120 of 300  [Cancel]
```

L: the dock, panel, scrubber, ProgressItems and Toasts mirror to the right. The wheel's rings open to
the off-hand side of the pen tip.

### Scrolling (bar receded): chip and scrubber

```
+------------------------------------------------------------+
|                   [ 12 of 300 ] chip, top centre, fades 1.5 s|
|S handle 56 high          +--------------------+              |
|S  bubble: p.120 + thumb  |  page              |              |
+------------------------------------------------------------+
```

The scroll CanvasChip shows only while the bar is receded (otherwise the NoteBar indicator says it).
The chip and scrubber fade 1.5 s after use. Chips sit below the full band stack, whether or not
each band is showing, so nothing shifts when the bar returns.

### Medium and Compact

Medium: the Pages tab overlays the off-hand edge at 320 wide; the page can be panned clear. Compact:
the Pages tab is the bottom panel with 3 thumbnail columns at Peek or half height; the page above
stays live; ProgressItems stack above the dock strip on the off-hand side; the scrubber is a 44 or 48 wide
handle on the off-hand edge (`size/hit/touch`). Short: panel `over`, dock a corner strip.

## 5. Components by region

Everything in note-notebook.md section 4, with these specifics.

| Region | Component | Notes |
|---|---|---|
| NoteBar | Button (page indicator "12 of 300") | Tap or Ctrl+Alt+G opens the Go to page Popover |
| Go to page | Popover recipe: TextField (numeric, 1 to 300, Enter goes), IconButton previous and next, ListRow for the PDF outline when present | Applies on Enter, no Done; the field has focus; on touch the keyboard raises with it |
| Scrubber | PageScrubber | Handle only, off-hand edge. A touch not starting on the handle does nothing in pen-only mode. Fine mode; bubble shows page number and a thumbnail once it exists. Adjustable value role: swipe changes the page |
| Chip | CanvasChip `kind` scroll, zoom | Zoom chip "150%" is a button "Reset zoom" (Fit width) |
| Pages tab | Picker slot PickerTile `pageThumbnail` in a ReorderList; Skeleton tiles; SelectionBar `panel` | Current page marked. Tap goes to the page and the panel stays open. Reorder by drag; Menu rows Move earlier, Move later, Move to page |
| Progress | ProgressItem | "Preparing pages, 120 of 300" after 1000 ms; Cancel stops preparing; the document stays |
| Highlighter | ToolWheel segment, QuickSlot | Option "Snap to text" in the Tool settings tab and the pen segment |
| Export | Sheet recipe ExportSheet | Section 7 |
| Banner | Banner | Scanned, damaged, newer version, storage |

## 6. Zoom

Pinch zooms around the pinch centre, 25% to 800%, snap hints at 100% and Fit. Commands Fit width
(Ctrl+1), Fit page (Ctrl+2), 100% (Ctrl+0), Zoom in and out. The zoom chip shows while zooming and for
1.5 s. Zoom and position are remembered per document and restored on reopen. Finger double tap is not
used for zoom. Ink and PDF text do not scale with the system text size; they zoom with the page.

## 7. Import and export screens (Sheets over the library or document)

### Import review, one file (Expanded; Compact is a bottom Sheet near full height)

```
+------------------------------------------------+
| Import PDF                                [x]  |
+------------------------------------------------+
|  [thumb]  Name  [ Lecture slides          ]    |
|           300 pages, 48 MB                     |
|  Location   Semester 1                      >  |
|  Pages      (All) (Range)                      |
|  (i) Text found. Search and Snap to text work. |
+------------------------------------------------+
|                         [ Cancel ] [ Import ]  |
+------------------------------------------------+
```

Components: Sheet with ImportReviewSheet `pdf`, TextField (Name), ListRow (Location, pushes
FolderChooser), SegmentedControl (All, Range) with two TextField for the range, Banner, Button.
Many files: one Sheet, a row per file, the bad file marked "Cannot be opened (skipped)" and never
collapsed, primary "Import 5".

| Import state | What the user sees |
|---|---|
| Reading | The file row is a Skeleton; Cancel only |
| 300 pages | "300 pages, 48 MB. It opens right away; pages finish preparing in the background." |
| Scanned | Banner neutral: "No text found. Search will not find words in it. Use the highlighter pen to mark it." |
| Password | TextField (password) and "Unlock"; wrong: "That password did not work." |
| Corrupt | Inline refusal "This file is damaged and cannot be opened." Close |
| Too large | "This PDF is too large to import at once (4,200 pages)." Primary "Import pages 1 to 1,000" (Range prefilled), Close |
| Failure | Toast "Could not import Lecture slides. There is not enough space." Retry; no half-made document |
| Cancel | Nothing created; Toast "Import cancelled." |

After Import the first page opens writable as soon as it is ready (see 13 progressive open).

Export (More, Export): ExportSheet with a Select format (PDF flattened, Images, Grapho file), pages
All or Range, Export. A 300 page export is a ProgressItem with Cancel; it ends as a Toast "Saved
Lecture slides.pdf" with the action "Share". An encrypted PDF stays encrypted in the stored copy and
asks for the password on a new device.

## 8. States

| State | What changes |
|---|---|
| Opening, normal | Frame at once; the last viewed page first, writable; pages render on demand (current, two ahead, one behind) with placeholders of the right size so scroll length is stable |
| 1 page | "1 of 1", no scrubber, Delete page disabled with the reason |
| Up to 20 pages | Indicator and Pages tab, no scrubber |
| 300 pages | Indicator, scrubber, virtualised Pages tab, ProgressItem for thumbnails |
| 10,000 pages | Lists virtualise; Go to page takes a number; scrubber in fine mode |
| Thumbnails not built | Skeleton tiles; bubble shows the number only |
| Out of range | Typed 400 of 300: the field shows "1 to 300" and Enter is disabled; a deep link clamps silently |
| Page fails to render | A framed placeholder "Could not show this page." Retry; the rest work |
| Damaged | Opens read-only with what can be read; Banner danger "3 pages could not be read. This PDF is open read-only." Open read-only, Export copy; "3 pages" opens the DamageDetailsSheet recipe |
| Newer version | Banner warn "Saved by a newer version of Grapho. Update to edit." |
| Offline | Not applicable; a document held on this device opens |
| Palm, pen-only | As the notebook |
| Long German | "Seite 12 von 300" fits the indicator; the NoteBar drops pinned items first |
| 200% text | NoteBar `core`; Go to page Popover grows; the Pages tab drops to 2 columns |

## 9. Behaviour on resize and split screen

Position, zoom, current page and the panel tab survive every class change. Crossing 840 turns the
beside panel into an overlay; crossing 600 turns it into the bottom panel at Peek. The Go to page
Popover closes if its anchor leaves view. A running ProgressItem moves between the toast stack and the
activity list and keeps its Cancel.

## 10. Keyboard and focus order

Page Up and Down, Ctrl+Home and End, Ctrl+Alt+G, Ctrl+1, Ctrl+2, Ctrl+0; with the indicator focused
Left and Right go to the previous and next page. Pages tab: arrows move thumbnails, Enter goes to the
page, Space toggles selection, Alt+Up and Alt+Down move the selected page, Delete deletes
(undoable). Focus returns to the indicator after Go to page. Screen reader: "Page 12 of 300"; Skeleton
tiles hidden; thumbnails named "Page 12"; progress announced every 25 percent.
