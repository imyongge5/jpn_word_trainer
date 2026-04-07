import os

from sqlalchemy import inspect, text
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker


DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./data/wordbook_server.db")

connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}

if DATABASE_URL.startswith("sqlite:///./"):
    sqlite_relative_path = DATABASE_URL.removeprefix("sqlite:///./")
    sqlite_directory = os.path.dirname(sqlite_relative_path)
    if sqlite_directory:
        os.makedirs(sqlite_directory, exist_ok=True)

engine = create_engine(DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def migrate_legacy_sync_schema() -> None:
    if engine.dialect.name != "sqlite":
        return

    inspector = inspect(engine)
    existing_tables = set(inspector.get_table_names())
    if "decks" not in existing_tables:
        return

    def has_column(table_name: str, column_name: str) -> bool:
        return any(column["name"] == column_name for column in inspector.get_columns(table_name))

    needs_rebuild = (
        ("words" in existing_tables and not has_column("words", "client_id")) or
        ("decks" in existing_tables and not has_column("decks", "client_id")) or
        ("deck_word_cross_ref" in existing_tables and not has_column("deck_word_cross_ref", "server_id")) or
        ("tests" in existing_tables and (not has_column("tests", "client_id") or not has_column("tests", "only_unseen_words"))) or
        ("test_word_log" in existing_tables and not has_column("test_word_log", "client_id")) or
        ("ended_test_result" in existing_tables and not has_column("ended_test_result", "client_id"))
    )
    if needs_rebuild:
        with engine.begin() as connection:
            for table_name in ("words", "decks", "deck_word_cross_ref", "tests", "test_word_log", "ended_test_result"):
                if table_name in existing_tables:
                    connection.execute(text(f"ALTER TABLE {table_name} RENAME TO {table_name}_legacy"))

            connection.execute(text("""
            CREATE TABLE words (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                client_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                reading_ja VARCHAR NOT NULL,
                reading_ko VARCHAR NOT NULL DEFAULT '',
                part_of_speech VARCHAR NOT NULL DEFAULT '',
                grammar VARCHAR NOT NULL DEFAULT '',
                kanji VARCHAR NOT NULL DEFAULT '',
                meaning_ja VARCHAR NOT NULL DEFAULT '',
                meaning_ko VARCHAR NOT NULL DEFAULT '',
                example_ja VARCHAR NOT NULL DEFAULT '',
                example_ko VARCHAR NOT NULL DEFAULT '',
                tag VARCHAR NOT NULL DEFAULT '',
                note VARCHAR NOT NULL DEFAULT '',
                is_kana_only BOOLEAN NOT NULL DEFAULT 0,
                created_at BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_words_user_client_id UNIQUE (user_id, client_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_words_server_id ON words(server_id)"))
            connection.execute(text("CREATE INDEX ix_words_user_id ON words(user_id)"))

            connection.execute(text("""
            CREATE TABLE decks (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                client_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                name VARCHAR NOT NULL,
                description VARCHAR NOT NULL DEFAULT '',
                type VARCHAR NOT NULL,
                source_tag VARCHAR NOT NULL DEFAULT '',
                display_order INTEGER NOT NULL,
                created_at BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_decks_user_client_id UNIQUE (user_id, client_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_decks_server_id ON decks(server_id)"))
            connection.execute(text("CREATE INDEX ix_decks_user_id ON decks(user_id)"))

            connection.execute(text("""
            CREATE TABLE deck_word_cross_ref (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                deck_id BIGINT NOT NULL,
                word_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                display_order INTEGER NOT NULL,
                added_at BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_deck_word_ref_user_ids UNIQUE (user_id, deck_id, word_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_deck_word_cross_ref_server_id ON deck_word_cross_ref(server_id)"))
            connection.execute(text("CREATE INDEX ix_deck_word_cross_ref_user_id ON deck_word_cross_ref(user_id)"))

            connection.execute(text("""
            CREATE TABLE tests (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                client_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                status VARCHAR NOT NULL,
                deck_id BIGINT,
                deck_name_snapshot VARCHAR NOT NULL DEFAULT '',
                is_ai_deck BOOLEAN NOT NULL DEFAULT 0,
                only_unseen_words BOOLEAN NOT NULL DEFAULT 0,
                word_order VARCHAR NOT NULL,
                front_field VARCHAR NOT NULL,
                reveal_field VARCHAR NOT NULL,
                word_ids_serialized VARCHAR NOT NULL DEFAULT '',
                total_word_count INTEGER NOT NULL,
                started_at BIGINT NOT NULL,
                changed_at BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_tests_user_client_id UNIQUE (user_id, client_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_tests_server_id ON tests(server_id)"))
            connection.execute(text("CREATE INDEX ix_tests_user_id ON tests(user_id)"))

            connection.execute(text("""
            CREATE TABLE test_word_log (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                client_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                test_id BIGINT NOT NULL,
                word_id BIGINT NOT NULL,
                sequence_index INTEGER NOT NULL,
                is_correct BOOLEAN NOT NULL,
                answered_at BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_test_word_log_user_client_id UNIQUE (user_id, client_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_test_word_log_server_id ON test_word_log(server_id)"))
            connection.execute(text("CREATE INDEX ix_test_word_log_user_id ON test_word_log(user_id)"))

            connection.execute(text("""
            CREATE TABLE ended_test_result (
                server_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                client_id BIGINT NOT NULL,
                user_id INTEGER NOT NULL,
                test_id BIGINT NOT NULL,
                deck_id BIGINT,
                deck_name_snapshot VARCHAR NOT NULL DEFAULT '',
                is_ai_deck BOOLEAN NOT NULL DEFAULT 0,
                total_word_count INTEGER NOT NULL,
                correct_count INTEGER NOT NULL,
                wrong_count INTEGER NOT NULL,
                accuracy_percent INTEGER NOT NULL,
                started_at BIGINT NOT NULL,
                ended_at BIGINT NOT NULL,
                duration_seconds BIGINT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id),
                CONSTRAINT uq_ended_test_result_user_client_id UNIQUE (user_id, client_id),
                CONSTRAINT uq_ended_test_result_user_test UNIQUE (user_id, test_id)
            )
        """))
            connection.execute(text("CREATE INDEX ix_ended_test_result_server_id ON ended_test_result(server_id)"))
            connection.execute(text("CREATE INDEX ix_ended_test_result_user_id ON ended_test_result(user_id)"))

            if "words_legacy" in {row[0] for row in connection.execute(text("SELECT name FROM sqlite_master WHERE type='table'"))}:
                connection.execute(text("""
                INSERT INTO words (
                    client_id, user_id, reading_ja, reading_ko, part_of_speech, grammar, kanji,
                    meaning_ja, meaning_ko, example_ja, example_ko, tag, note, is_kana_only, created_at
                )
                SELECT id, user_id, reading_ja, reading_ko, part_of_speech, grammar, kanji,
                       meaning_ja, meaning_ko, example_ja, example_ko, tag, note, is_kana_only, created_at
                FROM words_legacy
            """))
                connection.execute(text("""
                INSERT INTO decks (
                    client_id, user_id, name, description, type, source_tag, display_order, created_at
                )
                SELECT id, user_id, name, description, type, source_tag, display_order, created_at
                FROM decks_legacy
            """))
                connection.execute(text("""
                INSERT INTO deck_word_cross_ref (
                    deck_id, word_id, user_id, display_order, added_at
                )
                SELECT deck_id, word_id, user_id, display_order, added_at
                FROM deck_word_cross_ref_legacy
            """))
                connection.execute(text("""
                INSERT INTO tests (
                    client_id, user_id, status, deck_id, deck_name_snapshot, is_ai_deck, only_unseen_words,
                    word_order, front_field, reveal_field, word_ids_serialized, total_word_count, started_at, changed_at
                )
                SELECT id, user_id, status, deck_id, deck_name_snapshot, is_ai_deck, 0,
                       word_order, front_field, reveal_field, word_ids_serialized, total_word_count, started_at, changed_at
                FROM tests_legacy
            """))
                connection.execute(text("""
                INSERT INTO test_word_log (
                    client_id, user_id, test_id, word_id, sequence_index, is_correct, answered_at
                )
                SELECT id, user_id, test_id, word_id, sequence_index, is_correct, answered_at
                FROM test_word_log_legacy
            """))
                connection.execute(text("""
                INSERT INTO ended_test_result (
                    client_id, user_id, test_id, deck_id, deck_name_snapshot, is_ai_deck,
                    total_word_count, correct_count, wrong_count, accuracy_percent, started_at, ended_at, duration_seconds
                )
                SELECT id, user_id, test_id, deck_id, deck_name_snapshot, is_ai_deck,
                       total_word_count, correct_count, wrong_count, accuracy_percent, started_at, ended_at, duration_seconds
                FROM ended_test_result_legacy
            """))

            for legacy_table in ("words_legacy", "decks_legacy", "deck_word_cross_ref_legacy", "tests_legacy", "test_word_log_legacy", "ended_test_result_legacy"):
                connection.execute(text(f"DROP TABLE IF EXISTS {legacy_table}"))

    inspector = inspect(engine)

    def has_column_now(table_name: str, column_name: str) -> bool:
        refreshed = inspect(engine)
        return any(column["name"] == column_name for column in refreshed.get_columns(table_name))

    with engine.begin() as connection:
        if "decks" in existing_tables:
            if not has_column_now("decks", "stable_key"):
                connection.execute(text("ALTER TABLE decks ADD COLUMN stable_key VARCHAR"))
            if not has_column_now("decks", "deck_version_code"):
                connection.execute(text("ALTER TABLE decks ADD COLUMN deck_version_code INTEGER"))
            if not has_column_now("decks", "is_builtin"):
                connection.execute(text("ALTER TABLE decks ADD COLUMN is_builtin BOOLEAN NOT NULL DEFAULT 0"))
            connection.execute(text("""
                UPDATE decks
                SET stable_key = COALESCE(stable_key, source_tag),
                    deck_version_code = COALESCE(deck_version_code, 1),
                    is_builtin = CASE WHEN type != 'CUSTOM' THEN 1 ELSE is_builtin END
                WHERE type != 'CUSTOM'
            """))

        if "tests" in existing_tables:
            if not has_column_now("tests", "source_deck_stable_key"):
                connection.execute(text("ALTER TABLE tests ADD COLUMN source_deck_stable_key VARCHAR"))
            if not has_column_now("tests", "source_deck_version_code"):
                connection.execute(text("ALTER TABLE tests ADD COLUMN source_deck_version_code INTEGER"))
            connection.execute(text("""
                UPDATE tests
                SET source_deck_stable_key = COALESCE(
                        source_deck_stable_key,
                        (SELECT stable_key FROM decks WHERE decks.client_id = tests.deck_id AND decks.user_id = tests.user_id LIMIT 1)
                    ),
                    source_deck_version_code = COALESCE(
                        source_deck_version_code,
                        (SELECT deck_version_code FROM decks WHERE decks.client_id = tests.deck_id AND decks.user_id = tests.user_id LIMIT 1)
                    )
                WHERE deck_id IS NOT NULL
            """))

        if "ended_test_result" in existing_tables:
            if not has_column_now("ended_test_result", "source_deck_stable_key"):
                connection.execute(text("ALTER TABLE ended_test_result ADD COLUMN source_deck_stable_key VARCHAR"))
            if not has_column_now("ended_test_result", "source_deck_version_code"):
                connection.execute(text("ALTER TABLE ended_test_result ADD COLUMN source_deck_version_code INTEGER"))
            connection.execute(text("""
                UPDATE ended_test_result
                SET source_deck_stable_key = COALESCE(
                        source_deck_stable_key,
                        (SELECT source_deck_stable_key FROM tests WHERE tests.client_id = ended_test_result.test_id AND tests.user_id = ended_test_result.user_id LIMIT 1)
                    ),
                    source_deck_version_code = COALESCE(
                        source_deck_version_code,
                        (SELECT source_deck_version_code FROM tests WHERE tests.client_id = ended_test_result.test_id AND tests.user_id = ended_test_result.user_id LIMIT 1)
                    )
            """))

        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS user_sync_profile (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                client_sync_version INTEGER NOT NULL DEFAULT 1,
                protocol_mode VARCHAR NOT NULL DEFAULT 'LEGACY_V1',
                last_seen_at BIGINT NOT NULL,
                CONSTRAINT uq_user_sync_profile_user UNIQUE (user_id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """))
        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS user_builtin_deck_install (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                deck_id BIGINT,
                stable_key VARCHAR NOT NULL,
                current_version_code INTEGER NOT NULL DEFAULT 1,
                latest_known_version_code INTEGER NOT NULL DEFAULT 1,
                update_available BOOLEAN NOT NULL DEFAULT 0,
                is_legacy_version BOOLEAN NOT NULL DEFAULT 1,
                last_checked_at BIGINT NOT NULL,
                CONSTRAINT uq_user_builtin_deck_install UNIQUE (user_id, stable_key),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """))
        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS builtin_deck_catalog (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                stable_key VARCHAR NOT NULL,
                name VARCHAR NOT NULL,
                type VARCHAR NOT NULL DEFAULT 'JLPT',
                CONSTRAINT uq_builtin_deck_catalog_stable_key UNIQUE (stable_key)
            )
        """))
        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS builtin_deck_version (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                catalog_id INTEGER NOT NULL,
                version_code INTEGER NOT NULL,
                version_label VARCHAR NOT NULL,
                changelog VARCHAR NOT NULL DEFAULT '',
                published_at BIGINT NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT 1,
                CONSTRAINT uq_builtin_deck_version_catalog_version UNIQUE (catalog_id, version_code),
                FOREIGN KEY(catalog_id) REFERENCES builtin_deck_catalog(id)
            )
        """))
        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS builtin_deck_word_snapshot (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                deck_version_id INTEGER NOT NULL,
                word_id BIGINT NOT NULL,
                reading_ja VARCHAR NOT NULL,
                reading_ko VARCHAR NOT NULL DEFAULT '',
                part_of_speech VARCHAR NOT NULL DEFAULT '',
                grammar VARCHAR NOT NULL DEFAULT '',
                kanji VARCHAR NOT NULL DEFAULT '',
                meaning_ja VARCHAR NOT NULL DEFAULT '',
                meaning_ko VARCHAR NOT NULL DEFAULT '',
                example_ja VARCHAR NOT NULL DEFAULT '',
                example_ko VARCHAR NOT NULL DEFAULT '',
                tag VARCHAR NOT NULL DEFAULT '',
                note VARCHAR NOT NULL DEFAULT '',
                is_kana_only BOOLEAN NOT NULL DEFAULT 0,
                created_at BIGINT NOT NULL,
                display_order INTEGER NOT NULL,
                FOREIGN KEY(deck_version_id) REFERENCES builtin_deck_version(id)
            )
        """))
        connection.execute(text("""
            CREATE TABLE IF NOT EXISTS builtin_word_mapping (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                catalog_id INTEGER NOT NULL,
                from_version_code INTEGER NOT NULL,
                to_version_code INTEGER NOT NULL,
                old_word_id BIGINT NOT NULL,
                new_word_id BIGINT,
                mapping_type VARCHAR NOT NULL,
                FOREIGN KEY(catalog_id) REFERENCES builtin_deck_catalog(id)
            )
        """))
