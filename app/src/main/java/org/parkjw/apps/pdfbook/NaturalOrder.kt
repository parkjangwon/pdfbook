package org.parkjw.apps.pdfbook

object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        var zeroTie = 0
        while (i < a.length && j < b.length) {
            val aDigit = a[i].isAsciiDigit()
            val bDigit = b[j].isAsciiDigit()
            if (aDigit && bDigit) {
                val aEnd = runEnd(a, i, true)
                val bEnd = runEnd(b, j, true)
                val aStart = skipZeros(a, i, aEnd)
                val bStart = skipZeros(b, j, bEnd)
                val aLen = aEnd - aStart
                val bLen = bEnd - bStart
                if (aLen != bLen) return aLen.compareTo(bLen)
                for (k in 0 until aLen) {
                    val c = a[aStart + k].compareTo(b[bStart + k])
                    if (c != 0) return c
                }
                if (zeroTie == 0) zeroTie = (bStart - j).compareTo(aStart - i)
                i = aEnd
                j = bEnd
            } else if (!aDigit && !bDigit) {
                val aEnd = runEnd(a, i, false)
                val bEnd = runEnd(b, j, false)
                val c = String.CASE_INSENSITIVE_ORDER.compare(a.substring(i, aEnd), b.substring(j, bEnd))
                if (c != 0) return c
                i = aEnd
                j = bEnd
            } else {
                return if (aDigit) -1 else 1
            }
        }
        val rest = (a.length - i).compareTo(b.length - j)
        if (rest != 0) return rest
        if (zeroTie != 0) return zeroTie
        return a.compareTo(b)
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

    private fun runEnd(s: String, from: Int, digits: Boolean): Int {
        var k = from
        while (k < s.length && s[k].isAsciiDigit() == digits) k++
        return k
    }

    private fun skipZeros(s: String, from: Int, end: Int): Int {
        var k = from
        while (k < end - 1 && s[k] == '0') k++
        return k
    }
}
