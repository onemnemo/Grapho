# BlockPlaceholder

## 1. Purpose

Stand in for an extension block whose extension cannot run, and keep the note readable: it shows
the picture and text the block stored when it was last edited, says why it is not live, and offers
the way out. Nothing in the note is lost or hidden.

**When not to use:**
- A raster or vector asset over a size cap or damaged: the content type's own placeholder (the
  content registry), not this frame.
- A page that failed to render: the page's own "Could not show this page" placeholder.
- A lasting state of the whole document (offline, safe mode): Banner.
- A pack's items that are not installed: pack state is not shown in documents (a neutral Banner
  at document level covers a received document).

## 2. Anatomy

```
+------ Made with "Flashcards" (4c1f) ------+   title row
|  Picture saved in the note                 |   caption
|  +--------------------------------------+  |
|  |        stored fallback image         |  |
|  +--------------------------------------+  |
|  Deck: Biology chapter 4                   |   stored text
|--------------------------------------------|
|  Flashcards is not installed here.         |   status
|  [ Get extension ]      [ Keep as image ]  |   actions
+--------------------------------------------+
```

| Part | Notes |
|---|---|
| Frame | `radius/item`, edge `canvas/placeholder-edge`, fill `canvas/placeholder-bg` (page-relative, fixed in every theme: the page is paper); sits in the page at the block's own bounds. It is drawn inside the bounds and never over its neighbours |
| Title row | "Made with" and the stored extension name in quotes plus the short id, `type/label`, `canvas/placeholder-text`. The name is the stored one until the extension is installed |
| Caption | "Picture saved in the note", `type/caption`, `canvas/placeholder-muted` |
| Fallback | The stored picture, drawn at its stored size scaled to fit, `radius/item`, unchanged colours. Made by the core, not the extension |
| Stored text | The `describe` text, `type/body`, `canvas/placeholder-text` |
| Status | One line by cause, `type/caption`: text is `canvas/placeholder-muted` with a leading `info` icon in `size/icon/inline` |
| Actions | Button `secondary` `small` and `text` `small`, always visible at full size so recovery is discoverable on touch. Below `size/placeholder/actions-min` width they become one Menu on the status icon |

## 3. Variants and sizes

`cause`: `not-installed` | `off` | `in-trash` | `blocked` | `needs-newer` | `no-runtime`.

| Cause | Status line | Actions |
|---|---|---|
| `not-installed` | "Flashcards is not installed here." | Get extension, Keep as image |
| `off` | "Flashcards is turned off." | Turn on, Keep as image |
| `in-trash` | "Flashcards is in Trash." | Restore extension, Keep as image |
| `blocked` | "Flashcards is blocked on this device." | Keep as image |
| `needs-newer` | "Needs a newer version of Flashcards." | Update, Keep as image |
| `no-runtime` | "Not available on this device." | Keep as image only |

The frame scales with the block: `full` above `size/placeholder/actions-min` wide, and `compact`
(title row and fallback only, the status and actions in a Menu on tap) below it. Heights are by
content; the frame never exceeds the block's bounds.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Default | As anatomy. No animation, no pulsing |
| Hover | Mouse only: edge `border/control-hover` |
| Focus-visible | The focus recipe around the frame; |
| Active | A finger or mouse tap on the status icon opens the Menu (compact form); `selection/fill` while open |
| Selected | A block selected on the page: the canvas selection handles (`canvas/handle`) as for any block; the SelectionBar rides it (Copy, Duplicate, Delete) |
| Disabled | Editing is not available and no control looks disabled; the actions are the choices that exist |
| Loading | n/a. The frame is drawn from stored data at open, with no network |
| Empty | No stored text: the title, caption and picture only. No fallback picture (a very old block): the frame shows the title and status only |
| Error | The fallback fails to decode: the title and the status "Picture could not be shown." and Keep as image is offered only if a picture exists; the block stays |
| Restored | When the extension arrives or is turned on, the block is live again with no file change and no animation |

## 5. Content rules

- **Localised text:** the title uses the locale's quotation marks around the stored name; status
  lines are whole catalogue messages with the name as a parameter (case-safe). They wrap; actions wrap
  to a second row, never truncate.
- **Caption and id:** the caption shows only at full size. The short id shows only when the stored
  name is missing or two different extensions share a name; otherwise it is in the accessible description.
- **When it cannot fit** (a block smaller than its contents), parts drop in this order: stored text,
  then caption, then the fallback shrinks to its minimum, then the title row ellipsises. The status
  line and the way out (the actions or the status icon) never drop.
- **No text:** title and status always. **500 placeholders:** drawn as static pictures; status and
  actions are built only for the focused or tapped one.
- **Export and print:** the stored fallback picture is exported; the frame, title and status are not.

## 6. Overflow rule

Two actions at most. Below `size/placeholder/actions-min` they sit in one Menu on the status icon.
The status line wraps and is never dropped.

## 7. Extension slots

Block type (SYSTEM-MAP 7): this component is what a block type becomes when its extension is
missing. It is drawn by the shell, never by the extension, and cannot be restyled. An extension off:
the block becomes this frame; the extension's other additions disappear and return when it is turned
on.

## 8. Pack content

Not applicable. A block is an extension's, not a pack's. Documents never show pack state.

## 9. Input

- **Stylus:** the pen draws over a block like any page content; a pen contact never reveals or runs
  anything. A pen user lasso-selects the block, and the SelectionBar More shows the block's actions
  (Get extension, Keep as image and the cause's action) as core command rows.
- **Touch:** a finger tap on the status icon opens the Menu in the compact form; move, resize, copy and delete work as for any
  block; editing does not.
- **Mouse:** click the status icon or an action; right click opens the block's Menu.
- **Keyboard:** the frame is a Tab stop in reading order on the page. Enter opens the Menu in the
  compact form; Tab reaches the actions at full size.
- **Roles:** `group` named "Made with Flashcards", with the status as its description; actions
  are `button`. Compose: `Role.Button` on a container with `contentDescription`; WinUI `Group`
  automation; SwiftUI `accessibilityElement(children: .contain)`. The stored text is readable.
- **Actions** and commands: Get extension opens the catalogue at the extension's id with the name
  and id visible; Keep as image is one undo step and turns the block into a picture; Turn on, Restore
  and Update are core actions, not the extension's. To register: `block.keepAsImage`,
  `block.getExtension`, `block.turnOnExtension`, `block.restoreExtension`, `block.updateExtension`.
- **Tooltip:** not needed; every control has a visible label.

## 10. Tokens

| Token | Used for |
|---|---|
| `canvas/placeholder-bg`, `canvas/placeholder-edge`, `border/control-hover`, `radius/item`, `border-width/hairline` (the first two to add) | Frame |
| `canvas/placeholder-text`, `canvas/placeholder-muted` (to add), `type/label`, `type/caption`, `type/body` | Text |
| `size/icon/inline`, `space/sm`, `space/md`, `space/lg` | Status icon, spacing |
| `selection/fill`, `canvas/handle`, `canvas/handle-edge` | Active and selected block |
| `size/placeholder/actions-min` | Compact switch (to add) |
| `component/focus/*`, `border-width/focus` | Focus |

## 11. Do and Don't

**Do**
- Show the stored picture and text first, and the cause second.
- Show the stored name and short id until the extension is installed.
- Offer Keep as image as one undo step, always when a picture exists.
- Let the user move, resize, copy and delete the block.

**Don't**
- Don't draw over neighbouring blocks or ink.
- Don't animate, blink or reflow the page when the extension arrives.
- Don't show Get extension or Turn on when no runtime exists.
- Don't reveal actions by a pen tap, and don't use theme chrome tokens on the paper page.
- Don't reveal actions by a pen tap, and don't use theme chrome tokens on the paper page.
- Don't let the extension supply the frame, its text or its colours.

## 12. Traces to

Flows: 09 extensions (turn off, remove), 13 open-document (missing assets and extension blocks).
Content system: `content-system/surfaces.md` section 9, `documents.md` extension blocks,
`extension-api.md` fallback. Replaces: none (new; the content system's placeholder block).
