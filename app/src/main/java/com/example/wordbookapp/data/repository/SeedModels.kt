package com.example.wordbookapp.data.repository

data class SeedWordRecord(
    val deck: String,
    val readingJa: String,
    val readingKo: String,
    val partOfSpeech: String,
    val grammar: String,
    val kanji: String,
    val meaningJa: String,
    val meaningKo: String,
    val tag: String,
    val note: String,
)
