package org.parkjw.apps.pdfbook.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkGuardTest {
    private val networkApis = listOf(
        Regex("""\bHttpURLConnection\b"""),
        Regex("""\bHttpsURLConnection\b"""),
        Regex("""\bopenConnection\s*\("""),
        Regex("""\bjava\.net\.(URL|Socket|URI)\b"""),
        Regex("""\bSocket\s*\("""),
        Regex("""\bokhttp3?\b"""),
        Regex("""\bWebView\b"""),
    )
    private val allowed = setOf("GitHubReleaseSource.kt", "ApkDownloader.kt")

    @Test
    fun onlyUpdateCheckAndApkDownloadTouchTheNetwork() {
        val root = projectRoot()
        val offenders = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> networkApis.any { it.containsMatchIn(file.readText()) } }
            .map { it.relativeTo(root).path }
            .filterNot { path -> path.contains("/update/") && File(path).name in allowed }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun networkHostsAreGitHubOnly() {
        val root = projectRoot()
        val hosts = allowed.flatMap { name ->
            Regex("""https://([a-z0-9.-]+)""").findAll(File(root, "app/src/main/java/org/parkjw/apps/pdfbook/update/$name").readText())
                .map { it.groupValues[1] }
                .toList()
        }.toSet()
        assertEquals(setOf("github.com", "api.github.com"), hosts)
    }

    @Test
    fun manifestAddsOnlyUpdatePermissions() {
        val manifest = File(projectRoot(), "app/src/main/AndroidManifest.xml").readText()
        val permissions = Regex("""uses-permission\s+android:name="([^"]+)"""").findAll(manifest)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            setOf("android.permission.INTERNET", "android.permission.REQUEST_INSTALL_PACKAGES"),
            permissions,
        )
    }
}
