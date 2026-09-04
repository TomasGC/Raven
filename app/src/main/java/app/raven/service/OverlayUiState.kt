package app.raven.service

enum class OverlayUiState {
    PAUSED,
    RUNNING;

    fun toggled(): OverlayUiState = when (this) {
        PAUSED -> RUNNING
        RUNNING -> PAUSED
    }
}
