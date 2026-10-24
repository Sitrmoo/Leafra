package leafra.sitrmoo.com.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import leafra.sitrmoo.com.data.local.LeafraDatabase
import leafra.sitrmoo.com.data.local.NotebookEntity
import leafra.sitrmoo.com.data.local.NotebookSummary
import leafra.sitrmoo.com.data.local.PageEntity
import leafra.sitrmoo.com.data.model.CreateNotebookFailure
import leafra.sitrmoo.com.data.model.CreateNotebookResult
import leafra.sitrmoo.com.data.model.CoverStyle
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

    /**
     * 新建日记本，可同时导入初始页面。
     *
     * 如果初始图片导入失败，会删除刚创建的日记本并回滚已复制的文件，
     * 避免留下“创建成功但页面丢失”的半成品。
     */
    suspend fun createNotebook(
        rawName: String,
        coverStyle: Int,
        initialImages: List<Uri> = emptyList(),
    ): CreateNotebookResult = withContext(Dispatchers.IO) {
        val name = rawName.trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) {
            return@withContext CreateNotebookResult.Failure(CreateNotebookFailure.INVALID_NAME)
        }

        val style = if (CoverStyle.isValid(coverStyle)) coverStyle else CoverStyle.SAGE.id
        val now = System.currentTimeMillis()
        val entity = NotebookEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            coverImagePath = null,
            createdAt = now,
            updatedAt = now,
            lastReadPageId = null,
            coverStyle = style,
        )

        val inserted = runCatching { notebookDao.insert(entity) }.isSuccess
        if (!inserted) {
            return@withContext CreateNotebookResult.Failure(CreateNotebookFailure.DATABASE_FAILED)
        }

        if (initialImages.isEmpty()) {
            return@withContext CreateNotebookResult.Success(
                notebook = NotebookSummary(notebook = entity, pageCount = 0).toModel(),
                importedCount = 0,
            )
        }

        when (val imported = importImages(entity.id, initialImages)) {
            is ImportResult.Success -> {
                val refreshed = notebookDao.findById(entity.id) ?: entity
                val pageCount = pageDao.getPages(entity.id).size
                CreateNotebookResult.Success(
                    notebook = NotebookSummary(refreshed, pageCount).toModel(),
                    importedCount = imported.importedCount,
                )
            }

            else -> {
                // 导入失败时撤销整个创建动作：页面事务已回滚，删除空日记本。
                runCatching { deleteNotebook(entity.id) }
                CreateNotebookResult.Failure(imported.toCreateFailure())
            }
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

    /** 重命名日记本；名称规则与新建一致。 */
    suspend fun renameNotebook(notebookId: String, rawName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = rawName.trim()
                require(name.isNotEmpty()) { "日记本名称不能为空" }
                require(name.length <= MAX_NAME_LENGTH) {
                    "日记本名称不能超过 $MAX_NAME_LENGTH 个字"
                }
                requireNotNull(notebookDao.findById(notebookId)) { "日记本不存在" }
                notebookDao.updateName(notebookId, name, System.currentTimeMillis())
            }
        }

    /** 更换本地生成封面的风格；只改模板编号，不产生或覆盖任何图片文件。 */
    suspend fun updateCoverStyle(notebookId: String, coverStyle: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(CoverStyle.isValid(coverStyle)) { "未知的封面风格" }
                requireNotNull(notebookDao.findById(notebookId)) { "日记本不存在" }
                notebookDao.updateCoverStyle(notebookId, coverStyle, System.currentTimeMillis())
            }
        }

    /** 追加一个空白占位页：稳定 UUID、无图片路径、isBlank = true。 */
    suspend fun addBlankPage(notebookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                requireNotNull(notebookDao.findById(notebookId)) { "日记本不存在" }
                val now = System.currentTimeMillis()
                pageDao.insertAll(
                    listOf(
                        PageEntity(
                            id = UUID.randomUUID().toString(),
                            notebookId = notebookId,
                            sortOrder = pageDao.maxSortOrder(notebookId) + 1,
                            entryDate = null,
                            description = null,
                            imagePath = null,
                            isBlank = true,
                            isFavorite = false,
                            createdAt = now,
                        ),
                    ),
                )
                notebookDao.touch(notebookId, now)
            }
        }
    }

    /**
     * 用图片替换空白占位页：先复制文件，再写数据库。
     * 写库失败会删除本次复制的文件，空白页保持原样。
     */
    suspend fun replacePlaceholderImage(
        notebookId: String,
        pageId: String,
        uri: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            runCatching {
                val page = pageDao.findById(pageId) ?: error("页面不存在")
                require(page.notebookId == notebookId) { "页面不属于该日记本" }
                require(page.isBlank && page.imagePath == null) { "只有空白页可以替换图片" }

                val copiedPath = imageStore.copyFromUri(uri)
                try {
                    database.withTransaction {
                        pageDao.updateImage(pageId, copiedPath, isBlank = false)
                        notebookDao.touch(notebookId, System.currentTimeMillis())
                    }
                } catch (t: Throwable) {
                    imageStore.delete(copiedPath)
                    throw t
                }
            }
        }
    }

    /** 保存页面的描述、日期与收藏状态；按页面 ID 更新，与当前排序无关。 */
    suspend fun updatePageMeta(
        pageId: String,
        description: String?,
        entryDate: Long?,
        isFavorite: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val page = pageDao.findById(pageId) ?: error("页面不存在")
            val trimmedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            database.withTransaction {
                pageDao.updateMeta(pageId, trimmedDescription, entryDate, isFavorite)
                notebookDao.touch(page.notebookId, System.currentTimeMillis())
            }
        }
    }

    /** 只切换收藏状态，供大纲行与阅读页的收藏按钮使用。 */
    suspend fun setPageFavorite(pageId: String, isFavorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val page = pageDao.findById(pageId) ?: error("页面不存在")
                database.withTransaction {
                    pageDao.updateFavorite(pageId, isFavorite)
                    notebookDao.touch(page.notebookId, System.currentTimeMillis())
                }
            }
        }

    /**
     * 按传入顺序重排页面：sortOrder 重写为 1..n，页面 ID 与页面属性都不变。
     * 传入 ID 必须与当前页面集合完全一致，避免覆盖并发的添加/删除。
     */
    suspend fun reorderPages(
        notebookId: String,
        orderedPageIds: List<String>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                val existing = pageDao.getPages(notebookId)
                require(existing.size == orderedPageIds.size) { "页面列表已变化，请重试" }
                require(existing.map { it.id }.toSet() == orderedPageIds.toSet()) {
                    "页面列表已变化，请重试"
                }
                orderedPageIds.forEachIndexed { index, pageId ->
                    pageDao.updateSortOrder(pageId, index + 1L)
                }
                notebookDao.touch(notebookId, System.currentTimeMillis())
            }
        }
    }

    /** 删除页面；仅当图片没有被其它页面或封面引用时才删除文件。 */
    suspend fun deletePage(notebookId: String, pageId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                runCatching {
                    val page = pageDao.findById(pageId) ?: return@runCatching
                    require(page.notebookId == notebookId) { "页面不属于该日记本" }
                    val imagePath = page.imagePath

                    database.withTransaction {
                        pageDao.deleteById(pageId)
                        notebookDao.clearLastReadPage(notebookId, pageId)
                        notebookDao.touch(notebookId, System.currentTimeMillis())
                    }

                    if (imagePath != null) {
                        val remaining = pageDao.countByImagePath(imagePath) +
                            notebookDao.countCoverReferences(imagePath)
                        if (remaining == 0) {
                            imageStore.delete(imagePath)
                        }
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
        // 阅读进度写入不可取消：快速翻页或立刻返回时也要落库。
        withContext(Dispatchers.IO + NonCancellable) {
            notebookDao.updateLastReadPage(notebookId, pageId)
        }
    }

    /** 供界面加载缩略图使用的只读文件引用。 */
    fun imageFile(relativePath: String): File = imageStore.resolve(relativePath)

    private fun NotebookSummary.toModel() = Notebook(
        id = notebook.id,
        name = notebook.name,
        coverImagePath = notebook.coverImagePath,
        coverStyle = notebook.coverStyle,
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

    private fun ImportResult.toCreateFailure(): CreateNotebookFailure = when (this) {
        is ImportResult.Failure -> when (reason) {
            ImportFailureReason.COPY_FAILED -> CreateNotebookFailure.COPY_FAILED
            ImportFailureReason.DATABASE_FAILED -> CreateNotebookFailure.DATABASE_FAILED
            ImportFailureReason.NOTEBOOK_NOT_FOUND,
            ImportFailureReason.UNKNOWN,
            -> CreateNotebookFailure.UNKNOWN
        }

        ImportResult.EmptySelection -> CreateNotebookFailure.UNKNOWN
        is ImportResult.Success -> CreateNotebookFailure.UNKNOWN
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
