package org.parkjw.apps.pdfbook.update

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkDownloaderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val bytes = ByteArray(300_000) { (it % 251).toByte() }

    private fun asset(name: String = "pdfbook-0.3.1.apk", size: Long = bytes.size.toLong(), url: String = "https://github.com/x/$name") =
        ReleaseAsset(name, url, size, "application/vnd.android.package-archive")

    private fun dir() = File(tmp.root, "updates")

    @Test
    fun writesFileAndReportsMonotonicProgress() {
        val requested = mutableListOf<String>()
        val progress = mutableListOf<Int>()
        val file = FileApkDownloader(dir()) { url ->
            requested += url
            ApkStream(bytes.size.toLong(), ByteArrayInputStream(bytes))
        }.download(asset()) { progress += it }
        assertEquals(listOf("https://github.com/x/pdfbook-0.3.1.apk"), requested)
        assertArrayEquals(bytes, file.readBytes())
        assertEquals("pdfbook-0.3.1.apk", file.name)
        assertEquals(100, progress.last())
        assertEquals(progress.sorted(), progress)
        assertEquals(progress.distinct(), progress)
        assertEquals(listOf(file), dir().listFiles()!!.toList())
    }

    @Test
    fun removesOlderDownloads() {
        dir().mkdirs()
        File(dir(), "pdfbook-0.3.0.apk").writeText("old")
        FileApkDownloader(dir()) { ApkStream(-1, ByteArrayInputStream(bytes)) }.download(asset()) {}
        assertEquals(listOf("pdfbook-0.3.1.apk"), dir().list()!!.toList())
    }

    @Test
    fun truncatedOrOversizedDownloadsFailAndLeaveNothing() {
        val short = bytes.copyOf(1000)
        assertThrows(IOException::class.java) {
            FileApkDownloader(dir()) { ApkStream(-1, ByteArrayInputStream(short)) }.download(asset()) {}
        }
        assertTrue(dir().list()!!.isEmpty())
        assertThrows(IOException::class.java) {
            FileApkDownloader(dir()) { ApkStream(-1, ByteArrayInputStream(bytes)) }.download(asset(size = 10)) {}
        }
        assertTrue(dir().list()!!.isEmpty())
        assertThrows(IOException::class.java) {
            FileApkDownloader(dir()) { ApkStream(0, ByteArrayInputStream(ByteArray(0))) }.download(asset(size = 0)) {}
        }
    }

    @Test
    fun streamErrorsPropagateAsIOException() {
        val failing = object : java.io.InputStream() {
            var n = 0
            override fun read(): Int = if (n++ < 10) 1 else throw IOException("reset")
        }
        assertThrows(IOException::class.java) {
            FileApkDownloader(dir()) { ApkStream(-1, failing) }.download(asset()) {}
        }
        assertFalse(File(dir(), "pdfbook-0.3.1.apk").exists())
    }

    @Test
    fun refusesUnsafeTargets() {
        val downloader = FileApkDownloader(dir()) { ApkStream(-1, ByteArrayInputStream(bytes)) }
        listOf(
            asset(url = "http://github.com/x.apk"),
            asset(name = "../evil.apk"),
            asset(name = ".hidden.apk"),
        ).forEach { bad -> assertThrows(IOException::class.java) { downloader.download(bad) {} } }
    }

    @Test
    fun progressPercentIsClamped() {
        assertEquals(0, progressPercent(10, 0))
        assertEquals(0, progressPercent(0, 100))
        assertEquals(33, progressPercent(1, 3))
        assertEquals(100, progressPercent(150, 100))
    }
}
