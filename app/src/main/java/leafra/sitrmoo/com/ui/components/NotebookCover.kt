package leafra.sitrmoo.com.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.model.CoverStyle
import leafra.sitrmoo.com.data.model.Notebook
import leafra.sitrmoo.com.ui.theme.InkBrown
import leafra.sitrmoo.com.ui.theme.InkMuted
import leafra.sitrmoo.com.ui.theme.SageGreen

/**
 * 本地生成封面：不读写任何图片文件，只根据日记本名称与预设模板实时绘制。
 *
 * 这样做的好处：
 * - 永远不会覆盖原始日记照片；
 * - 不需要额外的封面文件、缩略图缓存或清理逻辑；
 * - 模板集中在 [paletteFor] 与 [drawDecoration] 中，新增风格只改这一处。
 */
private enum class CoverDecoration { SPRIG, BRANCH, LEAVES, DOTS }

private data class CoverPalette(
    val background: List<Color>,
    val accent: Color,
    val ink: Color,
    val mutedInk: Color,
    val decoration: CoverDecoration,
)

private val sagePalette = CoverPalette(
    background = listOf(Color(0xFFFCF8EF), Color(0xFFF0E7D4)),
    accent = SageGreen,
    ink = InkBrown,
    mutedInk = InkMuted,
    decoration = CoverDecoration.SPRIG,
)

private val olivePalette = CoverPalette(
    background = listOf(Color(0xFFF8F1E4), Color(0xFFECDFC7)),
    accent = Color(0xFF9A9B6E),
    ink = Color(0xFF46402F),
    mutedInk = Color(0xFF7E7461),
    decoration = CoverDecoration.BRANCH,
)

private val mistPalette = CoverPalette(
    background = listOf(Color(0xFFF3F5F1), Color(0xFFE5EAE3)),
    accent = Color(0xFF8CA49D),
    ink = Color(0xFF37413C),
    mutedInk = Color(0xFF6F7A74),
    decoration = CoverDecoration.LEAVES,
)

private val clayPalette = CoverPalette(
    background = listOf(Color(0xFFF8EFE7), Color(0xFFEBD9C9)),
    accent = Color(0xFFB08A6A),
    ink = Color(0xFF4A3B2F),
    mutedInk = Color(0xFF806B5A),
    decoration = CoverDecoration.DOTS,
)

private fun paletteFor(style: Int): CoverPalette = when (CoverStyle.fromId(style)) {
    CoverStyle.SAGE -> sagePalette
    CoverStyle.OLIVE -> olivePalette
    CoverStyle.MIST -> mistPalette
    CoverStyle.CLAY -> clayPalette
}

/** 封面风格在界面上的显示名称。 */
fun coverStyleLabelRes(style: CoverStyle): Int = when (style) {
    CoverStyle.SAGE -> R.string.cover_style_sage
    CoverStyle.OLIVE -> R.string.cover_style_olive
    CoverStyle.MIST -> R.string.cover_style_mist
    CoverStyle.CLAY -> R.string.cover_style_clay
}

@Composable
fun NotebookCover(
    notebook: Notebook,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(notebook.coverStyle)

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(palette.background)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCoverDecoration(palette)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawLeafMark(palette.accent)
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "LEAFORA",
                    color = palette.accent,
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column {
                Text(
                    text = notebook.name,
                    color = palette.ink,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.notebook_page_count, notebook.pageCount),
                    color = palette.mutedInk,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun DrawScope.drawLeafMark(color: Color) {
    val leaf = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.85f)
        cubicTo(
            size.width * 0.08f,
            size.height * 0.38f,
            size.width * 0.46f,
            size.height * 0.04f,
            size.width * 0.92f,
            size.height * 0.14f,
        )
        cubicTo(
            size.width * 0.96f,
            size.height * 0.62f,
            size.width * 0.60f,
            size.height * 0.96f,
            size.width * 0.15f,
            size.height * 0.85f,
        )
        close()
    }
    drawPath(leaf, color = color)
}

private fun DrawScope.drawCoverDecoration(palette: CoverPalette) {
    if (size.width <= 0f || size.height <= 0f) return

    val canvasWidth = size.width
    val canvasHeight = size.height
    val soft = palette.accent.copy(alpha = 0.38f)

    when (palette.decoration) {
        CoverDecoration.SPRIG -> {
            val stem = Path().apply {
                moveTo(canvasWidth * 0.72f, canvasHeight * 0.02f)
                cubicTo(
                    canvasWidth * 0.70f,
                    canvasHeight * 0.20f,
                    canvasWidth * 0.86f,
                    canvasHeight * 0.26f,
                    canvasWidth * 0.82f,
                    canvasHeight * 0.44f,
                )
            }
            drawPath(stem, soft, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
            drawLeaf(
                center = Offset(canvasWidth * 0.71f, canvasHeight * 0.10f),
                leafWidth = canvasWidth * 0.17f,
                leafHeight = canvasHeight * 0.035f,
                degrees = -35f,
                color = soft,
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.78f, canvasHeight * 0.18f),
                leafWidth = canvasWidth * 0.15f,
                leafHeight = canvasHeight * 0.032f,
                degrees = 25f,
                color = soft.copy(alpha = 0.30f),
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.81f, canvasHeight * 0.28f),
                leafWidth = canvasWidth * 0.14f,
                leafHeight = canvasHeight * 0.030f,
                degrees = -20f,
                color = soft.copy(alpha = 0.32f),
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.85f, canvasHeight * 0.38f),
                leafWidth = canvasWidth * 0.14f,
                leafHeight = canvasHeight * 0.030f,
                degrees = 30f,
                color = soft.copy(alpha = 0.28f),
            )
            drawCircle(
                color = soft.copy(alpha = 0.25f),
                radius = 2.dp.toPx(),
                center = Offset(canvasWidth * 0.62f, canvasHeight * 0.30f),
            )
            drawCircle(
                color = soft.copy(alpha = 0.20f),
                radius = 1.5.dp.toPx(),
                center = Offset(canvasWidth * 0.66f, canvasHeight * 0.36f),
            )
        }

        CoverDecoration.BRANCH -> {
            val branch = Path().apply {
                moveTo(canvasWidth * 0.16f, canvasHeight * 0.12f)
                cubicTo(
                    canvasWidth * 0.42f,
                    canvasHeight * 0.06f,
                    canvasWidth * 0.66f,
                    canvasHeight * 0.10f,
                    canvasWidth * 0.88f,
                    canvasHeight * 0.04f,
                )
            }
            drawPath(branch, soft, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
            drawLeaf(
                center = Offset(canvasWidth * 0.34f, canvasHeight * 0.09f),
                leafWidth = canvasWidth * 0.13f,
                leafHeight = canvasHeight * 0.032f,
                degrees = -25f,
                color = soft,
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.52f, canvasHeight * 0.07f),
                leafWidth = canvasWidth * 0.13f,
                leafHeight = canvasHeight * 0.032f,
                degrees = 20f,
                color = soft.copy(alpha = 0.30f),
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.70f, canvasHeight * 0.07f),
                leafWidth = canvasWidth * 0.12f,
                leafHeight = canvasHeight * 0.030f,
                degrees = -15f,
                color = soft,
            )
            drawCircle(
                color = soft.copy(alpha = 0.22f),
                radius = 2.2.dp.toPx(),
                center = Offset(canvasWidth * 0.90f, canvasHeight * 0.16f),
            )
            drawCircle(
                color = soft.copy(alpha = 0.18f),
                radius = 1.4.dp.toPx(),
                center = Offset(canvasWidth * 0.83f, canvasHeight * 0.20f),
            )
        }

        CoverDecoration.LEAVES -> {
            drawLeaf(
                center = Offset(canvasWidth * 0.84f, canvasHeight * 0.16f),
                leafWidth = canvasWidth * 0.34f,
                leafHeight = canvasHeight * 0.10f,
                degrees = 35f,
                color = soft.copy(alpha = 0.22f),
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.88f, canvasHeight * 0.30f),
                leafWidth = canvasWidth * 0.28f,
                leafHeight = canvasHeight * 0.085f,
                degrees = 65f,
                color = soft.copy(alpha = 0.18f),
            )
            drawLeaf(
                center = Offset(canvasWidth * 0.80f, canvasHeight * 0.42f),
                leafWidth = canvasWidth * 0.24f,
                leafHeight = canvasHeight * 0.070f,
                degrees = 15f,
                color = soft.copy(alpha = 0.20f),
            )
            val stem = Path().apply {
                moveTo(canvasWidth * 0.92f, canvasHeight * 0.06f)
                cubicTo(
                    canvasWidth * 0.86f,
                    canvasHeight * 0.20f,
                    canvasWidth * 0.90f,
                    canvasHeight * 0.30f,
                    canvasWidth * 0.82f,
                    canvasHeight * 0.44f,
                )
            }
            drawPath(stem, soft, style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
        }

        CoverDecoration.DOTS -> {
            val dots = listOf(
                Offset(canvasWidth * 0.84f, canvasHeight * 0.08f) to 2.4f,
                Offset(canvasWidth * 0.74f, canvasHeight * 0.14f) to 1.6f,
                Offset(canvasWidth * 0.88f, canvasHeight * 0.20f) to 3.0f,
                Offset(canvasWidth * 0.78f, canvasHeight * 0.27f) to 1.4f,
                Offset(canvasWidth * 0.90f, canvasHeight * 0.33f) to 2.0f,
            )
            dots.forEach { (center, radius) ->
                drawCircle(
                    color = soft.copy(alpha = 0.35f),
                    radius = radius.dp.toPx(),
                    center = center,
                )
            }
            val stem = Path().apply {
                moveTo(canvasWidth * 0.86f, canvasHeight * 0.06f)
                cubicTo(
                    canvasWidth * 0.80f,
                    canvasHeight * 0.16f,
                    canvasWidth * 0.84f,
                    canvasHeight * 0.24f,
                    canvasWidth * 0.78f,
                    canvasHeight * 0.34f,
                )
            }
            drawPath(
                stem,
                soft.copy(alpha = 0.40f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    // 内层细边框，让封面更像一张装订好的纸。
    drawRoundRect(
        color = palette.accent.copy(alpha = 0.16f),
        topLeft = Offset(7.dp.toPx(), 7.dp.toPx()),
        size = Size(canvasWidth - 14.dp.toPx(), canvasHeight - 14.dp.toPx()),
        cornerRadius = CornerRadius(11.dp.toPx()),
        style = Stroke(width = 0.8.dp.toPx()),
    )
}

private fun DrawScope.drawLeaf(
    center: Offset,
    leafWidth: Float,
    leafHeight: Float,
    degrees: Float,
    color: Color,
) {
    rotate(degrees = degrees, pivot = center) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - leafWidth / 2f, center.y - leafHeight / 2f),
            size = Size(leafWidth, leafHeight),
        )
    }
}
