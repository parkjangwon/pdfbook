package org.parkjw.apps.pdfbook.lock

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockSourceGuardTest {
    @Test
    fun l6_lockSourcesDoNotStorePlainPin() {
        val root = projectRoot()
        val banned = Regex("""stringPreferencesKey\(\s*"pin"\s*\)""")
        val offenders = File(root, "app/src/main/java/org/parkjw/apps/pdfbook/lock").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (banned.containsMatchIn(line)) "${file.name}:${index + 1}" else null
                }
            }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun l9_sourcesDoNotUseFlagSecure() {
        val javaRoot = File(projectRoot(), "app/src/main/java")
        val hit = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .any { it.readText().contains("FLAG_SECURE") }
        assertFalse(hit)
        val activity = File(javaRoot, "org/parkjw/apps/pdfbook/MainActivity.kt").readText()
        assertTrue(activity.contains("setRecentsScreenshotEnabled"))
    }

    @Test
    fun l10_lockScreenSignatureHasNoLibraryData() {
        val source = File(
            projectRoot(),
            "app/src/main/java/org/parkjw/apps/pdfbook/lock/PinEntry.kt",
        ).readText()
        val params = Regex("""fun LockScreen\((.*?)\)\s*\{""", RegexOption.DOT_MATCHES_ALL).find(source)!!.groupValues[1]
        assertFalse(params.contains("Folder"))
        assertFalse(params.contains("Book"))
        assertFalse(params.contains("cover"))
    }

    private fun projectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(8) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("settings.gradle.kts not found")
    }
}
