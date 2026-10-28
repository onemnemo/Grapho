# EmptyState

## 1. Purpose

Say that a place has nothing in it, and what to do. Three kinds (SYSTEM-MAP 4.6): **first use** (one
sentence, one primary action, the mascot allowed), **filtered or searched** (what is filtered, Clear, no
mascot) and **quiet** (a place that is simply empty, such as Trash or Recent: a title and a line, no
action).

**When not to use**
- A failed load: a `region` **Banner** with Retry. An error never looks like empty.
- A load in progress: **Skeleton**.
- A partial result: the items that worked plus a `warn` **Banner**.

## 2. Anatomy

```
        [ mascot or icon ]
      Nothing here yet            title (required)
   Start a notebook or import.    body (one sentence)
     [ New notebook ]  Import     primary (optional), secondary (optional)
```

Parts: `art`, `title`, `body`, `primary`, `secondary`. Centred in the empty region, with a text measure of
`layout/measure/empty`.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `kind` | `first-use` \| `filtered` \| `quiet` |
| `size` | `md` \| `sm` |

- `first-use`: `md` or `sm`. The mascot is allowed in `md` only, as an art prop at the largest whole pixel
  scale that fits. Primary required (`size/control/large` in `md`).
- `filtered`: always without art; the title names the filter ("No notes tagged Exams"); primary is Clear.
- `quiet`: title and one muted line, no art, no action. Used for Trash, Recent, an empty folder, the
  activity list.
- `sm`: a `size/icon/feature` icon or none; for Popovers and panels.

The mascot appears only in `first-use` `md`, never in `filtered`, `quiet`, a panel, or chrome.

## 4. States

| State | Tokens that change |
|---|---|
| Default | on the host surface; title `text/default`, body `text/muted` |
| Hover, active | on its buttons only |
| Focus-visible | focus recipe on buttons |
| Selected | not used |
| Disabled | a primary that cannot run is disabled with its reason as the button's description; the body does not change |
| Loading | not used; shown only after loading ends |
| Empty | this is it |
| Error | not used; see When not to use |
| Drag | a drop region shows `state/drag/target-fill` and `state/drag/target-line`, body "Drop to add" |

Reduced motion: the mascot is a still image showing its base colours.

## 5. Content rules

- Title: at most five words, always present. Body: one sentence.
- Very long text (German, Norwegian): wraps within `layout/measure/empty`; art scales down first; never
  clipped.
- No text: not allowed.
- 1 and 500 items: not shown. A list that gains an item replaces it with no animation.
- The mascot has no speech bubble.

## 6. Overflow rule

One primary and at most one secondary. A third action belongs in the screen's own toolbar.

## 7. Extension slots

None. An extension panel's empty state uses `sm` `quiet` and its own words.

## 8. Pack content

No pack state is shown here. An empty pack library is `first-use` with Get more; a filter matching nothing
is `filtered`. Pack state names appear only in the picker, Content pages and Packs pages.

## 9. Input

- **Stylus and touch**: tap the buttons; targets the larger of `size/hit/min` and 44. The art is not
  interactive.
- **Mouse**: standard button hover.
- **Keyboard**: buttons in Tab order; no keys of its own.
- **Name and tooltip**: its buttons are text buttons and read their label from the command (New notebook,
  Import).
- **Announcements**: a labelled group, not live. When a filter empties a list, the list count announces
  politely ("0 notes"). The art is decorative.
- Roles: ARIA group labelled by its title. Compose merged `semantics`. WinUI labelled `StackPanel`. SwiftUI
  `accessibilityElement(children: .contain)`.

## 10. Tokens

| Token | Use |
|---|---|
| `text/default`, `text/muted`, `type/heading`, `type/body` | text |
| `size/icon/feature`, `space/lg`, `space/xl`, `layout/measure/empty` | art and layout |
| `size/control/large`, `size/control/default`, `action/primary/*`, `action/quiet/*`, `text/on-action` | buttons |
| `state/drag/target-fill`, `state/drag/target-line` | drop region |
| `focus/*`, `size/hit/min` | focus and targets |

## 11. Do and Don't

Do
- Give a title and one sentence; add one primary only when there is a real action.
- Name the filter and offer Clear.
- Use `quiet` for places that are simply empty.

Don't
- Show the mascot outside first use `md`.
- Use it for a failed load.
- Invent an action for Trash.
- Add a speech bubble.

## 12. Traces to

Flows 01, 02, 05, 07, 08, 10, 11, 15. Replaces `EmptyState`; `md` with the mascot is `first-use`.
