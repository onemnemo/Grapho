# GroupHeading

## 1. Purpose

The heading above a group of tiles or rows: its name, a count, a collapse control and one optional
action ("Show all"). Includes the **AlphabetRail**, the letter scrubber shown when a long list is
sorted by Name.

**When not to use:**
- A page or sheet title: use the TopBar or Sheet header.
- A Menu section label: Menu headings.
- A form section inside Settings: a `type/heading` label with no control.

## 2. Anatomy

```
 v  Recent  8                                       Show all
 ---------------------------------------------------------
                                                      A
                                                      B   <- AlphabetRail
```

Parts: collapse chevron, name, count, action, rule; rail letters and the current-letter bubble.

## 3. Variants and sizes

- `kind`: `heading` | `rail`. A heading is one `size/control/default` high row of `type/heading`
  text; the count is `type/micro` `text/muted`. The whole heading (chevron, name, count) is one
  button that collapses the group; its action is a separate text Button.
- The rail is one column at the inline end of the scroll area, `size/hit/min` wide, letters at
  `type/micro` spaced to fit; fewer letters than fit appear in full, more are thinned to every
  second letter, never smaller.
- Sticky: the current group heading sticks under the top bar on `surface/base` with a `border/subtle`
  hairline, so covers scrolling under it do not show through.

## 4. States

| State | Changes |
|---|---|
| Default | name `text/default`; count `text/muted`; chevron `size/icon/small` |
| Hover, active | `hover/fill`, `press/fill` on the heading button |
| Focus-visible | ring |
| Collapsed | chevron turned; the group is hidden; the count stays; the state is remembered per place |
| Disabled | n/a |
| Loading | the name and count show; the group shows a Skeleton `grid` |
| Empty | the group is omitted; a heading with zero items never shows |
| Rail, default | letters `text/muted`; letters with no items `text/subtle` (on the surface only) |
| Rail, active | scrubbing: the current letter `text/default` on `selection/fill` with a bubble beside it on `surface/overlay` and `elevation/popover` |
| Drag | n/a. A group heading is not a drop target |

## 5. Content rules

- Names are fixed words: Recent, Folders, All. In Sort by Name the headings are the letters. Counts
  are the true count ("8", "1,240"), the number format follows the locale.
- Very long names (German "Zuletzt verwendet"): wrap to two lines before the count moves.
- The rail index is built from the union of scripts present in the data (ICU index buckets), ordered
  by the locale, with the UI language's script first. A library of English, Hindi and Japanese titles
  gets Latin, Devanagari and Kana buckets, not one "Other". `#` holds digits and symbols.
- The rail shows when the list is over the long-list size (`feedback/long-list`, 30 items) and the sort is Name. In
  Newest or Modified (date sorts) and Size the same `rail` is a scrubber with a bubble of the date or
  value at the thumb (month and year, or the size band); each sort key registers its bubble source.
  Typing a letter jumps in Name sort only.

## 6. Overflow rule

The heading never overflows; "Show all" stays, the count wraps. The rail thins letters; it never
scrolls.

## 7. Extension slots

None.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch:** tap the heading to collapse; drag along the rail scrubs and the list follows
  at once; letters announce while scrubbing. The rail sits inside `size/edge-guard` and the grid
  reserves its width, so it never covers a ••• or the back gesture. The rail slot ignores a palm
  while the pen is near.
- **Mouse:** click; hover on the rail highlights a letter; click jumps. Collapsed state persists.
- **Keyboard:** Enter or Space collapses; "Show all" is the next Tab stop. The rail is one Tab stop:
  Up and Down move the letter and jump; typing a letter in Name sort also jumps (grid shortcut).
- **Roles:** the heading is `heading` containing a `button` with `aria-expanded`; the rail is a
  `navigation` labelled "Jump to letter" with `button`s. Compose `Modifier.semantics { heading() }`;
  WinUI `Expander`-style header and `SemanticZoom`-like index; SwiftUI section header and
  `.sectionIndexLabel`. Name: "Recent, 8 items, expanded".

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/default`, `size/hit/min`, `size/icon/small` | geometry |
| `space/sm`, `space/md`, `layout/gutter/*` | spacing |
| `type/heading`, `type/micro`, `type/label` | text |
| `text/default`, `text/muted`, `text/subtle` (surface only) | text roles |
| `hover/fill`, `press/fill`, `selection/fill`, `border/subtle`, `surface/base`, `surface/overlay`, `elevation/popover`, `size/edge-guard` | states, bubble, sticky |

## 11. Do and Don't

- Do put the count in the control and say it truly.
- Do mirror the rail to the inline end in right to left.
- Don't show a rail with fewer than 30 items or outside Name sort.
- Don't thin letters by shrinking them below `type/micro`.

## 12. Traces to

Flow 05.
