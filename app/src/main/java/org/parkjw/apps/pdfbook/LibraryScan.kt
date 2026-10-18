package org.parkjw.apps.pdfbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class Listing(
    val folderName: String,
    val books: List<BookInfo>,
    val root: FolderNode? = null,
)

private const val PDF_MIME = "application/pdf"
private const val COVER_WIDTH = 300
private const val COVER_HEIGHT = 450

suspend fun listPdfs(context: Context, treeUri: Uri): Listing? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val folderName = resolver.query(
            rootUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return@withContext null
        val root = buildFolderTree(
            rootId = rootId,
            rootName = folderName,
            uriFor = { DocumentsContract.buildDocumentUriUsingTree(treeUri, it).toString() },
            children = { id -> queryChildren(context, treeUri, id) },
        ) ?: return@withContext null
        Listing(folderName, root.books, root)
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun queryChildren(context: Context, treeUri: Uri, documentId: String): List<DocEntry>? {
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    return context.contentResolver.query(
        children,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        ),
        null,
        null,
        null,
    )?.use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val mime = cursor.getString(2)
                add(
                    DocEntry(
                        documentId = cursor.getString(0),
                        name = cursor.getString(1) ?: "",
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        isPdf = mime == PDF_MIME,
                        size = cursor.getLong(3),
                    ),
                )
            }
        }
    }
}

suspend fun loadCover(context: Context, book: BookInfo): Bitmap? = withContext(Dispatchers.IO) {
    val file = File(File(context.cacheDir, "covers"), coverFileName(bookKey(book.uri, book.size)) + ".webp")
    BitmapFactory.decodeFile(file.path)?.let { return@withContext it }
    try {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(book.uri), "r")
            ?: return@withContext null
        val bitmap = try {
            PageRenderer.open(pfd)
        } catch (e: Exception) {
            pfd.close()
            throw e
        }.use { it.render(1, COVER_WIDTH, COVER_HEIGHT) }
            ?: return@withContext null
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, it) }
        bitmap
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: Exception) {
        null
    }
}
