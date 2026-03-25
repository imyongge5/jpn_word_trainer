package com.example.wordbookapp.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random
import com.example.wordbookapp.data.local.WordbookDatabase
import com.example.wordbookapp.data.local.entity.AppSettingEntity
import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.DeckWordCrossRef
import com.example.wordbookapp.data.local.entity.StudyAnswerEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity
import com.example.wordbookapp.data.local.entity.WordEntity
import com.example.wordbookapp.data.model.DeckDetailData
import com.example.wordbookapp.data.model.DeckType
import com.example.wordbookapp.data.model.ExamSessionData
import com.example.wordbookapp.data.model.ExamSettings
import com.example.wordbookapp.data.model.HomeData
import com.example.wordbookapp.data.model.SessionResult
import com.example.wordbookapp.data.model.SessionSummary
import com.example.wordbookapp.data.model.WordAggregateStat
import com.example.wordbookapp.data.model.WordDetailData
import com.example.wordbookapp.data.model.WordDraft
import com.example.wordbookapp.data.model.WordOrder

class WordbookRepository(
    private val context: Context,
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

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val deckIds = mutableMapOf<String, Long>()
        DEFAULT_DECKS.forEachIndexed { index, deck ->
            val existingDeck = deckDao.getDeckBySourceTag(deck.tag)
            val id = existingDeck?.id ?: deckDao.insertDeck(
                DeckEntity(
                    name = deck.name,
                    description = deck.description,
                    type = DeckType.JLPT,
                    sourceTag = deck.tag,
                    displayOrder = index,
                    createdAt = now + index,
                ),
            )
            deckIds[deck.key] = id
        }

        val allExistingWords = wordDao.getAllWordsByNewest().toMutableList()
        val wordIdBySignature = allExistingWords.associateBy(
            keySelector = { wordSignature(it.readingJa, it.kanji, it.meaningKo, it.tag) },
            valueTransform = { it.id },
        ).toMutableMap()

        val linkedSignaturesByDeck = deckIds.mapValues { (_, deckId) ->
            deckDao.getWordsForDeck(deckId).map {
                wordSignature(it.readingJa, it.kanji, it.meaningKo, it.tag)
            }.toMutableSet()
        }.toMutableMap()

        val displayOrderByDeck = deckIds.mapValues { (_, deckId) ->
            deckDao.getWordsForDeck(deckId).size
        }.toMutableMap()

        loadSeedRecords().forEachIndexed { index, record ->
            val signature = wordSignature(record.readingJa, record.kanji, record.meaningKo, record.tag)
            val wordId = wordIdBySignature[signature] ?: wordDao.insertWord(
                WordEntity(
                    readingJa = record.readingJa,
                    readingKo = record.readingKo,
                    partOfSpeech = record.partOfSpeech,
                    grammar = record.grammar,
                    kanji = record.kanji,
                    meaningJa = record.meaningJa,
                    meaningKo = record.meaningKo,
                    exampleJa = record.exampleJa,
                    exampleKo = record.exampleKo,
                    tag = record.tag,
                    note = record.note,
                    createdAt = now + 100 + index,
                ),
            ).also { insertedId ->
                wordIdBySignature[signature] = insertedId
            }

            val deckId = deckIds.getValue(record.deck)
            val linkedSignatures = linkedSignaturesByDeck.getValue(record.deck)
            if (signature !in linkedSignatures) {
                deckDao.insertDeckWordCrossRef(
                    DeckWordCrossRef(
                        deckId = deckId,
                        wordId = wordId,
                        displayOrder = displayOrderByDeck.getValue(record.deck),
                        addedAt = now + 100 + index,
                    ),
                )
                linkedSignatures += signature
                displayOrderByDeck[record.deck] = displayOrderByDeck.getValue(record.deck) + 1
            }
        }

        appSettingDao.upsert(AppSettingEntity(SEED_KEY, SEEDED_VALUE))
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
        val selectedWords = if (useAiSelection) {
            buildAiWordSelection()
        } else {
            requireNotNull(deckId)
            val deckWords = deckDao.getWordsForDeck(deckId)
            when (settings.wordOrder) {
                WordOrder.SEQUENTIAL -> deckWords
                WordOrder.RANDOM -> deckWords.shuffled(Random(System.currentTimeMillis()))
            }
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

    private suspend fun buildAiWordSelection(): List<WordEntity> {
        val words = wordDao.getAllWordsByNewest()
        val answers = studyDao.getAllAnswersNewestFirst()
        val stats = buildWordStats(words, answers)

        val frequentMissed = stats.filter { it.isFrequentlyMissed }.map { it.word }
        val newestWords = words.sortedByDescending { it.createdAt }
        val unseenWordIds = words.map { it.id }.toSet() - answers.map { it.wordId }.toSet()
        val unseenWords = words.filter { it.id in unseenWordIds }
        val shuffled = words.shuffled(Random(System.currentTimeMillis()))

        return buildList {
            addUnique(frequentMissed)
            addUnique(newestWords)
            addUnique(unseenWords)
            addUnique(shuffled)
        }.take(AI_DECK_SIZE)
    }

    private fun MutableList<WordEntity>.addUnique(words: List<WordEntity>) {
        words.forEach { word ->
            if (none { it.id == word.id } && size < AI_DECK_SIZE) {
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

    private fun loadSeedRecords(): List<SeedWordRecord> {
        val text = context.assets.open("jlpt_words.json").bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SeedWordRecord(
                        deck = item.getString("deck"),
                        readingJa = item.getString("readingJa"),
                        readingKo = item.getString("readingKo"),
                        partOfSpeech = item.getString("partOfSpeech"),
                        grammar = item.getString("grammar"),
                        kanji = item.getString("kanji"),
                        meaningJa = item.getString("meaningJa"),
                        meaningKo = item.getString("meaningKo"),
                        exampleJa = item.optString("exampleJa"),
                        exampleKo = item.optString("exampleKo"),
                        tag = item.getString("tag"),
                        note = item.getString("note"),
                    ),
                )
            }
        }
    }

    private fun parseWordIds(serialized: String): List<Long> =
        serialized.split(",").mapNotNull { it.toLongOrNull() }

    private fun wordSignature(
        readingJa: String,
        kanji: String,
        meaningKo: String,
        tag: String,
    ): String = listOf(readingJa, kanji, meaningKo, tag).joinToString("|")

    private data class DefaultDeck(
        val key: String,
        val name: String,
        val description: String,
        val tag: String,
    )

    private companion object {
        private const val AI_DECK_SIZE = 30
        private const val SEED_KEY = "seed_v2"
        private const val SEEDED_VALUE = "done"

        private val DEFAULT_DECKS = listOf(
            DefaultDeck("N5", "JLPT N5", "기초 일본어 단어장", "JLPT N5"),
            DefaultDeck("N4", "JLPT N4", "초급 일본어 단어장", "JLPT N4"),
            DefaultDeck("N3", "JLPT N3", "중급 입문 단어장", "JLPT N3"),
            DefaultDeck("N2", "JLPT N2", "중상급 일본어 단어장", "JLPT N2"),
        )
    }
}
