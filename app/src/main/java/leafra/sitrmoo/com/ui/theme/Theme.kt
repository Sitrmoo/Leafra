package leafra.sitrmoo.com.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LeafraColorScheme = lightColorScheme(
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
fun LeafraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeafraColorScheme,
        typography = LeafraTypography,
        content = content,
    )
}
