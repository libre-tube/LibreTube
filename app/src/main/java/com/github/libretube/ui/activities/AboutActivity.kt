package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import com.github.libretube.R
import com.github.libretube.constants.GITHUB_URL
import com.github.libretube.constants.LICENSE_URL
import com.github.libretube.constants.PIPED_GITHUB_URL
import com.github.libretube.constants.WEBLATE_URL
import com.github.libretube.constants.WEBSITE_URL
import com.github.libretube.databinding.ActivityAboutBinding
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.ui.base.BaseActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.appIcon.setOnClickListener {
            val sendIntent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, GITHUB_URL)
                .setType("text/plain")
            startActivity(Intent.createChooser(sendIntent, null))
        }

        setupCard(binding.website, WEBSITE_URL)
        setupCard(binding.piped, PIPED_GITHUB_URL)
        setupCard(binding.translate, WEBLATE_URL)
        setupCard(binding.github, GITHUB_URL)

        binding.license.setOnClickListener {
            showLicense()
        }
        binding.license.setOnLongClickListener {
            onLongClick(LICENSE_URL)
            true
        }

        binding.device.setOnClickListener {
            showDeviceInfo()
        }
    }

    private fun setupCard(card: MaterialCardView, link: String) {
        card.setOnClickListener {
            IntentHelper.openLinkFromHref(this, supportFragmentManager, link)
        }
        card.setOnLongClickListener {
            onLongClick(link)
            true
        }
    }

    private fun onLongClick(href: String) {
        // copy the link to the clipboard
        ClipboardHelper.save(this, href)
        // show the snackBar with open action
        Snackbar.make(
            binding.root,
            R.string.copied_to_clipboard,
            Snackbar.LENGTH_LONG,
        )
            .setAction(R.string.open_copied) {
                IntentHelper.openLinkFromHref(this, supportFragmentManager, href)
            }
            .setAnimationMode(Snackbar.ANIMATION_MODE_FADE)
            .show()
    }

    private fun showLicense() {
        val licenseHtml = assets.open("gpl3.html")
            .bufferedReader()
            .use { it.readText() }
            .parseAsHtml(HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH)

        MaterialAlertDialogBuilder(this)
            .setPositiveButton(getString(R.string.okay)) { _, _ -> }
            .setMessage(licenseHtml)
            .create()
            .show()
    }

    private fun showDeviceInfo() {
        val text = "Manufacturer: ${Build.MANUFACTURER}\n" +
            "Model: ${Build.MODEL}\n" +
            "SDK: ${Build.VERSION.SDK_INT}\n" +
            "Board: ${Build.BOARD}\n" +
            "OS: Android ${Build.VERSION.RELEASE}\n" +
            "Arch: ${Build.SUPPORTED_ABIS[0]}\n" +
            "Product: ${Build.PRODUCT}"

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.device_info)
            .setMessage(text)
            .setPositiveButton(R.string.okay, null)
            .show()
    }
}
