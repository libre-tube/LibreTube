package com.github.libretube.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import com.github.libretube.R
import com.github.libretube.constants.DONATE_URL
import com.github.libretube.constants.GITHUB_URL
import com.github.libretube.constants.LICENSE_URL
import com.github.libretube.constants.PIPED_GITHUB_URL
import com.github.libretube.constants.WEBLATE_URL
import com.github.libretube.constants.WEBSITE_URL
import com.github.libretube.databinding.ActivityAboutBinding
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.extensions.getStyledSnackBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appIcon.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, GITHUB_URL)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        binding.website.setOnClickListener {
            openLinkFromHref(WEBSITE_URL)
        }
        binding.website.setOnLongClickListener {
            onLongClick(WEBSITE_URL)
            true
        }

        binding.piped.setOnClickListener {
            openLinkFromHref(PIPED_GITHUB_URL)
        }
        binding.piped.setOnLongClickListener {
            onLongClick(PIPED_GITHUB_URL)
            true
        }

        binding.translate.setOnClickListener {
            openLinkFromHref(WEBLATE_URL)
        }
        binding.translate.setOnLongClickListener {
            onLongClick(WEBLATE_URL)
            true
        }

        binding.donate.setOnClickListener {
            openLinkFromHref(DONATE_URL)
        }
        binding.donate.setOnLongClickListener {
            onLongClick(DONATE_URL)
            true
        }

        binding.github.setOnClickListener {
            openLinkFromHref(GITHUB_URL)
        }
        binding.github.setOnLongClickListener {
            onLongClick(GITHUB_URL)
            true
        }

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

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }

    private fun onLongClick(href: String) {
        // copy the link to the clipboard
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.copied), href)
        clipboard.setPrimaryClip(clip)
        // show the snackBar with open action
        val snackBar = binding.root.getStyledSnackBar(R.string.copied_to_clipboard)
        snackBar.setAction(R.string.open_copied) {
            openLinkFromHref(href)
        }
        snackBar.animationMode = Snackbar.ANIMATION_MODE_FADE
        snackBar.show()
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
