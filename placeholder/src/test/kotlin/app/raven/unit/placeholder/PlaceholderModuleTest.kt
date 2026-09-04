package app.raven.placeholder

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderModuleTest {

    @Test
    fun `exposes the expected id and display name`() {
        assertEquals("placeholder", PlaceholderModule.id)
        assertEquals("Placeholder", PlaceholderModule.displayName)
    }
}
