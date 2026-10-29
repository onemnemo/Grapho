# FolderTile

## 1. Purpose

A folder in the library grid: the folder style as art, a peek of what is inside, the name, a count.
It is a drop target for items and folders. It shares every part and rule of [ItemTile](item-tile.md)
(check slot, ••• button, selection, drag source, input) and differs in the art and in accepting drops.

**When not to use:**
- A document: use ItemTile.
- A folder in the sidebar or a chooser: use FolderTree.
- A folder in the list view: use [ListRow](list-row.md) `kind` `item`.
- Choosing a folder style: use [PickerTile](picker-tile.md) `cover`.

## 2. Anatomy

```
 +----------------------+
 |                      |
 |   folder style art   |   pack or default style, large, 3:4 like a cover
 |   with one peek      |   the most recent item's preview in the opening
 |                (/)   |   check badge (selecting, hover, focus, selected)
 +----------------------+
  Biology                     name: up to 2 lines
  12 items            [...]   count; ••• at the end
```

Parts: tile, folder art, one peek, art edge, check badge, name, count, ••• button (in the text
block, never on the art), drop layer.

## 3. Variants and sizes

One style. Same column count, tile width and art size as ItemTile (3:4, columns table there), so a
Folders group and an All group line up on one grid and no dead space sits above the art. The folder
style fills the art. The peek is one preview of the most recent item, so a folder costs one
thumbnail load, not three. An empty folder shows the empty folder art with no peek. The art edge
rule of ItemTile applies.

## 4. States

| State | Changes |
|---|---|
| Default, hover, focus-visible, active, selecting, selected | as ItemTile (same tokens): the check badge uses `media/check-bg`, `media/check-fg`, `media/halo`; the art ring uses `selection/media-ring` |
| Loading | art in `skeleton/fill` at art size; name and count shown when known |
| Peek missing or failed | the peek is left out; the folder art is unchanged; nothing is announced |
| Style missing | the default folder (registry placeholder); a pack style that is gone renders from the folder's own copy |
| Empty | empty folder art, count "Empty" |
| Drag source | `state/drag/source-opacity` |
| Drop target, valid, on enter | `state/drag/target-fill` behind the tile and a `state/drag/target-line` edge (`border-width/emphasis`, `radius/surface`); the ghost's count chip adds "Move to NAME" |
| Drop target, invalid | no highlight; the ghost shows the reason ("Cannot move a folder into itself", "Already in this folder") |
| Drop, done | highlight clears; a Toast with Undo from the host; the peek updates |
| Error | a failed move is the host's Toast with Retry; the tile is unchanged |

A folder being dragged is a normal drag source and is refused on itself and its own subfolders. A
drop highlights only on enter, and keyboard and menu equivalents are Move to (`library.moveTo`).

## 5. Content rules

- The name is the user's text: any script and length, two lines then an end ellipsis, `dir` auto,
  full name in the Tooltip and the accessible name. No name: "Untitled folder".
- Count: "1 item", "12 items", "Empty". It counts items and folders directly inside; a folder holding
  only folders says "3 folders". German and Norwegian counts wrap rather than cut the name. The name
  stays at two lines and is the full accessible name; item detail shows it whole.
- 1 folder, 500 folders: virtualised; the Folders group shows eight in Home with "Show all" in its
  GroupHeading.

## 6. Overflow rule

None. Extra actions are in the ••• Menu (rename, style, move, star, delete).

## 7. Extension slots

None; Menu rows from extensions only.

## 8. Pack content

A folder style comes from a pack, but the folder embeds its copy, so no pack state is shown on the
tile. See ItemTile section 8.

## 9. Input

As ItemTile, plus: **drag enter** on a mouse after 4 px of travel then pointer over the tile; **touch**
drag over the tile highlights after 120 ms of hover so a pass over does not flicker; dropping onto
the Sidebar's folder row is the other route. Keyboard: Enter opens the folder; no keyboard drag, so
Move to is the route. Screen reader: "Biology, folder, 12 items"; drop targets are announced as
"Move 3 items to Biology" while dragging. Role: `gridcell` with a `button` action (web), `ListViewItem`
(WinUI), Compose selectable, SwiftUI element. Commands: `library.open`, `library.moveTo`,
`library.itemMenu`.

## 10. Tokens

ItemTile's table, plus `state/drag/target-fill`, `state/drag/target-line`, `border-width/emphasis`,
`media/check-bg`, `media/check-fg`, `media/halo`, `border/strong`, `border/subtle`, `border-width/hairline`. The 3:4 ratio is `ratio/cover`.

## 11. Do and Don't

- Do highlight only a valid target, and only on enter.
- Do say why a drop is refused, in the ghost.
- Don't reorder folders by drag; they follow the place's sort.
- Don't show document-level pack state here.

## 12. Traces to

Flow 05. Kept separate from ItemTile because it previews contents and accepts drops.
