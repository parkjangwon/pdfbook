package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap
import org.parkjw.apps.pdfbook.DEFAULT_TAP_ZONE_SIDE_PERCENT
import org.parkjw.apps.pdfbook.FolderCardUi
import org.parkjw.apps.pdfbook.FolderPickScreen
import org.parkjw.apps.pdfbook.FolderShelfScreen
import org.parkjw.apps.pdfbook.FolderShelfUi
import org.parkjw.apps.pdfbook.LibraryScreen
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSnackbar
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class MainScreenshotsTest {
    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun libraryLightFolded() = captureLibrary(dark = false, file = "library-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun libraryLightFoldedEn() = captureLibrary(dark = false, file = "library-light-folded-en.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun libraryDarkFolded() = captureLibrary(dark = true, file = "library-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun libraryLightUnfolded() = captureLibrary(dark = false, file = "library-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun libraryDarkUnfolded() = captureLibrary(dark = true, file = "library-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun folderLightFolded() = captureFolderEmpty(dark = false, file = "folder-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun folderDarkFolded() = captureFolderEmpty(dark = true, file = "folder-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun folderLightUnfolded() = captureFolderEmpty(dark = false, file = "folder-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun folderDarkUnfolded() = captureFolderEmpty(dark = true, file = "folder-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun firstRunLightFolded() = captureFolderEmpty(dark = false, file = "first-run-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun firstRunDarkFolded() = captureFolderEmpty(dark = true, file = "first-run-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun firstRunLightUnfolded() = captureFolderEmpty(dark = false, file = "first-run-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun firstRunDarkUnfolded() = captureFolderEmpty(dark = true, file = "first-run-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfLightFolded() = captureShelf(dark = false, file = "shelf-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfDarkFolded() = captureShelf(dark = true, file = "shelf-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfLightUnfolded() = captureShelf(dark = false, file = "shelf-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfDarkUnfolded() = captureShelf(dark = true, file = "shelf-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfExitLightFolded() = captureShelfExit(dark = false, file = "shelf-exit-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfExitDarkFolded() = captureShelfExit(dark = true, file = "shelf-exit-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfExitLightUnfolded() = captureShelfExit(dark = false, file = "shelf-exit-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfExitDarkUnfolded() = captureShelfExit(dark = true, file = "shelf-exit-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfBottomLightFolded() = captureShelfBottom(dark = false, file = "shelf-bottom-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfBottomDarkFolded() = captureShelfBottom(dark = true, file = "shelf-bottom-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfBottomLightUnfolded() = captureShelfBottom(dark = false, file = "shelf-bottom-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfBottomDarkUnfolded() = captureShelfBottom(dark = true, file = "shelf-bottom-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfDuplicateLightFolded() = captureShelfDuplicate(dark = false, file = "shelf-duplicate-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfDuplicateDarkFolded() = captureShelfDuplicate(dark = true, file = "shelf-duplicate-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfDuplicateLightUnfolded() = captureShelfDuplicate(dark = false, file = "shelf-duplicate-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfDuplicateDarkUnfolded() = captureShelfDuplicate(dark = true, file = "shelf-duplicate-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfLostLightFolded() = captureShelfLost(dark = false, file = "shelf-lost-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfLostDarkFolded() = captureShelfLost(dark = true, file = "shelf-lost-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfLostLightUnfolded() = captureShelfLost(dark = false, file = "shelf-lost-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfLostDarkUnfolded() = captureShelfLost(dark = true, file = "shelf-lost-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfMenuLightFolded() = captureShelfMenu(dark = false, file = "shelf-menu-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun shelfMenuDarkFolded() = captureShelfMenu(dark = true, file = "shelf-menu-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun shelfMenuLightUnfolded() = captureShelfMenu(dark = false, file = "shelf-menu-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun shelfMenuDarkUnfolded() = captureShelfMenu(dark = true, file = "shelf-menu-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun removeConfirmLightFolded() = captureRemoveConfirm(dark = false, file = "remove-confirm-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun removeConfirmDarkFolded() = captureRemoveConfirm(dark = true, file = "remove-confirm-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun removeConfirmLightUnfolded() = captureRemoveConfirm(dark = false, file = "remove-confirm-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun removeConfirmDarkUnfolded() = captureRemoveConfirm(dark = true, file = "remove-confirm-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun renameLightFolded() = captureRename(dark = false, file = "rename-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun renameDarkFolded() = captureRename(dark = true, file = "rename-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun renameLightUnfolded() = captureRename(dark = false, file = "rename-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun renameDarkUnfolded() = captureRename(dark = true, file = "rename-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsLightFolded() = captureSettings(dark = false, file = "settings-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsDarkFolded() = captureSettings(dark = true, file = "settings-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun settingsLightUnfolded() = captureSettings(dark = false, file = "settings-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun settingsDarkUnfolded() = captureSettings(dark = true, file = "settings-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsAboutLightFolded() = captureSettings(
        dark = false,
        file = "settings-about-light-folded.png",
        scrollToEnd = true,
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsAboutDarkFolded() = captureSettings(
        dark = true,
        file = "settings-about-dark-folded.png",
        scrollToEnd = true,
    )

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun settingsAboutLightUnfolded() = captureSettings(
        dark = false,
        file = "settings-about-light-unfolded.png",
        scrollToEnd = true,
    )

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun settingsAboutDarkUnfolded() = captureSettings(
        dark = true,
        file = "settings-about-dark-unfolded.png",
        scrollToEnd = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun dialogLightFolded() = capture("dialog-light-folded.png", dark = false) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("이름 바꾸기") },
            text = {
                OutlinedTextField(
                    value = "서재",
                    onValueChange = {},
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {}, colors = pdfBookTextButtonColors()) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {}, colors = pdfBookTextButtonColors()) {
                    Text("취소")
                }
            },
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun snackbarActionLight() = captureSnackbarAction(dark = false, file = "snackbar-action-light.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun snackbarActionDark() = captureSnackbarAction(dark = true, file = "snackbar-action-dark.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerLightFolded() = captureReader(dark = false, file = "reader-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun readerDarkFolded() = captureReader(dark = true, file = "reader-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun readerLightUnfolded() = captureReader(dark = false, file = "reader-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun readerDarkUnfolded() = captureReader(dark = true, file = "reader-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerGuideLightFolded() = captureReader(dark = false, file = "reader-guide-light-folded.png", showTapZoneGuide = true)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun readerGuideDarkFolded() = captureReader(dark = true, file = "reader-guide-dark-folded.png", showTapZoneGuide = true)

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun readerGuideLightUnfolded() = captureReader(dark = false, file = "reader-guide-light-unfolded.png", showTapZoneGuide = true)

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun readerGuideDarkUnfolded() = captureReader(dark = true, file = "reader-guide-dark-unfolded.png", showTapZoneGuide = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerGuideLtrLightFolded() = captureReader(
        dark = false,
        file = "reader-guide-ltr-light-folded.png",
        showTapZoneGuide = true,
        direction = ReadingDirection.LTR,
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun readerGuideLightFoldedEn() = captureReader(dark = false, file = "reader-guide-light-folded-en.png", showTapZoneGuide = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerTapWidthLightFolded() = captureReader(
        dark = false,
        file = "reader-tapwidth-light-folded.png",
        showTapZoneGuide = true,
        tapZoneSidePercent = 40,
        tapZoneWidthSheetVisible = true,
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun readerTapWidthDarkFolded() = captureReader(
        dark = true,
        file = "reader-tapwidth-dark-folded.png",
        showTapZoneGuide = true,
        tapZoneSidePercent = 40,
        tapZoneWidthSheetVisible = true,
    )

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun readerTapWidthLightUnfolded() = captureReader(
        dark = false,
        file = "reader-tapwidth-light-unfolded.png",
        showTapZoneGuide = true,
        tapZoneSidePercent = 40,
        tapZoneWidthSheetVisible = true,
    )

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun readerTapWidthDarkUnfolded() = captureReader(
        dark = true,
        file = "reader-tapwidth-dark-unfolded.png",
        showTapZoneGuide = true,
        tapZoneSidePercent = 40,
        tapZoneWidthSheetVisible = true,
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun readerTapWidthLightFoldedEn() = captureReader(
        dark = false,
        file = "reader-tapwidth-light-folded-en.png",
        showTapZoneGuide = true,
        tapZoneSidePercent = 40,
        tapZoneWidthSheetVisible = true,
    )

    private fun captureLibrary(dark: Boolean, file: String) {
        val listing = sampleListing()
        capture(file, dark) {
            LibraryScreen(
                listing = listing,
                states = sampleStates(listing),
                onOpen = {},
                onSettings = {},
                covers = sampleCovers(listing),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureFolderEmpty(dark: Boolean, file: String) {
        capture(file, dark) {
            FolderPickScreen(
                illustration = R.drawable.es_pick_folder,
                title = stringResource(R.string.es_pick_folder_title),
                body = stringResource(R.string.es_pick_folder_body),
                buttonLabel = stringResource(R.string.es_pick_folder_action),
                onPick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureShelfScreen(
        dark: Boolean,
        file: String,
        shelf: FolderShelfUi,
        covers: Map<String, Bitmap?>,
        snackbarMessage: String? = null,
        scrollToUri: String? = null,
        showRenameFor: FolderCardUi? = null,
        showMenuFor: String? = null,
        showRemoveConfirmFor: String? = null,
        exitHintToken: Int = 0,
    ) {
        capture(file, dark) {
            FolderShelfScreen(
                shelf = shelf,
                covers = covers,
                snackbarMessage = snackbarMessage,
                scrollToUri = scrollToUri,
                showRenameFor = showRenameFor,
                onSnackbarShown = {},
                onScrollHandled = {},
                onOpenFolder = {},
                onOpenResume = {},
                onAddFolder = {},
                onSettings = {},
                onRenameRequest = {},
                onRenameConfirm = { _, _ -> },
                onRenameDismiss = {},
                onRepick = {},
                onRemove = {},
                onLostFolder = {},
                modifier = Modifier.fillMaxSize(),
                showMenuFor = showMenuFor,
                showRemoveConfirmFor = showRemoveConfirmFor,
                exitHintToken = exitHintToken,
            )
        }
    }

    private fun captureShelf(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
        )
    }

    private fun captureShelfExit(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
            exitHintToken = 1,
        )
    }

    private fun captureShelfBottom(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
            scrollToUri = shelf.cards.last().uri,
        )
    }

    private fun captureShelfDuplicate(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleDuplicateShelfUi()
        capture(file, dark) {
            FolderShelfScreen(
                shelf = shelf,
                covers = sampleShelfCovers(listings),
                snackbarMessage = stringResource(R.string.folder_duplicate),
                scrollToUri = null,
                showRenameFor = null,
                onSnackbarShown = {},
                onScrollHandled = {},
                onOpenFolder = {},
                onOpenResume = {},
                onAddFolder = {},
                onSettings = {},
                onRenameRequest = {},
                onRenameConfirm = { _, _ -> },
                onRenameDismiss = {},
                onRepick = {},
                onRemove = {},
                onLostFolder = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureShelfLost(dark: Boolean, file: String) {
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = sampleLostOnlyShelfUi(),
            covers = emptyMap(),
        )
    }

    private fun captureShelfMenu(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
            showMenuFor = shelf.cards.first().uri,
        )
    }

    private fun captureRemoveConfirm(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
            showRemoveConfirmFor = shelf.cards.first().uri,
        )
    }

    private fun captureRename(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        captureShelfScreen(
            dark = dark,
            file = file,
            shelf = shelf,
            covers = sampleShelfCovers(listings),
            showRenameFor = shelf.cards.first(),
        )
    }

    private fun captureSettings(dark: Boolean, file: String, scrollToEnd: Boolean = false) {
        capture(file, dark) {
            SettingsScreen(
                settings = sampleSettings().copy(
                    themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                ),
                onSettingsChange = {},
                versionName = "0.3.0",
                modifier = Modifier.fillMaxSize(),
                scrollToEnd = scrollToEnd,
            )
        }
    }

    private fun captureReader(
        dark: Boolean,
        file: String,
        showTapZoneGuide: Boolean = false,
        direction: ReadingDirection = ReadingDirection.RTL,
        tapZoneSidePercent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
        tapZoneWidthSheetVisible: Boolean = false,
    ) {
        val pageCount = 12
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        capture(file, dark, systemBarsColor = ReaderBackground) {
            val pagerState = rememberPagerState(pageCount = { pageSpreads.size })
            ReaderScreen(
                renderer = SolidPageBitmaps(
                    pageCount = pageCount,
                    pageColor = 0xFFF7F2E8.toInt(),
                    accentColor = 0xFF3D5A80.toInt(),
                ),
                pagerState = pagerState,
                spreads = pageSpreads,
                title = "샘플 책",
                pageCount = pageCount,
                currentPage = 1,
                twoPage = false,
                onBack = {},
                onDirectionChange = {},
                direction = direction,
                overlayVisible = true,
                showTapZoneGuide = showTapZoneGuide,
                tapZoneSidePercent = tapZoneSidePercent,
                tapZoneWidthSheetVisible = tapZoneWidthSheetVisible,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureSnackbarAction(dark: Boolean, file: String) {
        val data = object : SnackbarData {
            override val visuals = object : SnackbarVisuals {
                override val message = "이미 목록에 있는 폴더라 맨 위로 올렸어요"
                override val actionLabel = "실행 취소"
                override val withDismissAction = false
                override val duration = SnackbarDuration.Indefinite
            }

            override fun performAction() = Unit

            override fun dismiss() = Unit
        }
        capture(file, dark) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                PdfBookSnackbar(snackbarData = data)
            }
        }
    }

    private fun capture(
        file: String,
        dark: Boolean = file.contains("-dark-"),
        systemBarsColor: Color? = null,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(file) {
            ScreenshotTheme(dark, systemBarsColor, content)
        }
    }
}
