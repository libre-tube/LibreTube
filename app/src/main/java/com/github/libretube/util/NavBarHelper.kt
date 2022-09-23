package com.github.libretube.util

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.core.view.forEach
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.NavBarItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

object NavBarHelper {
    private const val preferenceKey = "nav_bar_items"

    private val mapper = ObjectMapper()

    fun getNavBarItems(context: Context): List<NavBarItem> {
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
            val p = PopupMenu(context, null)
            MenuInflater(context).inflate(R.menu.bottom_menu, p.menu)
            val defaultNavBarItems = mutableListOf<NavBarItem>()
            p.menu.forEach {
                defaultNavBarItems.add(
                    NavBarItem(
                        it.itemId,
                        it.title.toString()
                    )
                )
            }
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
