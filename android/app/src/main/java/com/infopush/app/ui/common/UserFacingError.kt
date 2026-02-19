package com.infopush.app.ui.common

private data class ErrorHint(
    val title: String,
    val suggestion: String? = null
)

object UserFacingError {
    fun format(rawMessage: String?, fallback: String = "操作失败，请稍后重试"): String {
        val raw = rawMessage.orEmpty().trim()
        if (raw.isBlank()) return fallback

        val hint = mapHint(raw)
        val primary = hint?.title ?: cleanup(raw).ifBlank { fallback }
        val suggestion = hint?.suggestion
        val detail = buildString {
            append(primary)
            if (!suggestion.isNullOrBlank()) {
                append("\n")
                append(suggestion)
            }
            if (!sameMeaning(primary, raw)) {
                append("\n\n原始错误：")
                append(raw)
            }
        }
        return detail
    }

    private fun mapHint(raw: String): ErrorHint? {
        val normalized = raw.lowercase()
        return when {
            normalized.contains("source_probe_http_failed") && normalized.contains("http_status_403") -> {
                ErrorHint(
                    title = "该源拒绝访问(403)",
                    suggestion = "可更换镜像或新的源地址后重试"
                )
            }

            normalized.contains("source_probe_empty") || normalized.contains("feed_empty") -> {
                ErrorHint(
                    title = "该源暂无可解析内容",
                    suggestion = "可稍后重试，或切换到其他信息源"
                )
            }

            normalized.contains("source_probe_unsupported") -> {
                ErrorHint(
                    title = "暂不支持该源格式",
                    suggestion = "请改用 RSS/Atom 等受支持的源地址"
                )
            }

            normalized.contains("http 400") || normalized.contains("http 400 ") || normalized.contains("code 400") -> {
                ErrorHint(
                    title = "请求未通过(400)",
                    suggestion = "请检查源地址或稍后重试"
                )
            }

            else -> null
        }
    }

    private fun cleanup(text: String): String = text
        .replace("HTTP ", "HTTP ")
        .trim()

    private fun sameMeaning(primary: String, raw: String): Boolean {
        val p = primary.trim().lowercase()
        val r = raw.trim().lowercase()
        return p == r
    }
}
