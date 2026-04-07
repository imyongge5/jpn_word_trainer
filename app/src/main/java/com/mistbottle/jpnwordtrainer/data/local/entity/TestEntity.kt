package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mistbottle.jpnwordtrainer.data.model.TestStatus
import com.mistbottle.jpnwordtrainer.data.model.WordField
import com.mistbottle.jpnwordtrainer.data.model.WordOrder

@Entity(
    tableName = "tests",
    indices = [
        Index(value = ["status", "changedAt"]),
        Index(value = ["deckId", "status", "changedAt"]),
    ],
)
data class TestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: TestStatus,
    val deckId: Long?,
    val deckNameSnapshot: String,
    val sourceDeckStableKey: String? = null,
    val sourceDeckVersionCode: Int? = null,
    val isAiDeck: Boolean,
    val onlyUnseenWords: Boolean,
    val wordOrder: WordOrder,
    val frontField: WordField,
    val revealField: WordField,
    val wordIdsSerialized: String,
    val totalWordCount: Int,
    val startedAt: Long,
    val changedAt: Long,
)
