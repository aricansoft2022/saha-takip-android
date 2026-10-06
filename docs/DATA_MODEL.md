# Veri Modeli v0

## Tanımlar
Project, BlockType, Block, WorkItemDefinition, ProblemDefinition.

## Uygulama kayıtları
- BlockWorkItem: imalatın belirli bloktaki örneği
- ProblemRecord: belirli BlockWorkItem üzerindeki problem örneği
- Note: tarihçeli serbest not
- Photo: bağlama bağlı yerel fotoğraf
- AuditEvent: durum ve önemli saha aksiyonlarının değişmez günlüğü

ProblemDefinition katalog kaydıdır; ProblemRecord saha olayıdır.

Rapor üreticisi veri yaratmaz; mevcut saha verisini filtreleyip sunar.
