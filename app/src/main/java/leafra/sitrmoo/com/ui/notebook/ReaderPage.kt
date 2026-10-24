package leafra.sitrmoo.com.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.ui.components.BlankPageStyle
import leafra.sitrmoo.com.ui.components.PageImage

private const val MAX_ZOOM = 4f

/**
 * 单页阅读中的可缩放图片。
 *
 * 手势策略：
 * - 单指且未放大时不消费事件，交给 Pager 翻页；
 * - 双指捏合缩放（1x–4x），放大后单指拖动查看；
 * - 放大时由外层禁用 Pager 的滚动，避免翻页与拖动打架；
 * - 缩小回 1x 时自动归位并恢复翻页。
 */
@Composable
fun ZoomablePage(
    page: DiaryPage,
    repository: DiaryRepository,
    modifier: Modifier = Modifier,
    resetToken: Int = 0,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember(page.id) { mutableFloatStateOf(1f) }
    var offset by remember(page.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 翻页后强制复位，避免从上一页带回缩放状态。
    LaunchedEffect(resetToken) {
        scale = 1f
        offset = Offset.Zero
        onZoomChanged(false)
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(page.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                                scale = newScale
                                offset = clampOffset(
                                    offset = offset + panChange,
                                    scale = newScale,
                                    containerSize = containerSize,
                                )
                                event.changes.forEach { change -> change.consume() }
                                onZoomChanged(newScale > 1f)
                            }
                        } else if (scale > 1f) {
                            val panChange = event.calculatePan()
                            offset = clampOffset(
                                offset = offset + panChange,
                                scale = scale,
                                containerSize = containerSize,
                            )
                            event.changes.forEach { change -> change.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        PageImage(
            imagePath = page.imagePath,
            isBlank = page.isBlank,
            repository = repository,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            maxSizePx = 1600,
            blankStyle = BlankPageStyle.READER,
        )
    }
}

private fun clampOffset(
    offset: Offset,
    scale: Float,
    containerSize: IntSize,
): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = containerSize.width * (scale - 1f) / 2f
    val maxY = containerSize.height * (scale - 1f) / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

@Composable
fun PagerArrow(
    left: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val startX = if (left) size.width * 0.62f else size.width * 0.38f
        val elbowX = if (left) size.width * 0.34f else size.width * 0.66f
        val endX = if (left) size.width * 0.62f else size.width * 0.38f

        drawLine(
            color = color,
            start = Offset(startX, size.height * 0.18f),
            end = Offset(elbowX, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(elbowX, size.height * 0.50f),
            end = Offset(endX, size.height * 0.82f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
