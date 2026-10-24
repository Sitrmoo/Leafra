package leafra.sitrmoo.com.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LeafraColorScheme = lightColorScheme(
    primary = SageDeep,
    onPrimary = PaperCream,
    primaryContainer = SageMist,
    onPrimaryContainer = SageInk,
    inversePrimary = SageGreen,
    secondary = InkMuted,
    onSecondary = PaperCream,
    secondaryContainer = AccentSoft,
    onSecondaryContainer = InkBrown,
    tertiary = AccentBrown,
    onTertiary = PaperCream,
    tertiaryContainer = AccentSoft,
    onTertiaryContainer = InkBrown,
    background = PaperBackground,
    onBackground = InkBrown,
    surface = PaperCream,
    onSurface = InkBrown,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkMuted,
    surfaceTint = SageDeep,
    inverseSurface = InkBrown,
    inverseOnSurface = CreamPaper,
    error = ErrorWarm,
    onError = PaperCream,
    errorContainer = Color(0xFFF3DAD3),
    onErrorContainer = Color(0xFF5A2A20),
    outline = OutlineWarm,
    outlineVariant = Color(0xFFDDD0BB),
    scrim = Color(0xFF000000),
    surfaceBright = PaperCream,
    surfaceDim = PaperDeep,
    surfaceContainerLowest = Color(0xFFFFFDF7),
    surfaceContainerLow = Color(0xFFFBF6EA),
    surfaceContainer = Color(0xFFF8F1E2),
    surfaceContainerHigh = Color(0xFFF2E9D8),
    surfaceContainerHighest = Color(0xFFECE1CE),
)

@Composable
fun LeafraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeafraColorScheme,
        typography = LeafraTypography,
        content = content,
    )
}
