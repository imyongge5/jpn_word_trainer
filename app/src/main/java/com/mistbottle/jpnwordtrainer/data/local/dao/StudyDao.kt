package com.mistbottle.jpnwordtrainer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.mistbottle.jpnwordtrainer.data.local.entity.EndedTestResultEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestWordLogEntity

@Dao
interface StudyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestWordLog(log: TestWordLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestWordLogs(logs: List<TestWordLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndedTestResult(result: EndedTestResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndedTestResults(results: List<EndedTestResultEntity>)

    @Query("SELECT * FROM tests WHERE id = :testId LIMIT 1")
    suspend fun getTestById(testId: Long): TestEntity?

    @Query("SELECT * FROM ended_test_result WHERE testId = :testId LIMIT 1")
    suspend fun getEndedResultByTestId(testId: Long): EndedTestResultEntity?

    @Query(
        """
        SELECT * FROM tests
        WHERE deckId = :deckId AND status = 'IN_PROGRESS'
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getInProgressTestForDeck(deckId: Long): TestEntity?

    @Query(
        """
        SELECT * FROM tests
        WHERE isAiDeck = 1 AND status = 'IN_PROGRESS'
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getInProgressAiTest(): TestEntity?

    @Query("SELECT * FROM test_word_log WHERE testId = :testId ORDER BY sequenceIndex ASC")
    suspend fun getLogsForTest(testId: Long): List<TestWordLogEntity>

    @Query("SELECT * FROM test_word_log ORDER BY answeredAt DESC")
    suspend fun getAllTestWordLogs(): List<TestWordLogEntity>

    @Query(
        """
        SELECT twl.* FROM test_word_log twl
        INNER JOIN tests t ON t.id = twl.testId
        WHERE t.status != 'DELETED'
        ORDER BY twl.answeredAt DESC
        """,
    )
    suspend fun getAllActiveLogsNewestFirst(): List<TestWordLogEntity>

    @Query("SELECT COUNT(*) FROM test_word_log")
    fun observeTestWordLogCount(): Flow<Int>

    @Query("SELECT * FROM tests ORDER BY changedAt DESC, startedAt DESC")
    suspend fun getAllTests(): List<TestEntity>

    @Query("SELECT * FROM ended_test_result ORDER BY endedAt DESC")
    suspend fun getAllEndedTestResults(): List<EndedTestResultEntity>

    @Query(
        """
        SELECT * FROM tests
        WHERE deckId = :deckId
        ORDER BY changedAt DESC, startedAt DESC
        """,
    )
    suspend fun getTestsForDeck(deckId: Long): List<TestEntity>

    @Query(
        """
        SELECT * FROM test_word_log
        WHERE testId IN (:testIds)
        ORDER BY answeredAt DESC
        """,
    )
    suspend fun getLogsForTestIds(testIds: List<Long>): List<TestWordLogEntity>

    @Query(
        """
        SELECT etr.* FROM ended_test_result etr
        INNER JOIN tests t ON t.id = etr.testId
        WHERE t.status = 'COMPLETED'
        ORDER BY etr.endedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentCompletedResults(limit: Int): List<EndedTestResultEntity>

    @Query(
        """
        SELECT etr.* FROM ended_test_result etr
        INNER JOIN tests t ON t.id = etr.testId
        WHERE t.status = 'COMPLETED'
        ORDER BY etr.endedAt DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getCompletedResultPage(limit: Int, offset: Int): List<EndedTestResultEntity>

    @Query(
        """
        SELECT etr.* FROM ended_test_result etr
        INNER JOIN tests t ON t.id = etr.testId
        WHERE t.status = 'COMPLETED' AND etr.deckId = :deckId
        ORDER BY etr.endedAt DESC
        """,
    )
    suspend fun getCompletedResultsForDeck(deckId: Long): List<EndedTestResultEntity>

    @Query(
        """
        UPDATE tests
        SET status = :status,
            changedAt = :changedAt
        WHERE id = :testId
        """,
    )
    suspend fun updateTestStatus(
        testId: Long,
        status: String,
        changedAt: Long,
    )

    @Query(
        """
        UPDATE tests
        SET status = 'EXPIRED',
            changedAt = :changedAt
        WHERE status = 'IN_PROGRESS' AND startedAt < :expiresBefore
        """,
    )
    suspend fun expireStaleTests(
        expiresBefore: Long,
        changedAt: Long,
    )

    @Query("SELECT COUNT(*) FROM tests WHERE status = 'IN_PROGRESS'")
    suspend fun getInProgressTestCount(): Int

    @Query("DELETE FROM ended_test_result")
    suspend fun clearAllEndedTestResults()

    @Query("DELETE FROM test_word_log")
    suspend fun clearAllTestWordLogs()

    @Query("DELETE FROM tests")
    suspend fun clearAllTests()
}
