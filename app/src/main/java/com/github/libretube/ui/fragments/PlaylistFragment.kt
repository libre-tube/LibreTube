package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentPlaylistBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.PlaylistAdapter
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    // general playlist information
    private var playlistId: String? = null
    private var playlistName: String? = null
    private var playlistType: PlaylistType = PlaylistType.PUBLIC

    // runtime variables
    private var playlistFeed = mutableListOf<StreamItem>()
    private var playlistAdapter: PlaylistAdapter? = null
    private var nextPage: String? = null
    private var isLoading = true
    private var isBookmarked = false

    // view models
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString(IntentData.playlistId)
            playlistType = it.serializable(IntentData.playlistType) ?: PlaylistType.PUBLIC
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = playlistId!!.toID()
        binding.playlistRecView.layoutManager = LinearLayoutManager(context)

        binding.playlistProgress.visibility = View.VISIBLE

        isBookmarked = runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.playlistBookmarkDao().includes(playlistId!!)
        }
        updateBookmarkRes()

        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            binding.playlistRecView.updatePadding(
                bottom = if (it) (64).dpToPx().toInt() else 0
            )
        }

        fetchPlaylist()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateBookmarkRes() {
        binding.bookmark.setIconResource(
            if (isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outlined
        )
    }

    private fun fetchPlaylist() {
        binding.playlistScrollview.visibility = View.GONE
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        PlaylistsHelper.getPlaylist(playlistId!!)
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    return@repeatOnLifecycle
                }
                playlistFeed = response.relatedStreams.toMutableList()
                binding.playlistScrollview.visibility = View.VISIBLE
                nextPage = response.nextpage
                playlistName = response.name
                isLoading = false
                ImageHelper.loadImage(response.thumbnailUrl, binding.thumbnail)
                binding.playlistProgress.visibility = View.GONE
                binding.playlistName.text = response.name

                binding.playlistName.setOnClickListener {
                    binding.playlistName.maxLines =
                        if (binding.playlistName.maxLines == 2) Int.MAX_VALUE else 2
                }

                binding.playlistInfo.text = getChannelAndVideoString(response, response.videos)

                // show playlist options
                binding.optionsMenu.setOnClickListener {
                    PlaylistOptionsBottomSheet(
                        playlistId = playlistId.orEmpty(),
                        playlistName = playlistName.orEmpty(),
                        playlistType = playlistType,
                        onDelete = {
                            findNavController().popBackStack()
                        },
                        onRename = {
                            binding.playlistName.text = it
                            playlistName = it
                        }
                    ).show(
                        childFragmentManager,
                        PlaylistOptionsBottomSheet::class.java.name
                    )
                }

                binding.playAll.setOnClickListener {
                    if (playlistFeed.isEmpty()) return@setOnClickListener
                    NavigationHelper.navigateVideo(
                        requireContext(),
                        response.relatedStreams.first().url?.toID(),
                        playlistId
                    )
                }

                if (playlistType == PlaylistType.PUBLIC) {
                    binding.bookmark.setOnClickListener {
                        isBookmarked = !isBookmarked
                        updateBookmarkRes()
                        lifecycleScope.launch(Dispatchers.IO) {
                            if (!isBookmarked) {
                                DatabaseHolder.Database.playlistBookmarkDao()
                                    .deleteById(playlistId!!)
                            } else {
                                DatabaseHolder.Database.playlistBookmarkDao()
                                    .insert(response.toPlaylistBookmark(playlistId!!))
                            }
                        }
                    }
                } else {
                    // private playlist, means shuffle is possible because all videos are received at once
                    binding.bookmark.setIconResource(R.drawable.ic_shuffle)
                    binding.bookmark.text = getString(R.string.shuffle)
                    binding.bookmark.setOnClickListener {
                        if (playlistFeed.isEmpty()) return@setOnClickListener
                        val queue = playlistFeed.shuffled()
                        PlayingQueue.resetToDefaults()
                        PlayingQueue.add(*queue.toTypedArray())
                        NavigationHelper.navigateVideo(
                            requireContext(),
                            queue.first().url?.toID(),
                            playlistId = playlistId,
                            keepQueue = true
                        )
                    }
                }

                playlistAdapter = PlaylistAdapter(playlistFeed, playlistId!!, playlistType)

                // listen for playlist items to become deleted
                playlistAdapter!!.registerAdapterDataObserver(object :
                    RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        if (positionStart == 0) {
                            ImageHelper.loadImage(
                                playlistFeed.firstOrNull()?.thumbnail ?: "",
                                binding.thumbnail
                            )
                        }

                        binding.playlistInfo.text =
                            getChannelAndVideoString(response, playlistFeed.size)
                    }
                })

            binding.playlistRecView.adapter = playlistAdapter
            binding.playlistScrollview.viewTreeObserver.addOnScrollChangedListener {
                if (_binding?.playlistScrollview?.canScrollVertically(1) == false &&
                    !isLoading
                ) {
                    // append more playlists to the recycler view
                    if (playlistType != PlaylistType.PUBLIC) {
                        isLoading = true
                        playlistAdapter?.showMoreItems()
                        isLoading = false
                    } else {
                        fetchNextPage()
                    }
                }
            }

                // listener for swiping to the left or right
                if (playlistType != PlaylistType.PUBLIC) {
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

                withContext(Dispatchers.IO) {
                    // update the playlist thumbnail if bookmarked
                    val playlistBookmark = DatabaseHolder.Database.playlistBookmarkDao().getAll()
                        .firstOrNull { it.playlistId == playlistId }
                    playlistBookmark?.let {
                        if (it.thumbnailUrl != response.thumbnailUrl) {
                            it.thumbnailUrl = response.thumbnailUrl
                            DatabaseHolder.Database.playlistBookmarkDao().update(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun getChannelAndVideoString(playlist: Playlist, count: Int): String {
        return playlist.uploader?.let {
            getString(R.string.uploaderAndVideoCount, it, count)
        } ?: getString(R.string.videoCount, count)
    }

    private fun fetchNextPage() {
        if (nextPage == null || isLoading) return
        isLoading = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        // load locally stored playlists with the auth api
                        if (playlistType == PlaylistType.PRIVATE) {
                            RetrofitInstance.authApi.getPlaylistNextPage(playlistId!!, nextPage!!)
                        } else {
                            RetrofitInstance.api.getPlaylistNextPage(playlistId!!, nextPage!!)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    return@repeatOnLifecycle
                }

                nextPage = response.nextpage
                playlistAdapter?.updateItems(response.relatedStreams)
                isLoading = false
            }
        }
    }
}
