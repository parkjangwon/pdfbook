package org.parkjw.apps.pdfbook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val volumeDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val volumeDeltaFlow: SharedFlow<Int> = volumeDeltas
    @Volatile private var documentOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App(volumeDeltaFlow) { open -> documentOpen = open }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!documentOpen) return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDeltas.tryEmit(1)
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeDeltas.tryEmit(-1)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!documentOpen) return super.onKeyUp(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }
}

@Composable
private fun App(volumeDelta: SharedFlow<Int>, onDocumentOpenChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val store = remember { BookStore(context) }
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var openUri by rememberSaveable { mutableStateOf<String?>(null) }
    var listing by remember { mutableStateOf<Listing?>(null) }
    var scanned by remember { mutableStateOf(false) }
    val states by store.states.collectAsState(emptyMap())

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        scope.launch {
            store.setFolder(uri.toString())
            folder = uri.toString()
        }
    }

    LaunchedEffect(Unit) {
        folder = store.folder()
        loaded = true
    }
    LaunchedEffect(folder) {
        scanned = false
        listing = folder?.let { listPdfs(context, Uri.parse(it)) }
        scanned = true
    }

    val book = listing?.books?.firstOrNull { it.uri == openUri }
    DisposableEffect(book) {
        onDocumentOpenChange(book != null)
        onDispose { onDocumentOpenChange(false) }
    }
    BackHandler(enabled = book != null) { openUri = null }

    when {
        !loaded || (folder != null && !scanned) -> Unit
        book != null -> ReaderRoute(book, store, volumeDelta)
        folder == null -> FolderPickScreen(
            illustration = R.drawable.es_pick_folder,
            title = stringResource(R.string.es_pick_folder_title),
            body = stringResource(R.string.es_pick_folder_body),
            buttonLabel = stringResource(R.string.es_pick_folder_action),
            onPick = { pickFolder.launch(null) },
        )
        listing == null -> FolderPickScreen(
            illustration = R.drawable.es_permission_lost,
            title = stringResource(R.string.es_permission_lost_title),
            body = stringResource(R.string.es_permission_lost_body),
            buttonLabel = stringResource(R.string.es_permission_lost_action),
            onPick = { pickFolder.launch(null) },
        )
        listing!!.books.isEmpty() -> FolderPickScreen(
            illustration = R.drawable.es_empty_folder,
            title = stringResource(R.string.es_empty_folder_title),
            body = stringResource(R.string.es_empty_folder_body),
            buttonLabel = stringResource(R.string.es_empty_folder_action),
            onPick = { pickFolder.launch(null) },
        )
        else -> LibraryScreen(listing!!, states, onOpen = { openUri = it.uri })
    }
}
