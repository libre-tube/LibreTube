package com.github.libretube

import android.app.Application

class LibreTubeApplication : Application() {

    companion object {
        @JvmField
        var seekTo : Long? = 0
    }
}