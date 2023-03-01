package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.obj.AppShortcut
import com.github.libretube.ui.activities.MainActivity

object ShortcutHelper {
    private val shortcuts = listOf(
        AppShortcut("home", R.string.startpage, R.drawable.ic_home),
        AppShortcut("trends", R.string.trends, R.drawable.ic_trending),
        AppShortcut("subscriptions", R.string.subscriptions, R.drawable.ic_subscriptions),
        AppShortcut("library", R.string.library, R.drawable.ic_library)
    )

    private fun createShortcut(context: Context, appShortcut: AppShortcut): ShortcutInfoCompat {
        val label = context.getString(appShortcut.label)
        return ShortcutInfoCompat.Builder(context, appShortcut.action)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, appShortcut.drawable))
            .setIntent(
                Intent(Intent.ACTION_VIEW, null, context, MainActivity::class.java)
                    .putExtra(IntentData.fragmentToOpen, appShortcut.action)
            )
            .build()
    }

    fun createShortcuts(context: Context) {
        if (ShortcutManagerCompat.getDynamicShortcuts(context).isEmpty()) {
            val dynamicShortcuts = shortcuts.map { createShortcut(context, it) }
            ShortcutManagerCompat.setDynamicShortcuts(context, dynamicShortcuts)
        }
    }
}
