from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from pypdf import PdfReader


ENTRY_PATTERN = re.compile(r"^(?P<head>.+?)\s+N(?P<level>[1-5]),\s*(?P<meaning>.+)$")
BRACKET_PATTERN = re.compile(r"^(?P<reading>.+?)\[(?P<kanji>.+)\]$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdf", required=True, help="Path to source PDF")
    parser.add_argument("--asset", default="app/src/main/assets/jlpt_words.json", help="Path to jlpt_words.json")
    parser.add_argument("--note", required=True, help="Import note")
    return parser.parse_args()


def normalize_token(text: str) -> str:
    return re.sub(r"\s+", "", text.strip())


def parse_head(head: str) -> tuple[str, str]:
    normalized = head.strip()
    match = BRACKET_PATTERN.match(normalized)
    if match:
        reading = normalize_token(match.group("reading"))
        kanji = normalize_token(match.group("kanji"))
        return reading, kanji
    reading = normalize_token(normalized)
    return reading, reading


def parse_pdf_entries(pdf_path: Path) -> list[dict]:
    reader = PdfReader(str(pdf_path))
    entries: list[dict] = []
    current: dict | None = None

    for page in reader.pages:
        text = page.extract_text() or ""
        for raw_line in text.splitlines():
            line = raw_line.strip()
            if not line:
                continue

            entry_match = ENTRY_PATTERN.match(line)
            if entry_match:
                if current:
                    entries.append(current)
                reading, kanji = parse_head(entry_match.group("head"))
                level = f"N{entry_match.group('level')}"
                current = {
                    "deck": level,
                    "readingJa": reading,
                    "kanji": kanji,
                    "meaningKo": entry_match.group("meaning").strip(),
                }
                continue

            if current:
                current["meaningKo"] = f"{current['meaningKo']} {line}".strip()

    if current:
        entries.append(current)

    return entries


def record_signature(record: dict) -> tuple[str, str, str, str]:
    return (
        record.get("readingJa", "").strip(),
        record.get("kanji", "").strip(),
        record.get("meaningKo", "").strip(),
        record.get("tag", "").strip(),
    )


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    args = parse_args()
    pdf_path = Path(args.pdf)
    asset_path = Path(args.asset)

    existing_records = json.loads(asset_path.read_text(encoding="utf-8"))
    existing_signatures = {record_signature(record) for record in existing_records}

    parsed_entries = parse_pdf_entries(pdf_path)
    added_count = 0
    added_by_level = {f"N{level}": 0 for level in range(1, 6)}

    for entry in parsed_entries:
        level = entry["deck"]
        record = {
            "deck": level,
            "readingJa": entry["readingJa"],
            "readingKo": "",
            "partOfSpeech": "",
            "grammar": "",
            "kanji": entry["kanji"],
            "meaningJa": "",
            "meaningKo": entry["meaningKo"],
            "exampleJa": "",
            "exampleKo": "",
            "tag": f"JLPT {level}",
            "note": args.note,
        }
        signature = record_signature(record)
        if signature in existing_signatures:
            continue
        existing_records.append(record)
        existing_signatures.add(signature)
        added_count += 1
        added_by_level[level] += 1

    asset_path.write_text(
        json.dumps(existing_records, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(
        json.dumps(
            {
                "parsed": len(parsed_entries),
                "added": added_count,
                "added_by_level": added_by_level,
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
