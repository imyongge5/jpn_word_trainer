package com.mistbottle.jpnwordtrainer.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Stable
class JapaneseTtsController internal constructor(
    private val context: Context,
) {
    private var textToSpeech: TextToSpeech? = null
    var isAvailable by mutableStateOf(false)
        private set

    fun bind() {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                isAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                isAvailable = false
            }
        }
    }

    fun speak(text: String) {
        val message = text.trim()
        if (!isAvailable || message.isBlank()) return
        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jp-tts-${message.hashCode()}",
        )
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isAvailable = false
    }
}

@Composable
fun rememberJapaneseTtsController(): JapaneseTtsController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { JapaneseTtsController(context) }

    DisposableEffect(controller) {
        controller.bind()
        onDispose {
            controller.shutdown()
        }
    }

    return controller
}
