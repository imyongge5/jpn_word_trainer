package com.mistbottle.jpnwordtrainer.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import com.mistbottle.jpnwordtrainer.data.local.WordbookDatabase
import com.mistbottle.jpnwordtrainer.data.local.entity.AppSettingEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckWordCrossRef
import com.mistbottle.jpnwordtrainer.data.local.entity.EndedTestResultEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestWordLogEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity
import com.mistbottle.jpnwordtrainer.data.model.DeckDateSessionSummary
import com.mistbottle.jpnwordtrainer.data.model.DeckDateStatsData
import com.mistbottle.jpnwordtrainer.data.model.DeckDailyStat
import com.mistbottle.jpnwordtrainer.data.model.DeckDetailData
import com.mistbottle.jpnwordtrainer.data.model.DeckStatsData
import com.mistbottle.jpnwordtrainer.data.model.DeckStatsSummary
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.ExamSessionData
import com.mistbottle.jpnwordtrainer.data.model.ExamSettings
import com.mistbottle.jpnwordtrainer.data.model.GlobalDailyStat
import com.mistbottle.jpnwordtrainer.data.model.GlobalStatsData
import com.mistbottle.jpnwordtrainer.data.model.GlobalStatsSummary
import com.mistbottle.jpnwordtrainer.data.model.HomeData
import com.mistbottle.jpnwordtrainer.data.model.InProgressExamData
import com.mistbottle.jpnwordtrainer.data.model.ResultInsight
import com.mistbottle.jpnwordtrainer.data.model.ResultInsightType
import com.mistbottle.jpnwordtrainer.data.model.SessionProgressPoint
import com.mistbottle.jpnwordtrainer.data.model.SessionResult
import com.mistbottle.jpnwordtrainer.data.model.SessionSummary
import com.mistbottle.jpnwordtrainer.data.model.StatsDatePreset
import com.mistbottle.jpnwordtrainer.data.model.StatsDateRange
import com.mistbottle.jpnwordtrainer.data.model.TestStatus
import com.mistbottle.jpnwordtrainer.data.model.ThemePreset
import com.mistbottle.jpnwordtrainer.data.model.WordAggregateStat
import com.mistbottle.jpnwordtrainer.data.model.WordDetailData
import com.mistbottle.jpnwordtrainer.data.model.WordDraft
import com.mistbottle.jpnwordtrainer.data.model.WordOrder

class WordbookRepository(
    private val database: WordbookDatabase,
) {
    private val wordDao = database.wordDao()
    private val deckDao = database.deckDao()
    private val studyDao = database.studyDao()
    private val appSettingDao = database.appSettingDao()

    fun observeHomeData(): Flow<HomeData> = combine(
        deckDao.observeDecks(),
        studyDao.observeTestWordLogCount(),
    ) { decks, _ ->
        HomeData(
            jlptDecks = decks.filter { it.type == DeckType.JLPT },
            customDecks = decks.filter { it.type == DeckType.CUSTOM },
            totalWordCount = wordDao.getWordCount(),
        )
    }

    fun observeDeckWords(deckId: Long): Flow<List<WordEntity>> = deckDao.observeWordsForDeck(deckId)

    fun observeThemePreset(): Flow<ThemePreset> =
        appSettingDao.observeValue(THEME_PRESET_KEY).map { ThemePreset.fromStorage(it) }

    suspend fun getThemePreset(): ThemePreset = withContext(Dispatchers.IO) {
        ThemePreset.fromStorage(appSettingDao.getValue(THEME_PRESET_KEY))
    }

    suspend fun saveThemePreset(preset: ThemePreset) = withContext(Dispatchers.IO) {
        appSettingDao.upsert(AppSettingEntity(THEME_PRESET_KEY, preset.storageValue))
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        expireStaleTests()
        if (appSettingDao.getValue(SEED_KEY) == SEEDED_VALUE) {
            syncKanaOnlyFlags(wordDao.getAllWordsByNewest())
            return@withContext
        }
        val now = System.currentTimeMillis()
        ensureDefaultDecks(now)
        val allExistingWords = wordDao.getAllWordsByNewest().toMutableList()
        repairLegacySeedWords(allExistingWords)
        syncKanaOnlyFlags(wordDao.getAllWordsByNewest())
        appSettingDao.upsert(AppSettingEntity(SEED_KEY, SEEDED_VALUE))
    }

    private suspend fun ensureDefaultDecks(now: Long) {
        DEFAULT_DECKS.forEachIndexed { index, deck ->
            val existingDeck = deckDao.getDeckBySourceTag(deck.tag)
            if (existingDeck == null) {
                deckDao.insertDeck(
                    DeckEntity(
                        name = deck.name,
                        description = deck.description,
                        type = DeckType.JLPT,
                        sourceTag = deck.tag,
                        displayOrder = index,
                        createdAt = now + index,
                    ),
                )
            }
        }
    }

    suspend fun createCustomDeck(name: String): Long = withContext(Dispatchers.IO) {
        deckDao.insertDeck(
            DeckEntity(
                name = name.trim(),
                description = "직접 만든 커스텀 단어장",
                type = DeckType.CUSTOM,
                sourceTag = "CUSTOM",
                displayOrder = Int.MAX_VALUE,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getDeckDetail(deckId: Long): DeckDetailData = withContext(Dispatchers.IO) {
        expireStaleTests()
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        DeckDetailData(deck = deck, words = deckDao.getWordsForDeck(deckId))
    }

    suspend fun getAllDecks() = withContext(Dispatchers.IO) { deckDao.getDecks() }

    suspend fun getAllWords(): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getAllWordsByNewest()
    }

    suspend fun addWordToDeck(deckId: Long, draft: WordDraft): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val normalizedKanji = draft.kanji.trim()
        val wordId = wordDao.insertWord(
            WordEntity(
                readingJa = draft.readingJa.trim(),
                readingKo = draft.readingKo.trim(),
                partOfSpeech = draft.partOfSpeech.trim(),
                grammar = draft.grammar.trim(),
                kanji = normalizedKanji,
                meaningJa = draft.meaningJa.trim(),
                meaningKo = draft.meaningKo.trim(),
                exampleJa = draft.exampleJa.trim(),
                exampleKo = draft.exampleKo.trim(),
                tag = draft.tag.trim(),
                note = draft.note.trim(),
                isKanaOnly = computeKanaOnlyFlag(normalizedKanji),
                createdAt = now,
            ),
        )
        deckDao.insertDeckWordCrossRef(
            DeckWordCrossRef(
                deckId = deckId,
                wordId = wordId,
                displayOrder = deckDao.getWordsForDeck(deckId).size,
                addedAt = now,
            ),
        )
        wordId
    }

    suspend fun updateWord(wordId: Long, draft: WordDraft) = withContext(Dispatchers.IO) {
        val existing = requireNotNull(wordDao.getWordById(wordId))
        val normalizedKanji = draft.kanji.trim()
        wordDao.updateWord(
            existing.copy(
                readingJa = draft.readingJa.trim(),
                readingKo = draft.readingKo.trim(),
                partOfSpeech = draft.partOfSpeech.trim(),
                grammar = draft.grammar.trim(),
                kanji = normalizedKanji,
                meaningJa = draft.meaningJa.trim(),
                meaningKo = draft.meaningKo.trim(),
                exampleJa = draft.exampleJa.trim(),
                exampleKo = draft.exampleKo.trim(),
                tag = draft.tag.trim(),
                note = draft.note.trim(),
                isKanaOnly = computeKanaOnlyFlag(normalizedKanji),
            ),
        )
    }

    suspend fun getWord(wordId: Long): WordEntity? = withContext(Dispatchers.IO) {
        wordDao.getWordById(wordId)
    }

    suspend fun getWordDetail(wordId: Long): WordDetailData = withContext(Dispatchers.IO) {
        val word = requireNotNull(wordDao.getWordById(wordId))
        WordDetailData(
            word = word,
            includedDecks = deckDao.getDecksForWord(wordId),
            allDecks = deckDao.getDecks(),
            allWords = wordDao.getAllWordsByNewest(),
        )
    }

    suspend fun addWordToExistingDeck(wordId: Long, deckId: Long) = withContext(Dispatchers.IO) {
        val existingIds = deckDao.getWordsForDeck(deckId).map { it.id }.toSet()
        if (wordId in existingIds) return@withContext
        deckDao.insertDeckWordCrossRef(
            DeckWordCrossRef(
                deckId = deckId,
                wordId = wordId,
                displayOrder = existingIds.size,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getUnseenWordCountForDeck(deckId: Long): Int = withContext(Dispatchers.IO) {
        val attemptedWordIds = studyDao.getAllActiveLogsNewestFirst().map { it.wordId }.toSet()
        deckDao.getWordsForDeck(deckId).count { it.id !in attemptedWordIds }
    }

    suspend fun createExamSession(deckId: Long?, settings: ExamSettings, useAiSelection: Boolean): Long = withContext(Dispatchers.IO) {
        expireStaleTests()
        val requestedWordCount = settings.wordCount
        val selectedWords = if (useAiSelection) {
            buildAiWordSelection(limit = requestedWordCount)
        } else {
            requireNotNull(deckId)
            val attemptedWordIds = if (settings.onlyUnseenWords) {
                studyDao.getAllActiveLogsNewestFirst().map { it.wordId }.toSet()
            } else {
                emptySet()
            }
            val baseWords = deckDao.getWordsForDeck(deckId).let { words ->
                if (settings.onlyUnseenWords) words.filter { it.id !in attemptedWordIds } else words
            }
            val ordered = when (settings.wordOrder) {
                WordOrder.SEQUENTIAL -> baseWords
                WordOrder.RANDOM -> baseWords.shuffled(Random(System.currentTimeMillis()))
            }
            requestedWordCount?.let { ordered.take(it.coerceAtMost(ordered.size)) } ?: ordered
        }
        val deckName = if (useAiSelection) "AI 단어장" else requireNotNull(deckDao.getDeckById(requireNotNull(deckId))).name
        val now = System.currentTimeMillis()
        studyDao.insertTest(
            TestEntity(
                status = TestStatus.IN_PROGRESS,
                deckId = deckId,
                deckNameSnapshot = deckName,
                isAiDeck = useAiSelection,
                wordOrder = settings.wordOrder,
                frontField = settings.frontField,
                revealField = settings.revealField,
                wordIdsSerialized = selectedWords.joinToString(",") { it.id.toString() },
                totalWordCount = selectedWords.size,
                startedAt = now,
                changedAt = now,
            ),
        )
    }

    suspend fun getInProgressExamData(deckId: Long?, isAiDeck: Boolean): InProgressExamData? = withContext(Dispatchers.IO) {
        expireStaleTests()
        val test = when {
            isAiDeck -> studyDao.getInProgressAiTest()
            deckId != null -> studyDao.getInProgressTestForDeck(deckId)
            else -> null
        } ?: return@withContext null
        InProgressExamData(
            testId = test.id,
            deckName = test.deckNameSnapshot,
            answeredCount = studyDao.getLogsForTest(test.id).size,
            totalCount = test.totalWordCount,
            startedAt = test.startedAt,
        )
    }

    suspend fun getExamSessionData(sessionId: Long): ExamSessionData = withContext(Dispatchers.IO) {
        expireStaleTests()
        val test = requireNotNull(studyDao.getTestById(sessionId))
        val wordIds = parseWordIds(test.wordIdsSerialized)
        val wordsById = wordDao.getWordsByIds(wordIds).associateBy { it.id }
        ExamSessionData(
            test = test,
            words = wordIds.mapNotNull { wordsById[it] },
            answersCount = studyDao.getLogsForTest(sessionId).size,
        )
    }

    suspend fun recordExamAnswer(sessionId: Long, wordId: Long, sequenceIndex: Int, isCorrect: Boolean): Boolean = withContext(Dispatchers.IO) {
        expireStaleTests()
        val test = requireNotNull(studyDao.getTestById(sessionId))
        check(test.status == TestStatus.IN_PROGRESS) { "시험이 이미 종료되었어요." }
        val answeredAt = System.currentTimeMillis()
        studyDao.insertTestWordLog(
            TestWordLogEntity(
                testId = sessionId,
                wordId = wordId,
                sequenceIndex = sequenceIndex,
                isCorrect = isCorrect,
                answeredAt = answeredAt,
            ),
        )
        val logs = studyDao.getLogsForTest(sessionId)
        val finished = logs.size >= test.totalWordCount
        if (finished) {
            val correctCount = logs.count { it.isCorrect }
            val wrongCount = logs.size - correctCount
            studyDao.updateTestStatus(sessionId, TestStatus.COMPLETED.name, answeredAt)
            studyDao.insertEndedTestResult(
                EndedTestResultEntity(
                    testId = sessionId,
                    deckId = test.deckId,
                    deckNameSnapshot = test.deckNameSnapshot,
                    isAiDeck = test.isAiDeck,
                    totalWordCount = test.totalWordCount,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    accuracyPercent = if (logs.isEmpty()) 0 else (correctCount * 100) / logs.size,
                    startedAt = test.startedAt,
                    endedAt = answeredAt,
                    durationSeconds = ((answeredAt - test.startedAt) / 1000L).coerceAtLeast(0L),
                ),
            )
        }
        finished
    }

    suspend fun getSessionResult(sessionId: Long): SessionResult = withContext(Dispatchers.IO) {
        expireStaleTests()
        val test = requireNotNull(studyDao.getTestById(sessionId))
        val result = requireNotNull(studyDao.getEndedResultByTestId(sessionId))
        val sessionLogs = studyDao.getLogsForTest(sessionId)
        val allLogs = studyDao.getAllActiveLogsNewestFirst()
        val allWords = wordDao.getAllWordsByNewest()
        val wordsById = allWords.associateBy { it.id }
        val currentWords = parseWordIds(test.wordIdsSerialized).mapNotNull { wordsById[it] }
        val currentStats = buildSortedWordStats(currentWords, sessionLogs)
        SessionResult(
            summary = buildSessionSummary(result),
            progress = buildSessionProgress(sessionLogs),
            topMissedWords = buildSortedWordStats(allWords, allLogs).filter { it.wrongCount > 0 }.take(5),
            missedWords = currentStats.filter { it.wrongCount > 0 },
            insights = buildResultInsights(test, sessionLogs, allLogs, wordsById, result.endedAt),
        )
    }

    suspend fun getTopMissedWords(limit: Int): List<WordAggregateStat> = withContext(Dispatchers.IO) {
        buildSortedWordStats(wordDao.getAllWordsByNewest(), studyDao.getAllActiveLogsNewestFirst()).take(limit)
    }

    suspend fun getRecentSessionSummaries(): List<SessionSummary> = withContext(Dispatchers.IO) {
        buildRecentSessionSummaries(studyDao.getRecentCompletedResults(limit = 5))
    }

    suspend fun getDeckStats(deckId: Long): DeckStatsData = withContext(Dispatchers.IO) {
        expireStaleTests()
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val results = studyDao.getCompletedResultsForDeck(deckId)
        val logs = studyDao.getLogsForTestIds(results.map { it.testId })
        val wordStats = buildSortedWordStats(words, logs)
        DeckStatsData(
            summary = buildDeckStatsSummary(deckId, deck.name, words, results, logs),
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
            allWordStats = wordStats,
            dailyStats = buildDeckDailyStats(logs),
        )
    }

    suspend fun getDeckDateStats(deckId: Long, dateKey: String): DeckDateStatsData = withContext(Dispatchers.IO) {
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val results = studyDao.getCompletedResultsForDeck(deckId)
        val logs = studyDao.getLogsForTestIds(results.map { it.testId }).filter { dateKeyFromEpochMillis(it.answeredAt) == dateKey }
        val resultMap = results.associateBy { it.testId }
        val wordStats = buildSortedWordStats(words, logs)
        DeckDateStatsData(
            deckId = deckId,
            deckName = deck.name,
            dateKey = dateKey,
            dateLabel = dateLabelFromKey(dateKey),
            totalWordCount = words.size,
            studiedWordCount = logs.map { it.wordId }.toSet().size,
            unstudiedWordCount = (words.size - logs.map { it.wordId }.toSet().size).coerceAtLeast(0),
            sessions = logs.groupBy { it.testId }.mapNotNull { (testId, testLogs) ->
                val result = resultMap[testId] ?: return@mapNotNull null
                DeckDateSessionSummary(
                    testId = testId,
                    endedAt = result.endedAt,
                    answeredCount = testLogs.size,
                    totalCount = result.totalWordCount,
                    correctCount = testLogs.count { it.isCorrect },
                    wrongCount = testLogs.count { !it.isCorrect },
                    accuracyPercent = if (testLogs.isEmpty()) 0 else (testLogs.count { it.isCorrect } * 100) / testLogs.size,
                )
            }.sortedByDescending { it.endedAt },
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
        )
    }

    suspend fun getGlobalStats(range: StatsDateRange): GlobalStatsData = withContext(Dispatchers.IO) {
        expireStaleTests()
        val words = wordDao.getAllWordsByNewest()
        val filteredLogs = filterLogsByRange(studyDao.getAllActiveLogsNewestFirst(), range)
        val resultMap = studyDao.getRecentCompletedResults(limit = Int.MAX_VALUE).associateBy { it.testId }
        val filteredResults = resultMap.values.filter { result -> filteredLogs.any { it.testId == result.testId } }
        val wordStats = buildSortedWordStats(words, filteredLogs)
        GlobalStatsData(
            range = range,
            rangeLabel = describeRange(range),
            summary = buildGlobalStatsSummary(words, filteredResults, filteredLogs),
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
            allWordStats = wordStats,
            dailyStats = buildGlobalDailyStats(filteredLogs),
            recentSessions = buildRecentSessionSummaries(filteredResults.take(10)),
        )
    }

    suspend fun getCompletedTestResultPage(limit: Int, offset: Int): List<SessionSummary> = withContext(Dispatchers.IO) {
        expireStaleTests()
        buildRecentSessionSummaries(studyDao.getCompletedResultPage(limit = limit, offset = offset))
    }

    suspend fun markTestDeleted(testId: Long) = withContext(Dispatchers.IO) {
        val test = studyDao.getTestById(testId) ?: return@withContext
        if (test.status == TestStatus.DELETED) return@withContext
        studyDao.updateTestStatus(testId, TestStatus.DELETED.name, System.currentTimeMillis())
    }

    private suspend fun buildAiWordSelection(limit: Int?): List<WordEntity> {
        val targetSize = limit?.coerceAtLeast(1) ?: AI_DECK_SIZE
        val words = wordDao.getAllWordsByNewest()
        val logs = studyDao.getAllActiveLogsNewestFirst()
        val stats = buildWordStats(words, logs)
        val frequentMissed = stats.filter { it.isFrequentlyMissed }.map { it.word }
        val newestWords = words.sortedByDescending { it.createdAt }
        val unseenIds = words.map { it.id }.toSet() - logs.map { it.wordId }.toSet()
        val unseenWords = words.filter { it.id in unseenIds }
        val shuffled = words.shuffled(Random(System.currentTimeMillis()))
        return buildList {
            addUnique(frequentMissed, targetSize)
            addUnique(newestWords, targetSize)
            addUnique(unseenWords, targetSize)
            addUnique(shuffled, targetSize)
        }.take(targetSize)
    }

    private fun MutableList<WordEntity>.addUnique(words: List<WordEntity>, limit: Int) {
        words.forEach { word ->
            if (none { it.id == word.id } && size < limit) add(word)
        }
    }

    private fun buildRecentSessionSummaries(results: List<EndedTestResultEntity>): List<SessionSummary> =
        results.sortedByDescending { it.endedAt }.map(::buildSessionSummary)

    private fun buildSessionSummary(result: EndedTestResultEntity): SessionSummary = SessionSummary(
        testId = result.testId,
        deckName = result.deckNameSnapshot,
        totalCount = result.totalWordCount,
        // EndedTestResultEntity is only written after the exam is fully completed.
        answeredCount = result.totalWordCount,
        correctCount = result.correctCount,
        wrongCount = result.wrongCount,
        accuracyPercent = result.accuracyPercent,
        recordedAt = result.endedAt,
        isCompleted = true,
    )

    private fun buildSortedWordStats(words: List<WordEntity>, logs: List<TestWordLogEntity>): List<WordAggregateStat> =
        buildWordStats(words, logs).sortedWith(
            compareByDescending<WordAggregateStat> { it.wrongCount }
                .thenByDescending { it.attemptCount }
                .thenBy { it.word.id },
        )

    private fun buildWordStats(words: List<WordEntity>, logs: List<TestWordLogEntity>): List<WordAggregateStat> {
        val grouped = logs.groupBy { it.wordId }
        return words.map { word ->
            val items = grouped[word.id].orEmpty()
            val attempts = items.size
            val wrong = items.count { !it.isCorrect }
            WordAggregateStat(
                word = word,
                attemptCount = attempts,
                wrongCount = wrong,
                wrongRatePercent = if (attempts == 0) 0 else (wrong * 100) / attempts,
                recentWrongCount = items.take(5).count { !it.isCorrect },
                isFrequentlyMissed = items.take(5).count { !it.isCorrect } >= 3,
            )
        }
    }

    private fun buildSessionProgress(logs: List<TestWordLogEntity>): List<SessionProgressPoint> {
        var correctCount = 0
        var wrongCount = 0
        return logs.sortedBy { it.sequenceIndex }.mapIndexed { index, log ->
            if (log.isCorrect) correctCount += 1 else wrongCount += 1
            val answeredCount = index + 1
            SessionProgressPoint(
                step = answeredCount,
                answeredCount = answeredCount,
                correctCount = correctCount,
                wrongCount = wrongCount,
                accuracyPercent = (correctCount * 100) / answeredCount,
            )
        }
    }

    private fun buildResultInsights(
        test: TestEntity,
        sessionLogs: List<TestWordLogEntity>,
        allLogs: List<TestWordLogEntity>,
        wordsById: Map<Long, WordEntity>,
        sessionTimestamp: Long,
    ): List<ResultInsight> {
        val threeDayStart = sessionTimestamp - THREE_DAYS_MILLIS
        val currentWordIds = sessionLogs.map { it.wordId }.toSet()
        val previousLogs = allLogs.filter { it.testId != test.id && it.wordId in currentWordIds }
        val repeatedMissWords = sessionLogs.filter { !it.isCorrect }.mapNotNull { log ->
            if (previousLogs.any { it.wordId == log.wordId && !it.isCorrect && it.answeredAt in threeDayStart until log.answeredAt }) {
                wordsById[log.wordId]
            } else null
        }.distinctBy { it.id }
        val streakMissWords = sessionLogs.filter { !it.isCorrect }.mapNotNull { log ->
            if (previousLogs.count { it.wordId == log.wordId && !it.isCorrect && it.answeredAt in threeDayStart until log.answeredAt } >= 2) {
                wordsById[log.wordId]
            } else null
        }.distinctBy { it.id }
        val yesterday = Instant.ofEpochMilli(sessionTimestamp).atZone(APP_ZONE_ID).toLocalDate().minusDays(1)
        val improvedWords = sessionLogs.filter { it.isCorrect }.mapNotNull { log ->
            if (previousLogs.any {
                    it.wordId == log.wordId &&
                        !it.isCorrect &&
                        Instant.ofEpochMilli(it.answeredAt).atZone(APP_ZONE_ID).toLocalDate() == yesterday
                }
            ) {
                wordsById[log.wordId]
            } else null
        }.distinctBy { it.id }
        return buildList {
            if (repeatedMissWords.isNotEmpty()) {
                add(ResultInsight(ResultInsightType.REPEATED_MISS, "최근 3일 내 다시 틀린 단어", "최근에도 틀렸고 이번에도 흔들린 단어예요.", repeatedMissWords.take(5)))
            }
            if (streakMissWords.isNotEmpty()) {
                add(ResultInsight(ResultInsightType.STREAK_MISS, "연속 오답 경향 단어", "최근 3일 동안 반복해서 틀린 흐름이 이어지고 있어요.", streakMissWords.take(5)))
            }
            if (improvedWords.isNotEmpty()) {
                add(ResultInsight(ResultInsightType.IMPROVED_WORD, "어제보다 나아진 단어", "어제는 틀렸지만 이번에는 맞힌 단어예요.", improvedWords.take(5)))
            }
        }
    }

    private fun buildDeckStatsSummary(
        deckId: Long,
        deckName: String,
        words: List<WordEntity>,
        results: List<EndedTestResultEntity>,
        logs: List<TestWordLogEntity>,
    ): DeckStatsSummary {
        val totalQuestionCount = logs.size
        val totalCorrectCount = logs.count { it.isCorrect }
        val studiedWordCount = logs.map { it.wordId }.toSet().size
        return DeckStatsSummary(
            deckId = deckId,
            deckName = deckName,
            totalWordCount = words.size,
            studiedWordCount = studiedWordCount,
            unstudiedWordCount = (words.size - studiedWordCount).coerceAtLeast(0),
            recordedSessionCount = results.size,
            totalQuestionCount = totalQuestionCount,
            totalWrongCount = totalQuestionCount - totalCorrectCount,
            accuracyPercent = if (totalQuestionCount == 0) 0 else (totalCorrectCount * 100) / totalQuestionCount,
        )
    }

    private fun buildGlobalStatsSummary(
        words: List<WordEntity>,
        results: List<EndedTestResultEntity>,
        logs: List<TestWordLogEntity>,
    ): GlobalStatsSummary {
        val totalQuestionCount = logs.size
        val totalCorrectCount = logs.count { it.isCorrect }
        return GlobalStatsSummary(
            totalQuestionCount = totalQuestionCount,
            studiedWordCount = logs.map { it.wordId }.toSet().size.coerceAtMost(words.size),
            totalCorrectCount = totalCorrectCount,
            totalWrongCount = totalQuestionCount - totalCorrectCount,
            recordedSessionCount = results.size,
            accuracyPercent = if (totalQuestionCount == 0) 0 else (totalCorrectCount * 100) / totalQuestionCount,
        )
    }

    private fun buildDeckDailyStats(logs: List<TestWordLogEntity>): List<DeckDailyStat> =
        logs.groupBy { dateKeyFromEpochMillis(it.answeredAt) }
            .map { (dateKey, items) ->
                val total = items.size
                val correct = items.count { it.isCorrect }
                DeckDailyStat(
                    dateKey = dateKey,
                    dateLabel = dateLabelFromKey(dateKey),
                    completedSessionCount = items.map { it.testId }.distinct().size,
                    totalQuestionCount = total,
                    correctCount = correct,
                    wrongCount = total - correct,
                    accuracyPercent = if (total == 0) 0 else (correct * 100) / total,
                )
            }
            .sortedByDescending { it.dateKey }

    private fun buildGlobalDailyStats(logs: List<TestWordLogEntity>): List<GlobalDailyStat> =
        logs.groupBy { dateKeyFromEpochMillis(it.answeredAt) }
            .map { (dateKey, items) ->
                val total = items.size
                val correct = items.count { it.isCorrect }
                GlobalDailyStat(
                    dateKey = dateKey,
                    dateLabel = dateLabelFromKey(dateKey),
                    recordedSessionCount = items.map { it.testId }.distinct().size,
                    totalQuestionCount = total,
                    correctCount = correct,
                    wrongCount = total - correct,
                    uniqueWordCount = items.map { it.wordId }.distinct().size,
                    accuracyPercent = if (total == 0) 0 else (correct * 100) / total,
                )
            }
            .sortedByDescending { it.dateKey }

    private fun filterLogsByRange(logs: List<TestWordLogEntity>, range: StatsDateRange): List<TestWordLogEntity> {
        val (startMillis, endExclusiveMillis) = resolveRangeMillis(range)
        return logs.filter { log ->
            (startMillis == null || log.answeredAt >= startMillis) &&
                (endExclusiveMillis == null || log.answeredAt < endExclusiveMillis)
        }
    }

    private fun resolveRangeMillis(range: StatsDateRange): Pair<Long?, Long?> {
        val today = LocalDate.now(APP_ZONE_ID)
        val resolvedRange = when (range.preset) {
            StatsDatePreset.TODAY -> today to today.plusDays(1)
            StatsDatePreset.LAST_7_DAYS -> today.minusDays(6) to today.plusDays(1)
            StatsDatePreset.LAST_30_DAYS -> today.minusDays(29) to today.plusDays(1)
            StatsDatePreset.ALL -> null to null
            StatsDatePreset.CUSTOM -> range.startDate to range.endDate?.plusDays(1)
        }
        return resolvedRange.first?.atStartOfDay(APP_ZONE_ID)?.toInstant()?.toEpochMilli() to
            resolvedRange.second?.atStartOfDay(APP_ZONE_ID)?.toInstant()?.toEpochMilli()
    }

    private fun describeRange(range: StatsDateRange): String = when (range.preset) {
        StatsDatePreset.TODAY -> "오늘"
        StatsDatePreset.LAST_7_DAYS -> "최근 7일"
        StatsDatePreset.LAST_30_DAYS -> "최근 30일"
        StatsDatePreset.ALL -> "전체 기간"
        StatsDatePreset.CUSTOM -> {
            val start = range.startDate?.format(INPUT_DATE_FORMATTER) ?: "?"
            val end = range.endDate?.format(INPUT_DATE_FORMATTER) ?: "?"
            "$start ~ $end"
        }
    }

    private fun dateKeyFromEpochMillis(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(APP_ZONE_ID).toLocalDate().format(DATE_KEY_FORMATTER)

    private fun dateLabelFromKey(dateKey: String): String =
        DATE_KEY_FORMATTER.parse(dateKey, LocalDate::from).format(DATE_LABEL_FORMATTER)

    private fun parseWordIds(serialized: String): List<Long> =
        serialized.split(",").mapNotNull { it.toLongOrNull() }

    private suspend fun repairLegacySeedWords(allExistingWords: MutableList<WordEntity>) {
        val legacyWord = allExistingWords.firstOrNull {
            it.readingJa == "うまれる" &&
                it.kanji == "生(ま)れる·産(ま)れる" &&
                it.meaningKo == "태어나다. 출생하다"
        } ?: return
        val correctedWord = legacyWord.copy(kanji = "生まれる", isKanaOnly = false)
        wordDao.updateWord(correctedWord)
        val index = allExistingWords.indexOfFirst { it.id == legacyWord.id }
        if (index >= 0) allExistingWords[index] = correctedWord
    }

    private suspend fun syncKanaOnlyFlags(words: List<WordEntity>) {
        words.forEach { word ->
            val expected = computeKanaOnlyFlag(word.kanji)
            if (word.isKanaOnly != expected) {
                wordDao.updateWord(word.copy(isKanaOnly = expected))
            }
        }
    }

    private suspend fun expireStaleTests() {
        val now = System.currentTimeMillis()
        studyDao.expireStaleTests(now - TEST_EXPIRY_MILLIS, now)
    }

    private fun computeKanaOnlyFlag(kanjiText: String): Boolean {
        val normalized = kanjiText.trim()
        if (normalized.isBlank()) return false
        return normalized.all { it.isWhitespace() || it in KANA_COMPATIBLE_SYMBOLS || it.isKana() }
    }

    private fun Char.isKana(): Boolean =
        this in '\u3040'..'\u309f' || this in '\u30a0'..'\u30ff'

    private data class DefaultDeck(
        val key: String,
        val name: String,
        val description: String,
        val tag: String,
    )

    private companion object {
        private const val AI_DECK_SIZE = 30
        private const val SEED_KEY = "seed_v6"
        private const val SEEDED_VALUE = "done"
        private const val THEME_PRESET_KEY = "theme_preset"
        private const val TEST_EXPIRY_MILLIS = 24L * 60L * 60L * 1000L
        private const val THREE_DAYS_MILLIS = 3L * TEST_EXPIRY_MILLIS
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATE_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val INPUT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val KANA_COMPATIBLE_SYMBOLS = setOf('・', 'ー', '〜', '～', ' ', '　', '(', ')', '（', '）')

        private val DEFAULT_DECKS = listOf(
            DefaultDeck("N5", "JLPT N5", "기초 일본어 단어장", "JLPT N5"),
            DefaultDeck("N4", "JLPT N4", "초급 일본어 단어장", "JLPT N4"),
            DefaultDeck("N3", "JLPT N3", "중급 입문 단어장", "JLPT N3"),
            DefaultDeck("N2", "JLPT N2", "중상급 일본어 단어장", "JLPT N2"),
            DefaultDeck("N1", "JLPT N1", "고급 일본어 단어장", "JLPT N1"),
        )
    }
}
