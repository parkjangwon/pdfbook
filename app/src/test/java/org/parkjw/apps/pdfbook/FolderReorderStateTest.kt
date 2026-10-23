package org.parkjw.apps.pdfbook

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderReorderStateTest {
    private fun state(vararg order: String) =
        FolderReorderState(LazyGridState()).also { it.syncBase(order.toList()) }

    @Test
    fun canMoveIsFalseAtEdgesAndForUnknownUri() {
        val s = state("a", "b", "c")
        assertFalse(s.canMove("a", -1))
        assertTrue(s.canMove("a", 1))
        assertFalse(s.canMove("c", 1))
        assertFalse(s.canMove("x", 1))
    }

    @Test
    fun moveByReturnsNewOrderAndChainsOnPending() {
        val s = state("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), s.moveBy("a", 1))
        assertEquals(listOf("b", "a", "c"), s.pendingOrder)
        assertEquals(listOf("b", "c", "a"), s.moveBy("a", 1))
        assertNull(s.moveBy("a", 1))
    }

    @Test
    fun syncBaseWithChangedOrderClearsPendingWhenNotDragging() {
        val s = state("a", "b")
        s.moveBy("a", 1)
        s.syncBase(listOf("b", "a"))
        assertNull(s.pendingOrder)
        assertEquals(listOf("b", "a"), s.baseOrder)
    }

    @Test
    fun endWithoutDragAndCancelAreNoOps() {
        val s = state("a", "b")
        assertNull(s.end())
        s.moveBy("a", 1)
        s.cancel()
        assertNull(s.pendingOrder)
        assertNull(s.draggingUri)
    }

    @Test
    fun startWithoutLaidOutItemDoesNotBeginDrag() {
        val s = state("a", "b")
        s.start("a")
        s.drag(Offset(0f, 10f))
        assertNull(s.draggingUri)
    }
}
