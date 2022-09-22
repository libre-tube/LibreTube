package com.github.libretube.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.libretube.constants.*
import com.github.libretube.databinding.ActivityCommunityBinding
import com.github.libretube.extensions.BaseActivity

class CommunityActivity : BaseActivity() {
    private lateinit var binding: ActivityCommunityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.telegram.setOnClickListener {
            openLinkFromHref(TELEGRAM_URL)
        }

        binding.matrix.setOnClickListener {
            openLinkFromHref(MATRIX_URL)
        }

        binding.discord.setOnClickListener {
            openLinkFromHref(DISCORD_URL)
        }

        binding.reddit.setOnClickListener {
            openLinkFromHref(REDDIT_URL)
        }

        binding.twitter.setOnClickListener {
            openLinkFromHref(TWITTER_URL)
        }
    }

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }
}
