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

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getPages(notebookId: String): List<PageEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM pages WHERE notebookId = :notebookId")
    suspend fun maxSortOrder(notebookId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(pages: List<PageEntity>)

    @Query("SELECT imagePath FROM pages WHERE imagePath IS NOT NULL")
    suspend fun allImagePaths(): List<String>

    /** 删除文件前的引用计数：还有没有页面引用这个图片路径。 */
    @Query("SELECT COUNT(*) FROM pages WHERE imagePath = :imagePath")
    suspend fun countByImagePath(imagePath: String): Int
}
