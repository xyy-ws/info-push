package com.infopush.app.domain

data class FeedItem(
    val id: String,
    val sourceId: String,
    val title: String,
    val summary: String,
    val url: String,
    val publishedAt: String
)

data class InfoMessage(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: String
)
