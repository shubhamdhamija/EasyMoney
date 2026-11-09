package com.invest.easymoney.data.repository

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps transport errors to safe, user-facing text. Raw HTTP status codes and
 * exception messages must never reach the UI; log them in development only.
 */
internal fun Throwable.toAiInsightMessage(): String = toAiMessage("AI insight", "is")

internal fun Throwable.toAiPicksMessage(): String = toAiMessage("AI picks", "are")

private fun Throwable.toAiMessage(subject: String, verb: String): String =
    when (this) {
        is HttpException ->
            when (code()) {
                401, 402, 403 -> "$subject $verb temporarily unavailable."
                429 -> "$subject $verb receiving too many requests. Please try again later."
                // 503 is also what the backend returns when live AI is disabled.
                503 -> "$subject $verb temporarily unavailable."
                in 500..599 -> "$subject service is temporarily unavailable."
                else -> "$subject could not be loaded."
            }

        is SocketTimeoutException -> "The $subject request timed out."
        is UnknownHostException -> "Check your internet connection and try again."
        else -> "$subject could not be loaded."
    }
