package com.github.libretube.activity

import android.app.Activity
import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.databinding.ActivityPlayerBinding

class PlayerActivity : Activity() {
    private lateinit var binding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
