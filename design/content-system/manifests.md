# Manifests and containers

Specifications, not code. Field names are final enough to implement against; the persisted shape is
append only once released (repo rule 7). The document side (embedded items, validators, merge) is in
`documents.md`. Signing and trust are in `trust.md`. Extension runtime and API are in
`extension-api.md`.

## Shared rules

- **Encoding**: UTF-8 JSON. Unknown fields are ignored on read and kept in the stored original.
- **Canonical JSON** (anything hashed or signed): RFC 8785 (JCS). The canonical form is the stored
  form. Numbers in hashed JSON are integers or decimals with at most 6 fraction digits.
- **Hashes**: lowercase hex SHA-256. **Ids**: lowercase UUID v4.
- **Version**: `major.minor.patch`, each an integer 0 to 65535, compared numerically. App versions
  use the same form.
- **Types**: `string` is plain and not localised. `text` is localisable: a plain string, or a map
  from BCP 47 tag to string. Choice of text: the user's language exactly; then its language only
  (`nb` and `nn` count as `no`); then `en`; then the first key in sorted order. Lengths are counted
  in Unicode scalar values after NFC. Search and sort use every language. `author` and `tags` are
  `string`, not localised.
- **Display normalisation** of names and authors: bidi control characters are removed, NFC, runs of
  spaces collapsed, no control characters. The author "Grapho" is reserved to the Grapho publisher key.
- **Max** columns below are hard limits. A violation is a named refusal (Validation codes).

## Pack manifest (`manifest.json` in a `.graphopack`)

| Field | Type | Req | Max | Rules | Example |
|---|---|---|---|---|---|
| `format` | string | yes | | `grapho-pack` | `grapho-pack` |
| `schema` | integer | yes | | 1 or higher | `1` |
| `minAppVersion` | version | yes | | Below it, the pack is Incompatible as a whole (unless `schema` is newer, then the schema rule applies) | `1.4.0` |
| `id` | UUID | yes | | Made at first export, kept for every version. Identity is the triple (kind, `id`, publisher key): see `trust.md` | `0b6e1d9a-7c34-4f0e-9a52-3d1c8e4b7f21` |
| `name` | text | yes | 60 | Not unique | `Botanica` |
| `author` | string | yes | 60 | Display name chosen by the publisher, not trusted | `Ana Ruiz` |
| `publisher` | string | yes | 64 | Fingerprint of the publisher public key, 16 hex digits | `a41c09d27be3f580` |
| `version` | version | yes | | Higher means newer. Suggested as stored version plus one patch | `1.2.0` |
| `description` | text | no | 500 | Plain text | `Ferns and leaf borders.` |
| `preview` | path | no | 1024 by 640 px, 512 KB | PNG, listed in `files`. Missing: composed from item previews, up to 9, one per type first | `preview.png` |
| `licence` | string | yes | | `cc0`, `cc-by-4.0`, `cc-by-sa-4.0`, `all-rights-reserved`, `other`. Readers treat an unknown value as `other` | `cc-by-4.0` |
| `licenceText` | text | when `other` | 2000 | Shown in review | |
| `licenceUrl` | string | no | 300 | https only | |
| `homepage` | string | no | 300 | https only | |
| `createdWith` | version | no | | App version that wrote it. Informational | `1.4.0` |
| `requiresExtensions` | list of {id, name} | no | 8 | Informational only: never a runtime dependency. Shown in review as "Works best with Flashcards" | |
| `items` | list of item | yes | 5000 | At least one | |
| `files` | list of file | yes | 6000 | Every file except `manifest.json` and `signature.json` | |

### Item

| Field | Type | Req | Max | Rules |
|---|---|---|---|---|
| `assetId` | string | yes | 64 | `a-z0-9._-`, unique in the pack, stable across versions |
| `contentType` | string | yes | 32 | Registry id; unknown ids are listed "Not supported in this version" |
| `role` | `item` or `resource` | yes | | A resource appears in no picker |
| `name` | text | when `item` | 60 | |
| `tags` | list of string | no | 8 items of 24 | Lowercase letters, digits, hyphen; feed Filter |
| `file` | path | yes | 200 | Main file |
| `hash` | hash | yes | | SHA-256 of the shipped bytes of `file` (for JSON, of its canonical form). Update comparison uses this |
| `format` | string | yes | 16 | Declared by the content type |
| `def` | object | when the type has one | 64 KB, depth 16 | The type's definition fields (see `registry.md`). No expressions, links or block references |
| `uses` | list of assetId | no | 16 | Same pack only, depth at most 4, no cycles |
| `licence` | string | no | | Per item override; default is the pack's. An item copied from another pack keeps its own |
| `origin` | object | no | | `{packId, packName, author, licence}` when the exporter copied it from another pack |

Pack level rules: no two items may have the same `hash` and `contentType` ("These items are the same
picture"); the sum of declared sizes counts items, not files; resources are counted too.

### File

| Field | Type | Req | Max | Rules |
|---|---|---|---|---|
| `path` | string | yes | 200 bytes, depth 6 | Relative, `/` separators, NFC, no `..`, no leading `/`, no Windows reserved names, unique case-insensitively |
| `hash` | hash | yes | | Of the file bytes |
| `size` | integer | yes | 20 MB | Must equal the actual inflated size |

### Example (abridged)

```
format grapho-pack  schema 1  minAppVersion 1.4.0
id 0b6e1d9a-7c34-4f0e-9a52-3d1c8e4b7f21  name Botanica  author Ana Ruiz  version 1.2.0
publisher a41c09d27be3f580  licence cc-by-4.0
items
  fern-01   sticker  item      content/stickers/fern-01.svg  hash 9f2c1a...  def {size:[160,160], tintable:true}
  leaf-pen  pen      item      content/pens/leaf-pen.json    uses [leaf-tex]
  leaf-tex  pen      resource  content/res/leaf-tex.png
  spring    theme    item      content/themes/spring.json    (custom-themes.md "Theme pack data")
```

## Reading the archive (every shell and the core agree)

The reader works on the stream and never extracts to a path it did not build itself. Refused, each
with its own code and copy:

- not a zip, encrypted entries, zip64 beyond the limits, symlink entries, absolute paths, `..`
- duplicate entry names, names equal under case folding or NFC, Windows reserved names
- an entry not listed in `files`, or a listed file missing, or `preview` or `icon` not listed
- an entry whose inflated size exceeds its declared size or 20 MB (inflate is capped as it runs, not
  trusted from the header), total inflated size above 250 MB (pack) or 5 MB (extension), a deflate
  ratio above 100 to 1
- any hash or size mismatch
- executable, script, HTML or font-as-item content (detected by content, not extension)
- more than 5000 items, nesting `uses` deeper than 4, a cycle
- a `format` or `schema` it cannot read; a `def` that fails the type's schema
- a signature that does not verify (see `trust.md`)

Validation codes: `E_NOT_ARCHIVE`, `E_UNSAFE_PATH`, `E_DUPLICATE`, `E_UNLISTED`, `E_SIZE`, `E_RATIO`,
`E_HASH`, `E_CODE`, `E_LIMIT`, `E_NESTING`, `E_SCHEMA`, `E_SIGNATURE`, `E_UNSIGNED`. Each has user
copy in `flows.md`, worded for the source (file from a chat: "Ask them to send it again"; catalogue:
"Try again later").

## `.graphopack` container

```
Botanica-1.2.0.graphopack      a zip; media stored, JSON deflated
├── manifest.json
├── signature.json
├── preview.png
└── content/ (stickers, pens, res, themes, ...)
```

- Type registration: extension `.graphopack`, MIME `application/vnd.grapho.pack+zip`, a platform
  type identifier per shell (declared in the platform standards). Import accepts by content: a zip
  whose `manifest.json` has the right `format`. A file renamed `.zip` or `.bin` by a chat app is
  accepted; a zip containing exactly one `.graphopack` is unwrapped one level.
- File name suggestion: Unicode letters and digits kept, only reserved characters
  (`/ \ : * ? " < > |` and controls) replaced, name capped at 80 bytes, then `-` and the version.
- SVG assets follow the sanitised subset in `documents.md`. The importer sanitises; the stored hash
  is of the sanitised bytes; the shipped `hash` stays in the manifest for integrity and updates.
- JSON assets: no expressions, no links, no block references; depth 16, 256 KB.

## Extension manifest (`manifest.json` in a `.graphoext`)

| Field | Type | Req | Max | Rules | Example |
|---|---|---|---|---|---|
| `format` | string | yes | | `grapho-extension` | |
| `schema` | integer | yes | | 1 or higher | `1` |
| `minAppVersion` | version | yes | | Below it, the extension does not install | `1.5.0` |
| `id` | UUID | yes | | As for packs | |
| `name` | text | yes | 60 | | `Flashcards` |
| `author` | string | yes | 60 | | `Study Lab` |
| `publisher` | string | yes | 64 | Key fingerprint | |
| `version` | version | yes | | | `2.0.1` |
| `description` | text | yes | 500 | In the user's terms | |
| `icon` | path | yes | 8 KB | Monochrome SVG in the allowed subset, listed in `files` | `icon.svg` |
| `licence` | string | yes | | As for packs | |
| `homepage` | string | no | 300 | https only | |
| `runtime` | string | yes | | `grapho-script 1`; unknown runtime makes it Incompatible | |
| `main` | path | yes | 1 MB | Entry script; see `extension-api.md` | `app/main.js` |
| `permissions` | list of permission | yes | 16 | Fixed vocabulary below; must include every permission its contributions imply | |
| `contributes` | object | yes | 100 total | Keys are slot ids (`registry.md`); every contribution declares what it receives and produces | |
| `blockTypes` | list | when it creates blocks | 8 | `{id, name, dataSchema, render, describe}`: `dataSchema` is a JSON Schema subset (types, required, enums, max lengths, max items); `render` and `describe` name script functions. Blocks are declared here only; the slot `blockType` references an id from this list | |
| `files` | list of file | yes | 200 | | |

### Permission vocabulary (fixed)

A permission is `{id, arg?}`. The set is closed in a schema; an unknown id makes the extension
Incompatible. Wording is owned by the app, never by a manifest.

| Id | Argument | Plain-language line | Tier | Reads? |
|---|---|---|---|---|
| `network` | host, https only, port 443, exact name, no IP literals, no wildcards, up to 5 | Connect to: example.com | Outside the app | |
| `files.pick` | none | Files you choose | Outside the app | yes |
| `clipboard.read` | none | Read what you copy | Outside the app | yes |
| `clipboard.write` | none | Copy to your clipboard | Outside the app | |
| `microphone` | none | Use the microphone | Outside the app | yes |
| `camera` | none | Use the camera | Outside the app | yes |
| `library.read` | none | Read your library | Your library | yes |
| `library.write` | none | Change your library | Your library | |
| `doc.read` | none | Read this document | This document | yes |
| `doc.write` | none | Change this document | This document | |
| `selection.read` | none | Read what you select | This document | yes |
| `storage.own` | none | Keep its own settings on this device | Detail only, never in consent | |

Three tiers appear in consent, high risk first. `storage.own` is granted always and shown only in the
extension's detail. The "Reads?" column drives the generic sentence in `trust.md`.

## `.graphoext` container

```
Flashcards-2.0.1.graphoext
├── manifest.json
├── signature.json
├── icon.svg
└── app/ main.js, lib/, assets/
```

Limits: 5 MB archive, 200 files, 1 MB of script in total, assets as in packs. Same archive reading
rules. No remote code is fetched (see `extension-api.md` for what is and is not enforceable).

## `signature.json`

```
alg       ed25519
publicKey base64 of the 32 byte key
manifest  hash of the canonical manifest (which lists every file hash)
value     base64 signature over the manifest hash
```

`publisher` in the manifest is the first 16 hex digits of SHA-256 of `publicKey`. Verification and
trust rules: `trust.md`.

## On-device store (not shared)

| Record | Fields |
|---|---|
| Installed pack or extension | kind, id, publisher, version, state, on or off, order, installedAt, original archive path (the only copy; assets are read from it), previous version archive (30 days), trashedAt |
| Hidden items | (packId, assetId) set, kept outside the pack record so it survives a reinstall or expiry; by hash as well |
| Holders (core reference table) | holderId, holderKind (wheel slot, quick slot, saved look, default, theme, folder), itemHash, itemRecord (self-contained like a document item) |
| User items | itemHash, source (one of: yours, pack, built-in), name; origin label for kept copies `{packName, author}` |
| Usage index | per note: note id, set of item hashes and origins; per pack: counts by holder kind; rebuilt in the background |
| Made packs | id, name, version, item hashes, publisher key reference (the items stay in Yours) |

Thumbnails are an evictable cache and are never the source of truth. Trash is a flag on the record,
not a move; the archive stays where it is.
