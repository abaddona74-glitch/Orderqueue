package com.orderqueue.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.view.animation.ScaleAnimation
import android.view.animation.Animation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.orderqueue.R
import com.orderqueue.data.ProgressColor
import com.orderqueue.data.QueueState
import com.orderqueue.viewmodel.QueueViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: QueueViewModel by viewModels()

    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var queueCounterTextView: TextView
    private lateinit var percentageTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startPauseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupUI()
        observeQueueState()
    }

    private fun initializeViews() {
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        queueCounterTextView = findViewById(R.id.queueCounterTextView)
        percentageTextView = findViewById(R.id.percentageTextView)
        progressBar = findViewById(R.id.progressBar)
        startPauseButton = findViewById(R.id.startPauseButton)
    }

    private fun setupUI() {
        titleTextView.text = "Order Queue Outpost"
        descriptionTextView.text = "Press Play to start processing orders"
        
        startPauseButton.text = "Start"
        startPauseButton.setOnClickListener {
            if (viewModel.isStarted.value) {
                viewModel.pause()
            } else {
                viewModel.start()
            }
        }

        // Initialize progress bar
        progressBar.progress = 0
        updateProgressBarColor(ProgressColor.GREEN)
    }

    private fun observeQueueState() {
        lifecycleScope.launch {
            viewModel.queueState.collect { state ->
                updateQueueDisplay(state)
            }
        }

        lifecycleScope.launch {
            viewModel.isStarted.collect { isStarted ->
                startPauseButton.text = if (isStarted) "Pause" else "Start"
            }
        }
    }

    private fun updateQueueDisplay(state: QueueState) {
        // Update counter text
        queueCounterTextView.text = "Queue: ${state.queueSize}/${state.maxCapacity}"
        percentageTextView.text = "${state.fillPercentage}%"

        // Update progress bar
        val newProgress = state.fillPercentage
        if (progressBar.progress != newProgress) {
            progressBar.setProgress(newProgress, true)
            updateProgressBarColor(state.progressColor)

            // Show overflow animation if queue is full
            if (state.isOverflowing) {
                showOverflowAnimation()
            }
        }
    }

    private fun updateProgressBarColor(color: ProgressColor) {
        val colorRes = when (color) {
            ProgressColor.GREEN -> android.R.color.holo_green_light
            ProgressColor.YELLOW -> android.R.color.holo_orange_light
            ProgressColor.RED -> android.R.color.holo_red_light
            ProgressColor.OVERFLOW -> android.R.color.holo_red_dark
        }
        
        val colorValue = ContextCompat.getColor(this, colorRes)
        progressBar.progressDrawable?.setTint(colorValue)
    }

    private fun showOverflowAnimation() {
        val scaleAnimation = ScaleAnimation(
            1f, 1.1f, 1f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 300
        scaleAnimation.repeatMode = Animation.REVERSE
        scaleAnimation.repeatCount = 1
        queueCounterTextView.startAnimation(scaleAnimation)
    }
}
