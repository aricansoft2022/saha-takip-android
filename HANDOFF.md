# HANDOFF

## Aktif durum

- Branch: `v0-foundation`
- PR: #6 — draft
- Son doğrulanmış kod baseline: `798fb8ef47b822963bec131d027a76d89e8108c8`
- CI: Android CI #285 — SUCCESS
  - unit tests: success
  - Android lint: success
  - assembleDebug: success
  - debug APK artifact: success
  - Room schema artifact: success
- Merge kapısı: Issue #7 — gerçek Android cihaz saha / smoke testi

## Bu noktaya kadar tamamlanan kritik düzeltmeler

1. Work-item duplicate/REPLACE kaynaklı ilişki kopması engellendi.
2. Room ilişkilerine foreign key koruması eklendi.
3. v5 → v6 migration gerçek örnek veriyle test edildi ve CI'da geçti.
4. Fotoğrafın aynı anda problem/avantaj ve eksik kaydına bağlanması engellendi.
5. Problem/eksik fotoğraf bağlam uyuşmazlıkları repository + DB seviyesinde engellendi.
6. GKTE export/import fail-fast ve bütünlük kontrolleri güçlendirildi.
7. GKTE path traversal, duplicate entry, entry-count ve expanded-size sınırları eklendi.
8. ACTION_VIEW ile aynı GKTE'nin Activity recreation nedeniyle ikinci kez içe alınması engellendi.
9. Import DB yazımı transaction içinde; başarısız importta geri yüklenen fotoğraf dizini temizleniyor.
10. Fotoğraf thumbnail yüklemesi downsample edildi; çoklu fotoğraf şeritleri lazy hale getirildi.
11. Proje içi imalat filtresi ve matris isim yerine `workItemDefinitionId` ile eşleşiyor.
12. Hedef tarih gerçek takvim tarihi olarak doğrulanıyor.
13. Android otomatik backup bilinçli olarak kapatıldı.
14. CI artık test + lint + assembleDebug çalıştırıyor.

## Bilerek ertelenen bakım işi

Room schema JSON dosyasını ayrıca repoya commit etmek runtime doğruluğu için gerekli değil. CI şemayı üretip artifact olarak doğruluyor. Kullanıcının isteği doğrultusunda bu aşamada bakım/optimizasyon işi olarak ertelendi.

## Değişmez ürün kararları

- Anasayfada imalat dropdown yoktur; yalnız hızlı durum filtreleri vardır.
- İmalat filtresi projeye girdikten sonra görünür.
- Problem / Avantaj / İmalat Eksikleri aktif sayaçları 0 dahil her zaman görünür.
- Blok imalat listesindeki her kartta bilgi tooltip'i bulunur.
- RELATED_DISCIPLINE + NOT_STARTED, açık problem/aktif eksik yoksa yeşil avantaj kartıdır.
- Gerçek cihaz doğrulaması bitmeden PR #6 main'e merge edilmez.

## Sonraki tek hedef

`docs/FIELD_TEST_RUN_01.md` uygulanacak.

Run 01 geçmeden yeni özellik, kapsam genişletme veya mimari refactor yapılmayacak.

Run 01 sonrasında:
- Run 02: fotoğraflar + PDF/XLSX
- Run 03: GKTE Android → Android
- Sonra Issue #7 kapısı değerlendirilir.
