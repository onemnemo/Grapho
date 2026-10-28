# Documents: embedding, validation and merge

How a `.grapho` file carries what it uses, so it renders anywhere with or without the pack.
This text is meant to become part of `docs/file-format.md`.

## Rule A in one paragraph

A document embeds the **resolved item** for everything it uses from a pack, from the user's own
content or from an extension: the item's full definition (`def`) and its asset bytes. Rendering reads
only the embedded item. Built-in content is the exception: it is referenced by its built-in id and
never embedded (built-in ids are append only, so the app that wrote the note always has them).
No action on a pack, an extension or the library ever edits a document.

## Container

```
notes.grapho
├── manifest.json        version 3 or 4 (see Versions)
├── document.json        placements name an item by hash or by built-in id
├── items.json           the embedded items, keyed by item hash
├── blobs/
│   ├── 9f2c...e1        asset bytes, named by byte hash (SHA-256 of the stored bytes)
│   └── 3a07...b8
└── ... pages, pdfs, images as today
```

## Two content-addressed layers

1. **Blob**: bytes, named by their SHA-256. Same bytes, one file, whatever uses them.
2. **Item**: a record in `items.json`, named by the **item hash**: SHA-256 of the canonical JSON
   (RFC 8785) of `{contentType, role, def, blob, resources}` below. The key is content only: no
   name, pack id or date is in it. Two items with equal bytes but different `def` (a tintable and a
   non-tintable sticker) are two items; the same item from two packs is one.

The store is keyed by content hash only. Nothing in it is a counter, a position or a device id, so
two devices can never allocate the same key for different things.

### Item record

| Field | Type | Req | Max | Rules |
|---|---|---|---|---|
| `contentType` | string | yes | 32 | Registry id |
| `role` | `item` or `resource` | yes | | A resource has no picker presence (a font, a texture) |
| `def` | object | yes | 64 KB, depth 16 | The resolved definition: every field the renderer reads (see "Renderer inputs" below). No expressions, links or block references |
| `blob` | object | when the type has bytes | | `{hash, format, size}`; `format` is a raster format or `svg-subset` or `json` |
| `resources` | list of item hash | no | 16 | Items this one needs (font, texture); each must be in the store |
| `fallback` | hash of a PNG blob | no | | A rendering for readers that lack the type's renderer. Made by the core when the item is embedded, at most 512 px and 128 KB |
| `origins` | list of origin | yes | 8 | Provenance, not part of the key; merged by union (below) |

### Origin (provenance)

| Field | Type | Req | Rules |
|---|---|---|---|
| `source` | `pack`, `user`, `extension` | yes | `builtin` never appears: built-in items are not embedded |
| `contentType` | string | yes | As in the item |
| `packId` | UUID | when `pack` | The pack's id |
| `assetId` | string | when `pack` | Local id in the pack |
| `packVersion` | version | when `pack` | Version it was copied from |
| `publisher` | key fingerprint | when `pack` or `extension` | Short form of the publisher key |
| `extId` | UUID | when `extension` | |
| `claimed` | object | no | `{packName, author, licence}` as claimed by the file. Display only: "claimed by the file". It never raises a licence and never enables export (see Licences) |

A placement in `document.json` holds the **item hash** (or `builtin:<contentType>/<id>`). The old
four field reference is therefore the first `origin` of the item, and is what "Use the newer
versions" and "Uses 6 stickers from Botanica" read.

### Renderer inputs

Rule: every input a renderer reads is in `def`, the blob, or the core. Never in a pack only field.
Per type the exact `def` fields are in `registry.md`. A build check diffs the fields each renderer
reads against the fields `def` embeds, per content type, and fails when a renderer reads a field
that is not embedded.

Strokes store the full resolved pen parameter record, versioned and append only: kind, width and
width range, pressure curve, tilt response, opacity, nib shape, smoothing, texture (an item hash).
A pen change or removal changes no old stroke.

## Extension blocks and extension supplied items

- A block stores `{extId, extName, extVersion, blockType, data, assets, fallback}`.
- `data` is JSON, at most 1 MB. A reference to an asset inside `data` is written
  `{"$asset": "<item hash>"}` and nothing else. `assets` is the list of those hashes, maintained by
  the core, not the extension.
- The only way for an extension to place an asset is `doc.embed(source)` (see `extension-api.md`),
  which runs the validators, writes the item with `source: extension`, and returns the hash.
  Data that names a pack, a URL or an id of the pack store is rejected at write (`E_BLOCK_DATA`).
  Items returned by `insertSource` and `contentSource` follow the same path.
- Any `$asset` hash that is not in the store makes the write fail.

## Validators at every ingestion

The same validators, owned by the content type, run at: pack import, document open, "Keep in my
library", and the extension API. Validation is header first (dimensions are read before decoding).
Over any cap the item shows the type's missing-asset placeholder; the document still opens and is
never refused or crashed.

| Check | Cap |
|---|---|
| Bytes per blob | 20 MB |
| Raster formats | PNG, JPEG, WebP, decoded 16 megapixels, 8192 px per side |
| Vector | the sanitised SVG subset only (below) |
| Total `blobs/` per document | 500 MB |
| Entries in `items.json` | 20 000 |
| `def` JSON | 64 KB, depth 16, no floats beyond 6 decimals |
| Block `data` | 1 MB; fallback image 1024 px long side, 512 KB |
| Fonts | only as resources, only formats the platform shaper accepts, embedding bits checked |

### Sanitised SVG subset

Allowed: `svg`, `g`, `path`, `rect`, `circle`, `ellipse`, `line`, `polyline`, `polygon`,
`linearGradient`, `radialGradient`, `stop`, `clipPath`, `defs`, `use` to an id in the same file;
attributes for geometry, transform, solid fill and stroke, opacity, gradient references to the same
file. Not allowed, and removed by the sanitiser: `script`, `style`, `foreignObject`, `image`, `a`,
`animate*`, `set`, `filter` and filters, `mask`, `pattern`, `text` (convert to outlines first),
entities, any `href` that is not `#id`, any `url()` not `#id`. Render cost is bounded: 10 000
elements, 1 MB, `use` nesting 8.

The sanitiser is versioned and lives in the core. It writes `sanitiser: N` in the blob's item
`def`. The **stored hash** (named in the document) is of the sanitised bytes. The pack manifest
carries the hash of the shipped bytes, which is what update comparison uses, so a new sanitiser
never makes every sticker look "changed".

## Licences and received files

- Embedding for rendering is always allowed.
- `claimed.licence` is shown as "Claimed by the file". "Keep in my library" copies the item with
  licence `unknown`, which blocks Export as pack of that item.
- An item whose pack licence is `all-rights-reserved` is marked `noExtract` in `origins`: it renders,
  and "Keep in my library" and Export as pack are not offered for it. Open question: this rule is not
  yet confirmed.

## Versions

- Built-in references do not change the version, so notes with only built-in covers and stickers stay
  version 3.
- The first time a note gets a pack, user or extension item, its version becomes 4. Before the
  write, the side panel footer shows one line: "This note now needs Grapho 1.5 or newer." (no Dialog,
  no reflow of the page).
- Compaction (below) that leaves no items returns the note to version 3.
- Readers refuse a version higher than they support, as today.

## Compaction and merge

- **Compaction** removes items and blobs with no placement, resource or `$asset` reference. It runs
  only as part of a local save when the sync state has no unmerged remote base, or on the user action
  "Tidy file". It never runs during a merge.
- **Merge of two copies** (sync, or a document received twice): `items.json` is a union by item
  hash; `blobs/` a union by byte hash; `origins` a union by (source, packId, assetId, packVersion,
  extId). Nothing is deleted by a merge. A hash present on one side only is kept.
- **Blocks**: a block carries `rev` (counter per block) and `dataHash`. If both sides changed a block,
  both blocks are kept, the older one placed beside the newer and marked "Other version" (the sync
  conflict state, "both versions kept"). Halves are never mixed: a block's `data` and its `fallback`
  are one unit.

## Pack updates and documents

- A document keeps the embedded version of every item.
- "Use the newer versions" (document menu) appears when an origin's pack is installed at a higher
  version and the item at the same `assetId` has a different shipped hash. It replaces only the
  listed items' placements, as one undo step.
- Rename detection: an update whose item has a new `assetId` and the same shipped hash is a rename;
  holders and "newer versions" follow it.
