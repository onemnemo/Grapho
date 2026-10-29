# Sheets and dialogs

The modal layer. There is at most one: a Sheet, or a Dialog. A Sheet may have one Dialog above it for
its own confirm. A Sheet never opens a Sheet; it pushes a view (three levels below its root, each
with Back in its header).

## Members

| Component | File | Variants | Sizes | Flows |
|---|---|---|---|---|
| Sheet | [sheet.md](sheet.md), recipes in [sheet-recipes.md](sheet-recipes.md) | recipes: Move, Remove, Export, Pack builder, Catalogue; first run | `default`, `wide` | 01, 02, 04, 05, 07, 08, 09, 13, 14 |
| Dialog | [dialog.md](dialog.md) | `irreversible`, `unsaved` | one | 02, 07, 09, 10 |
| NewItemSheet | [new-item-sheet.md](new-item-sheet.md) | kind `notebook`, `canvas`, `folder`; mode `create`, `edit` | Sheet `wide` | 01, 02, 07 |
| ImportReviewSheet | [import-review-sheet.md](import-review-sheet.md) | `pdf`, `notebook`, `image`, `pack`, `extension` (the consent view) | Sheet `default`, `wide` | 04, 08, 09, 13, 14 |

## How they line up

| Property | Sheet | Dialog |
|---|---|---|
| Placement | bottom on compact, centred on medium and expanded | centred on every class |
| Width | compact: the window; else the smaller of the width token and the window minus two `layout/gutter/*` | same clamp with `size/dialog/width` |
| Surface, elevation | `surface/overlay`, `elevation/sheet`; edge `border/strong` under more contrast | same |
| Scrim, layer | `scrim/default`, `z/sheet` | `scrim/default` or `scrim/stacked` over a Sheet, `z/dialog` |
| Radius | `radius/container` (top corners on compact) | `radius/container` |
| Header | `size/sheet/header`, `type/heading` | none; title in the body |
| Padding | `space/lg` header and foot, `space/xl` body | `space/xl` |
| Foot | `size/control/default`: Cancel `text`, one primary, one secondary text button at most | safe button, action button |
| Stacked foot | primary on top and first in focus order | action on top and first in focus order |
| Default focus | first empty field, else first body control; consent: Cancel | the safe button |
| Escape | Cancel (the `unsaved` Dialog first if dirty) | Cancel; for `unsaved`, Keep editing |
| Scrim tap | only if clean and finger sized; palm never; first run never | never |
| Role | `dialog` | `alertdialog` |
| Motion | `motion/duration/pop` in, `exit` out | same |
| Overlay host | one shell host, Dialog drawn inside it | same |

## Rules that hold for the family

- A list, grid, steps or a field makes it a Sheet. A Dialog has a message and two buttons.
- Irreversible (empty Trash, delete forever, erase an extension's data): Dialog, danger button
  labelled with verb and count, default focus Cancel. No hold to confirm.
- One primary per surface. Enter and Ctrl or Cmd+Enter never run the primary in a consent or danger flow.
- A dirty Sheet asks once ("Discard changes?", default Keep editing). A scrim tap on a dirty Sheet
  pulses Cancel and announces it.
- At compact with the keyboard up, or at 200% text: full height, one line header, foot keeps only the
  primary, Cancel becomes Close, the body keeps a third of the visible height.
- A Sheet or Dialog over a document takes all input; it never opens while the pen is down.
- Refused and failed import items are never collapsed.

## Commands to register

| Id | Label | Context | Used by |
|---|---|---|---|
| `sheet.close` | Close | `sheet` | Close, Escape |
| `sheet.back` | Back | `sheet` | pushed view Back |
| `sheet.confirm` | the primary of the open Sheet | `sheet` | Enter; Ctrl or Cmd+Enter in a multi line field |
| `sheet.discard` | Discard changes | `dialog` | Dialog `unsaved` |
| `library.deleteForever` | Delete forever | `dialog` | Dialog `irreversible` |
| `library.emptyTrash` | Empty Trash | `dialog` | Dialog `irreversible` |

Ctrl or Cmd+N opens NewItemSheet kind notebook (existing `library.newNotebook`); the other kinds
keep their own commands.

## Tokens to add

| Token | Role | Proposed value |
|---|---|---|
| `size/sheet/width/default` | centred Sheet width | derived from a new primitive 560 |
| `size/sheet/width/wide` | grid body width | derived from a new primitive 760 |
| `size/sheet/list` | start-side list width in a Sheet with more than five sections | derived from a new primitive 200 |
| `size/dialog/width` | Dialog width | derived from a new primitive 400 |
| `border/strong` on `surface/overlay` | container edge under more contrast | pair to be declared per theme |

## Decisions to log

1. Four components; Move, Remove, Export, Pack builder and Catalogue are recipes of Sheet.
2. The `unsaved` Dialog makes Keep editing the primary; Escape there is Keep editing.
3. Dialog is always centred, never a bottom sheet, and has no field.
4. A tab row allows five; more becomes a start-side list.
5. ImportReviewSheet kind `extension` is the consent view; no separate Review permissions step.
6. Refused and failed import items are always listed; imports are all or nothing per file.
7. Sheet size belongs to the Sheet; a pushed view never resizes it.
