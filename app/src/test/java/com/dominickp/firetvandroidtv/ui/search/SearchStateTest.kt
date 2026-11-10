package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.data.model.VideoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SearchStateTest {
    @Test
    fun `Idle is an object singleton`() {
        val idle = SearchState.Idle
        assertSame(idle, SearchState.Idle)
    }

    @Test
    fun `Loading is an object singleton`() {
        val loading = SearchState.Loading
        assertSame(loading, SearchState.Loading)
    }

    @Test
    fun `Results contains items list`() {
        val items = listOf(VideoItem("1", "Item 1", "video"))
        val state = SearchState.Results(items)
        assertEquals(1, state.items.size)
        assertEquals("Item 1", state.items[0].title)
    }

    @Test
    fun `Results can have empty items`() {
        val state = SearchState.Results(emptyList())
        assertEquals(0, state.items.size)
    }

    @Test
    fun `Error contains message`() {
        val state = SearchState.Error("Something went wrong")
        assertEquals("Something went wrong", state.message)
    }

    @Test
    fun `Error can have empty message`() {
        val state = SearchState.Error("")
        assertEquals("", state.message)
    }

    @Test
    fun `Results preserves all video item properties`() {
        val items =
            listOf(
                VideoItem(
                    id = "vid1",
                    title = "Test Video",
                    shortDescription = "Short desc",
                    thumbnail = "http://example.com/thumb.jpg",
                    url = "http://example.com/watch/vid1",
                    content = null,
                    isLive = true,
                ),
            )
        val state = SearchState.Results(items)
        val video = state.items.first()
        assertEquals("vid1", video.id)
        assertEquals("Test Video", video.title)
        assertEquals("Short desc", video.shortDescription)
        assertEquals("http://example.com/watch/vid1", video.playableUrl)
        assertEquals("http://example.com/thumb.jpg", video.thumbnail)
        assertTrue(video.isLive)
        assertEquals(0, video.duration) // null content → 0
    }

    @Test
    fun `State transitions are mutually exclusive`() {
        val states = listOf(SearchState.Idle, SearchState.Loading)
        val results = SearchState.Results(emptyList())
        val error = SearchState.Error("err")

        assertTrue(states[0] is SearchState.Idle)
        assertTrue(states[1] is SearchState.Loading)
        assertTrue(results is SearchState.Results)
        assertTrue(error is SearchState.Error)

        assertNotEquals(states[0], states[1])
        assertNotEquals(states[0], results)
        assertNotEquals(states[0], error)
        assertNotEquals(states[1], results)
        assertNotEquals(states[1], error)
        assertNotEquals(results, error)
    }

    @Test
    fun `Results items are immutable reference`() {
        val items = mutableListOf(VideoItem("1", "Item 1", "video"))
        val state = SearchState.Results(items.toList())
        // Modify original list — should not affect state's items
        items.add(VideoItem("2", "Item 2", "video"))
        assertEquals(1, state.items.size)
        assertEquals("1", state.items[0].id)
    }

    @Test
    fun `Error message supports unicode characters`() {
        val state = SearchState.Error("网络錯誤 (Network Error)")
        assertEquals("网络錯誤 (Network Error)", state.message)
    }

    @Test
    fun `Results can contain multiple video items`() {
        val items =
            (1..10).map { i ->
                VideoItem(
                    id = "vid$i",
                    title = "Video $i",
                    shortDescription = "Description $i",
                    thumbnail = "http://example.com/thumb$i.jpg",
                    url = "http://example.com/watch/vid$i",
                    content = null,
                    isLive = i % 3 == 0,
                )
            }
        val state = SearchState.Results(items)
        assertEquals(10, state.items.size)
        assertEquals("vid5", state.items[4].id)
        assertEquals("Video 5", state.items[4].title)
        assertEquals("Description 5", state.items[4].shortDescription)
        assertFalse(state.items[4].isLive) // 5 % 3 != 0
        assertTrue(state.items[2].isLive) // 3 % 3 == 0
    }
}
