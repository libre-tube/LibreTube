package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.AddChannelToGroupSheetBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.ui.adapters.AddChannelToGroupAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddChannelToGroupSheet : ExpandedBottomSheet() {
    private lateinit var channelId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelId = arguments?.getString(IntentData.channelId)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = AddChannelToGroupSheetBinding.inflate(layoutInflater)

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

        return binding.root
    }
}
