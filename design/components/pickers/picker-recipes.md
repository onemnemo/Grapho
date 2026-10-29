# Picker recipes

Configurations of the one Picker. They are not components. The Picker file owns parts, states and counts.

## 1. Manage mode (every type)

Manage mode uses the selection grammar of SYSTEM-MAP 4.7 and SelectionBar `picker`. At count 0 the bar
offers "Select all 500". Each tile has the ... button; the Menu has the same entries as the bar.

| Selection | Visible four | More |
|---|---|---|
| One Yours item | Edit, Duplicate, Rename, Delete | Export as pack, Details |
| One built-in or pack item | Edit a copy, Duplicate, Hide, Export as pack | Rename a copy, Details |
| Several, all Yours | Duplicate, Export as pack, Delete | Select all |
| Several, all built-in or pack | Duplicate, Export as pack, Hide | Select all |
| Mixed | Duplicate, Export as pack | Delete and Hide, disabled, reason "Your items and shared items are selected" |
| In Hidden | Unhide | Select all |

- Delete (`picker.delete`) removes Yours items from the library as one undoable step with a Toast
  "Deleted 3 pens", Undo. It is not `library.delete` (Move to Trash). If an item is held (a wheel slot,
  a folder style in use) the Remove sheet recipe lists the holders first.
- Edit on a pack or built-in item works on a copy in Yours; the original is untouched.
- The default item and the active theme cannot be hidden or deleted; the reason is the description.
- Inline rename opens a small Popover with a TextField under the tile (names never fit under 72 dp).
- Export as pack on a selection lists in the New pack Sheet which items cannot be shared, with reasons.
- **In a Sheet host** (no Manage slot) the item Menu is reached by long press then lift, which enters
  manage mode for the pushed Picker: Rename and Delete of Yours items are there, so a saved look can be
  managed without leaving the form.

## 2. Theme picker with live preview

Host: Settings, Appearance, `page`, pick mode, Manage shown. The Theme row pushes it; the page title
"Theme" belongs to the host.

```
< Back   Theme
[Filter______________________] [Sort v] [grid|list]
( All 9 )( Built in 3 )( Yours 1 )( Botanica 5 )( Hidden 0 )
+------------+ +------------+ +------------+
| library    | | library    | | library    |   theme preview, drawn from each theme's own tokens
| note page  | | note page  | | note page  |
+------------+ +------------+ +------------+
 Ink  Current   Paper          Dusk  Adjusted
[Get more] [Import] [New] [Manage]
```

- **Live preview.** Each tile draws a miniature library and note from that theme's resolved tokens, in
  the current Mode and Density, and repaints when either changes above. Tap applies at once; the app
  repaints and the Picker stays open. There is no Preview button and no Apply step.
- **Keep or revert.** A custom or pack theme shows the Banner "Keep this theme?" with Keep and Revert for
  10 seconds (`themes/custom-themes.md`). Revert re-marks the previous theme. Built-in themes apply
  without it.
- **Guard on tiles.** Adjusted: caption "Adjusted". Rejected: placeholder, caption "Theme unavailable",
  disabled, reason as description.
- **Pack Off.** Its themes leave the list. The active theme keeps working and stays marked in the row
  that opened this page; no pack text appears in the Picker.
- **Safe mode.** Pack themes are not listed; the safe mode Banner sits above.
- **500 themes.** Virtual grid, lazy previews, evictable cache.
- **Depth.** Appearance (root), Theme Picker (1), ThemeEditor (2). Back pops one view; focus returns to the Theme row.
- The reset command is `appearance.resetTheme` ("Use the default theme").

## 3. Colour sets: the Picker's options for type colour set

There is no ColourSetSwitcher. Colour sets are a Picker `list` of rows with the swatch strip, with these
registry-driven options:

| Option | Rule |
|---|---|
| Active set | The active set carries the word "Active" and a leading bar; the check badge means selected in manage mode only |
| Apply from the panel | In the side panel Picker the target is stated, "Pen colour for Fine liner". A tap on a swatch sets the current pen's colour and makes its set active. A tap elsewhere on the row only makes the set active. The Status line shows the colour's computed name on focus |
| Limit | `limit` 24 colours per set. A drop or Add on a full set is refused with "This set has 24 colours" |
| Drop a colour on a set | `dropAccepts` colour: a colour dragged from ColourEditor's strip onto a row adds a copy (`state/drag/target-*`); Menu route "Move colour here" |
| Order | `manual` sort key lets the user reorder Yours sets (Alt+Up and Alt+Down; drag after long press and move) |
| Wheel | The colour ring shows the active set; its Sets segment opens this Picker in the side panel |

## 4. Insert tab

```
Insert                                   (tab title is the host's)
Tabs:  Stickers | Images | Templates | Flashcards ...      type row, one Tabs component
Picker (type of the chosen tab)
  [Filter___________] [View options]
  ( All 40 )( Built in 12 )( Yours 6 )( Botanica 30 )( Hidden 0 )    its own single source filter
  tiles
  [Get more] [Import] [New v] [Manage]
```

- A Tabs row (up to four, then the Tabs fallback rule) chooses the type: Stickers, Images, Templates, then
  extension insert sources. Under it is the standard Picker with its own single source filter, so there
  are not two source rows.
- **Images** is an Insert type of the document's added images (source Yours). Its **New** slot is a Menu:
  Take photo and Choose image (`insert.photo`, Ctrl+Shift+I, opens Take photo). The footer keeps its four slots.
- Tap places the item at the centre of the visible page; drag places it. `onPick` is `stay`.
- The target line reads "Adds to page 12".
- Smallest window: the Tabs row shares the header row with Filter only when it fits, else it sits above
  as a Select (Tabs fallback) and the Picker follows the smallest-window layout in picker.md.

## 5. SourceDetail contents

A pack's page shows one section per content type the pack holds. Each section is a Picker with
`scope` set to the pack, `mode` `manage`, no source filter, its own header (type name as a section
heading owned by SourceDetail), and a "Show all" link when it holds more than one screen. The section
shows the pack's items even when the pack is **Off**, and its Hidden items. Sections collapse.

## 6. Configuration rows by host and depth

Depth counts pushed views below the host root (limit three; a Popover one).

| Job | Host | Type | Mode, onPick | Footer | Depth to the end of the longest route |
|---|---|---|---|---|---|
| Add a pen | Tool settings tab; the Pens Picker is the tab's root list | Pens | pick, `stay` | Get more, Import, New, Manage | Get more list (1), pack page with Review and install in place (2) |
| Pen colour | Panel or wheel | Colour sets | pick, `stay` or `close` | all four (panel) | none |
| Cover for a new notebook | Sheet, pushed Picker | Covers | pick, `pop` | Get more, Import, New | Picker (1), Get more (2), pack page and review (3) |
| Folder style, canvas preset | Sheet, pushed Picker | Folder styles, presets | pick, `pop` | Get more, Import, New | as covers |
| Insert | Side panel, Insert tab | Stickers, Images, Templates | pick, `stay` | all four | Get more (1), pack page (2) |
| Content, one page per type | Settings, Content | any | manage | Get more, Import, New | pack page (1), review (2) |
| Pack contents | SourceDetail | per type | manage, `scope` | none | none |

Review and install on a pack page is the review in place, not a further pushed view.
