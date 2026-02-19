package com.infopush.app.data.remote

import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InfoPushApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: InfoPushApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NetworkModule.createApi(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get home sources uses correct path and parses response`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "sources": [
                    {"id": "s1", "name": "Top News", "description": "daily", "url": "https://example.com"}
                  ],
                  "items": [
                    {"id": "i1", "sourceId": "s1", "title": "hello", "summary": "world", "url": "https://example.com/a", "publishedAt": "2026-02-19T04:00:00Z"}
                  ]
                }
                """.trimIndent()
            )
        )

        val response = api.getHomeSources()
        val request = server.takeRequest()

        assertEquals("/v1/sources/home", request.path)
        assertEquals(1, response.sources.size)
        assertEquals("s1", response.sources.first().id)
        assertEquals(1, response.items.size)
        assertEquals("i1", response.items.first().id)
    }

    @Test
    fun `add favorite posts to favorites endpoint`() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"ok\":true,\"duplicated\":false}"))

        val response = api.addFavorite(FavoriteRequest(title = "hello", url = "https://example.com/a", itemId = "i1"))
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/v1/favorites", request.path)
        assertEquals(true, response.ok)
    }

    @Test
    fun `export data uses export endpoint and parses payload`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "version": 1,
                  "exportedAt": "2026-02-19T04:00:00Z",
                  "data": {
                    "favorites": [{"itemId":"i1"}],
                    "preferences": {"theme":"dark"}
                  }
                }
                """.trimIndent()
            )
        )

        val response: DataExportResponse = api.exportData()
        val request = server.takeRequest()

        assertEquals("/v1/data/export", request.path)
        assertEquals(1, response.version)
        assertEquals("dark", response.data.preferences["theme"])
        assertEquals("i1", response.data.favorites.first().itemId)
    }
}
