package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.min

class GeneratedImagePdf private constructor(
    val file: File,
    val pageCount: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    private val streams: List<LongRange>,
) {
    fun stream(page: Int): LongRange = streams[page - 1]

    fun open(): GeneratedPdfSource = GeneratedPdfSource(this)

    companion object {
        fun write(
            file: File,
            pageCount: Int,
            imageWidth: Int = 240,
            imageHeight: Int = 340,
        ): GeneratedImagePdf {
            val out = ByteArrayOutputStream()
            val offsets = LongArray(2 + pageCount * 3 + 1)
            val streams = ArrayList<LongRange>(pageCount)
            fun text(value: String) = out.write(value.toByteArray(Charsets.ISO_8859_1))
            fun begin(id: Int) {
                offsets[id] = out.size().toLong()
                text("$id 0 obj\n")
            }
            text("%PDF-1.4\n")
            begin(1)
            text("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            begin(2)
            val kids = (0 until pageCount).joinToString(" ") { "${3 + it * 3} 0 R" }
            text("<< /Type /Pages /Kids [$kids] /Count $pageCount >>\nendobj\n")
            val pageWidth = imageWidth * 2
            val pageHeight = imageHeight * 2
            for (index in 0 until pageCount) {
                val pageId = 3 + index * 3
                val contentId = pageId + 1
                val imageId = pageId + 2
                begin(pageId)
                text(
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] " +
                        "/Resources << /XObject << /Im0 $imageId 0 R >> >> /Contents $contentId 0 R >>\nendobj\n",
                )
                val content = "q $pageWidth 0 0 $pageHeight 0 0 cm /Im0 Do Q".toByteArray(Charsets.ISO_8859_1)
                begin(contentId)
                text("<< /Length ${content.size} >>\nstream\n")
                out.write(content)
                text("\nendstream\nendobj\n")
                val image = deflate(pixels(index + 1, imageWidth, imageHeight))
                begin(imageId)
                text(
                    "<< /Type /XObject /Subtype /Image /Width $imageWidth /Height $imageHeight " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length ${image.size} >>\nstream\n",
                )
                val start = out.size().toLong()
                out.write(image)
                streams += start until start + image.size
                text("\nendstream\nendobj\n")
            }
            val xref = out.size()
            val count = 3 + pageCount * 3
            text("xref\n0 $count\n0000000000 65535 f \n")
            for (id in 1 until count) {
                text("%010d 00000 n \n".format(offsets[id]))
            }
            text("trailer\n<< /Size $count /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
            file.writeBytes(out.toByteArray())
            return GeneratedImagePdf(file, pageCount, imageWidth, imageHeight, streams)
        }

        private fun pixels(page: Int, width: Int, height: Int): ByteArray {
            val rgb = ByteArray(width * height * 3)
            var i = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    rgb[i++] = (128 + (x * 127 / width + page * 7) % 128).toByte()
                    rgb[i++] = (128 + (y * 127 / height + page * 13) % 128).toByte()
                    rgb[i++] = (128 + ((x xor y) + page) % 128).toByte()
                }
            }
            return rgb
        }

        private fun deflate(data: ByteArray): ByteArray {
            val deflater = Deflater(Deflater.BEST_SPEED)
            deflater.setInput(data)
            deflater.finish()
            val out = ByteArrayOutputStream(data.size / 4)
            val buffer = ByteArray(64 * 1024)
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer))
            }
            deflater.end()
            return out.toByteArray()
        }
    }
}

class GeneratedPdfSource(private val pdf: GeneratedImagePdf) : PageSource {
    private val file = RandomAccessFile(pdf.file, "r")

    var draws = 0
        private set

    override val pageCount: Int get() = pdf.pageCount

    override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
        draws++
        val range = pdf.stream(page)
        val compressed = ByteArray((range.last - range.first + 1).toInt())
        file.seek(range.first)
        file.readFully(compressed)
        val rgb = ByteArray(pdf.imageWidth * pdf.imageHeight * 3)
        Inflater().apply {
            setInput(compressed)
            inflate(rgb)
            end()
        }
        val argb = IntArray(pdf.imageWidth * pdf.imageHeight) { i ->
            val o = i * 3
            (0xFF shl 24) or
                ((rgb[o].toInt() and 0xFF) shl 16) or
                ((rgb[o + 1].toInt() and 0xFF) shl 8) or
                (rgb[o + 2].toInt() and 0xFF)
        }
        val image = Bitmap.createBitmap(argb, pdf.imageWidth, pdf.imageHeight, Bitmap.Config.ARGB_8888)
        val scale = min(maxWidth.toFloat() / pdf.imageWidth, maxHeight.toFloat() / pdf.imageHeight)
        val width = (pdf.imageWidth * scale).toInt().coerceAtLeast(1)
        val height = (pdf.imageHeight * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(image, width, height, true)
        if (scaled !== image) image.recycle()
        return scaled
    }

    override fun close() {
        file.close()
    }
}
