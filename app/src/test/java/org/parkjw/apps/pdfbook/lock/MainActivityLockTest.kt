package org.parkjw.apps.pdfbook.lock

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.MainActivity
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class MainActivityLockTest {
    @Before
    fun reset() {
        AppLockProcess.state.resetForTest()
    }

    @Test
    fun l2_l3_recreateKeepsUnlockAndBackgroundLocks() {
        AppLockProcess.state.unlock()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { assertTrue(AppLockProcess.state.unlocked) }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { assertFalse(AppLockProcess.state.unlocked) }
        }
    }

    @Test
    fun l3_documentTreeIsExemptAndHttpsViewIsNot() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AppLockProcess.state.resetForTest()
                runCatching {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")))
                }
                assertFalse(AppLockProcess.state.pendingExempt())
                runCatching { activity.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)) }
                assertTrue(AppLockProcess.state.pendingExempt())
            }
        }
    }
}
