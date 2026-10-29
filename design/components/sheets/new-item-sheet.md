# NewItemSheet

## 1. Purpose

A Sheet that creates or edits one library item: a notebook, a canvas or a folder. It shows the
choice at a glance (preview, Name, current look) and pushes a Picker only when the user asks.

**When not to use:**
- Reviewing a file, pack or extension before it is added: use ImportReviewSheet.
- Renaming one item in place: a rename TextField in the item's own row or tile, not a Sheet.
- Designing a new look: use StudioShell (the Sheet closes with its draft kept, then returns).
- Moving items: a Sheet with a FolderChooser body (the MoveToSheet recipe of Sheet).

## 2. Anatomy

```
 +----------------------------------------------+  Sheet, size wide
 | New notebook                         [Close]  |
 |----------------------------------------------|
 |   [ preview: cover and page ]                 |  preview
 |   Name        [ Notebook 4____________ ]      |  TextField
 |   Size        [ A4                  v ]       |  Select (notebook only)
 |   Cover       [swatch] Paper blue         >   |  ListRow, pushes a Picker
 |   Template    [swatch] Plain              >   |  ListRow (notebook only)
 |   Options                                 >   |  ListRow, the home for new settings
 |   Location    Library                     >   |  ListRow, pushes FolderChooser
 |----------------------------------------------|
 |                       [Cancel]  [  Create  ]  |
 +----------------------------------------------+
```

Parts: Sheet shell, preview, Name field, Size select, look rows, Location row, pushed views, foot.

## 3. Variants and sizes

`kind`: `notebook` | `canvas` | `folder`. `mode`: `create` | `edit`. Sheet `size`: `wide`, so the pushed
Picker fits without resizing the container.

| Kind, mode | Rows | Primary |
|---|---|---|
| notebook, create | preview, Name, Size, Cover, Template, Location | Create |
| canvas, create | preview, Name, Canvas background (preset Picker), Location | Create |
| folder, create | Name, Style, Location | Create |
| any, edit | preview and the one look row, labelled Edit cover, Edit style or Edit background | Done |

- Rows are ListRows at `size/row/list`; the current choice shows as a swatch plus its name.
- Defaults make Create valid at once (a generated name, the last used cover), so the shortest path
  is New, then Create.

## 4. States

| State | Change |
|---|---|
| default | Sheet default; Name focused, text selected |
| hover, focus-visible, active | per ListRow, TextField, Select |
| selected | the staged choice shows on its row and in the preview at once |
| disabled | Create disabled when Name is empty or too long, with the reason "Name the notebook first" |
| loading | Create shows inline busy; the preview shows a Skeleton while a pack cover loads |
| empty | a Picker view with no items shows its EmptyState; the Sheet itself is never empty |
| hint | Name in use: a neutral hint under the field, "Another notebook has this name", no error styling; Create stays enabled (names are not unique) |
| edit | opens with the Picker as the Sheet's root view; Done commits and a Toast "Cover changed" with Undo follows; Cancel discards; Back inside is not a revert |
| kept look | a look whose pack was removed shows its kept copy's name only; pack state appears only inside the Picker |

## 5. Content rules

- Name: one line, up to 120 characters; very long names ellipsise at the end in rows and the
  preview, never in the field (it scrolls). Empty name is invalid. German and Norwegian rows keep the
  label above the value on compact so the value never loses width.
- 1 item: a Picker with one look still opens. 500 items: the Picker virtualises; the Sheet is
  unchanged.
- Location with a very deep path shows the last folder name and the parent, "Chemistry, in Year 2".

## 6. Overflow rule

Rows are fixed per kind. A new setting goes in the **Options** row, which pushes one view (for
example paper size units, page count). Pushed levels: Picker, Get more, pack review, within the
Sheet's three.

## 7. Extension slots

None on the Sheet. Extension-contributed covers, templates and styles appear as Picker items from
their pack, with the Picker's own rules.

## 8. Pack content

Shown only in the pushed Picker (source filter, Hidden entry), not in the Sheet's rows. A kept copy
reads **Kept from <pack>** on its row. The row of a look whose pack was removed shows the kept copy's name and no pack state. Names:
**Off**, **Hidden**, **Kept from <pack>**, **Not installed**.

## 9. Input

- Enter in Name runs Create. Ctrl or Cmd+N opens the Sheet. Arrows move through a Picker grid.
- Tab order: Name, Size, rows, Cancel, Create. Back in a pushed view returns to the Sheet; Escape
  and Back follow the Sheet's order.
- New look: the Sheet closes with its fields kept as a draft and Studio opens; Done returns with the
  new look selected; Cancel or Back asks once if changed, then returns with the draft intact.
- Name takes focus on open, but the IME is raised only if the last input was the keyboard. While the
  IME is up the preview collapses and the Sheet compact layout applies (Cancel moves to Close).
  Stylus: scribble in Name; a tap on a row opens the Picker.
- Dirty: Name edited, any staged look or location changed. Cancel then shows the one `unsaved`
  Dialog, "Discard changes?". A clean Sheet closes at once.
- Roles: the Sheet's `dialog`; rows are `button` with the current value as description.

## 10. Tokens

| Token | Use |
|---|---|
| all of Sheet | shell |
| `size/row/list`, `space/lg`, `space/md` | rows and gaps |
| `radius/item`, `border/subtle` | preview frame |
| `type/body`, `type/label`, `text/muted` | row label and value |
| `state/danger/fg`, `border/error` | Name error |

Row labels by kind (one noun each):

| Kind | Look row | Picker |
|---|---|---|
| notebook | Cover | cover Picker |
| canvas | Canvas background | canvas preset Picker |
| folder | Style | folder style Picker |

Depends on Picker, FolderChooser and ListRow, which share one row model.

## 11. Do and Don't

- Do start with a valid default so Create works at once.
- Do show the current choice on each look row.
- Do name the form where it is made: the Name field lives here, not in a second dialog.
- Don't open a Picker as a second Sheet; push it.
- Don't ask for a name inside Studio; the look is named where it is made.
- Don't show pack state or a Banner about packs in the rows.

## 12. Traces to

Flows 01 (first notebook), 02, 07.
