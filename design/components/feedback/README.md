# Feedback family

Report results, lasting states, running work, waiting and emptiness. Every member traces to a flow in
`design/flows/COMPONENTS.md` (Feedback). Rules come from SYSTEM-MAP 4.3 and 4.6.

| Component | Job | Surface | Min height | Padding | Radius | Icon | Label |
|---|---|---|---|---|---|---|---|
| [Toast](toast.md) | Result, one action | toast stack, off-hand side | `size/row/list` | `space/md` | `radius/surface` | `size/icon/default`, leading | `type/label` |
| [ProgressItem](progress-item.md) | Running job, Cancel | toast stack, inline, list | `size/row/list` | `space/md` | `radius/surface` (toast), `radius/item` (rows) | `size/icon/default`, leading | `type/label` |
| [Banner](banner.md) | Lasting state | top of surface, overlay, region | `size/row/list` | `space/md` | `radius/item` | `size/icon/default`, leading | `type/label` |
| [ActivityList](activity-list.md) | Record of work and notices | Popover, panel on compact | `size/row/list` rows | `space/md` | `radius/surface` | `size/icon/default`, leading | `type/label` |
| [EmptyState](empty-state.md) | Nothing here | the empty region | none | `space/xl` | none | `size/icon/feature` | `type/heading`, `type/body` |
| [Skeleton](skeleton.md) | Hold layout | the loading region | the real item | the real item | `radius/item` | none | none |
| [Tooltip](tooltip.md) | Name and shortcut | anchored | one line | `space/sm` | `radius/item` | none | `type/label`, `type/caption` |
| [SyncIndicator](sync-indicator.md) | Sync state | sidebar foot | `size/row/list` | `space/sm` | `radius/item` | `size/icon/default`, leading | `type/label` |

Radius rule: floating surfaces (Toast, toast ProgressItem, ActivityList) use `radius/surface`; in-flow rows
(Banner, inline rows, SyncIndicator) and small chips use `radius/item`. Icons and text start at the same
x across Toast, Banner, ProgressItem and SyncIndicator (leading icon, `space/sm` gap).

Shared rules: status colour always comes with an icon and words. Edges use `state/*/line`. Message text is
`text/default`; `state/*/fg` only on the icon, on a surface host. A status icon on a hover, press or
selection fill switches to `text/default`. Buttons on tinted hosts are `secondary`. Nothing animates while
the pen is on the page. Extensions cannot raise Toasts or Banners.

## Waiting (one table)

| Waiting on | Pattern | Delay | Owner |
|---|---|---|---|
| A button or a row action (including a field's own action) | inline busy: a small spinner, width kept | `feedback/delay/spinner` (300 ms) | the control |
| A content region | Skeleton, held at least `feedback/hold/skeleton` | `feedback/delay/skeleton` (300 ms) | the host |
| A core job | ProgressItem with Cancel | `feedback/delay/progress` (1000 ms) | the core job |

Exactly these three patterns (Skeleton, ProgressItem, inline busy). No standalone spinner, no blocking overlay, no full-screen loader, no "Loading" text line.

## Announcements and priority

| Order | Source | Polite | Assertive |
|---|---|---|---|
| 1 | Danger Banner or danger Toast | | yes |
| 2 | ProgressItem | every 25 percent, finish | failure |
| 3 | Toast, neutral Banner | result | |
| 4 | SyncIndicator | each change | |
| 5 | Skeleton | "Loading", then the count | |

At most one polite announcement per second; lower order waits. One event announces once: when a Banner and
a Toast report the same event, the Banner speaks and the Toast is silent (it is still logged in Notices).
Writing never triggers an announcement.

## Reduced motion

Toast and Banner appear by opacity or at once. Progress steps without easing; indeterminate is a static bar
with "Working". Skeleton is a static tint. Sync rotation stops. The mascot is still. Hold times are reading
time and do not shorten.

## Icons (Lucide names, registry ids snake case)

close `x`, dismiss uses `close`; danger `triangle-alert`; warn `triangle-alert`; neutral `info`; finished
`check`; failed `circle-alert`; job kinds `download`, `upload`, `file-down`; sync: `cloud-off`,
`cloud-check`, `refresh-cw`, `wifi-off`, `git-compare`, `hard-drive`. Provenance follows `icons/README.md`.

## Commands to register

`app.activity.show` (Open activity); `app.activity.clear` (Clear); `library.undo` (Undo on a Toast); `job.cancel` (Cancel); `job.retry` (Retry); `sync.now` (Sync now); `sync.compare` (Compare). Show, Share and View reuse the existing open and export commands. Dismiss and Got it edit a surface, not a command; Dismiss still has a Tooltip.

## Tokens to add

| Token | Value (rule) | Used by |
|---|---|---|
| `feedback/delay/progress` | 1000 ms | ProgressItem |
| `feedback/delay/skeleton`, `feedback/hold/skeleton` | 300 ms, 300 ms | Skeleton |
| `feedback/delay/spinner` | 300 ms | inline busy |
| `feedback/timeout/load` | per host; core default 15 s | Skeleton |
| `feedback/delay/tooltip` | 500 ms | Tooltip |
| `feedback/hold/touch-tooltip` | 1500 ms | Tooltip |
| `feedback/hold/screen-reader` | 10000 ms | Toast |
| `feedback/merge-window` | 500 ms | Toast, ProgressItem |
| `feedback/recent-max` | 20 | ActivityList |
| `delay/pen-idle` | 1500 ms (the note bar return rule) | Toast, Banner |
| `size/popover/width`, `size/popover/max-height` | one value each | ActivityList |
| `size/tooltip/max-width` | one value | Tooltip |
| `layout/measure/empty` | one value, not derived from a height | EmptyState |
| `progress/track`, `progress/fill`, `skeleton/fill` | to add | ProgressItem, Skeleton |
| declared pairs for `action/secondary/fg` on `state/*/bg` | contrast | Banner |

## Decisions

1. The toast stack sits on the off-hand half beside the dock, with a StackChip as its only third element.
2. Danger toasts are persistent like Undo toasts and are always logged in Notices.
3. Toasts and overlay Banners wait for pen idle; Banners in a document never reflow the page.
4. The activity list has groups Sync, Notices, Running, Recent; it is a Popover, a panel on compact.
5. A job started in a row reports in that row; a command job reports as a Toast.
6. EmptyState has three kinds; the mascot only in first-use `md`.
7. The first-use hint is a Popover, not a Tooltip.
8. Banner `info` is named `neutral`; it maps to `surface/raised`, not `state/info/*`.
