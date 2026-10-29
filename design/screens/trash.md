# Screen: Library, Trash

## 1. Purpose

Everything the user deleted, kept 30 days, with a certain way back. Trash holds notebooks, PDFs,
canvases, folders, removed packs and removed extensions as one list by removal date. It is the only
place where deletion is final, and only after a Dialog that names the verb and count.

## 2. Flows served

10 recover (main), 05 organise (delete with Undo), 08 packs (removed packs live here only), 09
extensions (removed extensions, erase stored data), 12 settings (link row "2 removed, in Trash").

## 3. Layout and regions

Regions: TopBar, Sidebar or drawer, Content (a list), SelectionBar, Toast stack, Dialog. Trash is a
list, not a grid, because each row carries "removes in N days".

### Expanded, R

```
+-----------+--------------------------------------------------------+
| Sidebar   | Trash          [Search....][Empty Trash][Select][Filter] |
| Home      +--------------------------------------------------------+
| Starred   | (i) 2 items leave the Trash in 3 days.        [ View ]  |
| Trash  .  |  [x] Week 3          Notebook     removes in 12 days    |
|           |  [x] Old PDF         PDF          removes in 29 days    |
|           |  [ ] Botanica        Pack         removes in 5 days     |
|           |  [ ] Flashcards      Extension    removes in 20 days    |
|           |  +---------------------------------------------------+ |
| [sync][gear]| 2 selected  [Restore][Delete forever][...]          | |
+-----------+--------------------------------------------------------+
```

L: Toasts bottom right; everything else is the same.

### Medium and Compact

As Expanded without the Sidebar (drawer from the Menu IconButton). Compact TopBar: Menu, "Trash",
Search icon, Empty Trash, More (Select, Filter in More). The SelectionBar shows icons with Tooltips and
the count. Dialogs are small and centred on every class.

### Short

The SelectionBar floats above the system bar with icons only; rows keep their height; the Banner
band scrolls with the list.

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Top | TopBar `place` trash | Visible: Search, Empty Trash (a labelled Button `secondary`), Select, Filter. More: Activity, Command palette, Settings. No New, so no primary in the bar |
| Start | Sidebar `kind` library, Trash row selected | |
| Banner | Banner neutral inline "2 items leave the Trash in 3 days." View (filters to those items) | Shown only when something is due within 3 days |
| Filter | Popover with Checkbox list: Notebooks, Packs, Extensions | Applies at once, no Done; active filter shows as a removable Token |
| List | ListRow `kind` item: art, name, kind, "removes in 12 days" | Virtualised, ordered by removal date; days left in each row |
| Selection | SelectionBar `placement` library, Trash set | Restore and Delete forever are the visible actions; More holds Select all |
| Dialog | Dialog `irreversible` | Title "Delete 3 forever?", body names items and "This cannot be undone.", danger Button "Delete 3 forever", `secondary` Cancel with default focus |
| Feedback | Toast ("Week 3 restored to Semester 1"), ProgressItem for a large purge, EmptyState `quiet`, Skeleton | |

Empty Trash is a Dialog too: "Delete 12 forever? This cannot be undone." with "Delete 12 forever".
There is no hold to confirm.

## 5. Rules

- Restore returns an item to its original folder, or to Home if that folder is gone, with the Toast
  "Restored to Library".
- A removed pack shows its name, "Pack" and days left. Restore brings back the pack in its old state;
  items kept as your copies stay as they are.
- A removed extension is restorable the same way. Erasing its stored data is a separate Dialog:
  "Delete the 12 decks Flashcards saved? This cannot be undone." with "Delete 12 forever". Notes keep
  their saved pictures.
- Trash expiry never runs while the app shows the Trash with a Dialog open.
- Importing a pack that is in Trash shows "You removed this on 24 Oct" with Restore in the review.

## 6. States

| State | What changes |
|---|---|
| Empty | EmptyState `quiet`: "Nothing in the Trash. Deleted items stay here for 30 days." No mascot, no action. TopBar Empty Trash and Select are not drawn |
| 1 item, 500 items | A virtualised list; the heading shows the count; Filter narrows by kind |
| Loading | Skeleton rows in the real list after 300 ms, held 300 ms |
| Error | Banner region danger "Could not open the Trash. Try again." Retry. Never reads as empty |
| Partial | Banner warn "2 items could not be read." View; the rest are listed |
| Restore target gone | Restores to Home; Toast "Restored to Library" |
| Delete forever fails | Inline in the Dialog "Could not delete. Try again."; nothing removed; the Dialog stays |
| Journal passed 100 | Activity shows "Too old to undo"; Trash still restores |
| Offline | Not applicable |
| Dialog open, Toast arrives | The Toast waits until the Dialog closes |
| Long German names | Row name wraps to two lines; "removes in 12 days" becomes "wird in 12 Tagen entfernt" on its own line; the danger button names only verb and count ("2 endgueltig loeschen") so it stays on one line |
| 200% text | Rows grow; kind and days move under the name; SelectionBar actions become icons with labels in Tooltips |

## 7. Behaviour on resize and split screen

Class changes keep the scroll, the selection and any open Dialog (a Dialog is centred on every class
and re-clamps to the window less two gutters). At 360 wide the TopBar holds Menu, title, Search icon,
Empty Trash and More. The selection and the Filter survive crossing 840.

## 8. Keyboard and focus order

Order: TopBar, Sidebar, list, SelectionBar. Arrival focus is the title "Trash". Arrows move rows,
Space toggles, Enter opens a read-only preview where the kind allows, Ctrl+A selects all, Delete on a
selection opens the forever Dialog, Ctrl+Z undoes the last journal entry (a restore is undone by
moving it back to Trash). Dialog default focus is Cancel; Escape cancels; focus returns to the button
that opened it, or to the next row if that row is gone. Restore announces "Week 3 restored to Semester
1".
