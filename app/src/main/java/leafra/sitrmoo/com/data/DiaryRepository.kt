package leafra.sitrmoo.com.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import leafra.sitrmoo.com.data.local.LeafraDatabase
import leafra.sitrmoo.com.data.local.NotebookEntity
import leafra.sitrmoo.com.data.local.NotebookSummary
import leafra.sitrmoo.com.data.local.PageEntity
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.data.model.ImportFailureReason
import leafra.sitrmoo.com.data.model.ImportResult
import leafra.sitrmoo.com.data.model.Notebook

/**
 * 应用唯一的数据入口。
 *
 * 职责：
 * - 把 Room 实体映射为领域模型（页码在这里按排序动态计算）；
 * - 串行化“复制图片 + 写数据库”的流程，失败时回滚并清理文件；
 * - 删除日记本时做引用计数，避免误删仍被其他记录引用的图片；
 * - 启动时清理数据库未引用的孤立图片。
 *
 * Composable 只依赖本类，不直接操作 DAO 或文件系统。
 */
class DiaryRepository(
    context: Context,
    private val database: LeafraDatabase,
) {

    private val notebookDao = database.notebookDao()
    private val pageDao = database.pageDao()
    private val imageStore = ImageStore(context.applicationContext)

    /** 串行化所有“文件 + 数据库”操作，避免清理任务删掉正在导入的文件。 */
    private val fileMutex = Mutex()

    private val cleanupStarted = AtomicBoolean(false)

    /** 书架列表，按更新时间倒序。 */
    val notebooks: Flow<List<Notebook>> =
        notebookDao.observeSummaries().map { rows -> rows.map { it.toModel() } }

    /**
     * 指定日记本的页面流。
     * 页码根据当前排序实时计算，从 1 开始连续编号，不写入数据库。
     */
    fun pages(notebookId: String): Flow<List<DiaryPage>> =
        pageDao.observePages(notebookId).map { entities ->
            entities.mapIndexed { index, entity -> entity.toModel(pageNumber = index + 1) }
        }

    /** 新建日记本；名称去除首尾空白后不能为空。 */
    suspend fun createNotebook(rawName: String): Result<Notebook> = withContext(Dispatchers.IO) {
        runCatching {
            val name = rawName.trim()
            require(name.isNotEmpty()) { "日记本名称不能为空" }
            require(name.length <= MAX_NAME_LENGTH) { "日记本名称不能超过 $MAX_NAME_LENGTH 个字" }

            val now = System.currentTimeMillis()
            val entity = NotebookEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                coverImagePath = null,
                createdAt = now,
                updatedAt = now,
                lastReadPageId = null,
            )
            notebookDao.insert(entity)
            NotebookSummary(notebook = entity, pageCount = 0).toModel()
        }
    }

    /**
     * 把选中的图片全部复制到私有目录，再在同一个数据库事务里写入页面记录。
     *
     * 失败策略：任意一张图片复制失败、或数据库写入失败，都会删除本次已复制的文件，
     * 数据库事务回滚，保证不会留下指向不存在文件的记录。
     */
    suspend fun importImages(notebookId: String, uris: List<Uri>): ImportResult {
        if (uris.isEmpty()) return ImportResult.EmptySelection

        return withContext(Dispatchers.IO) {
            fileMutex.withLock {
                if (notebookDao.findById(notebookId) == null) {
                    return@withLock ImportResult.Failure(ImportFailureReason.NOTEBOOK_NOT_FOUND)
                }

                val copiedPaths = mutableListOf<String>()
                try {
                    uris.forEach { uri ->
                        copiedPaths += imageStore.copyFromUri(uri)
                    }

                    val startOrder = pageDao.maxSortOrder(notebookId)
                    val now = System.currentTimeMillis()
                    val pages = copiedPaths.mapIndexed { index, path ->
                        PageEntity(
                            id = UUID.randomUUID().toString(),
                            notebookId = notebookId,
                            sortOrder = startOrder + index + 1,
                            entryDate = null,
                            description = null,
                            imagePath = path,
                            isBlank = false,
                            isFavorite = false,
                            createdAt = now,
                        )
                    }

                    database.withTransaction {
                        pageDao.insertAll(pages)
                        notebookDao.setCoverIfEmpty(notebookId, copiedPaths.first())
                        notebookDao.touch(notebookId, now)
                    }

                    ImportResult.Success(
                        importedCount = copiedPaths.size,
                        firstPageNumber = startOrder.toInt() + 1,
                    )
                } catch (t: Throwable) {
                    copiedPaths.forEach { path -> runCatching { imageStore.delete(path) } }
                    val reason = when (t) {
                        is IOException, is SecurityException -> ImportFailureReason.COPY_FAILED
                        else -> ImportFailureReason.DATABASE_FAILED
                    }
                    ImportResult.Failure(reason)
                }
            }
        }
    }

    /**
     * 删除日记本及其页面。
     *
     * 图片只有在没有任何页面（跨日记本）或任何封面继续引用时才删除，
     * 因此未来即使多个记录共享同一张图片，也不会被误删。
     */
    suspend fun deleteNotebook(notebookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            runCatching {
                val referencedPaths = database.withTransaction {
                    val pages = pageDao.getPages(notebookId)
                    val notebook = notebookDao.findById(notebookId)
                    notebookDao.delete(notebookId) // 外键 CASCADE 删除所属页面
                    (pages.mapNotNull { it.imagePath } + listOfNotNull(notebook?.coverImagePath))
                        .distinct()
                }

                referencedPaths.forEach { path ->
                    val stillReferenced =
                        pageDao.countByImagePath(path) + notebookDao.countCoverReferences(path)
                    if (stillReferenced == 0) {
                        imageStore.delete(path)
                    }
                }
            }
        }
    }

    /**
     * 清理私有目录中数据库未引用的孤立图片（例如复制完成后进程被杀）。
     * 每个进程只在启动时执行一次；与导入/删除共用文件锁，避免竞态。
     */
    suspend fun cleanupOrphanedImages() {
        if (!cleanupStarted.compareAndSet(false, true)) return

        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                runCatching {
                    val referenced =
                        (pageDao.allImagePaths() + notebookDao.allCoverPaths()).toSet()
                    imageStore.listExistingRelativePaths().forEach { path ->
                        if (path !in referenced) {
                            imageStore.delete(path)
                        }
                    }
                }
            }
        }
    }

    /**
     * 预留的阅读进度接口：记录日记本上次读到的页面。
     *
     * 当前界面不调用；后续阅读器只需传入稳定的页面 ID 即可，
     * 页码仍然由排序动态计算，不在数据库中保存。
     */
    suspend fun setLastReadPage(notebookId: String, pageId: String?) {
        withContext(Dispatchers.IO) {
            notebookDao.updateLastReadPage(notebookId, pageId, System.currentTimeMillis())
        }
    }

    /** 供界面加载缩略图使用的只读文件引用。 */
    fun imageFile(relativePath: String): File = imageStore.resolve(relativePath)

    private fun NotebookSummary.toModel() = Notebook(
        id = notebook.id,
        name = notebook.name,
        coverImagePath = notebook.coverImagePath,
        pageCount = pageCount,
        createdAt = notebook.createdAt,
        updatedAt = notebook.updatedAt,
        lastReadPageId = notebook.lastReadPageId,
    )

    private fun PageEntity.toModel(pageNumber: Int) = DiaryPage(
        id = id,
        notebookId = notebookId,
        sortOrder = sortOrder,
        pageNumber = pageNumber,
        entryDate = entryDate,
        description = description,
        imagePath = imagePath,
        isBlank = isBlank,
        isFavorite = isFavorite,
        createdAt = createdAt,
    )

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
