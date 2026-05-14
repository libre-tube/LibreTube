package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.SimpleOptionsRecyclerBinding
import com.github.libretube.ui.adapters.InstancesAdapter
import com.github.libretube.ui.models.InstancesModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SelectInstanceDialog : DialogFragment() {
    val viewModel: InstancesModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = requireArguments().getString(SELECT_INSTANCE_TITLE_EXTRA)!!
        var selectedInstanceUrl =
            requireArguments().getString(SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA)

        val binding = SimpleOptionsRecyclerBinding.inflate(layoutInflater)
        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            viewModel.instances.collect { instances ->
                val selectedIndex = instances.indexOfFirst { it.apiUrl == selectedInstanceUrl }
                val adapter = InstancesAdapter(selectedIndex) {
                    selectedInstanceUrl = instances[it].apiUrl
                }
                adapter.submitList(instances)
                binding.optionsRecycler.adapter = adapter
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setNeutralButton(R.string.addInstance) { _, _ ->
                CreateCustomInstanceDialog().show(requireActivity().supportFragmentManager, null)
            }
            .setPositiveButton(R.string.okay) { _, _ ->
                setFragmentResult(
                    SELECT_INSTANCE_RESULT_KEY,
                    bundleOf(SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA to selectedInstanceUrl)
                )
            }
            .show()
    }

    companion object {
        const val SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA = "current_instance"
        const val SELECT_INSTANCE_TITLE_EXTRA = "title"
        const val SELECT_INSTANCE_RESULT_KEY = "instance_select_result"
    }
}