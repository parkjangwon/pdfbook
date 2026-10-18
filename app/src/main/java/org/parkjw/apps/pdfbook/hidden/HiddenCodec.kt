package org.parkjw.apps.pdfbook.hidden

internal fun encodeHiddenItems(items: List<HiddenItem>): String = buildString {
    for (item in items) {
        append(enc(item.kind.name))
        append('\t')
        append(enc(item.id))
        append('\t')
        append(enc(item.name))
        append('\t')
        append(enc(item.folderUri.orEmpty()))
        append('\n')
    }
}

internal fun decodeHiddenItems(raw: String?): List<HiddenItem> {
    if (raw.isNullOrEmpty()) return emptyList()
    return raw.split('\n').mapNotNull { line ->
        if (line.isEmpty()) return@mapNotNull null
        val parts = line.split('\t')
        if (parts.size != 4) return@mapNotNull null
        val kindName = dec(parts[0]) ?: return@mapNotNull null
        val kind = runCatching { HiddenKind.valueOf(kindName) }.getOrNull() ?: return@mapNotNull null
        val id = dec(parts[1]) ?: return@mapNotNull null
        val name = dec(parts[2]) ?: return@mapNotNull null
        val folder = dec(parts[3]) ?: return@mapNotNull null
        HiddenItem(
            kind = kind,
            id = id,
            name = name,
            folderUri = folder.ifEmpty { null },
        )
    }
}

private fun enc(value: String): String =
    value.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
        "%02X".format(byte.toInt() and 0xFF)
    }

private fun dec(hex: String): String? {
    if (hex.length % 2 != 0) return null
    val bytes = ByteArray(hex.length / 2)
    for (index in bytes.indices) {
        val parsed = hex.substring(index * 2, index * 2 + 2).toIntOrNull(16) ?: return null
        bytes[index] = parsed.toByte()
    }
    return runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
}
