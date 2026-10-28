# Button

## 1. Purpose

A labelled control that runs one command: Create, Import, Install, Done, Delete forever. It is the
only control in the app with a visible verb on it.

**When not to use:**
- An action that is known by its icon alone (close, more, undo): use IconButton.
- A choice between two to three states of one setting: use SegmentedControl.
- A setting that turns on and off: use Toggle.
- A command inside a list of commands: use a Menu row.
- A filter that stays applied: use Chip.
- A link inside a sentence: a text link, not a Button.

## 2. Anatomy

```
 [ leading icon ]  [ label ]  [ trailing ]
 |<- padding ->|<-gap->|    |<-gap->|<- padding ->|
 +--------------------------------------------+  <- container: height role, radius/pill
 |   (+)  New notebook                         |
 +--------------------------------------------+
  hit slot: at least size/hit/min each way, container centred in it
  focus ring: gap then ring, outside the container
  busy layer: spinner centred over icon and label while loading
```

Parts: container, leading icon (optional), label, trailing (optional, `chevron` or `external`),
busy layer.

## 3. Variants and sizes

`variant`: `primary` | `secondary` | `text` | `danger`.
`command`: the command id the Button runs (required, see below).
`size`: `small` | `default` | `large`, which resolve to `size/control/small`, `default`, `large`.
`trailing`: `none` | `chevron` | `external`.
`width`: `content` | `fill`.

| Size | Height | Used for |
|---|---|---|
| `small` | `size/control/small` | Note bar and dock-adjacent buttons, rows of three or more siblings, inside a Banner |
| `default` | `size/control/default` | Everything else: Top bar, Sheet and Dialog feet, panels, forms |
| `large` | `size/control/large` | The one primary of a first run step or an EmptyState. Never in a bar, a Sheet foot or a Dialog foot |

| Variant | Meaning | Look |
|---|---|---|
| `primary` | The one action that finishes the surface (see the README rule) | `action/primary/*` |
| `secondary` | Any other action | `action/secondary/*` |
| `text` | Quiet or tertiary: Cancel in a Sheet, Skip, Sign in to restore, Install without permissions | `action/quiet/*`. In a foot it takes the foot's size and the foot's padding, so it is as tall as its neighbours; its container is invisible at rest |
| `danger` | Confirms an irreversible action, in a Dialog only | `state/danger/bg`, `state/danger/fg`, and a solid `border/error` edge at `border-width/emphasis`, so it outweighs the `secondary` Cancel beside it |

Sheet and Dialog feet use `default` throughout. Side padding: `space/md` small, `space/lg` default, `space/xl` large. Icon size is the role icon
size. Gap between icon and label is `space/sm`.

## 4. States

| State | Fill | Content | Other |
|---|---|---|---|
| Default | variant rest fill | variant fg | none |
| Hover | `primary`: `action/primary/bg-hover`; `secondary`: `action/secondary/bg-hover`; `text`: `action/quiet/bg-hover`; `danger`: `state/danger/bg-hover` | same | A pen that reports hover shows it; touch does not |
| Focus-visible | same as rest | same | Focus ring from the README |
| Active (pressed) | `action/primary/bg-pressed`, `action/secondary/bg-pressed`, `action/quiet/bg-pressed`; `danger`: `state/danger/bg-pressed` | same | No scale. Colour over `motion/duration/press` |
| Selected | n/a (a Button is not a toggle) | | Use Chip or SegmentedControl |
| Disabled | `primary`: `action/primary/bg-disabled`; `secondary`: `state/disabled/bg`; `text`: none; `danger`: `state/disabled/bg` | `action/primary/fg-disabled` or `state/disabled/fg` | Edge `state/disabled/border` on secondary and danger. Disabled stays in the focus order if it holds information. The reason is its accessible description and, on a tap, a visible reason hint (README) |
| Loading | rest fill | label hidden, spinner in label colour | Inline busy (README): spinner after 300 ms, width does not change. Optional `confirmation`: for Copy and Share a done check replaces the label for 1.5 s, same width |
| Empty | n/a | | A Button always has a label |
| Error | n/a | | Errors show inline beside the field or in a Banner, with Retry as a Button |
| Drag | n/a | | A Button is not draggable and is not a drop target |

Label colour: `primary` `text/on-action`; `secondary`
`action/secondary/fg`; `text` `action/quiet/fg`; `danger` `state/danger/fg`. Each sits only on the
hosts the contrast matrix allows.

## 5. Content rules

- Label: it is the command's `labelKey` from the registry, sentence case, a verb or verb and noun, `type/label`, one line when it fits. No full stops,
  no exclamation marks. Danger labels name the verb and the count: "Delete 12 forever".
- Very long text (German, Norwegian at 130 to 140%): the container grows in width up to its row.
  Buttons in a foot stack first (see overflow). Only when one full width Button still does not fit
  does the label wrap to two lines, and the container then uses `radius/container` so it does not
  bulge. A third line is cut with an end ellipsis and the full label stays in the accessible name.
  Never shrink the type.
- No text: not allowed. Use IconButton.
- One item: a single button sits at the end edge of its foot, `width` `content`; on compact width a
  lone primary may be `fill`.
- Five hundred items: n/a. A surface never holds more than three Buttons in one foot.

## 6. Overflow rule

A foot or row holds at most three controls, counting More: one primary, one secondary, one `text`.
More actions replace the `secondary` with a Menu trigger ("More", chevron). Buttons that do not fit
side by side stack vertically, primary first, each `width` `fill`; this happens before any label wraps. A bar never holds Buttons; it holds IconButtons.

## 7. Extension slots

The extension schema's `button` node maps to Button: its `quiet` style is `text`; its `danger` renders as `secondary` (danger is for the shell's own Dialog). Extensions contribute commands, and a command becomes a Menu row, a palette entry or an
IconButton (SYSTEM-MAP 7). An extension may not add a `primary` Button to a core surface. In its own
side panel an extension may use Buttons under the same one primary rule.

## 8. Pack content

None. If a pack's name appears in a label ("Remove Fruit stickers"), the name is data inside the
label, truncated in the middle at the wrap limit above, with the full text in the accessible name.

## 9. Input

- **Stylus:** tap activates on lift. Hover (S Pen, Pencil) shows the hover fill. Never reacts while
  the pen is writing on the page. Edge guard: a Button in the 16 dp edge guard on the writing hand
  side is inset, not shrunk.
- **Touch:** activates on lift inside the slot; sliding off cancels. Slot at least `size/hit/min`.
- **Mouse:** hover fill, click on release. Right click does nothing.
- **Keyboard:** Tab reaches it, Enter and Space activate (Space on key up, Enter on key down).
  Inside a Sheet and a non-destructive Dialog, Enter runs the primary when focus is not in a multi
  line field, and default focus is the primary. In a destructive Dialog default focus is Cancel, so
  Enter cancels. Escape belongs to the surface.

| Platform | Role |
|---|---|
| Web | `button` (a link that navigates is `link`) |
| Compose | `Role.Button` on a custom `Box` with `clickable`, not Material `Button` |
| WinUI | `Button` with a lightweight style, `AutomationProperties.Name` = label |
| SwiftUI | `Button` with a custom `ButtonStyle` |

A menu trigger also exposes `aria-haspopup="menu"` and expanded state.

**Command id.** A Button that runs an app action names its command id from `system/registries.md` and reads its label, enabled state and predicate from it: Create is `library.newNotebook`, Import `library.import`, Share `doc.share`, Done `studio.done`, Cancel `studio.cancel`, Restore and Delete forever `library.restore` and `library.deleteForever`, Skip and Next `onboarding.skip` and `onboarding.next`. A Button that only commits or dismisses its own surface (Cancel in a Sheet) is a surface action and needs no id. Ids not yet in the registry are listed in the README.

## 10. Tokens

| Token | Use |
|---|---|
| `size/control/small`, `default`, `large` | Height |
| `size/hit/min` | Hit slot |
| `radius/pill` | Container |
| `space/md`, `lg`, `xl`, `sm` | Padding and gap |
| `type/label` | Label |
| `size/icon/small`, `default`, `large`, `inline` | Leading icon by role; `inline` for trailing glyph |
| `action/primary/bg`, `bg-hover`, `bg-pressed`, `bg-disabled`, `fg-disabled` | Primary |
| `action/secondary/bg`, `bg-hover`, `bg-pressed`, `fg` | Secondary |
| `action/quiet/bg`, `bg-hover`, `bg-pressed`, `fg` | Text |
| `state/danger/bg`, `bg-hover`, `bg-pressed`, `fg` | Danger |
| `border/error`, `border-width/emphasis` | Danger edge |
| `radius/container` | Two line label |
| `state/disabled/bg`, `border`, `fg` | Disabled secondary, danger, text |
| `text/on-action` | Label on primary |
| `component/focus/*` | Focus ring |
| `motion/duration/press`, `fast`, `base`, `slow` | Colour, loading delay |
| `border/control` | Secondary edge in more contrast |
| `motion/duration/fast` | Loading minimum is `slow`; spinner delay is 300 ms (SYSTEM-MAP 4.6) |

## 11. Do and Don't

**Do**
- Put the primary at the end edge of the foot, with Cancel as `text` before it.
- Name the verb and the count on a danger button: "Delete 12 forever".
- Keep one size per row, and use `large` only for the one primary of a Sheet, Dialog or first run.
- Show loading by overlaying a spinner on the kept label, so the width is the same.

**Don't**
- Don't put two `primary` Buttons on one surface, or a `primary` beside a `danger`. A Banner action is never `primary`.
- Don't use colour for meaning outside `danger`. The accent is for the primary only, and never makes a
  secondary look important.
- Don't add a trailing icon other than a chevron or external arrow.
- Don't drop the label to fit; drop the icon first.
- Don't build hold to confirm. Irreversible actions use a Dialog.

## 12. Traces to

Flows: 01 first-run (Next, Skip, Sign in to restore), 07 customize-appearance (Done, Reset), 08
packs (Get, Remove), 09 extensions (Install, Install without permissions), 10 recover (Restore,
Delete 12 forever), 14 share-and-sync (Share, Turn on sync), 15 add-to-page (Take photo, Insert).

Replaces: Button (sizes `xs`, `sm`, `md`, `lg`, `bar` become `small`, `default`, `large`), HoldButton
(deleted).
