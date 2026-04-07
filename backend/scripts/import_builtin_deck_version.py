import argparse
import json
import os
import sys
import time
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))
os.environ.setdefault("DATABASE_URL", f"sqlite:///{(Path(__file__).resolve().parents[1] / 'data' / 'wordbook_server.db').as_posix()}")

from database import Base, SessionLocal, engine, migrate_legacy_sync_schema
from models import BuiltinDeckCatalog, BuiltinDeckVersion, BuiltinDeckWordSnapshot, BuiltinWordMapping


def main():
    parser = argparse.ArgumentParser(description="기본 덱 버전을 서버 DB에 등록합니다.")
    parser.add_argument("json_path", help="버전 데이터 JSON 파일 경로")
    args = parser.parse_args()

    payload = json.loads(Path(args.json_path).read_text(encoding="utf-8"))
    migrate_legacy_sync_schema()
    Base.metadata.create_all(bind=engine)
    now = int(time.time() * 1000)
    session = SessionLocal()
    try:
        catalog = session.query(BuiltinDeckCatalog).filter(
            BuiltinDeckCatalog.stable_key == payload["stable_key"]
        ).first()
        if catalog is None:
            catalog = BuiltinDeckCatalog(
                stable_key=payload["stable_key"],
                name=payload["name"],
                type=payload.get("type", "JLPT"),
            )
            session.add(catalog)
            session.flush()
        else:
            catalog.name = payload["name"]
            catalog.type = payload.get("type", catalog.type)

        version = session.query(BuiltinDeckVersion).filter(
            BuiltinDeckVersion.catalog_id == catalog.id,
            BuiltinDeckVersion.version_code == payload["version_code"],
        ).first()
        if version is None:
            version = BuiltinDeckVersion(
                catalog_id=catalog.id,
                version_code=payload["version_code"],
                version_label=payload.get("version_label", f"v{payload['version_code']}"),
                changelog=payload.get("changelog", ""),
                published_at=payload.get("published_at", now),
                is_active=True,
            )
            session.add(version)
            session.flush()
        else:
            version.version_label = payload.get("version_label", version.version_label)
            version.changelog = payload.get("changelog", version.changelog)
            version.published_at = payload.get("published_at", version.published_at)
            version.is_active = True

        session.query(BuiltinDeckWordSnapshot).filter(
            BuiltinDeckWordSnapshot.deck_version_id == version.id
        ).delete()
        for item in payload.get("words", []):
            session.add(
                BuiltinDeckWordSnapshot(
                    deck_version_id=version.id,
                    word_id=item["id"],
                    reading_ja=item["reading_ja"],
                    reading_ko=item.get("reading_ko", ""),
                    part_of_speech=item.get("part_of_speech", ""),
                    grammar=item.get("grammar", ""),
                    kanji=item.get("kanji", ""),
                    meaning_ja=item.get("meaning_ja", ""),
                    meaning_ko=item.get("meaning_ko", ""),
                    example_ja=item.get("example_ja", ""),
                    example_ko=item.get("example_ko", ""),
                    tag=item.get("tag", ""),
                    note=item.get("note", ""),
                    is_kana_only=item.get("is_kana_only", False),
                    created_at=item.get("created_at", now),
                    display_order=item["display_order"],
                )
            )

        from_version_code = payload.get("from_version_code")
        if from_version_code is not None:
            session.query(BuiltinWordMapping).filter(
                BuiltinWordMapping.catalog_id == catalog.id,
                BuiltinWordMapping.from_version_code == from_version_code,
                BuiltinWordMapping.to_version_code == payload["version_code"],
            ).delete()
            for item in payload.get("mappings", []):
                session.add(
                    BuiltinWordMapping(
                        catalog_id=catalog.id,
                        from_version_code=from_version_code,
                        to_version_code=payload["version_code"],
                        old_word_id=item["old_word_id"],
                        new_word_id=item.get("new_word_id"),
                        mapping_type=item["mapping_type"],
                    )
                )

        session.commit()
        print(f"Imported builtin deck {payload['stable_key']} version {payload['version_code']}")
    finally:
        session.close()


if __name__ == "__main__":
    main()
