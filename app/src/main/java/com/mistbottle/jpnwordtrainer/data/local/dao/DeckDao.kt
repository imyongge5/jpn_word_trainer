package com.mistbottle.jpnwordtrainer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import kotlinx.coroutines.flow.Flow
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckWordCrossRef
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity
import com.mistbottle.jpnwordtrainer.data.model.DeckType
import com.mistbottle.jpnwordtrainer.data.model.DeckWithCount

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckWordCrossRef(crossRef: DeckWordCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckWordCrossRefs(crossRefs: List<DeckWordCrossRef>)

    @Query(
        """
        SELECT d.id, d.name, d.description, d.type, d.sourceTag, d.displayOrder, d.createdAt,
               COUNT(dw.wordId) AS wordCount
        FROM decks d
        LEFT JOIN deck_word_cross_ref dw ON d.id = dw.deckId
        GROUP BY d.id
        ORDER BY
            CASE d.type WHEN 'JLPT' THEN 0 ELSE 1 END,
            d.displayOrder ASC,
            d.createdAt DESC
        """,
    )
    fun observeDecks(): Flow<List<DeckWithCount>>

    @Query(
        """
        SELECT d.id, d.name, d.description, d.type, d.sourceTag, d.displayOrder, d.createdAt,
               COUNT(dw.wordId) AS wordCount
        FROM decks d
        LEFT JOIN deck_word_cross_ref dw ON d.id = dw.deckId
        GROUP BY d.id
        ORDER BY
            CASE d.type WHEN 'JLPT' THEN 0 ELSE 1 END,
            d.displayOrder ASC,
            d.createdAt DESC
        """,
    )
    suspend fun getDecks(): List<DeckWithCount>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE sourceTag = :sourceTag LIMIT 1")
    suspend fun getDeckBySourceTag(sourceTag: String): DeckEntity?

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int

    @Query(
        """
        SELECT d.id, d.name, d.description, d.type, d.sourceTag, d.displayOrder, d.createdAt,
               COUNT(all_dw.wordId) AS wordCount
        FROM decks d
        INNER JOIN deck_word_cross_ref target_dw ON d.id = target_dw.deckId
        LEFT JOIN deck_word_cross_ref all_dw ON d.id = all_dw.deckId
        WHERE target_dw.wordId = :wordId
        GROUP BY d.id
        ORDER BY
            CASE d.type WHEN 'JLPT' THEN 0 ELSE 1 END,
            d.displayOrder ASC,
            d.createdAt DESC
        """,
    )
    suspend fun getDecksForWord(wordId: Long): List<DeckWithCount>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT * FROM words w
        INNER JOIN deck_word_cross_ref dw ON w.id = dw.wordId
        WHERE dw.deckId = :deckId
        ORDER BY dw.displayOrder ASC, dw.addedAt ASC, w.id ASC
        """,
    )
    fun observeWordsForDeck(deckId: Long): Flow<List<WordEntity>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT * FROM words w
        INNER JOIN deck_word_cross_ref dw ON w.id = dw.wordId
        WHERE dw.deckId = :deckId
        ORDER BY dw.displayOrder ASC, dw.addedAt ASC, w.id ASC
        """,
    )
    suspend fun getWordsForDeck(deckId: Long): List<WordEntity>

    @Query("SELECT * FROM deck_word_cross_ref")
    suspend fun getAllDeckWordCrossRefs(): List<DeckWordCrossRef>

    @Query("DELETE FROM deck_word_cross_ref")
    suspend fun clearAllDeckWordCrossRefs()

    @Query("DELETE FROM decks")
    suspend fun clearAllDecks()
}
