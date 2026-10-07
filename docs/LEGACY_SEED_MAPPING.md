# KONYA 444 Legacy Seed Mapping

The built-in starter seed is a migration of the user's uploaded workbook:

- Source: `KONYA 444 İLERLEME DURUMU.xlsx`
- Sheet: `Ekim İlerleme Durumu`
- Source SHA-256: `5abb01293ff5d29007af3d6419b97f182369bdb0791e3d80ddb504f2c0f08517`

## Migration rules

The legacy workbook has two different kinds of information in the same cells:

1. `✔` means the work item was finished.
2. Codes such as `E-1`, `E-2`, `B.H.`, `L.İ.E.` are problem references.

V0 deliberately **does not infer** Quality, Control or Blocked status from problem codes. Those are independent dimensions in the new application and the legacy sheet did not store them independently.

The workbook also contains a separate semantic case: `MUTFAK FAYANS DOLAP` is not an electrical work item. It is a related construction-trade item whose state affects electrical work. The right-side legend explicitly defines green `L.İ.E.` as favorable missing construction work and the purple checkmark as construction work completed against the electrical team's interest. Those meanings are preserved as first-class Advantage / Problem records; they are not inferred from ordinary electrical status.

Examples:

- `✔ E-1` → Progress = Bitti + open problem E-1.
- `E-3` → Progress = Başlanmadı + open problem E-3.
- `L.İ.E.` → Progress = Başlanmadı + open problem L.İ.E.; it is **not automatically marked Bloke**.
- `İ.Y.` → open problem İ.Y.; it is **not automatically converted to Kontrol edilemedi**.

This prevents migration from manufacturing facts that are absent from the source workbook.

## Related-discipline row

`MUTFAK FAYANS DOLAP` is migrated with kind `RELATED_DISCIPLINE` and is sorted after electrical work items.

- GK-1…GK-11 and GB-1…GB-3: the source has a purple checkmark. The related work item is `Bitti` and an open problem `TAM.İNŞ. — Aleyhimize tamamlanmış inşaat işi` is attached.
- GK-12…GK-17, GB-4…GB-6, A-1 and A-2: the source has green `L.İ.E.`. The related work is not finished and an open advantage `L.İ.E. — Lehimize inşaat eksiği` is attached.
- Open advantages are displayed separately from open problems. Matrix marker: `A` / `A<n>`.

Existing v1 application databases are upgraded through the v1→v2 Room migration so the built-in Konya project keeps the same semantics after an app update.

## Block parameter

The legacy `YATAY TAVA VAR MI` row is migrated as a block attribute, not a work item:

- `E` → `Evet`
- `H` → `Hayır`

GK values are populated for GK-1…GK-17 and GB values for GB-1…GB-6 exactly where the source carries E/H values. A-1/A-2 are left unset because the source cells are blank.

## Matrix behavior

Open problems are displayed independently from status with `P` / `P<n>` markers; open advantages use `A` / `A<n>`. Therefore a migrated cell can be both `✓` and `P`, preserving the original `✔ E-x` meaning without collapsing problem and completion state.
