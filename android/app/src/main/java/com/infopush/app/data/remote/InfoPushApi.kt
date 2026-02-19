package com.infopush.app.data.remote

import com.infopush.app.data.remote.model.AiDiscoverSourcesRequest
import com.infopush.app.data.remote.model.AiDiscoverSourcesResponse
import com.infopush.app.data.remote.model.CollectSourceResponse
import com.infopush.app.data.remote.model.CreateSourceRequest
import com.infopush.app.data.remote.model.CreateSourceResponse
import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import com.infopush.app.data.remote.model.FavoriteResponse
import com.infopush.app.data.remote.model.FavoritesResponse
import com.infopush.app.data.remote.model.HomeSourcesResponse
import com.infopush.app.data.remote.model.SourceItemsResponse
import com.infopush.app.data.remote.model.SourceListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InfoPushApi {
    @GET("v1/sources/home")
    suspend fun getHomeSources(): HomeSourcesResponse

    @GET("v1/favorites")
    suspend fun getFavorites(): FavoritesResponse

    @POST("v1/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): FavoriteResponse

    @GET("v1/data/export")
    suspend fun exportData(): DataExportResponse

    @POST("v1/data/import")
    suspend fun importData(@Body request: DataImportRequest): DataImportResponse

    @POST("v1/ai/discover-sources")
    suspend fun discoverSources(@Body request: AiDiscoverSourcesRequest): AiDiscoverSourcesResponse

    @GET("v1/sources")
    suspend fun getSources(): SourceListResponse

    @POST("v1/sources")
    suspend fun createSource(@Body request: CreateSourceRequest): CreateSourceResponse

    @POST("v1/sources/{sourceId}/collect")
    suspend fun collectSource(
        @Path("sourceId") sourceId: String,
        @Query("limit") limit: Int = 20
    ): CollectSourceResponse

    @GET("v1/sources/{sourceId}/items")
    suspend fun getSourceItems(
        @Path("sourceId") sourceId: String,
        @Query("limit") limit: Int = 20
    ): SourceItemsResponse
}
