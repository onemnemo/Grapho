# ThemeEditor

## 1. Purpose

Build or change a custom theme on a page pushed inside Settings, Appearance: a name, up to four input
colours, advanced overrides, a live preview and the contrast readout. The theme data and the guard are
defined once in `themes/custom-themes.md`; this file only draws them.

**When not to use:**
- Choosing among themes: Picker (type theme).
- Editing one colour of a set: ColourEditor.
- Making covers, styles or presets: StudioShell.

## 2. Anatomy

```
< Back   Harbour
-------------------------------------------------------------------------------
 Preview (live, the real library and a note drawn with the draft)  Light | Dark
 Name        [Harbour________________]
 Background  [swatch] [#EAF0F4]     required
 Surface     [swatch] [#D5E0E8]
 Accent      [swatch] [#1F6F8B]
 Page        [swatch] [#FFFFFF]
 Mode        ( Auto | Light | Dark )
 Contrast    All text is easy to read.      or      "Text would be hard to read here" (2)
 Advanced v  text.subtle [#4A5560]   Add an override
                                          [Cancel]    [Save]
 [Use the default theme]                  app action, below the foot, not part of the draft
```

| Part | Notes |
|---|---|
| Preview | The theme preview renderer at page width, plus a real library and note frame; repaints on every input change |
| Name | TextField |
| Colour rows | Swatch part and hex TextField per input; `background` marked required |
| Mode | SegmentedControl `default`, Auto, Light, Dark (maps to `scheme`) |
| Contrast readout | A list of the guard's notes as plain sentences, each with `alert_circle` or `check` and words |
| Advanced | Disclosure of override rows (token, hex, Remove) |
| Foot | Cancel (`text`), Save (`primary`, `appearance.saveTheme`) |
| Default theme | A link row "Use the default theme" (`appearance.resetTheme`) below the foot. It switches the app to Ink, never touches the draft, and is also on the adjusted-theme Banner |
| Preview mode | A SegmentedControl `small` Light, Dark above the preview; it changes only the preview |

## 3. Variants and sizes

One variant. `layout`: `split` (expanded: preview beside the form, foot pinned under the form column) and `stacked` (preview above, kept visible while the form scrolls on medium; on compact it shrinks to a reduced height strip and stays visible, never behind a tap).
Controls role `default`.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Default | `surface/base` page; rows in the FieldShell frame |
| Hover, focus-visible, active | Each control's own; `focus/*` |
| Selected | Mode segment: `selection/fill` plus indicator |
| Disabled | Save disabled while the name is empty or a required input is invalid; reason as description |
| Loading | Skeleton over the preview only, after `feedback/delay/skeleton` |
| Empty | A new theme starts from the active theme's inputs, name "Theme 2" |
| Error | Invalid hex: `border/error`, `alert_circle`, "Use three or six digits after the hash sign" (the string shared with ColourEditor). `unreadable`: the readout says "These colours cannot be made readable. Try a lighter or darker background." and Save is disabled |
| Adjusted | The guard changed a value: readout "Text would be hard to read here" with what was changed; Save stores the originals plus the adjusted values |
| Dirty | Back asks the `unsaved` Dialog once |

The preview uses the draft's resolved tokens only. The editor's own chrome uses the active theme, so
the user can always read it. A draft is never applied to the app until Save; after Save the "Keep this
theme?" Banner appears.

## 5. Content rules

- Name: one line, 60 characters, grapheme counted, NFC; long names ellipsised in lists.
- No input but Background: valid, other inputs derived. All four set plus overrides: shown.
- 1 override to many: the Advanced list scrolls inside the page; 50 overrides is the cap of the data.
- Hex fields accept 3 or 6 digits for inputs and 8 for alpha overrides only.

## 6. Overflow rule

The page scrolls; the preview pins on `split`. Advanced collapses by default and remembers state per
device. The foot stays visible.

## 7. Extension slots

None.

## 8. Pack content

Edit on a built-in or pack theme works on a copy in Yours; the first change names it "Dusk copy".
A saved theme is Yours and can be exported as a pack. A pack theme the guard rejects is not
editable ("Theme unavailable"). No pack state appears in the editor.

## 9. Input

- **Stylus and touch:** tap a swatch to open ColourEditor `plain` as a pushed page (depth: Appearance, Theme Picker 1, ThemeEditor 2, ColourEditor 3);
  the hex field raises the keyboard by tap only. No handwriting-to-colour.
- **Mouse and keyboard:** Tab through rows; Enter in a hex field commits; Escape reverts that field;
  Ctrl or Cmd+S (`appearance.saveTheme`) saves when focus is in the page; Esc on the page asks to leave when dirty.
- **Roles:** the page `form` labelled by the title; readout `status`; preview `img` with a text
  alternative "Preview of Harbour". Compose `Scaffold`; WinUI `Page` in a `Frame`; SwiftUI
  `NavigationStack` destination.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/base`, `surface/raised`, `surface/sunken`, `border/subtle` | page, groups, preview well |
| `size/control/default`, `size/swatch`, `size/hit/touch` | controls, swatches |
| `space/xl`, `space/lg`, `space/md`, `space/sm` | page padding, gaps |
| `radius/item`, `radius/surface` | fields, preview |
| `type/heading`, `type/label`, `type/body`, `type/caption` | text |
| `text/default`, `border/error`, `state/warning/bg`, `state/warning/fg` (icon only) | readout words are `text/default` |
| `selection/fill`, `selection/indicator`, `state/disabled/fg`, `focus/*` | states |

## 11. Do and Don't

Do:
- Show the contrast result as words with an icon, never by colour alone.
- Keep Reset to default theme visible on the page.

Don't:
- Apply the draft to the app before Save.
- Let the accent colour touch focus, selection or status; the guard owns that.

## 12. Traces to

Flow 07 (build a theme). Commands: `appearance.saveTheme` (to register), `appearance.resetTheme` (exists). Right to left: rows mirror; the preview does not. Replaces the existing ThemeEditor. Uses TextField, SegmentedControl, Swatch,
ColourEditor (pushed), Dialog `unsaved`, Button.
