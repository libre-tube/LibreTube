package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding

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
        val grid = PreferenceHelper.getString(
            requireContext(),
            "grid",
            resources.getInteger(R.integer.grid_items).toString()
        )!!
        binding.recview.layoutManager = GridLayoutManager(view.context, grid.toInt())
        fetchJson(binding.progressBar, binding.recview)
        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            Log.d(TAG, "hmm")
            fetchJson(binding.progressBar, binding.recview)
        }
    }

    private fun fetchJson(progressBar: ProgressBar, recyclerView: RecyclerView) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val region = PreferenceHelper.getString(requireContext(), "region", "US")
                    RetrofitInstance.api.getTrending(region!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } finally {
                    binding.homeRefresh.isRefreshing = false
                }
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    recyclerView.adapter = TrendingAdapter(response, childFragmentManager)
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
}
