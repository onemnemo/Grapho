# 02 Create a notebook, canvas or folder

## Goal

Make a new notebook, canvas or folder with the right look in 3 to 6 taps, using a cover, template
or style from the user's library or from packs, and land in it ready to write. The default path is a
one-glance Sheet; the Pickers open only when asked.

Counting rule: see 01. Start at the library.

## Entry points

- Top bar **New** (Menu): Notebook, Canvas, Folder, then Import (Notebook file, PDF, Image, Pack).
- The empty library primary button; Ctrl+N, Ctrl+Shift+N; the palette.
- Inside a folder, New preselects that folder.
- Edit later: the item's Menu (the ... button, right click or the Menu key; never long press, see
  SYSTEM-MAP 4.7.1), "Edit look" (labelled Edit cover, Edit style or Edit background by type).

## Surface

One `NewItemSheet` with kinds notebook, canvas and folder. New and edit modes.

Create mode rows, top to bottom: preview, `TextField` Name (the one naming rule: a Name field in the
surface that creates), `Select` Size (notebook only), then three `ListRow`s that show the current
choice and push a view inside the Sheet: **Cover** (or Style, or Canvas background), **Template**
(notebook only), **Location**. Foot: Cancel, Create.

Edit mode shows only the one look row and the preview. Primary "Done". The choice is staged:
Done commits, Cancel and Back revert. A Toast "Cover changed" with Undo follows Done.

Pushed views (Back returns to the Sheet):

- **Look Picker** (`Picker`, pick mode): grid, Filter, Sort, `SourceFilter`, footer Get more, Import,
  New. A tap on an item applies it and pops back to the Sheet.
- **Get more**: the catalogue view (08).
- **Location**: `FolderChooser` with Filter, "Library" first.
- **New look**: opens Studio. The Sheet closes with its fields kept as a draft; Studio opens; Done
  returns to the Sheet with the new look selected (SYSTEM-MAP section 3, Studio opened from a
  Sheet). Cancel or Back asks once if anything changed, then returns with the draft intact.

Canvas: same Sheet with the canvas preset Picker, no Size or Template. Folder: Name, Style row,
Location.

## Tap counts (library, expanded and compact differ only where noted)

| Task | Taps |
| --- | --- |
| Notebook, defaults | 3 (New, Notebook, Create); from the empty library 2 |
| Notebook, typed name | 4 (New, Notebook, Name field, Create); on a hardware keyboard Name has focus: 3 |
| Notebook with a pack cover | 6 (New, Notebook, Cover row, tile, Name field, Create); compact +1 if Get more is needed first |
| Notebook into a folder | 3 inside; elsewhere +2 (Location row, folder) |
| Canvas | 3 |
| Folder, named | 4 (New, Folder, Name field, Create) |
| Change a cover | 4 (..., Edit cover, tile, Done) |
| Make a new cover | 5 plus drawing and naming: ..., Edit cover, New, [draw], Studio Done, Done; naming is optional, +1 to edit the title field |

## States

| State | What the user sees |
|---|---|
| First use | Last-used values are defaults; Create works at once |
| Yours empty | `EmptyState` sm: "Nothing of yours yet." Buttons "New cover", "Get more" |
| Filtered, no match | "No covers match 'blue'." Clear, no mascot |
| Pack off or removed | Existing notebooks keep their cover; the pack's items leave the Picker |
| Loading covers | `Skeleton` tiles only after 300 ms, then at least 300 ms |
| 500 covers | Virtualised grid, Filter first; the source filter is a dropdown above six sources |
| Offline | Get more shows its offline Banner inside the pushed view |
| Error: cannot write | Inline foot: "Could not create the notebook. There is not enough space." Retry |
| Empty name | Create enabled; the name becomes "Untitled" |
| Cancel after changes | Dialog "Discard this notebook?" only if something changed |
| Keyboard up on compact | The Sheet scrolls; the foot buttons stay pinned above the keyboard |

## Exits

| Exit | Result |
|---|---|
| Create | Notebook or canvas opens; folder stays in the library, selected |
| Done (edit) | Look changes, Toast with Undo |
| Cancel, Escape | Closes; draft or staged choice discarded |
| Scrim tap | Closes only if nothing changed |
| Back in a pushed view | Returns to the Sheet |
| Failure | Stays with an inline error |

## Wireframes

Create, Expanded (centred, 540 wide):

```
+------------------------------------------------+
| New notebook                              [x]  |
+------------------------------------------------+
|   +--------+   Name  [ Untitled           ]    |
|   | cover  |   Size  [ A4 v ]                  |
|   +--------+                                   |
|  Cover      Botanica, Leaf green            >  |
|  Template   Lined                           >  |
|  Location   Library                         >  |
+------------------------------------------------+
|                         [ Cancel ] [ Create ]  |
+------------------------------------------------+
```

Cover view pushed inside the Sheet:

```
+------------------------------------------------+
| < Cover                                        |
| [ Filter ]  Sort v  (grid)                     |
| (All)(Built in)(Yours)(Botanica 12)(Hidden)    |
| [c][c][c][c][c][c]                             |
| [c][c][c][c][c][c]                             |
| Get more   Import   New                        |
+------------------------------------------------+
```

Compact: a bottom Sheet near full height; the foot is pinned; the Picker footer is one More button.
German check: "Erstellen" and "Abbrechen" fit the foot at 360 dp; row values truncate with an
ellipsis, the label never does. Medium: the same centred Sheet.

## Access

Keyboard: Enter in Name is Create; Ctrl or Cmd+N opens the Sheet; arrows move in the Picker grid,
Enter applies. Screen reader: rows read "Cover, Botanica Leaf green, button"; pushed views announce
their title; focus returns to the row that opened them. Focus returns to the New button when the
Sheet closes.
