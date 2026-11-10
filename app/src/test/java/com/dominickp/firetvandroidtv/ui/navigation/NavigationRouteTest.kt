package com.dominickp.firetvandroidtv.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for NavigationRoute navigation routing.
 */
class NavigationRouteTest {
    @Test
    fun `toNavigationRoute maps home to Home route`() {
        val route = "home".toNavigationRoute()
        assertTrue(route is NavigationRoute.Home)
    }

    @Test
    fun `toNavigationRoute maps search to Search route`() {
        val route = "search".toNavigationRoute()
        assertTrue(route is NavigationRoute.Search)
    }

    @Test
    fun `toNavigationRoute maps settings to Settings route`() {
        val route = "settings".toNavigationRoute()
        assertTrue(route is NavigationRoute.Settings)
    }

    @Test
    fun `toNavigationRoute maps subscription to Subscription route`() {
        val route = "subscription".toNavigationRoute()
        assertTrue(route is NavigationRoute.Subscription)
    }

    @Test
    fun `toNavigationRoute maps unknown page to Home route`() {
        val route = "unknown_page".toNavigationRoute()
        assertTrue(route is NavigationRoute.Home)
    }

    @Test
    fun `screenName returns home for Home route`() {
        assertEquals("home", NavigationRoute.Home.screenName)
    }

    @Test
    fun `screenName returns search for Search route`() {
        assertEquals("search", NavigationRoute.Search.screenName)
    }

    @Test
    fun `screenName returns settings for Settings route`() {
        assertEquals("settings", NavigationRoute.Settings.screenName)
    }

    @Test
    fun `screenName returns subscription for Subscription route`() {
        assertEquals("subscription", NavigationRoute.Subscription.screenName)
    }
}
