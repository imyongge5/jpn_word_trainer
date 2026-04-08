package com.mistbottle.jpnwordtrainer.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mistbottle.jpnwordtrainer.data.local.WordbookDatabase
import com.mistbottle.jpnwordtrainer.data.repository.WordbookRepository
import com.mistbottle.jpnwordtrainer.data.repository.SyncRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        WordbookDatabase::class.java,
        "wordbook.db",
    )
        .createFromAsset("databases/wordbook.db")
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .addMigrations(MIGRATION_6_7)
        .addMigrations(MIGRATION_7_8)
        .addMigrations(MIGRATION_8_9)
        .build()

    val repository: WordbookRepository = WordbookRepository(database = database)
    val syncRepository: SyncRepository = SyncRepository(database = database)

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN exampleJa TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN exampleKo TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN answeredCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN lastAnsweredAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'IN_PROGRESS'")
                db.execSQL("UPDATE study_sessions SET answeredCount = correctCount + wrongCount")
                db.execSQL("UPDATE study_sessions SET lastAnsweredAt = COALESCE(completedAt, startedAt)")
                db.execSQL(
                    """
                    UPDATE study_sessions
                    SET status = CASE
                        WHEN completedAt IS NOT NULL THEN 'COMPLETED'
                        ELSE 'IN_PROGRESS'
                    END
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                val expiryCutoff = now - 24L * 60L * 60L * 1000L

                db.execSQL("ALTER TABLE words ADD COLUMN isKanaOnly INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        status TEXT NOT NULL,
                        deckId INTEGER,
                        deckNameSnapshot TEXT NOT NULL DEFAULT '',
                        isAiDeck INTEGER NOT NULL,
                        wordOrder TEXT NOT NULL,
                        frontField TEXT NOT NULL,
                        revealField TEXT NOT NULL,
                        wordIdsSerialized TEXT NOT NULL,
                        totalWordCount INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        changedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tests_status_changedAt ON tests(status, changedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tests_deckId_status_changedAt ON tests(deckId, status, changedAt)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS test_word_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        testId INTEGER NOT NULL,
                        wordId INTEGER NOT NULL,
                        sequenceIndex INTEGER NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        answeredAt INTEGER NOT NULL,
                        FOREIGN KEY(testId) REFERENCES tests(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_test_word_log_testId_sequenceIndex ON test_word_log(testId, sequenceIndex)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_test_word_log_wordId_answeredAt ON test_word_log(wordId, answeredAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_test_word_log_answeredAt ON test_word_log(answeredAt)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ended_test_result (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        testId INTEGER NOT NULL,
                        deckId INTEGER,
                        deckNameSnapshot TEXT NOT NULL DEFAULT '',
                        isAiDeck INTEGER NOT NULL,
                        totalWordCount INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        wrongCount INTEGER NOT NULL,
                        accuracyPercent INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        FOREIGN KEY(testId) REFERENCES tests(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ended_test_result_testId ON ended_test_result(testId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ended_test_result_endedAt ON ended_test_result(endedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ended_test_result_deckId_endedAt ON ended_test_result(deckId, endedAt)")

                db.execSQL(
                    """
                    INSERT INTO tests (
                        id, status, deckId, deckNameSnapshot, isAiDeck, wordOrder, frontField,
                        revealField, wordIdsSerialized, totalWordCount, startedAt, changedAt
                    )
                    SELECT
                        id,
                        CASE
                            WHEN completedAt IS NOT NULL THEN 'COMPLETED'
                            WHEN startedAt < $expiryCutoff THEN 'EXPIRED'
                            ELSE 'IN_PROGRESS'
                        END,
                        deckId,
                        deckName,
                        isAiDeck,
                        wordOrder,
                        frontField,
                        revealField,
                        wordIdsSerialized,
                        totalCount,
                        startedAt,
                        CASE
                            WHEN completedAt IS NOT NULL THEN completedAt
                            ELSE COALESCE(lastAnsweredAt, startedAt)
                        END
                    FROM study_sessions
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO test_word_log (id, testId, wordId, sequenceIndex, isCorrect, answeredAt)
                    SELECT id, sessionId, wordId, sequenceIndex, isCorrect, answeredAt
                    FROM study_answers
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO ended_test_result (
                        id, testId, deckId, deckNameSnapshot, isAiDeck, totalWordCount,
                        correctCount, wrongCount, accuracyPercent, startedAt, endedAt, durationSeconds
                    )
                    SELECT
                        id,
                        id,
                        deckId,
                        deckName,
                        isAiDeck,
                        totalCount,
                        correctCount,
                        wrongCount,
                        CASE
                            WHEN totalCount = 0 THEN 0
                            ELSE (correctCount * 100) / totalCount
                        END,
                        startedAt,
                        completedAt,
                        CASE
                            WHEN completedAt IS NULL THEN 0
                            ELSE (completedAt - startedAt) / 1000
                        END
                    FROM study_sessions
                    WHERE completedAt IS NOT NULL
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op: key-value app_settings 확장을 위한 버전 상승만 처리합니다.
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tests ADD COLUMN onlyUnseenWords INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE decks ADD COLUMN stableKey TEXT")
                db.execSQL("ALTER TABLE decks ADD COLUMN deckVersionCode INTEGER")
                db.execSQL("ALTER TABLE decks ADD COLUMN isBuiltin INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE decks SET stableKey = sourceTag, deckVersionCode = 1, isBuiltin = 1 WHERE type = 'JLPT'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deck_install_state (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        deckId INTEGER NOT NULL,
                        stableKey TEXT NOT NULL,
                        currentVersionCode INTEGER NOT NULL,
                        latestKnownVersionCode INTEGER NOT NULL,
                        updateAvailable INTEGER NOT NULL,
                        isLegacyVersion INTEGER NOT NULL,
                        lastCheckedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_deck_install_state_deckId ON deck_install_state(deckId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_deck_install_state_stableKey ON deck_install_state(stableKey)")
                db.execSQL(
                    """
                    INSERT INTO deck_install_state (
                        deckId, stableKey, currentVersionCode, latestKnownVersionCode,
                        updateAvailable, isLegacyVersion, lastCheckedAt
                    )
                    SELECT id, stableKey, 1, 1, 0, 1, $now
                    FROM decks
                    WHERE isBuiltin = 1 AND stableKey IS NOT NULL
                    """.trimIndent(),
                )

                db.execSQL("ALTER TABLE tests ADD COLUMN sourceDeckStableKey TEXT")
                db.execSQL("ALTER TABLE tests ADD COLUMN sourceDeckVersionCode INTEGER")
                db.execSQL(
                    """
                    UPDATE tests
                    SET sourceDeckStableKey = (
                        SELECT stableKey FROM decks WHERE decks.id = tests.deckId
                    ),
                    sourceDeckVersionCode = (
                        SELECT deckVersionCode FROM decks WHERE decks.id = tests.deckId
                    )
                    WHERE deckId IS NOT NULL
                    """.trimIndent(),
                )

                db.execSQL("ALTER TABLE ended_test_result ADD COLUMN sourceDeckStableKey TEXT")
                db.execSQL("ALTER TABLE ended_test_result ADD COLUMN sourceDeckVersionCode INTEGER")
                db.execSQL(
                    """
                    UPDATE ended_test_result
                    SET sourceDeckStableKey = (
                        SELECT sourceDeckStableKey FROM tests WHERE tests.id = ended_test_result.testId
                    ),
                    sourceDeckVersionCode = (
                        SELECT sourceDeckVersionCode FROM tests WHERE tests.id = ended_test_result.testId
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tests ADD COLUMN revealFieldsSerialized TEXT NOT NULL DEFAULT 'READING_JA'")
                db.execSQL("ALTER TABLE tests ADD COLUMN excludeKanaOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tests ADD COLUMN wrongOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE tests SET revealFieldsSerialized = revealField")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tests_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        status TEXT NOT NULL,
                        deckId INTEGER,
                        deckNameSnapshot TEXT NOT NULL,
                        sourceDeckStableKey TEXT,
                        sourceDeckVersionCode INTEGER,
                        isAiDeck INTEGER NOT NULL,
                        onlyUnseenWords INTEGER NOT NULL,
                        excludeKanaOnly INTEGER NOT NULL,
                        wrongOnly INTEGER NOT NULL,
                        wordOrder TEXT NOT NULL,
                        frontField TEXT NOT NULL,
                        revealFieldsSerialized TEXT NOT NULL,
                        wordIdsSerialized TEXT NOT NULL,
                        totalWordCount INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        changedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO tests_new (
                        id, status, deckId, deckNameSnapshot, sourceDeckStableKey,
                        sourceDeckVersionCode, isAiDeck, onlyUnseenWords, excludeKanaOnly,
                        wrongOnly, wordOrder, frontField, revealFieldsSerialized,
                        wordIdsSerialized, totalWordCount, startedAt, changedAt
                    )
                    SELECT
                        id,
                        status,
                        deckId,
                        deckNameSnapshot,
                        sourceDeckStableKey,
                        sourceDeckVersionCode,
                        isAiDeck,
                        onlyUnseenWords,
                        excludeKanaOnly,
                        wrongOnly,
                        wordOrder,
                        frontField,
                        revealFieldsSerialized,
                        wordIdsSerialized,
                        totalWordCount,
                        startedAt,
                        changedAt
                    FROM tests
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE tests")
                db.execSQL("ALTER TABLE tests_new RENAME TO tests")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tests_status_changedAt ON tests(status, changedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tests_deckId_status_changedAt ON tests(deckId, status, changedAt)")
            }
        }
    }
}
