# ExtensionPanelHost

## 1. Purpose

The body of a Side panel tab that belongs to an extension. The shell owns the frame and the
extension's name; the extension supplies a declarative tree (`ui.render`), and the host draws it
with Grapho's own components. It also owns every state the tree can be in. Extensions get no
colours, fonts, sizes or positions (extension-api.md section 4).

**When not to use:**
- A built-in tab (Pages, Layers, Insert, Tool settings): those bodies are their own components.
- An extension's settings: its page in Settings, Extensions.
- A message from an extension to the user: the host's status line; extensions cannot raise Toasts,
  Banners outside this panel, Sheets or Dialogs.

## 2. Anatomy

```
+------------------------------------------+
| Flashcards                    (frame)    |  row 1: extension name (shell owned), provenance
| Installed by you                         |  row 2, sideloaded only
|------------------------------------------|
| status line (ui.status, one line)        |
| notice area (host Banner, when needed)   |
|                                          |
| tree body: built-in components           |
|   [button] [toggle] list/rows ...        |
|   drawing frame (at most half the panel) |
+------------------------------------------+
```

| Part | Notes |
|---|---|
| Frame | Name in `type/label` `text/muted` above the body, always shown, cannot be hidden or styled. Sits under the SidePanel title |
| Provenance | Sideloaded extensions show "Name, installed by you" in `type/caption` |
| Status line | `ui.status`, one line, `type/caption`, `text/muted`, truncates with a Tooltip |
| Notice area | A Banner, `placement` `region`, raised by the host only (never by a node) |
| Body | The rendered tree, scrolling inside the panel body |

## 3. Variants and sizes

`provenance`: `installed` | `sideloaded`. `state`: `ready` | `loading` | `error` | `over-quota` |
`off` | `needs-permission`.

Node mapping (the shell draws each node; the extension never sets a variant):

| Node | Component | Rule |
|---|---|---|
| `button`, `iconButton` | Button, IconButton | More than one `primary` demotes the extras to `secondary`; `danger` renders as `secondary`. `iconButton` requires `label`, used for its Tooltip |
| `toggle`, `segmented`, `checkbox`, `radioList`, `select`, `slider`, `stepper`, `field` | Toggle, SegmentedControl, Checkbox, RadioList, Select, Slider, Stepper, TextField or TextArea | `segmented` becomes a Select at runtime when labels do not fit |
| `list`, `row` | ListRow (rows family) | Until that family is built, the host draws a plain list of rows at `size/row/list` with the title, optional subtitle and up to three trailing controls |
| `image` | media component | `alt` required |
| `drawing` | drawing surface in a visible frame | At most half the panel height (in `peek`, half of the body), no text ops |

Limits: 500 nodes, depth 8, text 2000 characters. A tree over a limit is refused whole and the
host shows the error state; the previous good tree is not kept on screen.

## 4. States

| State | What the user sees |
|---|---|
| Ready | The tree. Updates diff in place, no animation while the pen is down |
| Loading | Skeleton rows after 300 ms, held 300 ms, in the real layout. The frame is already there |
| Error | An EmptyState `quiet` in the body: "Flashcards could not draw this panel." with Reload (`secondary` Button) and Details. Also for an over-limit tree and a handler that failed |
| Over quota | The extension was turned off for this session after strikes. A host Banner inside the panel: "Flashcards was turned off after problems." with Turn on again and Details. The body is empty; the tab then leaves the row as in `off` once the user moves away |
| Off | Only reachable by a deep link or a restored session. EmptyState `quiet`: "Flashcards is turned off." with Turn on (`secondary`). The tab itself disappears from the row (one rule, side-panel.md section 8) |
| Needs permission | A permission was revoked. EmptyState `quiet`: "Flashcards needs permission to read this document." with Review permissions, which opens its Settings page. Parts of the tree that do not need it stay live |
| Empty | The tree has no nodes: "Nothing to show yet." |
| Disabled control | A node with `enabled` false: `state/disabled/fg` |
| Hover, Focus-visible, Active | Per component |

Focus: when a state replaces the body, focus moves to the state's first button, or to the tab if it
has none. When the extension is turned off, focus goes to the Pages tab. Focus never moves while the
pen is writing.

## 5. Content rules

- **Very long text:** wraps; a node's text is capped at 2000 characters. German and Norwegian labels
  wrap or truncate with a full-text Tooltip as in the real component.
- **No tree:** Empty. **One node:** drawn as is. **500 nodes:** the host virtualises lists; depth is 8.
- The extension's name is never truncated in the frame; it wraps.

## 6. Overflow rule

None. The body scrolls inside the panel. A tab for the extension is in the SidePanel tab row, which
owns overflow.

## 7. Extension slots

This is the extension's slot (SYSTEM-MAP 7, side panel). It adds nothing else.

## 8. Pack content

None.

## 9. Input

As the components drawn. An `event` goes to the extension only through a node event, one at a time, with
the 50 ms UI event budget. Nothing runs while the extension is `off`. Stylus, touch, mouse and
keyboard follow the real components; Tab order follows the tree order.

| Platform | Role |
|---|---|
| Web | `region` labelled by the extension name inside the `tabpanel` |
| Compose | `paneTitle` with the name; real components inside |
| WinUI | `AutomationProperties.Name` on the host `ScrollViewer` |
| SwiftUI | `.accessibilityElement(children: .contain)` labelled with the name |

## 10. Tokens

| Token | Use |
|---|---|
| `surface/raised`, `border/subtle` | Host surface |
| `type/label`, `type/caption`, `text/default`, `text/muted` | Frame, status line |
| `space/md`, `space/lg`, `radius/item` | Gaps, padding |
| `skeleton/fill`, `state/disabled/fg` | Loading, disabled |
| `size/row/list`, `size/control/default` | Rows and controls |

## 11. Do and Don't

**Do**
- Keep the frame, the name and every state in the shell, so an extension cannot hide who it is.
- Show errors and quota as host states with Reload or Turn on again.
- Draw every node with the real component, with no extension styling.

**Don't**
- Don't let a node open a Sheet, Dialog, Banner or Toast, or draw outside the panel.
- Don't keep a stale tree on screen after an error.
- Don't show a disabled tab for an extension that is off.

## 12. Traces to

Flows: 09 extensions. Replaces: nothing; new (ExtensionPanelHost in COMPONENTS).
