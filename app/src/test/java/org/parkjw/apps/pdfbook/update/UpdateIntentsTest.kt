package org.parkjw.apps.pdfbook.update

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.parkjw.apps.pdfbook.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class UpdateIntentsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    // FileProvider caches path roots statically; Robolectric gives each test a new cacheDir.
    @Before
    fun clearFileProviderCache() {
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
            .let { (it.get(null) as MutableMap<*, *>).clear() }
    }

    private fun throwing(error: () -> Throwable): Context =
        object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) = throw error()
        }

    private fun cacheApk(ctx: Context = app): File =
        File(File(ctx.cacheDir, UPDATE_CACHE_DIR), "pdfbook-update.apk").also {
            it.parentFile?.mkdirs()
            it.writeBytes(byteArrayOf(0x50, 0x4B))
        }

    @Test
    fun activityNotFoundReturnsFalseForEveryLauncher() {
        val ctx = throwing { ActivityNotFoundException("none") }
        assertFalse(ctx.launchApkInstaller(cacheApk()))
        assertFalse(ctx.openInstallPermissionSettings())
        assertFalse(ctx.openBrowsableUrl("https://example.com"))
        assertFalse(ctx.openReleasePage("https://example.com"))
    }

    @Test
    fun securityExceptionReturnsFalse() {
        val ctx = throwing { SecurityException("denied") }
        assertFalse(ctx.launchApkInstaller(cacheApk()))
        assertFalse(ctx.openBrowsableUrl("https://example.com"))
    }

    @Test
    fun illegalArgumentFromStartActivityReturnsFalse() {
        assertFalse(throwing { IllegalArgumentException("bad") }.openBrowsableUrl("https://example.com"))
    }

    @Test
    fun otherExceptionsAreNotSwallowed() {
        val ctx = throwing { IllegalStateException("boom") }
        val thrown = runCatching { ctx.openBrowsableUrl("https://example.com") }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun apkOutsideProviderPathsReturnsFalseWithoutLaunching() {
        val outside = File(app.filesDir, "other.apk").also { it.writeBytes(byteArrayOf(1)) }
        assertFalse(app.launchApkInstaller(outside))
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun installerIntentGrantsReadAndUsesApkMime() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.launchApkInstaller(cacheApk(activity)))
                val started = requireNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals(Intent.ACTION_VIEW, started.action)
                assertTrue(started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            }
        }
    }

    @Test
    fun browsableUrlIntentCarriesBrowsableCategory() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.openBrowsableUrl("https://example.com/r"))
                val started = requireNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals("https://example.com/r", started.dataString)
                assertTrue(started.hasCategory(Intent.CATEGORY_BROWSABLE))
            }
        }
    }
}
