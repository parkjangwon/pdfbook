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
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.FolderShelfScreen
import org.parkjw.apps.pdfbook.FolderShelfUi
import org.parkjw.apps.pdfbook.LibraryScreen
import org.parkjw.apps.pdfbook.Listing
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

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun peekShelfLightFolded() = capturePeekShelf(dark = false, file = "peek-shelf-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun peekShelfDarkFolded() = capturePeekShelf(dark = true, file = "peek-shelf-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun peekShelfLightUnfolded() = capturePeekShelf(dark = false, file = "peek-shelf-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun peekShelfDarkUnfolded() = capturePeekShelf(dark = true, file = "peek-shelf-dark-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun peekMenuLightFolded() {
        val listings = sampleShelfListings()
        capture(file = "peek-menu-light-folded.png", dark = false) {
            FolderShelfScreen(
                shelf = samplePeekShelfUi(),
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
                peekEnabled = true,
                peekMenuEnabled = true,
                showPeekMenu = true,
            )
        }
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun peekLibraryLightFolded() {
        val listing = samplePeekListing()
        capture(file = "peek-library-light-folded.png", dark = false) {
            LibraryScreen(
                listing = listing,
                states = sampleStates(listing),
                onOpen = {},
                onSettings = {},
                covers = sampleCovers(listing),
                onBack = {},
                peekEnabled = true,
                peekMenuEnabled = true,
                peekHiddenBookIds = setOf(listing.books[1].uri),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun capturePeekShelf(dark: Boolean, file: String) {
        val listings = sampleShelfListings()
        capture(file, dark) {
            FolderShelfScreen(
                shelf = samplePeekShelfUi(),
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
                peekEnabled = true,
                peekMenuEnabled = true,
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

private fun samplePeekShelfUi(): FolderShelfUi {
    val base = sampleShelfUi()
    return base.copy(
        cards = base.cards.mapIndexed { index, card ->
            if (index < 2) {
                card.copy(
                    displayName = if (index == 0) "Sample Folder" else "Archive Folder",
                    isHidden = true,
                    resume = null,
                )
            } else {
                card
            }
        },
    )
}

private fun samplePeekListing(): Listing = Listing(
    folderName = "Sample Folder",
    books = listOf(
        BookInfo("content://peek/a.pdf", "Test Book 01.pdf", 1000),
        BookInfo("content://peek/b.pdf", "Test Book 02.pdf", 2000),
        BookInfo("content://peek/c.pdf", "Test Book 03.pdf", 3000),
    ),
)
