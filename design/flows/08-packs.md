# 08 Packs: find, install, make, share

## Goal

Get content (pens, colour sets, covers, folder styles, canvas presets, stickers, templates, themes)
into the user's hands and into use, make a pack from their own work, and share it.

Deeper rules (lifecycle, update, safe mode) are in `design/content-system/flows.md`, referenced by
section name. This file lists every user-visible outcome so it can be read on its own.

Counting rule: see 01. Compact adds 1 where the Picker footer is collapsed into More.

## Entry points

| Job | Where |
|---|---|
| Find | the "Get more" footer slot in any Picker; Settings, Packs, Get more |
| Install from a file | Picker footer Import; Settings, Packs, Import; New, Import, Pack; share target; drag in; open with |
| Make | a Picker or Content page in manage mode, Select, Export as pack; Settings, Packs, New pack |
| Share | Toast action after export; pack Menu and detail Menu |
| Manage | Settings, Packs (`grapho://packs`); a pack's page (`grapho://packs/<id>`) |

## Steps: find and install, used at once

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | In the cover Picker (New notebook), Get more (on compact, More then Get more) | view pushed in the Sheet | `CatalogueView` recipe, `SearchField`, `ListRow` per pack |
| 2 | Tap a pack: detail with preview, author, size, licence, contents by type, changelog | pushed view | `SourceDetail`; the primary is "Review and install", which reviews in place |
| 3 | Review in place: status (New, Update, Already installed, Older), content grid, refusals | same view, no new push | `ImportReviewSheet` content (pack variant); primary "Install" |
| 4 | Install; views pop back to the host Sheet | Sheet | `Button` |
| 5 | Progress, then Toast "Botanica installed" with an action "Show" | Progress item, Toast | `ProgressItem`, `Toast` |
| 6 | The pack is now a source in the Picker's `SourceFilter` | Sheet | `Picker` |

Started from Settings, Packs, the Toast action "Show" opens the pack's page, whose content tiles
have Use for the current target type.

## Steps: install from a file, share intent, drag in

All land in the same `ImportReviewSheet`. Share intent with the pen down: the Sheet is queued and a
Toast "Pack ready to review" with Review appears when the pen lifts. Drag in shows the `DropZone`
"Drop to install"; several files queue as "1 of 3" with Install all and Skip this one.

## Conflict outcomes at import

| Situation | Status line | Buttons |
|---|---|---|
| New id | New | Install, Cancel |
| Same id, same version | Already installed | Open, Cancel |
| Same id, higher | Update from 1.2.0 | Update, Cancel (changes list shown) |
| Same id, lower | Older than installed 1.3.0 | Cancel (primary), "Replace with older version" (text) |
| Same id and version, different bytes | Different from installed | Replace, Cancel |
| Different id, same name | New, "Same name as one you have, by Sample Author" | Install, Cancel |

## Refusals (inline, primary becomes Close)

"This file is not a Grapho pack." "This pack is damaged. Download it again." "This pack is too large
(300 MB). The limit is 250 MB." "Needs Grapho 1.5 or newer. You have 1.4." "There is not enough
space to install this pack."

## Steps: make a pack

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | A Picker's Manage, or Settings, Content, a type | Picker, manage mode | `Picker` |
| 2 | Select items; the bar reads "Export as pack, 14" | Selection bar | `SelectionBar` |
| 3 | Items view (by type, removable, licence reasons), then Details view: name, author, version, description, licence, preview, advanced id | Sheet | `PackBuilderSheet` recipe: `TextField`, `TextArea`, `Select`, `Checkbox` |
| 4 | Export | Progress item for large packs, Toast "Saved Botanica.graphopack" with the action "Share" | `Toast` |

New pack (Settings, Packs, New pack) opens the same Sheet with checkable tiles by type. One wizard.

## Steps: share

The Toast action "Share" and the pack Menu and detail Menu hand the file to the platform share
sheet; "Show in Files" is the second row. A pack with `redistribute: false` shows "This pack cannot
be shared" and the actions are disabled.

## Steps: manage, remove, restore

On and off: the row `Toggle`, Toast "Botanica is off" with Undo; usages stay. Remove follows
SYSTEM-MAP 4.2: a pack with nothing in use is removed at once with Toast "Botanica removed" and
Undo. A pack with items in use first shows the `RemoveSheet` ("3 items you use stay as your own
copies", primary "Remove"), then the Toast. Removed packs live in Trash only (10); the Packs page
shows one link row "2 removed, in Trash" that opens Trash filtered to Packs. Update: Dot on the
Packs entry, Update on the pack's page. Missing pack in a received document: Banner "Uses 6
stickers from Botanica by Sample Author." with Keep in my library and Get the pack.

## Tap counts (compact in brackets where different)

| Task | Taps |
| --- | --- |
| Install from the catalogue, from the New notebook Sheet | 7 (New, Notebook, Cover row, Get more [+1], pack, Review and install, Install); the cover is one tap away after, 1 more |
| Install from Settings | 6 (Settings, Packs, Get more, pack, Review and install, Install); compact +1 for the Sources group |
| Install from a file | 3 from a Picker (Import, file, Install); 5 from Settings (Settings, Packs, Import, file, Install) |
| Make and export, from a Picker | Manage, N ticks, Export as pack, Export; for 5 items 8; Details fields add taps only if edited |
| Share | 1 (Toast action) |

## States

| State | What the user sees |
|---|---|
| No packs | Settings, Packs `EmptyState` sm "No packs yet." Get more, Import |
| Offline | Catalogue Banner "You are offline. Showing packs you have already seen." Get disabled for uncached files; Import stays |
| No catalogue | "No pack catalogue yet." Import from file, Open grapho.one |
| Loading | `Skeleton` tiles after 300 ms; Cancel closes |
| Error | "Could not load packs. Try again." |
| 3 GB of stickers | Size shown before install; install is a Progress item with Cancel |
| Permission denied (files) | The Import row explains and links to system settings |
| Very large | Catalogue and content grids virtualise |

## Exits

Cancel, Back (pushed view) and Escape change nothing; nothing is installed, exported or removed
until the final primary. A failed install keeps the Sheet open with "Could not install. There is
not enough space." and Try again.

## Wireframes

Get more inside the New notebook Sheet:

```
+------------------------------------------------+
| < Get more covers                         [x]  |
| [ Search packs                       ]         |
| (i) You are offline. Showing packs you have    |
|     already seen.                              |
| Botanica  Sample Author  4 MB        [ Get ]   |
| Lab book  Grapho    2 MB        Installed      |
| Moon      Sample Author    9 MB       [ Update ] |
+------------------------------------------------+
```

Settings, Packs:

```
+-----------+------------------------------------------------+
| Packs  .  | Packs  [Filter] Sort v    [Get more][Import][New]|
| Extensions| Botanica 1.2  Sample Author  42 items    [on ]  ... |
|           | Lab book 2.0  Grapho    12 items     [off]  ... |
|           | 2 removed, in Trash                          >  |
+-----------+------------------------------------------------+
```

Compact: pack rows are full width; views are pushed in a bottom Sheet. German: "Pruefen und
installieren" is shortened to "Installieren" in the primary when the label does not fit.

## Access

Screen reader: each pack row reads name, author, state; Install progress is announced every 25%.
Keyboard: Tab to rows, Enter opens, Space toggles. Focus returns to the Get more row after the
views pop back.
