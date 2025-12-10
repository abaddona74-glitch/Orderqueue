package com.orderqueue.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderqueue.data.OrderQueueManager
import com.orderqueue.data.QueueState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the Order Queue UI state
 */
class QueueViewModel : ViewModel() {
    private val queueManager = OrderQueueManager()
    
    private val _queueState = MutableStateFlow<QueueState>(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    init {
        // Collect queue state updates
        viewModelScope.launch {
            queueManager.getQueueStateFlow().collect { state ->
                _queueState.value = state
            }
        }
    }

    /**
     * Start or resume the queue processing
     */
    fun start() {
        if (!_isStarted.value) {
            _isStarted.value = true
            viewModelScope.launch {
                queueManager.startProducer()
            }
        } else {
            // Resume consumer if it was paused
            queueManager.resumeConsumer()
        }
    }

    /**
     * Pause the queue consumer (producer continues)
     */
    fun pause() {
        queueManager.pauseConsumer()
        _isStarted.value = false
    }

    override fun onCleared() {
        super.onCleared()
        queueManager.stop()
    }
}
