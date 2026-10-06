# Saha Takip Android

Şantiye imalat ilerlemesi, problem kayıtları, tarihçeli notlar, bağlama bağlı fotoğraflar ve rapor/yedek üretimi için offline-first Android uygulaması.

> Geliştirme dalı: `v0-foundation`  
> Durum: v0 saha testi öncesi alpha

## Teknoloji

- Native Android
- Kotlin
- Jetpack Compose
- Room
- Android `PdfDocument`
- Bağımlılıksız Office Open XML (XLSX) üretimi
- FileProvider tabanlı fotoğraf/rapor/yedek paylaşımı
- GitHub Actions ile her committe debug build

## V0 çekirdeği

- Proje oluşturma
- Blok tipi oluşturma (GK, GB, A...)
- Blok oluşturma ve blok tipi imalat şablonunu miras alma
- İmalat kataloğu ve on-the-fly yeni imalat
- İmalatı yalnız bloğa / blok tipine / proje geneline yayma
- İlerleme, kalite, kontrol ve bloke durumlarını bağımsız tutma
- Tek dokunuşluk hızlı durumlar
- Elektrik / alakadar başka disiplin kalemi ayrımı
- Başka disiplin kalemlerini listenin sonunda ayrı renkle gösterme
- Problem + avantaj türünde tekrar kullanılabilir bulgu kataloğu
- Açık problem ve açık avantajın bağımsız yaşam döngüsü
- Duplicate bulgu kodu koruması
- Tarihçeli notlar
- Bağlama otomatik bağlı fotoğraf çekimi
- Not/problem/fotoğraf için sonradan değiştirilebilir “Rapora dahil” seçimi
- Tooltip / kısa açıklamalar
- Proje bazlı özel blok parametreleri
- Blok × imalat matrisi
- Fotoğraflı PDF raporu
- Analitik XLSX raporu
- `.sitepack` proje yedekleme ve güvenli içe aktarma
- Offline çalışma

## Temel veri ilkesi

Tek bir “durum” kolonu yoktur.

- İlerleme: Başlanmadı / Devam / Bitti
- Kalite: Değerlendirilmedi / Uygun / Kusurlu / Ağır kusurlu
- Kontrol: Kontrol edilmedi / Kontrol edilemedi / Kontrol edildi / Kabul
- Engel: Bloke / değil

Böylece “Bitti + Ağır kusurlu + Bloke” gibi gerçek saha halleri kaybolmaz.

## Fotoğraf bağlamı

Kamera doğrudan bir imalat detay ekranından açılır. Bu nedenle fotoğraf çekildiği anda aktif `blockWorkItemId` kaydını devralır. Fotoğrafın hangi blok/imalata ait olduğunu görüntü analiziyle tahmin etmeye çalışmayız.

## Raporlar

**PDF:** durumlar + rapora seçilmiş problem/not/fotoğraf kanıtları.  
**XLSX:** İmalat Matrisi, Problemler, Avantajlar, Notlar ve Fotoğraflar sayfaları.

## Yedek

`.sitepack` arşivi proje verisini ve fotoğrafları birlikte taşır. İçe aktarım mevcut projeyi ezmez; kimlikler yeniden eşlenerek bağımsız bir proje kopyası oluşturulur.

## Derleme

```bash
./gradlew :app:assembleDebug
```

CI başarılı olduğunda `saha-takip-debug` adlı APK artifact'i 7 gün saklanır.

## Dokümantasyon

- `docs/V0_PRODUCT.md`
- `docs/DATA_MODEL.md`
- `docs/FIELD_TEST_CHECKLIST.md`

## V0 dışında

Fotoğraf üstüne ok/daire/kutu/metin anotasyonu ve önce/sonra kanıt ilişkisi v0.1'e bırakılmıştır.
