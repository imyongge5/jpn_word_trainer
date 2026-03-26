package com.mistbottle.jpnwordtrainer

import android.app.Application
import com.mistbottle.jpnwordtrainer.data.AppContainer

class WordbookApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
