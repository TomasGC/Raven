package app.raven.service

sealed interface OverlayGestureResult {
    data object Tap : OverlayGestureResult
    data object Dragged : OverlayGestureResult
    data object Kill : OverlayGestureResult
}

// Pure classifier: knows only "was this a tap, a drag, or a drag that ended in the kill zone".
// Actual view repositioning is Android-dependent (WindowManager) and lives in the Service, which
// keeps this class testable without Robolectric, matching the injected-clock precedent this file
// used to set for the old long-press design (see contexts/design-patterns.md).
class OverlayButtonGestureDetector(
    private val touchSlopPx: Int
) {
    private var downRawX = 0f
    private var downRawY = 0f
    private var isDragging = false

    fun onDown(rawX: Float, rawY: Float) {
        downRawX = rawX
        downRawY = rawY
        isDragging = false
    }

    /** Returns whether the gesture is (now) a drag, so the caller knows when to start moving the view. */
    fun onMove(rawX: Float, rawY: Float): Boolean {
        if (!isDragging) {
            val dx = rawX - downRawX
            val dy = rawY - downRawY
            if (dx * dx + dy * dy >= touchSlopPx.toFloat() * touchSlopPx) {
                isDragging = true
            }
        }
        return isDragging
    }

    fun onUp(rawY: Float, killZoneTopY: Int): OverlayGestureResult = when {
        !isDragging -> OverlayGestureResult.Tap
        rawY >= killZoneTopY -> OverlayGestureResult.Kill
        else -> OverlayGestureResult.Dragged
    }
}
