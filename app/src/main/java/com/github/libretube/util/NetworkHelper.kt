package com.github.libretube.util

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService

object NetworkHelper {
    /**
     * Detect whether network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>()

        // this seems to not recognize vpn connections
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                // WiFi
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                // Mobile
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                // Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                // Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                // VPN
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        } else {
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
         */

        @Suppress("DEPRECATION")
        return connectivityManager?.activeNetworkInfo?.isConnected ?: false
    }

    /**
     * Detect whether the current network is metered
     * @param context Context of the application
     * @return whether the network is metered or not
     */
    fun isNetworkMetered(context: Context): Boolean {
        return context.getSystemService<ConnectivityManager>()!!.isActiveNetworkMetered
    }
}
