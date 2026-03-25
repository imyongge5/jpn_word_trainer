package com.example.wordbookapp.data.model

import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity
import com.example.wordbookapp.data.local.entity.WordEntity

enum class DeckType {
    JLPT,
    CUSTOM,
}

enum class ThemePreset(
    val storageValue: String,
    val displayName: String,
) {
    DEFAULT_LIGHT("default_light", "기본 라이트"),
    VSCODE_DARK_MODERN("vscode_dark_modern", "VS Code Dark Modern"),
    VSCODE_HIGH_CONTRAST("vscode_high_contrast", "VS Code High Contrast"),
    ONE_DARK_PRO("one_dark_pro", "One Dark Pro");

    companion object {
        fun fromStorage(value: String?): ThemePreset =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT_LIGHT
    }
}

enum class WordOrder {
    SEQUENTIAL,
    RANDOM,
}

enum class WordField {
    KANJI,
    READING_JA,
    READING_KO,
    MEANING_KO,
}

data class DeckWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val type: DeckType,
    val sourceTag: String,
    val displayOrder: Int,
    val createdAt: Long,
    val wordCount: Int,
)

data class WordDraft(
    val readingJa: String = "",
    val readingKo: String = "",
    val partOfSpeech: String = "",
    val grammar: String = "",
    val kanji: String = "",
    val meaningJa: String = "",
    val meaningKo: String = "",
    val exampleJa: String = "",
    val exampleKo: String = "",
    val tag: String = "",
    val note: String = "",
)

data class ExamSettings(
    val wordOrder: WordOrder = WordOrder.SEQUENTIAL,
    val frontField: WordField = WordField.KANJI,
    val revealField: WordField = WordField.READING_JA,
)

data class ExamSessionData(
    val session: StudySessionEntity,
    val words: List<WordEntity>,
    val answersCount: Int,
)

data class InProgressExamData(
    val sessionId: Long,
    val deckName: String,
    val answeredCount: Int,
    val totalCount: Int,
    val startedAt: Long,
)

data class SessionSummary(
    val sessionId: Long,
    val deckName: String,
    val totalCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
)

data class WordAggregateStat(
    val word: WordEntity,
    val attemptCount: Int,
    val wrongCount: Int,
    val wrongRatePercent: Int,
    val recentWrongCount: Int,
    val isFrequentlyMissed: Boolean,
)

data class SessionResult(
    val summary: SessionSummary,
    val topMissedWords: List<WordAggregateStat>,
)

data class DeckStatsSummary(
    val deckId: Long,
    val deckName: String,
    val totalWordCount: Int,
    val studiedWordCount: Int,
    val unstudiedWordCount: Int,
    val completedSessionCount: Int,
    val totalQuestionCount: Int,
    val totalWrongCount: Int,
    val accuracyPercent: Int,
)

data class DeckDailyStat(
    val dateKey: String,
    val dateLabel: String,
    val completedSessionCount: Int,
    val totalQuestionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
)

data class DeckStatsData(
    val summary: DeckStatsSummary,
    val topMissedWords: List<WordAggregateStat>,
    val allWordStats: List<WordAggregateStat>,
    val dailyStats: List<DeckDailyStat>,
)

data class DeckDateSessionSummary(
    val sessionId: Long,
    val completedAt: Long,
    val totalCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
)

data class DeckDateStatsData(
    val deckId: Long,
    val deckName: String,
    val dateKey: String,
    val dateLabel: String,
    val totalWordCount: Int,
    val studiedWordCount: Int,
    val unstudiedWordCount: Int,
    val sessions: List<DeckDateSessionSummary>,
    val topMissedWords: List<WordAggregateStat>,
)

data class HomeData(
    val jlptDecks: List<DeckWithCount>,
    val customDecks: List<DeckWithCount>,
    val totalWordCount: Int,
    val recentSessions: List<SessionSummary>,
)

data class DeckDetailData(
    val deck: DeckEntity,
    val words: List<WordEntity>,
)

data class WordDetailData(
    val word: WordEntity,
    val includedDecks: List<DeckWithCount>,
    val allDecks: List<DeckWithCount>,
    val allWords: List<WordEntity>,
)
