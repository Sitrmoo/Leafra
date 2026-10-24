package leafra.sitrmoo.com

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import leafra.sitrmoo.com.data.settings.AppSettings
import leafra.sitrmoo.com.data.settings.ThemeMode
import leafra.sitrmoo.com.ui.about.AboutScreen
import leafra.sitrmoo.com.ui.favorites.FavoritesScreen
import leafra.sitrmoo.com.ui.home.HomeScreen
import leafra.sitrmoo.com.ui.notebook.NotebookScreen
import leafra.sitrmoo.com.ui.settings.LocalShowPageDates
import leafra.sitrmoo.com.ui.settings.SettingsScreen
import leafra.sitrmoo.com.ui.theme.LeafraTheme

private enum class AppScreen {
    SHELF,
    NOTEBOOK,
    FAVORITES,
    SETTINGS,
    ABOUT,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as LeafraApplication
        val repository = application.repository
        val settingsRepository = application.settingsRepository

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            val context = view.context
            SideEffect {
                val window = context.findActivity()?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            CompositionLocalProvider(LocalShowPageDates provides settings.showPageDates) {
                LeafraTheme(darkTheme = darkTheme) {
                    var screenName by rememberSaveable { mutableStateOf(AppScreen.SHELF.name) }
                    var openedNotebookId by rememberSaveable { mutableStateOf<String?>(null) }
                    var openedPageId by rememberSaveable { mutableStateOf<String?>(null) }

                    val screen = runCatching { AppScreen.valueOf(screenName) }
                        .getOrDefault(AppScreen.SHELF)

                    fun openNotebook(notebookId: String, pageId: String?) {
                        openedNotebookId = notebookId
                        openedPageId = pageId
                        screenName = AppScreen.NOTEBOOK.name
                    }

                    fun backToShelf() {
                        screenName = AppScreen.SHELF.name
                        openedNotebookId = null
                        openedPageId = null
                    }

                    when (screen) {
                        AppScreen.SHELF -> HomeScreen(
                            repository = repository,
                            onOpenNotebook = { notebookId, pageId ->
                                openNotebook(notebookId, pageId)
                            },
                            onOpenFavorites = {
                                screenName = AppScreen.FAVORITES.name
                            },
                            onOpenSettings = {
                                screenName = AppScreen.SETTINGS.name
                            },
                            onOpenAbout = {
                                screenName = AppScreen.ABOUT.name
                            },
                        )

                        AppScreen.NOTEBOOK -> {
                            val notebookId = openedNotebookId
                            if (notebookId == null) {
                                LaunchedEffect(Unit) { backToShelf() }
                            } else {
                                NotebookScreen(
                                    repository = repository,
                                    notebookId = notebookId,
                                    initialPageId = openedPageId,
                                    onBack = { backToShelf() },
                                )
                            }
                        }

                        AppScreen.FAVORITES -> FavoritesScreen(
                            repository = repository,
                            onBack = { backToShelf() },
                            onOpenPage = { notebookId, pageId ->
                                openNotebook(notebookId, pageId)
                            },
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            settingsRepository = settingsRepository,
                            onBack = { backToShelf() },
                        )

                        AppScreen.ABOUT -> AboutScreen(
                            onBack = { backToShelf() },
                        )
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
