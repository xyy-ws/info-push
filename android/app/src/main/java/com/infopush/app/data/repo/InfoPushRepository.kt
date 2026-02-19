package com.infopush.app.data.repo

import com.infopush.app.data.local.InfoPushDatabase
import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.MessageEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import com.infopush.app.data.remote.InfoPushApi
import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import com.infopush.app.domain.FeedItem
import com.infopush.app.domain.InfoMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InfoPushRepository(
    private val database: InfoPushDatabase,
    private val api: InfoPushApi
) {
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

    suspend fun refreshSource(sourceId: String) {
        val response = api.getHomeSources()
        database.sourceDao().upsertSources(
            response.sources.map { source ->
                SourceEntity(
                    id = source.id,
                    name = source.name
                )
            }
        )
        database.sourceDao().upsertSourceItems(
            response.items
                .filter { it.sourceId == sourceId }
                .map { item ->
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
}

enum class AddFavoriteResult {
    Success,
    Duplicated
}
