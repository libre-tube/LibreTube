package com.github.libretube.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import retrofit2.HttpException
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.databinding.FragmentHomeBinding
import java.io.IOException

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHome.layoutManager =
            GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))
        fetchJson()
        binding.srlHome.isEnabled = true
        binding.srlHome.setOnRefreshListener {
            Log.d(TAG,"hmm")
            fetchJson()
        }
    }

    private fun fetchJson() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                    RetrofitInstance.api.getTrending(sharedPreferences.getString("region", "US")!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context,R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context,R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }finally {
                    binding.srlHome.isRefreshing = false
                }
                runOnUiThread {
                    binding.pbHome.isVisible = false
                    binding.rvHome.adapter = TrendingAdapter(response)
                }
            }
        }
        run()
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onDestroyView() {
        binding.rvHome.adapter = null
        super.onDestroyView()
    }
}
