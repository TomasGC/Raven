package app.raven

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.raven.service.OverlayService
import app.raven.ui.screen.HomeScreen
import app.raven.ui.theme.RavenTheme
import app.raven.ui.viewmodel.HomeUiEvent
import app.raven.ui.viewmodel.HomeViewModel
import app.raven.util.overlayPermissionSettingsIntent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            RavenTheme {
                val event by viewModel.events.collectAsState()

                HomeScreen(
                    modules = viewModel.modules,
                    onModuleSelected = viewModel::onModuleSelected
                )

                LaunchedEffect(event) {
                    when (val current = event) {
                        is HomeUiEvent.RequestOverlayPermission -> {
                            startActivity(overlayPermissionSettingsIntent(this@MainActivity))
                            viewModel.onEventConsumed()
                        }
                        is HomeUiEvent.StartOverlay -> {
                            startForegroundService(
                                OverlayService.newIntent(this@MainActivity, current.module)
                            )
                            viewModel.onEventConsumed()
                            moveTaskToBack(true)
                        }
                        null -> Unit
                    }
                }
            }
        }
    }
}
