# GKTE Project Exchange Format — v1

## Purpose

`.gkte` is the portable project exchange format for Saha Takip.

It is deliberately **not** a raw Android Room/SQLite database file. Android and the future Windows desktop application must be able to read the same package without sharing an implementation-specific database engine.

Canonical MIME type:

`application/vnd.aricansoft.gkte`

Canonical extension:

`.gkte`

## Container

A GKTE file is a ZIP-compatible container with this layout:

```
<project>.gkte
├── manifest.json
├── data.json
└── photos/
    ├── <photo-id>.jpg
    └── ...
```

ZIP is an implementation detail of the transport container. Applications should identify a GKTE package from `manifest.json`, not merely from the ZIP signature.

## manifest.json

Required v1 fields:

```json
{
  "format": "GKTE",
  "formatVersion": 1,
  "minReaderVersion": 1,
  "databaseVersion": 5,
  "fileExtension": ".gkte",
  "mimeType": "application/vnd.aricansoft.gkte",
  "encoding": "UTF-8",
  "payload": "data.json",
  "assetsRoot": "photos/",
  "project": {
    "id": "source-project-id",
    "name": "Project name"
  },
  "exportedAt": 1791290000000,
  "producer": {
    "application": "Saha Takip",
    "platform": "android"
  }
}
```

Readers must reject an unsupported `formatVersion` rather than silently guessing.

Readers may ignore unknown manifest fields so compatible metadata can be added later.

## data.json

`data.json` contains a versioned domain snapshot, not a database file.

Top level:

```json
{
  "formatVersion": 1,
  "databaseVersion": 5,
  "meta": {
    "projectId": "...",
    "projectName": "...",
    "exportedAt": 1791290000000
  },
  "tables": {
    "...": []
  }
}
```

Problem/advantage records in `problem_records` may also carry nullable event-specific fields:
- `specificDescription`
- `floor`
- `unitNumber`
- `unitName`

These fields belong to the individual field record, not to the reusable problem definition.

Current table collections:

- `projects`
- `block_types`
- `blocks`
- `work_item_definitions`
- `block_type_work_items`
- `block_work_items`
- `problem_definitions`
- `problem_records`
- `deficiencies`
- `notes`
- `photos`
- `block_attribute_definitions`
- `block_attribute_values`
- `audit_events`

IDs are opaque strings. A reader must not derive meaning from an ID prefix.

Timestamps are Unix epoch milliseconds.

Enum values are stable wire tokens such as `FINISHED`, `RELATED_DISCIPLINE`, `ADVANTAGE`, `OPEN`.

## Photos and platform independence

A GKTE package never depends on an Android `content://` URI.

For portability, exported photo rows carry an empty `localUri`. The binary asset is stored at:

`photos/<photo-id>.jpg`

A photo row may contain:

- `blockWorkItemId` — always present;
- `problemRecordId` — nullable. When present, the photo is evidence belonging to that exact problem/advantage record;
- `deficiencyId` — nullable. When present, the photo is evidence belonging to that exact deficiency record.

There is no format-level photo-count limit per finding or deficiency.

`deficiencies` carries the deficiency lifecycle and management metadata. Stable wire enum values:
- status: `OPEN`, `IN_PROGRESS`, `FIXED`, `VERIFIED`
- priority: `NORMAL`, `HIGH`, `CRITICAL`

A deficiency remains active until status `VERIFIED`.

On import, each platform creates its own local file/URI reference and must preserve optional `problemRecordId` and `deficiencyId` relationships.

A future Windows reader must therefore use the photo ID + `photos/` convention, not Android URI semantics.

## Import identity policy

Importing a GKTE file creates an independent local project copy.

Source IDs are remapped to new local IDs while preserving all relations.

This prevents an imported project from silently overwriting an existing project with the same source IDs.

## Backward compatibility

Android currently also accepts the earlier legacy `.sitepack` structure that contains `data.json` without a GKTE manifest.

New exports must use `.gkte`.

Legacy compatibility is a reader concern only; the Windows application does not need to emit `.sitepack`.

## Android behavior

The Android app:

1. Exports the active project as `.gkte`.
2. Shares it to WhatsApp when available; otherwise falls back to the Android share sheet.
3. Registers itself as a handler for the canonical GKTE MIME type.
4. Also recognizes `.gkte` content URIs presented by file managers/WhatsApp as generic octet-stream or ZIP.
5. Imports the package immediately and opens the imported project.

Android can still show an app chooser when multiple installed apps claim the same file/MIME type. The application cannot override that operating-system decision.

## Windows desktop contract

The future Windows desktop application must:

- register the `.gkte` file extension with Windows;
- read `manifest.json` first;
- support GKTE `formatVersion: 1`;
- parse `data.json` independently of Android/Room;
- resolve photos from `photos/<photo-id>.jpg`;
- preserve unknown compatible fields when practical;
- export files that conform to this same contract.

The Android and Windows applications may have different internal databases. GKTE is the interoperability boundary between them.
