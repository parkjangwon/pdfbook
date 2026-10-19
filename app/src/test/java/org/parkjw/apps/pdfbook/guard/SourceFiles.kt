package org.parkjw.apps.pdfbook.guard

import java.io.File

internal object SourceFiles {
    val root: File by lazy {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        dir ?: throw AssertionError("project root not found from ${System.getProperty("user.dir")}")
    }

    val javaRoot: File get() = File(root, "app/src/main/java/org/parkjw/apps/pdfbook")

    fun kotlin(): List<File> = javaRoot.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .sortedBy { it.path }
        .toList()

    fun rel(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    fun packagePath(file: File): String = file.parentFile.relativeTo(javaRoot).invariantSeparatorsPath

    fun imports(source: String): List<String> =
        Regex("""^import\s+([\w.]+)""", RegexOption.MULTILINE).findAll(source).map { it.groupValues[1] }.toList()

    fun lineOf(source: String, index: Int): Int = source.substring(0, index).count { it == '\n' } + 1

    fun blockAfter(source: String, openBraceIndex: Int): IntRange? {
        if (openBraceIndex !in source.indices || source[openBraceIndex] != '{') return null
        var depth = 0
        for (i in openBraceIndex until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return openBraceIndex..i
                }
            }
        }
        return null
    }

    fun blocks(source: String, opener: Regex): List<IntRange> =
        opener.findAll(source).mapNotNull { match ->
            val brace = source.indexOf('{', match.range.last)
            if (brace < 0) null else blockAfter(source, brace)
        }.toList()
}
