package com.github.libretube.ui.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityWelcomeBinding
import com.github.libretube.enums.SyncServerType
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.LoginDialog
import com.github.libretube.ui.dialogs.SelectInstanceDialog
import com.github.libretube.ui.models.WelcomeViewModel
import com.github.libretube.ui.preferences.BackupRestoreSettings
import com.github.libretube.ui.preferences.InstanceSettings.Companion.INSTANCE_DIALOG_REQUEST_KEY

class WelcomeActivity : BaseActivity() {
    private val viewModel by viewModels<WelcomeViewModel> { WelcomeViewModel.Factory }

    private val restoreFilePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            viewModel.restoreAdvancedBackup(this, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.restore.setOnClickListener {
            restoreFilePicker.launch(BackupRestoreSettings.JSON)
        }

        binding.okay.setOnClickListener {
            viewModel.refreshAndNavigate()
        }

        supportFragmentManager.setFragmentResultListener(
            INSTANCE_DIALOG_REQUEST_KEY,
            this
        ) { _, bundle ->
            val loggedIn = bundle.getBoolean(IntentData.loginTask)
            if (loggedIn) viewModel.setLoggedIn(true)
        }
        binding.login.setOnClickListener {
            LoginDialog().show(supportFragmentManager, null)
        }

        supportFragmentManager.setFragmentResultListener(
            SelectInstanceDialog.SELECT_INSTANCE_RESULT_KEY,
            this
        ) { _, bundle ->
            val apiUrl =
                bundle.getString(SelectInstanceDialog.SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA)
            if (apiUrl != null) {
                PreferenceHelper.setToken("")
                PreferenceHelper.putString(PreferenceKeys.AUTH_INSTANCE, apiUrl)
                viewModel.setPipedInstance(apiUrl)
            }
        }
        binding.selectInstance.setOnClickListener {
            SelectInstanceDialog()
                .apply {
                    val selectedInstance = this@WelcomeActivity.viewModel.uiState.value?.selectedPipedInstance
                    arguments = bundleOf(
                        SelectInstanceDialog.SELECT_INSTANCE_TITLE_EXTRA to this@WelcomeActivity.getString(R.string.auth_instance),
                        SelectInstanceDialog.SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA to selectedInstance
                    )
                }
                .show(supportFragmentManager, null)
        }

        binding.syncTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val index = binding.syncTypeGroup.children.indexOfFirst { it.id == checkedId }
                val syncServerType = SyncServerType.entries[index]
                viewModel.setSyncServerType(syncServerType)
            }
        }

        // HACK: check if user already used app before by checking if they already loaded the subscriptions feed
        val userAlreadyUsedAppBefore = PreferenceHelper.getLastCheckedFeedTime(false) != 0L

        viewModel.uiState.observe(this) { (syncServerType, loggedIn, pipedInstance, navigateToMain) ->
            binding.okay.isEnabled = syncServerType == SyncServerType.NONE || loggedIn

            binding.selectInstance.isVisible = syncServerType == SyncServerType.PIPED
            binding.login.isVisible = syncServerType != SyncServerType.NONE
            binding.login.isEnabled =
                syncServerType == SyncServerType.LIBRETUBE || (syncServerType == SyncServerType.PIPED && pipedInstance != null)

            binding.switchSyncTypeWarning.isVisible = syncServerType != SyncServerType.NONE && userAlreadyUsedAppBefore

            binding.infoText.text = when (syncServerType) {
                SyncServerType.NONE -> getString(R.string.sync_type_summary_none)
                SyncServerType.LIBRETUBE -> getString(
                    R.string.sync_type_summary_libretube,
                    "https://github.com/libre-tube/sync-server"
                )

                SyncServerType.PIPED -> getString(
                    R.string.sync_type_summary_piped,
                    "https://github.com/TeamPiped/piped"
                )
            }

            navigateToMain?.let {
                PreferenceHelper.putBoolean(PreferenceKeys.WELCOME_ACTIVITY_FINISHED, true)

                val mainActivityIntent = Intent(this, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
                viewModel.onNavigated()
            }
        }

        // set initially displayed option from settings
        val selected = UserDataRepositoryHelper.syncServerType
        binding.syncTypeGroup.check(binding.syncTypeGroup.children.toList()[selected.ordinal].id)
        // trigger initial change of sync server so that the info text is updated (see above)
        viewModel.setSyncServerType(selected)
    }

    override fun requestOrientationChange() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }
}