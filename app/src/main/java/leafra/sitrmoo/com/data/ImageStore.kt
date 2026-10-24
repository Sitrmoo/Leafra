package leafra.sitrmoo.com.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片文件仓库：负责把 Photo Picker 返回的 Uri 复制到应用私有目录，以及删除文件。
 *
 * 目录结构（相对 `filesDir`）：
 * ```
 * files/
 * └─ diary-images/
 *    ├─ 1f0c....jpg
 *    └─ 9ab2....png
 * ```
 *
 * 数据库只保存相对路径（例如 `diary-images/1f0c....jpg`），既不保存 Uri，
 * 也不保存图片字节，因此应用重启或系统回收 Uri 权限后仍能读取。
 */
class ImageStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR_NAME)

    /**
     * 把 [uri] 复制为私有目录中的新文件，返回相对路径。
     * 复制失败或内容为空时会删除半成品文件，保证不留下无效文件。
     */
    suspend fun copyFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val extension = extensionForMimeType(resolver.getType(uri))
        val relativePath = "$IMAGES_DIR_NAME/${UUID.randomUUID()}$extension"
        val target = resolve(relativePath)
        target.parentFile?.mkdirs()

        try {
            val input = resolver.openInputStream(uri)
                ?: throw IOException("无法打开所选图片")
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
            if (!target.isFile || target.length() == 0L) {
                throw IOException("所选图片内容为空")
            }
        } catch (t: Throwable) {
            target.delete()
            throw if (t is IOException) t else IOException("复制图片失败", t)
        }

        relativePath
    }

    /** 相对路径 -> 私有目录中的文件；拒绝跳出私有目录的路径。 */
    fun resolve(relativePath: String): File {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        if (!file.path.startsWith(root.path + File.separator)) {
            throw IllegalArgumentException("非法的图片路径：$relativePath")
        }
        return file
    }

    /** 文件 -> 相对路径（使用 `/` 分隔，保证跨平台一致）。 */
    fun relativePathOf(file: File): String =
        file.relativeTo(context.filesDir).invariantSeparatorsPath

    /** 列出私有图片目录中实际存在的所有文件相对路径，用于启动时清理孤立文件。 */
    suspend fun listExistingRelativePaths(): List<String> = withContext(Dispatchers.IO) {
        imagesDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.map { relativePathOf(it) }
            ?.toList()
            ?: emptyList()
    }

    /** 删除私有目录中的图片；文件不存在或删除失败都不抛异常。 */
    suspend fun delete(relativePath: String) {
        withContext(Dispatchers.IO) {
            runCatching { resolve(relativePath).delete() }
        }
    }

    private fun extensionForMimeType(mimeType: String?): String = when (mimeType) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/gif" -> ".gif"
        "image/heic", "image/heif" -> ".heic"
        else -> ".jpg"
    }

    private companion object {
        const val IMAGES_DIR_NAME = "diary-images"
    }
}
