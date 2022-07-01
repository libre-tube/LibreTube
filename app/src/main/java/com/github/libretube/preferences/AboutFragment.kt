package com.github.libretube.preferences

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.databinding.FragmentAboutBinding
import com.github.libretube.util.AUTHORS_URL
import com.github.libretube.util.CONTRIBUTING_URL
import com.github.libretube.util.DONATE_URL
import com.github.libretube.util.PIPED_GITHUB_URL
import com.github.libretube.util.WEBSITE_URL
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settingsActivity = activity as SettingsActivity
        settingsActivity.binding.topBarTextView.text = getString(R.string.about)

        binding.website.setOnClickListener {
            openLinkFromHref(WEBSITE_URL)
        }
        binding.authors.setOnClickListener {
            openLinkFromHref(AUTHORS_URL)
        }
        binding.piped.setOnClickListener {
            openLinkFromHref(PIPED_GITHUB_URL)
        }
        binding.donate.setOnClickListener {
            openLinkFromHref(DONATE_URL)
        }
        binding.contributing.setOnClickListener {
            openLinkFromHref(CONTRIBUTING_URL)
        }
        binding.license.setOnClickListener {
            val licenseString = view.context.assets
                .open("gpl3.html").bufferedReader().use {
                    it.readText()
                }
            val licenseHtml = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(licenseString, 1)
            } else {
                Html.fromHtml(licenseString)
            }

            MaterialAlertDialogBuilder(view.context!!)
                .setPositiveButton(getString(R.string.okay)) { _, _ -> }
                .setMessage(licenseHtml)
                .create()
                .show()
        }
    }

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }
}
