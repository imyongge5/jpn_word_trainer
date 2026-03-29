package com.mistbottle.jpnwordtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.mistbottle.jpnwordtrainer.data.model.ThemePreset
import com.mistbottle.jpnwordtrainer.ui.WordbookApp
import com.mistbottle.jpnwordtrainer.ui.theme.WordbookAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as WordbookApplication).container
        val repository = container.repository
        val syncRepository = container.syncRepository
        setContent {
            val savedThemePreset by repository.observeThemePreset()
                .collectAsStateWithLifecycle(initialValue = ThemePreset.DEFAULT_LIGHT)
            var previewThemePreset by remember { mutableStateOf<ThemePreset?>(null) }
            val scope = rememberCoroutineScope()

            WordbookAppTheme(themePreset = previewThemePreset ?: savedThemePreset) {
                WordbookApp(
                    repository = repository,
                    syncRepository = syncRepository,
                    currentThemePreset = savedThemePreset,
                    onPreviewTheme = { previewThemePreset = it },
                    onCancelThemePreview = { previewThemePreset = null },
                    onApplyTheme = { preset ->
                        scope.launch {
                            repository.saveThemePreset(preset)
                            previewThemePreset = null
                        }
                    },
                )
            }
        }
    }
}
