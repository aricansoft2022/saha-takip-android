# V0 Android Saha / Telefon Smoke Test Checklist

Bu liste `main` merge öncesi gerçek Android cihazda uygulanmalıdır. Derlemenin yeşil olması bu testlerin yerine geçmez.

## 1. Kurulum ve ilk açılış

- [ ] Debug APK temiz cihaza/temiz uygulama alanına kurulur.
- [ ] Uygulama crash olmadan açılır.
- [ ] Mevcut başlangıç projesi görünür.
- [ ] Uygulama kapatılıp açıldığında veriler korunur.
- [ ] Uçak modunda temel akışların tamamı çalışır.

## 2. Proje / blok tipi / blok

- [ ] Yeni boş proje oluşturulur.
- [ ] Yeni blok tipi `TEST` oluşturulur.
- [ ] `TEST-1` ve `TEST-2` blokları oluşturulur.
- [ ] Aynı blok numarası ikinci kez oluşturulamaz.
- [ ] Aynı blok tipi kodu aynı projede ikinci kez oluşturulamaz.

## 3. İmalat kataloğu ve kapsam

- [ ] TEST-1 içinde yeni imalat tanımlanır.
- [ ] “Yalnız bu blok” ile TEST-2'ye yayılmadığı doğrulanır.
- [ ] Başka bir imalat “aynı blok tipi” kapsamıyla eklenir.
- [ ] TEST-1 ve TEST-2'de göründüğü doğrulanır.
- [ ] Sonradan TEST-3 oluşturulur ve blok tipi şablonundaki imalatları miras aldığı doğrulanır.
- [ ] “Projedeki tüm bloklar” kapsamı ayrı blok tiplerinde doğrulanır.

## 4. Durumlar

- [ ] Hızlı durumdan Başlanmadı / Devam / Bitti çalışır.
- [ ] Kusurlu / Ağır kusurlu kalite eksenini değiştirir, ilerleme bilgisini silmez.
- [ ] Kontrol edilemedi / Kabul kontrol eksenini değiştirir.
- [ ] Bloke açılıp kapatılabilir.
- [ ] “Bitti + Kusurlu + Bloke” aynı anda saklanabilir.
- [ ] Uygulama yeniden açılınca durumlar aynı kalır.

## 5. Problem kataloğu

- [ ] Mevcut problem dropdown/arama ile bulunur ve eklenir.
- [ ] On-the-fly yeni kod + tanım oluşturulur.
- [ ] Yeni problem başka imalatta aramayla bulunabilir.
- [ ] Aynı kod tekrar yazıldığında ikinci tanım yaratılmaz.
- [ ] Uygulama mevcut tanımı gösterip “Mevcut problemi kullan” seçeneği sunar.
- [ ] Problem kapatılır, kapanma durumu korunur.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.

## 6. Notlar

- [ ] Serbest not eklenir.
- [ ] Tarih/saat otomatik görünür.
- [ ] İkinci not ilk notu ezmeden kronolojiye eklenir.
- [ ] Rapora dahil kutusu oluştururken ve sonradan değiştirilebilir.

## 7. Fotoğraflar

- [ ] TEST-1 → bir imalat detayından kamera açılır.
- [ ] Fotoğraf çekilir ve imalat ekranına döner.
- [ ] Fotoğraf thumbnail'i görünür.
- [ ] Fotoğraf doğru blok/imalata bağlıdır.
- [ ] Başka blokta görünmez.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.
- [ ] Uygulama yeniden açılınca fotoğraf okunabilir.
- [ ] Dikey ve yatay çekilen fotoğraflar raporda kabul edilebilir yönde görünür.

## 8. Bağımsız blok parametreleri

- [ ] Yeni parametre tanımlanır: `Yatay tava var mı?`.
- [ ] TEST-1 değeri `Evet`, TEST-2 değeri `Hayır` yapılır.
- [ ] Değer sonradan düzenlenir.
- [ ] Tooltip açılır.
- [ ] Aynı parametre tanımı başka blokta tekrar kullanılabilir.

## 9. Matris

- [ ] Proje ekranından İmalat Matrisi açılır.
- [ ] Yatay kaydırma çalışır.
- [ ] Durum sembolleri doğru görünür.
- [ ] Kusurlu/ağır kusurlu/bloke ayrımı görünür.
- [ ] Bir hücreye dokununca doğru imalat detayına gider.

## 10. PDF raporu

- [ ] PDF dışa aktarılır.
- [ ] Android paylaş menüsü açılır.
- [ ] PDF başka bir görüntüleyicide açılır.
- [ ] Proje adı ve üretim tarihi doğru.
- [ ] Blok/imalat durumları doğru.
- [ ] Yalnız rapora dahil problem/not/fotoğraflar görünür.
- [ ] Fotoğraflar bozuk veya ezilmiş görünmez.
- [ ] Uzun raporda sayfa geçişleri ve footer düzgündür.

## 11. XLSX raporu

- [ ] XLSX dışa aktarılır.
- [ ] Excel veya Google Sheets ile açılır.
- [ ] `İmalat Matrisi`, `Problemler`, `Notlar`, `Fotoğraflar` sayfaları vardır.
- [ ] Türkçe karakterler bozulmaz.
- [ ] Matris verileri uygulamayla eşleşir.
- [ ] Rapora dahil olmayan kayıtlar export'a girmez.

## 12. Sitepack yedek / restore

- [ ] Projede birkaç durum, problem, not ve fotoğraf oluşturulur.
- [ ] `.sitepack` yedeği dışa aktarılır.
- [ ] Aynı yedek ana ekrandan içe aktarılır.
- [ ] Yeni bağımsız proje oluşur; mevcut proje ezilmez.
- [ ] Bloklar ve imalat durumları eşleşir.
- [ ] Problem/not kayıtları eşleşir.
- [ ] Fotoğraflar yeni projede açılır.
- [ ] İçe aktarılmış projeden tekrar PDF/XLSX alınabilir.
- [ ] İçe aktarılmış projeden yeniden `.sitepack` alınabilir.

## 13. Dayanıklılık

- [ ] Veri girerken uygulama force-stop edilip yeniden açılır.
- [ ] Son kaydedilmiş veri korunur.
- [ ] Çok sayıda fotoğraf bulunan blokta liste kullanılabilir kalır.
- [ ] Geri tuşu/navigation beklenmeyen kayıt kaybı yaratmaz.
- [ ] Paylaş menüsünden vazgeçilince uygulama normal çalışmaya devam eder.

## V0 geçiş kriteri

Aşağıdaki dört madde başarısızsa v0 `main`e merge edilmez:

1. Fotoğraf doğru imalata bağlanmıyorsa.
2. Veri uygulama yeniden açılınca kayboluyorsa.
3. PDF veya sitepack export/restore crash yaratıyorsa.
4. Aynı problem kodu çakışması sessiz veri bozulmasına yol açıyorsa.
