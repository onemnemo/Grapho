# 12 Settings: index, contextual settings, shortcuts, deep links

## Goal

Find any preference in one place, change the same setting from where it matters without a second
copy, remap a shortcut, and have every setting, content page, pack and extension addressable.

Counting rule: see 01.

## Entry points

- Settings at the Sidebar foot (library) and in the overflow of the library Top bar; Note bar More,
  Settings; Ctrl+, (`app.settings`); a deep link; "More in Settings" on a contextual control.
- `CommandPalette`: Ctrl+K on a keyboard. On touch and stylus the palette opens from the "Command palette"
  row in the top bar overflow and in the Note bar More. Counts below
  that use Ctrl+K are labelled keyboard.
- Settings is a place, never a modal. Back returns to where it was opened, at the same scroll and
  selection.

## Structure

App: General (includes Keyboard), Appearance, Language, Pen and tools, Toolbar, Sync, Battery,
Search index, About. Content: one page per content type (Themes links to Appearance). Sources:
Packs, Extensions. On compact and medium, Content and Sources are collapsed groups in the list.
Text size follows the operating system.

## Steps: find a setting

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Open Settings | Settings page | `Sidebar` (Settings variant) |
| 2 | Type in the index field; it narrows the whole registry, including content, pack and extension pages | Settings page | `SearchField` (Filter scope) |
| 3 | Tap a result | Settings page | `SettingRow` result: name and section; the page opens scrolled to it with the row highlighted for 2 s |
| 4 | Change it; it applies at once | Settings page | `Toggle`, `SegmentedControl` (3 short options at most), `Select`, `RadioList`, `Slider`, `Stepper`, `ShortcutField` |

Rows may show a helper line and a Reset for a non-default value. A device-scoped setting shows
"This device only."

## Steps: contextual settings

A contextual appearance is the same setting and value, with "More in Settings". Menus hold commands
only, so a Menu row for a setting is a link row that opens Settings there.

| Where | Shows | Surface |
|---|---|---|
| First run | Language, hand, pen-only | Sheet (01) |
| Tool settings tab | Width, colour, smoothing, pressure, slot count, pen-only, snap | `SidePanel` |
| Library Top bar overflow | Appearance (opens Settings), Default view, Default sort as Menu radio rows | `Menu` |
| New notebook Sheet | Default template | `Picker` |
| Folder Menu | "Sync this folder" is a link row opening Settings, Sync | `Menu` |
| Picker, manage mode | Hidden items | `Picker` |
| Export Sheet | Default export format | `Select` |

Inside a Sheet, "More in Settings" asks to keep or discard changes, closes the Sheet, then
navigates.

## Steps: remap a shortcut

Settings, General, Keyboard: a `ListRow` per command with its shortcut per platform. Tap a row:
`ShortcutField` captures the next chord; a clash with a built-in names the command and refuses
("Already used by Undo"); Reset restores. An extension command that clashes with a built-in is left
unbound and its page says so. Gesture mappings (two-finger tap, three-finger tap, S Pen click, hold,
barrel) are in Pen and tools as `Select` rows of commands.

## Steps: deep links

`grapho://settings`, `grapho://settings/pen`, `grapho://settings/pen/pen.penOnly`,
`grapho://content/covers`, `grapho://packs/<id>`, `grapho://extensions/<id>`. The core resolves a
link to a place; an unknown target falls back to the nearest parent with a toast ("Setting not
found"). A link held during first run applies after. Commands are not linkable.

## Tap counts

| Task | Taps |
| --- | --- |
| Open Settings | 1 from the library; from a note 2 (More, Settings), mid-writing 3 |
| Change theme | 4 (see 07) |
| Find "palm guard" | 3 (Settings, index field, result) plus typing; keyboard Ctrl+, then type |
| Open Packs from a note | 4 (More, Settings, Sources group on compact, Packs); expanded 3 |
| Change handedness | 3 (Settings, Pen and tools, Left) |
| Remap a shortcut | 4 (Settings, General, row, press the chord) |

## States

| State | What the user sees |
|---|---|
| Index, no match | "No settings match 'sync2'." Clear |
| Sync off | `Banner` "Sync is off." with a `Toggle`; sync is off until chosen (14) |
| Sync states | up to date, syncing, offline (queued), conflict, storage full: a status line on the Sync page and the icon in the Sidebar foot (14) |
| Battery | "Indexing paused. Not charging." under `battery.chargeOnly` |
| Search index | A progress line and "Rebuild index"; a running rebuild is a Progress item |
| Safe mode | `Banner` "Safe mode. Extensions are off." |
| Updates | `Dot` on the Packs and Extensions entries only |
| 200% text, German | Rows wrap; the control moves below its label; no segmented control with more than 3 short options |
| Save error | Inline "Could not save. Try again." Retry |
| Offline | Only Sync and extension network access have an offline state |
| Very large | 50 extensions list with Filter; Content pages virtualise |

## Exits

Back returns to the opening place; on compact, from a page to the list first. Escape is Back. A deep
link to a gone target lands on the parent place with a toast.

## Wireframes

Expanded:

```
+--------------+---------------------------------------------+
| [Filter all ]| < Pen and tools                              |
| App          |  Pen-only drawing                  [ on  ]   |
|  General     |   Fingers scroll and zoom, never draw.       |
|  Appearance  |  Two-finger tap Undo               [ on  ]   |
|  Language    |  Pen button click        [ Open wheel v ]    |
|  Pen & tools |  Pen button hold         [ Eraser v ]        |
|  Toolbar     |  Palm guard      (Standard)(Strict)          |
|  Sync        |                                              |
|  About       |                                              |
| Content >    |                                              |
| Sources >    |                                              |
+--------------+---------------------------------------------+
```

Compact (list, then a page pushed with Back):

```
+--------------------------+     +--------------------------+
| Settings                 |     | < Pen and tools          |
| [ Find a setting     ]   |     | Pen-only drawing  [ on ] |
| General               >  |     | Two-finger Undo   [ on ] |
| Appearance            >  |     | ...                      |
| ...                      |     |                          |
+--------------------------+     +--------------------------+
```

Medium: list and page as two columns in landscape, one at a time in portrait.

## Access

Keyboard: Ctrl+, opens; Tab from the index field to the list; Enter opens; Space toggles. Screen
reader: each row reads label, helper, state and "This device only"; the highlighted row after a
search result is focused and announced. Focus returns to the opener on Back.
