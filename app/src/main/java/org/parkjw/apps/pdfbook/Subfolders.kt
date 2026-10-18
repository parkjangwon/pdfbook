package org.parkjw.apps.pdfbook

import org.parkjw.apps.pdfbook.hidden.HiddenItem
import org.parkjw.apps.pdfbook.hidden.HiddenKind
import org.parkjw.apps.pdfbook.hidden.isDotName

const val MAX_FOLDER_DEPTH = 10

data class FolderNode(
    val documentId: String,
    val uri: String,
    val name: String,
    val books: List<BookInfo>,
    val folders: List<FolderNode>,
)

data class DocEntry(
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val isPdf: Boolean,
    val size: Long,
)

data class SubfolderItem(
    val node: FolderNode,
    val path: List<String>,
    val bookCount: Int,
    val coverBooks: List<BookInfo>,
    val isHidden: Boolean,
)

data class VisibilityRules(
    val hiddenBookIds: Set<String> = emptySet(),
    val hiddenFolderIds: Set<String> = emptySet(),
    val showDotFiles: Boolean = false,
)

fun buildFolderTree(
    rootId: String,
    rootName: String,
    uriFor: (String) -> String,
    children: (String) -> List<DocEntry>?,
    maxDepth: Int = MAX_FOLDER_DEPTH,
): FolderNode? {
    fun build(id: String, name: String, depth: Int, seen: Set<String>): FolderNode? {
        val entries = children(id) ?: return null
        val books = entries.filter { it.isPdf && !it.isDirectory }
            .map { BookInfo(uriFor(it.documentId), it.name, it.size) }
        val folders = if (depth >= maxDepth) {
            emptyList()
        } else {
            entries.filter { it.isDirectory && it.documentId !in seen }
                .mapNotNull { build(it.documentId, it.name, depth + 1, seen + it.documentId) }
                .filter { it.books.isNotEmpty() || it.folders.isNotEmpty() }
        }
        return FolderNode(id, uriFor(id), name, books, folders)
    }
    return build(rootId, rootName, 0, setOf(rootId))
}

fun FolderNode.nodeAt(path: List<String>): FolderNode? {
    var node = this
    for (id in path) {
        node = node.folders.firstOrNull { it.documentId == id } ?: return null
    }
    return node
}

fun FolderNode.pathOfBook(bookUri: String): List<String>? {
    if (books.any { it.uri == bookUri }) return emptyList()
    for (child in folders) {
        child.pathOfBook(bookUri)?.let { return listOf(child.documentId) + it }
    }
    return null
}

fun FolderNode.findBook(bookUri: String): BookInfo? =
    books.firstOrNull { it.uri == bookUri } ?: folders.firstNotNullOfOrNull { it.findBook(bookUri) }

fun FolderNode.pathNames(path: List<String>): List<String> {
    val names = mutableListOf(name)
    var node = this
    for (id in path) {
        node = node.folders.firstOrNull { it.documentId == id } ?: break
        names += node.name
    }
    return names
}

fun trimPath(root: FolderNode?, path: List<String>): List<String> {
    if (root == null) return path
    var node: FolderNode = root
    val kept = mutableListOf<String>()
    for (id in path) {
        node = node.folders.firstOrNull { it.documentId == id } ?: break
        kept += id
    }
    return kept
}

fun parentPath(path: List<String>): List<String>? = if (path.isEmpty()) null else path.dropLast(1)

private fun isBookShown(book: BookInfo, rules: VisibilityRules): Boolean =
    book.uri !in rules.hiddenBookIds && (rules.showDotFiles || !isDotName(book.name))

private fun isFolderReachable(node: FolderNode, rules: VisibilityRules): Boolean =
    rules.showDotFiles || !isDotName(node.name)

fun visibleBooksUnder(node: FolderNode, rules: VisibilityRules): List<BookInfo> =
    node.books.filter { isBookShown(it, rules) } +
        node.folders
            .filter { isFolderReachable(it, rules) && it.uri !in rules.hiddenFolderIds }
            .flatMap { visibleBooksUnder(it, rules) }

private fun reachableBooksUnder(node: FolderNode, rules: VisibilityRules): List<BookInfo> =
    node.books.filter { rules.showDotFiles || !isDotName(it.name) } +
        node.folders.filter { isFolderReachable(it, rules) }.flatMap { reachableBooksUnder(it, rules) }

fun subfolderItems(
    node: FolderNode,
    path: List<String>,
    rules: VisibilityRules,
    peek: Boolean,
): List<SubfolderItem> =
    node.folders
        .filter { isFolderReachable(it, rules) }
        .sortedWith(compareBy(NaturalOrder) { it.name })
        .mapNotNull { child ->
            val userHidden = child.uri in rules.hiddenFolderIds
            val visible = if (userHidden) emptyList() else visibleBooksUnder(child, rules)
            val isHidden = visible.isEmpty()
            if (isHidden && (!peek || reachableBooksUnder(child, rules).isEmpty())) return@mapNotNull null
            val counted = if (isHidden) reachableBooksUnder(child, rules) else visible
            SubfolderItem(
                node = child,
                path = path + child.documentId,
                bookCount = counted.size,
                coverBooks = counted.sortedWith(compareBy(NaturalOrder) { titleOf(it.name) }).take(3),
                isHidden = isHidden,
            )
        }

fun flatListing(root: FolderNode, rules: VisibilityRules, folderName: String = root.name): Listing =
    Listing(folderName, visibleBooksUnder(root, rules))

data class FolderPath(val rootUri: String? = null, val path: List<String> = emptyList()) {
    fun pathFor(selectedRootUri: String?): List<String> =
        if (selectedRootUri != null && selectedRootUri == rootUri) path else emptyList()
}

fun allBookUris(node: FolderNode): Set<String> =
    buildSet {
        node.books.forEach { add(it.uri) }
        node.folders.forEach { addAll(allBookUris(it)) }
    }

fun allFolderUris(node: FolderNode): Set<String> =
    buildSet {
        add(node.uri)
        node.folders.forEach { addAll(allFolderUris(it)) }
    }

fun hiddenItemsUnder(node: FolderNode, hidden: List<HiddenItem>): List<HiddenItem> {
    val books = allBookUris(node)
    val folders = allFolderUris(node)
    return hidden.filter { item ->
        when (item.kind) {
            HiddenKind.Book -> item.id in books
            HiddenKind.Folder -> item.id in folders
        }
    }
}
