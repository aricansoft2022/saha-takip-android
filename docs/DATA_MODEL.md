# Veri Modeli v0

## Tanımlar
Project, BlockType, Block, WorkItemDefinition, ProblemDefinition.

## Uygulama kayıtları
- BlockWorkItem: imalatın belirli bloktaki örneği
- ProblemRecord: belirli BlockWorkItem üzerindeki problem/avantaj saha olayı
- Deficiency: belirli BlockWorkItem üzerindeki imalat eksiği yönetim kaydı
- Note: tarihçeli serbest not
- Photo: bağlama bağlı yerel fotoğraf
- AuditEvent: durum ve önemli saha aksiyonlarının günlüğü

ProblemDefinition katalog kaydıdır; ProblemRecord saha olayıdır.

## Deficiency

Deficiency ayrı bir yaşam döngüsüne sahiptir:

`OPEN → IN_PROGRESS → FIXED → VERIFIED`

- `OPEN`: Açık
- `IN_PROGRESS`: Gideriliyor
- `FIXED`: Giderildi; saha kontrolü bekler
- `VERIFIED`: Kontrol edildi; aktif eksik sayısından düşer

Alanlar:
- `blockWorkItemId`
- başlık ve açıklama
- kat
- mahal/daire/birim no
- mahal/daire/birim adı
- sorumlu kişi/ekip
- hedef tarih
- öncelik: NORMAL / HIGH / CRITICAL
- rapora dahil bayrağı
- createdAt / updatedAt

`FIXED` kapalı sayılmaz. Yönetim açısından kapanış `VERIFIED` durumudur.

## Fotoğraf ilişkileri

Photo daima `blockWorkItemId` taşır. Ayrıca iki opsiyonel bağdan en fazla biri kullanılabilir:

- `problemRecordId`: problem/avantaj kanıtı
- `deficiencyId`: imalat eksiği kanıtı

İkisi de null ise fotoğraf genel imalat fotoğrafıdır.

Rapor üreticisi veri yaratmaz; mevcut saha verisini filtreleyip sunar.
