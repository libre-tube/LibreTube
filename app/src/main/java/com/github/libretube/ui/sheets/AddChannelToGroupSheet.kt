package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogAddChannelToGroupBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.ui.adapters.AddChannelToGroupAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddChannelToGroupSheet : ExpandedBottomSheet(R.layout.dialog_add_channel_to_group) {
    private lateinit var channelId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelId = arguments?.getString(IntentData.channelId)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogAddChannelToGroupBinding.bind(view)

        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        binding.cancel.setOnClickListener {
            requireDialog().dismiss()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val subGroupsDao = DatabaseHolder.Database.subscriptionGroupsDao()
            val subscriptionGroups = subGroupsDao.getAll().sortedBy { it.index }.toMutableList()

            withContext(Dispatchers.Main) {
                binding.groupsRV.adapter = AddChannelToGroupAdapter(subscriptionGroups, channelId)

                binding.okay.setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch(Dispatchers.IO) {
                        subGroupsDao.updateAll(subscriptionGroups)

                        withContext(Dispatchers.Main) {
                            dialog?.dismiss()
                        }
                    }
                }
            }
        }
    }
}
