# CommandPalette

## 1. Purpose

Find and run any command by name (SYSTEM-MAP 3 and 4.1). A SearchField over a list of registry commands.
Light modality: no scrim; Escape or a tap outside closes it; a pen-down on the page outside closes it and
the stroke is drawn. It is the only home of "find a command"; there is no `>` mode in the Search field.

**When not to use**
- Finding notes, folders or text: **SearchField** in the Search place.
- Choosing a value: **Select**. Commands for one target: **Menu**.
- Finding a setting: the Settings index.

## 2. Anatomy

```
        +----------------------------------------+
        | [search] Type a command             (x)|  field   (SearchField, scope filter)
        |----------------------------------------|
        | Recent                                 |  heading (empty query only)
        | [ic] Undo            Edit      Ctrl+Z  |  result  (icon, name, caption, shortcut)
        | [ic] Clean up page   Notes Plus        |  extension rows name the extension
        | > Page  (collapsed)                    |  group heading (empty query, collapsible)
        | Show all 84 results                    |  row (when the list is cut)
        | Available in the library               |  hint row (wrong context, no run)
        +----------------------------------------+
```

Parts: `surface`, `field`, `heading`, `result` (`lead`, `name`, `caption`, `shortcut`), `show-all`, `hint`,
`empty`. Results use the Menu row look.

## 3. Variants and sizes

None. Width `size/palette/width` (at most the window minus `space/lg` each side), top centre,
`space/xl` below the top edge, at most `size/palette/max-height`. Rows use `size/row/menu`.

## 4. States

| State | Tokens that change |
|---|---|
| Surface | `surface/overlay`, `border/subtle`, `elevation/popover`, `radius/surface` |
| Result hover | `hover/fill` |
| Result focus-visible | the highlight uses `hover/fill` plus the inset focus recipe; focus stays in the field (`aria-activedescendant`) |
| Result active | `press/fill` |
| Disabled result | `state/disabled/fg`; sorted after enabled results in a query; Enter or a tap runs nothing, announces the reason, shows it in the `hint` line and pulses the row once (no pulse under reduced motion) |
| Loading | none; results are local |
| Empty | "No command matches"; a name valid in another context offers "Available in the library" as a hint row that never runs |
| Error | a failing command closes the palette and reports by Toast |

Open and close fade over `motion/duration/fast`. Reduced motion: none.

## 5. Content rules

- Names, icons and shortcut text come from the registry. **Every result shows a caption**: its group, or its
  extension's name, in both the empty and the query list, so an extension command is never confused with a
  built-in. The registry rejects extension labels equal to built-in labels.
- Matching is core owned and locale aware (å, æ, ø distinct): word start, then substring, then the
  registry's per-locale **keywords** (so "delete" finds "Remove from library"). Ties break by registry
  priority, never by use.
- **Recent** is recency: the last five commands run, most recent first. It is the one exemption from the
  priority rule and is only on an empty query.
- Empty query: Recent, then groups as collapsible headings, collapsed except Recent. Query: one ranked list.
- Very long names wrap to two lines; the shortcut and caption stay on the first line. Nothing truncates.
- 500 commands: the list is virtualised and never cut silently. The announced count is the true count. If
  more than the first screenful match, the last row is "Show all N results", which pages the list in place.
- Argument commands (a later "Go to page" or "Set pen size") push one argument view in the same surface,
  with Back, the same pattern as a Popover.

## 6. Overflow rule

The list scrolls inside `size/palette/max-height`; the field stays pinned. With the software keyboard up,
the list keeps at least three rows; below that the palette moves to the top of the window and the list
shrinks to three rows, scrolling.

## 7. Extension slots

Command slot: extension commands are listed with their extension's name in the caption, unlimited, and
obey the enabled predicate. When an extension is off its commands are hidden. An extension command cannot
take a built-in shortcut; a clashing one is shown without a shortcut.

## 8. Pack content

None.

## 9. Input

- **Open.** Ctrl+K (Cmd+K), command `app.palette`; the "Command palette" row in the top bar overflow of every
  place and in the Note bar More. On a tablet those rows are the way in. Opening closes any Popover or Menu.
  It sits at `z/palette`, over a Sheet or Dialog, and counts as a popover in the Back order.
- **Over a modal layer.** Commands that would open a Sheet or Dialog are disabled with reason `modal_open`
  while one is open; other commands run.
- **Field.** It is a SearchField with scope `filter` over commands, so it has the clear button, the Tooltip
  and handwriting from SearchField. Escape and Back clear the query first when it is not empty, then
  close (an exception to the SYSTEM-MAP order). Compact needs no extra close control; Back closes.
- **Stylus and touch.** Tap a result to run. The keyboard is raised only when the field is tapped, so the
  first list shows without it. Palm touches while the pen is in proximity never close it.
- **Mouse.** Hover highlights, click runs, click outside closes.
- **Keyboard.** Type to filter. Up and Down move the highlight (wrapping), Home and End, Enter runs and
  closes. Tab closes the palette and moves focus on (it is a light surface, not a trap); Ctrl+K again
  closes. Focus returns to where it was.
- **Placement.** Top centre. It does not follow the writing hand because it is centred, short lived and
  must stay predictable under Ctrl+K; the Menu and Popover mirror and this one does not.
- **Right to left.** The icon, shortcut and caption sides mirror.
- **Roles.** ARIA `dialog` (non-modal) containing a `combobox` and a `listbox`; group headings are `group`
  with a label. Compose `Popup` with `LazyColumn`, WinUI custom flyout with a `ListView`, SwiftUI custom
  overlay; native names are visual reference only. Announce: "Command palette", the true result count
  (polite, debounced), the highlighted result with caption, shortcut and reason.

## 10. Tokens

| Token | Use |
|---|---|
| `surface/overlay`, `border/subtle`, `elevation/popover`, `radius/surface` | surface |
| `size/palette/width`, `size/palette/max-height`, `size/row/menu`, `size/control/default` | size |
| `space/sm`, `space/md`, `space/lg`, `space/xl`, `radius/item` | padding |
| `text/default`, `text/muted`, `state/disabled/fg`, `type/body`, `type/label`, `type/caption` | text |
| `hover/fill`, `press/fill`, `component/focus/*`, `size/icon/default` | states and icons |
| `motion/duration/fast`, `z/palette` | timing and layer |

## 11. Do and Don't

Do
- List valid commands only, with the true count and a caption on every row.
- Show recents by recency and everything else by match then priority.
- Show a hint row for a command that exists in another context.

Don't
- Add a `>` mode, a second entry, or a silent cap on results.
- Run a command from another context, or rank by how often it is used.
- Put settings, notes or pack items in it.

## 12. Traces to

Flows 12 and 06 (entry).
