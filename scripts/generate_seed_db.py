from __future__ import annotations

import argparse
import json
import sqlite3
from pathlib import Path


SCHEMA_VERSION = 2
IDENTITY_HASH = "53837574952857a079b262c5dbd3ec33"
SEED_KEY = "seed_v6"
SEEDED_VALUE = "done"

DEFAULT_DECKS = [
    ("N5", "JLPT N5", "기초 일본어 단어장", "JLPT", "JLPT N5", 0),
    ("N4", "JLPT N4", "초급 일본어 단어장", "JLPT", "JLPT N4", 1),
    ("N3", "JLPT N3", "중급 입문 단어장", "JLPT", "JLPT N3", 2),
    ("N2", "JLPT N2", "중상급 일본어 단어장", "JLPT", "JLPT N2", 3),
    ("N1", "JLPT N1", "고급 일본어 단어장", "JLPT", "JLPT N1", 4),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--asset", default="data/seeds/jlpt_words.json")
    parser.add_argument("--output", default="app/src/main/assets/databases/wordbook.db")
    return parser.parse_args()


def word_signature(record: dict) -> tuple[str, str, str, str]:
    return (
        record.get("readingJa", "").strip(),
        record.get("kanji", "").strip(),
        record.get("meaningKo", "").strip(),
        record.get("tag", "").strip(),
    )


def create_schema(connection: sqlite3.Connection) -> None:
    connection.executescript(
        f"""
        PRAGMA foreign_keys = ON;
        CREATE TABLE app_settings (
            key TEXT NOT NULL PRIMARY KEY,
            value TEXT NOT NULL
        );
        CREATE TABLE decks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            type TEXT NOT NULL,
            sourceTag TEXT NOT NULL,
            displayOrder INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        );
        CREATE TABLE words (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            readingJa TEXT NOT NULL,
            readingKo TEXT NOT NULL,
            partOfSpeech TEXT NOT NULL,
            grammar TEXT NOT NULL,
            kanji TEXT NOT NULL,
            meaningJa TEXT NOT NULL,
            meaningKo TEXT NOT NULL,
            exampleJa TEXT NOT NULL,
            exampleKo TEXT NOT NULL,
            tag TEXT NOT NULL,
            note TEXT NOT NULL,
            createdAt INTEGER NOT NULL
        );
        CREATE TABLE deck_word_cross_ref (
            deckId INTEGER NOT NULL,
            wordId INTEGER NOT NULL,
            displayOrder INTEGER NOT NULL,
            addedAt INTEGER NOT NULL,
            PRIMARY KEY(deckId, wordId),
            FOREIGN KEY(deckId) REFERENCES decks(id) ON DELETE CASCADE,
            FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
        );
        CREATE INDEX index_deck_word_cross_ref_wordId ON deck_word_cross_ref(wordId);
        CREATE TABLE study_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            deckId INTEGER,
            deckName TEXT NOT NULL,
            isAiDeck INTEGER NOT NULL,
            wordOrder TEXT NOT NULL,
            frontField TEXT NOT NULL,
            revealField TEXT NOT NULL,
            wordIdsSerialized TEXT NOT NULL,
            totalCount INTEGER NOT NULL,
            correctCount INTEGER NOT NULL,
            wrongCount INTEGER NOT NULL,
            startedAt INTEGER NOT NULL,
            completedAt INTEGER
        );
        CREATE TABLE study_answers (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            sessionId INTEGER NOT NULL,
            wordId INTEGER NOT NULL,
            sequenceIndex INTEGER NOT NULL,
            isCorrect INTEGER NOT NULL,
            answeredAt INTEGER NOT NULL,
            FOREIGN KEY(sessionId) REFERENCES study_sessions(id) ON DELETE CASCADE,
            FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
        );
        CREATE INDEX index_study_answers_sessionId ON study_answers(sessionId);
        CREATE INDEX index_study_answers_wordId ON study_answers(wordId);
        CREATE TABLE room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        );
        INSERT OR REPLACE INTO room_master_table (id, identity_hash)
        VALUES (42, '{IDENTITY_HASH}');
        PRAGMA user_version = {SCHEMA_VERSION};
        """
    )


def main() -> int:
    args = parse_args()
    asset_path = Path(args.asset)
    output_path = Path(args.output)

    records: list[dict] = json.loads(asset_path.read_text(encoding="utf-8"))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    connection = sqlite3.connect(output_path)
    connection.execute("PRAGMA journal_mode = OFF")
    connection.execute("PRAGMA synchronous = OFF")
    connection.execute("PRAGMA temp_store = MEMORY")
    connection.execute("PRAGMA foreign_keys = ON")

    try:
        create_schema(connection)
        cursor = connection.cursor()

        now_base = 1_700_000_000_000
        deck_id_by_key: dict[str, int] = {}
        for deck_key, name, description, deck_type, source_tag, display_order in DEFAULT_DECKS:
            cursor.execute(
                """
                INSERT INTO decks (name, description, type, sourceTag, displayOrder, createdAt)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (name, description, deck_type, source_tag, display_order, now_base + display_order),
            )
            deck_id_by_key[deck_key] = int(cursor.lastrowid)

        signature_to_word_id: dict[tuple[str, str, str, str], int] = {}
        linked_signatures_by_deck: dict[str, set[tuple[str, str, str, str]]] = {
            deck_key: set() for deck_key, *_ in DEFAULT_DECKS
        }
        display_order_by_deck = {deck_key: 0 for deck_key, *_ in DEFAULT_DECKS}

        for index, record in enumerate(records):
            signature = word_signature(record)
            word_id = signature_to_word_id.get(signature)
            if word_id is None:
                cursor.execute(
                    """
                    INSERT INTO words (
                        readingJa, readingKo, partOfSpeech, grammar, kanji,
                        meaningJa, meaningKo, exampleJa, exampleKo, tag, note, createdAt
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        record.get("readingJa", ""),
                        record.get("readingKo", ""),
                        record.get("partOfSpeech", ""),
                        record.get("grammar", ""),
                        record.get("kanji", ""),
                        record.get("meaningJa", ""),
                        record.get("meaningKo", ""),
                        record.get("exampleJa", ""),
                        record.get("exampleKo", ""),
                        record.get("tag", ""),
                        record.get("note", ""),
                        now_base + 100 + index,
                    ),
                )
                word_id = int(cursor.lastrowid)
                signature_to_word_id[signature] = word_id

            deck_key = record["deck"]
            if deck_key not in deck_id_by_key:
                continue
            if signature in linked_signatures_by_deck[deck_key]:
                continue

            cursor.execute(
                """
                INSERT INTO deck_word_cross_ref (deckId, wordId, displayOrder, addedAt)
                VALUES (?, ?, ?, ?)
                """,
                (
                    deck_id_by_key[deck_key],
                    word_id,
                    display_order_by_deck[deck_key],
                    now_base + 100 + index,
                ),
            )
            linked_signatures_by_deck[deck_key].add(signature)
            display_order_by_deck[deck_key] += 1

        cursor.execute(
            "INSERT INTO app_settings(key, value) VALUES (?, ?)",
            (SEED_KEY, SEEDED_VALUE),
        )

        connection.commit()
        print(
            json.dumps(
                {
                    "output": str(output_path),
                    "decks": {key: display_order_by_deck[key] for key, *_ in DEFAULT_DECKS},
                    "words": len(signature_to_word_id),
                },
                ensure_ascii=False,
            )
        )
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
