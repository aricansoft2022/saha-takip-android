# v0 Ürün Sözleşmesi

## Ana saha akışı
Proje → Blok tipi → Blok → İmalat → Durum / Problem / Not / Fotoğraf.

Excel matrisi veri giriş yüzeyi değildir. İleride özet/matris görünümü olarak üretilecektir.

## Bağımsız eksenler
- İlerleme: Başlanmadı / Devam / Bitti
- Kalite: Değerlendirilmedi / Uygun / Kusurlu / Ağır kusurlu
- Kontrol: Kontrol edilmedi / Kontrol edilemedi / Kontrol edildi / Kabul
- Engel: Bloke / değil

Bu ayrım "Bitti + Kusurlu + Bloke" gibi gerçek saha hallerini korur.

## Tanım katalogları
Blok tipi, imalat tanımı ve problem tanımı saha kayıtlarından bağımsızdır.
Yeni problem kodu saha sırasında yaratılabilir; katalogda kalır ve tekrar kullanılır.

Yeni imalat kapsam seçimi sonraki iterasyonda:
- yalnız bu blok,
- aynı tipteki tüm bloklar,
- proje geneli.

## Notlar
Tarih/saat otomatik metadata olarak saklanır. Her notun rapora dahil bayrağı vardır.
Eski notun üstüne yazmak yerine yeni tarihçeli not eklemek varsayılandır.

## Fotoğraflar
Fotoğraf aktif blockWorkItemId ile kaydedilir. GK-9 / Daire Pano detayından açılan kamera otomatik olarak o imalata bağlanır.

## Tooltip
Kısaltma, durum ve tanım açıklamaları tooltip olarak sunulur; tanımların tooltip alanı veri modelindedir.

## Offline-first
Room yerel kaynaktır. Cloud/senkronizasyon v0 kapsamı dışındadır.
