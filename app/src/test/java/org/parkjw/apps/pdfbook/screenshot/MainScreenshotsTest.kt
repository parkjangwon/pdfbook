package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.FolderPickScreen
import org.parkjw.apps.pdfbook.FolderShelfScreen
import org.parkjw.apps.pdfbook.LibraryScreen
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
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

    private fun captureShelf(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        capture(file, dark) {
            FolderShelfScreen(
                shelf = shelf,
                covers = sampleShelfCovers(listings),
                snackbarMessage = null,
                scrollToUri = shelf.cards.last().uri,
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

    private fun captureRename(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        capture(file, dark) {
            FolderShelfScreen(
                shelf = shelf,
                covers = sampleShelfCovers(listings),
                snackbarMessage = null,
                scrollToUri = null,
                showRenameFor = shelf.cards.first(),
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

    private fun captureSettings(dark: Boolean, file: String) {
        capture(file, dark) {
            SettingsScreen(
                settings = sampleSettings().copy(
                    themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                ),
                onSettingsChange = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureReader(dark: Boolean, file: String) {
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
                direction = ReadingDirection.RTL,
                overlayVisible = true,
                modifier = Modifier.fillMaxSize(),
            )
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
