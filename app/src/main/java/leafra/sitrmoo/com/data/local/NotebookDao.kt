package leafra.sitrmoo.com.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 日记本数据访问接口；Composable 不直接使用它，只通过 Repository。 */
@Dao
interface NotebookDao {

    /** 书架列表：按更新时间倒序，并带出每个日记本的页面数。 */
    @Query(
        """
        SELECT notebooks.*,
               (SELECT COUNT(*) FROM pages WHERE pages.notebookId = notebooks.id) AS pageCount
        FROM notebooks
        ORDER BY notebooks.updatedAt DESC
        """
    )
    fun observeSummaries(): Flow<List<NotebookSummary>>

    @Query("SELECT * FROM notebooks WHERE id = :notebookId")
    suspend fun findById(notebookId: String): NotebookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun touch(notebookId: String, updatedAt: Long)

    @Query("UPDATE notebooks SET name = :name, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun updateName(notebookId: String, name: String, updatedAt: Long)

    /** 只更新生成封面的模板编号；本地生成封面不产生图片文件。 */
    @Query("UPDATE notebooks SET coverStyle = :coverStyle, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun updateCoverStyle(notebookId: String, coverStyle: Int, updatedAt: Long)

    /** 预留阅读进度接口：保存上次阅读的页面 ID（不是页码）。 */
    @Query("UPDATE notebooks SET lastReadPageId = :pageId, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun updateLastReadPage(notebookId: String, pageId: String?, updatedAt: Long)

    @Query("DELETE FROM notebooks WHERE id = :notebookId")
    suspend fun delete(notebookId: String)

    @Query("SELECT coverImagePath FROM notebooks WHERE coverImagePath IS NOT NULL")
    suspend fun allCoverPaths(): List<String>

    /** 删除文件前的引用计数：还有没有日记本把这个路径当封面。 */
    @Query("SELECT COUNT(*) FROM notebooks WHERE coverImagePath = :imagePath")
    suspend fun countCoverReferences(imagePath: String): Int
}
