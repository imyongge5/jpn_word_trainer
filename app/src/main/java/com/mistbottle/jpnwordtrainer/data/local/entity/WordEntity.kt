package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val readingJa: String,
    val readingKo: String,
    val partOfSpeech: String,
    val grammar: String,
    val kanji: String,
    val meaningJa: String,
    val meaningKo: String,
    val exampleJa: String,
    val exampleKo: String,
    val tag: String,
    val note: String,
    val isKanaOnly: Boolean = false,
    val createdAt: Long,
)
