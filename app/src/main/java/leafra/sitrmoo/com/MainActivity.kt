package leafra.sitrmoo.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import leafra.sitrmoo.com.ui.home.HomeScreen
import leafra.sitrmoo.com.ui.theme.LeafraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 仓库由 Application 持有；Composable 只依赖仓库，不直接接触 Room。
        val repository = (application as LeafraApplication).repository

        setContent {
            LeafraTheme {
                HomeScreen(repository = repository)
            }
        }
    }
}
