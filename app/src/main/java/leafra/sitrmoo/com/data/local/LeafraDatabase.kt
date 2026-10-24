package leafra.sitrmoo.com.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 应用数据库。
 *
 * 版本 2 在版本 1 的基础上为 `notebooks` 增加 `coverStyle`（本地生成封面的风格编号）。
 * 不启用 destructive migration；后续修改表结构时必须显式编写 Migration，
 * 避免用户数据被静默清空。
 */
@Database(
    entities = [NotebookEntity::class, PageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class LeafraDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao

    abstract fun pageDao(): PageDao

    companion object {
        private const val DATABASE_NAME = "leafra.db"

        /** 版本 1 -> 2：为已有日记本补充封面风格，默认 0（鼠尾草）。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notebooks ADD COLUMN coverStyle INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var instance: LeafraDatabase? = null

        fun getInstance(context: Context): LeafraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeafraDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
