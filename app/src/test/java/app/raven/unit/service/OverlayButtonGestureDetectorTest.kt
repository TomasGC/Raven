package app.raven.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayButtonGestureDetectorTest {

    private val touchSlopPx = 10
    private val killZoneTopY = 2000

    @Test
    fun `movement within touch slop does not start a drag`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)

        assertFalse(detector.onMove(105f, 104f))
    }

    @Test
    fun `movement beyond touch slop starts a drag`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)

        assertTrue(detector.onMove(100f, 200f))
    }

    @Test
    fun `release within touch slop is a tap`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)
        detector.onMove(103f, 102f)

        assertEquals(OverlayGestureResult.Tap, detector.onUp(102f, killZoneTopY))
    }

    @Test
    fun `release above the kill zone after dragging is Dragged`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)
        detector.onMove(100f, 300f)

        assertEquals(OverlayGestureResult.Dragged, detector.onUp(300f, killZoneTopY))
    }

    @Test
    fun `release exactly at the kill zone top after dragging is Kill`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)
        detector.onMove(100f, killZoneTopY.toFloat())

        assertEquals(OverlayGestureResult.Kill, detector.onUp(killZoneTopY.toFloat(), killZoneTopY))
    }

    @Test
    fun `release below the kill zone top after dragging is Kill`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)
        detector.onMove(100f, 2500f)

        assertEquals(OverlayGestureResult.Kill, detector.onUp(2500f, killZoneTopY))
    }

    @Test
    fun `onUp without any preceding movement is a tap`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)

        assertEquals(OverlayGestureResult.Tap, detector.onUp(100f, killZoneTopY))
    }

    @Test
    fun `a second gesture after a completed drag resets slop tracking`() {
        val detector = OverlayButtonGestureDetector(touchSlopPx)
        detector.onDown(100f, 100f)
        detector.onMove(100f, 300f)
        detector.onUp(300f, killZoneTopY)

        detector.onDown(400f, 400f)

        assertFalse(detector.onMove(402f, 401f))
    }
}
