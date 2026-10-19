package org.parkjw.apps.pdfbook.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureGuardTest {
    @Test
    fun uiCodeDoesNotUseRendererInternals() {
        val rendererFiles = setOf("PageRenderer.kt", "PageCache.kt")
        val offenders = SourceFiles.kotlin()
            .filter { it.name !in rendererFiles }
            .flatMap { file ->
                findRendererInternals(file.readText()).map { "${SourceFiles.rel(file)}: $it" }
            }
        assertEquals("renderer internals outside PageRenderer.kt/PageCache.kt:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun designSystemDoesNotImportFeaturePackages() {
        val offenders = SourceFiles.kotlin()
            .filter { SourceFiles.packagePath(it).let { pkg -> pkg == "ui" || pkg.startsWith("ui/") } }
            .flatMap { file ->
                findFeatureImportsFromDesignSystem(file.readText()).map { "${SourceFiles.rel(file)}: import $it" }
            }
        assertEquals("design system imports feature code:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun dataLayerDoesNotImportCompose() {
        val dataFile = Regex("""(Repository|Codec|Store|DataStore)\.kt$""")
        val extra = setOf("PageRenderer.kt", "PageCache.kt", "LibraryScan.kt")
        val files = SourceFiles.kotlin().filter { dataFile.containsMatchIn(it.name) || it.name in extra }
        assertTrue("no data layer files found", files.size >= 8)
        val offenders = files.flatMap { file ->
            findComposeImports(file.readText()).map { "${SourceFiles.rel(file)}: import $it" }
        }
        assertEquals("data layer imports Compose:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun updatePackageDoesNotImportOtherFeatures() {
        val offenders = SourceFiles.kotlin()
            .filter { SourceFiles.packagePath(it) == "update" }
            .flatMap { file ->
                findFeatureImportsFromUpdate(file.readText()).map { "${SourceFiles.rel(file)}: import $it" }
            }
        assertEquals("update imports another feature:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun globalScopeIsForbidden() {
        val offenders = SourceFiles.kotlin().flatMap { file ->
            val source = file.readText()
            findGlobalScope(source).map { "${SourceFiles.rel(file)}:${SourceFiles.lineOf(source, it)}: GlobalScope" }
        }
        assertEquals("GlobalScope used:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun guardsDetectSampleViolations() {
        assertEquals(listOf("android.graphics.pdf", "PdfRenderer"), findRendererInternals("import android.graphics.pdf.PdfRenderer"))
        assertEquals(listOf("PdfPageSource"), findRendererInternals("val s = PdfPageSource(pfd, pdf)"))
        assertEquals(emptyList<String>(), findRendererInternals("val r = PageRenderer.open(pfd)"))
        assertEquals(
            listOf("org.parkjw.apps.pdfbook.folders.Folder", "org.parkjw.apps.pdfbook.BookInfo"),
            findFeatureImportsFromDesignSystem(
                "import org.parkjw.apps.pdfbook.folders.Folder\nimport org.parkjw.apps.pdfbook.BookInfo\nimport org.parkjw.apps.pdfbook.R",
            ),
        )
        assertEquals(listOf("androidx.compose.runtime.State"), findComposeImports("import androidx.compose.runtime.State"))
        assertEquals(
            listOf("org.parkjw.apps.pdfbook.lock.LockConfig"),
            findFeatureImportsFromUpdate("import org.parkjw.apps.pdfbook.lock.LockConfig\nimport org.parkjw.apps.pdfbook.ui.theme.ButtonCorner"),
        )
        assertEquals(1, findGlobalScope("GlobalScope.launch { }").size)
        assertEquals(0, findGlobalScope("rememberCoroutineScope()").size)
    }

    companion object {
        private const val APP = "org.parkjw.apps.pdfbook"
        private val rendererInternal =
            Regex("""\bandroid\.graphics\.pdf\b|\b(?:PdfRenderer|PdfPageSource|PageSource|PageCache)\b""")
        private val designSystemAllowed = setOf("$APP.R", "$APP.settings.ThemeMode")

        fun findRendererInternals(source: String): List<String> =
            rendererInternal.findAll(source).map { it.value }.distinct().toList()

        fun findFeatureImportsFromDesignSystem(source: String): List<String> =
            SourceFiles.imports(source).filter { name ->
                name.startsWith("$APP.") && !name.startsWith("$APP.ui.") && name !in designSystemAllowed
            }

        fun findComposeImports(source: String): List<String> =
            SourceFiles.imports(source).filter { it.startsWith("androidx.compose.") }

        fun findFeatureImportsFromUpdate(source: String): List<String> =
            SourceFiles.imports(source).filter { name ->
                listOf("folders", "hidden", "lock").any { name.startsWith("$APP.$it.") }
            }

        fun findGlobalScope(source: String): List<Int> =
            Regex("""\bGlobalScope\b""").findAll(source).map { it.range.first }.toList()
    }
}
