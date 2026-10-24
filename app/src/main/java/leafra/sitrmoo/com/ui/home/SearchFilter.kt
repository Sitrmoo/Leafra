package leafra.sitrmoo.com.ui.home

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import leafra.sitrmoo.com.data.model.PageResult

private val DATE_SEARCH_PATTERNS = listOf(
    "yyyy-MM-dd",
    "yyyy/M/d",
    "yyyy.MM.dd",
    "yyyy年M月d日",
    "yyyy-MM",
    "yyyy年M月",
    "M月d日",
)

/**
 * 搜索匹配：日记本名称、页面描述、页面日期。
 * 日期用多种常见格式做包含匹配（例如 2024、2024-10、10月24日）。
 */
fun PageResult.matchesSearchQuery(rawQuery: String): Boolean {
    val query = rawQuery.trim()
    if (query.isEmpty()) return false

    if (notebookName.contains(query, ignoreCase = true)) return true
    if (!description.isNullOrBlank() && description.contains(query, ignoreCase = true)) return true

    val date = entryDate ?: return false
    return DATE_SEARCH_PATTERNS.any { pattern ->
        SimpleDateFormat(pattern, Locale.CHINA)
            .format(Date(date))
            .contains(query, ignoreCase = true)
    }
}
