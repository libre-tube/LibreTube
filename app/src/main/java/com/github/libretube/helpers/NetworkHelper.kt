package com.github.libretube.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService

object NetworkHelper {
    /**
     * Detect whether network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // In case we are using a VPN, we return true since we might be using reverse tethering
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return false

        if (Build.VERSION.SDK_INT >= 23) {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            val hasConnection = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            return hasConnection || isVpn
        } else {
            if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                return true
            }

            // activeNetworkInfo might return null instead of the VPN, so better check it explicitly
            val vpnInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)
            return vpnInfo?.isConnected == true
        }
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
