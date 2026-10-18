package org.parkjw.apps.pdfbook.update

import android.content.Context
import android.content.pm.PackageManager

fun Context.installedVersionName(): String =
    packageManager
        .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        .versionName.orEmpty()
