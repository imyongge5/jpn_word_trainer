package com.example.wordbookapp

import android.app.Application
import com.example.wordbookapp.data.AppContainer

class WordbookApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
