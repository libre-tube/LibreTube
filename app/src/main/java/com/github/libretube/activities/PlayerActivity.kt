package com.github.libretube.activities

import android.app.Activity
import android.os.Bundle
import com.github.libretube.R

class PlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }
}
