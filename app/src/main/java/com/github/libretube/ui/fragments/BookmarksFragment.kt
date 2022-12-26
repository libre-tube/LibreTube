package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.FragmentBookmarksBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.base.BaseFragment

class BookmarksFragment : BaseFragment() {
    private lateinit var binding: FragmentBookmarksBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarksBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookmarks = awaitQuery {
            Database.playlistBookmarkDao().getAll()
        }

        if (bookmarks.isEmpty()) return

        binding.bookmarksRV.layoutManager = LinearLayoutManager(context)
        binding.bookmarksRV.adapter = PlaylistBookmarkAdapter(bookmarks)

        binding.bookmarksRV.visibility = View.VISIBLE
        binding.emptyBookmarks.visibility = View.GONE
    }
}
