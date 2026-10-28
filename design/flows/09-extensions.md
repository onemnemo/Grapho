# 09 Extensions: install, use, missing, broken

## Goal

Add behaviour with an extension (a panel, a tool, a command), know what it can access before it
runs, use it where the note is, and never lose a note because an extension is gone.

Deeper rules are in `design/content-system/flows.md` (sections "Extension install with
permissions", "Permission increase on update", "Missing extension", "Safe mode"). Every
user-visible outcome is listed here. Slot rules: SYSTEM-MAP section 7. Counting rule: see 01.

## Entry points

| Job | Where |
|---|---|
| Find, install | Settings, Extensions, Get more or Import (`.graphoext`); share target; drag in |
| Use | Side panel tab rail, the wheel and quick slots, pinned Note bar items, the Extensions group of menus, the palette |
| Manage | Settings, Extensions (`grapho://extensions`); one page per extension |
| Missing | the placeholder frame in a note, its Menu |

## Steps: install

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Settings, Extensions, Get more or Import | Settings page | `SettingRow`, `Button` |
| 2 | Review: name, author, source (From Grapho, Installed by you), what it adds ("A side panel, 2 commands, a block"), size, version | Sheet | `ImportReviewSheet`, extension variant |
| 3 | Permissions | view pushed in the Sheet | `ConsentView` recipe: each permission in plain words, high risk first |
| 4 | **Install** (primary, grants as listed), text button **Install without permissions**, Cancel | Sheet | `Button` |
| 5 | Toast "Flashcards installed" with the action "Show" | Toast | `Toast` |

Footer: Cancel, then the primary "Install"; "Install without permissions" is a text button above
the foot, so no label is a negation of another and the foot holds two buttons at 360 dp even at
40% longer strings ("Installieren"). Nothing runs before step 4.

## Steps: use a side panel

1. Panel toggle (Ctrl+.); the extension's tab sits after the built-in tabs in `SidePanel` (six
   shown, the rest in the rail's overflow).
2. A sideloaded extension's panel header reads "Flashcards, installed by you" the first time.
3. The body is a declarative tree rendered by `ExtensionPanelHost` from standard controls
   (`ListRow`, `Button`, `Toggle`, `Slider`, `TextField`, an image, a drawing surface). It cannot
   draw outside its panel or a block.
4. Close: the panel button, Escape, the tab toggle.

## Steps: use a tool

An extension tool is a segment in the wheel's Tools ring (eight fit; more on the Tools page that
the user curates in 03, "Set up my tools") and can be pinned to a slot. Its commands appear in the
palette grouped under its name and in the Note bar if pinned (at most two extension items among
four).

## Missing, turned off or broken

- **Missing**: a block shows its stored fallback in a placeholder "Made with Flashcards". Tap: Menu
  with Get extension, Keep as image, or Turn on. The note opens normally; no Dialog.
- **Needs a newer extension**: the fallback with "Needs a newer version of Flashcards" and Update.
- **Broken**: if it fails to start or crashes, the runtime turns it off and shows a `Banner` in its
  own panel "Flashcards stopped. It is turned off." with Turn on again. After two failed app
  starts, safe mode starts: a Banner on the library and Settings "Safe mode. Extensions are off.";
  the default theme is active; extensions are turned on one by one in Settings, Extensions; one
  that fails again is marked "Turned off after an error".
- **Update with more permissions**: only the new permissions are shown with Allow and update,
  Update turned off, Cancel. Cancel keeps the old version.
- **Remove**: at once with Toast "Flashcards removed" and Undo; it goes to Trash. Erasing its
  stored data is irreversible: a `Dialog` "Delete the 12 decks Flashcards saved? This cannot be
  undone." with "Delete 12 forever".

## Outcomes at install

| Situation | Review shows | Buttons |
|---|---|---|
| New | New | Install, Cancel |
| Already installed, same version | Already installed | Open, Cancel |
| Newer | Update from 1.1 | Update, Cancel |
| Incompatible | "Needs Grapho 1.5 or newer" | Close |
| Unknown permission | "This extension asks for something this version of Grapho cannot grant." | Close |
| Damaged or not allowed | one sentence and a reason | Close |

## Tap counts

| Task | Taps |
| --- | --- |
| Install from a file | 6 (Settings, Extensions, Import, file, Review permissions, Install); from a note +2 (More, Settings); compact +1 |
| Enable Audio on pages and use it, first time | 4 (Settings, Extensions, Audio, Toggle) then 1 per use (palette or panel tab) |
| Open a panel | 2 (panel toggle, tab); mid-writing 3 |
| Use an extension tool | 2 (tap tool, tap its segment); 1 from a slot |
| Remove | 2 (row ..., Remove); Undo is one more tap in the Toast |

The extra install taps are the consent step.

## States

| State | What the user sees |
|---|---|
| None installed | The built-in group (off) and "Installed by you: nothing yet. Get extensions or import a file." |
| Offline | Network access by an extension is blocked and its page says so; Get more shows the offline Banner |
| Needs permission | Row "Needs your OK for 2 new permissions" with Review; no UI contribution until granted |
| Platform prompt | Camera or microphone prompt at first use, in context; denied shows the extension's own inline line |
| Clashing shortcut | The command is unbound and its page says so |
| 50 extensions | Settings list virtualises with Filter; a menu's Extensions group shows at most five |
| Panel loading, error | `Skeleton` after 300 ms; inline error with Retry in the panel only |
| Off with blocks in notes | Detail line "Its blocks in notes show their saved picture." |

## Exits

Cancel and Escape leave install with nothing installed. Back from the pushed consent view returns
to the review. Failure: refusal in the Sheet, then Close.

## Wireframes

Consent view (pushed in the Sheet):

```
+------------------------------------------------+
| < Flashcards                              [x]  |
| By Sample Author   Installed by you            |
| It will be able to:                            |
|  ! Read your notes and send data over the net  |
|  - Use the camera when you open its panel      |
|  - Save its own data (up to 20 MB)             |
|         Install without permissions            |
+------------------------------------------------+
|                         [ Cancel ] [ Install ] |
+------------------------------------------------+
```

Document with an extension panel, Expanded, right-handed:

```
+------------------------------------------------------------+
| < Week 4     12 of 40    [pin][Flash]               [More] |
+----+-----------------+-------------------------------------+
| T  | Pages Layers .. | Flashcards, installed by you        |
| S1 | Insert Tool [F] | [ list rows, buttons ]              |
| Un |                 |                                     |
+----+-----------------+-------------------------------------+
```

Compact: the panel is the bottom panel at half height; the consent view is a pushed view in the
bottom Sheet. Medium: the panel overlays the off-hand edge at 320 dp.

## Access

Keyboard: the panel toggle and tab shortcuts; extension commands show in the palette. Screen
reader: each permission is a list item read in order; the placeholder reads "Flashcards block,
extension missing, menu". Focus returns to the Install entry after the Sheet closes.
