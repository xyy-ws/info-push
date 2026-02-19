package com.infopush.app.domain

import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.MessageEntity
import com.infopush.app.data.local.entity.PreferenceEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class ImportMode {
    REPLACE,
    MERGE
}

class ImportExportUseCase(
    private val store: Store,
    private val nowProvider: () -> String = { java.time.Instant.now().toString() }
) {
    interface Store {
        suspend fun snapshot(): ExportData
        suspend fun replaceAll(data: ExportData)
        suspend fun merge(data: ExportData)
    }

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(ExportPayload::class.java)

    suspend fun exportJson(version: Int = CURRENT_VERSION): String {
        return adapter.toJson(
            ExportPayload(
                version = version,
                exportedAt = nowProvider(),
                data = store.snapshot()
            )
        )
    }

    suspend fun importJson(json: String, mode: ImportMode) {
        val data = requireNotNull(adapter.fromJson(json)?.data) { "Invalid import payload" }
        when (mode) {
            ImportMode.REPLACE -> store.replaceAll(data)
            ImportMode.MERGE -> store.merge(data)
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

data class ExportPayload(
    val version: Int,
    val exportedAt: String,
    val data: ExportData
)

data class ExportData(
    val sources: List<SourceEntity> = emptyList(),
    val sourceItems: List<SourceItemEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val preferences: List<PreferenceEntity> = emptyList()
)
