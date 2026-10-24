package leafra.sitrmoo.com.ui.notebook

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import leafra.sitrmoo.com.R
import leafra.sitrmoo.com.data.DiaryRepository
import leafra.sitrmoo.com.data.model.DiaryPage
import leafra.sitrmoo.com.data.model.ImportResult
import leafra.sitrmoo.com.ui.components.FavoriteStar
import leafra.sitrmoo.com.ui.components.BlankPageStyle
import leafra.sitrmoo.com.ui.components.PageImage
import leafra.sitrmoo.com.ui.theme.PaperBackground
import leafra.sitrmoo.com.ui.theme.PaperCream

/** 日记本内的三种视图；立体视图本阶段只做双页占位。 */
private enum class EditorView(val labelRes: Int) {
    STACK(R.string.view_stack),
    SINGLE(R.string.view_single),
    OUTLINE(R.string.view_outline),
}

@Composable
fun NotebookScreen(
    repository: DiaryRepository,
    notebookId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notebooks by repository.notebooks.collectAsState(initial = null)
    val notebook = notebooks?.firstOrNull { it.id == notebookId }
    val pages by remember(notebookId) {
        repository.pages(notebookId)
    }.collectAsState(initial = emptyList())

    var isEditing by rememberSaveable(notebookId) { mutableStateOf(false) }
    var viewOrdinal by rememberSaveable(notebookId) {
        mutableIntStateOf(EditorView.SINGLE.ordinal)
    }
    val editorView = EditorView.entries[
        viewOrdinal.coerceIn(0, EditorView.entries.lastIndex)
    ]

    var editingPage by remember { mutableStateOf<DiaryPage?>(null) }
    var deletingPage by remember { mutableStateOf<DiaryPage?>(null) }
    var replacingPageId by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    if (notebook == null) {
        LaunchedEffect(notebooks) {
            if (notebooks != null) onBack()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    BackHandler(onBack = onBack)

    val appendImagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_APPEND_IMAGES),
    ) { uris ->
        when {
            uris.isEmpty() -> notify(context.getString(R.string.append_empty))
            isBusy -> Unit
            else -> {
                isBusy = true
                scope.launch {
                    try {
                        when (val result = repository.importImages(notebookId, uris)) {
                            is ImportResult.Success -> notify(
                                context.getString(R.string.append_success, result.importedCount),
                            )

                            else -> notify(context.getString(R.string.append_failed))
                        }
                    } finally {
                        isBusy = false
                    }
                }
            }
        }
    }

    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val pageId = replacingPageId
        replacingPageId = null
        if (uri != null && pageId != null && !isBusy) {
            isBusy = true
            scope.launch {
                try {
                    repository.replacePlaceholderImage(notebookId, pageId, uri)
                        .onSuccess { notify(context.getString(R.string.replace_success)) }
                        .onFailure { notify(context.getString(R.string.replace_failed)) }
                } finally {
                    isBusy = false
                }
            }
        }
    }

    fun addBlankPage() {
        if (!isEditing || isBusy) return
        isBusy = true
        scope.launch {
            try {
                repository.addBlankPage(notebookId)
                    .onSuccess { notify(context.getString(R.string.blank_page_created)) }
                    .onFailure { notify(context.getString(R.string.blank_page_failed)) }
            } finally {
                isBusy = false
            }
        }
    }

    fun savePage(page: DiaryPage, description: String?, entryDate: Long?, favorite: Boolean) {
        editingPage = null
        if (isBusy) return
        isBusy = true
        scope.launch {
            try {
                repository.updatePageMeta(page.id, description, entryDate, favorite)
                    .onFailure { notify(context.getString(R.string.page_action_failed)) }
            } finally {
                isBusy = false
            }
        }
    }

    fun toggleFavorite(page: DiaryPage) {
        // 收藏是唯一允许在阅读模式使用的页面操作。
        if (isBusy) return
        isBusy = true
        scope.launch {
            try {
                repository.setPageFavorite(page.id, !page.isFavorite)
                    .onFailure { notify(context.getString(R.string.page_action_failed)) }
            } finally {
                isBusy = false
            }
        }
    }

    fun deletePage(page: DiaryPage) {
        if (isBusy) return
        isBusy = true
        scope.launch {
            try {
                repository.deletePage(notebookId, page.id)
                    .onSuccess { notify(context.getString(R.string.page_deleted)) }
                    .onFailure { notify(context.getString(R.string.page_action_failed)) }
            } finally {
                isBusy = false
            }
        }
    }

    val reorderAction: suspend (List<String>) -> Boolean = { orderedIds ->
        // 即使用户拖动后立刻返回书架，也把排序写入完成，避免顺序丢失。
        val result = withContext(NonCancellable) {
            repository.reorderPages(notebookId, orderedIds)
        }
        if (result.isFailure) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.reorder_failed)) }
        }
        result.isSuccess
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NotebookTopBar(
                title = notebook.name,
                pageCount = pages.size,
                isEditing = isEditing,
                onBack = onBack,
                onToggleEdit = { isEditing = !isEditing },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PaperBackground, PaperCream)))
                .padding(innerPadding),
        ) {
            EditorViewTabs(
                selected = editorView,
                onSelect = { viewOrdinal = it.ordinal },
            )

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (editorView) {
                    EditorView.STACK -> StackPagesView(
                        notebookId = notebookId,
                        pages = pages,
                        repository = repository,
                        isEditing = isEditing,
                        onEditPage = { page -> if (isEditing) editingPage = page },
                    )

                    EditorView.SINGLE -> SinglePageView(
                        pages = pages,
                        repository = repository,
                        isEditing = isEditing,
                        initialPageId = notebook.lastReadPageId,
                        onPageChanged = { page ->
                            repository.setLastReadPage(notebookId, page.id)
                        },
                        onEditPage = { page -> editingPage = page },
                        onToggleFavorite = { page -> toggleFavorite(page) },
                    )

                    EditorView.OUTLINE -> OutlineView(
                        notebookId = notebookId,
                        pages = pages,
                        repository = repository,
                        isEditing = isEditing,
                        isBusy = isBusy,
                        onEditPage = { page -> editingPage = page },
                        onToggleFavorite = { page -> toggleFavorite(page) },
                        onAddImages = {
                            if (!isBusy) {
                                appendImagesPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            }
                        },
                        onAddBlankPage = { addBlankPage() },
                        onReorder = reorderAction,
                    )
                }
            }
        }
    }

    editingPage?.let { page ->
        PageEditDialog(
            page = page,
            onDismiss = { editingPage = null },
            onSave = { description, entryDate, favorite ->
                savePage(page, description, entryDate, favorite)
            },
            onReplaceImage = {
                editingPage = null
                if (page.isBlank && !isBusy) {
                    replacingPageId = page.id
                    replaceImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            },
            onDelete = {
                editingPage = null
                deletingPage = page
            },
        )
    }

    deletingPage?.let { page ->
        AlertDialog(
            onDismissRequest = { deletingPage = null },
            title = { Text(stringResource(R.string.page_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.page_delete_confirm_message,
                        page.pageNumber,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingPage = null
                        deletePage(page)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPage = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NotebookTopBar(
    title: String,
    pageCount: Int,
    isEditing: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    BackArrow(color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Image(
                    painter = painterResource(R.drawable.ic_brand_mark),
                    contentDescription = null,
                    modifier = Modifier.size(width = 22.dp, height = 29.dp),
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.notebook_page_count, pageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        ReaderMoreGlyph(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isEditing) R.string.editor_done else R.string.editor_edit,
                                    ),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_back_to_shelf)) },
                            onClick = {
                                menuExpanded = false
                                onBack()
                            },
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun ReaderMoreGlyph(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val radius = 1.7.dp.toPx()
        val centerX = size.width / 2f
        listOf(0.28f, 0.5f, 0.72f).forEach { fraction ->
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(centerX, size.height * fraction),
            )
        }
    }
}

@Composable
private fun BackArrow(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.8.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.70f, size.height * 0.18f),
            end = Offset(size.width * 0.30f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.30f, size.height * 0.50f),
            end = Offset(size.width * 0.70f, size.height * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun EditorViewTabs(
    selected: EditorView,
    onSelect: (EditorView) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorView.entries.forEach { view ->
            val isSelected = view == selected
            Surface(
                onClick = { onSelect(view) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                ),
            ) {
                Text(
                    text = stringResource(view.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SinglePageView(
    pages: List<DiaryPage>,
    repository: DiaryRepository,
    isEditing: Boolean,
    initialPageId: String?,
    onPageChanged: suspend (DiaryPage) -> Unit,
    onEditPage: (DiaryPage) -> Unit,
    onToggleFavorite: (DiaryPage) -> Unit,
) {
    if (pages.isEmpty()) {
        EmptyNotebook(isEditing = isEditing)
        return
    }

    // 进度按页面 ID 恢复：页面被重排后仍能找到原页；被删除时回退到第一页。
    val initialIndex = pages.indexOfFirst { it.id == initialPageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()
    var isZoomed by remember { mutableStateOf(false) }
    var zoomResetToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
        zoomResetToken += 1
        pages.getOrNull(pagerState.currentPage)?.let { page -> onPageChanged(page) }
    }

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage > pages.lastIndex) {
            pagerState.scrollToPage(pages.lastIndex)
        }
    }

    val currentPage = pages.getOrNull(pagerState.currentPage)
    val canGoPrevious = pagerState.currentPage > 0
    val canGoNext = pagerState.currentPage < pages.lastIndex
    val previousLabel = stringResource(R.string.previous_spread)
    val nextLabel = stringResource(R.string.next_spread)

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            userScrollEnabled = !isZoomed,
        ) { index ->
            val page = pages[index]
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                ),
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    ZoomablePage(
                        page = page,
                        repository = repository,
                        resetToken = zoomResetToken,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onZoomChanged = { zoomed ->
                            if (index == pagerState.currentPage) {
                                isZoomed = zoomed
                            }
                        },
                    )
                    Text(
                        text = stringResource(R.string.page_number, page.pageNumber),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 阅读模式允许收藏当前页，其它编辑动作只在编辑模式出现。
            FavoriteStar(
                selected = currentPage?.isFavorite == true,
                enabled = currentPage != null,
                onClick = { currentPage?.let(onToggleFavorite) },
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    if (canGoPrevious) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                enabled = canGoPrevious,
                modifier = Modifier.semantics { contentDescription = previousLabel },
            ) {
                PagerArrow(
                    left = true,
                    color = if (canGoPrevious) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }

            Text(
                text = stringResource(
                    R.string.page_indicator,
                    pagerState.currentPage + 1,
                    pages.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            )

            IconButton(
                onClick = {
                    if (canGoNext) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = canGoNext,
                modifier = Modifier.semantics { contentDescription = nextLabel },
            ) {
                PagerArrow(
                    left = false,
                    color = if (canGoNext) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isEditing && currentPage != null) {
                TextButton(onClick = { onEditPage(currentPage) }) {
                    Text(stringResource(R.string.page_edit_action))
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun StackPagesView(
    notebookId: String,
    pages: List<DiaryPage>,
    repository: DiaryRepository,
    isEditing: Boolean,
    onEditPage: (DiaryPage) -> Unit,
) {
    if (pages.isEmpty()) {
        EmptyNotebook(isEditing = isEditing)
        return
    }

    val spreadCount = (pages.size + 1) / 2
    var spread by rememberSaveable(notebookId) { mutableIntStateOf(0) }

    LaunchedEffect(spreadCount) {
        if (spread > spreadCount - 1) {
            spread = (spreadCount - 1).coerceAtLeast(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SpreadPage(
                page = pages.getOrNull(spread * 2),
                pageNumber = spread * 2 + 1,
                repository = repository,
                isEditing = isEditing,
                onEditPage = onEditPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            )

            SpreadPage(
                page = pages.getOrNull(spread * 2 + 1),
                pageNumber = spread * 2 + 2,
                repository = repository,
                isEditing = isEditing,
                onEditPage = onEditPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { spread -= 1 },
                enabled = spread > 0,
            ) {
                Text(stringResource(R.string.previous_spread))
            }

            Text(
                text = stringResource(
                    R.string.page_spread_label,
                    spread * 2 + 1,
                    minOf(spread * 2 + 2, pages.size),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = { spread += 1 },
                enabled = spread < spreadCount - 1,
            ) {
                Text(stringResource(R.string.next_spread))
            }
        }
    }
}

@Composable
private fun SpreadPage(
    page: DiaryPage?,
    pageNumber: Int,
    repository: DiaryRepository,
    isEditing: Boolean,
    onEditPage: (DiaryPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(
            enabled = isEditing && page != null,
            onClick = { if (page != null) onEditPage(page) },
        ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        ),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            if (page == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                )
            } else {
                PageImage(
                    imagePath = page.imagePath,
                    isBlank = page.isBlank,
                    repository = repository,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    maxSizePx = 1200,
                    blankStyle = BlankPageStyle.READER,
                )
                Text(
                    text = stringResource(R.string.page_number, pageNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyNotebook(isEditing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.empty_notebook_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (isEditing) {
                        R.string.empty_notebook_edit_hint
                    } else {
                        R.string.empty_notebook_read_hint
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val MAX_APPEND_IMAGES = 30
