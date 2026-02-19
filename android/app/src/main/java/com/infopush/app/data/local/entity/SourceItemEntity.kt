package com.infopush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_items")
data class SourceItemEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val summary: String,
    val url: String,
    val publishedAt: String
)
