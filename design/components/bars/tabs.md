# Tabs

## 1. Purpose

Switch between two to four panels of content that share one subject, usually under a preview:
the Studio controls, and Compare. It is the same component the SYSTEM-MAP calls tabs in a Sheet.

**When not to use:**
- A setting value (a few short options): SegmentedControl. The boundary is one: Tabs choose a panel of content, SegmentedControl chooses a value, in a Sheet as everywhere.
- The side panel's panels: the SidePanel TabRail, an icon rail.
- Library places: Sidebar. Filtering by source: the Picker source filter.
- More than five panels: a list on the start side replaces the tabs (SYSTEM-MAP 8).

## 2. Anatomy

```
[ Studio ][ Colours ][ Layout ]       tab row, one line
-------------------------------      divider, border/subtle
 panel for the selected tab
```

| Part | Notes |
|---|---|
| Tab | A text target `size/control/default` high, `type/label`, centred. An optional leading icon `size/icon/default` |
| Indicator | A short bar of `border-width/emphasis` in `selection/indicator` under the selected label |
| Row | Equal width tabs from the widest label, filling the width, on `surface/raised` |
| Panel | The selected tab's content, owned by the host |

## 3. Variants and sizes

None. One style, one size (`default`). `count`: 2 to 4 tabs (5 allowed only in a Sheet, at 2x). Six or more becomes a list on the start side. Labels are
text, never icon only.

## 4. States

| State | Fill | Content | Edge |
|---|---|---|---|
| Rest | none | `action/quiet/fg` | |
| Hover | `action/quiet/bg-hover` | same | |
| Focus-visible | same | same | README focus ring, inset form |
| Active | `action/quiet/bg-pressed` | same | |
| Selected | `selection/fill`, hover `selection/fill-hover` | `text/default` | the indicator bar |
| Disabled | none | `state/disabled/fg` | reason as description |
| Loading, Empty, Error, Drag | n/a. The panel owns them | | |

Selection is the tint plus the bar, never fill alone. Switching tabs changes content at once, with a 100 ms cross fade; under reduced motion it is instant. Nothing slides.

## 5. Content rules

- **Very long label:** never truncated. The row is measured at runtime, not at build time. If the
  labels do not each fit on one line at the available width (German, Norwegian, 200 percent text),
  Tabs becomes a Select with the same options. This is the SegmentedControl rule.
- **No text:** not allowed; every tab has a label.
- **One tab:** not a Tabs; show the content. **Six or more:** a list on the start side (SYSTEM-MAP 8).
- Label sentence case, one or two words ("Studio", "Compare").

## 6. Overflow rule

Max four tabs by default, five in a Sheet. No scrolling row, no truncation. When labels do not fit,
the Select form replaces it, state kept (selected tab, focus, scroll of the panel).

## 7. Extension slots

None.

## 8. Pack content

None.

## 9. Input

- **Stylus and touch:** tap selects. A long press on a label does nothing special. Targets
  `size/hit/touch` high.
- **Mouse:** click, hover fill.
- **Keyboard:** one tab stop for the row; Left and Right move and select (automatic activation);
  Home and End jump; Tab enters the panel. Right to left reverses Left and Right.
- **Handedness:** none.

| Platform | Role |
|---|---|
| Web | `tablist`, `tab` with `aria-selected`, "2 of 3", `tabpanel` |
| Compose | `Row` with `selectableGroup` and `Role.Tab` items; the panel in a `Box` |
| WinUI | `TabView` (closeable off) or a `SelectorBar` |
| SwiftUI | Custom buttons with the `isSelected` trait and a tab-bar container trait |

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/default`, `size/hit/touch`, `size/icon/default` | Tab height, target, icon |
| `surface/raised`, `border/subtle` | Row and divider |
| `type/label`, `space/lg`, `radius/item` | Text, side padding, hover shape |
| `action/quiet/fg`, `bg-hover`, `bg-pressed`, `state/disabled/fg` | Tabs |
| `selection/fill`, `-hover`, `selection/indicator`, `border-width/emphasis` | Selected |
| `motion/duration/fast`, `component/focus/*` | Colour change, focus |

## 11. Do and Don't

**Do**
- Use two to four short text tabs under a preview or at the top of a panel.
- Measure at runtime and fall back to a Select when labels do not fit.
- Keep the selected tab and the panel's scroll when the form changes.

**Don't**
- Don't use Tabs to set a value; that is a SegmentedControl.
- Don't nest Tabs, or put them in the Side panel (its rail is the tabs).
- Don't scroll the row or truncate a label.
- Don't use icon-only tabs.

## 12. Traces to

Flows: 07 customize-appearance (Studio), 14 share-and-sync (Compare). Replaces: nothing; new
(the existing `Tabs` in `src/components` is the starting point).
