# Testing Guide for Order Queue

## Unit Tests

Run unit tests with:
```bash
./gradlew test
```

### Test Coverage

#### OrderQueueManagerTest
- **testInitialQueueState**: Verifies queue starts empty
- **testQueueStateProperties**: Tests state properties (fillPercentage, progressColor, isOverflowing)
- **testOrderCreation**: Ensures orders have unique IDs
- **testProgressColorTransitions**: Validates color transitions at all thresholds

## Integration Tests

### Manual Testing Checklist

#### ✅ Test 1: Initial State
- [ ] App launches with "Order Queue Outpost" title
- [ ] "Press Play to start processing orders" description visible
- [ ] Queue counter shows "Queue: 0/25"
- [ ] Progress bar is at 0%
- [ ] Progress bar is green
- [ ] Button shows "Start"

#### ✅ Test 2: Start Processing
- [ ] Tap "Start" button
- [ ] Button changes to "Pause"
- [ ] Queue counter begins updating
- [ ] Progress bar remains green (< 34%)
- [ ] Queue size stabilizes between 0-2 items

#### ✅ Test 3: Pause & Accumulation
- [ ] After processing for 1-2 seconds, tap "Pause"
- [ ] Button changes to "Start"
- [ ] Queue counter increments every 250ms
- [ ] Progress bar increases
- [ ] Observe color progression:
  - [ ] Green (0-33%) for first 2 seconds
  - [ ] Yellow (34-66%) for next 2 seconds
  - [ ] Red (67-99%) for next 2 seconds

#### ✅ Test 4: Overflow State
- [ ] Continue pausing for 5+ seconds total
- [ ] Queue reaches 25/25 (overflow)
- [ ] Progress bar reaches 100% and turns dark red
- [ ] Counter animation pulses (scale animation)

#### ✅ Test 5: Resume Processing
- [ ] Tap "Start" while in overflow state
- [ ] Queue counter decrements
- [ ] Progress bar decreases
- [ ] Observe color regression:
  - [ ] Dark Red → Red
  - [ ] Red → Yellow  
  - [ ] Yellow → Green
- [ ] Queue eventually returns to stable 0-2 items

#### ✅ Test 6: Multiple Cycles
- [ ] Repeat Start → Wait → Pause → Wait → Start sequence 3 times
- [ ] Verify consistent behavior each cycle
- [ ] Queue grows same amount during pause each time
- [ ] Queue shrinks consistently when resumed

#### ✅ Test 7: Rapid Toggling
- [ ] Tap Start, quickly tap Pause (< 1 second)
- [ ] Tap Start again
- [ ] Verify queue size doesn't exceed 25
- [ ] Verify no crashes

#### ✅ Test 8: Long Running Session
- [ ] Start processing
- [ ] Let it run for 1 minute
- [ ] Verify UI remains responsive
- [ ] Verify queue stays stable
- [ ] Check for memory leaks in Android Profiler

## Performance Testing

### Memory Usage
1. Open Android Profiler (Profile → Memory)
2. Record memory during:
   - Start processing
   - Pause & accumulate
   - Resume processing
3. Expected: No significant growth or memory leaks

### CPU Usage
1. Open Android Profiler (Profile → CPU)
2. Record CPU during:
   - Processing (consumer working)
   - Paused (only producer, minimal consumer)
3. Expected: Low CPU usage, no spike during updates

## UI/UX Testing

### Orientation Changes
1. Start processing
2. Rotate device (portrait ↔ landscape)
3. Verify:
   - [ ] UI redraw is smooth
   - [ ] Queue state preserved
   - [ ] Processing continues uninterrupted

### Visual Verification
1. Verify progress bar:
   - [ ] Green color is bright and visible
   - [ ] Yellow color is clearly different from green
   - [ ] Red color indicates urgency
   - [ ] Overflow red is darker
2. Verify text:
   - [ ] Counter is easy to read
   - [ ] Percentage is accurate
   - [ ] Button text is clear

### Animation Testing
1. Trigger overflow state
2. Verify counter pulses/scales
3. Verify animation is smooth (not jerky)
4. Verify animation duration is ~300ms

## Edge Cases

### ✅ Empty to Overflow
- Test queue growing from 0 to 25 items
- Expected: Smooth color transitions

### ✅ Overflow to Empty
- Test queue shrinking from 25 to 0 items
- Expected: Smooth color transitions in reverse

### ✅ Pause Immediately
- Tap Start, immediately tap Pause
- Expected: Queue size should be 0 or 1 (not negative)

### ✅ Rapid Pause/Resume
- Alternate pause/resume every 100ms
- Expected: No crashes, queue handles transitions

### ✅ Background → Foreground
- Start processing
- Send app to background (Home button)
- Bring app to foreground
- Expected: Processing resumes, UI updates correctly

## Regression Testing

Run before each release:

1. **Basic Workflow**
   - Start → Pause → Resume → Stop (in-app lifecycle)
   
2. **State Preservation**
   - Start processing
   - Rotate device
   - Verify state intact
   
3. **Performance**
   - Run for 2 minutes
   - Monitor memory in Profiler
   - Check CPU usage

4. **Visual Verification**
   - Verify all colors display correctly
   - Verify animations are smooth
   - Verify text is readable

## Debugging Tips

### Queue not growing when paused
- Check if consumer is actually pausing
- Verify delay in consumer is being applied
- Check logcat for exceptions

### Queue not shrinking when resumed
- Check if producer is still sending orders
- Verify consumer delay range (100-250ms)
- Check if pause flag is being reset

### Progress bar color not changing
- Verify fillPercentage calculation
- Check if progressColor logic matches thresholds
- Verify color resources are defined

### UI not updating
- Check if StateFlow collection is active
- Verify lifecycleScope is not canceled
- Check for exceptions in flow collection

### Memory leak
- Check if ViewModel is being cleared
- Verify channels are properly closed
- Check for listener leaks in observers

## Test Results Template

```
Date: ___________
Tester: _________
Device: ________ (API level: ___)
Android Version: _________

Test Results:
- Initial State: ✅ / ❌
- Start Processing: ✅ / ❌
- Pause & Accumulation: ✅ / ❌
- Overflow State: ✅ / ❌
- Resume Processing: ✅ / ❌
- Multiple Cycles: ✅ / ❌
- Rapid Toggling: ✅ / ❌
- Long Running: ✅ / ❌

Performance:
- Memory: __________ MB
- CPU: __________ %
- Responsive: ✅ / ❌

Issues Found:
- _______________
- _______________

Notes:
_______________
_______________
```

## Running Tests in CI/CD

GitHub Actions automatically runs tests on push:
```yaml
- name: Run Tests
  run: ./gradlew test
```

View results in GitHub Actions tab of your repository.
