# Screen: Pack detail (and extension detail)

## 1. Purpose

One page for a pack or an extension: who made it, its version and state, what it holds or adds, what it
may do, and the actions on it. A pack holds content only; an extension adds behaviour. The page is
SourceDetail, a child page pushed inside Settings, Packs or Settings, Extensions (`grapho://packs/<id>`,
`grapho://extensions/<id>`). The sheets that lead to it (review and install, consent, remove) are
specified here too, because the page cannot be understood without them.

## 2. Flows served

08 packs (main), 09 extensions (main), 10 (remove, restore), 07 (a theme pack), 13 (missing pack in a
received document links here via Get the pack).

## 3. Layout and regions

Regions: TopBar `place` settings with Back, Sidebar `kind` settings (Expanded), page header, summary,
state Banner, foot rows, contents, Toast stack. The page scrolls as one; each section's Picker grows with
the page and virtualises.

### Expanded, pack

```
+--------------+-----------------------------------------------------+
| Sidebar      | < Botanica                      (on)  [Share][Export] ...|
| (settings,  | [preview]  Sample Author  1.2.0  Free to share, credit me |
|  Packs       |  Ferns, flowers and leaf borders.  Publisher a41c 09d2   |
|  selected)   | (i) Update to 1.3.0 available   [ Update ] [Skip version]|
|              | In use: 2 items                                          |
|              | Revert to 1.1.0 (28 days)    Advanced v                  |
|              | [ Remove pack ]                                          |
|              | Stickers 120                                  Show all    |
|              |   [tile][tile][tile][tile][tile]                         |
|              | Pens 6                                         Show all    |
+--------------+-----------------------------------------------------+
```

### Medium and Compact

Medium: portrait is one column with Back; landscape keeps the Settings list beside. Compact: the header
actions fold into the ... Menu (Share, Export, Remove), the Toggle stays in the header, the summary
stacks the preview above the text. Foot rows keep their order above the contents. Short: as the width
class; the Banner and foot rows scroll with the page.

Handedness: nothing mirrors. Right to left mirrors order and directional icons.

## 4. Components by region

| Region | Component and variant | Notes |
|---|---|---|
| Header | IconButton (Back), title, Toggle (state), Button `secondary` actions, IconButton more with Menu | When a Banner action is showing it is the one primary; header actions are `secondary`; otherwise Share (made by you) or Export |
| Summary | SourceDetail `kind` pack or extension; MediaImage for the preview (120), ListRow lines for author, version, licence | Description up to four lines then "Show more"; no preview: the type icon on `surface/sunken` |
| State Banner | Banner `tone` neutral, warn or danger | One at a time, highest priority wins: Blocked, Failed, Incompatible, Needs permission, Update |
| Foot rows | ListRow plain: Revert to previous version, In use, Advanced disclosure (Id, Installed, Size, Publisher), Remove as a visible row | Above the contents so a long list never buries them |
| Contents (pack) | One section per content type, each a Picker (`scope` this pack, `mode` manage, no source filter) with a heading showing type and count and a Show all link; collapsible | Selection bar `picker` actions: Use for the current target type, Duplicate, Hide, Export as pack |
| Contents (extension) | ListRow list of contributions ("A side panel, 2 commands, a block") and a permissions list; Toggle per contribution where allowed; link row to its places | The list of every place it adds to, with Turn off |
| Sheets | ImportReviewSheet `pack` or `extension` (the consent view), Sheet recipe RemoveSheet, Dialog | Section 5 |
| Feedback | ProgressItem `inline` under the summary with Cancel, Toast, Skeleton | The Toggle is disabled while installing or updating |

## 5. Sheets and conflicts that belong to this page

**Review and install** (from Get more or a file): status line, the content grid, refusals, primary
"Install" (a pack) or "Install" with a text Button "Install without permissions" above the foot (an
extension). Consent view, pushed in the Sheet: each permission in plain words, high risk first, "needed
by" labels. Nothing runs before the primary. Foot: Cancel and the primary only, two buttons at 360 wide
even at 40% longer strings.

| Situation at import | Status line | Buttons |
|---|---|---|
| New id | New | Install, Cancel |
| Same id, same version | Already installed | Open, Cancel |
| Same id, higher | Update from 1.2.0 | Update, Cancel (changes list shown) |
| Same id, lower | Older than installed 1.3.0 | Cancel (primary), "Replace with older version" (text) |
| Same id and version, different bytes | Different from installed | Replace, Cancel |
| Different id, same name | New, "Same name as one you have, by Sample Author" | Install, Cancel |
| Different publisher, same id | "This is not an update. It is from a different publisher than your Botanica." | Install as a separate pack |
| Refusal | "This pack is damaged. Download it again." / "Needs Grapho 1.5 or newer. You have 1.4." / "There is not enough space to install this pack." | Close |

**Remove**: a pack with nothing in use is removed at once; Toast "Botanica removed" with Undo. A pack with
items in use first shows RemoveSheet: "3 items you use stay as your own copies", primary "Remove". The
kept items are labelled "Kept from Botanica". The pack goes to Trash; Restore from Trash brings it back.
Removing an extension is the same (at once, Toast with Undo, to Trash); erasing its stored data is a
separate Dialog `irreversible`: "Delete the 12 decks Flashcards saved? This cannot be undone." with
"Delete 12 forever". Turn off keeps everything where it is; its blocks in notes show their saved
picture.

**Update with more permissions**: only the new permissions marked New; primary "Update", text "Update
later" (the row reads "Update waiting for your OK", the extension stays On); Cancel keeps the old
version.

## 6. States

| State | What changes |
|---|---|
| On | Toggle on; contents live |
| Off | Toggle off; summary and items quiet (`text/muted`); its content leaves the Pickers; usages stay; an "In use" line when a holder uses an item |
| Installing, Updating | ProgressItem inline with Cancel; Toggle disabled |
| Update available | Neutral Banner (dismissible) with Update and Skip this version; a Dot on the list entry |
| Failed | Danger Banner with the reason and Try again; the previous state is untouched |
| Incompatible | Warn Banner "Needs Grapho 1.5 or newer"; no Toggle; Update the app |
| Blocked | Danger Banner "Grapho stopped this pack. Reason."; Toggle disabled with the reason; only Remove works |
| Broken extension | Banner in the extension's own panel "Flashcards stopped. It is turned off." with Turn on again; after two failed starts, safe mode (settings.md) |
| In Trash | Banner "Removes in 12 days" with Restore and Delete now |
| Needs permission | Banner with Review; permission lines marked New |
| Partial | Note "Some content is not supported in this version" with the list |
| Waiting to download | Summary only; contents say "Waiting to download" |
| Loading | Skeleton over the Pickers after 300 ms, held 300 ms |
| Error | Contents row with Retry; the header stays |
| Offline | Network access by an extension is blocked and the page says so; the catalogue shows its offline Banner |
| Very large | A 3 GB sticker pack shows size before install and installs as a ProgressItem with Cancel; sections virtualise; 50 extensions list with Filter |
| Built in (Grapho, 312 items) | No author line, no Toggle, no Remove |
| Long German text | Name one line with ellipsis, full text on focus; description four lines then "Show more"; "Pruefen und installieren" shortens to "Installieren" in the primary when it does not fit |
| 200% text | Header actions fold into the Menu; the summary stacks; foot rows wrap |

## 7. Behaviour on resize and split screen

Class changes keep the open page, scroll, the collapsed sections and any open Sheet or review (a pushed
view in the Sheet keeps its place). A running install keeps its ProgressItem across classes. A Sheet is a
bottom Sheet on Compact, centred otherwise; the review never loses its status line.

## 8. Keyboard and focus order

Order: Back, header Toggle and actions, Banner buttons, foot rows, then each section heading and its
Picker (Filter field, grid, footer). Enter opens a tile, Space toggles, Tab reaches Show all. Focus returns
to the Get more row after the review pops back, to the Install entry after a Sheet closes, to the row in
the Packs list after Back. Screen reader: each pack row reads name, author, state; install progress is
announced every 25 percent; each permission is a list item read in order; a Banner is announced when it
appears.
