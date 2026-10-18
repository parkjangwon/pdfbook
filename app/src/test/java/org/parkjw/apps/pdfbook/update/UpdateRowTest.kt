package org.parkjw.apps.pdfbook.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.parkjw.apps.pdfbook.R

class UpdateRowTest {
    private val release = Release(
        tagName = "v0.3.1",
        version = AppVersion(0, 3, 1),
        name = "v0.3.1",
        htmlUrl = "https://github.com/parkjangwon/pdfbook/releases/tag/v0.3.1",
        notes = "",
        publishedAt = null,
        draft = false,
        prerelease = false,
        assets = emptyList(),
    )
    private val apk = ReleaseAsset("pdfbook-0.3.1.apk", "https://github.com/x", 1, "")
    private val checkTitle = UpdateText.Res(R.string.update_check)
    private val availableTitle = UpdateText.Res(R.string.update_available, "v0.3.1")

    @Test
    fun idleCheckingAndLatestKeepCheckRow() {
        assertEquals(
            UpdateRow(checkTitle, UpdateText.WithCurrentVersion(null), UpdateTrailing.Check),
            updateRow(UpdateUiState.Idle),
        )
        assertEquals(
            UpdateRow(checkTitle, UpdateText.Res(R.string.update_checking), UpdateTrailing.Spinner),
            updateRow(UpdateUiState.Checking),
        )
        assertEquals(
            UpdateRow(checkTitle, UpdateText.CurrentVersionLatest, UpdateTrailing.Check),
            updateRow(UpdateUiState.UpToDate),
        )
    }

    @Test
    fun availableShowsVersionsAndInkDownload() {
        assertEquals(
            UpdateRow(
                availableTitle,
                UpdateText.WithCurrentVersion(R.string.update_current_version),
                UpdateTrailing.Download,
            ),
            updateRow(UpdateUiState.Available(release, apk, prompt = false)),
        )
    }

    @Test
    fun permissionAndDownloading() {
        assertEquals(
            UpdateRow(
                availableTitle,
                UpdateText.Res(R.string.update_need_permission),
                bottomAction = UpdateAction.OpenSettings,
            ),
            updateRow(UpdateUiState.NeedsInstallPermission(release, apk)),
        )
        val downloading = updateRow(UpdateUiState.Downloading(release, 62))
        assertEquals(UpdateText.Res(R.string.update_downloading, 62), downloading.title)
        assertEquals(0.62f, downloading.progress!!, 0.0001f)
        assertEquals(UpdateText.Res(R.string.update_download_hint), downloading.hint)
        assertNull(downloading.bottomAction)
        assertEquals(1f, updateRow(UpdateUiState.ReadyToInstall(release, File("x"))).progress!!, 0f)
    }

    @Test
    fun manualCheckFailureKeepsCheckButton() {
        assertEquals(
            UpdateRow(checkTitle, UpdateText.Res(R.string.update_check_failed), UpdateTrailing.Check),
            updateRow(UpdateUiState.Failed(UpdateProblem.Check, RELEASES_PAGE_URL)),
        )
    }

    @Test
    fun downloadVerifyAndInstallFailuresOfferReleasePage() {
        mapOf(
            UpdateProblem.Download to R.string.update_download_failed,
            UpdateProblem.Verify to R.string.update_verify_failed,
            UpdateProblem.Install to R.string.update_install_failed,
        ).forEach { (problem, text) ->
            assertEquals(
                UpdateRow(availableTitle, UpdateText.Res(text), bottomAction = UpdateAction.OpenReleasePage),
                updateRow(UpdateUiState.Failed(problem, RELEASES_PAGE_URL, release)),
            )
            assertEquals(checkTitle, updateRow(UpdateUiState.Failed(problem, RELEASES_PAGE_URL)).title)
        }
    }

    @Test
    fun versionsAreShownWithPrefix() {
        assertEquals("v0.3.1", displayVersion(release))
        assertEquals("v0.3.0", displayVersion("0.3.0"))
        assertEquals("v0.3.0", displayVersion("v0.3.0"))
        assertEquals("", displayVersion(""))
    }
}
