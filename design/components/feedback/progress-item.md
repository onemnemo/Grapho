# ProgressItem

## 1. Purpose

Show a core job that is still running, with progress and Cancel. It appears when the job is still running
after `feedback/delay/progress`; a faster job shows nothing and ends as a Toast (SYSTEM-MAP 4.3). The
same row is used in the toast stack, inline in a row or footer, and in the activity list.

**When not to use**
- A faster job: only the result Toast.
- Waiting for a view's content: **Skeleton**.
- An in-place action on a button or row: inline busy (see the README waiting table).
- A lasting condition: **Banner** or **SyncIndicator**.

## 2. Anatomy

```
 [icon]  Importing Maths.pdf               [ Cancel ]
         [=========-----------]  40 %  Page 12 of 30
```

Parts: `icon` (job kind), `title` (verb and subject), `bar`, `meta` (percent, a step, or the reason Cancel is
off), `cancel` (quiet button). The `inline` placement is a thin form with title, bar and Cancel on one
line, sitting in a pack row or above a footer (surfaces.md); there is no ring.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `placement` | `toast` \| `inline` \| `list` |
| `mode` | `determinate` \| `indeterminate` |
| `phase` | `running` \| `failed` \| `finished` |

Row minimum `size/row/list`; `inline` is `size/row/list` with the bar on the title line. Cancel is
`size/control/small` inside a slot of the larger of `size/hit/min` and 44 on stylus and touch. The result
of a job (done, cancelled) is not a phase of this component: it becomes a Toast, or in `list` a finished
row.

**Where the result goes.** A job started from a command ends as a Toast. A job started inside a row (a pack
install) reports in that row: the row shows the result or the failure in place and raises no Toast. The
same event never reports in both.

## 4. States

| State | Tokens that change |
|---|---|
| Default (running) | `surface/overlay` (toast) or `surface/raised` (inline, list); track `progress/track`; fill `progress/fill` |
| Hover | list and inline rows: `hover/fill`; Cancel `action/quiet/bg-hover` |
| Focus-visible | focus recipe on Cancel and the action |
| Active | `press/fill` |
| Selected | not used |
| Disabled | Cancel off while the job cannot stop: `state/disabled/fg`, and the reason ("Finishing") is shown in `meta` |
| Loading | this is the loading form; `indeterminate` when no fraction is known |
| Error | `phase=failed`: icon `state/danger/fg`, edge `state/danger/line`, reason and **Retry**; never empty |
| Finished (list) | check icon `state/success/fg` |
| Drag | not used |

**Status icon on a fill host.** On `hover/fill`, `press/fill` and `selection/fill` the failed and finished
icons are not allowed (usage rules). They switch to `text/default` while the row is hovered, pressed or
selected, and return on rest. The words and the edge carry the state.

Bar fills start to end. Indeterminate sweeps over `motion/duration/slow`. **Reduced motion**: determinate
steps without easing; indeterminate is a static bar with the text "Working".

## 5. Content rules

- Title: "Importing Maths.pdf". Long names truncate in the middle and keep the extension; the full name is the
  accessible name. Meta wraps.
- No text: not allowed.
- 1 job: one row. A batch is one row ("12 of 500") and Cancel stops the whole batch. Failures inside a
  batch are counted in the same row ("3 failed", View); they are not separate rows. A queue of separate
  jobs started within `feedback/merge-window` of each other merges into one row.

## 6. Overflow rule

A running job takes a toast slot and is never displaced. More than one running job: the stack shows the
newest and the StackChip "3 running". The `list` form scrolls inside the activity list.

## 7. Extension slots

None. An extension's long operation uses the core job API and gets this row, titled with the extension's
name. It cannot raise its own Toast.

## 8. Pack content

A pack install or update names the pack and uses `inline`. Removing a pack mid-job cancels the job and the
row shows the failure reason. Pack state names (Off, Hidden, Kept from, Not installed) are not shown here.

## 9. Input

- **Stylus and touch**: tap Cancel or an action. Swipe does not dismiss a running row.
- **Mouse**: hover fill in `list` and `inline`; click.
- **Keyboard**: F6 reaches the toast region; Tab moves between Cancel and the StackChip.
- **Name and tooltip**: Cancel (`job.cancel`) and Retry (`job.retry`) are text buttons read from the registry. If a thin `inline` form shows Cancel as an icon, it has a Tooltip with name and shortcut.
- **Announcements**: `progressbar` with a value plus a live status: "Importing Maths.pdf, 40 percent,
  Cancel, button". Spoken at every 25 percent, never continuously. Indeterminate says "in progress".
  Finished is a status; failed is assertive.
- Roles: ARIA `progressbar` (`aria-valuenow`). Compose `ProgressBarRangeInfo` with `liveRegion`. WinUI
  `ProgressBar` with a range peer. SwiftUI `ProgressView` with `accessibilityValue`.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `surface/raised`, `elevation/toast`, `border/subtle` | container |
| `progress/track`, `progress/fill` | bar (fill meets 3:1 against track and host) |
| `text/default`, `text/muted`, `type/label`, `type/caption` | text |
| `state/success/fg`, `state/danger/fg`, `state/danger/line`, `border-width/emphasis` | status |
| `action/quiet/bg-hover`, `hover/fill`, `press/fill` | fills |
| `size/row/list`, `size/control/small`, `size/hit/min`, `size/icon/default`, `size/icon/inline` | sizes |
| `space/sm`, `space/md`, `radius/surface`, `radius/item`, `radius/pill` | shape |
| `feedback/delay/progress`, `feedback/merge-window` | timing |
| `motion/duration/slow`, `focus/*`, `z/toast` | motion and layer |

## 11. Do and Don't

Do
- Wait before showing anything, then say what runs and on what, with Cancel.
- Report a row job in its row, a command job as a Toast.
- Keep a failed row until read, with Retry.

Don't
- Draw a ring, a bare spinner, or a bar that moves backwards.
- Report one event in the row and a Toast.
- Put a status icon on a hover or selection fill.
- Cover the page.

## 12. Traces to

Flows 04, 08, 11, 13, 14. Replaces the job part of `PictureWorking` and the busy overlays. The activity list
rows are specified in [activity-list.md](activity-list.md).
