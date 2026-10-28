# SyncIndicator

## 1. Purpose

Show the sync state in the sidebar foot and open the activity list (SYSTEM-MAP 3 and 4.6). It is the quiet
status; lasting problems also raise a **Banner** on the affected surface.

**When not to use**
- That a sync just finished or failed: **Toast**.
- A conflict or full storage needing action: the **Banner** (the indicator points to it).
- Job progress: **ProgressItem**.

## 2. Anatomy

```
 [icon]  Up to date            >
   |      |                    |
   glyph  label                opens the activity list
```

Parts: `icon`, `label`, `chevron`. In the rail only `icon` shows, with a Tooltip. The label wraps; it is
never truncated for `conflict` and `storage-full`.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `state` | `off` \| `up-to-date` \| `syncing` \| `offline` \| `conflict` \| `storage-full` |
| `form` | `row` \| `icon` |

Each state has its own Lucide glyph, declared in the icon registry (ids in the README): `off` cloud-off,
`up-to-date` cloud-check, `syncing` refresh-cw, `offline` wifi-off, `conflict` git-compare, `storage-full`
hard-drive. `row` is `size/row/list`. `icon` is a `size/control/default` icon button in a slot of the
larger of `size/hit/min` and 44; it lines up with the Settings entry beside it in the foot, same size, same
slot. Icon `size/icon/default`.

**When it shows.** If sync was never configured the row is not shown; sync lives in Settings, Sync. `off`
shows only after sync was turned off from a configured state.

## 4. States

| State | Tokens that change |
|---|---|
| Default | `off`, `up-to-date`, `offline`: `text/muted`. `syncing`: `text/default`. `conflict`: `state/warning/fg`. `storage-full`: `state/danger/fg`, on surface hosts |
| Hover, press | `hover/fill`, `press/fill`; the icon and label of every state switch to `text/default` while filled (status colours are not allowed on those fills) |
| Selected | `selection/fill` plus `selection/indicator` while the list is open; icon `text/default` |
| Focus-visible | focus recipe |
| Disabled, empty | not used |
| Loading | `syncing` |
| Error | `conflict`, `storage-full` (glyph and words change; the Banner explains) |
| Drag | not used |

State never rests on colour: each has its own glyph and words. Rotation of `syncing` runs over
`motion/duration/slow`; **reduced motion** a static glyph with the label "Syncing". Nothing animates
while the pen is on the page.

## 5. Content rules

- Labels: "Sync is off", "Up to date", "Syncing", "Offline, queued", "Conflict", "Storage full".
- Long text (German, Norwegian): the row wraps to two lines; nothing is truncated. The accessible name is
  always full.
- No text: the `icon` form has a Tooltip and name.
- 1 pending change: "Syncing". 500 pending: the row still reads "Syncing"; the count "500 waiting" is in
  the activity list's Sync row, never in the row.

## 6. Overflow rule

One indicator, no badge or dot (Dots are only for updates on Packs and Extensions). Per-item sync marks, if
ever added, need their own component and a decision.

## 7. Extension slots

None.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch**: tap opens the activity list.
- **Mouse**: click; hover fill.
- **Keyboard**: a Tab stop in the sidebar foot; Enter or Space opens; Escape closes and returns focus.
- **Name and tooltip**: opens command `app.activity.show` ("Open activity"); the `icon` form shows its name
  in a Tooltip.
- **Announcements**: a polite status on each change, once ("Sync is offline. Changes are queued").
  `conflict` and `storage-full` announce through their Banner (assertive for danger), not twice. Named
  "Sync, up to date, activity list".
- Roles: ARIA `button` with `aria-haspopup="dialog"`. Compose `Button` with `stateDescription`. WinUI
  `Button` with a `Flyout`. SwiftUI `Button` with an accessibility value.

## 10. Tokens

| Token | Use |
|---|---|
| `text/default`, `text/muted`, `type/label` | label and icon |
| `state/warning/fg`, `state/danger/fg` | conflict and storage-full at rest |
| `hover/fill`, `press/fill`, `selection/fill`, `selection/indicator` | filled states |
| `size/row/list`, `size/control/default`, `size/icon/default`, `size/hit/min` | sizes |
| `space/sm`, `space/md`, `radius/item` | spacing and shape |
| `motion/duration/slow`, `focus/*` | rotation and focus |

## 11. Do and Don't

Do
- Give each state its own glyph and words.
- Hide the row when sync was never configured.
- Open the activity list on activation.

Don't
- Put a status colour on a hover, press or selection fill.
- Add a badge or count dot.
- Truncate "Storage full".

## 12. Traces to

Flow 14. New.
