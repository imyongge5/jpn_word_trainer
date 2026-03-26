package com.mistbottle.jpnwordtrainer.ui

import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity
import com.mistbottle.jpnwordtrainer.data.model.DeckWithCount
import com.mistbottle.jpnwordtrainer.data.model.DeckDateStatsData
import com.mistbottle.jpnwordtrainer.data.model.DeckStatsData
import com.mistbottle.jpnwordtrainer.data.model.ExamSessionData
import com.mistbottle.jpnwordtrainer.data.model.ExamSettings
import com.mistbottle.jpnwordtrainer.data.model.HomeData
import com.mistbottle.jpnwordtrainer.data.model.InProgressExamData
import com.mistbottle.jpnwordtrainer.data.model.SessionResult
import com.mistbottle.jpnwordtrainer.data.model.WordDetailData
import com.mistbottle.jpnwordtrainer.data.model.WordDraft

data class HomeUiState(
    val isLoading: Boolean = true,
    val data: HomeData? = null,
    val errorMessage: String? = null,
)

data class DeckDetailUiState(
    val isLoading: Boolean = true,
    val deck: DeckEntity? = null,
    val words: List<WordEntity> = emptyList(),
    val errorMessage: String? = null,
)

data class WordEditorUiState(
    val isLoading: Boolean = true,
    val draft: WordDraft = WordDraft(),
    val isEditMode: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
)

data class ExamSetupUiState(
    val isLoading: Boolean = true,
    val deck: DeckEntity? = null,
    val settings: ExamSettings = ExamSettings(),
    val isAiDeck: Boolean = false,
    val canStart: Boolean = false,
    val inProgressExam: InProgressExamData? = null,
    val totalWordCount: Int = 0,
    val unseenWordCount: Int = 0,
    val availableWordCount: Int = 0,
    val selectedWordCountOption: ExamWordCountOption = ExamWordCountOption.ALL,
    val customWordCountInput: String = "",
)

enum class ExamWordCountOption(
    val displayName: String,
    val presetCount: Int?,
) {
    ALL("전체", null),
    TEN("10", 10),
    THIRTY("30", 30),
    SIXTY("60", 60),
    CUSTOM("직접입력", null),
}

data class ExamUiState(
    val isLoading: Boolean = true,
    val sessionData: ExamSessionData? = null,
    val revealed: Boolean = false,
)

data class ResultUiState(
    val isLoading: Boolean = true,
    val result: SessionResult? = null,
)

data class DeckStatsUiState(
    val isLoading: Boolean = true,
    val stats: DeckStatsData? = null,
)

data class DeckDateStatsUiState(
    val isLoading: Boolean = true,
    val stats: DeckDateStatsData? = null,
)

data class WordDetailUiState(
    val isLoading: Boolean = true,
    val detail: WordDetailData? = null,
    val saveToDeckSuccess: Boolean = false,
)

data class AllWordsUiState(
    val isLoading: Boolean = true,
    val words: List<WordEntity> = emptyList(),
    val decks: List<DeckWithCount> = emptyList(),
    val deckWordIds: Map<Long, Set<Long>> = emptyMap(),
)

sealed interface StartExamTarget {
    data class Deck(val deck: DeckWithCount) : StartExamTarget
    data object AiDeck : StartExamTarget
}
