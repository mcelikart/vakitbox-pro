# VakitBox Pro

VakitBox Pro; namaz vakitlerini, çevrimdışı Kur’an okuma ve konu aramasını, günlük ibadet araçlarını, yürüyüş takibini ve not defterini bir araya getiren Android uygulamasıdır. Kotlin ve Jetpack Compose ile geliştirilmiştir.

[Son sürümü ve APK’yi indir](https://github.com/mcelikart/vakitbox-pro/releases/latest)

## Ekran görüntüleri

<p align="center">
  <img src="screenshots/20_home_5_4_0.png" alt="Ana sayfa: geri sayım, vakitler, adımsayar ve sabah namazı alarmı" width="46%" />
  <img src="screenshots/21_quran_sources_5_4_0.png" alt="Çevrimdışı Kur’an meal kaynakları ve seçim ekranı" width="46%" />
</p>
<p align="center">
  <img src="screenshots/22_walk_5_4_0.png" alt="Sade yürüyüş ekranı: adım, mesafe, kalori, süre ve geçmiş" width="46%" />
  <img src="screenshots/23_notes_editor_5_4_0.png" alt="Kategori seçimi ve sesle yazma özellikli not defteri" width="46%" />
</p>

## Özellikler

### Namaz vakitleri ve bildirimler

- Seçilen konuma göre günlük namaz vakitleri ve bir sonraki vakte kalan süre.
- Vakit girmeden 10 dakika önce ve vakit başladığında bildirim.
- Bildirim alanında sürekli görünen geri sayım; vakit bildirimi geri sayımın altında kalır ve beş dakika sonra otomatik kapanır.
- Durum çubuğunda büyük, okunaklı ve canlı kalan-süre göstergesi.
- Konum seçimi ve farklı vakit hesaplama yöntemleri.

### Sabah Namazı alarmı

- Alarm saatini güneş doğuşuna göre her gün yeniler.
- Varsayılan alarm güneş doğuşundan 30 dakika öncedir; bu süre değiştirilebilir ve birden fazla alarm kurulabilir.
- Alarm sesi ve erteleme/kapatma kontrolleri Android alarm davranışını izler.

### Çevrimdışı Kur’an fihristi

- Sorguyu yalnızca kelime eşleşmesiyle aramaz; aranan konunun anlamına yakın ayetleri cihazda sıralar.
- Arama internet bağlantısı, API veya API anahtarı istemez. Model ve arama verileri uygulamayla birlikte gelir.
- 114 sure ve 6.236 ayet üzerinde çalışır; sonuçlar sure ve ayet bilgisiyle gösterilir.

### Kur’an okuma ve diğer araçlar

- 114 surenin ve 6.236 ayetin Arapça metni, seçilebilir mealleri ve Türkçe okunuşu uygulamayla birlikte gelir; metin okumak için API veya internet gerekmez.
- Diyanet İşleri, Elmalılı, Türkiye Diyanet Vakfı, Yaşar Nuri Öztürk, Suat Yıldırım, Abdülbaki Gölpınarlı, Süleyman Ateş, Saheeh International ve Rowwad kaynakları arasından seçim yapılabilir.
- Seçilen mealler ayrı sekmelerde gösterilir. Kaynak kimlikleri, mevcut dipnotlar ve ayet kayıtları korunur.
- Sesli tilavet ve Türkçe meal seslendirme desteği.
- Kıble pusulası, imsakiye ve hicrî takvim.
- Zikir sayacı, günlük adım sayar, namaz öğrenme rehberi ve kaza namazı takibi.
- Yakındaki camileri bulma, dua ve sure içerikleri, Esma-ül Hüsna ve zekât hesaplama.
- Tema seçenekleri, ana ekran araç takımı ve farklı ekran boyutlarına uyumlu arayüz.

### Yürüyüş takibi

- Günlük, haftalık ve aylık adım özeti; hedef, grafik ve geçmiş.
- Profile göre tahmini mesafe, aktif kalori ve süre.
- Tempo seçimi, profil/kalibrasyon ve yürüyüş kaydı.
- Adım sayımını açma/kapatma ve gerekli cihaz izinlerine erişim.

### Not defteri

- Notların bulunduğu listede kategoriler, kategoriye göre filtreleme ve not sayıları.
- Kategori ekleme, yeniden adlandırma ve silme; kategori silinirken notlar Kategorisiz altında korunur.
- Not düzenlerken cihazın konuşma tanımasıyla sesle yazma; mevcut metin korunarak imleç konumuna ekleme.
- Arama, sabitleme, yazı düzenleme ve kaydetme kontrolleri.

Kur’an metinleri ve fihrist araması cihazda çalışır. Hava durumu, harita, sesli tilavet ve isteğe bağlı asistan gibi bazı özellikler internet bağlantısı gerektirebilir. Sesle not yazma, cihazdaki konuşma tanıma uygulamasını kullanır.

## Kurulum

Yayımlanan kurulum paketleri için [GitHub Releases](https://github.com/mcelikart/vakitbox-pro/releases) sayfasına bakın. Kaynak kodunu Android Studio ile açabilir veya JDK 17 ve Android SDK kurulu bir ortamda derleyebilirsiniz.

Arama modeli Git LFS ile saklanır. Depoyu klonladıktan sonra model dosyasını indirin:

```sh
git lfs install
git lfs pull
```

Debug APK üretmek için:

```sh
./gradlew assembleDebug
```

Minify ve kaynak küçültmesi etkin, telefonda denemeye uygun APK için:

```sh
./gradlew assemblePreview
```

`assemblePreview` çıktısı `app/build/outputs/apk/preview/app-preview.apk` konumunda oluşur.

## Geliştirme bilgileri

- Android için Kotlin, Jetpack Compose ve Material 3.
- Kur’an konu araması için cihazda çalışan ONNX Runtime modeli.
- Minimum Android sürümü: Android 7.0 (API 24).
- Sürüm numarası ve Android yapı kodunun tek kaynağı `version.properties` dosyasıdır. Kaynak değişikliklerini paketlerken yama sürümü ve yapı kodu otomatik ilerler; ayrıntılar için [VERSIONING.md](VERSIONING.md) dosyasına bakın.
- Testler: `./gradlew testDebugUnitTest`.

## Kaynaklar ve lisanslar

Kur’an metni, Rowwad meali, arama modeli ve kaynak notları `app/src/main/assets/quran_search` altında bulunur. Diğer mealler ve Türkçe okunuş `app/src/main/assets/quran_reading` altında, kaynak kimlikleri ve dosya özetleriyle paketlenir. Kaynak metinleri yeniden yazılmaz; her dosyanın 114 sure ve 6.236 ayet sırası doğrulanır.

Meal ve okunuş dosyalarının kaynağı [Tanzil çevirileri](https://tanzil.net/trans/) sayfasıdır. Tanzil bu çevirileri gayriticari kullanım için sunar; ticari kullanımda çevirmen/yayıncı izni gerekir. Arapça metnin, çevirilerin ve arama modelinin lisansları farklıdır. Her iki dizindeki `NOTICE` ve lisans dosyalarını dağıtım sırasında koruyun. Kaynaklar uygulamanın Hakkında ekranında da gösterilir.

Okuyucu veri dosyalarını yeniden hazırlamak için:

```sh
python3 scripts/prepare-quran-reader.py
python3 scripts/test_quran_reader_corpus.py
```

## İletişim

Sorular ve geri bildirim için: [mcelik.art@gmail.com](mailto:mcelik.art@gmail.com)
