package com.github.libretube.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PipedMediaServiceRepositoryTest {
    @Test
    fun getTrendingCategoriesReturnsLiveCategory() {
        val repository = PipedMediaServiceRepository()

        assertEquals(listOf(TrendingCategory.LIVE), repository.getTrendingCategories())
    }
}
