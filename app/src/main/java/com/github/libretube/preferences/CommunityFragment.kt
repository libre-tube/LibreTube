package com.github.libretube.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.libretube.DISCORD_URL
import com.github.libretube.MATRIX_URL
import com.github.libretube.R
import com.github.libretube.REDDIT_URL
import com.github.libretube.TELEGRAM_URL
import com.github.libretube.TWITTER_URL
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.databinding.FragmentCommunityBinding

class CommunityFragment : Fragment() {
    private lateinit var binding: FragmentCommunityBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.community))

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
