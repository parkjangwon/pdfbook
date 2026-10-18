package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

class IconGeometryTest {
    private val tolerance = 0.25f

    @Test
    fun iconDrawablesHaveSquareSizeAndViewport() {
        val dir = File(projectRoot(), "app/src/main/res/drawable")
        val icons = dir.listFiles { f -> f.isFile && f.name.startsWith("ic_") && f.extension == "xml" }.orEmpty()
            .filterNot { it.name.startsWith("ic_launcher") }
        assertTrue(icons.isNotEmpty())
        val offenders = icons.mapNotNull { file ->
            val root = parse(file).documentElement
            if (root.tagName != "vector") return@mapNotNull null
            val width = root.getAttribute("android:width")
            val height = root.getAttribute("android:height")
            val viewportWidth = root.getAttribute("android:viewportWidth")
            val viewportHeight = root.getAttribute("android:viewportHeight")
            if (width == height && viewportWidth == viewportHeight) null else "${file.name}: $width x $height, viewport $viewportWidth x $viewportHeight"
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun settingsGearOutlineAndHubShareCenter() {
        val paths = pathData("ic_settings.xml")
        assertEquals(2, paths.size)
        val hub = paths[0]
        val gear = vertices(paths[1])
        val hubCenter = arcCenter(hub)
        assertNear("hub x", 12f, hubCenter.first)
        assertNear("hub y", 12f, hubCenter.second)
        val minX = gear.minOf { it.first }
        val maxX = gear.maxOf { it.first }
        val minY = gear.minOf { it.second }
        val maxY = gear.maxOf { it.second }
        assertNear("gear center x", 12f, (minX + maxX) / 2f)
        assertNear("gear center y", 12f, (minY + maxY) / 2f)
        assertTrue("gear inside viewport", minX >= 2f && minY >= 2f && maxX <= 22f && maxY <= 22f)
    }

    private fun assertNear(label: String, expected: Float, actual: Float) {
        assertTrue("$label: expected $expected, got $actual", abs(expected - actual) <= tolerance)
    }

    private fun arcCenter(d: String): Pair<Float, Float> {
        val nodes = PathParser().parsePathString(d).toNodes()
        val move = nodes.first() as? PathNode.MoveTo ?: error("hub must start with absolute move")
        val arc = nodes.drop(1).first() as? PathNode.RelativeArcTo ?: error("hub must be a relative arc")
        assertNear("hub radius", arc.horizontalEllipseRadius, arc.verticalEllipseRadius)
        return (move.x + arc.arcStartDx / 2f) to (move.y + arc.arcStartDy / 2f)
    }

    private fun vertices(d: String): List<Pair<Float, Float>> {
        var x = 0f
        var y = 0f
        var startX = 0f
        var startY = 0f
        val points = mutableListOf<Pair<Float, Float>>()
        for (node in PathParser().parsePathString(d).toNodes()) {
            when (node) {
                is PathNode.MoveTo -> { x = node.x; y = node.y; startX = x; startY = y }
                is PathNode.RelativeMoveTo -> { x += node.dx; y += node.dy; startX = x; startY = y }
                is PathNode.LineTo -> { x = node.x; y = node.y }
                is PathNode.RelativeLineTo -> { x += node.dx; y += node.dy }
                is PathNode.HorizontalTo -> x = node.x
                is PathNode.RelativeHorizontalTo -> x += node.dx
                is PathNode.VerticalTo -> y = node.y
                is PathNode.RelativeVerticalTo -> y += node.dy
                PathNode.Close -> { x = startX; y = startY; continue }
                else -> fail("unsupported gear path node $node")
            }
            points.add(x to y)
        }
        return points
    }

    private fun pathData(name: String): List<String> {
        val doc = parse(File(projectRoot(), "app/src/main/res/drawable/$name"))
        val nodes = doc.getElementsByTagName("path")
        return (0 until nodes.length).map { nodes.item(it).attributes.getNamedItem("android:pathData").nodeValue }
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun projectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(12) {
            if (File(dir, "settings.gradle.kts").isFile || File(dir, "settings.gradle").isFile) {
                return dir
            }
            dir = dir.parentFile ?: run {
                fail("settings.gradle.kts not found")
                error("unreachable")
            }
        }
        fail("settings.gradle.kts not found")
        error("unreachable")
    }
}
