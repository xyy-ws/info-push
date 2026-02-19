package com.infopush.app.data.repo

import com.infopush.app.data.local.InfoPushDatabase
import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.MessageEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import com.infopush.app.data.remote.InfoPushApi
import com.infopush.app.data.remote.model.AiDiscoverSourcesRequest
import com.infopush.app.data.remote.model.CreateSourceRequest
import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import com.infopush.app.domain.ExportData
import com.infopush.app.domain.FeedItem
import com.infopush.app.domain.ImportExportUseCase
import com.infopush.app.domain.ImportMode
import com.infopush.app.domain.InfoMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class InfoPushRepository(
    private val database: InfoPushDatabase,
    private val api: InfoPushApi,
    private val enableMockFallback: Boolean = false
) {
    fun observeSources(): Flow<List<SourceEntity>> = database.sourceDao().observeSources()

    suspend fun addSource(name: String, url: String, type: String, tags: String) {
        database.sourceDao().upsertSource(
            SourceEntity(
                id = "local-${UUID.randomUUID()}",
                name = name.trim(),
                url = url.trim(),
                type = type.trim().ifBlank { "rss" },
                tags = tags.trim(),
                enabled = true
            )
        )
    }

    suspend fun searchAiSources(keyword: String): List<AiDiscoveredSource> {
        val normalized = keyword.trim()
        if (normalized.isBlank()) return emptyList()
        val response = api.discoverSources(
            AiDiscoverSourcesRequest(query = normalized, keyword = normalized)
        )
        val candidates = if (response.items.isNotEmpty()) response.items else response.sources
        return candidates.map {
            AiDiscoveredSource(
                name = it.name,
                url = it.url,
                type = it.type.orEmpty().ifBlank { "rss" },
                reason = it.reason.orEmpty()
            )
        }
    }

    suspend fun addAiSourceToLocal(candidate: AiDiscoveredSource): AddSourceResult {
        val normalizedUrl = candidate.url.trim()
        if (normalizedUrl.isBlank()) return AddSourceResult.Invalid("URL 不能为空")
        val duplicated = database.sourceDao().getSourceByUrl(normalizedUrl) != null
        if (duplicated) return AddSourceResult.Duplicated

        database.sourceDao().upsertSource(
            SourceEntity(
                id = "local-${UUID.randomUUID()}",
                name = candidate.name.trim().ifBlank { normalizedUrl },
                url = normalizedUrl,
                type = candidate.type.trim().ifBlank { "rss" },
                tags = candidate.reason.trim(),
                enabled = true
            )
        )
        return AddSourceResult.Success
    }

    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        val source = database.sourceDao().getSourceById(sourceId) ?: return
        database.sourceDao().upsertSource(source.copy(enabled = enabled))
    }

    suspend fun deleteSource(sourceId: String) {
        database.sourceDao().deleteSourceItemsBySourceId(sourceId)
        database.sourceDao().deleteSource(sourceId)
    }

    fun observeFeed(sourceId: String): Flow<List<FeedItem>> {
        return database.sourceDao().observeSourceItems(sourceId).map { items ->
            items.map { item ->
                FeedItem(
                    id = item.id,
                    sourceId = item.sourceId,
                    title = item.title,
                    summary = item.summary,
                    url = item.url,
                    publishedAt = item.publishedAt
                )
            }
        }
    }

    suspend fun refreshSource(sourceId: String): RefreshResult {
        if (sourceId.isBlank()) return RefreshResult.Error("sourceId 不能为空")
        val localSource = database.sourceDao().getSourceById(sourceId)
            ?: return RefreshResult.Error("信息源不存在")

        return try {
            val backendSourceId = resolveBackendSourceId(localSource)
            val collect = api.collectSource(backendSourceId, limit = 20)
            if (!collect.ok) {
                return RefreshResult.Error(collect.message ?: collect.error ?: "远端拉取失败")
            }
            val itemsResponse = api.getSourceItems(backendSourceId, limit = 50)
            database.sourceDao().upsertSourceItems(
                itemsResponse.items.map { item ->
                    SourceItemEntity(
                        id = item.id,
                        sourceId = localSource.id,
                        title = item.title,
                        summary = item.summary.orEmpty(),
                        url = item.url.orEmpty(),
                        publishedAt = item.publishedAt.orEmpty()
                    )
                }
            )
            RefreshResult.Success()
        } catch (t: Throwable) {
            RefreshResult.Error("信息源刷新失败: ${t.message.orEmpty()}")
        }
    }

    suspend fun refreshSourcesAndFeed(): RefreshResult {
        return try {
            val response = api.getHomeSources()
            database.sourceDao().upsertSources(
                response.sources.map { source ->
                    SourceEntity(
                        id = source.id,
                        name = source.name,
                        url = source.url.orEmpty(),
                        type = "rss",
                        tags = source.description.orEmpty(),
                        enabled = true,
                        backendSourceId = source.id
                    )
                }
            )
            database.sourceDao().upsertSourceItems(
                response.items.map { item ->
                    SourceItemEntity(
                        id = item.id,
                        sourceId = item.sourceId,
                        title = item.title,
                        summary = item.summary.orEmpty(),
                        url = item.url.orEmpty(),
                        publishedAt = item.publishedAt.orEmpty()
                    )
                }
            )
            RefreshResult.Success()
        } catch (t: Throwable) {
            applySourcesMockFallback(t)
        }
    }

    private suspend fun resolveBackendSourceId(localSource: SourceEntity): String {
        localSource.backendSourceId?.takeIf { it.isNotBlank() }?.let { return it }

        val existingRemote = api.getSources().items.firstOrNull { it.url == localSource.url }
        if (existingRemote != null) {
            cacheBackendSourceId(localSource.id, existingRemote.id)
            return existingRemote.id
        }

        val createResp = api.createSource(
            CreateSourceRequest(
                name = localSource.name,
                url = localSource.url,
                type = localSource.type,
                reason = localSource.tags.ifBlank { null },
                enabled = localSource.enabled
            )
        )
        val backendId = createResp.item?.id
            ?: throw IllegalStateException(createResp.message ?: createResp.error ?: "远端注册信息源失败")
        cacheBackendSourceId(localSource.id, backendId)
        return backendId
    }

    private suspend fun cacheBackendSourceId(localSourceId: String, backendSourceId: String) {
        val latest = database.sourceDao().getSourceById(localSourceId) ?: return
        database.sourceDao().upsertSource(latest.copy(backendSourceId = backendSourceId))
    }

    fun observeFavorites(): Flow<List<FeedItem>> {
        return database.favoriteDao().observeAll().map { favorites ->
            favorites.map {
                FeedItem(
                    id = it.itemId,
                    sourceId = it.sourceId,
                    title = it.title,
                    summary = "",
                    url = it.url,
                    publishedAt = it.createdAt
                )
            }
        }
    }

    suspend fun refreshFavorites(): RefreshResult {
        return try {
            val response = api.getFavorites()
            response.items.forEach { item ->
                database.favoriteDao().upsert(
                    FavoriteEntity(
                        itemId = item.id,
                        sourceId = item.sourceId,
                        title = item.title,
                        url = item.url.orEmpty(),
                        createdAt = item.favoritedAt ?: item.publishedAt.orEmpty()
                    )
                )
            }
            RefreshResult.Success()
        } catch (t: Throwable) {
            applyFavoritesMockFallback(t)
        }
    }

    suspend fun addFavorite(item: FeedItem): AddFavoriteResult {
        val favorite = FavoriteEntity(
            itemId = item.id,
            sourceId = item.sourceId,
            title = item.title,
            url = item.url,
            createdAt = item.publishedAt
        )
        database.favoriteDao().upsert(favorite)

        val response = api.addFavorite(
            FavoriteRequest(
                id = item.id,
                sourceId = item.sourceId,
                title = item.title,
                summary = item.summary,
                url = item.url,
                publishedAt = item.publishedAt,
                itemId = item.id
            )
        )

        return if (response.duplicated) AddFavoriteResult.Duplicated else AddFavoriteResult.Success
    }

    suspend fun removeFavorite(itemId: String) {
        database.favoriteDao().deleteByItemId(itemId)
    }

    fun observeMessages(): Flow<List<InfoMessage>> {
        return database.messageDao().observeAll().map { messages ->
            messages.map { msg ->
                InfoMessage(
                    id = msg.id,
                    title = msg.title,
                    body = msg.body,
                    createdAt = msg.createdAt
                )
            }
        }
    }

    suspend fun refreshMessages(): RefreshResult {
        return try {
            val remote = api.exportData().data.messages
            upsertMessages(
                remote.map {
                    InfoMessage(
                        id = it.id,
                        title = it.title,
                        body = it.body.orEmpty(),
                        createdAt = it.createdAt.orEmpty()
                    )
                }
            )
            RefreshResult.Success()
        } catch (t: Throwable) {
            applyMessagesMockFallback(t)
        }
    }

    suspend fun importData(request: DataImportRequest): DataImportResponse {
        return api.importData(request)
    }

    suspend fun exportData(): DataExportResponse {
        return api.exportData()
    }

    suspend fun upsertMessages(messages: List<InfoMessage>) {
        database.messageDao().upsertAll(
            messages.map { message ->
                MessageEntity(
                    id = message.id,
                    title = message.title,
                    body = message.body,
                    createdAt = message.createdAt,
                    read = false
                )
            }
        )
    }

    suspend fun exportLocalJson(nowProvider: () -> String = { java.time.Instant.now().toString() }): String {
        val useCase = ImportExportUseCase(LocalStore(database), nowProvider)
        return useCase.exportJson()
    }

    suspend fun importLocalJson(json: String, mode: ImportMode) {
        val useCase = ImportExportUseCase(LocalStore(database))
        useCase.importJson(json, mode)
    }

    private suspend fun applySourcesMockFallback(t: Throwable): RefreshResult {
        if (!enableMockFallback) return RefreshResult.Error("信息源加载失败: ${t.message.orEmpty()}")

        database.sourceDao().upsertSources(MockData.sources)
        database.sourceDao().upsertSourceItems(MockData.sourceItems)
        return RefreshResult.Success(fromMock = true)
    }

    private suspend fun applyFavoritesMockFallback(t: Throwable): RefreshResult {
        if (!enableMockFallback) return RefreshResult.Error("收藏加载失败: ${t.message.orEmpty()}")

        database.favoriteDao().upsertAll(MockData.favorites)
        return RefreshResult.Success(fromMock = true)
    }

    private suspend fun applyMessagesMockFallback(t: Throwable): RefreshResult {
        if (!enableMockFallback) return RefreshResult.Error("消息加载失败: ${t.message.orEmpty()}")

        database.messageDao().upsertAll(MockData.messages)
        return RefreshResult.Success(fromMock = true)
    }
}

private class LocalStore(
    private val database: InfoPushDatabase
) : ImportExportUseCase.Store {
    override suspend fun snapshot(): ExportData {
        return ExportData(
            sources = database.sourceDao().listSources(),
            sourceItems = database.sourceDao().listSourceItems(),
            favorites = database.favoriteDao().listAll(),
            messages = database.messageDao().listAll(),
            preferences = database.preferenceDao().listAll()
        )
    }

    override suspend fun replaceAll(data: ExportData) {
        database.sourceDao().clearSourceItems()
        database.sourceDao().clearSources()
        database.favoriteDao().clearAll()
        database.messageDao().clearAll()
        database.preferenceDao().clearAll()
        merge(data)
    }

    override suspend fun merge(data: ExportData) {
        database.sourceDao().upsertSources(data.sources)
        database.sourceDao().upsertSourceItems(data.sourceItems)
        database.favoriteDao().upsertAll(data.favorites)
        database.messageDao().upsertAll(data.messages)
        database.preferenceDao().upsertAll(data.preferences)
    }
}

sealed interface RefreshResult {
    data class Success(val fromMock: Boolean = false) : RefreshResult
    data class Error(val message: String) : RefreshResult
}

data class AiDiscoveredSource(
    val name: String,
    val url: String,
    val type: String,
    val reason: String
)

sealed interface AddSourceResult {
    object Success : AddSourceResult
    object Duplicated : AddSourceResult
    data class Invalid(val message: String) : AddSourceResult
}

enum class AddFavoriteResult {
    Success,
    Duplicated
}

private object MockData {
    val sources = listOf(
        SourceEntity(id = "mock-tech", name = "Mock 科技"),
        SourceEntity(id = "mock-world", name = "Mock 全球")
    )

    val sourceItems = listOf(
        SourceItemEntity(
            id = "mock-item-1",
            sourceId = "mock-tech",
            title = "Mock: Kotlin 发布新版本",
            summary = "用于 API 不可用时的本地展示",
            url = "https://example.com/mock/1",
            publishedAt = "2026-02-19T00:00:00Z"
        ),
        SourceItemEntity(
            id = "mock-item-2",
            sourceId = "mock-world",
            title = "Mock: 世界热点",
            summary = "兜底示例内容",
            url = "https://example.com/mock/2",
            publishedAt = "2026-02-19T00:10:00Z"
        )
    )

    val favorites = listOf(
        FavoriteEntity(
            itemId = "mock-item-1",
            sourceId = "mock-tech",
            title = "Mock: Kotlin 发布新版本",
            url = "https://example.com/mock/1",
            createdAt = "2026-02-19T00:00:00Z"
        )
    )

    val messages = listOf(
        MessageEntity(
            id = "mock-msg-1",
            title = "Mock 消息",
            body = "当前处于离线兜底模式",
            createdAt = "2026-02-19T00:00:00Z",
            read = false
        )
    )
}
