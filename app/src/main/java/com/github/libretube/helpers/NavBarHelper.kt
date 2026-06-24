package com.github.libretube.helpers

import android.content.Context
import android.util.Log
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.iterator
import androidx.core.view.size
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.ui.dialogs.NavBarItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

object NavBarHelper {

    private const val SEPARATOR = ","

    fun hasTabs(): Boolean {
        val prefsItems = getNavBarPrefs()

        val tabsUnchanged = prefsItems.isEmpty()
        val allTabsHidden = prefsItems.all { it.contains("-") }

        return tabsUnchanged || !allTabsHidden
    }

    // contains "-" -> invisible menu item, else -> visible menu item
    fun getNavBarItemPreference(context: Context): List<Pair<Int, Boolean>> {
        val prefItems = try {
            getNavBarPrefs()
        } catch (e: Exception) {
            Log.e("fail to parse nav items", e.toString())
            return getDefaultNavBarItems(context).map { it.itemId to it.isVisible }
        }
        val p = PopupMenu(context, null)
        MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)

        if (prefItems.size == p.menu.size) {
            return prefItems.map {
                val menuItemId = p.menu[it.replace("-", "").toInt()].itemId
                val isVisible = !it.contains("-")
                menuItemId to isVisible
            }
        }
        return getDefaultNavBarItems(context).map { it.itemId to it.isVisible }
    }

    private fun getDefaultNavBarItems(context: Context): List<MenuItem> {
        val p = PopupMenu(context, null)
        MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)
        return p.menu.iterator().asSequence().toList()
    }

    fun setNavBarItemsPreference(context: Context, items: List<NavBarItem>) {
        val prefString = mutableListOf<String>()
        val defaultNavBarItems = getDefaultNavBarItems(context)
        items.forEach { newItem ->
            val index = defaultNavBarItems.indexOfFirst { newItem.itemId == it.itemId }
            prefString.add(if (newItem.isVisible) index.toString() else "-$index")
        }
        PreferenceHelper.putString(
            PreferenceKeys.NAVBAR_ITEMS,
            prefString.joinToString(SEPARATOR)
        )
    }

    /**
     * Apply the bottom navigation style configured in the preferences
     * @return Id of the start fragment
     */
    fun applyNavBarStyle(bottomNav: BottomNavigationView): Int {
        val labelVisibilityMode = when (
            PreferenceHelper.getString(PreferenceKeys.LABEL_VISIBILITY, "selected")
        ) {
            "always" -> NavigationBarView.LABEL_VISIBILITY_LABELED
            "selected" -> NavigationBarView.LABEL_VISIBILITY_SELECTED
            "never" -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
            else -> NavigationBarView.LABEL_VISIBILITY_AUTO
        }
        bottomNav.labelVisibilityMode = labelVisibilityMode

        val navBarItems = getNavBarItemPreference(bottomNav.context)
        val startFragmentId = getStartFragmentId(bottomNav.context)

        navBarItems.forEach { (menuItemId, isVisible) ->
            bottomNav.menu.findItem(menuItemId).isVisible = isVisible
        }
        if (navBarItems.none { (_, isVisible) -> isVisible }) bottomNav.isGone = true

        return startFragmentId
    }

    fun getStartFragmentId(context: Context): Int {
        val pref = PreferenceHelper.getInt(PreferenceKeys.START_FRAGMENT, Int.MAX_VALUE)
        val defaultNavItems = getDefaultNavBarItems(context)
        return if (pref == Int.MAX_VALUE) {
            getNavBarItemPreference(context).firstOrNull { (_, isVisible) -> isVisible }?.first
                ?: R.id.homeFragment
        } else {
            defaultNavItems[pref].itemId
        }
    }

    fun setStartFragment(context: Context, itemId: Int) {
        val index = getDefaultNavBarItems(context).indexOfFirst { it.itemId == itemId }
        PreferenceHelper.putInt(PreferenceKeys.START_FRAGMENT, index)
    }

    fun getNavBarItemTitle(context: Context, menuItemId: Int): String? {
        val p = PopupMenu(context, null)
        MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)
        return p.menu.findItem(menuItemId).title?.toString()
    }

    private fun getNavBarPrefs(): List<String> {
        return PreferenceHelper
            .getString(PreferenceKeys.NAVBAR_ITEMS, "")
            .split(SEPARATOR)
    }
}
