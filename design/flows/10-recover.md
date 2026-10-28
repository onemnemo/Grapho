# 10 Recover from a mistake

## Goal

Every mistake has one predictable way back, by consequence (SYSTEM-MAP 4.2), the same in every
shell: reversible acts happen at once and can be undone; only the irreversible asks.

Counting rule: see 01.

## The three rules

| Rule | Covers | Surface |
|---|---|---|
| Document undo stack | strokes, selections, page add, delete, reorder, rotate, layer changes | `ToolDock` Undo and Redo, Ctrl+Z |
| Library journal | delete, move, rename, tag, pack remove (Toast with Undo); star and pack on or off (journaled, no Toast, their switch undoes them) | `Toast` with Undo; Ctrl+Z in the library; Top bar overflow row "Undo <action>"; the Activity `Popover` |
| Irreversible | empty Trash, delete forever, erasing an extension's stored data | `Dialog` with a danger button naming verb and count: "Delete 12 forever" |

There is no hold-to-confirm. Trash keeps items 30 days; Restore returns to the original folder or
to Home if it is gone. The journal keeps 100 actions per device.

## Entry points

The Toast after an action; the dock; Ctrl+Z; the Top bar overflow row "Undo ..."; the Activity
Popover (Top bar overflow, palette); the Trash place (`grapho://library/trash`); Settings,
Appearance; Settings, Extensions, safe mode.

## Recovery paths

| Mistake | Way back | Cost |
|---|---|---|
| Wrong stroke, selection delete, deleted page | dock Undo | 1 |
| Deleted a notebook or folder | Toast Undo; later Trash, Restore | 1; later 3 (Trash, select, Restore) |
| Wrong move, rename, tag | Toast Undo; Ctrl+Z; overflow row; Activity | 1 |
| Two toasts, older Undo | the older moves to Activity with Undo intact | 2 (overflow, Undo) |
| Starred or turned a pack off by mistake | tap the same switch | 1 |
| Removed a pack | Toast Undo; Trash, Restore | 1; later 3 |
| Removed an extension | Toast Undo; Trash, Restore | 1; later 3 |
| Wrong theme | pick another; or Banner "Use the default theme" | 1 to 4 |
| App hard to read | the contrast guard prevents it; Settings, Appearance, "Use the default theme"; keyboard: Ctrl+K "Use the default theme"; safe mode | 4 touch |
| A pack or extension breaks the app | Safe mode (09) | automatic |
| Wrong cover edit | Cancel in the Sheet or Studio; or Undo in the Toast after Done | 1 |
| Lost device | Sign in to restore (14) | see 14 |
| Emptied Trash by mistake | not recoverable; the Dialog said so | n/a |

Removing a pack follows 4.2: with items in use the `RemoveSheet` lists what stays as your copies,
primary "Remove"; with nothing in use it is removed at once. Both end in the Toast with Undo.

## Steps: delete, then change your mind

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Select, Delete (bar or Delete key) | Selection bar | `SelectionBar` |
| 2 | Items leave the place at once; Toast "Moved 3 to Trash" with Undo | Toast | `Toast` (stays at least 10 s with a screen reader) |
| 3 | Undo from the Toast, Ctrl+Z, the overflow row or Activity | Toast, Popover | `Toast`, Activity `Popover` (running jobs and the last 10 actions, Undo per row; older ones by Ctrl+Z) |
| 4 | Trash from the Sidebar | Library place | `Sidebar`, `ItemTile`, "removes in 12 days" |
| 5 | Select, Restore (or Delete forever) | Selection bar | `SelectionBar` |
| 6 | Delete forever or Empty Trash | Dialog | `Dialog` "Delete 3 forever? This cannot be undone." Cancel has default focus; danger `Button` "Delete 3 forever" |

A stale Undo (the target changed since, for example a moved item later deleted) shows "Cannot undo:
Week 3 changed since." with a link to the item. When Trash holds items due within 3 days, the
library shows a `Banner` "2 items leave the Trash in 3 days." with View.

## Tap counts

| Task | Taps |
| --- | --- |
| Undo a wrong stroke | 1 |
| Undo a delete | 1 in the Toast; later 3 |
| Undo a move after other toasts | 2 (Top bar overflow, Undo) |
| Restore a removed pack | 3 (Trash, select the pack, Restore) |
| Reset a bad theme | 1 (Banner), or 4 (Settings, Appearance, Theme row, Default) |

## States

| State | What the user sees |
|---|---|
| Trash empty | `EmptyState` sm "Nothing in the Trash. Deleted items stay here for 30 days." |
| Trash with 1 or 500 items | Virtualised, by removal date; filter Notebooks, Packs, Extensions; days left on each |
| Journal passed 100 | Activity shows "Too old to undo"; Trash still works |
| Restore target folder gone | Restores to Home; Toast "Restored to Library" |
| Delete forever fails | Inline in the Dialog "Could not delete. Try again."; nothing removed |
| Pen down when a Toast arrives | Off-hand side, never over the writing centre, above the Selection bar |
| Dialog open, Toast arrives | The Toast waits |
| Offline | Not applicable |

## Exits

Undo and Restore bring the item back and the Toast or row confirms. Cancel on the Dialog changes
nothing. Escape closes the topmost thing first (SYSTEM-MAP order). Back leaves Trash for where it
was opened from.

## Wireframes

Trash, Expanded:

```
+-----------+------------------------------------------------+
| Home      | Trash                [Search][Empty Trash][Filter]|
| Trash  .  +------------------------------------------------+
|           |  [x] Week 3     removes in 12 days              |
|           |  [x] Old PDF    removes in 29 days              |
|           |  [ ] Botanica (pack)  removes in 5 days         |
|           |  +--------------------------------------------+ |
|           |  | 2 selected  [Restore][Delete forever][..]  | |
+-----------+------------------------------------------------+
```

Dialog and Activity Popover:

```
+------------------------------------+   +-----------------------------+
| Delete 2 forever?                  |   | Activity                    |
| Week 3 and Old PDF. This cannot be |   | Moved 3 to Semester 1 [Undo]|
| undone.                            |   | Deleted Week 3        [Undo]|
|      [ Cancel ] [ Delete 2 forever]|   | Importing slides 60% [Stop] |
+------------------------------------+   +-----------------------------+
```

Compact: Trash is a place from the drawer; the bar floats above the system bar; Dialogs are small
and centred; the Toast sits bottom left (right for left-handed). German check: the danger button
stays one line because it names the verb and count only ("2 endgueltig loeschen").

## Access

Dialog default focus is Cancel; Escape cancels. A Toast with Undo holds at least 10 s when a screen
reader is on and the same Undo is a Menu row ("Undo move") in the Top bar overflow. Restore
announces "Week 3 restored to Semester 1". Focus returns to the control that opened the Dialog.
