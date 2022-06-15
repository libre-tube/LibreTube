package com.github.libretube.preferences

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.libretube.AUTHORS_URL
import com.github.libretube.BuildConfig
import com.github.libretube.CONTRIBUTING_URL
import com.github.libretube.DONATE_URL
import com.github.libretube.R
import com.github.libretube.WEBSITE_URL
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val topBarText = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarText?.text = getString(R.string.about)

        val appVersion = view.findViewById<TextView>(R.id.app_version)
        appVersion.text = BuildConfig.VERSION_NAME

        val website = view.findViewById<LinearLayout>(R.id.website)
        website.setOnClickListener {
            openLinkFromHref(WEBSITE_URL)
        }
        val authors = view.findViewById<LinearLayout>(R.id.authors)
        authors.setOnClickListener {
            openLinkFromHref(AUTHORS_URL)
        }
        val donate = view.findViewById<LinearLayout>(R.id.donate)
        donate.setOnClickListener {
            openLinkFromHref(DONATE_URL)
        }
        val contributing = view.findViewById<LinearLayout>(R.id.contributing)
        contributing.setOnClickListener {
            openLinkFromHref(CONTRIBUTING_URL)
        }
        val license = view.findViewById<LinearLayout>(R.id.license)
        license.setOnClickListener {
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
