from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from auth import get_current_user
from database import get_db
from models import (
    BuiltinDeckCatalog,
    BuiltinDeckVersion,
    BuiltinDeckWordSnapshot,
    BuiltinWordMapping,
    User,
    UserBuiltinDeckInstall,
)
from schemas import BuiltinDeckUpdatePackageSchema, DeckWordRefSchema, BuiltinWordMappingSchema, WordSchema


router = APIRouter(prefix="/builtin-decks", tags=["builtin-decks"])


def _normalize_builtin_stable_key(value: str) -> str:
    normalized = value.strip().upper()
    if normalized.startswith("JLPT "):
        normalized = normalized.removeprefix("JLPT ").strip()
    return normalized


@router.get("/{stable_key}/update-package", response_model=BuiltinDeckUpdatePackageSchema)
def get_update_package(
    stable_key: str,
    current_version_code: int = Query(..., alias="current_version_code"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    normalized_stable_key = _normalize_builtin_stable_key(stable_key)
    catalog = db.query(BuiltinDeckCatalog).filter(BuiltinDeckCatalog.stable_key == normalized_stable_key).first()
    if catalog is None:
        raise HTTPException(status_code=404, detail="기본 덱 카탈로그를 찾지 못했습니다.")

    latest_version = (
        db.query(BuiltinDeckVersion)
        .filter(BuiltinDeckVersion.catalog_id == catalog.id)
        .order_by(BuiltinDeckVersion.version_code.desc())
        .first()
    )
    if latest_version is None:
        raise HTTPException(status_code=404, detail="기본 덱 버전이 등록되지 않았습니다.")

    install = (
        db.query(UserBuiltinDeckInstall)
        .filter(
            UserBuiltinDeckInstall.user_id == current_user.id,
            UserBuiltinDeckInstall.stable_key == normalized_stable_key,
        )
        .first()
    )
    current_version = install.current_version_code if install is not None else current_version_code
    update_available = latest_version.version_code > current_version
    snapshot_version = latest_version if update_available else (
        db.query(BuiltinDeckVersion)
        .filter(
            BuiltinDeckVersion.catalog_id == catalog.id,
            BuiltinDeckVersion.version_code == current_version,
        )
        .first() or latest_version
    )
    words = (
        db.query(BuiltinDeckWordSnapshot)
        .filter(BuiltinDeckWordSnapshot.deck_version_id == snapshot_version.id)
        .order_by(BuiltinDeckWordSnapshot.display_order.asc())
        .all()
    )
    mappings = (
        db.query(BuiltinWordMapping)
        .filter(
            BuiltinWordMapping.catalog_id == catalog.id,
            BuiltinWordMapping.from_version_code == current_version,
            BuiltinWordMapping.to_version_code == latest_version.version_code,
        )
        .all()
    ) if update_available else []

    return BuiltinDeckUpdatePackageSchema(
        stable_key=normalized_stable_key,
        name=catalog.name,
        current_version_code=current_version,
        target_version_code=latest_version.version_code,
        target_version_label=latest_version.version_label,
        changelog=latest_version.changelog,
        update_available=update_available,
        words=[
            WordSchema(
                id=item.word_id,
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
            for item in words
        ],
        deck_word_refs=[
            DeckWordRefSchema(
                deck_id=0,
                word_id=item.word_id,
                display_order=item.display_order,
                added_at=latest_version.published_at,
            )
            for item in words
        ],
        mappings=[
            BuiltinWordMappingSchema(
                old_word_id=item.old_word_id,
                new_word_id=item.new_word_id,
                mapping_type=item.mapping_type,
            )
            for item in mappings
        ],
    )
