package com.infopush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean
)
