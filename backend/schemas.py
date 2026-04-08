from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class RegisterRequest(BaseModel):
    username: str
    password: str


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str


class WordSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    reading_ja: str
    reading_ko: str
    part_of_speech: str
    grammar: str
    kanji: str
    meaning_ja: str
    meaning_ko: str
    example_ja: str
    example_ko: str
    tag: str
    note: str
    is_kana_only: bool
    created_at: int


class DeckSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    description: str
    type: str
    source_tag: str
    stable_key: Optional[str] = None
    deck_version_code: Optional[int] = None
    is_builtin: Optional[bool] = None
    display_order: int
    created_at: int


class DeckInstallStateSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    deck_id: int
    stable_key: str
    current_version_code: int
    latest_known_version_code: int
    update_available: bool
    is_legacy_version: bool
    last_checked_at: int


class DeckWordRefSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    deck_id: int
    word_id: int
    display_order: int
    added_at: int


class TestSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    status: str
    deck_id: Optional[int]
    deck_name_snapshot: str
    source_deck_stable_key: Optional[str] = None
    source_deck_version_code: Optional[int] = None
    is_ai_deck: bool
    only_unseen_words: bool = False
    exclude_kana_only: bool = False
    wrong_only: bool = False
    word_order: str
    front_field: str
    reveal_field: Optional[str] = None
    reveal_fields: List[str] = Field(default_factory=lambda: ["READING_JA"])
    word_ids_serialized: str
    total_word_count: int
    started_at: int
    changed_at: int


class TestWordLogSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    test_id: int
    word_id: int
    sequence_index: int
    is_correct: bool
    answered_at: int


class EndedTestResultSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    test_id: int
    deck_id: Optional[int]
    deck_name_snapshot: str
    source_deck_stable_key: Optional[str] = None
    source_deck_version_code: Optional[int] = None
    is_ai_deck: bool
    total_word_count: int
    correct_count: int
    wrong_count: int
    accuracy_percent: int
    started_at: int
    ended_at: int
    duration_seconds: int


class BuiltinWordMappingSchema(BaseModel):
    old_word_id: int
    new_word_id: Optional[int] = None
    mapping_type: str


class BuiltinDeckUpdatePackageSchema(BaseModel):
    stable_key: str
    name: str
    current_version_code: int
    target_version_code: int
    target_version_label: str
    changelog: str
    update_available: bool
    words: List[WordSchema]
    deck_word_refs: List[DeckWordRefSchema]
    mappings: List[BuiltinWordMappingSchema]


class SyncPayload(BaseModel):
    words: List[WordSchema]
    decks: List[DeckSchema]
    deck_install_states: List[DeckInstallStateSchema] = []
    deck_word_refs: List[DeckWordRefSchema]
    tests: List[TestSchema]
    test_word_logs: List[TestWordLogSchema]
    ended_test_results: List[EndedTestResultSchema]
    client_sync_version: Optional[int] = None
    synced_at: int


class SyncResponse(SyncPayload):
    pass
