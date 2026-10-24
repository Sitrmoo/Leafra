package leafra.sitrmoo.com.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import leafra.sitrmoo.com.R

/**
 * 首页固定顶栏：Logo、搜索占位、菜单。
 * 搜索在本阶段没有逻辑，点击只给出明确的“开发中”提示。
 */
@Composable
fun ShelfTopBar(
    onSearchClick: () -> Unit,
    onNewDiary: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_mark),
                    contentDescription = null,
                    modifier = Modifier.size(width = 24.dp, height = 32.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Leafora",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.width(12.dp))

                SearchPlaceholder(
                    modifier = Modifier.weight(1f),
                    onClick = onSearchClick,
                )

                Spacer(modifier = Modifier.width(2.dp))

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        MoreGlyph(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_new_diary)) },
                            onClick = {
                                menuExpanded = false
                                onNewDiary()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_favorites)) },
                            onClick = {
                                menuExpanded = false
                                onFavorites()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            onClick = {
                                menuExpanded = false
                                onSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            onClick = {
                                menuExpanded = false
                                onAbout()
                            },
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SearchPlaceholder(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(40.dp),
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchGlyph(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.6.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.30f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.62f, size.height * 0.62f),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MoreGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val radius = 1.7.dp.toPx()
        val centerX = size.width / 2f
        listOf(0.28f, 0.5f, 0.72f).forEach { fraction ->
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(centerX, size.height * fraction),
            )
        }
    }
}
