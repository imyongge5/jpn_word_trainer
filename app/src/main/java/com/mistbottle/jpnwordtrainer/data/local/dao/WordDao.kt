package com.mistbottle.jpnwordtrainer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>): List<Long>

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Long): WordEntity?

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    suspend fun getAllWordsByNewest(): List<WordEntity>

    @Query(
        """
        SELECT * FROM words
        WHERE kanji LIKE '%' || :query || '%'
           OR readingJa LIKE '%' || :query || '%'
        ORDER BY LENGTH(kanji) DESC, LENGTH(readingJa) DESC, createdAt DESC
        """,
    )
    suspend fun findWordsForInlineMatch(query: String): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int
}
