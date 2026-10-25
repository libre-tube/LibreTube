package com.github.libretube

import com.github.libretube.extensions.move
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveTest {
    @Test
    fun testMoveAdjacentForward() {
        val list = mutableListOf("A", "B", "C")
        list.move(0, 1)
        assertEquals(listOf("B", "A", "C"), list)
    }

    @Test
    fun testMoveToEnd() {
        val list = mutableListOf("A", "B", "C")
        list.move(0, 2)
        assertEquals(listOf("B", "C", "A"), list)
    }

    @Test
    fun testMoveBackward() {
        val list = mutableListOf("A", "B", "C")
        list.move(2, 0)
        assertEquals(listOf("C", "A", "B"), list)
    }

    @Test
    fun testMoveOutOfBounds() {
        val list = mutableListOf("A", "B")
        list.move(-1, 5)
        assertEquals(listOf("A", "B"), list)
    }
}
