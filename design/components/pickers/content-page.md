# ContentPage

## 1. Purpose

The Settings page for one content type, opened in manage mode: the registry's type name as the title
and the Picker below it. It adds only what a page needs around the Picker: the page header, type
counts and the route to Packs. It is the host, not another list.

**When not to use:**
- Picking for a target: Picker in its own host.
- Managing a pack's items: SourceDetail.
- A type's one-off options (default pen width): SettingRow in Settings.

## 2. Anatomy

```
Settings > Content     Pens                                  [Manage packs]
-----------------------------------------------------------------------------
 Picker (type: pen, placement: page, mode: manage) with its header, source filter,
 items, SelectionBar and footer: Get more, Import, New
```

| Part | Notes |
|---|---|
| Page header | Title is the type's plural label (the Picker has no title of its own); one link Button `text` "Manage packs" (`picker.managePacks`) to Settings, Packs |
| Picker | `placement` `page`, `mode` `manage`, footer kept (Manage slot absent) |
| Type index | On expanded, the Sidebar `settings` kind lists the eight types; with more types the Content entry becomes a list with Filter |

## 3. Variants and sizes

One variant per registry type; no other props. Width follows the settings content measure;
items use the type's registry preview size.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Default, hover, focus-visible, active, selected | The Picker's own |
| Disabled | A type with `editable` none hides New; nothing is greyed |
| Loading | Picker Skeleton; the page header is live |
| Empty | The Picker's empty states, with the type's New and Import |
| Error | The Picker error row; the header stays |
| Offline | The Picker rule: Get more opens its empty state |
| Pack Off while open | The Picker rule |

## 5. Content rules

- Title: the type label key, German and Norwegian longer by up to 40%; wraps to two lines at 200%
  text, never truncates.
- 0 items: the empty state; 1: one tile; 9: one screen; 500 and 5000: virtual grid, Filter, source
  counts (the Picker file).
- Seven Content pages plus a Themes link row that opens Appearance (themes are picked and managed there).

## 6. Overflow rule

The page scrolls with the Picker as the only scroll region on expanded; on compact the page header
scrolls away and the Picker header pins. Manage packs moves to the page More when width is short.

## 7. Extension slots

None here. Types come from the registry; an extension cannot add a Content page.

## 8. Pack content

The source filter inside carries the packs. Hide is offered for built-in and pack items, Delete for
Yours, Export as pack for all. Items from an **Off** pack are not listed here (they are in the pack's own page); "Manage packs"
shows the pack as Off. **Kept from** copies sit in Yours with their name alone.

## 9. Input

As the Picker. Extra: Back returns to the Settings index; Escape in manage mode with a selection
clears it, then leaves the page. F6 cycles page header, Picker, Settings sidebar.

Roles: page `main` with a `heading`; the Picker's roles inside. Compose, WinUI and SwiftUI: a
navigation destination with a large title.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/base`, `surface/raised` | page, Picker |
| `space/xl`, `space/lg`, `space/md` | page padding, gaps |
| `type/title`, `type/label` | page title, link |
| `action/quiet/fg`, `text/default` | link, text |
| `layout/gutter/*`, `focus/*` | gutters, focus |

## 11. Do and Don't

Do:
- Open every type in manage mode with the footer kept.
- Let the Picker own states and counts.

Don't:
- Add per-type controls to the page header.
- List packs here; that is Settings, Packs.

## 12. Traces to

Flow 07. Right to left: logical properties. Replaces ContentPage, ContentTile, ContentArt (tiles are PickerTile). Hosts Picker, uses
Sidebar `settings`, SettingRow, Button.
