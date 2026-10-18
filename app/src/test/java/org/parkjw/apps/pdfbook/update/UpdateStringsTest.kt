package org.parkjw.apps.pdfbook.update

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStringsTest {
    private val ko = mapOf(
        "settings_section_update" to "업데이트",
        "update_check" to "업데이트 확인",
        "update_auto" to "자동으로 업데이트 확인",
        "update_auto_desc" to "앱을 켤 때 하루 한 번 확인해요",
        "update_checking" to "확인하는 중…",
        "update_latest" to "최신 버전이에요",
        "update_available" to "새 버전이 있어요 (%1\$s)",
        "update_download" to "받고 설치하기",
        "update_later" to "나중에",
        "update_downloading" to "받는 중 %1\$d%%",
        "update_need_permission" to "설치하려면 PDFBook의 앱 설치를 허용해 주세요",
        "update_open_settings" to "설정 열기",
        "update_check_failed" to "업데이트를 확인하지 못했어요. 인터넷 연결을 확인해 주세요.",
        "update_download_failed" to "파일을 받지 못했어요",
        "update_verify_failed" to "파일을 확인하지 못해 설치하지 않았어요. 릴리스 페이지에서 받아 주세요.",
        "update_open_release" to "릴리스 페이지 열기",
    )
    private val en = mapOf(
        "settings_section_update" to "Updates",
        "update_check" to "Check for updates",
        "update_auto" to "Check for updates automatically",
        "update_auto_desc" to "Checks once a day when you open the app",
        "update_checking" to "Checking…",
        "update_latest" to "You're up to date",
        "update_available" to "New version available (%1\$s)",
        "update_download" to "Download and install",
        "update_later" to "Later",
        "update_downloading" to "Downloading %1\$d%%",
        "update_need_permission" to "Allow PDFBook to install apps to continue",
        "update_open_settings" to "Open settings",
        "update_check_failed" to "Couldn't check for updates. Check your connection.",
        "update_download_failed" to "Couldn't download the file",
        "update_verify_failed" to "Couldn't verify the file, so it wasn't installed. Please get it from the release page.",
        "update_open_release" to "Open release page",
    )

    @Test
    fun updateCopyMatchesScopeDocument() {
        assertEquals(16, ko.size)
        val root = projectRoot()
        for ((dir, expected) in listOf("values" to ko, "values-ko" to ko, "values-en" to en)) {
            val values = loadStrings(File(root, "app/src/main/res/$dir/strings.xml"))
            expected.forEach { (key, value) -> assertEquals("$dir/$key", value, values[key]) }
        }
    }

    private fun loadStrings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\'", "'")
        }
    }
}

internal fun projectRoot(): File {
    var dir: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    throw AssertionError("project root not found")
}
