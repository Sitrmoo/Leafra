package com.example.diarybook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.diarybook.ui.home.HomeScreen
import com.example.diarybook.ui.theme.DiaryBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaryBookTheme {
                HomeScreen()
            }
        }
    }
}
