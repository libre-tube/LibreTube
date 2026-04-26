package com.github.libretube.ui.models

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.SyncServerType
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class WelcomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = savedStateHandle.getStateFlow(
        UI_STATE, UiState()
    )
    val uiState = _uiState.asLiveData()

    fun setSyncServerType(syncServerType: SyncServerType) {
        savedStateHandle[UI_STATE] =
            _uiState.value.copy(syncServerType = syncServerType, loggedIn = false)
        PreferenceHelper.putString(
            PreferenceKeys.SYNC_SERVER_TYPE,
            _uiState.value.syncServerType.name.lowercase()
        )
    }

    fun setLoggedIn(loggedIn: Boolean) {
        savedStateHandle[UI_STATE] = _uiState.value.copy(loggedIn = loggedIn)
    }

    fun setPipedInstance(instanceApiUrl: String) {
        savedStateHandle[UI_STATE] = _uiState.value.copy(selectedPipedInstance = instanceApiUrl, loggedIn = false)
    }

    fun restoreAdvancedBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            BackupHelper.restoreAdvancedBackup(context, uri)

            // only skip the welcome activity if the restored backup contains an instance
            val instancePref = PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, "")
            if (instancePref.isNotEmpty()) {
                refreshAndNavigate()
            }
        }
    }

    fun refreshAndNavigate() {
        savedStateHandle[UI_STATE] = _uiState.value.copy(navigateToMain = Unit)
    }

    fun onNavigated() {
        savedStateHandle[UI_STATE] = _uiState.value.copy(navigateToMain = null)
    }

    @Parcelize
    data class UiState(
        val syncServerType: SyncServerType = UserDataRepositoryHelper.syncServerType,
        val loggedIn: Boolean = false,
        val selectedPipedInstance: String? = PreferenceHelper.getString(
            PreferenceKeys.AUTH_INSTANCE,
            ""
        ).ifBlank { null },
        val navigateToMain: Unit? = null,
    ) : Parcelable

    companion object {
        private const val UI_STATE = "ui_state"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WelcomeViewModel(
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}