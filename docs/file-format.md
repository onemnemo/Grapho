# The `.grapho` file format, version 4

A `.grapho` file is a standard ZIP archive, so any unzip tool can open it.
Structure is plain JSON. Ink is stored in a compact binary file per page,
because handwriting produces millions of points.

```
notes.grapho
├── manifest.json
├── document.json
├── pages/
│   ├── 6f1c…e2.strokes
│   └── 9a04…7b.strokes
├── pdfs/
│   └── c3d1…58.pdf
└── images/
    ├── 4e7a…0c.png
    └── b208…d9.jpg
```

The `pdfs/` folder only exists in notes that hold PDF pages, and the `images/`
folder only in notes that hold images.

## manifest.json

```json
{ "format": "grapho", "version": 1 }
```

The version is 4 when the file holds text boxes, 3 when it holds images but no
text boxes, 2 when it holds PDF pages but neither (see below) and 1 otherwise,
so older readers keep opening notes that don't use newer features. Readers
accept all four, and must refuse files with a `version` higher than they
support, rather than skipping data they don't understand.

## document.json

The document's structure: title, canvas type, pages and layers. It contains
no stroke data.

```json
{
  "id": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "title": "Biology",
  "canvas": {
    "kind": "paged",
    "paper": { "name": "a4" },
    "orientation": "portrait",
    "background": { "color": 4294967295, "pattern": { "kind": "lined", "spacing_mm": 8.0 } }
  },
  "pages": [
    {
      "id": "6f1c…e2",
      "layers": [
        {
          "id": "…", "name": "Layer 1", "visible": true, "locked": false, "opacity": 0.5,
          "images": [
            {
              "id": "…", "source": "4e7a…0c",
              "transform": [180.0, 0.0, 72.0, 0.0, 120.0, 96.0],
              "crop": [0.0, 0.1, 1.0, 0.9]
            }
          ]
        }
      ]
    },
    {
      "id": "9a04…7b",
      "size": { "width": 595.3, "height": 841.9 },
      "pdf": { "source": "c3d1…58", "index": 0 },
      "layers": [
        { "id": "…", "name": "Layer 1", "visible": true, "locked": false }
      ]
    }
  ],
  "pdfs": [
    { "id": "c3d1…58", "name": "lecture.pdf", "pages": 12 }
  ],
  "images": [
    { "id": "4e7a…0c", "kind": "png", "width": 1200, "height": 800 }
  ]
}
```

- `canvas.kind` is `paged` or `infinite`. An infinite canvas has exactly one
  page and no `paper` or `orientation`.
- `paper.name` is `a3`, `a4`, `a5`, `letter` or `custom` (with `width_mm` and
  `height_mm`). Sizes are given in portrait orientation.
- `pattern.kind` is `blank`, `lined`, `grid` or `dotted`.
- Colours are `0xRRGGBBAA` stored as an unsigned integer.
- `background.color` is the paper colour, and may be dark. Apps may draw
  near-black or near-white ink with its lightness flipped so it stays legible
  on dark or light paper, but must never rewrite the stroke colours stored in
  the file.
- `cover` is optional: `{ "color": 2847274495 }`. Readers that find no cover
  pick a default.
- Layers are listed bottom to top.
- A layer's `opacity` is optional: a number from 0 to 1, and absent means 1. Writers
  leave it out when it is exactly 1, so files that don't use it are unchanged,
  and readers that don't know it ignore it. Strokes on a layer with an opacity
  below 1 are drawn as a group at that opacity: they are composited together
  first, so overlapping strokes of the layer do not show through each other.
- A page's `size` is optional: `{ "width": 595.3, "height": 841.9 }` in points,
  as the page is shown. Absent means the canvas paper size. Blank pages added
  next to a PDF page copy its size.
- A page's `pdf` is optional: `{ "source": "<id>", "index": 3 }` names a PDF in
  `pdfs` and one of its pages, counted from 0. That page is drawn as the page's
  background in place of the paper pattern, over white, and the ink goes on
  top. A page with a `pdf` always has a `size`, the PDF page's size after its
  rotation. Version 2.
- `pdfs` is optional and lists the PDF files the pages refer to: `id`, the
  original file `name` and the number of `pages`. Each one is stored as
  `pdfs/<id>.pdf`. Writers only store a PDF that some page refers to. A reader
  refuses a file whose page refers to a PDF that is not listed, whose file is
  missing, or whose page index is past the end, as damaged. Version 2.
- A layer's `images` is optional and lists the pictures placed on it, bottom to
  top. They are drawn under the layer's strokes, and as part of the layer's
  opacity group. Each has an `id`, the `source` it shows (an entry of
  `images` at the top level), a `transform` and an optional `crop`. Version 3.
  - `transform` is `[sx, kx, tx, ky, sy, ty]`, mapping the unit square of the
    shown part of the source (0 to 1 across, 0 to 1 down, y down, like the
    page) onto the page in points: `x' = sx*x + kx*y + tx`,
    `y' = ky*x + sy*y + ty`. The unit square's corners are the picture's
    corners, so a rotated or mirrored picture is a rotated or mirrored matrix,
    and the picture's size on the page is the length of the matrix's columns.
  - `crop` is `[left, top, right, bottom]`, the part of the source that is
    shown as fractions of its width and height, with `left < right` and
    `top < bottom`, all from 0 to 1. Absent means `[0, 0, 1, 1]`. Writers leave
    it out when it is the whole picture. The part inside the crop is stretched
    over the unit square, and the rest of the source is not shown.
- `images` is optional and lists the picture files that layers refer to:
  `id`, `kind` (`png` or `jpeg`) and the pixel `width` and `height`. Each one
  is stored as `images/<id>.png` or `images/<id>.jpg`. Writers only store a
  picture that some layer places, and share one source between all the
  placements of the same bytes. A reader refuses a file whose image refers to
  a source that is not listed, whose picture file is missing or is not a PNG
  or JPEG of the stated kind and size, or whose `transform` or `crop` is not
  valid, as damaged. Version 3.
- A layer's `texts` is optional and lists the text boxes placed on it, bottom
  to top. They are drawn over the layer's images and under its strokes, and as
  part of the layer's opacity group. Version 4. Each has:
  - an `id`, and `x`, `y`, `width` and `height`, the box before it is turned,
    in points. `width` and `height` are at least 1. The shell lays the words
    out inside the box and writes back the height it needs.
  - `rotation`, optional: the clockwise turn about the centre of the box, in
    radians. Absent means 0.
  - `text`, the plain text, at most 20000 characters. A new line is `\n`.
  - `size`, the font size in points, from 4 to 400, and `color` as
    `0xRRGGBBAA`.
  - `align`: `start`, `centre` or `end`. Absent means `start`.
  - `bold`, optional. Absent means false.

  A reader refuses a file whose text box has a number that is not finite, a
  box below the least size, a font size out of range or too long a text, as
  damaged.
- All ids are UUIDs, so documents edited on two devices can be merged later.

## Coordinates

Document units are points (1/72 inch, as in PDF), with the origin at the
top-left corner of the page and y pointing down. An A4 page is about
595 × 842 points.

## pages/&lt;page-id&gt;.strokes

Every page has one strokes file, even if it's empty. All numbers are
little-endian.

| Field | Type | Notes |
|---|---|---|
| magic | 4 bytes | `GRST` |
| version | u16 | `1`, `2` if the page contains a fill, or `3` if any stroke is dashed or dotted |
| stroke count | u32 | |
| strokes | … | `stroke count` records, in drawing order |

Each stroke record:

| Field | Type | Notes |
|---|---|---|
| layer | u16 | Index into the page's `layers` in document.json |
| id | 16 bytes | UUID |
| tool | u8 | 0 pen, 1 pencil, 2 highlighter, 3 fill |
| line | u8 | Version 3 only: 0 solid, 1 dashed, 2 dotted. Absent in versions 1 and 2, which are all solid |
| color | u32 | `0xRRGGBBAA` |
| width | f32 | Width at full pressure, in document units |
| started at | i64 | Milliseconds since the Unix epoch |
| point count | u32 | |
| points | 16 bytes each | `x`, `y`, `pressure` (0 to 1), `time` (ms after start), all f32 |

A fill (tool 3) is a filled area rather than a line. Its points form a closed
polygon (the last point joins back to the first) in document units, with
pressure 1 and time 0, and `width` is 0. It is drawn in the stroke's colour
beneath the other strokes of its layer. Writers use strokes version 1 for
pages without fills and version 2 for pages with any, so older readers still
open files that don't use fills; readers accept both.

A dashed or dotted stroke stores its ordinary points. The pattern is derived
from them when drawing: a dash is 4 widths long with a 3 width gap (measured
between the centres of the round caps), and a dot is a disc of the pen's
radius every 2.5 widths, where the width is the stroke's `width`. The pattern
starts at the first point. Writers use version 3 only for pages with a
non-solid stroke, and readers refuse an unknown line value as damaged.

Point timestamps let audio recordings play back in sync with the ink.

## pdfs/&lt;pdf-id&gt;.pdf

An unmodified copy of the imported PDF. It is stored in the archive without
compression, since a PDF is already compressed. Listing notebooks does not read
these files.

## images/&lt;image-id&gt;.png and .jpg

The picture exactly as it was embedded, stored in the archive without
compression, since PNG and JPEG are already compressed. A PNG may have any
colour type and an alpha channel. A JPEG is baseline or progressive, 8 bit,
with one (gray) or three (colour) components. Pictures are at most 16384 pixels
on a side. Listing notebooks does not read these files.

## Compatibility

New optional JSON fields can be added without changing the version, and
readers ignore fields they don't know. Any change to the binary layout, or
any change that older readers would misread, bumps the version.

# The library file, version 1

The library is the index of a person's notebooks: folders, items and Trash. It is one JSON
document that lives next to the notebooks, not inside them (on Android, `library.json` in the
app's files directory). A `.grapho` file never knows which folder it is in. The library is device
data and is not part of the `.grapho` format above.

```json
{ "version": 1, "folders": [ ... ], "items": [ ... ] }
```

Ids are 128 bit random values written as 26 character Crockford base32 strings. Times are UTC
milliseconds. Names are camelCase. Every field except the ones marked required may be missing and
reads as its default.

A folder has `id` and `name` (required), `parentId` (null is the top level), `styleRef`,
`sortOrder` (manual order among siblings, in steps of 1024), `createdAt`, `modifiedAt`,
`deletedAt` and `deleteBatchId`. Sibling names are unique ignoring case, after trimming. A deleted
folder has `deletedAt` set on itself only; everything below it is in Trash by inheritance.

An item has `id`, `kind` (`notebook`, `pdf` or `canvas`) and `fileRef` (the `.grapho` file name,
required), and `title`, `folderId`, `coverRef` (for example `color:#RRGGBB`), `pageCount`,
`createdAt`, `modifiedAt`, `openedAt`, `starred`, `deletedAt`, `deletedFromFolderId` and
`deleteBatchId`. An item with `deletedAt` is in Trash. Items deleted together with a folder share
its `deleteBatchId`, so restoring the folder restores exactly those items. Anything in Trash for
more than 30 days is removed, and the shell deletes the files the removal reports.

Readers ignore fields they do not know and refuse a `version` above the one they support. Writers
replace the file in one step (write a temporary file, then rename it), so a crash never leaves a
half written library.

To share or back up many notebooks and folders at once, the app writes a
library package instead, see [library-package.md](library-package.md).
