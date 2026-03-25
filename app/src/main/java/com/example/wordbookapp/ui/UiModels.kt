package com.example.wordbookapp.ui

import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.WordEntity
import com.example.wordbookapp.data.model.DeckWithCount
import com.example.wordbookapp.data.model.ExamSessionData
import com.example.wordbookapp.data.model.ExamSettings
import com.example.wordbookapp.data.model.HomeData
import com.example.wordbookapp.data.model.SessionResult
import com.example.wordbookapp.data.model.WordDraft

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
)

data class ExamUiState(
    val isLoading: Boolean = true,
    val sessionData: ExamSessionData? = null,
    val revealed: Boolean = false,
)

data class ResultUiState(
    val isLoading: Boolean = true,
    val result: SessionResult? = null,
)

sealed interface StartExamTarget {
    data class Deck(val deck: DeckWithCount) : StartExamTarget
    data object AiDeck : StartExamTarget
}
