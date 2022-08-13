package com.github.libretube.util

import android.content.Context
import android.net.ConnectivityManager
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import com.github.libretube.Globals

object ConnectionHelper {
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
                actNw.hasCapability(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        } else {
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
         */

        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }

    lateinit var imageLoader: ImageLoader

    // load an image from a url into an imageView
    fun loadImage(url: String?, target: ImageView) {
        // only load the image if the data saver mode is disabled
        if (!Globals.DATA_SAVER_MODE_ENABLED) {
            target.load(url, imageLoader)
        }
    }
}
