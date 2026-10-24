package leafra.sitrmoo.com

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.local.LeafraDatabase

/**
 * 手动依赖注入入口：整个进程共用一个数据库与仓库实例。
 * 本阶段不引入 Hilt 等框架，保持依赖最少。
 */
class LeafraApplication : Application() {

    lateinit var repository: DiaryRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = LeafraDatabase.getInstance(this)
        repository = DiaryRepository(this, database)
        // 清理上次进程异常退出时遗留的、数据库未引用的孤立图片。
        applicationScope.launch { repository.cleanupOrphanedImages() }
    }
}
