package com.example.wordbookapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wordbookapp.data.model.WordField
import com.example.wordbookapp.data.model.WordOrder

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long?,
    val deckName: String,
    val isAiDeck: Boolean,
    val wordOrder: WordOrder,
    val frontField: WordField,
    val revealField: WordField,
    val wordIdsSerialized: String,
    val totalCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val startedAt: Long,
    val completedAt: Long?,
)
