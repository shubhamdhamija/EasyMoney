package com.invest.easymoney.data.repository

/** User-facing description of a failed research run. Never contains raw provider or HTTP details. */
data class ResearchFailure(val message: String, val retryable: Boolean)

/**
 * Maps the agent service's `error_code` to a clear message and a retry hint.
 * Unknown codes fall back to a generic, retryable message.
 */
fun describeResearchFailure(errorCode: String?, symbol: String?): ResearchFailure {
    val label = symbol?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
    return when (errorCode?.trim()?.uppercase()) {
        "QUOTE_NOT_FOUND" -> ResearchFailure(
            message = if (label != null) "No price data is available for $label." else "No price data is available for this symbol.",
            retryable = false,
        )
        "QUOTE_PROVIDER_UNAVAILABLE", "NEWS_PROVIDER_UNAVAILABLE" -> ResearchFailure(
            message = "The market data provider could not be reached. Please try again.",
            retryable = true,
        )
        "NEWS_NOT_FOUND" -> ResearchFailure(
            message = "No recent news was found, so a summary could not be prepared.",
            retryable = false,
        )
        "INSUFFICIENT_EVIDENCE" -> ResearchFailure(
            message = "There isn't enough information to produce a reliable summary.",
            retryable = false,
        )
        else -> ResearchFailure(
            message = "AI research could not be completed. Please try again.",
            retryable = true,
        )
    }
}

