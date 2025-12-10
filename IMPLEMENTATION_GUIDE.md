# Order Queue Implementation Guide

## Project Overview

This Android application demonstrates a real-time producer-consumer queue system using Kotlin Coroutines. The UI visualizes queue growth and shrinkage in real-time with color-coded progress indicators.

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│         UI Layer (MainActivity)          │
│  - Displays queue state                  │
│  - Handles user interactions             │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│    ViewModel Layer (QueueViewModel)      │
│  - Manages UI state using StateFlow      │
│  - Coordinates with data layer           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│    Data Layer (OrderQueueManager)        │
│  - Producer/Consumer logic               │
│  - Queue state management via Flow       │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Data Layer (`OrderQueue.kt`)

#### Order
```kotlin
data class Order(
    val id: String,
    val timestamp: Long
)
```
Represents a single order in the queue with unique identifier and creation time.

#### QueueState
```kotlin
data class QueueState(
    val queueSize: Int,
    val maxCapacity: Int,
    val isProcessing: Boolean
)
```
Represents the current state of the queue system with computed properties for fill percentage and progress color.

#### OrderQueueManager
The core of the producer-consumer pattern:

**Producer:**
- Emits one order every 250ms via a Kotlin Channel
- Continues even when consumer is paused
- Uses coroutines for non-blocking I/O

**Consumer:**
- Processes orders from the queue with random delay (100-250ms)
- Can be paused independently from producer
- When paused, queue grows; when resumed, queue shrinks

**Queue State Flow:**
- Continuously monitors internal queue
- Emits QueueState updates when:
  - New orders arrive from producer
  - Orders are consumed
  - Consumer is paused/resumed

### 2. ViewModel Layer (`QueueViewModel.kt`)

**Responsibilities:**
- Manages two StateFlows for UI consumption:
  - `queueState`: Current queue metrics
  - `isStarted`: Button state (Start/Pause)
- Launches coroutines in viewModelScope for proper lifecycle management
- Cleans up resources in onCleared()

**Key Functions:**
- `start()`: Begins producer or resumes consumer
- `pause()`: Pauses consumer (producer continues)

### 3. UI Layer (`MainActivity.kt`)

**Responsibilities:**
- Displays real-time queue metrics:
  - Queue counter: "Queue: X/25"
  - Progress percentage: "0-100%"
  - Progress bar with dynamic color
- Handles user interactions (Start/Pause button)
- Animates overflow state

**Color Transitions:**
- Green (0-33%): Queue stable
- Yellow (34-66%): Queue growing
- Red (67-99%): Queue critical
- Dark Red (100%): Queue overflow with animation

## Flow Diagrams

### Start/Pause Lifecycle

```
Initial State
    ↓
┌─────────────┐
│   STOPPED   │
└─────────────┘
    ↓ (tap Start)
┌─────────────────────────────────────┐
│        RUNNING - PROCESSING         │
│  Producer: 250ms/order              │
│  Consumer: 100-250ms/order          │
│  Queue: Stable or shrinking         │
└─────────────────────────────────────┘
    ↓ (tap Pause)
┌─────────────────────────────────────┐
│      RUNNING - CONSUMER PAUSED      │
│  Producer: Still 250ms/order        │
│  Consumer: Stopped                  │
│  Queue: Growing                     │
└─────────────────────────────────────┘
    ↓ (tap Start again)
Back to RUNNING - PROCESSING
```

### Queue Processing Timeline Example

```
Time: 0ms     - Start pressed
              - Producer begins: Order-1

Time: 250ms   - Producer: Order-1 (added to queue)
              - Consumer: Processing with random delay
              
Time: 350ms   - Consumer: Order-1 consumed (finished)
              
Time: 500ms   - Producer: Order-2 (added to queue)
              - Consumer: Processing
              
...

Time: T       - Pause pressed
              - Producer continues
              - Consumer stops
              
Time: T+250ms - Producer: Order-N (queue size increases)
Time: T+500ms - Producer: Order-N+1 (queue size increases more)

Time: T+XXX   - Start pressed again
              - Consumer resumes
              - Queue starts shrinking as consumer catches up
```

## Coroutine Design

### Producer Coroutine
```
startProducer() {
    while (isRunning) {
        val order = Order()
        orderChannel.send(order)  // Suspends if channel is full
        delay(250)                 // Suspends for 250ms
    }
}
```

### Consumer Flow
```
getQueueStateFlow() {
    while (true) {
        // Check for new orders
        val newOrder = orderChannel.tryReceive()
        
        // Add to queue if space available
        if (newOrder != null && queue.size < maxCapacity) {
            queue.add(newOrder)
            emit(newState)
        }
        
        // Consumer logic
        if (isRunning && !isPaused && queue.isNotEmpty()) {
            delay(Random(100, 250))  // Variable processing time
            queue.remove(0)
            emit(newState)
        }
    }
}
```

**Key Coroutine Patterns:**
- `Channel` for producer-consumer communication
- `Flow` for reactive state updates
- `StateFlow` for ViewModel state management
- `viewModelScope` for automatic cleanup
- `delay()` for suspension instead of Thread.sleep()

## State Management

### Queue State Properties
- **fillPercentage**: Calculated as (queueSize * 100) / maxCapacity
- **progressColor**: Determined by fillPercentage threshold
- **isOverflowing**: Boolean flag when queueSize >= maxCapacity

### ViewModel State
- **queueState**: Emits current QueueState
- **isStarted**: Reflects button state for two-way binding

## Error Handling

- Channel closure handled with try-catch
- Flow collection in lifecycleScope ensures proper cleanup
- ViewModel.onCleared() stops producer and clears queue

## Performance Considerations

1. **Efficient Updates**: Only emit when state actually changes
2. **Non-blocking**: Coroutines prevent UI thread blocking
3. **Memory**: UNLIMITED channel prevents queue overflow
4. **CPU**: 10ms delay in idle state prevents busy-waiting

## Testing Scenarios

1. **Normal Operation**
   - Tap Start
   - Observe queue processing
   - Queue remains near 0-1

2. **Pause Accumulation**
   - Start processing
   - Tap Pause
   - Wait 5 seconds
   - Observe queue growing to near capacity
   - Progress bar: Green → Yellow → Red → Overflow

3. **Resume Processing**
   - Tap Start while paused
   - Observe queue shrinking
   - Progress bar: Overflow → Red → Yellow → Green

4. **Multiple Cycles**
   - Start/Pause/Start cycles
   - Verify queue correctly grows and shrinks each time

## Dependencies

**Core Android:**
- androidx.core:core-ktx
- androidx.appcompat:appcompat
- androidx.constraintlayout:constraintlayout

**Async:**
- kotlinx-coroutines-core
- kotlinx-coroutines-android

**Lifecycle:**
- androidx.lifecycle:lifecycle-viewmodel-ktx
- androidx.lifecycle:lifecycle-runtime-ktx

All dependencies are from Jetpack (Android's official libraries) - no third-party libraries required.

## Build & Run

```bash
# Build
./gradlew build

# Run on emulator/device
./gradlew installDebug
adb shell am start -n com.orderqueue/.ui.MainActivity

# Or use Android Studio's Run button
```

## Key Learnings

1. **Coroutines**: Simplify async operations without callbacks
2. **Channels**: Elegant producer-consumer communication
3. **StateFlow**: Reactive state management for UI
4. **Flow**: Cold streams vs Hot StateFlows
5. **ViewModel**: Lifecycle-aware state preservation
