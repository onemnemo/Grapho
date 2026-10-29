# Minimap

## 1. Purpose

A small overview of a Canvas document: where the content is and where the view is. Tap or drag to
move the view. It is the way to find your place on a surface with no pages.

**When not to use:**
- Documents with pages (Notebook, PDF): PageScrubber and the Go to page Popover.
- Saved positions: the Views list in the Pages tab (named, with thumbnails).
- Framing everything at once: the `view.fitPage` command (Fit all).
- Seeing every page's thumbnail: the Pages tab.

## 2. Anatomy

```
 top corner of the page area, off-hand side, below the band stack
 +------------------------+
 |  .--.        .         |   content extents: blocks of ink and objects
 |  '--'   +------+       |
 |   ..     | view |       |   the viewport rectangle
 |          +------+       |
 +------------------------+
```

| Part | Notes |
|---|---|
| Frame | `radius/surface`, fill `media/glass`, edge `media/edge`; glass items carry no elevation. The page is unknown artwork, so the glass fill is used |
| Content | Rectangles for the extents of ink and objects, in `canvas/minimap/content` (to add). Drawn from a coarse grid, not a render of the strokes |
| Viewport | The current view as a rectangle: outline `canvas/minimap/viewport` (to add), `border-width/emphasis`, a light fill. Always at least `size/minimap/viewport-min` |
| Strip | A drag strip along the frame's top edge, at least `size/hit/touch` high, to move the frame to another corner (remembered per device) |
| Origin mark | A small dot at the canvas origin so an empty canvas still has a reference |

## 3. Variants and sizes

None. One shape. Size `size/minimap/width` by a height from the content's aspect ratio, between
`size/minimap/height-min` and `size/minimap/height-max`; on compact windows the width is
`size/minimap/width-compact`. The frame never exceeds a third of the page area's width or height.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Off | Not drawn. Default |
| On | Toggled by the Note bar's map toggle (`view.minimap`, Ctrl+M). It stays until toggled off, Escape (focus in it), or the command. It is not a transient overlay: the user asked for it |
| Idle | Frame `media/glass`; content and viewport drawn |
| Hover | A move cursor over the viewport rectangle and the strip; no fill change |
| Focus-visible | `component/focus/*`, gap colour set to the glass fill |
| Active (dragging) | The viewport rectangle follows the pointer; the page view follows live. Content blocks do not redraw while dragging |
| Updating | The content layer redraws when a stroke commits (pen up, undo, redo), at most once per `motion/duration/base`. The viewport updates with scroll and zoom, without animation |
| Pen down | The minimap does not redraw, move, resize or reposition while the pen is on the page. Its content updates at the lift |
| Empty | An empty canvas shows the frame, the origin mark and the viewport only. No text |
| Loading, Error | n/a. Extents are computed by the core and are available at open |
| Disabled | n/a. The toggle is hidden on documents with pages |

## 5. Content rules

- **Text:** none in the frame; the accessible name and Tooltip are localised.
- **No content:** the origin mark and the viewport. **One item:** one rectangle; the frame keeps its size.
- **Shape:** the frame has a fixed width and a height between `size/minimap/height-min` and
  `size/minimap/height-max`. The content is scaled to fit its extents with the aspect kept, and the
  rest of the frame is letterboxed, so a tall or wide canvas is never distorted.
- **500 items and huge canvases:** extents are coarse grid cells, so cost is constant. The scale fits
  all content up to `limit/minimap-reach` viewports from the view; farther content is clipped to the
  frame edge with a small edge mark. One rule, not two.

## 6. Overflow rule

One minimap. If the page area cannot hold it beside the dock and open panel (a window under
`layout/breakpoint/short` tall with the panel open), it is hidden and the toggle is disabled with the
reason ("No room for the map"); Views still work. It never exceeds a third of the page area.

## 7. Extension slots

None. An extension's blocks appear as ordinary content rectangles.

## 8. Pack content

None. Documents embed copies of what they use, so the minimap never shows pack state.

## 9. Input

- **Placement and handedness:** the page area's top corner on the off-hand side, `space/md` below
  the band stack (Note bar, FindBar), clear of the dock, the open panel and the scrubber. Right-handed:
  the top left of the page area. The toggle, Toasts and the Canvas chips never share its slot:
  Toasts are at the bottom, chips at top centre. When the panel opens or closes the minimap steps to
  its corner at once, without animation, and never while the pen is down.
- **Stylus:** the minimap sits in the corner where a canvas writer starts, so the frame is **ink
  except for two places**: a pen contact that starts on the viewport rectangle or on the strip drags
  it (navigates or moves the frame). Any other pen contact inside the frame is ink and passes through.
  Hover does nothing. The frame can be moved with the strip to another corner.
- **Touch:** a finger tap anywhere in the frame sets the viewport centre to that point; drag moves it. While the pen is in
  proximity or within `delay/palm-window`, touches larger than `input/palm-threshold` are ignored,
  and a finger tap counts only if under `input/chrome-tap/duration` and `input/chrome-tap/travel`.
- **Mouse:** click and drag; the wheel over it scrolls the view; Ctrl and the wheel zooms the view.
- **Keyboard:** the minimap is one Tab stop after the dock while on. Arrow keys move the view by
  a tenth of the viewport, Shift and arrows by a whole viewport, Home goes to the origin, Escape
  closes it and returns focus to the map toggle. Ctrl+M toggles from anywhere.
- **Roles:** the frame is `role="img"` named "Canvas map" with a description that updates when
  the view settles ("Showing across 2 to 6 of 12, down 1 to 4 of 9"), and it takes arrow keys as
  above. Compose: `contentDescription` plus `scrollBy` custom actions; WinUI `ScrollPattern`;
  SwiftUI `accessibilityScrollAction`.
- **Tooltip:** the Note bar's map toggle is "Show map, Ctrl+M" and "Hide map, Ctrl+M".
- **Commands:** `view.minimap`, `view.fitPage` (Fit all). Setting: none.

## 10. Tokens

| Token | Used for |
|---|---|
| `media/glass`, `media/edge`, `component/focus/*` | Frame, edge, focus |
| `canvas/minimap/content`, `canvas/minimap/viewport` | Content and viewport (to add, each with a pair of 3:1 on `media/glass` over a white page) |
| `radius/surface`, `radius/item`, `border-width/emphasis`, `space/md` | Shape and gaps |
| `size/minimap/width`, `width-compact`, `height-min`, `height-max`, `limit/minimap-reach` | Size and reach (to add). Compact is `layout/breakpoint/medium` and below |
| `size/minimap/viewport-min` | Minimum viewport rectangle (to add) |
| `motion/duration/base` | Content redraw rate |
| `delay/palm-window`, `input/palm-threshold`, `input/chrome-tap/*` | Palm rule |
| `z/floating-bar` | Layer |

## 11. Do and Don't

**Do**
- Let a pen draw through the frame except on the viewport and the strip.
- Draw content from a coarse grid so it costs the same on a huge canvas.
- Keep the viewport rectangle visible at any zoom, at its minimum size.
- Put it in the off-hand corner below the band stack and mirror for the left hand.
- Update the content layer at the stroke commit, not during the stroke.

**Don't**
- Don't fade it out; the user turned it on.
- Don't make the whole frame take the pen.
- Don't redraw, resize or reposition it while the pen is down.
- Don't cover the dock, the panel or the toast stack with it.
- Don't show it on Notebook or PDF documents.

## 12. Traces to

Flows: 11 navigate-long-document (canvas). Replaces: none (new). Document kind: Canvas
(`system/document.md` section 10).
