package org.parkjw.apps.pdfbook.update

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ApkStream(val contentLength: Long, val input: InputStream)

fun interface ApkTransport {
    @Throws(IOException::class)
    fun open(url: String): ApkStream
}

fun interface ApkDownloader {
    @Throws(IOException::class)
    fun download(asset: ReleaseAsset, onProgress: (Int) -> Unit): File
}

fun progressPercent(read: Long, total: Long): Int =
    if (total <= 0L) 0 else ((read * 100) / total).coerceIn(0L, 100L).toInt()

class FileApkDownloader(
    private val directory: File,
    private val transport: ApkTransport,
) : ApkDownloader {
    override fun download(asset: ReleaseAsset, onProgress: (Int) -> Unit): File {
        if (!asset.downloadUrl.startsWith("https://")) throw IOException("refusing non-https download")
        if (asset.name.contains('/') || asset.name.contains('\\') || asset.name.startsWith(".")) {
            throw IOException("unsafe asset name")
        }
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("cannot create $directory")
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, asset.name)
        val partial = File(directory, asset.name + ".part")
        val stream = transport.open(asset.downloadUrl)
        val expected = asset.sizeBytes.takeIf { it > 0L } ?: stream.contentLength
        var read = 0L
        var lastPercent = -1
        try {
            stream.input.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (expected > 0L && read > expected) throw IOException("download larger than expected")
                        val percent = progressPercent(read, expected)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
            if (read == 0L || (expected > 0L && read != expected)) throw IOException("incomplete download")
            if (!partial.renameTo(target)) throw IOException("cannot move download into place")
        } catch (e: IOException) {
            partial.delete()
            target.delete()
            throw e
        }
        return target
    }
}

class HttpsApkTransport(private val userAgent: String) : ApkTransport {
    override fun open(url: String): ApkStream {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/octet-stream")
        }
        val code = connection.responseCode
        if (code !in 200..299 || connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("download failed with HTTP $code")
        }
        return ApkStream(connection.contentLengthLong, connection.inputStream)
    }
}
