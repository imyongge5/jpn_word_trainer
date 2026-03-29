package com.mistbottle.jpnwordtrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeParseException
import com.mistbottle.jpnwordtrainer.data.model.ExamSettings
import com.mistbottle.jpnwordtrainer.data.model.HomeData
import com.mistbottle.jpnwordtrainer.data.model.StatsDatePreset
import com.mistbottle.jpnwordtrainer.data.model.StatsDateRange
import com.mistbottle.jpnwordtrainer.data.model.WordDraft
import com.mistbottle.jpnwordtrainer.data.model.WordField
import com.mistbottle.jpnwordtrainer.data.model.WordOrder
import com.mistbottle.jpnwordtrainer.data.repository.AuthResult
import com.mistbottle.jpnwordtrainer.data.repository.SyncRepository
import com.mistbottle.jpnwordtrainer.data.repository.SyncResult
import com.mistbottle.jpnwordtrainer.data.repository.WordbookRepository

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
    private val syncRepository: SyncRepository,
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
        } else if (syncRepository.isSyncOnExamCompleteEnabled()) {
            syncRepository.push()
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

class SettingsViewModel(
    private val repository: WordbookRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                repository.observeThemePreset(),
                syncRepository.observeServerUrl(),
                syncRepository.observeUsername(),
                syncRepository.observeIsLoggedIn(),
                syncRepository.observeSyncOnExamComplete(),
            ) { themePreset, serverUrl, username, isLoggedIn, syncOnExamComplete ->
                SettingsUiState(
                    isLoading = false,
                    currentThemePreset = themePreset,
                    serverUrl = serverUrl,
                    username = username,
                    isLoggedIn = isLoggedIn,
                    syncOnExamComplete = syncOnExamComplete,
                    password = _uiState.value.password,
                    isWorking = _uiState.value.isWorking,
                    message = _uiState.value.message,
                )
            }.collectLatest { _uiState.value = it }
        }
    }

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, message = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, message = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, message = null) }
    }

    fun setSyncOnExamComplete(enabled: Boolean) {
        viewModelScope.launch {
            syncRepository.setSyncOnExamComplete(enabled)
        }
    }

    fun saveServerUrl() {
        viewModelScope.launch {
            syncRepository.saveServerUrl(_uiState.value.serverUrl)
            _uiState.update { it.copy(message = "서버 주소를 저장했어요.") }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "서버 주소, 아이디, 비밀번호를 모두 입력해 주세요.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = null) }
            when (val result = syncRepository.login(state.serverUrl, state.username, state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isWorking = false, password = "", message = "${result.username} 계정으로 로그인했어요.")
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isWorking = false, message = result.message)
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "서버 주소, 아이디, 비밀번호를 모두 입력해 주세요.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = null) }
            when (val result = syncRepository.register(state.serverUrl, state.username, state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isWorking = false, password = "", message = "${result.username} 계정을 만들고 로그인했어요.")
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isWorking = false, message = result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            syncRepository.logout()
            _uiState.update { it.copy(password = "", message = "로그아웃했어요.") }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = null) }
            when (val result = syncRepository.syncAll()) {
                SyncResult.Success -> _uiState.update { it.copy(isWorking = false, message = "동기화가 완료됐어요.") }
                is SyncResult.Error -> _uiState.update { it.copy(isWorking = false, message = result.message) }
                SyncResult.NotConfigured -> _uiState.update {
                    it.copy(isWorking = false, message = "서버 주소와 로그인 정보를 먼저 설정해 주세요.")
                }
            }
        }
    }
}

class GlobalStatsViewModel(
    private val repository: WordbookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GlobalStatsUiState())
    val uiState: StateFlow<GlobalStatsUiState> = _uiState

    init {
        refresh()
    }

    fun setPreset(preset: StatsDatePreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                customRangeErrorMessage = null,
            )
        }
        if (preset != StatsDatePreset.CUSTOM) {
            refresh()
        }
    }

    fun setCustomStartDateInput(value: String) {
        _uiState.update {
            it.copy(
                customStartDateInput = value,
                customRangeErrorMessage = null,
            )
        }
    }

    fun setCustomEndDateInput(value: String) {
        _uiState.update {
            it.copy(
                customEndDateInput = value,
                customRangeErrorMessage = null,
            )
        }
    }

    fun applyCustomRange() {
        val startDate = parseLocalDate(_uiState.value.customStartDateInput)
        val endDate = parseLocalDate(_uiState.value.customEndDateInput)
        if (startDate == null || endDate == null) {
            _uiState.update { it.copy(customRangeErrorMessage = "날짜는 yyyy-MM-dd 형식으로 입력해 주세요.") }
            return
        }
        if (endDate.isBefore(startDate)) {
            _uiState.update { it.copy(customRangeErrorMessage = "종료일은 시작일보다 빠를 수 없어요.") }
            return
        }
        refresh(
            StatsDateRange(
                preset = StatsDatePreset.CUSTOM,
                startDate = startDate,
                endDate = endDate,
            ),
        )
    }

    fun loadMoreCompletedTests() {
        if (_uiState.value.isLoadingMoreCompletedTests || !_uiState.value.hasMoreCompletedTests) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCompletedTests = true) }
            val page = repository.getCompletedTestResultPage(
                limit = COMPLETED_TEST_PAGE_SIZE,
                offset = _uiState.value.completedTestsOffset,
            )
            _uiState.update {
                it.copy(
                    completedTests = it.completedTests + page,
                    completedTestsOffset = it.completedTestsOffset + page.size,
                    hasMoreCompletedTests = page.size == COMPLETED_TEST_PAGE_SIZE,
                    isLoadingMoreCompletedTests = false,
                )
            }
        }
    }

    fun hideCompletedTest(testId: Long) {
        viewModelScope.launch {
            repository.markTestDeleted(testId)
            val currentRange = _uiState.value.stats?.range ?: StatsDateRange(preset = _uiState.value.selectedPreset)
            refresh(currentRange)
        }
    }

    private fun refresh(rangeOverride: StatsDateRange? = null) {
        viewModelScope.launch {
            val range = rangeOverride ?: StatsDateRange(preset = _uiState.value.selectedPreset)
            _uiState.update { it.copy(isLoading = true, customRangeErrorMessage = null) }
            val stats = repository.getGlobalStats(range)
            val completedTests = repository.getCompletedTestResultPage(
                limit = COMPLETED_TEST_PAGE_SIZE,
                offset = 0,
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedPreset = range.preset,
                stats = stats,
                completedTests = completedTests,
                completedTestsOffset = completedTests.size,
                hasMoreCompletedTests = completedTests.size == COMPLETED_TEST_PAGE_SIZE,
                isLoadingMoreCompletedTests = false,
            )
        }
    }

    private fun parseLocalDate(value: String): LocalDate? = try {
        LocalDate.parse(value.trim())
    } catch (_: DateTimeParseException) {
        null
    }

    private companion object {
        private const val COMPLETED_TEST_PAGE_SIZE = 20
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
