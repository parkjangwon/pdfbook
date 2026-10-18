package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.folders.AddKind
import org.parkjw.apps.pdfbook.folders.ContentResolverUriPermissions
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.folders.FolderRepository
import org.parkjw.apps.pdfbook.folders.displayNameForUri
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App(
                        volumeDelta = volumeDeltaFlow,
                        settings = settings,
                        settingsReady = settingsState.value != null,
                        onSettingsChange = { transform -> scope.launch { settingsRepo.update(transform) } },
                        onDocumentOpenChange = { open -> documentOpen = open },
                    )
                }
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

private sealed interface FolderPickTarget {
    data object Add : FolderPickTarget
    data class Repick(val targetUri: String) : FolderPickTarget
}

private val FolderPickTargetSaver = listSaver<FolderPickTarget, Any>(
    save = { target ->
        when (target) {
            FolderPickTarget.Add -> listOf("add")
            is FolderPickTarget.Repick -> listOf("repick", target.targetUri)
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0) as? String) {
            "repick" -> FolderPickTarget.Repick(saved.getOrNull(1) as? String ?: return@listSaver FolderPickTarget.Add)
            else -> FolderPickTarget.Add
        }
    },
)

@Composable
private fun App(
    volumeDelta: SharedFlow<Int>,
    settings: AppSettings,
    settingsReady: Boolean,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onDocumentOpenChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BookStore(context) }
    val folderRepo = remember {
        FolderRepository(context, ContentResolverUriPermissions(context.contentResolver))
    }
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<Folder>?>(null) }
    var selectedFolderUri by rememberSaveable { mutableStateOf<String?>(null) }
    var openUri by rememberSaveable { mutableStateOf<String?>(null) }
    var listing by remember { mutableStateOf<Listing?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var shelfListings by remember { mutableStateOf<Map<String, Listing?>>(emptyMap()) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var startApplied by rememberSaveable { mutableStateOf(false) }
    var pickTarget by rememberSaveable(stateSaver = FolderPickTargetSaver) {
        mutableStateOf<FolderPickTarget>(FolderPickTarget.Add)
    }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var scrollToUri by remember { mutableStateOf<String?>(null) }
    var renameCard by remember { mutableStateOf<FolderCardUi?>(null) }
    var showLostForUri by rememberSaveable { mutableStateOf<String?>(null) }
    var folderScanToken by rememberSaveable { mutableStateOf(0) }
    val statesState = store.states.collectAsState(initial = null)
    val states = statesState.value ?: emptyMap()
    val folderList = folders.orEmpty()
    val selectedFolder = folderList.find { it.uri == selectedFolderUri }
    val duplicateMessage = stringResource(R.string.folder_duplicate)
    val lastBookUnavailable = stringResource(R.string.last_book_unavailable)

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val uriString = uri.toString()
        val name = displayNameForUri(uriString)
        val target = pickTarget
        pickTarget = FolderPickTarget.Add
        scope.launch {
            when (target) {
                FolderPickTarget.Add -> {
                    when (folderRepo.add(uriString, name)) {
                        AddKind.Duplicate -> {
                            snackbarMessage = duplicateMessage
                            scrollToUri = uriString
                        }
                        AddKind.Created, AddKind.Restored -> Unit
                    }
                }
                is FolderPickTarget.Repick -> {
                    folderRepo.repick(target.targetUri, uriString)
                    showLostForUri = null
                    if (selectedFolderUri == target.targetUri) {
                        selectedFolderUri = uriString
                        scanned = false
                        folderScanToken++
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        folderRepo.migrateFromLegacyIfNeeded()
        folderRepo.refreshAccess()
        folderRepo.folders.collect {
            folders = it
            loaded = true
        }
    }

    LaunchedEffect(folderList, loaded) {
        if (!loaded) return@LaunchedEffect
        val next = linkedMapOf<String, Listing?>()
        for (folder in folderList) {
            next[folder.uri] = if (folder.access == FolderAccess.Ok) {
                listPdfs(context, Uri.parse(folder.uri))
            } else {
                null
            }
        }
        shelfListings = next
    }

    LaunchedEffect(selectedFolderUri, selectedFolder?.displayName, loaded, folderScanToken) {
        if (!loaded) return@LaunchedEffect
        val uri = selectedFolderUri
        if (uri == null) {
            listing = null
            scanned = true
            return@LaunchedEffect
        }
        scanned = false
        val scannedListing = listPdfs(context, Uri.parse(uri))
        listing = listingWithFolderName(scannedListing, selectedFolder?.displayName)
        scanned = true
    }

    LaunchedEffect(loaded, settingsReady, settings.startScreen, statesState.value, shelfListings) {
        if (!canApplyStartScreen(
                startApplied = startApplied,
                loaded = loaded,
                settingsReady = settingsReady,
                statesReady = statesState.value != null,
                folderCount = folderList.size,
                shelfListingCount = shelfListings.size,
            )
        ) {
            return@LaunchedEffect
        }
        if (settings.startScreen != StartScreen.LastBook) {
            startApplied = true
            return@LaunchedEffect
        }
        val target = findLastBookTarget(folderList, shelfListings, states)
        if (target != null) {
            folderRepo.markOpened(target.folderUri)
            openUri = target.bookUri
            selectedFolderUri = target.folderUri
            scanned = false
            folderScanToken++
        } else {
            snackbarMessage = lastBookUnavailable
        }
        startApplied = true
    }

    val book = listing?.books?.firstOrNull { it.uri == openUri }
    DisposableEffect(book) {
        onDocumentOpenChange(book != null)
        onDispose { onDocumentOpenChange(false) }
    }
    val backAction = folderBackAction(
        showSettings = showSettings,
        readerOpen = book != null,
        folderSelected = selectedFolderUri != null && book == null,
        showLost = showLostForUri != null,
    )
    BackHandler(enabled = backAction == FolderBackAction.CloseSettings) { showSettings = false }
    BackHandler(enabled = backAction == FolderBackAction.CloseReader) { openUri = null }
    BackHandler(enabled = backAction == FolderBackAction.CloseLibrary) {
        selectedFolderUri = null
        listing = null
        openUri = null
    }
    BackHandler(enabled = backAction == FolderBackAction.CloseLost) { showLostForUri = null }

    LaunchedEffect(selectedFolderUri, selectedFolder?.access) {
        val uri = selectedFolderUri ?: return@LaunchedEffect
        if (selectedFolder?.access == FolderAccess.Lost) {
            showLostForUri = uri
            selectedFolderUri = null
            openUri = null
        }
    }

    val contentPhase = folderContentPhase(selectedFolderUri, scanned, listing)

    val shelfUi = remember(folderList, shelfListings, states) {
        buildFolderShelfUi(folderList, shelfListings, states)
    }
    var shelfCovers by remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    LaunchedEffect(shelfUi, shelfListings) {
        val needed = buildSet {
            shelfUi.globalResume?.let { add(bookKey(it.book.uri, it.book.size)) }
            shelfUi.cards.forEach { addAll(it.coverKeys) }
        }
        if (needed.isEmpty()) {
            shelfCovers = emptyMap()
            return@LaunchedEffect
        }
        val books = shelfListings.values.filterNotNull().flatMap { it.books }
            .associateBy { bookKey(it.uri, it.size) }
        shelfCovers = needed.associateWith { key ->
            books[key]?.let { loadCover(context, it) }
        }
    }

    when {
        showSettings -> SettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
        )
        showLostForUri != null -> FolderPickScreen(
            illustration = R.drawable.es_permission_lost,
            title = stringResource(R.string.es_permission_lost_title),
            body = stringResource(R.string.es_permission_lost_body),
            buttonLabel = stringResource(R.string.es_permission_lost_action),
            onPick = {
                pickTarget = FolderPickTarget.Repick(showLostForUri!!)
                pickFolder.launch(null)
            },
        )
        !loaded -> Unit
        book != null -> ReaderRoute(
            book,
            store,
            volumeDelta,
            defaultReadingDirection = settings.defaultReadingDirection,
            onBack = { openUri = null },
            showTapZoneGuide = settings.showTapZoneGuide,
        )
        contentPhase == FolderContentPhase.Loading -> Unit
        contentPhase == FolderContentPhase.AccessLost -> FolderPickScreen(
            illustration = R.drawable.es_permission_lost,
            title = stringResource(R.string.es_permission_lost_title),
            body = stringResource(R.string.es_permission_lost_body),
            buttonLabel = stringResource(R.string.es_permission_lost_action),
            onPick = {
                pickTarget = FolderPickTarget.Repick(selectedFolderUri!!)
                pickFolder.launch(null)
            },
        )
        contentPhase == FolderContentPhase.Empty -> FolderPickScreen(
            illustration = R.drawable.es_empty_folder,
            title = stringResource(R.string.es_empty_folder_title),
            body = stringResource(R.string.es_empty_folder_body),
            buttonLabel = stringResource(R.string.es_empty_folder_action),
            onPick = {
                selectedFolderUri = null
                listing = null
                openUri = null
            },
        )
        contentPhase == FolderContentPhase.Ready -> LibraryScreen(
            listing = listing!!,
            states = states,
            onOpen = { openUri = it.uri },
            onSettings = { showSettings = true },
            onBack = {
                selectedFolderUri = null
                listing = null
                openUri = null
            },
        )
        folderList.isEmpty() -> FolderPickScreen(
            illustration = R.drawable.es_pick_folder,
            title = stringResource(R.string.es_pick_folder_title),
            body = stringResource(R.string.es_pick_folder_body),
            buttonLabel = stringResource(R.string.es_pick_folder_action),
            onPick = {
                pickTarget = FolderPickTarget.Add
                pickFolder.launch(null)
            },
        )
        else -> FolderShelfScreen(
            shelf = shelfUi,
            covers = shelfCovers,
            snackbarMessage = snackbarMessage,
            scrollToUri = scrollToUri,
            showRenameFor = renameCard,
            onSnackbarShown = { snackbarMessage = null },
            onScrollHandled = { scrollToUri = null },
            onOpenFolder = { uri ->
                scope.launch {
                    folderRepo.markOpened(uri)
                    openUri = null
                    selectedFolderUri = uri
                    scanned = false
                    folderScanToken++
                }
            },
            onOpenResume = { resume ->
                scope.launch {
                    folderRepo.markOpened(resume.folderUri)
                    openUri = resume.book.uri
                    selectedFolderUri = resume.folderUri
                    scanned = false
                    folderScanToken++
                }
            },
            onAddFolder = {
                pickTarget = FolderPickTarget.Add
                pickFolder.launch(null)
            },
            onSettings = { showSettings = true },
            onRenameRequest = { renameCard = it },
            onRenameConfirm = { uri, name ->
                val defaultName = displayNameForUri(uri)
                renameCard = null
                scope.launch { folderRepo.rename(uri, name, defaultName) }
            },
            onRenameDismiss = { renameCard = null },
            onRepick = { uri ->
                pickTarget = FolderPickTarget.Repick(uri)
                pickFolder.launch(null)
            },
            onRemove = { uri -> scope.launch { folderRepo.remove(uri) } },
            onLostFolder = { uri -> showLostForUri = uri },
        )
    }
}
