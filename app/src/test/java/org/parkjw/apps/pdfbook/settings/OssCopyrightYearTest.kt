package org.parkjw.apps.pdfbook.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OssCopyrightYearTest {
    private val licenseFiles = mapOf(
        "oss_pdfbook_sans_copyright" to "licenses/Pretendard-OFL.txt",
        "oss_jetbrains_mono_copyright" to "licenses/JetBrainsMono-OFL.txt",
    )

    @Test
    fun ossCopyrightYearsMatchBundledLicenseFiles() {
        val root = projectRoot()
        for ((key, path) in licenseFiles) {
            val file = File(root, path)
            assertTrue("$path missing", file.isFile)
            val expectedYear = firstYear(file.readLines().first { it.isNotBlank() })
            assertNotNull("$path has no year on first line", expectedYear)
            for (dir in listOf("values", "values-ko", "values-en")) {
                val values = loadStrings(File(root, "app/src/main/res/$dir/strings_settings.xml"))
                val text = values[key]
                assertNotNull("$dir/$key missing", text)
                assertEquals("$dir/$key year", expectedYear, firstYear(text!!))
            }
        }
    }

    private fun firstYear(text: String): String? = Regex("""\b(19|20)\d{2}\b""").find(text)?.value

    private fun loadStrings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val values = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            values[name] = node.textContent
        }
        return values
    }

    private fun projectRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        throw AssertionError("project root not found")
    }
}
