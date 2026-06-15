package com.yourname.womensafety.ui.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the tour state machine.
 *
 * IMPORTANT: [next] and [back] no longer trigger navigation — that is the
 * responsibility of TourOverlay, which needs to navigate FIRST and wait for
 * the screen to render before advancing the index.
 *
 * Use [peekNext] / [peekPrev] to look ahead/behind without mutating state.
 */
class TourEngine {

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    val totalSteps: Int get() = TOUR_STEPS.size

    val currentStep: TourStep
        get() = TOUR_STEPS[_currentStepIndex.value.coerceIn(0, TOUR_STEPS.lastIndex)]

    /** Launch the tour from the beginning. */
    fun start() {
        _currentStepIndex.value = 0
        _isActive.value = true
    }

    /** Re-launch from step 0 (used by "Replay Tour" in Help screen). */
    fun restart() {
        _currentStepIndex.value = 0
        _isActive.value = true
    }

    /**
     * Peek at the next step without changing state.
     * Returns null if already at the last step.
     */
    fun peekNext(): TourStep? {
        val nextIdx = _currentStepIndex.value + 1
        return TOUR_STEPS.getOrNull(nextIdx)
    }

    /**
     * Peek at the previous step without changing state.
     * Returns null if already at step 0.
     */
    fun peekPrev(): TourStep? {
        val prevIdx = _currentStepIndex.value - 1
        return TOUR_STEPS.getOrNull(prevIdx.coerceAtLeast(1))
    }

    /**
     * Advance to the next step.
     * TourOverlay is responsible for navigating to the new screen BEFORE calling this.
     * Returns false if the tour just completed.
     */
    fun advance(): Boolean {
        val nextIdx = _currentStepIndex.value + 1
        if (nextIdx >= TOUR_STEPS.size) {
            _complete()
            return false
        }
        _currentStepIndex.value = nextIdx
        return true
    }

    /**
     * Go back one step (minimum step 1).
     * TourOverlay is responsible for navigating to the previous screen BEFORE calling this.
     */
    fun back() {
        val prev = (_currentStepIndex.value - 1).coerceAtLeast(1)
        _currentStepIndex.value = prev
    }

    /** Dismiss the tour immediately. */
    fun skip() {
        _complete()
    }

    private fun _complete() {
        _isActive.value = false
    }
}

@Composable
fun rememberTourEngine(): TourEngine = remember { TourEngine() }
