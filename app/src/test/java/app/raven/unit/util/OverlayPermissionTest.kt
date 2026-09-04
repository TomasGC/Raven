package app.raven.util

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class OverlayPermissionTest {

    @Test
    fun `hasOverlayPermission reflects Settings canDrawOverlays`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()

        ShadowSettings.setCanDrawOverlays(false)
        assertFalse(hasOverlayPermission(context))

        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(hasOverlayPermission(context))
    }

    @Test
    fun `overlayPermissionSettingsIntent targets this app's package`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()

        val intent = overlayPermissionSettingsIntent(context)

        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }
}
