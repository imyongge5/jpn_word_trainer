import time

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth import get_current_user
from database import get_db
from models import (
    Deck,
    DeckWordCrossRef,
    EndedTestResult,
    Test,
    TestWordLog,
    User,
    UserBuiltinDeckInstall,
    UserSyncProfile,
    Word,
)
from schemas import (
    DeckInstallStateSchema,
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


def _now_millis() -> int:
    return int(time.time() * 1000)


def _resolve_protocol(payload: SyncPayload) -> tuple[int, str]:
    client_sync_version = payload.client_sync_version or 1
    return client_sync_version, "MODERN_V2" if client_sync_version >= 2 else "LEGACY_V1"


def _normalize_builtin_stable_key(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip().upper()
    if not normalized:
        return None
    if normalized.startswith("JLPT "):
        normalized = normalized.removeprefix("JLPT ").strip()
    if normalized in {"N1", "N2", "N3", "N4", "N5"}:
        return normalized
    return value.strip()


def _upsert_sync_profile(db: Session, user_id: int, client_sync_version: int, protocol_mode: str):
    existing = db.query(UserSyncProfile).filter(UserSyncProfile.user_id == user_id).first()
    now = _now_millis()
    if existing is None:
        db.add(
            UserSyncProfile(
                user_id=user_id,
                client_sync_version=client_sync_version,
                protocol_mode=protocol_mode,
                last_seen_at=now,
            )
        )
    else:
        existing.client_sync_version = client_sync_version
        existing.protocol_mode = protocol_mode
        existing.last_seen_at = now


def _merge_words(db: Session, user_id: int, payload: SyncPayload):
    existing = {
        item.client_id: item
        for item in db.query(Word).filter(Word.user_id == user_id).all()
    }
    for item in payload.words:
        target = existing.get(item.id)
        if target is None:
            db.add(
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
            )
            continue
        target.reading_ja = item.reading_ja
        target.reading_ko = item.reading_ko
        target.part_of_speech = item.part_of_speech
        target.grammar = item.grammar
        target.kanji = item.kanji
        target.meaning_ja = item.meaning_ja
        target.meaning_ko = item.meaning_ko
        target.example_ja = item.example_ja
        target.example_ko = item.example_ko
        target.tag = item.tag
        target.note = item.note
        target.is_kana_only = item.is_kana_only
        target.created_at = item.created_at


def _merge_custom_decks(db: Session, user_id: int, payload: SyncPayload) -> set[int]:
    custom_decks = [item for item in payload.decks if item.type == "CUSTOM"]
    existing = {
        item.client_id: item
        for item in db.query(Deck).filter(Deck.user_id == user_id).all()
    }
    custom_ids = set()
    for item in custom_decks:
        custom_ids.add(item.id)
        target = existing.get(item.id)
        if target is None:
            db.add(
                Deck(
                    client_id=item.id,
                    user_id=user_id,
                    name=item.name,
                    description=item.description,
                    type=item.type,
                    source_tag=item.source_tag,
                    stable_key=_normalize_builtin_stable_key(item.stable_key or item.source_tag),
                    deck_version_code=item.deck_version_code,
                    is_builtin=item.is_builtin or False,
                    display_order=item.display_order,
                    created_at=item.created_at,
                )
            )
            continue
        target.name = item.name
        target.description = item.description
        target.type = item.type
        target.source_tag = item.source_tag
        target.stable_key = _normalize_builtin_stable_key(item.stable_key or item.source_tag)
        target.deck_version_code = item.deck_version_code
        target.is_builtin = item.is_builtin or False
        target.display_order = item.display_order
        target.created_at = item.created_at
    return custom_ids


def _merge_custom_refs(db: Session, user_id: int, payload: SyncPayload, custom_deck_ids: set[int]):
    if not custom_deck_ids:
        return
    existing = {
        (item.deck_id, item.word_id): item
        for item in db.query(DeckWordCrossRef).filter(DeckWordCrossRef.user_id == user_id).all()
    }
    for item in payload.deck_word_refs:
        if item.deck_id not in custom_deck_ids:
            continue
        target = existing.get((item.deck_id, item.word_id))
        if target is None:
            db.add(
                DeckWordCrossRef(
                    user_id=user_id,
                    deck_id=item.deck_id,
                    word_id=item.word_id,
                    display_order=item.display_order,
                    added_at=item.added_at,
                )
            )
            continue
        target.display_order = item.display_order
        target.added_at = item.added_at


def _merge_tests(db: Session, user_id: int, payload: SyncPayload):
    existing = {
        item.client_id: item
        for item in db.query(Test).filter(Test.user_id == user_id).all()
    }
    for item in payload.tests:
        resolved_reveal_fields = item.reveal_fields or ([item.reveal_field] if item.reveal_field else [])
        primary_reveal_field = resolved_reveal_fields[0] if resolved_reveal_fields else "READING_JA"
        target = existing.get(item.id)
        if target is None:
            db.add(
                Test(
                    client_id=item.id,
                    user_id=user_id,
                    status=item.status,
                    deck_id=item.deck_id,
                    deck_name_snapshot=item.deck_name_snapshot,
                    source_deck_stable_key=item.source_deck_stable_key,
                    source_deck_version_code=item.source_deck_version_code,
                    is_ai_deck=item.is_ai_deck,
                    only_unseen_words=item.only_unseen_words,
                    exclude_kana_only=item.exclude_kana_only,
                    wrong_only=item.wrong_only,
                    word_order=item.word_order,
                    front_field=item.front_field,
                    reveal_field=primary_reveal_field,
                    reveal_fields_serialized=",".join(resolved_reveal_fields) or primary_reveal_field,
                    word_ids_serialized=item.word_ids_serialized,
                    total_word_count=item.total_word_count,
                    started_at=item.started_at,
                    changed_at=item.changed_at,
                )
            )
            continue
        if item.changed_at >= target.changed_at:
            target.status = item.status
            target.deck_id = item.deck_id
            target.deck_name_snapshot = item.deck_name_snapshot
            target.source_deck_stable_key = item.source_deck_stable_key or target.source_deck_stable_key
            target.source_deck_version_code = item.source_deck_version_code or target.source_deck_version_code
            target.is_ai_deck = item.is_ai_deck
            target.only_unseen_words = item.only_unseen_words
            target.exclude_kana_only = item.exclude_kana_only
            target.wrong_only = item.wrong_only
            target.word_order = item.word_order
            target.front_field = item.front_field
            target.reveal_field = primary_reveal_field
            target.reveal_fields_serialized = ",".join(resolved_reveal_fields) or primary_reveal_field
            target.word_ids_serialized = item.word_ids_serialized
            target.total_word_count = item.total_word_count
            target.started_at = item.started_at
            target.changed_at = item.changed_at


def _merge_logs(db: Session, user_id: int, payload: SyncPayload):
    existing = {
        item.client_id: item
        for item in db.query(TestWordLog).filter(TestWordLog.user_id == user_id).all()
    }
    for item in payload.test_word_logs:
        if item.id in existing:
            continue
        db.add(
            TestWordLog(
                client_id=item.id,
                user_id=user_id,
                test_id=item.test_id,
                word_id=item.word_id,
                sequence_index=item.sequence_index,
                is_correct=item.is_correct,
                answered_at=item.answered_at,
            )
        )


def _merge_results(db: Session, user_id: int, payload: SyncPayload):
    existing = {
        item.client_id: item
        for item in db.query(EndedTestResult).filter(EndedTestResult.user_id == user_id).all()
    }
    for item in payload.ended_test_results:
        target = existing.get(item.id)
        if target is None:
            db.add(
                EndedTestResult(
                    client_id=item.id,
                    user_id=user_id,
                    test_id=item.test_id,
                    deck_id=item.deck_id,
                    deck_name_snapshot=item.deck_name_snapshot,
                    source_deck_stable_key=item.source_deck_stable_key,
                    source_deck_version_code=item.source_deck_version_code,
                    is_ai_deck=item.is_ai_deck,
                    total_word_count=item.total_word_count,
                    correct_count=item.correct_count,
                    wrong_count=item.wrong_count,
                    accuracy_percent=item.accuracy_percent,
                    started_at=item.started_at,
                    ended_at=item.ended_at,
                    duration_seconds=item.duration_seconds,
                )
            )
            continue
        if item.ended_at >= target.ended_at:
            target.test_id = item.test_id
            target.deck_id = item.deck_id
            target.deck_name_snapshot = item.deck_name_snapshot
            target.source_deck_stable_key = item.source_deck_stable_key or target.source_deck_stable_key
            target.source_deck_version_code = item.source_deck_version_code or target.source_deck_version_code
            target.is_ai_deck = item.is_ai_deck
            target.total_word_count = item.total_word_count
            target.correct_count = item.correct_count
            target.wrong_count = item.wrong_count
            target.accuracy_percent = item.accuracy_percent
            target.started_at = item.started_at
            target.ended_at = item.ended_at
            target.duration_seconds = item.duration_seconds


def _upsert_builtin_installs(db: Session, user_id: int, payload: SyncPayload, protocol_mode: str):
    existing = {
        item.stable_key: item
        for item in db.query(UserBuiltinDeckInstall).filter(UserBuiltinDeckInstall.user_id == user_id).all()
    }
    now = _now_millis()
    incoming = payload.deck_install_states
    if not incoming:
        incoming = [
            DeckInstallStateSchema(
                deck_id=item.id,
                stable_key=_normalize_builtin_stable_key(item.stable_key or item.source_tag) or (item.stable_key or item.source_tag),
                current_version_code=item.deck_version_code or 1,
                latest_known_version_code=item.deck_version_code or 1,
                update_available=False,
                is_legacy_version=protocol_mode == "LEGACY_V1",
                last_checked_at=now,
            )
            for item in payload.decks
            if item.type != "CUSTOM" and (item.stable_key or item.source_tag)
        ]

    for item in incoming:
        normalized_stable_key = _normalize_builtin_stable_key(item.stable_key) or item.stable_key
        target = existing.get(normalized_stable_key)
        if target is None:
            db.add(
                UserBuiltinDeckInstall(
                    user_id=user_id,
                    deck_id=item.deck_id,
                    stable_key=normalized_stable_key,
                    current_version_code=item.current_version_code,
                    latest_known_version_code=item.latest_known_version_code,
                    update_available=item.update_available,
                    is_legacy_version=item.is_legacy_version,
                    last_checked_at=item.last_checked_at,
                )
            )
            continue
        target.deck_id = item.deck_id
        target.stable_key = normalized_stable_key
        target.current_version_code = item.current_version_code
        target.latest_known_version_code = item.latest_known_version_code
        target.update_available = item.update_available
        target.is_legacy_version = item.is_legacy_version
        target.last_checked_at = item.last_checked_at


def _merge_user_data(db: Session, user_id: int, payload: SyncPayload):
    client_sync_version, protocol_mode = _resolve_protocol(payload)
    _upsert_sync_profile(db, user_id, client_sync_version, protocol_mode)
    _merge_words(db, user_id, payload)
    custom_deck_ids = _merge_custom_decks(db, user_id, payload)
    _merge_custom_refs(db, user_id, payload, custom_deck_ids)
    _merge_tests(db, user_id, payload)
    _merge_logs(db, user_id, payload)
    _merge_results(db, user_id, payload)
    _upsert_builtin_installs(db, user_id, payload, protocol_mode)
    return client_sync_version, protocol_mode


@router.post("/push")
def push_data(
    payload: SyncPayload,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    client_sync_version, protocol_mode = _merge_user_data(db, current_user.id, payload)
    db.commit()
    return {
        "synced": True,
        "synced_at": payload.synced_at,
        "client_sync_version": client_sync_version,
        "protocol_mode": protocol_mode,
    }


@router.get("/pull", response_model=SyncResponse)
def pull_data(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user_id = current_user.id
    profile = db.query(UserSyncProfile).filter(UserSyncProfile.user_id == user_id).first()
    protocol_mode = profile.protocol_mode if profile is not None else "LEGACY_V1"
    client_sync_version = profile.client_sync_version if profile is not None else 1

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
                stable_key=item.stable_key if protocol_mode != "LEGACY_V1" else None,
                deck_version_code=item.deck_version_code if protocol_mode != "LEGACY_V1" else None,
                is_builtin=item.is_builtin if protocol_mode != "LEGACY_V1" else None,
                display_order=item.display_order,
                created_at=item.created_at,
            )
            for item in db.query(Deck).filter(Deck.user_id == user_id).all()
        ],
        deck_install_states=[] if protocol_mode == "LEGACY_V1" else [
            DeckInstallStateSchema(
                deck_id=item.deck_id or 0,
                stable_key=item.stable_key,
                current_version_code=item.current_version_code,
                latest_known_version_code=item.latest_known_version_code,
                update_available=item.update_available,
                is_legacy_version=item.is_legacy_version,
                last_checked_at=item.last_checked_at,
            )
            for item in db.query(UserBuiltinDeckInstall).filter(UserBuiltinDeckInstall.user_id == user_id).all()
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
                source_deck_stable_key=item.source_deck_stable_key if protocol_mode != "LEGACY_V1" else None,
                source_deck_version_code=item.source_deck_version_code if protocol_mode != "LEGACY_V1" else None,
                is_ai_deck=item.is_ai_deck,
                only_unseen_words=item.only_unseen_words,
                exclude_kana_only=item.exclude_kana_only,
                wrong_only=item.wrong_only,
                word_order=item.word_order,
                front_field=item.front_field,
                reveal_field=item.reveal_field,
                reveal_fields=[field for field in item.reveal_fields_serialized.split(",") if field],
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
                source_deck_stable_key=item.source_deck_stable_key if protocol_mode != "LEGACY_V1" else None,
                source_deck_version_code=item.source_deck_version_code if protocol_mode != "LEGACY_V1" else None,
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
        client_sync_version=None if protocol_mode == "LEGACY_V1" else client_sync_version,
        synced_at=0,
    )
