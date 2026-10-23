package com.stapler.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Hosts the four steps: pick, arrange, finish (options + name), done. */
@Composable
fun StaplerApp(vm: PdfViewModel) {
    val images = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
        vm.addImages(it)
    }
    var replacing by remember { mutableStateOf<Long?>(null) }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = replacing
        if (uri != null && id != null) vm.replaceImage(id, uri)
        replacing = null
    }
    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.createPdf(uri)
    }

    BackHandler(enabled = vm.screen != Screen.Home && vm.progress == null) { vm.back() }

    AnimatedContent(
        targetState = vm.screen,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val dir = if (forward) 1 else -1
            (slideInHorizontally { it / 5 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 5 * dir } + fadeOut())
        },
        label = "screen",
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(onPick = { pickMany.launch(images) })
            Screen.Arrange -> ArrangeScreen(
                vm = vm,
                onAdd = { pickMany.launch(images) },
                onReplace = { id ->
                    replacing = id
                    pickOne.launch(images)
                },
            )
            Screen.Finish -> FinishScreen(vm = vm, onSaveAs = { saveAs.launch(vm.cleanFileName) })
            Screen.Done -> DoneScreen(vm = vm)
        }
    }
}
