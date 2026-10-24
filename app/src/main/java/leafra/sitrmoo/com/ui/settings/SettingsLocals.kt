package leafra.sitrmoo.com.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局“是否显示页面日期”开关。
 * 只影响列表/大纲中的展示，不会修改数据库中已保存的日期。
 */
val LocalShowPageDates = staticCompositionLocalOf { true }
