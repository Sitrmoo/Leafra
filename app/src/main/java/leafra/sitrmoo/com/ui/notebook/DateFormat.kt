package leafra.sitrmoo.com.ui.notebook

import java.text.DateFormat
import java.util.Date

/** 页面日期的本地化显示；null 表示未设置。 */
internal fun formatEntryDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
