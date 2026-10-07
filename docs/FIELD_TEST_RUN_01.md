# Field Test Run 01 — Kurulum + Veri Kalıcılığı + Temel CRUD

Bu dilim kısa tutulur. Amaç uygulamanın gerçek Android cihazda temel saha verisini güvenle oluşturup sakladığını doğrulamaktır.

## Başlangıç

Test edilecek kod baseline: `798fb8ef47b822963bec131d027a76d89e8108c8`

CI referansı: Android CI #285 — success.

## Test sırası

- [ ] Debug APK temiz uygulama alanına kurulur.
- [ ] Uygulama crash olmadan açılır.
- [ ] Başlangıç projesi görünür.
- [ ] Uçak modu açılır; uygulama temel ekranlarda çalışmaya devam eder.
- [ ] Yeni boş proje oluşturulur: `TEST SAHA`.
- [ ] Blok tipi oluşturulur: kod `TEST`, ad `Test Tip`.
- [ ] `TEST-1` ve `TEST-2` blokları oluşturulur.
- [ ] Aynı blok tipi kodu tekrar oluşturulamaz.
- [ ] Aynı blok numarası tekrar oluşturulamaz.
- [ ] TEST-1 içine `Daire Pano Test` imalatı “yalnız bu blok” kapsamında eklenir.
- [ ] TEST-2'de bu imalat görünmez.
- [ ] Başka bir imalat “aynı blok tipi” kapsamında eklenir.
- [ ] Bu ikinci imalat TEST-1 ve TEST-2'de görünür.
- [ ] Bir imalatın ilerleme durumu `Devam` yapılır.
- [ ] Aynı imalatta kalite `Kusurlu` yapılır; ilerleme durumu silinmez.
- [ ] Aynı imalat `Bloke` yapılır; önceki eksenler korunur.
- [ ] Bir serbest not eklenir.
- [ ] Uygulama tamamen kapatılır.
- [ ] Uygulama yeniden açılır.
- [ ] TEST SAHA projesi, bloklar, imalatlar, durumlar ve not aynen korunur.

## Geçiş kriteri

Aşağıdakilerden biri başarısızsa Run 01 FAIL sayılır ve sonraki run'a geçilmez:

1. Uygulama açılışta crash ederse.
2. Yeni proje/blok/imalat oluşturulamıyorsa.
3. Duplicate blok tipi veya blok sessizce ikinci kayıt oluşturuyorsa.
4. Durum eksenlerinden biri diğerini yanlışlıkla siliyorsa.
5. Uygulama yeniden açıldığında kayıtlı veri kayboluyorsa.

## Sonuç kaydı

Run sonunda Issue #7'ye yalnız şu formatta sonuç yazılır:

- Device / Android:
- APK baseline:
- PASS / FAIL:
- Başarısız adım:
- Gözlenen davranış:
- Ekran görüntüsü / video: varsa
