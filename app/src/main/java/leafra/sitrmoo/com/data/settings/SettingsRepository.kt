package leafra.sitrmoo.com.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** 主题模式：亮色、深色或跟随系统。 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val showPageDates: Boolean = true,
)

private val Context.settingsDataStore by preferencesDataStore(name = "leafra_settings")

/** 使用 DataStore 持久化本地设置；默认是鼠尾草绿 + 奶油白亮色主题。 */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext

    val settings: Flow<AppSettings> = appContext.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = runCatching {
                    ThemeMode.valueOf(preferences[THEME_KEY] ?: ThemeMode.LIGHT.name)
                }.getOrDefault(ThemeMode.LIGHT),
                showPageDates = preferences[SHOW_PAGE_DATES_KEY] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        // 设置写入不可取消：用户点选后立刻返回也不会丢失。
        withContext(NonCancellable) {
            appContext.settingsDataStore.edit { preferences ->
                preferences[THEME_KEY] = mode.name
            }
        }
    }

    suspend fun setShowPageDates(show: Boolean) {
        withContext(NonCancellable) {
            appContext.settingsDataStore.edit { preferences ->
                preferences[SHOW_PAGE_DATES_KEY] = show
            }
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val SHOW_PAGE_DATES_KEY = booleanPreferencesKey("show_page_dates")
    }
}
