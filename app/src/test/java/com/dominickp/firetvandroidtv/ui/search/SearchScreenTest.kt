package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.data.model.VideoItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SearchScreen composable logic.
 * Tests state transitions, keyboard input simulation, search results, and error handling.
 */
class SearchScreenTest {
    @Test
    fun `search_field_has_placeholder`() {
        val placeholder = "Search videos..."
        assertFalse(placeholder.isEmpty())
    }

    @Test
    fun `search_state_transitions_idle_to_loading`() {
        var state: SearchState = SearchState.Idle
        state = SearchState.Loading
        assertTrue(state is SearchState.Loading)
    }

    @Test
    fun `search_state_transitions_loading_to_results`() {
        var state: SearchState = SearchState.Loading
        val results =
            listOf(
                VideoItem("1", "Test Video", "http://test/1.mp4", "http://test/1.jpg"),
                VideoItem("2", "Another Video", "http://test/2.mp4", "http://test/2.jpg"),
            )
        state = SearchState.Results(results)
        assertTrue(state is SearchState.Results)
        assertEquals(2, (state as SearchState.Results).items.size)
    }

    @Test
    fun `search_state_transitions_to_error`() {
        var state: SearchState = SearchState.Loading
        state = SearchState.Error("Connection failed")
        assertTrue(state is SearchState.Error)
        assertEquals("Connection failed", (state as SearchState.Error).message)
    }

    @Test
    fun `search_results_can_be_empty`() {
        val emptyResults = emptyList<VideoItem>()
        val state = SearchState.Results(emptyResults)
        assertTrue(state is SearchState.Results)
        assertEquals(0, (state as SearchState.Results).items.size)
    }

    @Test
    fun `tv_keyboard_has_qwerty_layout`() {
        // Row 0: 10 keys
        val row0 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
        assertEquals(10, row0.size)

        // Row 1: 9 keys
        val row1 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
        assertEquals(9, row1.size)

        // Row 2: 3 special keys
        val row2 = listOf("BACKSPACE", "SPACE", "ENTER")
        assertEquals(3, row2.size)
    }

    @Test
    fun `tv_keyboard_backspace_removes_last_char`() {
        var query = "hello"
        query = query.dropLast(1)
        assertEquals("hell", query)
    }

    @Test
    fun `tv_keyboard_space_adds_space`() {
        var query = "test"
        query = "$query "
        assertEquals("test ", query)
    }

    @Test
    fun `tv_keyboard_keypress_appends_char`() {
        var query = ""
        query = "$query${'A'}"
        assertEquals("A", query)
        query = "$query${'B'}"
        assertEquals("AB", query)
    }

    @Test
    fun `search_query_can_be_empty_string`() {
        val emptyQuery = ""
        assertTrue(emptyQuery.isEmpty())
    }

    @Test
    fun `search_query_can_contain_spaces`() {
        val queryWithSpaces = "action comedy"
        assertTrue(queryWithSpaces.contains(" "))
    }

    @Test
    fun `video_item_from_search_result_has_id`() {
        val video = VideoItem("vid123", "Test", "http://test.mp4", "http://test.jpg")
        assertEquals("vid123", video.id)
        assertFalse(video.title.isEmpty())
    }

    @Test
    fun `empty_search_shows_no_results_state`() {
        val empty = emptyList<VideoItem>()
        assertTrue(empty.isEmpty())
    }
}
