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
    suspend fun upsertSourceItems(items: List<SourceItemEntity>)

    @Query("SELECT * FROM sources ORDER BY id")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM source_items WHERE sourceId = :sourceId ORDER BY publishedAt DESC")
    fun observeSourceItems(sourceId: String): Flow<List<SourceItemEntity>>
}
