package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.LocalDisplayMode
import org.parkjw.apps.pdfbook.OutlineEntry
import org.parkjw.apps.pdfbook.PageNavSheet
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageNavScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    fun buttonLightFolded() = captureButton("reader-page-nav-button-light-folded.png", dark = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun buttonDarkFolded() = captureButton("reader-page-nav-button-dark-folded.png", dark = true)

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun buttonLightFoldedEn() = captureButton("reader-page-nav-button-light-folded-en.png", dark = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun thumbnailsLightFolded() =
        captureSheet("reader-page-nav-thumbnails-light-folded.png", dark = false, pagesTab = true)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun thumbnailsDarkFolded() =
        captureSheet("reader-page-nav-thumbnails-dark-folded.png", dark = true, pagesTab = true)

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun thumbnailsLightFoldedEn() =
        captureSheet("reader-page-nav-thumbnails-light-folded-en.png", dark = false, pagesTab = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun thumbnailsInvertedLightFolded() =
        captureSheet(
            "reader-page-nav-thumbnails-inverted-light-folded.png",
            dark = false,
            pagesTab = true,
            displayMode = DisplayMode.Invert,
        )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun thumbnailsSepiaLightFolded() =
        captureSheet(
            "reader-page-nav-thumbnails-sepia-light-folded.png",
            dark = false,
            pagesTab = true,
            displayMode = DisplayMode.Sepia,
        )

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun thumbnailsLightUnfolded() =
        captureSheet("reader-page-nav-thumbnails-light-unfolded.png", dark = false, pagesTab = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun contentsLightFolded() =
        captureSheet("reader-page-nav-contents-light-folded.png", dark = false, pagesTab = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun contentsDarkFolded() =
        captureSheet("reader-page-nav-contents-dark-folded.png", dark = true, pagesTab = false)

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun contentsLightFoldedEn() =
        captureSheet("reader-page-nav-contents-light-folded-en.png", dark = false, pagesTab = false)

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureButton(file: String, dark: Boolean) {
        composeRule.setContent { ReaderContent(dark) }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureSheet(
        file: String,
        dark: Boolean,
        pagesTab: Boolean,
        displayMode: DisplayMode = DisplayMode.Default,
    ) {
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                CompositionLocalProvider(
                    LocalIndication provides NoRippleIndication,
                    LocalDisplayMode provides displayMode,
                ) {
                    Box(Modifier.fillMaxSize().background(ReaderBackground)) {
                        PageNavSheet(
                            renderer = samplePages(),
                            outline = sampleOutline,
                            pageCount = 120,
                            currentPage = 37,
                            onPick = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        if (pagesTab) {
            // NoRippleIndication keeps the clicked chip at readerChip, not a frozen pressed state layer.
            composeRule.onNodeWithTag("page_nav_tab_pages").performClick()
            composeRule.waitForIdle()
        }
        composeRule.mainClock.autoAdvance = false
        captureScreenRoboImage(file)
    }

    @Composable
    private fun ReaderContent(dark: Boolean) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        ScreenshotTheme(dark, ReaderBackground) {
            ReaderScreen(
                renderer = samplePages(),
                pagerState = rememberPagerState(initialPage = 36, pageCount = { pageSpreads.size }),
                spreads = pageSpreads,
                title = "샘플 시리즈 5권",
                pageCount = pageCount,
                currentPage = 37,
                twoPage = false,
                onBack = {},
                onDirectionChange = {},
                direction = ReadingDirection.RTL,
                overlayVisible = true,
                showTapZoneGuide = false,
                volumes = sampleReaderVolumes(5),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun samplePages() = SolidPageBitmaps(
        pageCount = 120,
        pageColor = 0xFFF7F2E8.toInt(),
        accentColor = 0xFF3D5A80.toInt(),
        outlineEntries = sampleOutline,
    )

    private val sampleOutline = listOf(
        OutlineEntry("표지", 1, 0),
        OutlineEntry("1장 · 첫 만남", 3, 0),
        OutlineEntry("1.1 골목길", 7, 1),
        OutlineEntry("2장 · 긴 오후", 21, 0),
        OutlineEntry("2.1 창가", 29, 1),
        OutlineEntry("3장 · 돌아오는 길", 48, 0),
    )
}
