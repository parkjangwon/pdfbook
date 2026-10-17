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
        assertTrue("folder_books_count must be plurals in values-en", "folder_books_count" in en.plurals)
        assertTrue("folder_books_count must be plurals in values-ko", "folder_books_count" in ko.plurals)
        assertTrue("folder_books_count must be plurals in values", "folder_books_count" in defaults.plurals)
        assertTrue("en folder_books_count needs one", "one" in en.quantities.getValue("folder_books_count"))
        assertTrue("en folder_books_count needs other", "other" in en.quantities.getValue("folder_books_count"))
    }

    private data class ResNames(
        val all: Set<String>,
        val plurals: Set<String>,
        val quantities: Map<String, Set<String>>,
    ) {
        operator fun contains(key: String): Boolean = key in all
    }

    private fun loadNames(dir: File): ResNames {
        if (!dir.isDirectory) fail("missing ${dir.path}")
        val names = linkedSetOf<String>()
        val plurals = linkedSetOf<String>()
        val quantities = linkedMapOf<String, MutableSet<String>>()
        val factory = DocumentBuilderFactory.newInstance()
        for (file in dir.listFiles { f -> f.isFile && f.extension == "xml" }.orEmpty()) {
            val doc = factory.newDocumentBuilder().parse(file)
            val stringNodes = doc.getElementsByTagName("string")
            for (i in 0 until stringNodes.length) {
                val name = stringNodes.item(i).attributes?.getNamedItem("name")?.nodeValue
                if (name != null) names += name
            }
            val pluralNodes = doc.getElementsByTagName("plurals")
            for (i in 0 until pluralNodes.length) {
                val node = pluralNodes.item(i)
                val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                names += name
                plurals += name
                val items = (node as org.w3c.dom.Element).getElementsByTagName("item")
                val qs = quantities.getOrPut(name) { linkedSetOf() }
                for (j in 0 until items.length) {
                    val quantity = items.item(j).attributes?.getNamedItem("quantity")?.nodeValue
                    if (quantity != null) qs += quantity
                }
            }
        }
        return ResNames(names, plurals, quantities)
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
