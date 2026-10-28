# Components derived from the flows

The minimum component list the fifteen flows need. A row is a **component** (built once, in the
library). **Parts** live inside a component and **recipes** are compositions of components named in
flows; neither is counted. "Replaces existing" names the components a new one supersedes. Every component below appears in a numbered step, state or wireframe of the flows listed.

"Used by flows" gives the flow file prefix: 01 first-run, 02 create-notebook, 03
write-and-switch-tools, 04 pdf-import-annotate, 05 organize-folders, 06 find-something, 07
customize-appearance, 08 packs, 09 extensions, 10 recover, 11 navigate-long-document, 12 settings,
13 open-document, 14 share-and-sync, 15 add-to-page.

## Components

### Actions

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| Button | 01, 07, 08, 09, 10, 14, 15 | Button, HoldButton | Primary, secondary, text, danger; md 44 and sm 36. No hold variant |
| IconButton | 05, 07, 11, 15 | IconButton, CloseButton | Quiet and solid; each has a Tooltip with name and shortcut; the Dot is a state |
| Toggle | 01, 03, 07, 08, 09, 12, 14, 15 | Toggle | One style; the marked variant is dropped |
| SegmentedControl | 04, 07, 12, 14 | SegmentedControl | At most 3 short options; more or longer becomes Select or RadioList |
| Chip | 05, 06 | Chip, RemovableChip | Selectable and removable filter chips; a toggle Chip for Starred |

### Inputs

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| TextField | 02, 03, 04, 05, 07, 08, 09, 11, 13, 14 | TextField | One stacked style plus inline for rename; password and numeric types |
| SearchField | 01, 05, 06, 08, 12, 15 | SearchField | The one search input for all three scopes |
| Select | 01, 02, 05, 06, 08, 10, 12, 15 | Select | Searchable above 8 options |
| RadioList | 12, 13, 14 | Radio, RadioGroup (kept, renamed) | Exclusive choices that do not fit a segmented row; radio group semantics |
| Slider | 03, 09, 12 | Slider | Width, smoothing, texture |
| Stepper | 03, 12 | Stepper | Quick slots shown, 0 to 5 |
| Checkbox | 05, 06, 08 | Checkbox | Mixed state for tags on a mixed selection; filter lists |
| TextArea | 08 | TextArea | Pack description |
| ShortcutField | 12 | new | Captures a chord, names clashes |
| DateField | 06 | new | Opens the platform date picker; locale format |

### Menus and popovers

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| Menu | 03, 04, 05, 07, 12, 13 | Menu, MenuItem, OverflowMenu, FolderMenu, NewMenu, SortMenu, SelectionMenu, FilterMenu, InsertMenu | Command lists from the registry; context menus are the same Menu; rows may be radio or link rows, never controls |
| Popover | 05, 06, 10, 11 | Popover | Applies at once, never has Done; anchored; kinds collapse to one |
| CommandPalette | 12 | new | Ctrl+K; on touch from the Search field (>) and a Note bar More row |

### Panels, sidebar, bars and dock

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| TopBar | 05 | LibraryHeader, SectionHeader | Priority per place (05); deep-folder path as a part; Settings and Sync live in the Sidebar foot |
| Sidebar | 07, 10, 12 | SettingsOverlay, SettingsNav | Library places; Settings variant for sections and index; drawer on small windows; holds the SyncIndicator |
| FolderTree | 05 | new | Indent four levels, collapse, Filter above 30; reused in FolderChooser |
| NoteBar | 03, 11, 13 | NoteBar | Recedes; returns by handle, edge tap, command or 1.5 s |
| NoteBarHandle | 03 | NoteBarHandle (kept) | The visible, focusable recall control for the receded bar |
| ToolDock | 03, 10, 13 | NoteBar tool buttons | Tool, slots (a part, stable positions), Undo, Redo; never recedes; no menu |
| SidePanel | 03, 07, 09, 11, 12, 15 | Panel, Inspector, WheelPanel, LayersPanel host | Beside, over the edge, or bottom panel; includes the TabRail part |
| SelectionBar | 05, 08, 10, 15 | SelectionBar, LassoBar | Library, picker, panel and canvas variants; icons below 600 dp |
| FindBar | 06 | new | Stays visible while open, outside the receding group |
| Tabs | 07, 14 | new | Two to four tabs under a preview (Studio and Compare on compact) |

### Sheets and dialogs

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| Sheet | 01, 02, 04, 05, 07, 08, 09, 13, 14 | Sheet | Bottom on compact; pushed views with Back; hosts first run and the recipes |
| Dialog | 09, 10 | Dialog, ConfirmDialog, UnsavedChangesDialog | Irreversible confirm and unsaved changes; at most one field; default focus Cancel |
| NewItemSheet | 01, 02, 07 | DesignSheet, NotebookSheet, CanvasSheet, FolderSheet, QuickSheet | Notebook, canvas, folder; create and edit modes; rows push the Pickers |
| ImportReviewSheet | 04, 08, 09, 13 | PackImportSheet | Variants: PDF (one or many), notebook file, image, pack, extension |

### Pickers and managers

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| Picker | 02, 03, 07, 08, 11, 12, 15 | PresetPicker, PackCategory, ManageColours lists, PenList | Pick and manage modes; SourceFilter and footer slots as parts |
| FolderChooser | 02, 05, 06, 14 | MoveMenu list | Library first, Filter, New folder, disabled rows with reasons |
| ColourEditor | 03 | ColourPicker, ColourDisc, ColourPane | In the Tool settings tab |
| ColourSetSwitcher | 03 | ManageColours (sets), AddToSet | Set list with Filter; at most 24 colours per set |
| StudioShell | 07 | StudioCanvas, StudioPanel, StudioRail, StudioSide, StudioTopBar | Title field, Done, Cancel; layout per class |
| ThemeEditor | 07 | ThemeEditor | A page pushed inside Settings, Appearance |
| ContentPage | 07 | ContentPage, ContentTile, ContentArt | Manage mode host for a content type |
| SourceDetail | 08 | PacksPage detail, ExtensionDetail, ExtensionsPage | One detail page for a pack or an extension (SYSTEM-MAP section 6) |
| ExtensionPanelHost | 09 | new | Renders the declarative tree with standard controls |
| CompareView | 14 | new | Two versions as page strips |

### Tiles, cards and rows

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| ItemTile | 05, 07, 10, 13 | CoverTile, SelectionBadge (a part) | Notebook, PDF, canvas; the ... button, selection check |
| FolderTile | 05 | FolderTile | Not merged into ItemTile: it previews contents and is a drop target |
| ListRow | 02, 03, 04, 05, 06, 07, 08, 09, 11, 12, 13, 15 | ListRow, ActionRow, LibraryRow, PackRow, ExtensionRow | One row with slots (leading, selection, toggle, menu) |
| SettingRow | 03, 07, 09, 12, 14 | FieldRow | Label, helper, control, Reset |
| PickerTile | 03, 11, 15 | PresetTile, ThemeTile, StickerTile, PackTile, OptionTile | Renderer kinds from the registry, including page thumbnail |
| SearchResultGroup | 06 | SearchBody rows | Not merged into ListRow: many snippets per document |
| GroupHeading | 05 | GroupHeading | Count in the control |
| AlphabetRail | 05 | AlphabetRail | Shown when sorted by Name |
| Swatch | 07 | Swatch, SwatchRow | Colour sets, theme editor |
| ChoiceCard | 01 | ChoiceCard | Two or three large choices (hand) |
| Highlight | 06 | Highlight | Search match mark in text |

### Feedback

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| Toast | 05, 07, 08, 09, 10, 13, 14 | Toast, ToastProvider | Max two visible; one action; off-hand side; held 10 s with a screen reader |
| ProgressItem | 04, 08, 11, 13, 14 | PictureWorking (job part), busy overlays | After 1000 ms; Cancel; ends as a Toast |
| Banner | 03, 04, 06, 07, 09, 10, 12, 13, 14 | Banner | Lasting states only; tones neutral, warn, danger |
| EmptyState | 01, 02, 05, 07, 08, 10, 11, 15 | EmptyState | md with the mascot (an art prop) for first use; sm without |
| Skeleton | 02, 04, 06, 07, 08, 09, 11, 13, 15 | new | After 300 ms, at least 300 ms |
| Tooltip | 01, 03 | Tooltip, NoteHint | Name and shortcut; first-use hint with Got it |
| SyncIndicator | 14 | new | Off, up to date, syncing, offline, conflict, storage full |

### Canvas tools

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| ToolWheel | 03 | PenWheel, WheelFace, PenWheelTab, CustomiseWheel | Rings, centre width and Previous; hover tracking; keyboard and screen reader modes |
| CanvasChip | 11 | new | Scroll chip, zoom chip and snapped chip (one chip); the Note bar page indicator is a Button |
| PageScrubber | 11 | new | Handle only; off-hand edge |
| Minimap | 11 | new | Canvas overview |
| FindMarks | 06 | new | On-canvas match marks for handwriting and PDF text |

### Anything else

| Component | Used by flows | Replaces existing | Notes |
|---|---|---|---|
| DragGhost | 05, 15 | ReorderList ghost | Top of the z order |
| DropZone | 04, 08, 13 | new | File drop overlay |
| ReorderList | 03, 11, 15 | ReorderList | Pages, Tools ring, layers, Views; every drag has a Menu equivalent |

## Parts and recipes (not counted)

| Name | Kind | Lives in | Used by |
|---|---|---|---|
| QuickSlot | part | ToolDock | 03 |
| TabRail | part | SidePanel | 09, 15 |
| SourceFilter, PickerFooter | parts | Picker | 02, 07, 08 |
| SelectionBadge | part | ItemTile | 05 |
| Dot | state | IconButton, ListRow | 12 |
| PathBar | part | TopBar | 05 |
| Go to page, filter bodies, tag list, colour swatches, Activity | recipes | Popover | 11, 06, 05, 03, 10 |
| First run | recipe | Sheet, ChoiceCard | 01 |
| ExportSheet, MoveToSheet, PackBuilderSheet, RemoveSheet | recipes | Sheet | 04 and 14, 05, 08, 08 and 10 |
| CatalogueView, ConsentView | recipes (pushed views) | Sheet | 08, 09 |

## Count by family

| Family | Components |
|---|---|
| Actions | 5 |
| Inputs | 10 |
| Menus and popovers | 3 |
| Panels, sidebar, bars and dock | 10 |
| Sheets and dialogs | 4 |
| Pickers and managers | 10 |
| Tiles, cards and rows | 11 |
| Feedback | 7 |
| Canvas tools | 5 |
| Anything else | 3 |
| **Total** | **68** |

## Merged components

The Popover bodies, MoveToSheet, RemoveSheet, ExportSheet, PackBuilderSheet, CatalogueView and ConsentView are recipes; NewDocumentSheet and FolderSheet are one NewItemSheet; SourceFilter and PickerFooter are parts of Picker; ThumbnailTile is PickerTile's page-thumbnail renderer; ZoomChip and the scroll chip are one CanvasChip, and PageIndicator is a Button; PackRow, ExtensionRow and ItemRow are ListRow slots; PackDetail and ExtensionDetail are SourceDetail; Dot and SelectionBadge are parts; SettingsNav is a Sidebar variant; QuickSlot, TabRail and PathBar are parts; Mascot is an EmptyState prop.

Kept separate, because a real difference would be hidden: FolderTile stays apart from ItemTile (it previews contents and is a drop target); SearchResultGroup stays apart from ListRow (many snippets per document); FindBar stays apart from Banner (it takes input and shows a count). Radio is kept as RadioList, and NoteBarHandle is kept.

## Replaced components

Each has exactly one successor. Not counted: renames (CoverTile to ItemTile, FieldRow
to SettingRow, Radio to RadioList) and components that stay (NoteBar, NoteBarHandle, SelectionBar,
FolderTile, Swatch, Highlight, ChoiceCard).

| Existing | Successor |
|---|---|
| HoldButton | none; unused |
| PagePreview | none; exported and unused |
| Panel, Inspector, WheelPanel | SidePanel |
| QuickSheet, DesignSheet, NotebookSheet, CanvasSheet, FolderSheet | NewItemSheet (SYSTEM-MAP lists QuickSheet under Side panel; see open question 7) |
| OverflowMenu, FolderMenu, NewMenu, SortMenu, SelectionMenu, FilterMenu, InsertMenu | Menu |
| LassoBar | SelectionBar |
| NoteHint | Tooltip first-use hint |
| PenList, PresetPicker, PackCategory | Picker |
| ManageColours | ColourSetSwitcher |
| ColourPicker, ColourDisc, ColourPane | ColourEditor |
| CustomiseWheel | SidePanel (Tool settings tab) |
| PackPicker, PackExportSheet, NewPackSheet, PackDetailsStep | PackBuilderSheet recipe |
| PackBrowseSheet | CatalogueView recipe |
| PackImportSheet | ImportReviewSheet |
| ExportDialog | ExportSheet recipe |
| RemovePackDialog | RemoveSheet recipe |
| SettingsOverlay | Sidebar (Settings variant) |
| OptionTile, PresetTile, ThemeTile, StickerTile, PackTile | PickerTile |
| LibraryRow, PackRow, ExtensionRow, ActionRow | ListRow |
| ExtensionDetail | SourceDetail |

Not used by these flows and kept: LayerList (as Layers rows, 15), Guides,
ShapeOptions, ShapeSuggest, LineOptions, GlyphPicker, AddPictureDialog, CropDialog, CropMode (used
in 15), BackgroundRemover, PictureWorking, PageBackground, LanguagePicker, ScrollRow, RollingNumber,
AppFrame, DeviceFrame, and the Android private copies of Chip, Toggle, Segment and Heading (replace
with the shared components).

## Open questions

These points are not yet decided:

1. **Short windows.** Classes are by width only. A 1200 by 450 window is Expanded and gets a
   vertical dock and a persistent sidebar. Proposed: a height class under 500 dp that forces a
   horizontal dock and a bottom Sheet (03).
2. **Registry homes.** `pen.penOnly` and `appearance.pageDark` list "document menu" as a contextual
   surface; the document menu now holds commands only (03). Their contextual home is the Tool
   settings tab or Settings.
3. **Selection bar in the Pages tab.** SYSTEM-MAP places it in the library and on the canvas; 11 uses
   a panel variant for multi-select pages.
4. **Wheel centre.** The width control and Previous tool are in the wheel (03); SYSTEM-MAP says pick tool, pen and colour.
5. **Commands to register.** `appearance.resetTheme`, `bar.show` (Show note bar, F6), and the
   palette's touch entry (a top bar overflow row and a Note bar More row; the `>` mode was dropped).
6. **One Highlighter.** `tool.textHighlight` becomes the "Snap to text" option of
   `tool.highlighter` (03, 04).
7. **QuickSheet.** SYSTEM-MAP merges it into the Side panel. As an existing component it is a create and edit Sheet for folder, cover and canvas, which NewItemSheet replaces.
8. **Sign in to restore.** A text link beside Import on the empty library (01, 14); SYSTEM-MAP names New notebook and Import only.
9. **First-run Sheet dismissal.** Skip keeps the defaults, a scrim tap does nothing, Escape is Skip.
   This differs from the Sheet row's dismiss rule.
10. **Settings and Sync at the Sidebar foot.** The Top bar holds Search, New, Select, View (05).
    Confirm that Settings and the SyncIndicator may leave the Top bar.
11. **Extension remove.** 4.2 names pack removal; extensions are treated the same (at once, Toast
    Undo, Trash), with a Dialog only for erasing stored data (09).
12. **Not yet specified.** Key renames for the text tool and wheel letters, the Studio drawing area on medium, and full compact drawings for every flow.
