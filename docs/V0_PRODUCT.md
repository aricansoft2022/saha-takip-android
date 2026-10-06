# v0 Ürün Sözleşmesi

## Amaç
Şantiyede blok ve imalat bazlı ilerlemeyi; problem, not, fotoğraf ve bağımsız kontrol durumlarıyla birlikte internet gerektirmeden kaydetmek.

Ana saha akışı:

**Proje → Blok tipi → Blok → İmalat → Durum / Problem / Not / Fotoğraf**

Excel matrisi ana veri giriş yüzeyi değildir. Özet, filtreleme ve drill-down yüzeyidir.

## Bağımsız eksenler
- İlerleme: Başlanmadı / Devam / Bitti
- Kalite: Değerlendirilmedi / Uygun / Kusurlu / Ağır kusurlu
- Kontrol: Kontrol edilmedi / Kontrol edilemedi / Kontrol edildi / Kabul
- Engel: Bloke / değil

Bu ayrım "Bitti + Kusurlu + Bloke" gibi gerçek saha hallerini korur.

Problem kaydı da bu eksenlerden bağımsızdır. Bir problem kodu kalite, kontrol veya bloke durumunu otomatik olarak uydurmaz.

## Paralel tanım yapıları
Saha verisi tek bir hiyerarşiye sıkıştırılmaz.

- Blok tipi: GK, GB, A...
- Blok: GK-1, GK-9...
- İmalat/takip kalemi: Daire Pano, Kolon Hattı, Mutfak Fayans / Dolap...
- Kalem türü: Elektrik imalatı / Alakadar başka disiplin kalemi
- Bulgu tanımı: Problem veya Avantaj; tekrar kullanılabilir kod + açıklama
- Blok parametresi: "Yatay tava var mı?" gibi imalattan bağımsız alanlar

Yeni blok, blok tipinin imalat şablonunu miras alır.

## İmalat kapsam yayılımı
Bir imalat tanımı saha sırasında mevcut katalogdan seçilebilir veya on-the-fly oluşturulabilir.

Kapsam:
- Yalnız bu blok
- Aynı blok tipindeki tüm bloklar
- Projedeki tüm bloklar

Blok tipi veya proje kapsamındaki imalatlar yeni oluşturulan bloklara şablon olarak da miras kalır.

## Problem ve avantaj kataloğu
Tanım ile saha kaydı ayrı kavramlardır. Bulgu türü de bağımsızdır:

- Problem
- Avantaj

Her ikisi de Açık / Kapalı yaşam döngüsüne sahiptir.
Kodlar proje içinde tek anlam taşır; aynı kod sessizce başka türde yeniden yaratılamaz.

Her saha problem/avantaj kaydı katalog tanımından bağımsız olarak şu olay-spesifik alanları taşıyabilir:
- Bu kayda özel tanım
- Kat
- Mahal / daire / birim numarası
- Mahal / daire / birim adı

Örneğin katalogdaki `E-1 — Kolon sigortası ve KAKR yok` aynı kalırken saha kaydı `3. kat / Daire 12 / Antre / pano önü` bağlamını ayrıca saklayabilir.
Her problem/avantaj kaydı rapora dahil/hariç bırakılabilir.

Avantaj, "problem yok" demek değildir. Örneğin başka disiplinin henüz tamamlamadığı ve elektrik müdahalesi için alan bırakan bir iş açık avantaj olarak tutulabilir.

## Alakadar başka disiplin kalemi
Elektrik işi olmayan fakat elektrik işinin yapılabilirliğini, maliyetini veya düzeltme yöntemini etkileyen işler ayrı türde tutulur.

Örnek: `Mutfak Fayans / Dolap`.

- Elektrik kalemlerinden sonra listelenir.
- Ayrı renkle gösterilir.
- Normal ilerleme bilgisi saklanabilir.
- Açık problem ve açık avantaj aynı kalem üzerinde bağımsız kayıtlar olarak bulunabilir.
- Matris üzerinde `P` açık problemi, `A` açık avantajı gösterir.

## Notlar
Tarih/saat otomatik metadata olarak saklanır. Her notun rapora dahil bayrağı vardır.
Eski notun üstüne yazmak yerine yeni tarihçeli not eklemek varsayılandır.

## Fotoğraflar
Fotoğraflar iki seviyede tutulur:

1. **Genel imalat fotoğrafı** — aktif `blockWorkItemId` ile bağlanır.
2. **Problem/avantaj kanıt fotoğrafı** — hem aktif `blockWorkItemId` hem de ilgili `problemRecordId` ile bağlanır.

Her problem ve her avantaj kaydına **sınırsız sayıda kanıt fotoğrafı** eklenebilir. Problem/avantaj kapatılmış olsa bile mevcut kanıt fotoğrafları korunur ve yeni kanıt eklenebilir.

Örnek: **GK-9 → Daire Pano → E-1 problemi** içinden çekilen fotoğraf doğrudan E-1 saha kaydına bağlanır; başka problem veya avantaja karışmaz.

Her fotoğrafın bağımsız “Rapora dahil” bayrağı vardır. PDF'de bulguya bağlı fotoğraflar ilgili problem/avantajın hemen altında gösterilir; XLSX Fotoğraflar sayfası fotoğrafın hangi bulguya bağlı olduğunu belirtir.

## Tooltip
Blok tipi, imalat, problem ve blok parametresi tanımlarında kısa açıklama/tooltip alanı bulunur.
Mobil yüzeyde bilgi ikonuyla açılır.

## Anasayfa hızlı durum filtresi
Proje listesi aşağıdaki canlı durum agregasyonlarıyla tek dokunuşla filtrelenebilir:

- Tümü
- Açık problem
- Açık avantaj
- Kusurlu
- Bloke
- Devam
- Bitti

Her proje kartında mevcut problem, avantaj, kusur, bloke, devam ve bitmiş imalat sayılarının kısa özeti görünür. Filtreler proje adından veya statik etiketten değil, proje içindeki gerçek saha kayıtlarından hesaplanır.

## İmalat matrisi
Matris özet ve drill-down ekranıdır.

- Satır: imalat
- Sütun: blok
- İlerleme sembolü
- Kusur / bloke / açık problem işaretleri
- Blok tipi filtresi
- Açık problem / açık avantaj / başka disiplin / kusurlu / bloke / devam / bitti filtreleri
- Hücreden doğrudan imalat detayına geçiş

## Raporlar
### PDF
- Proje ve durum özeti
- Blok / imalat durumları
- Rapora seçilmiş problem ve notlar
- Rapora seçilmiş gerçek saha fotoğrafları
- Android paylaş menüsü

### XLSX
- İmalat Matrisi
- Problemler
- Avantajlar
- Notlar
- Fotoğraflar

Rapor katmanı yeni saha gerçeği üretmez; Room verisini filtreleyip sunar.

## GKTE proje taşıma formatı
Yeni dışa aktarma biçimi `.gkte`dir.

GKTE:
- proje verisini,
- fotoğraf dosyalarını,
- format ve veri şeması sürüm bilgisini,
- platform bağımsız manifest metadata'sını

tek dosyada taşır.

Format ham Room/SQLite veritabanı değildir. Bu bilinçli bir karardır: aynı dosya gelecekteki Windows masaüstü uygulaması tarafından Android bağımlılığı olmadan okunup yazılabilmelidir.

Android:
- Proje ekranından GKTE üretir.
- WhatsApp kuruluysa belgeyi doğrudan WhatsApp paylaşımına verir; yoksa sistem paylaşım menüsüne düşer.
- `.gkte` dosyasını ACTION_VIEW ile açabilir.
- İçe aktarılan projeyi mevcut projeyi ezmeden bağımsız kopya olarak açar.

Eski `.sitepack` yalnız geriye dönük reader uyumluluğu için içe aktarılabilir. Yeni export üretilmez.

Normatif format: `docs/GKTE_FORMAT.md`.

## Offline-first
Room yerel kaynak-of-truth'tur. Temel saha akışı, rapor üretimi ve yedekleme internet olmadan çalışır.
Cloud/senkronizasyon v0 kapsamı dışındadır.

## V0 geçiş durumu
Kod tarafındaki v0 özellik sözleşmesi tamamlanmıştır. `main` merge öncesindeki kapı gerçek Android cihaz saha/smoke testidir.

Özellikle başarısız olmaması gerekenler:
1. Fotoğrafın doğru imalata bağlanması
2. Uygulama yeniden açıldığında verinin korunması
3. PDF ve `.gkte` export/import akışlarının crash üretmemesi
4. Duplicate problem kodunun sessiz veri bozulması yaratmaması

## V0 dışında
Fotoğraf üstüne ok/daire/kutu/metin anotasyonu ve önce/sonra kanıt ilişkisi v0.1 kapsamındadır.
