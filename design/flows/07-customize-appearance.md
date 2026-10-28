# 07 Customise appearance

## Goal

Change how Grapho looks (theme), how a notebook or folder looks (cover, folder style, canvas
preset), keep a look for reuse, and build one in Studio, always from the item or setting affected.

Counting rule: see 01. Start at the library, or the document when stated.

## Entry points

| What | Where |
|---|---|
| Theme, mode, density | Settings, Appearance (`grapho://settings/appearance`); the Top bar overflow row "Appearance" (opens Settings there); the palette (keyboard) |
| Cover, folder style, canvas preset | the item Menu (the ... button, right click, Menu key), "Edit look" |
| Manage and rename saved looks | Settings, Content, the type, manage mode |
| Studio | New or Edit in a look Picker; Content page New |
| Paper in dark mode | Settings, Appearance (`appearance.pageDark`) |

## Steps: change the theme

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Settings, Appearance | Settings page | `Sidebar` (Settings variant), `SettingRow` |
| 2 | Mode: Light, Dark, System | Settings page | `SegmentedControl` (3 options) |
| 3 | Density: Automatic, Touch, Compact | Settings page | `SegmentedControl` (3 short options) |
| 4 | Paper in dark mode | Settings page | `Toggle` "Dark paper in dark mode" |
| 5 | Theme row (shows the current theme) | Settings page | `ListRow` that pushes the Theme `Picker` (pick mode, `themePreview` 160 x 100, `SourceFilter`, footer Get more, Import, New) |
| 6 | Tap a theme | | the app repaints at once; the view stays |

Text size is not here: it follows the operating system.

## Steps: build a theme

New (or Edit on a Yours theme) pushes the `ThemeEditor` page inside Settings (Back returns): live
preview, `TextField` colour fields, `Swatch` pickers, a Name `TextField`, and the contrast readout.
The guard adjusts failing pairs ("Text would be hard to read here") and the theme saves with its
originals plus adjusted values (content-system/flows.md, "Theme pack that fails the contrast
guard"). Applying an adjusted theme shows the `Banner` "This theme was adjusted for contrast." with
the action "Use the default theme".

## Steps: change a notebook cover

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | The ... button on the tile | Library | `ItemTile`, `IconButton` |
| 2 | Edit cover | Menu | `Menu` |
| 3 | Cover row to the Picker; tap a cover; the view pops back | Sheet | `NewItemSheet` in edit mode, `Picker` |
| 4 | Done commits (Cancel reverts) | Sheet | `Button` |
| 5 | Result | Toast "Cover changed" with Undo | `Toast` |

Folder style and canvas background are the same with their own Picker.

## Steps: Studio and saved looks

1. New (or Edit) in a look Picker: the Sheet closes with its draft kept and Studio opens full
   screen (`StudioShell`: canvas and preview of the real tile, `IconButton` Undo and Redo, `Button`
   Done and Cancel). The look's name is the title in the Studio top bar, a `TextField` defaulting to
   "Cover 3" (the one naming rule: name it where it is made). No Dialog.
2. Done saves the look to Yours in that Picker and returns to the Sheet with it selected; the Sheet
   then commits with its own Done. Cancel and Back ask once ("Discard changes?" Dialog) if anything
   changed.
3. Manage later: Settings, Content, Covers, a `ContentPage` in manage mode: Edit, Duplicate, Rename, Delete (own) or
   Hide (built in or pack), Export as pack. Delete goes to Trash.

Studio layout: Expanded: rail left, canvas centre, controls right. Medium: rail as a top strip,
controls in the `SidePanel` over the off-hand edge. Compact: the preview and canvas share the top
area (the pen draws on the canvas; the preview is a small inset), controls in `Tabs` under it. The
rail and controls sit on the off-hand side; Done stays top right in both hands, Cancel top left.

## Tap counts

| Task | Taps |
| --- | --- |
| Change theme | 4 (Settings, Appearance, Theme row, tile); from a note 5 (More, Settings, Appearance, row, tile) |
| Change theme, keyboard | 2 (Ctrl+K, "Theme") then the tile |
| Change a notebook cover | 4 (..., Edit cover, tile, Done) |
| Change a folder style | 4 |
| Use a pack theme | 4, the same as any theme |
| Make a cover and keep it | 5 plus drawing: ..., Edit cover, New, Studio Done, Done; +1 to retitle |
| Rename a saved look | 5 (Settings, Content, Covers, item ..., Rename) plus typing |
| Reset a bad theme | 1 (Banner "Use the default theme"), or 4 in Settings |

## States

| State | What the user sees |
|---|---|
| Yours empty | `EmptyState` sm: "No themes of your own." "New theme", "Get more" |
| Pack theme fails the guard | Excluded at import; adjusted ones get the Banner when applied |
| Loading previews | `Skeleton` after 300 ms |
| Pack off | Its themes leave the Picker; the active theme and folder styles in use stay |
| Pack removed | Items in use are kept as user copies marked with their origin |
| Offline | Get more shows its offline Banner |
| Studio save error | Inline in the top bar with Retry; work is kept |
| Very large | 500 covers: virtualised, Filter first |
| Sync | Theme syncs (`appearance.theme`) |
| Dark mode and paper | Paper stays light unless "Dark paper" is on; one line explains it |

## Exits

Done commits and returns; Cancel and Back in Studio discard after one ask; Cancel on the Sheet
reverts the staged choice. A failed save stays in Studio.

## Wireframes

Settings, Appearance, Expanded:

```
+-----------+------------------------------------------------+
| General   | < Appearance                                   |
| Appearance| (i) This theme was adjusted for contrast.      |
| Language  |     [ Use the default theme ]                  |
| Pen/tools | Mode      (Light)(Dark)(System)                |
| Toolbar   | Density   (Automatic)(Touch)(Compact)           |
| Sync      | Dark paper in dark mode             [ off ]    |
| Battery   | Theme     Dusk                              >  |
| Search    |                                                |
| About     |                                                |
| Content > |                                                |
| Sources > |                                                |
+-----------+------------------------------------------------+
```

Studio, Expanded:

```
+------------------------------------------------------------+
| [Cancel]   [ Cover 3 ]       [Undo][Redo]           [ Done ]|
+--+-------------------------------------+-------------------+
|R |         +---------------+           | Colour  Pattern   |
|a |         | live cover    |           | [swatches]        |
|i |         +---------------+           | Texture [slider]  |
+--+-------------------------------------+-------------------+
```

Compact: Settings opens at a section list; each page pushes with Back. Studio: preview over `Tabs`.
German check: "Zuruecksetzen" would not fit a segment, so the Banner action is a text button.

## Access

Keyboard: Studio Done is Ctrl+Enter, Cancel is Escape; theme tiles are a grid with arrow keys and
Enter. Screen reader: tiles read "Dusk, theme, selected"; the Banner is announced when it appears;
the editor's contrast readout is text. Left-handed: Studio's rail and controls mirror (see above).
Focus returns to the Theme row after the Picker pops back.
