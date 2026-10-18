package org.parkjw.apps.pdfbook.lock

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.MainActivity
import org.parkjw.apps.pdfbook.update.UPDATE_CACHE_DIR
import org.parkjw.apps.pdfbook.update.launchApkInstaller
import org.parkjw.apps.pdfbook.update.openInstallPermissionSettings
import org.robolectric.Shadows.shadowOf
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

    @Test
    fun u7_l3_installerReturnKeepsAppUnlocked() {
        AppLockProcess.state.unlock()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val apk = File(File(activity.cacheDir, UPDATE_CACHE_DIR), "pdfbook-update.apk")
                apk.parentFile?.mkdirs()
                apk.writeBytes(byteArrayOf(0x50, 0x4B))
                assertTrue(activity.launchApkInstaller(apk))
                val started = requireNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals(Intent.ACTION_VIEW, started.action)
                assertEquals("application/vnd.android.package-archive", started.type)
                assertTrue(AppLockProcess.state.pendingExempt())
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { assertTrue(AppLockProcess.state.unlocked) }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                assertTrue(AppLockProcess.state.unlocked)
                assertFalse(AppLockProcess.state.pendingExempt())
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { assertFalse(AppLockProcess.state.unlocked) }
        }
    }

    @Test
    fun u7_l3_installPermissionSettingsReturnKeepsAppUnlocked() {
        AppLockProcess.state.unlock()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.openInstallPermissionSettings())
                val started = requireNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, started.action)
                assertEquals("package:${activity.packageName}", started.dataString)
                assertTrue(AppLockProcess.state.pendingExempt())
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                assertTrue(AppLockProcess.state.unlocked)
                assertFalse(AppLockProcess.state.pendingExempt())
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { assertFalse(AppLockProcess.state.unlocked) }
        }
    }
}
