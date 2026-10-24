package leafra.sitrmoo.com.ui.notebook

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.model.DiaryPage

private const val MAX_DESCRIPTION_LENGTH = 200

/**
 * 页面编辑对话框：描述、日期、收藏、空白页替换、删除。
 * 所有字段都以页面 ID 为单位保存，重排后不会错位。
 */
@Composable
fun PageEditDialog(
    page: DiaryPage,
    onDismiss: () -> Unit,
    onSave: (description: String?, entryDate: Long?, isFavorite: Boolean) -> Unit,
    onReplaceImage: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var description by remember(page.id) { mutableStateOf(page.description.orEmpty()) }
    var entryDate by remember(page.id) { mutableStateOf(page.entryDate) }
    var favorite by remember(page.id) { mutableStateOf(page.isFavorite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.page_edit_title, page.pageNumber),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (page.isBlank) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.blank_page_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            OutlinedButton(
                                onClick = onReplaceImage,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.page_replace_image))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { value ->
                        if (value.length <= MAX_DESCRIPTION_LENGTH) {
                            description = value
                        }
                    },
                    label = { Text(stringResource(R.string.page_description)) },
                    placeholder = { Text(stringResource(R.string.page_description_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column {
                    Text(
                        text = stringResource(R.string.page_date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entryDate?.let(::formatEntryDate)
                                ?: stringResource(R.string.page_date_unset),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                val base = Calendar.getInstance().apply {
                                    timeInMillis = entryDate ?: System.currentTimeMillis()
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        entryDate = Calendar.getInstance().apply {
                                            clear()
                                            set(year, month, day)
                                        }.timeInMillis
                                    },
                                    base.get(Calendar.YEAR),
                                    base.get(Calendar.MONTH),
                                    base.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                        ) {
                            Text(stringResource(R.string.page_pick_date))
                        }
                    }
                    if (entryDate != null) {
                        TextButton(onClick = { entryDate = null }) {
                            Text(stringResource(R.string.page_clear_date))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.page_favorite),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = favorite,
                        onCheckedChange = { favorite = it },
                    )
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.page_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        description.trim().takeIf { it.isNotEmpty() },
                        entryDate,
                        favorite,
                    )
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
