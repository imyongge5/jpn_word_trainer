package com.example.wordbookapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wordbookapp.data.local.dao.AppSettingDao
import com.example.wordbookapp.data.local.dao.DeckDao
import com.example.wordbookapp.data.local.dao.StudyDao
import com.example.wordbookapp.data.local.dao.WordDao
import com.example.wordbookapp.data.local.entity.AppSettingEntity
import com.example.wordbookapp.data.local.entity.DeckEntity
import com.example.wordbookapp.data.local.entity.DeckWordCrossRef
import com.example.wordbookapp.data.local.entity.StudyAnswerEntity
import com.example.wordbookapp.data.local.entity.StudySessionEntity
import com.example.wordbookapp.data.local.entity.WordEntity

@Database(
    entities = [
        AppSettingEntity::class,
        DeckEntity::class,
        DeckWordCrossRef::class,
        StudyAnswerEntity::class,
        StudySessionEntity::class,
        WordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WordbookDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun deckDao(): DeckDao
    abstract fun studyDao(): StudyDao
    abstract fun appSettingDao(): AppSettingDao
}
