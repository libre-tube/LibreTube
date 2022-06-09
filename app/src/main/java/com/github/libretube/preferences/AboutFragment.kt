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
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.util.checkUpdate
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

        val appVersion = view?.findViewById<TextView>(R.id.app_version)
        appVersion.text = BuildConfig.VERSION_NAME

        val website = view?.findViewById<LinearLayout>(R.id.website)
        website?.setOnClickListener {
            openLinkFromHref("https://libre-tube.github.io/")
        }
        val authors = view?.findViewById<LinearLayout>(R.id.authors)
        authors?.setOnClickListener {
            openLinkFromHref("https://github.com/libre-tube/LibreTube/graphs/contributors")
        }
        val donate = view?.findViewById<LinearLayout>(R.id.donate)
        donate?.setOnClickListener {
            openLinkFromHref("https://libre-tube.github.io/#donate")
        }
        val contributing = view?.findViewById<LinearLayout>(R.id.contributing)
        contributing?.setOnClickListener {
            openLinkFromHref("https://github.com/libre-tube/LibreTube")
        }
        val license = view.findViewById<LinearLayout>(R.id.license)
        license?.setOnClickListener {
            val licenseString = view?.context?.assets!!
                .open("gpl3.html").bufferedReader().use {
                    it.readText()
                }
            val licenseHtml = if (Build.VERSION.SDK_INT >= 24) Html.fromHtml(licenseString, 1)
            else Html.fromHtml(licenseString)

            MaterialAlertDialogBuilder(view?.context!!)
                .setPositiveButton(getString(R.string.okay)) { _, _ -> }
                .setMessage(licenseHtml)
                .create()
                .show()
            true
        }
        val update = view.findViewById<LinearLayout>(R.id.update)
        update?.setOnClickListener {
            checkUpdate(childFragmentManager)
        }
    }

    private fun openLinkFromHref(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
        startActivity(intent)
    }
}
