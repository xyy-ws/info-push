package com.infopush.app.ui.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val todayFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun readableTime(
    raw: String,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw.ifBlank { "时间未知" }
    val duration = Duration.between(instant, now)
    if (!duration.isNegative) {
        val minutes = duration.toMinutes()
        when {
            minutes < 1 -> return "刚刚"
            minutes < 60 -> return "${minutes}分钟前"
        }
    }

    val dateTime = instant.atZone(zoneId)
    return if (dateTime.toLocalDate() == LocalDate.now(zoneId)) {
        "今天 ${dateTime.format(todayFormatter)}"
    } else {
        dateTime.format(dateFormatter)
    }
}
