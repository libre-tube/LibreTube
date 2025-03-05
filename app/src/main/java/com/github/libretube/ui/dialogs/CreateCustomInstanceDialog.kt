package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogCustomInstanceBinding
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.models.CustomInstancesModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException

class CreateCustomInstanceDialog : DialogFragment() {
    val viewModel: CustomInstancesModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCustomInstanceBinding.inflate(layoutInflater)
        arguments?.parcelable<CustomInstance>(IntentData.customInstance)?.let { initialInstance ->
            binding.instanceName.setText(initialInstance.name)
            binding.instanceApiUrl.setText(initialInstance.apiUrl)
            binding.instanceFrontendUrl.setText(initialInstance.frontendUrl)
        }

        binding.instanceApiUrl.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !binding.instanceName.text.isNullOrEmpty()) return@setOnFocusChangeListener

            // automatically set the api name
            val apiUrl = binding.instanceApiUrl.text.toString().toHttpUrlOrNull()
            if (apiUrl != null) {
                binding.instanceName.setText(apiUrl.host)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.customInstance)
            .setView(binding.root)
            .setPositiveButton(R.string.addInstance, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val instanceName = binding.instanceName.text.toString()
                    val apiUrl = binding.instanceApiUrl.text.toString()
                    val frontendUrl = binding.instanceFrontendUrl.text.toString()

                    try {
                        viewModel.addCustomInstance(apiUrl, instanceName, frontendUrl)
                        requireDialog().dismiss()
                    } catch (e: IllegalArgumentException) {
                        context.toastFromMainThread(R.string.empty_instance)
                    } catch (e: MalformedURLException) {
                        context.toastFromMainThread(R.string.invalid_url)
                    }
                }
            }
    }
}
