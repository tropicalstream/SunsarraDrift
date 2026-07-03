package com.sunsarradrift

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import kotlin.math.abs

/**
 * The ONLY controls, as decreed: head movement (GazeCamera), TAP, and
 * SWIPE FORWARD / BACK on the right temple pad. Double-tap opens settings
 * (the one concession — it's two taps, still taps). Taps fire instantly
 * during play; menus use the deliberate delayed-tap variant.
 */
class GameGestures(
    private val onTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onSwipeForward: () -> Unit,
    private val onSwipeBack: () -> Unit
) {
    companion object {
        private const val TAP_MS = 230L
        private const val DOUBLE_MS = 300L
        private const val SLOP_PX = 34f
        private const val REPEAT_PX = 90f
    }

    private val handler = Handler(Looper.getMainLooper())
    private var downT = 0L
    private var downP = 0f
    private var emitted = 0
    private var moved = false
    private var lastTapT = 0L
    private var pendingTap: Runnable? = null

    fun onTouchEvent(e: MotionEvent): Boolean {
        // primary axis = dominant motion; forward = positive along the temple
        val now = SystemClock.uptimeMillis()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downT = now; downP = e.x + e.y   // captured for both axes
                moved = false; emitted = 0
            }
            MotionEvent.ACTION_MOVE -> {
                val d = (e.x + e.y) - downP
                if (abs(d) > SLOP_PX) moved = true
                val steps = (d / REPEAT_PX).toInt()
                while (emitted < abs(steps)) {
                    emitted++
                    var fwd = steps > 0
                    if (Game.invertSteer) fwd = !fwd
                    if (fwd) onSwipeForward() else onSwipeBack()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val held = now - downT
                if (moved || held > TAP_MS || e.actionMasked == MotionEvent.ACTION_CANCEL) return true
                val live = Game.phase == Game.PLAY && !Game.menuOpen && !Game.paused
                if (live) {
                    // instant taps in combat; second tap within window = settings
                    if (now - lastTapT <= DOUBLE_MS) { lastTapT = 0; onDoubleTap() }
                    else { lastTapT = now; onTap() }
                } else if (now - lastTapT <= DOUBLE_MS) {
                    pendingTap?.let { handler.removeCallbacks(it) }
                    pendingTap = null; lastTapT = 0
                    onDoubleTap()
                } else {
                    lastTapT = now
                    val r = Runnable { pendingTap = null; onTap() }
                    pendingTap = r
                    handler.postDelayed(r, DOUBLE_MS)
                }
            }
        }
        return true
    }
}
