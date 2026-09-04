package app.raven

import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.raven.util.hasOverlayPermission
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Real instrumented tests for the home screen, driven through [MainActivity] on an actual
 * device/emulator.
 *
 * MainActivity is `@AndroidEntryPoint` and injects [app.raven.ui.viewmodel.HomeViewModel] via
 * `by viewModels()`, so Hilt's test component must exist before the activity is created.
 * [HiltAndroidRule] handles that, but only if it runs *before* [createAndroidComposeRule]
 * launches the activity — the `order` value on `@Rule` enforces that (lower order = outer
 * rule = its "before" logic runs first).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    // MainActivity.onCreate() requests POST_NOTIFICATIONS only on API 33+ (it's not a runtime
    // permission below that, and GrantPermissionRule throws SecurityException if asked to grant
    // one that doesn't apply on the running API level). Without pre-granting it on 33+, the
    // system permission dialog would steal touch focus from the Compose content below.
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

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_displaysPlaceholderModule() {
        composeTestRule.onNodeWithText("Placeholder").assertIsDisplayed()
    }

    @Test
    fun tappingPlaceholderCard_requestsOverlayPermissionWhenNotGranted() {
        // A fresh device/emulator never has SYSTEM_ALERT_WINDOW pre-granted, so tapping the
        // card is expected to route through HomeUiEvent.RequestOverlayPermission rather than
        // HomeUiEvent.StartOverlay. This asserts that's really the branch this run will take.
        assertFalse(
            "Expected a fresh device/emulator without the overlay permission granted",
            hasOverlayPermission(composeTestRule.activity)
        )

        Intents.init()
        try {
            composeTestRule.onNodeWithText("Placeholder").performClick()
            composeTestRule.waitForIdle()

            intended(
                allOf(
                    hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                    hasData(Uri.parse("package:${composeTestRule.activity.packageName}"))
                )
            )
        } finally {
            Intents.release()
        }
    }
}
