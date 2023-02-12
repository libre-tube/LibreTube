package com.github.libretube.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Fragment.launchWhenCreatedIO(block: suspend () -> Unit) {
    lifecycleScope.launchWhenCreated {
        withContext(Dispatchers.IO) {
            block.invoke()
        }
    }
}
