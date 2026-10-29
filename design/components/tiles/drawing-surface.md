# DrawingSurface

## 1. Purpose

Draws an extension's vector ops inside its panel or settings page: the `drawing` node of the UI
schema (for example a chart or a timer face). Ops are the same as a block render, drawn natively by
the shell, cached by hash.

**When not to use:**
- A bitmap: use [MediaImage](media-image.md).
- A block on the page: that is the block render contract, not UI.
- Anything that must be read as text: use `row` text.

## 2. Anatomy

```
 +--------------------------+
 |                          |   frame: radius/item, hairline border
 |     drawn ops (sRGB)     |   fixed ratio, no text ops in a panel
 |                          |
 +--------------------------+
```

Parts: frame, canvas (ops), placeholder, tap layer.

## 3. Variants and sizes

One style. `ratio` (width over height) comes from the node; width fills the panel content width and
height follows the ratio, at most half the panel height, never below `size/hit/min`. The ops are
scaled to the frame with the ratio kept. The frame is drawn on `content/paper/white` in every theme
("drawings are drawn on paper"), so extension colours keep the contrast they were made for; a lint
warns on ops whose colour is within 3:1 of that ground. The frame has a `border/strong` hairline;
margins `space/md`.

## 4. States

| State | Changes |
|---|---|
| Default | `surface/sunken`, `border/subtle`; ops painted in their own sRGB colours |
| Hover | `hover/fill` on the frame edge only, if `tap` is handled |
| Focus-visible | n/a. Not focusable |
| Active | `media/veil` over the canvas |
| Selected, disabled | n/a |
| Loading | `skeleton/fill` at the ratio until the first draw (under the 20 ms budget this is rarely seen) |
| Empty | no ops: the frame shows nothing and keeps its size |
| Error | over budget (5000 ops, 256 KB of path data), a text op in a panel, or an invalid op: the frame shows `image_off` with "Drawing unavailable" in `text/muted`; the error goes to the extension, not the user |
| Drag | n/a |

## 5. Content rules

- Ops are the render contract's: `path`, `rect`, `ellipse`, `line`, `group`, `clip`, `transform`.
  `text` is refused in a panel (it exists only in block drawings). Colours are the extension's, so
  the shell guarantees no contrast; the frame supplies a neutral ground.
- Accessibility text: the node requires `alt` (see README). The shell never names it by extension and id.
- No ops: empty frame. 1 op: fine. 500 nodes: bound by the tree limit; one drawing per tab is
  advised, since each takes up to half the panel.
- Tap coordinates are normalised to the frame (0 to 1 in x and y), so they do not depend on size.

## 6. Overflow rule

Ops outside the frame are clipped by the frame. A drawing taller than the cap scales down at the
same ratio.

## 7. Extension slots

This is an extension control: `drawing` in side panel tabs, settings pages and block editors. No
styling props exist.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch:** tap sends `tap(x, y)`; a pen tap counts. No drawing on it: it is not a
  canvas. A drawing with `tap` must offer a button for the same job; lint warns when it does not.
- **Mouse:** click sends `tap`. **Keyboard:** never focusable and has no key route; the paired
  button is the keyboard route.
- **Roles:** always `img` with the `alt` label; `tap` is for pointer users and is not exposed to
  assistive technology, which uses the paired button; Compose `Canvas` with
  `semantics { contentDescription }`; WinUI `Canvas` with `AutomationProperties.Name`; SwiftUI
  `Canvas` with `.accessibilityLabel`.

## 10. Tokens

| Token | Use |
|---|---|
| `space/md`, `radius/item`, `size/hit/min`, `size/icon/feature` | geometry |
| `content/paper/white`, `border/strong`, `skeleton/fill` | ground, frame, loading |
| `hover/fill`, `media/veil` | tap |
| `type/caption`, `text/muted` | error text |

## 11. Do and Don't

- Do keep the frame, the cap and the neutral ground.
- Do pair a tappable drawing with a button.
- Don't use it for text or for something the user must read.
- Don't let ops draw outside the frame or beyond the budget.

## 12. Traces to

Flow 09.
