package com.example.wordbookapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wordbookapp.data.local.entity.StudyAnswerEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity

@Dao
interface StudyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: StudyAnswerEntity): Long

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query("SELECT * FROM study_answers WHERE sessionId = :sessionId ORDER BY sequenceIndex ASC")
    suspend fun getAnswersForSession(sessionId: Long): List<StudyAnswerEntity>

    @Query("SELECT * FROM study_answers ORDER BY answeredAt DESC")
    suspend fun getAllAnswersNewestFirst(): List<StudyAnswerEntity>

    @Query(
        """
        SELECT * FROM study_sessions
        WHERE deckId = :deckId AND completedAt IS NOT NULL
        ORDER BY completedAt DESC
        """,
    )
    suspend fun getCompletedSessionsForDeck(deckId: Long): List<StudySessionEntity>

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
        WHERE completedAt IS NOT NULL
        ORDER BY completedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentCompletedSessions(limit: Int): List<StudySessionEntity>

    @Query(
        """
        UPDATE study_sessions
        SET totalCount = :totalCount,
            correctCount = :correctCount,
            wrongCount = :wrongCount,
            completedAt = :completedAt
        WHERE id = :sessionId
        """,
    )
    suspend fun completeSession(
        sessionId: Long,
        totalCount: Int,
        correctCount: Int,
        wrongCount: Int,
        completedAt: Long,
    )
}
