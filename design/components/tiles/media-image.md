# MediaImage

## 1. Purpose

Shows one picture inside an extension's panel or settings page: the `image` node of the UI schema.
It is built in, styled by the shell, and the only way an extension shows a bitmap in its UI.

**When not to use:**
- Vector drawing from ops: use [DrawingSurface](drawing-surface.md).
- A picture on the page: that is a document image, not UI.
- Cover or thumbnail art: [ItemTile](item-tile.md), [PickerTile](picker-tile.md).
- An icon: Icon with a built-in name.

## 2. Anatomy

```
 +--------------------------+
 |                          |   frame: radius/item, hairline border
 |          image           |   fit: contain or cover
 |                          |
 +--------------------------+
  Caption from the row (optional)
```

Parts: frame, image, letterbox, placeholder, tap layer (only if `tap` is handled).

## 3. Variants and sizes

`fit`: `contain` | `cover`. Width fills the panel content width; height follows the image ratio, at
most half the panel height (the schema rule for a panel; a settings page uses the same cap). A
`cover` fit crops to a ratio the extension may set from a short list (4:3, 1:1, 3:4); the default is
the image's own ratio clamped to 3:4 to 4:3, so portrait art is not cut. A `contain` image capped at
half the panel height is centred and may be narrower than the panel. Frame radius `radius/item`;
the frame is always drawn so an image never touches a panel edge. Margins `space/md`.

## 4. States

| State | Changes |
|---|---|
| Default | `surface/sunken` letterbox, `border/subtle` hairline; the picture as is |
| Hover | no change unless tapped: `hover/fill` over the frame edge only |
| Focus-visible | ring on the frame (only when it has `tap`) |
| Active | `press/fill` veil `media/veil` |
| Selected, disabled | n/a. The extension disables its button instead |
| Loading | `skeleton/fill` at the declared ratio; archive images decode off the main thread |
| Empty | no `source`: not drawn; a tree with a missing `alt` is rejected |
| Error | placeholder: `surface/sunken`, `image_off` at `size/icon/feature`, and the `alt` text in `text/muted`; an error is logged for the extension, with no Toast |
| Drag | n/a. Images in UI do not drag |

## 5. Content rules

- `source` is an archive path or an item hash only, never a URL. A path outside the archive is
  refused. Accepted types: png, jpg, webp, svg without scripts; at most 4 MB and 4096 px on the
  long side, down-sampled to the frame at draw time.
- `alt` is required, plain text, up to 200 characters; German and Norwegian alt text is spoken, so it
  is not cut. Very long alt wraps in the placeholder. No image: node omitted. 500 images: bound by the
  500 node limit; lists of images virtualise inside `list`.
- An image never carries text the user must read; text ships as `row` text.

## 6. Overflow rule

An image taller than the cap scales down to fit (`contain`) or crops (`cover`). Never scrolls inside
the frame.

## 7. Extension slots

This is an extension control: the `image` node in a side panel tab, a settings page or a block
editor. Only the shell frames it; no colour, size or position props exist.

## 8. Pack content

None. Pack art uses ListRow or PickerTile.

## 9. Input

- **Stylus and touch:** tap sends `tap` when handled; else the frame is inert. Slot at least `size/hit/min`.
- **Mouse:** click; pointer cursor only when `tap` is handled. **Keyboard:** focusable only with
  `tap`; Enter and Space send it.
- **Roles:** `img` with `alt`, or `button` named by `alt` when tappable; Compose `Image` with
  `contentDescription`; WinUI `Image` with `AutomationProperties.Name`; SwiftUI `Image` with
  `.accessibilityLabel`. Decorative images are not allowed; the schema requires `alt`.

## 10. Tokens

| Token | Use |
|---|---|
| `space/md`, `radius/item`, `size/icon/feature`, `size/hit/min` | geometry |
| `surface/sunken`, `border/subtle`, `skeleton/fill` | frame, loading |
| `hover/fill`, `press/fill`, `media/veil` | tap |
| `type/caption`, `text/muted` | placeholder text |

## 11. Do and Don't

- Do require `alt` on every image.
- Do keep the frame and the cap; the panel stays the shell's.
- Don't load from a URL.
- Don't put controls over the image.

## 12. Traces to

Flow 09 (extension panels).
