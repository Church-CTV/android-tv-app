package com.dominickp.firetvandroidtv.util

import org.junit.Assert.*
import org.junit.Test

class LoggerTest {
    @Test
    fun `Logger_object_can_be_accessed`() {
        assertNotNull(Logger)
    }

    @Test
    fun `Logger_has_setLevel_method`() {
        val declMethods = Logger::class.java.declaredMethods
        val hasLevel = declMethods.any { it.name == "setLevel" }
        assertTrue(hasLevel)
    }

    @Test
    fun `LogLevel_has_expected_values`() {
        val values = Logger.LogLevel.values()
        assertEquals(6, values.size)
        assertTrue(values.contains(Logger.LogLevel.VERBOSE))
        assertTrue(values.contains(Logger.LogLevel.DEBUG))
        assertTrue(values.contains(Logger.LogLevel.INFO))
        assertTrue(values.contains(Logger.LogLevel.WARN))
        assertTrue(values.contains(Logger.LogLevel.ERROR))
        assertTrue(values.contains(Logger.LogLevel.NONE))
    }

    @Test
    fun `Logger_has_info_method`() {
        val declMethods = Logger::class.java.declaredMethods
        // Logger uses single-letter method names: d, i, w, e, v
        val hasI = declMethods.any { it.name == "i" }
        assertTrue(hasI)
    }

    @Test
    fun `Logger_has_debug_method`() {
        val declMethods = Logger::class.java.declaredMethods
        val hasD = declMethods.any { it.name == "d" }
        assertTrue(hasD)
    }

    @Test
    fun `Logger_has_warn_method`() {
        val declMethods = Logger::class.java.declaredMethods
        val hasW = declMethods.any { it.name == "w" }
        assertTrue(hasW)
    }

    @Test
    fun `Logger_has_error_method`() {
        val declMethods = Logger::class.java.declaredMethods
        val hasE = declMethods.any { it.name == "e" }
        assertTrue(hasE)
    }
}
