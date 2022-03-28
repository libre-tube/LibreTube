package com.github.libretube

import android.app.Application

class myApp : Application() {

    companion object {
        @JvmField
        var seekTo: Long? = 0
    }
}
