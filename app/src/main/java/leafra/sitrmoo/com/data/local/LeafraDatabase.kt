package leafra.sitrmoo.com.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用数据库。
 *
 * 版本 1 只包含日记本与页面两张表；不启用 destructive migration，
 * 后续修改表结构时必须显式编写 Migration，避免用户数据被静默清空。
 */
@Database(
    entities = [NotebookEntity::class, PageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LeafraDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao

    abstract fun pageDao(): PageDao

    companion object {
        private const val DATABASE_NAME = "leafra.db"

        @Volatile
        private var instance: LeafraDatabase? = null

        fun getInstance(context: Context): LeafraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeafraDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
