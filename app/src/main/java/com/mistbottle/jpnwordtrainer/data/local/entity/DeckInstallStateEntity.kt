package com.mistbottle.jpnwordtrainer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deck_install_state",
    indices = [
        Index(value = ["deckId"], unique = true),
        Index(value = ["stableKey"], unique = true),
    ],
)
data class DeckInstallStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val stableKey: String,
    val currentVersionCode: Int,
    val latestKnownVersionCode: Int,
    val updateAvailable: Boolean,
    val isLegacyVersion: Boolean,
    val lastCheckedAt: Long,
)
