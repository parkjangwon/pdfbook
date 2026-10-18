package org.parkjw.apps.pdfbook.update

import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UpdateProblem {
    Check,
    Download,
    Verify,
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: Release, val apk: ReleaseAsset?, val prompt: Boolean) : UpdateUiState
    data class NeedsInstallPermission(val release: Release, val apk: ReleaseAsset) : UpdateUiState
    data class Downloading(val release: Release, val percent: Int) : UpdateUiState
    data class ReadyToInstall(val release: Release, val apk: File) : UpdateUiState
    data class Failed(val problem: UpdateProblem, val releasePageUrl: String) : UpdateUiState
}

val UpdateUiState.isBusy: Boolean
    get() = this is UpdateUiState.Checking ||
        this is UpdateUiState.Downloading ||
        this is UpdateUiState.ReadyToInstall

fun interface InstallPermission {
    fun canRequestPackageInstalls(): Boolean
}

class UpdateFlow(
    private val checker: UpdateChecker,
    private val downloader: ApkDownloader,
    private val verifier: ApkVerifier,
    private val permission: InstallPermission,
    private val store: UpdateStore,
    private val currentVersionName: () -> String?,
    private val today: () -> Long,
) {
    private val mutableState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()

    suspend fun autoCheckOnLaunch(enabled: Boolean) {
        if (state.value != UpdateUiState.Idle) return
        val day = today()
        if (!UpdatePolicy.shouldAutoCheck(enabled, store.lastAutoCheckDay(), day)) return
        store.setLastAutoCheckDay(day)
        check(manual = false)
    }

    suspend fun manualCheck() {
        if (state.value.isBusy) return
        check(manual = true)
    }

    private suspend fun check(manual: Boolean) {
        mutableState.value = if (manual) UpdateUiState.Checking else UpdateUiState.Idle
        mutableState.value = when (val result = checker.check(currentVersionName())) {
            is UpdateCheckResult.UpToDate -> if (manual) UpdateUiState.UpToDate else UpdateUiState.Idle
            is UpdateCheckResult.Available -> {
                val prompt = !manual && UpdatePolicy.shouldPromptAfterAutoCheck(
                    result.release.version ?: result.current,
                    store.snooze(),
                    today(),
                )
                UpdateUiState.Available(result.release, result.apk, prompt)
            }
            is UpdateCheckResult.Failed ->
                if (UpdatePolicy.showsFailure(manual)) {
                    UpdateUiState.Failed(UpdateProblem.Check, RELEASES_PAGE_URL)
                } else {
                    UpdateUiState.Idle
                }
        }
    }

    suspend fun later() {
        val current = state.value as? UpdateUiState.Available ?: return
        current.release.version?.let { store.setSnooze(UpdateSnooze(it.toString(), today())) }
        mutableState.value = current.copy(prompt = false)
    }

    fun downloadAndInstall() {
        val release = when (val current = state.value) {
            is UpdateUiState.Available -> current.release to current.apk
            is UpdateUiState.NeedsInstallPermission -> current.release to current.apk
            else -> return
        }
        val (target, apk) = release
        if (apk == null) {
            mutableState.value = UpdateUiState.Failed(UpdateProblem.Download, releasePage(target))
            return
        }
        if (!permission.canRequestPackageInstalls()) {
            mutableState.value = UpdateUiState.NeedsInstallPermission(target, apk)
            return
        }
        mutableState.value = UpdateUiState.Downloading(target, 0)
        val file = try {
            downloader.download(apk) { percent ->
                mutableState.value = UpdateUiState.Downloading(target, percent)
            }
        } catch (e: IOException) {
            mutableState.value = UpdateUiState.Failed(UpdateProblem.Download, releasePage(target))
            return
        }
        mutableState.value = if (verifier.matchesInstalledApp(file)) {
            UpdateUiState.ReadyToInstall(target, file)
        } else {
            file.delete()
            UpdateUiState.Failed(UpdateProblem.Verify, releasePage(target))
        }
    }

    fun onResumed(): Boolean {
        val current = state.value as? UpdateUiState.NeedsInstallPermission ?: return false
        return permission.canRequestPackageInstalls().also { granted ->
            if (granted) mutableState.value = UpdateUiState.Available(current.release, current.apk, prompt = false)
        }
    }

    fun installerLaunched(launched: Boolean) {
        val current = state.value as? UpdateUiState.ReadyToInstall ?: return
        mutableState.value = if (launched) {
            UpdateUiState.Available(current.release, current.release.apkAsset(), prompt = false)
        } else {
            UpdateUiState.Failed(UpdateProblem.Verify, releasePage(current.release))
        }
    }

    private fun releasePage(release: Release): String =
        release.htmlUrl.takeIf { it.startsWith("https://github.com/parkjangwon/pdfbook/") } ?: RELEASES_PAGE_URL
}
