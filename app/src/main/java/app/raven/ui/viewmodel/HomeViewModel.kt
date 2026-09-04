package app.raven.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.raven.core.PuzzleModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface HomeUiEvent {
    data class RequestOverlayPermission(val module: PuzzleModule) : HomeUiEvent
    data class StartOverlay(val module: PuzzleModule) : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    // @JvmSuppressWildcards: Dagger's binding lookup needs the requested and provided generic
    // signatures to match exactly. Kotlin's declaration-site covariant List emits a wildcarded
    // Java signature (List<? extends PuzzleModule>) at consumption sites like this constructor
    // parameter, which would otherwise mismatch AppModule's plain List<PuzzleModule> binding.
    private val puzzleModules: @JvmSuppressWildcards List<PuzzleModule>,
    private val hasOverlayPermission: () -> Boolean
) : ViewModel() {

    val modules: List<PuzzleModule> = puzzleModules

    private val _events = MutableStateFlow<HomeUiEvent?>(null)
    val events: StateFlow<HomeUiEvent?> = _events.asStateFlow()

    fun onModuleSelected(module: PuzzleModule) {
        _events.value = if (hasOverlayPermission()) {
            HomeUiEvent.StartOverlay(module)
        } else {
            HomeUiEvent.RequestOverlayPermission(module)
        }
    }

    fun onEventConsumed() {
        _events.value = null
    }
}
