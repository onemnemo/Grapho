# Icons

One icon set, one grid, one stroke weight, on every platform.

## The set: Lucide

Grapho uses **Lucide** (ISC licence) for every line icon in chrome.

Why Lucide:

- **The licence is already on file.** `android/app/src/main/assets/licenses/lucide-ISC.txt` carries
  the ISC text and the MIT text for the icons Lucide inherited from Feather. ISC and MIT are
  permissive and fit a free, open source app with no field-of-use limits.
- **It matches the house style.** 24 by 24 grid, round caps and joins, outline only, `currentColor`.
  The existing design path set (`iconPaths.ts`, 187 names) is drawn on the same grid with round caps.
- **It is large and maintained.** Registries and extensions will keep asking for new icons, and a
  large set means most of them already exist in the same hand.
- **No weight variants.** Lucide has one drawing per name, so weights cannot be mixed.

Rejected: Phosphor and Tabler (permissive, but several weights or a different corner style invite
mixing, and no licence record exists), Material Symbols and Fluent (look like their platform, which
the brand rules call a beta look), SF Symbols (not licensed for use outside Apple platforms).

**Handoff item: provenance.** The design inventory records no declared source for the current path set of 187
icons, and this README has not checked it against Lucide. That check is not done. Before an icon ships in a
release it must be traced to a Lucide name and version, or declared a Grapho original (see "How to add an
icon"), and the licence file must cover everything shipped. Icons derived from Feather keep the MIT notice already in the licence file.

## Grid and stroke

| Property | Rule |
|---|---|
| Artboard | 24 by 24, no other viewBox |
| Live area | 20 by 20, 2 units of padding on every side |
| Stroke | Optical, by rendered size, in 24 grid units: 1.5 at 16 and 18, 1.75 at 20, 2 at 24 and 32 (`icon/stroke/px16` to `px32`). Every icon at a given size has the same stroke, so weights never mix on one screen |
| Caps and joins | Round |
| Fill | None. Colour comes from `currentColor` |
| Corners | At least 2 units radius where the shape allows, at least 2 units between parallel strokes |

The steps keep the rendered line between about 1.0 and 2.0 px at the sizes in use, so 16 does not blur to a grey
line on a 100% Windows display. The token is a number of grid units, never dp: a shell multiplies it by size over 24.
Snap the artboard to whole device pixels and check 16 and 18 at 100% scale on Windows before accepting an icon.

**State treatment is per icon, not universal.** Most tools (pen, eraser, lasso, ruler) are open paths where a fill
layer means nothing, so a toggle shows its state with the selection fill and `selection/indicator` of its
control. An icon with a distinct on form (star, pin, bookmark) declares it in the registry as `onState: fill`
or `onState: swap` with a second drawing, and that drawing is part of the icon's review.

## Sizes, tied to control roles

Icon size comes from the role of the control it sits in. Use the semantic size tokens, not numbers.

| Control role | Token | Touch | Compact |
|---|---|---|---|
| Small (36 touch, 28 compact) | `size/icon/small` | 18 | 16 |
| Default (44, 36) | `size/icon/default` | 20 | 18 |
| Large (52, 44) | `size/icon/large` | 24 | 20 |
| In a line of text | `size/icon/inline` | 16 | 16 |
| Empty state toolbar, unlabeled tile | `size/icon/feature` | 32 | 32 |

The icon is centred in the control and the control occupies a layout slot of at least `size/hit/min` (see
`tokens/README.md`), so a small icon never means a small target. Inline icons use the line height of the
style they sit in, at least 16.

## Icon and label

- Icon on the leading side, label after it, 8 apart (`space/sm`). A trailing icon is allowed only
  for disclosure (chevron), external links and a menu's submenu arrow.
- The icon takes the colour and opacity of the label, `currentColor`. The only coloured icons are
  status icons (`state/*/fg`). Selection changes the fill behind the control, never the icon colour.
- One icon per control. Do not put an icon and a count and a chevron in the same chip.
- In a menu, either every row has an icon or none does.
- Label type is `type/label` at every size. The label is never dropped to make room for the icon;
  the icon is.

## When icon-only is allowed

Icon-only is allowed when all of these hold:

1. The icon is one people already know (close, search, undo, add, more) or it is one of a tool row
   whose entries are learned together (the pen, eraser and lasso tools).
2. It has an accessible name, the same words as the tooltip: `contentDescription` on Android,
   `AutomationProperties.Name` on WinUI, `accessibilityLabel` on Apple, `aria-label` on the web.
3. It has a tooltip on pointer hover and keyboard focus, and on stylus hover where the platform
   reports it. On touch, a long press shows the same name.
4. Its hit area is at least 44 on coarse pointers.
5. A toggle shows its state with the selection fill and exposes it to assistive technology.

Icon-only is not allowed for: the single primary action of a sheet or dialog, a destructive action
outside an overflow menu, anything a first run screen introduces, or an icon whose meaning changes
with context. A bar holds at most four icon-only actions on its trailing side.

## The GraphoIcon seam

All shells draw icons through one function. Nothing else may reference a platform icon resource.

```
GraphoIcon(id: IconId, size: IconSize, tint: ColourToken, label: String? = null)
```

- `id` is a stable id from the icon registry (`arrow_left`, `add_page`), not a file name or a drawable.
- `size` is the role token (`small`, `default`, `large`, `inline`, `feature`), not a number.
- `tint` is a semantic colour token. Default is the text colour of the surroundings.
- `label` is required unless the call says the icon is decorative. The linter fails an undeclared
  icon with no label.
- Each shell holds a generated map from `id` to its platform asset (Android vector drawable, WinUI
  path geometry, Apple template image) produced from the one SVG source. Android's current
  `GraphoIcon(iconRes, ...)` takes a drawable; it moves to `IconId`.
- Extension and pack icons register under a namespaced id (`ext.<package>.<name>`), must follow the
  grid and stroke rules, and are validated when installed. A missing icon draws a neutral placeholder
  of the right size, never nothing.

## How to add an icon

1. Search Lucide for the concept. Prefer the Lucide drawing over a new one.
2. Name it by what it means in Grapho, in snake case (`delete`, not `trash_2`). One concept gets one
   icon and one icon gets one concept: do not reuse the bin to mean "remove from folder".
3. Copy the SVG into the icon source folder and normalise it: viewBox `0 0 24 24`, `stroke-width`
   from the stroke token for the size, round caps and joins, `stroke="currentColor"`, `fill="none"`, no ids, no classes.
4. If Lucide has nothing, draw it on the grid above in Lucide's manner and declare it a Grapho
   original.
5. Add a registry entry: id, source (`lucide:<name>@<version>` or `grapho`), search keywords, and
   its `onState` (`none`, `fill` or `swap`). A Feather-derived icon is listed in the licence file already.
6. Check it in the Icons story at 16, 18, 20 and 24 in every theme variant, and on a Windows 100% scale
   display at 16 and 18.
7. Run the generator for each shell. An icon that exists in one shell and not the others does not ship.

Directional icons (arrows, chevrons, undo and redo, back) mirror in right to left layouts. All other
icons never mirror.

## Pixel art

Pixel art is for brand moments only: the mascot, empty states, onboarding, and sticker and pack
covers. It is never used in tool chrome, controls, menus or settings rows, and it is not part of the
icon registry or the `GraphoIcon` seam. It is drawn at an integer scale **in device pixels** with nearest neighbour
sampling, never smoothed: the scale is the largest whole number of device pixels per art pixel that fits,
centred. The mascot's shimmer stops under reduced motion and shows its base colours. The pen wheel artwork is a drawing, not an icon, and stays outside this set.
