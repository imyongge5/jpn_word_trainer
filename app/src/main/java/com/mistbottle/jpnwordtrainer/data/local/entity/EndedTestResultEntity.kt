package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ended_test_result",
    foreignKeys = [
        ForeignKey(
            entity = TestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["testId"], unique = true),
        Index(value = ["endedAt"]),
        Index(value = ["deckId", "endedAt"]),
    ],
)
data class EndedTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val deckId: Long?,
    val deckNameSnapshot: String,
    val isAiDeck: Boolean,
    val totalWordCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
)
