package app.raven.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import app.raven.R
import timber.log.Timber

// Top-level (not a class member) so it doesn't push OverlayWindowController over detekt's
// TooManyFunctions threshold. Content description doubles as an automated-test hook (see the
// real-device instrumented tests for the overlay button): UiAutomator can't read a View's drawable
// resource id across process/window boundaries, but it can read contentDescription, which also
// happens to be the right accessibility behavior for a state-carrying icon button.
private fun overlayVisualsFor(state: OverlayUiState): Pair<Int, Int> = when (state) {
    OverlayUiState.PAUSED -> R.drawable.ic_overlay_start to R.string.overlay_button_state_paused
    OverlayUiState.RUNNING -> R.drawable.ic_overlay_pause to R.string.overlay_button_state_running
}

/**
 * Owns the overlay button's real WindowManager view, its drag/kill-zone indicator view, and the
 * touch-to-gesture wiring — split out of OverlayService so the Service itself stays a thin
 * lifecycle/notification shell (see contexts/design-patterns.md's "Pure Gesture Classifier,
 * Android I/O Kept in the Service" — this class is that "Android I/O" side, OverlayService
 * no longer needs to be).
 */
class OverlayWindowController(
    private val context: Context,
    private val onKill: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val gestureDetector = OverlayButtonGestureDetector(
        touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    )
    private val killZoneTopY = windowManager.currentWindowMetrics.bounds.height() - KILL_ZONE_HEIGHT_PX

    private var overlayView: ImageView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var killZoneView: View? = null
    private var uiState: OverlayUiState = OverlayUiState.PAUSED
    private var lastTouchRawX = 0f
    private var lastTouchRawY = 0f

    fun ensureOverlayShown() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = OVERLAY_Y_OFFSET_PX
        }

        val view = ImageView(context).apply {
            val (iconRes, descriptionRes) = overlayVisualsFor(uiState)
            setImageResource(iconRes)
            contentDescription = context.getString(descriptionRes)
            setBackgroundResource(R.drawable.bg_overlay_button)
            setPadding(
                OVERLAY_BUTTON_PADDING_PX,
                OVERLAY_BUTTON_PADDING_PX,
                OVERLAY_BUTTON_PADDING_PX,
                OVERLAY_BUTTON_PADDING_PX
            )
            setOnTouchListener { touchedView, event -> onOverlayTouch(touchedView, event) }
        }

        windowManager.addView(view, params)
        overlayView = view
        overlayParams = params
    }

    fun removeOverlayView() {
        hideKillZoneIndicator()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        overlayParams = null
    }

    private fun onOverlayTouch(touchedView: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureDetector.onDown(event.rawX, event.rawY)
                lastTouchRawX = event.rawX
                lastTouchRawY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val wasDragging = killZoneView != null
                val isDragging = gestureDetector.onMove(event.rawX, event.rawY)
                if (isDragging) {
                    if (!wasDragging) showKillZoneIndicator()
                    moveOverlayBy(
                        dx = (event.rawX - lastTouchRawX).toInt(),
                        dy = (event.rawY - lastTouchRawY).toInt()
                    )
                }
                lastTouchRawX = event.rawX
                lastTouchRawY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                hideKillZoneIndicator()
                when (gestureDetector.onUp(event.rawY, killZoneTopY)) {
                    OverlayGestureResult.Tap -> {
                        toggleUiState()
                        touchedView.performClick()
                    }
                    OverlayGestureResult.Kill -> {
                        Timber.tag(TAG).d("Overlay dragged into the kill zone, killing Raven overlay")
                        onKill()
                    }
                    OverlayGestureResult.Dragged -> Unit
                }
            }
        }
        return true
    }

    private fun moveOverlayBy(dx: Int, dy: Int) {
        val params = overlayParams ?: return
        // Gravity is TOP|END, so x is an inward offset from the right edge: moving the finger
        // right (positive dx) must shrink that offset, not grow it.
        params.x -= dx
        params.y += dy
        overlayView?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun showKillZoneIndicator() {
        if (killZoneView != null) return
        val view = View(context).apply {
            setBackgroundResource(R.drawable.bg_kill_zone_indicator)
        }
        val params = WindowManager.LayoutParams(
            KILL_ZONE_INDICATOR_SIZE_PX,
            KILL_ZONE_INDICATOR_SIZE_PX,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = KILL_ZONE_INDICATOR_MARGIN_BOTTOM_PX
        }
        windowManager.addView(view, params)
        killZoneView = view
    }

    private fun hideKillZoneIndicator() {
        killZoneView?.let { windowManager.removeView(it) }
        killZoneView = null
    }

    private fun toggleUiState() {
        uiState = uiState.toggled()
        Timber.tag(TAG).d("Overlay button toggled to $uiState")
        val (iconRes, descriptionRes) = overlayVisualsFor(uiState)
        overlayView?.setImageResource(iconRes)
        overlayView?.contentDescription = context.getString(descriptionRes)
    }

    companion object {
        private const val TAG = "OverlayWindowController"
        private const val OVERLAY_Y_OFFSET_PX = 200
        private const val OVERLAY_BUTTON_PADDING_PX = 16
        private const val KILL_ZONE_HEIGHT_PX = 260
        private const val KILL_ZONE_INDICATOR_SIZE_PX = 168
        private const val KILL_ZONE_INDICATOR_MARGIN_BOTTOM_PX = 60
    }
}
