package leafra.sitrmoo.com.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Ready(val bitmap: ImageBitmap) : ImageLoadState
    data object Missing : ImageLoadState
}

/** 空白占位页在编辑器与阅读器中的两种样式。 */
enum class BlankPageStyle {
    EDITOR,
    READER,
}

/**
 * 页面图片：空白占位页与“图片缺失”有明确区分，不会把占位页显示成已有照片。
 * 图片从应用私有目录按需下采样解码，避免大图占用过多内存。
 */
@Composable
fun PageImage(
    imagePath: String?,
    isBlank: Boolean,
    repository: DiaryRepository,
    modifier: Modifier = Modifier,
    maxSizePx: Int = 640,
    contentScale: ContentScale = ContentScale.Fit,
    blankStyle: BlankPageStyle = BlankPageStyle.EDITOR,
) {
    when {
        isBlank -> BlankPagePlaceholder(modifier = modifier, style = blankStyle)
        imagePath == null -> MissingPagePlaceholder(modifier = modifier)
        else -> LoadedPageImage(
            imagePath = imagePath,
            repository = repository,
            modifier = modifier,
            maxSizePx = maxSizePx,
            contentScale = contentScale,
        )
    }
}

@Composable
private fun LoadedPageImage(
    imagePath: String,
    repository: DiaryRepository,
    modifier: Modifier,
    maxSizePx: Int,
    contentScale: ContentScale,
) {
    val state by produceState<ImageLoadState>(
        initialValue = ImageLoadState.Loading,
        imagePath,
        maxSizePx,
    ) {
        value = withContext(Dispatchers.IO) {
            val bitmap = runCatching {
                decodeSampledBitmap(repository.imageFile(imagePath), maxSizePx)
            }.getOrNull()
            if (bitmap == null) ImageLoadState.Missing else ImageLoadState.Ready(bitmap)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val current = state) {
            ImageLoadState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            )

            is ImageLoadState.Ready -> Image(
                bitmap = current.bitmap,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )

            ImageLoadState.Missing -> MissingPagePlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BlankPagePlaceholder(
    modifier: Modifier = Modifier,
    style: BlankPageStyle = BlankPageStyle.EDITOR,
) {
    if (style == BlankPageStyle.READER) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.blank_page),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashed = PathEffect.dashPathEffect(
                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
            )
            drawRoundRect(
                color = outline,
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx(), pathEffect = dashed),
            )
        }
        Text(
            text = stringResource(R.string.blank_page),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MissingPagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.image_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun decodeSampledBitmap(file: File, maxSize: Int): ImageBitmap? {
    if (!file.isFile) return null

    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxSize || bounds.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
    } catch (t: Throwable) {
        null
    }
}
