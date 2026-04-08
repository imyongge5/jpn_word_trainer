package com.mistbottle.jpnwordtrainer.data.remote

import com.squareup.moshi.Json

data class AuthRequestDto(
    val username: String,
    val password: String,
)

data class AuthTokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    val username: String,
)

data class WordSyncDto(
    val id: Long,
    @Json(name = "reading_ja") val readingJa: String,
    @Json(name = "reading_ko") val readingKo: String,
    @Json(name = "part_of_speech") val partOfSpeech: String,
    val grammar: String,
    val kanji: String,
    @Json(name = "meaning_ja") val meaningJa: String,
    @Json(name = "meaning_ko") val meaningKo: String,
    @Json(name = "example_ja") val exampleJa: String,
    @Json(name = "example_ko") val exampleKo: String,
    val tag: String,
    val note: String,
    @Json(name = "is_kana_only") val isKanaOnly: Boolean,
    @Json(name = "created_at") val createdAt: Long,
)

data class DeckSyncDto(
    val id: Long,
    val name: String,
    val description: String,
    val type: String,
    @Json(name = "source_tag") val sourceTag: String,
    @Json(name = "stable_key") val stableKey: String? = null,
    @Json(name = "deck_version_code") val deckVersionCode: Int? = null,
    @Json(name = "is_builtin") val isBuiltin: Boolean? = null,
    @Json(name = "display_order") val displayOrder: Int,
    @Json(name = "created_at") val createdAt: Long,
)

data class DeckInstallStateSyncDto(
    @Json(name = "deck_id") val deckId: Long,
    @Json(name = "stable_key") val stableKey: String,
    @Json(name = "current_version_code") val currentVersionCode: Int,
    @Json(name = "latest_known_version_code") val latestKnownVersionCode: Int,
    @Json(name = "update_available") val updateAvailable: Boolean,
    @Json(name = "is_legacy_version") val isLegacyVersion: Boolean,
    @Json(name = "last_checked_at") val lastCheckedAt: Long,
)

data class DeckWordRefSyncDto(
    @Json(name = "deck_id") val deckId: Long,
    @Json(name = "word_id") val wordId: Long,
    @Json(name = "display_order") val displayOrder: Int,
    @Json(name = "added_at") val addedAt: Long,
)

data class TestSyncDto(
    val id: Long,
    val status: String,
    @Json(name = "deck_id") val deckId: Long?,
    @Json(name = "deck_name_snapshot") val deckNameSnapshot: String,
    @Json(name = "source_deck_stable_key") val sourceDeckStableKey: String? = null,
    @Json(name = "source_deck_version_code") val sourceDeckVersionCode: Int? = null,
    @Json(name = "is_ai_deck") val isAiDeck: Boolean,
    @Json(name = "only_unseen_words") val onlyUnseenWords: Boolean = false,
    @Json(name = "exclude_kana_only") val excludeKanaOnly: Boolean = false,
    @Json(name = "wrong_only") val wrongOnly: Boolean = false,
    @Json(name = "word_order") val wordOrder: String,
    @Json(name = "front_field") val frontField: String,
    @Json(name = "reveal_fields") val revealFields: List<String> = listOf("READING_JA"),
    @Json(name = "word_ids_serialized") val wordIdsSerialized: String,
    @Json(name = "total_word_count") val totalWordCount: Int,
    @Json(name = "started_at") val startedAt: Long,
    @Json(name = "changed_at") val changedAt: Long,
)

data class TestWordLogSyncDto(
    val id: Long,
    @Json(name = "test_id") val testId: Long,
    @Json(name = "word_id") val wordId: Long,
    @Json(name = "sequence_index") val sequenceIndex: Int,
    @Json(name = "is_correct") val isCorrect: Boolean,
    @Json(name = "answered_at") val answeredAt: Long,
)

data class EndedTestResultSyncDto(
    val id: Long,
    @Json(name = "test_id") val testId: Long,
    @Json(name = "deck_id") val deckId: Long?,
    @Json(name = "deck_name_snapshot") val deckNameSnapshot: String,
    @Json(name = "source_deck_stable_key") val sourceDeckStableKey: String? = null,
    @Json(name = "source_deck_version_code") val sourceDeckVersionCode: Int? = null,
    @Json(name = "is_ai_deck") val isAiDeck: Boolean,
    @Json(name = "total_word_count") val totalWordCount: Int,
    @Json(name = "correct_count") val correctCount: Int,
    @Json(name = "wrong_count") val wrongCount: Int,
    @Json(name = "accuracy_percent") val accuracyPercent: Int,
    @Json(name = "started_at") val startedAt: Long,
    @Json(name = "ended_at") val endedAt: Long,
    @Json(name = "duration_seconds") val durationSeconds: Long,
)

data class BuiltinWordMappingDto(
    @Json(name = "old_word_id") val oldWordId: Long,
    @Json(name = "new_word_id") val newWordId: Long?,
    @Json(name = "mapping_type") val mappingType: String,
)

data class BuiltinDeckUpdatePackageDto(
    @Json(name = "stable_key") val stableKey: String,
    val name: String,
    @Json(name = "current_version_code") val currentVersionCode: Int,
    @Json(name = "target_version_code") val targetVersionCode: Int,
    @Json(name = "target_version_label") val targetVersionLabel: String,
    val changelog: String,
    @Json(name = "update_available") val updateAvailable: Boolean,
    val words: List<WordSyncDto>,
    @Json(name = "deck_word_refs") val deckWordRefs: List<DeckWordRefSyncDto>,
    val mappings: List<BuiltinWordMappingDto>,
)

data class SyncPayloadDto(
    val words: List<WordSyncDto>,
    val decks: List<DeckSyncDto>,
    @Json(name = "deck_install_states") val deckInstallStates: List<DeckInstallStateSyncDto> = emptyList(),
    @Json(name = "deck_word_refs") val deckWordRefs: List<DeckWordRefSyncDto>,
    val tests: List<TestSyncDto>,
    @Json(name = "test_word_logs") val testWordLogs: List<TestWordLogSyncDto>,
    @Json(name = "ended_test_results") val endedTestResults: List<EndedTestResultSyncDto>,
    @Json(name = "client_sync_version") val clientSyncVersion: Int? = null,
    @Json(name = "synced_at") val syncedAt: Long,
)
