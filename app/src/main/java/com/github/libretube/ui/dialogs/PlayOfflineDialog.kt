package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogPlayOfflineBinding
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.OfflinePlayerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlayOfflineDialog(
    private val downloadItem : DownloadWithItems,
   private val onNegativeResult : DialogInterface.OnClickListener
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val binding = DialogPlayOfflineBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_play_offline_title))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.yes)){ _, _ ->
                //PreferenceHelper.putBoolean("play_offline_remember", binding.remember.isChecked) //TODO: play offline remember isn't enough explicative
                val intent = Intent(requireContext(), OfflinePlayerActivity::class.java)
                intent.putExtra(IntentData.videoId, downloadItem.download.videoId)
                requireContext().startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), onNegativeResult)
            .show()

    }


}