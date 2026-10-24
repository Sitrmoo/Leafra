package leafra.sitrmoo.com.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val LeafraDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9BDA0),
    onPrimary = Color(0xFF23301F),
    primaryContainer = Color(0xFF3A4634),
    onPrimaryContainer = Color(0xFFE5EEDD),
    inversePrimary = SageDeep,
    secondary = Color(0xFFC9C1B2),
    onSecondary = Color(0xFF2A2720),
    secondaryContainer = Color(0xFF3D382F),
    onSecondaryContainer = Color(0xFFEDE6D8),
    tertiary = Color(0xFFD3A987),
    onTertiary = Color(0xFF3A2415),
    tertiaryContainer = Color(0xFF4A3525),
    onTertiaryContainer = Color(0xFFF6DFC8),
    background = Color(0xFF1B1F1A),
    onBackground = Color(0xFFEDE6D8),
    surface = Color(0xFF232821),
    onSurface = Color(0xFFEDE6D8),
    surfaceVariant = Color(0xFF3A4037),
    onSurfaceVariant = Color(0xFFC6C0B2),
    surfaceTint = Color(0xFFA9BDA0),
    inverseSurface = Color(0xFFEDE6D8),
    inverseOnSurface = Color(0xFF2A3028),
    error = Color(0xFFFFB4A3),
    onError = Color(0xFF561F14),
    errorContainer = Color(0xFF733426),
    onErrorContainer = Color(0xFFFFDAD1),
    outline = Color(0xFF918A7C),
    outlineVariant = Color(0xFF464C42),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF32382F),
    surfaceDim = Color(0xFF1B1F1A),
    surfaceContainerLowest = Color(0xFF161A15),
    surfaceContainerLow = Color(0xFF1F241D),
    surfaceContainer = Color(0xFF232821),
    surfaceContainerHigh = Color(0xFF2C322A),
    surfaceContainerHighest = Color(0xFF373D34),
)

@Composable
fun LeafraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LeafraDarkColorScheme else LeafraColorScheme,
        typography = LeafraTypography,
        content = content,
    )
}
