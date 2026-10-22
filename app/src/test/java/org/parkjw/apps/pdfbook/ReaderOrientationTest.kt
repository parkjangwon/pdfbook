package org.parkjw.apps.pdfbook

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ScreenOrientation
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class ReaderOrientationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun enteringReaderSetsOrientationAndLeavingRestores() {
        var readerShown by mutableStateOf(true)
        rule.setContent {
            if (readerShown) OrientationLock(ScreenOrientation.Portrait)
        }
        rule.waitForIdle()
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT,
            rule.activity.requestedOrientation,
        )
        rule.runOnUiThread { readerShown = false }
        rule.waitForIdle()
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            rule.activity.requestedOrientation,
        )
    }

    @Test
    fun landscapeFollowsSettingAndChangeReapplies() {
        var orientation by mutableStateOf(ScreenOrientation.Landscape)
        rule.setContent {
            OrientationLock(orientation)
        }
        rule.waitForIdle()
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,
            rule.activity.requestedOrientation,
        )
        rule.runOnUiThread { orientation = ScreenOrientation.Auto }
        rule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, rule.activity.requestedOrientation)
    }
}
