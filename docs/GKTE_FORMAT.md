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
  "databaseVersion": 6,
  "fileExtension": ".gkte",
  "mimeType": "application/vnd.aricansoft.gkte",
  "encoding": "UTF-8",
  "payload": "data.json",
  "payloadSha256": "<sha256>",
  "assetsRoot": "photos/",
  "assets": [{"path":"photos/<photo-id>.jpg","size":12345,"sha256":"<sha256>"}],
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

Readers may ignore unknown manifest fields so compatible metadata can be added later. New Android exports include SHA-256 and byte-size metadata for the payload and every photo asset; readers that receive these fields must validate them before importing.

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


## Integrity and resource limits

New Android exports are fail-fast: a project is not reported as successfully exported when any referenced photo cannot be read.

Android import rejects path traversal, duplicate archive entries, more than 5,000 ZIP entries, any single expanded entry over 100 MiB, or total expanded content over 2 GiB. New packages also validate `payloadSha256` and every declared asset hash/size before database insertion.

Legacy packages that predate integrity metadata remain readable, but new exports always include the integrity metadata.


## Database version 6

Android database version 6 adds relational foreign-key protection without changing the portable GKTE format version. GKTE remains `formatVersion: 1`; `databaseVersion` is producer metadata, not the cross-platform format version.

A failed Android import is transactional at the database layer. Photo files copied before a database insertion failure are removed, and restored-photo directories that do not correspond to a local project are cleaned on subsequent application startup/import.
