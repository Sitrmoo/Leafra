package leafra.sitrmoo.com.ui.notebook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.ui.components.FavoriteStar
import leafra.sitrmoo.com.ui.components.PageImage

private val PAGE_ROW_HEIGHT = 96.dp
private val PAGE_ROW_SPACING = 8.dp

/**
 * 大纲视图：按当前顺序展示所有页面缩略图。
 * 编辑模式下可拖动左侧手柄调整顺序；拖动结束后一次性持久化 1..n 的 sortOrder。
 */
@Composable
fun OutlineView(
    notebookId: String,
    pages: List<DiaryPage>,
    repository: DiaryRepository,
    isEditing: Boolean,
    isBusy: Boolean,
    onEditPage: (DiaryPage) -> Unit,
    onToggleFavorite: (DiaryPage) -> Unit,
    onAddImages: () -> Unit,
    onAddBlankPage: () -> Unit,
    onReorder: suspend (List<String>) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) {
        (PAGE_ROW_HEIGHT + PAGE_ROW_SPACING).toPx()
    }

    var orderedPages by remember { mutableStateOf(pages) }
    var dragPageId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isSavingOrder by remember { mutableStateOf(false) }

    LaunchedEffect(pages, dragPageId, isSavingOrder) {
        if (dragPageId == null && !isSavingOrder) {
            orderedPages = pages
        }
    }

    fun finishDrag() {
        val orderedIds = orderedPages.map { it.id }
        val originalIds = pages.map { it.id }
        dragPageId = null
        dragOffset = 0f

        if (orderedIds != originalIds) {
            isSavingOrder = true
            scope.launch {
                val saved = onReorder(orderedIds)
                isSavingOrder = false
                if (!saved) {
                    orderedPages = pages
                }
            }
        }
    }

    fun dragModifier(page: DiaryPage, longPress: Boolean): Modifier =
        Modifier.pointerInput(page.id, isEditing, longPress) {
            if (!isEditing) return@pointerInput

            fun applyDrag(deltaY: Float) {
                dragOffset += deltaY
                val currentIndex = orderedPages.indexOfFirst { it.id == page.id }
                if (currentIndex < 0) return

                if (dragOffset > stepPx / 2 && currentIndex < orderedPages.lastIndex) {
                    orderedPages = orderedPages.toMutableList().apply {
                        add(currentIndex + 1, removeAt(currentIndex))
                    }
                    dragOffset -= stepPx
                } else if (dragOffset < -stepPx / 2 && currentIndex > 0) {
                    orderedPages = orderedPages.toMutableList().apply {
                        add(currentIndex - 1, removeAt(currentIndex))
                    }
                    dragOffset += stepPx
                }
            }

            val onDragStart: (Offset) -> Unit = {
                dragPageId = page.id
                dragOffset = 0f
            }
            val onDrag: (PointerInputChange, Offset) -> Unit = { change, amount ->
                change.consume()
                applyDrag(amount.y)
            }
            val onDragEnd: () -> Unit = { finishDrag() }

            if (longPress) {
                detectDragGesturesAfterLongPress(
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            } else {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(PAGE_ROW_SPACING),
    ) {
        if (isEditing && orderedPages.size > 1) {
            item(key = "drag-hint") {
                Text(
                    text = stringResource(R.string.outline_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                )
            }
        }

        if (orderedPages.isEmpty()) {
            item(key = "empty") {
                EmptyOutlineHint(isEditing = isEditing)
            }
        }

        itemsIndexed(
            items = orderedPages,
            key = { _, page -> page.id },
        ) { index, page ->
            val isDragged = dragPageId == page.id

            val dragHandleModifier = dragModifier(page, longPress = false)
            val thumbnailDragModifier = dragModifier(page, longPress = true)

            OutlineRow(
                page = page,
                pageNumber = index + 1,
                repository = repository,
                isEditing = isEditing,
                isDragged = isDragged,
                onEdit = { onEditPage(page) },
                onToggleFavorite = { onToggleFavorite(page) },
                dragHandleModifier = dragHandleModifier,
                thumbnailDragModifier = thumbnailDragModifier,
            )
        }

        item(key = "add") {
            if (isEditing) {
                AddPageEntry(
                    enabled = !isBusy && dragPageId == null,
                    onAddImages = onAddImages,
                    onAddBlankPage = onAddBlankPage,
                )
            } else {
                Text(
                    text = stringResource(R.string.outline_read_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun OutlineRow(
    page: DiaryPage,
    pageNumber: Int,
    repository: DiaryRepository,
    isEditing: Boolean,
    isDragged: Boolean,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    dragHandleModifier: Modifier,
    thumbnailDragModifier: Modifier,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PAGE_ROW_HEIGHT)
            .zIndex(if (isDragged) 1f else 0f),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = if (isDragged) 0.85f else 0.35f,
            ),
        ),
        shadowElevation = if (isDragged) 4.dp else 0.dp,
        onClick = onEdit,
        enabled = isEditing,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditing) {
                Box(
                    modifier = dragHandleModifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DragHandle()
                }
                Spacer(modifier = Modifier.width(2.dp))
            }

            PageImage(
                imagePath = page.imagePath,
                isBlank = page.isBlank,
                repository = repository,
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .then(thumbnailDragModifier),
                maxSizePx = 300,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.page_number, pageNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val description = page.description
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val entryDate = page.entryDate
                if (entryDate != null || page.isFavorite) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entryDate != null) {
                            Text(
                                text = formatEntryDate(entryDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (page.isFavorite) {
                            if (entryDate != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.page_favorited),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }

            FavoriteStar(
                selected = page.isFavorite,
                enabled = isEditing,
                onClick = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun DragHandle() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(20.dp)) {
        val lineWidth = size.width * 0.55f
        val startX = (size.width - lineWidth) / 2f
        listOf(-5f, 0f, 5f).forEach { offsetY ->
            drawLine(
                color = color,
                start = Offset(startX, size.height / 2f + offsetY),
                end = Offset(startX + lineWidth, size.height / 2f + offsetY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun EmptyOutlineHint(isEditing: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_notebook_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (isEditing) {
                    R.string.outline_empty_edit_hint
                } else {
                    R.string.outline_empty_read_hint
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddPageEntry(
    enabled: Boolean,
    onAddImages: () -> Unit,
    onAddBlankPage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.outline_add_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.outline_add_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onAddImages,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.outline_add_images))
                }
                OutlinedButton(
                    onClick = onAddBlankPage,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.outline_add_blank))
                }
            }
        }
    }
}
