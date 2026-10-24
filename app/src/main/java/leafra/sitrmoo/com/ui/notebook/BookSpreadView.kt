package leafra.sitrmoo.com.ui.notebook

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.data.model.Notebook
import leafra.sitrmoo.com.ui.components.BlankPageStyle
import leafra.sitrmoo.com.ui.components.NotebookCover
import leafra.sitrmoo.com.ui.components.PageImage
import leafra.sitrmoo.com.ui.theme.OutlineWarm
import leafra.sitrmoo.com.ui.theme.PaperCream
import leafra.sitrmoo.com.ui.theme.PaperDeep

private const val BOOK_ASPECT_RATIO = 1.5f

/** 一本打开的书：第 1 页在右侧，之后每两页组成一个跨页。 */
private data class BookSpread(
    val left: Int?,
    val right: Int?,
)

private fun buildSpreads(pageCount: Int): List<BookSpread> {
    if (pageCount <= 0) return emptyList()

    val spreads = mutableListOf(BookSpread(left = null, right = 0))
    var left = 1
    while (left < pageCount) {
        spreads += BookSpread(
            left = left,
            right = if (left + 1 < pageCount) left + 1 else null,
        )
        left += 2
    }
    return spreads
}

private fun spreadForPageIndex(index: Int): Int =
    if (index <= 0) 0 else (index + 1) / 2

/**
 * 轻量立体书阅读模式。
 *
 * 渲染技术：Compose 布局 + Canvas/渐变阴影 + `graphicsLayer` 的 rotationY/cameraDistance
 * 透视动画，不引入游戏引擎。翻页时把当前页作为一张“纸”绕书脊旋转 180°，
 * 背面显示纸张底色（不镜像下一张扫描图），这是本阶段的已知简化。
 */
@Composable
fun BookSpreadView(
    notebook: Notebook,
    pages: List<DiaryPage>,
    repository: DiaryRepository,
    isEditing: Boolean,
    initialPageId: String?,
    onOpenSinglePage: (DiaryPage) -> Unit,
    onEditPage: (DiaryPage) -> Unit,
    onPageChanged: suspend (DiaryPage) -> Unit,
) {
    if (pages.isEmpty()) return

    val spreads = remember(pages.size) { buildSpreads(pages.size) }
    if (spreads.isEmpty()) return

    val initialPageIndex = pages.indexOfFirst { it.id == initialPageId }
    val initialSpread = spreadForPageIndex(initialPageIndex)
        .coerceIn(0, spreads.lastIndex)

    var spreadIndex by rememberSaveable(notebook.id) {
        mutableIntStateOf(initialSpread)
    }
    var turnProgress by remember { mutableFloatStateOf(0f) }
    var turnDirection by remember { mutableIntStateOf(0) }
    var turnFromSpread by remember { mutableIntStateOf(0) }
    var isAnimating by remember { mutableStateOf(false) }
    var bookScale by remember { mutableFloatStateOf(1f) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    LaunchedEffect(spreads.size) {
        if (spreadIndex > spreads.lastIndex) {
            spreadIndex = spreads.lastIndex.coerceAtLeast(0)
        }
    }

    LaunchedEffect(spreadIndex, pages.size) {
        val spread = spreads.getOrNull(spreadIndex) ?: return@LaunchedEffect
        val pageIndex = spread.right ?: spread.left ?: return@LaunchedEffect
        pages.getOrNull(pageIndex)?.let { page -> onPageChanged(page) }
    }

    fun turnPage(direction: Int) {
        if (isAnimating) return
        val target = spreadIndex + direction
        if (target !in spreads.indices) return

        turnDirection = direction
        turnFromSpread = spreadIndex
        turnProgress = 0f
        isAnimating = true

        scope.launch {
            val animation = Animatable(0f)
            animation.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f),
                ),
            ) {
                turnProgress = value
                if (value >= 0.5f && spreadIndex != target) {
                    spreadIndex = target
                }
            }
            turnProgress = 0f
            turnDirection = 0
            isAnimating = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    val swipeThreshold = 72.dp.toPx()
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var dragTotalX = 0f
                        var dragTotalY = 0f
                        var zooming = false
                        var dragging = false
                        var dragFromSpread = spreadIndex
                        var dragDirection = 0

                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }

                            if (pressedCount >= 2) {
                                if (dragging) {
                                    // 第二根手指加入时取消单指翻页，恢复原跨页
                                    dragging = false
                                    turnProgress = 0f
                                    turnDirection = 0
                                    spreadIndex = dragFromSpread
                                }
                                zooming = true
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) {
                                    bookScale = (bookScale * zoomChange).coerceIn(0.8f, 1.5f)
                                    event.changes.forEach { change -> change.consume() }
                                }
                            } else if (!zooming) {
                                val delta = event.changes.firstOrNull()?.positionChange()
                                    ?: Offset.Zero
                                dragTotalX += delta.x
                                dragTotalY += delta.y

                                if (!dragging &&
                                    abs(dragTotalX) > viewConfiguration.touchSlop &&
                                    abs(dragTotalX) > abs(dragTotalY)
                                ) {
                                    val direction = if (dragTotalX < 0) 1 else -1
                                    if (spreadIndex + direction in spreads.indices) {
                                        dragging = true
                                        dragFromSpread = spreadIndex
                                        dragDirection = direction
                                        turnFromSpread = spreadIndex
                                        turnDirection = direction
                                        turnProgress = 0f
                                    }
                                }

                                if (dragging) {
                                    val distance = (size.width * 0.7f).coerceAtLeast(1f)
                                    turnProgress = (abs(dragTotalX) / distance)
                                        .coerceIn(0f, 1f)
                                    val target = dragFromSpread + dragDirection
                                    spreadIndex = if (turnProgress >= 0.5f) {
                                        target
                                    } else {
                                        dragFromSpread
                                    }
                                    event.changes.forEach { change -> change.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (dragging) {
                            val from = dragFromSpread
                            val target = from + dragDirection
                            val completed = turnProgress >= 0.35f
                            isAnimating = true
                            scope.launch {
                                val settle = Animatable(turnProgress)
                                settle.animateTo(
                                    targetValue = if (completed) 1f else 0f,
                                    animationSpec = tween(
                                        durationMillis = if (completed) 300 else 220,
                                        easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f),
                                    ),
                                ) {
                                    turnProgress = value
                                    if (completed && value >= 0.5f && spreadIndex != target) {
                                        spreadIndex = target
                                    }
                                }
                                spreadIndex = if (completed) target else from
                                turnProgress = 0f
                                turnDirection = 0
                                isAnimating = false
                            }
                        } else if (!zooming &&
                            abs(dragTotalX) > swipeThreshold &&
                            abs(dragTotalX) > abs(dragTotalY)
                        ) {
                            // 快速短促滑动没有进入拖拽状态时，也触发整页动画
                            if (dragTotalX < 0) turnPage(1) else turnPage(-1)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bookWidth = minOf(maxWidth * 0.96f, maxHeight * BOOK_ASPECT_RATIO)
                val bookHeight = bookWidth / BOOK_ASPECT_RATIO
                val spread = spreads[spreadIndex]

                Box(
                    modifier = Modifier
                        .size(bookWidth, bookHeight)
                        .graphicsLayer {
                            scaleX = bookScale
                            scaleY = bookScale
                            rotationX = 3f
                            cameraDistance = 18f * density
                        },
                ) {
                    // 书底阴影
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 8.dp)
                            .shadow(16.dp, RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    )

                    // 封面与书脊外壳
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .shadow(7.dp, RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF5C6E54), Color(0xFF46543F)),
                                ),
                                RoundedCornerShape(12.dp),
                            ),
                    )

                    // 纸张厚度：两层错位的纸块
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(6.dp)
                            .background(PaperDeep, RoundedCornerShape(8.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(9.dp)
                            .background(PaperCream, RoundedCornerShape(6.dp)),
                    )

                    Row(modifier = Modifier.matchParentSize().padding(12.dp)) {
                        BookPagePane(
                            page = spread.left?.let { pages.getOrNull(it) },
                            pageNumber = spread.left?.plus(1),
                            isFrontCover = spread.left == null,
                            isRightPage = false,
                            notebook = notebook,
                            repository = repository,
                            enabled = !isAnimating,
                            onPageClick = { page ->
                                if (isEditing) onEditPage(page) else onOpenSinglePage(page)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )

                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.22f),
                                            Color(0xFFD9CDB6),
                                            Color.Black.copy(alpha = 0.22f),
                                        ),
                                    ),
                                ),
                        )

                        BookPagePane(
                            page = spread.right?.let { pages.getOrNull(it) },
                            pageNumber = spread.right?.plus(1),
                            isFrontCover = false,
                            isRightPage = true,
                            notebook = notebook,
                            repository = repository,
                            enabled = !isAnimating,
                            onPageClick = { page ->
                                if (isEditing) onEditPage(page) else onOpenSinglePage(page)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }

                    if (turnDirection != 0) {
                        val sourceSpread = spreads.getOrNull(turnFromSpread)
                        val pageIndex = if (turnDirection > 0) {
                            sourceSpread?.right
                        } else {
                            sourceSpread?.left
                        }
                        val page = pageIndex?.let { pages.getOrNull(it) }
                        val angle = if (turnDirection > 0) {
                            -180f * turnProgress
                        } else {
                            180f * turnProgress
                        }
                        val backFace = turnProgress > 0.5f

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .align(
                                    if (turnDirection > 0) {
                                        Alignment.CenterEnd
                                    } else {
                                        Alignment.CenterStart
                                    },
                                )
                                .padding(12.dp)
                                .graphicsLayer {
                                    rotationY = angle
                                    transformOrigin = TransformOrigin(
                                        pivotFractionX = if (turnDirection > 0) 0f else 1f,
                                        pivotFractionY = 0.5f,
                                    )
                                    cameraDistance = 22f * density
                                    // 收尾时淡出，避免纸页落定瞬间突兀消失。
                                    alpha = if (turnProgress > 0.85f) {
                                        ((1f - turnProgress) / 0.15f).coerceIn(0f, 1f)
                                    } else {
                                        1f
                                    }
                                }
                                .shadow(8.dp, RoundedCornerShape(3.dp))
                                .background(PaperCream, RoundedCornerShape(3.dp))
                                .border(
                                    width = 0.5.dp,
                                    color = OutlineWarm.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(3.dp),
                                ),
                        ) {
                            if (page != null && !backFace) {
                                PageImage(
                                    imagePath = page.imagePath,
                                    isBlank = page.isBlank,
                                    repository = repository,
                                    modifier = Modifier.fillMaxSize(),
                                    maxSizePx = 1000,
                                    blankStyle = BlankPageStyle.READER,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PaperCream),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.05f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.07f),
                                                    ),
                                                ),
                                            ),
                                    )
                                }
                            }

                            // 翻页纸的阴影随角度变化
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Black.copy(
                                                    alpha = 0.16f * (1f - turnProgress),
                                                ),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }
        }

        val currentSpread = spreads[spreadIndex]
        val spreadLabel = when {
            currentSpread.left == null && currentSpread.right != null ->
                stringResource(R.string.page_number, currentSpread.right + 1)

            currentSpread.right == null && currentSpread.left != null ->
                stringResource(R.string.page_number, currentSpread.left + 1)

            currentSpread.left != null && currentSpread.right != null ->
                stringResource(
                    R.string.page_spread_label,
                    currentSpread.left + 1,
                    currentSpread.right + 1,
                )

            else -> ""
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { turnPage(-1) },
                enabled = !isAnimating && spreadIndex > 0,
            ) {
                PagerArrow(
                    left = true,
                    color = if (!isAnimating && spreadIndex > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }

            Text(
                text = spreadLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = { turnPage(1) },
                enabled = !isAnimating && spreadIndex < spreads.lastIndex,
            ) {
                PagerArrow(
                    left = false,
                    color = if (!isAnimating && spreadIndex < spreads.lastIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}

@Composable
private fun BookPagePane(
    page: DiaryPage?,
    pageNumber: Int?,
    isFrontCover: Boolean,
    isRightPage: Boolean,
    notebook: Notebook,
    repository: DiaryRepository,
    enabled: Boolean,
    onPageClick: (DiaryPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(PaperCream)
            .border(
                width = 0.5.dp,
                color = OutlineWarm.copy(alpha = 0.55f),
                shape = RoundedCornerShape(3.dp),
            )
            .clickable(enabled = enabled && page != null) {
                page?.let(onPageClick)
            },
    ) {
        when {
            page != null -> PageImage(
                imagePath = page.imagePath,
                isBlank = page.isBlank,
                repository = repository,
                modifier = Modifier.fillMaxSize(),
                maxSizePx = 1000,
                blankStyle = BlankPageStyle.READER,
            )

            isFrontCover -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                NotebookCover(
                    notebook = notebook,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperDeep.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.book_back_cover),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 书脊一侧的内阴影
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(18.dp)
                .align(if (isRightPage) Alignment.CenterStart else Alignment.CenterEnd)
                .background(
                    Brush.horizontalGradient(
                        if (isRightPage) {
                            listOf(Color.Black.copy(alpha = 0.14f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f))
                        },
                    ),
                ),
        )

        if (pageNumber != null) {
            Text(
                text = stringResource(R.string.page_number, pageNumber),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
            )
        }
    }
}
