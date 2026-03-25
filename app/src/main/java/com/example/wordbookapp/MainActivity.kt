package com.example.wordbookapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.wordbookapp.data.model.ThemePreset
import com.example.wordbookapp.ui.WordbookApp
import com.example.wordbookapp.ui.theme.WordbookAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as WordbookApplication).container.repository
        setContent {
            val savedThemePreset by repository.observeThemePreset()
                .collectAsStateWithLifecycle(initialValue = ThemePreset.DEFAULT_LIGHT)
            var previewThemePreset by remember { mutableStateOf<ThemePreset?>(null) }
            val scope = rememberCoroutineScope()

            WordbookAppTheme(themePreset = previewThemePreset ?: savedThemePreset) {
                WordbookApp(
                    repository = repository,
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
