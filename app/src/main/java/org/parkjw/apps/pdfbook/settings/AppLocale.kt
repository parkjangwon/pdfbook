package org.parkjw.apps.pdfbook.settings

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun AppLanguage.toLocaleList(): LocaleList = when (this) {
    AppLanguage.System -> LocaleList.getEmptyLocaleList()
    AppLanguage.Korean -> LocaleList.forLanguageTags("ko")
    AppLanguage.English -> LocaleList.forLanguageTags("en")
}

fun LocaleList.toAppLanguage(): AppLanguage = toLanguageTags().toAppLanguage()

fun String.toAppLanguage(): AppLanguage {
    val language = substringBefore(',').trim().substringBefore('-').substringBefore('_').lowercase()
    return when (language) {
        "ko" -> AppLanguage.Korean
        "en" -> AppLanguage.English
        else -> AppLanguage.System
    }
}

internal sealed interface AppLocaleStep {
    data object Wait : AppLocaleStep
    data class MirrorStore(val language: AppLanguage) : AppLocaleStep
    data class ApplyManager(val language: AppLanguage) : AppLocaleStep
    data class Hold(val language: AppLanguage) : AppLocaleStep
}

internal fun appLocaleStep(
    ready: Boolean,
    stored: AppLanguage,
    applied: AppLanguage,
    settled: AppLanguage?,
): AppLocaleStep {
    if (!ready) return AppLocaleStep.Wait
    if (settled == null) {
        return if (stored == applied) AppLocaleStep.Hold(applied) else AppLocaleStep.MirrorStore(applied)
    }
    if (stored == settled || stored == applied) return AppLocaleStep.Hold(stored)
    return AppLocaleStep.ApplyManager(stored)
}

@Composable
fun SyncAppLanguage(
    ready: Boolean,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    val localeManager = remember(context) { context.getSystemService(LocaleManager::class.java) }
    var settled by remember { mutableStateOf<AppLanguage?>(null) }
    LaunchedEffect(ready, language, settled) {
        when (val step = appLocaleStep(ready, language, localeManager.applicationLocales.toAppLanguage(), settled)) {
            AppLocaleStep.Wait -> Unit
            is AppLocaleStep.MirrorStore -> onLanguage(step.language)
            is AppLocaleStep.ApplyManager -> {
                localeManager.applicationLocales = step.language.toLocaleList()
                settled = step.language
            }
            is AppLocaleStep.Hold -> settled = step.language
        }
    }
}
