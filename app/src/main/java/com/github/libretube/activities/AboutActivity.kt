package com.github.libretube.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import com.github.libretube.DONATE_URL
import com.github.libretube.GITHUB_URL
import com.github.libretube.PIPED_GITHUB_URL
import com.github.libretube.R
import com.github.libretube.WEBLATE_URL
import com.github.libretube.WEBSITE_URL
import com.github.libretube.databinding.ActivityAboutBinding
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.extensions.showSnackBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.website.setOnClickListener {
            openLinkFromHref(WEBSITE_URL)
        }
        binding.website.setOnLongClickListener {
            binding.root.showSnackBar(R.string.website_summary)
            true
        }

        binding.piped.setOnClickListener {
            openLinkFromHref(PIPED_GITHUB_URL)
        }
        binding.piped.setOnLongClickListener {
            binding.root.showSnackBar(R.string.piped_summary)
            true
        }

        binding.translate.setOnClickListener {
            openLinkFromHref(WEBLATE_URL)
        }
        binding.translate.setOnLongClickListener {
            binding.root.showSnackBar(R.string.translate_summary)
            true
        }

        binding.donate.setOnClickListener {
            openLinkFromHref(DONATE_URL)
        }
        binding.donate.setOnLongClickListener {
            binding.root.showSnackBar(R.string.donate_summary)
            true
        }

        binding.github.setOnClickListener {
            openLinkFromHref(GITHUB_URL)
        }
        binding.github.setOnLongClickListener {
            binding.root.showSnackBar(R.string.contributing_summary)
            true
        }

        binding.license.setOnClickListener {
            showLicense()
        }
        binding.license.setOnLongClickListener {
            binding.root.showSnackBar(R.string.license_summary)
            true
        }
    }

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }

    private fun showLicense() {
        val licenseString = assets
            ?.open("gpl3.html")
            ?.bufferedReader()
            .use {
                it?.readText()
            }

        val licenseHtml = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(licenseString.toString(), 1)
        } else {
            Html.fromHtml(licenseString.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setPositiveButton(getString(R.string.okay)) { _, _ -> }
            .setMessage(licenseHtml)
            .create()
            .show()
    }
}
