package com.example.wordbookapp.data.model

import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity
import com.example.wordbookapp.data.local.entity.WordEntity

enum class DeckType {
    JLPT,
    CUSTOM,
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
