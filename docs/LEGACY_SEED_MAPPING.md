# KONYA 444 Legacy Seed Mapping

The built-in starter seed is a migration of the user's uploaded workbook:

- Source: `KONYA 444 İLERLEME DURUMU.xlsx`
- Sheet: `Ekim İlerleme Durumu`
- Source SHA-256: `5abb01293ff5d29007af3d6419b97f182369bdb0791e3d80ddb504f2c0f08517`

## Migration rules

The legacy workbook has two different kinds of information in the same cells:

1. `✔` means the work item was finished.
2. Codes such as `E-1`, `E-2`, `B.H.`, `L.İ.E.` are problem references.

V0 deliberately **does not infer** Quality, Control or Blocked status from those problem codes. Those are independent dimensions in the new application and the legacy sheet did not store them independently.

Examples:

- `✔ E-1` → Progress = Bitti + open problem E-1.
- `E-3` → Progress = Başlanmadı + open problem E-3.
- `L.İ.E.` → Progress = Başlanmadı + open problem L.İ.E.; it is **not automatically marked Bloke**.
- `İ.Y.` → open problem İ.Y.; it is **not automatically converted to Kontrol edilemedi**.

This prevents migration from manufacturing facts that are absent from the source workbook.

## Block parameter

The legacy `YATAY TAVA VAR MI` row is migrated as a block attribute, not a work item:

- `E` → `Evet`
- `H` → `Hayır`

GK values are populated for GK-1…GK-17 and GB values for GB-1…GB-6 exactly where the source carries E/H values. A-1/A-2 are left unset because the source cells are blank.

## Matrix behavior

Open problems are displayed independently from status with `P` / `P<n>` markers. Therefore a migrated cell can be both `✓` and `P`, preserving the original `✔ E-x` meaning without collapsing problem and completion state.
