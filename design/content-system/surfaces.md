# Surfaces

Wireframe-level specs. ASCII layouts at the expanded size; compact notes follow. All colour comes
from tokens; icons through `GraphoIcon`; one solid white primary per surface; selected is a soft
fill; no redundant subtext; 44 dp targets. Legend: `[ ]` button, `( )` switch, `<..>` icon.

## 1. Settings, Packs page

```
Packs                                         [New pack] [Import]
-----------------------------------------------------------------
 Built in
  <ico> Grapho                          312 items         (on, locked)
 Installed
 ::  <art> Botanica       Author    1.2.0                  (on)   ...
 ::  <art> Botanica       Study Lab 1.0.0                  (off)  ...
 ::  <art> Moon phases    Update to 1.3.0                  (on)   ...
 Made by you
  <art> My biology set    1.1.0              [Share]            ...
 Trash (2)                                                   >
 [Get more]                                  Total 214 MB
```

A pack is a bundle of pens, stickers, themes and more. The empty state says so in one line and offers
Get more and Import. Rows: drag handle, art 48 dp, name, author, version in quiet grey, switch,
overflow Menu (Share, Export, Remove, Details). The author is always shown. Made by you
rows have no switch: their items are Yours; the menu has Share, Save a new version, Save a copy,
Delete record. A record whose archive is waiting to download says "Waiting to download". States: off
(quiet), update available (line "Update to 1.3.0"; the dot on the Settings entry only when a catalogue
exists or a newer file has just arrived), installing (ring and Cancel), failed (reason and Try again),
incompatible and blocked (reason; switch disabled with the reason as accessible description), partial
("Some content not supported"), "Some content could not be shown". Search above 12 packs uses Filter
and Sort as everywhere (SYSTEM-MAP 8). Compact: two lines per row.

## 2. Pack detail

```
<Back>  Botanica                         (on)  [Share] [Export] ...
-------------------------------------------------------------------
 [preview]   Author     1.2.0   Free to share, credit me
             Ferns, flowers and leaf borders.   Publisher a41c 09d2
             Update to 1.3.0 available  [Update]     In use: 2 items
 Stickers (120)                                              Show all
 +----+ +----+ +----+ +----+      a Picker in manage mode, scope: this pack
 Pens (6)                                                    Show all
 +----+ +----+ +----+          one section per content type the pack holds
 Revert to 1.1.0 (28 days)
 Advanced v   Id 0b6e1d9a-...  Installed 28 Oct  Size 18 MB  Publisher a41c09d27be3f580
 [Remove pack]
```

Share is the primary action of a pack the user made; for an installed pack it sits beside Export.
Remove is a visible row, not under Advanced. "In use" appears only when a holder uses an item of an Off
pack. The picker in manage mode offers Hide and Export as pack on its items; Delete is not offered
(items are not Yours). Linked items are shown under their original source with a quiet "also in this
pack". 500 items: Show all opens the type's source-scoped Picker, virtual grid, Filter live. Only a
source-scoped Picker lists an Off pack's items. Compact: preview above, sections stack.

## 3. Settings, Extensions page

```
Extensions                                [Install from file]
---------------------------------------------------------------
 Built in
  <ico> Highlights to cards                              (on)
 From Grapho
  <ico> Flashcards  Study Lab 2.0.1   Update            (on)  ...
 Installed by you
 ::  <ico> Audio on pages  Author 1.1.0
        Can use the microphone and connect to 1 site     (on)  ...
 ::  <ico> Timer   Needs your OK for 1 permission [Review] (off)
 Trash (1)                                                  >
 [Get more]   Safety list updated 2 days ago   [Start in safe mode]
```

The risk line on a row is the generic sentence in short form when read-like and network permissions
coexist ("Can read your library and connect to 1 site"), else only the outside-the-app items, else
nothing. This line is a deliberate exception to the rule against redundant subtext. Row states: on, off, turned
off after problems (line and Turn on again), needs permission, update waiting for your OK, installing,
failed, incompatible, blocked. A Banner "Safe mode. Extensions are off." sits on top in safe mode, and
"Flashcards was turned off after problems." after the third strike. Compact: Start in safe mode moves to
the top bar overflow.

## 4. Extension detail

```
<Back>  Flashcards                                (on)   ...
------------------------------------------------------------
 <icon>  Flashcards   Study Lab   2.0.1   From Grapho
         Turn highlights into review cards.   Publisher a41c 09d2
 It can read this document and send information to cards.example.com.
 Can
   Outside the app   Connect to: cards.example.com   (granted)
   This document     Read this document              (granted)  needed by Export as PDF
 Adds to Grapho                                   [Turn all off]
   Side panel  Cards (on)   Command  Make a card (on)
   Block  Deck (on)         Export format  PDF (on)
 Sent to cards.example.com today: 3.2 MB
 Storage 12 of 100 MB   [Clear data]
 What happened (last 20)     Revert to 2.0.0 (29 days)
 Settings page   Advanced v  Id ...  [Remove extension]
```

Permission lines have a granted switch (not `storage.own`). Turning one off lists the contributions it
turns off. Turning a Read permission off while a Change permission stays on is allowed. States: needs
permission (lines marked New, primary **Review**), update waiting for your OK (Update, Update
later), turned off after problems (Banner and Turn on again), blocked (Banner; switches disabled),
off (contributions listed quiet).

## 5. Import review Sheet

```
+-----------------------------------------------------------+
| Install a pack                                        <x> |
| [preview]  Botanica        Author     1.2.0   Free to share, credit me
|            New publisher    From a file    18 MB          |
| Adds 120 stickers and 2 themes. Nothing changes until you use it.
|------------------------------------------------------------|
|  Stickers 120   Pens 6   Themes 2   Covers 4               |
|  [virtual grid]                          3 items you already have
|  Themes: Spring ok   Dusk adjusted   Neon unavailable      |
|  Not supported in this version (2) v                       |
|------------------------------------------------------------|
|                                      [Cancel] [Install]    |
+-----------------------------------------------------------+
```

Status and primary by situation (lifecycle, Conflicts): Install, Update (with changes and "3 items in
your wheel changed: Keep mine or Use new"), Open, Cancel with secondary Replace with older version,
Restore, Install as a separate pack. Extensions show Source, what it adds, "Asks for N permissions" and
primary Install, secondary Install turned off, and the permissions in the same view (section 8). States: reading, refusal (Close and one sentence), failed (Retry), queue ("1 of 3"). Refused and failed items sit in a Not imported group and are never collapsed. Compact:
full height bottom Sheet, three-column grid, stacked buttons.

## 6. Remove sheet (shown only when something uses the pack)

```
+-----------------------------------------------------------+
| Remove Botanica?                                      <x> |
| Used in 10 covers and 3 notes. They keep working.          |
| Used in the pen wheel (2) and by 3 folders.                |
|   They stay in Yours as "Kept from Botanica".              |
| Removed: 42 unused items     [Show items]                  |
| It stays in Trash for 30 days.                             |
|                                       [Cancel] [Remove]    |
+-----------------------------------------------------------+
```

Lines appear only when non-zero. Items that are Yours are never listed as removed. "At least 3 notes"
while the scan runs. If the active theme comes from the pack: "The Dusk theme stays active as your
copy." With no usage there is no Sheet: the pack is removed at once with the Undo toast. Compact: bottom
Sheet.

## 7. New pack Sheet (also reached from Export as pack)

```
+------------------------------------------------------------+
| Share as pack                                          <x> |
|  14 stickers  (tap to change)                              |
|  Name        [Botanica___________________]                 |
|  More details v                                            |
|     Description [__________________]                       |
|     Licence  v Free to share, credit me                    |
|     Preview  [auto]  [Replace]                             |
|     Version  1.3.0 (only for a pack you already made)      |
|     Advanced v  Id (read only)                             |
|------------------------------------------------------------|
|                  [Save to files]  [Share]                  |
+------------------------------------------------------------+
```

Name is the only required field. Licence options in plain words, technical name quiet beside each:
Free to share, credit me (CC BY 4.0, default); Free to use, no credit (CC0); Share alike (CC BY-SA 4.0);
Only for people I send it to (All rights reserved); Other (text). First share adds an inline "Name on
shared packs" with "Stay anonymous". The item summary opens type tabs with checkable tiles; an empty tab
shows its import or editor action. Items that cannot be shared are unchecked with a reason. Preview is
composed from up to 9 items, at least one per type present. File name below the field shows the
suggested name. Compact: single column.

## 8. Permission consent (ImportReviewSheet kind extension is this view)

```
+------------------------------------------------------+
| Flashcards wants to                               <x> |
|  OUTSIDE THE APP                                      |
|   Connect to: cards.example.com                       |
|     Only allow sites you trust.                       |
|  THIS DOCUMENT                                        |
|   Read this document        needed by Export as PDF   |
|   Change this document      needed by Make a card     |
|                                                       |
|  It can read this document and send information to    |
|  cards.example.com. It loads data from that site.     |
|                                                       |
|  [Cancel]  [Install turned off]  [Install]            |
+------------------------------------------------------+
```

Tiers high risk first; "needed by" shows the slot that implies each permission; the sentence is the
generic one (`trust.md`) and lists every readable source and every host. Foot: Cancel, primary Install (Update on an
update), and one secondary, Install turned off (Update later on an update). Update shows only new lines marked "New" and "Keeps 2
others". A Sheet because it holds a list; 44 dp targets; the list scrolls inside.

## 9. Missing-extension placeholder block

```
+------ Made with "Flashcards" (4c1f) ------+
|  Picture saved in the note                 |
|  [stored fallback image]                   |
|  Deck: Biology chapter 4 (stored text)     |
|--------------------------------------------|
|  Flashcards is not installed here.         |
|  [Get extension]        [Keep as image]    |
+--------------------------------------------+
```

The name is the stored name in quotes with the short id until the extension is installed. The status
line changes by cause (not installed, off, in Trash, blocked, needs a newer version, "Not available on
this device" with Keep as image only). Actions show on focus or tap and move into a Menu below 200 dp
wide. No animation; the frame never draws over neighbours.

## 10. Safe mode Banner

```
 <ico> Safe mode. Themes, packs and extensions are off.
       [Turn them back on]   [Restart normally]
```

Inline at the top of the Library and Settings; in a note a one line strip above the note bar that does
not move the page. Turn them back on opens the one by one extension list with the previous theme offered
("Apply Dusk").

## 11. Picker footer slots and routes

Footer in fixed order, kept in both modes: **Get more**, **Import**, **New** (editable types), **Manage**
(pick mode in a side panel or Settings only). Manage mode replaces only the selection area. The source
filter ends with **Hidden** (always present; its count may be 0) listing hidden items from every source
with Unhide; an item Menu in every picker has **Hide**. With two sources of one name the author is added
("Botanica, Study Lab").

### Route for every host

A pushed view counts toward the depth limit of three. "Panel" means the side panel.

| Host | Pick | Get more | Import | Export as pack | Depth |
|---|---|---|---|---|---|
| Tool wheel | compact pens and colours | More opens the Panel picker for pens (footer there) | via the Panel | via the Panel | none in the wheel |
| Panel in a document (stickers, templates, pens, colour sets) | picker with footer | pushed view in the Panel: list, pack with review, consent | file chooser, then review pushed in the Panel | Manage mode, Select, Export as pack, opens the New pack Sheet over the document | 3 |
| Sheet host (cover in New notebook, folder style, canvas preset, template in New notebook) | the picker is a pushed view in the Sheet (the form keeps its draft) | pushed inside the same Sheet: list, pack with review | pushed review | not offered: Manage lives in Settings or the Panel | 3 |
| Settings page, Content, a type | manage mode with footer kept | opens the catalogue Sheet | file chooser, then review Sheet | Select, Export as pack, opens the New pack Sheet | 3 inside a Sheet |
| Settings, Appearance (themes) | picker with footer, same rules as a Content page | catalogue Sheet | review Sheet | Select, Export as pack | 3 |
| Settings, Packs | n/a | Get more button: catalogue Sheet | Import button | New pack Sheet | 3 |
| Library New menu, Pack | n/a | n/a | review Sheet | New pack Sheet | 3 |

Catalogue unavailable: Get more stays enabled and opens the empty state (Import from file, Open
grapho.one). During import a thin progress item sits above the footer. In narrow hosts the footer is
measured in three steps: labels, then icon buttons with tooltips, then one More button. It never wraps.
The source filter is measured too: chips while they fit, at most six sources, otherwise its dropdown.
