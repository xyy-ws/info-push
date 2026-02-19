package com.infopush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val itemId: String,
    val sourceId: String,
    val title: String,
    val url: String,
    val createdAt: String
)
