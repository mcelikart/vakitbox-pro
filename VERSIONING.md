# Uygulama sürümleri

Tek sürüm kaynağı `version.properties` dosyasıdır. Android paketinin ve Ayarlar → Uygulama Hakkında ekranının sürümleri aynı kaynaktan gelir. Mevcut sürüm **5.4.0**, Android yapı numarası **14**’tür. İlk otomatik sürümleme tabanı **5.2.0 (7)** idi.

Normal APK/AAB oluşturma komutları sürümü otomatik hazırlar:

```sh
./gradlew assemblePreview
./gradlew assembleDebug
./gradlew bundleRelease
```

İlk paket kaynakların parmak izini kaydeder ve `5.2.0` sürümünü korur. Sonraki kod, görsel, varlık, model veya derleme ayarı değişikliğinde paket hazırlanırken yama sürümü ve Android yapı numarası bir artar: `5.2.0 (7)` → `5.2.1 (8)`. Aynı kaynaklarla tekrar paket oluşturmak numaraları artırmaz.

Test, derleme kontrolü, Android Studio eşitlemesi ve `help` sürümü artırmaz; kaynak değişiklikleri sonraki paket için korunur. Oluşturulmuş `build/` dosyaları, Gradle önbelleği, yerel SDK/anahtar ayarları ve belgeler parmak izine alınmaz. `app/src/`, kök `assets/`, Gradle ayarları ve sürüm betikleri alınır.

Yeni özellik veya ana sürüm için, değişen kaynakların ilk paketinde artış türü açıkça seçilebilir:

```sh
./gradlew assemblePreview -PversionBump=minor
./gradlew assemblePreview -PversionBump=major
```

`minor` yama hanesini, `major` hem küçük hem yama hanelerini sıfırlar. Aynı kaynaklarla bu komutun tekrarı da numaraları artırmaz. Görev adlarını kısaltmadan kullanın. Sürüm paketleme isteği başında ayrılır; paketleme başarısız olursa aynı kaynaklarla yeniden deneme aynı numarayı kullanır. Gradle önbelleği normal çalıştırma ile `--dry-run` arasında paylaşıldığı için paketleme görevinin kuru çalıştırması da değişen kaynaklara sürüm ayırır; takip eden gerçek paket aynı numarayı kullanır.

Sürüm aracı yalnızca Gradle'ın kullandığı JDK'yı gerektirir. Yapılandırma önbelleği etkin olsa da kaynak değişikliği kontrolü tekrar yapılır; sürüm değişmişse Android yapılandırması yenilenir. Dosya kilidi ve atomik kayıt eşzamanlı paketlerin aynı değişiklik için iki kez sürüm artırmasını önler. `version.properties` kaynaklarla birlikte saklanmalıdır.

Bağımsız sürüm regresyon kontrolleri:

```sh
python3 scripts/test_versioning.py
```
