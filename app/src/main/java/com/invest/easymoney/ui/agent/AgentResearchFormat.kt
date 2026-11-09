package com.invest.easymoney.ui.agent

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats an ISO-8601 UTC timestamp (e.g. `2026-11-03T21:04:24Z` or with fractional seconds) as a
 * short local label such as `Nov 3, 4:04 PM`. Returns null when the value is missing or malformed.
 * Uses SimpleDateFormat because the app supports API 24 without java.time desugaring.
 */
internal fun formatAsOf(
    isoTimestamp: String?,
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): String? {
    val raw = isoTimestamp?.trim().orEmpty()
    if (raw.length < 19) return null
    val parsed: Date = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            this.timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(raw.substring(0, 19))
    }.getOrNull() ?: return null
    return SimpleDateFormat("MMM d, h:mm a", locale).apply { this.timeZone = timeZone }.format(parsed)
}

/** Short, human readable stage text for a running research request, based on elapsed seconds. */
internal fun researchProgressLabel(elapsedSeconds: Int): String = when {
    elapsedSeconds < 4 -> "Collecting the latest quote and news…"
    elapsedSeconds < 12 -> "Reviewing what was found…"
    elapsedSeconds < 30 -> "Preparing the summary…"
    else -> "Still working — this can take up to a minute."
}

