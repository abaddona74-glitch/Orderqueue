# Order Queue - Producer-Consumer Queue System

A real-time queue visualization system for Android demonstrating producer-consumer patterns using Kotlin Coroutines.

## Features

- **Real-time Queue Visualization**: Visual progress bar showing queue fill percentage
- **Color-coded Progress Indicator**:
  - Green (0-33%): Queue stable
  - Yellow (34-66%): Queue growing
  - Red (67-99%): Queue critical
  - Dark Red (100%): Queue overflow
- **Start/Pause Control**: Toggle between processing and paused states
- **Producer-Consumer Pattern**: 
  - Producer sends orders every 250ms
  - Consumer processes orders with random delays (100-250ms)
- **Overflow Handling**: Animated feedback when queue reaches capacity

## Architecture

### Data Layer (`OrderQueue.kt`)
- `Order`: Data class representing individual orders
- `QueueState`: Current queue status and metrics
- `OrderQueueManager`: Manages producer/consumer operations using Kotlin Channels

### ViewModel Layer (`QueueViewModel.kt`)
- `QueueViewModel`: Manages queue state and UI updates using StateFlow

### UI Layer (`MainActivity.kt`)
- `MainActivity`: Displays queue metrics and controls

## Key Implementation Details

### Producer-Consumer Logic
```
Producer -> Channel -> Internal Queue -> Consumer (with pause support)
```

### Queue State Flow
The queue state is exposed as a `Flow<QueueState>` which emits updates whenever:
- New orders are added to the queue
- Orders are consumed
- Consumer is paused/resumed

### Color Transitions
Progress bar color automatically transitions based on fill percentage:
- Smooth visual feedback for queue status
- Overflow state triggers animation

## Requirements Met

✅ Initial screen with title, description, and Start button
✅ Producer emits orders every 250ms
✅ Consumer processes with random 100-250ms delays
✅ Pause halts consumer, producer continues
✅ Resume restarts consumer processing
✅ Queue counter display (X/25 format)
✅ Color-coded progress bar
✅ Overflow animation and state
✅ Uses Kotlin Coroutines for all async operations
✅ No third-party libraries beyond Jetpack
✅ Proper resource management in ViewModel

## Testing the Application

1. Launch the app
2. Tap "Start" - observe queue processing
3. Tap "Pause" - queue grows as producer continues
4. Watch progress bar turn yellow then red
5. Tap "Start" again - consumer resumes, queue shrinks

## Build & Run

```bash
./gradlew build
./gradlew installDebug
```

Or use Android Studio to build and run directly.
