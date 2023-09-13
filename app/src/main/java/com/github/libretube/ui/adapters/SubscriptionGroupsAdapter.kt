package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SubscriptionGroupRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.models.EditChannelGroupsModel
import com.github.libretube.ui.sheets.EditChannelGroupSheet
import com.github.libretube.ui.viewholders.SubscriptionGroupsViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionGroupsAdapter(
    var groups: MutableList<SubscriptionGroup>,
    private val viewModel: EditChannelGroupsModel,
    private val parentFragmentManager: FragmentManager
) : RecyclerView.Adapter<SubscriptionGroupsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionGroupsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubscriptionGroupRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionGroupsViewHolder(binding)
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: SubscriptionGroupsViewHolder, position: Int) {
        val subscriptionGroup = groups[position]
        holder.binding.apply {
            groupName.text = subscriptionGroup.name

            deleteGroup.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseHolder.Database.subscriptionGroupsDao()
                        .deleteGroup(subscriptionGroup.name)

                    groups.removeAt(position)
                    viewModel.groups.postValue(groups)
                }
            }

            editGroup.setOnClickListener {
                viewModel.groupToEdit = subscriptionGroup
                EditChannelGroupSheet().show(parentFragmentManager, null)
            }
        }
    }
}
