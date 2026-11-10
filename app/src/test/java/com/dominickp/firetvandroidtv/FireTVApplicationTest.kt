package com.dominickp.firetvandroidtv

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FireTVApplicationTest {
    @Test
    fun `FireTVApplication can be instantiated`() {
        val app = FireTVApplication()
        assertNotNull(app)
    }

    @Test
    fun `FireTVApplication has correct package`() {
        val app = FireTVApplication()
        assertEquals("com.dominickp.firetvandroidtv", app::class.java.`package`?.name)
    }
}
