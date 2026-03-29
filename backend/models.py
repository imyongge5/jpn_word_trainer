from sqlalchemy import BigInteger, Boolean, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String, unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String, nullable=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class Word(Base):
    __tablename__ = "words"
    __table_args__ = (UniqueConstraint("user_id", "id", name="uq_words_user_id_id"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
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
    __table_args__ = (UniqueConstraint("user_id", "id", name="uq_decks_user_id_id"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str] = mapped_column(String, nullable=False, default="")
    type: Mapped[str] = mapped_column(String, nullable=False)
    source_tag: Mapped[str] = mapped_column(String, nullable=False, default="")
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class DeckWordCrossRef(Base):
    __tablename__ = "deck_word_cross_ref"

    deck_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    word_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    display_order: Mapped[int] = mapped_column(Integer, nullable=False)
    added_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class Test(Base):
    __tablename__ = "tests"
    __table_args__ = (UniqueConstraint("user_id", "id", name="uq_tests_user_id_id"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String, nullable=False)
    deck_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    deck_name_snapshot: Mapped[str] = mapped_column(String, nullable=False, default="")
    is_ai_deck: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    word_order: Mapped[str] = mapped_column(String, nullable=False)
    front_field: Mapped[str] = mapped_column(String, nullable=False)
    reveal_field: Mapped[str] = mapped_column(String, nullable=False)
    word_ids_serialized: Mapped[str] = mapped_column(String, nullable=False, default="")
    total_word_count: Mapped[int] = mapped_column(Integer, nullable=False)
    started_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    changed_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class TestWordLog(Base):
    __tablename__ = "test_word_log"
    __table_args__ = (UniqueConstraint("user_id", "id", name="uq_test_word_log_user_id_id"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    test_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    word_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    sequence_index: Mapped[int] = mapped_column(Integer, nullable=False)
    is_correct: Mapped[bool] = mapped_column(Boolean, nullable=False)
    answered_at: Mapped[int] = mapped_column(BigInteger, nullable=False)


class EndedTestResult(Base):
    __tablename__ = "ended_test_result"
    __table_args__ = (UniqueConstraint("user_id", "test_id", name="uq_ended_test_result_user_test"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    test_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    deck_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    deck_name_snapshot: Mapped[str] = mapped_column(String, nullable=False, default="")
    is_ai_deck: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    total_word_count: Mapped[int] = mapped_column(Integer, nullable=False)
    correct_count: Mapped[int] = mapped_column(Integer, nullable=False)
    wrong_count: Mapped[int] = mapped_column(Integer, nullable=False)
    accuracy_percent: Mapped[int] = mapped_column(Integer, nullable=False)
    started_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    ended_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    duration_seconds: Mapped[int] = mapped_column(BigInteger, nullable=False)
