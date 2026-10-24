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
        setContent {
            LeafraTheme {
                HomeScreen()
            }
        }
    }
}
