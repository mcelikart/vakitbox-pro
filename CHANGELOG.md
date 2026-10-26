# Sürüm notları

## 5.5.1 — 27 Ekim 2026

- Arka planda sayaç yenilenirken oluşan geçici hataların uygulamayı kapatması önlendi; son bildirim korunarak artan bekleme süresiyle tekrar denenir.
- Sayaç servisi, veri ve tema işlemlerinden önce ön plana alınır. Android servisi yeniden oluşturduğunda etkin adımsayar da geri başlatılır.
- Birbirini erteleyebilen kurtarma alarmları kaldırıldı. İşlem sonlandırıldığında da kalan, sistemde saklanan bakım görevi ve saat değişiminden etkilenmeyen ayrı kısa toparlanma isteği eklendi.
- Pil kısıtlaması, otomatik başlatma, kesin alarm ve geri sayım kanalı kontrolleri yenilendi; sistem ayarlarından dönüşte durum güncellenir. Eksik izinler için kapatılabilir bir defalık rehber eklendi.
- Android kullanıcı tarafından Zorla durdurulduğunda bu tercih korunur; uygulama yeniden açıldığında çalışma devam eder.

## 5.5.0 — 25 Ekim 2026

- Uygulamanın görünen adı Vakit Pro olarak yenilendi; mevcut ikon, kurulum kimliği ve kullanıcı verileri korundu.
- Sabah Namazı Alarmı menüsüne alarm sesi, çalma/erteleme süresi, ses düzeyi, kademeli ses artışı, ön bildirim, telefonu çevirerek erteleme ve ses tuşu ayarları eklendi.
- Not defteri klasörlerle gezinilen yapıya geçti. Klasör açıldığında yalnız içindeki notlar gösterilir; ekleme ve yönetim kontrolleri menüdedir. Silinen klasörlerin notları korunur.
- Sesle yazma, ayrı bir Google ekranı açmadan doğrudan not editöründe çalışır. Ara sonuçlar aynı metni tekrar eklemeden güncellenir; dinleme düğmeyle durdurulur.
- Üst durum çubuğu geri sayımında daha kalın, kompakt yazı tipi kullanıldı.
- GitHub tanıtımı, uygulamanın bütün özelliklerini açıklayan metin ve yeni ekran görüntüleriyle yenilendi.

## 5.4.0 — 25 Ekim 2026

- Kur’an okuyucusundaki meal seçimleri geri getirildi. Diyanet, Elmalılı, TDV, Öztürk, Yıldırım, Gölpınarlı, Ateş, Sahih ve Rowwad mealleri, Arapça metin ve tam Türkçe okunuş çevrimdışı açılır. Kaynak kimlikleri ve mevcut dipnotlar korunur.
- Not kategorileri notların bulunduğu listede gösterilir; ekleme, yeniden adlandırma ve silme kontrolleri vardır. Kategori silme, notları Kategorisiz altında korur.
- Not düzenleyicisine sesle yazma eklendi. Tanınan metin mevcut içeriği silmeden eklenir; taslak ve imleç ekran yeniden oluşturulduğunda korunur.
- Yürüyüş ekranı adım, hedef, tahmini mesafe/kalori/süre, grafik, geçmiş ve yürüyüş kaydı çevresinde sadeleştirildi.
- Ana sayfa kartlarında ortak cam yüzey kullanılır. Hava durumunun altındaki kartların altın kenar çizgileri korunur. Vakit kutularının ve geri sayım yerleşiminin tasarımı korunur.
- Tesbihte dört malzemenin aynı yedek tona düşmesine neden olan ses başlatma hatası düzeltildi; sayfa kaydırma alanı korundu.
