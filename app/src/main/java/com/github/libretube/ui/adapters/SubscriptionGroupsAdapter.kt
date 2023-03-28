package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SubscriptionGroupRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.dialogs.EditChannelGroupDialog
import com.github.libretube.ui.viewholders.SubscriptionGroupsViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SubscriptionGroupsAdapter(
    private val groups: MutableList<SubscriptionGroup>,
    private val parentFragmentManager: FragmentManager,
    private val onGroupsChanged: (List<SubscriptionGroup>) -> Unit
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
                groups.remove(subscriptionGroup)
                runBlocking(Dispatchers.IO) {
                    DatabaseHolder.Database.subscriptionGroupsDao().deleteGroup(
                        subscriptionGroup.name
                    )
                }
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemCount)
            }
            editGroup.setOnClickListener {
                EditChannelGroupDialog(subscriptionGroup) {
                    groups[position] = it
                    runBlocking(Dispatchers.IO) {
                        // delete the old one as it might have a different name
                        DatabaseHolder.Database.subscriptionGroupsDao().deleteGroup(
                            subscriptionGroup.name
                        )
                        DatabaseHolder.Database.subscriptionGroupsDao().createGroup(it)
                    }
                    notifyItemChanged(position)
                    onGroupsChanged(groups)
                }.show(parentFragmentManager, null)
            }
        }
    }

    fun insertItem(subscriptionsGroup: SubscriptionGroup) {
        groups.add(subscriptionsGroup)
        notifyItemInserted(itemCount - 1)
    }
}
