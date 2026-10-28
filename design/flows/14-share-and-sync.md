# 14 Share a document, resolve a conflict, turn on sync and restore

## Goal

Hand work to someone else in a format they can open, keep both versions when two devices disagree,
and bring a library to a new device. These carry the highest stakes for principle 2.

Counting rule: see 01.

## Entry points

| Job | Where |
|---|---|
| Share | document More, Share (Ctrl+Shift+S); the item Menu; the Selection bar More |
| Export | the same places, Export (Ctrl+Shift+E) |
| Compare | the Banner "Both versions kept." on a library item or a document |
| Sync on, restore | Settings, Sync (`grapho://settings/sync`); the empty library's "Sign in to restore" |

## Steps: share

Share is Export to a chosen format handed to the platform share sheet.

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Share (More, Share; or ..., Share) | Menu to Sheet | `ExportSheet` recipe |
| 2 | Choose a format: `RadioList` PDF (flattened, the default for sharing), Images, Grapho notebook file; pages All or Range; "Include other pack assets" is not asked (a document carries its own copies) | Sheet | `RadioList`, `SegmentedControl` (All, Range), `TextField` |
| 3 | Share (primary) | Progress item if long | `ProgressItem` with Cancel |
| 4 | The platform share sheet opens with the file; Toast on return "Shared Week 4.pdf" | system | `Toast` |

Export is the same Sheet with primary "Export", which writes a file and ends with Toast "Saved Week
4.pdf" and the action "Show in Files". Many selected items share as one zip. Failure: Toast "Could
not export. There is not enough space." Retry.

## Steps: resolve a conflict

Two devices changed the same notebook. Neither version is lost: both are kept, and a `Banner` reads
"Both versions of Week 4 are kept." with Compare.

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Compare | Sheet | `CompareView`: the two versions side by side as page strips (this device, Tablet, with times), differing pages marked |
| 2 | Choose | Sheet | `RadioList`: Keep both (default; the other becomes a copy "Week 4 (Tablet)"), Keep this device's, Keep the other |
| 3 | Apply | Sheet | `Button` "Resolve"; the discarded version goes to Trash for 30 days |
| 4 | Toast "Conflict resolved" with Undo | Toast | `Toast` |

On compact the strips stack with a `Tabs` switch (This device, Tablet). Keep both needs no further
step. The Banner stays until resolved.

## Steps: turn on sync, restore on a new device

| # | Step | Surface | Components |
|---|---|---|---|
| 1 | Settings, Sync, `Toggle` "Sync" (off until chosen) | Settings page | `SettingRow`, `Toggle` |
| 2 | Sign in in the system browser; return to the app | system | `Button` "Sign in" |
| 3 | Choose scope: `RadioList` All notebooks, Chosen folders (a `FolderChooser` list); mobile data `Toggle` | Settings page | `RadioList`, `Toggle` |
| 4 | First sync runs as a Progress item "Syncing 412 notebooks" | Progress item | `ProgressItem`; the library fills as items arrive |

New device: first run (01), then the empty library's "Sign in to restore" runs steps 2 to 4; the
library fills, items first show "Downloading" tiles and open when ready (13).

## Sync states

`SyncIndicator` at the Sidebar foot and the status line on the Sync page: off, up to date, syncing,
offline (queued, "3 changes waiting"), conflict (Banner with Compare), storage full (Banner "Your
sync storage is full. Free space or change plan." with a link). Storage full on the device uses the
document Banner in 13.

## Tap counts

| Task | Taps |
| --- | --- |
| Share a notebook as PDF | 4 (More, Share, Share, pick a target in the system sheet); mid-writing 5 |
| Resolve a conflict, keep both | 2 (Compare, Resolve; Keep both is preselected) |
| Turn on sync | 3 (Settings, Sync, Toggle) plus sign in |
| Restore on a new device | 3 (Sign in to restore, sign in, scope confirm) |

## States

| State | What the user sees |
|---|---|
| Share, large | `ProgressItem` with Cancel; the Sheet is already closed |
| Share, nothing to share | Share disabled with the reason on an empty document |
| Compare, 2,000 pages | Page strips virtualise; only differing pages are listed first |
| Offline | Sync shows "Offline, 3 changes waiting"; Share and Export still work |
| Sign-in fails | Inline "Could not sign in." Try again |
| Storage full | see Sync states |
| Very large | Syncing 10,000 notebooks: Progress item with a count; the library is usable meanwhile |

## Exits

Cancel and Escape close the Sheet with nothing changed. Resolve applies; Undo in the Toast. Sign-in
cancelled returns to the Sync page with the Toggle off.

## Wireframes

Share Sheet and Compare, Expanded:

```
+------------------------------------+   +--------------------------------------+
| Share Week 4                  [x]  |   | Compare Week 4                  [x]  |
|  (x) PDF                           |   |  This device, Mon 10:12 | Tablet, 09:40|
|  ( ) Images                        |   |  [p1][p2][p3*]          | [p1][p2][p3*]|
|  ( ) Grapho file                   |   |  * differs                           |
|  Pages  (All) (Range)              |   |  (x) Keep both                       |
|        [ Cancel ] [ Share ]        |   |  ( ) Keep this device's              |
+------------------------------------+   |  ( ) Keep the other                  |
                                         |        [ Cancel ] [ Resolve ]        |
                                         +--------------------------------------+
```

Compact: both are bottom Sheets; Compare uses `Tabs`. German check: "Beide behalten", "Zusammenfuehren"
are rows, not buttons, so they wrap instead of truncating.

## Access

Screen reader: the Compare strips are lists "Page 3, differs"; radio group semantics; progress is
announced every 25%. Keyboard: Ctrl+Shift+S, arrows in the radio list, Enter applies. Focus returns
to the Banner's Compare button, or to the item.
