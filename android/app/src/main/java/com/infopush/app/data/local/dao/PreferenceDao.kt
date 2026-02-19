package com.infopush.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.infopush.app.data.local.entity.PreferenceEntity

@Dao
interface PreferenceDao {
    @Upsert
    suspend fun upsert(preference: PreferenceEntity)

    @Upsert
    suspend fun upsertAll(preferences: List<PreferenceEntity>)

    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): PreferenceEntity?

    @Query("SELECT * FROM preferences")
    suspend fun listAll(): List<PreferenceEntity>

    @Query("DELETE FROM preferences")
    suspend fun clearAll()
}
