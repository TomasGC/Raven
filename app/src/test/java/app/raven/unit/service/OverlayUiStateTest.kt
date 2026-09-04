package app.raven.service

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayUiStateTest {

    @Test
    fun `paused toggles to running`() {
        assertEquals(OverlayUiState.RUNNING, OverlayUiState.PAUSED.toggled())
    }

    @Test
    fun `running toggles to paused`() {
        assertEquals(OverlayUiState.PAUSED, OverlayUiState.RUNNING.toggled())
    }
}
