package org.parkjw.apps.pdfbook.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest

data class SignerSnapshot(
    val packageName: String,
    val signers: Set<String>,
)

fun interface ApkVerifier {
    fun matchesInstalledApp(apk: File): Boolean
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun signaturesMatch(installed: SignerSnapshot?, archive: SignerSnapshot?): Boolean {
    if (installed == null || archive == null) return false
    if (installed.packageName != archive.packageName) return false
    return installed.signers.isNotEmpty() && installed.signers == archive.signers
}

fun PackageInfo.signerSnapshot(): SignerSnapshot? {
    val info = signingInfo ?: return null
    val signers = info.apkContentsSigners.orEmpty().map { sha256Hex(it.toByteArray()) }.toSet()
    return SignerSnapshot(packageName, signers)
}

class PackageManagerApkVerifier(private val context: Context) : ApkVerifier {
    override fun matchesInstalledApp(apk: File): Boolean = runCatching {
        val pm = context.packageManager
        val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
        val installed = pm.getPackageInfo(context.packageName, flags).signerSnapshot()
        val archive = pm.getPackageArchiveInfo(apk.path, flags)?.signerSnapshot()
        signaturesMatch(installed, archive)
    }.getOrDefault(false)
}
