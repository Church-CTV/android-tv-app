package com.dominickp.firetvandroidtv.db

import org.junit.Assert.*
import org.junit.Test

class FireTVDatabaseTest {
    @Test
    fun `FireTVDatabase_is_abstract_class`() {
        assertTrue(java.lang.reflect.Modifier.isAbstract(FireTVDatabase::class.java.modifiers))
    }

    @Test
    fun `FireTVDatabase_has_continueWatchingDao_method`() {
        val methods = FireTVDatabase::class.java.declaredMethods
        assertTrue(methods.any { it.name == "continueWatchingDao" })
    }
}
