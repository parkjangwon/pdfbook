package org.parkjw.apps.pdfbook

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.folders.AddKind
import org.parkjw.apps.pdfbook.lock.AppLockProcess
import org.parkjw.apps.pdfbook.lock.LockConfig
import org.parkjw.apps.pdfbook.lock.LockGate
import org.parkjw.apps.pdfbook.lock.LockRepository
import org.parkjw.apps.pdfbook.lock.applyRecentsScreenshot
import org.parkjw.apps.pdfbook.lock.canAuthenticateBiometric
import org.parkjw.apps.pdfbook.lock.isExemptSystemIntent
import org.parkjw.apps.pdfbook.folders.ContentResolverUriPermissions
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.folders.FolderRepository
import org.parkjw.apps.pdfbook.folders.displayNameForUri
import org.parkjw.apps.pdfbook.hidden.HiddenItem
import org.parkjw.apps.pdfbook.hidden.HiddenItemsScreen
import org.parkjw.apps.pdfbook.hidden.HiddenKind
import org.parkjw.apps.pdfbook.hidden.HiddenPeek
import org.parkjw.apps.pdfbook.hidden.HiddenRepository
import org.parkjw.apps.pdfbook.hidden.buildPeekShelfUi
import org.parkjw.apps.pdfbook.hidden.filterListing
import org.parkjw.apps.pdfbook.hidden.hiddenBookIds
import org.parkjw.apps.pdfbook.hidden.hiddenFolderIds
import org.parkjw.apps.pdfbook.hidden.libraryBooks
import org.parkjw.apps.pdfbook.hidden.peekMenuEnabled
import org.parkjw.apps.pdfbook.hidden.shelfFolders
import org.parkjw.apps.pdfbook.hidden.shouldResetPeekOnStop
import org.parkjw.apps.pdfbook.hidden.visibleFolders
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import org.parkjw.apps.pdfbook.lock.AppLockSettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SyncAppLanguage
import org.parkjw.apps.pdfbook.settings.StartScreen
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.update.RELEASES_PAGE_URL
import org.parkjw.apps.pdfbook.update.UpdateAction
import org.parkjw.apps.pdfbook.update.UpdateBanner
import org.parkjw.apps.pdfbook.update.displayVersion
import org.parkjw.apps.pdfbook.update.UpdateUiState
import org.parkjw.apps.pdfbook.update.UpdateViewModel
import org.parkjw.apps.pdfbook.update.installedVersionCode
import org.parkjw.apps.pdfbook.update.installedVersionName
import org.parkjw.apps.pdfbook.update.launchApkInstaller
import org.parkjw.apps.pdfbook.update.openInstallPermissionSettings
import org.parkjw.apps.pdfbook.update.openReleasePage

class MainActivity : ComponentActivity() {
    private val volumeDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val volumeDeltaFlow: SharedFlow<Int> = volumeDeltas
    @Volatile private var documentOpen = false
    @Volatile private var volumeKeyPaging = true
    private val updates: UpdateViewModel by viewModels()
    @Volatile private var lockBlocksInput = true

    override fun onStart() {
        super.onStart()
        AppLockProcess.state.onStart(SystemClock.elapsedRealtime())
    }

    override fun onResume() {
        super.onResume()
        AppLockProcess.state.onResume()
    }

    override fun onStop() {
        AppLockProcess.state.onStop(isChangingConfigurations, SystemClock.elapsedRealtime())
        if (shouldResetPeekOnStop(isChangingConfigurations)) {
            HiddenPeek.reset()
        }
        super.onStop()
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        val exempt = noteExempt(intent)
        try {
            super.startActivity(intent, options)
        } catch (error: RuntimeException) {
            if (exempt) AppLockProcess.state.cancelSystemReturn()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        val exempt = noteExempt(intent)
        try {
            super.startActivityForResult(intent, requestCode, options)
        } catch (error: RuntimeException) {
            if (exempt) AppLockProcess.state.cancelSystemReturn()
            throw error
        }
    }

    private fun noteExempt(intent: Intent?): Boolean {
        if (!isExemptSystemIntent(intent?.action, intent?.type)) return false
        AppLockProcess.state.expectSystemReturn()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsRepo = remember { SettingsRepository(context) }
            val settingsState = settingsRepo.settings.collectAsState(initial = null)
            val settings = settingsState.value ?: AppSettings()
            val scope = rememberCoroutineScope()
            val lockRepo = remember { LockRepository(context) }
            val lockConfig = lockRepo.config.collectAsState(initial = null).value
            var biometricAvailable by remember { mutableStateOf(canAuthenticateBiometric(context)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                biometricAvailable = canAuthenticateBiometric(context)
            }
            val session = AppLockProcess.state
            SyncAppLanguage(
                ready = settingsState.value != null,
                language = settings.language,
            ) { language ->
                scope.launch { settingsRepo.update { it.copy(language = language) } }
            }
            LaunchedEffect(settings.volumeKeyPaging) {
                volumeKeyPaging = settings.volumeKeyPaging
            }
            val updateState by updates.state.collectAsState()
            LaunchedEffect(settingsState.value != null) {
                if (settingsState.value != null) updates.autoCheckOnLaunch(settings.autoUpdateCheck)
            }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.onResumed() }
            LaunchedEffect(updateState) {
                val ready = updateState as? UpdateUiState.ReadyToInstall ?: return@LaunchedEffect
                updates.installerLaunched(context.launchApkInstaller(ready.apk))
            }
            PdfBookTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SideEffect {
                        if (lockConfig == null) {
                            setRecentsScreenshotEnabled(false)
                        } else {
                            applyRecentsScreenshot(lockConfig) { setRecentsScreenshotEnabled(it) }
                        }
                    }
                    LockGate(
                        config = lockConfig,
                        session = session,
                        canAuthenticate = biometricAvailable,
                        verify = { pin, now -> lockRepo.verifyPin(pin, now) },
                        onUnlocked = { session.unlock() },
                        onBack = { moveTaskToBack(true) },
                        onBlockedChange = { lockBlocksInput = it },
                    ) {
                        App(
                            volumeDelta = volumeDeltaFlow,
                            settings = settings,
                            settingsReady = settingsState.value != null,
                            onSettingsChange = { transform -> scope.launch { settingsRepo.update(transform) } },
                            onDocumentOpenChange = { open -> documentOpen = open },
                            lockConfig = lockConfig ?: LockConfig(),
                            biometricAvailable = biometricAvailable,
                            lockRepository = lockRepo,
                            updateState = updateState,
                            onUpdateAction = { action ->
                                when (action) {
                                    UpdateAction.Check -> updates.manualCheck()
                                    UpdateAction.Download -> updates.downloadAndInstall()
                                    UpdateAction.Later -> updates.later()
                                    UpdateAction.OpenSettings -> context.openInstallPermissionSettings()
                                    UpdateAction.OpenReleasePage -> context.openReleasePage(
                                        (updateState as? UpdateUiState.Failed)?.releasePageUrl ?: RELEASES_PAGE_URL,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (lockBlocksInput || !documentOpen || !volumeKeyPaging) return super.onKeyDown(keyCode, event)
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
        if (lockBlocksInput || !documentOpen || !volumeKeyPaging) return super.onKeyUp(keyCode, event)
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

private val FolderPathSaver = listSaver<FolderPath, String>(
    save = { listOf(it.rootUri.orEmpty()) + it.path },
    restore = { saved -> FolderPath(saved.firstOrNull()?.ifEmpty { null }, saved.drop(1)) },
)

@Composable
private fun App(
    volumeDelta: SharedFlow<Int>,
    settings: AppSettings,
    settingsReady: Boolean,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onDocumentOpenChange: (Boolean) -> Unit,
    lockConfig: LockConfig,
    biometricAvailable: Boolean,
    lockRepository: LockRepository,
    updateState: UpdateUiState,
    onUpdateAction: (UpdateAction) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BookStore(context) }
    val sortStore = remember { SortStore(context) }
    val sorts by sortStore.sorts.collectAsState(initial = emptyMap())
    val folderRepo = remember {
        FolderRepository(context, ContentResolverUriPermissions(context.contentResolver))
    }
    val hiddenRepo = remember { HiddenRepository(context) }
    val hiddenState = hiddenRepo.items.collectAsState(initial = null)
    val hiddenItems = hiddenState.value.orEmpty()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<Folder>?>(null) }
    var selectedFolderUri by rememberSaveable { mutableStateOf<String?>(null) }
    var openUri by rememberSaveable { mutableStateOf<String?>(null) }
    var switchedToUri by remember { mutableStateOf<String?>(null) }
    var folderPathState by rememberSaveable(stateSaver = FolderPathSaver) { mutableStateOf(FolderPath()) }
    var listing by remember { mutableStateOf<Listing?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var shelfListings by remember { mutableStateOf<Map<String, Listing?>>(emptyMap()) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAppLock by rememberSaveable { mutableStateOf(false) }
    var showHiddenItems by rememberSaveable { mutableStateOf(false) }
    var undoItem by remember { mutableStateOf<HiddenItem?>(null) }
    var undoWasUnhide by remember { mutableStateOf(false) }
    val peekEnabled by HiddenPeek.enabled.collectAsState()
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
    val hiddenSnackbar = stringResource(R.string.hidden_snackbar)
    val unhiddenSnackbar = stringResource(R.string.unhidden_snackbar)
    val undoLabel = stringResource(R.string.action_undo)

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
                            hiddenRepo.unhide(HiddenKind.Folder, uriString)
                            undoItem = null
                            snackbarMessage = duplicateMessage
                            scrollToUri = uriString
                        }
                        AddKind.Restored -> {
                            hiddenRepo.unhide(HiddenKind.Folder, uriString)
                            undoItem = null
                        }
                        AddKind.Created -> Unit
                    }
                }
                is FolderPickTarget.Repick -> {
                    folderRepo.repick(target.targetUri, uriString)
                    sortStore.moveRoot(target.targetUri, uriString)
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

    val resumeFolders = visibleFolders(folderList, hiddenItems)
    val shownBookIds = hiddenBookIds(hiddenItems)
    val rules = VisibilityRules(shownBookIds, hiddenFolderIds(hiddenItems), settings.showDotFiles)
    val resumeListings = shelfListings.mapValues { (_, value) ->
        value?.let { found ->
            found.root?.let { flatListing(it, rules, found.folderName) }
                ?: filterListing(found, shownBookIds, settings.showDotFiles)
        }
    }
    val treeRoot = listing?.root
    val folderPath = trimPath(treeRoot, folderPathState.pathFor(selectedFolderUri))
    fun setFolderPath(path: List<String>) {
        folderPathState = FolderPath(selectedFolderUri, path)
    }
    val currentNode = treeRoot?.nodeAt(folderPath)
    val pathFolderUris = folderPath.indices.mapNotNull { i -> treeRoot?.nodeAt(folderPath.take(i + 1))?.uri }.toSet()
    val hiddenAncestors = hiddenItems.filter { it.kind == HiddenKind.Folder && it.id in pathFolderUris }
    val insideHiddenFolder = hiddenAncestors.isNotEmpty()
    val subfolders = currentNode?.let { subfolderItems(it, folderPath, rules, peekEnabled) }.orEmpty()
        .let { items ->
            when {
                !insideHiddenFolder -> items
                peekEnabled -> items.map { it.copy(isHidden = true) }
                else -> emptyList()
            }
        }
    val currentBooks = currentNode?.books ?: listing?.books.orEmpty()
    val libraryHiddenIds = if (insideHiddenFolder) shownBookIds + currentBooks.map { it.uri } else shownBookIds
    val shelfFolderItems = shelfFolders(folderList, hiddenItems, peekEnabled)
    val displayFolders = shelfFolderItems.map { it.folder }
    val peekHiddenFolderIds = shelfFolderItems.filter { it.isHidden }.map { it.folder.uri }.toSet()
    val libraryItems = libraryBooks(currentBooks, libraryHiddenIds, peekEnabled, settings.showDotFiles)
    val displayListing = listing?.copy(
        folderName = if (folderPath.isEmpty() || currentNode == null) listing!!.folderName else currentNode.name,
        books = libraryItems.map { it.book },
    )
    val pathLine = if (folderPath.isEmpty() || treeRoot == null) {
        null
    } else {
        (listOf(listing!!.folderName) + treeRoot.pathNames(folderPath).drop(1)).joinToString(" / ")
    }
    val peekHiddenBookIds = libraryItems.filter { it.isHidden }.map { it.book.uri }.toSet()
    val currentFolderEmpty = scanned && treeRoot != null && folderPath.isNotEmpty() &&
        subfolders.isEmpty() && libraryItems.isEmpty()
    LaunchedEffect(currentFolderEmpty, folderPath) {
        if (currentFolderEmpty && openUri == null) parentPath(folderPath)?.let { setFolderPath(it) }
    }
    val canPeek = peekMenuEnabled(hiddenItems.size)

    LaunchedEffect(loaded, settingsReady, settings.startScreen, statesState.value, shelfListings, hiddenState.value, settings.showDotFiles) {
        if (hiddenState.value == null) return@LaunchedEffect
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
        val target = findLastBookTarget(resumeFolders, resumeListings, states)
        if (target != null) {
            folderRepo.markOpened(target.folderUri)
            openUri = target.bookUri
            selectedFolderUri = target.folderUri
            scanned = false
            folderScanToken++
        } else {
            undoItem = null
            snackbarMessage = lastBookUnavailable
        }
        startApplied = true
    }

    val book = openUri?.let { uri -> treeRoot?.findBook(uri) ?: listing?.books?.firstOrNull { it.uri == uri } }
    val readerOrderStates = remember(openUri != null) { states }
    LaunchedEffect(openUri, treeRoot) {
        val uri = openUri ?: return@LaunchedEffect
        val path = treeRoot?.pathOfBook(uri) ?: return@LaunchedEffect
        if (path != folderPath) setFolderPath(path)
    }
    DisposableEffect(book) {
        onDocumentOpenChange(book != null)
        onDispose { onDocumentOpenChange(false) }
    }
    val backAction = folderBackAction(
        showSettings = showSettings,
        readerOpen = book != null,
        folderSelected = selectedFolderUri != null && book == null,
        showLost = showLostForUri != null,
        inSubfolder = folderPath.isNotEmpty(),
    )
    BackHandler(enabled = backAction == FolderBackAction.UpFolder) {
        parentPath(folderPath)?.let { setFolderPath(it) }
    }
    BackHandler(enabled = backAction == FolderBackAction.CloseSettings || showHiddenItems) {
        when {
            showAppLock -> showAppLock = false
            showHiddenItems -> showHiddenItems = false
            else -> showSettings = false
        }
    }
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

    val contentPhase = folderContentPhase(selectedFolderUri, scanned, displayListing, subfolders.isNotEmpty())
    val rootFolderList = showsRootFolderList(
        showSettings = showSettings,
        showLost = showLostForUri != null,
        loaded = loaded,
        readerOpen = book != null,
        contentPhase = contentPhase,
        folderCount = folderList.size,
    )
    val activity = LocalActivity.current
    val exitGate = remember { DoubleBackExit() }
    var exitHintToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(rootFolderList) {
        if (!rootFolderList) {
            exitGate.reset()
            exitHintToken = 0
        }
    }
    BackHandler(enabled = rootFolderList) {
        when (exitGate.onBack(SystemClock.elapsedRealtime())) {
            DoubleBackDecision.ShowHint -> exitHintToken += 1
            DoubleBackDecision.Exit -> activity?.finish()
        }
    }

    val shelfUi = remember(displayFolders, resumeFolders, resumeListings, states, peekHiddenFolderIds) {
        buildPeekShelfUi(displayFolders, resumeFolders, peekHiddenFolderIds, resumeListings, states)
    }
    var shelfCovers by remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    LaunchedEffect(shelfUi, resumeListings) {
        val needed = buildSet {
            shelfUi.globalResume?.let { add(bookKey(it.book.uri, it.book.size)) }
            shelfUi.cards.forEach { addAll(it.coverKeys) }
        }
        if (needed.isEmpty()) {
            shelfCovers = emptyMap()
            return@LaunchedEffect
        }
        val books = resumeListings.values.filterNotNull().flatMap { it.books }
            .associateBy { bookKey(it.uri, it.size) }
        shelfCovers = needed.associateWith { key ->
            books[key]?.let { loadCover(context, it) }
        }
    }

    val updateBanner: (@Composable () -> Unit)? =
        (updateState as? UpdateUiState.Available)?.takeIf { it.prompt }?.let { available ->
            {
                UpdateBanner(
                    version = displayVersion(available.release),
                    onLater = { onUpdateAction(UpdateAction.Later) },
                    onDownload = {
                        showSettings = true
                        onUpdateAction(UpdateAction.Download)
                    },
                )
            }
        }

    val undoHidden = {
        val item = undoItem
        val wasUnhide = undoWasUnhide
        undoItem = null
        undoWasUnhide = false
        if (item != null) {
            scope.launch {
                if (wasUnhide) hiddenRepo.hide(item) else hiddenRepo.unhide(item.kind, item.id)
            }
        }
    }
    fun hideItem(item: HiddenItem) {
        scope.launch {
            hiddenRepo.hide(item)
            undoItem = item
            undoWasUnhide = false
            snackbarMessage = hiddenSnackbar
        }
    }
    fun unhideItem(item: HiddenItem) {
        scope.launch {
            hiddenRepo.unhide(item.kind, item.id)
            undoItem = item
            undoWasUnhide = true
            snackbarMessage = unhiddenSnackbar
        }
    }
    fun unhideAll(items: List<HiddenItem>) {
        if (items.size == 1) {
            unhideItem(items.single())
        } else if (items.isNotEmpty()) {
            scope.launch {
                for (hidden in items) hiddenRepo.unhide(hidden.kind, hidden.id)
                undoItem = null
                snackbarMessage = unhiddenSnackbar
            }
        }
    }
    val togglePeek = {
        HiddenPeek.set(!HiddenPeek.enabled.value)
    }
    when {
        showSettings && showAppLock -> AppLockSettingsScreen(
            config = lockConfig,
            session = AppLockProcess.state,
            biometricAvailable = biometricAvailable,
            onBack = { showAppLock = false },
            onAuthenticated = { AppLockProcess.state.unlock() },
            onVerify = { pin, now -> lockRepository.verifyPin(pin, now) },
            onSavePin = { pin, mode -> lockRepository.saveNewPin(pin, mode) },
            onSetMode = { mode -> lockRepository.setMode(mode) },
            onSetHideInRecents = { hide -> lockRepository.setHideInRecents(hide) },
        )
        showHiddenItems -> HiddenItemsScreen(
            items = hiddenItems,
            onBack = { showHiddenItems = false },
            onUnhide = { item -> scope.launch { hiddenRepo.unhide(item.kind, item.id) } },
        )
        showSettings -> SettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            appLockMode = lockConfig.mode,
            onOpenAppLock = { showAppLock = true },
            versionName = remember(context) { context.installedVersionName() },
            versionCode = remember(context) { context.installedVersionCode() },
            updateState = updateState,
            onUpdateAction = onUpdateAction,
            hiddenCount = hiddenItems.size,
            onOpenHiddenItems = { showHiddenItems = true },
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
        book != null -> {
            val bookPath = treeRoot?.pathOfBook(book.uri)
            val bookFolder = bookPath?.let { treeRoot.nodeAt(it) }
            val folderBooks = folderBooksOf(treeRoot, book.uri, listing?.books.orEmpty())
            val order = volumeOrder(sortFor(sorts, selectedFolderUri, bookPath.orEmpty()), readerOrderStates)
            val volumes = volumeList(folderBooks, shownBookIds, settings.showDotFiles, order)
            val adjacent = adjacentVolumes(volumes, book, order)
            val switchTo: (BookInfo) -> Unit = { next ->
                switchedToUri = next.uri
                openUri = next.uri
            }
            val bookListName = if (bookPath.isNullOrEmpty() || bookFolder == null) {
                listing?.folderName.orEmpty()
            } else {
                bookFolder.name
            }
            ReaderRoute(
                book,
                store,
                volumeDelta,
                defaultReadingDirection = settings.defaultReadingDirection,
                onBack = { openUri = null },
                showTapZoneGuide = settings.showTapZoneGuide,
                tapZoneSidePercent = settings.tapZoneSidePercent,
                keepZoom = settings.keepZoomOnTurn,
                twoPageMode = settings.twoPageMode,
                keepScreenOn = settings.keepScreenOn,
                onTapZoneSidePercentChange = { value -> onSettingsChange { it.copy(tapZoneSidePercent = value) } },
                volumes = ReaderVolumes(
                    onPrevious = adjacent.previous?.let { { switchTo(it) } },
                    onNext = adjacent.next?.let { { switchTo(it) } },
                    nextBook = adjacent.next,
                    bookList = BookListUi(bookListName, bookListEntries(volumes, book, states, order)),
                    onPick = switchTo,
                ),
                showTitleToast = switchedToUri == book.uri,
                onTitleToastShown = { switchedToUri = null },
            )
        }
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
            listing = displayListing!!,
            states = states,
            onOpen = { openUri = it.uri },
            onSettings = { showSettings = true },
            onBack = {
                val parent = parentPath(folderPath)
                if (parent != null) {
                    setFolderPath(parent)
                } else {
                    selectedFolderUri = null
                    listing = null
                    openUri = null
                }
            },
            snackbarMessage = snackbarMessage,
            snackbarActionLabel = if (undoItem != null) undoLabel else null,
            onSnackbarShown = { snackbarMessage = null; undoItem = null; undoWasUnhide = false },
            onSnackbarAction = undoHidden,
            onHideBook = { bookInfo ->
                hideItem(
                    HiddenItem(
                        kind = HiddenKind.Book,
                        id = bookInfo.uri,
                        name = titleOf(bookInfo.name),
                        folderUri = selectedFolderUri,
                    ),
                )
            },
            onUnhideBook = { bookInfo ->
                if (insideHiddenFolder) {
                    val own = hiddenItems.filter { it.kind == HiddenKind.Book && it.id == bookInfo.uri }
                    unhideAll(own + hiddenAncestors)
                } else {
                    unhideItem(
                        HiddenItem(
                            kind = HiddenKind.Book,
                            id = bookInfo.uri,
                            name = titleOf(bookInfo.name),
                            folderUri = selectedFolderUri,
                        ),
                    )
                }
            },
            peekEnabled = peekEnabled,
            peekMenuEnabled = canPeek,
            onPeekToggle = togglePeek,
            peekHiddenBookIds = peekHiddenBookIds,
            sort = sortFor(sorts, selectedFolderUri, folderPath),
            onSortChange = { sort ->
                val uri = selectedFolderUri
                val path = folderPath
                if (uri != null) scope.launch { sortStore.save(sortKey(uri, path), sort) }
            },
            subfolders = subfolders,
            onOpenSubfolder = { item -> setFolderPath(item.path) },
            onHideSubfolder = { item ->
                hideItem(HiddenItem(HiddenKind.Folder, item.node.uri, item.node.name, selectedFolderUri))
            },
            onUnhideSubfolder = { item ->
                val own = hiddenItems.firstOrNull { it.kind == HiddenKind.Folder && it.id == item.node.uri }
                val under = if (own != null) listOf(own) else hiddenItemsUnder(item.node, hiddenItems)
                unhideAll((under + hiddenAncestors).distinct())
            },
            pathLine = pathLine,
            resumeBooks = currentNode?.let { visibleBooksUnder(it, rules) },
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
            onSnackbarShown = { snackbarMessage = null; undoItem = null; undoWasUnhide = false },
            onScrollHandled = { scrollToUri = null },
            onOpenFolder = { uri ->
                scope.launch {
                    folderRepo.markOpened(uri)
                    openUri = null
                    folderPathState = FolderPath(uri)
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
            onRemove = { uri ->
                scope.launch {
                    folderRepo.remove(uri)
                    sortStore.removeRoot(uri)
                }
            },
            onLostFolder = { uri -> showLostForUri = uri },
            updateBanner = updateBanner,
            snackbarActionLabel = if (undoItem != null) undoLabel else null,
            onSnackbarAction = undoHidden,
            onHide = { uri ->
                val card = shelfUi.cards.find { it.uri == uri }
                if (card != null) {
                    hideItem(HiddenItem(HiddenKind.Folder, uri, card.displayName, uri))
                }
            },
            peekEnabled = peekEnabled,
            peekMenuEnabled = canPeek,
            onPeekToggle = togglePeek,
            onUnhide = { uri ->
                val card = shelfUi.cards.find { it.uri == uri }
                if (card != null) {
                    unhideItem(HiddenItem(HiddenKind.Folder, uri, card.displayName, uri))
                }
            },
            exitHintToken = exitHintToken,
            onReorder = { uris -> scope.launch { folderRepo.reorderVisible(uris) } },
        )
    }
}
