# Banner

## 1. Purpose

Say a lasting state of the surface it sits on: offline, safe mode, theme adjusted, sync conflict, storage
full, a partial result, a failed load. Inline on that surface. **Never a floating corner notice**
(SYSTEM-MAP 4.3).

**When not to use**
- A result of something just done: **Toast**.
- A running job: **ProgressItem**.
- A field's own error: the field's message.
- A decision that must be answered: Dialog.
- Search in a document with input and a count: Find bar.
- Anything from an extension. Extensions cannot show Banners.

## 2. Anatomy

```
 | [icon]  Sync is offline. Changes are saved here and will sync later.   [ Details ]  [x]
 |  |       |                                                              |            |
 edge icon  message                                                         action       dismiss
```

Parts: `edge` (a leading bar clipped to the corner), `icon`, `message`, `action` (zero to two buttons),
`dismiss` (icon-only, named "Dismiss", with a Tooltip). In a list view, below a stated width
(`layout/breakpoint/medium`) the actions move under the message.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `tone` | `neutral` \| `warn` \| `danger` |
| `placement` | `inline` \| `overlay` \| `region` |
| `dismiss` | `allowed` \| `never` |

- `inline`: in the flow at the top of a library place, a panel or Settings. Content moves down.
- `overlay`: in a document. It overlays the top of the page area and **pushes nothing**; the page never
  reflows or shifts under the pen.
- `region`: the error state of a view. Centred in the region with `size/icon/feature`, the message and
  **Retry** as the primary (`size/control/large`), so the biggest failure is the loudest thing on screen.

Every Banner is `allowed` to dismiss except safe mode and a blocking error (`never`). A `warn` Banner is
dismissible; dismissing a partial-result Banner records it as acknowledged. Row minimum `size/row/list`.
Neutral tone uses `surface/raised` (not `state/info`); the old `info` tone is `neutral`.

## 4. States

| State | Tokens that change |
|---|---|
| Default, neutral | `surface/raised`, `border/subtle`, edge `border/subtle`, `text/default` |
| Default, warn | `state/warning/bg`, edge `state/warning/line`, icon `state/warning/fg`, text `text/default` |
| Default, danger | `state/danger/bg`, edge `state/danger/line`, icon `state/danger/fg`, text `text/default` |
| Hover, press | on its buttons only: `action/quiet/bg-hover` is not allowed over a tint, so Banner buttons are `secondary` (`action/secondary/*`) with `border/control`; hover and press use `action/secondary/bg-hover` and `bg-pressed`. The row has no hover fill |
| Focus-visible | focus recipe on buttons |
| Selected, disabled | not used |
| Loading | a waiting action shows inline busy on its button; the Banner stays |
| Empty | not rendered |
| Error | the danger tone; `region` for a failed view |
| Drag | not used |

Tone is carried by the icon, the edge and the words, never colour alone. `overlay` and `region` appear by
opacity over `motion/duration/base`; `inline` does not animate its height. **Reduced motion**: at once.
`overlay` waits for `delay/pen-idle` after pen contact before it arrives. It goes away when the state
resolves; "Back online" is a Toast.

## 5. Content rules

- Message: what is true, then what to do. "Storage is full. Free space or remove old backups."
- Very long text (German, Norwegian): wraps; never truncated.
- No text: not allowed. Title is optional; the message is required.
- 1 item: one Banner. **Two show at once**, ordered danger, warn, neutral. Any further notice is not
  hidden: the surface shows a quiet button "N more notices" (`size/control/default`, `type/label`) that
  opens the activity list, where Notices holds each banner at full content with its actions. A `danger`
  Banner is never one of the hidden ones.
- 500 items: counts, never lists ("500 pages could not be imported", View).

## 6. Overflow rule

Two buttons at most; a third action means the state needs its own page, linked from the Banner. The cap of
two applies to app Banners only.

## 7. Extension slots

None. Extensions cannot show Banners, inside or outside their panel; the one-line `ui.status` of an
extension panel is owned by the shell and is not a Banner.

## 8. Pack content

A Banner does not show pack state. The one document-level case is a received document whose items came
from packs the user does not have: a `neutral` Banner "Made with packs you don't have", with Show, `dismiss`
allowed. Pack state names (Off, Hidden, Kept from, Not installed) appear only in the picker, Content pages,
Packs and Extensions pages, and an item's detail.

## 9. Input

- **Stylus and touch**: tap a button; targets the larger of `size/hit/min` and the 44 stylus rule. Swipe
  does not dismiss.
- **Mouse**: hover on buttons.
- **Keyboard**: buttons in Tab order at the Banner's place. Escape does not dismiss it.
- **Focus**: when a `region` Banner replaces content the view had focus in, focus moves to Retry. After
  Dismiss, focus moves to the next control in reading order, or the surface heading.
- **Name and tooltip**: Dismiss is not a command; its name comes from the host and it has a Tooltip.
  Actions that repeat a command read their name from the registry.
- **Announcements**: `status` (polite) for neutral and warn; `alert` (assertive) for danger. Labelled by
  its first sentence. Announced once on arrival. If the same event also logs a Toast, the Banner announces
  and the Toast stays silent.
- Roles: ARIA `status` or `alert`. Compose `liveRegion` on a labelled container. WinUI `InfoBar`.
  SwiftUI a labelled group with an announcement on arrival.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/raised`, `border/subtle`, `text/default`, `type/label` | neutral |
| `state/warning/bg`, `state/warning/fg`, `state/warning/line` | warn |
| `state/danger/bg`, `state/danger/fg`, `state/danger/line` | danger |
| `border-width/emphasis`, `border-width/hairline`, `border/control` | edge and button outline |
| `action/secondary/fg`, `action/secondary/bg-hover`, `action/secondary/bg-pressed` | buttons |
| `size/row/list`, `size/control/default`, `size/control/large`, `size/icon/default`, `size/icon/feature` | sizes |
| `layout/breakpoint/medium`, `space/md`, `space/sm`, `space/xl`, `radius/item` | layout and shape |
| `delay/pen-idle`, `motion/duration/base`, `z/chrome`, `focus/*` | timing and layer |

`action/secondary/fg` is used on tinted hosts only once its pairs on `state/*/bg` are declared (see README,
Tokens to add).

## 11. Do and Don't

Do
- Overlay in a document; inline everywhere else.
- Make a failed view loud: `region` with Retry.
- Let people dismiss it, except safe mode and a blocking error.

Don't
- Reflow the page under the pen.
- Hide a danger notice behind the cap.
- Put `action/quiet/fg` or a status colour on a tint it is not allowed on.
- Show pack state or an extension Banner.

## 12. Traces to

Flows 03, 04, 06, 07, 09, 10, 12, 13, 14. Replaces `Banner`; the old info tone is `neutral`.
