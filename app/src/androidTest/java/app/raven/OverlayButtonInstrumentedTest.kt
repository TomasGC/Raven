package app.raven

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Real device/emulator tests for the floating overlay button, driven through the actual
 * MainActivity -> HomeViewModel -> OverlayService flow (the same path a real user takes),
 * not through Robolectric shadows.
 *
 * These cover what nothing else does yet:
 * - [OverlayServiceIntegrationTest][app.raven.integration.service.OverlayServiceIntegrationTest]
 *   (Robolectric) only proves a view was added to a *shadow* WindowManager, never that anything
 *   is actually visible on a real compositor.
 * - [MainActivityTest] only exercises the permission-NOT-granted branch, never the granted one
 *   that actually starts the overlay.
 * - Nothing before this exercised a real drag gesture end-to-end; the unit test for
 *   [app.raven.service.OverlayButtonGestureDetector] only proves the tap/drag/kill
 *   classification logic in isolation, never that dragging actually moves the real window or
 *   that dropping it in the bottom kill zone actually removes the overlay.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OverlayButtonInstrumentedTest {

    // See MainActivityTest's identical rule for why this must be conditional on API 33+.
    @get:Rule(order = 0)
    val grantPermissionRule: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base, _ -> base }
        }

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice
    private lateinit var packageName: String

    @Before
    fun setUp() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        packageName = composeTestRule.activity.packageName

        // SYSTEM_ALERT_WINDOW is an AppOps-backed special permission: there is no runtime-permission
        // dialog for it and `adb shell pm grant` does not apply to it. The `appops` shell command is
        // the only way to grant it from an automated test short of scripting the real Settings UI.
        runShellCommand("appops set $packageName SYSTEM_ALERT_WINDOW allow")

        composeTestRule.onNodeWithText("Placeholder").performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "expected the overlay button to attach to the real window after a granted-permission tap",
            device.wait(Until.hasObject(overlaySelector()), STARTUP_TIMEOUT_MILLIS)
        )
    }

    @After
    fun tearDown() {
        // Reset to the fresh-install default so MainActivityTest's assumption ("a fresh
        // device/emulator never has SYSTEM_ALERT_WINDOW pre-granted") still holds regardless of
        // which order the instrumented test classes run in.
        runShellCommand("appops set $packageName SYSTEM_ALERT_WINDOW default")
    }

    @Test
    fun overlayButton_isVisibleOnRealWindowAfterServiceStarts() {
        val overlay = requireNotNull(device.findObject(overlaySelector())) {
            "overlay button should be attached to the real system window"
        }

        assertTrue(overlay.visibleBounds.width() > 0 && overlay.visibleBounds.height() > 0)
    }

    @Test
    fun realTap_togglesButtonIconForReal() {
        val overlay = requireNotNull(device.findObject(overlaySelector())) {
            "overlay button should be attached to the real system window before tapping it"
        }

        overlay.click()

        assertTrue(
            "expected the overlay's content description to switch to the running state after a real tap",
            device.wait(Until.hasObject(By.desc(runningDescription())), TOGGLE_TIMEOUT_MILLIS)
        )
    }

    @Test
    fun realDragToBottomZone_killsTheOverlay() {
        val overlay = requireNotNull(device.findObject(overlaySelector())) {
            "overlay button should be attached to the real system window before dragging it"
        }
        val activityManager = composeTestRule.activity.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
            as ActivityManager

        overlay.drag(Point(overlay.visibleBounds.centerX(), device.displayHeight - DRAG_TARGET_BOTTOM_MARGIN_PX))

        assertTrue(
            "expected the overlay to be removed from the real window after dragging it into the bottom kill zone",
            device.wait(Until.gone(overlaySelector()), KILL_TIMEOUT_MILLIS)
        )
        assertTrue(
            "expected Raven's own app task to be removed from Recents after a kill-zone drag, not just the " +
                "overlay view — regressed once before: killSelf() stopped the service but never called " +
                "finishAndRemoveTask()",
            waitUntilAppTaskRemoved(activityManager)
        )
    }

    @Test
    fun realDrag_movesTheOverlayWithoutKillingIt() {
        val overlay = requireNotNull(device.findObject(overlaySelector())) {
            "overlay button should be attached to the real system window before dragging it"
        }
        val startBounds = overlay.visibleBounds

        overlay.drag(Point(startBounds.centerX() - DRAG_OFFSET_PX, startBounds.centerY() - DRAG_OFFSET_PX))
        composeTestRule.waitForIdle()

        val movedOverlay = requireNotNull(device.findObject(overlaySelector())) {
            "overlay button should still be attached to the real window after a short drag"
        }
        assertTrue(
            "expected the overlay to have moved from its original position",
            movedOverlay.visibleBounds.centerX() != startBounds.centerX() ||
                movedOverlay.visibleBounds.centerY() != startBounds.centerY()
        )
    }

    private fun waitUntilAppTaskRemoved(activityManager: ActivityManager): Boolean {
        val deadline = System.currentTimeMillis() + APP_TASK_REMOVAL_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline && activityManager.appTasks.isNotEmpty()) {
            Thread.sleep(APP_TASK_POLL_INTERVAL_MILLIS)
        }
        return activityManager.appTasks.isEmpty()
    }

    private fun overlaySelector(): BySelector = By.pkg(packageName).clazz(ImageView::class.java)

    private fun runningDescription(): String =
        composeTestRule.activity.getString(R.string.overlay_button_state_running)

    private fun runShellCommand(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    companion object {
        private const val STARTUP_TIMEOUT_MILLIS = 5_000L
        private const val TOGGLE_TIMEOUT_MILLIS = 3_000L
        private const val KILL_TIMEOUT_MILLIS = 3_000L
        private const val APP_TASK_REMOVAL_TIMEOUT_MILLIS = 3_000L
        private const val APP_TASK_POLL_INTERVAL_MILLIS = 100L
        private const val DRAG_TARGET_BOTTOM_MARGIN_PX = 50
        private const val DRAG_OFFSET_PX = 300
    }
}
