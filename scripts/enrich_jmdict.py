from __future__ import annotations

import argparse
import gzip
import json
import re
import urllib.request
import xml.etree.ElementTree as et
from dataclasses import dataclass
from pathlib import Path


DEFAULT_SOURCE_URL = "https://ftp.edrdg.org/pub/Nihongo/JMdict_e_examp.gz"


POS_TO_PART_OF_SPEECH = {
    "noun (common) (futsuumeishi)": "명사",
    "noun, used as a suffix": "접미 명사",
    "noun, used as a prefix": "접두 명사",
    "noun or participle which takes the aux. verb suru": "する명사",
    "suru verb": "する동사",
    "suru verb - included": "する동사",
    "Godan verb with 'u' ending": "동사",
    "Godan verb with 'ku' ending": "동사",
    "Godan verb with 'gu' ending": "동사",
    "Godan verb with 'su' ending": "동사",
    "Godan verb with 'tsu' ending": "동사",
    "Godan verb with 'nu' ending": "동사",
    "Godan verb with 'bu' ending": "동사",
    "Godan verb with 'mu' ending": "동사",
    "Godan verb with 'ru' ending": "동사",
    "Ichidan verb": "동사",
    "Kuru verb - special class": "동사",
    "intransitive verb": "자동사",
    "transitive verb": "타동사",
    "adjectival nouns or quasi-adjectives (keiyodoshi)": "형용동사",
    "adjective (keiyoushi)": "형용사",
    "adverb (fukushi)": "부사",
    "adverb taking the 'to' particle": "부사",
    "adverbial noun": "부사",
    "expressions (phrases, clauses, etc.)": "표현",
    "interjection (kandoushi)": "감탄사",
    "conjunction": "접속사",
    "pronoun": "대명사",
    "counter": "조수사",
    "auxiliary verb": "조동사",
    "prefix": "접두사",
    "suffix": "접미사",
}


GRAMMAR_HINTS = {
    "Godan verb with 'u' ending": "5단동사",
    "Godan verb with 'ku' ending": "5단동사",
    "Godan verb with 'gu' ending": "5단동사",
    "Godan verb with 'su' ending": "5단동사",
    "Godan verb with 'tsu' ending": "5단동사",
    "Godan verb with 'nu' ending": "5단동사",
    "Godan verb with 'bu' ending": "5단동사",
    "Godan verb with 'mu' ending": "5단동사",
    "Godan verb with 'ru' ending": "5단동사",
    "Ichidan verb": "1단동사",
    "Kuru verb - special class": "불규칙동사",
    "suru verb": "する활용",
    "suru verb - included": "する활용",
    "intransitive verb": "자동사",
    "transitive verb": "타동사",
    "adjectival nouns or quasi-adjectives (keiyodoshi)": "형용동사",
    "adjective (keiyoushi)": "い형용사",
}


@dataclass
class JmdictEntry:
    kanji_forms: list[str]
    reading_forms: list[str]
    primary_part_of_speech: str
    grammar: str
    example_ja: str
    example_en: str


@dataclass
class JmdictIndex:
    by_kanji: dict[str, list[JmdictEntry]]
    by_reading: dict[str, list[JmdictEntry]]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source",
        default="data/sources/JMdict_e_examp.gz",
        help="Path to local JMdict_e_examp.gz",
    )
    parser.add_argument(
        "--asset",
        default="app/src/main/assets/jlpt_words.json",
        help="Path to jlpt_words.json",
    )
    parser.add_argument(
        "--download-if-missing",
        action="store_true",
        help="Download the official source if the local file does not exist.",
    )
    return parser.parse_args()


def ensure_source(source_path: Path, download_if_missing: bool) -> None:
    if source_path.exists():
        return
    if not download_if_missing:
        raise FileNotFoundError(f"Missing source file: {source_path}")
    source_path.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(DEFAULT_SOURCE_URL) as response, source_path.open("wb") as target:
        target.write(response.read())


def normalize(value: str) -> str:
    return re.sub(r"\s+", "", value or "")


def choose_primary_part_of_speech(tags: list[str]) -> str:
    for tag in tags:
        if tag in POS_TO_PART_OF_SPEECH:
            mapped = POS_TO_PART_OF_SPEECH[tag]
            if mapped not in {"자동사", "타동사"}:
                return mapped
    for tag in tags:
        mapped = POS_TO_PART_OF_SPEECH.get(tag)
        if mapped:
            return mapped
    return ""


def choose_grammar(tags: list[str]) -> str:
    hints: list[str] = []
    for tag in tags:
        mapped = GRAMMAR_HINTS.get(tag)
        if mapped and mapped not in hints:
            hints.append(mapped)
    return ", ".join(hints)


def parse_jmdict(source_path: Path) -> JmdictIndex:
    by_kanji: dict[str, list[JmdictEntry]] = {}
    by_reading: dict[str, list[JmdictEntry]] = {}
    with gzip.open(source_path, "rb") as handle:
        for _, elem in et.iterparse(handle, events=("end",)):
            if elem.tag != "entry":
                continue

            kanji_forms = [text.text for text in elem.findall("./k_ele/keb") if text.text]
            reading_forms = [text.text for text in elem.findall("./r_ele/reb") if text.text]

            pos_tags: list[str] = []
            example_ja = ""
            example_en = ""

            for sense in elem.findall("./sense"):
                pos_tags.extend([node.text for node in sense.findall("pos") if node.text])
                if not example_ja:
                    for example in sense.findall("example"):
                        example_sentences = [node.text for node in example.findall("ex_sent") if node.text]
                        if example_sentences:
                            example_ja = example_sentences[0]
                            if len(example_sentences) > 1:
                                example_en = example_sentences[1]
                            break

            entry = JmdictEntry(
                kanji_forms=kanji_forms,
                reading_forms=reading_forms,
                primary_part_of_speech=choose_primary_part_of_speech(pos_tags),
                grammar=choose_grammar(pos_tags),
                example_ja=example_ja,
                example_en=example_en,
            )
            for kanji in {normalize(value) for value in kanji_forms if value}:
                by_kanji.setdefault(kanji, []).append(entry)
            for reading in {normalize(value) for value in reading_forms if value}:
                by_reading.setdefault(reading, []).append(entry)
            elem.clear()
    return JmdictIndex(by_kanji=by_kanji, by_reading=by_reading)


def score_entry(record: dict, entry: JmdictEntry) -> int:
    kanji = normalize(record.get("kanji", ""))
    reading = normalize(record.get("readingJa", ""))
    kanji_forms = {normalize(value) for value in entry.kanji_forms}
    reading_forms = {normalize(value) for value in entry.reading_forms}

    score = 0
    if kanji and kanji in kanji_forms:
        score += 3
    if reading and reading in reading_forms:
        score += 2
    if not kanji and reading and reading in kanji_forms:
        score += 1
    return score


def enrich_asset(asset_path: Path, index: JmdictIndex) -> dict[str, int]:
    records = json.loads(asset_path.read_text(encoding="utf-8"))
    enriched_count = 0
    example_count = 0
    grammar_count = 0
    pos_count = 0

    for record in records:
        best_entry = None
        best_score = 0
        kanji = normalize(record.get("kanji", ""))
        reading = normalize(record.get("readingJa", ""))
        candidates = []
        if kanji:
            candidates.extend(index.by_kanji.get(kanji, []))
        if reading:
            candidates.extend(index.by_reading.get(reading, []))
        seen_ids: set[int] = set()
        unique_candidates: list[JmdictEntry] = []
        for entry in candidates:
            marker = id(entry)
            if marker not in seen_ids:
                seen_ids.add(marker)
                unique_candidates.append(entry)
        for entry in unique_candidates:
            score = score_entry(record, entry)
            if score > best_score:
                best_score = score
                best_entry = entry
                if score >= 5:
                    break

        if best_entry is None or best_score == 0:
            record.setdefault("exampleJa", "")
            record.setdefault("exampleKo", "")
            continue

        changed = False
        if not record.get("partOfSpeech") and best_entry.primary_part_of_speech:
            record["partOfSpeech"] = best_entry.primary_part_of_speech
            pos_count += 1
            changed = True
        if not record.get("grammar") and best_entry.grammar:
            record["grammar"] = best_entry.grammar
            grammar_count += 1
            changed = True
        if not record.get("exampleJa") and best_entry.example_ja:
            record["exampleJa"] = best_entry.example_ja
            example_count += 1
            changed = True
        if "exampleKo" not in record:
            record["exampleKo"] = ""
        if changed:
            enriched_count += 1

    asset_path.write_text(
        json.dumps(records, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return {
        "total": len(records),
        "enriched": enriched_count,
        "pos": pos_count,
        "grammar": grammar_count,
        "examples": example_count,
    }


def main() -> None:
    args = parse_args()
    source_path = Path(args.source)
    asset_path = Path(args.asset)
    ensure_source(source_path, args.download_if_missing)
    index = parse_jmdict(source_path)
    stats = enrich_asset(asset_path, index)
    print(json.dumps(stats, ensure_ascii=False))


if __name__ == "__main__":
    main()
