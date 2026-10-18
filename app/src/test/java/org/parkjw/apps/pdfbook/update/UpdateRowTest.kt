package org.parkjw.apps.pdfbook.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun manualCheckOutcomesEachHaveText() {
        assertNull(updateRow(UpdateUiState.Idle).status)
        assertEquals(R.string.update_checking, updateRow(UpdateUiState.Checking).status?.text)
        assertFalse(updateRow(UpdateUiState.Checking).checkEnabled)
        assertEquals(R.string.update_latest, updateRow(UpdateUiState.UpToDate).status?.text)
        val available = updateRow(UpdateUiState.Available(release, apk, prompt = false))
        assertEquals(UpdateStatus(R.string.update_available, "0.3.1"), available.status)
        assertEquals(listOf(UpdateAction.Download), available.actions)
    }

    @Test
    fun everyFailureOffersReleasePage() {
        mapOf(
            UpdateProblem.Check to R.string.update_check_failed,
            UpdateProblem.Download to R.string.update_download_failed,
            UpdateProblem.Verify to R.string.update_verify_failed,
        ).forEach { (problem, text) ->
            val row = updateRow(UpdateUiState.Failed(problem, RELEASES_PAGE_URL))
            assertEquals(text, row.status?.text)
            assertTrue(row.status!!.isError)
            assertEquals(listOf(UpdateAction.OpenReleasePage), row.actions)
            assertTrue(row.checkEnabled)
        }
    }

    @Test
    fun permissionAndProgressStates() {
        val permission = updateRow(UpdateUiState.NeedsInstallPermission(release, apk))
        assertEquals(R.string.update_need_permission, permission.status?.text)
        assertEquals(listOf(UpdateAction.OpenSettings), permission.actions)
        val downloading = updateRow(UpdateUiState.Downloading(release, 42))
        assertEquals(UpdateStatus(R.string.update_downloading, 42), downloading.status)
        assertEquals(0.42f, downloading.progress!!, 0.0001f)
        assertFalse(downloading.checkEnabled)
        assertEquals(1f, updateRow(UpdateUiState.ReadyToInstall(release, File("x"))).progress!!, 0f)
    }
}
