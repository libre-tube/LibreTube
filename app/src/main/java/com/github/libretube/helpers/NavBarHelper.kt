package com.github.libretube.helpers

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.size
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

object NavBarHelper {
    private const val SEPARATOR = ","

    // contains "-" -> invisible menu item, else -> visible menu item
    fun getNavBarItems(context: Context): List<MenuItem> {
        val prefItems = try {
            PreferenceHelper.getString(
                PreferenceKeys.NAVBAR_ITEMS,
                ""
            ).split(SEPARATOR)
        } catch (e: Exception) {
            Log.e("fail to parse nav items", e.toString())
            return getDefaultNavBarItems(context)
        }
        val p = PopupMenu(context, null)
        MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)

        if (prefItems.size == p.menu.size) {
            val navBarItems = mutableListOf<MenuItem>()
            prefItems.forEach {
                navBarItems.add(
                    p.menu[it.replace("-", "").toInt()].apply {
                        this.isVisible = !it.contains("-")
                    }
                )
            }
            return navBarItems
        }
        return getDefaultNavBarItems(context)
    }

    private fun getDefaultNavBarItems(context: Context): List<MenuItem> {
        val p = PopupMenu(context, null)
        MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)
        val navBarItems = mutableListOf<MenuItem>()
        p.menu.forEach {
            navBarItems.add(it)
        }
        return navBarItems
    }

    fun setNavBarItems(items: List<MenuItem>, context: Context) {
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

        val navBarItems = getNavBarItems(bottomNav.context)

        val menuItems = mutableListOf<MenuItem>()
        // remove the old items
        navBarItems.forEach {
            menuItems.add(
                bottomNav.menu.findItem(it.itemId)
            )
            bottomNav.menu.removeItem(it.itemId)
        }

        navBarItems.forEach { navBarItem ->
            if (navBarItem.isVisible) {
                val menuItem = menuItems.first { it.itemId == navBarItem.itemId }

                bottomNav.menu.add(
                    menuItem.groupId,
                    menuItem.itemId,
                    Menu.NONE,
                    menuItem.title
                ).icon = menuItem.icon
            }
        }
        if (navBarItems.filter { it.isVisible }.isEmpty()) bottomNav.visibility = View.GONE

        return getStartFragmentId(bottomNav.context)
    }

    fun getStartFragmentId(context: Context): Int {
        val pref = PreferenceHelper.getInt(PreferenceKeys.START_FRAGMENT, Int.MAX_VALUE)
        val defaultNavItems = getDefaultNavBarItems(context)
        return if (pref == Int.MAX_VALUE) {
            getNavBarItems(context).firstOrNull { it.isVisible }?.itemId ?: R.id.homeFragment
        } else {
            defaultNavItems.get(pref).itemId
        }
    }

    fun setStartFragment(context: Context, itemId: Int) {
        val index = getDefaultNavBarItems(context).indexOfFirst { it.itemId == itemId }
        PreferenceHelper.putInt(PreferenceKeys.START_FRAGMENT, index)
    }
}
