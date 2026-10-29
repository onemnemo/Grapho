# Popover

## 1. Purpose

A small choice tied to its anchor that applies at once and never has Done or Cancel (SYSTEM-MAP 3): a size,
a quick colour, a filter, a tag or folder checklist with Filter, the activity list. Also the one-time
first-use hint. Light modality: no scrim; a finger or mouse tap outside closes it and is not passed through;
a pen-down on the page outside closes it and the stroke is drawn. A Popover stays open while its controls
change, which is what separates it from a Menu.

**When not to use**
- A list of commands, or a short value list that closes on choice: **Menu**.
- Anything that needs Done or Cancel, a second picker, or several fields: **Sheet**.
- A short irreversible decision: **Dialog**.
- Naming an icon: **Tooltip**.
- A report of what happened: **Toast**.

## 2. Anatomy

```
        [ anchor ]
 +-------------------------------+
 | [<] Title                 [x] |  head   (Back only in a pushed view; Close optional)
 | [ Filter...              (x)] |  filter (optional, pinned, above 8 items)
 | Select all  Clear             |  actions (optional, two quiet text Buttons)
 |  body: slider, checklist,     |  body   (scrolls)
 |  chips, swatches, list        |
 |  error message     [ Retry ]  |  message (error state only)
 |-------------------------------|
 |  Reset                        |  foot   (optional, one text Button)
 +-------------------------------+
```

A hint has no head: `message` and one `Got it` Button. Back, Close and the Filter clear each carry a
Tooltip with the name.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `kind` | `content` \| `hint` |
| `width` | `default` \| `narrow` |
| `placement` | `below` \| `above` \| `start` \| `end` (a preference; collisions decide) |

`default` is `size/popover/width`: lists, checklists, the activity list. `narrow` is
`size/popover/width-narrow`: one control, such as a size Slider or a row of swatches. A hint uses
`size/tooltip/max-width`. Height is content up to `size/popover/max-height`; then the body scrolls with
`head`, `filter`, `actions` and `foot` pinned.

**One job.** A Popover holds one job. The lint flags more than four controls, or more than one Slider, and
sends the design to a Sheet or a Side panel. Popovers anchor to chrome or to a control inside a panel or
Sheet, never over page content.

## 4. States

| State | Tokens that change |
|---|---|
| Surface | `surface/overlay`, `border/subtle`, `elevation/popover`, `radius/surface` |
| Hover, focus-visible, active, selected, disabled | belong to the controls inside |
| Loading | skeleton in the body after 300 ms, shown at least 300 ms (`skeleton/fill`); size does not jump |
| Empty | one muted line "Nothing here" (`text/muted`); Filter stays |
| Error | the `message` row: `alert_circle` plus text in `state/danger/fg`, with a text Button Retry when a retry exists; the Popover stays open |
| Drag | not used; ignores drops |

Open and close fade over `motion/duration/fast`; a pushed view slides over `motion/duration/base` toward
the inline end and back, mirrored in right to left. Reduced motion: cross fade at most. Forced colours:
system window colours. More contrast: `border/strong`.

## 5. Content rules

- Controls apply at once; there is no Done. A change is one entry in the host's undo.
- Very long text (German and Norwegian): labels wrap, the width holds, the height grows, then scrolls.
- No text: an unlabelled control, or a hint without a sentence, is a lint error.
- 1 item: shown. 500 items: a virtual list with Filter pinned (Filter above 8 items) and `actions` with
  Select all and Clear, which act on the model query, not the rows on screen.
- A hint is one plain sentence in `type/label`, and Got it.

## 6. Overflow rule

- Too tall: the body scrolls; `head`, `filter`, `actions` and `foot` stay.
- Too many controls: a Sheet or panel (see One job).
- **Pushed views.** A Select or a DateField's calendar inside a Popover pushes **one** view in the same
  Popover (a Popover allows one level). The pushed view replaces `head` and `body`; `head`
  shows Back (an IconButton, `chevron_left` mirrored in right to left, Tooltip "Back", `size/hit/min` slot)
  and the control's label. Choosing an option sets the value, pops back and returns focus to the control.
  A pushed view cannot push. The width holds; the body scrolls if the height is at its maximum. In a Sheet
  or panel the same control opens its own Popover; never a Popover on a Popover.
- **Back and Escape** close the pushed view first, then the Popover, per the SYSTEM-MAP order.

## 7. Extension slots

None. An extension cannot open a Popover, and hints are built in only.

## 8. Pack content

None. Swatch and tool lists show names only. A pack-fed list is a Picker.

## 9. Input

- **Hint state machine.** Hidden, then Shown when its anchor is visible and the pen is not on the page.
  Shown ends in **Seen** by Got it, or by the first stroke in the first note. Escape, or any other surface
  opening, returns it to Hidden **unseen**; it shows again at the next first-note open until Seen. It has no
  outside-tap dismissal and is not light-dismissed (WinUI `Flyout` with light dismiss off). It never takes
  focus and has no Tab trap: Got it is in the normal tab order after its anchor, it is announced once as a
  polite status, and it is overlaid with no reflow and mirrors for the left hand. Seen is stored once and
  never shown again.
- **Stylus.** A pen-down on the page outside a `content` Popover closes it and becomes the stroke. Hover is
  not required.
- **Touch.** A finger tap outside closes. While the pen is in proximity or within 1 s of its last contact,
  touches larger than the palm threshold never close it. Controls use their own targets.
- **Mouse.** A click outside closes. Hover over the anchor or surface does not close it.
- **Keyboard (content).** Opening moves focus into the Popover: to the Filter only when a keyboard or
  mouse was the last input, otherwise to the surface, so touch and pen never raise the keyboard until the
  Filter is tapped. Tab stays inside while open; Escape closes (a pushed view first) and returns focus to
  the anchor, visibly.
- **Placement.** The same rules and hand table as Menu: away from the writing hand relative to the contact
  point, then toward the inline end, then the remaining side, then shrink and scroll; never covers its
  anchor; `space/md` inside the window. On compact it stays anchored. With the software keyboard up it moves
  above the keyboard with at least three body rows or scrolls; it never becomes a bottom sheet.
- **Stack.** It closes when anything else opens, including a Menu, and sits above Sheets and Dialogs
  (`z/popover`). Back and Escape close it first.
- **Roles.** `content`: ARIA non-modal `dialog` labelled by its title or the anchor name; anchor
  `aria-haspopup="dialog"` and `aria-expanded`. `hint`: `role="status"` with Got it as a button. Compose
  `Popup` (focusable only for `content`), WinUI `Flyout`, SwiftUI `.popover` with
  `presentationCompactAdaptation(.popover)`; native names are visual reference, the pushed view, Filter and
  hint rules are custom.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `border/subtle`, `border/strong`, `elevation/popover`, `radius/surface` | surface |
| `size/popover/width`, `size/popover/width-narrow`, `size/popover/max-height`, `size/tooltip/max-width`, `size/hit/min` | size |
| `space/sm`, `space/md`, `space/lg` | padding |
| `text/default`, `text/muted`, `state/danger/fg`, `type/label`, `type/body`, `type/caption` | text |
| `skeleton/fill`, `component/focus/*` | loading and focus |
| `motion/duration/fast`, `motion/duration/base` | timing |
| `z/popover` | layer |

## 11. Do and Don't

Do
- Apply at once, push one view for a Select or calendar, and show the reason on error with a Retry.
- Keep a hint to one sentence, with no focus steal, and mark it Seen only on Got it or the first stroke.
- Anchor to chrome and let the pen draw through it.

Don't
- Open a Popover from a Popover, add Cancel, or pack two jobs into one.
- Put commands in it (use a Menu).
- Show a hint over the page while the pen writes.

## 12. Traces to

Flows 01 (hint), 05, 06, 10, 11.
