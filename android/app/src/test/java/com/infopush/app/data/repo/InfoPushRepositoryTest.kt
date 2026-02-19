package com.infopush.app.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.infopush.app.data.local.InfoPushDatabase
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import com.infopush.app.data.remote.InfoPushApi
import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import com.infopush.app.data.remote.model.FavoriteResponse
import com.infopush.app.data.remote.model.FavoritesResponse
import com.infopush.app.data.remote.model.HomeSourcesResponse
import com.infopush.app.data.remote.model.SourceDto
import com.infopush.app.data.remote.model.SourceItemDto
import com.infopush.app.domain.FeedItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InfoPushRepositoryTest {
    private lateinit var db: InfoPushDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            InfoPushDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeFeed_localFirstThenRemoteRefreshAndWriteBack() = runTest {
        val fakeApi = FakeInfoPushApi(
            homeSourcesResponse = HomeSourcesResponse(
                sources = listOf(SourceDto(id = "tech", name = "Tech")),
                items = listOf(
                    SourceItemDto(
                        id = "remote-1",
                        sourceId = "tech",
                        title = "Remote item",
                        summary = "from api",
                        url = "https://example.com/remote",
                        publishedAt = "2026-02-19T01:00:00Z"
                    )
                )
            )
        )
        val repository = InfoPushRepository(db, fakeApi)

        db.sourceDao().upsertSources(listOf(SourceEntity(id = "tech", name = "Tech")))
        db.sourceDao().upsertSourceItems(
            listOf(
                SourceItemEntity(
                    id = "local-1",
                    sourceId = "tech",
                    title = "Local item",
                    summary = "cached",
                    url = "https://example.com/local",
                    publishedAt = "2026-02-19T00:00:00Z"
                )
            )
        )

        val firstEmission = repository.observeFeed("tech").first()
        assertEquals(1, firstEmission.size)
        assertEquals("local-1", firstEmission.first().id)

        val refreshResult = repository.refreshSourcesAndFeed()
        assertTrue(refreshResult is RefreshResult.Success)

        val refreshed = repository.observeFeed("tech").first()
        assertEquals(2, refreshed.size)
        assertTrue(refreshed.any { it.id == "remote-1" })

        val persisted = db.sourceDao().observeSourceItems("tech").first()
        assertEquals(2, persisted.size)
        assertTrue(persisted.any { it.id == "remote-1" })
    }

    @Test
    fun addFavorite_optimisticUpdateAndHandleDuplicate() = runTest {
        val fakeApi = FakeInfoPushApi(
            favoriteResponses = mutableListOf(
                FavoriteResponse(ok = true, duplicated = false),
                FavoriteResponse(ok = true, duplicated = true)
            )
        )
        val repository = InfoPushRepository(db, fakeApi)
        val item = FeedItem(
            id = "item-1",
            sourceId = "tech",
            title = "Kotlin 2.0",
            summary = "released",
            url = "https://example.com/1",
            publishedAt = "2026-02-19T00:00:00Z"
        )

        val firstResult = repository.addFavorite(item)
        assertEquals(AddFavoriteResult.Success, firstResult)

        val afterFirst = db.favoriteDao().observeAll().first()
        assertEquals(1, afterFirst.size)

        val secondResult = repository.addFavorite(item)
        assertEquals(AddFavoriteResult.Duplicated, secondResult)

        val afterSecond = db.favoriteDao().observeAll().first()
        assertEquals(1, afterSecond.size)
        assertEquals("item-1", afterSecond.first().itemId)
        assertEquals(2, fakeApi.addFavoriteRequests.size)
    }

    private class FakeInfoPushApi(
        private val homeSourcesResponse: HomeSourcesResponse = HomeSourcesResponse(),
        private val favoriteResponses: MutableList<FavoriteResponse> = mutableListOf(FavoriteResponse(ok = true))
    ) : InfoPushApi {
        val addFavoriteRequests: MutableList<FavoriteRequest> = mutableListOf()

        override suspend fun getHomeSources(): HomeSourcesResponse = homeSourcesResponse

        override suspend fun getFavorites(): FavoritesResponse = FavoritesResponse()

        override suspend fun addFavorite(request: FavoriteRequest): FavoriteResponse {
            addFavoriteRequests += request
            return if (favoriteResponses.isNotEmpty()) {
                favoriteResponses.removeAt(0)
            } else {
                FavoriteResponse(ok = true)
            }
        }

        override suspend fun exportData(): DataExportResponse = DataExportResponse()

        override suspend fun importData(request: DataImportRequest): DataImportResponse = DataImportResponse(ok = true)
    }
}
