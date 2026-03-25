package com.example.wordbookapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wordbookapp.ui.theme.WordbookAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordbookAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WordbookHome(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun WordbookHome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "단어장 앱",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Kotlin + Jetpack Compose 프로젝트가 준비됐어요.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("첫 단어 추가")
        }
    }
}
