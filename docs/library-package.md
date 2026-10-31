# The `.grapholib` library package, version 1

A library package holds many notebooks, canvases and folders in one file, so a
part of the library, or all of it, can be shared or backed up and imported
again with its folder tree. A single notebook is still exported as a plain
`.grapho` file (see [file-format.md](file-format.md)); the package never
changes that format.

Like a notebook, a package is a standard ZIP archive:

```
School.grapholib
├── manifest.json
├── library.json
└── notebooks/
    ├── 01JB8Y…3K.grapho
    └── 01JB8Z…7Q.grapho
```

Each file under `notebooks/` is a complete `.grapho` file, byte for byte as it
was in the library. They are stored without compression, since a notebook is
already compressed. Entries of 4 GiB or more use zip64.

## manifest.json

```json
{ "format": "grapho-library", "version": 1 }
```

The `format` tells a package apart from a notebook, whose manifest says
`grapho`. Readers must refuse a `version` higher than they support, rather
than skipping data they don't understand.

## library.json

```json
{
  "exportedAt": 1793412000000,
  "folders": [
    { "id": "01JB8X…", "parentId": null, "name": "School", "styleRef": null,
      "createdAt": 1793400000000, "modifiedAt": 1793400000000 },
    { "id": "01JB8Y…", "parentId": "01JB8X…", "name": "Maths" }
  ],
  "items": [
    { "id": "01JB8Y…3K", "kind": "notebook", "title": "Algebra",
      "folderId": "01JB8Y…", "coverRef": "color:#336699", "starred": false,
      "pageCount": 12, "createdAt": 1793400000000, "modifiedAt": 1793410000000,
      "openedAt": null, "file": "notebooks/01JB8Y…3K.grapho" }
  ]
}
```

- Ids are the library's own 26 character ids. They only link folders and items
  inside the package; an import gives everything new ids.
- `parentId` and `folderId` are `null`, or missing, for the top level of the
  package. A parent the package does not hold also means the top level.
- `kind` is `notebook`, `pdf` or `canvas`. An importer may work it out from the
  notebook file instead.
- Every field but `id`, `name`, `title` and `file` may be missing.

## Export

Exporting picked folders includes every folder and notebook below them. A
picked folder whose parent is not picked becomes a top level folder of the
package, and a picked notebook outside the picked folders goes to its top
level. Picking everything at the top of the library backs up the whole
library. Things in Trash are left out, and so is a notebook whose file cannot
be read.

## Import

An import adds the package's folders and notebooks under the folder the person
is in, keeping the tree. A folder whose name a sibling already has gets " 2",
" 3" and so on. A notebook that is missing or cannot be read is skipped and
counted. Folders whose parents form a cycle are left out. Any other failure
leaves the library as it was and removes the files the import wrote.
