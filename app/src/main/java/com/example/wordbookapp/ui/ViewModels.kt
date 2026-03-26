package com.example.wordbookapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import com.example.wordbookapp.data.model.ExamSettings
import com.example.wordbookapp.data.model.HomeData
import com.example.wordbookapp.data.model.WordDraft
import com.example.wordbookapp.data.model.WordField
import com.example.wordbookapp.data.model.WordOrder
import com.example.wordbookapp.data.repository.WordbookRepository

class HomeViewModel(
    private val repository: WordbookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.observeHomeData().collectLatest { data: HomeData ->
                _uiState.value = HomeUiState(
                    isLoading = false,
                    data = data,
                )
            }
        }
    }

    suspend fun createCustomDeck(name: String): Long = repository.createCustomDeck(name)
}

class AllWordsViewModel(
    private val repository: WordbookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AllWordsUiState())
    val uiState: StateFlow<AllWordsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val decks = repository.getAllDecks()
            val words = repository.getAllWords()
            val deckWordIds = decks.associate { deck ->
                deck.id to repository.getDeckDetail(deck.id).words.map { it.id }.toSet()
            }
            _uiState.value = AllWordsUiState(
                isLoading = false,
                words = words,
                decks = decks,
                deckWordIds = deckWordIds,
            )
        }
    }
}

class DeckDetailViewModel(
    private val repository: WordbookRepository,
    private val deckId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeckDetailUiState())
    val uiState: StateFlow<DeckDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val detail = repository.getDeckDetail(deckId)
            _uiState.value = DeckDetailUiState(
                isLoading = false,
                deck = detail.deck,
                words = detail.words,
            )
            repository.observeDeckWords(deckId).collectLatest { words ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    words = words,
                )
            }
        }
    }
}

class WordEditorViewModel(
    private val repository: WordbookRepository,
    private val deckId: Long,
    private val wordId: Long?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordEditorUiState())
    val uiState: StateFlow<WordEditorUiState> = _uiState

    init {
        viewModelScope.launch {
            val word = wordId?.let { repository.getWord(it) }
            _uiState.value = WordEditorUiState(
                isLoading = false,
                isEditMode = word != null,
                draft = if (word == null) {
                    WordDraft()
                } else {
                    WordDraft(
                        readingJa = word.readingJa,
                        readingKo = word.readingKo,
                        partOfSpeech = word.partOfSpeech,
                        grammar = word.grammar,
                        kanji = word.kanji,
                        meaningJa = word.meaningJa,
                        meaningKo = word.meaningKo,
                        exampleJa = word.exampleJa,
                        exampleKo = word.exampleKo,
                        tag = word.tag,
                        note = word.note,
                    )
                },
            )
        }
    }

    fun updateDraft(transform: (WordDraft) -> WordDraft) {
        _uiState.value = _uiState.value.copy(
            draft = transform(_uiState.value.draft),
            errorMessage = null,
        )
    }

    fun save() {
        val draft = _uiState.value.draft
        if (draft.kanji.isBlank() || draft.readingJa.isBlank() || draft.meaningKo.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "한자, 읽는 방법(일본어), 뜻(한국어)는 필수예요.",
            )
            return
        }
        viewModelScope.launch {
            if (wordId == null) {
                repository.addWordToDeck(deckId, draft)
            } else {
                repository.updateWord(wordId, draft)
            }
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }
}

class ExamSetupViewModel(
    private val repository: WordbookRepository,
    private val deckId: Long?,
    private val isAiDeck: Boolean,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExamSetupUiState(isAiDeck = isAiDeck))
    val uiState: StateFlow<ExamSetupUiState> = _uiState

    init {
        viewModelScope.launch {
            val detail = deckId?.let { repository.getDeckDetail(it) }
            val totalWordCount = if (isAiDeck) 30 else detail?.words?.size ?: 0
            val unseenWordCount = if (isAiDeck || deckId == null) 0 else repository.getUnseenWordCountForDeck(deckId)
            val inProgressExam = repository.getInProgressExamData(deckId = deckId, isAiDeck = isAiDeck)
            _uiState.value = ExamSetupUiState(
                isLoading = false,
                deck = detail?.deck,
                settings = ExamSettings(),
                isAiDeck = isAiDeck,
                canStart = isAiDeck || totalWordCount > 0,
                inProgressExam = inProgressExam,
                totalWordCount = totalWordCount,
                unseenWordCount = unseenWordCount,
                availableWordCount = totalWordCount,
            )
        }
    }

    fun setWordOrder(value: WordOrder) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(wordOrder = value),
        )
    }

    fun setFrontField(value: WordField) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(frontField = value),
        )
    }

    fun setRevealField(value: WordField) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(revealField = value),
        )
    }

    fun setOnlyUnseenWords(value: Boolean) {
        val updatedSettings = _uiState.value.settings.copy(onlyUnseenWords = value)
        val updatedAvailableWordCount = resolveAvailableWordCount(
            isAiDeck = _uiState.value.isAiDeck,
            totalWordCount = _uiState.value.totalWordCount,
            unseenWordCount = _uiState.value.unseenWordCount,
            onlyUnseenWords = value,
        )
        _uiState.value = _uiState.value.copy(
            settings = updatedSettings,
            availableWordCount = updatedAvailableWordCount,
            canStart = canStartExam(
                selectedOption = _uiState.value.selectedWordCountOption,
                customWordCountInput = _uiState.value.customWordCountInput,
                availableWordCount = updatedAvailableWordCount,
                hasWords = updatedAvailableWordCount > 0,
            ),
        )
    }

    fun setWordCountOption(value: ExamWordCountOption) {
        val resolvedWordCount = when (value) {
            ExamWordCountOption.ALL -> null
            ExamWordCountOption.TEN,
            ExamWordCountOption.THIRTY,
            ExamWordCountOption.SIXTY -> value.presetCount
            ExamWordCountOption.CUSTOM -> _uiState.value.customWordCountInput.toIntOrNull()
        }
        _uiState.value = _uiState.value.copy(
            selectedWordCountOption = value,
            settings = _uiState.value.settings.copy(wordCount = resolvedWordCount),
            canStart = canStartExam(
                selectedOption = value,
                customWordCountInput = _uiState.value.customWordCountInput,
                availableWordCount = _uiState.value.availableWordCount,
                hasWords = _uiState.value.isAiDeck || _uiState.value.availableWordCount > 0,
            ),
        )
    }

    fun setCustomWordCountInput(value: String) {
        val numericOnly = value.filter(Char::isDigit).take(3)
        val shouldUseCustomValue = _uiState.value.selectedWordCountOption == ExamWordCountOption.CUSTOM
        _uiState.value = _uiState.value.copy(
            customWordCountInput = numericOnly,
            settings = if (shouldUseCustomValue) {
                _uiState.value.settings.copy(wordCount = numericOnly.toIntOrNull())
            } else {
                _uiState.value.settings
            },
            canStart = canStartExam(
                selectedOption = _uiState.value.selectedWordCountOption,
                customWordCountInput = numericOnly,
                availableWordCount = _uiState.value.availableWordCount,
                hasWords = _uiState.value.isAiDeck || _uiState.value.availableWordCount > 0,
            ),
        )
    }

    suspend fun startExam(): Long = repository.createExamSession(
        deckId = deckId,
        settings = _uiState.value.settings,
        useAiSelection = isAiDeck,
    )

    fun getInProgressSessionId(): Long? = _uiState.value.inProgressExam?.sessionId

    private fun canStartExam(
        selectedOption: ExamWordCountOption,
        customWordCountInput: String,
        availableWordCount: Int,
        hasWords: Boolean,
    ): Boolean {
        if (!hasWords) return false
        if (selectedOption != ExamWordCountOption.CUSTOM) return true
        val customCount = customWordCountInput.toIntOrNull() ?: return false
        return customCount > 0 && customCount <= availableWordCount
    }

    private fun resolveAvailableWordCount(
        isAiDeck: Boolean,
        totalWordCount: Int,
        unseenWordCount: Int,
        onlyUnseenWords: Boolean,
    ): Int {
        if (isAiDeck) return totalWordCount
        return if (onlyUnseenWords) unseenWordCount else totalWordCount
    }
}

class ExamViewModel(
    private val repository: WordbookRepository,
    private val sessionId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState

    init {
        refresh()
    }

    fun reveal() {
        _uiState.value = _uiState.value.copy(revealed = true)
    }

    suspend fun answer(isCorrect: Boolean): Boolean {
        val sessionData = _uiState.value.sessionData ?: return false
        val currentIndex = sessionData.answersCount
        val currentWord = sessionData.words[currentIndex]
        val finished = repository.recordExamAnswer(
            sessionId = sessionId,
            wordId = currentWord.id,
            sequenceIndex = currentIndex,
            isCorrect = isCorrect,
        )
        if (!finished) {
            refresh()
        }
        return finished
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = ExamUiState(
                isLoading = false,
                sessionData = repository.getExamSessionData(sessionId),
                revealed = false,
            )
        }
    }
}

class ResultViewModel(
    private val repository: WordbookRepository,
    private val sessionId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = ResultUiState(
                isLoading = false,
                result = repository.getSessionResult(sessionId),
            )
        }
    }
}

class DeckStatsViewModel(
    private val repository: WordbookRepository,
    private val deckId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeckStatsUiState())
    val uiState: StateFlow<DeckStatsUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = DeckStatsUiState(
                isLoading = false,
                stats = repository.getDeckStats(deckId),
            )
        }
    }
}

class DeckDateStatsViewModel(
    private val repository: WordbookRepository,
    private val deckId: Long,
    private val dateKey: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeckDateStatsUiState())
    val uiState: StateFlow<DeckDateStatsUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = DeckDateStatsUiState(
                isLoading = false,
                stats = repository.getDeckDateStats(deckId, dateKey),
            )
        }
    }
}

class WordDetailViewModel(
    private val repository: WordbookRepository,
    private val wordId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordDetailUiState())
    val uiState: StateFlow<WordDetailUiState> = _uiState

    init {
        refresh()
    }

    fun clearSaveToDeckSuccess() {
        _uiState.update { it.copy(saveToDeckSuccess = false) }
    }

    fun addToDeck(deckId: Long) {
        viewModelScope.launch {
            repository.addWordToExistingDeck(wordId, deckId)
            _uiState.value = WordDetailUiState(
                isLoading = false,
                detail = repository.getWordDetail(wordId),
                saveToDeckSuccess = true,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WordDetailUiState(
                isLoading = false,
                detail = repository.getWordDetail(wordId),
            )
        }
    }
}

class WordbookViewModelFactory<T : ViewModel>(
    private val creator: () -> T,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
