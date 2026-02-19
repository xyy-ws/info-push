package com.infopush.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Upsert
    suspend fun upsertSources(sources: List<SourceEntity>)

    @Upsert
    suspend fun upsertSource(source: SourceEntity)

    @Upsert
    suspend fun upsertSourceItems(items: List<SourceItemEntity>)

    @Query("SELECT * FROM sources ORDER BY id")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY id")
    suspend fun listSources(): List<SourceEntity>

    @Query("SELECT * FROM source_items WHERE sourceId = :sourceId ORDER BY publishedAt DESC")
    fun observeSourceItems(sourceId: String): Flow<List<SourceItemEntity>>

    @Query("SELECT * FROM source_items ORDER BY publishedAt DESC")
    suspend fun listSourceItems(): List<SourceItemEntity>

    @Query("SELECT * FROM sources WHERE id = :sourceId LIMIT 1")
    suspend fun getSourceById(sourceId: String): SourceEntity?

    @Query("SELECT * FROM sources WHERE url = :url LIMIT 1")
    suspend fun getSourceByUrl(url: String): SourceEntity?

    @Query("DELETE FROM sources WHERE id = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("DELETE FROM source_items WHERE sourceId = :sourceId")
    suspend fun deleteSourceItemsBySourceId(sourceId: String)

    @Query("DELETE FROM sources")
    suspend fun clearSources()

    @Query("DELETE FROM source_items")
    suspend fun clearSourceItems()
}
