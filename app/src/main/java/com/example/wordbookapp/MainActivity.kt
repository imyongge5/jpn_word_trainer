package com.example.wordbookapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.wordbookapp.ui.WordbookApp
import com.example.wordbookapp.ui.theme.WordbookAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as WordbookApplication).container.repository
        setContent {
            WordbookAppTheme {
                WordbookApp(repository = repository)
            }
        }
    }
}
