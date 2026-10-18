package org.parkjw.apps.pdfbook.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AboutStringsTest {
    @Test
    fun aboutKeysExistWithVersionFormat() {
        val root = projectRoot()
        val expected = mapOf(
            "values" to ("정보" to "버전 %1\$s"),
            "values-ko" to ("정보" to "버전 %1\$s"),
            "values-en" to ("About" to "Version %1\$s"),
        )
        for ((dir, pair) in expected) {
            val (section, version) = pair
            val values = loadStrings(File(root, "app/src/main/res/$dir/strings.xml"))
            assertEquals(section, values["settings_section_about"])
            assertEquals(version, values["about_version"])
            assertTrue(values.getValue("about_version").contains("%1\$s"))
        }
    }

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
