package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.InstanceHelper
import com.github.libretube.api.obj.Instances
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityWelcomeBinding
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.InstancesAdapter
import com.github.libretube.ui.base.BaseActivity
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WelcomeActivity: BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private var selectedInstance: Instances? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.IO) {
            val instances = try {
                InstanceHelper.getInstances(this@WelcomeActivity)
            } catch (e: Exception) {
                toastFromMainDispatcher(e.message.orEmpty())
                InstanceHelper.getInstancesFallback(this@WelcomeActivity)
            }

            withContext(Dispatchers.Main) {
                binding.instancesRecycler.layoutManager = LinearLayoutManager(this@WelcomeActivity)
                binding.instancesRecycler.adapter = InstancesAdapter(instances) { index ->
                    selectedInstance = instances[index]
                    binding.okay.alpha = 1f
                }
                binding.progress.isGone = true
            }
        }

        binding.okay.setOnClickListener {
            if (selectedInstance != null) {
                PreferenceHelper.putString(PreferenceKeys.FETCH_INSTANCE, selectedInstance!!.apiUrl)
                val mainActivityIntent = Intent(this@WelcomeActivity, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
            } else {
                Toast.makeText(this, R.string.choose_instance, Toast.LENGTH_LONG).show()
            }
        }
    }
}
