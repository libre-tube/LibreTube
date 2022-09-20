package com.github.libretube.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.constants.navBarItems
import com.github.libretube.obj.NavBarItem

object NavBarHelper {
    val preferenceKey = "nav_bar_items"
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
            return navBarItems
        }
    }

    fun setNavBarItems(items: List<NavBarItem>) {
        PreferenceHelper.putString(
            preferenceKey,
            mapper.writeValueAsString(items)
        )
    }
}
