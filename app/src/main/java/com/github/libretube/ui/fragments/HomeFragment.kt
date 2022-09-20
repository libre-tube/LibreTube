package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.adapters.ChannelAdapter
import com.github.libretube.ui.adapters.TrendingAdapter
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.PreferenceHelper
import retrofit2.HttpException
import java.io.IOException

class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var region: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val regionPref = PreferenceHelper.getString(PreferenceKeys.REGION, "sys")

        // get the system default country if auto region selected
        region = if (regionPref == "sys") {
            LocaleHelper
                .getDetectedCountry(requireContext(), "UK")
                .uppercase()
        } else {
            regionPref
        }

        fetchTrending()
        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            fetchTrending()
        }
    }

    private fun fetchTrending() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getTrending(region)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } finally {
                binding.homeRefresh.isRefreshing = false
            }
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (
                    PreferenceHelper.getBoolean(
                        PreferenceKeys.ALTERNATIVE_TRENDING_LAYOUT,
                        false
                    )
                ) {
                    binding.recview.layoutManager = LinearLayoutManager(context)

                    binding.recview.adapter = ChannelAdapter(
                        response.toMutableList(),
                        childFragmentManager
                    )
                } else {
                    binding.recview.layoutManager = GridLayoutManager(
                        context,
                        PreferenceHelper.getString(
                            PreferenceKeys.GRID_COLUMNS,
                            resources.getInteger(R.integer.grid_items).toString()
                        ).toInt()
                    )

                    binding.recview.adapter = TrendingAdapter(response, childFragmentManager)
                }
            }
        }
    }
}
