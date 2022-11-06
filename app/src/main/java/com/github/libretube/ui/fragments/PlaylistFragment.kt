package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentPlaylistBinding
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.adapters.PlaylistAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import retrofit2.HttpException
import java.io.IOException

class PlaylistFragment : BaseFragment() {
    private lateinit var binding: FragmentPlaylistBinding

    private var playlistId: String? = null
    private var playlistName: String? = null
    private var isOwner: Boolean = false
    private var nextPage: String? = null
    private var playlistAdapter: PlaylistAdapter? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString(IntentData.playlistId)
            isOwner = it.getBoolean("isOwner")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = playlistId!!.toID()
        binding.playlistRecView.layoutManager = LinearLayoutManager(context)

        binding.playlistProgress.visibility = View.VISIBLE
        fetchPlaylist()
    }

    @SuppressLint("SetTextI18n")
    private fun fetchPlaylist() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                // load locally stored playlists with the auth api
                if (isOwner) {
                    RetrofitInstance.authApi.getPlaylist(playlistId!!)
                } else {
                    RetrofitInstance.api.getPlaylist(playlistId!!)
                }
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            nextPage = response.nextpage
            playlistName = response.name
            isLoading = false
            runOnUiThread {
                ImageHelper.loadImage(response.thumbnailUrl, binding.thumbnail)
                binding.playlistProgress.visibility = View.GONE
                binding.playlistName.text = response.name
                binding.playlistInfo.text = response.uploader + " • " + getString(R.string.videoCount, response.videos.toString())

                // show playlist options
                binding.optionsMenu.setOnClickListener {
                    PlaylistOptionsBottomSheet(playlistId!!, playlistName!!, isOwner).show(
                        childFragmentManager,
                        PlaylistOptionsBottomSheet::class.java.name
                    )
                }

                binding.playAll.setOnClickListener {
                    NavigationHelper.navigateVideo(
                        requireContext(),
                        response.relatedStreams?.first()?.url?.toID(),
                        playlistId
                    )
                }

                binding.share.setOnClickListener {
                    ShareDialog(
                        playlistId!!,
                        ShareObjectType.PLAYLIST,
                        ShareData(currentPlaylist = response.name)
                    ).show(childFragmentManager, null)
                }

                playlistAdapter = PlaylistAdapter(
                    response.relatedStreams.orEmpty().toMutableList(),
                    playlistId!!,
                    isOwner,
                    childFragmentManager
                )

                // listen for playlist items to become deleted
                playlistAdapter!!.registerAdapterDataObserver(object :
                        RecyclerView.AdapterDataObserver() {
                        override fun onChanged() {
                            binding.playlistInfo.text =
                                binding.playlistInfo.text.split(" • ").first() + " • " + getString(
                                R.string.videoCount,
                                playlistAdapter!!.itemCount.toString()
                            )
                        }
                    })

                binding.playlistRecView.adapter = playlistAdapter
                binding.playlistScrollview.viewTreeObserver
                    .addOnScrollChangedListener {
                        if (binding.playlistScrollview.getChildAt(0).bottom
                            == (binding.playlistScrollview.height + binding.playlistScrollview.scrollY)
                        ) {
                            // scroll view is at bottom
                            if (nextPage != null && !isLoading) {
                                isLoading = true
                                fetchNextPage()
                            }
                        }
                    }

                /**
                 * listener for swiping to the left or right
                 */
                if (isOwner) {
                    val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT
                    ) {
                        override fun onMove(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                        ): Boolean {
                            return false
                        }

                        override fun onSwiped(
                            viewHolder: RecyclerView.ViewHolder,
                            direction: Int
                        ) {
                            val position = viewHolder.absoluteAdapterPosition
                            playlistAdapter!!.removeFromPlaylist(requireContext(), position)
                        }
                    }

                    val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
                    itemTouchHelper.attachToRecyclerView(binding.playlistRecView)
                }
            }
        }
    }

    private fun fetchNextPage() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    // load locally stored playlists with the auth api
                    if (isOwner) {
                        RetrofitInstance.authApi.getPlaylistNextPage(
                            playlistId!!,
                            nextPage!!
                        )
                    } else {
                        RetrofitInstance.api.getPlaylistNextPage(
                            playlistId!!,
                            nextPage!!
                        )
                    }
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                playlistAdapter?.updateItems(response.relatedStreams!!)
                isLoading = false
            }
        }
        run()
    }
}
