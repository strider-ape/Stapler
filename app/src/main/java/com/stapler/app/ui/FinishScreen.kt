package com.stapler.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.stapler.app.data.Margin
import com.stapler.app.data.PageSize
import com.stapler.app.data.PdfSaver
import com.stapler.app.data.Quality

/** The last step: check the preview, tune page size, margins and quality, then name and save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(vm: PdfViewModel, onSaveAs: () -> Unit) {
    val focus = LocalFocusManager.current
    val pager = rememberPagerState { vm.pages.size }
    val canSave = vm.fileNameError == null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.goTo(Screen.Arrange) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to pages")
                    }
                },
                title = { Text("Create PDF") },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                ) {
                    // Kept beside the buttons so the destination is always visible, not scrolled away.
                    Text(
                        when {
                            // The name field shows the error; a blank line keeps the bar from jumping.
                            !canSave -> " "
                            PdfSaver.canSaveToDownloads -> "Saves as ${vm.cleanFileName} in Downloads/${PdfSaver.FOLDER}"
                            else -> "You'll choose where to save ${vm.cleanFileName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { focus.clearFocus(); onSaveAs() },
                            enabled = canSave,
                            modifier = Modifier.height(52.dp),
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save to…")
                        }
                        Button(
                            onClick = {
                                focus.clearFocus()
                                if (PdfSaver.canSaveToDownloads) vm.createPdf() else onSaveAs()
                            },
                            enabled = canSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create PDF", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 56.dp),
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 12.dp),
            ) { index ->
                vm.pages.getOrNull(index)?.let { PagePreview(it, vm.options, Modifier.fillMaxSize()) }
            }
            Text(
                "Page ${pager.currentPage + 1} of ${vm.pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Section("Page size") {
                    Choice(PageSize.entries, vm.options.pageSize, { it.label }) { vm.options = vm.options.copy(pageSize = it) }
                }
                Section("White margins") {
                    Choice(Margin.entries, vm.options.margin, { it.label }) { vm.options = vm.options.copy(margin = it) }
                }
                Section("Quality") {
                    Choice(Quality.entries, vm.options.quality, { it.label }) { vm.options = vm.options.copy(quality = it) }
                    Text(
                        vm.options.quality.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                    )
                }
                Section("File name") {
                    FileNameField(vm, focus)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    vm.progress?.let { done -> ProgressDialog(done, vm.pages.size) }
    vm.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.error = null },
            title = { Text("Couldn't create the PDF") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun FileNameField(vm: PdfViewModel, focus: FocusManager) {
    val error = vm.fileNameError
    OutlinedTextField(
        value = vm.fileName,
        onValueChange = { vm.fileName = it },
        singleLine = true,
        suffix = { Text(".pdf") },
        isError = error != null,
        trailingIcon = {
            if (vm.fileName.isNotEmpty()) {
                IconButton(onClick = { vm.fileName = "" }) { Icon(Icons.Outlined.Close, contentDescription = "Clear name") }
            }
        },
        supportingText = error?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, item ->
            SegmentedButton(
                selected = item == selected,
                onClick = { onSelect(item) },
                shape = SegmentedButtonDefaults.itemShape(i, items.size),
                label = { Text(label(item), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun ProgressDialog(done: Int, total: Int) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Creating PDF") },
        text = {
            Column {
                Text(if (done < total) "Adding page ${done + 1} of $total" else "Finishing up…")
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
    )
}
