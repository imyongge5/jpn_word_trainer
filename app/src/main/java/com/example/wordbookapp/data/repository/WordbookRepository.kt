package com.example.wordbookapp.data.repository

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
import com.example.wordbookapp.data.local.WordbookDatabase
import com.example.wordbookapp.data.local.entity.AppSettingEntity
import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.DeckWordCrossRef
import com.example.wordbookapp.data.local.entity.StudyAnswerEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity
import com.example.wordbookapp.data.local.entity.WordEntity
import com.example.wordbookapp.data.model.DeckDetailData
import com.example.wordbookapp.data.model.DeckDateSessionSummary
import com.example.wordbookapp.data.model.DeckDateStatsData
import com.example.wordbookapp.data.model.DeckDailyStat
import com.example.wordbookapp.data.model.DeckStatsData
import com.example.wordbookapp.data.model.DeckStatsSummary
import com.example.wordbookapp.data.model.DeckType
import com.example.wordbookapp.data.model.ExamSessionData
import com.example.wordbookapp.data.model.ExamSettings
import com.example.wordbookapp.data.model.HomeData
import com.example.wordbookapp.data.model.InProgressExamData
import com.example.wordbookapp.data.model.SessionResult
import com.example.wordbookapp.data.model.SessionSummary
import com.example.wordbookapp.data.model.ThemePreset
import com.example.wordbookapp.data.model.WordAggregateStat
import com.example.wordbookapp.data.model.WordDetailData
import com.example.wordbookapp.data.model.WordDraft
import com.example.wordbookapp.data.model.WordOrder

class WordbookRepository(
    private val database: WordbookDatabase,
) {
    private val wordDao = database.wordDao()
    private val deckDao = database.deckDao()
    private val studyDao = database.studyDao()
    private val appSettingDao = database.appSettingDao()

    fun observeHomeData(): Flow<HomeData> = combine(
        deckDao.observeDecks(),
    ) { values ->
        val decks = values.first()
        HomeData(
            jlptDecks = decks.filter { it.type == DeckType.JLPT },
            customDecks = decks.filter { it.type == DeckType.CUSTOM },
            totalWordCount = wordDao.getWordCount(),
            recentSessions = getRecentSessionSummaries(),
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

    suspend fun getAllDecks(): List<com.example.wordbookapp.data.model.DeckWithCount> = withContext(Dispatchers.IO) {
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
            val deckWords = deckDao.getWordsForDeck(deckId)
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
                correctCount = 0,
                wrongCount = 0,
                startedAt = System.currentTimeMillis(),
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

        val answers = studyDao.getAnswersForSession(session.id)
        InProgressExamData(
            sessionId = session.id,
            deckName = session.deckName,
            answeredCount = answers.size,
            totalCount = parseWordIds(session.wordIdsSerialized).size,
            startedAt = session.startedAt,
        )
    }

    suspend fun getExamSessionData(sessionId: Long): ExamSessionData = withContext(Dispatchers.IO) {
        val session = requireNotNull(studyDao.getSessionById(sessionId))
        val wordIds = parseWordIds(session.wordIdsSerialized)
        val wordsById = wordDao.getWordsByIds(wordIds).associateBy { it.id }
        val orderedWords = wordIds.mapNotNull { wordsById[it] }
        val answers = studyDao.getAnswersForSession(sessionId)
        ExamSessionData(
            session = session,
            words = orderedWords,
            answersCount = answers.size,
        )
    }

    suspend fun recordExamAnswer(
        sessionId: Long,
        wordId: Long,
        sequenceIndex: Int,
        isCorrect: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        studyDao.insertAnswer(
            StudyAnswerEntity(
                sessionId = sessionId,
                wordId = wordId,
                sequenceIndex = sequenceIndex,
                isCorrect = isCorrect,
                answeredAt = System.currentTimeMillis(),
            ),
        )
        val session = requireNotNull(studyDao.getSessionById(sessionId))
        val answers = studyDao.getAnswersForSession(sessionId)
        val targetCount = parseWordIds(session.wordIdsSerialized).size
        val finished = answers.size >= targetCount
        if (finished) {
            val correctCount = answers.count { it.isCorrect }
            val wrongCount = answers.size - correctCount
            studyDao.completeSession(
                sessionId = sessionId,
                totalCount = targetCount,
                correctCount = correctCount,
                wrongCount = wrongCount,
                completedAt = System.currentTimeMillis(),
            )
        }
        finished
    }

    suspend fun getSessionResult(sessionId: Long): SessionResult = withContext(Dispatchers.IO) {
        val session = requireNotNull(studyDao.getSessionById(sessionId))
        SessionResult(
            summary = SessionSummary(
                sessionId = sessionId,
                deckName = session.deckName,
                totalCount = session.totalCount,
                correctCount = session.correctCount,
                wrongCount = session.wrongCount,
                accuracyPercent = if (session.totalCount == 0) 0 else (session.correctCount * 100) / session.totalCount,
            ),
            topMissedWords = getTopMissedWords(limit = 5),
        )
    }

    suspend fun getTopMissedWords(limit: Int): List<WordAggregateStat> = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsByNewest()
        val answers = studyDao.getAllAnswersNewestFirst()
        buildWordStats(words, answers).sortedByDescending { it.wrongCount }.take(limit)
    }

    suspend fun getRecentSessionSummaries(): List<SessionSummary> = withContext(Dispatchers.IO) {
        studyDao.getRecentCompletedSessions(limit = 5).map { session ->
            SessionSummary(
                sessionId = session.id,
                deckName = session.deckName,
                totalCount = session.totalCount,
                correctCount = session.correctCount,
                wrongCount = session.wrongCount,
                accuracyPercent = if (session.totalCount == 0) 0 else (session.correctCount * 100) / session.totalCount,
            )
        }
    }

    suspend fun getDeckStats(deckId: Long): DeckStatsData = withContext(Dispatchers.IO) {
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val sessions = studyDao.getCompletedSessionsForDeck(deckId)
        val sessionIds = sessions.map { it.id }
        val answers = if (sessionIds.isEmpty()) {
            emptyList()
        } else {
            studyDao.getAnswersForSessionIds(sessionIds)
        }
        val wordStats = buildWordStats(words, answers).sortedWith(
            compareByDescending<WordAggregateStat> { it.wrongCount }
                .thenByDescending { it.attemptCount }
                .thenBy { it.word.readingJa }
        )
        val studiedWordIds = answers.map { it.wordId }.toSet()
        val totalQuestionCount = sessions.sumOf { it.totalCount }
        val totalWrongCount = sessions.sumOf { it.wrongCount }
        val totalCorrectCount = sessions.sumOf { it.correctCount }

        DeckStatsData(
            summary = DeckStatsSummary(
                deckId = deckId,
                deckName = deck.name,
                totalWordCount = words.size,
                studiedWordCount = studiedWordIds.size,
                unstudiedWordCount = (words.size - studiedWordIds.size).coerceAtLeast(0),
                completedSessionCount = sessions.size,
                totalQuestionCount = totalQuestionCount,
                totalWrongCount = totalWrongCount,
                accuracyPercent = if (totalQuestionCount == 0) 0 else (totalCorrectCount * 100) / totalQuestionCount,
            ),
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
            allWordStats = wordStats,
            dailyStats = buildDeckDailyStats(sessions),
        )
    }

    suspend fun getDeckDateStats(deckId: Long, dateKey: String): DeckDateStatsData = withContext(Dispatchers.IO) {
        val deck = requireNotNull(deckDao.getDeckById(deckId))
        val words = deckDao.getWordsForDeck(deckId)
        val sessions = studyDao.getCompletedSessionsForDeck(deckId)
        val sessionsForDate = sessions.filter { session ->
            session.completedAt?.let(::dateKeyFromEpochMillis) == dateKey
        }
        val sessionIds = sessionsForDate.map { it.id }
        val answers = if (sessionIds.isEmpty()) {
            emptyList()
        } else {
            studyDao.getAnswersForSessionIds(sessionIds)
        }
        val wordStats = buildWordStats(words, answers).sortedWith(
            compareByDescending<WordAggregateStat> { it.wrongCount }
                .thenByDescending { it.attemptCount }
                .thenBy { it.word.readingJa }
        )
        val studiedWordIds = answers.map { it.wordId }.toSet()

        DeckDateStatsData(
            deckId = deckId,
            deckName = deck.name,
            dateKey = dateKey,
            dateLabel = dateLabelFromKey(dateKey),
            totalWordCount = words.size,
            studiedWordCount = studiedWordIds.size,
            unstudiedWordCount = (words.size - studiedWordIds.size).coerceAtLeast(0),
            sessions = sessionsForDate.map { session ->
                DeckDateSessionSummary(
                    sessionId = session.id,
                    completedAt = session.completedAt ?: session.startedAt,
                    totalCount = session.totalCount,
                    correctCount = session.correctCount,
                    wrongCount = session.wrongCount,
                    accuracyPercent = if (session.totalCount == 0) 0 else (session.correctCount * 100) / session.totalCount,
                )
            },
            topMissedWords = wordStats.filter { it.wrongCount > 0 }.take(10),
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

    private fun buildDeckDailyStats(
        sessions: List<StudySessionEntity>,
    ): List<DeckDailyStat> {
        return sessions
            .filter { it.completedAt != null }
            .groupBy { session -> dateKeyFromEpochMillis(session.completedAt!!) }
            .map { (dateKey, items) ->
                val totalQuestionCount = items.sumOf { it.totalCount }
                val correctCount = items.sumOf { it.correctCount }
                val wrongCount = items.sumOf { it.wrongCount }
                DeckDailyStat(
                    dateKey = dateKey,
                    dateLabel = dateLabelFromKey(dateKey),
                    completedSessionCount = items.size,
                    totalQuestionCount = totalQuestionCount,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    accuracyPercent = if (totalQuestionCount == 0) 0 else (correctCount * 100) / totalQuestionCount,
                )
            }
            .sortedByDescending { it.dateKey }
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
        private val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATE_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

        private val DEFAULT_DECKS = listOf(
            DefaultDeck("N5", "JLPT N5", "기초 일본어 단어장", "JLPT N5"),
            DefaultDeck("N4", "JLPT N4", "초급 일본어 단어장", "JLPT N4"),
            DefaultDeck("N3", "JLPT N3", "중급 입문 단어장", "JLPT N3"),
            DefaultDeck("N2", "JLPT N2", "중상급 일본어 단어장", "JLPT N2"),
            DefaultDeck("N1", "JLPT N1", "고급 일본어 단어장", "JLPT N1"),
        )
    }
}
