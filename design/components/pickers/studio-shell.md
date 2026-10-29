# StudioShell

## 1. Purpose

The full-screen frame for making a look (cover, folder style, canvas preset, template, pen; a sticker has no
editor, it is imported): a title field, Undo and Redo, Done and Cancel, a preview of the real item and the editor's own
panel. One shell for every look; the type supplies the canvas and the panel. It replaces five Studio
parts.

**When not to use:**
- Editing a theme: ThemeEditor (a page in Settings).
- A short form with a few fields: Sheet.
- Editing a document: the document itself.

## 2. Anatomy

```
Expanded
+------------------------------------------------------------------------+
| [Cancel]   Cover 3 (title field)                 [Undo][Redo]  [Done]   |
|----------------------------------------------+-------------------------|
|                                              | editor panel            |
|        canvas / preview of the real tile     |  (type specific tabs)   |
|                                              |                         |
+----------------------------------------------+-------------------------+
Compact: canvas on top, panel as a bottom panel with peek and full
```

| Part | Notes |
|---|---|
| Top bar | Cancel (Button `text`), Title (TextField `inline`), Undo and Redo (IconButton `quiet`, commands `studio.undo` and `studio.redo`, Tooltips with keys), Done (Button `primary`, `studio.done`, the one primary), Cancel (`studio.cancel`) |
| Canvas | The type's editing surface, with a preview of the real tile beside or below it |
| Panel | The type's tools and options in SidePanel structure; tabs from Tabs when two to four |
| Preview | Always the real renderer-kind tile at its registry size |

## 3. Variants and sizes

`layout` by window class: `beside` (expanded), `over` (medium), `bottom` (compact). Top bar is
`size/bar/top`; controls role `default`. In `over` the canvas shifts away from the panel and the real
tile preview becomes an inset on the canvas corner; the canvas can still pan. At compact the title
field keeps a minimum width; when Cancel and Done labels (German "Abbrechen", "Fertig") would take it
below that, Cancel becomes a Back arrow IconButton.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Default | `surface/base`; canvas well `surface/sunken` |
| Hover, focus-visible, active | Each control's own; focus ring `focus/*` |
| Selected | The active tool or tab, `selection/fill` plus `selection/indicator` |
| Disabled | Undo or Redo with nothing to do: `state/disabled/fg`, plain; Done disabled when the title is empty, reason as description |
| Loading | Skeleton in the canvas after `feedback/delay/skeleton` |
| Empty | A new look starts from the type's default; never blank without a reason |
| Error | A failed save keeps the Studio open: danger Banner with Retry; nothing is lost |
| Return | Studio is a place. From a Sheet it returns to the Sheet with the look selected; from a document panel it returns to the document and reopens the panel on the same tab |
| Dirty | Cancel with changes asks the `unsaved` Dialog once (default Keep editing) |
| Saving | Done shows inline busy after `feedback/delay/spinner` and keeps its width |

## 5. Content rules

- Title: defaults to the type's next name ("Cover 3"); long titles one line with ellipsis, full in
  the field; German and Norwegian text allowed; empty is refused with "Name it to save".
- 1 undo step to hundreds: history is per Studio session, bounded by the core; Redo clears on a new edit.
- The preview shows long text (a cover title) in its real wrap, at 200% too.

## 6. Overflow rule

Panel tabs: up to four, then the SidePanel TabRail rule. Top bar: Cancel, Title, Undo, Redo, Done
never overflow; on compact Undo and Redo stay, the title field shrinks first.

## 7. Extension slots

None. Studio panels are built in.

## 8. Pack content

The look is saved to Yours in the Picker it came from. Edit on a pack or built-in look works on a
copy; the title shows the copy's name. Components from a pack are copied with licence checks by
Export as pack, not here.

## 9. Input

- **Stylus and touch:** the pen draws or manipulates on the canvas; there is no palm rejection
  difference from a document. Chrome follows the palm rule of the bars family.
- **Mouse and keyboard:** Ctrl or Cmd+Z, Shift+Z (`studio.undo`, `studio.redo`); Ctrl or Cmd+Enter is
  `studio.done` from any focus in Studio; Escape is `studio.cancel` and asks first when dirty; F6
  cycles title, canvas, panel. Right to left mirrors the top bar order, not the canvas.
- **Roles:** the shell `main` labelled by the title; canvas `application` with a text
  alternative; panel `region`. Back and Escape follow the dismiss order of SYSTEM-MAP 3. Compose, WinUI
  and SwiftUI: a full screen destination, not a dialog.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/base`, `surface/raised`, `surface/sunken` | shell, panel, canvas well |
| `size/bar/top`, `size/control/default`, `size/panel/width`, `size/panel/width-over` | bar, controls, panel |
| `space/lg`, `space/md`, `space/sm` | edges, gaps |
| `radius/item`, `radius/surface` | canvas, panel |
| `type/heading`, `type/label` | title, buttons |
| `action/primary/*`, `action/quiet/*` | Done, quiet controls |
| `selection/fill`, `selection/indicator`, `state/disabled/fg` | states |
| `z/sheet`, `elevation/sheet`, `focus/*` | layer, edge, focus |

## 11. Do and Don't

Do:
- Name the look where it is made, in the top bar title.
- Return to the originating Sheet with the new look selected.

Don't:
- Ask a Dialog for a name.
- Add a second primary or a Save and a Done.
- Show the preview with sample data other than the real tile.

## 12. Traces to

Flow 07 (Studio and saved looks). Commands to register: `studio.undo`, `studio.redo` (`studio.done`, `studio.cancel` exist). Replaces StudioCanvas, StudioPanel, StudioRail, StudioSide,
StudioTopBar. Uses TextField, Button, IconButton, Tabs, SidePanel structure, Dialog `unsaved`.
