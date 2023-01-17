package com.github.libretube.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.libretube.constants.DISCORD_URL
import com.github.libretube.constants.FAQ_URL
import com.github.libretube.constants.MATRIX_URL
import com.github.libretube.constants.REDDIT_URL
import com.github.libretube.constants.TELEGRAM_URL
import com.github.libretube.constants.TWITTER_URL
import com.github.libretube.databinding.ActivityHelpBinding
import com.github.libretube.ui.base.BaseActivity

class HelpActivity : BaseActivity() {
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.faq.setOnClickListener {
            openLinkFromHref(FAQ_URL)
        }

        binding.matrix.setOnClickListener {
            openLinkFromHref(MATRIX_URL)
        }

        binding.telegram.setOnClickListener {
            openLinkFromHref(TELEGRAM_URL)
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
