package org.parkjw.apps.pdfbook

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class FolderStringsTest {
    private val keys = listOf(
        "folders_title",
        "folders_add",
        "folder_rename",
        "folder_repick",
        "folder_remove",
        "folder_remove_confirm_title",
        "folder_remove_confirm_body",
        "folder_duplicate",
        "folder_books_count",
        "folder_no_access_badge",
        "last_book_unavailable",
    )

    @Test
    fun folderKeysExistInKoreanAndEnglish() {
        val root = projectRoot()
        val ko = loadNames(File(root, "app/src/main/res/values-ko"))
        val en = loadNames(File(root, "app/src/main/res/values-en"))
        val defaults = loadNames(File(root, "app/src/main/res/values"))
        for (key in keys) {
            assertTrue("missing $key in values-ko", key in ko)
            assertTrue("missing $key in values-en", key in en)
            assertTrue("missing $key in values", key in defaults)
        }
    }

    private fun loadNames(dir: File): Set<String> {
        if (!dir.isDirectory) fail("missing ${dir.path}")
        val names = linkedSetOf<String>()
        val factory = DocumentBuilderFactory.newInstance()
        for (file in dir.listFiles { f -> f.isFile && f.extension == "xml" }.orEmpty()) {
            val doc = factory.newDocumentBuilder().parse(file)
            for (tag in listOf("string", "plurals")) {
                val nodes = doc.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val name = nodes.item(i).attributes?.getNamedItem("name")?.nodeValue
                    if (name != null) names += name
                }
            }
        }
        return names
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
