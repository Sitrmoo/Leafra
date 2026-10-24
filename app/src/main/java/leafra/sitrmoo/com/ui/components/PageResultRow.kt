package leafra.sitrmoo.com.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.PageResult
import leafra.sitrmoo.com.ui.notebook.formatEntryDate
import leafra.sitrmoo.com.ui.settings.LocalShowPageDates

/** 搜索结果与收藏列表共用的横向页面条目。 */
@Composable
fun PageResultRow(
    result: PageResult,
    repository: DiaryRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteToggle: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val showPageDates = LocalShowPageDates.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageImage(
                imagePath = result.imagePath,
                isBlank = result.isBlank,
                repository = repository,
                modifier = Modifier
                    .width(54.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                maxSizePx = 300,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.notebookName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.page_number, result.pageNumber),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val date = result.entryDate
                    if (showPageDates && date != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatEntryDate(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val description = result.description
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showFavoriteToggle && onToggleFavorite != null) {
                FavoriteStar(
                    selected = result.isFavorite,
                    enabled = true,
                    onClick = onToggleFavorite,
                    modifier = Modifier.widthIn(max = 44.dp),
                )
            }
        }
    }
}
