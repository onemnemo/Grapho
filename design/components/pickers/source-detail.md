# SourceDetail

## 1. Purpose

One detail page for a pack or an extension: who made it, its version and state, what it holds or adds,
and the actions on it. For a pack the contents are a Picker in manage mode; for an extension the
contents are the list of contributions and permissions. States and names follow
`content-system/lifecycle.md`.

**When not to use:**
- Listing all packs or extensions: the Packs and Extensions pages (ListRow lists).
- Reviewing an install before it happens: ImportReviewSheet (its consent view for extensions).
- Items of one type across sources: ContentPage.

## 2. Anatomy

```
< Back   Botanica                          (on)   [Share] [Export]  ...
------------------------------------------------------------------------
 [preview]  Ana Ruiz   1.2.0   Free to share, credit me
            Ferns, flowers and leaf borders.   Publisher a41c 09d2
            [Banner: Update to 1.3.0 available   Update | Skip this version]
            In use: 2 items
 Revert to 1.1.0 (28 days)       Advanced v   Id  Installed  Size  Publisher
 [Remove pack]                                   foot rows sit above the contents
 Stickers 120                                         Show all
   Picker (scope: this pack, manage)  ...
 Pens 6                                               Show all
   Picker (scope: this pack, manage)  ...
```

| Part | Notes |
|---|---|
| Header | Back, name, state Toggle, primary action by case, More Menu |
| Summary | Preview (art, 120 dp), author, version, licence, description, publisher short id |
| State Banner | One at a time by lifecycle: Update available, Failed with Try again, Incompatible, Blocked with the reason, Needs your OK (extension) |
| Contents | Pack: one section per content type the pack holds, each a Picker with `scope` set to this pack, `mode` `manage`, no source filter, a section heading (type and count) owned here, a Show all link, collapsible. The sections list the pack's items and Hidden items even when it is **Off**. Extension: "Can" permission lines each with a granted Toggle, "Adds to Grapho" list with per-contribution Toggles |
| Foot rows | Above the contents so a long list never buries them: Revert to previous version, In use, Advanced disclosure, Remove as a visible row. The page scrolls; each section's Picker grows with its page, virtualized against the page scroll |

## 3. Variants and sizes

`kind`: `pack` | `extension`. `source`: `installed` | `made-by-you` (pack) | `built-in`. One size. `built-in` (Grapho, 312 items) has no author line, no Toggle and no Remove.

## 4. States

| State | Behaviour and tokens |
|---|---|
| On | Toggle on; contents live |
| Off | Toggle off; summary and items shown quiet with `text/muted`; Banner none; "In use" line if a holder uses an item |
| Installing, Updating | ProgressItem `inline` under the summary with Cancel; Toggle disabled |
| Update available | Neutral Banner, dismissible; actions Update and Skip this version |
| Failed | Danger Banner with the reason and Try again; previous state untouched |
| Incompatible | Warn Banner "Needs Grapho 1.5 or newer"; no Toggle; Update the app |
| Blocked | Danger Banner "Grapho stopped this pack. Reason."; Toggle disabled with the reason; only Remove |
| In Trash | Banner "Removes in 12 days" with Restore and Delete now |
| Needs permission (extension) | Banner with Review; the permission lines marked New |
| Partial | Note "Some content is not supported in this version" with the list |
| Waiting to download | Summary only; contents say "Waiting to download" |
| Loading | Skeleton over the Picker after `feedback/delay/skeleton` |
| Error | Contents row with Retry; header stays |

One primary only: when a Banner with an action is shown its button is the primary and the header
actions are `secondary`; otherwise Share (made by you) or Export. Disabled controls keep their height and give the reason as description. One Banner at a time; the
highest priority wins (Blocked, Failed, Incompatible, Needs permission, Update).

## 5. Content rules

- Long names and descriptions: name one line with ellipsis and full text on focus; description up to
  four lines then "Show more". German and Norwegian grow by 40%.
- No preview: the type icon on `surface/sunken`.
- 1 item: shown. 500 items: the Picker is virtual and the type segment narrows. An unknown
  type: listed under "Not supported in this version" and left in the stored original.

## 6. Overflow rule

Header actions: Share, Export, then More; Remove never hides (it is a row). On compact the
header holds Back, name, Toggle and More. The page scrolls; the Picker is the last region.

## 7. Extension slots

None. An extension's contributions are listed, each with its own switch, but the page adds no slot.

## 8. Pack content

This page is where pack state is shown (SYSTEM-MAP 5 and 6). Names: **Off**, **Hidden** (an item
hidden, listed in the Picker's Hidden filter), **Kept from <pack>** (copies in Yours, listed under
In use), **Not installed** has no page here: it appears only on a received document's Banner and in item detail.
Linked items read "also in this pack". Delete is never offered on pack items.

## 9. Input

- **Stylus and touch:** tap a Toggle or action; the Picker follows its own rules.
- **Mouse and keyboard:** Tab order: Back, Toggle, actions, Banner, Picker, foot rows. Enter on a
  Toggle toggles. Turning a permission off lists the contributions it turns off first.
  F2 renames a pack made by you.
- **Roles:** page `main`; Toggle `switch`; Banner `status` (`alert` for danger); Advanced a
  disclosure `button` with `aria-expanded`. Compose, WinUI and SwiftUI: a navigation destination.

## 10. Tokens

| Token | Used for |
|---|---|
| `surface/base`, `surface/raised`, `surface/sunken` | page, groups, art well |
| `space/xl`, `space/lg`, `space/md`, `space/sm` | padding, gaps |
| `size/control/default`, `size/row/list`, `size/hit/touch`, `size/icon/large` | controls, rows, state icon |
| `radius/item`, `radius/surface` | art, Banner |
| `type/title`, `type/label`, `type/body`, `type/caption` | text |
| `text/default`, `text/muted` | text, quiet Off |
| `switch/track-off`, `switch/track-on`, `switch/thumb-off`, `switch/thumb-on` | Toggle |
| `state/danger/*`, `state/warning/*`, `state/info/*` | Banners |
| `state/disabled/fg`, `focus/*` | disabled, focus |

## 11. Do and Don't

Do:
- Show the author on every pack and extension.
- Keep Remove as a visible row, in words.

Don't:
- Offer Delete on pack items.
- Show a state with colour alone; every state has an icon and words.

## 12. Traces to

Flow 08 step 2, flow 09. Commands to register: `pack.update`, `pack.skipVersion`, `pack.revert`, `pack.share`, `pack.export`, `pack.remove`, `pack.restore`. Right to left: logical properties. Replaces PacksPage detail, ExtensionDetail, ExtensionsPage. Uses Picker,
Banner, Toggle, ProgressItem, SegmentedControl, Menu, Button.
