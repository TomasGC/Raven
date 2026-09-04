package app.raven.ui.viewmodel

import app.raven.core.PuzzleModule
import org.junit.Assert.assertEquals
import org.junit.Test

private object FakeModule : PuzzleModule {
    override val id: String = "fake"
    override val displayName: String = "Fake"
}

class HomeViewModelTest {

    @Test
    fun `selecting a module with permission granted emits StartOverlay`() {
        val viewModel = HomeViewModel(puzzleModules = listOf(FakeModule), hasOverlayPermission = { true })

        viewModel.onModuleSelected(FakeModule)

        assertEquals(HomeUiEvent.StartOverlay(FakeModule), viewModel.events.value)
    }

    @Test
    fun `selecting a module without permission emits RequestOverlayPermission`() {
        val viewModel = HomeViewModel(puzzleModules = listOf(FakeModule), hasOverlayPermission = { false })

        viewModel.onModuleSelected(FakeModule)

        assertEquals(HomeUiEvent.RequestOverlayPermission(FakeModule), viewModel.events.value)
    }

    @Test
    fun `onEventConsumed clears the pending event`() {
        val viewModel = HomeViewModel(puzzleModules = listOf(FakeModule), hasOverlayPermission = { true })
        viewModel.onModuleSelected(FakeModule)

        viewModel.onEventConsumed()

        assertEquals(null, viewModel.events.value)
    }

    @Test
    fun `modules exposes the injected list`() {
        val viewModel = HomeViewModel(puzzleModules = listOf(FakeModule), hasOverlayPermission = { true })

        assertEquals(listOf(FakeModule), viewModel.modules)
    }
}
