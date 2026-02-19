package com.infopush.app.data.remote.model

data class SourceDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val url: String? = null
)

data class SourceItemDto(
    val id: String = "",
    val sourceId: String = "",
    val title: String = "",
    val summary: String? = null,
    val url: String? = null,
    val publishedAt: String? = null,
    val favoritedAt: String? = null
)

data class HomeSourcesResponse(
    val sources: List<SourceDto> = emptyList(),
    val items: List<SourceItemDto> = emptyList()
)

data class FavoriteRequest(
    val id: String? = null,
    val sourceId: String? = null,
    val title: String = "",
    val summary: String? = null,
    val url: String = "",
    val publishedAt: String? = null,
    val itemId: String? = null
)

data class FavoriteResponse(
    val ok: Boolean = false,
    val duplicated: Boolean = false,
    val item: SourceItemDto? = null,
    val error: String? = null
)

data class FavoritesResponse(
    val items: List<SourceItemDto> = emptyList()
)

data class MessageDto(
    val id: String = "",
    val title: String = "",
    val body: String? = null,
    val createdAt: String? = null
)

data class FavoriteExportDto(
    val itemId: String = ""
)

data class DataExportPayload(
    val sources: List<SourceDto> = emptyList(),
    val sourceItems: List<SourceItemDto> = emptyList(),
    val favorites: List<FavoriteExportDto> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val messages: List<MessageDto> = emptyList()
)

data class DataExportResponse(
    val version: Int = 1,
    val exportedAt: String = "",
    val data: DataExportPayload = DataExportPayload()
)

data class DataImportRequest(
    val mode: String = "merge",
    val data: DataExportPayload = DataExportPayload()
)

data class DataImportCounts(
    val sources: Int = 0,
    val sourceItems: Int = 0,
    val favorites: Int = 0,
    val messages: Int = 0
)

data class DataImportResponse(
    val ok: Boolean = false,
    val mode: String? = null,
    val counts: DataImportCounts = DataImportCounts(),
    val error: String? = null
)

data class AiDiscoverSourcesRequest(
    val keyword: String = ""
)

data class AiDiscoveredSourceDto(
    val name: String = "",
    val url: String = "",
    val type: String? = null,
    val reason: String? = null
)

data class AiDiscoverSourcesResponse(
    val sources: List<AiDiscoveredSourceDto> = emptyList()
)
