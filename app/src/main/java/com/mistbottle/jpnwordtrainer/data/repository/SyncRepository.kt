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
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckInstallStateEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckWordCrossRef
import com.mistbottle.jpnwordtrainer.data.local.entity.EndedTestResultEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestWordLogEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity
import com.mistbottle.jpnwordtrainer.data.remote.AuthTokenResponseDto
import com.mistbottle.jpnwordtrainer.data.remote.BuiltinDeckVersionPackageDto
import com.mistbottle.jpnwordtrainer.data.remote.BuiltinDeckUpdatePackageDto
import com.mistbottle.jpnwordtrainer.data.remote.DeckSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.DeckInstallStateSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.DeckWordRefSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.EndedTestResultSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.SyncApiClient
import com.mistbottle.jpnwordtrainer.data.remote.SyncApiException
import com.mistbottle.jpnwordtrainer.data.remote.SyncPayloadDto
import com.mistbottle.jpnwordtrainer.data.remote.TestSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.TestWordLogSyncDto
import com.mistbottle.jpnwordtrainer.data.remote.WordSyncDto
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.BuiltinDeckVersionCatalog
import com.mistbottle.jpnwordtrainer.data.model.BuiltinDeckVersionItem
import com.mistbottle.jpnwordtrainer.data.model.BuiltinDeckUpdateInfo
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

    suspend fun restoreFromServer(): SyncResult = pull()

    suspend fun getBuiltinDeckUpdateInfo(deckId: Long): BuiltinDeckUpdateInfo? = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext null
        val deck = deckDao.getDeckById(deckId) ?: return@withContext null
        if (!deck.isBuiltin || deck.stableKey.isNullOrBlank()) return@withContext null
        val installState = deckDao.getDeckInstallState(deckId)
        val currentVersionCode = installState?.currentVersionCode ?: deck.deckVersionCode ?: 1
        val updatePackage = SyncApiClient(serverUrl, httpClient).getBuiltinDeckUpdatePackage(
            token = token,
            stableKey = deck.stableKey,
            currentVersionCode = currentVersionCode,
        )
        upsertDeckInstallState(
            stableKey = updatePackage.stableKey,
            deckId = deckId,
            currentVersionCode = currentVersionCode,
            latestKnownVersionCode = maxOf(currentVersionCode, updatePackage.targetVersionCode),
            updateAvailable = updatePackage.updateAvailable,
            isLegacyVersion = installState?.isLegacyVersion ?: true,
        )
        BuiltinDeckUpdateInfo(
            stableKey = updatePackage.stableKey,
            name = updatePackage.name,
            currentVersionCode = currentVersionCode,
            targetVersionCode = updatePackage.targetVersionCode,
            targetVersionLabel = updatePackage.targetVersionLabel,
            changelog = updatePackage.changelog,
            updateAvailable = updatePackage.updateAvailable,
        )
    }

    suspend fun getBuiltinDeckVersionCatalog(deckId: Long): BuiltinDeckVersionCatalog? = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext null
        val deck = deckDao.getDeckById(deckId) ?: return@withContext null
        if (!deck.isBuiltin || deck.stableKey.isNullOrBlank()) return@withContext null
        val installState = deckDao.getDeckInstallState(deckId)
        val currentVersionCode = installState?.currentVersionCode ?: deck.deckVersionCode ?: 1
        val versionList = SyncApiClient(serverUrl, httpClient).getBuiltinDeckVersions(
            token = token,
            stableKey = deck.stableKey,
        )
        upsertDeckInstallState(
            stableKey = versionList.stableKey,
            deckId = deckId,
            currentVersionCode = currentVersionCode,
            latestKnownVersionCode = versionList.latestVersionCode,
            updateAvailable = versionList.latestVersionCode > currentVersionCode,
            isLegacyVersion = installState?.isLegacyVersion ?: true,
        )
        BuiltinDeckVersionCatalog(
            stableKey = versionList.stableKey,
            name = versionList.name,
            currentVersionCode = currentVersionCode,
            latestVersionCode = versionList.latestVersionCode,
            versions = versionList.versions.map { version ->
                BuiltinDeckVersionItem(
                    versionCode = version.versionCode,
                    versionLabel = version.versionLabel,
                    changelog = version.changelog,
                    publishedAt = version.publishedAt,
                    isLatest = version.isLatest,
                    isCurrent = version.versionCode == currentVersionCode,
                )
            },
        )
    }

    suspend fun applyBuiltinDeckUpdate(deckId: Long): SyncResult = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext SyncResult.NotConfigured
        val deck = deckDao.getDeckById(deckId) ?: return@withContext SyncResult.Error("덱 정보를 찾지 못했어요.")
        if (!deck.isBuiltin || deck.stableKey.isNullOrBlank()) {
            return@withContext SyncResult.Error("기본 덱만 업데이트할 수 있어요.")
        }
        if (studyDao.getInProgressTestForDeck(deckId) != null) {
            return@withContext SyncResult.Error("진행 중인 시험이 있어서 지금은 업데이트할 수 없어요.")
        }
        runCatching {
            val currentVersionCode = deckDao.getDeckInstallState(deckId)?.currentVersionCode ?: deck.deckVersionCode ?: 1
            val updatePackage = SyncApiClient(serverUrl, httpClient).getBuiltinDeckUpdatePackage(
                token = token,
                stableKey = deck.stableKey,
                currentVersionCode = currentVersionCode,
            )
            applyBuiltinDeckUpdatePackage(deck, updatePackage)
        }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Error(it.message ?: "기본 덱 업데이트에 실패했어요.") },
        )
    }

    suspend fun applyBuiltinDeckVersion(deckId: Long, versionCode: Int): SyncResult = withContext(Dispatchers.IO) {
        val serverUrl = appSettingDao.getValue(SERVER_URL_KEY).orEmpty()
        val token = appSettingDao.getValue(AUTH_TOKEN_KEY).orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) return@withContext SyncResult.NotConfigured
        val deck = deckDao.getDeckById(deckId) ?: return@withContext SyncResult.Error("단어장 정보를 찾지 못했어요.")
        if (!deck.isBuiltin || deck.stableKey.isNullOrBlank()) {
            return@withContext SyncResult.Error("기본 덱만 버전 업데이트를 할 수 있어요.")
        }
        if (studyDao.getInProgressTestForDeck(deckId) != null) {
            return@withContext SyncResult.Error("진행 중인 시험이 있어서 지금은 버전을 바꿀 수 없어요.")
        }
        runCatching {
            val versionPackage = SyncApiClient(serverUrl, httpClient).getBuiltinDeckVersionPackage(
                token = token,
                stableKey = deck.stableKey,
                versionCode = versionCode,
            )
            applyBuiltinDeckVersionPackage(deck, versionPackage)
        }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Error(it.message ?: "기본 덱 버전 적용에 실패했어요.") },
        )
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
        val customDecks = decks.filter { it.type == DeckType.CUSTOM }
        val customDeckIds = customDecks.map { it.id }.toSet()
        val customRefs = refs.filter { it.deckId in customDeckIds }
        val customWordIds = customRefs.map { it.wordId }.toSet()
        val syncedWords = words.filter { it.id in customWordIds }
        val tests = studyDao.getAllTests()
        val logs = studyDao.getAllTestWordLogs()
        val results = studyDao.getAllEndedTestResults()
        return SyncPayloadDto(
            words = syncedWords.map(::wordToDto),
            decks = customDecks.map(::deckToDto),
            deckInstallStates = deckDao.getAllDeckInstallStates().map(::installStateToDto),
            deckWordRefs = customRefs.map(::refToDto),
            tests = tests.map(::testToDto),
            testWordLogs = logs.map(::logToDto),
            endedTestResults = results.map(::resultToDto),
            clientSyncVersion = CLIENT_SYNC_VERSION,
            syncedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun applyPullPayload(payload: SyncPayloadDto) {
        database.withTransaction {
            wordDao.insertWords(payload.words.map(::dtoToWord))
            deckDao.insertDecks(payload.decks.map(::dtoToDeck))
            if (payload.deckInstallStates.isNotEmpty()) {
                deckDao.insertDeckInstallStates(payload.deckInstallStates.map(::dtoToInstallState))
            }
            val incomingDeckIds = payload.decks.map { it.id }
            if (incomingDeckIds.isNotEmpty()) {
                deckDao.deleteDeckWordCrossRefsForDeckIds(incomingDeckIds)
            }
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
        stableKey = entity.stableKey,
        deckVersionCode = entity.deckVersionCode,
        isBuiltin = entity.isBuiltin,
        displayOrder = entity.displayOrder,
        createdAt = entity.createdAt,
    )

    private fun installStateToDto(entity: DeckInstallStateEntity) = DeckInstallStateSyncDto(
        deckId = entity.deckId,
        stableKey = entity.stableKey,
        currentVersionCode = entity.currentVersionCode,
        latestKnownVersionCode = entity.latestKnownVersionCode,
        updateAvailable = entity.updateAvailable,
        isLegacyVersion = entity.isLegacyVersion,
        lastCheckedAt = entity.lastCheckedAt,
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
        sourceDeckStableKey = entity.sourceDeckStableKey,
        sourceDeckVersionCode = entity.sourceDeckVersionCode,
        isAiDeck = entity.isAiDeck,
        onlyUnseenWords = entity.onlyUnseenWords,
        excludeKanaOnly = entity.excludeKanaOnly,
        wrongOnly = entity.wrongOnly,
        wordOrder = entity.wordOrder.name,
        frontField = entity.frontField.name,
        revealFields = deserializeRevealFields(entity.revealFieldsSerialized).map { it.name },
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
        sourceDeckStableKey = entity.sourceDeckStableKey,
        sourceDeckVersionCode = entity.sourceDeckVersionCode,
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
        stableKey = dto.stableKey,
        deckVersionCode = dto.deckVersionCode,
        isBuiltin = dto.isBuiltin ?: false,
        displayOrder = dto.displayOrder,
        createdAt = dto.createdAt,
    )

    private fun dtoToInstallState(dto: DeckInstallStateSyncDto) = DeckInstallStateEntity(
        deckId = dto.deckId,
        stableKey = dto.stableKey,
        currentVersionCode = dto.currentVersionCode,
        latestKnownVersionCode = dto.latestKnownVersionCode,
        updateAvailable = dto.updateAvailable,
        isLegacyVersion = dto.isLegacyVersion,
        lastCheckedAt = dto.lastCheckedAt,
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
        sourceDeckStableKey = dto.sourceDeckStableKey,
        sourceDeckVersionCode = dto.sourceDeckVersionCode,
        isAiDeck = dto.isAiDeck,
        onlyUnseenWords = dto.onlyUnseenWords,
        excludeKanaOnly = dto.excludeKanaOnly,
        wrongOnly = dto.wrongOnly,
        wordOrder = WordOrder.valueOf(dto.wordOrder),
        frontField = WordField.valueOf(dto.frontField),
        revealFieldsSerialized = serializeRevealFields(dto.revealFields.map(WordField::valueOf).toSet()),
        wordIdsSerialized = dto.wordIdsSerialized,
        totalWordCount = dto.totalWordCount,
        startedAt = dto.startedAt,
        changedAt = dto.changedAt,
    )

    private fun serializeRevealFields(fields: Set<WordField>): String =
        if (fields.isEmpty()) {
            WordField.READING_JA.name
        } else {
            fields.joinToString(",") { it.name }
        }

    private fun deserializeRevealFields(serialized: String): Set<WordField> =
        serialized.split(",")
            .mapNotNull { raw ->
                raw.trim().takeIf { it.isNotEmpty() }?.let {
                    runCatching { WordField.valueOf(it) }.getOrNull()
                }
            }
            .toSet()
            .ifEmpty { setOf(WordField.READING_JA) }

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
        sourceDeckStableKey = dto.sourceDeckStableKey,
        sourceDeckVersionCode = dto.sourceDeckVersionCode,
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
        private const val CLIENT_SYNC_VERSION = 2
        private const val SERVER_URL_KEY = "server_url"
        private const val USERNAME_KEY = "sync_username"
        private const val AUTH_TOKEN_KEY = "sync_auth_token"
        private const val SYNC_ON_EXAM_COMPLETE_KEY = "sync_on_exam_complete"
        private const val TRUE_VALUE = "true"
        private const val FALSE_VALUE = "false"
    }

    private suspend fun applyBuiltinDeckUpdatePackage(
        deck: DeckEntity,
        updatePackage: BuiltinDeckUpdatePackageDto,
    ): SyncResult {
        if (!updatePackage.updateAvailable) {
            upsertDeckInstallState(
                stableKey = updatePackage.stableKey,
                deckId = deck.id,
                currentVersionCode = deck.deckVersionCode ?: 1,
                latestKnownVersionCode = updatePackage.targetVersionCode,
                updateAvailable = false,
                isLegacyVersion = false,
            )
            return SyncResult.Success
        }
        val invalidPackageMessage = "업데이트 패키지가 비정상이라 적용을 중단했어요."
        if (deck.stableKey != updatePackage.stableKey) {
            return SyncResult.Error("$invalidPackageMessage 현재 단어장과 맞지 않는 업데이트예요.")
        }
        if (updatePackage.words.isEmpty()) {
            return SyncResult.Error("$invalidPackageMessage 단어 목록이 비어 있어요.")
        }
        if (updatePackage.deckWordRefs.isEmpty()) {
            return SyncResult.Error("$invalidPackageMessage 단어장 연결 정보가 비어 있어요.")
        }
        val updateWordIds = updatePackage.words.mapTo(mutableSetOf()) { it.id }
        val hasDanglingRefs = updatePackage.deckWordRefs.any { it.wordId !in updateWordIds }
        if (hasDanglingRefs) {
            return SyncResult.Error("$invalidPackageMessage 연결된 단어 정보가 일부 누락됐어요.")
        }
        val existingCrossRefCount = deckDao.getDeckWordCrossRefCount(deck.id)
        val incomingCrossRefCount = updatePackage.deckWordRefs.size
        if (deck.type == DeckType.JLPT && existingCrossRefCount > 0 && incomingCrossRefCount == 0) {
            return SyncResult.Error("$invalidPackageMessage 기본 덱 단어가 0개가 되는 업데이트는 차단했어요.")
        }
        database.withTransaction {
            wordDao.insertWords(updatePackage.words.map(::dtoToWord))
            deckDao.insertDeck(
                deck.copy(
                    name = updatePackage.name,
                    stableKey = updatePackage.stableKey,
                    deckVersionCode = updatePackage.targetVersionCode,
                    isBuiltin = true,
                ),
            )
            deckDao.deleteDeckWordCrossRefsForDeck(deck.id)
            deckDao.insertDeckWordCrossRefs(
                updatePackage.deckWordRefs.map { ref ->
                    DeckWordCrossRef(
                        deckId = deck.id,
                        wordId = ref.wordId,
                        displayOrder = ref.displayOrder,
                        addedAt = ref.addedAt,
                    )
                },
            )
            upsertDeckInstallState(
                stableKey = updatePackage.stableKey,
                deckId = deck.id,
                currentVersionCode = updatePackage.targetVersionCode,
                latestKnownVersionCode = updatePackage.targetVersionCode,
                updateAvailable = false,
                isLegacyVersion = false,
            )
        }
        return SyncResult.Success
    }

    private suspend fun applyBuiltinDeckVersionPackage(
        deck: DeckEntity,
        versionPackage: BuiltinDeckVersionPackageDto,
    ): SyncResult = applyBuiltinDeckUpdatePackage(
        deck = deck,
        updatePackage = BuiltinDeckUpdatePackageDto(
            stableKey = versionPackage.stableKey,
            name = versionPackage.name,
            currentVersionCode = deck.deckVersionCode ?: 1,
            targetVersionCode = versionPackage.versionCode,
            targetVersionLabel = versionPackage.versionLabel,
            changelog = versionPackage.changelog,
            updateAvailable = true,
            words = versionPackage.words,
            deckWordRefs = versionPackage.deckWordRefs,
            mappings = emptyList(),
        ),
    )

    private suspend fun upsertDeckInstallState(
        stableKey: String,
        deckId: Long,
        currentVersionCode: Int,
        latestKnownVersionCode: Int,
        updateAvailable: Boolean,
        isLegacyVersion: Boolean,
    ) {
        val existing = deckDao.getDeckInstallStateByStableKey(stableKey)
        deckDao.insertDeckInstallState(
            DeckInstallStateEntity(
                id = existing?.id ?: 0,
                deckId = deckId,
                stableKey = stableKey,
                currentVersionCode = currentVersionCode,
                latestKnownVersionCode = latestKnownVersionCode,
                updateAvailable = updateAvailable,
                isLegacyVersion = isLegacyVersion,
                lastCheckedAt = System.currentTimeMillis(),
            ),
        )
    }
}
