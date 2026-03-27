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
import com.mistbottle.jpnwordtrainer.data.local.entity.StudyAnswerEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.StudySessionEntity
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
import com.mistbottle.jpnwordtrainer.data.model.SessionStatus
import com.mistbottle.jpnwordtrainer.data.model.SessionSummary
import com.mistbottle.jpnwordtrainer.data.model.StatsDatePreset
import com.mistbottle.jpnwordtrainer.data.model.StatsDateRange
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
        studyDao.observeAnswerCount(),
    ) { decks, _ ->
        val allWords = wordDao.getAllWordsByNewest()
        val allSessions = studyDao.getAllSessions()
        val allAnswers = studyDao.getAllAnswersNewestFirst()
        HomeData(
            jlptDecks = decks.filter { it.type == DeckType.JLPT },
            customDecks = decks.filter { it.type == DeckType.CUSTOM },
            totalWordCount = allWords.size,
            recentSessions = buildRecentSessionSummaries(allSessions, limit = 5),
            globalStatsSummary = buildGlobalStatsSummary(allWords, allSessions, allAnswers),
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
        if (appSettingDao.getValue(SEED_KEY) == SEEDED_VALUE) {
            return@withContext
        }

        val now = System.currentTimeMillis()
        ensureDefaultDecks(now)
        val allExistingWords = wordDao.getAllWordsByNewest().toMutableList()
        repairLegacySeedWords(allExistingWords)
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
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        DeckDetailData(deck = deck, words = words)
    }

    suspend fun getAllDecks(): List<com.mistbottle.jpnwordtrainer.data.model.DeckWithCount> = withContext(Dispatchers.IO) {
        deckDao.getDecks()
    }

    suspend fun getAllWords(): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getAllWordsByNewest()
    }

    suspend fun addWordToDeck(deckId: Long, draft: WordDraft): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val wordId = wordDao.insertWord(
            WordEntity(
                readingJa = draft.readingJa.trim(),
                readingKo = draft.readingKo.trim(),
                partOfSpeech = draft.partOfSpeech.trim(),
                grammar = draft.grammar.trim(),
                kanji = draft.kanji.trim(),
                meaningJa = draft.meaningJa.trim(),
                meaningKo = draft.meaningKo.trim(),
                exampleJa = draft.exampleJa.trim(),
                exampleKo = draft.exampleKo.trim(),
                tag = draft.tag.trim(),
                note = draft.note.trim(),
                createdAt = now,
            ),
        )
        val displayOrder = deckDao.getWordsForDeck(deckId).size
        deckDao.insertDeckWordCrossRef(
            DeckWordCrossRef(
                deckId = deckId,
                wordId = wordId,
                displayOrder = displayOrder,
                addedAt = now,
            ),
        )
        wordId
    }

    suspend fun updateWord(wordId: Long, draft: WordDraft) = withContext(Dispatchers.IO) {
        val existing = requireNotNull(wordDao.getWordById(wordId))
        wordDao.updateWord(
            existing.copy(
                readingJa = draft.readingJa.trim(),
                readingKo = draft.readingKo.trim(),
                partOfSpeech = draft.partOfSpeech.trim(),
                grammar = draft.grammar.trim(),
                kanji = draft.kanji.trim(),
                meaningJa = draft.meaningJa.trim(),
                meaningKo = draft.meaningKo.trim(),
                exampleJa = draft.exampleJa.trim(),
                exampleKo = draft.exampleKo.trim(),
                tag = draft.tag.trim(),
                note = draft.note.trim(),
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
        val attemptedWordIds = studyDao.getAllAnswersNewestFirst().map { it.wordId }.toSet()
        deckDao.getWordsForDeck(deckId).count { it.id !in attemptedWordIds }
    }

    suspend fun createExamSession(
        deckId: Long?,
        settings: ExamSettings,
        useAiSelection: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        val requestedWordCount = settings.wordCount
        val selectedWords = if (useAiSelection) {
            buildAiWordSelection(limit = requestedWordCount)
        } else {
            requireNotNull(deckId)
            val attemptedWordIds = if (settings.onlyUnseenWords) {
                studyDao.getAllAnswersNewestFirst().map { it.wordId }.toSet()
            } else {
                emptySet()
            }
            val deckWords = deckDao.getWordsForDeck(deckId).let { words ->
                if (settings.onlyUnseenWords) {
                    words.filter { it.id !in attemptedWordIds }
                } else {
                    words
                }
            }
            val orderedWords = when (settings.wordOrder) {
                WordOrder.SEQUENTIAL -> deckWords
                WordOrder.RANDOM -> deckWords.shuffled(Random(System.currentTimeMillis()))
            }
            requestedWordCount?.let { limit ->
                orderedWords.take(limit.coerceAtMost(orderedWords.size))
            } ?: orderedWords
        }

        val deckName = if (useAiSelection) {
            "AI 단어장"
        } else {
            requireNotNull(deckDao.getDeckById(requireNotNull(deckId))).name
        }

        val now = System.currentTimeMillis()
        studyDao.insertSession(
            StudySessionEntity(
                deckId = deckId,
                deckName = deckName,
                isAiDeck = useAiSelection,
                wordOrder = settings.wordOrder,
                frontField = settings.frontField,
                revealField = settings.revealField,
                wordIdsSerialized = selectedWords.joinToString(",") { it.id.toString() },
                totalCount = selectedWords.size,
                answeredCount = 0,
                correctCount = 0,
                wrongCount = 0,
                startedAt = now,
                lastAnsweredAt = now,
                status = SessionStatus.IN_PROGRESS,
                completedAt = null,
            ),
        )
    }

    suspend fun getInProgressExamData(
        deckId: Long?,
        isAiDeck: Boolean,
    ): InProgressExamData? = withContext(Dispatchers.IO) {
        val session = when {
            isAiDeck -> studyDao.getInProgressAiSession()
            deckId != null -> studyDao.getInProgressSessionForDeck(deckId)
            else -> null
        } ?: return@withContext null

        InProgressExamData(
            sessionId = session.id,
            deckName = session.deckName,
            answeredCount = session.answeredCount,
            totalCount = session.totalCount,
            startedAt = session.startedAt,
        )
    }

    suspend fun getExamSessionData(sessionId: Long): ExamSessionData = withContext(Dispatchers.IO) {
        val session = requireNotNull(studyDao.getSessionById(sessionId))
        val wordIds = parseWordIds(session.wordIdsSerialized)
        val wordsById = wordDao.getWordsByIds(wordIds).associateBy { it.id }
        val orderedWords = wordIds.mapNotNull { wordsById[it] }
        ExamSessionData(
            session = session,
            words = orderedWords,
            answersCount = session.answeredCount,
        )
    }

    suspend fun recordExamAnswer(
        sessionId: Long,
        wordId: Long,
        sequenceIndex: Int,
        isCorrect: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val answeredAt = System.currentTimeMillis()
        studyDao.insertAnswer(
            StudyAnswerEntity(
                sessionId = sessionId,
                wordId = wordId,
                sequenceIndex = sequenceIndex,
                isCorrect = isCorrect,
                answeredAt = answeredAt,
            ),
        )

        val session = requireNotNull(studyDao.getSessionById(sessionId))
        val answeredCount = session.answeredCount + 1
        val correctCount = session.correctCount + if (isCorrect) 1 else 0
        val wrongCount = session.wrongCount + if (isCorrect) 0 else 1
        val finished = answeredCount >= session.totalCount
        studyDao.updateSessionProgress(
            sessionId = sessionId,
            answeredCount = answeredCount,
            correctCount = correctCount,
            wrongCount = wrongCount,
            lastAnsweredAt = answeredAt,
            status = if (finished) SessionStatus.COMPLETED.name else SessionStatus.IN_PROGRESS.name,
            completedAt = if (finished) answeredAt else null,
        )
        finished
    }

    suspend fun getSessionResult(sessionId: Long): SessionResult = withContext(Dispatchers.IO) {
        val session = requireNotNull(studyDao.getSessionById(sessionId))
        val sessionAnswers = studyDao.getAnswersForSession(sessionId)
        val allWords = wordDao.getAllWordsByNewest()
        val wordsById = allWords.associateBy { it.id }
        val currentSessionWords = parseWordIds(session.wordIdsSerialized).mapNotNull { wordsById[it] }
        val currentSessionWordStats = buildSortedWordStats(currentSessionWords, sessionAnswers)
        val allAnswers = studyDao.getAllAnswersNewestFirst()

        SessionResult(
            summary = buildSessionSummary(session),
            progress = buildSessionProgress(sessionAnswers),
            topMissedWords = buildSortedWordStats(allWords, allAnswers)
                .filter { it.wrongCount > 0 }
                .take(5),
            missedWords = currentSessionWordStats.filter { it.wrongCount > 0 },
            insights = buildResultInsights(
                session = session,
                sessionAnswers = sessionAnswers,
                allAnswers = allAnswers,
                wordsById = wordsById,
            ),
        )
    }

    suspend fun getTopMissedWords(limit: Int): List<WordAggregateStat> = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsByNewest()
        val answers = studyDao.getAllAnswersNewestFirst()
        buildWordStats(words, answers).sortedByDescending { it.wrongCount }.take(limit)
    }

    suspend fun getRecentSessionSummaries(): List<SessionSummary> = withContext(Dispatchers.IO) {
        buildRecentSessionSummaries(studyDao.getAllSessions(), limit = 5)
    }

    suspend fun getDeckStats(deckId: Long): DeckStatsData = withContext(Dispatchers.IO) {
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val sessions = studyDao.getSessionsForDeck(deckId).filter { it.answeredCount > 0 }
        val sessionIds = sessions.map { it.id }.toSet()
        val answers = studyDao.getAllAnswersNewestFirst().filter { it.sessionId in sessionIds }
        val wordStats = buildSortedWordStats(words, answers)

        DeckStatsData(
            summary = buildDeckStatsSummary(deckId, deck.name, words, sessions, answers),
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
            allWordStats = wordStats,
            dailyStats = buildDeckDailyStats(answers, sessions.associateBy { it.id }),
        )
    }

    suspend fun getDeckDateStats(deckId: Long, dateKey: String): DeckDateStatsData = withContext(Dispatchers.IO) {
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val sessions = studyDao.getSessionsForDeck(deckId).filter { it.answeredCount > 0 }
        val sessionIds = sessions.map { it.id }.toSet()
        val dayAnswers = studyDao.getAllAnswersNewestFirst()
            .filter { it.sessionId in sessionIds }
            .filter { dateKeyFromEpochMillis(it.answeredAt) == dateKey }
        val sessionMap = sessions.associateBy { it.id }
        val wordStats = buildSortedWordStats(words, dayAnswers)
        val studiedWordCount = dayAnswers.map { it.wordId }.toSet().size

        DeckDateStatsData(
            deckId = deckId,
            deckName = deck.name,
            dateKey = dateKey,
            dateLabel = dateLabelFromKey(dateKey),
            totalWordCount = words.size,
            studiedWordCount = studiedWordCount,
            unstudiedWordCount = (words.size - studiedWordCount).coerceAtLeast(0),
            sessions = dayAnswers
                .groupBy { it.sessionId }
                .mapNotNull { (sessionId, answersForSession) ->
                    val session = sessionMap[sessionId] ?: return@mapNotNull null
                    val answeredCount = answersForSession.size
                    val correctCount = answersForSession.count { it.isCorrect }
                    val wrongCount = answeredCount - correctCount
                    DeckDateSessionSummary(
                        sessionId = sessionId,
                        completedAt = answersForSession.maxOf { it.answeredAt },
                        answeredCount = answeredCount,
                        totalCount = session.totalCount,
                        correctCount = correctCount,
                        wrongCount = wrongCount,
                        accuracyPercent = if (answeredCount == 0) 0 else (correctCount * 100) / answeredCount,
                    )
                }
                .sortedByDescending { it.completedAt },
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
        )
    }

    suspend fun getGlobalStats(range: StatsDateRange): GlobalStatsData = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsByNewest()
        val sessions = studyDao.getAllSessions().filter { it.answeredCount > 0 }
        val answers = filterAnswersByRange(studyDao.getAllAnswersNewestFirst(), range)
        val filteredSessionIds = answers.map { it.sessionId }.toSet()
        val filteredSessions = sessions.filter { it.id in filteredSessionIds }
        val wordStats = buildSortedWordStats(words, answers)

        GlobalStatsData(
            range = range,
            rangeLabel = describeRange(range),
            summary = buildGlobalStatsSummary(words, filteredSessions, answers),
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
            allWordStats = wordStats,
            dailyStats = buildGlobalDailyStats(answers),
            recentSessions = buildRecentSessionSummaries(filteredSessions, limit = 10),
        )
    }

    private suspend fun buildAiWordSelection(limit: Int?): List<WordEntity> {
        val targetSize = limit?.coerceAtLeast(1) ?: AI_DECK_SIZE
        val words = wordDao.getAllWordsByNewest()
        val answers = studyDao.getAllAnswersNewestFirst()
        val stats = buildWordStats(words, answers)

        val frequentMissed = stats.filter { it.isFrequentlyMissed }.map { it.word }
        val newestWords = words.sortedByDescending { it.createdAt }
        val unseenWordIds = words.map { it.id }.toSet() - answers.map { it.wordId }.toSet()
        val unseenWords = words.filter { it.id in unseenWordIds }
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
            if (none { it.id == word.id } && size < limit) {
                add(word)
            }
        }
    }

    private fun buildRecentSessionSummaries(
        sessions: List<StudySessionEntity>,
        limit: Int,
    ): List<SessionSummary> {
        return sessions
            .filter { it.answeredCount > 0 }
            .sortedByDescending { it.lastAnsweredAt }
            .take(limit)
            .map(::buildSessionSummary)
    }

    private fun buildSessionSummary(session: StudySessionEntity): SessionSummary {
        return SessionSummary(
            sessionId = session.id,
            deckName = session.deckName,
            totalCount = session.totalCount,
            answeredCount = session.answeredCount,
            correctCount = session.correctCount,
            wrongCount = session.wrongCount,
            accuracyPercent = if (session.answeredCount == 0) 0 else (session.correctCount * 100) / session.answeredCount,
            recordedAt = session.completedAt ?: session.lastAnsweredAt,
            isCompleted = session.status == SessionStatus.COMPLETED,
        )
    }

    private fun buildSortedWordStats(
        words: List<WordEntity>,
        answersNewestFirst: List<StudyAnswerEntity>,
    ): List<WordAggregateStat> {
        return buildWordStats(words, answersNewestFirst).sortedWith(
            compareByDescending<WordAggregateStat> { it.wrongCount }
                .thenByDescending { it.attemptCount }
                .thenBy { it.word.readingJa }
        )
    }

    private fun buildWordStats(
        words: List<WordEntity>,
        answersNewestFirst: List<StudyAnswerEntity>,
    ): List<WordAggregateStat> {
        val grouped = answersNewestFirst.groupBy { it.wordId }
        return words.map { word ->
            val answers = grouped[word.id].orEmpty()
            val attemptCount = answers.size
            val wrongCount = answers.count { !it.isCorrect }
            val recentWrongCount = answers.take(5).count { !it.isCorrect }
            WordAggregateStat(
                word = word,
                attemptCount = attemptCount,
                wrongCount = wrongCount,
                wrongRatePercent = if (attemptCount == 0) 0 else (wrongCount * 100) / attemptCount,
                recentWrongCount = recentWrongCount,
                isFrequentlyMissed = recentWrongCount >= 3,
            )
        }
    }

    private fun buildSessionProgress(
        answers: List<StudyAnswerEntity>,
    ): List<SessionProgressPoint> {
        var correctCount = 0
        var wrongCount = 0
        return answers.sortedBy { it.sequenceIndex }.mapIndexed { index, answer ->
            if (answer.isCorrect) correctCount += 1 else wrongCount += 1
            val answeredCount = index + 1
            SessionProgressPoint(
                step = index + 1,
                answeredCount = answeredCount,
                correctCount = correctCount,
                wrongCount = wrongCount,
                accuracyPercent = (correctCount * 100) / answeredCount,
            )
        }
    }

    private fun buildResultInsights(
        session: StudySessionEntity,
        sessionAnswers: List<StudyAnswerEntity>,
        allAnswers: List<StudyAnswerEntity>,
        wordsById: Map<Long, WordEntity>,
    ): List<ResultInsight> {
        val sessionTimestamp = session.completedAt ?: session.lastAnsweredAt
        val threeDayStart = sessionTimestamp - THREE_DAYS_MILLIS
        val currentWordIds = sessionAnswers.map { it.wordId }.toSet()
        val previousAnswers = allAnswers.filter { it.sessionId != session.id && it.wordId in currentWordIds }

        val repeatedMissWords = sessionAnswers
            .filter { !it.isCorrect }
            .mapNotNull { answer ->
                val repeated = previousAnswers.any {
                    it.wordId == answer.wordId &&
                        !it.isCorrect &&
                        it.answeredAt in threeDayStart until answer.answeredAt
                }
                if (repeated) wordsById[answer.wordId] else null
            }
            .distinctBy { it.id }

        val streakMissWords = sessionAnswers
            .filter { !it.isCorrect }
            .mapNotNull { answer ->
                val streakWrongCount = previousAnswers.count {
                    it.wordId == answer.wordId &&
                        !it.isCorrect &&
                        it.answeredAt in threeDayStart until answer.answeredAt
                }
                if (streakWrongCount >= 2) wordsById[answer.wordId] else null
            }
            .distinctBy { it.id }

        val yesterday = Instant.ofEpochMilli(sessionTimestamp).atZone(APP_ZONE_ID).toLocalDate().minusDays(1)
        val improvedWords = sessionAnswers
            .filter { it.isCorrect }
            .mapNotNull { answer ->
                val hadYesterdayWrong = previousAnswers.any {
                    it.wordId == answer.wordId &&
                        !it.isCorrect &&
                        Instant.ofEpochMilli(it.answeredAt).atZone(APP_ZONE_ID).toLocalDate() == yesterday
                }
                if (hadYesterdayWrong) wordsById[answer.wordId] else null
            }
            .distinctBy { it.id }

        return buildList {
            if (repeatedMissWords.isNotEmpty()) {
                add(
                    ResultInsight(
                        type = ResultInsightType.REPEATED_MISS,
                        title = "최근 3일 내 다시 틀린 단어",
                        message = "최근에도 틀렸고 이번에도 흔들린 단어예요.",
                        words = repeatedMissWords.take(5),
                    ),
                )
            }
            if (streakMissWords.isNotEmpty()) {
                add(
                    ResultInsight(
                        type = ResultInsightType.STREAK_MISS,
                        title = "연속 오답 경향 단어",
                        message = "최근 3일 동안 반복해서 틀린 흐름이 이어지고 있어요.",
                        words = streakMissWords.take(5),
                    ),
                )
            }
            if (improvedWords.isNotEmpty()) {
                add(
                    ResultInsight(
                        type = ResultInsightType.IMPROVED_WORD,
                        title = "어제보다 나아진 단어",
                        message = "어제는 틀렸지만 이번에는 맞힌 단어예요.",
                        words = improvedWords.take(5),
                    ),
                )
            }
        }
    }

    private fun buildDeckStatsSummary(
        deckId: Long,
        deckName: String,
        words: List<WordEntity>,
        sessions: List<StudySessionEntity>,
        answers: List<StudyAnswerEntity>,
    ): DeckStatsSummary {
        val studiedWordIds = answers.map { it.wordId }.toSet()
        val totalQuestionCount = answers.size
        val totalCorrectCount = answers.count { it.isCorrect }
        val totalWrongCount = totalQuestionCount - totalCorrectCount

        return DeckStatsSummary(
            deckId = deckId,
            deckName = deckName,
            totalWordCount = words.size,
            studiedWordCount = studiedWordIds.size,
            unstudiedWordCount = (words.size - studiedWordIds.size).coerceAtLeast(0),
            recordedSessionCount = sessions.size,
            totalQuestionCount = totalQuestionCount,
            totalWrongCount = totalWrongCount,
            accuracyPercent = if (totalQuestionCount == 0) 0 else (totalCorrectCount * 100) / totalQuestionCount,
        )
    }

    private fun buildGlobalStatsSummary(
        words: List<WordEntity>,
        sessions: List<StudySessionEntity>,
        answers: List<StudyAnswerEntity>,
    ): GlobalStatsSummary {
        val studiedWordIds = answers.map { it.wordId }.toSet()
        val totalQuestionCount = answers.size
        val totalCorrectCount = answers.count { it.isCorrect }
        val totalWrongCount = totalQuestionCount - totalCorrectCount

        return GlobalStatsSummary(
            totalQuestionCount = totalQuestionCount,
            studiedWordCount = studiedWordIds.size.coerceAtMost(words.size),
            totalCorrectCount = totalCorrectCount,
            totalWrongCount = totalWrongCount,
            recordedSessionCount = sessions.count { it.answeredCount > 0 },
            accuracyPercent = if (totalQuestionCount == 0) 0 else (totalCorrectCount * 100) / totalQuestionCount,
        )
    }

    private fun buildDeckDailyStats(
        answers: List<StudyAnswerEntity>,
        sessionsById: Map<Long, StudySessionEntity>,
    ): List<DeckDailyStat> {
        return answers
            .groupBy { dateKeyFromEpochMillis(it.answeredAt) }
            .map { (dateKey, items) ->
                val totalQuestionCount = items.size
                val correctCount = items.count { it.isCorrect }
                val wrongCount = totalQuestionCount - correctCount
                DeckDailyStat(
                    dateKey = dateKey,
                    dateLabel = dateLabelFromKey(dateKey),
                    completedSessionCount = items.mapNotNull { sessionsById[it.sessionId]?.id }.distinct().size,
                    totalQuestionCount = totalQuestionCount,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    accuracyPercent = if (totalQuestionCount == 0) 0 else (correctCount * 100) / totalQuestionCount,
                )
            }
            .sortedByDescending { it.dateKey }
    }

    private fun buildGlobalDailyStats(
        answers: List<StudyAnswerEntity>,
    ): List<GlobalDailyStat> {
        return answers
            .groupBy { dateKeyFromEpochMillis(it.answeredAt) }
            .map { (dateKey, items) ->
                val totalQuestionCount = items.size
                val correctCount = items.count { it.isCorrect }
                val wrongCount = totalQuestionCount - correctCount
                GlobalDailyStat(
                    dateKey = dateKey,
                    dateLabel = dateLabelFromKey(dateKey),
                    recordedSessionCount = items.map { it.sessionId }.distinct().size,
                    totalQuestionCount = totalQuestionCount,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    uniqueWordCount = items.map { it.wordId }.distinct().size,
                    accuracyPercent = if (totalQuestionCount == 0) 0 else (correctCount * 100) / totalQuestionCount,
                )
            }
            .sortedByDescending { it.dateKey }
    }

    private fun filterAnswersByRange(
        answers: List<StudyAnswerEntity>,
        range: StatsDateRange,
    ): List<StudyAnswerEntity> {
        val (startMillis, endExclusiveMillis) = resolveRangeMillis(range)
        return answers.filter { answer ->
            val afterStart = startMillis == null || answer.answeredAt >= startMillis
            val beforeEnd = endExclusiveMillis == null || answer.answeredAt < endExclusiveMillis
            afterStart && beforeEnd
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
        Instant.ofEpochMilli(epochMillis)
            .atZone(APP_ZONE_ID)
            .toLocalDate()
            .format(DATE_KEY_FORMATTER)

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

        val correctedWord = legacyWord.copy(kanji = "生まれる")
        wordDao.updateWord(correctedWord)
        val index = allExistingWords.indexOfFirst { it.id == legacyWord.id }
        if (index >= 0) {
            allExistingWords[index] = correctedWord
        }
    }

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
        private const val THREE_DAYS_MILLIS = 3L * 24L * 60L * 60L * 1000L
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATE_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val INPUT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        private val DEFAULT_DECKS = listOf(
            DefaultDeck("N5", "JLPT N5", "기초 일본어 단어장", "JLPT N5"),
            DefaultDeck("N4", "JLPT N4", "초급 일본어 단어장", "JLPT N4"),
            DefaultDeck("N3", "JLPT N3", "중급 입문 단어장", "JLPT N3"),
            DefaultDeck("N2", "JLPT N2", "중상급 일본어 단어장", "JLPT N2"),
            DefaultDeck("N1", "JLPT N1", "고급 일본어 단어장", "JLPT N1"),
        )
    }
}
