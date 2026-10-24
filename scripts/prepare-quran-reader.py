#!/usr/bin/env python3
"""Package the original reader editions as verified, offline, per-ayah arrays."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import re
import urllib.request

EDITIONS = {
    "tr.diyanet": "Diyanet İşleri",
    "tr.ozturk": "Yaşar Nuri Öztürk",
    "tr.yildirim": "Suat Yıldırım",
    "tr.vakfi": "Türkiye Diyanet Vakfı",
    "tr.golpinarli": "Abdulbaki Gölpınarlı",
    "tr.ates": "Süleyman Ateş",
    "tr.yazir": "Elmalılı Hamdi Yazır",
    "en.sahih": "Saheeh International",
    "tr.transliteration": "Muhammet Abay — Türkçe Okunuş",
}


def parse_source(raw):
    records = []
    key, chunks, footer = None, [], []
    in_footer = False

    def finish():
        if key is not None:
            # Blank lines before the next key/footer are file separators. Keep
            # internal continuation lines, including Diyanet 2:102, intact.
            text = "\n".join(chunks).rstrip("\r\n")
            if not text:
                raise ValueError(f"Empty ayah {key}")
            records.append((key, text))

    for line in raw.decode("utf-8-sig").splitlines():
        if line.startswith("#") or in_footer:
            if not in_footer:
                finish()
                key = None
                in_footer = True
            footer.append(line)
            continue
        match = re.match(r"^(\d+)\|(\d+)\|(.*)$", line)
        if match:
            finish()
            key = (int(match[1]), int(match[2]))
            chunks = [match[3]]
        elif key is not None:
            chunks.append(line)
        elif line.strip():
            raise ValueError(f"Unexpected source line: {line!r}")
    finish()
    return records, "\n".join(footer)


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", type=Path, default=root / "build/quran-reading-downloads")
    args = parser.parse_args()
    args.cache_dir.mkdir(parents=True, exist_ok=True)
    out = root / "app/src/main/assets/quran_reading"
    out.mkdir(parents=True, exist_ok=True)
    source_verses = json.loads((root / "app/src/main/assets/quran_search/verses.json").read_text())
    expected_keys = [(v["surahId"], v["verseNumber"]) for v in source_verses]
    assert len(expected_keys) == 6236

    def prepare(edition):
        url = f"https://tanzil.net/trans/{edition}"
        cached = args.cache_dir / f"{edition}.txt"
        if not cached.exists():
            request = urllib.request.Request(url, headers={"User-Agent": "VakitBox-Pro-offline-corpus/1.0"})
            cached.write_bytes(urllib.request.urlopen(request, timeout=90).read())
        raw = cached.read_bytes()
        records, footer = parse_source(raw)
        assert [k for k, _ in records] == expected_keys, f"Incomplete/misnumbered edition: {edition}"
        assert f"#  ID: {edition}" in footer, f"Mismatched source identity: {edition}"
        payload = json.dumps([text for _, text in records], ensure_ascii=False, separators=(",", ":")).encode()
        (out / f"{edition}.json").write_bytes(payload)
        return {
            "id": edition, "name": EDITIONS[edition], "url": url, "ayahCount": len(records),
            "sourceSha256": hashlib.sha256(raw).hexdigest(),
            "assetSha256": hashlib.sha256(payload).hexdigest(), "sourceFooter": footer,
        }

    with ThreadPoolExecutor(max_workers=4) as pool:
        sources = list(pool.map(prepare, EDITIONS))
    manifest = {
        "source": "Tanzil.net", "termsUrl": "https://tanzil.net/trans/",
        "format": "6236 original text strings in Quran ayah order, 1:1 through 114:6",
        "editions": sources,
    }
    (out / "SOURCES.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    notice = (
        "VakitBox Pro — çevrimdışı Kur’an okuyucu kaynakları\n\n"
        "Bu dosyalar Tanzil'in yayımladığı meal ve çeviriyazı metinleridir. Ayet\n"
        "içerikleri değiştirilmeden, sure ve ayet numaraları doğrulanarak paketlenir.\n"
        "Türkçe ve İngilizce mealler birbirinin yerine etiketlenmez.\n\n"
        "Kaynak ve güncellemeler: https://tanzil.net/trans/\n"
        "Kullanım koşulları: https://tanzil.net/trans/\n"
        "Tanzil bu çevirileri gayriticari kullanıma sunar. Ticari kullanım için\n"
        "çevirmen/yayıncı izni gerekir. Üçten fazla çeviri kullanıldığında Tanzil'in\n"
        "çeviri sayfasına bağlantı verilmesi istenir. Çevirilerin telifleri ilgili\n"
        "çevirmen/yayıncılara aittir; Arapça Tanzil metninin lisansı meallere\n"
        "uygulanmış sayılmaz. Kaynak üstverileri aşağıda korunmuştur.\n\n"
        "Rowwad 1.0.4 ve Arapça Uthmani 1.1 için ayrıca quran_search/NOTICE.txt\n"
        "ve ilgili lisans dosyalarına bakınız.\n\n"
    )
    for source in sources:
        notice += f"{source['name']}\n{source['url']}\n{source['sourceFooter']}\n\n"
    (out / "NOTICE.txt").write_text(notice, encoding="utf-8")
    for source in sources:
        print(f"{source['id']}: {source['ayahCount']} ayahs, SHA-256 {source['assetSha256']}")


if __name__ == "__main__":
    main()
