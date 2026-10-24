package leafra.sitrmoo.com

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import leafra.sitrmoo.com.ui.home.HomeScreen
import leafra.sitrmoo.com.ui.notebook.NotebookScreen
import leafra.sitrmoo.com.ui.theme.LeafraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用只有奶油白浅色主题：固定使用深色状态栏 / 导航栏图标，
        // 避免系统深色模式下浅色图标叠在浅色纸张背景上。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        // 仓库由 Application 持有；Composable 只依赖仓库，不直接接触 Room。
        val repository = (application as LeafraApplication).repository

        setContent {
            LeafraTheme {
                var openedNotebookId by rememberSaveable { mutableStateOf<String?>(null) }
                val openNotebookId = openedNotebookId

                if (openNotebookId == null) {
                    HomeScreen(
                        repository = repository,
                        onOpenNotebook = { openedNotebookId = it },
                    )
                } else {
                    NotebookScreen(
                        repository = repository,
                        notebookId = openNotebookId,
                        onBack = { openedNotebookId = null },
                    )
                }
            }
        }
    }
}
