# v0 Ürün Sözleşmesi

## Amaç
Şantiyede blok ve imalat bazlı ilerlemeyi; problem, not, fotoğraf ve bağımsız kontrol durumlarıyla birlikte internet gerektirmeden kaydetmek.

Ana saha akışı:

**Proje → Blok tipi → Blok → İmalat → Durum / Problem / Avantaj / Eksik / Not / Fotoğraf**

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
- `Başlanmadı` durumundaki alakadar başka disiplin kalemi lehimize durum kabul edilir; blok imalat listesinde yeşil arka plan ve `LEHİMİZE BAŞLANMADI` etiketiyle gösterilir.
- Açık problem veya aktif eksik varsa uyarı rengi bu yeşil avantaj renginden önceliklidir.
- Açık problem ve açık avantaj aynı kalem üzerinde bağımsız kayıtlar olarak bulunabilir.
- Matris üzerinde `P` açık problemi, `A` açık avantajı gösterir.

## İmalat eksik takip yönetimi
Eksik, problem/avantajdan ve imalatın ilerleme/kalite/kontrol eksenlerinden ayrı bir yönetim kaydıdır.

Her eksik şu alanları taşıyabilir:
- Eksik / yapılacak iş başlığı
- Açıklama
- Kat
- Mahal / daire / birim numarası
- Mahal / daire / birim adı
- Sorumlu kişi / ekip
- Hedef tarih
- Öncelik: Normal / Yüksek / Kritik

Başlık, açıklama, konum, sorumlu, hedef tarih ve öncelik sonradan düzenlenebilir; düzenleme mevcut durum yaşam döngüsünü sıfırlamaz.
- Rapora dahil bayrağı
- Sınırsız kanıt fotoğrafı

Yaşam döngüsü:
**Açık → Gideriliyor → Giderildi → Kontrol edildi**

`Giderildi` nihai kapanış değildir. Eksik ancak `Kontrol edildi` olduğunda aktif eksik listesinden düşer. Durum gerektiğinde geri alınabilir.

Eksikler iki yüzeyden yönetilir:
1. İmalat detayında o imalata ait eksikler.
2. Proje seviyesindeki **İmalat Eksik Takibi** ekranında tüm blok/imalat eksikleri; durum, öncelik ve metin aramasıyla.

Anasayfada `Açık eksik` hızlı filtresi vardır. Matris hücresinde aktif eksik `E` / `E<n>` ile gösterilir.

## Notlar
Tarih/saat otomatik metadata olarak saklanır. Her notun rapora dahil bayrağı vardır.
Eski notun üstüne yazmak yerine yeni tarihçeli not eklemek varsayılandır.

## Fotoğraflar
Fotoğraflar iki seviyede tutulur:

1. **Genel imalat fotoğrafı** — aktif `blockWorkItemId` ile bağlanır.
2. **Problem/avantaj kanıt fotoğrafı** — hem aktif `blockWorkItemId` hem de ilgili `problemRecordId` ile bağlanır.
3. **Eksik kanıt fotoğrafı** — hem aktif `blockWorkItemId` hem de ilgili `deficiencyId` ile bağlanır.

Her problem ve her avantaj kaydına **sınırsız sayıda kanıt fotoğrafı** eklenebilir. Problem/avantaj kapatılmış olsa bile mevcut kanıt fotoğrafları korunur ve yeni kanıt eklenebilir.

Örnek: **GK-9 → Daire Pano → E-1 problemi** içinden çekilen fotoğraf doğrudan E-1 saha kaydına bağlanır; başka problem veya avantaja karışmaz.

Her fotoğrafın bağımsız “Rapora dahil” bayrağı vardır. PDF'de bulguya bağlı fotoğraflar ilgili problem/avantajın hemen altında gösterilir; XLSX Fotoğraflar sayfası fotoğrafın hangi bulguya bağlı olduğunu belirtir.

## Tooltip
Blok tipi, imalat, problem ve blok parametresi tanımlarında kısa açıklama/tooltip alanı bulunur.
Mobil yüzeyde bilgi ikonuyla açılır.

## Anasayfa hızlı durum filtresi
Anasayfa proje seçim yüzeyidir. Burada **imalat seçimi yapılmaz**.

Proje kartları aşağıdaki proje-geneli canlı durum agregasyonlarıyla filtrelenebilir:

- Tümü
- Açık problem
- Açık eksik
- Açık avantaj
- Kusurlu
- Bloke
- Devam
- Bitti

Her proje kartındaki kısa özet proje içindeki gerçek saha kayıtlarından hesaplanır.

## Proje içi imalat filtresi
İmalat filtresi yalnız proje açıldıktan sonra **Bloklar** ekranında bulunur.

- Tüm imalatlar veya belirli bir imalat seçilebilir.
- Dropdown aranabilir ve Türkçe büyük/küçük harf duyarsızdır.
- Her imalat kaç blokta bulunduğunu gösterir.
- Belirli imalat seçilince yalnız o imalatın bulunduğu bloklar listelenir.
- Seçili imalat için Tümü / Açık problem / Açık eksik / Açık avantaj / Kusurlu / Bloke / Devam / Bitti durum filtreleri birlikte çalışır.
- Örneğin `Daire Pano + Açık eksik`, yalnız Daire Pano imalatında aktif eksik bulunan blokları gösterir.
- Filtreli blok kartı seçili imalatın durum özetini gösterir.
- Belirli imalat seçiliyken blok kartına dokunmak doğrudan o bloktaki seçili imalat detayını açar.
- `Tüm imalatlar` seçildiğinde normal blok listesine dönülür.

## İmalat matrisi
Matris özet ve drill-down ekranıdır.

- Satır: imalat
- Sütun: blok
- İlerleme sembolü
- Kusur / bloke / açık problem / aktif eksik işaretleri
- Blok tipi filtresi
- Açık problem / açık eksik / açık avantaj / başka disiplin / kusurlu / bloke / devam / bitti filtreleri
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
- Eksikler
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
