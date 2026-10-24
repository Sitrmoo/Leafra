package leafra.sitrmoo.com.data.model

/**
 * 日记本领域模型，不直接暴露 Room 实体。
 *
 * [coverImagePath] 是可选的真实照片封面引用（本阶段不写入，保留给后续功能）。
 * [coverStyle] 决定本地生成封面的模板，封面由 Compose 按名称实时绘制，不产生图片文件。
 */
data class Notebook(
    val id: String,
    val name: String,
    val coverImagePath: String?,
    val coverStyle: Int,
    val pageCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** 预留：上次阅读的页面 ID；当前界面尚未写入或使用。 */
    val lastReadPageId: String?,
)

/**
 * 日记页面领域模型。
 *
 * [pageNumber] 由仓库层按 [sortOrder] 动态计算，从 1 开始连续编号；
 * 它不是数据库主键，也不用于寻址，[id] 才是稳定标识。
 */
data class DiaryPage(
    val id: String,
    val notebookId: String,
    val sortOrder: Long,
    val pageNumber: Int,
    /** 可选的日记日期（epoch millis），导入时暂不填写。 */
    val entryDate: Long?,
    /** 可选的页面描述，导入时暂不填写。 */
    val description: String?,
    /** 相对应用私有目录的图片路径，例如 `diary-images/xxx.jpg`；空白页为 null。 */
    val imagePath: String?,
    /** 空白占位页标记：没有图片但需要在书中保留一页时使用。 */
    val isBlank: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long,
)

/** 本地生成封面的预设模板；id 持久化在 `notebooks.coverStyle` 中。 */
enum class CoverStyle(val id: Int) {
    SAGE(0),
    OLIVE(1),
    MIST(2),
    CLAY(3),
    ;

    companion object {
        fun fromId(id: Int): CoverStyle = entries.firstOrNull { it.id == id } ?: SAGE

        fun isValid(id: Int): Boolean = entries.any { it.id == id }
    }
}

/** 导入结果，界面据此给出提示，不直接依赖异常文本。 */
sealed interface ImportResult {
    data class Success(
        val importedCount: Int,
        val firstPageNumber: Int,
    ) : ImportResult

    data object EmptySelection : ImportResult

    data class Failure(val reason: ImportFailureReason) : ImportResult
}

enum class ImportFailureReason {
    NOTEBOOK_NOT_FOUND,
    COPY_FAILED,
    DATABASE_FAILED,
    UNKNOWN,
}

/** 新建日记本（可带初始页面）的结果。 */
sealed interface CreateNotebookResult {
    data class Success(
        val notebook: Notebook,
        val importedCount: Int,
    ) : CreateNotebookResult

    data class Failure(val reason: CreateNotebookFailure) : CreateNotebookResult
}

enum class CreateNotebookFailure {
    INVALID_NAME,
    COPY_FAILED,
    DATABASE_FAILED,
    UNKNOWN,
}
