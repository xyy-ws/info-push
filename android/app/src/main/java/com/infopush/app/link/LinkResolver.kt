package com.infopush.app.link

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

interface LinkResolver {
    suspend fun resolve(url: String, timeoutMs: Long = 2000): String?
}

class HttpLinkResolver : LinkResolver {
    private val unwrapKeys = setOf("url", "target", "u", "to", "redirect", "redirect_url")

    override suspend fun resolve(url: String, timeoutMs: Long): String? = withContext(Dispatchers.IO) {
        val seed = unwrapCommonJumpUrl(url)
        val normalizedSeed = LinkNormalizer.normalize(seed) ?: return@withContext null
        runCatching {
            val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()

            var current = normalizedSeed
            repeat(5) {
                val request = Request.Builder().url(current).get().build()
                client.newCall(request).execute().use { response ->
                    val location = response.header("Location")
                    if (location.isNullOrBlank()) return@runCatching current
                    val next = resolveAgainst(current, location)
                    current = LinkNormalizer.normalize(next) ?: return@runCatching current
                }
            }
            current
        }.getOrNull()
    }

    private fun unwrapCommonJumpUrl(url: String): String {
        return runCatching {
            val uri = URI(url)
            val query = uri.rawQuery ?: return@runCatching url
            query.split("&")
                .mapNotNull { pair ->
                    val idx = pair.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    val key = URLDecoder.decode(pair.substring(0, idx), Charsets.UTF_8.name())
                    val value = URLDecoder.decode(pair.substring(idx + 1), Charsets.UTF_8.name())
                    if (key.lowercase() in unwrapKeys) value else null
                }
                .firstOrNull { LinkNormalizer.normalize(it) != null }
                ?: url
        }.getOrDefault(url)
    }

    private fun resolveAgainst(base: String, location: String): String {
        return runCatching { URI(base).resolve(location).toString() }.getOrDefault(location)
    }
}

class LinkProcessor(
    private val resolver: LinkResolver = HttpLinkResolver()
) {
    suspend fun prepare(rawUrl: String): PreparedLink {
        val normalized = LinkNormalizer.normalize(rawUrl)
            ?: return PreparedLink.Invalid("链接无效：$rawUrl")

        val resolved = runCatching { resolver.resolve(normalized, 2000) }.getOrNull()
        val finalUrl = LinkNormalizer.normalize(resolved ?: normalized) ?: normalized
        return PreparedLink.Valid(finalUrl = finalUrl, fallbackUrl = normalized)
    }
}

sealed interface PreparedLink {
    data class Valid(val finalUrl: String, val fallbackUrl: String) : PreparedLink
    data class Invalid(val message: String) : PreparedLink
}
