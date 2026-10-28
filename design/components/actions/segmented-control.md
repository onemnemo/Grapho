# SegmentedControl

## 1. Purpose

A single choice among two or three short, always visible options, applied at once: Light, Dark,
System; Pages, Fit width; Original, Copy; Page, Selection.

**When not to use:**
- More than three options: use Select, or RadioList when each option needs a hint.
- A yes or no setting: use Toggle.
- Filters that combine: use Chip.
- Panels of content in a Sheet or Side panel: use Tabs.
- Choosing a look or a thing with a picture: use PickerTile in a Picker.

## 2. Anatomy

```
  +-------------------------------------------+   track: height role, radius/pill
  |  [ Light ]  [ Dark ]   System             |
  |  ^ thumb: selection/fill + indicator edge |
  +-------------------------------------------+
   segment: equal width, label (and optional icon), own hit slot
   dividers: none; the thumb and spacing separate segments
```

Parts: track, thumb, segment (label, optional leading icon), focus ring (on the segment in focus).

## 3. Variants and sizes

One style. `size`: `small` | `default`, resolving to `size/control/small` or `size/control/default`,
the outer height of the track. No `large`.

`content`: `label` | `icon`. A `label` segment may carry one leading icon. An `icon` control has
every segment as an icon with an accessible name and a Tooltip, and is allowed only for a learned
set (page layout, alignment).

Count: 2 or 3 options. Four is a lint error. All options in one control share the same `content`.
The setting that owns the control is named by its settings registry id (`appearance.mode`,
`sync.scope`); the control binds to that id, not to a command.

**Runtime fit.** The control measures its widest label at the current text scale, language and
available width. If the options do not fit on one line with `space/lg` padding, it renders as a
Select with the same options, the same label and the same value, at that moment and without a
build time switch. It switches back when room returns, never mid-gesture. Labels never truncate or
wrap inside a segmented control. A design check keeps labels short (about 12 characters in English),
but the rule above is what holds at 200% text, in a half width split and in every language.

Segments are equal width, set by the widest label, so the thumb does not change size when it moves.
Side padding is `space/lg` (default) or `space/md` (small). The thumb sits inside the track inset by
`space/xs`.

## 4. States

Per segment, unless the whole control is stated.

| State | Fill | Edge | Content |
|---|---|---|---|
| Default (unselected) | track `surface/control` | track `border/control` | `text/default` |
| Hover (unselected) | `hover/fill` over the track | track `border/control-hover` | `text/default` |
| Focus-visible | same | README focus ring around the segment | same |
| Active (pressed) | `press/fill` | same | same |
| Selected | thumb `selection/fill` (hover `selection/fill-hover`) | none; the indicator is a bar in `selection/indicator`, `border-width/emphasis` thick, along the thumb's lower inner edge | `text/default`, plus the icon if present |
| Disabled (whole) | `state/disabled/bg` | `state/disabled/border` | `state/disabled/fg` |
| Disabled (one segment) | track as rest | same | `state/disabled/fg`; the reason is its description, shown as the reason hint on a tap (README); it stays focusable |
| Loading | n/a. The old value shows until confirmed, then the thumb moves | | |
| Empty | n/a. Fewer than two options is not a SegmentedControl; render nothing or a plain label | | |
| Error | n/a. A required choice with none selected shows `border/error` on the track and a message below | | |
| Drag | The thumb can be dragged; it snaps to the nearest segment on release | | |

The thumb moves over `motion/duration/fast`, no overshoot. Under reduced motion it jumps. Selection
is never fill alone: the indicator bar is always drawn. Unselected labels use `text/default` so they
stay readable on `surface/control` and on hover fills.

## 5. Content rules

- Labels: one or two short words in sentence case, no icon-plus-label unless an icon is the learned
  mark for it. "Light", "Dark", "System".
- Very long text (German, Norwegian, plus 30 to 40%): if the widest label plus padding does not fit
  at the current scale and width, the runtime fit rule in section 3 renders a Select instead.
- No text: an `icon` control needs names for all segments.
- One item: not allowed; use a plain label. Two items is the minimum.
- Five hundred items: not allowed; use Select with Filter.

## 6. Overflow rule

More than three options is a Select or RadioList from the start. Labels that do not fit at runtime
become a Select (section 3). A registry that adds options moves the setting to Select from the
fourth option. A control never scrolls, collapses or truncates.

## 7. Extension slots

The extension schema has no segmented node today. Extension settings that need a choice use Select
or RadioList once the schema adds those nodes. An extension never adds a
segment to a built-in control.

## 8. Pack content

None. Packs do not appear as segments. If a setting's current value names a pack that is missing,
disabled or removed, the setting is a Select, and the control never shows a pack name.

## 9. Input

- **Stylus:** tap a segment to choose; drag the thumb (a tap is always the alternative to the drag). Hover shows `hover/fill` on the segment. Never
  reacts while the pen writes on the page.
- **Touch:** tap selects on lift; each segment's slot is at least `size/hit/min` high. A drag that starts on
  the thumb moves it; a swipe elsewhere scrolls the Sheet, so it never fights the back gesture.
- **Mouse:** click selects; hover fill.
- **Keyboard:** the control is one tab stop. Left and Right (Up and Down too) move focus and select
  (radio group behaviour), reversed in right to left. Home and End go to the first and last. Space
  re-selects the focused segment. Tab leaves the group.

| Platform | Role |
|---|---|
| Web | `radiogroup` with `radio` children and `aria-checked`; the group has an accessible name |
| Compose | `Role.RadioButton` on each segment, `selectableGroup()` on the row; custom, not Material segmented button |
| WinUI | One custom control: `RadioButtons` with a templated item (UIA role radio button, group, position and size in set); `SelectorBar` is not used |
| SwiftUI | One custom view of `Button`s inside `accessibilityRepresentation` of a `Picker`, so VoiceOver reads a picker; `PickerStyle` is not public API |

Each segment is read as "Dark, selected, 2 of 3".

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `size/control/default` | Track height |
| `size/hit/min` | Segment hit slot |
| `radius/pill` | Track and thumb |
| `space/lg`, `space/md`, `space/sm`, `space/xs` | Padding, gap, thumb inset |
| `type/label` | Label |
| `size/icon/small`, `size/icon/default` | Optional icon |
| `surface/control` | Track |
| `border/control`, `border/control-hover` | Track edge |
| `hover/fill`, `press/fill` | Unselected hover and press |
| `selection/fill`, `selection/fill-hover`, `selection/indicator` | Thumb |
| `border-width/emphasis` | Indicator bar |
| `state/disabled/bg`, `fg`, `border` | Disabled |
| `border/error` | Required, none chosen |
| `text/default` | Labels |
| `component/focus/*` | Focus ring |
| `motion/duration/fast`, `motion/easing/standard` | Thumb travel |

## 11. Do and Don't

**Do**
- Use it for two or three short options that apply at once.
- Draw the thumb with both the selection fill and the indicator bar.
- Use `size/control/small` when the control sits in a row with other small controls.

**Don't**
- Don't use it for four options, long options or a filter; use Select, RadioList or Chip.
- Don't truncate, wrap or shrink labels; let the control become a Select.
- Don't use icons in some segments and not others.
- Don't put a Done or Cancel on its choice; it applies at once.

## 12. Traces to

Flows: 04 pdf-import-annotate (Original or Copy; Page or Selection), 07 customize-appearance
(Light, Dark, System), 12 settings (density), 14 share-and-sync (export range).

Replaces: SegmentedControl (the Android private Segment too).
