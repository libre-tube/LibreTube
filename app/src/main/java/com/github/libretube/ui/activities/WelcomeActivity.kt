package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isGone
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityWelcomeBinding
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.InstancesAdapter
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.models.WelcomeModel

class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private var viewModel: WelcomeModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get()

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel!!.instances.observe(this) { instances ->
            binding.instancesRecycler?.layoutManager = LinearLayoutManager(this@WelcomeActivity)
            binding.instancesRecycler?.adapter = InstancesAdapter(instances, viewModel!!) { index ->
                viewModel!!.selectedInstanceIndex.value = index
                binding.okay?.alpha = 1f
            }
            binding.progress?.isGone = true
        }
        viewModel!!.fetchInstances(this)

        binding.okay?.alpha = if (viewModel!!.selectedInstanceIndex.value != null) 1f else 0.5f
        binding.okay?.setOnClickListener {
            if (viewModel!!.selectedInstanceIndex.value != null) {
                val selectedInstance =
                    viewModel!!.instances.value!![viewModel!!.selectedInstanceIndex.value!!]
                PreferenceHelper.putString(PreferenceKeys.FETCH_INSTANCE, selectedInstance.apiUrl)
                val mainActivityIntent = Intent(this@WelcomeActivity, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
            } else {
                Toast.makeText(this, R.string.choose_instance, Toast.LENGTH_LONG).show()
            }
        }
    }
}
