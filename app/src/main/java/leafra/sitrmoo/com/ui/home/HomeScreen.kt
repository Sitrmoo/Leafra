package leafra.sitrmoo.com.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.CreateNotebookFailure
import leafra.sitrmoo.com.data.model.CreateNotebookResult
import leafra.sitrmoo.com.data.model.CoverStyle
import leafra.sitrmoo.com.data.model.Notebook
import leafra.sitrmoo.com.ui.components.NotebookCover
import leafra.sitrmoo.com.ui.components.coverStyleLabelRes
import leafra.sitrmoo.com.ui.theme.PaperBackground
import leafra.sitrmoo.com.ui.theme.PaperCream

/** Photo Picker 单次最多选择的初始页面数量。 */
private const val MAX_INITIAL_IMAGES = 30

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

    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var newStyle by rememberSaveable { mutableIntStateOf(CoverStyle.SAGE.id) }
    var newImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCreating by remember { mutableStateOf(false) }

    var actionsTarget by remember { mutableStateOf<Notebook?>(null) }
    var renameTarget by remember { mutableStateOf<Notebook?>(null) }
    var coverTarget by remember { mutableStateOf<Notebook?>(null) }
    var deleteTarget by remember { mutableStateOf<Notebook?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val initialImagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_INITIAL_IMAGES),
    ) { uris ->
        if (uris.isNotEmpty()) {
            newImages = uris
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ShelfTopBar(
                onSearchClick = {
                    showMessage(context.getString(R.string.search_in_development))
                },
                onNewDiary = { showCreateDialog = true },
                onFavorites = {
                    showMessage(context.getString(R.string.favorites_in_development))
                },
                onSettings = {
                    showMessage(context.getString(R.string.settings_in_development))
                },
                onAbout = { showAbout = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PaperBackground, PaperCream)))
                .padding(innerPadding),
        ) {
            if (notebooks.isEmpty()) {
                EmptyShelf(onCreate = { showCreateDialog = true })
            } else {
                NotebookShelf(
                    notebooks = notebooks,
                    onNotebookClick = { actionsTarget = it },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateNotebookDialog(
            name = newName,
            onNameChange = { newName = it },
            selectedStyle = newStyle,
            onStyleChange = { newStyle = it },
            imageCount = newImages.size,
            isBusy = isCreating,
            onPickImages = {
                initialImagesPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDismiss = {
                if (!isCreating) {
                    showCreateDialog = false
                    newName = ""
                    newStyle = CoverStyle.SAGE.id
                    newImages = emptyList()
                }
            },
            onCreate = {
                if (!isCreating) {
                    isCreating = true
                    scope.launch {
                        try {
                            when (val result = repository.createNotebook(newName, newStyle, newImages)) {
                                is CreateNotebookResult.Success -> {
                                    showCreateDialog = false
                                    newName = ""
                                    newStyle = CoverStyle.SAGE.id
                                    newImages = emptyList()
                                    val message = if (result.importedCount > 0) {
                                        context.getString(
                                            R.string.notebook_created_with_pages,
                                            result.notebook.name,
                                            result.importedCount,
                                        )
                                    } else {
                                        context.getString(
                                            R.string.notebook_created,
                                            result.notebook.name,
                                        )
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }

                                is CreateNotebookResult.Failure -> {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            createFailureMessage(result.reason),
                                        ),
                                    )
                                }
                            }
                        } finally {
                            isCreating = false
                        }
                    }
                }
            },
        )
    }

    actionsTarget?.let { notebook ->
        NotebookActionsDialog(
            notebook = notebook,
            onDismiss = { actionsTarget = null },
            onRename = {
                actionsTarget = null
                renameTarget = notebook
            },
            onChangeCover = {
                actionsTarget = null
                coverTarget = notebook
            },
            onDelete = {
                actionsTarget = null
                deleteTarget = notebook
            },
        )
    }

    renameTarget?.let { notebook ->
        RenameNotebookDialog(
            notebook = notebook,
            onDismiss = { renameTarget = null },
            onConfirm = { updatedName ->
                renameTarget = null
                scope.launch {
                    repository.renameNotebook(notebook.id, updatedName)
                        .onSuccess {
                            snackbarHostState.showSnackbar(
                                context.getString(
                                    R.string.notebook_renamed,
                                    updatedName.trim(),
                                ),
                            )
                        }
                        .onFailure {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.rename_failed),
                            )
                        }
                }
            },
        )
    }

    coverTarget?.let { notebook ->
        CoverStyleDialog(
            notebook = notebook,
            onDismiss = { coverTarget = null },
            onConfirm = { styleId ->
                coverTarget = null
                scope.launch {
                    repository.updateCoverStyle(notebook.id, styleId)
                        .onSuccess {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.cover_updated),
                            )
                        }
                        .onFailure {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.cover_update_failed),
                            )
                        }
                }
            },
        )
    }

    deleteTarget?.let { notebook ->
        DeleteNotebookDialog(
            notebook = notebook,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
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

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun NotebookShelf(
    notebooks: List<Notebook>,
    onNotebookClick: (Notebook) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ShelfHeader(notebookCount = notebooks.size)
        }

        items(items = notebooks, key = { notebook -> notebook.id }) { notebook ->
            NotebookCard(
                notebook = notebook,
                onClick = { onNotebookClick(notebook) },
            )
        }
    }
}

@Composable
private fun ShelfHeader(notebookCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.shelf_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.shelf_count, notebookCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.shelf_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        NotebookCover(
            notebook = notebook,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = notebook.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.notebook_page_count, notebook.pageCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyShelf(onCreate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 32.dp),
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
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_shelf_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.create_notebook_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateNotebookDialog(
    name: String,
    onNameChange: (String) -> Unit,
    selectedStyle: Int,
    onStyleChange: (Int) -> Unit,
    imageCount: Int,
    isBusy: Boolean,
    onPickImages: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    val untitledName = stringResource(R.string.untitled_notebook)
    val previewNotebook = Notebook(
        id = "preview",
        name = name.ifBlank { untitledName },
        coverImagePath = null,
        coverStyle = selectedStyle,
        pageCount = imageCount,
        createdAt = 0L,
        updatedAt = 0L,
        lastReadPageId = null,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_notebook_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        if (value.length <= MAX_NOTEBOOK_NAME_LENGTH) {
                            onNameChange(value)
                        }
                    },
                    enabled = !isBusy,
                    singleLine = true,
                    label = { Text(stringResource(R.string.notebook_name_label)) },
                    placeholder = { Text(stringResource(R.string.notebook_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.cover_style_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CoverStyle.entries.forEach { style ->
                        val selected = style.id == selectedStyle
                        Column(
                            modifier = Modifier.width(92.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            NotebookCover(
                                notebook = previewNotebook.copy(coverStyle = style.id),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .clickable(enabled = !isBusy) { onStyleChange(style.id) },
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(coverStyleLabelRes(style)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickImages,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (imageCount == 0) {
                            stringResource(R.string.select_initial_images)
                        } else {
                            stringResource(R.string.initial_images_selected, imageCount)
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Text(
                    text = stringResource(R.string.initial_images_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !isBusy,
                onClick = onCreate,
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
private fun NotebookActionsDialog(
    notebook: Notebook,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = notebook.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.notebook_actions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_rename))
                }
                TextButton(
                    onClick = onChangeCover,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_change_cover))
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun RenameNotebookDialog(
    notebook: Notebook,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(notebook.id) { mutableStateOf(notebook.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_notebook_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    if (input.length <= MAX_NOTEBOOK_NAME_LENGTH) {
                        value = input
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.notebook_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() && value.trim() != notebook.name,
                onClick = { onConfirm(value) },
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

@Composable
private fun CoverStyleDialog(
    notebook: Notebook,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember(notebook.id) { mutableIntStateOf(notebook.coverStyle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cover_style_title)) },
        text = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoverStyle.entries.forEach { style ->
                    val isSelected = style.id == selected
                    Column(
                        modifier = Modifier.width(104.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NotebookCover(
                            notebook = notebook.copy(coverStyle = style.id),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable { selected = style.id },
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(coverStyleLabelRes(style)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

private fun createFailureMessage(reason: CreateNotebookFailure): Int = when (reason) {
    CreateNotebookFailure.INVALID_NAME -> R.string.create_failed_name
    CreateNotebookFailure.COPY_FAILED -> R.string.create_failed_copy
    CreateNotebookFailure.DATABASE_FAILED -> R.string.create_failed_database
    CreateNotebookFailure.UNKNOWN -> R.string.create_failed_generic
}
