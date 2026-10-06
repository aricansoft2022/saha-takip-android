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
Fotoğraf aktif `blockWorkItemId` ile kaydedilir.

Örnek: **GK-9 → Daire Pano** ekranından kamera açılırsa fotoğraf çekildiği anda o imalat kaydına bağlanır. Bunu görüntü analiziyle tahmin etmeyiz; ekran bağlamı zaten kesin bilgiyi taşır.

Her fotoğrafın rapora dahil bayrağı vardır.

## Tooltip
Blok tipi, imalat, problem ve blok parametresi tanımlarında kısa açıklama/tooltip alanı bulunur.
Mobil yüzeyde bilgi ikonuyla açılır.

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

## Yedek / geri yükleme
`.sitepack`:
- Proje verisini
- Fotoğraf dosyalarını
- Format/şema sürüm bilgisini

tek arşivde taşır.

Restore mevcut projeyi ezmez. Kimlikler yeniden eşlenir ve bağımsız proje kopyası oluşturulur.

## Offline-first
Room yerel kaynak-of-truth'tur. Temel saha akışı, rapor üretimi ve yedekleme internet olmadan çalışır.
Cloud/senkronizasyon v0 kapsamı dışındadır.

## V0 geçiş durumu
Kod tarafındaki v0 özellik sözleşmesi tamamlanmıştır. `main` merge öncesindeki kapı gerçek Android cihaz saha/smoke testidir.

Özellikle başarısız olmaması gerekenler:
1. Fotoğrafın doğru imalata bağlanması
2. Uygulama yeniden açıldığında verinin korunması
3. PDF ve `.sitepack` export/restore akışlarının crash üretmemesi
4. Duplicate problem kodunun sessiz veri bozulması yaratmaması

## V0 dışında
Fotoğraf üstüne ok/daire/kutu/metin anotasyonu ve önce/sonra kanıt ilişkisi v0.1 kapsamındadır.
