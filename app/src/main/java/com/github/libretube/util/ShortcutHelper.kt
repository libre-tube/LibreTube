package com.github.libretube.util

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
    ).reversed()

    private fun createShortcut(context: Context, action: String, label: String, icon: IconCompat) {
        val shortcut = ShortcutInfoCompat.Builder(context, action)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(icon)
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    this.action = Intent.ACTION_VIEW
                    putExtra(IntentData.fragmentToOpen, action)
                }
            )
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    fun createShortcuts(context: Context) {
        ShortcutManagerCompat.getDynamicShortcuts(context).takeIf { it.isEmpty() } ?: return

        shortcuts.forEach {
            val icon = IconCompat.createWithResource(context, it.drawable)
            createShortcut(context, it.action, context.getString(it.label), icon)
        }
    }
}
