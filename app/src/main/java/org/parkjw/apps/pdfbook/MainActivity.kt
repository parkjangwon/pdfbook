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
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.StartScreen
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme

class MainActivity : ComponentActivity() {
    private val volumeDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val volumeDeltaFlow: SharedFlow<Int> = volumeDeltas
    @Volatile private var documentOpen = false
    @Volatile private var volumeKeyPaging = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository(context) }
            val settingsState = settingsRepo.settings.collectAsState(initial = null)
            val settings = settingsState.value ?: AppSettings()
            val scope = rememberCoroutineScope()
            LaunchedEffect(settings.volumeKeyPaging) {
                volumeKeyPaging = settings.volumeKeyPaging
            }
            PdfBookTheme(themeMode = settings.themeMode) {
                App(
                    volumeDelta = volumeDeltaFlow,
                    settings = settings,
                    settingsReady = settingsState.value != null,
                    onSettingsChange = { next -> scope.launch { settingsRepo.update { next } } },
                    onDocumentOpenChange = { open -> documentOpen = open },
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!documentOpen || !volumeKeyPaging) return super.onKeyDown(keyCode, event)
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
        if (!documentOpen || !volumeKeyPaging) return super.onKeyUp(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }
}

@Composable
private fun App(
    volumeDelta: SharedFlow<Int>,
    settings: AppSettings,
    settingsReady: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
    onDocumentOpenChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BookStore(context) }
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var openUri by rememberSaveable { mutableStateOf<String?>(null) }
    var listing by remember { mutableStateOf<Listing?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var startApplied by rememberSaveable { mutableStateOf(false) }
    val statesState = store.states.collectAsState(initial = null)
    val states = statesState.value ?: emptyMap()

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
    LaunchedEffect(listing, scanned, settingsReady, settings.startScreen, statesState.value, startApplied) {
        if (startApplied || !scanned || !settingsReady || statesState.value == null) return@LaunchedEffect
        val books = listing?.books
        if (books != null && settings.startScreen == StartScreen.LastBook) {
            val last = sortBooks(books, states, byName = false).firstOrNull {
                (states[bookKey(it.uri, it.size)]?.openedAt ?: 0L) > 0L
            }
            if (last != null) openUri = last.uri
        }
        startApplied = true
    }

    val book = listing?.books?.firstOrNull { it.uri == openUri }
    DisposableEffect(book) {
        onDocumentOpenChange(book != null)
        onDispose { onDocumentOpenChange(false) }
    }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = book != null && !showSettings) { openUri = null }

    when {
        showSettings -> SettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onChangeFolder = { pickFolder.launch(null) },
        )
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
        else -> LibraryScreen(
            listing = listing!!,
            states = states,
            onOpen = { openUri = it.uri },
            onSettings = { showSettings = true },
        )
    }
}
