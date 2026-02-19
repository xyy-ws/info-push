package com.infopush.app.ui.navigation

import android.net.Uri

data class ArticleNavData(
    val title: String,
    val url: String,
    val sourceId: String,
    val itemId: String
)

object ArticleRoute {
    const val WEB = "article/web"
    const val titleArg = "title"
    const val urlArg = "url"
    const val sourceIdArg = "sourceId"
    const val itemIdArg = "itemId"

    const val pattern = "$WEB?$titleArg={$titleArg}&$urlArg={$urlArg}&$sourceIdArg={$sourceIdArg}&$itemIdArg={$itemIdArg}"

    fun create(data: ArticleNavData): String {
        return "$WEB?$titleArg=${Uri.encode(data.title)}&$urlArg=${Uri.encode(data.url)}&$sourceIdArg=${Uri.encode(data.sourceId)}&$itemIdArg=${Uri.encode(data.itemId)}"
    }
}
