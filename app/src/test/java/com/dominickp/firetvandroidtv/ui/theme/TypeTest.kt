package com.dominickp.firetvandroidtv.ui.theme

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TypeTest {
    @Test
    fun `AppTypography has headlineMedium`() {
        assertNotNull(AppTypography.headlineMedium)
    }

    @Test
    fun `AppTypography has titleLarge`() {
        assertNotNull(AppTypography.titleLarge)
    }

    @Test
    fun `AppTypography has bodyLarge`() {
        assertNotNull(AppTypography.bodyLarge)
    }

    @Test
    fun `AppTypography has bodyMedium`() {
        assertNotNull(AppTypography.bodyMedium)
    }

    @Test
    fun `AppTypography has labelLarge`() {
        assertNotNull(AppTypography.labelLarge)
    }
}
