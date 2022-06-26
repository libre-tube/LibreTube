package com.github.libretube.preferences

import android.os.Bundle
import android.widget.TextView
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R

class SponsorBlockSettings : PreferenceFragmentCompat() {
    private val TAG = "SponsorBlockSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)

        val topBarTextView = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarTextView?.text = getString(R.string.sponsorblock)
    }
}
