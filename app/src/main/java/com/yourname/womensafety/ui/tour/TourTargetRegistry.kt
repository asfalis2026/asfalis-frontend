package com.yourname.womensafety.ui.tour

import androidx.compose.ui.geometry.Rect

/**
 * Global singleton registry that maps semantics test-tags to their
 * on-screen bounds (in root coordinates).
 *
 * Screens write their composable bounds here via [register].
 * [TourOverlay] reads them via [getBounds] to position the spotlight.
 */
object TourTargetRegistry {
    private val bounds = mutableMapOf<String, Rect>()

    /** Called from [onGloballyPositioned] inside each screen composable. */
    fun register(tag: String, rect: Rect) {
        bounds[tag] = rect
    }

    /** Returns the last-known bounds for [tag], or null if not yet registered. */
    fun getBounds(tag: String): Rect? = bounds[tag]

    /** Call when a screen is disposed to avoid stale rects. */
    fun unregister(tag: String) {
        bounds.remove(tag)
    }

    /** Wipe everything — call on logout or if the registry becomes inconsistent. */
    fun clear() {
        bounds.clear()
    }
}
