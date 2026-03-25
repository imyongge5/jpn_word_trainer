package com.example.wordbookapp.data

import android.content.Context
import androidx.room.Room
import com.example.wordbookapp.data.local.WordbookDatabase
import com.example.wordbookapp.data.repository.WordbookRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        WordbookDatabase::class.java,
        "wordbook.db",
    ).build()

    val repository: WordbookRepository = WordbookRepository(
        context = context,
        database = database,
    )
}
