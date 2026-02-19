package com.infopush.app.link

import java.net.URI

object LinkNormalizer {
    fun normalize(raw: String): String? {
        var value = raw.trim().trim('"', '\'', '“', '”')
        if (value.isBlank()) return null

        value = value.replace("：", ":").replace("／", "/")

        if (value.startsWith("//")) value = "https:$value"
        if (!value.contains("://") && value.matches(Regex("^[A-Za-z0-9.-]+\\.[A-Za-z]{2,}([/:?#].*)?$"))) {
            value = "https://$value"
        }

        value = value.replace(Regex("^(https?):/+"), "$1://")

        return runCatching {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            val host = uri.host ?: return null
            if (host.isBlank()) return null
            uri.toASCIIString()
        }.getOrNull()
    }
}
