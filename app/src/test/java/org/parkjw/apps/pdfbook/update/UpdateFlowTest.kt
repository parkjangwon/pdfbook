package org.parkjw.apps.pdfbook.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class UpdateFlowTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class MemoryStore : UpdateStore {
        var day: Long? = null
        var snoozed: UpdateSnooze? = null
        override suspend fun lastAutoCheckDay() = day
        override suspend fun setLastAutoCheckDay(epochDay: Long) {
            day = epochDay
        }
        override suspend fun snooze() = snoozed
        override suspend fun setSnooze(snooze: UpdateSnooze) {
            snoozed = snooze
        }
    }

    private var response: () -> ReleaseResponse = { ReleaseResponse(200, ReleaseFixtures.list(ReleaseFixtures.release("v0.3.1"))) }
    private var fetches = 0
    private var canInstall = true
    private var signatureOk = true
    private var downloadError: IOException? = null
    private val progress = mutableListOf<Int>()
    private val store = MemoryStore()
    private var today = 100L

    private fun flow() = UpdateFlow(
        checker = UpdateChecker {
            fetches++
            response()
        },
        downloader = { asset, onProgress ->
            downloadError?.let { throw it }
            listOf(0, 50, 100).forEach {
                progress += it
                onProgress(it)
            }
            tmp.newFile(asset.name)
        },
        verifier = { signatureOk },
        permission = { canInstall },
        store = store,
        currentVersionName = { "0.3.0" },
        today = { today },
    )

    @Test
    fun autoCheckIsDailyAndRespectsSwitch() = runBlocking {
        flow().autoCheckOnLaunch(enabled = false)
        assertEquals(0, fetches)
        val f = flow()
        f.autoCheckOnLaunch(enabled = true)
        assertEquals(1, fetches)
        assertEquals(100L, store.day)
        flow().autoCheckOnLaunch(enabled = true)
        assertEquals(1, fetches)
        today = 101
        flow().autoCheckOnLaunch(enabled = true)
        assertEquals(2, fetches)
    }

    @Test
    fun autoCheckPromptsAndLaterSnoozesUntilNextDay() = runBlocking {
        val f = flow()
        f.autoCheckOnLaunch(enabled = true)
        val available = f.state.value as UpdateUiState.Available
        assertTrue(available.prompt)
        f.later()
        assertEquals(UpdateSnooze("0.3.1", 100), store.snoozed)
        assertFalse((f.state.value as UpdateUiState.Available).prompt)
        store.day = null
        val again = flow().also { it.autoCheckOnLaunch(enabled = true) }
        assertFalse((again.state.value as UpdateUiState.Available).prompt)
        today = 101
        val nextDay = flow().also { it.autoCheckOnLaunch(enabled = true) }
        assertTrue((nextDay.state.value as UpdateUiState.Available).prompt)
    }

    @Test
    fun autoCheckFailureAndUpToDateStaySilent() = runBlocking {
        response = { throw IOException("offline") }
        val failing = flow().also { it.autoCheckOnLaunch(enabled = true) }
        assertEquals(UpdateUiState.Idle, failing.state.value)
        today = 101
        response = { ReleaseResponse(200, ReleaseFixtures.list(ReleaseFixtures.release("v0.3.0"))) }
        val latest = flow().also { it.autoCheckOnLaunch(enabled = true) }
        assertEquals(UpdateUiState.Idle, latest.state.value)
    }

    @Test
    fun manualCheckIgnoresSwitchAndThrottleAndShowsEveryOutcome() = runBlocking {
        store.day = today
        val f = flow()
        f.manualCheck()
        assertEquals(1, fetches)
        assertFalse((f.state.value as UpdateUiState.Available).prompt)
        response = { ReleaseResponse(200, ReleaseFixtures.list(ReleaseFixtures.release("v0.3.0"))) }
        f.manualCheck()
        assertEquals(UpdateUiState.UpToDate, f.state.value)
        response = { throw IOException("offline") }
        f.manualCheck()
        assertEquals(UpdateUiState.Failed(UpdateProblem.Check, RELEASES_PAGE_URL), f.state.value)
        response = { ReleaseResponse(500, "") }
        f.manualCheck()
        assertEquals(UpdateUiState.Failed(UpdateProblem.Check, RELEASES_PAGE_URL), f.state.value)
    }

    @Test
    fun downloadVerifiesAndHandsOffToInstaller() = runBlocking {
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        assertEquals(listOf(0, 50, 100), progress)
        val ready = f.state.value as UpdateUiState.ReadyToInstall
        assertEquals("pdfbook-0.3.1.apk", ready.apk.name)
        f.installerLaunched(true)
        assertTrue(f.state.value is UpdateUiState.Available)
    }

    @Test
    fun mismatchedSignatureNeverReachesInstallerAndLinksReleasePage() = runBlocking {
        signatureOk = false
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        assertEquals(
            UpdateUiState.Failed(UpdateProblem.Verify, "https://github.com/parkjangwon/pdfbook/releases/tag/v0.3.1"),
            f.state.value,
        )
        assertFalse(File(tmp.root, "pdfbook-0.3.1.apk").exists())
    }

    @Test
    fun downloadFailureAndMissingApkLinkReleasePage() = runBlocking {
        downloadError = IOException("reset")
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        assertEquals(UpdateProblem.Download, (f.state.value as UpdateUiState.Failed).problem)

        downloadError = null
        response = {
            ReleaseResponse(200, ReleaseFixtures.list(ReleaseFixtures.release("v0.3.1", assets = emptyList())))
        }
        val noApk = flow().also { it.manualCheck() }
        noApk.downloadAndInstall()
        assertEquals(
            UpdateUiState.Failed(UpdateProblem.Download, "https://github.com/parkjangwon/pdfbook/releases/tag/v0.3.1"),
            noApk.state.value,
        )
    }

    @Test
    fun missingInstallPermissionWaitsThenResumes() = runBlocking {
        canInstall = false
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        assertTrue(f.state.value is UpdateUiState.NeedsInstallPermission)
        assertFalse(f.onResumed())
        assertTrue(f.state.value is UpdateUiState.NeedsInstallPermission)
        canInstall = true
        assertTrue(f.onResumed())
        f.downloadAndInstall()
        assertTrue(f.state.value is UpdateUiState.ReadyToInstall)
    }

    @Test
    fun installerLaunchFailureFallsBackToReleasePage() = runBlocking {
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        f.installerLaunched(false)
        assertEquals(UpdateProblem.Verify, (f.state.value as UpdateUiState.Failed).problem)
    }

    @Test
    fun releasePageFallsBackWhenHtmlUrlIsForeign() = runBlocking {
        signatureOk = false
        response = {
            ReleaseResponse(
                200,
                """[{"tag_name":"v0.3.1","html_url":"https://evil.example/x","assets":[{"name":"pdfbook-0.3.1.apk","browser_download_url":"https://github.com/a.apk","size":1}]}]""",
            )
        }
        val f = flow().also { it.manualCheck() }
        f.downloadAndInstall()
        assertEquals(RELEASES_PAGE_URL, (f.state.value as UpdateUiState.Failed).releasePageUrl)
    }
}
