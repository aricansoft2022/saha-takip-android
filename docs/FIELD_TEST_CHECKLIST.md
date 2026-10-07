# V0 Android Saha / Telefon Smoke Test Checklist

Bu liste `main` merge öncesi gerçek Android cihazda uygulanmalıdır. Derlemenin yeşil olması bu testlerin yerine geçmez.

## 1. Kurulum ve ilk açılış

- [ ] Debug APK temiz cihaza/temiz uygulama alanına kurulur.
- [ ] Uygulama crash olmadan açılır.
- [ ] Mevcut başlangıç projesi görünür.
- [ ] Uygulama kapatılıp açıldığında veriler korunur.
- [ ] Uçak modunda temel akışların tamamı çalışır.

## 2. Anasayfa hızlı durum filtresi

- [ ] Anasayfada imalat dropdown'ı YOKTUR.
- [ ] Hızlı filtre çipleri: Tümü / Açık problem / Açık eksik / Açık avantaj / Kusurlu / Bloke / Devam / Bitti görünür.
- [ ] Her çipte eşleşen proje sayısı görünür.
- [ ] Açık problem filtresi yalnız açık problemi olan projeleri gösterir.
- [ ] Açık eksik filtresi yalnız aktif eksiği olan projeleri gösterir.
- [ ] Açık avantaj filtresi yalnız açık avantajı olan projeleri gösterir.
- [ ] Kusurlu / Bloke / Devam / Bitti filtreleri doğru projeleri gösterir.
- [ ] Tümü filtresi tüm proje kartlarını geri getirir.

## 3. Proje içi imalat filtresi ve bloklar

- [ ] Projeye girdikten sonra İmalat filtresi görünür.
- [ ] Varsayılan seçim `Tüm imalatlar`dır.
- [ ] Dropdown mevcut imalatları listeler ve her birinde blok sayısı gösterir.
- [ ] İmalat araması Türkçe büyük/küçük harf duyarsız çalışır.
- [ ] Bir imalat seçilince yalnız o imalatın bulunduğu bloklar görünür.
- [ ] Seçili imalat için Tümü / Açık problem / Açık eksik / Açık avantaj / Kusurlu / Bloke / Devam / Bitti filtreleri görünür.
- [ ] `Daire Pano + Açık problem` yalnız Daire Pano'da açık problemi olan blokları gösterir.
- [ ] `Daire Pano + Açık eksik` yalnız Daire Pano'da aktif eksiği olan blokları gösterir.
- [ ] Blok kartındaki özet seçilen imalatın gerçek durumunu gösterir.
- [ ] Seçili imalatta blok kartına dokununca doğru `blockWorkItemId` detayına doğrudan gider.
- [ ] `Tüm imalatlar` seçilince tüm bloklar geri gelir ve kartlar normal blok ekranına gider.

## 4. Proje / blok tipi / blok



- [ ] Yeni boş proje oluşturulur.
- [ ] Yeni blok tipi `TEST` oluşturulur.
- [ ] `TEST-1` ve `TEST-2` blokları oluşturulur.
- [ ] Aynı blok numarası ikinci kez oluşturulamaz.
- [ ] Aynı blok tipi kodu aynı projede ikinci kez oluşturulamaz.

## 5. İmalat kataloğu ve kapsam

- [ ] TEST-1 içinde yeni imalat tanımlanır.
- [ ] “Yalnız bu blok” ile TEST-2'ye yayılmadığı doğrulanır.
- [ ] Başka bir imalat “aynı blok tipi” kapsamıyla eklenir.
- [ ] TEST-1 ve TEST-2'de göründüğü doğrulanır.
- [ ] Sonradan TEST-3 oluşturulur ve blok tipi şablonundaki imalatları miras aldığı doğrulanır.
- [ ] “Projedeki tüm bloklar” kapsamı ayrı blok tiplerinde doğrulanır.

## 6. Alakadar başka disiplin + avantaj

- [ ] Yeni imalat oluştururken “Alakadar başka disiplin kalemi” işaretlenebilir.
- [ ] Başka disiplin kalemleri blok listesinin sonunda ve farklı zeminle görünür.
- [ ] Başka disiplin kalemi Başlanmadı ise blok imalat kartının arka planı yeşildir.
- [ ] Yeşil kartta `LEHİMİZE BAŞLANMADI` etiketi görünür.
- [ ] Aynı kayıtta açık problem veya aktif eksik varsa uyarı rengi yeşilden önceliklidir.
- [ ] Bir başka-disiplin kalemine açık problem eklenebilir.
- [ ] Aynı kaleme açık avantaj eklenebilir.
- [ ] Problem ve avantaj birbirini silmez; ikisi aynı anda açık kalabilir.
- [ ] Avantaj kapatılabilir ve kapanma durumu korunur.
- [ ] Matris açık problem için P, açık avantaj için A gösterir.
- [ ] “Açık avantaj” ve “Başka disiplin” matris filtreleri çalışır.
- [ ] Konya seed'de Mutfak Fayans / Dolap satırı elektrik kalemlerinin sonunda görünür.
- [ ] Konya seed'de L.İ.E. kayıtları problem değil avantaj olarak görünür.
- [ ] Eski v1 veritabanından güncellemede bu sınıflandırma korunur.

## 7. Durumlar

- [ ] Hızlı durumdan Başlanmadı / Devam / Bitti çalışır.
- [ ] Kusurlu / Ağır kusurlu kalite eksenini değiştirir, ilerleme bilgisini silmez.
- [ ] Kontrol edilemedi / Kabul kontrol eksenini değiştirir.
- [ ] Bloke açılıp kapatılabilir.
- [ ] “Bitti + Kusurlu + Bloke” aynı anda saklanabilir.
- [ ] Uygulama yeniden açılınca durumlar aynı kalır.

## 8. Problem kataloğu ve saha bağlamı

- [ ] Problem eklerken “Bu probleme özel tanım” girilebilir.
- [ ] Kat girilebilir.
- [ ] Mahal / daire / birim numarası girilebilir.
- [ ] Mahal / daire / birim adı girilebilir.
- [ ] Aynı katalog problemi iki farklı kat/daire bağlamıyla ayrı saha kaydı olarak eklenebilir.
- [ ] Problem kartında özel tanım + kat + no + ad doğru görünür.
- [ ] Aynı alanlar avantaj kaydında da çalışır.
- [ ] PDF ve XLSX bu saha bağlamını korur.
- [ ] GKTE export/import sonrası özel tanım ve konum alanları korunur.
- [ ] Mevcut problem dropdown/arama ile bulunur ve eklenir.
- [ ] On-the-fly yeni kod + tanım oluşturulur.
- [ ] Yeni problem başka imalatta aramayla bulunabilir.
- [ ] Aynı kod tekrar yazıldığında ikinci tanım yaratılmaz.
- [ ] Uygulama mevcut tanımı gösterip “Mevcut problemi kullan” seçeneği sunar.
- [ ] Problem kapatılır, kapanma durumu korunur.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.

## 9. İmalat eksik takip yönetimi

- [ ] Bir imalat detayından yeni eksik açılabilir.
- [ ] Eksik başlığı zorunludur; açıklama opsiyoneldir.
- [ ] Kat, mahal/daire/birim no ve adı girilebilir.
- [ ] Sorumlu kişi/ekip girilebilir.
- [ ] Hedef tarih `YYYY-AA-GG` biçiminde girilebilir.
- [ ] Açılmış eksikte açıklama, konum, sorumlu, hedef tarih ve öncelik sonradan düzenlenebilir.
- [ ] Eksik düzenlemek durum yaşam döngüsünü sıfırlamaz.
- [ ] Öncelik Normal / Yüksek / Kritik seçilebilir.
- [ ] Yeni eksik varsayılan olarak Açık durumundadır.
- [ ] Yaşam döngüsü Açık → Gideriliyor → Giderildi → Kontrol edildi çalışır.
- [ ] Giderildi kaydı aktif eksik sayısından düşmez.
- [ ] Yalnız Kontrol edildi kaydı aktif eksik sayısından düşer.
- [ ] Durum gerektiğinde geriye alınabilir.
- [ ] Her eksiğe art arda en az 5 fotoğraf eklenebilir.
- [ ] Eksik fotoğrafları genel imalat veya problem/avantaj fotoğraflarına karışmaz.
- [ ] Her eksik fotoğrafının Rapora dahil seçimi bağımsız çalışır.
- [ ] Proje ekranındaki Eksik Takibi açılır ve tüm blok/imalat eksiklerini listeler.
- [ ] Proje eksik ekranında Aktif / Açık / Gideriliyor / Giderildi / Kontrol edildi filtreleri çalışır.
- [ ] Öncelik filtresi Tümü / Normal / Yüksek / Kritik olarak durum filtresiyle birlikte çalışır.
- [ ] Blok, imalat, mahal veya sorumlu metniyle arama çalışır.
- [ ] Bir eksik kartına dokununca doğru imalat detayına gider.
- [ ] Anasayfada Açık eksik filtresi doğru projeleri getirir.
- [ ] Seçili imalat + Açık eksik filtresi aynı imalat üzerinde kesişimli çalışır.
- [ ] Blok imalat kartında `E` / `E<n>` aktif eksik sayısı görünür.
- [ ] Matris hücresinde `E` / `E<n>` aktif eksik işareti görünür.
- [ ] Matris Açık eksik filtresi doğru kayıtları gösterir.
- [ ] PDF eksikleri ilgili imalat altında ve eksik fotoğraflarıyla gösterir.
- [ ] XLSX içinde ayrı `Eksikler` sayfası vardır.
- [ ] GKTE export/import sonrası eksik durumu, sorumlu, hedef tarih, konum ve fotoğraf bağları korunur.
- [ ] v4 → v5 migration mevcut saha verilerini kaybetmez.

## 10. Notlar

- [ ] Serbest not eklenir.
- [ ] Tarih/saat otomatik görünür.
- [ ] İkinci not ilk notu ezmeden kronolojiye eklenir.
- [ ] Rapora dahil kutusu oluştururken ve sonradan değiştirilebilir.

## 11. Problem / avantaj kanıt fotoğrafları

- [ ] Bir problem kaydında “Kanıt fotoğrafları” alanı görünür.
- [ ] Aynı probleme art arda en az 5 fotoğraf eklenebilir.
- [ ] Bir avantaj kaydına ayrıca birden fazla fotoğraf eklenebilir.
- [ ] Problem A'ya eklenen fotoğraf Problem B veya Avantaj C altında görünmez.
- [ ] Kapalı problem/avantajın mevcut fotoğrafları korunur.
- [ ] Kapalı problem/avantaja yeni kanıt fotoğrafı eklenebilir.
- [ ] Her kanıt fotoğrafının “Rapora dahil” kutusu bağımsız çalışır.
- [ ] PDF'de kanıt fotoğrafları doğru problem/avantajın altında görünür.
- [ ] XLSX Fotoğraflar sayfasında bağlı problem/avantaj kodu ve tanımı görünür.
- [ ] GKTE export/import sonrası fotoğraf → problem/avantaj bağı korunur.
- [ ] v2 → v3 veritabanı migration mevcut genel fotoğrafları kaybetmez.

## 12. Genel imalat fotoğrafları

- [ ] TEST-1 → bir imalat detayından kamera açılır.
- [ ] Fotoğraf çekilir ve imalat ekranına döner.
- [ ] Fotoğraf thumbnail'i görünür.
- [ ] Fotoğraf doğru blok/imalata bağlıdır.
- [ ] Başka blokta görünmez.
- [ ] Rapora dahil kutusu sonradan değiştirilebilir.
- [ ] Uygulama yeniden açılınca fotoğraf okunabilir.
- [ ] Dikey ve yatay çekilen fotoğraflar raporda kabul edilebilir yönde görünür.

## 13. Bağımsız blok parametreleri

- [ ] Yeni parametre tanımlanır: `Yatay tava var mı?`.
- [ ] TEST-1 değeri `Evet`, TEST-2 değeri `Hayır` yapılır.
- [ ] Değer sonradan düzenlenir.
- [ ] Tooltip açılır.
- [ ] Aynı parametre tanımı başka blokta tekrar kullanılabilir.

## 14. Matris

- [ ] Proje ekranından İmalat Matrisi açılır.
- [ ] Yatay kaydırma çalışır.
- [ ] Durum sembolleri doğru görünür.
- [ ] Kusurlu/ağır kusurlu/bloke ayrımı görünür.
- [ ] Açık problem bulunan hücrelerde P / P<n> işareti görünür.
- [ ] Blok tipi filtresi yalnız seçilen tipin bloklarını gösterir.
- [ ] Açık problem / Açık eksik / Kusurlu / Bloke / Devam / Bitti filtreleri doğru kayıtları gösterir.
- [ ] Filtre sonucu yoksa boş durum mesajı gösterilir.
- [ ] Filtre temizlenince tüm matris geri gelir.
- [ ] Bir hücreye dokununca doğru imalat detayına gider.

## 15. PDF raporu

- [ ] PDF dışa aktarılır.
- [ ] Android paylaş menüsü açılır.
- [ ] PDF başka bir görüntüleyicide açılır.
- [ ] Proje adı ve üretim tarihi doğru.
- [ ] Blok/imalat durumları doğru.
- [ ] Yalnız rapora dahil problem/not/fotoğraflar görünür.
- [ ] Fotoğraflar bozuk veya ezilmiş görünmez.
- [ ] Uzun raporda sayfa geçişleri ve footer düzgündür.

## 16. XLSX raporu

- [ ] XLSX dışa aktarılır.
- [ ] Excel veya Google Sheets ile açılır.
- [ ] `İmalat Matrisi`, `Problemler`, `Avantajlar`, `Eksikler`, `Notlar`, `Fotoğraflar` sayfaları vardır.
- [ ] Türkçe karakterler bozulmaz.
- [ ] Matris verileri uygulamayla eşleşir.
- [ ] Rapora dahil olmayan kayıtlar export'a girmez.

## 17. GKTE export / Android ↔ Android taşıma

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
- [ ] Problem/avantaj/eksik/not kayıtları eşleşir.
- [ ] Eksik durum, sorumlu, hedef tarih, öncelik ve fotoğraf ilişkileri eşleşir.
- [ ] Fotoğraflar yeni projede açılır.
- [ ] İçe aktarılmış projeden tekrar PDF/XLSX alınabilir.
- [ ] İçe aktarılmış projeden yeniden `.gkte` alınabilir.
- [ ] Yeni GKTE arşivinde `manifest.json`, `data.json` ve varsa `photos/` bulunur.
- [ ] `data.json` içindeki fotoğraf `localUri` değerleri Android cihaz URI'sına bağımlı değildir.
- [ ] Eski `.sitepack` dosyası geriye dönük olarak hâlâ içe aktarılabilir.

## 18. Dayanıklılık

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
