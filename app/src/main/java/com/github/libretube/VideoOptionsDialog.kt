package com.github.libretube

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsDialog(private val videoId: String, context: Context) : DialogFragment() {
    /**
     * List that stores the different menu options. In the future could be add more options here.
     */
    private val list = listOf(
        context.getString(R.string.playOnBackground),
        context.getString(R.string.addToPlaylist),
        context.getString(R.string.share)
    )

    /**
     * Dialog that returns a [MaterialAlertDialogBuilder] showing a menu of options.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.video_options_dialog_item,
                    list
                )
            ) { dialog, which ->
                // For now, this checks the position of the option with the position that is in the
                // list. I don't like it, but we will do like this for now.
                when (which) {
                    // This for example will be the "Background mode" option
                    0 -> {
                        BackgroundMode
                            .getInstance()
                            .playOnBackgroundMode(requireContext(), videoId, 0)
                    }
                    // Add Video to Playlist Dialog
                    1 -> {
                        val sharedPref = context?.getSharedPreferences(
                            "token",
                            Context.MODE_PRIVATE
                        )
                        val token = sharedPref?.getString("token", "")
                        if (token != "") {
                            val newFragment = AddtoPlaylistDialog()
                            val bundle = Bundle()
                            bundle.putString("videoId", videoId)
                            newFragment.arguments = bundle
                            newFragment.show(parentFragmentManager, "AddToPlaylist")
                        } else {
                            Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        /* crashes
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val instancePref = sharedPreferences.getString(
                            "instance",
                            "https://pipedapi.kavin.rocks"
                        )!!
                        val instance = "&instance=${URLEncoder.encode(instancePref, "UTF-8")}"
                        val shareOptions = arrayOf(
                            getString(R.string.piped),
                            getString(R.string.instance),
                            getString(R.string.youtube)
                        )
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.share))
                            .setItems(
                                shareOptions
                            ) { _, id ->
                                val url = when (id) {
                                    0 -> "https://piped.kavin.rocks/watch?v=$videoId"
                                    1 -> "https://piped.kavin.rocks/watch?v=$videoId$instance"
                                    2 -> "https://youtu.be/$videoId"
                                    else -> "https://piped.kavin.rocks/watch?v=$videoId"
                                }
                                dismiss()
                                val intent = Intent()
                                intent.action = Intent.ACTION_SEND
                                intent.putExtra(Intent.EXTRA_TEXT, url)
                                intent.type = "text/plain"
                                startActivity(Intent.createChooser(intent, "Share Url To:"))
                            }
                            .show()
                         */
                        val intent = Intent()
                        intent.action = Intent.ACTION_SEND
                        intent.putExtra(
                            Intent.EXTRA_TEXT,
                            "https://piped.kavin.rocks/watch?v=$videoId"
                        )
                        intent.type = "text/plain"
                        startActivity(Intent.createChooser(intent, "Share Url To:"))
                    }
                    else -> {
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    companion object {
        const val TAG = "VideoOptionsDialog"
    }
}
