package leafra.sitrmoo.com.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 页面数据访问接口；页码不落库，由 Repository 根据排序计算。 */
@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY sortOrder ASC, createdAt ASC")
    fun observePages(notebookId: String): Flow<List<PageEntity>>

    /** 搜索与收藏使用的全量页面投影；页码由 Repository 按日记本分别计算。 */
    @Query(
        """
        SELECT pages.*, notebooks.name AS notebookName
        FROM pages INNER JOIN notebooks ON notebooks.id = pages.notebookId
        ORDER BY pages.notebookId ASC, pages.sortOrder ASC, pages.createdAt ASC
        """
    )
    fun observePagesWithNotebook(): Flow<List<PageWithNotebook>>

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getPages(notebookId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun findById(pageId: String): PageEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM pages WHERE notebookId = :notebookId")
    suspend fun maxSortOrder(notebookId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(pages: List<PageEntity>)

    @Query("UPDATE pages SET sortOrder = :sortOrder WHERE id = :pageId")
    suspend fun updateSortOrder(pageId: String, sortOrder: Long)

    @Query("UPDATE pages SET imagePath = :imagePath, isBlank = :isBlank WHERE id = :pageId")
    suspend fun updateImage(pageId: String, imagePath: String?, isBlank: Boolean)

    @Query(
        "UPDATE pages SET description = :description, entryDate = :entryDate, isFavorite = :isFavorite " +
            "WHERE id = :pageId"
    )
    suspend fun updateMeta(
        pageId: String,
        description: String?,
        entryDate: Long?,
        isFavorite: Boolean,
    )

    @Query("UPDATE pages SET isFavorite = :isFavorite WHERE id = :pageId")
    suspend fun updateFavorite(pageId: String, isFavorite: Boolean)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deleteById(pageId: String)

    @Query("SELECT imagePath FROM pages WHERE imagePath IS NOT NULL")
    suspend fun allImagePaths(): List<String>

    /** 删除文件前的引用计数：还有没有页面引用这个图片路径。 */
    @Query("SELECT COUNT(*) FROM pages WHERE imagePath = :imagePath")
    suspend fun countByImagePath(imagePath: String): Int
}
