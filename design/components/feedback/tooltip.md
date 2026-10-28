# Tooltip

## 1. Purpose

Name an icon-only control and show its effective shortcut. Nothing else. Every icon-only control has one,
with no exception, including clear, reveal, calendar, stepper buttons, token remove, and dismiss.

**When not to use**
- A first-use hint: an anchored **Popover** with one sentence and Got it (see below).
- Required information: put it in the label or a **Banner**.
- Help for a setting: the setting's help text.

**First-use hint.** Hints are not tooltips. A hint is a Popover anchored to its control, one sentence and a
Got it button, held until Got it is pressed, and recorded as seen only then. It is hoverable, persistent
and focusable like any Popover, and it does not show over the page while the pen writes. It is used for
the first note.

## 2. Anatomy

```
 [ Pen                P ]
   |                  |
   name               shortcut
 [ Pen                P ]
   From Notes Plus        source (extension controls only)
```

Parts: `surface`, `name`, `shortcut` (optional), `source` (optional, its own line), `reason` (disabled
controls, its own line). Offset from the anchor `space/sm`. No arrow.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `placement` | `above` \| `below` \| `start` \| `end` (kept inside the window; default `below`) |

One size. Content is `{name, shortcut?, source?, reason?}` read at show time from the command registry
(`system/registries.md`): the shortcut is the effective one for the platform and context, after remapping,
as the registry's textual chord. Name `type/label`; shortcut, source and reason `type/caption`; max width
`size/tooltip/max-width`.

## 4. States

| State | Tokens that change |
|---|---|
| Default | `surface/overlay`, `border/subtle`, `elevation/popover`, `text/default` |
| Hover (anchor) | shows after `feedback/delay/tooltip` |
| Focus-visible (anchor) | shows at once on keyboard focus, not on focus from a click |
| Active | hides |
| Selected, loading, empty, error | not used |
| Disabled | still shows the name, plus the reason on its own line in `text/muted` |
| Drag | hidden while dragging |

Fade over `motion/duration/fast`. **Reduced motion**: appears at once. Never shown over the page while the
pen writes.

## 5. Content rules

- Name is the same words as the control's accessible name. Shortcut in the platform glyphs.
- Very long text (German, Norwegian): name wraps to two lines at max width; nothing truncated. Name,
  shortcut, source and reason may take four lines at 200% text; that is allowed.
- Extension control: the name stays the accessible name; "From <Extension>" is the `source` line and is the
  description, so the name is not doubled.
- No name: a lint error.
- 1 control: one. 500 controls: each has its own, one shown at a time.

## 6. Overflow rule

One tooltip at a time; a new one replaces the old.

## 7. Extension slots

Everything an extension adds carries its name in tooltips (SYSTEM-MAP 7), as the `source` line. Name,
shortcut and source all fit the layout above.

## 8. Pack content

A pack item's tooltip shows its name only. Pack state is not shown in a tooltip.

## 9. Input

- **Stylus**: S Pen hover shows it after the delay where hover is reported; touching activates.
- **Touch**: a long press on an icon-only control shows it without activating; a short tap activates. It
  stays while the finger is down and for `feedback/hold/touch-tooltip` after release.
- **Mouse**: after the delay; hides on Escape or when the pointer leaves.
- **Keyboard**: on keyboard focus; Escape hides.
- **Announcements**: the accessible description of its control, not a live region, not a Tab stop.
- Roles: ARIA `tooltip` with `aria-describedby`. Compose `TooltipBox` with `PlainTooltip`. WinUI
  `ToolTip`. SwiftUI `.help` and `accessibilityHint`.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `border/subtle`, `elevation/popover` | surface |
| `text/default`, `text/muted`, `type/label`, `type/caption` | text |
| `radius/item`, `space/sm`, `space/md`, `size/tooltip/max-width` | shape and size |
| `feedback/delay/tooltip`, `feedback/hold/touch-tooltip`, `motion/duration/fast` | timing |
| `z/tooltip` | layer |

## 11. Do and Don't

Do
- Read name and shortcut from the registry at show time.
- Give every icon-only control a tooltip and an accessible name.
- Put a hint in a Popover with Got it.

Don't
- Put a button, a link or required text in a tooltip.
- Show one over the page while the pen writes.
- Show a tooltip on a control that has a visible label.

## 12. Traces to

Flows 01, 03, and every icon-only control (via IconButton). Replaces `Tooltip` and `NoteHint`.
