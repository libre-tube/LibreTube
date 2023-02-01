package com.github.libretube.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.libretube.constants.DISCORD_URL
import com.github.libretube.constants.FAQ_URL
import com.github.libretube.constants.MASTODON_URL
import com.github.libretube.constants.MATRIX_URL
import com.github.libretube.constants.TELEGRAM_URL
import com.github.libretube.databinding.ActivityHelpBinding
import com.github.libretube.ui.base.BaseActivity
import com.google.android.material.card.MaterialCardView

class HelpActivity : BaseActivity() {
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupCard(binding.faq, FAQ_URL)
        setupCard(binding.matrix, MATRIX_URL)
        setupCard(binding.mastodon, MASTODON_URL)
        setupCard(binding.telegram, TELEGRAM_URL)
        setupCard(binding.discord, DISCORD_URL)
        setupCard(binding.mastodon, MASTODON_URL)
    }

    private fun setupCard(card: MaterialCardView, link: String) {
        card.setOnClickListener {
            openLinkFromHref(link)
        }
    }

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }
}
