package com.github.libretube

import android.app.Activity
import android.os.Bundle

class Player : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }
}
