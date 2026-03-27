package com.mistbottle.jpnwordtrainer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.mistbottle.jpnwordtrainer.data.local.entity.StudyAnswerEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.StudySessionEntity

@Dao
interface StudyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: StudyAnswerEntity): Long

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query(
        """
        SELECT * FROM study_sessions
        WHERE deckId = :deckId AND completedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getInProgressSessionForDeck(deckId: Long): StudySessionEntity?

    @Query(
        """
        SELECT * FROM study_sessions
        WHERE isAiDeck = 1 AND completedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getInProgressAiSession(): StudySessionEntity?

    @Query("SELECT * FROM study_answers WHERE sessionId = :sessionId ORDER BY sequenceIndex ASC")
    suspend fun getAnswersForSession(sessionId: Long): List<StudyAnswerEntity>

    @Query("SELECT * FROM study_answers ORDER BY answeredAt DESC")
    suspend fun getAllAnswersNewestFirst(): List<StudyAnswerEntity>

    @Query("SELECT COUNT(*) FROM study_answers")
    fun observeAnswerCount(): Flow<Int>

    @Query("SELECT * FROM study_sessions ORDER BY lastAnsweredAt DESC, startedAt DESC")
    suspend fun getAllSessions(): List<StudySessionEntity>

    @Query(
        """
        SELECT * FROM study_sessions
        WHERE deckId = :deckId
        ORDER BY lastAnsweredAt DESC, startedAt DESC
        """,
    )
    suspend fun getSessionsForDeck(deckId: Long): List<StudySessionEntity>

    @Query(
        """
        SELECT * FROM study_answers
        WHERE sessionId IN (:sessionIds)
        ORDER BY answeredAt DESC
        """,
    )
    suspend fun getAnswersForSessionIds(sessionIds: List<Long>): List<StudyAnswerEntity>

    @Query(
        """
        SELECT * FROM study_sessions
        WHERE answeredCount > 0
        ORDER BY lastAnsweredAt DESC, startedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentAnsweredSessions(limit: Int): List<StudySessionEntity>

    @Query(
        """
        UPDATE study_sessions
        SET answeredCount = :answeredCount,
            correctCount = :correctCount,
            wrongCount = :wrongCount,
            lastAnsweredAt = :lastAnsweredAt,
            status = :status,
            completedAt = :completedAt
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionProgress(
        sessionId: Long,
        answeredCount: Int,
        correctCount: Int,
        wrongCount: Int,
        lastAnsweredAt: Long,
        status: String,
        completedAt: Long?,
    )
}
