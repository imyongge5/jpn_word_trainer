package com.mistbottle.jpnwordtrainer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mistbottle.jpnwordtrainer.data.local.dao.AppSettingDao
import com.mistbottle.jpnwordtrainer.data.local.dao.DeckDao
import com.mistbottle.jpnwordtrainer.data.local.dao.StudyDao
import com.mistbottle.jpnwordtrainer.data.local.dao.WordDao
import com.mistbottle.jpnwordtrainer.data.local.entity.AppSettingEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.DeckWordCrossRef
import com.mistbottle.jpnwordtrainer.data.local.entity.EndedTestResultEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.TestWordLogEntity
import com.mistbottle.jpnwordtrainer.data.local.entity.WordEntity

@Database(
    entities = [
        AppSettingEntity::class,
        DeckEntity::class,
        DeckWordCrossRef::class,
        EndedTestResultEntity::class,
        TestEntity::class,
        TestWordLogEntity::class,
        WordEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WordbookDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun deckDao(): DeckDao
    abstract fun studyDao(): StudyDao
    abstract fun appSettingDao(): AppSettingDao
}
