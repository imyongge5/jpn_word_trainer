from sqlalchemy import BigInteger, Boolean, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String, unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String, nullable=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class UserSyncProfile(Base):
    __tablename__ = "user_sync_profile"
    __table_args__ = (UniqueConstraint("user_id", name="uq_user_sync_profile_user"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    client_sync_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    protocol_mode: Mapped[str] = mapped_column(String, nullable=False, default="LEGACY_V1")
    last_seen_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class UserBuiltinDeckInstall(Base):
    __tablename__ = "user_builtin_deck_install"
    __table_args__ = (UniqueConstraint("user_id", "stable_key", name="uq_user_builtin_deck_install"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    deck_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    stable_key: Mapped[str] = mapped_column(String, nullable=False)
    current_version_code: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    latest_known_version_code: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    update_available: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    is_legacy_version: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    last_checked_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class BuiltinDeckCatalog(Base):
    __tablename__ = "builtin_deck_catalog"
    __table_args__ = (UniqueConstraint("stable_key", name="uq_builtin_deck_catalog_stable_key"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    stable_key: Mapped[str] = mapped_column(String, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    type: Mapped[str] = mapped_column(String, nullable=False, default="JLPT")


class BuiltinDeckVersion(Base):
    __tablename__ = "builtin_deck_version"
    __table_args__ = (UniqueConstraint("catalog_id", "version_code", name="uq_builtin_deck_version_catalog_version"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    catalog_id: Mapped[int] = mapped_column(ForeignKey("builtin_deck_catalog.id"), nullable=False)
    version_code: Mapped[int] = mapped_column(Integer, nullable=False)
    version_label: Mapped[str] = mapped_column(String, nullable=False)
    changelog: Mapped[str] = mapped_column(String, nullable=False, default="")
    published_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class BuiltinDeckWordSnapshot(Base):
    __tablename__ = "builtin_deck_word_snapshot"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    deck_version_id: Mapped[int] = mapped_column(ForeignKey("builtin_deck_version.id"), nullable=False)
    word_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    reading_ja: Mapped[str] = mapped_column(String, nullable=False)
    reading_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    part_of_speech: Mapped[str] = mapped_column(String, nullable=False, default="")
    grammar: Mapped[str] = mapped_column(String, nullable=False, default="")
    kanji: Mapped[str] = mapped_column(String, nullable=False, default="")
    meaning_ja: Mapped[str] = mapped_column(String, nullable=False, default="")
    meaning_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    example_ja: Mapped[str] = mapped_column(String, nullable=False, default="")
    example_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    tag: Mapped[str] = mapped_column(String, nullable=False, default="")
    note: Mapped[str] = mapped_column(String, nullable=False, default="")
    is_kana_only: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)


class BuiltinWordMapping(Base):
    __tablename__ = "builtin_word_mapping"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    catalog_id: Mapped[int] = mapped_column(ForeignKey("builtin_deck_catalog.id"), nullable=False)
    from_version_code: Mapped[int] = mapped_column(Integer, nullable=False)
    to_version_code: Mapped[int] = mapped_column(Integer, nullable=False)
    old_word_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    new_word_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    mapping_type: Mapped[str] = mapped_column(String, nullable=False)


class Word(Base):
    __tablename__ = "words"
    __table_args__ = (UniqueConstraint("user_id", "client_id", name="uq_words_user_client_id"),)

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    client_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    reading_ja: Mapped[str] = mapped_column(String, nullable=False)
    reading_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    part_of_speech: Mapped[str] = mapped_column(String, nullable=False, default="")
    grammar: Mapped[str] = mapped_column(String, nullable=False, default="")
    kanji: Mapped[str] = mapped_column(String, nullable=False, default="")
    meaning_ja: Mapped[str] = mapped_column(String, nullable=False, default="")
    meaning_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    example_ja: Mapped[str] = mapped_column(String, nullable=False, default="")
    example_ko: Mapped[str] = mapped_column(String, nullable=False, default="")
    tag: Mapped[str] = mapped_column(String, nullable=False, default="")
    note: Mapped[str] = mapped_column(String, nullable=False, default="")
    is_kana_only: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class Deck(Base):
    __tablename__ = "decks"
    __table_args__ = (UniqueConstraint("user_id", "client_id", name="uq_decks_user_client_id"),)

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    client_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str] = mapped_column(String, nullable=False, default="")
    type: Mapped[str] = mapped_column(String, nullable=False)
    source_tag: Mapped[str] = mapped_column(String, nullable=False, default="")
    stable_key: Mapped[str | None] = mapped_column(String, nullable=True)
    deck_version_code: Mapped[int | None] = mapped_column(Integer, nullable=True)
    is_builtin: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class DeckWordCrossRef(Base):
    __tablename__ = "deck_word_cross_ref"
    __table_args__ = (UniqueConstraint("user_id", "deck_id", "word_id", name="uq_deck_word_ref_user_ids"),)

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    deck_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    word_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)
    added_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class Test(Base):
    __tablename__ = "tests"
    __table_args__ = (UniqueConstraint("user_id", "client_id", name="uq_tests_user_client_id"),)

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    client_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    status: Mapped[str] = mapped_column(String, nullable=False)
    deck_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    deck_name_snapshot: Mapped[str] = mapped_column(String, nullable=False, default="")
    source_deck_stable_key: Mapped[str | None] = mapped_column(String, nullable=True)
    source_deck_version_code: Mapped[int | None] = mapped_column(Integer, nullable=True)
    is_ai_deck: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    only_unseen_words: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    exclude_kana_only: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    wrong_only: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    word_order: Mapped[str] = mapped_column(String, nullable=False)
    front_field: Mapped[str] = mapped_column(String, nullable=False)
    reveal_field: Mapped[str] = mapped_column(String, nullable=False, default="READING_JA")
    reveal_fields_serialized: Mapped[str] = mapped_column(String, nullable=False, default="READING_JA")
    word_ids_serialized: Mapped[str] = mapped_column(String, nullable=False, default="")
    total_word_count: Mapped[int] = mapped_column(Integer, nullable=False)
    started_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    changed_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class TestWordLog(Base):
    __tablename__ = "test_word_log"
    __table_args__ = (UniqueConstraint("user_id", "client_id", name="uq_test_word_log_user_client_id"),)

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    client_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    test_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    word_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    sequence_index: Mapped[int] = mapped_column(Integer, nullable=False)
    is_correct: Mapped[bool] = mapped_column(Boolean, nullable=False)
    answered_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class EndedTestResult(Base):
    __tablename__ = "ended_test_result"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_ended_test_result_user_client_id"),
        UniqueConstraint("user_id", "test_id", name="uq_ended_test_result_user_test"),
    )

    server_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    client_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    test_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    deck_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    deck_name_snapshot: Mapped[str] = mapped_column(String, nullable=False, default="")
    source_deck_stable_key: Mapped[str | None] = mapped_column(String, nullable=True)
    source_deck_version_code: Mapped[int | None] = mapped_column(Integer, nullable=True)
    is_ai_deck: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    total_word_count: Mapped[int] = mapped_column(Integer, nullable=False)
    correct_count: Mapped[int] = mapped_column(Integer, nullable=False)
    wrong_count: Mapped[int] = mapped_column(Integer, nullable=False)
    accuracy_percent: Mapped[int] = mapped_column(Integer, nullable=False)
    started_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    ended_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    duration_seconds: Mapped[int] = mapped_column(BigInteger, nullable=False)
