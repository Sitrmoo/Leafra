package leafra.sitrmoo.com.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日记本表。
 *
 * [id] 为 UUID 字符串，作为稳定主键，不依赖自增序号。
 * [coverImagePath] 保留为“真实照片封面”的可选引用（第一阶段写入过第一页路径，
 * 本阶段改用本地生成封面，不再写入新值）；[coverStyle] 决定生成封面的模板。
 * [lastReadPageId] 为后续阅读进度预留：保存上次阅读的页面 ID，不保存页码。
 */
@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReadPageId: String?,
    @ColumnInfo(defaultValue = "0")
    val coverStyle: Int,
)

/**
 * 页面表。
 *
 * [imagePath] 保存应用私有目录下的相对路径，不保存 Uri，也不保存图片字节。
 * [isBlank] 为空白占位标记，配合 [imagePath] 为 null 使用。
 * [sortOrder] 决定页面顺序；展示页码由 Repository 动态计算，与主键无关。
 */
@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("notebookId"),
        Index(value = ["notebookId", "sortOrder"]),
    ],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val notebookId: String,
    val sortOrder: Long,
    val entryDate: Long?,
    val description: String?,
    val imagePath: String?,
    val isBlank: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long,
)

/** 书架列表投影：日记本实体 + 页面数量。 */
data class NotebookSummary(
    @Embedded val notebook: NotebookEntity,
    val pageCount: Int,
)

/** 搜索 / 收藏投影：页面实体 + 所属日记本名称。 */
data class PageWithNotebook(
    @Embedded val page: PageEntity,
    val notebookName: String,
)
