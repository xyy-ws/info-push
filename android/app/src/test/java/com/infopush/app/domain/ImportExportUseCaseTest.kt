package com.infopush.app.domain

import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.MessageEntity
import com.infopush.app.data.local.entity.PreferenceEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportUseCaseTest {

    @Test
    fun `export should include version exportedAt and data`() = runTest {
        val store = FakeStore(snapshotData = sampleData())
        val useCase = ImportExportUseCase(store) { "2026-02-19T05:38:00Z" }

        val json = useCase.exportJson()
        val payload = parsePayload(json)

        assertEquals(1, payload.version)
        assertEquals("2026-02-19T05:38:00Z", payload.exportedAt)
        assertTrue(payload.data.sources.isNotEmpty())
        assertTrue(payload.data.sourceItems.isNotEmpty())
        assertTrue(payload.data.favorites.isNotEmpty())
        assertTrue(payload.data.messages.isNotEmpty())
        assertTrue(payload.data.preferences.isNotEmpty())
    }

    @Test
    fun `import replace should call replaceAll`() = runTest {
        val store = FakeStore(snapshotData = sampleData())
        val useCase = ImportExportUseCase(store)
        val json = useCase.exportJson()

        useCase.importJson(json, ImportMode.REPLACE)

        assertEquals(1, store.replaceCalls)
        assertEquals(0, store.mergeCalls)
        assertEquals("source-1", store.lastImported?.sources?.first()?.id)
    }

    @Test
    fun `import merge should call merge`() = runTest {
        val store = FakeStore(snapshotData = sampleData())
        val useCase = ImportExportUseCase(store)
        val json = useCase.exportJson()

        useCase.importJson(json, ImportMode.MERGE)

        assertEquals(0, store.replaceCalls)
        assertEquals(1, store.mergeCalls)
        assertEquals("item-1", store.lastImported?.sourceItems?.first()?.id)
    }

    private fun parsePayload(json: String): ExportPayload {
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ExportPayload::class.java)
        return requireNotNull(adapter.fromJson(json))
    }

    private fun sampleData() = ExportData(
        sources = listOf(SourceEntity(id = "source-1", name = "Tech")),
        sourceItems = listOf(
            SourceItemEntity(
                id = "item-1",
                sourceId = "source-1",
                title = "hello",
                summary = "world",
                url = "https://example.com",
                publishedAt = "2026-02-19T00:00:00Z"
            )
        ),
        favorites = listOf(
            FavoriteEntity(
                itemId = "item-1",
                sourceId = "source-1",
                title = "hello",
                url = "https://example.com",
                createdAt = "2026-02-19T00:00:00Z"
            )
        ),
        messages = listOf(
            MessageEntity(
                id = "msg-1",
                title = "t",
                body = "b",
                createdAt = "2026-02-19T00:00:00Z",
                read = false
            )
        ),
        preferences = listOf(PreferenceEntity(key = "theme", value = "dark"))
    )
}

private class FakeStore(
    private val snapshotData: ExportData
) : ImportExportUseCase.Store {
    var replaceCalls = 0
    var mergeCalls = 0
    var lastImported: ExportData? = null

    override suspend fun snapshot(): ExportData = snapshotData

    override suspend fun replaceAll(data: ExportData) {
        replaceCalls += 1
        lastImported = data
    }

    override suspend fun merge(data: ExportData) {
        mergeCalls += 1
        lastImported = data
    }
}
