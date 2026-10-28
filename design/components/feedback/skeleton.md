# Skeleton

## 1. Purpose

Hold the real layout while content loads, so nothing jumps. Shown only after `feedback/delay/skeleton`
(300 ms) of loading, and once shown it stays for at least `feedback/hold/skeleton` (300 ms) (SYSTEM-MAP
4.6). It is one of three loading patterns: Skeleton for a content area, ProgressItem for a
core job, inline busy inside a Button or row.

**When not to use**
- A core job with a result: **ProgressItem**.
- An empty result: **EmptyState**. A failed load: a `region` **Banner** with Retry.
- A control waiting on a reply: inline busy on the control.

## 2. Anatomy

```
 +---------+  +---------+      block: the size of the real item
 | block   |  | block   |      line:  a text line at the real line height
 +---------+  +---------+      disc:  avatars and swatches
 [line_____]  [line_____]
```

Parts: `block`, `line`, `disc`. Built from the real component's own layout so content replaces it with no
shift. Not a free-floating widget.

## 3. Variants and sizes

| Prop | Values |
|---|---|
| `shape` | `block` \| `line` \| `disc` |
| `layout` | `grid` \| `list` \| `panel` \| `page` |

`page` is a canvas page loading (a PDF import, a page thumbnail). Heights come from the real item (a row
is `size/row/list`, a line is the text style's line box). No size of its own.

## 4. States

| State | Tokens that change |
|---|---|
| Default | fill `skeleton/fill` on `surface/base` or `surface/raised` |
| Hover, focus-visible, active, selected, disabled | not interactive, not focusable |
| Loading | this is it; shimmer is a slow brightness sweep over `motion/duration/slow`, repeating |
| Empty | replaced by EmptyState in the same region |
| Error | replaced by a `region` Banner with Retry; a skeleton never lingers |
| Drag | not used |

**Reduced motion**: a static tint, no shimmer. Nothing animates while the pen is on the page.

Timing in the host: start the delay when loading begins. If loading ends first, show content only. If the
delay fires, show the skeleton and keep it for the hold time before swapping. If loading exceeds
`feedback/timeout/load`, the host stops the skeleton and shows the `region` Banner with Retry and the reason.

## 5. Content rules

- Long text (German, Norwegian): lines are shares of the container width, so real text fits without shift;
  the last line is shorter.
- No text: a title already known is shown, not skeletoned.
- 1 item: one placeholder. 500 items: only as many shapes as fill the viewport; virtualised lists never
  render more.
- There is no "Loading" text line and no "Still loading" message. The timeout handles a hang.

## 6. Overflow rule

Never more than the viewport fills. One skeleton per region; no nesting.

## 7. Extension slots

None. A loading extension panel gets a `panel` Skeleton from the host.

## 8. Pack content

A cover grid loading uses a `grid` Skeleton at the real cover size. Pack state is not loading and is not
shown here.

## 9. Input

None. Not focusable, no pointer, no keys; taps do nothing and do not swallow taps meant for controls
around it.
- **Name and tooltip**: none.
- **Announcements**: the region has `aria-busy` while a skeleton shows. One polite status "Loading" when it
  first appears (only if it appears), then the count when content arrives ("24 notes"). Shapes are hidden
  from assistive technology.
- Roles: ARIA `aria-busy`, shapes `aria-hidden`. Compose `clearAndSetSemantics` on shapes and
  `stateDescription` on the region. WinUI `AccessibilityView="Raw"`. SwiftUI `accessibilityHidden(true)`.

## 10. Tokens

| Token | Use |
|---|---|
| `skeleton/fill` | all shapes (declared pairs on `surface/base` and `surface/raised`) |
| `radius/item`, `radius/pill`, `space/sm`, `space/md` | shape and gaps |
| `size/row/list` and the real item's size tokens | geometry |
| `feedback/delay/skeleton`, `feedback/hold/skeleton`, `feedback/timeout/load` | timing |
| `motion/duration/slow`, `motion/easing/standard` | shimmer |

## 11. Do and Don't

Do
- Match the real layout so nothing shifts on swap.
- Wait before showing; hold once shown.
- Time out into a Banner with Retry.

Don't
- Show a skeleton for a fast, empty or failed load.
- Make shapes focusable or announce each one.
- Add a text "Loading" line.

## 12. Traces to

Flows 02, 04, 06, 07, 08, 09, 11, 13, 15. New; replaces spinners over lists and grids.
