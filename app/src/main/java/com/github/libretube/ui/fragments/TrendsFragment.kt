package com.github.libretube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.FragmentTrendsBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.util.deArrow
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class TrendsFragment : DynamicLayoutManagerFragment() {
    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.recview?.layoutManager = VideosAdapter.getLayout(requireContext(), gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchTrending()
        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            fetchTrending()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchTrending() {
        lifecycleScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    val region = LocaleHelper.getTrendingRegion(requireContext())
                    RetrofitInstance.api.getTrending(region).deArrow()
                }
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launch
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val binding = _binding ?: return@launch
            binding.homeRefresh.isRefreshing = false
            binding.progressBar.isGone = true

            // show a [SnackBar] if there are no trending videos available
            if (response.isEmpty()) {
                Snackbar.make(binding.root, R.string.change_region, Snackbar.LENGTH_LONG)
                    .setAction(R.string.settings) {
                        val settingsIntent = Intent(context, SettingsActivity::class.java)
                        startActivity(settingsIntent)
                    }
                    .show()
                return@launch
            }

            binding.recview.adapter = VideosAdapter(response.toMutableList())
        }
    }
}
