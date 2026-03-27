package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_word_log",
    foreignKeys = [
        ForeignKey(
            entity = TestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["testId", "sequenceIndex"]),
        Index(value = ["wordId", "answeredAt"]),
        Index(value = ["answeredAt"]),
    ],
)
data class TestWordLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val wordId: Long,
    val sequenceIndex: Int,
    val isCorrect: Boolean,
    val answeredAt: Long,
)
