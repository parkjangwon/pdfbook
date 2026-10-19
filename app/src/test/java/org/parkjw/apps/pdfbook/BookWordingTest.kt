package org.parkjw.apps.pdfbook

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class BookWordingTest {
    @Test
    fun stringsAndReadmeHaveNoComicWording() {
        val root = projectRoot()
        val stringFiles = File(root, "app/src/main/res")
            .listFiles { f -> f.isDirectory && f.name.startsWith("values") }
            ?.flatMap { dir -> dir.listFiles { f -> f.isFile && f.name.matches(Regex("strings.*\\.xml")) }.orEmpty().toList() }
            .orEmpty()
            .sortedBy { it.path }
        assertTrue("no strings.xml under values*", stringFiles.isNotEmpty())
        val readme = File(root, "README.md")
        if (!readme.isFile) fail("missing README.md at ${readme.path}")

        val forbidden = Regex("만화|comic|manga", RegexOption.IGNORE_CASE)
        for (file in stringFiles + readme) {
            val text = file.readText()
            val hit = forbidden.find(text)
            if (hit != null) {
                fail("${file.relativeTo(root)} contains '${hit.value}'")
            }
        }
    }

    private fun projectRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        throw AssertionError("project root not found from ${System.getProperty("user.dir")}")
    }
}
