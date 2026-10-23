package com.stapler.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stapler.app.data.PageItem
import com.stapler.app.data.PdfOptions
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrangeScreen(vm: PdfViewModel, onAdd: () -> Unit, onReplace: (Long) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var confirmDiscard by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        vm.move(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    fun delete(page: PageItem) {
        val removed = vm.remove(page.id) ?: return
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Page deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.restore(removed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { confirmDiscard = true }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Discard")
                    }
                },
                title = {
                    Column {
                        Text("Arrange pages")
                        Text(
                            pageCountLabel(vm.pages.size) + if (vm.pages.size > 1) " · hold and drag to move" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add images")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.goTo(Screen.Finish) },
                    enabled = vm.pages.isNotEmpty(),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (vm.pages.isEmpty()) {
            EmptyPages(Modifier.padding(padding), onAdd)
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(vm.pages, key = { _, page -> page.id }) { index, page ->
                ReorderableItem(reorderState, key = page.id) { dragging ->
                    PageCard(
                        page = page,
                        number = index + 1,
                        isFirst = index == 0,
                        isLast = index == vm.pages.lastIndex,
                        options = vm.options,
                        dragging = dragging,
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        ),
                        onRotate = { vm.rotate(page.id) },
                        onReplace = { onReplace(page.id) },
                        onMove = { delta -> vm.move(index, index + delta) },
                        onDelete = { delete(page) },
                    )
                }
            }
            item(key = "add") {
                AddTile(onAdd)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard these pages?") },
            text = { Text("Your selection and order will be lost. The original images are not changed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    vm.startOver()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun PageCard(
    page: PageItem,
    number: Int,
    isFirst: Boolean,
    isLast: Boolean,
    options: PdfOptions,
    dragging: Boolean,
    modifier: Modifier,
    onRotate: () -> Unit,
    onReplace: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val elevation by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "lift")
    val scale by animateFloatAsState(if (dragging) 1.04f else 1f, label = "scale")
    var menu by remember { mutableStateOf(false) }

    Surface(
        onClick = { menu = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = elevation,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                PagePreview(page, options, Modifier.fillMaxSize())
            }
            Row(
                Modifier.padding(start = 12.dp, end = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRotate) {
                    Icon(RotateClockwise, contentDescription = "Rotate page $number")
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options for page $number")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        MenuItem("Rotate", RotateClockwise) { menu = false; onRotate() }
                        MenuItem("Replace image", Icons.Outlined.FindReplace) { menu = false; onReplace() }
                        if (!isFirst) MenuItem("Move earlier", Icons.AutoMirrored.Outlined.ArrowBack) { menu = false; onMove(-1) }
                        if (!isLast) MenuItem("Move later", Icons.AutoMirrored.Outlined.ArrowForward) { menu = false; onMove(1) }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
private fun AddTile(onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text("Add images", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyPages(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("No pages left", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add images to keep building your PDF.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd) { Text("Add images") }
    }
}

fun pageCountLabel(count: Int) = if (count == 1) "1 page" else "$count pages"

/**
 * Pages always turn clockwise, so the icon must not flip in right-to-left languages the way
 * the AutoMirrored replacement does.
 */
@Suppress("DEPRECATION")
private val RotateClockwise: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Outlined.RotateRight
