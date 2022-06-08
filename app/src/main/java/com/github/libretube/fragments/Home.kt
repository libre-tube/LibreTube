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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.R
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.util.RetrofitInstance
import java.io.IOException
import retrofit2.HttpException

class Home : Fragment() {

    private val TAG = "HomeFragment"
    private var refreshLayout: SwipeRefreshLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recview)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val grid = sharedPreferences.getString(
            "grid",
            resources.getInteger(R.integer.grid_items).toString()
        )!!
        recyclerView.layoutManager = GridLayoutManager(view.context, grid.toInt())
        val progressbar = view.findViewById<ProgressBar>(R.id.progressBar)
        fetchJson(progressbar, recyclerView)
        refreshLayout = view.findViewById(R.id.home_refresh)
        refreshLayout?.isEnabled = true
        refreshLayout?.setOnRefreshListener {
            Log.d(TAG, "hmm")
            fetchJson(progressbar, recyclerView)
        }
    }

    private fun fetchJson(progressBar: ProgressBar, recyclerView: RecyclerView) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                    RetrofitInstance.api.getTrending(sharedPreferences.getString("region", "US")!!)
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
                    refreshLayout?.isRefreshing = false
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

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.recview)?.adapter = null
        refreshLayout = null
        Log.e(TAG, "destroyview")
        super.onDestroyView()
    }
}
