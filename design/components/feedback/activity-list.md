# ActivityList

## 1. Purpose

The record of what the app is doing and has told you: running jobs, recent results, sync, and notices. It
is a Popover (SYSTEM-MAP 3) that opens from the StackChip, the SyncIndicator and the "N more notices"
control. It exists so nothing that matters is lost when a Toast leaves.

**When not to use**
- A history of the library: Trash and the journal.
- Anything that needs Done or Cancel: a Sheet.

## 2. Anatomy

```
 Activity                                  [ Clear ]
 ---------------------------------------------------
 Sync        Offline, queued                [ Sync now ]
 Notices     (full banner content and actions)
 Running     ProgressItem rows
 Recent      finished jobs, danger toasts, Undo toasts
```

Parts: `header` (title, Clear), `sync row` (state in words and one action), `group label` (`type/caption`,
`text/muted`), `rows`, `separator` (`border/subtle` hairline). Width `size/popover/width`; height up to
`size/popover/max-height`, then it scrolls. Groups appear only when they have rows, in this order: Sync
(hidden if sync was never configured), Notices, Running, Recent. With nothing in any group it shows
"Nothing running" as EmptyState `quiet`.

**Notices** holds banners that did not fit (see Banner) at full content with their actions, and every
`danger` Toast, with its action. **Recent** holds finished jobs and Toasts moved here.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `placement` | `popover` \| `panel` |

`popover` on medium and expanded widths. On compact it opens as the bottom panel (SYSTEM-MAP 3, side panel
on compact), because a scrolling list with Cancel is a panel, not a small choice. Rows use `size/row/list`.

## 4. States

| State | Tokens that change |
|---|---|
| Default | `surface/raised`, `border/subtle`, `elevation/popover`, `text/default` |
| Row hover, press, selected | `hover/fill`, `press/fill`; a status icon in the row switches to `text/default` there |
| Focus-visible | focus recipe on the row's buttons |
| Disabled | not used |
| Loading | rows show their own progress |
| Empty | EmptyState `quiet`: "Nothing running" |
| Error | a failed row (reason and Retry) |
| Drag | not used |

Reduced motion: opens by opacity, 0 ms.

## 5. Content rules

- Recent keeps the last `feedback/recent-max` finished rows. **Failed rows and rows with a live Undo are
  exempt from the cap** and stay until read, retried, dismissed, or the library journal ends the Undo.
- **Clear** removes finished rows only. It skips rows with a live Undo, which stay with a note
  "Undo still available"; Clear never ends an Undo.
- Long text wraps; nothing in a row with an action is truncated.
- 1 row: one row. 500 rows: virtualised; the cap bounds Recent so only Running and Notices can be long.
- A new `danger` Toast adds a Notices row without opening the list.

## 6. Overflow rule

Scrolls at max height. Groups never collapse. Notices cannot be hidden by the cap.

## 7. Extension slots

None. An extension's jobs appear in Running and Recent under its name; it adds no group.

## 8. Pack content

Pack install jobs appear as rows naming the pack. Pack state names are not shown here.

## 9. Input

- **Stylus and touch**: tap a row's buttons; scroll by drag.
- **Mouse**: hover fill; click.
- **Keyboard**: focus moves in on open; Up and Down move between rows; Tab reaches each row's buttons;
  Escape closes and returns focus to the opener.
- **Name and tooltip**: opened by command `app.activity.show` ("Open activity"), registered below; the
  openers show its name in a Tooltip. Clear is a text button running `app.activity.clear`; Sync now and Compare run `sync.now` and `sync.compare`.
- **Announcements**: non-modal `dialog` labelled "Activity"; rows are list items with their state.
  Opening announces the counts ("2 running, 1 notice"). Rows do not announce while it is closed.
- Roles: ARIA `dialog` (non-modal) with `list`. Compose a `Popup` or bottom panel with list semantics.
  WinUI `Flyout`. SwiftUI `popover` or a sheet panel on compact.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/raised`, `border/subtle`, `elevation/popover` | surface |
| `text/default`, `text/muted`, `type/label`, `type/caption` | text |
| `hover/fill`, `press/fill`, `action/quiet/bg-hover` | fills |
| `size/popover/width`, `size/popover/max-height`, `size/row/list` | size |
| `space/sm`, `space/md`, `radius/surface` | spacing and shape |
| `feedback/recent-max`, `z/popover`, `focus/*` | cap, layer, focus |

## 11. Do and Don't

Do
- Keep every danger notice and every live Undo until used or read.
- Show the full banner content in Notices, with its action.
- Open as a panel on compact.

Don't
- Let Clear end an Undo.
- Add a group for an extension.
- Use it as a history of the library.

## 12. Traces to

Flows 04, 08, 13, 14 (via Toast, Banner, ProgressItem, SyncIndicator). New; replaces any downloads panel.
