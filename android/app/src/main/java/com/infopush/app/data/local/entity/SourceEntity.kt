package com.infopush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String = "",
    val type: String = "rss",
    val tags: String = "",
    val enabled: Boolean = true,
    val backendSourceId: String? = null
)
