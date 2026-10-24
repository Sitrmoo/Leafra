package com.example.diarybook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DiaryBookColorScheme = lightColorScheme(
    primary = AccentBrown,
    onPrimary = PaperCream,
    primaryContainer = AccentSoft,
    onPrimaryContainer = InkBrown,
    secondary = InkMuted,
    onSecondary = PaperCream,
    background = PaperBackground,
    onBackground = InkBrown,
    surface = PaperCream,
    onSurface = InkBrown,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarm,
)

@Composable
fun DiaryBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaryBookColorScheme,
        typography = DiaryBookTypography,
        content = content,
    )
}
