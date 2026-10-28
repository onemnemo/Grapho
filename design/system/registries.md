# Registries: commands, settings, content types, deep links

Detail spec for the registries. The three registries are shipped data
with string keys, owned by the core. Shells own the renderers: menus, the palette, shortcut
capture, settings controls and pickers. Anything that changes `standards/01-architecture.md` or
`docs/file-format.md` is marked **Proposed change**.

## 1. Command registry

### Entry

| Field | Type | Meaning |
|---|---|---|
| id | string | dotted, lowercase, stable and never reused: `doc.undo`, `library.newFolder` |
| labelKey | string | key into the string table. Extension labels are keys in the extension's own table |
| icon | icon id or null | from the icon registry, rendered through `GraphoIcon`. Null means text only |
| group | string | menu group heading key: `edit`, `view`, `insert`, `page`, `file`, `tools`, `library`, `go`, or an extension name |
| priority | integer | fixed, lower comes first. Decides overflow order. Never usage frequency |
| contexts | set of enum | library, document, canvas, selection, find, picker, studio, settings, sheet, dialog, onboarding |
| enabledWhen | predicate expression | see section 1.1. Absent means always enabled where the context holds |
| invocation | enum | once, toggle, while-held |
| shortcuts | map platform to chord list | platforms: android, windows, macos (iPad uses macos). Empty means none |
| source | enum | builtin, extension (with `extensionId`) |
| destructive | bool | the row is drawn with danger styling and never takes a default Enter |
| keywords | map locale to list | extra palette search terms per locale |
| surfaces | set | where it may appear besides the palette: noteBar, toolDock, menu, selectionBar, topBar. A hint, the shell and the user's layout decide |

Priority bands keep the order stable when commands are added: 0 to 99 core document actions
(undo, redo, tools, pages, more), 100 to 199 edit, 200 to 299 insert and page, 300 to 399
view, 400 to 499 file, 500 to 599 library, 600 and up extensions. Within a band, ties break
by id.

### 1.1 Predicate vocabulary

A predicate expression is `and`, `or`, `not` over these predicates, evaluated by the core on
every state change that can affect it. Nothing else is allowed, so a command's enablement is
always explainable.

| Predicate | Exact meaning |
|---|---|
| hasSelection | at least one item (library) or object (canvas selection) is selected. A text caret alone is not a selection |
| selectionKind(k) | every selected thing is of kind k. Kinds: item, folder, page, stroke, shape, image, sticker, textBlock, extensionBlock, mixed. `mixed` is true only when the selection holds more than one kind |
| documentKind(k) | the open document is k: notebook, pdf, canvas. False when no document is open |
| canUndo | the stack for the current keyboard context has an applied entry (document stack inside a document, library journal in the library) |
| canRedo | the same stack has an undone entry |
| editable | the target can be changed: the document is not read only, not locked, not mid export; for library targets, not in Trash |
| windowClass(c) | the current window class is c: compact, medium, expanded. Changes live on resize |
| clipboardHas(t) | the platform clipboard holds a type Grapho accepts: strokes, image, text, item, page. The shell reports clipboard changes to the core |
| extensionOn(id) | the extension is installed, enabled and not in safe mode |

When a command is disabled, the shell shows it dimmed and, in menus and the palette, the reason
from a fixed list (`nothing_selected`, `read_only`, `wrong_document`, `nothing_to_undo`,
`window_too_small`, `clipboard_empty`, `extension_off`, `modal_open`). A command that is not valid for the
context at all is hidden, not disabled.

### 1.2 Invocation modes

| Mode | Behaviour |
|---|---|
| once | runs on activation (press, tap, palette Enter) |
| toggle | flips a state; the control shows a pressed state with a non-colour cue (see accessibility.md). Shortcut press flips it |
| while-held | active while the key, pen button or control is held; releases on key up, pen button up, touch up or loss of focus. Example: the eraser on the S Pen side button. The palette lists it as a toggle because it cannot be held there |

### 1.3 Shortcut rules

1. A chord is modifiers plus one key. Single key chords (`T`, `E`) are allowed only in the
   document and canvas contexts and only when no text field has focus.
2. The primary modifier is Ctrl on Android with a keyboard and on Windows, and Cmd on macOS and
   iPad. Alt on Windows and Option on macOS are for secondary variants.
3. Built-in shortcuts are unique per platform and context. Two built-in commands never share a
   chord in one context; the same chord may map to different commands in different contexts only
   through the keyboard context rule (section 2).
4. Extension commands may not take a built-in shortcut. A clash leaves the extension command
   unbound and says so on the extension's page ("Ctrl+K is taken by Command palette").
   Extensions may claim a free chord, never one the user already bound.
5. The user may remap any command in Settings, General, Keyboard. A remap that clashes asks
   which command keeps the chord; the other becomes unbound. Reset restores the defaults per
   command or all.
6. Reserved by the platform and never bound: Alt+F4 and Win key chords (Windows), Cmd+Q, Cmd+H,
   Cmd+Tab (macOS), Alt+Tab, Android system keys.
7. Tooltips show the effective shortcut for the current platform, after remapping.

### 1.4 Default shortcut table

Win is Windows, And is Android with a hardware keyboard, Mac is macOS and iPad. Ctrl and Cmd are
the primary modifier. "Ctx" is the context in which the chord resolves.

| Command id | Label | Ctx | And | Win | Mac |
|---|---|---|---|---|---|
| app.palette | Command palette | all | Ctrl+K | Ctrl+K | Cmd+K |
| app.settings | Settings | all | Ctrl+, | Ctrl+, | Cmd+, |
| app.search | Search | library | Ctrl+F | Ctrl+F | Cmd+F |
| doc.find | Find in document | document | Ctrl+F | Ctrl+F | Cmd+F |
| doc.findNext | Next match | document, find | F3, or Ctrl+G while the find bar has focus | F3, or Ctrl+G while the find bar has focus | Cmd+G while the find bar has focus |
| doc.findPrev | Previous match | document, find | Shift+F3, or Ctrl+Shift+G while the find bar has focus | Shift+F3, or Ctrl+Shift+G while the find bar has focus | Cmd+Shift+G while the find bar has focus |
| app.help | Shortcuts list | all | Ctrl+/ | Ctrl+/ | Cmd+/ |
| library.newNotebook | New notebook | library | Ctrl+N | Ctrl+N | Cmd+N |
| library.newFolder | New folder | library | Ctrl+Shift+N | Ctrl+Shift+N | Cmd+Shift+N |
| library.import | Import | library | Ctrl+O | Ctrl+O | Cmd+O |
| library.select | Select | library, picker | Ctrl+Alt+A | Ctrl+Alt+A | Cmd+Option+A |
| library.selectAll | Select all | library, picker | Ctrl+A | Ctrl+A | Cmd+A |
| library.open | Open | library | Enter | Enter | Cmd+Down |
| library.rename | Rename | library, selection | F2 | F2 | Enter |
| library.star | Star or unstar | library, selection | Ctrl+D | Ctrl+D | Cmd+D |
| library.moveTo | Move to | library, selection | Ctrl+Shift+M | Ctrl+Shift+M | Cmd+Shift+M |
| library.delete | Move to Trash | library, selection | Delete | Delete | Cmd+Backspace |
| library.drawer | Toggle sidebar | library | Ctrl+B | Ctrl+B | Cmd+B |
| library.undo | Undo (library journal) | library | Ctrl+Z | Ctrl+Z | Cmd+Z |
| library.redo | Redo (library journal) | library | Ctrl+Shift+Z | Ctrl+Shift+Z or Ctrl+Y | Cmd+Shift+Z |
| doc.close | Close document | document | Ctrl+W | Ctrl+W | Cmd+W |
| doc.undo | Undo | document | Ctrl+Z | Ctrl+Z | Cmd+Z |
| doc.redo | Redo | document | Ctrl+Shift+Z | Ctrl+Shift+Z or Ctrl+Y | Cmd+Shift+Z |
| doc.export | Export | document | Ctrl+Shift+E | Ctrl+Shift+E | Cmd+Shift+E |
| doc.share | Share | document | Ctrl+Shift+S | Ctrl+Shift+S | Cmd+Shift+S |
| doc.print | Print | document | Ctrl+P | Ctrl+P | Cmd+P |
| tool.wheel | Tools (opens the wheel) | document | T | T | T |
| tool.pen | Pen | document | P | P | P |
| tool.highlighter | Highlighter | document | H | H | H |
| tool.eraser | Eraser | document | E | E | E |
| tool.eraserHold | Eraser while held | document | Hold X | Hold X | Hold X |
| tool.lasso | Lasso | document | L | L | L |
| tool.text | Text | document | Y | Y | Y |
| tool.textHighlight | Text highlight (PDF with text) | document | Shift+H | Shift+H | Shift+H |
| tool.previous | Previous tool | document | Q | Q | Q |
| tool.slot1 to 5 | Quick slot 1 to 5 | document | 1 to 5 | 1 to 5 | 1 to 5 |
| panel.toggle | Toggle side panel | document | Ctrl+. | Ctrl+. | Cmd+. |
| panel.pages | Pages tab | document | Ctrl+Shift+1 | Ctrl+Shift+1 | Cmd+Option+1 |
| panel.layers | Layers tab | document | Ctrl+Shift+2 | Ctrl+Shift+2 | Cmd+Option+2 |
| panel.insert | Insert tab | document | Ctrl+Shift+3 | Ctrl+Shift+3 | Cmd+Option+3 |
| panel.toolSettings | Tool settings tab | document | Ctrl+Shift+4 | Ctrl+Shift+4 | Cmd+Option+4 |
| view.fitWidth | Fit width | document | Ctrl+1 | Ctrl+1 | Cmd+1 |
| view.fitPage | Fit page | document | Ctrl+2 | Ctrl+2 | Cmd+2 |
| view.zoom100 | 100% | document | Ctrl+0 | Ctrl+0 | Cmd+0 |
| view.zoomIn | Zoom in | document | Ctrl+= | Ctrl+= | Cmd+= |
| view.zoomOut | Zoom out | document | Ctrl+- | Ctrl+- | Cmd+- |
| view.minimap | Minimap (canvas) | canvas | Ctrl+M | Ctrl+M | Cmd+M |
| nav.goToPage | Go to page | document | Ctrl+Alt+G | Ctrl+Alt+G | Cmd+Option+G |
| nav.nextPage | Next page | document | Page Down | Page Down | Page Down |
| nav.prevPage | Previous page | document | Page Up | Page Up | Page Up |
| nav.firstPage | First page | document | Ctrl+Home | Ctrl+Home | Cmd+Up |
| nav.lastPage | Last page | document | Ctrl+End | Ctrl+End | Cmd+Down |
| page.add | Add page | document | Ctrl+Enter | Ctrl+Enter | Cmd+Enter |
| page.duplicate | Duplicate page | document | Ctrl+Shift+D | Ctrl+Shift+D | Cmd+Shift+D |
| page.delete | Delete page | document | Ctrl+Shift+Delete | Ctrl+Shift+Delete | Cmd+Shift+Backspace |
| edit.cut | Cut | selection | Ctrl+X | Ctrl+X | Cmd+X |
| edit.copy | Copy | selection | Ctrl+C | Ctrl+C | Cmd+C |
| edit.paste | Paste | document | Ctrl+V | Ctrl+V | Cmd+V |
| edit.duplicate | Duplicate | selection | Ctrl+Shift+V | Ctrl+Shift+V | Cmd+Shift+V |
| edit.delete | Delete selection | selection | Delete or Backspace | Delete or Backspace | Backspace or Delete |
| edit.selectAll | Select all on page | document | Ctrl+A | Ctrl+A | Cmd+A |
| edit.group | Group | selection | Ctrl+G | Ctrl+G | Cmd+G |
| edit.ungroup | Ungroup | selection | Ctrl+Shift+G | Ctrl+Shift+G | Cmd+Shift+G |
| edit.bringForward | Bring forward | selection | Ctrl+] | Ctrl+] | Cmd+] |
| edit.sendBackward | Send backward | selection | Ctrl+[ | Ctrl+[ | Cmd+[ |
| insert.photo | Insert image or take photo | document | Ctrl+Shift+I | Ctrl+Shift+I | Cmd+Shift+I |
| studio.done | Done | studio | Ctrl+Enter | Ctrl+Enter | Cmd+Enter |
| studio.cancel | Cancel | studio | Escape | Escape | Escape |
| bar.more | More | document | none | none | none |
| selection.more | More | selection | none | none | none |
| selection.clear | Clear selection | selection | Escape | Escape | Escape |
| library.emptyTrash | Empty Trash | library, dialog | none | none | none |
| doc.rename | Rename document | document | F2 | F2 | Return |
| tool.slotPin | Pin to quick slots | document | none | none | none |
| tool.slotReplace | Replace slot | document | none | none | none |
| tool.slotMoveEarlier | Move slot earlier | document | none | none | none |
| tool.slotMoveLater | Move slot later | document | none | none | none |
| appearance.resetTheme | Use the default theme | all | none | none | none |
| bar.show | Show note bar | document | none | none | none |
| app.activity.show | Open activity | all | none | none | none |
| app.activity.clear | Clear activity | all | none | none | none |
| job.cancel | Cancel job | all | none | none | none |
| job.retry | Retry job | all | none | none | none |
| sync.now | Sync now | all | none | none | none |
| sync.compare | Compare versions | library, document | none | none | none |
| library.restore | Restore | library, selection | none | none | none |
| library.deleteForever | Delete forever | library, selection, dialog | none | none | none |
| onboarding.next | Next | onboarding | none | none | none |
| onboarding.skip | Skip | onboarding | Escape | Escape | Escape |
| tool.pensMore | All pens | document | none | none | none |
| tool.toolsPage | Tools page | document | none | none | none |
| tool.colourSets | Colour sets | document | none | none | none |
| shape.undoSnap | Undo shape snap | canvas, document | none | none | none |
| selection.colour | Colour of selection | selection | none | none | none |
| nav.scrub | Show page scrubber | document | none | none | none |
| block.keepAsImage | Keep as image | selection | none | none | none |
| block.getExtension | Get extension | selection | none | none | none |
| block.turnOnExtension | Turn on extension | selection | none | none | none |
| block.restoreExtension | Restore extension | selection | none | none | none |
| block.updateExtension | Update extension | selection | none | none | none |
| picker.manage | Manage (toggle) | picker | none | none | none |
| picker.edit | Edit | picker | Enter | Enter | Cmd+Down |
| picker.duplicate | Duplicate | picker | none | none | none |
| picker.rename | Rename | picker | F2 | F2 | Return |
| picker.hide | Hide | picker | none | none | none |
| picker.unhide | Unhide | picker | none | none | none |
| picker.delete | Delete (Yours; undoable, not Move to Trash) | picker | Delete | Delete | Cmd+Backspace |
| picker.exportPack | Export as pack | picker | none | none | none |
| picker.details | Details | picker | none | none | none |
| picker.getMore | Get more | picker | none | none | none |
| picker.import | Import | picker | none | none | none |
| picker.new | New | picker | none | none | none |
| picker.viewOptions | View options | picker | none | none | none |
| picker.managePacks | Manage packs | settings | none | none | none |
| colour.addToSet | Add to set | picker | none | none | none |
| colour.replace | Replace | picker | none | none | none |
| colour.remove | Remove | picker | none | none | none |
| studio.undo | Undo (Studio) | studio | Ctrl+Z | Ctrl+Z | Cmd+Z |
| studio.redo | Redo (Studio) | studio | Ctrl+Shift+Z | Ctrl+Shift+Z or Ctrl+Y | Cmd+Shift+Z |
| appearance.saveTheme | Save theme | settings | Ctrl+S | Ctrl+S | Cmd+S |
| pack.update | Update pack | settings | none | none | none |
| pack.skipVersion | Skip this version | settings | none | none | none |
| pack.revert | Revert pack | settings | none | none | none |
| pack.share | Share pack | settings | none | none | none |
| pack.export | Export pack | settings | none | none | none |
| pack.remove | Remove pack | settings | none | none | none |
| pack.restore | Restore pack | settings | none | none | none |
| compare.next | Next difference | sheet | F8 | F8 | Cmd+Option+Down |
| compare.previous | Previous difference | sheet | Shift+F8 | Shift+F8 | Cmd+Option+Up |
| sheet.close | Close | sheet, dialog | Escape | Escape | Escape |
| sheet.back | Back (closes a pushed view before its host) | sheet | Escape or Alt+Left | Escape or Alt+Left | Escape or Cmd+[ |
| sheet.confirm | The primary of the open Sheet | sheet | Enter, or Ctrl+Enter in a multi line field | Enter, or Ctrl+Enter in a multi line field | Enter, or Cmd+Enter in a multi line field |
| sheet.discard | Discard changes | dialog | none | none | none |

Gesture bindings, not keyboard chords, for the same commands: two-finger tap is `doc.undo`,
three-finger tap is `doc.redo`, S Pen button click is `tool.wheel`, S Pen button held is
`tool.eraserHold`. They are listed in Settings, Pen and tools and are remappable to any
`once` or `while-held` command.

## 2. Keyboard context resolution

The core resolves a chord against the **focused context**, in this order, first match wins:

1. A text field with focus (rename, Filter, find field, note title): text editing chords go to
   the field (copy, paste, undo of the typed text, select all). Single key chords do not fire.
2. A modal layer if open (Dialog, Sheet): only chords registered for context `picker` or
   `settings` that the layer declares, plus Escape and the palette.
3. The focused panel or picker (side panel tab, settings page): its own context.
4. The place: `studio`, `canvas` (canvas documents), `document`, or `library`.
5. Global commands (`app.*`).

Consequences:

- Ctrl+F is find in document in a document and global search in the library.
- Ctrl+Z is the document stack inside a document and the library journal in the library. The
  same chord maps to two command ids, `doc.undo` and `library.undo`, in different contexts.
- Ctrl+G (Cmd+G) is `edit.group` in the selection context and `doc.findNext` only while the find bar
  has focus. Ctrl+Shift+G (Cmd+Shift+G) is `edit.ungroup` in selection and `doc.findPrev` in the find bar.
  Go to page is Ctrl+Alt+G (Cmd+Option+G).
- The Undo on a toast always undoes its own action by entry id and does not depend on the
  keyboard context.
- A selection on the page adds the `selection` context on top of `document`; `selection`
  commands win over `document` ones with the same chord.
- The palette lists commands valid in the current context only, plus global ones. Typing a
  command name from another context offers "Available in the library" as a hint, not a run.

## 3. Settings registry

### Entry

| Field | Type | Meaning |
|---|---|---|
| id | string | `appearance.theme`, stable and never reused |
| type | enum | bool, enum, int, float, string, colorRef, contentRef, shortcutMap, list |
| default | value | shipped by the core |
| scope | enum | app (one value for the user), device (this device only), document (stored with a document), tool (stored per tool) |
| syncs | yes or no | app and tool scope usually yes; device scope always no; document scope travels with the file |
| home | section id | exactly one global Settings section that is the index |
| control | enum | toggle, segmented, select, slider, stepper, picker, shortcuts, link |
| surfaces | list | contextual surfaces where the same setting appears |
| labelKey, helpKey | string | |
| dependsOn | predicate or null | hides or disables the setting, with the reason |

A contextual appearance is the same setting and the same value, not a second setting. Each
contextual surface carries a "More in Settings" link to `grapho://settings/<section>/<settingId>`.
The settings search matches home settings and contextual ones; a contextual match opens the home
section scrolled to the setting.

### Sections and settings

Text size follows the operating system scaling. There is no Grapho text size setting, so
Appearance has none.

| Section | Id | Syncs | Scope | Control | Contextual surfaces |
|---|---|---|---|---|---|
| **General** | | | | | |
| Handedness | `general.handedness` | no | device | segmented: right, left | first run, Tool settings tab |
| Default library view | `general.libraryView` | yes | app | segmented: grid, list | library top bar view control |
| Default sort | `general.librarySort` | yes | app | select | library sort menu |
| Confirm before leaving Studio | fixed behaviour, not a setting | | | | |
| Keyboard shortcuts | `general.shortcuts` | yes | app | shortcuts | tooltips, palette entry |
| Share and export default format | `general.exportFormat` | yes | app | select | Export Sheet |
| **Appearance** | | | | | |
| Theme | `appearance.theme` | yes | app | picker (theme content type) | library menu "Theme", picker in Settings |
| Light, dark or system | `appearance.mode` | yes | app | segmented | none |
| Density | `appearance.density` | yes | app | segmented: automatic, touch, compact | none |
| Reduce motion | `appearance.reduceMotion` | no | device | segmented: follow system, on, off | none |
| Page background in dark mode | `appearance.pageDark` | yes | app | segmented: keep paper, dark paper | document menu |
| **Language** | | | | | |
| App language | `language.app` | no | device | select | first run, only if unsupported |
| Handwriting recognition language | `language.recognition` | yes | app | list | Search index section, document menu |
| **Pen and tools** | | | | | |
| Pen-only drawing | `pen.penOnly` | no | device | toggle | first run, document menu |
| Two-finger tap Undo | `pen.twoFingerUndo` | no | device | toggle | none |
| Three-finger tap Redo | `pen.threeFingerRedo` | no | device | toggle | none |
| Pen button action (click, hold) | `pen.buttonClick`, `pen.buttonHold` | no | device | select (a command) | none |
| Barrel double tap or squeeze action | `pen.barrel` | no | device | select (a command) | none |
| Palm guard strength | `pen.palmGuard` | no | device | segmented: standard, strict | none |
| Pen width, colour | `tool.<toolId>.width`, `.color` | yes | tool | slider, colorRef | wheel, Tool settings tab |
| Pen smoothing and pressure curve | `tool.<toolId>.smoothing`, `.pressure` | yes | tool | slider | Tool settings tab |
| Hold to snap shapes | `pen.snap` | yes | app | toggle | Tool settings tab |
| Default notebook template | `notebook.template` | yes | app | picker (template) | New notebook Sheet |
| **Toolbar** | | | | | |
| Note bar position | `toolbar.position` | yes | app | segmented: floating, full line | note bar customise mode |
| Pinned note bar items | `toolbar.pinned` | yes | app | list (max 4, max 2 extensions) | note bar customise mode |
| Quick slots, pinned | `toolbar.slotsPinned` | yes | app | list | Tool dock slot menu |
| Show quick slots | `toolbar.slotsShown` | no | device | stepper 0 to 5 | Customise link row in note bar More |
| Page scrubber | `toolbar.scrubber` | no | device | toggle | palette entry `nav.scrub` |
| Side panel width | `toolbar.panelWidth` | no | device | slider (between the panel minimum and maximum) | side panel resize handle |
| **Sync** | | | | | |
| Sync on or off | `sync.enabled` | no | device | toggle | Sidebar foot sync status |
| Sync over mobile data | `sync.cellular` | no | device | toggle | none |
| Sync what | `sync.scope` | yes | app | segmented: all, chosen folders | folder menu |
| **Battery** | | | | | |
| Index and thumbnails only while charging | `battery.chargeOnly` | no | device | toggle | none |
| Lower ink latency features on battery saver | `battery.saver` | no | device | select | none |
| **Search index** | | | | | |
| Index handwriting | `search.handwriting` | no | device | toggle | none |
| Index PDF text | `search.pdf` | no | device | toggle | none |
| Rebuild index | command `search.rebuild` | | | link | none |
| **About** | | | | | |
| Version, licences, open source notices | read only | | | link | none |

Content, Packs and Extensions pages hold content and install state rather than preferences; their
settings are `content.<typeId>.hidden` (list of hidden item ids, syncs yes, app scope, home
Content, contextual in the picker Manage mode), `packs.<packId>.enabled` (device-independent, syncs
yes), `extensions.<extId>.enabled` and `extensions.<extId>.pinned` (syncs yes), and each extension's
own settings page, built from standard controls under Extensions.

## 4. Content type registry

### Entry

Data only. A new type that fits an existing renderer kind needs no shell code. A new renderer kind
is code in each shell, but no new screens.

| Field | Type | Meaning |
|---|---|---|
| id | string | `pens`, `colorSets`, `covers`, `folderStyles`, `canvasPresets`, `stickers`, `templates`, `themes` |
| labelKeys | map | singular, plural, empty-state sentence, and the verb strings ("Add sticker") |
| rendererKind | enum | swatch, imageTile, cover, penSample, pageThumbnail, themePreview |
| previewSize | size token | the size the picker shows items at |
| editorKind | enum or null | studio.cover, studio.folderStyle, penEditor, colorSetEditor, importOnly, none |
| appliesTo | set | the targets: notebook, folder, canvas, page, tool, app |
| sortKeys | list | recent, name, newest, plus type specific keys such as `family` |
| packable | bool | can be put into a pack |
| placeholder | renderer spec | what a missing asset renders as, with its message key |
| viewModes | set | grid, list |
| acceptsImport | list | file types the Import footer accepts besides `.graphopack` |
| maxPerSet | integer or null | for example 24 for a colour set |

### The eight built-in types

| id | rendererKind | previewSize | editorKind | appliesTo | sortKeys | packable | viewModes | acceptsImport | Placeholder |
|---|---|---|---|---|---|---|---|---|---|
| pens | penSample | wide, 96 x 48 dp | penEditor | tool | recent, name, newest, family | yes | list, grid | none | grey pen sample with name |
| colorSets | swatch | 40 dp swatch, row | colorSetEditor | tool | recent, name, newest | yes | list | none | empty swatch row |
| covers | cover | 120 x 160 dp | studio.cover | notebook | recent, name, newest | yes | grid | png, jpg | plain cover in the neutral cover colour |
| folderStyles | cover | 96 dp | studio.folderStyle | folder | recent, name, newest | yes | grid | png | default folder |
| canvasPresets | pageThumbnail | 120 x 90 dp | studio.cover | canvas | recent, name, newest | yes | grid | none | blank canvas thumbnail |
| stickers | imageTile | 72 dp | importOnly | page | recent, name, newest | yes | grid | png, svg, jpg | outline of the sticker size with "missing" |
| templates | pageThumbnail | 120 x 160 dp | importOnly | notebook, page | recent, name, newest | yes | grid | pdf, png | blank page thumbnail |
| themes | themePreview | 160 x 100 dp | none (built through Appearance) | app | recent, name, newest | yes | grid | none | default theme preview |

Themes are picked and managed in Appearance. The Content section lists Themes as a link to it, so
the theme manager has one home. The same holds for the source filter ending in Hidden.

Rendering rules shared by all types: the renderer draws real content at the preview size, a
missing asset draws the placeholder with its message and never an empty cell, and every item has a
text alternative for screen readers from its name.

Fields to add to the entry: `hosts` (placements), `insertable` (adds an Insert tab type), `names` (ICU
message keys: placeholder name, empty, plural-aware counts), `sortDefault`, `tags` and `keywords` on
items, `nameVisible` per view, `dropAccepts` (colour sets), `limit` (24 for colour sets) and `manual`
as a sort key. Where `components/pickers/README.md` disagrees on preview sizes or view modes, this
registry wins.

**Proposed change** to `docs/file-format.md`: content references stored in a document are
`{typeId, assetId, contentHash}`, with `typeId` strings append only and unknown type ids kept and
round tripped.

## 5. Deep link grammar

```
link        = "grapho://" place
place       = library | doc | settings | content | packs | extensions
library     = "library" [ "/folder/" id | "/starred" | "/trash" ]
doc         = "doc/" id [ "?page=" n ]
settings    = "settings" [ "/" section [ "/" settingId ] ]
content     = "content/" typeId
packs       = "packs" [ "/" packId ]
extensions  = "extensions" [ "/" extId ]
```

- `id` is the 26 character item or folder id. `n` is 1 based. `section` is a section id from
  section 3 (`general`, `appearance`, `language`, `pen`, `toolbar`, `sync`, `battery`, `search`,
  `about`). `settingId` is a setting id with its dots kept.
- The core resolves a link to a **place descriptor**: place kind, target ids, and the scroll or
  highlight to apply. The shell navigates to the descriptor.
- Commands are not linkable from outside the app. The in-app palette runs commands directly.
  External apps and the share target can only reach places.

### Resolution and fallback

| Case | Result |
|---|---|
| Folder id gone | the nearest parent place, Library Home, and a toast "Folder not found" |
| Folder in Trash | `library/trash`, with a toast naming the folder |
| Document id gone or purged | the place the link was opened from, or Library Home, and a toast |
| Document in Trash | Trash with the item highlighted and Restore offered |
| `page` out of range | the last page for too large, the first for less than 1, no toast |
| Document file missing on this device | a Banner in the library item, with Retry once sync finishes |
| Unknown setting id in a known section | the section, no scroll, a toast "Setting not found" |
| Unknown section | `settings` root, with a toast |
| Unknown or unregistered type id | `settings/content`, which is the Content list, with a toast |
| Pack or extension not installed | the Packs or Extensions list, with a toast |
| Malformed link | ignored, with a toast "Link not recognised"; no navigation |
| App locked or first run not done | the link is held and applied after, never lost |

A fallback always goes to the nearest parent place that exists and always says what happened. It
never leaves the user on a blank screen. Contextual "More in Settings" links and the command palette
use these links, so the same resolver handles them. Extensions may emit links only to places, and
the core refuses links with a scheme other than `grapho`.

## Open questions

1. Single letter tool chords (`P`, `E`, `L`) clash with typing into the note title when the
   title field is not focused but a physical keyboard is attached. Rule 1 in 1.3 handles focus,
   but should single key chords be a setting that is off by default on Android tablets?
2. `tool.slot1 to 5` use the number keys, which are also Fit width and so on on some layouts. The
   table uses Ctrl for the view chords, so there is no clash. Open question: confirm that quick slots
   may use bare number keys.
3. The `windowClass` predicate is listed, but no command needs it today. The spec keeps it
   (for example hiding Toggle side panel at compact, where the panel is a bottom panel). Open
   question: confirm this use.
4. Theme sync: should the theme be synced across devices (spec says yes) or per device?
