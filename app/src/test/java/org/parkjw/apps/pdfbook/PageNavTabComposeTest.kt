package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class PageNavTabComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var fontScale by mutableFloatStateOf(1f)
    private var clicks by mutableIntStateOf(0)

    @Test
    fun tabKeeps40dpChipAnd48dpTouchTargetAtLargeFont() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Row(Modifier.fillMaxWidth()) {
                    NavTab(R.string.page_nav_tab_contents, selected = true, tag = "tab") { clicks++ }
                }
            }
        }
        assertSizes(minChip = 40.dp)
        // The 40dp chip is centred in the 48dp interactive area, so a tap 3dp above it still lands on the tab.
        composeRule.onNodeWithTag("tab").performTouchInput { click(Offset(center.x, -3.dp.toPx())) }
        composeRule.waitForIdle()
        assertEquals(1, clicks)
        fontScale = 2f
        composeRule.waitForIdle()
        assertSizes(minChip = 48.dp)
        composeRule.onNodeWithTag("tab").performTouchInput { click() }
        composeRule.waitForIdle()
        assertEquals(2, clicks)
    }

    private fun assertSizes(minChip: Dp) {
        val tab = composeRule.onNodeWithTag("tab").assertIsDisplayed()
        val bounds = tab.getBoundsInRoot()
        val chip = bounds.bottom - bounds.top
        assertTrue("chip $chip at $fontScale", chip >= minChip)
        composeRule.onNodeWithText("목차", useUnmergedTree = true).assertIsDisplayed()
    }
}
