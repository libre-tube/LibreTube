package com.github.libretube.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.adapters.DownloadsAdapter
import com.github.libretube.databinding.FragmentDownloadsBinding
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.util.DownloadHelper

class DownloadsFragment : BaseFragment() {
    private lateinit var binding: FragmentDownloadsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val files = DownloadHelper.getDownloadedFiles(requireContext())

        binding.downloads.layoutManager = LinearLayoutManager(context)
        binding.downloads.adapter = DownloadsAdapter(files)
    }
}
