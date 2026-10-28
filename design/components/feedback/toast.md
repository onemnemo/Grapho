# Toast

## 1. Purpose

Report what just happened, in plain words, with at most one action (Undo, Show, Share, View, Retry). It
is the single place a result is reported (SYSTEM-MAP 3 and 4.3). Reversible library actions (delete, move,
rename, tag, remove a pack) end here with Undo (4.2).

**When not to use**
- A lasting state: **Banner**.
- A job still running: **ProgressItem**; a job that began in a row reports in that row.
- A choice that needs an answer: Dialog.
- Star, or turning a pack on or off: no toast; the switch undoes in place.
- Anything from an extension. Extensions cannot raise Toasts (extension API).

## 2. Anatomy

```
 [icon]  Moved 3 items to Maths                  [ Undo ]  [x]
  |       |                                        |         |
  icon    message (wraps, never truncated)         action    dismiss
```

Parts: `surface`, `edge` (danger only, a leading bar clipped to the corner), `icon` (danger and done),
`message`, `action` (zero or one), `dismiss` (icon-only button named "Dismiss", with a Tooltip; always
shown while a screen reader runs). Below a stated width (`layout/breakpoint/medium`) the dismiss moves into
the action row under the message, so the message keeps the full width.

The **toast stack** holds at most two toast slots plus one **StackChip**: a quiet button "3 running" or
"2 in activity" that opens the activity list (size `size/control/default`, `type/label`, `radius/pill`,
`surface/overlay`). A running ProgressItem takes a slot. The chip is the only third element.

Placement: the bottom of the page area, on the half **away from the writing hand**, inside the platform
edge guard (16 dp) and beside the dock (which shares that edge), never over it. If the dock is a bottom
strip (compact), the stack sits above it. Compact width: centred, `min(toast/width-max, width minus two
gutters)`. Over a selection or floating bar it sits `toast/gap-above-bar` above it; else `toast/bottom`
from the edge. Layer `z/toast`.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `tone` | `neutral` \| `danger` |
| `action` | `none` \| `present` (the label is text: the verb, from the command or job) |

One size, minimum height `size/row/list` (the same token as Banner and ProgressItem, so a density change
moves them together). The action is a quiet button at `size/control/default`.

## 4. States

| State | Tokens that change |
|---|---|
| Default | `surface/overlay`, `border/subtle` hairline, `elevation/toast`, `text/default`, `radius/surface` |
| Hover (pointer on toast) | no fill change; the timer pauses |
| Focus-visible (action or dismiss) | focus recipe; the timer pauses |
| Active (action pressed) | `press/fill` on the action; its label is `text/default` |
| Selected, disabled, loading, empty | not used |
| Error | `danger`: icon `state/danger/fg` (on `surface/overlay`, allowed), leading edge `state/danger/line`, message `text/default` |
| Drag | swipe **down** to dismiss, never toward a screen edge, so it cannot trigger the system back gesture; follows the finger, release past a third of its height dismisses |

Enter: fade over `motion/duration/base`. Exit: fade over `motion/duration/exit`. **Reduced motion**: no
movement; the toast appears and leaves by opacity at 0 ms. Nothing animates while the pen is on the page.

## 5. Content rules

- Message: a plain sentence. "Moved 3 items to Maths". Voice rules live in `design/docs/branding.md`.
- Very long text (German, Norwegian, up to 40% longer): wraps to as many lines as needed; **a message
  with an action is never truncated**. A message without an action may stop at four lines, with the full
  text in the activity list.
- No text: not allowed.
- 1 item names it ("Deleted Maths notes"). Many count ("Deleted 12 items"). Events of one verb within the
  merge window (`feedback/merge-window`) become one toast with a count.
- **Queue** (4.3). At most two slots. A new toast removes the oldest one that has no Undo **and is not
  `danger`**. If both visible toasts have Undo or are danger, the older moves to the activity list with
  its action still working. **Undo is never lost.** A `danger` toast is never evicted by a neutral one.
- **Every `danger` toast is also logged in the activity list under Notices**, with its action.
- **Time**: a `danger` toast with no action lasts as long as a toast with Undo (`toast/duration-undo`).
  A neutral toast without an action lasts `toast/duration-plain`. Both extend to the system accessibility
  timeout if longer, and to `feedback/hold/screen-reader` while a screen reader runs. Neither dismisses
  while it has focus or a pointer on it. Reduced motion does not shorten them.
- **Pen**: a toast that arrives while the pen is on the page, or within `delay/pen-idle` of the last
  contact, waits and shows after that idle time, not between strokes. A toast that arrives while a Dialog
  is open waits for it to close. A queued Undo shows with the action it belongs to named in the message.

## 6. Overflow rule

Two slots and the chip. Waiting toasts queue in arrival order; a displaced neutral toast is dropped (it
is not an Undo or a failure); a displaced one that matters moves to the activity list. One action only;
a second action means a Banner or a Dialog.

## 7. Extension slots

None. Extensions cannot raise Toasts. A panel's one-line `ui.status` belongs to the extension panel, is not
a feedback component, and is not specified here.

## 8. Pack content

A toast names a pack only for an action just taken ("Removed Space stickers"). It never shows pack state
(Off, Hidden, Kept from, Not installed); those belong to the picker, Content pages and Packs pages.

## 9. Input

- **Stylus and touch**: tap the action or dismiss; swipe down to dismiss. Targets are the larger of
  `size/hit/min` and the 44 stylus and touch rule.
- **Mouse**: hover pauses; dismiss shows on hover.
- **Keyboard**: not in the Tab order. **F6** reaches the toast region; Enter runs the action; Escape
  dismisses the focused toast. A Popover or Menu closes on Escape first (SYSTEM-MAP 3 order); a toast that
  arrives while the activity list is open shows above it and does not take focus.
- **Name and tooltip**: dismiss is not a command; its name "Dismiss" comes from the host and has a Tooltip.
  A toast action that repeats a command (Undo) reads its name and shortcut from the registry
  (`library.undo`).
- **Announcements**: `status` (polite): "Moved 3 items to Maths. Undo, button". `danger` is `alert`
  (assertive). The announcement order and one-event rule are in the README.
- Roles: ARIA `status` or `alert`. Compose `liveRegion` with a custom action "Undo". WinUI `LiveSetting` on
  an automation peer. SwiftUI `AccessibilityNotification.Announcement` plus an action.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `border/subtle`, `elevation/toast` | surface |
| `radius/surface`, `radius/pill`, `space/md`, `space/sm` | shape and spacing |
| `text/default`, `type/label` | message and action label |
| `action/quiet/bg-hover`, `press/fill` | action fills |
| `state/danger/fg`, `state/danger/line`, `border-width/emphasis` | danger icon and edge |
| `size/row/list`, `size/control/default`, `size/hit/min`, `size/icon/default` | sizes |
| `layout/breakpoint/medium`, `toast/width-max`, `toast/bottom`, `toast/gap-above-bar` | placement |
| `toast/duration-undo`, `toast/duration-plain`, `delay/pen-idle`, `feedback/hold/screen-reader`, `feedback/merge-window` | time |
| `motion/duration/base`, `motion/duration/exit` | motion |
| `focus/ring/*`, `focus/gap/*`, `z/toast` | focus and layer |

## 11. Do and Don't

Do
- Say the result and the count; give Undo for what the library can reverse.
- Keep a failure on screen and in Notices until it is read or dismissed.
- Wait for pen idle before showing.

Don't
- Show a third toast, drop an Undo, or evict a danger toast with a neutral one.
- Swipe toward an edge, or truncate a message that has an action.
- Use a toast for a lasting state, or from an extension.
- Put a status icon on a hover or press fill: the icon turns `text/default` there.

## 12. Traces to

Flows 05, 07, 08, 09, 10, 13, 14. Replaces `Toast` and `ToastProvider`. Keeps the provider as the queue.
