package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.BackupOptionsAdapter
import com.github.libretube.databinding.DialogBackupBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.await
import com.github.libretube.obj.BackupFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BackupDialog(
    private val createBackupFile: (BackupFile) -> Unit
) : DialogFragment() {
    private lateinit var binding: DialogBackupBinding

    val backupFile = BackupFile()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val backupOptions = listOf(
            R.string.watch_history,
            R.string.watch_positions,
            R.string.search_history,
            R.string.local_subscriptions,
            R.string.backup_customInstances
        )

        val selected = mutableListOf(false, false, false, false, false)

        binding = DialogBackupBinding.inflate(layoutInflater)
        binding.backupOptionsRecycler.layoutManager = LinearLayoutManager(context)
        binding.backupOptionsRecycler.adapter =
            BackupOptionsAdapter(backupOptions) { position, isChecked ->
                selected[position] = isChecked
            }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup) { _, _ ->
                Thread {
                    if (selected[0]) {
                        backupFile.watchHistory =
                            DatabaseHolder.db.watchHistoryDao().getAll()
                    }
                    if (selected[1]) {
                        backupFile.watchPositions =
                            DatabaseHolder.db.watchPositionDao().getAll()
                    }
                    if (selected[2]) {
                        backupFile.searchHistory =
                            DatabaseHolder.db.searchHistoryDao().getAll()
                    }
                    if (selected[3]) {
                        backupFile.localSubscriptions =
                            DatabaseHolder.db.localSubscriptionDao().getAll()
                    }
                    if (selected[4]) {
                        backupFile.customInstances =
                            DatabaseHolder.db.customInstanceDao().getAll()
                    }
                }.await()

                createBackupFile(backupFile)
            }
            .create()
    }
}
