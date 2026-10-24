package leafra.sitrmoo.com.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import leafra.sitrmoo.com.R

/** 收藏星标；阅读模式下只展示状态，编辑模式才可点击切换。 */
@Composable
fun FavoriteStar(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = stringResource(
        if (selected) R.string.page_unfavorite else R.string.page_favorite,
    )

    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = label },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = if (selected) "★" else "☆",
            fontSize = 20.sp,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
        )
    }
}
