package app.raven.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.raven.R
import app.raven.core.PuzzleModule
import app.raven.util.hasOverlayPermission
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    // @JvmSuppressWildcards: Dagger's binding lookup needs the requested and provided generic
    // signatures to match exactly. Kotlin's declaration-site covariant List emits a wildcarded
    // Java signature (List<? extends PuzzleModule>) at consumption sites like this injected field,
    // which would otherwise mismatch AppModule's plain List<PuzzleModule> binding.
    @Inject
    lateinit var puzzleModules: @JvmSuppressWildcards List<PuzzleModule>

    private lateinit var overlayWindowController: OverlayWindowController

    override fun onCreate() {
        super.onCreate()
        overlayWindowController = OverlayWindowController(context = this, onKill = ::killSelf)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val moduleId = intent?.getStringExtra(EXTRA_MODULE_ID)
        val module = puzzleModules.firstOrNull { it.id == moduleId }
        val rejectionReason = when {
            module == null -> "No valid puzzle module id provided, stopping"
            !hasOverlayPermission(this) -> "Overlay permission not granted, stopping"
            else -> null
        }
        if (rejectionReason != null) {
            Timber.tag(TAG).e(rejectionReason)
            stopSelf()
            return START_NOT_STICKY
        }

        val validModule = requireNotNull(module)
        Timber.tag(TAG).d("Starting overlay for ${validModule.displayName}")
        startForegroundWithNotification(validModule)
        overlayWindowController.ensureOverlayShown()
        return START_STICKY
    }

    private fun startForegroundWithNotification(module: PuzzleModule) {
        val notification = buildNotification(module)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun killSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Stopping the service alone leaves MainActivity's task sitting in Recents — dragging
        // into the kill zone is meant to remove Raven entirely, not just the floating button.
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.appTasks.forEach { it.finishAndRemoveTask() }
    }

    override fun onDestroy() {
        overlayWindowController.removeOverlayView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.overlay_channel_description)
            setSound(null, null)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(module: PuzzleModule): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_content, module.displayName))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "raven_overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_MODULE_ID = "extra_module_id"

        fun newIntent(context: Context, module: PuzzleModule): Intent =
            Intent(context, OverlayService::class.java).apply {
                putExtra(EXTRA_MODULE_ID, module.id)
            }
    }
}
