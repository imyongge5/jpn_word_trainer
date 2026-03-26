package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mistbottle.jpnwordtrainer.data.model.DeckType

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val type: DeckType,
    val sourceTag: String,
    val displayOrder: Int,
    val createdAt: Long,
)
