# Extension API: `grapho-script 1`

The contract between an extension and the core. Shells never see script; they draw the declarative
UI tree natively. Conformance is checked by test vectors the core ships (call, expected result), so
two engines or two shells cannot drift. The engine is an engineering seam; this contract is not.

## 1. Language

- Standard JavaScript, an ES2020 subset, run in a core-owned sandboxed engine.
- Removed: `eval`, `Function` constructor, dynamic `import()`, `with`, `Proxy`, `Reflect`,
  `WeakRef`, `SharedArrayBuffer`, `Atomics`, `setTimeout` and `setInterval` (use `host.after`), and
  every host global (no `window`, no `fetch`, no DOM). Static `import` loads only files in the
  archive.
- Present: the ES2020 standard library, `JSON`, `Math`, `Date` (read only, from `host.now()`),
  `Intl` for number and date formats, `TextEncoder`, `TextDecoder`, and typed arrays.
- The only way out of the engine is a capability object, below.

## 2. Execution model

- Each extension runs in its own engine instance, on a thread or process that is not the input,
  ink or render thread. A call never blocks drawing.
- **No extension code runs on the pen-down path.** Canvas tools get the finished gesture at pen-up
  (the path with pressure and timestamps) and return commands. Live previews are described as data
  returned once at pen-down (a stroke style), never computed per sample.
- Handlers run one at a time per extension, in order. A handler may `await` host calls.
- Every handler has a budget (section 8). Exceeding it aborts the handler with `E_QUOTA_CPU`.
- Extension code cannot see another extension, the pack store or the permission table.

## 3. Capability objects

Present only when the permission is granted; otherwise the name is `undefined` and any attempt
via an event payload is stripped. Every method can throw an error object `{code, message}`.

| Object | Permission | Methods (summary) |
|---|---|---|
| `host` | none | `id`, `version`, `api` (integer), `locale`, `now()`, `after(ms, fn)` (max 60 s, not repeating faster than 1 s), `on(event, fn)`, `log(text)`, `register(slot, id, handlers)` |
| `ui` | none (own panels and blocks only) | `render(tree)`, `status(text)` (one line inside its own panel), `ask(kind)` for the system pickers listed in `files` |
| `storage` | `storage.own` | `get(key)`, `set(key, json)`, `remove(key)`, `list(prefix)`, `blob(name)` read and write bytes; quota 100 MB |
| `doc` | `doc.read` | `info()`, `pages()`, `page(i).strokes()`, `page(i).images()`, `page(i).text()`, `blocks()` (only its own) |
| `doc` | `doc.write` | `apply(ops)` one undo step labelled with the extension name; `embed(source)` returns an item hash (validated, written with `source: extension`); `insertBlock(type, data)` |
| `selection` | `selection.read` | `strokes()`, `images()`, `bounds()` of the current selection only |
| `library` | `library.read` | `documents(filter)` returns ids, titles, dates; `folders()`; `content(type)` lists items (name, hash, source) |
| `library` | `library.write` | `addItem(type, item)` (to Yours), `rename(id, name)`, `move(id, folder)`, `createDocument(spec)`, `trash(id)`. No permanent delete, no access to packs, extensions or permissions |
| `net` | `network` | `fetch(url, {method, headers, body})`, https, port 443, host exactly as declared, same host redirects only, no IP literals, no cookies, 1 MB request, 5 MB response, 60 requests per minute |
| `clipboard` | `clipboard.read` or `.write` | `read()` only inside a user started command; `write(text or png)` |
| `files` | `files.pick` | `pick({types})` opens the system chooser and returns a handle; `read(handle)`; `save(name, bytes)` opens the system save dialog |
| `media` | `microphone` or `camera` | `record()` returns a blob; the app shows a non-hideable recording indicator |

Draw and document operations (`ops`) are a closed list: `addStroke`, `addShape`, `addText`,
`addImage` (by item hash), `move`, `recolour`, `delete`, `setPageBackground`. Each op is validated;
one bad op fails the batch and nothing is applied.

## 4. UI schema (declarative JSON tree)

Panels, settings pages and block editors are trees of the standard control set only. The extension
sends a whole tree with `ui.render`; the shell diffs it. No colours, fonts, sizes or free positioning
exist in the schema: the shell styles every node from tokens.

Node: `{type, id, ...props, children?}`. Limits: 500 nodes, depth 8, text 2000 characters, ids
unique within a tree and at most 64 characters.

| type | Props | Events |
|---|---|---|
| `list` | `items`: list of `row`; `selectable`: none, one, many; `empty`: text | `select(ids)` |
| `row` | `title` (text), `subtitle` (text, optional), `icon` (archive path or built-in icon name), `children` (up to 3 of `button`, `toggle`) | `tap` |
| `button` | `label`, `icon`, `style`: primary, secondary, text, danger; `enabled` | `press` |
| `iconButton` | `icon` (built-in icon name), `label` (required, used for the tooltip and accessible name), `enabled` | `press` |
| `toggle` | `label`, `value` | `change(value)` |
| `checkbox` | `label`, `value`: unchecked, checked, mixed | `change(value)` |
| `radioList` | `label`, `options` (2 to 12, each `{id, label, description?}`), `value` | `change(id)` |
| `segmented` | `options` (2 or 3, each `{id, label}`), `value` | `change(id)`. Falls back to a select at runtime when labels do not fit, like the built-in control |
| `select` | `label`, `options` (up to 200), `value` | `change(id)` |
| `slider` | `label`, `min`, `max`, `step`, `value` | `change(value)`, `commit(value)` |
| `stepper` | `label`, `min`, `max`, `step`, `value` | `change(value)` |
| `field` | `label`, `value`, `kind`: text, number, multiline; `maxLength` | `change(value)`, `commit(value)`. There is no password kind |
| `image` | `source`: archive path or item hash only (never a URL), `alt` (required), `fit` | `tap` |
| `drawing` | `ratio`, `ops` (vector drawing, section 6), `alt` (required) | `tap(x, y)` |

Each node is rendered by the matching built-in component, with no extension-specific styling:
`button` and `iconButton` by Button and IconButton (`components/actions/`); `toggle` and
`segmented` by Toggle and SegmentedControl; `checkbox`, `radioList`, `select`, `slider`,
`stepper` and `field` by the components of the same name in `components/inputs/` (`field` is
TextField or TextArea); `list` and `row` by the rows family; `image` and `drawing` by the media
components.

Rules enforced by the shell:
- More than one `primary` button in a tree: the extras are drawn as `secondary`.
- A control with an icon and no label must have `alt` or `label`.
- A `drawing` in a panel is drawn inside a visible content frame, at most half the panel height,
  and without text ops. Text ops exist only in block drawings.
- The panel frame, with the extension name, belongs to the shell and cannot be styled or hidden.
- No node can open a Sheet, Dialog, Banner or Toast. `ui.status` is one line inside the panel.

## 5. Lifecycle events

Delivered only when the extension is On and holds the needed permission. Each handler is optional.

| Event | When | Needs |
|---|---|---|
| `start` | app launch, or the extension turned on | none |
| `stop` | turned off, removed, or app quit; 1 s to save state | none |
| `install` | first time installed | none |
| `update(oldVersion)` | after an update; runs migrations against a staged copy of storage; failure leaves the old storage and the old version | none |
| `command(id, args)` | user runs a command | per call |
| `gesture(path)` | canvas tool, at pen-up | none to receive; `doc.write` to apply |
| `documentOpen`, `documentClose` | a document opens or closes | `doc.read` |
| `selectionChange` | selection changes (debounced to 100 ms) | `selection.read` |
| `block.render`, `block.describe`, `block.edit` | see section 6 | none |
| `permissionChange` | a permission is granted or revoked | none |

## 6. Block render contract

A block type is declared in the manifest and implemented with three functions:

- `render(data, size) -> drawing`: a pure function (no host calls, no async, deterministic), 20 ms
  budget. A drawing is a list of ops: `path`, `rect`, `ellipse`, `line`, `group`, `clip`,
  `transform`, `text` (built-in families only), with solid or gradient fills and strokes in sRGB.
  Limits: 5000 ops, 256 KB of path data. The shell draws it natively; results are cached by
  `hash(data, size, renderer version)`.
- `describe(data) -> text`: plain text, up to 1000 characters.
- `edit(data) -> tree`: the editor as a UI tree in the block's frame or the side panel.

The **fallback** is made by the core, not by the extension: after each committed change (pen-up,
blur or 1 s without change) the core rasterises `render` at 1024 px on the long side, 512 KB at
most, stores it as the block's fallback with `describe` text, and replaces the previous fallback
(never accumulating). Block `data` must pass the manifest `dataSchema` and be at most 1 MB; any
asset reference is `{"$asset": hash}` of an item in the document. `data` carries `{v, minReader}`;
a reader that does not meet `minReader` shows the fallback with "Needs a newer version of NAME".

## 7. Errors

| Code | Cause | User effect |
|---|---|---|
| `E_PERMISSION` | call without the grant | none; logged |
| `E_ARG` | bad argument or op | none; logged |
| `E_UNAVAILABLE` | method unknown for this `api` | none; logged |
| `E_NET_HOST` | host not declared, redirect off host, IP literal | none; logged |
| `E_QUOTA_CPU`, `E_QUOTA_MEM` | budget exceeded | a strike (section 8) |
| `E_QUOTA_STORAGE` | storage or block quota | write refused; extension page shows "Storage full" with Clear data |
| `E_BLOCK_DATA` | data fails schema, names a non-embedded asset, or is over 1 MB | write refused |
| `E_CANCELLED` | user cancelled a system picker | handled by the script |
| `E_CRASH` | engine fault or out of memory | a strike |

Errors are thrown to the script as `{code, message}`. Unhandled errors end the handler. The last 20
events per extension are kept in a ring buffer shown in the extension detail ("What happened",
copyable). No stack traces reach the user. `host.api` is an integer; additions are append only and an
unknown method is `E_UNAVAILABLE`.

## 8. Quotas

| Resource | Limit |
|---|---|
| CPU per handler | `start` 2 s, `command` and `update` 1 s, UI event 50 ms, `documentOpen` 500 ms, `selectionChange` 50 ms, `block.render` 20 ms, `gesture` 200 ms |
| CPU overall | 25% of one core averaged over 10 s |
| Memory | 64 MB heap per extension |
| Storage | 100 MB, shown as a meter on the extension page, with Clear data |
| Block `data`; fallback | 1 MB; 512 KB and 1024 px |
| Network | per call limits in section 3; a log of bytes sent per host per day, shown in the detail |
| UI tree; drawing | 500 nodes; 5000 ops |
| Declarations | 100 contributions in total; commands labelled with the extension name as an unremovable prefix; labels cannot equal a reserved verb (Delete, Undo, Save, Share, Export, Print) |
| Archive | 5 MB, 200 files, 1 MB of script |

**Strikes**: an abort for CPU or memory, or a crash, counts one strike per extension per session.
At the third strike the extension is turned off and a Banner appears in the Settings and Library
places, and in the extension's row: "Flashcards was turned off after problems." with Turn on again
and Details. This is separate from start-up safe mode.

## 9. Slot to permission matrix

Consent is computed from this table plus the declared permissions. The manifest must declare every
permission its contributions imply; an install with a gap is refused (`E_SCHEMA`). Consent shows
each permission with the slots that need it ("Read this document, needed by Export as PDF").

| Slot | Receives | May return | Implies |
|---|---|---|---|
| `command` | arguments only | nothing, or ops (needs `doc.write`) | nothing by itself; each API call needs its own permission |
| `noteBarItem` | tap | triggers a command | nothing |
| `sidePanel` | UI events; `selectionChange` if subscribed | UI tree | `selection.read` if it subscribes; `doc.read` if it calls `doc.*` |
| `canvasTool` | the finished gesture path | ops | `doc.write` when `produces` is strokes or shapes; nothing for a tool that only measures |
| `blockType` | its own `data` only | drawing, text, UI tree | nothing: it can edit its own blocks only |
| `selectionAction` | the selection content | ops | `selection.read`; plus `doc.write` if it returns ops |
| `libraryContextAction` | the target item's metadata (title, ids, dates); a document's content only with `doc.read` | ops, files | `library.read`; `doc.read` if it opens content |
| `insertSource` | search text | items to insert (validated and embedded by the host) | nothing; `network` or `files.pick` if it fetches |
| `importFormat` | the file the user chose | ops or a new document | `files.pick`; `doc.write` if into the open document, `library.write` if it creates one |
| `exportFormat` | the whole document, one-shot | bytes, saved through the system dialog | `doc.read` |
| `settingsSection` | UI events | UI tree | `storage.own` only |
| `contentSource` | search text, paging | items of one content type (validated, embedded on use) | nothing; `network` if it fetches |

If an extension also has `network`, the generic sentence in `trust.md` names every readable source
the table implies, whether or not it was declared separately.

## 10. Apple platforms

Whether interpreted extensions are allowed in the App Store builds is an open policy question. The
rule until it is answered: a shell without a runtime reports `runtime: none`; the review of a
`.graphoext` there says "Extensions are not available on this device" with Close and no install; a
block shows the stored fallback and a frame saying "Not available on this device", with Keep as
image only (never Get extension or Turn on, which could not work). Packs are unaffected.
