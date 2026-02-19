package com.infopush.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
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
    fun sourceDao_insertAndQueryItems() = runTest {
        db.sourceDao().upsertSources(
            listOf(SourceEntity(id = "tech", name = "Tech"))
        )
        db.sourceDao().upsertSourceItems(
            listOf(
                SourceItemEntity(
                    id = "item-1",
                    sourceId = "tech",
                    title = "Kotlin 2.0",
                    summary = "released",
                    url = "https://example.com/1",
                    publishedAt = "2026-02-19T00:00:00Z"
                )
            )
        )

        val sources = db.sourceDao().observeSources().first()
        val items = db.sourceDao().observeSourceItems("tech").first()

        assertEquals(1, sources.size)
        assertEquals("Tech", sources.first().name)
        assertEquals(1, items.size)
        assertEquals("item-1", items.first().id)
    }

    @Test
    fun favoriteDao_upsertAndDelete() = runTest {
        db.favoriteDao().upsert(
            FavoriteEntity(
                itemId = "item-1",
                sourceId = "tech",
                title = "Kotlin 2.0",
                url = "https://example.com/1",
                createdAt = "2026-02-19T00:00:00Z"
            )
        )

        val inserted = db.favoriteDao().observeAll().first()
        assertEquals(1, inserted.size)

        db.favoriteDao().deleteByItemId("item-1")

        val deleted = db.favoriteDao().observeAll().first()
        assertEquals(0, deleted.size)
    }
}
