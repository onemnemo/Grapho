# CompareView

## 1. Purpose

Show two versions of one notebook side by side as strips of page thumbnails, with the differing pages
marked, so the user can decide which to keep. It is the body of the conflict Sheet; the choice itself
(Keep both, Keep this device's, Keep the other) is a RadioList in the same Sheet.

**When not to use:**
- Browsing the pages of one document: SidePanel Pages tab.
- Picking a version to restore from history: a ListRow list in the same Sheet.
- Comparing text: not offered.

## 2. Anatomy

```
 This device, 14:02                 Tablet, 13:40
 +----+ +----+ +----+ +----+      +----+ +----+ +----+ +----+
 | 1  | | 2* | | 3  | | 4  |      | 1  | | 2* | | 3  | | 5+ |
 +----+ +----+ +----+ +----+      +----+ +----+ +----+ +----+
 * changed   + only here                      4 pages differ
```

| Part | Notes |
|---|---|
| Column heading | Device name, time and a short version id ("Tablet, 13:40, 4c1f"), `type/label`, so two devices of one name differ |
| Strip | Virtualized page thumbnails (PickerTile, page thumbnail kind), page numbers beneath |
| Mark | A round badge over the art on `media/glass` with `media/text`: `edit` for a changed page, `plus` for a page only on one side. Always icon plus word in the legend; `state/*/fg` is never drawn on art |
| Other changes | A list under the strips for differences a thumbnail cannot show: title, cover, folder, layers, annotations |
| Legend | "Changed", "Only here", and the summary count, a live status |

## 3. Variants and sizes

`layout`: `side-by-side` (expanded and medium) | `single` (compact: one strip with each page showing both versions as a left and right pair, and a SegmentedControl `small` "Differences only" | "All"). Thumbnails use `size/preview/medium`.

## 4. States

| State | Behaviour and tokens |
|---|---|
| Default | `surface/overlay` Sheet body; thumbnails on `canvas/page` |
| Hover | `hover/fill` on a thumbnail; tooltip "Page 2, changed" |
| Focus-visible | `focus/*` inset on one thumbnail |
| Selected | A thumbnail opened in a larger view: `selection/media-ring` and `-halo` |
| Disabled | Not used |
| Loading | Skeleton thumbnails after `feedback/delay/skeleton`; the choice RadioList stays usable |
| Empty | Pages and Other changes both empty: "These versions are the same." and the Sheet offers only Keep this one |
| Error | A strip that fails to load shows "Could not load Tablet's pages" with Retry; the choice can still be made |
| Scroll linked | The two strips scroll together; the focused page number stays aligned |

## 5. Content rules

- Device names: one line with ellipsis, full in Tooltip; times in the locale.
- 1 page: one thumbnail per side. 9 pages: all visible. 500 pages: virtualized and linked; a "Next
  difference" and "Previous difference" pair of IconButtons jumps among marked pages.
- A deleted-on-one-side page leaves a dashed empty slot so the strips stay aligned.

## 6. Overflow rule

Strips scroll along their long axis together. Pages beyond the window are not created. Nothing
collapses into a count; marked pages are always reachable by Next difference.

## 7. Extension slots

None.

## 8. Pack content

None. A page shows the content it has, drawn from the embedded copies; a missing pack never changes a
thumbnail.

## 9. Input

- **Stylus and touch:** tap a thumbnail to open it larger in a pushed view (read only; the pen does
  not draw here); drag scrolls both strips.
- **Mouse and keyboard:** arrows move through thumbnails (mirrored in right to left, as does the column order); Home and End; `compare.next` (F8) and `compare.previous` (Shift+F8) jump between differences; Enter opens the larger view; Escape returns.
- **Roles:** each strip a `list` with `listitem`s labelled "Page 2, changed"; the legend `status`.
  Compose `LazyRow`; WinUI `ItemsRepeater`; SwiftUI `LazyHStack`.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/overlay`, `surface/sunken`, `canvas/page` | body, strip well, page |
| `space/md`, `space/sm` | gaps |
| `size/preview/medium` (to add), `size/hit/touch`, `size/control/small` | thumbnails, targets, buttons |
| `radius/item` | thumbnail |
| `type/label`, `type/caption` | headings, numbers |
| `media/glass`, `media/text` | marks over art |
| `selection/media-ring`, `selection/media-halo`, `hover/fill` | selected, hover |
| `skeleton/fill`, `feedback/delay/skeleton`, `focus/*` | loading, focus |

## 11. Do and Don't

Do:
- Pair every mark with an icon and a word in the legend.
- Keep the strips aligned even where a page is missing.

Don't:
- Let the pen draw in the view.
- Hide the choice while thumbnails load.

## 12. Traces to

Flow 14 (conflict). New. Belongs with the sheets or sync family (it is a sheet body, not a picker); kept here until moved. Commands to register: `compare.next`, `compare.previous`. Uses PickerTile, RadioList (in the Sheet), Tabs (compact), IconButton, Skeleton.
