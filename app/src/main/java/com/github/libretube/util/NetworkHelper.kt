package com.github.libretube.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkHelper {
    /**
     * Detect whether network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }

    /**
     * Detect whether the current network is mobile data
     * @param context Context of the application
     * @return isNetworkMobile
     */
    @Suppress("DEPRECATION")
    fun isNetworkMobile(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.activeNetwork ?: return false
            )
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        } else {
            val activeNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            return activeNetwork != null && activeNetwork.isConnected
        }
    }
}
