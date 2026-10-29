# Dialog

## 1. Purpose

A short decision that must be answered: confirm an irreversible action, or resolve unsaved changes.
It has a title, a message and two buttons. It has no field and no list.

**When not to use:**
- Anything with a list, grid, steps or a field: use Sheet.
- A reversible action: do it at once and show a Toast with Undo (SYSTEM-MAP 4.2).
- A hint or one-time tip: use Popover with Got it.
- A report of what happened: use Toast or Banner.

## 2. Anatomy

```
 scrim (scrim/default; scrim/stacked over a Sheet)
        +--------------------------------------------+  surface/overlay, radius/container
        |  Title                                      |  type/heading
        |  Message: names what will be lost           |  type/body, text/muted
        |                                             |
        |         [ Cancel ]  [ Delete 12 forever ]   |  foot
        +--------------------------------------------+
```

Parts: scrim, container, title, message, error line, foot with two buttons. No close control
and no icon.

## 3. Variants and sizes

`kind`: `irreversible` | `unsaved`. One size: width the smaller of `size/dialog/width` and the window
minus two `layout/gutter/*`, centred in every window class, never a bottom sheet. There is no
`confirm` kind: a reversible action runs and shows a Toast with Undo.

| Kind | Title pattern | Safe button | Action button |
|---|---|---|---|
| `irreversible` | "Delete 12 items forever?" | Cancel (`secondary`) | `danger`, verb and count: "Delete 12 forever" |
| `unsaved` | "Discard changes?" | Keep editing (`primary`) | Discard (`text`, the lighter button: it is the one that loses work) |

- In `unsaved` the safe choice is the primary because it keeps the work. In `irreversible` the
  danger Button is the only filled button, so the Dialog has one filled region.
- Buttons are `size/control/default`, equal width when they fit. When 40% longer labels do not fit
  they stack with the action on top and first in focus order; they never truncate. The safe button
  still takes default focus.

## 4. States

| State | Change |
|---|---|
| default | `surface/overlay`, `elevation/sheet`, `z/dialog` |
| opening | fade and scale 0.96 to 1 over `motion/duration/pop`; reduced motion: none |
| hover, active | per Button; danger pressed uses `state/danger/bg-pressed` |
| focus-visible | ring on the focused button; default focus is the safe button |
| selected | not applicable |
| disabled | not used: the action is always available |
| loading | the action button shows inline busy and keeps its width; the safe button stays live; Escape still cancels; past 1000 ms the job becomes a Progress item and the Dialog closes |
| empty | not applicable (a Dialog always has a message) |
| error | the error line is added above the message in `state/danger/fg`; the message stays; the same button retries |
| over a Sheet | scrim `scrim/stacked`; the Sheet below is inert |

## 5. Content rules

- The message names what is lost, with the count and the thing: "12 notebooks and 3 packs leave the
  Trash for good". Never "Are you sure?".
- Title up to two lines. The message keeps a visible minimum of three lines at 200% text on compact;
  past that the whole Dialog scrolls and the foot stays pinned.
- No text is not allowed. 1 item: name it. 500 items: give the count and the first name ("Delete 500
  forever"), never list them.

## 6. Overflow rule

None to grow into. Two buttons and one message. A third choice means the flow is a
Sheet. Two Dialogs are never open at once.

## 7. Extension slots

None. Extensions cannot raise Dialogs. Erasing an extension's stored data is the host's
`irreversible` Dialog: "Erase Flashcards data forever".

## 8. Pack content

None, except the message may name a pack in plain words ("Kept from Notebook Basics"). No pack
state controls.

## 9. Input

- Focus is trapped. First focus: the safe button.
- Enter runs the focused button, so it runs the danger action only when that has focus. Escape and
  Android Back are Cancel (for `unsaved`, Escape is Keep editing). Scrim tap does nothing; a pulse of
  the safe button and the polite status "Choose Cancel or Discard" answer it. A palm never acts.
- Over a document the Dialog takes all input like a Sheet. No Dialog opens while the pen is down; it
  queues.
- Tab order: field, safe button, action button. Left and Right arrows also move between the buttons.
- Stylus and touch: buttons are at least `size/hit/min`; the danger button is `space/md` from the
  safe one so one tap cannot hit both.
- Dismiss order: a Dialog above a Sheet closes first, then the pushed view, then the Sheet.
- Keyboard up: the Dialog stays centred in the space above the software keyboard.
- Roles: ARIA `alertdialog`, labelled by the title, described by the message (read before the
  buttons). Compose `AlertDialog`. SwiftUI `.alert` with a destructive role on the action button.
  On Windows the Dialog is drawn inside the shell's one overlay host, not a second ContentDialog.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `elevation/sheet`, `radius/container` | container |
| `scrim/default`, `scrim/stacked`, `z/dialog` | scrim and layer |
| `size/dialog/width` (README, Tokens to add), `space/xl` padding, `space/lg` text to foot, `space/md` button gap | layout |
| `type/heading`, `type/body`, `text/default`, `text/muted`, `state/danger/fg` | text |
| `action/primary/*`, `action/quiet/*`, `state/danger/*`, `state/danger/bg-pressed` | buttons |
| `motion/duration/pop`, `size/control/default`, `component/focus/*` | motion, size, ring |

## 11. Do and Don't

- Do label the danger button with verb and count: "Delete 12 forever".
- Do give default focus to Cancel or Keep editing.
- Do name what is lost in the message.
- Don't use a Dialog for a reversible delete or a first-time hint.
- Don't add a list, a Picker or a field.
- Don't make the danger button the default focus.
- Don't hold to confirm.

## 12. Traces to

Flows 09 (erase extension data), 10 (empty Trash, delete forever), 02 and 07 (discard from a Sheet
or Studio). No flow needs a plain confirm.
