package leafra.sitrmoo.com.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.data.model.ImportFailureReason
import leafra.sitrmoo.com.data.model.ImportResult
import leafra.sitrmoo.com.data.model.Notebook
import leafra.sitrmoo.com.ui.theme.PaperBackground
import leafra.sitrmoo.com.ui.theme.PaperCream

/** Photo Picker 单次最多选择的图片数量。 */
private const val MAX_SELECTED_IMAGES = 30

/** 日记本名称最大长度，与仓库层校验保持一致。 */
private const val MAX_NOTEBOOK_NAME_LENGTH = 40

@Composable
fun HomeScreen(
    repository: DiaryRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notebooks by repository.notebooks.collectAsState(initial = emptyList())
    var selectedNotebookId by rememberSaveable { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Notebook?>(null) }

    val selectedNotebook = notebooks.firstOrNull { it.id == selectedNotebookId }

    // 选中日记本变化时切换到对应的页面流；没有选中时给一个空流。
    val pagesFlow = remember(selectedNotebookId) {
        selectedNotebookId?.let(repository::pages) ?: flowOf<List<DiaryPage>>(emptyList())
    }
    val pages by pagesFlow.collectAsState(initial = emptyList())

    // 列表异步加载完成、或选中的日记本被删除后，自动选中第一本。
    LaunchedEffect(notebooks, selectedNotebookId) {
        if (notebooks.none { it.id == selectedNotebookId }) {
            selectedNotebookId = notebooks.firstOrNull()?.id
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTED_IMAGES),
    ) { uris ->
        val target = selectedNotebookId
        when {
            target == null -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.no_notebook_selected))
            }

            uris.isEmpty() -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.import_empty))
            }

            else -> {
                isImporting = true
                scope.launch {
                    try {
                        val result = repository.importImages(target, uris)
                        snackbarHostState.showSnackbar(result.toMessage(context))
                    } finally {
                        isImporting = false
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNotebookDialog(
            isBusy = isCreating,
            onDismiss = { if (!isCreating) showCreateDialog = false },
            onCreate = { rawName ->
                isCreating = true
                scope.launch {
                    try {
                        repository.createNotebook(rawName)
                            .onSuccess { created ->
                                selectedNotebookId = created.id
                                showCreateDialog = false
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.notebook_created, created.name),
                                )
                            }
                            .onFailure { error ->
                                snackbarHostState.showSnackbar(
                                    error.message
                                        ?: context.getString(R.string.notebook_create_failed),
                                )
                            }
                    } finally {
                        isCreating = false
                    }
                }
            },
        )
    }

    pendingDelete?.let { notebook ->
        DeleteNotebookDialog(
            notebook = notebook,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    repository.deleteNotebook(notebook.id)
                        .onSuccess {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.notebook_deleted),
                            )
                        }
                        .onFailure {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.notebook_delete_failed),
                            )
                        }
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PaperBackground, PaperCream),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (notebooks.isEmpty()) {
                    EmptyShelfCard()
                } else {
                    NotebookList(
                        notebooks = notebooks,
                        selectedNotebookId = selectedNotebookId,
                        onSelect = { selectedNotebookId = it },
                        onDelete = { pendingDelete = it },
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PageStrip(
                        pages = pages,
                        repository = repository,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    enabled = selectedNotebook != null && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.import_images),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    enabled = !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.create_notebook),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.reading_mode_placeholder),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.reading_mode),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.photo_picker_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EmptyShelfCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.empty_shelf_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.empty_shelf_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_shelf_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NotebookList(
    notebooks: List<Notebook>,
    selectedNotebookId: String?,
    onSelect: (String) -> Unit,
    onDelete: (Notebook) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.notebook_list_title, notebooks.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        notebooks.forEach { notebook ->
            val selected = notebook.id == selectedNotebookId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(notebook.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notebook.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.notebook_page_count,
                                notebook.pageCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    TextButton(onClick = { onDelete(notebook) }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageStrip(
    pages: List<DiaryPage>,
    repository: DiaryRepository,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.page_strip_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (pages.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_pages_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = pages, key = { page -> page.id }) { page ->
                        PageThumbnail(page = page, repository = repository)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    page: DiaryPage,
    repository: DiaryRepository,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, page.imagePath) {
        value = page.imagePath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching { decodeSampledBitmap(repository.imageFile(path)) }.getOrNull()
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(width = 96.dp, height = 128.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            ),
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(
                        R.string.page_image_description,
                        page.pageNumber,
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (page.isBlank) {
                            stringResource(R.string.blank_page)
                        } else {
                            stringResource(R.string.image_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.page_number, page.pageNumber),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateNotebookDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_notebook)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    if (value.length <= MAX_NOTEBOOK_NAME_LENGTH) {
                        name = value
                    }
                },
                enabled = !isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.notebook_name_label)) },
                placeholder = { Text(stringResource(R.string.notebook_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !isBusy,
                onClick = { onCreate(name) },
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isBusy,
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DeleteNotebookDialog(
    notebook: Notebook,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_notebook_title)) },
        text = {
            Text(
                stringResource(
                    R.string.delete_notebook_message,
                    notebook.name,
                    notebook.pageCount,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** 把导入结果转换成界面文案，避免 Composable 里散落 when 分支。 */
private fun ImportResult.toMessage(context: Context): String = when (this) {
    is ImportResult.Success -> context.getString(
        R.string.import_success,
        importedCount,
        firstPageNumber,
    )

    ImportResult.EmptySelection -> context.getString(R.string.import_empty)

    is ImportResult.Failure -> context.getString(
        when (reason) {
            ImportFailureReason.NOTEBOOK_NOT_FOUND -> R.string.import_failed_notebook
            ImportFailureReason.COPY_FAILED -> R.string.import_failed_copy
            ImportFailureReason.DATABASE_FAILED -> R.string.import_failed_database
            ImportFailureReason.UNKNOWN -> R.string.import_failed_generic
        },
    )
}

/** 按需下采样解码缩略图；失败或文件缺失时返回 null，由界面显示占位。 */
private fun decodeSampledBitmap(file: File, maxSize: Int = 320): ImageBitmap? {
    if (!file.isFile) return null

    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxSize || bounds.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
    } catch (t: Throwable) {
        null
    }
}
