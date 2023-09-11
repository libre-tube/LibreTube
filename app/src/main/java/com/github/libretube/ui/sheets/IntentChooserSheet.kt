package com.github.libretube.ui.sheets

import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.ui.adapters.IntentChooserAdapter

class IntentChooserSheet : BaseBottomSheet() {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        url = arguments?.getString(IntentData.url)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding
        val packages = IntentHelper.getResolveInfo(requireContext(), url)
        binding.optionsRecycler.layoutManager = GridLayoutManager(context, 3)
        binding.optionsRecycler.adapter = IntentChooserAdapter(packages, url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
