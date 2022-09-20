package com.github.libretube.util

import android.view.Menu
import android.view.MenuItem
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.NavBarItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

object NavBarHelper {
    val preferenceKey = "nav_bar_items"

    val defaultNavBarItems = listOf(
        NavBarItem(
            R.id.homeFragment,
            R.string.startpage
        ),
        NavBarItem(
            R.id.subscriptionsFragment,
            R.string.subscriptions
        ),
        NavBarItem(
            R.id.libraryFragment,
            R.string.library
        )
    )

    val mapper = ObjectMapper()

    fun getNavBarItems(): List<NavBarItem> {
        return try {
            val type = object : TypeReference<List<NavBarItem>>() {}
            mapper.readValue(
                PreferenceHelper.getString(
                    preferenceKey,
                    ""
                ),
                type
            )
        } catch (e: Exception) {
            return defaultNavBarItems
        }
    }

    fun setNavBarItems(items: List<NavBarItem>) {
        PreferenceHelper.putString(
            preferenceKey,
            mapper.writeValueAsString(items)
        )
    }

    /**
     * Apply the bottom navigation style configured in the preferences
     * @return Id of the start fragment
     */
    fun applyNavBarStyle(bottomNav: BottomNavigationView): Int {
        val labelVisibilityMode = when (
            PreferenceHelper.getString(PreferenceKeys.LABEL_VISIBILITY, "always")
        ) {
            "always" -> NavigationBarView.LABEL_VISIBILITY_LABELED
            "selected" -> NavigationBarView.LABEL_VISIBILITY_SELECTED
            "never" -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
            else -> NavigationBarView.LABEL_VISIBILITY_AUTO
        }
        bottomNav.labelVisibilityMode = labelVisibilityMode

        val navBarItems = getNavBarItems()

        val menuItems = mutableListOf<MenuItem>()
        // remove the old items
        navBarItems.forEach {
            menuItems.add(
                bottomNav.menu.findItem(it.id)
            )
            bottomNav.menu.removeItem(it.id)
        }

        navBarItems.forEach { navBarItem ->
            if (navBarItem.isEnabled) {
                val menuItem = menuItems.filter { it.itemId == navBarItem.id }[0]

                bottomNav.menu.add(
                    menuItem.groupId,
                    menuItem.itemId,
                    Menu.NONE,
                    menuItem.title
                ).icon = menuItem.icon
            }
        }
        return navBarItems[0].id
    }
}
