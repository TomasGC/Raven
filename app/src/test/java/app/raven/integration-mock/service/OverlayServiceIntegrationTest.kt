package app.raven.integration.service

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import app.raven.core.PuzzleModule
import app.raven.placeholder.PlaceholderModule
import app.raven.service.OverlayService
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl

// Real module ids never collide with this, so it exercises the "unresolvable module id" branch
// without relying on knowledge of OverlayService's private EXTRA_MODULE_ID key.
private object UnresolvableModule : PuzzleModule {
    override val id: String = "unresolvable-module-id"
    override val displayName: String = "Unresolvable"
}

@RunWith(RobolectricTestRunner::class)
class OverlayServiceIntegrationTest {

    // The overlay view registry (ShadowWindowManagerImpl.views) is a process-wide static
    // multimap, so it can be inspected from any Context's WindowManager, not just the
    // service's own private instance.
    private fun overlayViews(): List<View> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return (shadowOf(windowManager) as ShadowWindowManagerImpl).views
    }

    @Test
    fun `valid module id with overlay permission granted adds the overlay view and keeps the service alive`() {
        ShadowSettings.setCanDrawOverlays(true)
        val intent = OverlayService.newIntent(ApplicationProvider.getApplicationContext(), PlaceholderModule)
        val controller = Robolectric.buildService(OverlayService::class.java, intent)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)

        controller.create().startCommand(0, 1)

        assertFalse(shadowOf(service).isStoppedBySelf)
        assertEquals(1, overlayViews().size)
    }

    @Test
    fun `unresolvable module id stops the service without adding an overlay view`() {
        ShadowSettings.setCanDrawOverlays(true)
        val intent = OverlayService.newIntent(ApplicationProvider.getApplicationContext(), UnresolvableModule)
        val controller = Robolectric.buildService(OverlayService::class.java, intent)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)

        controller.create().startCommand(0, 1)

        assertTrue(shadowOf(service).isStoppedBySelf)
        assertTrue(overlayViews().isEmpty())
    }

    @Test
    fun `valid module id without overlay permission stops the service without adding an overlay view`() {
        ShadowSettings.setCanDrawOverlays(false)
        val intent = OverlayService.newIntent(ApplicationProvider.getApplicationContext(), PlaceholderModule)
        val controller = Robolectric.buildService(OverlayService::class.java, intent)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)

        controller.create().startCommand(0, 1)

        assertTrue(shadowOf(service).isStoppedBySelf)
        assertTrue(overlayViews().isEmpty())
    }

    @Test
    fun `null intent from an OS-initiated restart stops the service without crashing or adding an overlay view`() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(OverlayService::class.java, null)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)

        controller.create().startCommand(0, 1)

        assertTrue(shadowOf(service).isStoppedBySelf)
        assertTrue(overlayViews().isEmpty())
    }

    @Test
    fun `onDestroy after a successful start removes the overlay view`() {
        ShadowSettings.setCanDrawOverlays(true)
        val intent = OverlayService.newIntent(ApplicationProvider.getApplicationContext(), PlaceholderModule)
        val controller = Robolectric.buildService(OverlayService::class.java, intent)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)
        controller.create().startCommand(0, 1)
        assertEquals(1, overlayViews().size)

        controller.destroy()

        assertTrue(overlayViews().isEmpty())
    }

    @Test
    fun `dragging the overlay into the kill zone stops the service and removes the app task`() {
        ShadowSettings.setCanDrawOverlays(true)
        val intent = OverlayService.newIntent(ApplicationProvider.getApplicationContext(), PlaceholderModule)
        val controller = Robolectric.buildService(OverlayService::class.java, intent)
        val service = controller.get()
        service.puzzleModules = listOf(PlaceholderModule)
        controller.create().startCommand(0, 1)
        val overlayView = overlayViews().single()

        val fakeAppTask = mockk<ActivityManager.AppTask>(relaxed = true)
        val activityManager = ApplicationProvider.getApplicationContext<Application>()
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        shadowOf(activityManager).setAppTasks(listOf(fakeAppTask))

        // A huge Y guarantees landing past the kill-zone threshold regardless of Robolectric's
        // default simulated screen height, without needing to know that height here.
        overlayView.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 100f, 100f, 0))
        overlayView.dispatchTouchEvent(MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_MOVE, 100f, 50_000f, 0))
        overlayView.dispatchTouchEvent(MotionEvent.obtain(0L, 20L, MotionEvent.ACTION_UP, 100f, 50_000f, 0))

        assertTrue(shadowOf(service).isStoppedBySelf)
        verify { fakeAppTask.finishAndRemoveTask() }
    }
}
