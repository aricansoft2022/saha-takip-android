# V0 Android Saha / Telefon Smoke Test Checklist

Bu liste `main` merge öncesi gerçek Android cihazda uygulanmalıdır. Derlemenin yeşil olması bu testlerin yerine geçmez.

## 1. Kurulum ve ilk açılış

- [ ] Debug APK temiz cihaza/temiz uygulama alanına kurulur.
- [ ] Uygulama crash olmadan açılır.
- [ ] Mevcut başlangıç projesi görünür.
- [ ] Uygulama kapatılıp açıldığında veriler korunur.
- [ ] Uçak modunda temel akışların tamamı çalışır.

## 2. Anasayfa hızlı durum filtresi

- [ ] Hızlı filtre çipleri: Tümü / Açık problem / Açık avantaj / Kusurlu / Bloke / Devam / Bitti görünür.
- [ ] Her çipte eşleşen proje sayısı görünür.
- [ ] Projedeki bir imalatın durumu değişince anasayfa sayımları otomatik güncellenir.
- [ ] Açık problem filtresi yalnız açık problemi olan projeleri gösterir.
- [ ] Açık avantaj filtresi yalnız açık avantajı olan projeleri gösterir.
- [ ] Kusurlu / Bloke / Devam / Bitti filtreleri doğru projeleri gösterir.
- [ ] Seçili filtrede sonuç yoksa boş durum mesajı görünür.
- [ ] Tümü filtresi tüm proje kartlarını geri getirir.
- [ ] Proje kartındaki kısa durum özeti gerçek kayıtlarla eşleşir.

## 3. Proje / blok tipi / blok

- [ ] Yeni boş proje oluşturulur.
- [ ] Yeni blok tipi `TEST` oluşturulur.
- [ ] `TEST-1` ve `TEST-2` blokları oluşturulur.
- [ ] Aynı blok numarası ikinci kez oluşturulamaz.
- [ ] Aynı blok tipi kodu aynı projede ikinci kez oluşturulamaz.

## 4. İmalat kataloğu ve kapsam

- [ ] TEST-1 içinde yeni imalat tanımlanır.
- [ ] “Yalnız bu blok” ile TEST-2'ye yayılmadığı doğrulanır.
- [ ] Başka bir imalat “aynı blok tipi” kapsamıyla eklenir.
- [ ] TEST-1 ve TEST-2'de göründüğü doğrulanır.
- [ ] Sonradan TEST-3 oluşturulur ve blok tipi şablonundaki imalatları miras aldığı doğrulanır.
- [ ] “Projedeki tüm bloklar” kapsamı ayrı blok tiplerinde doğrulanır.

## 5. Alakadar başka disiplin + avantaj

- [ ] Yeni imalat oluştururken “Alakadar başka disiplin kalemi” işaretlenebilir.
- [ ] Başka disiplin kalemleri blok listesinin sonunda ve farklı zeminle görünür.
- [ ] Bir başka-disiplin kalemine açık problem eklenebilir.
- [ ] Aynı kaleme açık avantaj eklenebilir.
- [ ] Problem ve avantaj birbirini silmez; ikisi aynı anda açık kalabilir.
- [ ] Avantaj kapatılabilir ve kapanma durumu korunur.
- [ ] Matris açık problem için P, açık avantaj için A gösterir.
- [ ] “Açık avantaj” ve “Başka disiplin” matris filtreleri çalışır.
- [ ] Konya seed'de Mutfak Fayans / Dolap satırı elektrik kalemlerinin sonunda görünür.
- [ ] Konya seed'de L.İ.E. kayıtları problem değil avantaj olarak görünür.
- [ ] Eski v1 veritabanından güncellemede bu sınıflandırma korunur.

## 6. Durumlar

- [ ] Hızlı durumdan Başlanmadı / Devam / Bitti çalışır.
- [ ] Kusurlu / Ağır kusurlu kalite eksenini değiştirir, ilerleme bilgisini silmez.
- [ ] Kontrol edilemedi / Kabul kontrol eksenini değiştirir.
- [ ] Bloke açılıp kapatılabilir.
- [ ] “Bitti + Kusurlu + Bloke” aynı anda saklanabilir.
- [ ] Uygulama yeniden açılınca durumlar aynı kalır.

## 7. Problem kataloğu

- [ ] Mevcut problem dropdown/arama ile bulunur ve eklenir.
- [ ] On-the-fly yeni kod + tanım oluşturulur.
- [ ] Yeni problem başka imalatta aramayla bulunabilir.
- [ ] Aynı kod tekrar yazıldığında ikinci tanım yaratılmaz.
- [ ] Uygulama mevcut tanımı gösterip “Mevcut problemi kullan” seçeneği sunar.
- [ ] Problem kapatılır, kapanma durumu korunur.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.

## 8. Notlar

- [ ] Serbest not eklenir.
- [ ] Tarih/saat otomatik görünür.
- [ ] İkinci not ilk notu ezmeden kronolojiye eklenir.
- [ ] Rapora dahil kutusu oluştururken ve sonradan değiştirilebilir.

## 9. Fotoğraflar

- [ ] TEST-1 → bir imalat detayından kamera açılır.
- [ ] Fotoğraf çekilir ve imalat ekranına döner.
- [ ] Fotoğraf thumbnail'i görünür.
- [ ] Fotoğraf doğru blok/imalata bağlıdır.
- [ ] Başka blokta görünmez.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.
- [ ] Uygulama yeniden açılınca fotoğraf okunabilir.
- [ ] Dikey ve yatay çekilen fotoğraflar raporda kabul edilebilir yönde görünür.

## 10. Bağımsız blok parametreleri

- [ ] Yeni parametre tanımlanır: `Yatay tava var mı?`.
- [ ] TEST-1 değeri `Evet`, TEST-2 değeri `Hayır` yapılır.
- [ ] Değer sonradan düzenlenir.
- [ ] Tooltip açılır.
- [ ] Aynı parametre tanımı başka blokta tekrar kullanılabilir.

## 11. Matris

- [ ] Proje ekranından İmalat Matrisi açılır.
- [ ] Yatay kaydırma çalışır.
- [ ] Durum sembolleri doğru görünür.
- [ ] Kusurlu/ağır kusurlu/bloke ayrımı görünür.
- [ ] Açık problem bulunan hücrelerde P / P<n> işareti görünür.
- [ ] Blok tipi filtresi yalnız seçilen tipin bloklarını gösterir.
- [ ] Açık problem / Kusurlu / Bloke / Devam / Bitti filtreleri doğru kayıtları gösterir.
- [ ] Filtre sonucu yoksa boş durum mesajı gösterilir.
- [ ] Filtre temizlenince tüm matris geri gelir.
- [ ] Bir hücreye dokununca doğru imalat detayına gider.

## 12. PDF raporu

- [ ] PDF dışa aktarılır.
- [ ] Android paylaş menüsü açılır.
- [ ] PDF başka bir görüntüleyicide açılır.
- [ ] Proje adı ve üretim tarihi doğru.
- [ ] Blok/imalat durumları doğru.
- [ ] Yalnız rapora dahil problem/not/fotoğraflar görünür.
- [ ] Fotoğraflar bozuk veya ezilmiş görünmez.
- [ ] Uzun raporda sayfa geçişleri ve footer düzgündür.

## 13. XLSX raporu

- [ ] XLSX dışa aktarılır.
- [ ] Excel veya Google Sheets ile açılır.
- [ ] `İmalat Matrisi`, `Problemler`, `Avantajlar`, `Notlar`, `Fotoğraflar` sayfaları vardır.
- [ ] Türkçe karakterler bozulmaz.
- [ ] Matris verileri uygulamayla eşleşir.
- [ ] Rapora dahil olmayan kayıtlar export'a girmez.

## 14. GKTE export / Android ↔ Android taşıma

- [ ] Projede birkaç durum, problem, avantaj, not ve fotoğraf oluşturulur.
- [ ] Proje menüsünden `.gkte` dosyası üretilir.
- [ ] Dosya adı gerçekten `.gkte` ile biter; `.zip` veya `.sitepack` olmaz.
- [ ] WhatsApp kurulu cihazda paylaş komutu WhatsApp belge gönderimine gider.
- [ ] WhatsApp olmayan cihazda Android paylaşım menüsüne düşer.
- [ ] GKTE WhatsApp ile ikinci Android telefona gönderilir.
- [ ] İkinci telefonda dosya indirildikten sonra dosyaya dokunulur.
- [ ] Saha Takip dosyayı açabilecek uygulama olarak görünür; tek uygun handler ise doğrudan açılır.
- [ ] GKTE içe aktarılır ve yeni projenin ekranı otomatik açılır.
- [ ] Yeni bağımsız proje oluşur; mevcut proje ezilmez.
- [ ] Bloklar ve imalat durumları eşleşir.
- [ ] Problem/avantaj/not kayıtları eşleşir.
- [ ] Fotoğraflar yeni projede açılır.
- [ ] İçe aktarılmış projeden tekrar PDF/XLSX alınabilir.
- [ ] İçe aktarılmış projeden yeniden `.gkte` alınabilir.
- [ ] Yeni GKTE arşivinde `manifest.json`, `data.json` ve varsa `photos/` bulunur.
- [ ] `data.json` içindeki fotoğraf `localUri` değerleri Android cihaz URI'sına bağımlı değildir.
- [ ] Eski `.sitepack` dosyası geriye dönük olarak hâlâ içe aktarılabilir.

## 15. Dayanıklılık

- [ ] Veri girerken uygulama force-stop edilip yeniden açılır.
- [ ] Son kaydedilmiş veri korunur.
- [ ] Çok sayıda fotoğraf bulunan blokta liste kullanılabilir kalır.
- [ ] Geri tuşu/navigation beklenmeyen kayıt kaybı yaratmaz.
- [ ] Paylaş menüsünden vazgeçilince uygulama normal çalışmaya devam eder.

## V0 geçiş kriteri

Aşağıdaki dört madde başarısızsa v0 `main`e merge edilmez:

1. Fotoğraf doğru imalata bağlanmıyorsa.
2. Veri uygulama yeniden açılınca kayboluyorsa.
3. PDF veya GKTE export/import crash yaratıyorsa.
4. Aynı problem kodu çakışması sessiz veri bozulmasına yol açıyorsa.
