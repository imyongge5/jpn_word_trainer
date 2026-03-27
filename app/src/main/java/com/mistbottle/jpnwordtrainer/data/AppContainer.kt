package com.mistbottle.jpnwordtrainer.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mistbottle.jpnwordtrainer.data.local.WordbookDatabase
import com.mistbottle.jpnwordtrainer.data.repository.WordbookRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        WordbookDatabase::class.java,
        "wordbook.db",
    )
        .createFromAsset("databases/wordbook.db")
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .build()

    val repository: WordbookRepository = WordbookRepository(database = database)

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
    }
}
