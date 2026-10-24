import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("reader_corpus", Path(__file__).with_name("prepare-quran-reader.py"))
corpus = importlib.util.module_from_spec(spec)
spec.loader.exec_module(corpus)


class ReaderCorpusParserTest(unittest.TestCase):
    def test_continuation_lines_belong_to_their_ayah_and_footer_is_separate(self):
        source = "2|101|Birinci satır\nAyetin devamı; değiştirilmez.\n2|102|İkinci ayet\n\n#  ID: tr.example\n"
        rows, footer = corpus.parse_source(source.encode("utf-8"))
        self.assertEqual([((2, 101), "Birinci satır\nAyetin devamı; değiştirilmez."), ((2, 102), "İkinci ayet")], rows)
        self.assertEqual("#  ID: tr.example", footer)

    def test_diacritics_punctuation_and_internal_whitespace_are_preserved(self):
        text = "ḳul hüve-llâhü eḥad.  ‘İki boşluk’ — (1)"
        rows, _ = corpus.parse_source(("112|1|" + text + "\n").encode("utf-8"))
        self.assertEqual([((112, 1), text)], rows)


if __name__ == "__main__":
    unittest.main()
