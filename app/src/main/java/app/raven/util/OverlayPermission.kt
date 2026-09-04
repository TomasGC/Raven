package app.raven.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun hasOverlayPermission(context: Context): Boolean =
    Settings.canDrawOverlays(context)

fun overlayPermissionSettingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
