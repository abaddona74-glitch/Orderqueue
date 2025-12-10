package com.orderqueue.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Represents a single order in the queue
 */
data class Order(
    val id: String = "Order-${System.currentTimeMillis()}-${Random.nextInt(1000)}",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the state of the queue system
 */
data class QueueState(
    val queueSize: Int = 0,
    val maxCapacity: Int = 25,
    val isProcessing: Boolean = false
) {
    val fillPercentage: Int
        get() = if (maxCapacity > 0) (queueSize * 100) / maxCapacity else 0

    val progressColor: ProgressColor
        get() = when {
            fillPercentage <= 33 -> ProgressColor.GREEN
            fillPercentage <= 66 -> ProgressColor.YELLOW
            fillPercentage < 100 -> ProgressColor.RED
            else -> ProgressColor.OVERFLOW
        }

    val isOverflowing: Boolean
        get() = queueSize >= maxCapacity
}

enum class ProgressColor {
    GREEN, YELLOW, RED, OVERFLOW
}

/**
 * Manages the order queue with producer and consumer operations using coroutines
 */
class OrderQueueManager(
    private val maxCapacity: Int = 25,
    private val producerDelayMs: Long = 250,
    private val consumerMinDelayMs: Long = 100,
    private val consumerMaxDelayMs: Long = 250
) {
    private val orderChannel = Channel<Order>(Channel.UNLIMITED)
    private val internalQueue = mutableListOf<Order>()
    
    private var isRunning = false
    private var isConsumerPaused = false

    /**
     * Represents the current queue state as a Flow.
     * This flow continuously monitors the queue state and emits updates
     * when orders are added or removed.
     */
    fun getQueueStateFlow(): Flow<QueueState> = flow {
        var currentState = QueueState(0, maxCapacity, false)
        emit(currentState)

        while (true) {
            // Try to receive a new order from the producer
            val newOrder = try {
                orderChannel.tryReceive().getOrNull()
            } catch (e: Exception) {
                null
            }

            // Add the order to the internal queue if there's space
            if (newOrder != null && internalQueue.size < maxCapacity) {
                internalQueue.add(newOrder)
                currentState = QueueState(
                    internalQueue.size,
                    maxCapacity,
                    isRunning && !isConsumerPaused
                )
                emit(currentState)
            }

            // Consumer processes orders if not paused and queue is not empty
            if (isRunning && !isConsumerPaused && internalQueue.isNotEmpty()) {
                val delay = Random.nextLong(consumerMinDelayMs, consumerMaxDelayMs + 1)
                delay(delay)
                internalQueue.removeAt(0)
                currentState = QueueState(
                    internalQueue.size,
                    maxCapacity,
                    isRunning && !isConsumerPaused
                )
                emit(currentState)
            } else {
                // Small delay to prevent busy waiting
                delay(10)
            }
        }
    }

    /**
     * Starts the producer emitting orders continuously.
     * Each order is emitted with a fixed 250ms delay.
     */
    suspend fun startProducer() {
        isRunning = true
        isConsumerPaused = false
        
        while (isRunning) {
            try {
                val order = Order()
                orderChannel.send(order)
                delay(producerDelayMs)
            } catch (e: Exception) {
                isRunning = false
                break
            }
        }
    }

    /**
     * Pauses the consumer while allowing the producer to continue.
     * This causes the queue to grow as orders keep coming in.
     */
    fun pauseConsumer() {
        isConsumerPaused = true
    }

    /**
     * Resumes the consumer to process queued orders.
     */
    fun resumeConsumer() {
        isConsumerPaused = false
    }

    /**
     * Stops all operations and cleans up resources.
     */
    fun stop() {
        isRunning = false
        isConsumerPaused = false
        try {
            orderChannel.close()
        } catch (e: Exception) {
            // Channel already closed
        }
        internalQueue.clear()
    }
}
