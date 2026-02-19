package com.infopush.app.data.remote

import com.infopush.app.data.remote.model.DataExportResponse
import com.infopush.app.data.remote.model.DataImportRequest
import com.infopush.app.data.remote.model.DataImportResponse
import com.infopush.app.data.remote.model.FavoriteRequest
import com.infopush.app.data.remote.model.FavoriteResponse
import com.infopush.app.data.remote.model.FavoritesResponse
import com.infopush.app.data.remote.model.HomeSourcesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface InfoPushApi {
    @GET("/v1/sources/home")
    suspend fun getHomeSources(): HomeSourcesResponse

    @GET("/v1/favorites")
    suspend fun getFavorites(): FavoritesResponse

    @POST("/v1/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): FavoriteResponse

    @GET("/v1/data/export")
    suspend fun exportData(): DataExportResponse

    @POST("/v1/data/import")
    suspend fun importData(@Body request: DataImportRequest): DataImportResponse
}
