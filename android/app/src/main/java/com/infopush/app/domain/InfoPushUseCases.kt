package com.infopush.app.domain

import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.repo.AddFavoriteResult
import com.infopush.app.data.repo.InfoPushRepository
import kotlinx.coroutines.flow.Flow

class ObserveFeedUseCase(private val repository: InfoPushRepository) {
    operator fun invoke(sourceId: String): Flow<List<FeedItem>> = repository.observeFeed(sourceId)
}

class RefreshSourceUseCase(private val repository: InfoPushRepository) {
    suspend operator fun invoke(sourceId: String) = repository.refreshSource(sourceId)
}

class AddFavoriteUseCase(private val repository: InfoPushRepository) {
    suspend operator fun invoke(item: FeedItem): AddFavoriteResult = repository.addFavorite(item)
}

class RemoveFavoriteUseCase(private val repository: InfoPushRepository) {
    suspend operator fun invoke(itemId: String) = repository.removeFavorite(itemId)
}

class ObserveMessagesUseCase(private val repository: InfoPushRepository) {
    operator fun invoke(): Flow<List<InfoMessage>> = repository.observeMessages()
}

class ImportDataUseCase(private val repository: InfoPushRepository) {
    suspend operator fun invoke(request: DataImportRequest): DataImportResponse = repository.importData(request)
}

class ExportDataUseCase(private val repository: InfoPushRepository) {
    suspend operator fun invoke(): DataExportResponse = repository.exportData()
}
