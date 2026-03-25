package com.example.wordbookapp.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wordbookapp.data.local.WordbookDatabase
import com.example.wordbookapp.data.repository.WordbookRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        WordbookDatabase::class.java,
        "wordbook.db",
    )
        .createFromAsset("databases/wordbook.db")
        .addMigrations(MIGRATION_1_2)
        .build()

    val repository: WordbookRepository = WordbookRepository(
        context = context,
        database = database,
    )

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN exampleJa TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE words ADD COLUMN exampleKo TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
