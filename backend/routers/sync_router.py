from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth import get_current_user
from database import get_db
from models import Deck, DeckWordCrossRef, EndedTestResult, Test, TestWordLog, User, Word
from schemas import (
    DeckSchema,
    DeckWordRefSchema,
    EndedTestResultSchema,
    SyncPayload,
    SyncResponse,
    TestSchema,
    TestWordLogSchema,
    WordSchema,
)


router = APIRouter(prefix="/sync", tags=["sync"])


def _replace_user_data(db: Session, user_id: int, payload: SyncPayload):
    db.query(EndedTestResult).filter(EndedTestResult.user_id == user_id).delete()
    db.query(TestWordLog).filter(TestWordLog.user_id == user_id).delete()
    db.query(Test).filter(Test.user_id == user_id).delete()
    db.query(DeckWordCrossRef).filter(DeckWordCrossRef.user_id == user_id).delete()
    db.query(Deck).filter(Deck.user_id == user_id).delete()
    db.query(Word).filter(Word.user_id == user_id).delete()

    db.add_all(
        [
            Word(
                client_id=item.id,
                user_id=user_id,
                reading_ja=item.reading_ja,
                reading_ko=item.reading_ko,
                part_of_speech=item.part_of_speech,
                grammar=item.grammar,
                kanji=item.kanji,
                meaning_ja=item.meaning_ja,
                meaning_ko=item.meaning_ko,
                example_ja=item.example_ja,
                example_ko=item.example_ko,
                tag=item.tag,
                note=item.note,
                is_kana_only=item.is_kana_only,
                created_at=item.created_at,
            )
            for item in payload.words
        ],
    )
    db.add_all(
        [
            Deck(
                client_id=item.id,
                user_id=user_id,
                name=item.name,
                description=item.description,
                type=item.type,
                source_tag=item.source_tag,
                display_order=item.display_order,
                created_at=item.created_at,
            )
            for item in payload.decks
        ],
    )
    db.add_all(
        [
            DeckWordCrossRef(
                user_id=user_id,
                deck_id=item.deck_id,
                word_id=item.word_id,
                display_order=item.display_order,
                added_at=item.added_at,
            )
            for item in payload.deck_word_refs
        ],
    )
    db.add_all(
        [
            Test(
                client_id=item.id,
                user_id=user_id,
                status=item.status,
                deck_id=item.deck_id,
                deck_name_snapshot=item.deck_name_snapshot,
                is_ai_deck=item.is_ai_deck,
                only_unseen_words=item.only_unseen_words,
                word_order=item.word_order,
                front_field=item.front_field,
                reveal_field=item.reveal_field,
                word_ids_serialized=item.word_ids_serialized,
                total_word_count=item.total_word_count,
                started_at=item.started_at,
                changed_at=item.changed_at,
            )
            for item in payload.tests
        ],
    )
    db.add_all(
        [
            TestWordLog(
                client_id=item.id,
                user_id=user_id,
                test_id=item.test_id,
                word_id=item.word_id,
                sequence_index=item.sequence_index,
                is_correct=item.is_correct,
                answered_at=item.answered_at,
            )
            for item in payload.test_word_logs
        ],
    )
    db.add_all(
        [
            EndedTestResult(
                client_id=item.id,
                user_id=user_id,
                test_id=item.test_id,
                deck_id=item.deck_id,
                deck_name_snapshot=item.deck_name_snapshot,
                is_ai_deck=item.is_ai_deck,
                total_word_count=item.total_word_count,
                correct_count=item.correct_count,
                wrong_count=item.wrong_count,
                accuracy_percent=item.accuracy_percent,
                started_at=item.started_at,
                ended_at=item.ended_at,
                duration_seconds=item.duration_seconds,
            )
            for item in payload.ended_test_results
        ],
    )


@router.post("/push")
def push_data(
    payload: SyncPayload,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _replace_user_data(db, current_user.id, payload)
    db.commit()
    return {"synced": True, "synced_at": payload.synced_at}


@router.get("/pull", response_model=SyncResponse)
def pull_data(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user_id = current_user.id
    return SyncResponse(
        words=[
            WordSchema(
                id=item.client_id,
                reading_ja=item.reading_ja,
                reading_ko=item.reading_ko,
                part_of_speech=item.part_of_speech,
                grammar=item.grammar,
                kanji=item.kanji,
                meaning_ja=item.meaning_ja,
                meaning_ko=item.meaning_ko,
                example_ja=item.example_ja,
                example_ko=item.example_ko,
                tag=item.tag,
                note=item.note,
                is_kana_only=item.is_kana_only,
                created_at=item.created_at,
            )
            for item in db.query(Word).filter(Word.user_id == user_id).all()
        ],
        decks=[
            DeckSchema(
                id=item.client_id,
                name=item.name,
                description=item.description,
                type=item.type,
                source_tag=item.source_tag,
                display_order=item.display_order,
                created_at=item.created_at,
            )
            for item in db.query(Deck).filter(Deck.user_id == user_id).all()
        ],
        deck_word_refs=[
            DeckWordRefSchema(
                deck_id=item.deck_id,
                word_id=item.word_id,
                display_order=item.display_order,
                added_at=item.added_at,
            )
            for item in db.query(DeckWordCrossRef).filter(DeckWordCrossRef.user_id == user_id).all()
        ],
        tests=[
            TestSchema(
                id=item.client_id,
                status=item.status,
                deck_id=item.deck_id,
                deck_name_snapshot=item.deck_name_snapshot,
                is_ai_deck=item.is_ai_deck,
                only_unseen_words=item.only_unseen_words,
                word_order=item.word_order,
                front_field=item.front_field,
                reveal_field=item.reveal_field,
                word_ids_serialized=item.word_ids_serialized,
                total_word_count=item.total_word_count,
                started_at=item.started_at,
                changed_at=item.changed_at,
            )
            for item in db.query(Test).filter(Test.user_id == user_id).all()
        ],
        test_word_logs=[
            TestWordLogSchema(
                id=item.client_id,
                test_id=item.test_id,
                word_id=item.word_id,
                sequence_index=item.sequence_index,
                is_correct=item.is_correct,
                answered_at=item.answered_at,
            )
            for item in db.query(TestWordLog).filter(TestWordLog.user_id == user_id).all()
        ],
        ended_test_results=[
            EndedTestResultSchema(
                id=item.client_id,
                test_id=item.test_id,
                deck_id=item.deck_id,
                deck_name_snapshot=item.deck_name_snapshot,
                is_ai_deck=item.is_ai_deck,
                total_word_count=item.total_word_count,
                correct_count=item.correct_count,
                wrong_count=item.wrong_count,
                accuracy_percent=item.accuracy_percent,
                started_at=item.started_at,
                ended_at=item.ended_at,
                duration_seconds=item.duration_seconds,
            )
            for item in db.query(EndedTestResult).filter(EndedTestResult.user_id == user_id).all()
        ],
        synced_at=0,
    )
