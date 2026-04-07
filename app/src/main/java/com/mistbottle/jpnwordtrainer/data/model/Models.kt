package com.mistbottle.jpnwordtrainer.data.model

import java.time.LocalDate
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckInstallStateEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity

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

enum class TestStatus {
    IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    DELETED,
}

enum class StatsDatePreset(
    val displayName: String,
) {
    TODAY("오늘"),
    LAST_7_DAYS("7일"),
    LAST_30_DAYS("30일"),
    ALL("전체"),
    CUSTOM("직접 선택"),
}

enum class ResultInsightType {
    REPEATED_MISS,
    STREAK_MISS,
    IMPROVED_WORD,
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
    val wordCount: Int? = null,
    val onlyUnseenWords: Boolean = false,
)

data class ExamSessionData(
    val test: TestEntity,
    val words: List<WordEntity>,
    val answersCount: Int,
) {
    val session: TestEntity
        get() = test
}

data class InProgressExamData(
    val testId: Long,
    val deckName: String,
    val answeredCount: Int,
    val totalCount: Int,
    val startedAt: Long,
) {
    val sessionId: Long
        get() = testId
}

data class SessionSummary(
    val testId: Long,
    val deckName: String,
    val totalCount: Int,
    val answeredCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
    val recordedAt: Long,
    val isCompleted: Boolean,
) {
    val sessionId: Long
        get() = testId
}

data class WordAggregateStat(
    val word: WordEntity,
    val attemptCount: Int,
    val wrongCount: Int,
    val wrongRatePercent: Int,
    val recentWrongCount: Int,
    val isFrequentlyMissed: Boolean,
)

data class SessionProgressPoint(
    val step: Int,
    val answeredCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
)

data class ResultInsight(
    val type: ResultInsightType,
    val title: String,
    val message: String,
    val words: List<WordEntity>,
)

data class SessionResult(
    val summary: SessionSummary,
    val progress: List<SessionProgressPoint>,
    val topMissedWords: List<WordAggregateStat>,
    val missedWords: List<WordAggregateStat>,
    val insights: List<ResultInsight>,
)

data class DeckStatsSummary(
    val deckId: Long,
    val deckName: String,
    val totalWordCount: Int,
    val studiedWordCount: Int,
    val unstudiedWordCount: Int,
    val recordedSessionCount: Int,
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
    val testId: Long,
    val endedAt: Long,
    val answeredCount: Int,
    val totalCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
) {
    val sessionId: Long
        get() = testId

    val completedAt: Long
        get() = endedAt
}

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

data class StatsDateRange(
    val preset: StatsDatePreset = StatsDatePreset.LAST_7_DAYS,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

data class GlobalStatsSummary(
    val totalQuestionCount: Int,
    val studiedWordCount: Int,
    val totalCorrectCount: Int,
    val totalWrongCount: Int,
    val recordedSessionCount: Int,
    val accuracyPercent: Int,
)

data class GlobalDailyStat(
    val dateKey: String,
    val dateLabel: String,
    val recordedSessionCount: Int,
    val totalQuestionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val uniqueWordCount: Int,
    val accuracyPercent: Int,
)

data class GlobalStatsData(
    val range: StatsDateRange,
    val rangeLabel: String,
    val summary: GlobalStatsSummary,
    val topMissedWords: List<WordAggregateStat>,
    val allWordStats: List<WordAggregateStat>,
    val dailyStats: List<GlobalDailyStat>,
    val recentSessions: List<SessionSummary>,
)

data class HomeData(
    val jlptDecks: List<DeckWithCount>,
    val customDecks: List<DeckWithCount>,
    val totalWordCount: Int,
)

data class DeckDetailData(
    val deck: DeckEntity,
    val words: List<WordEntity>,
    val installState: DeckInstallStateEntity? = null,
)

data class BuiltinDeckUpdateInfo(
    val stableKey: String,
    val name: String,
    val currentVersionCode: Int,
    val targetVersionCode: Int,
    val targetVersionLabel: String,
    val changelog: String,
    val updateAvailable: Boolean,
)

data class WordDetailData(
    val word: WordEntity,
    val includedDecks: List<DeckWithCount>,
    val allDecks: List<DeckWithCount>,
    val allWords: List<WordEntity>,
)
