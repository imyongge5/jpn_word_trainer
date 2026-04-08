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
    private val preferredEnginePackage = "com.google.android.tts"
    private var textToSpeech: TextToSpeech? = null
    var isAvailable by mutableStateOf(false)
        private set

    fun bind() {
        if (textToSpeech != null) return
        val enginePackage = context.packageManager
            .queryIntentServices(android.content.Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .firstOrNull { it.serviceInfo.packageName == preferredEnginePackage }
            ?.serviceInfo
            ?.packageName

        lateinit var tts: TextToSpeech
        val listener: (Int) -> Unit = { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeResult = tts.setLanguage(Locale.JAPAN).takeIf {
                    it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED
                } ?: tts.setLanguage(Locale.JAPANESE)

                isAvailable = localeResult != TextToSpeech.LANG_MISSING_DATA &&
                    localeResult != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                isAvailable = false
            }
        }

        tts = if (enginePackage != null) {
            TextToSpeech(context, listener, enginePackage)
        } else {
            TextToSpeech(context, listener)
        }
        textToSpeech = tts
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
