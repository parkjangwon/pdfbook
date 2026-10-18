package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.FolderShelfScreen
import org.parkjw.apps.pdfbook.hidden.HiddenItem
import org.parkjw.apps.pdfbook.hidden.HiddenItemsScreen
import org.parkjw.apps.pdfbook.hidden.HiddenKind
import org.parkjw.apps.pdfbook.settings.StorageSettings
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class HiddenScreenshotsTest {
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
    fun hiddenItemsLightFolded() = captureHidden(dark = false, file = "hidden-items-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun hiddenItemsDarkFolded() = captureHidden(dark = true, file = "hidden-items-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun hiddenItemsLightUnfolded() = captureHidden(dark = false, file = "hidden-items-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun hiddenItemsDarkUnfolded() = captureHidden(dark = true, file = "hidden-items-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun hiddenItemsEmptyLightFolded() = capture(file = "hidden-items-empty-light-folded.png", dark = false) {
        HiddenItemsScreen(items = emptyList(), onBack = {}, onUnhide = {}, modifier = Modifier.fillMaxSize())
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsStorageLightFolded() = captureStorage(dark = false, file = "settings-storage-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsStorageDarkFolded() = captureStorage(dark = true, file = "settings-storage-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun settingsStorageLightUnfolded() = captureStorage(dark = false, file = "settings-storage-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun settingsStorageDarkUnfolded() = captureStorage(dark = true, file = "settings-storage-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun shelfHideMenuLightFolded() {
        val listings = sampleShelfListings()
        val shelf = sampleShelfUi()
        capture(file = "shelf-hide-menu-light-folded.png", dark = false) {
            FolderShelfScreen(
                shelf = shelf,
                covers = sampleShelfCovers(listings),
                snackbarMessage = null,
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
                showMenuFor = shelf.cards.first().uri,
            )
        }
    }

    private fun captureHidden(dark: Boolean, file: String) {
        capture(file, dark) {
            HiddenItemsScreen(
                items = sampleHiddenItems(),
                onBack = {},
                onUnhide = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureStorage(dark: Boolean, file: String) {
        capture(file, dark) {
            Column(Modifier.fillMaxSize()) {
                StorageSettings(
                    hiddenCount = 3,
                    showDotFiles = false,
                    onOpenHiddenItems = {},
                    onShowDotFilesChange = {},
                )
            }
        }
    }

    private fun capture(
        file: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(file) {
            ScreenshotTheme(dark, content = content)
        }
    }
}

private fun sampleHiddenItems(): List<HiddenItem> = listOf(
    HiddenItem(HiddenKind.Folder, "content://folder", "Sample Folder", "content://folder"),
    HiddenItem(HiddenKind.Book, "content://b1", "Test Book 01", "content://folder"),
    HiddenItem(HiddenKind.Book, "content://b2", "Test Book 02", "content://folder"),
)
