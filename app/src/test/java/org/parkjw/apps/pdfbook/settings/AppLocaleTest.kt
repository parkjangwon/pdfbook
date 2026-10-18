package org.parkjw.apps.pdfbook.settings

import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AppLocaleTest {
    @Test
    fun appLanguageMapsToLocaleList() {
        assertEquals("", AppLanguage.System.toLocaleList().toLanguageTags())
        assertEquals("ko", AppLanguage.Korean.toLocaleList().toLanguageTags())
        assertEquals("en", AppLanguage.English.toLocaleList().toLanguageTags())
    }

    @Test
    fun localeListAndTagsMapToAppLanguage() {
        assertEquals(AppLanguage.System, LocaleList.getEmptyLocaleList().toAppLanguage())
        assertEquals(AppLanguage.Korean, LocaleList.forLanguageTags("ko").toAppLanguage())
        assertEquals(AppLanguage.Korean, LocaleList.forLanguageTags("ko-KR").toAppLanguage())
        assertEquals(AppLanguage.English, LocaleList.forLanguageTags("en-US").toAppLanguage())
        assertEquals(AppLanguage.System, LocaleList.forLanguageTags("ja").toAppLanguage())
        assertEquals(AppLanguage.Korean, "ko_KR".toAppLanguage())
        assertEquals(AppLanguage.English, "en,ko".toAppLanguage())
    }

    @Test
    fun startupMirrorsManagerThenEditsApply() {
        assertEquals(AppLocaleStep.Wait, appLocaleStep(false, AppLanguage.Korean, AppLanguage.System, null))
        assertEquals(
            AppLocaleStep.MirrorStore(AppLanguage.English),
            appLocaleStep(true, AppLanguage.Korean, AppLanguage.English, null),
        )
        assertEquals(
            AppLocaleStep.Hold(AppLanguage.Korean),
            appLocaleStep(true, AppLanguage.Korean, AppLanguage.Korean, null),
        )
        assertEquals(
            AppLocaleStep.ApplyManager(AppLanguage.English),
            appLocaleStep(true, AppLanguage.English, AppLanguage.Korean, AppLanguage.Korean),
        )
        assertEquals(
            AppLocaleStep.Hold(AppLanguage.English),
            appLocaleStep(true, AppLanguage.English, AppLanguage.English, AppLanguage.Korean),
        )
    }
}
