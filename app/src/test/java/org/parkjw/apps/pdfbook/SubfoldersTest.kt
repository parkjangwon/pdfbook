package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.hidden.HiddenItem
import org.parkjw.apps.pdfbook.hidden.HiddenKind

class SubfoldersTest {
    private class FakeTree {
        val dirs = linkedMapOf<String, MutableList<DocEntry>>()

        fun dir(id: String, vararg children: DocEntry) {
            dirs[id] = children.toMutableList()
        }

        fun build(maxDepth: Int = MAX_FOLDER_DEPTH) = buildFolderTree(
            rootId = "root",
            rootName = "Root",
            uriFor = { "doc:$it" },
            children = { dirs[it] },
            maxDepth = maxDepth,
        )!!
    }

    private fun folder(id: String, name: String = id) = DocEntry(id, name, isDirectory = true, isPdf = false, size = 0)
    private fun pdf(id: String, name: String = "$id.pdf") = DocEntry(id, name, isDirectory = false, isPdf = true, size = 1)
    private fun txt(id: String) = DocEntry(id, "$id.txt", isDirectory = false, isPdf = false, size = 1)

    private fun seriesTree(): FolderNode {
        val tree = FakeTree()
        tree.dir("root", folder("s10", "시리즈 10"), folder("s2", "시리즈 2"), folder("empty", "빈 폴더"), folder("s1", "시리즈 1"))
        tree.dir("s1", pdf("s1b2", "2권.pdf"), pdf("s1b1", "1권.pdf"), pdf("s1b10", "10권.pdf"))
        tree.dir("s2", folder("p1", "1부"), folder("notes", "메모"))
        tree.dir("p1", folder("deep", "3부"))
        tree.dir("deep", pdf("d1", "끝.pdf"))
        tree.dir("notes", txt("n1"))
        tree.dir("s10", pdf("s10b1"))
        tree.dir("empty", folder("inner"))
        tree.dir("inner")
        return tree.build()
    }

    @Test
    fun parentWithOnlySubfoldersShowsFolderEntriesInNaturalOrder() {
        val root = seriesTree()
        assertTrue(root.books.isEmpty())
        val items = subfolderItems(root, emptyList(), VisibilityRules(), peek = false)
        assertEquals(listOf("시리즈 1", "시리즈 2", "시리즈 10"), items.map { it.node.name })
        assertEquals(listOf(3, 1, 1), items.map { it.bookCount })
        assertEquals(listOf("1권.pdf", "2권.pdf", "10권.pdf"), items[0].coverBooks.map { it.name })
        assertEquals(FolderContentPhase.Ready, folderContentPhase("u", true, Listing("Root", emptyList(), root), items.isNotEmpty()))
        assertEquals(FolderContentPhase.Empty, folderContentPhase("u", true, Listing("Root", emptyList(), root), false))
    }

    @Test
    fun pdfLessFoldersAreDroppedAtEveryLevel() {
        val root = seriesTree()
        assertNull(root.folders.firstOrNull { it.name == "빈 폴더" })
        val s2 = root.folders.first { it.name == "시리즈 2" }
        assertEquals(listOf("1부"), s2.folders.map { it.name })
    }

    @Test
    fun navigatesThreeLevelsAndBackOneAtATime() {
        val root = seriesTree()
        val s2 = subfolderItems(root, emptyList(), VisibilityRules(), false).first { it.node.name == "시리즈 2" }
        val p1 = subfolderItems(s2.node, s2.path, VisibilityRules(), false).single()
        val deep = subfolderItems(p1.node, p1.path, VisibilityRules(), false).single()
        assertEquals(listOf("s2", "p1", "deep"), deep.path)
        assertEquals("끝.pdf", root.nodeAt(deep.path)!!.books.single().name)
        assertEquals(listOf("Root", "시리즈 2", "1부", "3부"), root.pathNames(deep.path))
        assertEquals(listOf("s2", "p1"), parentPath(deep.path))
        assertEquals(emptyList<String>(), parentPath(listOf("s2")))
        assertNull(parentPath(emptyList()))
    }

    @Test
    fun backOrderIsReaderFolderParentListThenExit() {
        assertEquals(FolderBackAction.CloseReader, folderBackAction(false, true, false, false, inSubfolder = true))
        assertEquals(FolderBackAction.UpFolder, folderBackAction(false, false, true, false, inSubfolder = true))
        assertEquals(FolderBackAction.CloseLibrary, folderBackAction(false, false, true, false, inSubfolder = false))
        assertEquals(FolderBackAction.None, folderBackAction(false, false, false, false))
    }

    @Test
    fun bookPathIsFoundForReturningFromReader() {
        val root = seriesTree()
        assertEquals(listOf("s2", "p1", "deep"), root.pathOfBook("doc:d1"))
        assertEquals(listOf("s1"), root.pathOfBook("doc:s1b1"))
        assertNull(root.pathOfBook("doc:missing"))
        assertEquals("끝.pdf", root.findBook("doc:d1")?.name)
    }

    @Test
    fun hiddenFolderHidesEverythingUnderItUnlessPeeking() {
        val root = seriesTree()
        val rules = VisibilityRules(hiddenFolderIds = setOf("doc:s2"))
        val shown = subfolderItems(root, emptyList(), rules, peek = false)
        assertEquals(listOf("시리즈 1", "시리즈 10"), shown.map { it.node.name })
        assertFalse(visibleBooksUnder(root, rules).any { it.uri == "doc:d1" })
        val peeked = subfolderItems(root, emptyList(), rules, peek = true)
        assertEquals(listOf("시리즈 1", "시리즈 2", "시리즈 10"), peeked.map { it.node.name })
        assertTrue(peeked[1].isHidden)
        assertEquals(1, peeked[1].bookCount)
    }

    @Test
    fun folderWithAllBooksHiddenIsHiddenAndCountsSkipHiddenBooks() {
        val root = seriesTree()
        val rules = VisibilityRules(hiddenBookIds = setOf("doc:s10b1", "doc:s1b2"))
        val items = subfolderItems(root, emptyList(), rules, peek = false)
        assertEquals(listOf("시리즈 1", "시리즈 2"), items.map { it.node.name })
        assertEquals(2, items[0].bookCount)
        val peeked = subfolderItems(root, emptyList(), rules, peek = true)
        assertTrue(peeked.single { it.node.name == "시리즈 10" }.isHidden)
        assertEquals(
            listOf(HiddenItem(HiddenKind.Book, "doc:s10b1", "s10b1", null)),
            hiddenItemsUnder(
                root.folders.first { it.name == "시리즈 10" },
                listOf(
                    HiddenItem(HiddenKind.Book, "doc:s10b1", "s10b1", null),
                    HiddenItem(HiddenKind.Book, "doc:s1b2", "2권", null),
                ),
            ),
        )
    }

    @Test
    fun unhidingDimmedFolderCoversNestedHiddenFoldersAndBooks() {
        val root = seriesTree()
        val series2 = root.folders.first { it.name == "시리즈 2" }
        val nested = HiddenItem(HiddenKind.Folder, series2.folders.first().uri, "1부", null)
        val book = HiddenItem(HiddenKind.Book, "doc:d1", "끝", null)
        val other = HiddenItem(HiddenKind.Book, "doc:s1b1", "1권", null)
        val self = HiddenItem(HiddenKind.Folder, series2.uri, "시리즈 2", null)
        assertEquals(listOf(nested, book, self), hiddenItemsUnder(series2, listOf(nested, book, other, self)))
        val rules = VisibilityRules(hiddenFolderIds = setOf(nested.id))
        assertTrue(subfolderItems(root, emptyList(), rules, peek = true).single { it.node.name == "시리즈 2" }.isHidden)
    }

    @Test
    fun dotFoldersFollowShowDotFiles() {
        val tree = FakeTree()
        tree.dir("root", folder("dot", ".private"), pdf("a"))
        tree.dir("dot", pdf("secret"))
        val root = tree.build()
        assertTrue(subfolderItems(root, emptyList(), VisibilityRules(showDotFiles = false), true).isEmpty())
        assertTrue(subfolderItems(root, emptyList(), VisibilityRules(showDotFiles = false), false).isEmpty())
        assertEquals(listOf("doc:a"), visibleBooksUnder(root, VisibilityRules()).map { it.uri })
        assertEquals(listOf(".private"), subfolderItems(root, emptyList(), VisibilityRules(showDotFiles = true), false).map { it.node.name })
        assertEquals(2, visibleBooksUnder(root, VisibilityRules(showDotFiles = true)).size)
    }

    @Test
    fun flatListingCountsNestedBooksForShelfCard() {
        val root = seriesTree()
        val flat = flatListing(root, VisibilityRules(hiddenBookIds = setOf("doc:s1b1")), "Shown name")
        assertEquals("Shown name", flat.folderName)
        assertEquals(4, flat.books.size)
        assertTrue(flat.books.none { it.uri == "doc:s1b1" })
    }

    @Test
    fun depthIsCappedAndCyclesStop() {
        val tree = FakeTree()
        tree.dir("root", folder("l1"))
        tree.dir("l1", folder("l2"), folder("root"))
        tree.dir("l2", pdf("deep"))
        assertTrue(tree.build(maxDepth = 1).folders.isEmpty())
        val full = tree.build()
        assertEquals(listOf("l1"), full.folders.map { it.documentId })
        assertEquals(listOf("l2"), full.folders.single().folders.map { it.documentId })
    }

    @Test
    fun stalePathIsTrimmedAndPathBelongsToItsRoot() {
        val root = seriesTree()
        assertEquals(listOf("s2"), trimPath(root, listOf("s2", "gone", "deep")))
        assertEquals(emptyList<String>(), trimPath(root, listOf("gone")))
        val saved = FolderPath("content://a", listOf("s2"))
        assertEquals(listOf("s2"), saved.pathFor("content://a"))
        assertEquals(emptyList<String>(), saved.pathFor("content://b"))
        assertEquals(emptyList<String>(), saved.pathFor(null))
    }

    @Test
    fun subfolderSortFollowsParentUntilChosen() {
        val sorts = mapOf(sortKey("content://a") to BookSort.Name)
        assertEquals(BookSort.Name, sortFor(sorts, "content://a", listOf("s2", "p1")))
        val chosen = sorts + (sortKey("content://a", listOf("s2")) to BookSort.Recent)
        assertEquals(BookSort.Recent, sortFor(chosen, "content://a", listOf("s2", "p1")))
    }
}
