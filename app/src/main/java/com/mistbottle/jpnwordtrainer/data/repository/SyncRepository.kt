package com.mistbottle.jpnwordtrainer.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.mistbottle.jpnwordtrainer.data.local.WordbookDatabase
import com.mistbottle.jpnwordtrainer.data.local.entity.AppSettingEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckWordCrossRef
import com.mistbottle.jpnwordtrainer.data.local.entity.EndedTestResultEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestWordLogEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity
import com.mistbottle.jpnwordtrainer.data.remote.AuthTokenResponseDto
import com.mistbottle.jpnwordtrainer.data.remote.DeckSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.DeckWordRefSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.EndedTestResultSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.SyncApiClient
import com.mistbottle.jpnwordtrainer.data.remote.SyncApiException
import com.mistbottle.jpnwordtrainer.data.remote.SyncPayloadDto
import com.mistbottle.jpnwordtrainer.data.remote.TestSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.TestWordLogSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.WordSyncDto
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.TestStatus
import com.mistbottle.jpnwordtrainer.data.model.WordField
import com.mistbottle.jpnwordtrainer.data.model.WordOrder

sealed interface AuthResult {
    data class Success(val username: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

sealed interface SyncResult {
    data object Success : SyncResult
    data class Error(val message: String) : SyncResult
    data object NotConfigured : SyncResult
}

class SyncRepository(
    private val database: WordbookDatabase,
) {
    private val wordDao = database.wordDao()
    private val deckDao = database.deckDao()
    private val studyDao = database.studyDao()
    private val appSettingDao = database.appSettingDao()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun observeServerUrl(): Flow<String> =
        appSettingDao.observeValue(SERVER_URL_KEY).map { it.orEmpty() }

    fun observeUsername(): Flow<String> =
        appSettingDao.observeValue(USERNAME_KEY).map { it.orEmpty() }

    fun observeSyncOnExamComplete(): Flow<Boolean> =
        appSettingDao.observeValue(SYNC_ON_EXAM_COMPLETE_KEY).map { it == TRUE_VALUE }

    fun observeIsLoggedIn(): Flow<Boolean> =
        appSettingDao.observeValue(AUTH_TOKEN_KEY).map { !it.isNullOrBlank() }

    suspend fun getServerUrl(): String = withContext(Dispatchers.IO) {
        appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
    }

    suspend fun saveServerUrl(url: String) = withContext(Dispatchers.IO) {
        appSettingDao.upsert(AppSettingEntity(SERVER_URL_KEY, normalizeServerUrl(url)))
    }

    suspend fun setSyncOnExamComplete(enabled: Boolean) = withContext(Dispatchers.IO) {
        appSettingDao.upsert(AppSettingEntity(SYNC_ON_EXAM_COMPLETE_KEY, if (enabled) TRUE_VALUE else FALSE_VALUE))
    }

    suspend fun isSyncOnExamCompleteEnabled(): Boolean = withContext(Dispatchers.IO) {
        appSettingDao.getValue(SYNC_ON_EXAM_COMPLETE_KEY) == TRUE_VALUE
    }

    suspend fun login(serverUrl: String, username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeServerUrl(serverUrl)
        if (normalizedUrl.isBlank()) return@withContext AuthResult.Error("서버 URL을 입력해 주세요.")
        return@withContext runCatching {
            SyncApiClient(normalizedUrl, httpClient).login(username.trim(), password)
        }.fold(
            onSuccess = { response ->
                saveAuthState(normalizedUrl, response)
                AuthResult.Success(response.username)
            },
            onFailure = { error ->
                AuthResult.Error(error.message ?: "로그인에 실패했습니다.")
            },
        )
    }

    suspend fun register(serverUrl: String, username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeServerUrl(serverUrl)
        if (normalizedUrl.isBlank()) return@withContext AuthResult.Error("서버 URL을 입력해 주세요.")
        return@withContext runCatching {
            val client = SyncApiClient(normalizedUrl, httpClient)
            client.register(username.trim(), password)
            client.login(username.trim(), password)
        }.fold(
            onSuccess = { response ->
                saveAuthState(normalizedUrl, response)
                AuthResult.Success(response.username)
            },
            onFailure = { error ->
                AuthResult.Error(error.message ?: "회원가입에 실패했습니다.")
            },
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        appSettingDao.upsert(AppSettingEntity(AUTH_TOKEN_KEY, ""))
        appSettingDao.upsert(AppSettingEntity(USERNAME_KEY, ""))
    }

    suspend fun push(): SyncResult = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext SyncResult.NotConfigured
        val payload = buildPushPayload()
        return@withContext runCatching {
            SyncApiClient(serverUrl, httpClient).push(token, payload)
        }.fold(
            onSuccess = { SyncResult.Success },
            onFailure = { error -> SyncResult.Error(error.message ?: "동기화 업로드에 실패했습니다.") },
        )
    }

    suspend fun pull(): SyncResult = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext SyncResult.NotConfigured
        if (studyDao.getInProgressTestCount() > 0) {
            return@withContext SyncResult.Error("진행 중인 시험이 있어서 서버 데이터를 가져올 수 없어요.")
        }
        return@withContext runCatching {
            val payload = SyncApiClient(serverUrl, httpClient).pull(token)
            applyPullPayload(payload)
        }.fold(
            onSuccess = { SyncResult.Success },
            onFailure = { error ->
                SyncResult.Error(error.message ?: "동기화 다운로드에 실패했습니다.")
            },
        )
    }

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        when (val pushResult = push()) {
            SyncResult.Success -> pull()
            is SyncResult.Error -> pushResult
            SyncResult.NotConfigured -> SyncResult.NotConfigured
        }
    }

    private suspend fun saveAuthState(serverUrl: String, response: AuthTokenResponseDto) {
        appSettingDao.upsert(AppSettingEntity(SERVER_URL_KEY, serverUrl))
        appSettingDao.upsert(AppSettingEntity(AUTH_TOKEN_KEY, response.accessToken))
        appSettingDao.upsert(AppSettingEntity(USERNAME_KEY, response.username))
    }

    private suspend fun buildPushPayload(): SyncPayloadDto {
        val words = wordDao.getAllWordsByNewest()
        val decks = deckDao.getDecks().mapNotNull { deckWithCount -> deckDao.getDeckById(deckWithCount.id) }
        val refs = deckDao.getAllDeckWordCrossRefs()
        val tests = studyDao.getAllTests()
        val logs = studyDao.getAllTestWordLogs()
        val results = studyDao.getAllEndedTestResults()
        return SyncPayloadDto(
            words = words.map(::wordToDto),
            decks = decks.map(::deckToDto),
            deckWordRefs = refs.map(::refToDto),
            tests = tests.map(::testToDto),
            testWordLogs = logs.map(::logToDto),
            endedTestResults = results.map(::resultToDto),
            syncedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun applyPullPayload(payload: SyncPayloadDto) {
        database.withTransaction {
            studyDao.clearAllEndedTestResults()
            studyDao.clearAllTestWordLogs()
            studyDao.clearAllTests()
            deckDao.clearAllDeckWordCrossRefs()
            deckDao.clearAllDecks()
            wordDao.clearAll()

            wordDao.insertWords(payload.words.map(::dtoToWord))
            deckDao.insertDecks(payload.decks.map(::dtoToDeck))
            deckDao.insertDeckWordCrossRefs(payload.deckWordRefs.map(::dtoToRef))
            studyDao.insertTests(payload.tests.map(::dtoToTest))
            studyDao.insertTestWordLogs(payload.testWordLogs.map(::dtoToLog))
            studyDao.insertEndedTestResults(payload.endedTestResults.map(::dtoToResult))
        }
    }

    private fun wordToDto(entity: WordEntity) = WordSyncDto(
        id = entity.id,
        readingJa = entity.readingJa,
        readingKo = entity.readingKo,
        partOfSpeech = entity.partOfSpeech,
        grammar = entity.grammar,
        kanji = entity.kanji,
        meaningJa = entity.meaningJa,
        meaningKo = entity.meaningKo,
        exampleJa = entity.exampleJa,
        exampleKo = entity.exampleKo,
        tag = entity.tag,
        note = entity.note,
        isKanaOnly = entity.isKanaOnly,
        createdAt = entity.createdAt,
    )

    private fun deckToDto(entity: DeckEntity) = DeckSyncDto(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        type = entity.type.name,
        sourceTag = entity.sourceTag,
        displayOrder = entity.displayOrder,
        createdAt = entity.createdAt,
    )

    private fun refToDto(entity: DeckWordCrossRef) = DeckWordRefSyncDto(
        deckId = entity.deckId,
        wordId = entity.wordId,
        displayOrder = entity.displayOrder,
        addedAt = entity.addedAt,
    )

    private fun testToDto(entity: TestEntity) = TestSyncDto(
        id = entity.id,
        status = entity.status.name,
        deckId = entity.deckId,
        deckNameSnapshot = entity.deckNameSnapshot,
        isAiDeck = entity.isAiDeck,
        wordOrder = entity.wordOrder.name,
        frontField = entity.frontField.name,
        revealField = entity.revealField.name,
        wordIdsSerialized = entity.wordIdsSerialized,
        totalWordCount = entity.totalWordCount,
        startedAt = entity.startedAt,
        changedAt = entity.changedAt,
    )

    private fun logToDto(entity: TestWordLogEntity) = TestWordLogSyncDto(
        id = entity.id,
        testId = entity.testId,
        wordId = entity.wordId,
        sequenceIndex = entity.sequenceIndex,
        isCorrect = entity.isCorrect,
        answeredAt = entity.answeredAt,
    )

    private fun resultToDto(entity: EndedTestResultEntity) = EndedTestResultSyncDto(
        id = entity.id,
        testId = entity.testId,
        deckId = entity.deckId,
        deckNameSnapshot = entity.deckNameSnapshot,
        isAiDeck = entity.isAiDeck,
        totalWordCount = entity.totalWordCount,
        correctCount = entity.correctCount,
        wrongCount = entity.wrongCount,
        accuracyPercent = entity.accuracyPercent,
        startedAt = entity.startedAt,
        endedAt = entity.endedAt,
        durationSeconds = entity.durationSeconds,
    )

    private fun dtoToWord(dto: WordSyncDto) = WordEntity(
        id = dto.id,
        readingJa = dto.readingJa,
        readingKo = dto.readingKo,
        partOfSpeech = dto.partOfSpeech,
        grammar = dto.grammar,
        kanji = dto.kanji,
        meaningJa = dto.meaningJa,
        meaningKo = dto.meaningKo,
        exampleJa = dto.exampleJa,
        exampleKo = dto.exampleKo,
        tag = dto.tag,
        note = dto.note,
        isKanaOnly = dto.isKanaOnly,
        createdAt = dto.createdAt,
    )

    private fun dtoToDeck(dto: DeckSyncDto) = DeckEntity(
        id = dto.id,
        name = dto.name,
        description = dto.description,
        type = DeckType.valueOf(dto.type),
        sourceTag = dto.sourceTag,
        displayOrder = dto.displayOrder,
        createdAt = dto.createdAt,
    )

    private fun dtoToRef(dto: DeckWordRefSyncDto) = DeckWordCrossRef(
        deckId = dto.deckId,
        wordId = dto.wordId,
        displayOrder = dto.displayOrder,
        addedAt = dto.addedAt,
    )

    private fun dtoToTest(dto: TestSyncDto) = TestEntity(
        id = dto.id,
        status = TestStatus.valueOf(dto.status),
        deckId = dto.deckId,
        deckNameSnapshot = dto.deckNameSnapshot,
        isAiDeck = dto.isAiDeck,
        wordOrder = WordOrder.valueOf(dto.wordOrder),
        frontField = WordField.valueOf(dto.frontField),
        revealField = WordField.valueOf(dto.revealField),
        wordIdsSerialized = dto.wordIdsSerialized,
        totalWordCount = dto.totalWordCount,
        startedAt = dto.startedAt,
        changedAt = dto.changedAt,
    )

    private fun dtoToLog(dto: TestWordLogSyncDto) = TestWordLogEntity(
        id = dto.id,
        testId = dto.testId,
        wordId = dto.wordId,
        sequenceIndex = dto.sequenceIndex,
        isCorrect = dto.isCorrect,
        answeredAt = dto.answeredAt,
    )

    private fun dtoToResult(dto: EndedTestResultSyncDto) = EndedTestResultEntity(
        id = dto.id,
        testId = dto.testId,
        deckId = dto.deckId,
        deckNameSnapshot = dto.deckNameSnapshot,
        isAiDeck = dto.isAiDeck,
        totalWordCount = dto.totalWordCount,
        correctCount = dto.correctCount,
        wrongCount = dto.wrongCount,
        accuracyPercent = dto.accuracyPercent,
        startedAt = dto.startedAt,
        endedAt = dto.endedAt,
        durationSeconds = dto.durationSeconds,
    )

    private fun normalizeServerUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    companion object {
        private const val SERVER_URL_KEY = "server_url"
        private const val USERNAME_KEY = "sync_username"
        private const val AUTH_TOKEN_KEY = "sync_auth_token"
        private const val SYNC_ON_EXAM_COMPLETE_KEY = "sync_on_exam_complete"
        private const val TRUE_VALUE = "true"
        private const val FALSE_VALUE = "false"
    }
}
