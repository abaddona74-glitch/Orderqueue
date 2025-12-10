package com.orderqueue.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OrderQueueManager
 * Tests producer-consumer behavior, state transitions, and pause/resume functionality
 */
class OrderQueueManagerTest {

    @Test
    fun testInitialQueueState() = runTest {
        val manager = OrderQueueManager()
        val stateFlow = manager.getQueueStateFlow()
        
        var receivedState: QueueState? = null
        launch {
            stateFlow.collect { state ->
                receivedState = state
            }
        }
        
        delay(100)
        assertNotNull(receivedState)
        assertEquals(0, receivedState?.queueSize)
        assertEquals(25, receivedState?.maxCapacity)
        assertFalse(receivedState?.isProcessing ?: true)
    }

    @Test
    fun testQueueStateProperties() {
        val state0 = QueueState(queueSize = 0, maxCapacity = 25, isProcessing = false)
        assertEquals(0, state0.fillPercentage)
        assertEquals(ProgressColor.GREEN, state0.progressColor)
        assertFalse(state0.isOverflowing)

        val state8 = QueueState(queueSize = 8, maxCapacity = 25, isProcessing = true)
        assertEquals(32, state8.fillPercentage) // (8 * 100) / 25 = 32%
        assertEquals(ProgressColor.YELLOW, state8.progressColor)
        assertFalse(state8.isOverflowing)

        val state17 = QueueState(queueSize = 17, maxCapacity = 25, isProcessing = true)
        assertEquals(68, state17.fillPercentage)
        assertEquals(ProgressColor.RED, state17.progressColor)
        assertFalse(state17.isOverflowing)

        val state25 = QueueState(queueSize = 25, maxCapacity = 25, isProcessing = false)
        assertEquals(100, state25.fillPercentage)
        assertEquals(ProgressColor.OVERFLOW, state25.progressColor)
        assertTrue(state25.isOverflowing)
    }

    @Test
    fun testOrderCreation() {
        val order1 = Order()
        val order2 = Order()
        
        assertNotNull(order1.id)
        assertNotNull(order2.id)
        assertNotEquals(order1.id, order2.id)
        
        assertTrue(order1.timestamp > 0)
        assertTrue(order2.timestamp > 0)
    }

    @Test
    fun testProgressColorTransitions() {
        val colors = listOf(
            Triple(0, ProgressColor.GREEN, "Empty queue"),
            Triple(33, ProgressColor.GREEN, "At 33% threshold"),
            Triple(34, ProgressColor.YELLOW, "Just above 33%"),
            Triple(66, ProgressColor.YELLOW, "At 66% threshold"),
            Triple(67, ProgressColor.RED, "Just above 66%"),
            Triple(99, ProgressColor.RED, "Near full"),
            Triple(100, ProgressColor.OVERFLOW, "Full")
        )
        
        colors.forEach { (percentage, expectedColor, description) ->
            val queueSize = (percentage * 25) / 100
            val state = QueueState(queueSize = queueSize, maxCapacity = 25)
            assertEquals("$description (${state.fillPercentage}%)", expectedColor, state.progressColor)
        }
    }
}
