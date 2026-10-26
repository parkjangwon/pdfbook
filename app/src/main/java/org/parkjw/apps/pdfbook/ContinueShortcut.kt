package org.parkjw.apps.pdfbook

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

const val ACTION_CONTINUE_READING = "org.parkjw.apps.pdfbook.action.CONTINUE_READING"
private const val CONTINUE_SHORTCUT_ID = "continue_reading"

fun Intent?.isContinueReading(): Boolean = this?.action == ACTION_CONTINUE_READING

// The intent carries no book: the target is resolved at launch from the latest reading state,
// so the shortcut only has to exist while there is something to continue.
fun syncContinueShortcut(context: Context, hasLastBook: Boolean) {
    if (!hasLastBook) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(CONTINUE_SHORTCUT_ID))
        return
    }
    val shortcut = ShortcutInfoCompat.Builder(context, CONTINUE_SHORTCUT_ID)
        .setShortLabel(context.getString(R.string.shortcut_continue_reading))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(Intent(context, MainActivity::class.java).setAction(ACTION_CONTINUE_READING))
        .build()
    ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
}
